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

import io.cloudevents.CloudEvent;
import io.cloudevents.core.v1.CloudEventBuilder;
import org.jemberai.dataintake.domain.EventExtensionRecord;
import org.jemberai.dataintake.domain.EventRecord;
import org.jemberai.dataintake.messages.NewEventMessage;
import org.jemberai.dataintake.repositories.EventRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Created by jt, Spring Framework Guru.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class EventRecordServiceImpl implements EventRecordService {

    public static final String JEMBERAIEVENTID = "jemberaieventid";
    private final EventRecordRepository eventRecordRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public Optional<CloudEvent> findById(UUID id) {
        EventRecord er = eventRecordRepository.findById(id).orElse(null);

        if (er != null) {
            try {
                return Optional.of(eventRecordToCloudEvent(er));
            } catch (URISyntaxException e) {
                e.printStackTrace();
                throw  new IllegalStateException("Error converting EventRecord to CloudEvent");
            }
        }

        return Optional.empty();
    }

    /**
     * Save a CloudEvent to the database. This will return a CloudEvent with the
     * jemberaiEventId extension set to the id of the saved event record.
     * @param event CloudEvent to save
     * @return updated CloudEvent
     */
    @Override
    public CloudEvent save(CloudEvent event, String clientId) {
        var eventRecord = cloudEventToEventRecord(event, clientId);

        var savedEventRecord = eventRecordRepository.save(eventRecord);

        log.debug("Saved EventRecord Id: {}", savedEventRecord.getId());
        log.debug("Publishing NewEventMessage");
        applicationEventPublisher.publishEvent(NewEventMessage.builder().newEventRecord(savedEventRecord).build());

        var ceBuilder = new CloudEventBuilder(event);

        ceBuilder.withExtension(JEMBERAIEVENTID, savedEventRecord.getId().toString());

        if (savedEventRecord.getExtensions() != null ){
            savedEventRecord.getExtensions().add(EventExtensionRecord.builder()
                    .fieldName(JEMBERAIEVENTID)
                    .fieldValue(savedEventRecord.getId().toString())
                    .build());
        } else {
            savedEventRecord.setExtensions(Set.of(EventExtensionRecord.builder()
                    .fieldName(JEMBERAIEVENTID)
                    .fieldValue(savedEventRecord.getId().toString())
                    .build()));
        }

        return ceBuilder.build();
    }

    private EventRecord cloudEventToEventRecord(CloudEvent event, String clientId){
        var builder = EventRecord.builder()
                .clientId(clientId)
                .specVersion(event.getSpecVersion().toString())
                .eventType(event.getType())
                .subject(event.getSubject())
                .dataContentType(event.getDataContentType());

        if (event.getSource() != null) {
            builder.source(event.getSource().toString());
        }

        if (event.getData() != null) {
            builder.data(event.getData().toBytes());
            String sha256hex = org.apache.commons.codec.digest.DigestUtils.sha256Hex(event.getData().toBytes());
            builder.sha256(sha256hex);
        }

        if (event.getTime() != null) {
            builder.time(event.getTime());
        }

        if (event.getExtensionNames() != null && !event.getExtensionNames().isEmpty()){
            var extensionRecords = event.getExtensionNames().stream()
                    .map(name -> EventExtensionRecord.builder()
                            .fieldName(name)
                            .fieldValue(event.getExtension(name) == null ? "" : event.getExtension(name).toString()) //todo prob need to make this more robust
                            .build())
                    .collect(Collectors.toSet());

            builder.extensions(extensionRecords);
        }

        return builder.build();
    }

    private CloudEvent eventRecordToCloudEvent(EventRecord eventRecord) throws URISyntaxException {
        var builder = new CloudEventBuilder()
                .withId(eventRecord.getId().toString())
                .withType(eventRecord.getEventType())
                .withSubject(eventRecord.getSubject())
                .withTime(eventRecord.getTime())
                .withDataContentType(eventRecord.getDataContentType())
                .withData(eventRecord.getDataContentType(), eventRecord.getData());

        if (eventRecord.getSource() != null) {
            builder.withSource(new URI(eventRecord.getSource()));
        }

        if (eventRecord.getExtensions() != null && !eventRecord.getExtensions().isEmpty()){
            eventRecord.getExtensions().forEach(er -> builder.withExtension(er.getFieldName(), er.getFieldValue()));
        }

        return builder.build();
    }
}
