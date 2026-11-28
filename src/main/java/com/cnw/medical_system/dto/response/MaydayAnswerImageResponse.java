package com.cnw.medical_system.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MaydayAnswerImageResponse(
        String id,
        @JsonProperty("answer_image_id") String answerImageId,
        @JsonProperty("answer_id") String answerId,
        @JsonProperty("attachment_id") String attachmentId,
        @JsonProperty("option_value") String optionValue,
        @JsonProperty("created_at") String createdAt) { }
