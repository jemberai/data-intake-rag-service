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
import io.milvus.client.MilvusClient;
import lombok.val;
import org.jemberai.dataintake.embedding.EmbeddingStoreFactory;
import org.jemberai.dataintake.repositories.ClientConfigurationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.springframework.ai.vectorstore.MilvusVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Created by jt, Spring Framework Guru.
 */
@SpringBootTest
class EmbeddingServiceImplUnitTests {

    @Mock
    ClientConfigurationRepository clientConfigurationRepository;

    @Mock
    ApplicationEventPublisher applicationEventPublisher;

    @Mock
    EmbeddingStoreFactory embeddingStoreFactory;

    @MockBean
    MilvusVectorStore milvusVectorStore;

    @MockBean
    MilvusClient milvusClient;

    @Value("classpath:files/St.-Anthonys-Triathlon-Olympic-Bike-Map-23-3002600_Final.pdf")
    Resource pdfFile;

    @Value("classpath:files/IMG_0546.jpeg")
    Resource jpegImage;

    @Value("classpath:files/movies500Trimed.csv")
    Resource csvFile;

    EmbeddingServiceImpl embeddingService;

    @BeforeEach
    void setUp() {
        embeddingService = new EmbeddingServiceImpl(clientConfigurationRepository, applicationEventPublisher, embeddingStoreFactory);
    }

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
            "files/index.html"})
    void testVariousDocFormats(String fileName) {

        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);

        Document document = embeddingService.parse(inputStream);

        assertThat(document).isNotNull();
        assertThat(document.text()).isNotEmpty();
        assertThat(document.metadata()).isNotNull();
    }
}
