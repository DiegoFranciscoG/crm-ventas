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
 * Solicitud de ejercicio de derechos del titular. El responsable debe registrarlas todas con el detalle de la
 * atención dada (Reglamento LOPDP Art. 15). {@code contactId} no es FK: el registro sobrevive a la eliminación.
 */
@Entity
@Table(name = "data_subject_requests")
public class DataSubjectRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contact_id", nullable = false)
    private Long contactId;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, length = 20)
    private RequestType requestType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestChannel channel;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestStatus status;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(length = 500)
    private String resolution;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "handled_by", nullable = false)
    private User handledBy;

    protected DataSubjectRequest() {
    }

    public DataSubjectRequest(Long contactId, RequestType requestType, RequestChannel channel,
                              Instant receivedAt, Instant dueAt, User handledBy) {
        this.contactId = contactId;
        this.requestType = requestType;
        this.channel = channel;
        this.receivedAt = receivedAt;
        this.dueAt = dueAt;
        this.handledBy = handledBy;
        this.status = RequestStatus.RECEIVED;
    }

    public void complete(Instant when, String resolution) {
        this.status = RequestStatus.COMPLETED;
        this.completedAt = when;
        this.resolution = resolution;
    }

    public Long getId() {
        return id;
    }

    public Long getContactId() {
        return contactId;
    }

    public RequestType getRequestType() {
        return requestType;
    }

    public RequestChannel getChannel() {
        return channel;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public Instant getDueAt() {
        return dueAt;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public String getResolution() {
        return resolution;
    }

    public User getHandledBy() {
        return handledBy;
    }
}
