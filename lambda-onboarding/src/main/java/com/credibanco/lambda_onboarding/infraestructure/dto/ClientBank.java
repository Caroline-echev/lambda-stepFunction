package com.credibanco.lambda_onboarding.infraestructure.dto;

import lombok.Data;

@Data
public class ClientBank {
    private Boolean BOOL;

    public Boolean getBOOL() {
        return BOOL;
    }

    public void setBOOL(Boolean BOOL) {
        this.BOOL = BOOL;
    }
}
