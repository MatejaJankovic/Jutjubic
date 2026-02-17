package rs.ftn.isa.jutjubicbackend.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String JSON_QUEUE = "upload-json-queue";
    public static final String PROTO_QUEUE = "upload-protobuf-queue";

    @Bean
    public Queue jsonQueue() {
        return new Queue(JSON_QUEUE, true);
    }

    @Bean
    public Queue protoQueue() {
        return new Queue(PROTO_QUEUE, true);
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }
}
