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
create table encryption_keys (
     id char(36) not null,
     version integer,
     client_id varchar(255),
     key_id varchar(36),
     aes_key_id uuid,
     aes_provider varchar(255),
     aes_hmac varbinary(255),
     aes_initialization_vector varbinary(255),
     aes_encrypted_value varbinary(255),
     hmac_key_id uuid,
     hmac_provider varchar(255),
     hmac_hmac varbinary(255),
     hmac_initialization_vector varbinary(255),
     hmac_encrypted_value varbinary(255),
     date_created   TIMESTAMP,
     date_updated   TIMESTAMP,
     primary key (id)
);