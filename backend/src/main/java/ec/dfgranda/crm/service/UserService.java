package ec.dfgranda.crm.service;

import ec.dfgranda.crm.domain.Role;
import ec.dfgranda.crm.domain.User;
import ec.dfgranda.crm.dto.UserCreateRequest;
import ec.dfgranda.crm.dto.UserResponse;
import ec.dfgranda.crm.dto.UserUpdateRequest;
import ec.dfgranda.crm.exception.BusinessRuleException;
import ec.dfgranda.crm.exception.ConflictException;
import ec.dfgranda.crm.exception.NotFoundException;
import ec.dfgranda.crm.mapper.UserMapper;
import ec.dfgranda.crm.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Gestión de usuarios (solo ADMIN escribe). Los usuarios no se eliminan: se desactivan. */
@Service
public class UserService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AccessPolicy accessPolicy;

    public UserService(UserRepository users, PasswordEncoder passwordEncoder, AccessPolicy accessPolicy) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.accessPolicy = accessPolicy;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> list() {
        return users.findAllByOrderByFullNameAsc().stream().map(UserMapper::toResponse).toList();
    }

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        String email = User.normalizeEmail(request.email());
        if (users.existsByEmail(email)) {
            throw new ConflictException("Ya existe un usuario con ese correo.");
        }
        User user = new User(email, request.fullName().trim(), passwordEncoder.encode(request.password()), request.role());
        return UserMapper.toResponse(users.save(user));
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = users.findById(id).orElseThrow(() -> new NotFoundException("Usuario no encontrado."));
        if (user.getId().equals(accessPolicy.user().id()) && (!request.active() || request.role() != Role.ADMIN)) {
            throw new BusinessRuleException("No puede desactivarse ni quitarse el rol de administrador a sí mismo.");
        }
        user.setFullName(request.fullName().trim());
        user.setRole(request.role());
        user.setActive(request.active());
        if (request.newPassword() != null && !request.newPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        }
        return UserMapper.toResponse(user);
    }

    /** Resuelve el dueño asignado: un gerente puede asignar a otro usuario activo; el vendedor siempre es el dueño. */
    @Transactional(readOnly = true)
    public User resolveOwner(Long requestedOwnerId) {
        Long me = accessPolicy.user().id();
        Long ownerId = requestedOwnerId != null && accessPolicy.user().hasGlobalVisibility() ? requestedOwnerId : me;
        User owner = users.findById(ownerId).orElseThrow(() -> new BusinessRuleException("El usuario asignado no existe."));
        if (!owner.isActive()) {
            throw new BusinessRuleException("No se puede asignar a un usuario desactivado.");
        }
        return owner;
    }
}
