package org.egov.bpa.config;

import java.util.Map;
import java.util.TimeZone;

import javax.annotation.PostConstruct;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Component
public class BPAConfiguration {

	@Value("${app.timezone}")
	private String timeZone;

	@PostConstruct
	public void initialize() {
		TimeZone.setDefault(TimeZone.getTimeZone(timeZone));
	}
	
    @Value("${dev.mode}")
    private boolean devMode;
	
	// User Config
	@Value("${egov.user.host}")
	private String userHost;

	@Value("${egov.user.context.path}")
	private String userContextPath;

	@Value("${egov.user.create.path}")
	private String userCreateEndpoint;

	@Value("${egov.user.search.path}")
	private String userSearchEndpoint;

	@Value("${egov.user.update.path}")
	private String userUpdateEndpoint;

	@Value("${egov.user.username.prefix}")
	private String usernamePrefix;

	// Idgen Config
	@Value("${egov.idgen.host}")
	private String idGenHost;

	@Value("${egov.idgen.path}")
	private String idGenPath;

	@Value("${egov.idgen.bpa.applicationNum.name}")
	private String applicationNoIdgenName;

	@Value("${egov.idgen.bpa.applicationNum.format}")
	private String applicationNoIdgenFormat;

	@Value("${egov.idgen.bpa.permitNum.name}")
	private String permitNoIdgenName;

	@Value("${egov.idgen.bpa.permitNum.format}")
	private String permitNoIdgenFormat;

	// Persister Config
	@Value("${persister.save.buildingplan.topic}")
	private String saveTopic;

	@Value("${persister.update.buildingplan.topic}")
	private String updateTopic;

	@Value("${persister.update.buildingplan.workflow.topic}")
	private String updateWorkflowTopic;

	@Value("${persister.update.buildingplan.adhoc.topic}")
	private String updateAdhocTopic;

	@Value("${persister.update.permit.letter.preview}")
	private String updatePermitLetterPreview;
	
	// Location Config
	@Value("${egov.location.host}")
	private String locationHost;

	@Value("${egov.location.context.path}")
	private String locationContextPath;

	@Value("${egov.location.endpoint}")
	private String locationEndpoint;

	@Value("${egov.location.hierarchyTypeCode}")
	private String hierarchyTypeCode;

	@Value("${egov.bpa.default.limit}")
	private Integer defaultLimit;

	@Value("${egov.bpa.default.offset}")
	private Integer defaultOffset;

	@Value("${egov.bpa.max.limit}")
	private Integer maxSearchLimit;

	// EDCR Service
	@Value("${egov.edcr.host}")
	private String edcrHost;

	@Value("${egov.edcr.authtoken.endpoint}")
	private String edcrAuthEndPoint;

	@Value("${egov.edcr.getPlan.endpoint}")
	private String getPlanEndPoint;

	// Institutional key word
	@Value("${egov.ownershipcategory.institutional}")
	private String institutional;

	@Value("${egov.receipt.businessservice}")
	private String businessService;
	
	@Value("${egov.receipt.regularization.businessservice}")
	private String regularizationBusinessService;
	

	@Value("${egov.receipt.demolition.businessservice}")
	private String demolitionBusinessService;
	
	// Property Service
	@Value("${egov.property.service.host}")
	private String propertyHost;

	@Value("${egov.property.service.context.path}")
	private String propertyContextPath;

	@Value("${egov.property.endpoint}")
	private String propertySearchEndpoint;

	// SMS
	@Value("${kafka.topics.notification.sms}")
	private String smsNotifTopic;

	@Value("${notification.sms.enabled}")
	private Boolean isSMSEnabled;

	// Localization
	@Value("${egov.localization.host}")
	private String localizationHost;

	@Value("${egov.localization.context.path}")
	private String localizationContextPath;

	@Value("${egov.localization.search.endpoint}")
	private String localizationSearchEndpoint;

	@Value("${egov.localization.statelevel}")
	private Boolean isLocalizationStateLevel;

	// Calculator
	@Value("${egov.bpa.calculator.host}")
	private String calculatorHost;

	@Value("${egov.bpa.calculator.calculate.endpoint}")
	private String calulatorEndPoint;

	@Value("${egov.billingservice.host}")
	private String billingHost;

	@Value("${egov.demand.search.endpoint}")
	private String demandSearchEndpoint;

	// MDMS
	@Value("${egov.mdms.host}")
	private String mdmsHost;

	@Value("${egov.mdms.search.endpoint}")
	private String mdmsEndPoint;

	// Allowed Search Parameters
	@Value("${citizen.allowed.search.params}")
	private String allowedCitizenSearchParameters;

	@Value("${employee.allowed.search.params}")
	private String allowedEmployeeSearchParameters;

	@Value("${egov.tl.previous.allowed}")
	private Boolean isPreviousTLAllowed;

	@Value("${egov.tl.min.period}")
	private Long minPeriod;

	// Workflow
	@Value("${create.bpa.workflow.name}")
	private String businessServiceValue;

	@Value("${create.bpa.low.workflow.name}")
	private String lowBusinessServiceValue;

	@Value("${workflow.context.path}")
	private String wfHost;

	@Value("${workflow.transition.path}")
	private String wfTransitionPath;

	@Value("${workflow.businessservice.search.path}")
	private String wfBusinessServiceSearchPath;

	@Value("${workflow.process.path}")
	private String wfProcessPath;

	@Value("${is.external.workflow.enabled}")
	private Boolean isExternalWorkFlowEnabled;

	// USER EVENTS
	@Value("${egov.ui.app.host}")
	private String uiAppHost;

	@Value("${egov.usr.events.create.topic}")
	private String saveUserEventsTopic;

	@Value("${egov.usr.events.pay.link}")
	private String payLink;

	@Value("${egov.usr.events.pay.code}")
	private String payCode;

	@Value("${egov.user.event.notification.enabled}")
	private Boolean isUserEventsNotificationEnabled;

	@Value("${egov.usr.events.pay.triggers}")
	private String payTriggers;

	@Value("${egov.collection.service.host}")
	private String collectionServiceHost;

	@Value("${egov.collection.service.search.endpoint}")
	private String collectionServiceSearchEndPoint;

	@Value("${egov.bpa.validity.date.in.months}")
	private Integer validityInMonths;
	
	//landInfo
	
	@Value("${egov.landinfo.host}")
	private String landInfoHost;
	
	@Value("${egov.landinfo.create.endpoint}")
	private String landInfoCreate;
	
	@Value("${egov.landinfo.update.endpoint}")
	private String landInfoUpdate;
	
	@Value("${egov.landinfo.search.endpoint}")
	private String landInfoSearch;
	
	@Value("${persister.save.landinfo.topic}")
	private String saveLandInfoTopic;
	
	@Value("${persister.update.landinfo.topic}")
	private String updateLandInfoTopic;
	
	@Value("#{${appSrvTypeBussSrvCode}}")
	private Map<String,Map<String,String>> appSrvTypeBussSrvCode;
	
	@Value("${egov.bpa.skippayment.status}")
	private String skipPaymentStatuses;
	
	@Value("${egov.noc.service.host}")
	private String nocServiceHost;
	
	@Value("${egov.noc.create.endpoint}")
	private String nocCreateEndpoint;
	
	@Value("${egov.noc.update.endpoint}")
	private String nocUpdateEndpoint;
	
	@Value("${egov.noc.search.endpoint}")
	private String nocSearchEndpoint;
	
	@Value("${validate.required.nocs}")
	private Boolean validateRequiredNoc;
	
	@Value("${validate.required.nocs.statuses}")
	private String nocValidationCheckStatuses;
	
	@Value("${egov.noc.initiate.action}")
	private String nocInitiateAction;
	
	@Value("${egov.noc.void.action}")
	private String nocVoidAction;
	
	@Value("${egov.noc.autoapprove.action}")
	private String nocAutoApproveAction;
	
	@Value("${egov.noc.autoapproved.state}")
	private String nocAutoApprovedState;
	
	@Value("${egov.noc.approved.state}")
	private String nocApprovedState;
  
//	action and status constants
	@Value("${egov.sendtocitizen.action}")
	private String actionsendtocitizen;
	
	@Value("${egov.inprogress.action}")
	private String actioninprogress;
	
	@Value("${egov.approve.action}")
	private String actionapprove;
	
	@Value("${egov.pendingapplfee.stsus}")
	private String statuspendingapplfee;
	
	@Value("${egov.inprogress.stsus}")
	private String statusinprogress;
	

	@Value("#{${nocSourceConfig}}")
	private Map<String,String> nocSourceConfig;

	@Value("#{${workflowStatusFeeBusinessSrvMap}}")
	private Map<String,Map<String,String>> workflowStatusFeeBusinessSrvMap;
	
	// Email
	@Value("${notification.email.enabled}")
	private Boolean isEmailNotificationEnabled;
	
	@Value("${kafka.topics.notification.email}")
	private String emailNotifTopic;
	
	@Value("${notification.email.subject}")
	private String emailSubject;
	
	@Value("${notification.email.regularization.subject}")
	private String regularizationEmailSubject;
	
	@Value("${notification.email.demolition.subject}")
	private String demolitionEmailSubject;
	
	//digital certificate integration
	@Value("${persister.update.bpa.dscdetails.topic}")
	private String updateDscDetailsTopic;
	
	@Value("${persister.update.bpa.plan.dscdetails.topic}")
	private String updatePlanDscDetailsTopic;
	
	@Value("${egov.bpa.calculator.estimate.endpoint}")
	private String bpaCalculationEstimateEndpoint;
	
	@Value("${egov.filestore.host}")
	private String filestoreHost;
	
	@Value("${egov.filestore.fetch.path}")
	private String filestoreFetchPath;
	
	@Value("${egov.filestore.upload.path}")
	private String filestoreUploadPath;
	
	@Value("${persister.save.preapprovedplan.topic}")
	private String savePreApprovedPlanTopicName;

	@Value("${egov.idgen.bpa.drawingNum.name}")
	private String drawingNoIdGenName;

	@Value("${egov.idgen.bpa.drawingNum.format}")
	private String drawingNoIdGenFormat;

	@Value("${persister.update.preapprovedplan.topic}")
	private String updatePreApprovedPlanTopicName;
	
	@Value("${persister.save.notice.topic}")
	private String savenoticeTopicName;
	
	@Value("${persister.update.notice.topic}")
	private String updatenoticeTopicName;
	
	@Value("${egov.bpa.calculator.installments.search.endpoint}")
	private String fetchAllInstallmentsEndpoint;
	
	@Value("${egov.bpa.calculator.installments.create.endpoint}")
	private String createInstallmentsEndpoint;
	
	@Value("${egov.bpa.calculator.installments.generateDemands.endpoint}")
	private String generateDemandsFromInstallmentsEndpoint;
	
	@Value("${egov.bpa.enableInstallmentOnApproval:false}")
	private boolean enableInstallmentOnApproval;
	
	@Value("${persister.update.bpa.installment}")
	private String updateInstallmentTopic;

	@Value("${egov.idgen.bpa.applicationNum.BPA5name}")
	private String applicationNoIdgenNameforBPA5;
	
	@Value("${egov.idgen.bpa.applicationNum.BPA5format}")
	private String applicationNoIdgenFormatforBPA5;
	
	@Value("${egov.idgen.bpa.applicationNum.BPA6name}")
	private String applicationNoIdgenNameforBPA6;
	
	@Value("${egov.idgen.bpa.applicationNum.BPA6format}")
	private String applicationNoIdgenFormatforBPA6;
	
	@Value("${persister.save.revision.topic}")
	private String saveRevisionTopicName;
	
	@Value("${persister.update.revision.topic}")
	private String updateRevisionTopicName;
	
	@Value("${persister.save.accreditedperson.topic}")
	private String saveAccreditedPersonTopicName;
	
	@Value("${persister.save.fieldinspection.topic}")
	private String savefieldinspectionTopicName;
	
//	@Value("${egov.bpa.scheduler.edcrdatapull.batchsize}")
//	private String edcrDataPullBatchSize;
	
//	@Value("${egov.bpa.scheduler.edcrdatapull.createtopicname}")
//	private String edcrDataCreateTopicName;
//	
//	@Value("${egov.bpa.scheduler.edcrdatapull.user.uuid}")
//	private String edcrDataPullUserUUID;
//	
//	@Value("${egov.bpa.scheduler.edcrdatapull.user.type}")
//	private String edcrDataPullUserType;
	
	@Value("${bpa.auto.escalation.user.uuid}")
	private String bpaAutoEscalationUserUuid;
	
	@Value("${bpa.auto.escalation.user.type}")
	private String bpaAutoEscalationUserType;
	
	@Value("${egov.hrms.host}")
    private String hrmshost;
    
    @Value("${egov.hrms.context.path}")
    private String hrmsContextPath;
    
    @Value("${egov.hrms.employee.search.path}")
    private String hrmsSearchEndpoint;
    
    @Value("${bpa.auto.escalation.cronexpression}")
    private String bpaAutoEscalationCronexpression;
    
    @Value("${bpa.auto.escalation.notification.reminder.after.day}")
    private int bpaAutoEscalationRemainderAfterDay;

    @Value("${bpa.auto.escalation.escalate.after.day}")
    private int bpaAutoEscalationEscalateAfterDay;
    
    @Value("${bpa.auto.showcause.notice.generate.after.day}")
    private int bpaShowcauseNoticeGenerateAfterDay;
    
    @Value("${egov.pdf.host}")
    private String pdfhost;
    
    @Value("${egov.pdf.context.path}")
    private String pdfContextPath;
    
    @Value("${egov.pdf.path}")
    private String pdfSearchEndpoint;
    
    @Value("${egov.idgen.bpa.letterNum.name}")
    private String letterNoIdgenName;
    
    @Value("${egov.idgen.bpa.letterNum.format}")
    private String letterNoIdgenFormat;
    
    @Value("${egov.idgen.bpa.rscn.letterNum.format}")
    private String refusalLetterNoIdgenFormat;
    
    @Value("${persister.save.DocRemark.topic}")
    private String saveDocRemarkTopicName;

    @Value("${persister.update.DocRemark.topic}")
    private String updateDocRemarkTopicName;
    
    @Value("${bpa.showcause.notice.enable}")
    private boolean bpaShowcauseNoticeEnabled;
    
    @Value("${bpa.auto.escalation.enable}")
    private boolean bpaAutoescalationEnabled;
    
    @Value("${bpa.showcause.notice.revoke.period}")
    private int bpaNoticeRevokePeriod;
    
	@Value("${egov.filestore.fetch.path.new}")
	private String filestoreFetchPathNew;
	
	@Value("${persister.save.revalidation.topic}")
	private String saveRevalidationTopicName;

	@Value("${persister.update.revalidation.topic}")
	private String updateRevalidationTopicName;
	
	@Value("${egov.idgen.bpa.applicationNum.BPARVname}")
	private String applicationNoIdgenNameforBPARV;

	@Value("${egov.idgen.bpa.applicationNum.BPARVformat}")
	private String applicationNoIdgenFormatforBPARV;
	
	@Value("${bpa.auto.escalation.schedular.enable}")
	private boolean bpaAutoEscalationSchedularEnable;
	
	@Value("${egov.bpa.regularization.format}")
	private String bpaRegularizationFormat;
	
	@Value("${egov.bpa.regularization.name}")
	private String bpaRegularizationName;
	
	@Value("${persister.save.regularization.topic}")
	private String saveRegularizationTopic;
	
	@Value("${egov.bpa.regularization.permit.format}")
	private String regularizationPermitFormat;

	@Value("${egov.bpa.regularization.permit.name}")
	private String regularizationPermitName;

	@Value("${persister.update.regularization.topic}")
	private String updateRegularizationTopic;
	
	@Value("${persister.update.regularization.dscdetails.topic}")
	private String updateRegularizationDscDetailsTopic;
	
	@Value("${egov.bpa.calculator.regularization.estimate.endpoint}")
	private String regularizationEsimateEndPoint;
	
	@Value("${egov.bpa.calculator.regularization.calculator.endpoint}")
	private String regularizationCalculateEndPoint;
	
	@Value("${persister.save.regularization.notice.topic}")
	private String saveRegularizationNotice;
	
	@Value("${persister.update.regularization.notice.topic}")
	private String updateRegularizationNotice;
	
	@Value("${egov.bpa.regularization.notice.format}")
	private String regularizationNoticeFormat;
	
	@Value("${egov.bpa.regularization.notice.name}")
	private String regularizationNoticeName;
	
	@Value("#{${egov.receipt.regularization.status.businessService.map}}")
	private Map<String,String> regularizationStatusBusinessServiceMap;
	
	@Value("${persister.regularization.update.permit.letter.preview}")
	private String updateRegularizationPermitLetterPreview;
	
	@Value("${persister.save.regularization.fieldinspection.topic}")
	private String saveRegularizationFieldInspectionTopicName;
	
	@Value("${persister.save.regularization.docremark.topic}")
	private String saveRegularizationDocRemarkTopic;

    @Value("${persister.update.regularization.docremark.topic}")
    private String updateRegularizationDocRemarkTopic;

    @Value("${persister.save.document.upload.topic}")
    private String documentUploadTopic;
    
    @Value("${persister.update.document.upload.topic}")
    private String documentUpdateTopic;
    
    @Value("${persister.save.bpa.planningassistant.checklist.topic}")
	private String savePlanningAssistantChecklistTopicName;
    
    @Value("${persister.update.bpa.planningassistant.checklist.topic}")
	private String updatePlanningAssistantChecklistTopicName;
    
    @Value("${bpa.payment.issuefix}")
	private Boolean bpaPaymentIssuefix;
    
    @Value("${bpa.status.mismatch.issuefix}")
	private Boolean bpaStatusMismatchIssuefix;
    
	@Value("${bpa.issuefix.rolecode}")
	private String bpaIssueFixRoleCode;

	@Value("${bpa.issuefix.tenantid}")
	private String bpaIssueFixTenantId;

	@Value("${bpa.issue.resolver.uuid}")
	private String bpaIssueFixUUID;
	
	@Value("${regularization.payment.issuefix}")
	private Boolean regularizationPaymentIssuefix;
    
    @Value("${regularization.status.mismatch.issuefix}")
	private Boolean regularizationStatusMismatchIssuefix;
    
    @Value("${persister.save.bpa.plinth.approval.topic}")
	private String savePlinthApprovalTopic;
    
    @Value("${persister.update.bpa.plinth.approval.topic}")
	private String updatePlinthApprovalTopic;
    
    @Value("${egov.idgen.plinth.applicationNum.name}")
	private String plinthApplName;
    
    @Value("${egov.idgen.plinth.applicationNum.format}")
	private String plinthApplNumFormat;

    @Value("${persister.update.fieldinspection.topic}")
	private String updateFieldInspectionTopicName;
    
    @Value("${persister.update.regularization.fieldinspection.topic}")
	private String updateRegularizationFieldInspectionTopicName;
    
    @Value("${persister.save.demolition.topic}")
    private String saveDemolitionTopic;
    
    @Value("${persister.update.demolition.topic}")
    private String updateDemolitionTopic;
    
    @Value("${egov.bpa.demolition.format}")
    private String demolitionApplicationFormat;
    
    @Value("${egov.bpa.demolition.permit.format}")
    private String demolitionApplicationName;
    
    @Value("${egov.bpa.demolition.permit.format}")
    private String demolitionPermitFormat;
    
    @Value("${egov.bpa.demolition.permit.name}")
    private String demolitionPermitName;
    
    @Value("${egov.bpa.calculator.demolition.estimate.endpoint}")
    private String demolitionEstimateEndpoint;
    
    @Value("${egov.bpa.calculator.demolition.calculator.endpoint}")
    private String demolitionCalculationEndpoint;  
    
    @Value("${persister.demolition.update.permit.letter.preview}")
	private String updateDemolitionPermitLetterPreview;
    
    @Value("${persister.save.regl.document.upload.topic}")
    private String documentRegularizationUploadTopic;  
    
    @Value("${persister.update.regl.document.upload.topic}")
	private String documentRegularizationUpdateTopic;
    
    @Value("${bpa.refusal.scn.enable}")
	private Boolean bpaRefusalShowCauseNoticeEnable;
    
    @Value("${employee.notification.sms.enabled}")
   	private Boolean employeeSmsEnable;

    @Value("${persister.bpa.savedraft.topic}")
   	private String bpaSaveDraftTopic;   
    
    @Value("${persister.regularization.savedraft.topic}")
   	private String regularizationSaveDraftTopic;  
    
    @Value("${egov.regularization.draft.format}")
	private String regularizationDraftFormat;
    
    @Value("${egov.oc.draft.format}")
	private String ocDraftFormat;
    
    @Value("${egov.idgen.completioncertificate.applicationNum.name}")
	private String completionCertificateApplName;
    
    @Value("${egov.idgen.completioncertificate.applicationNum.format}")
	private String completionCertificateNumFormat;
    
	@Value("${persister.save.completion.certificate.topic}")
	private String saveCompletionCertificateTopic;

	@Value("${persister.update.completion.certificate.topic}")
	private String updateCompletionCertificateTopic;
	
	@Value("${persister.save.statewise.report.topic}")
	private String saveStateWiseReportTopic;

	@Value("${persister.update.statewise.report.topic}")
	private String updateStateWiseReportTopic;
	
	@Value("${stagewise.report.max.filestore.limit}")
	private Integer stageWiseReportMaxFilestoreLimit;
	
	@Value("${bpa.stagewise.report.filestoreid}")
	private String stageWiseReportFilestoreId;
	
	@Value("${max.count.pullback.action.limit}")
	private Integer maxCountPullBackActionLimit;	
	
	@Value("${persister.save.demolition.fieldinspection.topic}")
	private String saveDemolitionFieldInspectionTopicName;
	
	@Value("${persister.update.demolition.fieldinspection.topic}")
	private String updateDemolitionFieldInspectionTopicName;
	
	@Value("${egov.igr.host}")
	private String igrHost;
}
