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

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jemberai.dataintake.domain.DocumentMetadataKeys;
import org.jemberai.dataintake.domain.EventRecordChunk;
import org.jemberai.dataintake.embedding.EmbeddingStoreFactory;
import org.jemberai.dataintake.model.QueryRequest;
import org.jemberai.dataintake.model.QueryResponseDocument;
import org.jemberai.dataintake.repositories.EventRecordChunkRepository;
import org.jemberai.dataintake.utils.StringUtil;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jt, Spring Framework Guru.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueryServiceImpl implements QueryService {

    private final EmbeddingStoreFactory embeddingStoreFactory;
    private final EmbeddingModel embeddingModel;
    private final EventRecordChunkRepository eventRecordChunkRepository;

    @Override
    public List<QueryResponseDocument> getDocuments(String clientId, QueryRequest queryRequest) {
        log.debug("Querying for documents");

        EmbeddingStore<TextSegment> embeddingStore = embeddingStoreFactory.createEmbeddingStore(clientId, embeddingModel.dimension());

        Embedding queryEmbedding = embeddingModel.embed(queryRequest.getQuery()).content();

        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(queryRequest.getTopK() != null ? queryRequest.getTopK() : 10)
                .minScore(queryRequest.getSimilarityThreshold())
                .build();

        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(searchRequest);

        List<String> matchIds = result.matches().stream().map(EmbeddingMatch::embeddingId)
                .toList();

        List<EventRecordChunk> chunks = eventRecordChunkRepository.findAllByEventRecord_ClientIdAndEmbeddingIdIn(clientId, matchIds);

        return chunks.stream().map(chunk -> {
            Map<String, Object> metadata = new HashMap<>();
            String parentDocumentId = chunk.getEventRecord() != null ? chunk.getEventRecord().getId().toString() : null;
            metadata.put(DocumentMetadataKeys.PARENT_DOCUMENT_ID, parentDocumentId);

            if (chunk.getEventRecord() != null && StringUtil.isNotEmpty(chunk.getEventRecord().getCsvHeader())) {
                metadata.put(DocumentMetadataKeys.CSV_HEADER, chunk.getEventRecord().getCsvHeader());
            }

            return QueryResponseDocument.builder()
                    .id(chunk.getId().toString())
                    .embeddingId(chunk.getEmbeddingId())
                    .content(new String(chunk.getData()))
                    .metadata(metadata)
                    .build();
        }).toList();
    }
}
