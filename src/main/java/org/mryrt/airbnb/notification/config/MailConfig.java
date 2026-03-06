package org.mryrt.airbnb.notification.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * Явное объявление бина JavaMailSender при наличии spring.mail.host.
 * Spring Boot создаёт этот бин через MailSenderAutoConfiguration только когда
 * подставлены свойства (в т.ч. spring.mail.host). При инспекции зависимостей IDE
 * часто не подгружает application-*.properties, поэтому «не видит» автоконфиг и
 * ругается на отсутствие бина. Этот класс даёт определение бина в нашем коде.
 */
@Configuration
@ConditionalOnProperty(name = "spring.mail.host")
public class MailConfig {

    @Bean
    public JavaMailSender javaMailSender(org.springframework.boot.autoconfigure.mail.MailProperties mailProperties) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(mailProperties.getHost());
        if (mailProperties.getPort() != null) {
            sender.setPort(mailProperties.getPort());
        }
        sender.setUsername(mailProperties.getUsername());
        sender.setPassword(mailProperties.getPassword());
        sender.setProtocol(mailProperties.getProtocol() != null ? mailProperties.getProtocol() : "smtp");
        if (mailProperties.getDefaultEncoding() != null) {
            sender.setDefaultEncoding(mailProperties.getDefaultEncoding().name());
        }
        if (mailProperties.getProperties() != null && !mailProperties.getProperties().isEmpty()) {
            sender.setJavaMailProperties(asProperties(mailProperties.getProperties()));
        }
        return sender;
    }

    private static Properties asProperties(java.util.Map<String, String> map) {
        Properties props = new Properties();
        props.putAll(map);
        return props;
    }
}
