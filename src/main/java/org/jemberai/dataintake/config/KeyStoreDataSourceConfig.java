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

import com.zaxxer.hikari.HikariDataSource;
import org.jemberai.cryptography.domain.EncryptionKeysListener;
import org.jemberai.cryptography.keymanagement.AesKeyDTO;
import org.jemberai.cryptography.keymanagement.InMemoryKeyService;
import org.jemberai.cryptography.keymanagement.JpaKeyService;
import org.jemberai.cryptography.keymanagement.KeyService;
import org.jemberai.cryptography.provider.EncryptionProvider;
import org.jemberai.cryptography.provider.EncryptionProviderImpl;
import org.jemberai.cryptography.repositories.DefaultEncryptionKeyRepository;
import org.jemberai.cryptography.repositories.EncryptionKeysRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateProperties;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

/**
 * Created by jt, Spring Framework Guru.
 */
@Configuration(proxyBeanMethods = false)
@EnableCaching
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = {"org.jemberai.cryptography.repositories"},
        entityManagerFactoryRef = "keyStoreEntityManagerFactory",
        transactionManagerRef = "keyStoreTransactionManager")
public class KeyStoreDataSourceConfig {

    @Bean
    @ConfigurationProperties("org.jemberai.datasource.keystore")
    public DataSourceProperties dataSourcePropertiesKeyStore() {
        return new DataSourceProperties();
    }

    @Bean
    public HikariDataSource dataSourceKeyStore(@Qualifier("dataSourcePropertiesKeyStore") DataSourceProperties dataSourcePropertiesKeyStore) {
        return dataSourcePropertiesKeyStore.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean
    @ConfigurationProperties("org.jemberai.jpa.keystore")
    public JpaProperties jpaPropertiesKeyStore() {
        return new JpaProperties();
    }

    @Bean
    @ConfigurationProperties("org.jemberai.jpa.keystore.hibernate")
    public HibernateProperties hibernatePropertiesKeyStore() {
        return new HibernateProperties();
    }

    @Bean
    public HibernateBaseDataSourceConfig hibernateBaseDataSourceConfigKeyStore(@Qualifier("dataSourceKeyStore") DataSource dataSourceKeyStore,
                                                                               @Qualifier("jpaPropertiesKeyStore") JpaProperties jpaPropertiesKeyStore,
                                                                               @Qualifier("hibernatePropertiesKeyStore") HibernateProperties hibernatePropertiesKeyStore) {
        return new HibernateBaseDataSourceConfig(dataSourceKeyStore,
                jpaPropertiesKeyStore, hibernatePropertiesKeyStore);
    }

    @Bean("keyStoreEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean keyStoreEntityManagerFactory(@Qualifier("hibernateBaseDataSourceConfigKeyStore") HibernateBaseDataSourceConfig hibernateBaseDataSourceConfigKeyStore,
                                                                               @Qualifier("jpaPropertiesKeyStore") JpaProperties jpaPropertiesKeyStore,
                                                                               ConfigurableListableBeanFactory beanFactory,
                                                                               @Qualifier("flywayMigratorKeyStore") FlywayMigrator flywayMigratorKeyStore) {
        //perform migration before Hibernate initialization, hibernate validation will fail if the schema is not up to date
        flywayMigratorKeyStore.doMigration();

        EntityManagerFactoryBuilder emfBuilder = hibernateBaseDataSourceConfigKeyStore.createEntityManagerFactoryBuilder(jpaPropertiesKeyStore);

        return hibernateBaseDataSourceConfigKeyStore.entityManagerFactory(emfBuilder, beanFactory,
                "org.jemberai.cryptography.domain", "keyStore");
    }

    @Bean
    @ConfigurationProperties(prefix = "org.jemberai.cryptography")
    public CryptographyProperties cryptographyProperties() {
        return new CryptographyProperties();
    }

    @Bean
    public JpaKeyService jpaKeyService(EncryptionKeysRepository aesKeyRepository, DefaultEncryptionKeyRepository defaultKeyRepository) {
        return new JpaKeyService(aesKeyRepository, defaultKeyRepository);
    }

    @Bean
    EncryptionProvider encryptionProviderJPA(JpaKeyService jpaKeyService) {
        return new EncryptionProviderImpl(jpaKeyService);
    }

    @Bean
    public PlatformTransactionManager keyStoreTransactionManager(final @Qualifier("keyStoreEntityManagerFactory") LocalContainerEntityManagerFactoryBean builder) {
        return new JpaTransactionManager(Objects.requireNonNull(builder.getObject()));
    }

    @Primary
    @Bean("encryptionProviderInternal")
    public EncryptionProvider encryptionProviderInternal(CryptographyProperties cryptographyProperties) {
        KeyService keyService = new InMemoryKeyService(AesKeyDTO
                .builder()
                .clientId(EncryptionKeysListener.JEMBER_INTERNAL)
                .keyId(UUID.fromString(cryptographyProperties.getJemberKeyId()))
                .aesKey(Base64.getDecoder().decode(cryptographyProperties.getJemberAesKey()))
                .hmacKey(Base64.getDecoder().decode(cryptographyProperties.getJemberHmacKey()))
                .build());

        return new EncryptionProviderImpl(keyService);
    }

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("defaultKey", "getKeyById");
    }

    @Bean
    @ConfigurationProperties("org.jemberai.datasource.keystore-flyway")
    public DataSourceProperties dataSourcePropertiesKeyStoreFlyway() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource dataSourceKeyStoreFlyway(@Qualifier("dataSourcePropertiesKeyStoreFlyway") DataSourceProperties dataSourcePropertiesKeyStore) {
        return dataSourcePropertiesKeyStore.initializeDataSourceBuilder()
                .build();
    }
}
