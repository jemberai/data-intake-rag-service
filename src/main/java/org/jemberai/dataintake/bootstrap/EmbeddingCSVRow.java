/*
 * Copyright 2023 - 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jemberai.dataintake.bootstrap;

import com.opencsv.bean.CsvBindByName;
import org.jemberai.dataintake.domain.EmbeddingModelEnum;
import org.jemberai.dataintake.domain.ModelEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by jt, Spring Framework Guru.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingCSVRow {
    @CsvBindByName
    private String clientId;

    @CsvBindByName
    private String eventType;

    @CsvBindByName
    private ModelEnum modelName;

    @CsvBindByName
    private EmbeddingModelEnum embeddingModel;
}
