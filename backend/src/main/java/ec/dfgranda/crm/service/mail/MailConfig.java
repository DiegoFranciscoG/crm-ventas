package ec.dfgranda.crm.service.mail;

import ec.dfgranda.crm.config.MailProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class MailConfig {

    @Bean
    EmailService emailService(MailProperties properties, ObjectProvider<JavaMailSender> javaMailSender) {
        EmailSender sender = switch (properties.provider()) {
            case SMTP -> new SmtpEmailSender(javaMailSender.getObject(), properties);
            case BREVO -> new BrevoEmailSender(brevoClient(), properties);
            case LOG -> new LoggingEmailSender();
        };
        return new EmailService(sender, properties.provider());
    }

    private static RestClient brevoClient() {
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(10));
        return RestClient.builder().requestFactory(factory).build();
    }
}
