package org.egov.pt.service.issuefix;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.egov.pt.models.Assessment;
import org.egov.pt.models.AssessmentSearchCriteria;
import org.egov.pt.models.Property;
import org.egov.pt.models.PropertyCriteria;
import org.egov.pt.models.enums.Status;
import org.egov.pt.models.issuefix.IssueFix;
import org.egov.pt.models.issuefix.IssueFixRequest;
import org.egov.pt.repository.AssessmentRepository;
import org.egov.pt.service.AssessmentService;
import org.egov.pt.service.PropertyService;
import org.egov.pt.validator.IssueFixValidator;
import org.egov.pt.web.contracts.AssessmentRequest;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;

@Service("assesmentCreateWithoutWorkflowIssueFix")
@Slf4j
public class AssesmentCreateWithoutWorkflowIssueFix implements IIssueFixService {

    @Autowired
    private AssessmentService assessmentService;

    @Autowired
    private PropertyService propertyService;

    @Autowired
    private AssessmentRepository assessmentRepository;
    
    @Autowired
    private IssueFixValidator issueFixValidator;

    @Override
    public IssueFix issueFix(IssueFixRequest issueFixRequest) {
        log.info("Starting issueFix process for IssueFixRequest: {}", issueFixRequest);
        issueFixValidator.validateIssueFixRequest(issueFixRequest);

        PropertyCriteria propertyCriteria = buildPropertyCriteria(issueFixRequest);
        List<Property> properties = propertyService.searchPropertyPlainSearch(propertyCriteria, issueFixRequest.getRequestInfo());
        log.info("Properties found: {}", properties);

        issueFixValidator.validateProperties(properties);

        AssessmentSearchCriteria assessmentCriteria = buildAssessmentSearchCriteria(issueFixRequest);
        List<Assessment> assessments = assessmentRepository.getAssessments(assessmentCriteria);
        log.info("Assessments found: {}", assessments);

        issueFixValidator.validateAssessments(assessments);

        AssessmentRequest assessmentRequest = buildAssessmentRequest(issueFixRequest);
        log.info("Creating assessment with AssessmentRequest: {}", assessmentRequest);
        assessmentService.createAssessment(assessmentRequest, true);

        log.info("IssueFix process completed successfully for IssueFixRequest: {}", issueFixRequest);
        return issueFixRequest.getIssueFix();
    }


    private PropertyCriteria buildPropertyCriteria(IssueFixRequest issueFixRequest) {
        return PropertyCriteria.builder()
                .tenantId(issueFixRequest.getIssueFix().getTenantId())
                .propertyIds(Collections.singleton(issueFixRequest.getIssueFix().getPropertyId()))
                .build();
    }

    private AssessmentSearchCriteria buildAssessmentSearchCriteria(IssueFixRequest issueFixRequest) {
        return AssessmentSearchCriteria.builder()
                .tenantId(issueFixRequest.getIssueFix().getTenantId())
                .status(Status.INWORKFLOW)
                .propertyIds(Collections.singleton(issueFixRequest.getIssueFix().getPropertyId()))
                .build();
    }

    private AssessmentRequest buildAssessmentRequest(IssueFixRequest issueFixRequest) {
        return AssessmentRequest.builder()
                .requestInfo(issueFixRequest.getRequestInfo())
                .assessment(issueFixRequest.getIssueFix().getAssessment())
                .build();
    }


}
