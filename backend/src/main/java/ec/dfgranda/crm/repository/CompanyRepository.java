package ec.dfgranda.crm.repository;

import ec.dfgranda.crm.domain.Company;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    boolean existsByRuc(String ruc);
}
