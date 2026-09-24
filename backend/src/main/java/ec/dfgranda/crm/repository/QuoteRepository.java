package ec.dfgranda.crm.repository;

import ec.dfgranda.crm.domain.Quote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface QuoteRepository extends JpaRepository<Quote, Long>, JpaSpecificationExecutor<Quote> {

    List<Quote> findByContactId(Long contactId);

    @Override
    @EntityGraph(attributePaths = {"opportunity", "opportunity.company"})
    Page<Quote> findAll(Specification<Quote> spec, Pageable pageable);

    @Query(value = "select nextval('quote_number_seq')", nativeQuery = true)
    long nextNumber();
}
