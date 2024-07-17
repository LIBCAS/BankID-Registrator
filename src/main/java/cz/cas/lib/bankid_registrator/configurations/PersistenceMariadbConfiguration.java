/*
 * Copyright (C) 2022 Academy of Sciences Library
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package cz.cas.lib.bankid_registrator.configurations;

import com.zaxxer.hikari.HikariDataSource;
import java.util.Properties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.core.env.Environment;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@PropertySource({"classpath:config.properties"})
@EnableJpaRepositories(
    basePackages = "cz.cas.lib.bankid_registrator.dao.mariadb",
    entityManagerFactoryRef = "mariadbEntityManager",
    transactionManagerRef = "mariadbTransactionManager"
)
public class PersistenceMariadbConfiguration extends ConfigurationAbstract
{
    @Autowired
    private Environment env;

    public PersistenceMariadbConfiguration() {
        super();
    }

    @Primary
    @Bean
    public DataSourceProperties hikariDataSourceProperties() {

        final DataSourceProperties dataSourceProperties = new DataSourceProperties();

        dataSourceProperties.setDriverClassName(this.env.getProperty("spring.datasource.driver-class-name"));
        dataSourceProperties.setUrl(this.env.getProperty("spring.datasource.jdbc-url"));
        dataSourceProperties.setUsername(this.env.getProperty("spring.datasource.username"));
        dataSourceProperties.setPassword(this.env.getProperty("spring.datasource.password"));

        return dataSourceProperties;
    }

    @Bean
    @Primary
    public HikariDataSource patronDataSource() {

        final HikariDataSource hikariDataSource = hikariDataSourceProperties()
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();

        hikariDataSource.setMaximumPoolSize(Integer.valueOf(this.env.getProperty("spring.datasource.hikari.maximumPoolSize")));
        hikariDataSource.setMinimumIdle(Integer.valueOf(this.env.getProperty("spring.datasource.hikari.minimumIdle")));
        hikariDataSource.setIdleTimeout(Long.valueOf(this.env.getProperty("spring.datasource.hikari.idleTimeout")));
        hikariDataSource.setMaxLifetime(Long.valueOf(this.env.getProperty("spring.datasource.hikari.maxLifetime")));
        hikariDataSource.setConnectionTimeout(Long.valueOf(this.env.getProperty("spring.datasource.hikari.connectionTimeout")));

        return hikariDataSource;

    }

    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean mariadbEntityManager () {

        final Properties jpaProperties = new Properties();

        jpaProperties.setProperty("hibernate.hbm2ddl.auto", this.env.getProperty("spring.jpa.hibernate.ddl-auto"));
        jpaProperties.setProperty("hibernate.dialect", this.env.getProperty("spring.jpa.hibernate.dialect"));

        final HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();

        final LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();

        factory.setDataSource(patronDataSource());
        factory.setPackagesToScan("cz.cas.lib.bankid_registrator.model", "cz.cas.lib.bankid_registrator.dto");
        factory.setJpaVendorAdapter(vendorAdapter);
        factory.setJpaProperties(jpaProperties);
        factory.afterPropertiesSet();

        return factory;
    }

    @Bean
    @Primary
    public PlatformTransactionManager mariadbTransactionManager() {

        final JpaTransactionManager txManager = new JpaTransactionManager();

        txManager.setEntityManagerFactory(mariadbEntityManager().getObject());

        return txManager;
    }
}