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

create table event_extension_record
(
    id              char(36)     not null,
    version integer,
    event_record_id char(36)     not null,
    field_name     varchar(255),
    field_value    varchar(255),
    date_created   TIMESTAMP,
    date_updated   TIMESTAMP,
    primary key (id),
    constraint fk_event_record_id
        foreign key (event_record_id) references event_record (id)
);

create table embedding_configuration
(
    id           char(36) not null,
    version integer,
    client_id    varchar(255) not null,
    event_type   varchar(255) not null,
    model_name   varchar(255) not null,
    embedding_model varchar(255) not null,
    date_created   TIMESTAMP,
    date_updated   TIMESTAMP,
    primary key (id),
    constraint uc_embedding_configuration unique (client_id, event_type, model_name)
);

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
