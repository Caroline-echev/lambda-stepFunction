package com.credibanco.lambda_onboarding.infraestructure.exception;

import lombok.Data;

@Data
public class ResponseError<T> {

    private int httpStatusCode;
    private T responseBody;
    private Error error;

    public ResponseError(int httpStatusCode, T responseBody, Error error) {
        this.httpStatusCode = httpStatusCode;
        this.responseBody = responseBody;
        this.error = error;
    }

    @Override
    public String toString() {
        return "Response{" +
                "httpStatusCode=" + httpStatusCode +
                ", responseBody=" + responseBody +
                ", error=" + error +
                '}';
    }
}
