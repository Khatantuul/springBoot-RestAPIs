package com.learning.restapi.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class JsonValidator {

    public List<String> validateJson(Map<String, Object> jsonData, String schemaPath) {
        try {
            JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
            InputStream schemaInputStream = getClass().getClassLoader().getResourceAsStream(schemaPath);
            JsonSchema schema = schemaFactory.getSchema(schemaInputStream);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.valueToTree(jsonData);

            Set<ValidationMessage> validationResult = schema.validate(jsonNode);

            List<String> errorMessages = new ArrayList<>();
            if (!validationResult.isEmpty()) {
                for (ValidationMessage error : validationResult) {
                    errorMessages.add(error.getMessage());
                }
                return errorMessages;
            }

            return null;

        } catch (Exception e) {
            e.printStackTrace();
            return List.of("Internal server error: " + e.getMessage());
        }
    }
}
