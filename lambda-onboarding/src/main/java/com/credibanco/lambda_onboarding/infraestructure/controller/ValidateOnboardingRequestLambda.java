package com.credibanco.lambda_onboarding.infraestructure.controller;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.credibanco.lambda_onboarding.infraestructure.config.ByteBufferAdapter;
import com.credibanco.lambda_onboarding.infraestructure.dto.OnboardingRequest;
import com.credibanco.lambda_onboarding.infraestructure.exception.Error;
import com.credibanco.lambda_onboarding.infraestructure.exception.ResponseError;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.http.HttpStatus;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.credibanco.lambda_onboarding.infraestructure.util.Constants.*;

public class ValidateOnboardingRequestLambda implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final Gson gson;
    private final AmazonDynamoDB dynamoDBClient;
    private static final String TABLE_NAME = "onboarding_request";
    public ValidateOnboardingRequestLambda() {
        this.dynamoDBClient = AmazonDynamoDBClientBuilder.defaultClient();
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(java.nio.ByteBuffer.class, new ByteBufferAdapter())
                .create();
    }
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        final LambdaLogger logger = context.getLogger();
        logger.log(TITLE_REQUEST + event.getBody());
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        try {
            OnboardingRequest input = gson.fromJson(event.getBody(), OnboardingRequest.class);
            if (input == null || input.getRequestEmail() == null || input.getRequestName() == null
                    || input.getType() == null || input.getRequestStatus() == null || input.getTPP() == null) {
                return returnApiResponse(HttpStatus.SC_BAD_REQUEST, REQUEST_BODY_BAD_REQUEST,
                        ERROR_MESSAGE_BAD_REQUEST, logger);
            }

            if (input.getRequestEmail().isEmpty() || input.getRequestName().isEmpty() || input.getType().isEmpty()) {
                return returnApiResponse(HttpStatus.SC_BAD_REQUEST, REQUEST_BODY_BAD_REQUEST,
                        ERROR_MESSAGE_BAD_REQUEST, logger);
            }

            String createdAtFormatted = formatDate(input.getCreatedAt());
            String updatedAtFormatted = formatDate(input.getUpdatedAt());


            Map<String, AttributeValue> item = new HashMap<>();
            item.put("requestId", new AttributeValue().withS(input.getRequestId()));
            item.put("createdAt", new AttributeValue().withS(createdAtFormatted));
            item.put("requestEmail", new AttributeValue().withS(input.getRequestEmail()));
            item.put("requestName", new AttributeValue().withS(input.getRequestName()));
            item.put("requestStatus", new AttributeValue().withS(input.getRequestStatus()));
            item.put("TPP", new AttributeValue().withN(String.valueOf(input.getTPP())));
            item.put("type", new AttributeValue().withS(input.getType()));
            item.put("updatedAt", new AttributeValue().withS(updatedAtFormatted));

            if (input.getAdditionalInfo() != null && input.getAdditionalInfo().getClientBank() != null) {
                item.put("additionalInfo", new AttributeValue().withM(new HashMap<String, AttributeValue>() {{
                    put("clientBank", new AttributeValue().withBOOL(input.getAdditionalInfo().getClientBank().getBOOL()));
                }}));
            }

            dynamoDBClient.putItem(new PutItemRequest().withTableName(TABLE_NAME).withItem(item));
            logger.log("Item successfully saved to DynamoDB.");

            response.setStatusCode(HttpStatus.SC_OK);
            response.setHeaders(getDefaultHeaders());
            response.setBody(gson.toJson(item));

        } catch (Exception e) {
            logger.log(TITLE_ERROR + e.getMessage());
            return returnApiResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, REQUEST_BODY_INTERNAL_SERVER_ERROR,
                    e.getMessage(), logger);
        }

        return response;
    }

    private String formatDate(String dateStr) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            Date date = inputFormat.parse(dateStr);

            SimpleDateFormat outputFormat = new SimpleDateFormat("dd-MM-yyyy");
            return outputFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Map<String, String> getDefaultHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put(CONTENT_TYPE, APPLICATION_JSON);
        return headers;
    }

    private APIGatewayProxyResponseEvent returnApiResponse(int statusCode, String responseBody,
                                                           String errorMessage, LambdaLogger logger) {
        final Error error =
                new Error();
        error.setErrorMessage(errorMessage);

        ResponseError<String> response =
                new ResponseError<>(statusCode, responseBody, error);

        APIGatewayProxyResponseEvent responseEvent = new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withHeaders(getDefaultHeaders())
                .withBody(gson.toJson(response));

        logger.log(TITLE_RESPONSE + responseEvent);
        return responseEvent;
    }
}
