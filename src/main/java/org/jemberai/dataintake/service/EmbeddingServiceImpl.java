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

import dev.langchain4j.data.document.BlankDocumentException;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jemberai.dataintake.embedding.EmbeddingStoreFactory;
import org.jemberai.dataintake.messages.EmbeddingRequestCompleteMessage;
import org.jemberai.dataintake.messages.EmbeddingRequestMessage;
import org.jemberai.dataintake.utils.tika.ApacheTikaDocumentMetaParser;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jt, Spring Framework Guru.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class EmbeddingServiceImpl implements EmbeddingService {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final EmbeddingStoreFactory embeddingStoreFactory;
    private final EmbeddingModel embeddingModel;

    @SneakyThrows
    @Override
    public void processOpenAPIEmbeddingRequest(EmbeddingRequestMessage message) {

        try {
           Document payloadDocument = parse(new ByteArrayInputStream(message.getEventRecord().getData()));
            processDocument(payloadDocument, message);
        } catch (BlankDocumentException e) {
            //todo handle better
            log.warn("Blank document received Event Id: {}", message.getEventRecord().getId());

            applicationEventPublisher.publishEvent(EmbeddingRequestCompleteMessage.builder()
                    .eventRecord(message.getEventRecord())
                    .status(EmbeddingRequestCompleteMessage.EmbeddingRequestStatus.EMPTY)
                    .build());
        } catch (Exception e) {
            //todo handle better
            log.error("Error parsing document Event Id: {}", message.getEventRecord().getId(), e);

            applicationEventPublisher.publishEvent(EmbeddingRequestCompleteMessage.builder()
                    .eventRecord(message.getEventRecord())
                    .status(EmbeddingRequestCompleteMessage.EmbeddingRequestStatus.ERROR)
                    .build());
        }
    }

    private void processDocument(Document payloadDocument, EmbeddingRequestMessage message){
        // split the document into segments
        DocumentSplitter documentSplitter = DocumentSplitters.recursive(300, 0);
        List<TextSegment> textSegments = documentSplitter.split(payloadDocument);

        // get the embedding for each segment
        EmbeddingStore<TextSegment> embeddingStore = embeddingStoreFactory.createEmbeddingStore(message.getEventRecord().getClientId(), embeddingModel.dimension());
        // add the embedding to the vector store, get id
        Map<String, TextSegment> segmentMap = new HashMap<>(textSegments.size());

        // store chunk id and id in the database
        textSegments.forEach(textSegment -> {
            // get the embedding
            log.debug("Adding embedding to vector store");
            Response<Embedding> embedding = embeddingModel.embed(textSegment);
            // add to the vector store
            // get the id
            log.debug("Embedding added to vector store");
            log.debug(embedding.toString());
            String id = embeddingStore.add(embedding.content());
            // store the id and chunk id in the database
            segmentMap.put(id, textSegment);
        });

        applicationEventPublisher.publishEvent(EmbeddingRequestCompleteMessage.builder()
                .eventRecord(message.getEventRecord())
                .textSegments(segmentMap)
                .document(payloadDocument)
                .status(EmbeddingRequestCompleteMessage.EmbeddingRequestStatus.SUCCESS)
                .build());
    }

    public Document parse(InputStream inputStream) {
        DocumentParser parser = new ApacheTikaDocumentMetaParser();
        return parser.parse(inputStream);
    }
}
