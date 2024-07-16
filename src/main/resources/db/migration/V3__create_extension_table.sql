create table event_extension_record
(
    id              char(36)     not null,
    event_record_id char(36)     not null,
    field_name     varchar(255),
    field_value    varchar(255),
    date_created   TIMESTAMP,
    date_updated   TIMESTAMP,
    primary key (id),
    constraint fk_event_record_id
        foreign key (event_record_id) references event_record (id)
);