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

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Created by jt, Spring Framework Guru.
 */
@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "org.jemberai")
public class JemberProperties {

    private Llm llm = new Llm();
    private Vectorstore vectorstore = new Vectorstore();
    private DataSource datasource = new DataSource();
    private Jpa jpa = new Jpa();
    private CryptographyProperties cryptography = new CryptographyProperties();

    @Setter
    @Getter
    public static class Llm {
        private OpenAi openAi = new OpenAi();

        @Getter
        @Setter
        public static class OpenAi {
            private String apiKey;
        }
    }

    @Setter
    @Getter
    public static class Vectorstore {
        private Milvus milvus = new Milvus();

        @Getter
        @Setter
        public static class Milvus {
            private String host;
            private String port;
            private String databaseName;
            private String username;
            private String password;
        }
    }

    @Configuration
    @Setter
    @ConfigurationProperties(prefix = "org.jemberai.datasource")
    public static class DataSource {
        @NestedConfigurationProperty
        private DataSourceProperties primary = new DataSourceProperties();

        @NestedConfigurationProperty
        private DataSourceProperties primaryFlyway = new DataSourceProperties();

        @NestedConfigurationProperty
        private DataSourceProperties keystore = new DataSourceProperties();

        @NestedConfigurationProperty
        private DataSourceProperties keystoreFlyway = new DataSourceProperties();

        @Primary
        @Bean("dataSourcePropertiesPrimary")
        public DataSourceProperties getPrimary() {
            return primary;
        }

        @Bean("dataSourcePropertiesPrimaryFlyway")
        public DataSourceProperties getPrimaryFlyway() {
            return primaryFlyway;
        }

        @Bean("dataSourcePropertiesKeyStore")
        public DataSourceProperties getKeystore() {
            return keystore;
        }

        @Bean("dataSourcePropertiesKeyStoreFlyway")
        public DataSourceProperties getKeystoreFlyway() {
            return keystoreFlyway;
        }
    }

    @Configuration
    @Setter
    public static class Jpa {

        @NestedConfigurationProperty
        private JpaProperties primary = new JpaProperties();

        @NestedConfigurationProperty
        private JpaProperties keystore = new JpaProperties();

        @Primary
        @Bean("jpaPropertiesPrimary")
        public JpaProperties getPrimary() {
            return primary;
        }

        @Bean("jpaPropertiesKeyStore")
        public JpaProperties getKeystore() {
            return keystore;
        }
    }

    @Getter
    @Setter
    public static class CryptographyProperties {
        private String jemberKeyId;
        private String jemberAesKey;
        private String jemberHmacKey;
    }
}