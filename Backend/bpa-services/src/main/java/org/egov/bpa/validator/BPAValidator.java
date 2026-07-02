package org.egov.bpa.validator;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.apache.commons.lang.StringUtils;
import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.repository.IssueFixRepository;
import org.egov.bpa.repository.ScnRepository;
import org.egov.bpa.service.DocRemarkService;
import org.egov.bpa.service.EDCRService;
import org.egov.bpa.service.NocService;
import org.egov.bpa.service.PreapprovedPlanService;
import org.egov.bpa.util.BPAConstants;
import org.egov.bpa.util.BPAErrorConstants;
import org.egov.bpa.util.BPAUtil;
import org.egov.bpa.util.IssueFixConstants;
import org.egov.bpa.web.model.AuditDetails;
import org.egov.bpa.web.model.BPA;
import org.egov.bpa.web.model.BPADraft;
import org.egov.bpa.web.model.BPADraftRequest;
import org.egov.bpa.web.model.BPADraftSearchCriteria;
import org.egov.bpa.web.model.BPARequest;
import org.egov.bpa.web.model.BPASearchCriteria;
import org.egov.bpa.web.model.CompletionCertificate;
import org.egov.bpa.web.model.CompletionCertificateRequest;
import org.egov.bpa.web.model.CompletionCertificateSearchCriteria;
import org.egov.bpa.web.model.DocRemark;
import org.egov.bpa.web.model.DocRemarkSearchCriteria;
import org.egov.bpa.web.model.DocUploadRequest;
import org.egov.bpa.web.model.Document;
import org.egov.bpa.web.model.DscDetails;
import org.egov.bpa.web.model.FieldInspection;
import org.egov.bpa.web.model.Installment;
import org.egov.bpa.web.model.InstallmentSearchCriteria;
import org.egov.bpa.web.model.Notice;
import org.egov.bpa.web.model.NoticeSearchCriteria;
import org.egov.bpa.web.model.PlanningAssistantChecklist;
import org.egov.bpa.web.model.PlinthApproval;
import org.egov.bpa.web.model.PreapprovedPlan;
import org.egov.bpa.web.model.PreapprovedPlanSearchCriteria;
import org.egov.bpa.web.model.StageWiseReport;
import org.egov.bpa.web.model.StageWiseReportRequest;
import org.egov.bpa.web.model.VillageSearchCriteria;
import org.egov.bpa.web.model.NOC.Noc;
import org.egov.bpa.web.model.collection.Demand;
import org.egov.bpa.web.model.collection.DemandSearchCriteria;
import org.egov.bpa.web.model.collection.Payment;
import org.egov.bpa.web.model.collection.PaymentSearchCriteria;
import org.egov.bpa.web.model.workflow.ProcessInstance;
import org.egov.bpa.workflow.WorkflowService;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class BPAValidator {

	@Autowired
	private MDMSValidator mdmsValidator;

	@Autowired
	private BPAConfiguration config;
	
	@Autowired
	private PreapprovedPlanService preapprovedPlanService;
	
	@Autowired
	private EDCRService edcrService;

	@Autowired
	private BPAUtil bpaUtil;
	
	@Autowired
	private NocService nocService;
	
	@Autowired
	private IssueFixRepository repository;
	
	@Autowired
	private ScnRepository scnRepository;
	
	@Autowired
	private DocRemarkService docRemarkService;
	
	@Autowired
	private WorkflowService workflowService;
	
	public void validateCreate(BPARequest bpaRequest, Object mdmsData, Map<String, String> values) {
		mdmsValidator.validateMdmsData(bpaRequest, mdmsData);
		validateApplicationDocuments(bpaRequest, mdmsData, null, values);
		validateOwnerDetails(bpaRequest);
	}


	/**
	 * Validates the application documents of the BPA comparing the document types configured in the mdms
	 * @param request
	 * @param mdmsData
	 * @param currentState
	 * @param values
	 */
	private void validateApplicationDocuments(BPARequest request, Object mdmsData, String currentState, Map<String, String> values) {
		Map<String, List<String>> masterData = mdmsValidator.getAttributeValues(mdmsData);
		BPA bpa = request.getBPA();

		if (!bpa.getWorkflow().getAction().equalsIgnoreCase(BPAConstants.ACTION_REJECT)
				&& !bpa.getWorkflow().getAction().equalsIgnoreCase(BPAConstants.ACTION_ADHOC)
				&& !bpa.getWorkflow().getAction().equalsIgnoreCase(BPAConstants.ACTION_PAY) 
				&& !bpa.getWorkflow().getAction().equalsIgnoreCase(BPAConstants.ACTION_INITIATE)) {

			String applicationType = values.get(BPAConstants.APPLICATIONTYPE);
			String serviceType = values.get(BPAConstants.SERVICETYPE);

			String filterExp = "$.[?(@.applicationType=='" + applicationType + "' && @.ServiceType=='" + serviceType
					+ "' && @.RiskType=='" + bpa.getRiskType() + "' && @.WFState=='" + currentState + "')].docTypes";
			
			List<Object> docTypeMappings = JsonPath.read(masterData.get(BPAConstants.DOCUMENT_TYPE_MAPPING), filterExp);

			List<Document> allDocuments = new ArrayList<Document>();
			if (bpa.getDocuments() != null) {
				allDocuments.addAll(bpa.getDocuments());
			}

			if (CollectionUtils.isEmpty(docTypeMappings)) {
				return;
			}

			filterExp = "$.[?(@.required==true)].code";
			List<String> requiredDocTypes = JsonPath.read(docTypeMappings.get(0), filterExp);
			if (!request.getBPA().getBusinessService().equalsIgnoreCase(BPAConstants.BPA_PAP_MODULE_CODE)) {
				if (request.getEdcrResponse().get(BPAConstants.LIFTCOUNTRELAXATION) != null && request.getEdcrResponse().get(BPAConstants.LIFTCOUNTRELAXATION).equalsIgnoreCase("YES")) {
					if (request.getBPA().getBusinessService().equalsIgnoreCase(BPAConstants.BPA_FI_ARCH_MODULE_CODE)) {
						requiredDocTypes.add(BPAConstants.BPA7_LIFTCOUNTRELAXATIONCERT_CERT);
					} else {
						requiredDocTypes.add(BPAConstants.BPD_LIFTCOUNTRELAXATIONCERT_CERT);
					}
				}
			}
			List<String> validDocumentTypes = masterData.get(BPAConstants.DOCUMENT_TYPE);

			if (!CollectionUtils.isEmpty(allDocuments)) {

				allDocuments.forEach(document -> {

					if (!validDocumentTypes.contains(document.getDocumentType())) {
						throw new CustomException(BPAErrorConstants.BPA_UNKNOWN_DOCUMENTTYPE,
								document.getDocumentType() + " is Unkown");
					}
				});

				if (!bpa.getBusinessService().equalsIgnoreCase(BPAConstants.BPA_PAP_MODULE_CODE) && requiredDocTypes.size() > 0 && allDocuments.size() < requiredDocTypes.size() ) {

					throw new CustomException(BPAErrorConstants.BPA_MDNADATORY_DOCUMENTPYE_MISSING,
							BPAErrorConstants.BPA_UNKNOWN_DOCS_MSG);
				} else if (requiredDocTypes.size() > 0) {

					List<String> addedDocTypes = new ArrayList<String>();
					allDocuments.forEach(document -> {

						String docType = document.getDocumentType();
						int lastIndex = docType.lastIndexOf(".");
						String documentNs = "";
						if (lastIndex > 1) {
							documentNs = docType.substring(0, lastIndex);
						} else if (lastIndex == 1) {
							throw new CustomException(BPAErrorConstants.BPA_INVALID_DOCUMENTTYPE,
									document.getDocumentType() + " is Invalid");
						} else {
							documentNs = docType;
						}

						addedDocTypes.add(documentNs);
					});
					requiredDocTypes.forEach(docType -> {
						String docType1 = docType.toString();
						if (!addedDocTypes.contains(docType1) && !bpa.getBusinessService().equalsIgnoreCase(BPAConstants.BPA_PAP_MODULE_CODE)) {
							if(docType1.equalsIgnoreCase(BPAConstants.BPD_LIFTCOUNTRELAXATIONCERT_CERT)) {
								throw new CustomException(BPAErrorConstants.BPA_MDNADATORY_DOCUMENTPYE_MISSING,
										"This document is mandatory if the applicant has declared in the plan info that they want to avail lift count relaxation");
							}
							throw new CustomException(BPAErrorConstants.BPA_MDNADATORY_DOCUMENTPYE_MISSING,
									"Document Type " + docType1 + " is Missing");
						}
					});
				}
			} else if (requiredDocTypes.size() > 0) {
				throw new CustomException(BPAErrorConstants.BPA_MDNADATORY_DOCUMENTPYE_MISSING,
						"Atleast " + requiredDocTypes.size() + " Documents are requied ");
			}
			bpa.setDocuments(allDocuments);
		}

	}

	/** 
	 * validate duplicates documents in the bpa request
	 * @param request
	 */
	private void validateDuplicateDocuments(BPARequest request) {
		if (request.getBPA().getDocuments() != null) {
			List<String> documentFileStoreIds = new LinkedList<String>();
			request.getBPA().getDocuments().forEach(document -> {
				if (documentFileStoreIds.contains(document.getFileStoreId()))
					throw new CustomException(BPAErrorConstants.BPA_DUPLICATE_DOCUMENT, "Same document cannot be used multiple times");
				else
					documentFileStoreIds.add(document.getFileStoreId());
			});
		}
	}

	/**
	 * Validates if the search parameters are valid
	 * 
	 * @param requestInfo
	 *            The requestInfo of the incoming request
	 * @param criteria
	 *            The BPASearch Criteria
	 */
//TODO need to make the changes in the data
	public void validateSearch(RequestInfo requestInfo, BPASearchCriteria criteria) {
		if (!requestInfo.getUserInfo().getType().equalsIgnoreCase(BPAConstants.CITIZEN) && criteria.isEmpty())
			throw new CustomException(BPAErrorConstants.INVALID_SEARCH, "Search without any paramters is not allowed");

		if (!requestInfo.getUserInfo().getType().equalsIgnoreCase(BPAConstants.CITIZEN) && !criteria.tenantIdOnly()
				&& criteria.getTenantId() == null)
			throw new CustomException(BPAErrorConstants.INVALID_SEARCH, "TenantId is mandatory in search");

		if (requestInfo.getUserInfo().getType().equalsIgnoreCase(BPAConstants.CITIZEN) && !criteria.isEmpty()
				&& !criteria.tenantIdOnly() && criteria.getTenantId() == null)
			throw new CustomException(BPAErrorConstants.INVALID_SEARCH, "TenantId is mandatory in search");
		
		if ((criteria.isEscalatedToMe() || criteria.isAboutToEscalate()) && requestInfo.getUserInfo().getUuid()==null)
            throw new CustomException(BPAErrorConstants.INVALID_SEARCH, "uuid is mandatory in search");

		String allowedParamStr = null;

		if (requestInfo.getUserInfo().getType().equalsIgnoreCase(BPAConstants.CITIZEN))
			allowedParamStr = config.getAllowedCitizenSearchParameters();
		else if (requestInfo.getUserInfo().getType().equalsIgnoreCase(BPAConstants.EMPLOYEE))
			allowedParamStr = config.getAllowedEmployeeSearchParameters();
		// handling for edcr data pull scheduler search- 
		else if (requestInfo.getUserInfo().getType().equalsIgnoreCase(BPAConstants.SYSTEM))
			allowedParamStr = config.getAllowedEmployeeSearchParameters();
		else
			throw new CustomException(BPAErrorConstants.INVALID_SEARCH,
					"The userType: " + requestInfo.getUserInfo().getType() + " does not have any search config");

		if (StringUtils.isEmpty(allowedParamStr) && !criteria.isEmpty())
			throw new CustomException(BPAErrorConstants.INVALID_SEARCH, "No search parameters are expected");
		else {
			List<String> allowedParams = Arrays.asList(allowedParamStr.split(","));
			validateSearchParams(criteria, allowedParams);
		}
	}

	/**
	 * Validates if the paramters coming in search are allowed
	 * 
	 * @param criteria
	 *            BPA search criteria
	 * @param allowedParams
	 *            Allowed Params for search
	 */
	private void validateSearchParams(BPASearchCriteria criteria, List<String> allowedParams) {

		if (criteria.getApplicationNo() != null && !allowedParams.contains("applicationNo"))
			throw new CustomException(BPAErrorConstants.INVALID_SEARCH, "Search on applicationNo is not allowed");

		if (criteria.getEdcrNumber() != null && !allowedParams.contains("edcrNumber"))
			throw new CustomException(BPAErrorConstants.INVALID_SEARCH, "Search on edcrNumber is not allowed");

		if (criteria.getStatus() != null && !allowedParams.contains("status"))
			throw new CustomException(BPAErrorConstants.INVALID_SEARCH, "Search on Status is not allowed");

		if (criteria.getIds() != null && !allowedParams.contains("ids"))
			throw new CustomException(BPAErrorConstants.INVALID_SEARCH, "Search on ids is not allowed");

		if (criteria.getMobileNumber() != null && !allowedParams.contains("mobileNumber"))
			throw new CustomException(BPAErrorConstants.INVALID_SEARCH, "Search on mobileNumber is not allowed");

		if (criteria.getOffset() != null && !allowedParams.contains("offset"))
			throw new CustomException(BPAErrorConstants.INVALID_SEARCH, "Search on offset is not allowed");

		if (criteria.getLimit() != null && !allowedParams.contains("limit"))
			throw new CustomException(BPAErrorConstants.INVALID_SEARCH, "Search on limit is not allowed");
		
		if (criteria.getApprovalDate() != null && (criteria.getApprovalDate() > new Date().getTime()))
			throw new CustomException(BPAErrorConstants.INVALID_SEARCH, "Permit Order Genarated date cannot be a future date");
		
		if (criteria.getFromDate() != null && (criteria.getFromDate() > new Date().getTime()))
			throw new CustomException(BPAErrorConstants.INVALID_SEARCH, "From date cannot be a future date");

		if (criteria.getToDate() != null && criteria.getFromDate() != null
				&& (criteria.getFromDate() > criteria.getToDate()))
			throw new CustomException(BPAErrorConstants.INVALID_SEARCH, "To date cannot be prior to from date");
	}

	/**
	 * valide the update BPARequest
	 * @param bpaRequest
	 * @param searchResult
	 * @param mdmsData
	 * @param currentState
	 * @param edcrResponse
	 */
	public void validateUpdate(BPARequest bpaRequest, List<BPA> searchResult, Object mdmsData, String currentState, Map<String, String> edcrResponse) {

		BPA bpa = bpaRequest.getBPA();
		validateApplicationDocuments(bpaRequest, mdmsData, currentState, edcrResponse);
		validateAllIds(searchResult, bpa);
		mdmsValidator.validateMdmsData(bpaRequest, mdmsData);
		validateDuplicateDocuments(bpaRequest);
		setFieldsFromSearch(bpaRequest, searchResult, mdmsData);

	}

	/**
	 * set the fields from search response to the bpaRequest for furhter processing
	 * @param bpaRequest
	 * @param searchResult
	 * @param mdmsData
	 */
	private void setFieldsFromSearch(BPARequest bpaRequest, List<BPA> searchResult, Object mdmsData) {
		Map<String, BPA> idToBPAFromSearch = new HashMap<>();

		searchResult.forEach(bpa -> {
			idToBPAFromSearch.put(bpa.getId(), bpa);
		});

		bpaRequest.getBPA().getAuditDetails()
				.setCreatedBy(idToBPAFromSearch.get(bpaRequest.getBPA().getId()).getAuditDetails().getCreatedBy());
		bpaRequest.getBPA().getAuditDetails()
				.setCreatedTime(idToBPAFromSearch.get(bpaRequest.getBPA().getId()).getAuditDetails().getCreatedTime());
		bpaRequest.getBPA().setStatus(idToBPAFromSearch.get(bpaRequest.getBPA().getId()).getStatus());
	}



	/**
	 * Validate the ids of the search results
	 * @param searchResult
	 * @param bpa
	 */
	private void validateAllIds(List<BPA> searchResult, BPA bpa) {

		Map<String, BPA> idToBPAFromSearch = new HashMap<>();
		searchResult.forEach(bpas -> {
			idToBPAFromSearch.put(bpas.getId(), bpas);
		});

		Map<String, String> errorMap = new HashMap<>();
		BPA searchedBpa = idToBPAFromSearch.get(bpa.getId());

		if (!searchedBpa.getApplicationNo().equalsIgnoreCase(bpa.getApplicationNo()))
			errorMap.put("INVALID UPDATE", "The application number from search: " + searchedBpa.getApplicationNo()
					+ " and from update: " + bpa.getApplicationNo() + " does not match");

		if (!searchedBpa.getId().equalsIgnoreCase(bpa.getId()))
			errorMap.put("INVALID UPDATE", "The id " + bpa.getId() + " does not exist");




		if (!CollectionUtils.isEmpty(errorMap))
			throw new CustomException(errorMap);
	}




	/**
	 * validate the fields inspection checlist data populated by the user against the mdms
	 * @param mdmsData
	 * @param bpaRequest
	 * @param wfState
	 */
	public void validateCheckList(Object mdmsData, BPARequest bpaRequest, String wfState) {
		BPA bpa = bpaRequest.getBPA();
		String businessServices = bpaRequest.getBPA().getBusinessService(); 
        
	     
		Map<String, String> edcrResponse = new HashMap<>();
		if (StringUtils.isNotEmpty(businessServices) && "BPA6".equals(businessServices)) {
			//System.out.println("inside this");
			getEdcrDetailsForPreapprovedPlan(edcrResponse,bpaRequest);
		}else if(bpaRequest.getEdcrResponse() != null && !CollectionUtils.isEmpty(bpaRequest.getEdcrResponse())) {
			edcrResponse = bpaRequest.getEdcrResponse();
		} else {
			edcrResponse = edcrService.getEDCRDetails(bpaRequest.getRequestInfo(), bpaRequest.getBPA());
		}
		bpaRequest.setEdcrResponse(edcrResponse);
		
		log.debug("applicationType is " + edcrResponse.get(BPAConstants.APPLICATIONTYPE));
        log.debug("serviceType is " + edcrResponse.get(BPAConstants.SERVICETYPE));
        
		// allow validation of questions and checklist documents only for
		// field-inspection forward action,
		// as getting error when sendback action performed from field-inspection
        
        //Now onwards seprate api will validate these documents
//		if (BPAConstants.FI_STATUS.equalsIgnoreCase(bpa.getStatus()) && Objects.nonNull(bpa.getWorkflow())
//				&& StringUtils.isNotEmpty(bpa.getWorkflow().getAction())
//				&& BPAConstants.ACTION_FORWORD.equalsIgnoreCase(bpa.getWorkflow().getAction())) {
//			//validateQuestions(mdmsData, bpa, wfState, edcrResponse);
//			validateFIDocTypes(mdmsData, bpa, wfState, edcrResponse);
//		}
		
	}

	private void getEdcrDetailsForPreapprovedPlan(Map<String, String> edcrResponse, BPARequest bpaRequest) {
		
		PreapprovedPlanSearchCriteria preapprovedPlanSearchCriteria = new PreapprovedPlanSearchCriteria();
		preapprovedPlanSearchCriteria.setDrawingNo(bpaRequest.getBPA().getEdcrNumber());
		List<PreapprovedPlan> preapprovedPlans = preapprovedPlanService
				.getPreapprovedPlanFromCriteria(preapprovedPlanSearchCriteria);
		if (CollectionUtils.isEmpty(preapprovedPlans)) {
			log.error("no preapproved plan found for provided drawingNo:" + bpaRequest.getBPA().getEdcrNumber());
			throw new CustomException("no preapproved plan found for provided drawingNo",
					"no preapproved plan found for provided drawingNo");
		}
		PreapprovedPlan preapprovedPlanFromDb = preapprovedPlans.get(0);
		Map<String, Object> drawingDetail = (Map<String, Object>) preapprovedPlanFromDb.getDrawingDetail();
		
		edcrResponse.put(BPAConstants.SERVICETYPE, drawingDetail.get("serviceType") + "");// NEW_CONSTRUCTION
		edcrResponse.put(BPAConstants.APPLICATIONTYPE, drawingDetail.get("applicationType") + "");// BUILDING_PLAN_SCRUTINY
		
			//	edcrResponse.put(BPAConstants.SERVICETYPE,  "NEW_CONSTRUCTION");// NEW_CONSTRUCTION
				//edcrResponse.put(BPAConstants.APPLICATIONTYPE,   "BUILDING_PLAN_SCRUTINY");// BUILDING_PLAN_SCRUTINY
				
		
	}


	/**
	 * validate the fields insepction report questions agains the MDMS
	 * @param mdmsData
	 * @param bpa
	 * @param wfState
	 * @param edcrResponse
	 */
	@SuppressWarnings(value = { "unchecked", "rawtypes" })
	private void validateQuestions(Object mdmsData, BPA bpa, String wfState, Map<String, String> edcrResponse) {
		List<String> mdmsQns = null;

		log.debug("Fetching MDMS result for the state " + wfState);

		try {
			String questionsPath = BPAConstants.QUESTIONS_MAP.replace("{1}", wfState)
					.replace("{2}", bpa.getRiskType().toString()).replace("{3}", edcrResponse.get(BPAConstants.SERVICETYPE))
					.replace("{4}", edcrResponse.get(BPAConstants.APPLICATIONTYPE));

			List<Object> mdmsQuestionsArray = (List<Object>) JsonPath.read(mdmsData, questionsPath);

			if (!CollectionUtils.isEmpty(mdmsQuestionsArray))
				mdmsQns = JsonPath.read(mdmsQuestionsArray.get(0), BPAConstants.QUESTIONS_PATH);

			log.debug("MDMS questions " + mdmsQns);
			if (!CollectionUtils.isEmpty(mdmsQns)) {
				if (bpa.getAdditionalDetails() != null) {
					List checkListFromReq = (List) ((Map) bpa.getAdditionalDetails()).get(wfState.toLowerCase());
					if (!CollectionUtils.isEmpty(checkListFromReq)) {
						for (int i = 0; i < checkListFromReq.size(); i++) {
							// MultiItem framework adding isDeleted object to
							// additionDetails object whenever report is being
							// removed.
							// So skipping that object validation.
							if (((Map) checkListFromReq.get(i)).containsKey("isDeleted")) {
								checkListFromReq.remove(i);
								i--;
								continue;
							}
							List<Map> requestCheckList = new ArrayList<Map>();
							List<String> requestQns = new ArrayList<String>();
							validateDateTime((Map)checkListFromReq.get(i));
							List<Map> questions = ((Map) checkListFromReq.get(i))
									.get(BPAConstants.QUESTIONS_TYPE) != null
											? (List<Map>) ((Map) checkListFromReq.get(i))
													.get(BPAConstants.QUESTIONS_TYPE)
											: null;
							if (questions != null)
								requestCheckList.addAll(questions);
							if (!CollectionUtils.isEmpty(requestCheckList)) {
								for (Map reqQn : requestCheckList) {
									requestQns.add((String) reqQn.get(BPAConstants.QUESTION_TYPE));
								}
							}

							log.debug("Request questions " + requestQns);

							if (!CollectionUtils.isEmpty(requestQns)) {
								if (requestQns.size() < mdmsQns.size())
									throw new CustomException(BPAErrorConstants.BPA_UNKNOWN_QUESTIONS,
											BPAErrorConstants.BPA_UNKNOWN_QUESTIONS_MSG);
								else {
									List<String> pendingQns = new ArrayList<String>();
									for (String qn : mdmsQns) {
										if (!requestQns.contains(qn)) {
											pendingQns.add(qn);
										}
									}
									if (pendingQns.size() > 0) {
										throw new CustomException(BPAErrorConstants.BPA_UNKNOWN_QUESTIONS,
												BPAErrorConstants.BPA_UNKNOWN_QUESTIONS_MSG);
									}
								}
							} else {
								throw new CustomException(BPAErrorConstants.BPA_UNKNOWN_QUESTIONS,
										BPAErrorConstants.BPA_UNKNOWN_QUESTIONS_MSG);
							}
						}
					} else {
						throw new CustomException(BPAErrorConstants.BPA_UNKNOWN_QUESTIONS, BPAErrorConstants.BPA_UNKNOWN_QUESTIONS_MSG);
					}
				} else {
					throw new CustomException(BPAErrorConstants.BPA_UNKNOWN_QUESTIONS, BPAErrorConstants.BPA_UNKNOWN_QUESTIONS_MSG);
				}
			}
		} catch (PathNotFoundException ex) {
			log.error("Exception occured while validating the Checklist Questions" + ex.getMessage());
		}
	}

	/**
	 * Validate fieldinspection documents and their documentTypes
	 * @param mdmsData
	 * @param bpa
	 * @param wfState
	 * @param edcrResponse
	 */
	@SuppressWarnings(value = { "unchecked", "rawtypes" })
	private void validateFIDocTypes(Object mdmsData, BPA bpa, String wfState, Map<String, String> edcrResponse) {
		List<String> mdmsDocs = null;

		log.debug("Fetching MDMS result for the state " + wfState);

		try {
			String docTypesPath = BPAConstants.DOCTYPES_MAP.replace("{1}", wfState)
					.replace("{2}", bpa.getRiskType().toString()).replace("{3}", edcrResponse.get(BPAConstants.SERVICETYPE))
					.replace("{4}", edcrResponse.get(BPAConstants.APPLICATIONTYPE));;

			List<Object> docTypesArray = (List<Object>) JsonPath.read(mdmsData, docTypesPath);

			if (!CollectionUtils.isEmpty(docTypesArray))
				mdmsDocs = JsonPath.read(docTypesArray.get(0), BPAConstants.DOCTYPESS_PATH);

			log.debug("MDMS DocTypes " + mdmsDocs);
			if (!CollectionUtils.isEmpty(mdmsDocs)) {
				if (bpa.getAdditionalDetails() != null) {
					List checkListFromReq = (List) ((Map) bpa.getAdditionalDetails()).get(wfState.toLowerCase());
					if (!CollectionUtils.isEmpty(checkListFromReq)) {
						for (int i = 0; i < checkListFromReq.size(); i++) {
							List<Map> requestCheckList = new ArrayList<Map>();
							List<String> requestDocs = new ArrayList<String>();
							List<Map> docs = ((Map) checkListFromReq.get(i)).get(BPAConstants.DOCS) != null
									? (List<Map>) ((Map) checkListFromReq.get(i)).get(BPAConstants.DOCS) : null;
							if (docs != null)
								requestCheckList.addAll(docs);
							
							if (!CollectionUtils.isEmpty(requestCheckList)) {
								for (Map reqDoc : requestCheckList) {
									String fileStoreId = ((String) reqDoc.get(BPAConstants.FILESTOREID));
									if (!StringUtils.isEmpty(fileStoreId)) {
										String docType = (String) reqDoc.get(BPAConstants.CODE);
										int lastIndex = docType.lastIndexOf(".");
										String documentNs = "";
										if (lastIndex > 1) {
											documentNs = docType.substring(0, lastIndex);
										} else if (lastIndex == 1) {
											throw new CustomException(BPAErrorConstants.BPA_INVALID_DOCUMENTTYPE,
													(String) reqDoc.get(BPAConstants.CODE) + " is Invalid");
										} else {
											documentNs = docType;
										}
										requestDocs.add(documentNs);
									} else {
										throw new CustomException(BPAErrorConstants.BPA_UNKNOWN_DOCS,
												BPAErrorConstants.BPA_UNKNOWN_DOCS_MSG);
									}
								}
							}

							log.debug("Request Docs " + requestDocs);

							if (!CollectionUtils.isEmpty(requestDocs)) {
								if (requestDocs.size() < mdmsDocs.size())
									throw new CustomException(BPAErrorConstants.BPA_UNKNOWN_DOCS,
											BPAErrorConstants.BPA_UNKNOWN_DOCS_MSG);
								else {
									List<String> pendingDocs = new ArrayList<String>();
									for (String doc : mdmsDocs) {
										if (!requestDocs.contains(doc)) {
											pendingDocs.add(doc);
										}
									}
									if (pendingDocs.size() > 0) {
										throw new CustomException(BPAErrorConstants.BPA_UNKNOWN_DOCS,
												BPAErrorConstants.BPA_UNKNOWN_DOCS_MSG);
									}
								}
							} else {
								throw new CustomException(BPAErrorConstants.BPA_UNKNOWN_DOCS, BPAErrorConstants.BPA_UNKNOWN_DOCS_MSG);
							}
						}
					} else {
						throw new CustomException(BPAErrorConstants.BPA_UNKNOWN_DOCS, BPAErrorConstants.BPA_UNKNOWN_DOCS_MSG);
					}
				} else {
					throw new CustomException(BPAErrorConstants.BPA_UNKNOWN_DOCS, BPAErrorConstants.BPA_UNKNOWN_DOCS_MSG);
				}
			}
		} catch (PathNotFoundException ex) {
			log.error("Exception occured while validating the Checklist Documents" + ex.getMessage());
		}
	}
	
	/**
	 * Validate FieldINpsection report date and time
	 * @param checkListFromRequest
	 */
	private void validateDateTime(@SuppressWarnings("rawtypes") Map checkListFromRequest) {

		if (checkListFromRequest.get(BPAConstants.INSPECTION_DATE) == null
				|| StringUtils.isEmpty(checkListFromRequest.get(BPAConstants.INSPECTION_DATE).toString())) {
			throw new CustomException(BPAErrorConstants.BPA_UNKNOWN_DATE, "Please mention the inspection date");
		} else {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			Date dt;
			try {
				dt = sdf.parse(checkListFromRequest.get(BPAConstants.INSPECTION_DATE).toString());
				long inspectionEpoch = dt.getTime();
				if (inspectionEpoch > new Date().getTime()) {
					throw new CustomException(BPAErrorConstants.BPA_UNKNOWN_DATE, "Inspection date cannot be a future date");
				} else if (inspectionEpoch < 0) {
					throw new CustomException(BPAErrorConstants.BPA_UNKNOWN_DATE, "Provide the date in specified format 'yyyy-MM-dd'");
				}
			} catch (ParseException e) {
				throw new CustomException(BPAErrorConstants.BPA_UNKNOWN_DATE, "Unable to parase the inspection date");
			}
		}
		if (checkListFromRequest.get(BPAConstants.INSPECTION_TIME) == null
				|| StringUtils.isEmpty(checkListFromRequest.get(BPAConstants.INSPECTION_TIME).toString())) {
			throw new CustomException(BPAErrorConstants.BPA_UNKNOWN_TIME, "Please mention the inspection time");
		}
	}

	/**
	 * validate the workflow and the nocapproval stages to move forward
	 * @param bpaRequest
	 * @param mdmsRes
	 */
	public void validatePreEnrichData(BPARequest bpaRequest, Object mdmsRes) {		
		validateSkipPaymentAction(bpaRequest);
		if(!bpaRequest.getBPA().getOCOutsideSujogApplication())
			validateNocApprove(bpaRequest, mdmsRes);
		validateBpaForward(bpaRequest);
	}
	
	/**
	 * Validate workflowActions against the skipPayment 
	 * @param bpaRequest
	 */
	private void validateSkipPaymentAction(BPARequest bpaRequest) {
		BPA bpa = bpaRequest.getBPA();
		if (bpa.getWorkflow().getAction() != null && (bpa.getWorkflow().getAction().equalsIgnoreCase(BPAConstants.ACTION_SKIP_PAY))) {
			BigDecimal demandAmount = bpaUtil.getDemandAmount(bpaRequest);
			if ((demandAmount.compareTo(BigDecimal.ZERO) > 0)) {
				throw new CustomException(BPAErrorConstants.BPA_INVALID_ACTION, "Payment can't be skipped once demand is generated.");
			}
		}
	}
	
	/**
	 * Validates the NOC approval state to move forward the bpa applicaiton
	 * @param bpaRequest
	 * @param mdmsRes
	 */
	@SuppressWarnings("unchecked")
	private void validateNocApprove(BPARequest bpaRequest, Object mdmsRes) {
		BPA bpa = bpaRequest.getBPA();
		log.info("===========> valdiateNocApprove method called");
		log.info("BPA Status : " + bpa.getStatus() + " action" + bpa.getWorkflow().getAction());
		if (config.getValidateRequiredNoc()) {
			if ((bpa.getStatus().equalsIgnoreCase(BPAConstants.APPROVAL_INPROGRESS)
					&& bpa.getWorkflow().getAction().equalsIgnoreCase(BPAConstants.ACTION_APPROVE))
					) {
				log.info("Inside validateRequiredNoc method");	
				String businessService = 	bpa.getBusinessService();
				Map<String, String> edcrResponse = new HashMap<>();
				if(!(businessService.isEmpty()) && businessService.equalsIgnoreCase(BPAConstants.BPA_PAP_MODULE_CODE)) {
					edcrResponse = edcrService.getEdcrDetailsForPreapprovedPlan(edcrResponse, bpaRequest);
				}else if(bpaRequest.getEdcrResponse() != null && !CollectionUtils.isEmpty(bpaRequest.getEdcrResponse())) {
					edcrResponse = bpaRequest.getEdcrResponse();
				} else {
					edcrResponse = edcrService.getEDCRDetails(bpaRequest.getRequestInfo(),
							bpaRequest.getBPA());
				}
				log.info("edcrResponse : "+edcrResponse);
				bpaRequest.setEdcrResponse(edcrResponse);
				
				log.info("===========> valdiateNocApprove method called, application is in noc verification pending");
				String riskType = "ALL";
				if (StringUtils.isEmpty(bpa.getRiskType()) || bpa.getRiskType().equalsIgnoreCase("LOW")) {
					riskType = bpa.getRiskType();
				}
				log.info("fetching NocTypeMapping record having riskType : " + riskType);
				
				String nocPath = BPAConstants.NOCTYPE_REQUIRED_MAP
						.replace("{1}", edcrResponse.get(BPAConstants.APPLICATIONTYPE))
						.replace("{2}", edcrResponse.get(BPAConstants.SERVICETYPE)).replace("{3}", riskType);
				log.info("nocPath : " + nocPath);
				List<Object> nocMappingResponse = (List<Object>) JsonPath.read(mdmsRes, nocPath);
				List<String> nocTypes = JsonPath.read(nocMappingResponse, "$..type");

				log.info("===========> valdiateNocApprove method called, noctypes====",nocTypes);
				List<Noc> nocs = nocService.fetchNocRecords(bpaRequest);
				log.info("Noc : " + nocs);
				if (!CollectionUtils.isEmpty(nocs)) {
					for (Noc noc : nocs) {
						if (!nocTypes.isEmpty() && nocTypes.contains(noc.getNocType())) {
							List<String> statuses = Arrays.asList(config.getNocValidationCheckStatuses().split(","));
							if (!statuses.contains(noc.getApplicationStatus())) {
								log.error("Noc is not approved having applicationNo :" + noc.getApplicationNo());
								throw new CustomException(BPAErrorConstants.NOC_SERVICE_EXCEPTION,
										" Application can't be forwarded without NOC "
												+ StringUtils.join(statuses, " or "));
							}
						}
					}
				} else {
					log.debug("No NOC record found to validate with sourceRefId " + bpa.getApplicationNo());
				}
			}
		}
	}
	
	public void validateDscSearch(BPASearchCriteria criteria, RequestInfo requestInfo) {
		if (StringUtils.isEmpty(criteria.getTenantId()))
			throw new CustomException(BPAErrorConstants.INVALID_SEARCH, "TenantId is mandatory in search");
	}
	
	public void validateDscDetails(BPARequest request, List<BPA> searchResult) {
		Map<String, BPA> idToBpaMapFromSearch = new HashMap<>();
		searchResult.forEach(bpa -> {
			idToBpaMapFromSearch.put(bpa.getId(), bpa);
		});

		BPA bpaFromRequest = request.getBPA();

		BPA bpaFromSearch = idToBpaMapFromSearch.get(bpaFromRequest.getId());

		if (bpaFromSearch == null) {
			throw new CustomException("APPLICATION  ERROR", "The Application does not exist .");
		}
		
		if (!(bpaFromSearch.getStatus().equalsIgnoreCase("APPROVED")
				|| bpaFromSearch.getStatus().equalsIgnoreCase("REJECTED"))) {
			throw new CustomException("APPLICATION STATUS ERROR",
					"The Application should be in approved or rejected state for document signing .");
		}

		List<DscDetails> dscDetailsFromSearch = bpaFromSearch.getDscDetails();
		List<DscDetails> dscDetailsFromRequest = bpaFromRequest.getDscDetails();

		if (!CollectionUtils.isEmpty(dscDetailsFromSearch)) {
			if (dscDetailsFromSearch.size() != 1)
				throw new CustomException("DSC DETAILS ERROR",
						"There should be only one Digitally signed documemts for application .");

			DscDetails searchedDscDetail = dscDetailsFromSearch.get(0);

			if (!StringUtils.isEmpty(searchedDscDetail.getDocumentId()))
				throw new CustomException("DSC DETAILS ERROR", "This application License is already digitally signed.");

		} else
			throw new CustomException("DSC DETAILS ERROR",
					"There are no Digitally signed documemts for this application .");

		if (!CollectionUtils.isEmpty(dscDetailsFromRequest)) {
			if (dscDetailsFromRequest.size() != 1)
				throw new CustomException("DSC DETAILS ERROR",
						"There should be only one Digitally signed documemts for application .");

			DscDetails requestDscDetail = dscDetailsFromRequest.get(0);
			DscDetails searchedDscDetail = dscDetailsFromSearch.get(0);

			if (StringUtils.isEmpty(requestDscDetail.getApprovedBy()))
				throw new CustomException("DSC DETAILS ERROR",
						"Approved by is mandatory in Digitally signed documemts .");

			if (StringUtils.isEmpty(requestDscDetail.getApplicationNo()))
				throw new CustomException("DSC DETAILS ERROR",
						"Application Number is mandatory in Digitally signed documemts .");

			if (StringUtils.isEmpty(requestDscDetail.getDocumentType()))
				throw new CustomException("DSC DETAILS ERROR",
						"Document Type is mandatory in Digitally signed documemts .");

			if (StringUtils.isEmpty(requestDscDetail.getId()))
				throw new CustomException("DSC DETAILS ERROR", "Id is mandatory in Digitally signed documemts .");

			if (StringUtils.isEmpty(requestDscDetail.getTenantId()))
				throw new CustomException("DSC DETAILS ERROR", "TenantId is mandatory in Digitally signed documemts .");

			if (StringUtils.isEmpty(requestDscDetail.getDocumentId()))
				throw new CustomException("DSC DETAILS ERROR",
						"DocumentId is mandatory in Digitally signed documemts .");

			if (!searchedDscDetail.getApplicationNo().equalsIgnoreCase(requestDscDetail.getApplicationNo()))
				throw new CustomException("DSC DETAILS ERROR", "DSC Document Application Number does not match .");

			if (!searchedDscDetail.getId().equalsIgnoreCase(requestDscDetail.getId()))
				throw new CustomException("DSC DETAILS ERROR", "DSC Document Id does not match .");

			if (!searchedDscDetail.getApprovedBy().equalsIgnoreCase(requestDscDetail.getApprovedBy()))
				throw new CustomException("DSC DETAILS ERROR", "DSC Document Approved by does not match .");

			if (!searchedDscDetail.getApprovedBy().equalsIgnoreCase(request.getRequestInfo().getUserInfo().getUuid()))
				throw new CustomException("DSC DETAILS ERROR",
						"DSC Document Can only be signed by the person who approved it .");

		} else
			throw new CustomException("DSC DETAILS ERROR",
					"There are no Digitally signed documemts for this application .");

	}
	
	public void validatePlanDscDetails(BPARequest request, List<BPA> searchResult) {
		Map<String, BPA> idToBpaMapFromSearch = new HashMap<>();
		searchResult.forEach(bpa -> {
			idToBpaMapFromSearch.put(bpa.getId(), bpa);
		});

		BPA bpaFromRequest = request.getBPA();

		BPA bpaFromSearch = idToBpaMapFromSearch.get(bpaFromRequest.getId());

		if (bpaFromSearch == null) {
			throw new CustomException("APPLICATION  ERROR", "The Application does not exist .");
		}
		
		if (!(bpaFromSearch.getStatus().equalsIgnoreCase("APPROVED")
				|| bpaFromSearch.getStatus().equalsIgnoreCase("REJECTED"))) {
			throw new CustomException("APPLICATION STATUS ERROR",
					"The Application should be in approved or rejected state for document signing .");
		}

		List<DscDetails> planDscDetailFromSearch = bpaFromSearch.getPlanDscDetails();
		List<DscDetails> planDscDetailFromRequest = bpaFromRequest.getPlanDscDetails();

		if (!CollectionUtils.isEmpty(planDscDetailFromSearch)) {
			if (planDscDetailFromSearch.size() != 1)
				throw new CustomException("PLAN DSC DETAILS ERROR",
						"There should be only one Digitally signed documemts for application .");

			DscDetails searchedDscDetail = planDscDetailFromSearch.get(0);

			if (!StringUtils.isEmpty(searchedDscDetail.getDocumentId()))
				throw new CustomException("PLAN DSC DETAILS ERROR", "This application plan pdf is already digitally signed.");

		} else
			throw new CustomException("PLAN DSC DETAILS ERROR",
					"There are no Digitally signed documemts for this application .");

		if (!CollectionUtils.isEmpty(planDscDetailFromRequest)) {
			if (planDscDetailFromRequest.size() != 1)
				throw new CustomException("PLAN DSC DETAILS ERROR",
						"There should be only one Digitally signed documemts for application .");

			DscDetails requestPlanDscDetail = planDscDetailFromRequest.get(0);
			DscDetails searchPlanDscDetail = planDscDetailFromSearch.get(0);

			if (StringUtils.isEmpty(requestPlanDscDetail.getApprovedBy()))
				throw new CustomException("PLAN DSC DETAILS ERROR",
						"Approved by is mandatory in Digitally signed documemts .");

			if (StringUtils.isEmpty(requestPlanDscDetail.getApplicationNo()))
				throw new CustomException("PLAN DSC DETAILS ERROR",
						"Application Number is mandatory in Digitally signed documemts .");

			if (StringUtils.isEmpty(requestPlanDscDetail.getDocumentType()))
				throw new CustomException("PLAN DSC DETAILS ERROR",
						"Document Type is mandatory in Digitally signed documemts .");

			if (StringUtils.isEmpty(requestPlanDscDetail.getId()))
				throw new CustomException("PLAN DSC DETAILS ERROR", "Id is mandatory in Digitally signed documemts .");

			if (StringUtils.isEmpty(requestPlanDscDetail.getTenantId()))
				throw new CustomException("PLAN DSC DETAILS ERROR", "TenantId is mandatory in Digitally signed documemts .");

			if (StringUtils.isEmpty(requestPlanDscDetail.getDocumentId()))
				throw new CustomException("PLAN DSC DETAILS ERROR",
						"DocumentId is mandatory in Digitally signed documemts .");

			if (!searchPlanDscDetail.getApplicationNo().equalsIgnoreCase(requestPlanDscDetail.getApplicationNo()))
				throw new CustomException("PLAN DSC DETAILS ERROR", "Plan DSC Document Application Number does not match .");

			if (!searchPlanDscDetail.getId().equalsIgnoreCase(requestPlanDscDetail.getId()))
				throw new CustomException("PLAN DSC DETAILS ERROR", "Plan DSC Document Id does not match .");

			if (!searchPlanDscDetail.getApprovedBy().equalsIgnoreCase(requestPlanDscDetail.getApprovedBy()))
				throw new CustomException("PLAN DSC DETAILS ERROR", "Plan DSC Document Approved by does not match .");

			if (!searchPlanDscDetail.getApprovedBy().equalsIgnoreCase(request.getRequestInfo().getUserInfo().getUuid()))
				throw new CustomException("PLAN DSC DETAILS ERROR",
						"Plan DSC Document Can only be signed by the person who approved it .");

		} else
			throw new CustomException("PLAN DSC DETAILS ERROR",
					"There are no Digitally signed documemts for this application .");

	}
	
	/**
	 * Validates fatherOrHusbandName and gender if ownerType is individual.single
	 * 
	 * @param request
	 */
	private void validateOwnerDetails(BPARequest bpaRequest) {
		
	/*	Remove mandatory validation for single owner gender and fatherOrHusbandName
	 * 
	 * if (bpaRequest.getBPA().getLandInfo().getOwnershipCategory().toLowerCase().contains("individual.singleowner")
				&& Objects.isNull(bpaRequest.getBPA().getLandInfo().getOwners().get(0).getGender())) {
			throw new CustomException(BPAErrorConstants.BPA_GENDER_MISSING,
					"Gender is mandatory for Individual Single Owner");
		}
		if (bpaRequest.getBPA().getLandInfo().getOwnershipCategory().toLowerCase().contains("individual.singleowner")
				&& Objects.isNull(bpaRequest.getBPA().getLandInfo().getOwners().get(0).getFatherOrHusbandName())) {
			throw new CustomException(BPAErrorConstants.BPA_FATHER_NAME_MISSING,
					"Father or Husband's Name is mandatory for Individual Single Owner");
		}
	*/
		
	 // Add mandatory validation for individual.singleowner
		
		if (bpaRequest.getBPA().getLandInfo().getOwnershipCategory().toLowerCase().contains("individual.singleowner")
				&& Objects.isNull(bpaRequest.getBPA().getLandInfo().getOwners().get(0).getMobileNumber())) {
			throw new CustomException(BPAErrorConstants.BPA_MOBILE_NUMBER_MISSING,
					"Mobile number is mandatory for Individual Single Owner");
		}
		
		if (bpaRequest.getBPA().getLandInfo().getOwnershipCategory().toLowerCase().contains("individual.singleowner")
				&& Objects.isNull(bpaRequest.getBPA().getLandInfo().getOwners().get(0).getName())) {
			throw new CustomException(BPAErrorConstants.BPA_APPLICANT_NAME_MISSING,
					"Applicant name is mandatory for Individual Single Owner");
		}
		
		if (bpaRequest.getBPA().getLandInfo().getOwnershipCategory().toLowerCase().contains("individual.singleowner")
				&& Objects.isNull(bpaRequest.getBPA().getLandInfo().getOwners().get(0).getCorrespondenceAddress())) {
			throw new CustomException(BPAErrorConstants.BPA_CORRESPONDENCE_ADDRESS_MISSING,
					"Applicant Correspondence Address is mandatory for Individual Single Owner");
		}
		
	}
	
	/**
	 * Validates assignees while forwarding got low risk application
	 * @param bpaRequest
	 */
	private void validateBpaForward(BPARequest bpaRequest) {
		if(BPAConstants.BPA_AC_MODULE_CODE.equalsIgnoreCase(bpaRequest.getBPA().getBusinessService())) {
			if(bpaRequest.getBPA().getWorkflow() != null
					&& BPAConstants.ACTION_FORWORD.equalsIgnoreCase(bpaRequest.getBPA().getWorkflow().getAction())
					&& bpaRequest.getBPA().getWorkflow().getAssignes().isEmpty()) {
				throw new CustomException(BPAErrorConstants.BPA_ASSIGNE_MISSING,
						"Plase assign some body while forwarding.");
			}
			
			
//			if(bpaRequest.getBPA().getWorkflow() != null
//					&& BPAConstants.ACTION_FORWORD.equalsIgnoreCase(bpaRequest.getBPA().getWorkflow().getAction())
//					&& !bpaRequest.getBPA().getWorkflow().getAssignes().isEmpty()
//					&& bpaRequest.getBPA().getWorkflow().getAssignes().contains(bpaRequest.getRequestInfo().getUserInfo().getUuid())) {
//				throw new CustomException(BPAErrorConstants.BPA_FORWARD_ISSUE,
//						"Cannot forward to yourself");
//			}
		}
		
		// Validate Doc Remark while forwarding by CITIZEN
		if (BPAConstants.ACTION_FORWORD.equalsIgnoreCase(bpaRequest.getBPA().getWorkflow().getAction())
				&& bpaRequest.getRequestInfo().getUserInfo().getType().equalsIgnoreCase(BPAConstants.CITIZEN)) {
			BPA bpa = bpaRequest.getBPA();
			List<String> bpaDocuments = new ArrayList<>();
			for (Document doc : bpa.getDocuments()) {
				bpaDocuments.add(convertDocType(doc.getDocumentType()));
			}
			DocRemarkSearchCriteria criteria = DocRemarkSearchCriteria.builder()
					.businessId(bpaRequest.getBPA().getApplicationNo()).build();
			List<DocRemark> docRemarks = docRemarkService.search(criteria);
			if (!CollectionUtils.isEmpty(docRemarks)) {
				for (DocRemark docRemark : docRemarks) {
					if (docRemark.getIsUpdatable().equals(Boolean.TRUE)
							&& !bpaDocuments.contains(docRemark.getDocumentCode())) {
						throw new CustomException(BPAErrorConstants.BPA_UNKNOWN_DOCS_MSG,
								"Some documents have been marked for upload by the employee. Please upload the required documents.");
					}
				}
			}
		}
		 
	}
	
	
	private String convertDocType(String docType) {
		String[] parts = docType.split("\\.");
		if (parts.length < 2) {
			return docType;
		}
		return parts[0] + "_" + parts[1];
	}
	
	
	/**
	 * Validates if the search parameters are valid
	 * 
	 * @param requestInfo
	 *            The requestInfo of the incoming request
	 * @param criteria
	 *            The BPASearch Criteria
	 */
//TODO need to make the changes in the data
	public void validateReportSearch(RequestInfo requestInfo, BPASearchCriteria criteria) {

		String allowedParamStr = null;

		if (requestInfo.getUserInfo().getType().equalsIgnoreCase(BPAConstants.CITIZEN))
			allowedParamStr = config.getAllowedCitizenSearchParameters();
		else if (requestInfo.getUserInfo().getType().equalsIgnoreCase(BPAConstants.EMPLOYEE))
			allowedParamStr = config.getAllowedEmployeeSearchParameters();
		else
			throw new CustomException(BPAErrorConstants.INVALID_SEARCH,
					"The userType: " + requestInfo.getUserInfo().getType() + " does not have any search config");

		if (StringUtils.isEmpty(allowedParamStr) && !criteria.isEmpty())
			throw new CustomException(BPAErrorConstants.INVALID_SEARCH, "No search parameters are expected");
		else {
			List<String> allowedParams = Arrays.asList(allowedParamStr.split(","));
			validateSearchParams(criteria, allowedParams);
		}
	}


	public void validatefieldInspectionRequest(FieldInspection fieldInspection) {
		
		validateDocs(fieldInspection);
		Map<Integer,List<String>> docTypes = getAlldocTypeforphotos(fieldInspection);
		log.info("docs list:"+docTypes);
		validateAllImagesUpload(fieldInspection,docTypes);
		log.info("docs list:"+docTypes);
		
	}
	
	public void validatePlanningAssitantChecklistRequest(PlanningAssistantChecklist planningAssistantChecklist) {

		// Add Validation for Mandatory Fields

	}


	private void validateDocs(FieldInspection fieldInspection) {
		// TODO Auto-generated method stub
		Object ReportDetails = fieldInspection.getReportDetails();
		if (Objects.isNull(ReportDetails) || ReportDetails.toString().isEmpty())
			throw new CustomException("create error",
					"Failed to create feild inspection report, reportdetails can't be empty !");
		Object details = ((List) ReportDetails).get(0);
		Object docs = (((Map) details).get(BPAConstants.DOCS));
		//Object date = (((Map) details).get(BPAConstants.INSPECTION_DATE));
		//Object time = (((Map) details).get(BPAConstants.INSPECTION_TIME));
		//Object questions = (((Map) details).get(BPAConstants.QUESTIONS_TYPE));
		if (Objects.isNull(docs) ) {
			throw new CustomException("create error",
					"Failed to create feild inspection report, docs,questions is mandatory!");

		}
	}


	private Map<Integer, List<String>> getAlldocTypeforphotos(FieldInspection fieldInspection) {
		// TODO Auto-generated method stub
		Object ReportDetails = fieldInspection.getReportDetails();
		Map<Integer, List<String>> doctype = new HashMap<>();
		List report = (List) ReportDetails;
		int count = 1;
		for (Object o : report) {
			// photos will be there
			List photos = (List) (((Map) o).get(BPAConstants.PHOTOS));
			if (Objects.nonNull(photos)) {
				List<String> docList = new ArrayList<String>();
				for (Object obj : photos) {
					Object docType = ((Map) obj).get(BPAConstants.DOCUMENTTYPE);
					if(Objects.nonNull(docType))
					docList.add(docType.toString());

				}
				doctype.put(count++, docList);
			}

		}

		return doctype;
	}

	private void validateAllImagesUpload(FieldInspection fieldInspection, Map<Integer, List<String>> docTypes) {
		Boolean check = Boolean.FALSE;
		Object approachRoad = fieldInspection.getApproachRoad();
		log.info("approach road" + approachRoad);
		Object roadSideDrain = ((Map) approachRoad).get(BPAConstants.IS_ROAD_SIDE);
		if (Objects.isNull(roadSideDrain) || roadSideDrain.toString().isEmpty())
			throw new CustomException("create error",
					"Failed to create feild inspection report, roadSideDrain can't be empty !");
		String value = roadSideDrain.toString();
		if (value.equalsIgnoreCase(BPAConstants.YES)) {
			docTypes.forEach((k, v) -> {
				if (!v.contains(BPAConstants.ROAD_SIDE_DRAIN)) {
					throw new CustomException("create error",
							"Failed to create feild inspection report, pls upload road side drain image for all reports !");
				}
			});
		}
		Object siteSituation = fieldInspection.getSiteSituation();
		Object waterSupply = ((Map) siteSituation).get(BPAConstants.IS_WATER_SUPPLY);
		if (Objects.isNull(waterSupply) || waterSupply.toString().isEmpty())
			throw new CustomException("create error",
					"Failed to create feild inspection report, waterSupply can't be empty !");

		if (waterSupply.toString().equalsIgnoreCase(BPAConstants.YES)) {
			docTypes.forEach((k, v) -> {
				if (!v.contains(BPAConstants.WATER_SUPPLY_DOCTYPE)) {
					throw new CustomException("create error",
							"Failed to create feild inspection report, pls upload water supply image for all reports !");
				}
			});

		}

		Object electricSupply = ((Map) siteSituation).get(BPAConstants.IS_ELECTRIC_SUPPLY);
		if (Objects.isNull(electricSupply) || electricSupply.toString().isEmpty())
			throw new CustomException("create error",
					"Failed to create feild inspection report, electricitySupply can't be empty !");

		if (electricSupply.toString().equalsIgnoreCase(BPAConstants.YES)) {
			docTypes.forEach((k, v) -> {
				if (!v.contains(BPAConstants.ELECTRIC_SUPPLY_DOCTYPE)) {
					throw new CustomException("create error",
							"Failed to create feild inspection report, pls upload electricity supply image for all reports !");
				}
			});

		}

		Object drainage = ((Map) siteSituation).get(BPAConstants.IS_DRAINAGE_REQUIRED);
		if (Objects.isNull(drainage) || drainage.toString().isEmpty())
			throw new CustomException("create error",
					"Failed to create feild inspection report, drainage can't be empty !");

		if (drainage.toString().equalsIgnoreCase(BPAConstants.YES)) {
			docTypes.forEach((k, v) -> {
				if (!v.contains(BPAConstants.DRAINAGE_DOCTYPE)) {
					throw new CustomException("create error",
							"Failed to create feild inspection report, pls upload drainage image for all reports !");
				}
			});

		}

		Object sewregeDisposal = ((Map) siteSituation).get(BPAConstants.IS_SEWARGE_REQUIRED);
		if (Objects.isNull(sewregeDisposal) || sewregeDisposal.toString().isEmpty())
			throw new CustomException("create error",
					"Failed to create feild inspection report, sewrage can't be empty !");

		if (sewregeDisposal.toString().equalsIgnoreCase(BPAConstants.YES)) {
			docTypes.forEach((k, v) -> {
				if (!v.contains(BPAConstants.SEWARGE_DOCTYPE)) {
					throw new CustomException("create error",
							"Failed to create feild inspection report, pls upload sewrage image for all reports !");
				}
			});

		}

		Object rainWaterHarvesting = ((Map) siteSituation).get(BPAConstants.IS_RAINWATER_HARVESTING_REQUIRED);
		/*if (Objects.isNull(rainWaterHarvesting) || rainWaterHarvesting.toString().isEmpty())
			throw new CustomException("create error",
					"Failed to create feild inspection report, rainWaterHarvesting can't be empty !");*/
		
		if (!Objects.isNull(rainWaterHarvesting) && !String.valueOf(rainWaterHarvesting).isEmpty()) {
			if (rainWaterHarvesting.toString().equalsIgnoreCase(BPAConstants.YES)) {
				docTypes.forEach((k, v) -> {
					if (!v.contains(BPAConstants.RAINWATER_HARVESTING_DOCTYPE)) {
						throw new CustomException("create error",
								"Failed to create feild inspection report, pls upload rainWaterHarvesting image for all reports !");
					}
				});

			}
		}

		Object noOfTrees = ((Map) siteSituation).get(BPAConstants.NO_OF_TREES);
		/*if (Objects.isNull(noOfTrees) || noOfTrees.toString().isEmpty())
			throw new CustomException("create error",
					"Failed to create feild inspection report, noOfTrees can't be empty !");*/
		if (!Objects.isNull(noOfTrees) && !String.valueOf(noOfTrees).isEmpty()) {
			if (Integer.valueOf(noOfTrees.toString()) > 0) {
				log.info("Total No of trees:" + Integer.valueOf(noOfTrees.toString()));
				docTypes.forEach((k, v) -> {
					if (!v.contains(BPAConstants.NO_OF_TREES_DOCTYPE)) {
						throw new CustomException("create error",
								"Failed to create feild inspection report, pls upload noOfTrees image for all reports !");
					}
				});

			}
		}

		Object noOfRechargingPits = ((Map) siteSituation).get(BPAConstants.NO_OF_RECHARGING_PITS);
		/*if (Objects.isNull(noOfRechargingPits) || noOfRechargingPits.toString().isEmpty())
			throw new CustomException("create error",
					"Failed to create feild inspection report, noOfRechargingPits can't be empty !");*/
		
		if (!Objects.isNull(noOfRechargingPits) && !String.valueOf(noOfRechargingPits).isEmpty()) {
			if (Integer.valueOf(noOfRechargingPits.toString()) > 0) {
				log.info("Total No of Recharging Pits:" + Integer.valueOf(noOfRechargingPits.toString()));
				docTypes.forEach((k, v) -> {
					if (!v.contains(BPAConstants.NO_OF_RECHARGING_PITS_DOCTYPE)) {
						throw new CustomException("create error",
								"Failed to create feild inspection report, pls upload noOfRechargingPits image for all reports !");
					}
				});

			}
		}

		Object buildingSituation = fieldInspection.getBuildingSituation();
		/*if (Objects.isNull(buildingSituation))
			throw new CustomException("create error",
					"Failed to create feild inspection report, buildingSituation can't be empty !");*/
		if(!Objects.isNull(buildingSituation) && !String.valueOf(buildingSituation).isEmpty()) {
		Object noOfLifts = ((Map) (((List) buildingSituation).get(0))).get(BPAConstants.NO_OF_LIFTS);
		/*if (Objects.isNull(noOfLifts) || noOfLifts.toString().isEmpty())
			throw new CustomException("create error",
					"Failed to create feild inspection report, noOfLifts can't be empty !");*/
		if (!Objects.isNull(noOfLifts) && !String.valueOf(noOfLifts).isEmpty()) {
			if (Integer.valueOf(noOfLifts.toString()) > 0) {
				log.info("Total No of noOfLifts:" + Integer.valueOf(noOfLifts.toString()));
				docTypes.forEach((k, v) -> {
					if (!v.contains(BPAConstants.NO_OF_LIFTS_DOCTYPE)) {
						throw new CustomException("create error",
								"Failed to create feild inspection report, pls upload noOfLifts image for all reports !");
					}
				});

			}
		}

		Object noOfStairCases = ((Map) (((List) buildingSituation).get(0))).get(BPAConstants.NO_OF_STAIR_CASE);
		/*
		 * if (Objects.isNull(noOfStairCases) || noOfStairCases.toString().isEmpty())
		 * throw new CustomException("create error",
		 * "Failed to create feild inspection report, noOfLifts can't be empty !");
		 */
		if (!Objects.isNull(noOfStairCases) && !String.valueOf(noOfStairCases).isEmpty()) {
			if (Integer.valueOf(noOfStairCases.toString()) > 0) {
				log.info("Total No of noOfStairCases:" + Integer.valueOf(noOfStairCases.toString()));
				docTypes.forEach((k, v) -> {
					if (!v.contains(BPAConstants.NO_OF_STAIR_CASE_DOCTYPE)) {
						throw new CustomException("create error",
								"Failed to create feild inspection report, pls upload noOfstairCase image for all reports !");
					}
				});

			}
		}

	}
}


	public void validateBPADeletion(BPA bpa) {
		
		if (!(bpa.getStatus().equalsIgnoreCase(BPAConstants.BPA_INITIATED)
				|| bpa.getStatus().equalsIgnoreCase(BPAConstants.STATUS_CITIZEN_APPROVAL_INPROCESS)
				|| bpa.getStatus().equalsIgnoreCase(BPAConstants.INPROGRESS_STATUS)
				|| bpa.getStatus().equalsIgnoreCase(BPAConstants.APPL_FEE_STATE))) {
			throw new CustomException("Delete BPA Error",
					"Application is not allowed to delete after Application Fee Payment");
		}
		
	}
	
	public void validateDocUpload(DocUploadRequest docUploadRequest, List<BPA> bpas, Object mdmsData,
			String currentState) {
		
		BPA bpa = bpas.get(0);
		validateIfDocsAllowed(docUploadRequest, bpa,mdmsData);
		validateDuplicateDocs(docUploadRequest);
		
	}

	private void validateIfDocsAllowed(DocUploadRequest docUploadRequest, BPA bpa, Object mdmsData) {
		List<Document> existingDocuments = bpa.getDocuments();
		Set<String> existingDocTypes = existingDocuments.stream().map(Document::getDocumentType)
				.collect(Collectors.toSet());
		Set<String> newDocTypes = docUploadRequest.getDocuments().stream().map(document -> document.getDocumentType())
				.collect(Collectors.toSet());
		Map<String, List<String>> masterData = mdmsValidator.getAttributeValues(mdmsData);
		Set<String> allowedDocumentTypes = masterData.get("DocumentType").stream()
				//.filter(docType -> docType.startsWith("NOC"))
				.collect(Collectors.toSet());

		List<String> invalidDocuments = newDocTypes.stream().filter(Doc -> !allowedDocumentTypes.contains(Doc))
				.collect(Collectors.toList());

		if (!CollectionUtils.isEmpty(invalidDocuments)) {
			throw new CustomException(BPAErrorConstants.BPA_INVALID_DOCUMENTTYPE,
					"Invalid Document Type for " + invalidDocuments);
		}
		
		Set<String> newDocTypesForUpload = docUploadRequest.getDocuments().stream().filter(document -> !org.springframework.util.StringUtils.hasText(document.getId())).map(document -> document.getDocumentType())
				.collect(Collectors.toSet());

		boolean hasCommonDocs = newDocTypesForUpload.stream().filter(Doc -> !Doc.equalsIgnoreCase("ADDL.ADDLDOCUMENTS")).anyMatch(existingDocTypes::contains);
		if (hasCommonDocs) {
			throw new CustomException(BPAErrorConstants.BPA_DUPLICATE_DOCUMENT,
					"One or All of the documents already uploaded");
		}
	}

	private void validateDuplicateDocs(DocUploadRequest docUploadRequest) {
		if (docUploadRequest.getDocuments() != null) {
			List<String> documentFileStoreIds = new LinkedList<String>();
			List<String> documentTypes = new LinkedList<String>();
			docUploadRequest.getDocuments().forEach(document -> {
				if (documentFileStoreIds.contains(document.getFileStoreId()) || documentTypes.contains(document.getDocumentType()) && !document.getDocumentType().equals("ADDL.ADDLDOCUMENTS"))
					throw new CustomException(BPAErrorConstants.BPA_DUPLICATE_DOCUMENT,
							"Same document cannot be used multiple times");
				else {
					documentFileStoreIds.add(document.getFileStoreId());
					documentTypes.add(document.getDocumentType());
				}
			});
		}
	}


	public void validateDocUploadRequest(DocUploadRequest docUploadRequest) {
		
		log.info("Validating Document Upload Request");

		
		if(!org.springframework.util.StringUtils.hasText(docUploadRequest.getTenantId())){
			throw new CustomException(BPAErrorConstants.INVALID_TENANT_ID_MDMS_KEY,
					"TenantID is Mandatory ");
		}
		
		if(!org.springframework.util.StringUtils.hasText(docUploadRequest.getApplicationNo())){
			throw new CustomException(BPAErrorConstants.INVALID_APPLICATION_NO_KEY,
					"BPA Application No is Mandatory ");
		}
		
		if(CollectionUtils.isEmpty(docUploadRequest.getDocuments())){
			throw new CustomException(BPAErrorConstants.INVALID_DOCUMENTS_KEY,
					"Documents are Mandatory ");
		}
		
		if(!org.springframework.util.StringUtils.hasText(docUploadRequest.getDocUploadType())){
			throw new CustomException(BPAErrorConstants.INVALID_DOCUMENTTYPE_KEY,
					"BPA Document Upload Type is Mandatory ");
		}
		
		if(!BPAConstants.OFFLINE_DOC_UPLOAD_ALLOWED.contains(docUploadRequest.getDocUploadType())){
			throw new CustomException(BPAErrorConstants.INVALID_DOCUMENTTYPE_KEY,
					"This BPA Document Upload Type is not allowed ");
		}
		
	}
	
	public void validateplinthApprovalRequest(PlinthApproval plinthApproval) {
		if(plinthApproval.getBpaApplicationNo().isEmpty()){
			throw new CustomException(BPAErrorConstants.INVALID_CREATE,
					"BPA application number is mandatory ");
		}
		
		if(plinthApproval.getStatus().isEmpty()){
			throw new CustomException(BPAErrorConstants.INVALID_CREATE,
					"Plinth Approval status is mandatory ");
		}
		
		if(CollectionUtils.isEmpty(plinthApproval.getDocuments())){
			throw new CustomException(BPAErrorConstants.INVALID_CREATE,
					"Plinth Approval document is mandatory ");
		}
	}


	public void validateBPAForPlinthCreateRequest(List<BPA> bpa, String bpaApplicationNumber) {
		
		if (CollectionUtils.isEmpty(bpa) || bpa.size() > 1) {
			throw new CustomException("create error",
					"Failed to create Plinth Level Approval, Found None or multiple bpa applications for application number : "
							+ bpaApplicationNumber);
		}
		
		List<DscDetails> dscDetails = bpa.get(0).getDscDetails();
		
		if(CollectionUtils.isEmpty(dscDetails)) {
			
			throw new CustomException("create error",
					"Failed to create Plinth Level Approval, BPA Application Mentioned is not Digitally Signed : "
							+ bpaApplicationNumber);
		}
		
		if(StringUtils.isEmpty(dscDetails.get(0).getDocumentId())) {
			
			throw new CustomException("create error",
					"Failed to create Plinth Level Approval, BPA Application Mentioned is not Digitally Signed : "
							+ bpaApplicationNumber);
			
		}
		Map<String, Object> additionalDetails = (Map<String, Object>) bpa.get(0).getAdditionalDetails();

		validateCommencementDeclaration(bpaApplicationNumber, additionalDetails);

		validateNocRelaxationChecklist(bpa, bpaApplicationNumber, additionalDetails);
		
		
	}


	private void validateCommencementDeclaration(String bpaApplicationNumber, Map<String, Object> additionalDetails) {
		if (!ObjectUtils.isEmpty(additionalDetails) && additionalDetails.containsKey("CommencementDec")
				&& additionalDetails.get("CommencementDec") != null) {

			Boolean commencementDec = (Boolean) additionalDetails.get("CommencementDec");
			if (!commencementDec) {
				throw new CustomException("create error",
						"Failed to create Plinth Level Approval, : Commencement Declaration is Mandatory to Create Plinth Level Application "
								+ bpaApplicationNumber);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void validateNocRelaxationChecklist(List<BPA> bpa, String bpaApplicationNumber,
			Map<String, Object> additionalDetails) {
		
		List<String> nocRelaxationList =
		        (List<String>) additionalDetails.get("nocRelaxationCheckList");

		if (CollectionUtils.isEmpty(nocRelaxationList)) {
		    return;
		}

		if (nocRelaxationList.contains("NOC.NA")) {
		    log.info("No Offline NOC documents required for Application No: {}",
		            bpaApplicationNumber);
		    return;
		}

		Set<String> uploadedDocumentTypes = Optional.ofNullable(bpa.get(0).getDocuments())
		        .orElse(Collections.emptyList())
		        .stream()
		        .map(Document::getDocumentType)
		        .filter(Objects::nonNull)
		        .collect(Collectors.toSet());

		List<String> missingDocuments = nocRelaxationList.stream()
		        .filter(docType -> !uploadedDocumentTypes.contains(docType))
		        .collect(Collectors.toList());

		if (!missingDocuments.isEmpty()) {
		    throw new CustomException(
		            "MISSING_NOC_DOCUMENTS",
		            String.format(
		                    "Cannot create Plinth Level Application. Please upload the required NOC document(s): %s for Application No: %s.",
		                    String.join(", ", missingDocuments),
		                    bpaApplicationNumber));
		}
	}


	/**
	 * Validator of Village Search Request
	 * 
	 * @param criteria
	 */
	public void validateVillageSearchRequest(@Valid VillageSearchCriteria criteria) {
		
		// Don't Allow the request to process, if Application Number is missing
		if(CollectionUtils.isEmpty(criteria.getApplicationNos())) {
			
			throw new CustomException("INVALID_SEARCH_PARAM",
					"Kindly provide Application Number to Search the Villages !!");
		}
		
	}


	/**
	 * Validate all the components of FI Report here
	 * 
	 * @param fieldInspection
	 */
	public void validatefieldInspectionRequestV2(FieldInspection fieldInspection) {
		// Validate Report Details Json
		// validateReportDetails(fieldInspection);
		// Validate Site Situation Json
		validateSiteSituation(fieldInspection);
		// Validate Building Situation Json
		validateBuildingSituation(fieldInspection);
		// Validate Approch Raod Json 
		validateApprochRoad(fieldInspection);
	}


	/**
	 * Validate Approch road json and all the applicable photos
	 * 
	 * @param fieldInspection
	 */
	private void validateApprochRoad(FieldInspection fieldInspection) {

		Object approachRoad = fieldInspection.getApproachRoad();

		if (Objects.isNull(approachRoad) || approachRoad.toString().isEmpty()) {
//			throw new CustomException("create error",
//					"Failed to create feild inspection report, approachRoad can't be empty !");
			log.info("Approach road empty, no validations to be checked...");
		} else {

			// Validations for Road Side Drain
			Object roadSideDrain = ((Map) approachRoad).get(BPAConstants.IS_ROAD_SIDE);
			if (Objects.isNull(roadSideDrain) || roadSideDrain.toString().isEmpty()) {
//				throw new CustomException("create error",
//						"Failed to create feild inspection report, roadSideDrain can't be empty !");
				log.info("Road Side Drain empty, no validations to be checked...");
			} else {
				Object valueForRoadSideDrain = ((Map) roadSideDrain).get(BPAConstants.VALUE);
				if (valueForRoadSideDrain.toString().equalsIgnoreCase("YES")) {
					List photos = (List) (((Map) roadSideDrain).get(BPAConstants.PHOTOS));
					List<String> docList = new ArrayList<String>();
					for (Object obj : photos) {
						Object docType = ((Map) obj).get(BPAConstants.DOCUMENTTYPE);
						if (Objects.nonNull(docType))
							docList.add(docType.toString());
					}

					if (!docList.contains(BPAConstants.ROAD_SIDE_DRAIN)) {
						throw new CustomException("create_error",
								"Failed to create feild inspection report, pls upload roadSideDrain image for all reports !");
					}
				}
			}
		}
	}

	/**
	 * Validate Building Situation json and all the applicable photos
	 * 
	 * @param fieldInspection
	 */
	private void validateBuildingSituation(FieldInspection fieldInspection) {

		Object buildingSituation = fieldInspection.getBuildingSituation();

		if (Objects.isNull(buildingSituation) || buildingSituation.toString().isEmpty()) {

//			throw new CustomException("create error",
//					"Failed to create feild inspection report, buildingSituation can't be empty !");
			log.info("buildingSituation empty, no validations to be checked...");
		} else {

			Object blocks = ((Map) buildingSituation).get(BPAConstants.BLOCK);

			Object noOfBlocks = ((Map) buildingSituation).get("noOfBlock");

			List block = (List) blocks;

			if (!noOfBlocks.toString().equalsIgnoreCase(BPAConstants.ZERO)) {

				// Validation for No of Staircase
				for (Object o : block) {

					Object noOfStairCase = ((Map) o).get(BPAConstants.NO_OF_STAIR_CASE);
					if (Objects.isNull(noOfStairCase) || noOfStairCase.toString().isEmpty()) {
//						throw new CustomException("create error",
//								"Failed to create feild inspection report, noOfStairCase can't be empty !");
						log.info("noOfStairCase empty, no validations to be checked...");
					} else {
						Object valueFornoOfStairCase = ((Map) noOfStairCase).get(BPAConstants.VALUE);
						if (!valueFornoOfStairCase.toString().equalsIgnoreCase(BPAConstants.ZERO)) {
							List photos = (List) (((Map) noOfStairCase).get(BPAConstants.PHOTOS));
							List<String> docList = new ArrayList<String>();
							for (Object obj : photos) {
								Object docType = ((Map) obj).get(BPAConstants.DOCUMENTTYPE);
								if (Objects.nonNull(docType))
									docList.add(docType.toString());
							}

							if (!docList.contains(BPAConstants.NO_OF_STAIR_CASE_DOCTYPE)) {
								throw new CustomException("create_error",
										"Failed to create feild inspection report, pls upload noOfStairCase image for all reports !");
							}
						}
					}

				}

				// Validation for noOfLifts
				for (Object o : block) {

					Object noOfLifts = ((Map) o).get(BPAConstants.NO_OF_LIFTS);
					if (Objects.isNull(noOfLifts) || noOfLifts.toString().isEmpty()) {
//						throw new CustomException("create error",
//								"Failed to create feild inspection report, noOfLifts can't be empty !");
						log.info("noOfLifts empty, no validations to be checked..");
					} else {
						Object valueForNoOfLifts = ((Map) noOfLifts).get(BPAConstants.VALUE);
						if (!valueForNoOfLifts.toString().equalsIgnoreCase(BPAConstants.ZERO)) {
							List photos = (List) (((Map) noOfLifts).get(BPAConstants.PHOTOS));
							List<String> docList = new ArrayList<String>();
							for (Object obj : photos) {
								Object docType = ((Map) obj).get(BPAConstants.DOCUMENTTYPE);
								if (Objects.nonNull(docType))
									docList.add(docType.toString());
							}

							if (!docList.contains(BPAConstants.NO_OF_LIFTS_DOCTYPE)) {
								throw new CustomException("create_error",
										"Failed to create feild inspection report, pls upload noOfLifts image for all reports !");
							}
						}
					}
				}

				// Validation for blockWisePhoto
				for (Object o : block) {

					Object blockWisePhoto = ((Map) o).get(BPAConstants.IS_BLOCK_WISE_PHOTO);
					if (Objects.isNull(blockWisePhoto) || blockWisePhoto.toString().isEmpty()) {
//					throw new CustomException("create error",
//							"Failed to create feild inspection report, blockWisePhoto can't be empty !");
						log.info("blockWisePhoto empty, no validations to be checked..");
					} else {
						List blockWisePhotos = (List) blockWisePhoto;

						List<String> docList = new ArrayList<String>();
						for (Object obj : blockWisePhotos) {
							Object docType = ((Map) obj).get(BPAConstants.DOCUMENTTYPE);
							if (Objects.nonNull(docType))
								docList.add(docType.toString());
						}

						if (!docList.contains(BPAConstants.BLOCK_WISE_PHOTO_DOCTYPE)) {
							throw new CustomException("create_error",
									"Failed to create feild inspection report, pls upload BlockWise image for all reports !");
						}
					}

				}
			}

		}
	}

	/**
	 * Validate Site Situation json and all the applicable photos
	 * 
	 * @param fieldInspection
	 */
	private void validateSiteSituation(FieldInspection fieldInspection) {
		
		Object siteSituation = fieldInspection.getSiteSituation();

		if (Objects.isNull(siteSituation) || siteSituation.toString().isEmpty()) {

//			throw new CustomException("create error",
//					"Failed to create feild inspection report, siteSituation can't be empty !");
			log.info("Site Situation is empty, no validations will be checked...");
		} else {
			// Validations for Water Supply
			Object waterSupply = ((Map) siteSituation).get(BPAConstants.IS_WATER_SUPPLY);
			if (Objects.isNull(waterSupply) || waterSupply.toString().isEmpty()) {
//				throw new CustomException("create error",
//						"Failed to create feild inspection report, waterSupply can't be empty !");
				log.info("Water supply empty, no validations for this..");
			} else {
				Object valueForWaterSupply = ((Map) waterSupply).get(BPAConstants.VALUE);
				if (valueForWaterSupply.toString().equalsIgnoreCase(BPAConstants.YES)) {
					List photos = (List) (((Map) waterSupply).get(BPAConstants.PHOTOS));
					List<String> docList = new ArrayList<String>();
					for (Object obj : photos) {
						Object docType = ((Map) obj).get(BPAConstants.DOCUMENTTYPE);
						if (Objects.nonNull(docType))
							docList.add(docType.toString());
					}

					if (!docList.contains(BPAConstants.WATER_SUPPLY_DOCTYPE)) {
						throw new CustomException("create_error",
								"Failed to create feild inspection report, pls upload water supply image for all reports !");
					}
				}
			}

			// Validations for Electricity
			Object electricity = ((Map) siteSituation).get(BPAConstants.IS_ELECTRIC_SUPPLY);
			if (Objects.isNull(electricity) || electricity.toString().isEmpty()) {
//				throw new CustomException("create error",
//						"Failed to create feild inspection report, electricity can't be empty !");
				log.info("Electricity empty, no validations for this..");
			} else {
				Object valueForElectricity = ((Map) electricity).get(BPAConstants.VALUE);
				if (valueForElectricity.toString().equalsIgnoreCase(BPAConstants.YES)) {
					List photos = (List) (((Map) electricity).get(BPAConstants.PHOTOS));
					List<String> docList = new ArrayList<String>();
					for (Object obj : photos) {
						Object docType = ((Map) obj).get(BPAConstants.DOCUMENTTYPE);
						if (Objects.nonNull(docType))
							docList.add(docType.toString());
					}

					if (!docList.contains(BPAConstants.ELECTRIC_SUPPLY_DOCTYPE)) {
						throw new CustomException("create_error",
								"Failed to create feild inspection report, pls upload electricity image for all reports !");
					}
				}
			}

			// Validations for drainage
			Object drainage = ((Map) siteSituation).get(BPAConstants.IS_DRAINAGE_REQUIRED);
			if (Objects.isNull(drainage) || drainage.toString().isEmpty()) {
//				throw new CustomException("create error",
//						"Failed to create feild inspection report, drainage can't be empty !");
				log.info("drainage empty, no validations for this..");
			} else {
				Object valueForDrainage = ((Map) drainage).get(BPAConstants.VALUE);
				if (valueForDrainage.toString().equalsIgnoreCase(BPAConstants.YES)) {
					List photos = (List) (((Map) drainage).get(BPAConstants.PHOTOS));
					List<String> docList = new ArrayList<String>();
					for (Object obj : photos) {
						Object docType = ((Map) obj).get(BPAConstants.DOCUMENTTYPE);
						if (Objects.nonNull(docType))
							docList.add(docType.toString());
					}

					if (!docList.contains(BPAConstants.DRAINAGE_DOCTYPE)) {
						throw new CustomException("create_error",
								"Failed to create feild inspection report, pls upload drainage image for all reports !");
					}
				}
			}

			// Validations for sewrage
			Object sewrage = ((Map) siteSituation).get(BPAConstants.IS_SEWARGE_REQUIRED);
			if (Objects.isNull(sewrage) || sewrage.toString().isEmpty()) {
//				throw new CustomException("create error",
//						"Failed to create feild inspection report, sewrage can't be empty !");
				log.info("Sewerage empty, no validations for this..");
			} else {
				Object valueForSewrage = ((Map) sewrage).get(BPAConstants.VALUE);
				if (valueForSewrage.toString().equalsIgnoreCase(BPAConstants.YES)) {
					List photos = (List) (((Map) sewrage).get(BPAConstants.PHOTOS));
					List<String> docList = new ArrayList<String>();
					for (Object obj : photos) {
						Object docType = ((Map) obj).get(BPAConstants.DOCUMENTTYPE);
						if (Objects.nonNull(docType))
							docList.add(docType.toString());
					}

					if (!docList.contains(BPAConstants.SEWARGE_DOCTYPE)) {
						throw new CustomException("create_error",
								"Failed to create feild inspection report, pls upload sewrage image for all reports !");
					}
				}
			}

			// Validations for rainwaterharvesting
			Object rainwaterharvesting = ((Map) siteSituation).get(BPAConstants.IS_RAINWATER_HARVESTING_REQUIRED);
			if (Objects.isNull(rainwaterharvesting) || rainwaterharvesting.toString().isEmpty()) {
//				throw new CustomException("create error",
//						"Failed to create feild inspection report, rainwaterharvesting can't be empty !");
				log.info("rainwaterharvesting empty, no validations for this..");
			} else {

				Object valueForRainWaterHarvesting = ((Map) rainwaterharvesting).get(BPAConstants.VALUE);
				if (valueForRainWaterHarvesting.toString().equalsIgnoreCase(BPAConstants.YES)) {
					List photos = (List) (((Map) rainwaterharvesting).get(BPAConstants.PHOTOS));
					List<String> docList = new ArrayList<String>();
					for (Object obj : photos) {
						Object docType = ((Map) obj).get(BPAConstants.DOCUMENTTYPE);
						if (Objects.nonNull(docType))
							docList.add(docType.toString());
					}

					if (!docList.contains(BPAConstants.RAINWATER_HARVESTING_DOCTYPE)) {
						throw new CustomException("create_error",
								"Failed to create feild inspection report, pls upload rainwaterharvesting image for all reports !");
					}
				}
			}

			// Validations for noOfTrees
			Object noOfTrees = ((Map) siteSituation).get(BPAConstants.NO_OF_TREES);
			if (Objects.isNull(noOfTrees) || noOfTrees.toString().isEmpty()) {
//				throw new CustomException("create error",
//						"Failed to create feild inspection report, noOfTrees can't be empty !");
				log.info("noOfTrees empty, no validations for this..");
			} else {

				Object valueForNoOfTrees = ((Map) noOfTrees).get(BPAConstants.VALUE);
				if (!valueForNoOfTrees.toString().equalsIgnoreCase("0")) {
					List photos = (List) (((Map) noOfTrees).get(BPAConstants.PHOTOS));
					List<String> docList = new ArrayList<String>();
					for (Object obj : photos) {
						Object docType = ((Map) obj).get(BPAConstants.DOCUMENTTYPE);
						if (Objects.nonNull(docType))
							docList.add(docType.toString());
					}

					if (!docList.contains(BPAConstants.NO_OF_TREES_DOCTYPE)) {
						throw new CustomException("create_error",
								"Failed to create feild inspection report, pls upload noOfTrees image for all reports !");
					}
				}
			}

			// Validations for numberofRechargingPits
			Object numberofRechargingPits = ((Map) siteSituation).get(BPAConstants.NO_OF_RECHARGING_PITS);
			if (Objects.isNull(numberofRechargingPits) || numberofRechargingPits.toString().isEmpty()) {

//				throw new CustomException("create error",
//						"Failed to create feild inspection report, numberofRechargingPits can't be empty !");
				log.info("numberofRechargingPits empty, no validations for this..");
			} else {
				Object valueForNumberofRechargingPits = ((Map) numberofRechargingPits).get(BPAConstants.VALUE);
				if (!valueForNumberofRechargingPits.toString().equalsIgnoreCase("0")) {
					List photos = (List) (((Map) numberofRechargingPits).get(BPAConstants.PHOTOS));
					List<String> docList = new ArrayList<String>();
					for (Object obj : photos) {
						Object docType = ((Map) obj).get(BPAConstants.DOCUMENTTYPE);
						if (Objects.nonNull(docType))
							docList.add(docType.toString());
					}

					if (!docList.contains(BPAConstants.NO_OF_RECHARGING_PITS_DOCTYPE)) {
						throw new CustomException("create_error",
								"Failed to create feild inspection report, pls upload valueForNumberofRechargingPits image for all reports !");
					}
				}
			}
		}
	}


	/**
	 * Validate Report Details json and all the applicable photos
	 * 
	 * @param fieldInspection
	 */
	private void validateReportDetails(FieldInspection fieldInspection) {

		Object reportDetails = fieldInspection.getReportDetails();

		if (Objects.isNull(reportDetails) || reportDetails.toString().isEmpty()) {

			throw new CustomException("create error",
					"Failed to create feild inspection report, reportdetails can't be empty !");
		}

		List report = (List) reportDetails;

		for (Object o : report) {
			// photos will be there
			List photos = (List) (((Map) o).get(BPAConstants.PHOTOS));
			List docs = (List) (((Map) o).get(BPAConstants.DOCS));
			if (Objects.nonNull(photos)) {
				List<String> docList = new ArrayList<String>();
				for (Object obj : photos) {
					Object docType = ((Map) obj).get(BPAConstants.DOCUMENTTYPE);
					if (Objects.nonNull(docType))
						docList.add(docType.toString());
				}

				if (!docList.contains("FI.SIPU.SIPU")) {
					throw new CustomException("create_error", "Site Inspection Report Missing...");
				}
			}

			if (Objects.nonNull(docs)) {
				List<String> docList = new ArrayList<String>();
				for (Object obj : docs) {
					Object docType = ((Map) obj).get(BPAConstants.DOCUMENTTYPE);
					if (Objects.nonNull(docType))
						docList.add(docType.toString());
				}

				if (!docList.contains("FI.FIR.FIR")) {
					throw new CustomException("create_error", "Field Inspection Report Missing...");
				}
			}
		}
	}

	/**
	 * Validate if payment Received for Pull Back Reuquest
	 * 
	 * @param bpaRequest
	 * @param applicationType 
	 * @return
	 */
	public Boolean validateIfPaymentReceived(BPARequest bpaRequest, String applicationType) {
		Boolean isPaymentReceived = Boolean.FALSE;

		String tenantId = bpaRequest.getBPA().getTenantId();

		String applicationNumber = bpaRequest.getBPA().getApplicationNo();

		PaymentSearchCriteria paymentSearchCriteria = PaymentSearchCriteria.builder().consumerCode(applicationNumber)
				.businessService(IssueFixConstants.BPA_SAN_FEE).tenantId(tenantId).build();
		
		if(applicationType.equalsIgnoreCase(BPAConstants.BUILDING_PLAN_OC)) {
			paymentSearchCriteria.setBusinessService(IssueFixConstants.OC_SAN_FEE);
		}
		
		List<Payment> payments = repository.getPayments(paymentSearchCriteria);

		if (payments.size() >= 1) {
			isPaymentReceived = Boolean.TRUE;
			throw new CustomException("PAYMENT_ISSUE", "Sanc fee payment has been done for application no :" + applicationNumber + " Can't step back !");
		}

		log.info("Sanc Fee payment has not been done.. Checking other details !!");

		return isPaymentReceived;
	}

	/**
	 * Validate the demand to be deleted for Pull Back Request
	 * 
	 * @param bpaRequest
	 * @param isDataUpdateNeeded
	 * @param applicationType 
	 * @return
	 */
	public Demand validateDemandToBeDeleted(BPARequest bpaRequest, Map<String, Boolean> isDataUpdateNeeded, String applicationType) {
		
		Demand demand = new Demand();

		String tenantId = bpaRequest.getBPA().getTenantId();

		String applicationNumber = bpaRequest.getBPA().getApplicationNo();

		DemandSearchCriteria demandSearchCriteria = DemandSearchCriteria.builder().consumerCode(applicationNumber)
				.businessService(IssueFixConstants.BPA_SAN_FEE).tenantId(tenantId).build();
		
		if(applicationType.equalsIgnoreCase(BPAConstants.BUILDING_PLAN_OC)) {
			demandSearchCriteria.setBusinessService(IssueFixConstants.OC_SAN_FEE);
		}

		// Search demand here
		List<Demand> demands = repository.getDemands(demandSearchCriteria);
		if (demands.size() > 1) {
			throw new CustomException("MULTIPLE_DEMNADS_FOUND", "Multiple Sanction Fee demand found for application no :" + applicationNumber);
		} else if (!CollectionUtils.isEmpty(demands)) {
			if (demands.get(0).getIsPaymentCompleted().equals(Boolean.TRUE)) {
				throw new CustomException("PAYMENT_DONE_FOR_DEMAND", "Payment has been done for Sanction Fee demand application no :" + applicationNumber);
			} else {
				demand = demands.get(0);
				isDataUpdateNeeded.put(IssueFixConstants.IS_DEMAND_DELETE_NEEDED, true);
			}
		}
		return demand;
	}

	/**
	 * Validate the installment to be deleted for Pull Back request
	 * 
	 * @param bpaRequest
	 * @param isDataUpdateNeeded
	 * @return
	 */
	public Installment validateIfInstallmentToBeDeleted(BPARequest bpaRequest,
			Map<String, Boolean> isDataUpdateNeeded) {

		Installment installment = new Installment();
		InstallmentSearchCriteria installmentSearchCriteria = InstallmentSearchCriteria.builder()
				.consumerCode(bpaRequest.getBPA().getApplicationNo()).build();
		List<Installment> installments = repository.getInstallments(installmentSearchCriteria);
		if (!CollectionUtils.isEmpty(installments)) {
			installment = installments.get(0);
			isDataUpdateNeeded.put(IssueFixConstants.IS_INSTALLMENT_DELETE_NEEDED, true);

		}
		return installment;
	}


	/**
	 * Validate the DSC details to be deleted for Pull Back Request
	 * 
	 * @param bpaRequest
	 * @param isDataUpdateNeeded
	 * @return
	 */
	public DscDetails validateIfDscToBeDeleted(BPARequest bpaRequest, Map<String, Boolean> isDataUpdateNeeded) {

		BPASearchCriteria searchCriteria = BPASearchCriteria.builder()
				.applicationNo(bpaRequest.getBPA().getApplicationNo()).tenantId(bpaRequest.getBPA().getTenantId())
				.build();

		DscDetails dsc = new DscDetails();
		List<DscDetails> dscDetails = repository.getDscDetails(searchCriteria);

		if (!CollectionUtils.isEmpty(dscDetails)) {
			dsc = dscDetails.get(0);
			isDataUpdateNeeded.put(IssueFixConstants.IS_DSC_DELETE_NEEDED, true);

		}
		return dsc;
	}

	
	/**
	 * Validate Search Request for Applications Pending at Sanc Fee Status
	 * 
	 * @param criteria
	 */
	public void validateSearchFeePendingRequest(@Valid BPASearchCriteria criteria) {
		if (criteria.isEmpty()) {
			throw new CustomException("search_error", "Search Criteria cannot be empty !!!");
		}

		if (criteria.getTenantId().isEmpty() || criteria.getApprovedBy().isEmpty()) {

			throw new CustomException("search_error", "TenantId or Approved By cannot be empty !!!");
		}
	}


	public void validatePullBackAction(BPARequest bpaRequest) {
		BPA bpa = bpaRequest.getBPA();		
		if (bpa.getWorkflow().getAction().equalsIgnoreCase(BPAConstants.ACTION_PULL_BACK)) {
			String uuid = bpaRequest.getRequestInfo().getUserInfo().getUuid();
			if (!bpa.getDscDetails().get(0).getApprovedBy().equalsIgnoreCase(uuid)) {
				throw new CustomException("UNAUTHORISED_ACTION", "User is unauthorised to perform this action.");
			}
		
		
		List<ProcessInstance> processInstances =
		        workflowService.getProcessInstances(bpa, bpaRequest.getRequestInfo(), true);
		
		long createdTime = processInstances.get(0).getAuditDetails().getCreatedTime();
		log.info("last process instance createdTime is {}",createdTime);
			
		long count = processInstances.stream()
		        .filter(p -> BPAConstants.ACTION_PULL_BACK.equals(p.getAction()))
		        .count();
		log.info("total pull back count for the application is {}",count);
		// Current time in millis
		
	/*	if(!BPAConstants.ACTION_APPROVE.equalsIgnoreCase(processInstances.get(0).getAction())) {
			throw new CustomException("INVALID_STATE","Application is not in approve state.");
		} */
		
		long currentTime = System.currentTimeMillis();

		// 30 days in milliseconds
		long thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000;

		// Check if older than 30 days
		if(count>=config.getMaxCountPullBackActionLimit()) {
			if(currentTime - createdTime <= thirtyDaysInMillis ) {
			throw new CustomException( "BPA_INVALID_PULL_BACK_ACTION",
				    "Cannot withdraw the application. Pull-back action has already been performed " +count + " times. Withdrawal is allowed only once in 30 days.");
			}
		}
		}
	}


	public void validateRefusalSCNonApprove(BPARequest bpaRequest) {
		BPA bpa = bpaRequest.getBPA();
		if (!ObjectUtils.isEmpty(bpa.getWorkflow())) {
			if (bpa.getWorkflow().getAction().equalsIgnoreCase(BPAConstants.ACTION_APPROVE)
					&& bpa.getStatus().equalsIgnoreCase(BPAConstants.APPROVAL_INPROGRESS)) {
				if (config.getBpaRefusalShowCauseNoticeEnable()) {
					String applicationNo = bpa.getApplicationNo();
					NoticeSearchCriteria noticeSearchCriteria = NoticeSearchCriteria.builder().businessid(applicationNo)
							.letterType(BPAConstants.REFUSAL_SHOWCAUSE_LETTER_TYPE).build();

					List<Notice> notices = scnRepository.getNoticeData(noticeSearchCriteria);

					if (!CollectionUtils.isEmpty(notices)) {
						throw new CustomException("REFUSAL_SCN_EXIST", String.format(
								"A refusal SCN already exists for Business ID: %s and Letter Number: %s. Approval of the application is not possible.",
								applicationNo, notices.get(0).getLetterNo()));

					}
				}
			}
		}

	}


	public DscDetails validateIfPlanDscToBeDeleted(BPARequest bpaRequest, Map<String, Boolean> isDataUpdateNeeded) {

		BPASearchCriteria searchCriteria = BPASearchCriteria.builder()
				.applicationNo(bpaRequest.getBPA().getApplicationNo()).tenantId(bpaRequest.getBPA().getTenantId())
				.build();

		DscDetails dsc = new DscDetails();
		List<DscDetails> dscDetails = repository.getPlanDscDetails(searchCriteria);

		if (!CollectionUtils.isEmpty(dscDetails)) {
			dsc = dscDetails.get(0);
			isDataUpdateNeeded.put(IssueFixConstants.IS_PLAN_DSC_DELETE_NEEDED, true);

		}
		return dsc;
	}


	public void validateSaveDraft(@Valid BPADraftRequest request) {
		RequestInfo requestInfo = request.getRequestInfo();
		String tenantId = request.getBpaDraft().getTenantId();
		BPADraft draft = request.getBpaDraft();

		if (tenantId.split("\\.").length == 1) {
			throw new CustomException(BPAErrorConstants.INVALID_TENANT, "Draft cannot be saved at StateLevel");
		}
		
		if (ObjectUtils.isEmpty(draft)) {
			throw new CustomException(BPAErrorConstants.INVALID_REQUEST,
					"BPA Draft object can't be null or empty.");
		}
		
//		if (StringUtils.isEmpty(draft.getEdcrNo())) {
//			throw new CustomException(BPAErrorConstants.INVALID_EDCR_NUMBER,
//					"EDCR Number can't be null or empty.");
//		}
	}


	public void validateDraftSearch(RequestInfo requestInfo, @Valid BPADraftSearchCriteria criteria) {
		if (StringUtils.isEmpty(criteria.getEdcrNo()) && StringUtils.isEmpty(criteria.getTenantId())) {
			throw new CustomException(BPAErrorConstants.INVALID_SEARCH,
					"Either Edcr Number or tenant id is mandatory for BPA Draft Search.");
		}

	}
	
		
	// Validate Doc Remark while forwarding by CITIZEN
	public void validateRevalidationDocRemark(BPARequest bpaRequest) {
		if (BPAConstants.ACTION_FORWORD.equalsIgnoreCase(bpaRequest.getBPA().getWorkflow().getAction())
				&& bpaRequest.getRequestInfo().getUserInfo().getType().equalsIgnoreCase(BPAConstants.CITIZEN)) {
			BPA bpa = bpaRequest.getBPA();
			List<String> bpaDocuments = new ArrayList<>();
			for (Document doc : bpa.getDocuments()) {
				bpaDocuments.add(convertDocType(doc.getDocumentType()));
			}
			DocRemarkSearchCriteria criteria = DocRemarkSearchCriteria.builder()
					.businessId(bpaRequest.getBPA().getApplicationNo()).build();
			List<DocRemark> docRemarks = docRemarkService.search(criteria);
			if (!CollectionUtils.isEmpty(docRemarks)) {
				for (DocRemark docRemark : docRemarks) {
					if (docRemark.getIsUpdatable().equals(Boolean.TRUE)
							&& !bpaDocuments.contains(docRemark.getDocumentCode())) {
						throw new CustomException(BPAErrorConstants.BPA_UNKNOWN_DOCS_MSG,
								"Some documents have been marked for upload by the employee. Please upload the required documents.");
					}
				}
			}
		}

	}


	public void validateOtherFees(BPARequest bpaRequest) {

		Map<String, String> additionalDetails = bpaRequest.getBPA().getAdditionalDetails() != null
				? (Map) bpaRequest.getBPA().getAdditionalDetails()
				: new HashMap<>();

		if (additionalDetails.get(BPAConstants.BPA_ADD_DETAILS_OTHER_FEE_ADJUSTMENT_AMOUNT_KEY) != null) {
			List<Map<String, Object>> otherFees = new ArrayList<>();

			for (Map<String, Object> fee : otherFees) {
				String amount = fee.get("amount") != null ? fee.get("amount").toString().trim() : "";
				String reason = fee.get("reason") != null ? fee.get("reason").toString().trim() : "";
				String comment = fee.get("comment") != null ? fee.get("comment").toString().trim() : "";

				if (!amount.isEmpty()) {
					if (reason.isEmpty() || comment.isEmpty()) {
						throw new IllegalArgumentException("Invalid Other Fee at order: " + fee.get("order"));
					}
				}
			}

		}

	}


	public void validateCompletionCertificateCreateRequest(
			@Valid CompletionCertificateRequest completionCertificateRequest) {
		CompletionCertificate cc = completionCertificateRequest.getCompletionCertificate();

		if (ObjectUtils.isEmpty(cc)) {
			throw new CustomException(BPAErrorConstants.INVALID_CREATE,
					"Completion Certificate object cannot be null or empty.");
		}
		
		if (StringUtils.isEmpty(cc.getTenantId())) {
			throw new CustomException(BPAErrorConstants.INVALID_TENANT,
					"Tenant Id cannot be null or empty.");
		}
		
		if (StringUtils.isEmpty(cc.getBpaPermitNumber())) {
			throw new CustomException(BPAErrorConstants.CREATE_ERROR,
					"BPA Permit number cannot be null or empty.");
		}

	}


	public void validateCompletionCertificateSearchRequest(@Valid CompletionCertificateSearchCriteria criteria) {

		if (ObjectUtils.isEmpty(criteria.getLimit())) {
			throw new CustomException(BPAErrorConstants.INVALID_SEARCH,
					"Limit can not be null or empty for Completion Certificate.");
		}

		if (ObjectUtils.isEmpty(criteria.getOffset())) {
			throw new CustomException(BPAErrorConstants.INVALID_SEARCH,
					"Offset can not be null or empty for Completion Certificate.");
		}
	}


	public void validateStageWiseReportRequest(@Valid StageWiseReportRequest stageWiseReportRequest) {

		if (stageWiseReportRequest == null || stageWiseReportRequest.getStageWiseReports() == null
				|| stageWiseReportRequest.getStageWiseReports().isEmpty()) {

			throw new CustomException(BPAErrorConstants.INVALID_REQUEST, "stageWiseReports cannot be empty");
		}

		for (StageWiseReport report : stageWiseReportRequest.getStageWiseReports()) {

			validateStageWiseReportMandatoryFields(report);
			validateStageWiseReportLevelType(report);

			// Validate documentDetails count
			int count = getDocumentDetailsCount(report.getDocumentDetails());
			if (count > config.getStageWiseReportMaxFilestoreLimit()) {
				throw new CustomException(BPAErrorConstants.MAX_DOCUMENTS_EXCEEDED, "DocumentDetails cannot contain more than "
						+ config.getStageWiseReportMaxFilestoreLimit() + " filestore entries.");
			}

		}
	}

	private void validateStageWiseReportLevelType(StageWiseReport report) {
		try {
			StageWiseReport.LevelType.valueOf(report.getLevelType().name());
		} catch (Exception e) {
			throw new CustomException("INVALID_LEVEL_TYPE", "Level Type must be: FOUNDATION, PLINTH, FLOOR");
		}
	}

	private void validateStageWiseReportMandatoryFields(StageWiseReport report) {
		if (report.getApplicationNo() == null || report.getApplicationNo().trim().isEmpty()) {
			throw new CustomException(BPAErrorConstants.MANDATORY_FIELD_MISSING, "Application No is mandatory");
		}
		if (report.getLevelType() == null) {
			throw new CustomException(BPAErrorConstants.MANDATORY_FIELD_MISSING, "Level Type is mandatory");
		}
		if (report.getBlockNo() == null || report.getBlockNo().trim().isEmpty()) {
			throw new CustomException(BPAErrorConstants.MANDATORY_FIELD_MISSING, "Block No is mandatory");
		}
		if (report.getFloorNo() == null || report.getFloorNo().trim().isEmpty()) {
			throw new CustomException(BPAErrorConstants.MANDATORY_FIELD_MISSING, "Floor No is mandatory");
		}
	}

	private int getDocumentDetailsCount(Object docDetails) {
		if (docDetails == null)
			return 0;
		if (docDetails instanceof List<?>)
			return ((List<?>) docDetails).size();
		if (docDetails instanceof Map)
			return 1;
		throw new CustomException("INVALID_DOCUMENT_DETAILS", "documentDetails must be an object or a list of objects");
	}
	
}
