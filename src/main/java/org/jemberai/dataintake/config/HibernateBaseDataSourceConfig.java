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

import org.hibernate.cfg.AvailableSettings;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateProperties;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateSettings;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.orm.hibernate5.SpringBeanContainer;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.AbstractJpaVendorAdapter;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

/**
 * Largely taken from Spring Boot's HibernateJpaConfiguration used in Spring Boot auto configuration.
 *
 * Created by jt, Spring Framework Guru.
 */
public class HibernateBaseDataSourceConfig {
    private final DataSource dataSource;
    private final JpaProperties properties;
    private final HibernateProperties hibernateProperties;


    public HibernateBaseDataSourceConfig(DataSource dataSource, JpaProperties properties,
                                         HibernateProperties hibernateProperties) {
        this.dataSource = dataSource;
        this.properties = properties;
        this.hibernateProperties = hibernateProperties;
    }

    protected EntityManagerFactoryBuilder createEntityManagerFactoryBuilder(JpaProperties jpaProperties) {
        JpaVendorAdapter jpaVendorAdapter = jpaVendorAdapter();
        return new EntityManagerFactoryBuilder(jpaVendorAdapter, jpaProperties.getProperties(), null);
    }

    public JpaVendorAdapter jpaVendorAdapter() {
        AbstractJpaVendorAdapter adapter = createJpaVendorAdapter();
        adapter.setShowSql(this.properties.isShowSql());
        if (this.properties.getDatabase() != null) {
            adapter.setDatabase(this.properties.getDatabase());
        }
        if (this.properties.getDatabasePlatform() != null) {
            adapter.setDatabasePlatform(this.properties.getDatabasePlatform());
        }
        adapter.setGenerateDdl(this.properties.isGenerateDdl());
        return adapter;
    }

    protected AbstractJpaVendorAdapter createJpaVendorAdapter() {
        return new HibernateJpaVendorAdapter();
    }

    public LocalContainerEntityManagerFactoryBean entityManagerFactory(EntityManagerFactoryBuilder factoryBuilder,
                                                                       ConfigurableListableBeanFactory beanFactory,
                                                                       String packagesToScan, String persistenceUnit) {
        Map<String, Object> vendorProperties = getVendorProperties();
        customizeVendorProperties(vendorProperties);
        return factoryBuilder.dataSource(this.dataSource)
                .properties(vendorProperties)
                .mappingResources(getMappingResources())
                .jta(isJta())
                .packages(packagesToScan)
                .persistenceUnit(persistenceUnit)
                .properties(Map.of(AvailableSettings.BEAN_CONTAINER, new SpringBeanContainer(beanFactory)))
                .build();
    }

    protected Map<String, Object> getVendorProperties() {
        HibernateSettings settings = new HibernateSettings();

        return this.hibernateProperties.determineHibernateProperties(this.properties.getProperties(), settings);
    }
    /**
     * Customize vendor properties before they are used. Allows for post-processing (for
     * example to configure JTA specific settings).
     * @param vendorProperties the vendor properties to customize
     */
    protected void customizeVendorProperties(Map<String, Object> vendorProperties) {
    }

    protected final JpaProperties getProperties() {
        return this.properties;
    }

    protected final boolean isJta() {
        return false;
    }

    private String[] getMappingResources() {
        List<String> mappingResources = this.properties.getMappingResources();
        return (!ObjectUtils.isEmpty(mappingResources) ? StringUtils.toStringArray(mappingResources) : null);
    }
}
