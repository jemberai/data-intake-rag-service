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

package org.jemberai.dataintake.bootstrap;

import com.opencsv.bean.CsvToBeanBuilder;
import org.jemberai.dataintake.domain.ClientConfiguration;
import org.jemberai.dataintake.domain.EmbeddingConfiguration;
import org.jemberai.dataintake.repositories.ClientConfigurationRepository;
import org.jemberai.dataintake.repositories.EmbeddingConfigurationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileReader;
import java.util.List;

/**
 * Created by jt, Spring Framework Guru.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InitConfigData implements CommandLineRunner {

    private final EmbeddingConfigurationRepository embeddingConfigurationRepository;
    private final ClientConfigurationRepository clientConfigurationRepository;

    @Override
    public void run(String... args) throws Exception {
        initEmbeddingConfig();
        initClientConfig();
    }

    private void initClientConfig() {
        saveOrUpdateClientConfig("jember-client", "jember-client");

        log.debug("showing client config");
        embeddingConfigurationRepository.findAll().forEach(embeddingConfiguration -> {
            System.out.println(embeddingConfiguration.toString());
        });
    }

    private void saveOrUpdateClientConfig(String clientId, String milvusCollection){
        clientConfigurationRepository.findByClientId(clientId)
                .ifPresentOrElse(clientConfiguration -> {
                    clientConfiguration.setMilvusCollection(milvusCollection);
                    clientConfigurationRepository.save(clientConfiguration);
                }, () -> {
                    clientConfigurationRepository.save(ClientConfiguration.builder()
                            .clientId(clientId)
                            .milvusCollection(milvusCollection)
                            .build());
                });
    }

    private void initEmbeddingConfig() throws Exception {
        File file = ResourceUtils.getFile("classpath:csvdata/embeddingcfg.csv");

        List<EmbeddingCSVRow> csvRecords = new CsvToBeanBuilder<EmbeddingCSVRow>(new FileReader(file))
                .withType(EmbeddingCSVRow.class)
                .build().parse();

        csvRecords.forEach(csvRecord -> {
            embeddingConfigurationRepository.findByClientIdAndEventTypeAndModelName(csvRecord.getClientId(), csvRecord.getEventType(), csvRecord.getModelName())
                    .ifPresentOrElse(embeddingConfiguration -> {
                        embeddingConfiguration.setEmbeddingModel(csvRecord.getEmbeddingModel());
                        embeddingConfigurationRepository.save(embeddingConfiguration);
                    }, () -> {
                        EmbeddingConfiguration embeddingConfiguration = new EmbeddingConfiguration();
                        embeddingConfiguration.setClientId(csvRecord.getClientId());
                        embeddingConfiguration.setEventType(csvRecord.getEventType());
                        embeddingConfiguration.setModelName(csvRecord.getModelName());
                        embeddingConfiguration.setEmbeddingModel(csvRecord.getEmbeddingModel());
                        embeddingConfigurationRepository.save(embeddingConfiguration);
                    });
        });
    }
}
