package ec.dfgranda.crm.repository;

import ec.dfgranda.crm.domain.ContactConsent;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ContactConsentRepository extends JpaRepository<ContactConsent, Long> {

    @EntityGraph(attributePaths = {"purpose", "recordedBy"})
    List<ContactConsent> findByContactIdOrderByGrantedAtDesc(Long contactId);

    Optional<ContactConsent> findFirstByContactIdAndPurposeCodeAndRevokedAtIsNull(Long contactId, String purposeCode);

    @Query("""
            select count(c) > 0 from ContactConsent c
            where c.contact.id = :contactId and c.purpose.code = :purpose
              and c.revokedAt is null and c.expiresAt > :now
            """)
    boolean existsActive(@Param("contactId") Long contactId, @Param("purpose") String purpose, @Param("now") Instant now);

    @Query("""
            select c.contact.id from ContactConsent c
            where c.contact.id in :contactIds and c.purpose.code = :purpose
              and c.revokedAt is null and c.expiresAt > :now
            """)
    List<Long> findContactIdsWithActive(@Param("contactIds") Collection<Long> contactIds,
                                        @Param("purpose") String purpose, @Param("now") Instant now);

    @Modifying
    @Query("delete from ContactConsent c where c.contact.id = :contactId")
    int deleteByContactId(@Param("contactId") Long contactId);
}
