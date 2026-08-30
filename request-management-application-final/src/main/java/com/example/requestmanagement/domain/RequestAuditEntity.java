package com.example.requestmanagement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "REQUEST_AUDIT")
public class RequestAuditEntity {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "request_audit_seq_generator"
    )
    @SequenceGenerator(
            name = "request_audit_seq_generator",
            sequenceName = "REQUEST_AUDIT_SEQ",
            allocationSize = 50
    )
    @Column(name = "ID", nullable = false)
    private Long id;

    @Column(name = "REQUEST_ID", nullable = false)
    private Long requestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "FROM_STATE", length = 20)
    private RequestState fromState;

    @Enumerated(EnumType.STRING)
    @Column(name = "TO_STATE", length = 20)
    private RequestState toState;

    @Enumerated(EnumType.STRING)
    @Column(name = "ACTION", nullable = false, length = 50)
    private AuditAction action;

    @Column(name = "REASON", length = 1000)
    private String reason;

    @Column(name = "ACTOR", length = 255)
    private String actor;

    @Column(name = "OCCURRED_AT", nullable = false)
    private OffsetDateTime occurredAt;

    protected RequestAuditEntity() {
    }

    private RequestAuditEntity(
            Long requestId,
            RequestState fromState,
            RequestState toState,
            AuditAction action,
            String reason,
            String actor,
            OffsetDateTime occurredAt) {
        this.requestId = requestId;
        this.fromState = fromState;
        this.toState = toState;
        this.action = action;
        this.reason = reason;
        this.actor = actor;
        this.occurredAt = occurredAt;
    }

    public static RequestAuditEntity created(
            Long requestId,
            RequestState state,
            String actor,
            OffsetDateTime occurredAt) {
        return new RequestAuditEntity(
                requestId,
                null,
                state,
                AuditAction.CREATED,
                null,
                actor,
                occurredAt
        );
    }

    public static RequestAuditEntity contentUpdated(
            Long requestId,
            RequestState currentState,
            String actor,
            OffsetDateTime occurredAt) {
        return new RequestAuditEntity(
                requestId,
                currentState,
                currentState,
                AuditAction.CONTENT_UPDATED,
                null,
                actor,
                occurredAt
        );
    }

    public static RequestAuditEntity stateChanged(
            Long requestId,
            RequestState fromState,
            RequestState toState,
            String reason,
            String actor,
            OffsetDateTime occurredAt) {
        return new RequestAuditEntity(
                requestId,
                fromState,
                toState,
                AuditAction.STATE_CHANGED,
                reason,
                actor,
                occurredAt
        );
    }

    public Long getId() {
        return id;
    }

    public Long getRequestId() {
        return requestId;
    }

    public RequestState getFromState() {
        return fromState;
    }

    public RequestState getToState() {
        return toState;
    }

    public AuditAction getAction() {
        return action;
    }

    public String getReason() {
        return reason;
    }

    public String getActor() {
        return actor;
    }

    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }
}