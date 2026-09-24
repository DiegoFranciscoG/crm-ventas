package ec.dfgranda.crm.repository;

import ec.dfgranda.crm.domain.VatRate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VatRateRepository extends JpaRepository<VatRate, String> {
}
