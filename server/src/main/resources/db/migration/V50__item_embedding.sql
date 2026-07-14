-- Semantic search: one embedding vector per item (generated at ingest when
-- app.ai.embedding.url/model are configured)
create sequence item_embedding_seq start with 1 increment by 50;

create table item_embedding (
    id bigint not null,
    version bigint,
    item_uuid varchar(16) not null unique,
    model varchar(255) not null,
    dimension integer not null,
    vector bytea not null,
    date_created timestamp(6),
    primary key (id)
);

create index idx_item_embedding_uuid on item_embedding (item_uuid);
