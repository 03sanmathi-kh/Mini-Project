package edu.kle.permits;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import org.springframework.data.repository.query.Param;

@Entity @Table(name="registration_mutex") class RegistrationMutex { @Id Long id; }
@Entity @Table(name="registration_limits") class RegistrationLimit { @Id String id;Instant windowStart;int attempts; }
interface RegistrationMutexes extends JpaRepository<RegistrationMutex,Long> {
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select m from RegistrationMutex m where m.id=1") RegistrationMutex acquire();
}
interface RegistrationRequests extends JpaRepository<RegistrationRequest,Long> {
 Optional<RegistrationRequest> findByActiveEmail(String email);
 Optional<RegistrationRequest> findByTrackingHashAndPersonalEmail(String hash,String email);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select r from RegistrationRequest r where r.id=:id") Optional<RegistrationRequest> lockById(@Param("id")Long id);
 Page<RegistrationRequest> findByStatus(RegistrationStatus status,Pageable page);
}
interface RegistrationLimits extends JpaRepository<RegistrationLimit,String> {}
