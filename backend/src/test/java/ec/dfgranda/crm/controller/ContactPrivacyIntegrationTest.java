package ec.dfgranda.crm.controller;

import ec.dfgranda.crm.domain.AccessAuditLog;
import ec.dfgranda.crm.domain.AuditAction;
import ec.dfgranda.crm.domain.AuditResourceType;
import ec.dfgranda.crm.repository.AccessAuditLogRepository;
import ec.dfgranda.crm.support.ApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Registro de consentimiento, log de accesos y derechos del titular (LOPDP). */
class ContactPrivacyIntegrationTest extends ApiTest {

    @Autowired
    private AccessAuditLogRepository auditLog;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("no se puede registrar un contacto sin consentimiento inicial demostrable")
    void contactRequiresInitialConsent() throws Exception {
        long company = createCompany(REP1_EMAIL, "Consentimiento S.A.");
        postAs(REP1_EMAIL, "/api/contacts", """
                {"companyId":%d,"firstName":"Sin","lastName":"Consentimiento","dataSource":"DIRECT"}
                """.formatted(company))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.initialConsent").exists());
        postAs(REP1_EMAIL, "/api/contacts", """
                {"companyId":%d,"firstName":"Evidencia","lastName":"Corta","dataSource":"DIRECT",
                 "initialConsent":{"purposeCode":"COMMERCIAL_CONTACT","channel":"EMAIL","evidence":"ok"}}
                """.formatted(company))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['errors']['initialConsent.evidence']").exists());
    }

    @Test
    @DisplayName("la cédula se valida, se cifra y la API solo la devuelve enmascarada; cada acceso queda registrado")
    void nationalIdIsMaskedAndAccessIsLogged() throws Exception {
        long company = createCompany(REP1_EMAIL, "Cedula S.A.");
        String withInvalidId = """
                {"companyId":%d,"firstName":"Luis","lastName":"Mena","dataSource":"DIRECT","nationalId":"2059999992",
                 "initialConsent":{"purposeCode":"COMMERCIAL_CONTACT","channel":"IN_PERSON","evidence":"Formulario firmado de prueba"}}
                """.formatted(company);
        postAs(REP1_EMAIL, "/api/contacts", withInvalidId).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.nationalId").value("El dígito verificador de la cédula no es correcto."));

        long contact = readId(postAs(REP1_EMAIL, "/api/contacts", withInvalidId.replace("2059999992", "2059999991"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nationalIdMasked").value("******9991"))
                .andExpect(jsonPath("$.consents[0].purposeCode").value("COMMERCIAL_CONTACT"))
                .andExpect(jsonPath("$.consents[0].active").value(true)));

        getAs(REP1_EMAIL, "/api/contacts/{id}", contact).andExpect(status().isOk())
                .andExpect(jsonPath("$.nationalId").doesNotExist());

        String stored = jdbc.queryForObject("select national_id_encrypted from contacts where id = ?", String.class, contact);
        assertThat(stored).doesNotContain("2059999991");
        List<AuditAction> actions = auditLog.findByResourceTypeAndResourceIdOrderByOccurredAtAsc(AuditResourceType.CONTACT, contact)
                .stream().map(AccessAuditLog::getAction).toList();
        assertThat(actions).contains(AuditAction.CREATE, AuditAction.CONSENT_GRANT, AuditAction.VIEW);
    }

    @Test
    @DisplayName("sin consentimiento vigente no se puede agendar una llamada al contacto; revocar no borra la evidencia")
    void revokedConsentBlocksDirectContact() throws Exception {
        long company = createCompany(REP1_EMAIL, "Revocatoria S.A.");
        long contact = createContact(REP1_EMAIL, company, "Marta");
        long consentId = ((Number) read(getAs(REP1_EMAIL, "/api/contacts/{id}", contact), "$.consents[0].id")).longValue();
        String call = """
                {"type":"CALL","subject":"Llamada de seguimiento","companyId":%d,"contactId":%d,"dueAt":"2030-01-10T15:00:00Z"}
                """.formatted(company, contact);
        postAs(REP1_EMAIL, "/api/activities", call).andExpect(status().isCreated());

        postAs(REP1_EMAIL, "/api/contacts/{id}/consents/{consentId}/revoke", "{\"reason\":\"\"}", contact, consentId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.revokedAt").exists());

        postAs(REP1_EMAIL, "/api/activities", call)
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.detail", containsString("no tiene un consentimiento vigente")));
        // Una tarea interna sobre la empresa no implica contactar a la persona.
        postAs(REP1_EMAIL, "/api/activities", call.replace("\"CALL\"", "\"TASK\"")).andExpect(status().isCreated());
        getAs(REP1_EMAIL, "/api/contacts/{id}", contact)
                .andExpect(jsonPath("$.consents[0].revokedAt").exists());
    }

    @Test
    @DisplayName("un segundo consentimiento vigente para la misma finalidad responde 409")
    void duplicateActiveConsentConflicts() throws Exception {
        long company = createCompany(REP1_EMAIL, "Duplicado S.A.");
        long contact = createContact(REP1_EMAIL, company, "Irene");
        String consent = "{\"purposeCode\":\"COMMERCIAL_CONTACT\",\"channel\":\"EMAIL\",\"evidence\":\"Correo de confirmación archivado\"}";
        postAs(REP1_EMAIL, "/api/contacts/{id}/consents", consent, contact).andExpect(status().isConflict());
        postAs(REP1_EMAIL, "/api/contacts/{id}/consents", consent.replace("COMMERCIAL_CONTACT", "EMAIL_MARKETING"), contact)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.legalBasis").value("CONSENT"));
    }

    @Test
    @DisplayName("derecho de acceso: el gerente exporta los datos en JSON y la solicitud queda registrada")
    void exportsPersonalDataAndRegistersRequest() throws Exception {
        long company = createCompany(REP1_EMAIL, "Exportación S.A.");
        long contact = createContact(REP1_EMAIL, company, "Julia");
        putAs(REP1_EMAIL, "/api/contacts/{id}", """
                {"firstName":"Julia","lastName":"Prueba","email":"julia.prueba@example.com","nationalId":"3059999981","dataSource":"EVENT"}
                """, contact).andExpect(status().isOk());

        postAs(MANAGER_EMAIL, "/api/contacts/{id}/export", "{\"channel\":\"EMAIL\",\"notes\":\"Pedido por correo\"}", contact)
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("contacto-" + contact + "-datos.json")))
                .andExpect(jsonPath("$.format").value("crm-ventas/contact-export@1"))
                .andExpect(jsonPath("$.personalData.nationalId").value("3059999981"))
                .andExpect(jsonPath("$.consents[0].purposeCode").value("COMMERCIAL_CONTACT"))
                .andExpect(jsonPath("$.purposes.length()").value(3))
                .andExpect(jsonPath("$.controller.legalName").exists());

        getAs(MANAGER_EMAIL, "/api/privacy/requests?size=100")
                .andExpect(jsonPath("$.content[?(@.contactId == " + contact + ")].requestType").value("PORTABILITY"))
                .andExpect(jsonPath("$.content[?(@.contactId == " + contact + ")].status").value("COMPLETED"));
        assertThat(auditLog.findByResourceTypeAndResourceIdOrderByOccurredAtAsc(AuditResourceType.CONTACT, contact))
                .extracting(AccessAuditLog::getAction).contains(AuditAction.UPDATE, AuditAction.EXPORT);
    }

    @Test
    @DisplayName("derecho de eliminación: borra al titular, anonimiza actividades y conserva solo IDs en los registros")
    void erasesContactAndAnonymizesReferences() throws Exception {
        long company = createCompany(REP1_EMAIL, "Eliminación S.A.");
        long contact = createContact(REP1_EMAIL, company, "Pedro");
        long opportunity = readId(postAs(REP1_EMAIL, "/api/opportunities", """
                {"title":"Venta con Pedro","companyId":%d,"primaryContactId":%d,"amount":500.00,"expectedCloseDate":"2030-03-01"}
                """.formatted(company, contact)).andExpect(status().isCreated()));
        long activity = readId(postAs(REP1_EMAIL, "/api/activities", """
                {"type":"MEETING","subject":"Reunión con Pedro","notes":"Pedro prefiere las mañanas","opportunityId":%d,"contactId":%d,
                 "dueAt":"2030-02-01T15:00:00Z"}
                """.formatted(opportunity, contact)).andExpect(status().isCreated()));

        ResultActions erasure = postAs(MANAGER_EMAIL, "/api/contacts/{id}/erasure", "{\"channel\":\"LETTER\"}", contact)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.request.requestType").value("ERASURE"))
                .andExpect(jsonPath("$.request.status").value("COMPLETED"))
                .andExpect(jsonPath("$.activitiesAnonymized").value(1))
                .andExpect(jsonPath("$.opportunitiesDetached").value(1))
                .andExpect(jsonPath("$.consentsDeleted").value(1));
        assertThat((String) read(erasure, "$.request.dueAt")).isNotBlank();

        getAs(MANAGER_EMAIL, "/api/contacts/{id}", contact).andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("select count(*) from contact_consents where contact_id = ?", Integer.class, contact)).isZero();
        assertThat(jdbc.queryForMap("select subject, notes, contact_id from activities where id = ?", activity))
                .containsEntry("subject", "[Contacto eliminado] MEETING")
                .containsEntry("notes", null)
                .containsEntry("contact_id", null);
        getAs(REP1_EMAIL, "/api/opportunities/{id}", opportunity).andExpect(jsonPath("$.primaryContactId").doesNotExist());
        // El log de accesos sobrevive a la eliminación y sigue siendo inmutable.
        assertThat(auditLog.findByResourceTypeAndResourceIdOrderByOccurredAtAsc(AuditResourceType.CONTACT, contact))
                .extracting(AccessAuditLog::getAction).contains(AuditAction.CREATE, AuditAction.ERASE);
    }
}
