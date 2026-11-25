package cl.eventpass.lambda;

import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Ejecuta la MISMA funcion del handler en tu maquina: hace long-polling de la cola (ElasticMQ local)
 * y entrega los mensajes al handler, imitando el "event source mapping" que AWS hace en la nube.
 * Solo para desarrollo/demo local; en AWS la invoca Lambda directamente.
 */
public class LocalRunner {

    public static void main(String[] args) throws Exception {
        String queueUrl = requerida("SQS_QUEUE_URL");
        String endpoint = System.getenv().getOrDefault("SQS_ENDPOINT", "");
        String region = System.getenv().getOrDefault("AWS_REGION", "us-east-1");

        SqsClientBuilder builder = SqsClient.builder().region(Region.of(region));
        if (!endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }
        EmisionTicketsHandler handler = new EmisionTicketsHandler();

        System.out.println("LocalRunner escuchando " + queueUrl);
        try (SqsClient sqs = builder.build()) {
            while (true) {
                try {
                    List<Message> mensajes = sqs.receiveMessage(ReceiveMessageRequest.builder()
                            .queueUrl(queueUrl).maxNumberOfMessages(5).waitTimeSeconds(10).build()).messages();
                    if (mensajes.isEmpty()) {
                        continue;
                    }
                    List<SQSEvent.SQSMessage> registros = new ArrayList<>();
                    for (Message m : mensajes) {
                        SQSEvent.SQSMessage registro = new SQSEvent.SQSMessage();
                        registro.setMessageId(m.messageId());
                        registro.setBody(m.body());
                        registros.add(registro);
                    }
                    SQSEvent evento = new SQSEvent();
                    evento.setRecords(registros);

                    SQSBatchResponse respuesta = handler.handleRequest(evento, null);
                    Set<String> fallidos = new HashSet<>();
                    respuesta.getBatchItemFailures().forEach(f -> fallidos.add(f.getItemIdentifier()));

                    for (Message m : mensajes) {
                        if (!fallidos.contains(m.messageId())) {
                            sqs.deleteMessage(DeleteMessageRequest.builder()
                                    .queueUrl(queueUrl).receiptHandle(m.receiptHandle()).build());
                        }
                    }
                } catch (RuntimeException e) {
                    System.err.println("Error consultando la cola: " + e.getMessage());
                    Thread.sleep(2000);
                }
            }
        }
    }

    private static String requerida(String nombre) {
        String valor = System.getenv(nombre);
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException("Falta la variable de entorno " + nombre);
        }
        return valor;
    }
}
