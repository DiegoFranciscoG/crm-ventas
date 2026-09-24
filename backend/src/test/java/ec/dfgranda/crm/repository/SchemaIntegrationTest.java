package ec.dfgranda.crm.repository;

import ec.dfgranda.crm.domain.Company;
import ec.dfgranda.crm.domain.Contact;
import ec.dfgranda.crm.domain.ContactSource;
import ec.dfgranda.crm.domain.Role;
import ec.dfgranda.crm.domain.User;
import ec.dfgranda.crm.support.IntegrationTest;
import ec.dfgranda.crm.validation.TaxpayerType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Verifica migraciones Flyway, mapeo JPA (ddl-auto=validate) y las garantías de la base de datos. */
class SchemaIntegrationTest extends IntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private UserRepository users;
    @Autowired
    private CompanyRepository companies;
    @Autowired
    private ContactRepository contacts;
    @Autowired
    private ProvinceRepository provinces;

    @Test
    void loadsOfficialCatalogs() {
        assertThat(count("provinces")).isEqualTo(25);
        assertThat(count("industries")).isEqualTo(21);
        assertThat(count("pipeline_stages")).isEqualTo(7);
        assertThat(jdbc.queryForObject("select percentage from vat_rates where code = '4'", String.class))
                .isEqualTo("15.00");
    }

    @Test
    void accessAuditLogIsAppendOnly() {
        User user = users.save(new User("audit-" + System.nanoTime() + "@example.com", "Auditor", "x", Role.ADMIN));
        jdbc.update("insert into access_audit_log (user_id, action, resource_type, resource_id, success) values (?, 'VIEW', 'CONTACT', 1, true)",
                user.getId());

        assertThatThrownBy(() -> jdbc.update("update access_audit_log set success = false where user_id = ?", user.getId()))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("append-only");
        assertThatThrownBy(() -> jdbc.update("delete from access_audit_log where user_id = ?", user.getId()))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.execute("truncate access_audit_log"))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void storesNationalIdEncryptedAtRest() {
        User owner = users.save(new User("owner-" + System.nanoTime() + "@example.com", "Owner", "x", Role.SALES_REP));
        Company company = new Company("2099998701001", TaxpayerType.PRIVATE_COMPANY, true, owner);
        company.setLegalName("Empresa de prueba S.A.");
        company.setProvince(provinces.findById("20").orElseThrow());
        company = companies.save(company);
        Contact contact = new Contact(company, "Ana", "Prueba", ContactSource.DIRECT);
        contact.setNationalId("2059999991");
        Long id = contacts.saveAndFlush(contact).getId();

        String raw = jdbc.queryForObject("select national_id_encrypted from contacts where id = ?", String.class, id);

        assertThat(raw).startsWith("v1:").doesNotContain("2059999991");
        assertThat(contacts.findById(id).orElseThrow().getNationalId()).isEqualTo("2059999991");
    }

    private Integer count(String table) {
        return jdbc.queryForObject("select count(*) from " + table, Integer.class);
    }
}
