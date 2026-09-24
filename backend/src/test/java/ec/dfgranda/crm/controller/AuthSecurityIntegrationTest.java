package ec.dfgranda.crm.controller;

import ec.dfgranda.crm.support.ApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthSecurityIntegrationTest extends ApiTest {

    @Test
    void loginReturnsShortLivedBearerToken() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"GERENTE@example.com\",\"password\":\"" + DEMO_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.role").value("MANAGER"))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist());
    }

    @Test
    void wrongPasswordAndUnknownUserGetTheSameGenericAnswer() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"vendedor1@example.com\",\"password\":\"incorrecta-123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Correo o contraseña incorrectos."));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nadie@example.com\",\"password\":\"incorrecta-123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Correo o contraseña incorrectos."));
    }

    @Test
    void blocksLoginAfterFiveFailedAttempts() throws Exception {
        String body = "{\"email\":\"fuerza-bruta@example.com\",\"password\":\"clave-equivocada\"}";
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void rejectsMissingOrForgedTokens() throws Exception {
        mvc.perform(get("/api/companies")).andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"));
        mvc.perform(get("/api/companies").header("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.e30.firma-falsa"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validatesRequestBodies() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"no-es-correo\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void exposesOnlyHealthFromActuatorAndSendsSecurityHeaders() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().string("Content-Security-Policy", containsString("frame-ancestors 'none'")));
        mvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
        getAs(ADMIN_EMAIL, "/actuator/env").andExpect(status().isNotFound());
    }

    @Test
    void corsOnlyAllowsConfiguredOrigin() throws Exception {
        mvc.perform(options("/api/companies").header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
        mvc.perform(options("/api/companies").header("Origin", "https://sitio-malicioso.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    void validatesIdentificationWithoutPersistingAnything() throws Exception {
        postAs(REP1_EMAIL, "/api/validation/identification", "{\"type\":\"RUC\",\"number\":\"2299981235001\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.checksumVerified").value(false))
                .andExpect(jsonPath("$.taxpayerType").value("PRIVATE_COMPANY"));
        postAs(REP1_EMAIL, "/api/validation/identification", "{\"type\":\"CEDULA\",\"number\":\"2059999992\"}")
                .andExpect(jsonPath("$.valid").value(false));
    }

    @Test
    void onlyAdminManagesUsers() throws Exception {
        String newUser = """
                {"email":"nuevo.vendedor@example.com","fullName":"Nuevo Vendedor","password":"una-clave-larga-segura","role":"SALES_REP"}
                """;
        postAs(REP1_EMAIL, "/api/users", newUser).andExpect(status().isForbidden());
        postAs(MANAGER_EMAIL, "/api/users", newUser).andExpect(status().isForbidden());
        postAs(ADMIN_EMAIL, "/api/users", newUser).andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("SALES_REP"));
        postAs(ADMIN_EMAIL, "/api/users", newUser).andExpect(status().isConflict());
        getAs(MANAGER_EMAIL, "/api/users").andExpect(status().isOk());
        getAs(REP1_EMAIL, "/api/users").andExpect(status().isForbidden());
    }
}
