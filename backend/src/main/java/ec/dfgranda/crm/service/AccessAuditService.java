package ec.dfgranda.crm.service;

import ec.dfgranda.crm.domain.AccessAuditLog;
import ec.dfgranda.crm.domain.AuditAction;
import ec.dfgranda.crm.domain.AuditResourceType;
import ec.dfgranda.crm.domain.User;
import ec.dfgranda.crm.dto.AuditLogResponse;
import ec.dfgranda.crm.dto.PageResponse;
import ec.dfgranda.crm.mapper.PrivacyMapper;
import ec.dfgranda.crm.repository.AccessAuditLogRepository;
import ec.dfgranda.crm.repository.Specs;
import ec.dfgranda.crm.repository.UserRepository;
import ec.dfgranda.crm.security.AuthenticatedUser;
import ec.dfgranda.crm.security.CurrentUserProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Log de accesos a datos personales (responsabilidad proactiva, LOPDP Art. 10 k).
 * <ul>
 *   <li>{@link #record}: se une a la transacción del negocio; si la operación falla, el registro no queda.</li>
 *   <li>{@link #recordDenied}: transacción propia, porque la operación de negocio se revierte.</li>
 * </ul>
 */
@Service
public class AccessAuditService {

    private static final int MAX_DETAIL = 200;

    private final AccessAuditLogRepository repository;
    private final UserRepository users;
    private final CurrentUserProvider currentUser;
    private final Clock clock;

    public AccessAuditService(AccessAuditLogRepository repository, UserRepository users,
                              CurrentUserProvider currentUser, Clock clock) {
        this.repository = repository;
        this.users = users;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(AuditAction action, AuditResourceType type, Long resourceId, String detail) {
        save(action, type, resourceId, true, detail);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordDenied(AuditAction action, AuditResourceType type, Long resourceId) {
        save(action, type, resourceId, false, "Acceso denegado: el recurso pertenece a otro usuario");
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> search(AuditResourceType type, Long resourceId, Long userId, boolean onlyDenied,
                                                 Instant from, Instant to, int page, int size) {
        Page<AccessAuditLog> result = repository.findAll(
                Specs.all(Specs.auditResource(type, resourceId), Specs.auditUser(userId),
                        Specs.auditOnlyDenied(onlyDenied), Specs.auditBetween(from, to)),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "occurredAt", "id")));
        Set<Long> userIds = new HashSet<>();
        result.forEach(log -> userIds.add(log.getUserId()));
        Map<Long, String> names = users.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getFullName, (a, b) -> a));
        return PageResponse.of(result, log -> PrivacyMapper.toResponse(log, names.get(log.getUserId())));
    }

    private void save(AuditAction action, AuditResourceType type, Long resourceId, boolean success, String detail) {
        AuthenticatedUser user = currentUser.get();
        String safeDetail = detail == null || detail.length() <= MAX_DETAIL ? detail : detail.substring(0, MAX_DETAIL);
        repository.save(new AccessAuditLog(clock.instant(), user.id(), action, type, resourceId, success,
                currentUser.clientIp(), safeDetail));
    }
}
