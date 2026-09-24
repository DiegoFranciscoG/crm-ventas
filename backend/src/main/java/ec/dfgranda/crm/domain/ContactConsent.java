package ec.dfgranda.crm.domain;

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

import java.time.Instant;

/**
 * Registro de consentimiento (o base legal) por finalidad. Revocar no borra el registro: la revocatoria
 * no tiene efecto retroactivo y el consentimiento debe poder demostrarse (LOPDP Art. 8; Reglamento Arts. 5-6).
 */
@Entity
@Table(name = "contact_consents")
public class ContactConsent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contact_id", nullable = false)
    private Contact contact;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purpose_code", nullable = false)
    private ProcessingPurpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(name = "legal_basis", nullable = false, length = 30)
    private LegalBasis legalBasis;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConsentChannel channel;

    @Column(nullable = false, length = 500)
    private String evidence;

    @Column(name = "privacy_notice_version", nullable = false, length = 20)
    private String privacyNoticeVersion;

    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revocation_reason", length = 300)
    private String revocationReason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recorded_by", nullable = false)
    private User recordedBy;

    protected ContactConsent() {
    }

    public ContactConsent(Contact contact, ProcessingPurpose purpose, ConsentChannel channel, String evidence,
                          String privacyNoticeVersion, Instant grantedAt, Instant expiresAt, User recordedBy) {
        this.contact = contact;
        this.purpose = purpose;
        this.legalBasis = purpose.getLegalBasis();
        this.channel = channel;
        this.evidence = evidence;
        this.privacyNoticeVersion = privacyNoticeVersion;
        this.grantedAt = grantedAt;
        this.expiresAt = expiresAt;
        this.recordedBy = recordedBy;
    }

    /** Vigente = no revocado y dentro del plazo de conservación. */
    public boolean isActiveAt(Instant instant) {
        return revokedAt == null && instant.isBefore(expiresAt);
    }

    public void revoke(Instant when, String reason) {
        if (revokedAt != null) {
            throw new IllegalStateException("El consentimiento ya fue revocado.");
        }
        this.revokedAt = when;
        this.revocationReason = reason;
    }

    public Long getId() {
        return id;
    }

    public Contact getContact() {
        return contact;
    }

    public ProcessingPurpose getPurpose() {
        return purpose;
    }

    public LegalBasis getLegalBasis() {
        return legalBasis;
    }

    public ConsentChannel getChannel() {
        return channel;
    }

    public String getEvidence() {
        return evidence;
    }

    public String getPrivacyNoticeVersion() {
        return privacyNoticeVersion;
    }

    public Instant getGrantedAt() {
        return grantedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public String getRevocationReason() {
        return revocationReason;
    }

    public User getRecordedBy() {
        return recordedBy;
    }
}
