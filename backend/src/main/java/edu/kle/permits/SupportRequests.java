package edu.kle.permits;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface SupportRequests extends JpaRepository<SupportRequest,Long>, JpaSpecificationExecutor<SupportRequest> {
 @Query(value="select nextval('support_ticket_numbers')",nativeQuery=true) long nextTicketSequence();
 boolean existsByRequestIpHashAndCreatedAtAfter(String requestIpHash,Instant after);
 Optional<SupportRequest> findByTicketNumberAndPersonalContactEmailIgnoreCase(String ticketNumber,String personalContactEmail);
 long countByStatusIn(java.util.Collection<SupportStatus> statuses);
}
