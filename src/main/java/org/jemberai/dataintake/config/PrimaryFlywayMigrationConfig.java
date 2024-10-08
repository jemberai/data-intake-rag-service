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

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Created by jt, Spring Framework Guru.
 */
@Configuration
public class PrimaryFlywayMigrationConfig {

    public static final String LOCATION = "classpath:db/migration";
    public static final String VENDOR_LOCATION_PLACEHOLDER = "classpath:db/vendor/{vendor}";

    @Bean
    public FlywayMigrator flywayMigratorPrimary(@Qualifier("dataSourcePrimaryFlyway") DataSource dataSourcePrimaryFlyway) {
        return new FlywayMigrator( dataSourcePrimaryFlyway, LOCATION, VENDOR_LOCATION_PLACEHOLDER);
    }
}
