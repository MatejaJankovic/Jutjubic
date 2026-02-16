package rs.ftn.isa.jutjubicbackend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class RabbitMQConfig {

    public static final String TRANSCODING_EXCHANGE = "video.transcoding.exchange";
    public static final String TRANSCODING_QUEUE = "video.transcoding.queue";
    public static final String TRANSCODING_ROUTING_KEY = "video.transcoding";

    @Value("${spring.rabbitmq.host:localhost}")
    private String rabbitmqHost;

    @Value("${spring.rabbitmq.port:5672}")
    private int rabbitmqPort;

    @Value("${spring.rabbitmq.username:guest}")
    private String rabbitmqUsername;

    /**
     * Declare the direct exchange for video transcoding
     */
    @Bean
    public DirectExchange transcodingExchange() {
        log.info("✓ Declaring DirectExchange bean: {}", TRANSCODING_EXCHANGE);
        return new DirectExchange(TRANSCODING_EXCHANGE, true, false);
    }

    /**
     * Declare the queue for video transcoding jobs
     */
    @Bean
    public Queue transcodingQueue() {
        log.info("✓ Declaring Queue bean: {}", TRANSCODING_QUEUE);
        return new Queue(TRANSCODING_QUEUE, true);
    }

    /**
     * Bind the queue to the exchange with routing key
     */
    @Bean
    public Binding transcodingBinding(Queue transcodingQueue, DirectExchange transcodingExchange) {
        log.info("✓ Declaring Binding bean: {} → {} with routing key '{}'",
                TRANSCODING_QUEUE, TRANSCODING_EXCHANGE, TRANSCODING_ROUTING_KEY);
        return BindingBuilder
                .bind(transcodingQueue)
                .to(transcodingExchange)
                .with(TRANSCODING_ROUTING_KEY);
    }

    /**
     * Configure Jackson2JsonMessageConverter for JSON serialization/deserialization
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        log.info("✓ Configuring Jackson2JsonMessageConverter");
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Configure RabbitTemplate with JSON message converter
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                        MessageConverter jsonMessageConverter) {
        log.info("✓ Configuring RabbitTemplate with JSON message converter");
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter);
        return rabbitTemplate;
    }

    /**
     * RabbitAdmin - CRITICAL for auto-declaring exchanges, queues, and bindings
     * Without this, Spring Boot will NOT declare your infrastructure!
     */
    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        log.info("✓ Creating RabbitAdmin with autoStartup=true");
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        rabbitAdmin.setAutoStartup(true);
        return rabbitAdmin;
    }

    /**
     * Connectivity check and forced declaration on startup
     * This ensures we fail fast if RabbitMQ is not accessible
     */
    @Bean
    public ApplicationRunner rabbitMQStartupVerifier(
            ConnectionFactory connectionFactory,
            RabbitAdmin rabbitAdmin,
            DirectExchange transcodingExchange,
            Queue transcodingQueue,
            Binding transcodingBinding) {

        return args -> {
            log.info("========================================");
            log.info("RabbitMQ Configuration Verification");
            log.info("========================================");
            log.info("Host: {}", rabbitmqHost);
            log.info("Port: {}", rabbitmqPort);
            log.info("Username: {}", rabbitmqUsername);
            log.info("Virtual Host: /");
            log.info("----------------------------------------");

            try {
                // Test connectivity by executing a simple operation
                connectionFactory.createConnection().createChannel(false).close();
                log.info(" Successfully connected to RabbitMQ at {}:{}", rabbitmqHost, rabbitmqPort);
            } catch (Exception e) {
                String errorMsg = String.format(
                    "FATAL: Cannot connect to RabbitMQ at %s:%d. " +
                    "Check that RabbitMQ service is running, credentials are correct, and firewall allows connections.",
                    rabbitmqHost, rabbitmqPort
                );
                log.error(errorMsg, e);
                throw new IllegalStateException(errorMsg, e);
            }

            // Force declaration of infrastructure
            log.info(" Forcing declaration of RabbitMQ infrastructure...");

            try {
                rabbitAdmin.declareExchange(transcodingExchange);
                log.info(" Exchange declared: {} (type: {}, durable: {})",
                        TRANSCODING_EXCHANGE,
                        transcodingExchange.getType(),
                        transcodingExchange.isDurable());

                rabbitAdmin.declareQueue(transcodingQueue);
                log.info("Queue declared: {} (durable: {})",
                        TRANSCODING_QUEUE,
                        transcodingQueue.isDurable());

                rabbitAdmin.declareBinding(transcodingBinding);
                log.info("Binding declared: {} → {} [routing key: '{}']",
                        TRANSCODING_QUEUE,
                        TRANSCODING_EXCHANGE,
                        TRANSCODING_ROUTING_KEY);

                log.info("========================================");
                log.info("RabbitMQ infrastructure successfully declared!");
                log.info("   View at: http://localhost:15672");
                log.info("========================================");

            } catch (Exception e) {
                log.error("FATAL: Failed to declare RabbitMQ infrastructure", e);
                throw new IllegalStateException("Failed to declare RabbitMQ infrastructure", e);
            }
        };
    }
}

