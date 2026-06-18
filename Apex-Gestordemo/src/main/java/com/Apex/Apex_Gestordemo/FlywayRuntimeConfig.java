package com.Apex.Apex_Gestordemo;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class FlywayRuntimeConfig {

    @Bean
    @ConditionalOnProperty(prefix = "spring.flyway", name = "enabled", havingValue = "true", matchIfMissing = true)
    static BeanFactoryPostProcessor flywayBeforeJpa() {
        return beanFactory -> {
            if (!beanFactory.containsBeanDefinition("entityManagerFactory")) {
                return;
            }
            BeanDefinition entityManagerFactory = beanFactory.getBeanDefinition("entityManagerFactory");
            String[] currentDependsOn = entityManagerFactory.getDependsOn();
            if (currentDependsOn == null || currentDependsOn.length == 0) {
                entityManagerFactory.setDependsOn("apexFlyway");
                return;
            }
            for (String dependency : currentDependsOn) {
                if ("apexFlyway".equals(dependency)) {
                    return;
                }
            }
            String[] merged = java.util.Arrays.copyOf(currentDependsOn, currentDependsOn.length + 1);
            merged[merged.length - 1] = "apexFlyway";
            entityManagerFactory.setDependsOn(merged);
        };
    }

    @Bean(initMethod = "migrate")
    @ConditionalOnProperty(prefix = "spring.flyway", name = "enabled", havingValue = "true", matchIfMissing = true)
    Flyway apexFlyway(DataSource dataSource, Environment environment) {
        String[] locations = environment
                .getProperty("spring.flyway.locations", "classpath:db/migration")
                .split("\\s*,\\s*");
        boolean baselineOnMigrate = environment
                .getProperty("spring.flyway.baseline-on-migrate", Boolean.class, Boolean.TRUE);
        boolean validateOnMigrate = environment
                .getProperty("spring.flyway.validate-on-migrate", Boolean.class, Boolean.TRUE);

        return Flyway.configure()
                .dataSource(dataSource)
                .locations(locations)
                .baselineOnMigrate(baselineOnMigrate)
                .validateOnMigrate(validateOnMigrate)
                .load();
    }
}
