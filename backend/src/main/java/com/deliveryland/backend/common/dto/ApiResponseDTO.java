package com.deliveryland.backend.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponseDTO {
    private String message;
    private Object data;

    public ApiResponseDTO(String message, Object data) {
        this.message = message;
        this.data = data;
    }

    public ApiResponseDTO(String message) {
        this.message = message;
    }

}
