package ec.dfgranda.crm.repository;

import ec.dfgranda.crm.domain.Contact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ContactRepository extends JpaRepository<Contact, Long>, JpaSpecificationExecutor<Contact> {

    boolean existsByCompanyId(Long companyId);

    @Override
    @EntityGraph(attributePaths = {"company"})
    Page<Contact> findAll(Specification<Contact> spec, Pageable pageable);
}
