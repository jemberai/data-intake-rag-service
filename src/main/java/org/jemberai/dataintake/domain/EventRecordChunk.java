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

package org.jemberai.dataintake.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Created by jt, Spring Framework Guru.
 */
@Setter
@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(EventRecordChunkListener.class)
public class EventRecordChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36, columnDefinition = "char(36)", updatable = false, nullable = false)
    private UUID id;

    @Version
    private Integer version;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "event_record_id", columnDefinition = "char(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private EventRecord eventRecord;

    // This is the embedding id that is used to store the embedding in the embedding store.
    private String embeddingId;

    /**
     * The data for this chunk. Unable to make this transient as it is used in the listener. Turns out that
     * transient fields are sent to listeners when it is a top level entity, but not in collections. The
     * Listener will mask this field and encrypt/decrypt it as needed.
     */
    private byte[] data;

    @Column(name = "data_provider")
    private String provider;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "data_key_id", columnDefinition = "char(36)")
    private UUID keyId;

    @Column(name = "data_hmac")
    private byte[] hmac;

    @Column(name = "data_encrypted_value")
    private byte[] encryptedValue;

    @Column(name = "data_initialization_vector")
    private byte[] initializationVector;

    @Column(name = "sha_256")
    private String sha256;

    @CreationTimestamp
    private LocalDateTime dateCreated;

    @UpdateTimestamp
    private LocalDateTime dateUpdated;
}
