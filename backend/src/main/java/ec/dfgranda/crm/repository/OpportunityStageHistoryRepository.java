package ec.dfgranda.crm.repository;

import ec.dfgranda.crm.domain.OpportunityStageHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OpportunityStageHistoryRepository extends JpaRepository<OpportunityStageHistory, Long> {

    List<OpportunityStageHistory> findByOpportunityIdOrderByChangedAtAsc(Long opportunityId);
}
