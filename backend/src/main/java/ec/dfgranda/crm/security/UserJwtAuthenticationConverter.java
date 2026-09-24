package ec.dfgranda.crm.security;

import ec.dfgranda.crm.domain.User;
import ec.dfgranda.crm.repository.UserRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Convierte un JWT válido en el usuario autenticado. Consulta la base de datos en cada petición: si el
 * usuario fue desactivado o cambió de rol, el efecto es inmediato aunque el token siga vigente.
 */
@Component
public class UserJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserRepository users;

    public UserJwtAuthenticationConverter(UserRepository users) {
        this.users = users;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        long userId;
        try {
            userId = Long.parseLong(jwt.getSubject());
        } catch (NumberFormatException e) {
            throw new InvalidBearerTokenException("Token inválido.");
        }
        User user = users.findById(userId)
                .filter(User::isActive)
                .orElseThrow(() -> new InvalidBearerTokenException("Usuario inexistente o desactivado."));
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getEmail(), user.getFullName(), user.getRole());
        return UsernamePasswordAuthenticationToken.authenticated(principal, jwt,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
    }
}
