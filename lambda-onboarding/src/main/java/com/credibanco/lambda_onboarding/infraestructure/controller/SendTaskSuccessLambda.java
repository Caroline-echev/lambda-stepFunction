package com.credibanco.lambda_onboarding.infraestructure.controller;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.AWSStepFunctionsClientBuilder;
import com.amazonaws.services.stepfunctions.model.SendTaskSuccessRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class SendTaskSuccessLambda implements RequestHandler<Map<String, Object>, String> {

    private final AWSStepFunctions stepFunctionsClient = AWSStepFunctionsClientBuilder.defaultClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String handleRequest(Map<String, Object> event, Context context) {
        String taskToken = (String) event.get("TaskToken");

        if (taskToken == null || taskToken.isEmpty()) {
            context.getLogger().log("TaskToken is missing or empty.");
            return "Task failed: Missing or empty TaskToken";
        }

        Map<String, Object> output = Map.of("Payload", Map.of("key", "hola mundo"));

        try {
            String outputJson = objectMapper.writeValueAsString(output);

            SendTaskSuccessRequest request = new SendTaskSuccessRequest()
                    .withTaskToken(taskToken)
                    .withOutput(outputJson);

            stepFunctionsClient.sendTaskSuccess(request);
            context.getLogger().log("Task success sent with TaskToken: " + taskToken);

            return "Task success sent with output: " + outputJson;

        } catch (Exception e) {
            context.getLogger().log("Error: " + e.getMessage());
            return "Task failed: " + e.getMessage();
        }
    }
}