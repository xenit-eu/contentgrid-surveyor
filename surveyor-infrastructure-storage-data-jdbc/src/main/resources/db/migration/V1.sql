create extension if not exists timescaledb;

create table resource (
    id bigint primary key generated always as identity,
    resource_type text not null,
    resource_id text not null,
    metric_type text not null
);
create unique index resource_identity on resource (resource_type, resource_id, metric_type);

create table metrics_gauges (
    sample_time timestamptz not null,
    resource_id bigint references resource(id) not null,
    value numeric not null
);

select create_hypertable('metrics_gauges', 'sample_time');

create table metrics_events (
    start_time timestamptz not null,
    end_time timestamptz not null,
    resource_id bigint references resource(id) not null,
    amount numeric not null,
);

select create_hypertable('metrics_events', 'end_time');