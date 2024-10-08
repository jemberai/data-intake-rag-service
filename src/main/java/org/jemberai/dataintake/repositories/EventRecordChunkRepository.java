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

package org.jemberai.dataintake.repositories;

import org.jemberai.dataintake.domain.EventRecordChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Created by jt, Spring Framework Guru.
 */
public interface EventRecordChunkRepository extends JpaRepository<EventRecordChunk, UUID> {

    List<EventRecordChunk> findAllByEmbeddingIdIn(List<String> embeddingIds);

    List<EventRecordChunk> findAllByEventRecord_ClientIdAndEmbeddingIdIn(String clientId, List<String> embeddingIds);
}
