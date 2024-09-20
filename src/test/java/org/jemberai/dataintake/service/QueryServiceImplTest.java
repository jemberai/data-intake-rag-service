package org.jemberai.dataintake.service;
/*
 * Created by Ashok Kumar Pant
 * Email: asokpant@gmail.com
 * Created on 20/09/2024.
 */


import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.jemberai.dataintake.config.JemberProperties;
import org.jemberai.dataintake.domain.EventRecordChunk;
import org.jemberai.dataintake.embedding.EmbeddingStoreFactory;
import org.jemberai.dataintake.model.QueryRequest;
import org.jemberai.dataintake.model.QueryResponseDocument;
import org.jemberai.dataintake.repositories.EventRecordChunkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Slf4j
@ContextConfiguration(classes = {JemberProperties.class})
public class QueryServiceImplTest {

    @Mock
    private EmbeddingStoreFactory embeddingStoreFactory;

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private EventRecordChunkRepository eventRecordChunkRepository;

    @Mock
    private EmbeddingStore<TextSegment> embeddingStore;

    @InjectMocks
    private QueryServiceImpl queryService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGetDocuments() {
        String clientId = "test-client";
        QueryRequest queryRequest = new QueryRequest();
        queryRequest.setQuery("test query");
        queryRequest.setTopK(5);
        queryRequest.setSimilarityThreshold(0.8);

        Embedding queryEmbedding = new Embedding(new float[]{0.1f, 0.2f, 0.3f});
        TokenUsage tokenUsage = new TokenUsage(100, 1000, 1100);
        Response<Embedding> embeddingResponse = Response.from(queryEmbedding, tokenUsage, FinishReason.STOP);
        when(embeddingModel.embed(any(String.class))).thenReturn(embeddingResponse);
        when(embeddingStoreFactory.createEmbeddingStore(any(String.class), any(Integer.class))).thenReturn(embeddingStore);

        EmbeddingMatch<TextSegment> match = new EmbeddingMatch<>(0.9, "match-id-1", new Embedding(new float[]{0.1f, 0.2f, 0.3f}), null);
        EmbeddingSearchResult<TextSegment> searchResult = new EmbeddingSearchResult<>(List.of(match));
        when(embeddingStore.search(any(EmbeddingSearchRequest.class))).thenReturn(searchResult);

        EventRecordChunk chunk = new EventRecordChunk();
        UUID chunkId1 = UUID.randomUUID();
        chunk.setId(chunkId1);
        chunk.setData("test data".getBytes());
        when(eventRecordChunkRepository.findAllByEventRecord_ClientIdAndEmbeddingIdIn(any(String.class), any(List.class)))
                .thenReturn(List.of(chunk));

        List<QueryResponseDocument> documents = queryService.getDocuments(clientId, queryRequest);

        assertEquals(1, documents.size());
        assertEquals(chunkId1.toString(), documents.getFirst().getId());
        assertEquals("test data", documents.getFirst().getContent());
    }
}
