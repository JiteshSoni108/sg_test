package com.example.requestmanagement.web;

import com.example.requestmanagement.domain.AuditAction;
import com.example.requestmanagement.domain.RequestAuditEntity;
import com.example.requestmanagement.domain.RequestState;

import java.time.OffsetDateTime;

public record AuditResponse(Long id, Long requestId, RequestState fromState, RequestState toState, AuditAction action,
                            String reason, String actor, OffsetDateTime occurredAt) {

    public static AuditResponse from(RequestAuditEntity entity) {
        return new AuditResponse(entity.getId(), entity.getRequestId(), entity.getFromState(), entity.getToState(), entity.getAction(), entity.getReason(), entity.getActor(), entity.getOccurredAt());
    }
}