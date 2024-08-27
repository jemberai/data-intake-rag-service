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

package org.jemberai.dataintake.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.DatabaseMetaData;

/**
 * Created by jt, Spring Framework Guru.
 */
@Slf4j
@RequiredArgsConstructor
public class FlywayMigrator {
    private final DataSource dataSource;
    private final String location;
    private final String vendorLocationPlaceholder;

    void doMigration() {
        log.info("Starting KeyStore Flyway migration");

        final String VENDOR_PLACEHOLDER = "{vendor}";
        DatabaseDriver databaseDriver = getDatabaseDriver(this.dataSource);
        String vendor = databaseDriver.getId();
        String vendorLocation = this.vendorLocationPlaceholder.replace(VENDOR_PLACEHOLDER, vendor);

        String[] locations = {this.location, vendorLocation};

        Flyway flyway = Flyway.configure()
                .dataSource(this.dataSource)
                .locations(locations)
                .baselineOnMigrate(true)
                .encoding(StandardCharsets.UTF_8)
                .load();

        flyway.migrate();

        log.info("KeyStore Flyway migration complete");
    }

    private DatabaseDriver getDatabaseDriver(DataSource dataSource) {
        try {
            String url = JdbcUtils.extractDatabaseMetaData(dataSource, DatabaseMetaData::getURL);
            return DatabaseDriver.fromJdbcUrl(url);
        }
        catch (MetaDataAccessException ex) {
            throw new IllegalStateException(ex);
        }
    }
}