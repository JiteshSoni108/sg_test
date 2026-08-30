package com.example.requestmanagement.web;

import com.example.requestmanagement.domain.RequestEntity;
import com.example.requestmanagement.domain.RequestState;
import java.time.OffsetDateTime;

public record RequestResponse(
        Long id,
        String name,
        String content,
        RequestState state,
        Long publicationNumber,
        String terminationReason,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static RequestResponse from(RequestEntity e) {
        return new RequestResponse(e.getId(), e.getName(), e.getContent(), e.getState(),
                e.getPublicationNumber(), e.getTerminationReason(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
