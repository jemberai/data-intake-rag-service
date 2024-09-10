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

import org.jemberai.cryptography.keymanagement.JpaKeyService;
import org.jemberai.cryptography.repositories.DefaultEncryptionKeyRepository;
import org.jemberai.dataintake.repositories.EventRecordRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.milvus.MilvusContainer;

import javax.sql.DataSource;

/**
 * Created by jt, Spring Framework Guru.
 */
public class BaseIT {
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
}
