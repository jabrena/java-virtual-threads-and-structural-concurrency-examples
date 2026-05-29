#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
COMPOSE_FILE="${REPO_ROOT}/frameworks/docker-compose.yml"
BENCHMARK_DIR="${REPO_ROOT}/benchmark/gatling-frameworks"
SUMMARY_SCRIPT="${SCRIPT_DIR}/generate-framework-summary.sh"
RSS_SAMPLE_INTERVAL_SECONDS="${RSS_SAMPLE_INTERVAL_SECONDS:-2}"
RSS_SAMPLES_FILE=""
RSS_SAMPLER_PID=""

PROFILE="${1:-${PROFILE:-baseline}}"

print_usage() {
  cat <<EOF
Usage: $0 [profile]

Profiles:
  smoke     5 users/sec per framework for 30 seconds
  baseline  25 users/sec per framework for 120 seconds
  stress    75 users/sec per framework for 180 seconds
  heavy     150 users/sec per framework for 300 seconds
  soak      50 users/sec per framework for 900 seconds

Environment overrides:
  USERS_PER_SECOND=100 DURATION_SECONDS=120 WARMUP_USERS=20 $0 custom
EOF
}

configure_profile() {
  case "${PROFILE}" in
    smoke)
      DEFAULT_USERS_PER_SECOND=5
      DEFAULT_DURATION_SECONDS=30
      DEFAULT_WARMUP_USERS=3
      ;;
    baseline)
      DEFAULT_USERS_PER_SECOND=25
      DEFAULT_DURATION_SECONDS=120
      DEFAULT_WARMUP_USERS=10
      ;;
    stress)
      DEFAULT_USERS_PER_SECOND=75
      DEFAULT_DURATION_SECONDS=180
      DEFAULT_WARMUP_USERS=20
      ;;
    heavy)
      DEFAULT_USERS_PER_SECOND=150
      DEFAULT_DURATION_SECONDS=300
      DEFAULT_WARMUP_USERS=30
      ;;
    soak)
      DEFAULT_USERS_PER_SECOND=50
      DEFAULT_DURATION_SECONDS=900
      DEFAULT_WARMUP_USERS=20
      ;;
    custom)
      DEFAULT_USERS_PER_SECOND=10
      DEFAULT_DURATION_SECONDS=30
      DEFAULT_WARMUP_USERS=3
      ;;
    -h|--help|help)
      print_usage
      exit 0
      ;;
    *)
      echo "Unknown benchmark profile: ${PROFILE}" >&2
      print_usage >&2
      exit 2
      ;;
  esac

  USERS_PER_SECOND="${USERS_PER_SECOND:-${DEFAULT_USERS_PER_SECOND}}"
  DURATION_SECONDS="${DURATION_SECONDS:-${DEFAULT_DURATION_SECONDS}}"
  WARMUP_USERS="${WARMUP_USERS:-${DEFAULT_WARMUP_USERS}}"
}

compose() {
  docker compose -f "${COMPOSE_FILE}" "$@"
}

stop_compose() {
  echo "Stopping Docker Compose stack..."
  compose down
}

cleanup() {
  stop_rss_sampler
  stop_compose
}

wait_for_endpoint() {
  local name="$1"
  local url="$2"
  local attempts=120

  echo "Waiting for ${name} at ${url}..."
  for ((attempt = 1; attempt <= attempts; attempt++)); do
    if curl -fsS --max-time 2 "${url}" >/dev/null 2>&1; then
      echo "${name} is ready."
      return 0
    fi
    sleep 2
  done

  echo "Timed out waiting for ${name} at ${url}." >&2
  return 1
}

container_rss_bytes() {
  local container="$1"

  docker exec "${container}" sh -c '
    page_size="$(getconf PAGESIZE)"
    total_pages=0
    for statm in /proc/[0-9]*/statm; do
      if read -r _ rss_pages _ < "${statm}"; then
        total_pages=$((total_pages + rss_pages))
      fi
    done
    echo $((total_pages * page_size))
  ' 2>/dev/null || echo 0
}

sample_rss_once() {
  local timestamp
  timestamp="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"

  printf "%s,01 Spring Boot,fruit-store-spring-boot,%s\n" "${timestamp}" "$(container_rss_bytes fruit-store-spring-boot)" >> "${RSS_SAMPLES_FILE}"
  printf "%s,02 Quarkus,fruit-store-quarkus,%s\n" "${timestamp}" "$(container_rss_bytes fruit-store-quarkus)" >> "${RSS_SAMPLES_FILE}"
  printf "%s,03 Micronaut,fruit-store-micronaut,%s\n" "${timestamp}" "$(container_rss_bytes fruit-store-micronaut)" >> "${RSS_SAMPLES_FILE}"
}

start_rss_sampler() {
  mkdir -p "${BENCHMARK_DIR}/target"
  RSS_SAMPLES_FILE="${BENCHMARK_DIR}/target/rss-samples-$(date -u +"%Y%m%d%H%M%S").csv"
  echo "timestamp,framework,container,rss_bytes" > "${RSS_SAMPLES_FILE}"

  echo "Sampling framework container RSS every ${RSS_SAMPLE_INTERVAL_SECONDS}s: ${RSS_SAMPLES_FILE}"
  (
    while true; do
      sample_rss_once
      sleep "${RSS_SAMPLE_INTERVAL_SECONDS}"
    done
  ) &
  RSS_SAMPLER_PID="$!"
}

stop_rss_sampler() {
  if [[ -n "${RSS_SAMPLER_PID}" ]] && kill -0 "${RSS_SAMPLER_PID}" >/dev/null 2>&1; then
    kill "${RSS_SAMPLER_PID}" >/dev/null 2>&1 || true
    wait "${RSS_SAMPLER_PID}" >/dev/null 2>&1 || true
  fi
  RSS_SAMPLER_PID=""
}

latest_report() {
  local report
  report="$(find "${BENCHMARK_DIR}/target/gatling" -mindepth 2 -maxdepth 2 -name index.html -type f -print 2>/dev/null \
    | sort \
    | tail -n 1)"

  if [[ -z "${report}" ]]; then
    echo "No Gatling report found under ${BENCHMARK_DIR}/target/gatling." >&2
    return 1
  fi

  echo "${report}"
}

open_file() {
  local report="$1"

  echo "Opening benchmark report: ${report}"
  if command -v open >/dev/null 2>&1; then
    open "${report}"
  elif command -v xdg-open >/dev/null 2>&1; then
    xdg-open "${report}"
  else
    echo "Open this report manually: ${report}"
  fi
}

open_report() {
  local report
  local summary
  local report_dir

  report="$(latest_report)"
  report_dir="$(cd "$(dirname "${report}")" && pwd)"
  if [[ -n "${RSS_SAMPLES_FILE}" && -f "${RSS_SAMPLES_FILE}" ]]; then
    cp "${RSS_SAMPLES_FILE}" "${report_dir}/rss-samples.csv"
    summary="$("${SUMMARY_SCRIPT}" "${report}" "${report_dir}/rss-samples.csv")"
  else
    summary="$("${SUMMARY_SCRIPT}" "${report}")"
  fi
  echo "Generated framework summary: ${summary}"
  open_file "${summary}"
}

configure_profile

cd "${REPO_ROOT}"

echo "Benchmark profile: ${PROFILE}"
echo "Load per framework: ${USERS_PER_SECOND} users/sec, ${DURATION_SECONDS}s duration, ${WARMUP_USERS} warmup users"
echo "Total target load across 3 frameworks: $(awk "BEGIN { print ${USERS_PER_SECOND} * 3 }") users/sec"

echo "Starting Docker Compose stack..."
trap cleanup EXIT
compose up -d --build

wait_for_endpoint "Spring Boot" "http://localhost:8080/fruits"
wait_for_endpoint "Quarkus" "http://localhost:8081/fruits"
wait_for_endpoint "Micronaut" "http://localhost:8082/fruits"

start_rss_sampler

echo "Running Gatling benchmark..."
(
  cd "${BENCHMARK_DIR}"
  mvn gatling:test \
    -DusersPerSecond="${USERS_PER_SECOND}" \
    -DdurationSeconds="${DURATION_SECONDS}" \
    -DwarmupUsers="${WARMUP_USERS}"
)
stop_rss_sampler

trap - EXIT
stop_compose
open_report
