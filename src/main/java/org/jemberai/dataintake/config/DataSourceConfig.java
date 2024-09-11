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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.flyway.FlywayDataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateProperties;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Objects;

/**
 * Created by jt, Spring Framework Guru.
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = {"org.jemberai.dataintake.repositories"},
        entityManagerFactoryRef = "entityManagerFactoryPrimary",
        transactionManagerRef = "transactionManagerPrimary")
public class DataSourceConfig {

    @Primary
    @Bean
    public HikariDataSource dataSourcePrimary(@Qualifier("dataSourcePropertiesPrimary") DataSourceProperties dataSourcePropertiesPrimary) {
        return dataSourcePropertiesPrimary.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @FlywayDataSource
    @Bean("dataSourcePrimaryFlyway")
    public DataSource dataSourcePrimaryFlyway(@Qualifier("dataSourcePropertiesPrimaryFlyway") DataSourceProperties dataSourcePropertiesKeyStore) {
        return dataSourcePropertiesKeyStore.initializeDataSourceBuilder()
                .build();
    }

    @Primary
    @Bean
    @ConfigurationProperties("org.jemberai.jpa.primary.hibernate")
    public HibernateProperties hibernatePropertiesPrimary() {
        return new HibernateProperties();
    }

    @Bean
    public HibernateBaseDataSourceConfig hibernateBaseDataSourceConfigPrimary(@Qualifier("dataSourcePrimary") DataSource dataSourcePrimary,
                                                                              @Qualifier("jpaPropertiesPrimary") JpaProperties jpaPropertiesPrimary,
                                                                              @Qualifier("hibernatePropertiesPrimary") HibernateProperties hibernatePropertiesPrimary) {
        return new HibernateBaseDataSourceConfig(dataSourcePrimary,
                jpaPropertiesPrimary, hibernatePropertiesPrimary);
    }

    @Primary
    @Bean("entityManagerFactoryPrimary")
    public LocalContainerEntityManagerFactoryBean entityManagerFactoryPrimary(@Qualifier("hibernateBaseDataSourceConfigPrimary") HibernateBaseDataSourceConfig hibernateBaseDataSourceConfigPrimary,
                                                                              @Qualifier("jpaPropertiesPrimary") JpaProperties jpaPropertiesPrimary,
                                                                              ConfigurableListableBeanFactory beanFactory) {
        EntityManagerFactoryBuilder emfBuilder = hibernateBaseDataSourceConfigPrimary.createEntityManagerFactoryBuilder(jpaPropertiesPrimary);

        return hibernateBaseDataSourceConfigPrimary.entityManagerFactory(emfBuilder,
                beanFactory, "org.jemberai.dataintake.domain", "primary");
    }

    @Primary
    @Bean("transactionManagerPrimary")
    public PlatformTransactionManager transactionManagerPrimary(final @Qualifier("entityManagerFactoryPrimary") LocalContainerEntityManagerFactoryBean builder) {
        return new JpaTransactionManager(Objects.requireNonNull(builder.getObject()));
    }

    @Bean
    public OpenEntityManagerInViewInterceptor openEntityManagerInViewInterceptor(@Qualifier("jpaPropertiesPrimary") JpaProperties jpaPropertiesPrimary) {
        if (jpaPropertiesPrimary.getOpenInView() == null) {
            log.warn("spring.jpa.open-in-view is enabled by default. "
                    + "Therefore, database queries may be performed during view "
                    + "rendering. Explicitly configure spring.jpa.open-in-view to disable this warning");
        }
        return new OpenEntityManagerInViewInterceptor();
    }
}
