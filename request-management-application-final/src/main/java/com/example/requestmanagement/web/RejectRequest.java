package com.example.requestmanagement.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectRequest(
        @NotBlank(message = "Reason is required") @Size(max = 1000, message = "Reason must not exceed 1000 characters") String reason) {
}