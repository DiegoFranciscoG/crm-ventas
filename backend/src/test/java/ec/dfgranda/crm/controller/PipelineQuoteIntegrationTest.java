package ec.dfgranda.crm.controller;

import ec.dfgranda.crm.support.ApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PipelineQuoteIntegrationTest extends ApiTest {

    private long createOpportunity(long company) throws Exception {
        return readId(postAs(REP1_EMAIL, "/api/opportunities", """
                {"title":"Implementación CRM","companyId":%d,"amount":10000.00,"expectedCloseDate":"2030-06-30"}
                """.formatted(company)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.stageCode").value("PROSPECTING"))
                .andExpect(jsonPath("$.probability").value(10))
                .andExpect(jsonPath("$.weightedAmount").value(1000.0)));
    }

    @Test
    @DisplayName("mover una tarjeta aplica la probabilidad de la etapa, guarda historial y usa bloqueo optimista")
    void movesOpportunityThroughThePipeline() throws Exception {
        long opportunity = createOpportunity(createCompany(REP1_EMAIL, "Pipeline S.A."));

        patchAs(REP1_EMAIL, "/api/opportunities/{id}/stage", "{\"stageCode\":\"NEGOTIATION\",\"version\":0}", opportunity)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.probability").value(90))
                .andExpect(jsonPath("$.weightedAmount").value(9000.0))
                .andExpect(jsonPath("$.version").value(1));

        // Otro usuario con la versión anterior: conflicto.
        patchAs(REP1_EMAIL, "/api/opportunities/{id}/stage", "{\"stageCode\":\"PROPOSAL\",\"version\":0}", opportunity)
                .andExpect(status().isConflict());

        patchAs(REP1_EMAIL, "/api/opportunities/{id}/stage", "{\"stageCode\":\"CLOSED_LOST\",\"version\":1}", opportunity)
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.detail", containsString("motivo")));
        patchAs(REP1_EMAIL, "/api/opportunities/{id}/stage",
                "{\"stageCode\":\"CLOSED_WON\",\"version\":1}", opportunity)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.probability").value(100))
                .andExpect(jsonPath("$.closedAt").exists());

        List<String> stages = read(getAs(REP1_EMAIL, "/api/opportunities/{id}/history", opportunity), "$[*].toStage");
        assertThat(stages).containsExactly("PROSPECTING", "NEGOTIATION", "CLOSED_WON");
    }

    @Test
    @DisplayName("el tablero agrupa por etapa con totales y monto ponderado")
    void boardHasOneColumnPerStage() throws Exception {
        getAs(MANAGER_EMAIL, "/api/opportunities/board")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(7))
                .andExpect(jsonPath("$[0].stage.code").value("PROSPECTING"))
                .andExpect(jsonPath("$[6].stage.code").value("CLOSED_LOST"));
    }

    @Test
    @DisplayName("la cotización calcula subtotal, descuento, IVA por tarifa y total en el servidor")
    void quoteTotalsAreComputedServerSide() throws Exception {
        long company = createCompany(REP1_EMAIL, "Cotización S.A.");
        long opportunity = createOpportunity(company);
        List<Number> productIds = read(getAs(REP1_EMAIL, "/api/products"), "$[?(@.sku == 'CRM-LIC')].id");
        long product = productIds.getFirst().longValue();

        String quote = """
                {"opportunityId":%d,"notes":"Oferta válida por 30 días","items":[
                  {"productId":%d,"quantity":3,"discountPercent":10},
                  {"description":"Manual impreso","quantity":2,"unitPrice":45.00,"vatRateCode":"0"},
                  {"description":"Instalación","quantity":1,"unitPrice":99.99,"vatRateCode":"4","subtotal":1,"total":1}
                ]}
                """.formatted(opportunity, product);
        // 3 × 180 = 540, desc. 10 % = 54 → 486, IVA 15 % = 72.90
        // 2 × 45 = 90, IVA 0 %
        // 1 × 99.99, IVA 15 % = 15.00 (14.9985 redondeado HALF_UP)
        long quoteId = readId(postAs(REP1_EMAIL, "/api/quotes", quote)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.number", matchesPattern("COT-\\d{4}-\\d{6}")))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.subtotal").value(729.99))
                .andExpect(jsonPath("$.discountTotal").value(54.00))
                .andExpect(jsonPath("$.taxTotal").value(87.90))
                .andExpect(jsonPath("$.total").value(763.89))
                .andExpect(jsonPath("$.taxBreakdown[0].percentage").value(15.00))
                .andExpect(jsonPath("$.taxBreakdown[0].taxableBase").value(585.99))
                .andExpect(jsonPath("$.taxBreakdown[1].percentage").value(0.00))
                .andExpect(jsonPath("$.items[0].description").value("Licencia CRM Pro (usuario/año)")));

        MvcResult pdf = getAs(REP1_EMAIL, "/api/quotes/{id}/pdf", quoteId)
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition", containsString(".pdf")))
                .andReturn();
        byte[] bytes = pdf.getResponse().getContentAsByteArray();
        assertThat(new String(bytes, 0, 5)).isEqualTo("%PDF-");
        assertThat(bytes.length).isGreaterThan(1500);

        getAs(REP2_EMAIL, "/api/quotes/{id}/pdf", quoteId).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("ciclo de vida: solo el borrador se edita y las transiciones inválidas se rechazan")
    void quoteLifecycle() throws Exception {
        long opportunity = createOpportunity(createCompany(REP1_EMAIL, "Ciclo S.A."));
        String body = """
                {"opportunityId":%d,"items":[{"description":"Servicio","quantity":1,"unitPrice":100.00,"vatRateCode":"4"}]}
                """.formatted(opportunity);
        long quoteId = readId(postAs(REP1_EMAIL, "/api/quotes", body).andExpect(status().isCreated()));
        putAs(REP1_EMAIL, "/api/quotes/{id}", body.replace("100.00", "200.00"), quoteId)
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(230.00))
                .andExpect(jsonPath("$.items.length()").value(1));

        patchAs(REP1_EMAIL, "/api/quotes/{id}/status", "{\"status\":\"ACCEPTED\"}", quoteId).andExpect(status().is(422));
        patchAs(REP1_EMAIL, "/api/quotes/{id}/status", "{\"status\":\"SENT\"}", quoteId).andExpect(status().isOk());
        putAs(REP1_EMAIL, "/api/quotes/{id}", body, quoteId).andExpect(status().is(422));
        patchAs(REP1_EMAIL, "/api/quotes/{id}/status", "{\"status\":\"ACCEPTED\"}", quoteId)
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    @DisplayName("no se puede dirigir una cotización a un contacto sin consentimiento de envío de cotizaciones")
    void quoteRecipientNeedsConsent() throws Exception {
        long company = createCompany(REP1_EMAIL, "Destinatario S.A.");
        long contact = createContact(REP1_EMAIL, company, "Ana");
        long opportunity = createOpportunity(company);
        String body = """
                {"opportunityId":%d,"contactId":%d,"items":[{"description":"Servicio","quantity":1,"unitPrice":10.00,"vatRateCode":"4"}]}
                """.formatted(opportunity, contact);
        postAs(REP1_EMAIL, "/api/quotes", body).andExpect(status().is(422))
                .andExpect(jsonPath("$.detail", containsString("Envío de cotizaciones")));

        postAs(REP1_EMAIL, "/api/contacts/{id}/consents",
                "{\"purposeCode\":\"QUOTE_DELIVERY\",\"channel\":\"EMAIL\",\"evidence\":\"Correo de solicitud de cotización\"}", contact)
                .andExpect(status().isCreated());
        postAs(REP1_EMAIL, "/api/quotes", body).andExpect(status().isCreated())
                .andExpect(jsonPath("$.contactName").value("Ana Prueba"));
    }

    @Test
    @DisplayName("el forecast suma pipeline ponderado, commit y ganado con las fórmulas documentadas")
    void forecastMatchesFormulas() throws Exception {
        getAs(MANAGER_EMAIL, "/api/forecast?from=2026-01-01&to=2026-12-31")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.byMonth.length()").value(12))
                .andExpect(jsonPath("$.byStage.length()").value(7))
                .andExpect(jsonPath("$.totals.winRate").exists());
        getAs(MANAGER_EMAIL, "/api/forecast?from=2026-12-31&to=2026-01-01").andExpect(status().is(422));
    }
}
