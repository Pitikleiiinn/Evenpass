package cl.eventpass.ordenes.messaging;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;

import java.net.URI;

@Configuration
public class SqsConfig {

    /**
     * Credenciales: cadena por defecto del SDK (rol IAM de la instancia EC2 en AWS;
     * variables AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY en local con ElasticMQ).
     */
    @Bean
    SqsClient sqsClient(@Value("${eventpass.sqs.region}") String region,
                        @Value("${eventpass.sqs.endpoint:}") String endpoint) {
        SqsClientBuilder builder = SqsClient.builder().region(Region.of(region));
        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }
        return builder.build();
    }
}
