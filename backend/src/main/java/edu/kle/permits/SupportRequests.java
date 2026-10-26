package edu.kle.permits;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface SupportRequests extends JpaRepository<SupportRequest,Long> {
 @Query(value="select nextval('support_ticket_numbers')",nativeQuery=true) long nextTicketSequence();
 boolean existsByRequestIpHashAndCreatedAtAfter(String requestIpHash,Instant after);
 Optional<SupportRequest> findByTicketNumberAndPersonalContactEmailIgnoreCase(String ticketNumber,String personalContactEmail);
 @Query("select s from SupportRequest s where (:status is null or s.status=:status) and (:category is null or s.category=:category) and (:q is null or lower(s.ticketNumber) like lower(concat('%',:q,'%')) or lower(s.requesterName) like lower(concat('%',:q,'%')) or lower(s.personalContactEmail) like lower(concat('%',:q,'%')) or lower(coalesce(s.permitworksEmail,'')) like lower(concat('%',:q,'%'))) order by s.createdAt desc")
 Page<SupportRequest> search(@Param("q")String query,@Param("status")SupportStatus status,@Param("category")SupportCategory category,Pageable pageable);
 long countByStatusIn(java.util.Collection<SupportStatus> statuses);
}
