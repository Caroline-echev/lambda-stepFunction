package com.credibanco.lambda_onboarding.infraestructure.dto;


import lombok.Data;

@Data
public class AdditionalInfo {
    private ClientBank clientBank;

    public ClientBank getClientBank() {
        return clientBank;
    }

    public void setClientBank(ClientBank clientBank) {
        this.clientBank = clientBank;
    }
}