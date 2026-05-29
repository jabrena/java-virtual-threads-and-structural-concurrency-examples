#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 || $# -gt 2 ]]; then
  echo "Usage: $0 /path/to/gatling-report/index.html [/path/to/rss-samples.csv]" >&2
  exit 2
fi

REPORT_INDEX="$1"
RSS_SAMPLES="${2:-}"
REPORT_DIR="$(cd "$(dirname "${REPORT_INDEX}")" && pwd)"
SUMMARY_FILE="${REPORT_DIR}/framework-summary.html"

if [[ ! -f "${REPORT_INDEX}" ]]; then
  echo "Gatling report index not found: ${REPORT_INDEX}" >&2
  exit 1
fi

LC_ALL=C perl -MHTML::Entities - "${REPORT_INDEX}" "${SUMMARY_FILE}" "${RSS_SAMPLES}" <<'PERL'
use strict;
use warnings;

my ($index_file, $summary_file, $rss_samples_file) = @ARGV;
open my $in, '<', $index_file or die "Cannot read $index_file: $!";
my $html = do {
  local $/;
  <$in>;
};
close $in;

my %colors = (
  '01 Spring Boot' => '#f15b4f',
  '02 Quarkus' => '#68b65c',
  '03 Micronaut' => '#5E7BE2',
);

my %rss = map { $_ => { samples => 0, total => 0, max => 0 } } keys %colors;

if (defined $rss_samples_file && length $rss_samples_file && -f $rss_samples_file) {
  open my $rss_in, '<', $rss_samples_file or die "Cannot read $rss_samples_file: $!";
  my $header = <$rss_in>;
  while (my $line = <$rss_in>) {
    chomp $line;
    my ($timestamp, $framework, $container, $rss_bytes) = split /,/, $line;
    next unless defined $framework && exists $rss{$framework};
    next unless defined $rss_bytes && $rss_bytes =~ /^\d+$/;

    $rss{$framework}{samples}++;
    $rss{$framework}{total} += $rss_bytes;
    $rss{$framework}{max} = $rss_bytes if $rss_bytes > $rss{$framework}{max};
  }
  close $rss_in;
}

my @rows;
while ($html =~ m{<tr id="(group_[^"]+)" data-parent="ROOT">(.*?)</tr>}sg) {
  my ($id, $row_html) = ($1, $2);
  my ($name) = $row_html =~ m{class="ellipsed-name">([^<]+)</span>}s;
  next unless defined $name && exists $colors{$name};

  my @values = $row_html =~ m{<td class="value [^"]+">([^<]+)</td>}g;
  next unless @values >= 13;

  push @rows, {
    id => $id,
    name => decode_entities($name),
    color => $colors{$name},
    total => $values[0],
    ok => $values[1],
    ko => $values[2],
    ko_percent => $values[3],
    throughput => $values[4],
    min => $values[5],
    p50 => $values[6],
    p75 => $values[7],
    p95 => $values[8],
    p99 => $values[9],
    max => $values[10],
    mean => $values[11],
    stddev => $values[12],
  };
}

for my $row (@rows) {
  my $stats = $rss{$row->{name}};
  if ($stats && $stats->{samples} > 0) {
    $row->{rss_avg} = format_bytes($stats->{total} / $stats->{samples});
    $row->{rss_max} = format_bytes($stats->{max});
    $row->{rss_samples} = $stats->{samples};
  } else {
    $row->{rss_avg} = 'n/a';
    $row->{rss_max} = 'n/a';
    $row->{rss_samples} = 0;
  }
}

@rows = sort { $a->{name} cmp $b->{name} } @rows;
die "No framework group rows found in $index_file\n" unless @rows;

sub format_bytes {
  my ($bytes) = @_;
  return '0 MiB' unless defined $bytes && $bytes > 0;
  return sprintf '%.1f MiB', $bytes / 1024 / 1024;
}

my $generated_at = scalar localtime;
open my $out, '>', $summary_file or die "Cannot write $summary_file: $!";

print {$out} <<"HTML";
<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Framework Benchmark Summary</title>
  <style>
    :root {
      color-scheme: light dark;
      --background: #f7f7f7;
      --surface: #ffffff;
      --text: #1f2024;
      --muted: #646b75;
      --border: #d9dde3;
      --ok: #68b65c;
      --ko: #f15b4f;
    }
    \@media (prefers-color-scheme: dark) {
      :root {
        --background: #1e2225;
        --surface: #272c30;
        --text: #dee2e6;
        --muted: #aeb6bf;
        --border: #555;
      }
    }
    * { box-sizing: border-box; }
    body {
      margin: 0;
      background: var(--background);
      color: var(--text);
      font-family: Arial, sans-serif;
      font-size: 14px;
    }
    main {
      max-width: 1180px;
      margin: 0 auto;
      padding: 28px 18px 42px;
    }
    header {
      display: flex;
      align-items: flex-end;
      justify-content: space-between;
      gap: 16px;
      margin-bottom: 18px;
    }
    h1 {
      margin: 0 0 6px;
      font-size: 28px;
      font-weight: 700;
    }
    p {
      margin: 0;
      color: var(--muted);
    }
    a {
      color: inherit;
      font-weight: 700;
    }
    .summary-grid {
      display: grid;
      grid-template-columns: repeat(3, minmax(0, 1fr));
      gap: 12px;
      margin: 18px 0;
    }
    .framework {
      background: var(--surface);
      border: 1px solid var(--border);
      border-top: 6px solid var(--framework-color);
      border-radius: 8px;
      padding: 14px;
    }
    .framework h2 {
      margin: 0 0 12px;
      font-size: 18px;
    }
    .metric {
      display: flex;
      justify-content: space-between;
      gap: 12px;
      border-top: 1px solid var(--border);
      padding: 8px 0;
    }
    .metric:first-of-type {
      border-top: 0;
    }
    .label {
      color: var(--muted);
    }
    .value {
      font-weight: 700;
      text-align: right;
    }
    .ok { color: var(--ok); }
    .ko { color: var(--ko); }
    table {
      width: 100%;
      border-collapse: collapse;
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: 8px;
      overflow: hidden;
    }
    th, td {
      padding: 10px 12px;
      border-bottom: 1px solid var(--border);
      text-align: right;
      white-space: nowrap;
    }
    th:first-child, td:first-child {
      text-align: left;
    }
    tr:last-child td {
      border-bottom: 0;
    }
    .name {
      border-left: 6px solid var(--framework-color);
      font-weight: 700;
    }
    footer {
      margin-top: 14px;
      color: var(--muted);
      font-size: 12px;
    }
    \@media (max-width: 900px) {
      .summary-grid {
        grid-template-columns: 1fr;
      }
      header {
        align-items: flex-start;
        flex-direction: column;
      }
      .table-wrap {
        overflow-x: auto;
      }
    }
  </style>
</head>
<body>
<main>
  <header>
    <div>
      <h1>Framework Benchmark Summary</h1>
      <p>Framework-colored summary extracted from the Gatling stats table.</p>
    </div>
    <p><a href="index.html">Open full Gatling report</a></p>
  </header>
  <section class="summary-grid" aria-label="Framework cards">
HTML

for my $row (@rows) {
  print {$out} <<"HTML";
    <article class="framework" style="--framework-color: $row->{color}">
      <h2>$row->{name}</h2>
      <div class="metric"><span class="label">Requests</span><span class="value">$row->{total}</span></div>
      <div class="metric"><span class="label">OK</span><span class="value ok">$row->{ok}</span></div>
      <div class="metric"><span class="label">KO</span><span class="value ko">$row->{ko}</span></div>
      <div class="metric"><span class="label">Throughput</span><span class="value">$row->{throughput} req/s</span></div>
      <div class="metric"><span class="label">p95</span><span class="value">$row->{p95} ms</span></div>
      <div class="metric"><span class="label">p99</span><span class="value">$row->{p99} ms</span></div>
      <div class="metric"><span class="label">Avg RSS</span><span class="value">$row->{rss_avg}</span></div>
      <div class="metric"><span class="label">Max RSS</span><span class="value">$row->{rss_max}</span></div>
    </article>
HTML
}

print {$out} <<"HTML";
  </section>
  <section class="table-wrap" aria-label="Framework comparison table">
    <table>
      <thead>
        <tr>
          <th>Framework</th>
          <th>Total</th>
          <th>OK</th>
          <th>KO</th>
          <th>KO %</th>
          <th>Req/s</th>
          <th>Min</th>
          <th>p50</th>
          <th>p75</th>
          <th>p95</th>
          <th>p99</th>
          <th>Max</th>
          <th>Mean</th>
          <th>Std dev</th>
          <th>Avg RSS</th>
          <th>Max RSS</th>
        </tr>
      </thead>
      <tbody>
HTML

for my $row (@rows) {
  print {$out} <<"HTML";
        <tr>
          <td class="name" style="--framework-color: $row->{color}"><a href="$row->{id}.html">$row->{name}</a></td>
          <td>$row->{total}</td>
          <td class="ok">$row->{ok}</td>
          <td class="ko">$row->{ko}</td>
          <td>$row->{ko_percent}</td>
          <td>$row->{throughput}</td>
          <td>$row->{min} ms</td>
          <td>$row->{p50} ms</td>
          <td>$row->{p75} ms</td>
          <td>$row->{p95} ms</td>
          <td>$row->{p99} ms</td>
          <td>$row->{max} ms</td>
          <td>$row->{mean} ms</td>
          <td>$row->{stddev} ms</td>
          <td>$row->{rss_avg}</td>
          <td>$row->{rss_max}</td>
        </tr>
HTML
}

print {$out} <<"HTML";
      </tbody>
    </table>
  </section>
  <footer>Generated at $generated_at from <code>index.html</code>.</footer>
</main>
</body>
</html>
HTML

close $out;
print "$summary_file\n";
PERL
