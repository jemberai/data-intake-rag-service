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
import org.jemberai.dataintake.domain.EventRecord;
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

import javax.sql.DataSource;
import java.util.Map;

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
    }

    @AfterAll
    static void afterAll() {
        primaryDb.stop();
        keyStoreDb.stop();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        //primary db
        registry.add("org.jemberai.datasource.primary.url", primaryDb::getJdbcUrl);
        registry.add("org.jemberai.datasource.primary.username", primaryDb::getUsername);
        registry.add("org.jemberai.datasource.primary.password", primaryDb::getPassword);
        registry.add("org.jemberai.datasource.primary.driver-class-name", primaryDb::getDriverClassName);
        registry.add("org.jemberai.jpa.primary.hibernate.ddl-auto", () -> "validate");

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
    }

    @Transactional
    @BeforeEach
    void setUp() {

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
        val eventRecord = buildEventRecord;
        val savedEventRecord = eventRecordRepository.saveAndFlush(eventRecord);

        log.info("Saved Event Record: {}", savedEventRecord);
        //then
        val count = eventRecordRepository.count();
        assertThat(count).isGreaterThan(0);
        assertThat(savedEventRecord.getData()).isEqualTo(eventRecord.getData());

        //verify data is encrypted in database, query db directly
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSourcePrimary);

        Map<String, Object> result = jdbcTemplate.queryForMap("SELECT * FROM event_record");

        assertThat(result).isNotNull();

        byte[] data = (byte[]) result.get("data_encrypted_value");
        assertThat(data).isNotNull();
        String dataAsString = new String(data);
        assertThat(dataAsString).isNotEqualTo(eventRecord.getData().toString());
    }

    EventRecord buildEventRecord = EventRecord.builder()
            .eventId("123")
            .clientId(TEST_CLIENT_ID)
            .data("This is some text".getBytes())
            .specVersion("1.0")
            .dataContentType(MimeType.valueOf("text/plain").toString())
            .source("test")
            .build();
}
