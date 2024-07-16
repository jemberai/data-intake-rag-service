ALTER TABLE event_record ADD COLUMN sha_256 VARCHAR(255) NULL;

ALTER TABLE event_record ADD COLUMN embedding_status VARCHAR(255) NULL;

create table client_configuration
(
    id           char(36) not null,
    client_id    varchar(255) not null,
    milvus_collection   varchar(255) not null,
    date_created   TIMESTAMP,
    date_updated   TIMESTAMP,
    primary key (id),
    constraint uc_client_configuration unique (client_id)
);