package com.localllm.sovereign_ai_workbench.Dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequestDto(
        @NotBlank(message = "conversationId is required")
        String conversationId,

        @NotBlank(message = "message is required")
        String message
) {}
