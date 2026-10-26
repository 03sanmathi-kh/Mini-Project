package edu.kle.permits;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;
public interface Users extends JpaRepository<AppUser,Long> {
 Optional<AppUser> findByEmail(String email);
 boolean existsByEmailIgnoreCase(String email);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select u from AppUser u where lower(u.email)=lower(:email)") Optional<AppUser> lockByEmail(@Param("email") String email);
 java.util.List<AppUser> findByRoleAndEnabledTrueOrderByFullName(Role role);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select u from AppUser u where u.id=:id") Optional<AppUser> lockById(@Param("id") Long id);
}
