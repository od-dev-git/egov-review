package org.egov.pt.validator;

import org.egov.pt.models.Assessment;
import org.egov.pt.models.Property;
import org.egov.pt.models.enums.Channel;
import org.egov.pt.models.enums.Status;
import org.egov.pt.models.workflow.ProcessInstance;
import org.egov.pt.util.IssueFixConstants;
import org.egov.pt.models.issuefix.IssueFix;
import org.egov.pt.models.issuefix.IssueFixRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.egov.pt.util.PTConstants;
import org.egov.tracer.model.CustomException;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@Slf4j
public class IssueFixValidator {

    public void validateIssueFix(IssueFixRequest issueFixRequest) {

        log.info("Validating Issue Fix Requests ...");
        if (StringUtils.isEmpty(issueFixRequest.getIssueFix().getIssueName())) {
            throw new CustomException("EMPTY_ISSUE_NAME", "Issue Name passed is null or empty");
        }

        if (StringUtils.isEmpty(issueFixRequest.getIssueFix().getApplicationNo())
                && StringUtils.isEmpty(issueFixRequest.getIssueFix().getPropertyId()) && StringUtils.isEmpty(issueFixRequest.getIssueFix().getAssessmentNo())) {
            throw new CustomException("INVALID_DATA", "Application Number, Property Id, Assessment No  passed both can't be empty");
        }
        
        if (StringUtils.isEmpty(issueFixRequest.getIssueFix().getTenantId())) {
            throw new CustomException("INVALID_DATA", "Tenant Id passed is null or empty");
        }
    }

    public void validateProcessInstanceApplicationStatusMismatch(Property property, List<ProcessInstance> processInstance) {
        if (CollectionUtils.isEmpty(processInstance)) {
            throw new CustomException("INVALID_DATA", "No Data was found in Process Instances");
        }
        ProcessInstance currentProcessInstance = processInstance.get(0);
        if (currentProcessInstance.getState().getApplicationStatus().equalsIgnoreCase(property.getStatus().toString())) {
            throw new CustomException("INVALID_INPUT", "The Application data is having no mismatch");
        }
    }


    public void validatePropertySearch(List<Property> propertyList) {
        if (CollectionUtils.isEmpty(propertyList)) {
            throw new CustomException("NO_DATA_FOUND", "No Property was found with the given criteria");
        }
        if (propertyList.size() >= 2) {
            throw new CustomException("INVALID_DATA", "Multiple Properties were found");
        }
    }

    public Property validatePropertySearchForInactiveProperty(List<Property> propertyList) {
        if (CollectionUtils.isEmpty(propertyList)) {
            throw new CustomException("NO_DATA_FOUND", "No Property was found with the given criteria");
        }
        checkForActiveOrInWorkflowProperties(propertyList);
        List<Property> inactivePropertyList=filterOutInactiveApplications(propertyList);
        if(CollectionUtils.isEmpty(inactivePropertyList)){
            throw new CustomException("INVALID_PROPERTY_ID","No Inactive Applications were found");
        }
        return propertyList.get(0);
    }

    private List<Property> filterOutInactiveApplications(List<Property> propertyList) {
        return propertyList.stream().filter(property -> property.getStatus().equals(Status.INACTIVE))
                .sorted(Comparator.comparing(property -> property.getAuditDetails().getCreatedTime(),Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    private void checkForActiveOrInWorkflowProperties(List<Property> propertyList) {
        propertyList.forEach(property -> {
            if(property.getStatus().equals(Status.ACTIVE)||property.getStatus().equals(Status.INWORKFLOW)){
                throw new CustomException("INVALID_PROPERTY_ID","An Active or In Progress application was already found against this Property ID");
            }
        });
    }

    public void validatePaymentIssueRequest(IssueFixRequest issueFixRequest) {

        IssueFix issueFix = issueFixRequest.getIssueFix();
        if (org.springframework.util.StringUtils.isEmpty(issueFix.getApplicationNo())) {
            throw new CustomException("INVALID_REQUEST",
                    "Application Number is mandatory for Payment Issues, Kindly provide application number to proceed !!");
        }
    }

    public void checkIfStatusIsAtPaymentStage(Property property) {

        if(!property.getStatus().toString().equalsIgnoreCase(IssueFixConstants.PENDING_FOR_PAYMENT)) {
            throw new CustomException("APPLICATION_STATUS_ERROR",
                    "Application is not at Pending Payment Stage. Kindly Check !!");
        }
    }

    public void validateRequestForInactiveIssue(IssueFix issueFix) {

        if (StringUtils.isEmpty(issueFix.getPropertyId())){
            throw new CustomException("INVALID_INPUT","Property Id passed can't be null or empty for Inactive issue fix");
        }
    }

	public void validateAssessmentSearch(List<Assessment> assessmentList) {
		if (CollectionUtils.isEmpty(assessmentList)) {
            throw new CustomException("NO_DATA_FOUND", "No assesment was found with the given criteria");
        }
        if (assessmentList.size() >= 2) {
            throw new CustomException("INVALID_DATA", "Multiple assesment were found");
        }
		
	}

	public void validateAssesmentProcessInstanceApplicationStatusMismatch(Assessment assessment,
			List<ProcessInstance> processInstance) {
		if (CollectionUtils.isEmpty(processInstance)) {
            throw new CustomException("INVALID_DATA", "No Data was found in Process Instances");
        }
        ProcessInstance currentProcessInstance = processInstance.get(0);
        if (currentProcessInstance.getState().getApplicationStatus().equalsIgnoreCase(assessment.getStatus().toString())) {
            throw new CustomException("INVALID_INPUT", "The Assesment data is having no mismatch");
        }
		
	}

	public void validateAssessmentIssueFixRequest(IssueFixRequest issueFixRequest) {
		log.info("Validating Issue Fix Requests ...");
        if (StringUtils.isEmpty(issueFixRequest.getIssueFix().getIssueName())) {
            throw new CustomException("EMPTY_ISSUE_NAME", "Issue Name passed is null or empty");
        }

        if (StringUtils.isEmpty(issueFixRequest.getIssueFix().getAssessmentNo())) {
            throw new CustomException("INVALID_DATA", "Assessment No  passed can't be empty");
        }
		
	}
	
	public void validateIssueFixRequest(IssueFixRequest issueFixRequest) {
        log.info("Validating IssueFixRequest: {}", issueFixRequest);
        validateBasicFields(issueFixRequest);

        Assessment assessmentFromRequest = issueFixRequest.getIssueFix().getAssessment();
        validateAssessmentFields(assessmentFromRequest);

        JsonNode additionalDetails = assessmentFromRequest.getAdditionalDetails();
        validateAdditionalDetails(additionalDetails);
    }

    public void validateBasicFields(IssueFixRequest issueFixRequest) {
        log.info("Validating basic fields for IssueFixRequest");

        String tenantId = issueFixRequest.getIssueFix().getTenantId();
        String propertyId = issueFixRequest.getIssueFix().getPropertyId();

        // Validate non-null fields for tenant ID and property ID
        validateNonNullField(tenantId, "Tenant ID");
        validateNonNullField(propertyId, "Property ID");

        // Get the Assessment object from the request
        Assessment assessmentFromRequest = issueFixRequest.getIssueFix().getAssessment();

        // Validate that the Assessment object is not null
        if (assessmentFromRequest == null) {
            log.error("Validation failed: Assessment object cannot be null");
            throw new CustomException("INVALID_REQUEST", "Assessment object cannot be null");
        }

        // Validate matching tenant ID and property ID fields
        validateMatchingFields(tenantId, assessmentFromRequest.getTenantId(), "Tenant ID in request must match");
        validateMatchingFields(propertyId, assessmentFromRequest.getPropertyId(), "Property IDs in request must match");

        // Additional validation for the Assessment fields
        validateAssessmentFields(assessmentFromRequest);
    }


    private void validateAssessmentFields(Assessment assessment) {
        log.info("Validating fields in Assessment from request");

        validateNonNullField(assessment.getTenantId(), "Tenant ID");
        validateNonNullField(assessment.getPropertyId(), "Property ID");
        validateNonNullField(assessment.getFinancialYear(), "Financial year");
        validateNonNullField(assessment.getAssessmentDate(), "Assessment date");

        // Validate "source"
        Assessment.Source sourceObj = assessment.getSource();
        if (sourceObj == null) {
            throw new IllegalArgumentException("Source cannot be null");
        }
        String source = sourceObj.toString();
        validateNonNullField(source, "Source");
        validateSource(source);

        // Validate "channel"
        Channel channelObj = assessment.getChannel();
        if (channelObj == null) {
            throw new IllegalArgumentException("Channel cannot be null");
        }
        String channel = channelObj.toString();
        validateNonNullField(channel, "Channel");
        validateChannel(channel);

        // Validate "skipWorkflow"
        Boolean skipWorkflow = assessment.getSkipWorkflow();
        validateNonNullField(skipWorkflow, "Skip workflow flag");
        validateSkipWorkflow(skipWorkflow);
    }


    public void validateProperties(List<Property> properties) {
        log.info("Validating properties: {}", properties);
        if (CollectionUtils.isEmpty(properties)) {
            throw new CustomException("INVALID_REQUEST", "No properties found for the given criteria");
        }

        long activeCount = properties.stream().filter(property -> property.getStatus().equals(Status.ACTIVE)).count();
        if (activeCount != 1) {
            throw new CustomException("INVALID_REQUEST", "There should be exactly one active property.");
        }

        properties.forEach(property -> {
            if (property.getStatus().equals(Status.INWORKFLOW)) {
                throw new CustomException("INVALID_REQUEST", "The property has another property in workflow");
            }
        });
    }

    public void validateAssessments(List<Assessment> assessments) {
        log.info("Validating assessments: {}", assessments);
        if (!CollectionUtils.isEmpty(assessments)) {
            throw new CustomException("INVALID_REQUEST", "The property has another assessment in workflow");
        }
    }


    public void validateAdditionalDetails(JsonNode additionalDetails) {
        log.info("Validating additional details: {}", additionalDetails);
        if (Objects.isNull(additionalDetails)) {
            throw new CustomException("INVALID_REQUEST", "Additional details cannot be null");
        }

        String[] keysToValidate = {"penalty", "interest", "lightTax", "waterTax", "otherDues", "holdingTax",
                "latrineTax", "parkingTax", "serviceTax", "drainageTax", "totalAmount", "usageExemption",
                "ownershipExemption", "solidWasteUserCharges"};

        for (String key : keysToValidate) {
            validateNonNullAndNumeric(additionalDetails, key);
        }
    }

    public void validateNonNullField(Object field, String fieldName) {
        if (Objects.isNull(field)) {
            log.error("Validation failed: {} cannot be null", fieldName);
            throw new CustomException("INVALID_REQUEST", fieldName + " cannot be null");
        }
    }

    public void validateMatchingFields(Object field1, Object field2, String errorMessage) {
        if (!Objects.equals(field1, field2)) {
            log.error("Validation failed: {}", errorMessage);
            throw new CustomException("INVALID_REQUEST", errorMessage);
        }
    }

    public void validateNonNullAndNumeric(JsonNode jsonNode, String key) {
        validateNonNull(jsonNode, key);
        String value = jsonNode.get(key).asText();
        validateNumericValue(value, key);
    }

    public void validateNonNull(JsonNode jsonNode, String key) {
        if (!jsonNode.has(key) || jsonNode.get(key).asText().isEmpty()) {
            log.error("Validation failed: {} cannot be null or empty", key);
            throw new CustomException("INVALID_REQUEST", key + " cannot be null or empty");
        }
    }

    public void validateNumericValue(String value, String key) {
        try {
            BigDecimal numericValue = new BigDecimal(value);

            // Check for non-negative values
            if (numericValue.compareTo(BigDecimal.ZERO) < 0) {
                throw new CustomException("INVALID_REQUEST", key + " must be a non-negative number");
            }

            // Check precision and scale (allowing max 2 decimal places)
            if (numericValue.scale() > 2) {
                throw new CustomException("INVALID_REQUEST", key + " must not have more than 2 decimal places");
            }

            if (numericValue.precision() - numericValue.scale() > 8) {
                throw new CustomException("INVALID_REQUEST", key + " exceeds maximum allowed value");
            }

        } catch (NumberFormatException e) {
            throw new CustomException("INVALID_REQUEST", key + " must be a valid number");
        }
    }
    
    public void validateSkipWorkflow(Boolean skipWorkflow) {
        log.info("Validating skipWorkflow: " + skipWorkflow);
        if (!Boolean.TRUE.equals(skipWorkflow)) {
            throw new CustomException("INVALID_REQUEST", "Skip workflow must be true");
        }
    }

    
    public void validateChannel(String channel) {
        log.info("Validating channel: " + channel);
        if (!"CFC_COUNTER".equals(channel)) {
            throw new CustomException("INVALID_REQUEST", "Channel must be 'CFC_COUNTER'");
        }
    }

    public void validateSource(String source) {
        log.info("Validating source: " + source);
        if (!"MUNICIPAL_RECORDS".equals(source)) {
            throw new CustomException("INVALID_REQUEST", "Source must be 'MUNICIPAL_RECORDS'");
        }
    }


}
