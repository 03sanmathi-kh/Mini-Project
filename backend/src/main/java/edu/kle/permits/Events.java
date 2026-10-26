package edu.kle.permits;

import org.springframework.data.repository.Repository;
import org.springframework.data.domain.*;
public interface Events extends Repository<SecurityEvent,Long> {
 SecurityEvent save(SecurityEvent event);
 Page<SecurityEvent> findAllByOrderByOccurredAtDesc(Pageable pageable);
 long countByAction(String action);
}
