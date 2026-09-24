package ec.dfgranda.crm.repository;

import ec.dfgranda.crm.domain.DataSubjectRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataSubjectRequestRepository extends JpaRepository<DataSubjectRequest, Long> {

    @EntityGraph(attributePaths = {"handledBy"})
    Page<DataSubjectRequest> findAllByOrderByReceivedAtDesc(Pageable pageable);
}
