package ec.dfgranda.crm.repository;

import ec.dfgranda.crm.domain.AccessAuditLog;
import ec.dfgranda.crm.domain.Activity;
import ec.dfgranda.crm.domain.AuditResourceType;
import ec.dfgranda.crm.domain.Company;
import ec.dfgranda.crm.domain.Contact;
import ec.dfgranda.crm.domain.Opportunity;
import ec.dfgranda.crm.domain.Quote;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;

/**
 * Filtros dinámicos. Cada método devuelve {@code null} cuando el filtro no aplica (Spring Data lo ignora).
 * Los filtros {@code ownedBy} implementan la regla "el vendedor ve lo suyo" a nivel de consulta.
 */
public final class Specs {

    private Specs() {
    }

    /** Combina con AND los filtros no nulos. */
    @SafeVarargs
    public static <T> Specification<T> all(Specification<T>... parts) {
        Specification<T> result = Specification.unrestricted();
        for (Specification<T> part : parts) {
            if (part != null) {
                result = result.and(part);
            }
        }
        return result;
    }

    static String likePattern(String text) {
        String escaped = text.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }

    // ---- Empresas
    public static Specification<Company> companyOwnedBy(Long ownerId) {
        return ownerId == null ? null : (root, query, cb) -> cb.equal(root.get("owner").get("id"), ownerId);
    }

    public static Specification<Company> companyMatches(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String pattern = likePattern(text);
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("legalName")), pattern, '\\'),
                cb.like(cb.lower(root.get("tradeName")), pattern, '\\'),
                cb.like(root.get("ruc"), pattern, '\\'));
    }

    // ---- Contactos (visibles si la empresa es del vendedor)
    public static Specification<Contact> contactOwnedBy(Long ownerId) {
        return ownerId == null ? null : (root, query, cb) -> cb.equal(root.get("company").get("owner").get("id"), ownerId);
    }

    public static Specification<Contact> contactOfCompany(Long companyId) {
        return companyId == null ? null : (root, query, cb) -> cb.equal(root.get("company").get("id"), companyId);
    }

    public static Specification<Contact> contactMatches(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String pattern = likePattern(text);
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("firstName")), pattern, '\\'),
                cb.like(cb.lower(root.get("lastName")), pattern, '\\'),
                cb.like(cb.lower(root.get("email")), pattern, '\\'),
                cb.like(cb.lower(root.get("company").get("legalName")), pattern, '\\'));
    }

    // ---- Oportunidades
    public static Specification<Opportunity> opportunityOwnedBy(Long ownerId) {
        return ownerId == null ? null : (root, query, cb) -> cb.equal(root.get("owner").get("id"), ownerId);
    }

    public static Specification<Opportunity> opportunityOfCompany(Long companyId) {
        return companyId == null ? null : (root, query, cb) -> cb.equal(root.get("company").get("id"), companyId);
    }

    public static Specification<Opportunity> opportunityInStage(String stageCode) {
        return stageCode == null || stageCode.isBlank() ? null
                : (root, query, cb) -> cb.equal(root.get("stage").get("code"), stageCode);
    }

    public static Specification<Opportunity> opportunityMatches(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String pattern = likePattern(text);
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), pattern, '\\'),
                cb.like(cb.lower(root.get("company").get("legalName")), pattern, '\\'));
    }

    /** Tablero: abiertas + cerradas desde {@code closedSince} (para ver lo ganado/perdido reciente). */
    public static Specification<Opportunity> openOrClosedSince(Instant closedSince) {
        return (root, query, cb) -> cb.or(
                cb.isFalse(root.get("stage").get("closed")),
                cb.greaterThanOrEqualTo(root.get("closedAt"), closedSince));
    }

    /** Forecast: abiertas con cierre previsto en el período, o cerradas dentro del período. */
    public static Specification<Opportunity> relevantForPeriod(LocalDate from, LocalDate toInclusive, Instant fromInstant,
                                                               Instant toExclusive) {
        return (root, query, cb) -> cb.or(
                cb.and(cb.isFalse(root.get("stage").get("closed")),
                        cb.between(root.get("expectedCloseDate"), from, toInclusive)),
                cb.and(cb.isTrue(root.get("stage").get("closed")),
                        cb.greaterThanOrEqualTo(root.get("closedAt"), fromInstant),
                        cb.lessThan(root.get("closedAt"), toExclusive)));
    }

    // ---- Actividades
    public static Specification<Activity> activityOwnedBy(Long ownerId) {
        return ownerId == null ? null : (root, query, cb) -> cb.equal(root.get("owner").get("id"), ownerId);
    }

    public static Specification<Activity> activityOfOpportunity(Long opportunityId) {
        return opportunityId == null ? null : (root, query, cb) -> cb.equal(root.get("opportunity").get("id"), opportunityId);
    }

    public static Specification<Activity> activityOfCompany(Long companyId) {
        return companyId == null ? null : (root, query, cb) -> cb.equal(root.get("company").get("id"), companyId);
    }

    public static Specification<Activity> activityOfContact(Long contactId) {
        return contactId == null ? null : (root, query, cb) -> cb.equal(root.get("contact").get("id"), contactId);
    }

    public static Specification<Activity> activityCompleted(Boolean completed) {
        if (completed == null) {
            return null;
        }
        return (root, query, cb) -> completed ? cb.isNotNull(root.get("completedAt")) : cb.isNull(root.get("completedAt"));
    }

    public static Specification<Activity> activityDueBetween(Instant from, Instant to) {
        if (from == null && to == null) {
            return null;
        }
        return (root, query, cb) -> {
            if (from != null && to != null) {
                return cb.between(root.get("dueAt"), from, to);
            }
            return from != null ? cb.greaterThanOrEqualTo(root.get("dueAt"), from) : cb.lessThanOrEqualTo(root.get("dueAt"), to);
        };
    }

    // ---- Cotizaciones
    public static Specification<Quote> quoteOwnedBy(Long ownerId) {
        return ownerId == null ? null : (root, query, cb) -> cb.equal(root.get("opportunity").get("owner").get("id"), ownerId);
    }

    public static Specification<Quote> quoteOfOpportunity(Long opportunityId) {
        return opportunityId == null ? null : (root, query, cb) -> cb.equal(root.get("opportunity").get("id"), opportunityId);
    }

    // ---- Log de accesos
    public static Specification<AccessAuditLog> auditResource(AuditResourceType type, Long resourceId) {
        if (type == null) {
            return null;
        }
        return (root, query, cb) -> resourceId == null ? cb.equal(root.get("resourceType"), type)
                : cb.and(cb.equal(root.get("resourceType"), type), cb.equal(root.get("resourceId"), resourceId));
    }

    public static Specification<AccessAuditLog> auditUser(Long userId) {
        return userId == null ? null : (root, query, cb) -> cb.equal(root.get("userId"), userId);
    }

    public static Specification<AccessAuditLog> auditOnlyDenied(boolean onlyDenied) {
        return onlyDenied ? (root, query, cb) -> cb.isFalse(root.get("success")) : null;
    }

    public static Specification<AccessAuditLog> auditBetween(Instant from, Instant to) {
        return (root, query, cb) -> cb.and(cb.greaterThanOrEqualTo(root.get("occurredAt"), from),
                cb.lessThan(root.get("occurredAt"), to));
    }
}
