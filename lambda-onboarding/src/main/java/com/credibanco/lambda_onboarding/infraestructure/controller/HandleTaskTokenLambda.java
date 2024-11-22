package com.credibanco.lambda_onboarding.infraestructure.controller;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.credibanco.lambda_onboarding.infraestructure.config.ByteBufferAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.AWSStepFunctionsClientBuilder;
import com.amazonaws.services.stepfunctions.model.SendTaskSuccessRequest;
import com.amazonaws.services.stepfunctions.model.SendTaskFailureRequest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class HandleTaskTokenLambda implements RequestHandler<Map<String, Object>, String> {
    private final Gson gson;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AWSStepFunctions stepFunctionsClient = AWSStepFunctionsClientBuilder.defaultClient();
    private final AmazonDynamoDB dynamoDBClient;
    private static final String TABLE_NAME = "step_results";

    public HandleTaskTokenLambda() {
        this.dynamoDBClient = AmazonDynamoDBClientBuilder.defaultClient();
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(java.nio.ByteBuffer.class, new ByteBufferAdapter())
                .create();
    }


    @Override
    public String handleRequest(Map<String, Object> event, Context context) {

        try {
            context.getLogger().log("Event received: " + objectMapper.writeValueAsString(event));
            String taskToken = (String) event.get("TaskToken");
            Map<String, Object> input = (Map<String, Object>) event.get("Input");

            if (taskToken == null || taskToken.isEmpty()) {
                throw new IllegalArgumentException("TaskToken is missing or empty");
            }

            if (input == null || input.isEmpty()) {
                throw new IllegalArgumentException("Input data is missing or empty");
            }

            context.getLogger().log("Processing TaskToken: " + taskToken);
            context.getLogger().log("Processing Input: " + objectMapper.writeValueAsString(input));

            String requestId = (String) input.get("requestId");
            String currentDateTime = getCurrentDateTime();

            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("requestName", input.get("requestName"));
            resultMap.put("createdAt", input.get("createdAt"));
            resultMap.put("requestEmail", input.get("requestEmail"));
            resultMap.put("TPP", input.get("TPP"));
            resultMap.put("requestId", input.get("requestId"));
            resultMap.put("additionalInfo", input.get("additionalInfo"));
            resultMap.put("type", input.get("type"));
            resultMap.put("requestStatus", input.get("requestStatus"));
            resultMap.put("updatedAt", input.get("updatedAt"));


            String resultJson = gson.toJson(resultMap);

            Map<String, AttributeValue> item = new HashMap<>();
            item.put("stepId", new AttributeValue().withS("step-" + requestId));
            item.put("requestId", new AttributeValue().withS(requestId));
            item.put("name", new AttributeValue().withS("Register Onboarding Request"));
            item.put("status", new AttributeValue().withS("Pending"));
            item.put("token", new AttributeValue().withS(taskToken));
            item.put("result", new AttributeValue().withS(resultJson));
            item.put("created", new AttributeValue().withS(currentDateTime));

            dynamoDBClient.putItem(new PutItemRequest().withTableName(TABLE_NAME).withItem(item));

            context.getLogger().log("Item successfully saved to DynamoDB.");

            return "Task token saved successfully. Awaiting external trigger.";

        } catch (Exception e) {
            context.getLogger().log("Error processing TaskToken: " + e.getMessage());

            sendTaskFailure((String) event.get("TaskToken"), e.getMessage());
            throw new RuntimeException("Failed to process TaskToken", e);
        }
    }


    private void sendTaskSuccess(String taskToken, String output) {
        try {
            SendTaskSuccessRequest request = new SendTaskSuccessRequest()
                    .withTaskToken(taskToken)
                    .withOutput("{\"status\":\"" + output + "\"}");

            stepFunctionsClient.sendTaskSuccess(request);
            System.out.println("Task success sent for token: " + taskToken);
        } catch (Exception e) {
            System.err.println("Failed to send task success: " + e.getMessage());
        }
    }

    private void sendTaskFailure(String taskToken, String errorMessage) {
        try {
            SendTaskFailureRequest request = new SendTaskFailureRequest()
                    .withTaskToken(taskToken)
                    .withError(errorMessage);

            stepFunctionsClient.sendTaskFailure(request);
            System.out.println("Task failure sent for token: " + taskToken);
        } catch (Exception e) {
            System.err.println("Failed to send task failure: " + e.getMessage());
        }
    }
    private String getCurrentDateTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return now.format(formatter);
    }

}
