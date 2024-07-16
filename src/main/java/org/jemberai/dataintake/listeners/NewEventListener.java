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

package org.jemberai.dataintake.listeners;

import org.jemberai.dataintake.domain.EmbeddingConfiguration;
import org.jemberai.dataintake.domain.EmbeddingModelEnum;
import org.jemberai.dataintake.domain.ModelEnum;
import org.jemberai.dataintake.messages.EmbeddingRequestMessage;
import org.jemberai.dataintake.messages.NewEventMessage;
import org.jemberai.dataintake.repositories.EmbeddingConfigurationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by jt, Spring Framework Guru.
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class NewEventListener {

    private final EmbeddingConfigurationRepository embeddingConfigurationRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    private static final ModelEnum DEFAULT_MODEL = ModelEnum.OPENAI;
    private static final EmbeddingModelEnum DEFAULT_EMBEDDING_MODEL = EmbeddingModelEnum.TEXT_EMBEDDING_3_SMALL;

    @Async
    @EventListener
    public void listen(NewEventMessage msg) {

        log.debug("New event received: " + msg.getNewEventRecord().getId());

        List<EmbeddingConfiguration> embeddingConfigurations = embeddingConfigurationRepository
                .findByClientIdAndEventType(msg.getNewEventRecord().getClientId(),
                        msg.getNewEventRecord().getEventType());

        if (embeddingConfigurations.isEmpty()) {
            log.debug("No embedding configurations found for event: " + msg.getNewEventRecord().getId());

            applicationEventPublisher.publishEvent(EmbeddingRequestMessage.builder()
                    .eventRecord(msg.getNewEventRecord())
                    .model(DEFAULT_MODEL)
                    .embeddingModel(DEFAULT_EMBEDDING_MODEL)
                    .build());
        } else {
            embeddingConfigurations.forEach(embeddingConfiguration -> {

                log.debug("Sending embedding request for event: " + msg.getNewEventRecord().getId());

                applicationEventPublisher.publishEvent(EmbeddingRequestMessage.builder()
                        .eventRecord(msg.getNewEventRecord())
                        .model(embeddingConfiguration.getModelName())
                        .embeddingModel(embeddingConfiguration.getEmbeddingModel())
                        .build());
            });
        }
    }
}
