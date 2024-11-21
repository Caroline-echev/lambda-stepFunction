package com.credibanco.lambda_onboarding.infraestructure.exception;

public class Error {
    private String errorMessage;

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public String toString() {
        return "Error{" +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}