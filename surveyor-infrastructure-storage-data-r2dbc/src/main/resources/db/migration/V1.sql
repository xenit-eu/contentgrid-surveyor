create extension if not exists timescaledb;

create table resource (
    id bigint primary key generated always as identity,
    source_system text not null,
    resource_type text not null,
    resource_id text not null,
    metric_name text not null
);
create unique index resource_identity on resource (source_system, resource_type, resource_id, metric_name);

create table metric_events (
    resource_id bigint references resource(id) not null,
    start_time timestamptz not null,
    end_time timestamptz not null,
    "value" numeric not null
);

select create_hypertable('metric_events', 'end_time');