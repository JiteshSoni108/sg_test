package com.example.requestmanagement.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 4000) String content) {
}
