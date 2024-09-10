/*
 *
 *  * Copyright 2023 - 2024 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * https://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

create table event_record
(
    id             char(36)     not null,
    version integer,
    client_id      varchar(255),
    spec_version   varchar(4)   not null,
    event_type     varchar(255),
    source         varchar(255),
    subject        varchar(255),
    event_id       varchar(255),
    time           TIMESTAMP,
    data_content_type varchar(255),
    sha_256 VARCHAR(255) NULL,
    embedding_status VARCHAR(255) NULL,
    tika_metadata  varchar(2000),
    data_key_id    char(36),
    data_provider  varchar(255),
    data_hmac      bytea,
    data_initialization_vector bytea,
    data_encrypted_value bytea,
    date_created   TIMESTAMP,
    date_updated   TIMESTAMP,
    primary key (id)
);

create table event_record_chunk
(
    id             char(36)  not null,
    version integer,
    event_record_id char(36) not null,
    embedding_id varchar(255),
    data bytea,
    data_key_id    char(36),
    data_provider  varchar(255),
    data_hmac      bytea,
    data_initialization_vector bytea,
    data_encrypted_value bytea,
    sha_256 VARCHAR(255) NULL,
    date_created   TIMESTAMP,
    date_updated   TIMESTAMP,
    primary key (id),
    constraint event_record_event_record_fk
        foreign key (event_record_id) references event_record (id),
    constraint embedding_id_uk unique (embedding_id)
);