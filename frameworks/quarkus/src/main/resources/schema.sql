create sequence if not exists fruits_seq start with 1 increment by 1;
create sequence if not exists stores_seq start with 1 increment by 1;

drop table if exists store_fruit_prices;
drop table if exists stores;
drop table if exists fruits;

create table fruits (
  id bigint primary key default nextval('fruits_seq'),
  name varchar(255) not null unique,
  description varchar(255)
);

create table stores (
  id bigint primary key default nextval('stores_seq'),
  name varchar(255) not null unique,
  currency varchar(255) not null,
  address varchar(255) not null,
  city varchar(255) not null,
  country varchar(255) not null
);

create table store_fruit_prices (
  store_id bigint not null references stores(id),
  fruit_id bigint not null references fruits(id),
  price numeric(12, 2) not null,
  primary key (store_id, fruit_id)
);
