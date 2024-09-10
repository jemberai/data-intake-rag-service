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

package org.jemberai.dataintake.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jemberai.dataintake.model.QueryRequest;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by jt, Spring Framework Guru.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueryServiceImpl implements QueryService {

    private final EmbeddingService embeddingService;

    @Override
    public List<Document> getDocuments(String clientId, QueryRequest queryRequest) {
        log.debug("Querying for documents");

        //todo - update to use the new vector store
//        VectorStore vectorStore = embeddingService
//                .getVectorStore(EmbeddingModelEnum.TEXT_EMBEDDING_3_SMALL, clientId);
//
//        List<Document> documents = vectorStore.similaritySearch(SearchRequest
//                .query(queryRequest.getQuery())
//                .withTopK(queryRequest.getTopK())
//                .withSimilarityThreshold(queryRequest.getSimilarityThreshold()));
//
//        log.debug("Found {} documents", documents.size());

        return null;
    }
}
