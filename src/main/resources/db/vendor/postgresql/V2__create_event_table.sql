create table event_record
(
    id             char(36)     not null,
    spec_version   varchar(4)   not null,
    event_type     varchar(255),
    source         varchar(255),
    subject        varchar(255),
    event_id       varchar(255),
    time           TIMESTAMP,
    data_content_type varchar(255),
    data           bytea,
    date_created   TIMESTAMP,
    date_updated   TIMESTAMP,
    primary key (id)
);
