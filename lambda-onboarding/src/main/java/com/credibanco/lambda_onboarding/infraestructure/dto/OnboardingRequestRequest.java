package com.credibanco.lambda_onboarding.infraestructure.dto;

import lombok.Data;

@Data
public class OnboardingRequestRequest {
    private String requestId;
    private String requestName;
    private String TPP;
}
