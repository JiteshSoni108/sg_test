package com.example.requestmanagement.web;

import com.example.requestmanagement.domain.RequestState;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TransitionRequest(
        @NotNull RequestState targetState,
        @Size(max = 1000) String reason) {
}
