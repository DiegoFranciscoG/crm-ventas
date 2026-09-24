package ec.dfgranda.crm.service.mail;

import ec.dfgranda.crm.config.MailProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class EmailServiceTest {

    private static final EmailMessage TO_DEMO = new EmailMessage("vendedor1@example.com", "Recordatorio", "Texto");
    private static final EmailMessage TO_REAL = new EmailMessage("persona@empresa.ec", "Recordatorio", "Texto");

    private static MailProperties properties(MailProperties.Provider provider) {
        return new MailProperties(provider, "no-reply@crm.ec", "CRM Ventas", provider == MailProperties.Provider.BREVO ? "clave-api" : null,
                "https://api.brevo.com/v3/smtp/email", "http://localhost:4200");
    }

    @ParameterizedTest
    @ValueSource(strings = {"a@example.com", "a@ventas.example.org", "a@demo.test", "a@x.invalid", "a@EXAMPLE.NET"})
    void detectsRfc2606ReservedDomains(String address) {
        assertThat(EmailService.isReserved(address)).isTrue();
    }

    @Test
    void doesNotFlagRealDomains() {
        assertThat(EmailService.isReserved("persona@empresa.ec")).isFalse();
        assertThat(EmailService.isReserved("persona@notexample.com")).isFalse();
    }

    @Test
    void realProviderNeverSendsToReservedDomains() {
        EmailSender sender = mock(EmailSender.class);
        EmailService service = new EmailService(sender, MailProperties.Provider.BREVO);

        assertThat(service.send(TO_DEMO)).isFalse();
        verify(sender, never()).send(any());
        assertThat(service.send(TO_REAL)).isTrue();
        verify(sender).send(TO_REAL);
    }

    @Test
    void smtpSendsEverythingBecauseMailpitCapturesIt() {
        JavaMailSender javaMailSender = mock(JavaMailSender.class);
        EmailService service = new EmailService(new SmtpEmailSender(javaMailSender, properties(MailProperties.Provider.SMTP)),
                MailProperties.Provider.SMTP);

        assertThat(service.send(TO_DEMO)).isTrue();
        verify(javaMailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void brevoSenderCallsTheTransactionalApiWithTheKeyInAHeader() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.brevo.com/v3/smtp/email"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("api-key", "clave-api"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {"sender":{"name":"CRM Ventas","email":"no-reply@crm.ec"},
                         "to":[{"email":"persona@empresa.ec"}],"subject":"Recordatorio","textContent":"Texto"}
                        """))
                .andRespond(withSuccess("{\"messageId\":\"1\"}", MediaType.APPLICATION_JSON));

        new BrevoEmailSender(builder.build(), properties(MailProperties.Provider.BREVO)).send(TO_REAL);

        server.verify();
    }

    @Test
    void loggingSenderDoesNotFail() {
        new LoggingEmailSender().send(TO_REAL);
    }
}
