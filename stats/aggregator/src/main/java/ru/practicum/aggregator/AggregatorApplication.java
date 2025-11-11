package ru.practicum.aggregator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.ConfigurableApplicationContext;
import ru.practicum.aggregator.service.AggregatorStarter;

@EnableDiscoveryClient
@SpringBootApplication
@ConfigurationPropertiesScan
public class AggregatorApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(AggregatorApplication.class, args);
        AggregatorStarter aggregator = context.getBean(AggregatorStarter.class);
        aggregator.run();
    }
}