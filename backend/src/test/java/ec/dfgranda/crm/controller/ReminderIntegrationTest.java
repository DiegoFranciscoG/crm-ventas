package ec.dfgranda.crm.controller;

import ec.dfgranda.crm.support.ApiTest;
import ec.dfgranda.crm.support.TestSecrets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReminderIntegrationTest extends ApiTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("el cron externo necesita el token correcto y cada recordatorio se envía una sola vez")
    void dispatchesDueRemindersOnce() throws Exception {
        long company = createCompany(REP1_EMAIL, "Recordatorios S.A.");
        Instant due = Instant.now().plus(1, ChronoUnit.MINUTES);
        Instant remind = Instant.now().minus(1, ChronoUnit.MINUTES);
        long activity = readId(postAs(REP1_EMAIL, "/api/activities", """
                {"type":"TASK","subject":"Preparar propuesta","companyId":%d,"dueAt":"%s","remindAt":"%s"}
                """.formatted(company, due, remind)).andExpect(status().isCreated()));

        mvc.perform(post("/api/internal/reminders/dispatch")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/internal/reminders/dispatch").header("X-Cron-Token", "token-incorrecto"))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/internal/reminders/dispatch").header("X-Cron-Token", TestSecrets.CRON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").isNumber());
        assertThat(jdbc.queryForObject("select reminder_sent_at from activities where id = ?", Instant.class, activity))
                .isNotNull();

        Integer processedAgain = com.jayway.jsonpath.JsonPath.read(
                mvc.perform(post("/api/internal/reminders/dispatch").header("X-Cron-Token", TestSecrets.CRON))
                        .andReturn().getResponse().getContentAsString(), "$.processed");
        assertThat(processedAgain).isZero();
    }

    @Test
    @DisplayName("un recordatorio posterior a la fecha de la actividad se rechaza")
    void rejectsReminderAfterDueDate() throws Exception {
        long company = createCompany(REP1_EMAIL, "Fechas S.A.");
        postAs(REP1_EMAIL, "/api/activities", """
                {"type":"TASK","subject":"Tarea","companyId":%d,"dueAt":"2030-01-01T10:00:00Z","remindAt":"2030-01-02T10:00:00Z"}
                """.formatted(company)).andExpect(status().is(422));
        postAs(REP1_EMAIL, "/api/activities", """
                {"type":"TASK","subject":"Tarea sin empresa"}
                """).andExpect(status().is(422));
    }
}
