package ec.dfgranda.crm.controller;

import ec.dfgranda.crm.domain.AccessAuditLog;
import ec.dfgranda.crm.domain.AuditResourceType;
import ec.dfgranda.crm.repository.AccessAuditLogRepository;
import ec.dfgranda.crm.support.ApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Criterio de aceptación: el vendedor ve lo suyo, el gerente ve todo. */
class RoleVisibilityIntegrationTest extends ApiTest {

    @Autowired
    private AccessAuditLogRepository auditLog;

    @Test
    @DisplayName("el vendedor solo lista sus empresas; el gerente ve las de todos")
    void companyListIsScopedByOwner() throws Exception {
        getAs(REP1_EMAIL, "/api/companies?size=100")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].owner.fullName", everyItem(is("Andrés Villacís"))));

        List<String> managerOwners = read(getAs(MANAGER_EMAIL, "/api/companies?size=100"), "$.content[*].owner.fullName");
        assertThat(managerOwners).contains("Andrés Villacís", "Lucía Cevallos");
    }

    @Test
    @DisplayName("acceder a la empresa de otro vendedor responde 404 y queda en el log como acceso denegado")
    void foreignCompanyIsHiddenAndAudited() throws Exception {
        long foreignCompany = createCompany(REP2_EMAIL, "Empresa de Lucía S.A.");

        getAs(REP1_EMAIL, "/api/companies/{id}", foreignCompany).andExpect(status().isNotFound());
        getAs(REP2_EMAIL, "/api/companies/{id}", foreignCompany).andExpect(status().isOk());
        getAs(MANAGER_EMAIL, "/api/companies/{id}", foreignCompany).andExpect(status().isOk());

        List<AccessAuditLog> entries = auditLog.findByResourceTypeAndResourceIdOrderByOccurredAtAsc(
                AuditResourceType.COMPANY, foreignCompany);
        assertThat(entries).anySatisfy(entry -> assertThat(entry.isSuccess()).isFalse());
    }

    @Test
    @DisplayName("contactos, oportunidades y cotizaciones de otro vendedor tampoco son accesibles")
    void foreignRecordsAreNotAccessible() throws Exception {
        long company = createCompany(REP2_EMAIL, "Cliente de Lucía Cía. Ltda.");
        long contact = createContact(REP2_EMAIL, company, "Rosa");
        long opportunity = readId(postAs(REP2_EMAIL, "/api/opportunities", """
                {"title":"Proyecto privado","companyId":%d,"amount":1000.00,"expectedCloseDate":"2030-01-15"}
                """.formatted(company)).andExpect(status().isCreated()));

        getAs(REP1_EMAIL, "/api/contacts/{id}", contact).andExpect(status().isNotFound());
        getAs(REP1_EMAIL, "/api/opportunities/{id}", opportunity).andExpect(status().isNotFound());
        patchAs(REP1_EMAIL, "/api/opportunities/{id}/stage", "{\"stageCode\":\"QUALIFICATION\",\"version\":0}", opportunity)
                .andExpect(status().isNotFound());
        getAs(REP1_EMAIL, "/api/contacts?companyId={id}", company).andExpect(status().isNotFound());
        getAs(MANAGER_EMAIL, "/api/contacts/{id}", contact).andExpect(status().isOk());

        assertThat(auditLog.findByResourceTypeAndResourceIdOrderByOccurredAtAsc(AuditResourceType.CONTACT, contact))
                .anySatisfy(entry -> assertThat(entry.isSuccess()).isFalse());
    }

    @Test
    @DisplayName("el tablero y el forecast del vendedor solo incluyen sus oportunidades aunque pida las de otro")
    void boardAndForecastAreScoped() throws Exception {
        ResultActions board = getAs(REP1_EMAIL, "/api/opportunities/board").andExpect(status().isOk());
        List<String> owners = read(board, "$[*].opportunities[*].owner.fullName");
        assertThat(owners).isNotEmpty().containsOnly("Andrés Villacís");

        long rep2Id = ((Number) read(getAs(REP2_EMAIL, "/api/auth/me"), "$.id")).longValue();
        getAs(REP1_EMAIL, "/api/forecast?from=2020-01-01&to=2031-12-31&ownerId={id}", rep2Id)
                .andExpect(status().is(422));
        ResultActions forecast = getAs(REP1_EMAIL, "/api/forecast?from=2026-01-01&to=2027-12-31&ownerId={id}", rep2Id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.byOwner").isEmpty());
        long rep1Id = ((Number) read(getAs(REP1_EMAIL, "/api/auth/me"), "$.id")).longValue();
        assertThat(((Number) read(forecast, "$.ownerId")).longValue()).isEqualTo(rep1Id);

        List<String> managerOwners = read(getAs(MANAGER_EMAIL, "/api/forecast?from=2026-01-01&to=2027-12-31"),
                "$.byOwner[*].ownerName");
        assertThat(managerOwners).contains("Andrés Villacís", "Lucía Cevallos");
    }

    @Test
    @DisplayName("funciones de privacidad y catálogo de productos restringidas por rol (BFLA)")
    void functionLevelAuthorization() throws Exception {
        long company = createCompany(REP1_EMAIL, "Empresa BFLA S.A.");
        long contact = createContact(REP1_EMAIL, company, "Tomas");
        String dsr = "{\"channel\":\"EMAIL\",\"notes\":\"Solicitud de prueba\"}";

        postAs(REP1_EMAIL, "/api/contacts/{id}/export", dsr, contact).andExpect(status().isForbidden());
        postAs(REP1_EMAIL, "/api/contacts/{id}/erasure", dsr, contact).andExpect(status().isForbidden());
        getAs(REP1_EMAIL, "/api/privacy/access-log").andExpect(status().isForbidden());
        getAs(REP1_EMAIL, "/api/privacy/requests").andExpect(status().isForbidden());
        postAs(REP1_EMAIL, "/api/products", """
                {"sku":"X-1","name":"X","unitPrice":1.00,"vatRateCode":"4","active":true}
                """).andExpect(status().isForbidden());
        getAs(REP1_EMAIL, "/api/products").andExpect(status().isOk());
        deleteAs(REP1_EMAIL, "/api/companies/{id}", company).andExpect(status().isForbidden());

        getAs(MANAGER_EMAIL, "/api/privacy/access-log?resourceType=CONTACT&resourceId={id}", contact)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].action").exists());
    }
}
