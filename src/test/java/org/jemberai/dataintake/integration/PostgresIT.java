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

package org.jemberai.dataintake.integration;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jemberai.cryptography.keymanagement.AesKeyDTO;
import org.jemberai.cryptography.keymanagement.JpaKeyService;
import org.jemberai.cryptography.keymanagement.KeyUtils;
import org.jemberai.cryptography.repositories.DefaultEncryptionKeyRepository;
import org.jemberai.dataintake.config.JemberProperties;
import org.jemberai.dataintake.domain.EventExtensionRecord;
import org.jemberai.dataintake.domain.EventRecord;
import org.jemberai.dataintake.domain.EventRecordChunk;
import org.jemberai.dataintake.repositories.EventRecordRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeType;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.milvus.MilvusContainer;

import javax.sql.DataSource;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by jt, Spring Framework Guru.
 */
@Slf4j
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest
public class PostgresIT {
    @Container
    @ServiceConnection
    static PostgreSQLContainer primaryDb = new PostgreSQLContainer("postgres:16-alpine");

    @Container
    static PostgreSQLContainer keyStoreDb = new PostgreSQLContainer("postgres:16-alpine");

    @Container
    public static MilvusContainer milvusContainer = new MilvusContainer("milvusdb/milvus:v2.3.9");

    @Autowired
    EventRecordRepository eventRecordRepository;

    @Autowired
    DefaultEncryptionKeyRepository defaultEncryptionKeyRepository;

    @Autowired
    JpaKeyService jpaKeyService;

    @Autowired
    @Qualifier("dataSourcePrimary")
    DataSource dataSourcePrimary;

    public static final String TEST_CLIENT_ID = "test";

    @BeforeAll
    static void beforeAll() {
        primaryDb.start();
        keyStoreDb.start();
        milvusContainer.start();
    }

    @AfterAll
    static void afterAll() {
        primaryDb.stop();
        keyStoreDb.stop();
        milvusContainer.stop();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        //primary db
        registry.add("org.jemberai.datasource.primary.url", primaryDb::getJdbcUrl);
        registry.add("org.jemberai.datasource.primary.username", primaryDb::getUsername);
        registry.add("org.jemberai.datasource.primary.password", primaryDb::getPassword);
        registry.add("org.jemberai.datasource.primary.driver-class-name", primaryDb::getDriverClassName);
        registry.add("org.jemberai.jpa.primary.hibernate.ddl-auto", () -> "validate");

        // primary flyway
        //primary db
        registry.add("org.jemberai.datasource.primary-flyway.url", primaryDb::getJdbcUrl);
        registry.add("org.jemberai.datasource.primary-flyway.username", primaryDb::getUsername);
        registry.add("org.jemberai.datasource.primary-flyway.password", primaryDb::getPassword);
        registry.add("org.jemberai.datasource.primary-flyway.driver-class-name", primaryDb::getDriverClassName);
        registry.add("org.jemberai.jpa.primary-flyway.hibernate.ddl-auto", () -> "validate");

       // registry.add("spring.flyway.enabled", () -> false);

        //keystore db
        registry.add("org.jemberai.datasource.keystore.url", keyStoreDb::getJdbcUrl);
        registry.add("org.jemberai.datasource.keystore.username", keyStoreDb::getUsername);
        registry.add("org.jemberai.datasource.keystore.password", keyStoreDb::getPassword);
        registry.add("org.jemberai.datasource.keystore.driver-class-name", keyStoreDb::getDriverClassName);
        registry.add("org.jemberai.jpa.keystore.hibernate.ddl-auto", () -> "validate");

        //keystore flyway
        registry.add("org.jemberai.datasource.keystore-flyway.url", keyStoreDb::getJdbcUrl);
        registry.add("org.jemberai.datasource.keystore-flyway.username", keyStoreDb::getUsername);
        registry.add("org.jemberai.datasource.keystore-flyway.password", keyStoreDb::getPassword);
        registry.add("org.jemberai.datasource.keystore-flyway.driver-class-name", keyStoreDb::getDriverClassName);

        //milvus
        registry.add("spring.ai.vectorstore.milvus.client.host", milvusContainer::getHost);
        registry.add("spring.ai.vectorstore.milvus.client.port", () ->  milvusContainer.getMappedPort(19530));
        registry.add("spring.ai.vectorstore.milvus.client.username", () -> "minioadmin");
        registry.add("spring.ai.vectorstore.milvus.client.password", () -> "minioadmin");

        //mivlus jember
        registry.add("org.jemberai.vectorstore.milvus.host", milvusContainer::getHost);
        registry.add("org.jemberai.vectorstore.milvus.port", () ->  milvusContainer.getMappedPort(19530));
        registry.add("org.jemberai.vectorstore.milvus.username", () -> "minioadmin");
        registry.add("org.jemberai.vectorstore.milvus.password", () -> "minioadmin");
    }

    @Transactional
    @BeforeEach
    void setUp() {

        JemberProperties foo = new JemberProperties();
        foo.getLlm().getOpenAi().setApiKey("asdf");

        if (jpaKeyService.getDefaultKey(TEST_CLIENT_ID) == null) {
            log.info("Generating AES Key");
            AesKeyDTO aesKeyDTO = KeyUtils.generateAesKeyDTO();
            aesKeyDTO.setClientId(TEST_CLIENT_ID);
            jpaKeyService.setDefaultKey(TEST_CLIENT_ID, aesKeyDTO);

            log.info("Default Key Set");

            assertThat(jpaKeyService.getDefaultKey(TEST_CLIENT_ID)).isNotNull();
        }
    }

    @Test
    void testSaveRecord() {
        //given
        log.info("Saving Event Record");
        val eventRecord = getBuildEventRecord();
        val savedEventRecord = eventRecordRepository.saveAndFlush(eventRecord);

        log.info("Saved Event Record: {}", savedEventRecord);
        //then
        val count = eventRecordRepository.count();
        assertThat(count).isGreaterThan(0);
        assertThat(savedEventRecord.getData()).isEqualTo(eventRecord.getData());
        assertThat(savedEventRecord.getDataContentType()).isEqualTo(eventRecord.getDataContentType());
        assertThat(savedEventRecord.getSpecVersion()).isEqualTo(eventRecord.getSpecVersion());
        assertThat(savedEventRecord.getSource()).isEqualTo(eventRecord.getSource());
        assertThat(savedEventRecord.getExtensions()).hasSize(1);
        assertThat(savedEventRecord.getChunks()).hasSize(1);
        assertThat(savedEventRecord.getExtensions().iterator().next().getFieldName()).isEqualTo("test");
        assertThat(savedEventRecord.getExtensions().iterator().next().getId()).isNotNull();
        assertThat(savedEventRecord.getChunks().iterator().next().getData()).isEqualTo(eventRecord.getChunks().iterator().next().getData());
        assertThat(savedEventRecord.getChunks().iterator().next().getEncryptedValue()).isNotNull();
        assertThat(savedEventRecord.getChunks().iterator().next().getEventRecord()).isNotNull();
        assertThat(savedEventRecord.getChunks().iterator().next().getId()).isNotNull();
        //verify data is encrypted in database, query db directly
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSourcePrimary);

        Map<String, Object> result = jdbcTemplate.queryForMap("SELECT * FROM event_record");

        assertThat(result).isNotNull();

        byte[] data = (byte[]) result.get("data_encrypted_value");
        assertThat(data).isNotNull();
        String dataAsString = new String(data);
        assertThat(dataAsString).isNotEqualTo(eventRecord.getData().toString());
    }

    @Transactional
    @Test
    void testAddOfChildRecords() {
        //given
        log.info("Saving Event Record");
        val eventRecord = getBuildEventRecord();
        val savedEventRecord = eventRecordRepository.saveAndFlush(eventRecord);

        val fetchedEventRecord = eventRecordRepository.findById(savedEventRecord.getId()).get();

        fetchedEventRecord.addExtension(EventExtensionRecord.builder()
                .fieldName("test2")
                .fieldValue("value2")
                .build());

        fetchedEventRecord.addChunk(EventRecordChunk.builder()
                .data("This is some more text".getBytes())
                        .provider("foo")
                .build());

        log.debug("Saving updated Event Record with new chunk");
        val updatedEventRecord = eventRecordRepository.saveAndFlush(fetchedEventRecord);

        assertThat(updatedEventRecord.getExtensions()).hasSize(2);
        assertThat(updatedEventRecord.getChunks()).hasSize(2);

        eventRecord.getChunks().forEach(chunk -> {
            assertThat(chunk.getEncryptedValue()).isNotNull();
            assertThat(chunk.getEventRecord()).isNotNull();
            assertThat(chunk.getId()).isNotNull();

            log.debug(new String(chunk.getData()));
        });

        val checkUpdatedRecord = eventRecordRepository.findById(updatedEventRecord.getId()).get();

        assertThat(checkUpdatedRecord.getExtensions()).hasSize(2);
        assertThat(checkUpdatedRecord.getChunks()).hasSize(2);

        checkUpdatedRecord.getChunks().forEach(chunk -> {
            assertThat(chunk.getEncryptedValue()).isNotNull();
            assertThat(chunk.getEventRecord()).isNotNull();
            assertThat(chunk.getId()).isNotNull();

            log.debug(new String(chunk.getData()));
        });

        //verify data is encrypted in database, query db directly
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSourcePrimary);

        val result = jdbcTemplate.queryForRowSet("SELECT * FROM event_record_chunk");

        assertThat(result).isNotNull();

        //verify data is encrypted in the database
        while (result.next()) {
            Object obj = result.getObject(4);
            byte[] data = (byte[]) obj;
            assertThat(data).isNotNull();
            String dataAsString = new String(data);
            assertThat(dataAsString).isEqualTo("encrypted");
            log.debug(dataAsString);
        }
    }

    EventRecord getBuildEventRecord() {
        List<EventExtensionRecord> extensionRecords = new ArrayList<>();
        extensionRecords.add(EventExtensionRecord.builder()
                .fieldName("test")
                .fieldValue("value")
                .build());

        List<EventRecordChunk> chunks = new ArrayList<>();
        chunks.add(EventRecordChunk.builder()
                .data("This is some text".getBytes())
                .build());

        return EventRecord.builder()
                .eventId("123")
                .clientId(TEST_CLIENT_ID)
                .data("This is some text".getBytes())
                .specVersion("1.0")
                .dataContentType(MimeType.valueOf("text/plain").toString())
                .source("test")
                .extensions(extensionRecords)
            .chunks(chunks)
                .build();
    }
}
