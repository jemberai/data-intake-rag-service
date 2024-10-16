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
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by jt, Spring Framework Guru.
 */
@ToString(exclude = {"extensions", "data", "encryptedValue"})
@Getter
@Setter
@RequiredArgsConstructor
@Entity
@EntityListeners(EventRecordListener.class)
public class EventRecord {

    @Builder
    public EventRecord(UUID id, Integer version, String clientId, String specVersion, String eventType, String source,
                       String subject, String eventId, OffsetDateTime time, String dataContentType,
                       byte[] data, String sha256, EmbeddingStatusEnum embeddingStatus, List<EventExtensionRecord> extensions,
                       List<EventRecordChunk> chunks, LocalDateTime dateCreated, LocalDateTime dateUpdated, String csvHeader) {
        this.id = id;
        this.version = version;
        this.clientId = clientId;
        this.specVersion = specVersion;
        this.eventType = eventType;
        this.source = source;
        this.subject = subject;
        this.eventId = eventId;
        this.time = time;
        this.dataContentType = dataContentType;
        this.data = data;
        this.sha256 = sha256;
        if (embeddingStatus != null) this.embeddingStatus = embeddingStatus;
        this.extensions = extensions;
        this.chunks = chunks;
        this.dateCreated = dateCreated;
        this.dateUpdated = dateUpdated;
        this.csvHeader = csvHeader;

        //Keeps JPA happy
        if (extensions != null) {
            this.extensions.forEach(extension -> extension.setEventRecord(this));
        }

        if (this.chunks != null) {
            this.chunks.forEach(chunk -> chunk.setEventRecord(this));
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36, columnDefinition = "char(36)", updatable = false, nullable = false)
    private UUID id;

    @Version
    private Integer version;

    private String clientId;

    @NotNull
    private String specVersion;

    private String eventType;

    private String source;

    private String subject;

    private String eventId;

    @Temporal(TemporalType.TIMESTAMP)
    private OffsetDateTime time;

    private String dataContentType;

    @Transient
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

    @Enumerated(EnumType.STRING)
    private EmbeddingStatusEnum embeddingStatus = EmbeddingStatusEnum.NEW;

    private String tikaMetadata;

    private String csvHeader; // Comma separated header attributes

    @OneToMany(mappedBy = "eventRecord", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private List<EventExtensionRecord> extensions = new ArrayList<>();

    @OneToMany(mappedBy = "eventRecord", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private List<EventRecordChunk> chunks = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime dateCreated;

    @UpdateTimestamp
    private LocalDateTime dateUpdated;

    public void addExtension(EventExtensionRecord extension) {
        List<EventExtensionRecord> newExtensions = new ArrayList<>(extensions);
        newExtensions.add(extension);
        extensions = newExtensions;
        extension.setEventRecord(this);
    }

    public void addChunk(EventRecordChunk chunk) {
        chunk.setEventRecord(this);
        this.chunks.add(chunk);
    }
}
