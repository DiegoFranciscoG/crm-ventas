package ec.dfgranda.crm.repository;

import ec.dfgranda.crm.domain.Role;
import ec.dfgranda.crm.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByRole(Role role);

    List<User> findAllByOrderByFullNameAsc();
}
