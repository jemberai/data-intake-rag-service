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

package org.jemberai.dataintake.embedding.milvus;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import org.jemberai.dataintake.embedding.EmbeddingStoreFactory;

/**
 * Created by jt, Spring Framework Guru.
 */
public class MilvusEmbeddingStoreFactory implements EmbeddingStoreFactory {

    private final MilvusEmbeddingStore.Builder builder;

    public MilvusEmbeddingStoreFactory(MilvusEmbeddingStore.Builder builder) {
        this.builder = builder;
    }

    @Override
    public EmbeddingStore<TextSegment> createEmbeddingStore(String collectionName, int dimension) {

        if (collectionName == null) {
            throw new IllegalArgumentException("Collection name must not be null");
        }

        if (collectionName.contains("-")){
            collectionName = collectionName.replace("-", "_");
        }

        return builder
                .collectionName(collectionName)
                .dimension(dimension)
                .build();
    }
}
