package ec.dfgranda.crm.dto;

import ec.dfgranda.crm.domain.Role;

import java.time.Instant;

public record UserResponse(Long id, String email, String fullName, Role role, boolean active, Instant lastLoginAt) {
}
