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
import org.jemberai.cryptography.keymanagement.AesKeyDTO;
import org.jemberai.cryptography.keymanagement.JpaKeyService;
import org.jemberai.cryptography.keymanagement.KeyUtils;
import org.jemberai.cryptography.repositories.DefaultEncryptionKeyRepository;
import org.jemberai.dataintake.repositories.EventRecordRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.milvus.MilvusContainer;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

/**
 * Created by jt, Spring Framework Guru.
 */
@Slf4j
@SpringBootTest
public class BaseIT {
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> primaryDb = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static PostgreSQLContainer<?> keyStoreDb = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static MilvusContainer milvusContainer = new MilvusContainer("milvusdb/milvus:v2.4.11");

    @Autowired
    EventRecordRepository eventRecordRepository;

    @Autowired
    DefaultEncryptionKeyRepository defaultEncryptionKeyRepository;

    @Autowired
    JpaKeyService jpaKeyService;

    @Autowired
    @Qualifier("dataSourcePrimary")
    DataSource dataSourcePrimary;

    @Autowired
    public WebApplicationContext wac;

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
        registry.add("spring.ai.vectorstore.milvus.client.port", () -> milvusContainer.getMappedPort(19530));
        registry.add("spring.ai.vectorstore.milvus.client.username", () -> "minioadmin");
        registry.add("spring.ai.vectorstore.milvus.client.password", () -> "minioadmin");

        //mivlus jember
        registry.add("org.jemberai.vectorstore.milvus.host", milvusContainer::getHost);
        registry.add("org.jemberai.vectorstore.milvus.port", () -> milvusContainer.getMappedPort(19530));
        registry.add("org.jemberai.vectorstore.milvus.username", () -> "minioadmin");
        registry.add("org.jemberai.vectorstore.milvus.password", () -> "minioadmin");
    }

    public MockMvc mockMvc;

    public static final String JEMBER_CLIENT = "jember-client";

    public static final SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtRequestPostProcessor =
            jwt().jwt(jwt -> {
                jwt.claims(claims -> {
                            claims.put("scope", "read");
                            claims.put("scope", "write");
                        })
                        .subject(JEMBER_CLIENT)
                        .audience(List.of(JEMBER_CLIENT))
                        .notBefore(Instant.now().minusSeconds(5l));
            });

    @BeforeEach
    void setUp() {

        if (jpaKeyService.getDefaultKey(JEMBER_CLIENT) == null) {
            log.info("Generating AES Key");
            AesKeyDTO aesKeyDTO = KeyUtils.generateAesKeyDTO();
            aesKeyDTO.setClientId(JEMBER_CLIENT);
            jpaKeyService.setDefaultKey(JEMBER_CLIENT, aesKeyDTO);

            log.info("Default Key Set");

            assertThat(jpaKeyService.getDefaultKey(JEMBER_CLIENT)).isNotNull();
        }
        mockMvc = MockMvcBuilders
                .webAppContextSetup(wac)
                .apply(springSecurity())
                .build();
    }
}
