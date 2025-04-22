package com.example.config;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class RabbitMQConfig {

    @Value("${crm.update.exchange.name}")
    private String crmUpdateExchangeName;

    private final QueueCrmUpdateProperties queueCrmUpdateProperties;

    @Bean
    public TopicExchange baseExchange() {
        return new TopicExchange(crmUpdateExchangeName);
    }

    @Bean
    public Queue crmCustomerUpdateQueue() {
        return new Queue(queueCrmUpdateProperties.getCustomer().getName(), true);
    }

    @Bean
    public Binding customerLatviaUpdateInitBinding() {
        return BindingBuilder
            .bind(crmCustomerUpdateQueue())
            .to(baseExchange())
            .with(queueCrmUpdateProperties.getPrefix() + "customer.init_registry.LVBIG.v1");
    }

    @Bean
    public Binding customerLatviaUpdateBinding() {
        return BindingBuilder
            .bind(crmCustomerUpdateQueue())
            .to(baseExchange())
            .with(queueCrmUpdateProperties.getPrefix() + "customer.LVBIG.v1");
    }

    @Bean
    public Queue crmCustomerRelationUpdateQueue() {
        return new Queue(queueCrmUpdateProperties.getCustomerRelation().getName(), true);
    }

    @Bean
    public Binding customerRelationUpdateBinding() {
        return BindingBuilder
            .bind(crmCustomerRelationUpdateQueue())
            .to(baseExchange())
            .with(queueCrmUpdateProperties.getPrefix() + "relation.v1");
    }

    @Bean
    public Binding customerRelationUpdateInitBinding() {
        return BindingBuilder
            .bind(crmCustomerRelationUpdateQueue())
            .to(baseExchange())
            .with(queueCrmUpdateProperties.getPrefix() + "relation.init_registry.v1");
    }

    @Bean
    public Queue crmIdDocumentUpdateQueue() {
        return new Queue(queueCrmUpdateProperties.getIdDocument().getName(), true);
    }

    @Bean
    public Binding idDocumentUpdateBinding() {
        return BindingBuilder
            .bind(crmIdDocumentUpdateQueue())
            .to(baseExchange())
            .with(queueCrmUpdateProperties.getPrefix() + "idDocument.v1");
    }

    @Bean
    public Binding idDocumentUpdateInitBinding() {
        return BindingBuilder
            .bind(crmIdDocumentUpdateQueue())
            .to(baseExchange())
            .with(queueCrmUpdateProperties.getPrefix() + "idDocument.init_registry.v1");
    }

    @Bean
    public Queue crmIdNumberUpdateQueue() {
        return new Queue(queueCrmUpdateProperties.getIdNumber().getName(), true);
    }

    @Bean
    public Binding idNumberUpdateBinding() {
        return BindingBuilder
            .bind(crmIdNumberUpdateQueue())
            .to(baseExchange())
            .with(queueCrmUpdateProperties.getPrefix() + "idNumber.v1");
    }

    @Bean
    public Binding idNumberUpdateInitBinding() {
        return BindingBuilder
            .bind(crmIdNumberUpdateQueue())
            .to(baseExchange())
            .with(queueCrmUpdateProperties.getPrefix() + "idNumber.init_registry.v1");
    }

    @Bean
    public Jackson2JsonMessageConverter converter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(converter());
        return rabbitTemplate;
    }
} 