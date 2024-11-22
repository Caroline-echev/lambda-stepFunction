package com.credibanco.lambda_onboarding.infraestructure.controller;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.stepfunctions.AWSStepFunctionsClient;
import com.amazonaws.services.stepfunctions.model.StartExecutionRequest;
import com.amazonaws.services.stepfunctions.model.StartExecutionResult;
import com.credibanco.lambda_onboarding.infraestructure.config.ByteBufferAdapter;
import com.credibanco.lambda_onboarding.infraestructure.dto.OnboardingRequest;
import com.credibanco.lambda_onboarding.infraestructure.dto.OnboardingRequestRequest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.http.HttpStatus;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.credibanco.lambda_onboarding.infraestructure.util.Constants.*;
import static com.credibanco.lambda_onboarding.infraestructure.util.Constants.TITLE_RESPONSE;

public class OnboardingRequestLambda implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final Gson gson;
    private final AWSStepFunctionsClient stepFunctionsClient;
    public OnboardingRequestLambda() {
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(java.nio.ByteBuffer.class, new ByteBufferAdapter())
                .create();
        this.stepFunctionsClient = new AWSStepFunctionsClient();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        final LambdaLogger logger = context.getLogger();
        logger.log(TITLE_REQUEST + event.getBody());

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();

        try {
            OnboardingRequestRequest input = gson.fromJson(event.getBody(), OnboardingRequestRequest.class);

            if (input == null || input.getRequestId() == null || input.getRequestName() == null || input.getTPP() == null
            || input.getType() == null ) {
                return returnApiResponse(HttpStatus.SC_BAD_REQUEST, REQUEST_BODY_BAD_REQUEST,
                        ERROR_MESSAGE_BAD_REQUEST, logger);
            }

            if (input.getRequestId().isEmpty() || input.getRequestName().isEmpty() || input.getType().isEmpty()) {
                return returnApiResponse(HttpStatus.SC_BAD_REQUEST, REQUEST_BODY_BAD_REQUEST,
                        ERROR_MESSAGE_BAD_REQUEST, logger);
            }

            String currentTimestamp = getCurrentTimestamp();
            String cleanedEmail = input.getRequestName().replaceAll("\\s", "") + "@example.com";

            Map<String, Object> additionalInfo = new HashMap<>();
            additionalInfo.put("clientBank", true);

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("requestName", input.getRequestName());
            responseBody.put("createdAt", currentTimestamp);
            responseBody.put("requestEmail", cleanedEmail);
            responseBody.put("TPP", input.getTPP());
            responseBody.put("requestId", input.getRequestId());
            responseBody.put("additionalInfo", additionalInfo);
            responseBody.put("type", input.getType());
            responseBody.put("requestStatus", "Pending");
            responseBody.put("updatedAt", currentTimestamp);


            String stateMachineArn = "arn:aws:states:us-east-1:182399719769:stateMachine:OnboardingTPPFlow";

            try {
                StartExecutionRequest startExecutionRequest = new StartExecutionRequest()
                        .withStateMachineArn(stateMachineArn)
                        .withInput(gson.toJson(responseBody));

                StartExecutionResult executionResult = stepFunctionsClient.startExecution(startExecutionRequest);
                logger.log("Step Function execution started with ARN: " + executionResult.getExecutionArn());

                responseBody.put("stepFunctionExecutionArn", executionResult.getExecutionArn());
            } catch (Exception e) {
                logger.log("Error invoking Step Function: " + e.getMessage());
                return returnApiResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, REQUEST_BODY_INTERNAL_SERVER_ERROR,
                        "Failed to invoke Step Function: " + e.getMessage(), logger);
            }

            response.setStatusCode(HttpStatus.SC_OK);
            response.setHeaders(getDefaultHeaders());
            response.setBody(gson.toJson(responseBody));

        } catch (Exception e) {
            logger.log(TITLE_ERROR + e.getMessage());
            return returnApiResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, REQUEST_BODY_INTERNAL_SERVER_ERROR,
                    e.getMessage(), logger);
        }

        return response;
    }
    private String getCurrentTimestamp() {
        SimpleDateFormat customFormat = new SimpleDateFormat("dd-MM-yyyy");
        customFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        return customFormat.format(new Date());
    }
    private Map<String, String> getDefaultHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put(CONTENT_TYPE, APPLICATION_JSON);
        return headers;
    }

    private APIGatewayProxyResponseEvent returnApiResponse(int statusCode, String responseBody,
                                                           String errorMessage, LambdaLogger logger) {
        final com.credibanco.lambda_onboarding.infraestructure.exception.Error error =
                new com.credibanco.lambda_onboarding.infraestructure.exception.Error();
        error.setErrorMessage(errorMessage);

        com.credibanco.lambda_onboarding.infraestructure.exception.ResponseError<String> response =
                new com.credibanco.lambda_onboarding.infraestructure.exception.ResponseError<>(statusCode, responseBody, error);

        APIGatewayProxyResponseEvent responseEvent = new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withHeaders(getDefaultHeaders())
                .withBody(gson.toJson(response));

        logger.log(TITLE_RESPONSE + responseEvent);
        return responseEvent;
    }
}
