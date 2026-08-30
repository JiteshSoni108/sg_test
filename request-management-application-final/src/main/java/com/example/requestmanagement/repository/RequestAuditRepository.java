package com.example.requestmanagement.repository;

import com.example.requestmanagement.domain.RequestAuditEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequestAuditRepository extends JpaRepository<RequestAuditEntity, Long> {
    List<RequestAuditEntity> findByRequestIdOrderByOccurredAtAscIdAsc(Long requestId);
}
