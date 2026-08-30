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
import jakarta.persistence.Version;

import java.time.OffsetDateTime;

@Entity
@Table(name = "REQUESTS")
public class RequestEntity {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "request_seq_generator"
    )
    @SequenceGenerator(
            name = "request_seq_generator",
            sequenceName = "REQUEST_SEQ",
            allocationSize = 50
    )
    @Column(name = "ID", nullable = false)
    private Long id;

    @Column(name = "NAME", nullable = false, length = 200)
    private String name;

    @Column(name = "CONTENT", nullable = false, length = 4000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATE", nullable = false, length = 20)
    private RequestState state;

    @Column(name = "PUBLICATION_NUMBER", unique = true)
    private Long publicationNumber;

    @Column(name = "TERMINATION_REASON", length = 1000)
    private String terminationReason;

    @Column(name = "CREATED_AT", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "VERSION", nullable = false)
    private long version;

    protected RequestEntity() {
    }

    public RequestEntity(
            String name,
            String content,
            OffsetDateTime now) {
        this.name = name;
        this.content = content;
        this.state = RequestState.CREATED;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getContent() {
        return content;
    }

    public RequestState getState() {
        return state;
    }

    public Long getPublicationNumber() {
        return publicationNumber;
    }

    public String getTerminationReason() {
        return terminationReason;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }

    public void updateContent(
            String content,
            OffsetDateTime now) {
        this.content = content;
        this.updatedAt = now;
    }

    public void changeState(
            RequestState newState,
            String reason,
            Long publicationNumber,
            OffsetDateTime now) {
        this.state = newState;
        this.terminationReason = reason;
        this.publicationNumber = publicationNumber;
        this.updatedAt = now;
    }
}