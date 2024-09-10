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

package org.jemberai.dataintake.integration;

import org.jemberai.dataintake.domain.EmbeddingModelEnum;
import org.jemberai.dataintake.domain.EventRecord;
import org.jemberai.dataintake.domain.ModelEnum;
import org.jemberai.dataintake.messages.EmbeddingRequestCompleteMessage;
import org.jemberai.dataintake.messages.EmbeddingRequestMessage;
import org.jemberai.dataintake.service.EmbeddingServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.ai.autoconfigure.openai.OpenAiConnectionProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DirtiesContext
@Testcontainers
@SpringBootTest
@RecordApplicationEvents
class EmbeddingServiceImplTest extends BaseIT{

    @Autowired
    EmbeddingServiceImpl embeddingService;

    @Autowired
    OpenAiConnectionProperties openAiConnectionProperties;

    @Autowired
    ApplicationEvents events;

    @Test
    void testOpenAIAPIKeySet() {
        assertNotNull(openAiConnectionProperties.getApiKey());
    }

    @Test
    void processOpenAPIEmbeddingRequest() {
        var json = """
                    {
                        "appinfoA" : "abc",
                        "appinfoB" : 123,
                        "appinfoC" : true
                    }
                """;

        var eventRecord = EventRecord.builder()
                .id(UUID.randomUUID())
                .eventId("idasdf")
                .clientId("jember-client")
                .eventType("io.spring.event")
                .dataContentType("application/json")
                .data(json.getBytes())
                .build();

        var message = EmbeddingRequestMessage.builder()
                .eventRecord(eventRecord)
                .model(ModelEnum.OPENAI)
                .embeddingModel(EmbeddingModelEnum.TEXT_EMBEDDING_3_SMALL)
                .build();

        embeddingService.processOpenAPIEmbeddingRequest(message);

        long numEvents = events.stream(EmbeddingRequestCompleteMessage.class).count();
        assertThat(numEvents).isEqualTo(1);
    }
}