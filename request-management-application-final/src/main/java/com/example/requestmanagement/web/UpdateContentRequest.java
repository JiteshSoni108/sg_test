package com.example.requestmanagement.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateContentRequest(@NotBlank @Size(max = 4000) String content) {
}
