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

import io.milvus.client.MilvusServiceClient;
import org.jemberai.dataintake.config.VectorStoreProperties;
import org.jemberai.dataintake.domain.ClientConfiguration;
import org.jemberai.dataintake.domain.EmbeddingModelEnum;
import org.jemberai.dataintake.domain.EventRecord;
import org.jemberai.dataintake.messages.EmbeddingRequestCompleteMessage;
import org.jemberai.dataintake.messages.EmbeddingRequestMessage;
import org.jemberai.dataintake.repositories.ClientConfigurationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;
import org.springframework.ai.autoconfigure.openai.OpenAiConnectionProperties;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.MilvusVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by jt, Spring Framework Guru.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class EmbeddingServiceImpl implements EmbeddingService {

    private final OpenAiConnectionProperties openAiConnectionProperties;
    private final MilvusServiceClient milvusClient;
    private final VectorStoreProperties vectorStoreProperties;
    private final ClientConfigurationRepository clientConfigurationRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void processOpenAPIEmbeddingRequest(EmbeddingRequestMessage message) {

        String fileExtension = null;

        try {
            fileExtension = getFileExtension(message.getEventRecord());
        } catch (MimeTypeException e) {
            //todo handle better
            throw new RuntimeException(e);
        }

        Resource payload = new ByteArrayResource(message.getEventRecord().getData(),
                message.getEventRecord().getId() + fileExtension);

        TikaDocumentReader documentReader = new TikaDocumentReader(payload);

        List<Document> documents = documentReader.get();

        TextSplitter textSplitter = new TokenTextSplitter();

        List<Document> splitDocuments = textSplitter.apply(documents);

        log.debug("Getting embedding and adding to vector store");

        getVectorStore(message.getEmbeddingModel(), message.getEventRecord().getClientId())
                .add(splitDocuments);

        log.debug("Embedding added to vector store");

        applicationEventPublisher.publishEvent(EmbeddingRequestCompleteMessage.builder()
                .eventRecord(message.getEventRecord())
                .build());
    }

    /**
     * Get the file extension based on the content type
     * <p>
     * See this link for supported types <a href="https://tika.apache.org/2.9.0/formats.html">...</a>
     *
     * @param eventRecord
     * @return
     * @throws MimeTypeException
     */
    private String getFileExtension(EventRecord eventRecord) throws MimeTypeException {
        return MimeTypes.getDefaultMimeTypes()
                .forName(eventRecord.getDataContentType()).getExtension();
    }

    public VectorStore getVectorStore(EmbeddingModelEnum embeddingModel, String clientId) {
        EmbeddingModel model = embeddingModel(embeddingModel);

        val collectionName = clientConfigurationRepository.findByClientId(clientId)
                .map(ClientConfiguration::getMilvusCollection)
                .orElseThrow(RuntimeException::new);

        return vectorStore(milvusClient, model, collectionName);
    }

    public EmbeddingModel embeddingModel(EmbeddingModelEnum embeddingModel) {
        String model = "";

        switch (embeddingModel) {
            case EmbeddingModelEnum.TEXT_EMBEDDING_3_SMALL  -> model = "text-embedding-3-small";
            case EmbeddingModelEnum.TEXT_EMBEDDING_3_LARGE -> model = "text-embedding-3-large";
            case EmbeddingModelEnum.TEXT_EMBEDDING_ADA_002 -> model = "text-embedding-ada-002";
            default -> model = "text-embedding-3-small";
        }

        return new OpenAiEmbeddingModel(new OpenAiApi(openAiConnectionProperties.getApiKey()),
                MetadataMode.EMBED,
                 OpenAiEmbeddingOptions.builder().withModel(model).build());
    }

    public VectorStore vectorStore(MilvusServiceClient milvusClient, EmbeddingModel embeddingModel, String collectionName) {
        MilvusVectorStore.MilvusVectorStoreConfig config = MilvusVectorStore.MilvusVectorStoreConfig.builder()
                .withCollectionName(collectionName)

                .build();

        MilvusVectorStore vectorStore = new MilvusVectorStore(milvusClient, embeddingModel, config , true);

        // will create collection if missing
        try {
            vectorStore.afterPropertiesSet();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return vectorStore;
    }
}
