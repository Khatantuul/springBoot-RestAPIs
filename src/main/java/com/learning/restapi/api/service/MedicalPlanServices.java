package com.learning.restapi.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.restapi.api.exceptions.AlreadyExistsException;
import com.learning.restapi.api.exceptions.NotFoundException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.util.*;

@Service
public class MedicalPlanServices {

    private final RedisTemplate<String, Object> redisTemplate;
    ObjectMapper objectMapper;
    //we can use objectmapper from jackson without creating a custom bean, it provides a default objectmapper to
    //use inside the code

    @Autowired
    public MedicalPlanServices(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> getPlan(String id) {
        String plankey = "plan:" + id;

        try {
            Boolean doesExist = redisTemplate.hasKey(plankey);
            if (doesExist != null && !doesExist) {
                throw new NotFoundException("Plan not found!");
            } else if (doesExist == null) {
                throw new NullPointerException();
            }

//            find plan

            Map<Object, Object> plan = redisTemplate.opsForHash().entries(plankey);
            // Convert the Map<Object, Object> to Map<String, Object>
            Map<String, Object> planMap = new HashMap<>();
            plan.forEach((k, v) -> planMap.put(k.toString(), v));

//            find planCostShares

            String planCostSharesKey = plankey + ":" + "planCostShares";
            System.out.println("planCostSharesKey -> " + planCostSharesKey);
            List<Object> planCostSharesValues = redisTemplate.opsForList().range(planCostSharesKey, 0, -1);
            for (Object pointer : planCostSharesValues) {
                Map<Object, Object> planCostShares_membercostshare = redisTemplate.opsForHash().entries((String) pointer);
                Map<String, Object> planCostSharesMap = new HashMap<>();
                planCostShares_membercostshare.forEach((k, v) -> planCostSharesMap.put(k.toString(), v));
                planMap.put("planCostShares", planCostSharesMap);
            }

//            find linkedPlanServices

            String linkedPlanServicesKey = plankey + ":" + "linkedPlanServices";
            List<Object> linkedPlanServicesValues = redisTemplate.opsForList().range(linkedPlanServicesKey, 0, -1);
            List<Map<String, Object>> populatedList = new ArrayList<>();

            for (Object pointer : linkedPlanServicesValues) {
                Map<Object, Object> linkedPlanServices_planservice = redisTemplate.opsForHash().entries((String) pointer);
                Map<String, Object> planserviceMap = new HashMap<>();
                linkedPlanServices_planservice.forEach((k, v) -> planserviceMap.put(k.toString(), v));

//                find linkedService
                String linkedServiceKey = pointer + ":" + "linkedService";
                List<Object> linkedServiceValues = redisTemplate.opsForList().range(linkedServiceKey, 0, -1);
                for (Object linkedServicePointer : linkedServiceValues) {
                    Map<Object, Object> planservice_linkedService = redisTemplate.opsForHash().entries((String) linkedServicePointer);
                    Map<String, Object> linkedServiceMap = new HashMap<>();
                    planservice_linkedService.forEach((k, v) -> linkedServiceMap.put(k.toString(), v));
                    planserviceMap.put("linkedService", linkedServiceMap);
                }

//                find planserviceCostShares
                String planserviceCostSharesKey = pointer + ":" + "planserviceCostShares";
                List<Object> planserviceCostSharesValues = redisTemplate.opsForList().range(planserviceCostSharesKey, 0, -1);
                for (Object planserviceCostSharesPointer : planserviceCostSharesValues) {
                    Map<Object, Object> planserviceCostShares = redisTemplate.opsForHash().entries((String) planserviceCostSharesPointer);
                    Map<String, Object> planserviceCostSharesMap = new HashMap<>();
                    planserviceCostShares.forEach((k, v) -> planserviceCostSharesMap.put(k.toString(), v));
                    planserviceMap.put("planserviceCostShares", planserviceCostSharesMap);

                }
                populatedList.add(planserviceMap);
            }
            planMap.put("linkedPlanServices", populatedList);
            return planMap;
        } catch (Exception e) {
            throw e;
        }
    }


    public Map<String, Object> savePlan(Map<String, Object> plan) throws Exception {
        try {

            String planKey = "plan:" + plan.get("objectId");
            Boolean planExists = redisTemplate.hasKey(planKey);

            if (planExists != null && planExists) {
                throw new AlreadyExistsException("Plan already exists!");
            } else if (planExists == null) {
                throw new IllegalStateException("Plan check returned null");
            }

            Map<String, Object> planCollectedSimpleProps = new HashMap<>();
            Map<String, Object> planserviceCollectedSimpleProps = new HashMap<>();
            for (Map.Entry<String, Object> entry : plan.entrySet()) {
                String propertyName = entry.getKey();
                Object propertyValue = entry.getValue();

                if (propertyValue instanceof Map) {

                    String nodeKey = addNode((Map<String, Object>) propertyValue);
                    String relationKey0 = planKey + ":" + propertyName;
                    addRelation(relationKey0, nodeKey);
                } else if (propertyValue instanceof List) {
                    List<Map<String, Object>> linkedPlanServices = (List<Map<String, Object>>) propertyValue;
                    String relationKey1 = planKey + ":" + propertyName;
                    for (Map<String, Object> item : linkedPlanServices) {
                        String objectItemKey = item.get("objectType") + ":" + item.get("objectId");
                        for (Map.Entry<String, Object> entry1 : item.entrySet()) {
                            String propertyName1 = entry1.getKey();
                            Object propertyValue1 = entry1.getValue();
                            if (propertyValue1 instanceof Map) {
                                String nodeKey1 = addNode((Map<String, Object>) propertyValue1);
                                String relationKey2 = objectItemKey + ":" + propertyName1;
                                addRelation(relationKey2, nodeKey1);
                            } else {
                                planserviceCollectedSimpleProps.put(propertyName1, propertyValue1);
                            }
                        }
                        String nodeKey2 = addNode(objectItemKey, planserviceCollectedSimpleProps);
                        addRelation(relationKey1, nodeKey2);
                    }
                } else {
                    planCollectedSimpleProps.put(propertyName, propertyValue);
                }
            }
            addNode(planKey, planCollectedSimpleProps);
            return plan;
        } catch (Exception e) {
            System.out.println("Error in saving the plan -> " + e.getMessage());
            throw e;
        }
    }


    public void deletePlan(String id) {
//        find plan
        String planKey = "plan:" + id;

//        find planCostShares
        String planCostSharesKey = planKey + ":" + "planCostShares";
        List<Object> planCostSharesValues = redisTemplate.opsForList().range(planCostSharesKey, 0, -1);
        for (Object pointer : planCostSharesValues) {
            redisTemplate.delete((String) pointer);
        }

//        find linkedPlanServices -> only the relation
        String linkedPlanServicesKey = planKey + ":" + "linkedPlanServices";

        List<String> keysToDelete = Arrays.asList(planKey, planCostSharesKey, linkedPlanServicesKey);
        redisTemplate.delete(keysToDelete);
    }


    public void saveEtag(String id, String etag) {
        String plankey = "plan:" + id;
        String etagkey = "etag:" + id;
        redisTemplate.opsForHash().put(plankey, etagkey, etag);
    }

    public String getEtag(String id) {
        String plankey = "plan:" + id;
        String etagkey = "etag:" + id;

        Boolean planExists = redisTemplate.hasKey(plankey);
        if (planExists != null && planExists) {
            return (String) redisTemplate.opsForHash().get(plankey, etagkey);
        } else {
            throw new NotFoundException("Plan not found!");
        }
    }

    public boolean hasKey(String id) throws Exception {

        try {
            String plankey = "plan:" + id;
            return redisTemplate.hasKey(plankey);
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Internal Server Error: " + e.getMessage());
        }
    }

    public String generateTag(Map<String, Object> mapObj) {

        try {
            byte[] serializedData = this.objectMapper.writeValueAsBytes(mapObj);
            //Method that can be used to serialize any Java value as a byte array.
            //	writeValueAsString(Object value)
            //Method that can be used to serialize any Java value as a String. (json)

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashOfMapObj = digest.digest(serializedData);
            String hexString = bytesToHexString(hashOfMapObj);
            return "\"" + hexString + "\""; // Enclose in double quotes for a strong ETag

        } catch (Exception e) {
            throw new RuntimeException("Error generating Etag", e);
        }
    }

    private String bytesToHexString(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private String addNode(Map<String, Object> nodeValue) {
        String nodeKey = nodeValue.get("objectType") + ":" + nodeValue.get("objectId");
        redisTemplate.opsForHash().putAll(nodeKey, nodeValue);
        return nodeKey;
    }

    private String addNode(String key, Map<String, Object> nodeValue) {
        String nodeKey = key;
        redisTemplate.opsForHash().putAll(nodeKey, nodeValue);
        return nodeKey;
    }

    private void addRelation(String relationKey, String relatedNodeKey) {
        redisTemplate.opsForList().leftPush(relationKey, relatedNodeKey);
    }


    private void updateLinkedPlanServices(String linkedPlanServicesKey, Map<String, Object> patch) throws AlreadyExistsException {

        List<Map<String,Object>> patchplanservices = (List<Map<String,Object>>) patch.get("linkedPlanServices");
        List<Object> linkedPlanServicesValues = redisTemplate.opsForList().range(linkedPlanServicesKey, 0, -1);

        for (Map<String,Object> patchplanservice : patchplanservices){
            String patchplanserviceKey = patchplanservice.get("objectType") + ":" + patchplanservice.get("objectId");
            Boolean patchplanserviceExists = redisTemplate.hasKey(patchplanserviceKey);

            Boolean shouldAddplanservice = Boolean.FALSE;
            for (Object pointer : linkedPlanServicesValues) {
                String pointerStr = (String) pointer;
                if (pointerStr.equals(patchplanserviceKey)) {
//                    already among linkedPlanServices array
                    throw new AlreadyExistsException("Plan already has this plan service!");
                } else {
                    shouldAddplanservice = Boolean.TRUE;
                }
            }
            if (shouldAddplanservice && patchplanserviceExists) {
                redisTemplate.opsForList().leftPush(linkedPlanServicesKey,patchplanserviceKey);
            } else if (shouldAddplanservice && !patchplanserviceExists) {
                Map<String, Object> collectedSimpleProps = new HashMap<>();

                    for (Map.Entry<String, Object> entry : patchplanservice.entrySet()) {
                        String propertyName = entry.getKey();
                        Object propertyValue = entry.getValue();

                        if (propertyValue instanceof Map) {

                            String nodeKey = addNode((Map<String, Object>) propertyValue);
                            String relationKey = patchplanserviceKey + ":" + propertyName;
                            addRelation(relationKey, nodeKey);
                        } else {
                            collectedSimpleProps.put(propertyName, propertyValue);
                        }
                    }
                    addNode(patchplanserviceKey, collectedSimpleProps);
                    redisTemplate.opsForList().leftPush(linkedPlanServicesKey,patchplanserviceKey);
                }

            }
        }

       private void updatePlanCostShares(String planCostSharesKey, Map<String,Object> patch){

            List<Object> planCostSharesValues = redisTemplate.opsForList().range(planCostSharesKey,0,-1);
            String planCostShares_membercostshareKey = (String) planCostSharesValues.get(0);
           Map<String,Object> planCostShares = new HashMap<>();
           Map<Object,Object> planCostSharesFetched = redisTemplate.opsForHash().entries(planCostShares_membercostshareKey);
           planCostSharesFetched.forEach((k, v) -> planCostShares.put(k.toString(), v));

           Map<String,Object> patchplanCostShares = (Map<String,Object>) patch.get("planCostShares");

           for (Map.Entry<String, Object> entry : patchplanCostShares.entrySet()){
               String propertyName = entry.getKey();
               Object propertyValue = entry.getValue();


               Object oldvalue = planCostShares.get(propertyName);
//               Map<String,Object> newPlanCostShares = new HashMap<>();

               if (oldvalue != propertyValue){
                   redisTemplate.opsForHash().put(planCostShares_membercostshareKey,propertyName,propertyValue);
               }

           }
       }

    private void updatePlan(String planKey, Map<String,Object> patch){
        Map<String,Object> plan = new HashMap<>();
        Map<Object,Object> planFetched = redisTemplate.opsForHash().entries(planKey);
        planFetched.forEach((k, v) -> plan.put(k.toString(), v));

        for (Map.Entry<String, Object> entry : patch.entrySet()){
            String propertyName = entry.getKey();
            Object propertyValue = entry.getValue();
            Object oldvalue = plan.get(propertyName);

            if (oldvalue != propertyValue){
                redisTemplate.opsForHash().put(planKey,propertyName,propertyValue);
            }

        }

    }



    public Map<String,Object> patchtoPlan(String id, Map<String,Object> patch) throws AlreadyExistsException {
    String planKey = "plan:" + id;
    for (String key : patch.keySet()){
        if (key.equals("planCostShares")){
            String planCostSharesKey = planKey + ":" + key;
            updatePlanCostShares(planCostSharesKey,patch);
        }else if (key.equals("linkedPlanServices")) {
            String linkedPlanServicesKey = planKey + ":" + key;
            updateLinkedPlanServices(linkedPlanServicesKey,patch);
        }else{
            updatePlan(planKey,patch);
        }

    }

    Map<String,Object> updatedPlan = getPlan(id);
    return updatedPlan;
}


}


