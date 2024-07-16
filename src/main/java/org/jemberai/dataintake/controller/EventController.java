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

import io.cloudevents.CloudEvent;
import io.cloudevents.core.v1.CloudEventBuilder;
import org.jemberai.dataintake.service.EventRecordService;
import lombok.RequiredArgsConstructor;
import org.jemberai.dataintake.service.EventRecordServiceImpl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

/**
 * Created by jt, Spring Framework Guru.
 */

@RequiredArgsConstructor
@RestController
public class EventController {

    private final EventRecordService eventRecordService;

    @GetMapping(value ="/v1/event/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CloudEvent> getEventById(@PathVariable("id") UUID id) {
        return eventRecordService.findById(id)
                .map(event -> new CloudEventBuilder(event).build())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/v1/event")
    public ResponseEntity<CloudEvent> ce(@RequestBody CloudEvent event, Authentication authentication) throws IOException {

        //get authentication user from spring security
        String clientId = authentication.getName();

        CloudEvent savedEvent = eventRecordService.save(event, clientId);

        return ResponseEntity.created(URI.create("/v1/event/" + savedEvent.getExtension(EventRecordServiceImpl.JEMBERAIEVENTID)))
                .body(new CloudEventBuilder(savedEvent).build());
    }
}
