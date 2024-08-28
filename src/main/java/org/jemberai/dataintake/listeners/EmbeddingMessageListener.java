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

import org.jemberai.dataintake.domain.EmbeddingStatusEnum;
import org.jemberai.dataintake.messages.EmbeddingRequestCompleteMessage;
import org.jemberai.dataintake.messages.EmbeddingRequestMessage;
import org.jemberai.dataintake.repositories.EventRecordRepository;
import org.jemberai.dataintake.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Created by jt, Spring Framework Guru.
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class EmbeddingMessageListener {

    private final EmbeddingService embeddingService;
    private final EventRecordRepository eventRecordRepository;

    @Async
    @EventListener
    public void listen(EmbeddingRequestMessage msg) {

        log.debug("Received embedding request message: Event RecordId " + msg.getEventRecord().getId());
        //using switch since this list will grow
        switch (msg.getModel()) {
            case OPENAI:
                embeddingService.processOpenAPIEmbeddingRequest(msg);
                break;
            default:
                throw new RuntimeException("Unknown embedding model: " + msg.getEmbeddingModel());
        }
    }

    @Async
    @EventListener
    public void listen(EmbeddingRequestCompleteMessage msg) {

        log.debug("Received embedding request complete message. Event Record Id " + msg.getEventRecord().getId());

        eventRecordRepository.findById(msg.getEventRecord().getId()).ifPresentOrElse(eventRecord -> {

            log.debug("Updating event record with embedding status COMPLETED: " + msg.getEventRecord().getId());

            eventRecord.setEmbeddingStatus(EmbeddingStatusEnum.COMPLETED);
            eventRecordRepository.saveAndFlush(eventRecord);
        },() -> {
                log.warn("Event record not found for id: " + msg.getEventRecord().getId());
            }
        );
    }
}
