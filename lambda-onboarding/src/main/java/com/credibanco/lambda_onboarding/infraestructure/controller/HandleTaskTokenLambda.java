package com.credibanco.lambda_onboarding.infraestructure.controller;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.AWSStepFunctionsClientBuilder;
import com.amazonaws.services.stepfunctions.model.SendTaskSuccessRequest;
import com.amazonaws.services.stepfunctions.model.SendTaskFailureRequest;

import java.util.Map;

public class HandleTaskTokenLambda implements RequestHandler<Map<String, Object>, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AWSStepFunctions stepFunctionsClient = AWSStepFunctionsClientBuilder.defaultClient();

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


            String outputMessage = "Task handled successfully";


            sendTaskSuccess(taskToken, outputMessage);

            return "Task handled successfully";

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
}
