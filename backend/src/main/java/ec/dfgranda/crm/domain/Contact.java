package ec.dfgranda.crm.domain;

import ec.dfgranda.crm.security.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Persona de contacto de una empresa cliente (titular de datos personales). Solo se guardan los datos
 * necesarios (minimización, LOPDP Art. 10 e); la cédula es opcional y se cifra en reposo (Art. 37).
 */
@Entity
@Table(name = "contacts")
public class Contact extends TimestampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(length = 254)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(name = "job_title", length = 100)
    private String jobTitle;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "national_id_encrypted")
    private String nationalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_source", nullable = false, length = 20)
    private ContactSource dataSource;

    protected Contact() {
    }

    public Contact(Company company, String firstName, String lastName, ContactSource dataSource) {
        this.company = company;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dataSource = dataSource;
    }

    public String fullName() {
        return firstName + " " + lastName;
    }

    public Long getId() {
        return id;
    }

    public Company getCompany() {
        return company;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = User.normalizeEmail(email);
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getNationalId() {
        return nationalId;
    }

    public void setNationalId(String nationalId) {
        this.nationalId = nationalId == null || nationalId.isBlank() ? null : nationalId;
    }

    public ContactSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(ContactSource dataSource) {
        this.dataSource = dataSource;
    }
}
