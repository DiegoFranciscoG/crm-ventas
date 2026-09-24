package ec.dfgranda.crm.repository;

import ec.dfgranda.crm.domain.Contact;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactRepository extends JpaRepository<Contact, Long> {
}
