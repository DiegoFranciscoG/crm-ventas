package ec.dfgranda.crm.repository;

import ec.dfgranda.crm.domain.ProcessingPurpose;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessingPurposeRepository extends JpaRepository<ProcessingPurpose, String> {
}
