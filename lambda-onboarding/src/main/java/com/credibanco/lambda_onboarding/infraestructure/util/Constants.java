package com.credibanco.lambda_onboarding.infraestructure.util;

public class Constants {
    private Constants() {

    }

    public static final String REQUEST_BODY_BAD_REQUEST = "Request body not valid";
    public static final String ERROR_MESSAGE_BAD_REQUEST ="Los campos 'name', 'lastName', y 'email' no deben estar vacios.";
    public static final String REQUEST_BODY_INTERNAL_SERVER_ERROR = "Internal server error";
    public static final String ERROR_MESSAGE_INTERNAL_SERVER_ERROR = "Falta un campo obligatorio";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String APPLICATION_JSON = "application/json";

    public static final String TITLE_ERROR = "Error: ";
    public static final String TITLE_REQUEST = "Request: ";
    public static final String TITLE_RESPONSE = "Response: ";

}

