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

package org.jemberai.dataintake.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import org.jemberai.dataintake.embedding.EmbeddingStoreFactory;
import org.jemberai.dataintake.embedding.milvus.MilvusEmbeddingStoreFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static dev.langchain4j.model.openai.OpenAiEmbeddingModelName.TEXT_EMBEDDING_3_SMALL;

/**
 * Created by jt, Spring Framework Guru.
 */
@Configuration
public class LangChainConfig {

    @Bean
    EmbeddingModel openAiEmbeddingModelLangChain(JemberProperties jemberProperties) {
        return OpenAiEmbeddingModel.builder()
                .apiKey(jemberProperties.getLlm().getOpenAi().getApiKey())
                .modelName(TEXT_EMBEDDING_3_SMALL)
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    @Bean
    public EmbeddingStoreFactory milvusEmbeddingStoreFactory(JemberProperties jemberProperties) {
        return new MilvusEmbeddingStoreFactory(MilvusEmbeddingStore.builder()
                .host(jemberProperties.getVectorstore().getMilvus().getHost())
                .port(Integer.parseInt(jemberProperties.getVectorstore().getMilvus().getPort())));
    }
}
