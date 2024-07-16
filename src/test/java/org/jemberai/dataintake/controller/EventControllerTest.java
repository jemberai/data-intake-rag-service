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

package org.jemberai.dataintake.controller;

import io.milvus.client.MilvusClient;
import org.jemberai.dataintake.BaseTest;
import org.jemberai.dataintake.domain.EmbeddingStatusEnum;
import org.jemberai.dataintake.messages.EmbeddingRequestCompleteMessage;
import org.jemberai.dataintake.repositories.EventRecordRepository;
import lombok.val;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.ai.autoconfigure.vectorstore.milvus.MilvusServiceClientProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles({"test" , "json-logs"})
@DirtiesContext
@RecordApplicationEvents
@Testcontainers
@SpringBootTest
class EventControllerTest extends BaseTest {

    @Value("classpath:files/IMG_0546.jpeg")
    Resource jpegImage;

    @Value("classpath:files/St.-Anthonys-Triathlon-Olympic-Bike-Map-23-3002600_Final.pdf")
    Resource pdfFile;

    @Value("classpath:files/movies500Trimed.csv")
    Resource csvFile;

    @Autowired
    EventRecordRepository eventRecordRepository;

    @Autowired
    MilvusServiceClientProperties milvusServiceClientProperties;

    @Autowired
    MilvusClient milvusClient;

    @Autowired
    ApplicationEvents applicationEvents;

    @Test
    void testPostJpeg() throws Exception {
        byte[] bytes = jpegImage.getContentAsByteArray();

        //base64 encode the bytes
        String base64Encoded = Base64.getEncoder().encodeToString(bytes);

        var mvcResponse = mockMvc.perform(post("/v1/event")
                        .with(jwtRequestPostProcessor)
                        .contentType(MediaType.IMAGE_JPEG_VALUE)
                        .header("ce-specversion", "1.0")
                        .header("ce-id", "12345")
                        .header("ce-type", "io.spring.event")
                        .header("ce-source", "https://spring.io/events")
                        .header("ce-datacontenttype", "image/jpeg")
                        .header("ce-comexampleextension1", "value")
                        .content(base64Encoded))
                .andExpect(status().isCreated());

        val response = mvcResponse.andReturn().getResponse();

        response.getHeaderNames().forEach(hName -> {
            System.out.println(hName + " : " + response.getHeader(hName));
        });

        byte[] decodedBytes = Base64.getDecoder().decode(response.getContentAsByteArray());
        Path toFile = Paths.get("target", "img.jpeg");
        Files.write(toFile, decodedBytes);
    }

    @Test
    void testPostPDF() throws Exception {
        byte[] bytes = pdfFile.getContentAsByteArray();

        //base64 encode the bytes
        String base64Encoded = Base64.getEncoder().encodeToString(bytes);

        var mvcResponse = mockMvc.perform(post("/v1/event")
                        .with(jwtRequestPostProcessor)
                        .contentType(MediaType.APPLICATION_PDF_VALUE)
                        .header("ce-specversion", "1.0")
                        .header("ce-id", "12345")
                        .header("ce-type", "io.spring.event")
                        .header("ce-source", "https://spring.io/events")
                        .header("ce-datacontenttype", "application/pdf")
                        .header("ce-comexampleextension1", "value")
                        .content(base64Encoded))
                .andExpect(status().isCreated());

        val response = mvcResponse.andReturn().getResponse();

        response.getHeaderNames().forEach(hName -> {
            System.out.println(hName + " : " + response.getHeader(hName));
        });

        byte[] decodedBytes = Base64.getDecoder().decode(response.getContentAsByteArray());
        Path toFile = Paths.get("target", "saved.pdf");
        Files.write(toFile, decodedBytes);
    }

    @Test
    void testPostCsv() throws Exception {

        String csv = csvFile.getContentAsString(StandardCharsets.UTF_8);

        var mvcResponse = mockMvc.perform(post("/v1/event")
                        .with(jwtRequestPostProcessor)
                        .contentType("text/csv")
                        .header("ce-specversion", "1.0")
                        .header("ce-id", "12345")
                        .header("ce-type", "io.spring.event")
                        .header("ce-source", "https://spring.io/events")
                        .header("ce-datacontenttype", "text/csv")
                        .header("ce-comexampleextension1", "value")
                        .content(csv))
                .andExpect(status().isCreated());

        val response = mvcResponse.andReturn().getResponse();

        response.getHeaderNames().forEach(hName -> {
            System.out.println(hName + " : " + response.getHeader(hName));
        });

        Path toFile = Paths.get("target", "data.csv");
        Files.write(toFile, response.getContentAsByteArray());
    }

    @Test
    void testEventPost() throws Exception {
        var json = """
                    {
                        "appinfoA" : "abc",
                        "appinfoB" : 123,
                        "appinfoC" : true
                    }
                """;

        var mvcResult = mockMvc.perform(post("/v1/event")
                .contentType(MediaType.APPLICATION_JSON)
                        .with(jwtRequestPostProcessor)
                        .header("ce-specversion", "1.0")
                        .header("ce-id", "12345")
                        .header("ce-type", "io.spring.event")
                        .header("ce-source", "https://spring.io/events")
                        .header("ce-datacontenttype", "application/json")
                        .header("ce-comexampleextension1", "value")
                .content(json))
                .andExpect(status().isCreated());

        var response = mvcResult.andReturn().getResponse();

        System.out.println( response.getContentAsString());

        response.getHeaderNames().forEach(hName -> {
            System.out.println(hName + " : " + response.getHeader(hName));
        });
    }

    @Test
    void testGetById() throws Exception {
        var json = """
                    {
                        "appinfoA" : "abc",
                        "appinfoB" : 123,
                        "appinfoC" : true
                    }
                """;

        var mvcResult = mockMvc.perform(post("/v1/event")
                        .with(jwtRequestPostProcessor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("ce-specversion", "1.0")
                        .header("ce-id", "12345")
                        .header("ce-type", "io.spring.event")
                        .header("ce-source", "https://spring.io/events")
                        .header("ce-datacontenttype", "application/json")
                        .header("ce-comexampleextension1", "value")
                        .content(json))
                .andExpect(status().isCreated());

        var response = mvcResult.andReturn().getResponse();

        System.out.println( response.getContentAsString());

        response.getHeaderNames().forEach(hName -> {
            System.out.println(hName + " : " + response.getHeader(hName));
        });

        mockMvc.perform(get("/v1/event/{id}", response.getHeader("ce-jemberaieventid"))
                        .with(jwtRequestPostProcessor)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appinfoA").value("abc"))
                .andExpect(jsonPath("$.appinfoB").value(123))
                .andExpect(jsonPath("$.appinfoC").value(true));

        val savedId = UUID.fromString(Objects.requireNonNull(response.getHeader("ce-jemberaieventid")));

        await().atMost(60, TimeUnit.SECONDS).until(() -> eventRecordRepository.findById(savedId).get().getEmbeddingStatus().equals(EmbeddingStatusEnum.COMPLETED));

        assertThat(eventRecordRepository.findById(savedId).get().getEmbeddingStatus()).isEqualTo(EmbeddingStatusEnum.COMPLETED);

        assertThat(applicationEvents.stream(EmbeddingRequestCompleteMessage.class).count()).isEqualTo(1);
    }


    @Test
    void testDefaultEmbedding() throws Exception {
        var json = """
                    {
                        "appinfoA" : "abc",
                        "appinfoB" : 123,
                        "appinfoC" : true
                    }
                """;

        var mvcResult = mockMvc.perform(post("/v1/event")
                        .with(jwtRequestPostProcessor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("ce-specversion", "1.0")
                        .header("ce-id", "12345")
                        .header("ce-type", "UNKNOWN_EVENT_TYPE")
                        .header("ce-source", "https://spring.io/events")
                        .header("ce-datacontenttype", "application/json")
                        .header("ce-comexampleextension1", "value")
                        .content(json))
                .andExpect(status().isCreated());

        var response = mvcResult.andReturn().getResponse();

        val savedId = UUID.fromString(Objects.requireNonNull(response.getHeader("ce-jemberaieventid")));

        await().atMost(60, TimeUnit.SECONDS).until(() -> eventRecordRepository.findById(savedId).get().getEmbeddingStatus().equals(EmbeddingStatusEnum.COMPLETED));

        assertThat(eventRecordRepository.findById(savedId).get().getEmbeddingStatus()).isEqualTo(EmbeddingStatusEnum.COMPLETED);

        assertThat(applicationEvents.stream(EmbeddingRequestCompleteMessage.class).count()).isEqualTo(1);
    }

    @Test
    void testGetImageEvent() throws Exception {
        byte[] bytes = jpegImage.getContentAsByteArray();

        //base64 encode the bytes
        String base64Encoded = Base64.getEncoder().encodeToString(bytes);

        var mvcResponse = mockMvc.perform(post("/v1/event")
                        .with(jwtRequestPostProcessor)
                        .contentType(MediaType.IMAGE_JPEG_VALUE)
                        .header("ce-specversion", "1.0")
                        .header("ce-id", "12345")
                        .header("ce-type", "io.spring.event")
                        .header("ce-source", "https://spring.io/events")
                        .header("ce-datacontenttype", "image/jpeg")
                        .header("ce-comexampleextension1", "value")
                        .content(base64Encoded))
                .andExpect(status().isCreated());

        var response = mvcResponse.andReturn().getResponse();

        var mvcResponse2 = mockMvc.perform(get("/v1/event/{id}", response.getHeader("ce-jemberaieventid"))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .with(jwtRequestPostProcessor))
                .andExpect(status().isOk())
                .andExpect(content().string(base64Encoded));

    }

    @Disabled // just for testing the behavior of cloud events
    @Test
    void testEventPostCE() throws Exception {
        var json = """
                {
                    "specversion" : "1.0",
                    "type" : "com.example.someevent",
                    "source" : "/mycontext",
                    "subject": null,
                    "id" : "C234-1234-1234",
                    "time" : "2018-04-05T17:31:00Z",
                    "comexampleextension1" : "value",
                    "comexampleothervalue" : 5,
                    "datacontenttype" : "application/json",
                    "data" : {
                        "appinfoA" : "abc",
                        "appinfoB" : 123,
                        "appinfoC" : true
                    }
                }
                """;

        var foo = mockMvc.perform(post("/v1/event")
                        .with(jwtRequestPostProcessor)
                        .contentType(new MediaType("application", "cloudevents+json"))
                        .content(json))
                .andExpect(status().isOk());

        System.out.println( foo.andReturn().getResponse().getContentAsString());
    }
}