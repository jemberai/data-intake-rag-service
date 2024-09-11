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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jemberai.dataintake.domain.EmbeddingStatusEnum;
import org.jemberai.dataintake.domain.EventRecordChunk;
import org.jemberai.dataintake.messages.EmbeddingRequestCompleteMessage;
import org.jemberai.dataintake.messages.EmbeddingRequestMessage;
import org.jemberai.dataintake.repositories.EventRecordRepository;
import org.jemberai.dataintake.service.EmbeddingService;
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
    private final ObjectMapper objectMapper;

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
        switch (msg.getStatus()) {
            case SUCCESS:
                log.debug("Received embedding request success message. Event Record Id " + msg.getEventRecord().getId());
                processSuccessMessage(msg);
                break;
            case EMPTY:
                log.debug("Received embedding request empty message. Event Record Id " + msg.getEventRecord().getId());
                processEmptyMessage(msg);
                break;
            case ERROR:
                log.debug("Received embedding request error message. Event Record Id " + msg.getEventRecord().getId());
                processErrorMessage(msg);
                break;
            default:
                throw new RuntimeException("Unknown embedding request status: " + msg.getStatus());
        }
    }

    private void processSuccessMessage(EmbeddingRequestCompleteMessage msg) {
        eventRecordRepository.findById(msg.getEventRecord().getId()).ifPresentOrElse(eventRecord -> {

                    log.debug("Updating event record with embedding status COMPLETED: " + msg.getEventRecord().getId());

                    eventRecord.setEmbeddingStatus(EmbeddingStatusEnum.COMPLETED);

                    if (msg.getDocument().metadata() != null) {
                        try {
                            eventRecord.setTikaMetadata(objectMapper.writeValueAsString(msg.getDocument().metadata().toMap()));
                        } catch (JsonProcessingException e) {
                            log.error("Error processing metadata for event record id: {}", msg.getEventRecord().getId());
                        }
                    }

                    msg.getTextSegments().forEach((key, value) -> eventRecord.addChunk(EventRecordChunk.builder()
                            .embeddingId(key)
                            .data(value.text().getBytes())
                            .build()));

                    eventRecordRepository.saveAndFlush(eventRecord);
                },() -> {
                    log.warn("Event record not found for id: " + msg.getEventRecord().getId());
                }
        );
    }

    private void processEmptyMessage(EmbeddingRequestCompleteMessage msg) {
        eventRecordRepository.findById(msg.getEventRecord().getId()).ifPresentOrElse(eventRecord -> {

                    log.debug("Updating event record with embedding status EMPTY: " + msg.getEventRecord().getId());

                    eventRecord.setEmbeddingStatus(EmbeddingStatusEnum.NO_DATA);

                    eventRecordRepository.saveAndFlush(eventRecord);
                },() -> {
                    log.warn("Event record not found for id: " + msg.getEventRecord().getId());
                }
        );
    }

    private void processErrorMessage(EmbeddingRequestCompleteMessage msg) {
        eventRecordRepository.findById(msg.getEventRecord().getId()).ifPresentOrElse(eventRecord -> {

                    log.error("Updating event record with embedding status ERROR: " + msg.getEventRecord().getId());

                    eventRecord.setEmbeddingStatus(EmbeddingStatusEnum.ERROR);

                    eventRecordRepository.saveAndFlush(eventRecord);
                },() -> {
                    log.warn("Event record not found for id: " + msg.getEventRecord().getId());
                }
        );
    }
}
