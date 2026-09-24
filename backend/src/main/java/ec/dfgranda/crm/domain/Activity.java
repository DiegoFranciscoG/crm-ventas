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

/** Llamada, reunión, email, tarea o nota, con recordatorio opcional por correo al dueño. */
@Entity
@Table(name = "activities")
public class Activity extends TimestampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActivityType type;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(length = 4000)
    private String notes;

    @Column(name = "due_at")
    private Instant dueAt;

    @Column(name = "remind_at")
    private Instant remindAt;

    @Column(name = "reminder_sent_at")
    private Instant reminderSentAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id")
    private Contact contact;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opportunity_id")
    private Opportunity opportunity;

    protected Activity() {
    }

    public Activity(ActivityType type, String subject, User owner) {
        this.type = type;
        this.subject = subject;
        this.owner = owner;
    }

    /** Llamadas, reuniones y emails implican contactar al titular: requieren consentimiento vigente. */
    public boolean involvesDirectContact() {
        return type == ActivityType.CALL || type == ActivityType.MEETING || type == ActivityType.EMAIL;
    }

    public void schedule(Instant dueAt, Instant remindAt) {
        if (remindAt != null && dueAt != null && remindAt.isAfter(dueAt)) {
            throw new IllegalArgumentException("El recordatorio debe ser anterior o igual a la fecha de la actividad.");
        }
        boolean reminderChanged = remindAt == null ? this.remindAt != null : !remindAt.equals(this.remindAt);
        this.dueAt = dueAt;
        this.remindAt = remindAt;
        if (reminderChanged) {
            this.reminderSentAt = null;
        }
    }

    public void complete(Instant when) {
        this.completedAt = when;
    }

    public void reopen() {
        this.completedAt = null;
    }

    public void markReminderSent(Instant when) {
        this.reminderSentAt = when;
    }

    /** Anonimiza la actividad cuando se elimina al titular vinculado (LOPDP Art. 15). */
    public void detachErasedContact() {
        this.contact = null;
        this.subject = "[Contacto eliminado] " + type.name();
        this.notes = null;
    }

    public Long getId() {
        return id;
    }

    public ActivityType getType() {
        return type;
    }

    public void setType(ActivityType type) {
        this.type = type;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getDueAt() {
        return dueAt;
    }

    public Instant getRemindAt() {
        return remindAt;
    }

    public Instant getReminderSentAt() {
        return reminderSentAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public Contact getContact() {
        return contact;
    }

    public void setContact(Contact contact) {
        this.contact = contact;
    }

    public Opportunity getOpportunity() {
        return opportunity;
    }

    public void setOpportunity(Opportunity opportunity) {
        this.opportunity = opportunity;
    }
}
