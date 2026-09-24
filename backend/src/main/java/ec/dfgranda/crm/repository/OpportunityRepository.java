package ec.dfgranda.crm.repository;

import ec.dfgranda.crm.domain.Opportunity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface OpportunityRepository extends JpaRepository<Opportunity, Long>, JpaSpecificationExecutor<Opportunity> {

    boolean existsByCompanyId(Long companyId);

    List<Opportunity> findByPrimaryContactId(Long contactId);

    @Override
    @EntityGraph(attributePaths = {"company", "owner", "stage", "primaryContact"})
    List<Opportunity> findAll(Specification<Opportunity> spec, Sort sort);

    @Override
    @EntityGraph(attributePaths = {"company", "owner", "stage", "primaryContact"})
    Page<Opportunity> findAll(Specification<Opportunity> spec, Pageable pageable);
}
