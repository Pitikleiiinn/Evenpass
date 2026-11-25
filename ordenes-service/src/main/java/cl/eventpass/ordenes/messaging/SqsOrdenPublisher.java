package cl.eventpass.ordenes.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

/** Productor: encola la orden aceptada en Amazon SQS para su emision asincrona. */
@Component
public class SqsOrdenPublisher implements OrdenPublisher {

    private final SqsClient sqs;
    private final ObjectMapper mapper;
    private final String queueUrl;

    public SqsOrdenPublisher(SqsClient sqs, ObjectMapper mapper,
                             @Value("${eventpass.sqs.queue-url}") String queueUrl) {
        this.sqs = sqs;
        this.mapper = mapper;
        this.queueUrl = queueUrl;
    }

    @Override
    public void publicar(OrdenMensaje mensaje) {
        try {
            sqs.sendMessage(SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(mapper.writeValueAsString(mensaje))
                    .build());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar la orden " + mensaje.ordenId(), e);
        }
    }
}
