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

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.milvus.MilvusContainer;

import java.time.Instant;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

/**
 * Created by jt, Spring Framework Guru.
 */
@SpringBootTest
public class BaseTest {

    @Container
    public static MilvusContainer milvusContainer = new MilvusContainer("milvusdb/milvus:v2.3.9");

    @DynamicPropertySource
    public static void milvusProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.vectorstore.milvus.client.host", milvusContainer::getHost);
        registry.add("spring.ai.vectorstore.milvus.client.port", () ->  milvusContainer.getMappedPort(19530));
        registry.add("spring.ai.vectorstore.milvus.client.username", () -> "minioadmin");
        registry.add("spring.ai.vectorstore.milvus.client.password", () -> "minioadmin");
    }

    @Autowired
    public WebApplicationContext wac;

    public MockMvc mockMvc;

    public static final SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtRequestPostProcessor =
            jwt().jwt(jwt -> {
                jwt.claims(claims -> {
                            claims.put("scope", "read");
                            claims.put("scope", "write");
                        })
                        .subject("jember-client")
                        .audience(List.of("jember-client"))
                        .notBefore(Instant.now().minusSeconds(5l));
            });

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(wac)
                .apply(springSecurity())
                .build();
    }
}
