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

package org.jemberai.dataintake;

import lombok.extern.slf4j.Slf4j;
import org.jemberai.cryptography.keymanagement.AesKeyDTO;
import org.jemberai.cryptography.keymanagement.JpaKeyService;
import org.jemberai.cryptography.keymanagement.KeyUtils;
import org.jemberai.cryptography.repositories.DefaultEncryptionKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

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
public class BaseTest {

//    @Container
//    public static MilvusContainer milvusContainer = new MilvusContainer("milvusdb/milvus:v2.3.9");
//
//    @DynamicPropertySource
//    public static void milvusProperties(DynamicPropertyRegistry registry) {
//        registry.add("spring.ai.vectorstore.milvus.client.host", milvusContainer::getHost);
//        registry.add("spring.ai.vectorstore.milvus.client.port", () ->  milvusContainer.getMappedPort(19530));
//        registry.add("spring.ai.vectorstore.milvus.client.username", () -> "minioadmin");
//        registry.add("spring.ai.vectorstore.milvus.client.password", () -> "minioadmin");
//    }

    @Autowired
    public WebApplicationContext wac;

    @Autowired
    DefaultEncryptionKeyRepository defaultEncryptionKeyRepository;

    @Autowired
    JpaKeyService jpaKeyService;

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
