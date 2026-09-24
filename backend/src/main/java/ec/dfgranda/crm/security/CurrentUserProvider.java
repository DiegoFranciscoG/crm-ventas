package ec.dfgranda.crm.security;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Acceso al usuario autenticado y a la IP de origen; se inyecta en los servicios para poder simularlo en tests. */
@Component
public class CurrentUserProvider {

    public AuthenticatedUser get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user;
        }
        throw new AuthenticationCredentialsNotFoundException("No hay un usuario autenticado.");
    }

    /** IP del cliente. Con {@code forward-headers-strategy: native} Tomcat ya resolvió X-Forwarded-For. */
    public String clientIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest().getRemoteAddr();
        }
        return null;
    }
}
