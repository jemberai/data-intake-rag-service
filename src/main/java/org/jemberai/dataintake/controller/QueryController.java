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

import lombok.RequiredArgsConstructor;
import org.jemberai.dataintake.model.QueryRequest;
import org.jemberai.dataintake.model.QueryResponseDocument;
import org.jemberai.dataintake.service.QueryService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Created by jt, Spring Framework Guru.
 */
@RequiredArgsConstructor
@RestController
public class QueryController {

    private final QueryService queryService;

    @PostMapping("/v1/query")
    public List<QueryResponseDocument> getDocuments(@RequestParam(required = false) String clientId,
                                                    @RequestBody QueryRequest queryRequest,
                                                    Authentication authentication) {
        // TODO Handle clientId from authentication to better track client requests
        if (clientId == null || clientId.isEmpty()) {
            clientId = authentication.getName();
        }

        return queryService.getDocuments(clientId, queryRequest);
    }
}
