package cz.cas.lib.bankid_registrator.configurations;

import java.util.HashMap;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@PropertySource({"classpath:config.properties"})
@EnableJpaRepositories(
    basePackages = "cz.cas.lib.bankid_registrator.dao.oracle",
    entityManagerFactoryRef = "oracleEntityManager",
    transactionManagerRef = "oracleTransactionManager"
)
public class PersistenceOracleConfiguration extends ConfigurationAbstract {
    @Autowired
    private Environment env;

    public PersistenceOracleConfiguration() {
        super();
    }

    @Bean
    public DataSource oracleDataSource() {
        final DriverManagerDataSource dataSource = new DriverManagerDataSource();

        dataSource.setDriverClassName(env.getProperty("spring.oracle-datasource.driver-class-name"));
        dataSource.setUrl(env.getProperty("spring.oracle-datasource.jdbc-url"));
        dataSource.setUsername(env.getProperty("spring.oracle-datasource.username"));
        dataSource.setPassword(env.getProperty("spring.oracle-datasource.password"));

        return dataSource;
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean oracleEntityManager() {
        final LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(oracleDataSource());
        em.setPackagesToScan("cz.cas.lib.bankid_registrator.model.oracle");

        final HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);

        final HashMap<String, Object> properties = new HashMap<>();
        properties.put("hibernate.hbm2ddl.auto", env.getProperty("spring.oracle-jpa.hibernate.ddl-auto"));
        properties.put("hibernate.dialect", env.getProperty("spring.oracle-jpa.hibernate.dialect"));
        em.setJpaPropertyMap(properties);

        return em;
    }

    @Bean
    public PlatformTransactionManager oracleTransactionManager() {
        final JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(oracleEntityManager().getObject());
        return transactionManager;
    }
}