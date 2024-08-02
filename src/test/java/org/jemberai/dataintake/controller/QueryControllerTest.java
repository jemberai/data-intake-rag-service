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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jemberai.dataintake.BaseTest;
import org.jemberai.dataintake.domain.EmbeddingStatusEnum;
import org.jemberai.dataintake.model.QueryRequest;
import org.jemberai.dataintake.repositories.EventRecordRepository;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DirtiesContext
@Testcontainers
@SpringBootTest
class QueryControllerTest extends BaseTest {

    @Value("classpath:files/movies200Trimed.csv")
    Resource csvFile;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    EventRecordRepository eventRecordRepository;

    @BeforeEach
    void setUp() throws Exception {

        mockMvc = MockMvcBuilders
                .webAppContextSetup(wac)
                .apply(springSecurity())
                .build();

        //load movies csv
        String csv = csvFile.getContentAsString(StandardCharsets.UTF_8);

        var response = mockMvc.perform(post("/v1/event")
                        .with(jwtRequestPostProcessor)
                        .contentType("text/csv")
                        .header("ce-specversion", "1.0")
                        .header("ce-id", "12345")
                        .header("ce-type", "io.spring.event")
                        .header("ce-source", "https://spring.io/events")
                        .header("ce-datacontenttype", "text/csv")
                        .header("ce-comexampleextension1", "value")
                        .content(csv))
                .andExpect(status().isCreated())
                .andReturn().getResponse();

        val savedId = UUID.fromString(Objects.requireNonNull(response.getHeader("ce-jembereventid")));

        await().atMost(300, TimeUnit.SECONDS).until(() -> eventRecordRepository.findById(savedId).get().getEmbeddingStatus().equals(EmbeddingStatusEnum.COMPLETED));

        System.out.println("Event saved and embedded with id: " + savedId);
    }

    @Disabled
    @Test
    void getDocuments() throws Exception {

        val queryRequest = QueryRequest.builder()
                .query("Spiderman")
                .build();

        var mvcResponse = mockMvc.perform(post("/v1/query")
                .with(jwtRequestPostProcessor)
                .contentType("application/json").content(objectMapper.writeValueAsString(queryRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andReturn();

        val body = mvcResponse.getResponse().getContentAsString();

        System.out.println(body);
    }
}