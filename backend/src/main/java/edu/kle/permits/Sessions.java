package edu.kle.permits;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface Sessions extends JpaRepository<AuthSession,UUID> {
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select s from AuthSession s where s.id=:id") Optional<AuthSession> lockById(@Param("id") UUID id);
 @Modifying @Query("update AuthSession s set s.revoked=true where s.userId=:userId") void revokeForUser(@Param("userId") Long userId);
}
