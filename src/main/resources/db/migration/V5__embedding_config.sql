create table embedding_configuration
(
    id           char(36) not null,
    client_id    varchar(255) not null,
    event_type   varchar(255) not null,
    model_name   varchar(255) not null,
    embedding_model varchar(255) not null,
    date_created   TIMESTAMP,
    date_updated   TIMESTAMP,
    primary key (id),
    constraint uc_embedding_configuration unique (client_id, event_type, model_name)
);