package ec.dfgranda.crm.service;

import ec.dfgranda.crm.domain.User;
import ec.dfgranda.crm.dto.LoginRequest;
import ec.dfgranda.crm.dto.LoginResponse;
import ec.dfgranda.crm.dto.UserResponse;
import ec.dfgranda.crm.mapper.UserMapper;
import ec.dfgranda.crm.repository.UserRepository;
import ec.dfgranda.crm.security.JwtService;
import ec.dfgranda.crm.security.LoginRateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;

/**
 * Login con rate limiting. Siempre se ejecuta una comparación BCrypt (aunque el correo no exista) para que el
 * tiempo de respuesta no revele qué cuentas existen (enumeración de usuarios).
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginRateLimiter rateLimiter;
    private final Clock clock;
    private final String dummyHash;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService,
                       LoginRateLimiter rateLimiter, Clock clock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.rateLimiter = rateLimiter;
        this.clock = clock;
        this.dummyHash = passwordEncoder.encode("timing-equalizer-" + System.nanoTime());
    }

    @Transactional
    public LoginResponse login(LoginRequest request, String clientIp) {
        String email = User.normalizeEmail(request.email());
        rateLimiter.checkAllowed(email, clientIp);

        Optional<User> candidate = users.findByEmail(email);
        boolean passwordMatches = passwordEncoder.matches(request.password(),
                candidate.map(User::getPasswordHash).orElse(dummyHash));
        if (candidate.isEmpty() || !passwordMatches || !candidate.get().isActive()) {
            rateLimiter.recordFailure(email, clientIp);
            log.info("Login fallido desde {}", clientIp);
            throw new BadCredentialsException("Credenciales inválidas");
        }

        User user = candidate.get();
        rateLimiter.recordSuccess(email);
        user.registerLogin(clock.instant());
        JwtService.IssuedToken token = jwtService.issue(user);
        return new LoginResponse(token.value(), "Bearer", token.expiresAt(), UserMapper.toResponse(user));
    }

    @Transactional(readOnly = true)
    public UserResponse me(Long userId) {
        return users.findById(userId).map(UserMapper::toResponse)
                .orElseThrow(() -> new BadCredentialsException("Usuario no encontrado"));
    }
}
