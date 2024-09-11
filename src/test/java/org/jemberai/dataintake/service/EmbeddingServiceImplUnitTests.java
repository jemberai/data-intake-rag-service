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
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jemberai.dataintake.config.JemberProperties;
import org.jemberai.dataintake.domain.EmbeddingStatusEnum;
import org.jemberai.dataintake.domain.EventRecord;
import org.jemberai.dataintake.embedding.EmbeddingStoreFactory;
import org.jemberai.dataintake.messages.EmbeddingRequestCompleteMessage;
import org.jemberai.dataintake.messages.EmbeddingRequestMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

/**
 * Created by jt, Spring Framework Guru.
 */
@Slf4j
@SpringBootTest
@ContextConfiguration(classes = {JemberProperties.class})
class EmbeddingServiceImplUnitTests {

    @Value("classpath:files/St.-Anthonys-Triathlon-Olympic-Bike-Map-23-3002600_Final.pdf")
    Resource pdfFile;

    @Value("classpath:files/IMG_0546.jpeg")
    Resource jpegImage;

    @Value("classpath:files/movies500Trimed.csv")
    Resource csvFile;

    @Mock
    ApplicationEventPublisher applicationEventPublisher;

    @Mock
    EmbeddingStoreFactory embeddingStoreFactory;

    @Mock
    EmbeddingModel embeddingModel;

    @InjectMocks
    EmbeddingServiceImpl embeddingService;

    //mock embedding store
    @Mock
    EmbeddingStore<TextSegment> esMock;

    @Captor
    ArgumentCaptor<EmbeddingRequestCompleteMessage> completeCaptor;

    @Test
    void testParsePdf() throws IOException {

        val document = embeddingService.parse(pdfFile.getInputStream());

        assertThat(document).isNotNull();
        assertThat(document.text()).isNotEmpty();
        assertThat(document.metadata()).isNotNull();
        assertThat(document.metadata().getString("Content-Type")).isEqualTo("application/pdf");
    }

    @Test
    void testParseJson() {

        var json = """
                    {
                        "appinfoA" : "abc",
                        "appinfoB" : 123,
                        "appinfoC" : true
                    }
                """;

        val document = embeddingService.parse(new ByteArrayInputStream(json.getBytes()));

        assertThat(document).isNotNull();
        assertThat(document.text()).isNotEmpty();
        assertThat(document.metadata()).isNotNull();
    }

    @Test
    void testParseCsv() throws IOException {

        val document = embeddingService.parse(csvFile.getInputStream());

        assertThat(document).isNotNull();
        assertThat(document.text()).isNotEmpty();
        assertThat(document.metadata()).isNotNull();
    }

    @Test
    void testParseImage() {

        assertThrows(BlankDocumentException.class, () -> {
            embeddingService.parse(jpegImage.getInputStream());
        });
    }

    @ParameterizedTest
    @ValueSource(strings = { "files/file-sample_100kb.docx",
            "files/file_example_XLS_10.xls",
            "files/file_example_XLSX_10.xlsx", "files/file_example_XML_24kb.xml",
            "files/index2.html"})
    void testVariousDocFormats(String fileName) throws IOException {

        log.info("Processing file: " + fileName);
        log.info("Processing file: **" + fileName + "**");
        File file = new File(getClass().getClassLoader().getResource("files").getFile());
        //System.out.println(file.listFiles());
        Arrays.stream(file.listFiles()).forEach(f -> {
            System.out.println("**" + f.getName() + "**");
            System.out.println(f.length());
            System.out.println(f.canRead());
            System.out.println(f.exists());
            System.out.println(f.getName().equals("index2.html"));
        }) ;


        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);

        Document document = embeddingService.parse(inputStream);

        assertThat(document).isNotNull();
        assertThat(document.text()).isNotEmpty();
        assertThat(document.metadata()).isNotNull();
    }

    @Test
    void testProcessGoodRequest() throws IOException {
        EventRecord eventRecord = EventRecord.builder()
                .id(UUID.randomUUID())
                .clientId("jember-client")
                .data(pdfFile.getContentAsByteArray())
                .embeddingStatus(EmbeddingStatusEnum.NEW)
                .build();

        when(embeddingStoreFactory.createEmbeddingStore(anyString(), anyInt())).thenReturn(esMock);
        when(embeddingModel.embed((TextSegment) any())).thenReturn(new Response<>(new Embedding(new float[]{1.0f, 2.0f, 3.0f})));
        when(esMock.add(any())).thenReturn("123");

        embeddingService.processOpenAPIEmbeddingRequest(EmbeddingRequestMessage.builder()
                .eventRecord(eventRecord)
                .build());

        then(embeddingStoreFactory).should().createEmbeddingStore(anyString(), anyInt());
        then(embeddingModel).should(times(6)).embed((TextSegment) any());
        then(esMock).should(times(6)).add(any());
        then(applicationEventPublisher).should().publishEvent(completeCaptor.capture());

        assertThat(completeCaptor.getValue().getStatus()).isEqualTo(EmbeddingRequestCompleteMessage.EmbeddingRequestStatus.SUCCESS);
    }

    @Test
    void testProcessNoData() throws IOException {
        EventRecord eventRecord = EventRecord.builder()
                .id(UUID.randomUUID())
                .clientId("jember-client")
                .data(jpegImage.getInputStream().readAllBytes())
                .embeddingStatus(EmbeddingStatusEnum.NEW)
                .build();

        embeddingService.processOpenAPIEmbeddingRequest(EmbeddingRequestMessage.builder()
                .eventRecord(eventRecord)
                .build());

        then(applicationEventPublisher).should().publishEvent(completeCaptor.capture());

        assertThat(completeCaptor.getValue().getStatus()).isEqualTo(EmbeddingRequestCompleteMessage.EmbeddingRequestStatus.EMPTY);
    }

    @Test
    void testProcessError() throws IOException {
        EventRecord eventRecord = EventRecord.builder()
                .id(UUID.randomUUID())
                .clientId("jember-client")
                .embeddingStatus(EmbeddingStatusEnum.NEW)
                .build();

        embeddingService.processOpenAPIEmbeddingRequest(EmbeddingRequestMessage.builder()
                .eventRecord(eventRecord)
                .build());

        then(applicationEventPublisher).should().publishEvent(completeCaptor.capture());

        assertThat(completeCaptor.getValue().getStatus()).isEqualTo(EmbeddingRequestCompleteMessage.EmbeddingRequestStatus.ERROR);
    }
}
