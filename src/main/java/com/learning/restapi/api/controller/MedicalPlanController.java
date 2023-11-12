package com.learning.restapi.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.restapi.api.exceptions.AlreadyExistsException;
import com.learning.restapi.api.exceptions.NotFoundException;
import com.learning.restapi.api.service.JwtVerificationService;
import com.learning.restapi.api.service.MedicalPlanServices;
import com.learning.restapi.validator.JsonValidator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/plans")
public class MedicalPlanController {

    private MedicalPlanServices planServices;
    private final JsonValidator jsonValidator;
    private ObjectMapper objectMapper;

    public MedicalPlanController(ObjectMapper objectMapper, MedicalPlanServices planServices, JsonValidator jsonValidator) {
        this.planServices = planServices;
        this.jsonValidator = jsonValidator;
        this.objectMapper = objectMapper;
    }


    @GetMapping("/{objectId}")
    public ResponseEntity<Map<String, Object>> getPlan(@PathVariable String objectId, @RequestHeader(name = HttpHeaders.IF_NONE_MATCH, required = false) String ifnonematch) {
        try {
            Map<String, Object> plan = planServices.getPlan(objectId);
            String theEtag = planServices.getEtag(objectId);

            if (theEtag.equals(ifnonematch)) {
                return ResponseEntity.status(304).build();
            } else {
                if (plan != null) {
                    return ResponseEntity.ok().eTag(theEtag).body(plan);
                } else {
                    return ResponseEntity.notFound().build();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", e.getMessage()));
        }
    }


    @PostMapping(consumes = "application/json")
    public ResponseEntity<Map<String, Object>> addPlan(@RequestBody Map<String, Object> jsonData) {

        String postMappingSchemaPath = "schema/medicalPlan-schema.json";
        List<String> validationMessages = jsonValidator.validateJson(jsonData, postMappingSchemaPath);

        if (validationMessages != null) {
            return ResponseEntity.badRequest().body(Map.of("error", validationMessages));
        }

        try {
            Map<String, Object> savedPlan = planServices.savePlan(jsonData);
            String etag = planServices.generateTag(savedPlan);
            System.out.println(etag);
            planServices.saveEtag((String) jsonData.get("objectId"), etag);


            HttpHeaders headers = new HttpHeaders();
            headers.setETag(etag);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .headers(headers)
                    .body(savedPlan);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (AlreadyExistsException e) {
            return ResponseEntity.status(409).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{objectId}")
    public ResponseEntity<Map<String, Object>> deletePlan(@PathVariable String objectId, @RequestHeader(name = HttpHeaders.IF_MATCH, required = false) String ifmatch) {

        try {

            String etag = planServices.getEtag(objectId);

            if (ifmatch != null && ifmatch.equals(etag)) {
                planServices.deletePlan(objectId);
                return ResponseEntity.noContent().build();
            } else {
                return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).build();
            }

        } catch (NotFoundException nfe) {
            return ResponseEntity.status(404).body(Map.of("message", nfe.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

    }

    @PatchMapping("/{objectId}")
    public ResponseEntity<Map<String, Object>> updatePlan(@PathVariable String objectId, @RequestHeader(name = HttpHeaders.IF_MATCH, required = true) String ifmatch, @RequestBody Map<String, Object> patch) throws Exception {

        try {
            String etag = planServices.getEtag(objectId);

            if (ifmatch != null && ifmatch.equals(etag)) {

                for (String key : patch.keySet()) {
                    if (key.equals("planCostShares")) {
                        String patchPlanCostSharesSchemaPath = "schema/medicalPlan_planCostShares-schema.json";
                        List<String> validationMessages = jsonValidator.validateJson(patch, patchPlanCostSharesSchemaPath);

                        if (validationMessages != null) {
                            return ResponseEntity.badRequest().body(Map.of("error", validationMessages));
                        }
                    } else if (key.equals("linkedPlanServices")) {
                        String patchLinkedPlanServicesSchemaPath = "schema/medicalPlan_linkedPlanServices-schema.json";
                        List<String> validationMessages = jsonValidator.validateJson(patch, patchLinkedPlanServicesSchemaPath);

                        if (validationMessages != null) {
                            return ResponseEntity.badRequest().body(Map.of("error", validationMessages));
                        }
                    } else {
                        String patchPlanSchemaPath = "schema/medicalPlan_plan-schema.json";
                        List<String> validationMessages = jsonValidator.validateJson(patch, patchPlanSchemaPath);

                        if (validationMessages != null) {
                            return ResponseEntity.badRequest().body(Map.of("error", validationMessages));
                        }
                    }
                }

                Map<String, Object> updatedPlan = planServices.patchtoPlan(objectId, patch);

                String newEtag = planServices.generateTag(updatedPlan);

                planServices.saveEtag((String) objectId, newEtag);
                HttpHeaders headers = new HttpHeaders();
                headers.setETag(newEtag);
                return ResponseEntity.status(HttpStatus.CREATED)
                        .headers(headers)
                        .body(updatedPlan);
            } else {
                return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).body(Map.of("message", "Etag not provided"));
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(Map.of("Plan service exists", e.getMessage()));

        }
    }

}


