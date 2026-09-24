package ec.dfgranda.crm.domain;

import ec.dfgranda.crm.validation.TaxpayerType;
import jakarta.persistence.Column;
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

/** Empresa cliente identificada por su RUC. Su dueño (vendedor) determina quién la ve. */
@Entity
@Table(name = "companies")
public class Company extends TimestampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 13)
    private String ruc;

    @Enumerated(EnumType.STRING)
    @Column(name = "taxpayer_type", nullable = false, length = 20)
    private TaxpayerType taxpayerType;

    @Column(name = "ruc_checksum_verified", nullable = false)
    private boolean rucChecksumVerified;

    @Column(name = "legal_name", nullable = false, length = 300)
    private String legalName;

    @Column(name = "trade_name", length = 300)
    private String tradeName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "industry_code")
    private Industry industry;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "province_code", nullable = false)
    private Province province;

    @Column(length = 100)
    private String city;

    @Column(length = 300)
    private String address;

    @Column(length = 20)
    private String phone;

    @Column(length = 254)
    private String email;

    @Column(length = 200)
    private String website;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    protected Company() {
    }

    public Company(String ruc, TaxpayerType taxpayerType, boolean rucChecksumVerified, User owner) {
        this.owner = owner;
        changeRuc(ruc, taxpayerType, rucChecksumVerified);
    }

    public final void changeRuc(String ruc, TaxpayerType taxpayerType, boolean rucChecksumVerified) {
        this.ruc = ruc;
        this.taxpayerType = taxpayerType;
        this.rucChecksumVerified = rucChecksumVerified;
    }

    /** El RUC de una persona natural contiene su cédula: es un dato personal (LOPDP). */
    public boolean holdsPersonalData() {
        return taxpayerType == TaxpayerType.NATURAL_PERSON;
    }

    public Long getId() {
        return id;
    }

    public String getRuc() {
        return ruc;
    }

    public TaxpayerType getTaxpayerType() {
        return taxpayerType;
    }

    public boolean isRucChecksumVerified() {
        return rucChecksumVerified;
    }

    public String getLegalName() {
        return legalName;
    }

    public void setLegalName(String legalName) {
        this.legalName = legalName;
    }

    public String getTradeName() {
        return tradeName;
    }

    public void setTradeName(String tradeName) {
        this.tradeName = tradeName;
    }

    public Industry getIndustry() {
        return industry;
    }

    public void setIndustry(Industry industry) {
        this.industry = industry;
    }

    public Province getProvince() {
        return province;
    }

    public void setProvince(Province province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = User.normalizeEmail(email);
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }
}
