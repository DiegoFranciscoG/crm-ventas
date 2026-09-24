package ec.dfgranda.crm.support;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Tests de API sobre el contexto completo: seguridad real (JWT), base de datos real y MockMvc. */
@AutoConfigureMockMvc
public abstract class ApiTest extends IntegrationTest {

    private static final Map<String, String> TOKENS = new ConcurrentHashMap<>();

    @Autowired
    protected MockMvc mvc;

    protected String token(String email) {
        return TOKENS.computeIfAbsent(email, e -> {
            String password = ADMIN_EMAIL.equals(e) ? TestSecrets.ADMIN_PASSWORD : DEMO_PASSWORD;
            try {
                MvcResult result = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"" + e + "\",\"password\":\"" + password + "\"}"))
                        .andExpect(status().isOk()).andReturn();
                return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
            } catch (Exception ex) {
                throw new IllegalStateException("No se pudo iniciar sesión con " + e, ex);
            }
        });
    }

    protected ResultActions getAs(String email, String url, Object... vars) throws Exception {
        return mvc.perform(auth(get(url, vars), email));
    }

    protected ResultActions postAs(String email, String url, String json, Object... vars) throws Exception {
        return mvc.perform(auth(post(url, vars), email).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    protected ResultActions putAs(String email, String url, String json, Object... vars) throws Exception {
        return mvc.perform(auth(put(url, vars), email).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    protected ResultActions patchAs(String email, String url, String json, Object... vars) throws Exception {
        return mvc.perform(auth(patch(url, vars), email).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    protected ResultActions deleteAs(String email, String url, Object... vars) throws Exception {
        return mvc.perform(auth(delete(url, vars), email));
    }

    protected static <T> T read(ResultActions result, String path) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), path);
    }

    protected static long readId(ResultActions result) throws Exception {
        return ((Number) read(result, "$.id")).longValue();
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder builder, String email) {
        return builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(email));
    }

    /** Crea una empresa con RUC sintético único para el usuario indicado y devuelve su ID. */
    protected long createCompany(String email, String legalName) throws Exception {
        String json = """
                {"ruc":"%s","legalName":"%s","provinceCode":"24","city":"Santa Elena","industryCode":"J"}
                """.formatted(TestIds.uniquePrivateRuc(), legalName);
        return readId(postAs(email, "/api/companies", json).andExpect(status().isCreated()));
    }

    /** Crea un contacto con consentimiento de contacto comercial y devuelve su ID. */
    protected long createContact(String email, long companyId, String firstName) throws Exception {
        String json = """
                {"companyId":%d,"firstName":"%s","lastName":"Prueba","email":"%s.prueba@example.com",
                 "dataSource":"DIRECT",
                 "initialConsent":{"purposeCode":"COMMERCIAL_CONTACT","channel":"WEB_FORM",
                                   "evidence":"Formulario web de prueba con aviso v1.0"}}
                """.formatted(companyId, firstName, firstName.toLowerCase());
        return readId(postAs(email, "/api/contacts", json).andExpect(status().isCreated()));
    }
}
