alter index resource_identity rename to resource_identity_idx;
create table resource_identity (
    id bigint primary key generated always as identity,
    source_system text not null,
    resource_type text not null,
    resource_id text not null,
    link_org_ref text default null,
    link_project_ref text default null,
    link_application_ref text default null
);

alter table resource_identity add constraint resource_identity_uniq unique (source_system, resource_type, resource_id);

insert into resource_identity(source_system, resource_type, resource_id)
    select source_system, resource_type, resource_id from resource
    on conflict do nothing;

alter table resource add column resource_identity_id bigint references resource_identity(id);

update resource set resource_identity_id = (
    select id from resource_identity ri where ri.source_system = source_system
        and ri.resource_type = resource_type
        and ri.resource_id = resource_id
);

alter table resource alter column resource_identity_id set not null;

alter table resource drop column source_system;
alter table resource drop column resource_type;
alter table resource drop column resource_id;

create extension if not exists hstore;

alter table resource add column tags hstore not null default ''::hstore;
alter table resource rename to metric;
alter table metric add constraint resource_tags_uniq unique (resource_identity_id, metric_name, tags);

alter table metric_events rename to measurement;
alter table measurement rename column resource_id to metric_id;
