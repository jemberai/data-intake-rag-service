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
    data_key_id    char(36),
    data_provider  varchar(255),
    data_hmac      varbinary(255),
    data_initialization_vector varbinary(255),
    data_encrypted_value binary large object,
    date_created   TIMESTAMP,
    date_updated   TIMESTAMP,
    primary key (id)
);

create table encryption_keys (
     id char(36) not null,
     version smallint,
     client_id varchar(255),
     key_id varchar(36),
     aes_key_id varchar(36),
     aes_provider varchar(255),
     aes_hmac binary,
     aes_initialization_vector binary,
     aes_encrypted_value binary,
     hmac_key_id varchar(36),
     hmac_provider varchar(255),
     hmac_hmac binary,
     hmac_initialization_vector binary,
     hmac_encrypted_value binary,
     date_created   TIMESTAMP,
     date_updated   TIMESTAMP,
     primary key (id)
);