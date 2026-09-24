package ec.dfgranda.crm.controller;

import ec.dfgranda.crm.dto.LoginRequest;
import ec.dfgranda.crm.dto.LoginResponse;
import ec.dfgranda.crm.dto.UserResponse;
import ec.dfgranda.crm.security.CurrentUserProvider;
import ec.dfgranda.crm.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación")
public class AuthController {

    private final AuthService authService;
    private final CurrentUserProvider currentUser;

    public AuthController(AuthService authService, CurrentUserProvider currentUser) {
        this.authService = authService;
        this.currentUser = currentUser;
    }

    @PostMapping("/login")
    @Operation(summary = "Inicia sesión y devuelve un JWT de corta duración (máx. 5 intentos fallidos / 15 min)")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request, currentUser.clientIp());
    }

    @GetMapping("/me")
    @Operation(summary = "Perfil del usuario autenticado")
    public UserResponse me() {
        return authService.me(currentUser.get().id());
    }
}
