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

package org.jemberai.dataintake.repositories;

import org.jemberai.dataintake.domain.EmbeddingConfiguration;
import org.jemberai.dataintake.domain.ModelEnum;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.milvus.MilvusContainer;

import java.util.Optional;

/**
 * Created by jt, Spring Framework Guru.
 */
@DirtiesContext
@Testcontainers
@SpringBootTest
public class EmbeddingConfigRepoTest {

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
    EmbeddingConfigurationRepository embeddingConfigurationRepository;

    @Test
    void testGetConfig() {

        Optional<EmbeddingConfiguration> cfgOptional = embeddingConfigurationRepository.findByClientIdAndEventTypeAndModelName("jember-client", "io.spring.event", ModelEnum.OPENAI);

        assert cfgOptional.isPresent();

    }
}
