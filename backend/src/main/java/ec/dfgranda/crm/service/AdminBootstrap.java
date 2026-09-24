package ec.dfgranda.crm.service;

import ec.dfgranda.crm.domain.Role;
import ec.dfgranda.crm.domain.User;
import ec.dfgranda.crm.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Crea el primer administrador desde variables de entorno ({@code ADMIN_EMAIL} y {@code ADMIN_PASSWORD}) si todavía
 * no existe ninguno. Así nunca hay un administrador con contraseña conocida en el código o en los seeds.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);
    private static final int MIN_PASSWORD = 12;
    private static final int MAX_PASSWORD = 72;

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public AdminBootstrap(UserRepository users, PasswordEncoder passwordEncoder,
                          @Value("${ADMIN_EMAIL:}") String email, @Value("${ADMIN_PASSWORD:}") String password) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank() || users.existsByRole(Role.ADMIN)) {
            return;
        }
        if (password.length() < MIN_PASSWORD || password.length() > MAX_PASSWORD) {
            throw new IllegalStateException("ADMIN_PASSWORD debe tener entre 12 y 72 caracteres.");
        }
        users.save(new User(email, "Administrador", passwordEncoder.encode(password), Role.ADMIN));
        log.info("Administrador inicial creado a partir de ADMIN_EMAIL.");
    }
}
