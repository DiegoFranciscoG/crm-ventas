package ec.dfgranda.crm.security;

import ec.dfgranda.crm.domain.Role;

/** Usuario autenticado en la petición actual. El rol se lee de la base de datos, no del token. */
public record AuthenticatedUser(Long id, String email, String fullName, Role role) {

    public boolean hasGlobalVisibility() {
        return role.hasGlobalVisibility();
    }
}
