package org.egov.bpa.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URL;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.repository.RevalidationRepository;
import org.egov.bpa.repository.ServiceRequestRepository;
import org.egov.bpa.util.BPAConstants;
import org.egov.bpa.util.BPAErrorConstants;
import org.egov.bpa.web.model.BPA;
import org.egov.bpa.web.model.BPARequest;
import org.egov.bpa.web.model.BPASearchCriteria;
import org.egov.bpa.web.model.Document;
import org.egov.bpa.web.model.Revalidation;
import org.egov.bpa.web.model.RevalidationRequest;
import org.egov.bpa.web.model.RevalidationSearchCriteria;
import org.egov.bpa.web.model.revalidation.Block;
import org.egov.bpa.web.model.revalidation.Floor;
import org.egov.bpa.web.model.revalidation.Occupancy;
import org.egov.bpa.web.model.revalidation.OccupancyEnum;
import org.egov.bpa.web.model.revalidation.RevalidationConstants;
import org.egov.bpa.web.model.revalidation.RevalidationPlanNonSujog;
import org.egov.bpa.web.model.revalidation.SubOccupancyEnum;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.google.gson.Gson;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RevalidationService {

	@Autowired
	private RevalidationRepository repository;

	@Autowired
	private EDCRService edcrService;

	@Autowired
	private BPAConfiguration config;

	@Autowired
	private BPAService bpaService;

	@Autowired
	EnrichmentService enrichmentService;

	@Autowired
	private ServiceRequestRepository serviceRequestRepository;

	@Autowired
	private CalculationService calculationService;

	/**
	 * does all the validations required to create BPA Record in the system
	 * 
	 * @param bpaRequest
	 * @return
	 */
	public Revalidation create(RevalidationRequest revalidationRequest) {

		//validateRevalidationAlreadyExists(revalidationRequest);
		
		// ✅ Step 2: Add BPL docs from reference (IMPORTANT)
		log.info("Disabled validation inside create() Revalidation");
	    try {
	        addBplDocsFromReference(revalidationRequest);
	    } catch (Exception e) {
	        log.error("Error while fetching BPL docs from reference application", e);
	    }
	    
		enrichmentService.enrichRevalidationCreateRequest(revalidationRequest);
		Revalidation revalidation = revalidationRequest.getRevalidation();
		revalidation.setPermitExpiryDate(this.modifyDate(revalidation.getPermitDate(), 3));
		
		log.info("Print the dates : permit Date "+ revalidation.getPermitDate() + 
				" "
				+ " :: Permit Expiry Date :: "+ revalidation.getPermitExpiryDate());
		
		// repository.save(revalidationRequest);
		return revalidationRequest.getRevalidation();
	}

	/**
	 * Validates that no revalidation request already exists for the given
	 * application.
	 * 
	 * <p>
	 * This method performs three validation checks:
	 * <ol>
	 * <li>Checks if revalidation exists for the BPA application number</li>
	 * <li>Checks if revalidation exists for the reference BPA application
	 * number</li>
	 * <li>Checks if revalidation exists for the permit number (with active status
	 * check)</li>
	 * </ol>
	 * </p>
	 * 
	 * @param revalidationRequest The revalidation request to validate
	 * @throws CustomException if revalidation already exists for any of the
	 *                         identifiers
	 */
	private void validateRevalidationAlreadyExists(RevalidationRequest revalidationRequest) {

		Revalidation revalidation = revalidationRequest.getRevalidation();

		// Validation 1: Check by BPA Application Number
		validateByBpaApplicationNo(
			    revalidation.getBpaApplicationNo(),
			    revalidationRequest.getRequestInfo()
			);

		// Validation 2: Check by Reference BPA Application Number
		validateByRefBpaApplicationNo(
			    revalidation.getRefBpaApplicationNo(),
			    revalidationRequest.getRequestInfo()
			);

		// Validation 3: Check by Permit Number (with active status check)
		validateByPermitNo(revalidation.getPermitNo(), revalidationRequest.getRequestInfo());
	}

	/**
	 * Validates that no revalidation exists for the given BPA application number.
	 * 
	 * @param bpaApplicationNo The BPA application number to check
	 * @throws CustomException if revalidation already exists
	 */
//	old code
//	private void validateByBpaApplicationNo(String bpaApplicationNo) {
//
//		if (StringUtils.isEmpty(bpaApplicationNo)) {
//			return;
//		}
//
//		RevalidationSearchCriteria searchCriteria = RevalidationSearchCriteria.builder()
//				.bpaApplicationNo(bpaApplicationNo).build();
//
//		List<Revalidation> existingRevalidations = repository.getRevalidationData(searchCriteria);
//
//		if (Objects.nonNull(existingRevalidations) && !existingRevalidations.isEmpty()) {
//			throw new CustomException("Validation Error",
//					"Found already existing revalidation data for given bpaApplicationNo: " + bpaApplicationNo);
//		}
//	}
	private void validateByBpaApplicationNo(String bpaApplicationNo, RequestInfo requestInfo) {

	    final String method = "validateByBpaApplicationNo";
	    log.info("[{}] Start :: bpaApplicationNo={}", method, bpaApplicationNo);

	    if (StringUtils.isEmpty(bpaApplicationNo)) {
	        log.warn("[{}] Empty bpaApplicationNo → Skipping", method);
	        return;
	    }

	    List<Revalidation> revalidations;

	    try {
	        revalidations = repository.getRevalidationData(
	                RevalidationSearchCriteria.builder()
	                        .bpaApplicationNo(bpaApplicationNo)
	                        .build()
	        );
	    } catch (Exception ex) {
	        log.error("[{}] DB error while fetching data", method, ex);
	        throw new CustomException("SYSTEM_ERROR",
	                "Unable to validate existing revalidation (BPA Application No)");
	    }

	    if (CollectionUtils.isEmpty(revalidations)) {
	        log.info("[{}] No existing records found", method);
	        return;
	    }

	    for (Revalidation revalidation : revalidations) {

	        if (revalidation == null) continue;

	        String applicationNo = revalidation.getBpaApplicationNo();
	        if (StringUtils.isEmpty(applicationNo)) continue;

	        if (isAssociatedBpaActive(applicationNo, requestInfo)) {

	            log.error("[{}] ACTIVE BPA found → Blocking bpaApplicationNo={}", method, bpaApplicationNo);

	            throw new CustomException("Validation Error",
	                    "Revalidation already exists for BPA Application No: " + bpaApplicationNo);
	        }
	    }

	    log.info("[{}] Completed successfully", method);
	}
	/**
	 * Validates that no revalidation exists for the given reference BPA application
	 * number.
	 * 
	 * @param refBpaApplicationNo The reference BPA application number to check
	 * @throws CustomException if revalidation already exists
	 */
//	//old code
//	private void validateByRefBpaApplicationNo(String refBpaApplicationNo) {
//
//		if (StringUtils.isEmpty(refBpaApplicationNo)) {
//			return;
//		}
//
//		RevalidationSearchCriteria searchCriteria = RevalidationSearchCriteria.builder()
//				.refBpaApplicationNo(refBpaApplicationNo).build();
//
//		List<Revalidation> existingRevalidations = repository.getRevalidationData(searchCriteria);
//
//		if (Objects.nonNull(existingRevalidations) && !existingRevalidations.isEmpty()) {
//			throw new CustomException("Validation Error",
//					"Found already existing revalidation data for given RefBpaApplicationNo: " + refBpaApplicationNo);
//		}
//	}
	private void validateByRefBpaApplicationNo(String refBpaApplicationNo, RequestInfo requestInfo) {

	    final String method = "validateByRefBpaApplicationNo";
	    log.info("[{}] Start :: refBpaApplicationNo={}", method, refBpaApplicationNo);

	    if (StringUtils.isEmpty(refBpaApplicationNo)) {
	        return;
	    }

	    List<Revalidation> revalidations;

	    try {
	        revalidations = repository.getRevalidationData(
	                RevalidationSearchCriteria.builder()
	                        .refBpaApplicationNo(refBpaApplicationNo)
	                        .build()
	        );
	    } catch (Exception ex) {
	        log.error("[{}] DB error", method, ex);
	        throw new CustomException("SYSTEM_ERROR",
	                "Unable to validate existing revalidation (Ref BPA Application No)");
	    }

	    if (CollectionUtils.isEmpty(revalidations)) {
	        return;
	    }

	    for (Revalidation revalidation : revalidations) {

	        if (revalidation == null) continue;

	        String applicationNo = revalidation.getBpaApplicationNo();
	        if (StringUtils.isEmpty(applicationNo)) continue;

	        if (isAssociatedBpaActive(applicationNo, requestInfo)) {

	            log.error("[{}] ACTIVE BPA found → Blocking refBpaApplicationNo={}", method, refBpaApplicationNo);

	            throw new CustomException("Validation Error",
	                    "Revalidation already exists for Ref BPA Application No: " + refBpaApplicationNo);
	        }
	    }

	    log.info("[{}] Completed successfully", method);
	}	
	/**
	
	 * Validates that no active revalidation exists for the given permit number.
	 * 
	 * <p>
	 * This validation includes an additional check to verify the associated BPA
	 * application is not in DELETED status. Deleted applications are allowed to
	 * have new revalidation requests.
	 * </p>
	 * 
	 * @param permitNo    The permit number to check
	 * @param requestInfo The request info for BPA search
	 * @throws CustomException if active revalidation already exists
	 */
//	Old logic
//	private void validateByPermitNo(String permitNo, RequestInfo requestInfo) {
//
//		if (StringUtils.isEmpty(permitNo)) {
//			return;
//		}
//
//		RevalidationSearchCriteria searchCriteria = RevalidationSearchCriteria.builder().permitNo(permitNo).build();
//
//		List<Revalidation> existingRevalidations = repository.getRevalidationData(searchCriteria);
//
//		if (CollectionUtils.isEmpty(existingRevalidations)) {
//			return;
//		}
//
//		// Check if the associated BPA application is active (not deleted)
//		String applicationNo = existingRevalidations.get(0).getBpaApplicationNo();
//
//		if (isAssociatedBpaActive(applicationNo, requestInfo)) {
//			throw new CustomException("Validation Error",
//					"Found already existing revalidation data for given PermitNo: " + permitNo);
//		}
//	}
//	new updated logic
	private void validateByPermitNo(String permitNo, RequestInfo requestInfo) {

	    final String method = "validateByPermitNo";
	    log.info("[{}] Start :: permitNo={}", method, permitNo);

	    if (StringUtils.isEmpty(permitNo)) {
	        return;
	    }

	    List<Revalidation> revalidations;

	    try {
	        revalidations = repository.getRevalidationData(
	                RevalidationSearchCriteria.builder()
	                        .permitNo(permitNo)
	                        .build()
	        );
	    } catch (Exception ex) {
	        log.error("[{}] DB error", method, ex);
	        throw new CustomException("SYSTEM_ERROR",
	                "Unable to validate permit number. Please try again.");
	    }

	    if (CollectionUtils.isEmpty(revalidations)) {
	        log.info("[{}] No existing revalidation found", method);
	        return;
	    }

	    for (Revalidation revalidation : revalidations) {

	        if (revalidation == null) continue;

	        String applicationNo = revalidation.getBpaApplicationNo();
	        if (StringUtils.isEmpty(applicationNo)) continue;

	        if (isAssociatedBpaActive(applicationNo, requestInfo)) {

	            log.error("[{}] ACTIVE BPA exists → Blocking permitNo={}", method, permitNo);

	            throw new CustomException("Validation Error",
	                    "Revalidation already exists for Permit No: " + permitNo);
	        }
	    }

	    log.info("[{}] All BPA are DELETED → Allowing", method);
	}
	/**
	 * Checks if the associated BPA application is active (not deleted).
	 * 
	 * @param applicationNo The application number to check
	 * @param requestInfo   The request info for BPA search
	 * @return true if BPA exists and is not in DELETED status
	 */
//	private boolean isAssociatedBpaActive(String applicationNo, RequestInfo requestInfo) {
//
//		BPASearchCriteria bpaSearchCriteria = BPASearchCriteria.builder().applicationNo(applicationNo).build();
//
//		List<BPA> bpas = bpaService.getBPAFromCriteria(bpaSearchCriteria, requestInfo, null);
//
//		// If BPA exists and is not deleted, it's considered active
//		return !CollectionUtils.isEmpty(bpas) && !BPAConstants.BPA_DELETED.equalsIgnoreCase(bpas.get(0).getStatus());
//	}
// new code
	private boolean isAssociatedBpaActive(String applicationNo, RequestInfo requestInfo) {

	    final String method = "isAssociatedBpaActive";
	    log.info("[{}] Checking BPA status for applicationNo={}", method, applicationNo);

	    if (StringUtils.isEmpty(applicationNo)) {
	        return false;
	    }

	    List<BPA> bpas;

	    try {
	        bpas = bpaService.getBPAFromCriteria(
	                BPASearchCriteria.builder().applicationNo(applicationNo).build(),
	                requestInfo,
	                null
	        );
	    } catch (Exception ex) {
	        log.error("[{}] BPA service error → applicationNo={}", method, applicationNo, ex);

	        throw new CustomException("SYSTEM_ERROR",
	                "Unable to verify BPA status. Please try again.");
	    }

	    if (CollectionUtils.isEmpty(bpas)) {
	        log.info("[{}] No BPA found → treating as NOT ACTIVE", method);
	        return false;
	    }

	    for (BPA bpa : bpas) {

	        if (bpa == null) continue;

	        String status = bpa.getStatus();
	        if (status == null) continue;

	        if (!status.trim().equalsIgnoreCase(BPAConstants.BPA_DELETED)) {

	            log.info("[{}] ACTIVE BPA found → applicationNo={}, status={}",
	                    method, applicationNo, status);

	            return true;
	        }
	    }

	    log.info("[{}] All BPA are DELETED → applicationNo={}", method, applicationNo);
	    return false;
	}
	
	private boolean isBPRVInProgress(String applicationNo, RequestInfo requestInfo) {

	    final String method = "isBPRVInProgress";
	    log.info("[{}] Checking BPA status for applicationNo={}", method, applicationNo);

	    if (StringUtils.isEmpty(applicationNo)) {
	        return false;
	    }

	    List<BPA> bpas;

	    try {
	        bpas = bpaService.getBPAFromCriteria(
	                BPASearchCriteria.builder().applicationNo(applicationNo).build(),
	                requestInfo,
	                null
	        );
	    } catch (Exception ex) {
	        log.error("[{}] BPA service error → applicationNo={}", method, applicationNo, ex);

	        throw new CustomException("SYSTEM_ERROR",
	                "Unable to verify BPA status. Please try again.");
	    }

	    if (CollectionUtils.isEmpty(bpas)) {
	        log.info("[{}] No BPA found → treating as NOT ACTIVE", method);
	        return false;
	    }
	    
	    BPA bpa = bpas.get(0);
	    if(!BPAConstants.BPA_DELETED.equalsIgnoreCase(bpa.getStatus())
	    		&& (bpa.getApprovalDate() == null || bpa.getApprovalDate().longValue()==0)) {
	    	//Application not yet digitally signed- approval date will be null or 0
	    	if(isPermitExpired(bpa.getPermitExpiryDate()) == false)
           log.info("[{}] ACTIVE BPA found → applicationNo={}, status={}",
        		   method, applicationNo, bpa.getStatus());
           return true;
	    }
	    
	    log.info("[{}] All BPA are processed → applicationNo={}", method, applicationNo);
	    return false;
	}
	
	/**
	 * Returns the revalidation with enriched owners from user service
	 * 
	 * @param criteria    The object containing the parameters on which to search
	 * @param requestInfo The search request's requestInfo
	 * @return List of revalidation for the given criteria
	 */
	public List<Revalidation> getRevalidationFromCriteria(RevalidationSearchCriteria criteria) {
		List<Revalidation> revalidations = repository.getRevalidationData(criteria);
		if (revalidations.isEmpty())
			return Collections.emptyList();
		return revalidations;
	}

	/**
	 * Updates an existing revalidation request in the system.
	 * 
	 * <p>
	 * This method orchestrates the update process:
	 * <ol>
	 * <li>Validates that the revalidation has a valid ID</li>
	 * <li>Fetches and validates the existing revalidation from database</li>
	 * <li>Preserves the original audit details (created by, created time)</li>
	 * <li>Enriches the request with update metadata</li>
	 * <li>Persists the updated revalidation via repository</li>
	 * </ol>
	 * </p>
	 * 
	 * @param revalidationRequest The update request containing revalidation details
	 * @return The updated revalidation entity
	 * @throws CustomException if revalidation ID is missing or not found in
	 *                         database
	 */
	@SuppressWarnings("unchecked")
	public Revalidation update(RevalidationRequest revalidationRequest) {

		Revalidation revalidation = revalidationRequest.getRevalidation();

		// Step 1: Validate revalidation ID is present
		validateRevalidationIdPresent(revalidation);

		// Step 2: Fetch and validate existing revalidation from database
		Revalidation existingRevalidation = fetchAndValidateExistingRevalidation(revalidationRequest);
		
		// ✅ BPL doc fetch from reference application
	    try {
	        addBplDocsFromReference(revalidationRequest);
	    } catch (Exception e) {
	        log.error("Error while fetching BPL docs from reference application", e);
	    }

		// Step 3: Preserve original audit details (created by, created time)
		revalidation.setAuditDetails(existingRevalidation.getAuditDetails());

		// Step 4: Enrich request with update metadata
		enrichmentService.enrichRevalidationUpdateRequest(revalidationRequest);

		// Step 5: Persist update to database
		repository.update(revalidationRequest);

		return revalidationRequest.getRevalidation();
	}
	
	private void addBplDocsFromReference(RevalidationRequest request) {

	    if (request == null || request.getRevalidation() == null) {
	        log.warn("Revalidation request is null, skipping BPL enrichment");
	        return;
	    }

	    Revalidation revalidation = request.getRevalidation();

	    // ✅ Only for Sujog applications
	    if (!revalidation.isSujogExistingApplication()) {
	        log.debug("Skipping BPL enrichment - Not a Sujog application");
	        return;
	    }

	    // ✅ Ref number is mandatory for fetch
	    if (revalidation.getRefBpaApplicationNo() == null) {
	        log.warn("Ref BPA Application No is null, cannot fetch BPL docs");
	        return;
	    }

	    try {
	        log.info("Fetching BPL docs for Ref Application No: {}", 
	                revalidation.getRefBpaApplicationNo());

	        // Step 1: Fetch BPA
	        BPASearchCriteria criteria = BPASearchCriteria.builder()
	                .applicationNo(revalidation.getRefBpaApplicationNo())
	                .tenantId(revalidation.getTenantId())
	                .build();

	        List<BPA> bpas = bpaService.getBPAFromCriteria(criteria, request.getRequestInfo(), null);

	        if (CollectionUtils.isEmpty(bpas)) {
	            log.warn("No BPA found for Ref Application No: {}", 
	                    revalidation.getRefBpaApplicationNo());
	            return;
	        }

	        BPA refBpa = bpas.get(0);

	        if (CollectionUtils.isEmpty(refBpa.getDocuments())) {
	            log.warn("No documents found in reference BPA");
	            return;
	        }

	        // Ensure target list exists
	        if (CollectionUtils.isEmpty(revalidation.getDocuments())) {
	            revalidation.setDocuments(new ArrayList<>());
	        }

	        int addedCount = 0;

	        List<Document> refDocs = refBpa.getDocuments();

	        // 🔥 STEP 1: Find SIGNED BPL
	        List<Document> signedDocs = refDocs.stream()
	                .filter(doc -> doc != null &&
	                        doc.getDocumentType() != null &&
	                        doc.getFileStoreId() != null &&
	                        "BPD.SIGNED.BPL".equalsIgnoreCase(doc.getDocumentType()))
	                .collect(java.util.stream.Collectors.toList());

	        // 🔥 STEP 2: If SIGNED not found → find NORMAL BPL
	        List<Document> normalDocs = refDocs.stream()
	                .filter(doc -> doc != null &&
	                        doc.getDocumentType() != null &&
	                        doc.getFileStoreId() != null &&
	                        "BPD.BPL.BPL".equalsIgnoreCase(doc.getDocumentType()))
	                .collect(java.util.stream.Collectors.toList());

	        List<Document> finalDocs;

	        if (!CollectionUtils.isEmpty(signedDocs)) {
	            finalDocs = signedDocs; // ✅ priority 1
	            log.info("Using SIGNED BPL documents");
	        } else if (!CollectionUtils.isEmpty(normalDocs)) {
	            finalDocs = normalDocs; // ✅ fallback
	            log.info("Using NORMAL BPL documents");
	        } else {
	            log.info("No BPL documents found to add");
	            return;
	        }

	        // 🔥 STEP 3: Add with duplicate check
	        for (Document doc : finalDocs) {

	            boolean exists = revalidation.getDocuments().stream()
	                    .anyMatch(d ->
	                            doc.getFileStoreId().equals(d.getFileStoreId()) &&
	                            doc.getDocumentType().equalsIgnoreCase(d.getDocumentType())
	                    );

	            if (!exists) {

	                Document newDoc = Document.builder()
//	                        .documentType(doc.getDocumentType())
	                		.documentType("BPD.BPL.BPL")
	                        .fileStoreId(doc.getFileStoreId())
	                        .documentUid(doc.getDocumentUid())
	                        .additionalDetails(doc.getAdditionalDetails())
	                        .build();

	                revalidation.getDocuments().add(newDoc);
	                addedCount++;

	                log.debug("Added BPL doc: {}", doc.getDocumentType());
	            }
	        }

	        log.info("BPL enrichment completed. Total added: {}", addedCount);

	    } catch (Exception e) {
	        log.error("Error while fetching BPL docs for Ref Application No: {}", 
	                revalidation.getRefBpaApplicationNo(), e);
	    }
	}	
	/**
	 * Validates that the revalidation has a valid ID for update operation.
	 * 
	 * @param revalidation The revalidation entity to validate
	 * @throws CustomException if revalidation ID is null
	 */
	private void validateRevalidationIdPresent(Revalidation revalidation) {

		if (revalidation.getId() == null) {
			throw new CustomException(BPAErrorConstants.UPDATE_ERROR,
					"Revalidation not found in the System" + revalidation);
		}
	}

	/**
	 * Fetches and validates the existing revalidation from the database.
	 * 
	 * <p>
	 * Ensures that exactly one revalidation exists with the given ID and tenant ID.
	 * </p>
	 * 
	 * @param revalidationRequest The request containing revalidation with ID to
	 *                            search
	 * @return The existing revalidation from database
	 * @throws CustomException if no revalidation found or multiple revalidations
	 *                         found
	 */
	private Revalidation fetchAndValidateExistingRevalidation(RevalidationRequest revalidationRequest) {

		List<Revalidation> searchResult = getRevalidationsWithId(revalidationRequest);

		if (CollectionUtils.isEmpty(searchResult) || searchResult.size() > 1) {
			throw new CustomException(BPAErrorConstants.UPDATE_ERROR,
					"Failed to Update the Application, Found None or multiple revalidation application!");
		}

		return searchResult.get(0);
	}

	/**
	 * Returns Revalidation from db for the update request
	 * 
	 * @param request The update request
	 * @return List of Revalidation
	 */
	public List<Revalidation> getRevalidationsWithId(RevalidationRequest request) {
		RevalidationSearchCriteria criteria = new RevalidationSearchCriteria();
		List<String> ids = new LinkedList<>();
		ids.add(request.getRevalidation().getId());
		criteria.setTenantId(request.getRevalidation().getTenantId());
		criteria.setIds(ids);
		List<Revalidation> revalidations = repository.getRevalidationData(criteria);
		return revalidations;
	}

	public BPARequest getBPARequestFromPermitNo(RevalidationRequest request) {
		BPASearchCriteria c = new BPASearchCriteria();
		c.setTenantId(request.getRevalidation().getTenantId());
		c.setApprovalNo(request.getRevalidation().getPermitNo());
		c.setApprovalDate(request.getRevalidation().getPermitDate());
		BPA bpa = bpaService.getBPAFromCriteria(c, request.getRequestInfo(), null).get(0);
		// bpa.setApplicationNo(request.getRevalidation().getBpaApplicationNo());
		BPARequest bpaRequest = new BPARequest();
		bpaRequest.setRequestInfo(request.getRequestInfo());
		bpaRequest.setBPA(bpa);
		bpaRequest.setRevalidation(request.getRevalidation());
		return bpaRequest;
	}

	public long modifyDate(long milliseconds, int year) {
		Instant instant = Instant.ofEpochMilli(milliseconds);
		ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(instant, ZoneOffset.UTC);
		ZonedDateTime newZonedDateTime = zonedDateTime.plusYears(year);
		Instant newDate = newZonedDateTime.toInstant();
		return newDate.toEpochMilli();
	}

	/**
	 * Converts an Excel/CSV file from filestore to JSON format for revalidation
	 * plan.
	 * 
	 * <p>
	 * This method orchestrates the conversion process:
	 * <ol>
	 * <li>Builds the filestore URL and fetches the file download URL</li>
	 * <li>Reads and parses the CSV data from the file</li>
	 * <li>Validates headers and processes each row into block/floor/occupancy
	 * structure</li>
	 * <li>Converts the structured data to JSON format</li>
	 * </ol>
	 * </p>
	 * 
	 * @param fileStoreIds The filestore IDs to fetch
	 * @param tenantId     The tenant ID for filestore lookup
	 * @return JSON string representation of the revalidation plan
	 * @throws Exception if file reading fails or data validation fails
	 */
	public String convertExcelToJsonFromUrl(String fileStoreIds, String tenantId) throws Exception {

		// Step 1: Fetch the file download URL from filestore
		String fileUrl = fetchFileUrlFromFilestore(fileStoreIds, tenantId);

		// Step 2: Parse CSV data and build blocks structure
		List<Block> blocksList = parseCsvAndBuildBlocks(fileUrl);

		// Step 3: Convert to JSON and return
		return convertBlocksToJson(blocksList);
	}

	/**
	 * Fetches the file download URL from the filestore service.
	 * 
	 * @param fileStoreIds The filestore IDs
	 * @param tenantId     The tenant ID
	 * @return The download URL for the file
	 */
	@SuppressWarnings("unchecked")
	private String fetchFileUrlFromFilestore(String fileStoreIds, String tenantId) {

		// Build filestore API URL
		StringBuilder uri = new StringBuilder(config.getFilestoreHost());
		uri.append(config.getFilestoreFetchPathNew());

		if (tenantId != null) {
			uri.append("?tenantId=").append(tenantId);
		}
		if (fileStoreIds != null) {
			uri.append("&fileStoreIds=").append(fileStoreIds);
		}

		// Fetch result from filestore service
		LinkedHashMap<String, Object> fetchResult = (LinkedHashMap<String, Object>) serviceRequestRepository
				.fetchResultGet(uri);

		// Extract file URL from response
		ArrayList<?> fileStoreid = (ArrayList<?>) fetchResult.get("fileStoreIds");
		return ((HashMap<String, String>) fileStoreid.get(0)).get("url");
	}

	/**
	 * Parses CSV file from URL and builds the blocks structure.
	 * 
	 * @param fileUrl The URL to read the CSV file from
	 * @return List of Block objects with floor and occupancy data
	 * @throws Exception if parsing fails or data is invalid
	 */
	private List<Block> parseCsvAndBuildBlocks(String fileUrl) throws Exception {

		URL excelUrl = new URL(fileUrl);
		List<Block> blocksList = new ArrayList<>();

		try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(excelUrl.openStream()))) {

			String[] headers = null;
			String line;

			while ((line = bufferedReader.readLine()) != null) {
				String[] cells = line.split(",");

				if (headers == null) {
					// First row is headers
					headers = cells;
				} else {
					// Process data row
					Map<String, String> dataMap = buildDataMapFromRow(headers, cells);
					validateRequiredHeaders(dataMap);
					addRowToBlocksList(dataMap, blocksList);
				}
			}
		}

		return blocksList;
	}

	/**
	 * Builds a data map from CSV row using headers as keys.
	 * 
	 * @param headers The CSV header row
	 * @param cells   The current data row cells
	 * @return Map of header to cell value
	 * @throws Exception if empty cells are found
	 */
	private Map<String, String> buildDataMapFromRow(String[] headers, String[] cells) throws Exception {

		Map<String, String> dataMap = new LinkedHashMap<>();

		for (int i = 0; i < headers.length; i++) {
			String cellValue = i < cells.length ? cells[i].trim() : "";

			if (cellValue.isEmpty()) {
				throw new Exception("One or more empty cells found in the sheet");
			}

			dataMap.put(headers[i], cellValue);
		}

		return dataMap;
	}

	/**
	 * Validates that all required headers are present in the data map.
	 * 
	 * @param dataMap The data map to validate
	 * @throws Exception if required headers are missing
	 */
	private void validateRequiredHeaders(Map<String, String> dataMap) throws Exception {

		boolean hasAllRequiredHeaders = dataMap.containsKey(BPAConstants.Block_No)
				&& dataMap.containsKey(BPAConstants.Floor_No) && dataMap.containsKey(BPAConstants.Existing_Proposed)
				&& dataMap.containsKey(BPAConstants.Occupancy) && dataMap.containsKey(BPAConstants.Sub_Occupancy)
				&& dataMap.containsKey(BPAConstants.Total_Built_Up_Area)
				&& dataMap.containsKey(BPAConstants.Total_Floor_Area)
				&& dataMap.containsKey(BPAConstants.Total_Carpet_Area);

		if (!hasAllRequiredHeaders) {
			throw new Exception("Please input the correct headers");
		}
	}

	/**
	 * Adds a data row to the blocks list, creating or updating
	 * block/floor/occupancy structure.
	 * 
	 * <p>
	 * Hierarchy: Block -> Floor -> Occupancy If block already exists, adds to
	 * existing block. Same for floor within block.
	 * </p>
	 * 
	 * @param dataMap    The data map containing row values
	 * @param blocksList The list of blocks to add to
	 */
	private void addRowToBlocksList(Map<String, String> dataMap, List<Block> blocksList) {

		String blockNo = dataMap.get(BPAConstants.Block_No);
		String floorNo = dataMap.get(BPAConstants.Floor_No);

		// Create occupancy from row data
		Occupancy occupancy = createOccupancyFromDataMap(dataMap);

		// Find or create block
		Block existingBlock = findBlockByNumber(blocksList, Integer.parseInt(blockNo));

		if (existingBlock == null) {
			// Create new block with floor and occupancy
			Block newBlock = createNewBlock(Integer.parseInt(blockNo), Integer.parseInt(floorNo), occupancy);
			blocksList.add(newBlock);
		} else {
			// Add to existing block
			addOccupancyToExistingBlock(existingBlock, Integer.parseInt(floorNo), occupancy);
		}
	}

	/**
	 * Creates an Occupancy object from the data map.
	 * 
	 * @param dataMap The data map containing occupancy values
	 * @return The created Occupancy object
	 */
	private Occupancy createOccupancyFromDataMap(Map<String, String> dataMap) {

		Occupancy occupancy = new Occupancy();

		// Set type and subtype with their codes
		occupancy.setType(dataMap.get(BPAConstants.Occupancy));
		occupancy.setSubType(dataMap.get(BPAConstants.Sub_Occupancy));
		occupancy.setTypeCode(OccupancyEnum.fromString(occupancy.getType()).name());
		occupancy.setSubTypeCode(SubOccupancyEnum.fromString(occupancy.getSubType()).name().replace('_', '-'));

		// Set area values
		occupancy.setBuildupArea(new BigDecimal(Double.parseDouble(dataMap.get(BPAConstants.Total_Built_Up_Area))));
		occupancy.setFloorArea(new BigDecimal(Double.parseDouble(dataMap.get(BPAConstants.Total_Floor_Area))));
		occupancy.setCarpetArea(new BigDecimal(Double.parseDouble(dataMap.get(BPAConstants.Total_Carpet_Area))));
		occupancy.setAreaType(dataMap.get(BPAConstants.Existing_Proposed));

		return occupancy;
	}

	/**
	 * Finds a block by its number in the blocks list.
	 * 
	 * @param blocksList The list of blocks to search
	 * @param blockNo    The block number to find
	 * @return The block if found, null otherwise
	 */
	private Block findBlockByNumber(List<Block> blocksList, int blockNo) {

		return blocksList.stream().filter(b -> b.getNumber().equals(blockNo)).findFirst().orElse(null);
	}

	/**
	 * Creates a new Block with the given floor and occupancy.
	 * 
	 * @param blockNo   The block number
	 * @param floorNo   The floor number
	 * @param occupancy The occupancy to add
	 * @return The created Block
	 */
	private Block createNewBlock(int blockNo, int floorNo, Occupancy occupancy) {

		Block block = new Block();
		block.setNumber(blockNo);

		Floor floor = new Floor();
		floor.setNumber(floorNo);

		List<Occupancy> occupanciesList = new ArrayList<>();
		occupanciesList.add(occupancy);
		floor.setOccupancies(occupanciesList);

		List<Floor> floorsList = new ArrayList<>();
		floorsList.add(floor);
		block.setFloors(floorsList);

		return block;
	}

	/**
	 * Adds an occupancy to an existing block, creating floor if needed.
	 * 
	 * @param existingBlock The existing block to add to
	 * @param floorNo       The floor number
	 * @param occupancy     The occupancy to add
	 */
	private void addOccupancyToExistingBlock(Block existingBlock, int floorNo, Occupancy occupancy) {

		List<Floor> existingFloorsList = existingBlock.getFloors();

		// Find existing floor
		Floor existingFloor = existingFloorsList.stream().filter(f -> f.getNumber().equals(floorNo)).findFirst()
				.orElse(null);

		if (existingFloor == null) {
			// Create new floor with occupancy
			Floor newFloor = new Floor();
			newFloor.setNumber(floorNo);

			List<Occupancy> occupanciesList = new ArrayList<>();
			occupanciesList.add(occupancy);
			newFloor.setOccupancies(occupanciesList);

			existingFloorsList.add(newFloor);
		} else {
			// Add occupancy to existing floor
			existingFloor.getOccupancies().add(occupancy);
		}
	}

	/**
	 * Converts the blocks list to JSON string.
	 * 
	 * @param blocksList The list of blocks to convert
	 * @return JSON string representation
	 */
	private String convertBlocksToJson(List<Block> blocksList) {

		RevalidationPlanNonSujog revalidationPlanNonSujog = new RevalidationPlanNonSujog();
		revalidationPlanNonSujog.setBlocks(blocksList);

		Gson gson = new Gson();
		String jsonOutput = gson.toJson(revalidationPlanNonSujog);

		log.debug("Generated JSON output: {}", jsonOutput);

		return jsonOutput;
	}

	public boolean isThreeYearsOld(long dateInMillis) {
		long currentMillis = System.currentTimeMillis();
		long threeYearsInMillis = TimeUnit.DAYS.toMillis(3 * 365);
		long threeYearsAgoMillis = currentMillis - threeYearsInMillis;
		log.info("currentMillis:" + currentMillis + " ,threeYearsAgoMillis:" + threeYearsAgoMillis);
		return dateInMillis <= threeYearsAgoMillis;
	}
	
	public boolean isPermitExpired(long permitExpiryMillis) {
	    return Instant.ofEpochMilli(permitExpiryMillis)
	                  .isBefore(Instant.now());
	}

	/**
	 * Calculates and applies revalidation fees for a BPA application.
	 * 
	 * <p>
	 * This method orchestrates the fee calculation process:
	 * <ol>
	 * <li>Preserves the original business service and trims it for calculation</li>
	 * <li>Extracts application type and service type based on SUJOG/Non-SUJOG
	 * application</li>
	 * <li>Processes either application fee or sanction fee based on fee type</li>
	 * <li>Restores the original business service after calculation</li>
	 * </ol>
	 * </p>
	 * 
	 * @param bpaRequest The BPA request containing application details
	 * @param feeType    The type of fee to calculate (APPLICATION_FEE_KEY or
	 *                   SANCTION_FEE_KEY)
	 */
	@SuppressWarnings("unchecked")
	public void revalidationFee(BPARequest bpaRequest, String feeType) {

		log.info("Inside method RevalidationFee with feeType: {}", feeType);

		// Step 1: Preserve original business service and trim for calculation
		String originalBusinessService = bpaRequest.getBPA().getBusinessService();
		trimBusinessServiceForCalculation(bpaRequest);

		// Step 2: Extract application type and service type
		ApplicationTypeDetails typeDetails = extractApplicationTypeDetails(bpaRequest);

		// Step 3: Process fee based on fee type
		processFeeByType(bpaRequest, feeType, typeDetails);

		// Step 4: Restore original business service
		bpaRequest.getBPA().setBusinessService(originalBusinessService);
		log.info("Restored original BusinessService: {}", originalBusinessService);
	}

	/**
	 * Trims the business service to first 4 characters for fee calculation.
	 * 
	 * @param bpaRequest The BPA request to update
	 */
	private void trimBusinessServiceForCalculation(BPARequest bpaRequest) {

		String businessService = bpaRequest.getBPA().getBusinessService();
		log.info("Original BusinessService: {}", businessService);

		if (businessService != null && businessService.length() >= 4) {
			String trimmedService = businessService.substring(0, 4);
			bpaRequest.getBPA().setBusinessService(trimmedService);
			log.info("Trimmed BusinessService to: {}", trimmedService);
		}
	}

	/**
	 * Extracts application type and service type details based on application
	 * source.
	 * 
	 * <p>
	 * For Non-SUJOG applications, extracts from additional details. For SUJOG
	 * applications, fetches from EDCR service.
	 * </p>
	 * 
	 * @param bpaRequest The BPA request
	 * @return ApplicationTypeDetails containing applicationType and serviceType
	 */
	private ApplicationTypeDetails extractApplicationTypeDetails(BPARequest bpaRequest) {

		String applicationType = null;
		String serviceType = null;

		if (!bpaRequest.getRevalidation().isSujogExistingApplication()) {
			// Non-SUJOG: Extract from additional details
			log.info("Processing Non-Sujog existing application");
			DocumentContext context = JsonPath.parse(bpaRequest.getBPA().getAdditionalDetails());
			applicationType = context.read("$.RevalidationPlanNonSujog.applicationType");
			serviceType = context.read("$.RevalidationPlanNonSujog.serviceType");
		} else {
			// SUJOG: Fetch from EDCR service
			log.info("Processing Sujog existing application");
			Map<String, String> edcrResponse = edcrService.getEDCRDetails(bpaRequest.getRequestInfo(),
					bpaRequest.getBPA());
			log.info("Received EDCR Response: {}", edcrResponse);
			bpaRequest.setEdcrResponse(edcrResponse);
			applicationType = edcrResponse.get(BPAConstants.APPLICATIONTYPE);
			serviceType = edcrResponse.get(BPAConstants.SERVICETYPE);
		}

	    log.info("Extracted applicationType: {}, serviceType: {}", applicationType, serviceType);

	    return new ApplicationTypeDetails(applicationType, serviceType);
	}

	/**
	 * Processes fee calculation based on the fee type.
	 * 
	 * @param bpaRequest  The BPA request
	 * @param feeType     The fee type (APPLICATION_FEE_KEY or SANCTION_FEE_KEY)
	 * @param typeDetails The application type details
	 */
	@SuppressWarnings("unchecked")
	private void processFeeByType(BPARequest bpaRequest, String feeType, ApplicationTypeDetails typeDetails) {

		if (BPAConstants.APPLICATION_FEE_KEY.equals(feeType)) {
			processApplicationFee(bpaRequest, feeType, typeDetails);
		} else if (BPAConstants.SANCTION_FEE_KEY.equals(feeType)) {
			processSanctionFee(bpaRequest, feeType, typeDetails);
		}
	}

	/**
	 * Processes application fee calculation.
	 * 
	 * @param bpaRequest  The BPA request
	 * @param feeType     The fee type
	 * @param typeDetails The application type details
	 */
	private void processApplicationFee(BPARequest bpaRequest, String feeType, ApplicationTypeDetails typeDetails) {

		log.info("Calculating Application Fee");
		calculationService.addCalculationV2(bpaRequest, feeType, typeDetails.getApplicationType(),
				typeDetails.getServiceType());
	}

	/**
	 * Processes sanction fee calculation.
	 * 
	 * <p>
	 * Sanction fee is only calculated if the other fee adjustment amount is present
	 * in additional details.
	 * </p>
	 * 
	 * @param bpaRequest  The BPA request
	 * @param feeType     The fee type
	 * @param typeDetails The application type details
	 */
	@SuppressWarnings("unchecked")
	private void processSanctionFee(BPARequest bpaRequest, String feeType, ApplicationTypeDetails typeDetails) {

		log.info("Calculating Sanction Fee");

		Map<String, String> additionalDetails = bpaRequest.getBPA().getAdditionalDetails() != null
				? (Map<String, String>) bpaRequest.getBPA().getAdditionalDetails()
				: new HashMap<>();

		log.info("AdditionalDetails: {}", additionalDetails);

		// Only calculate if other fee adjustment amount is present
		if (additionalDetails.get(BPAConstants.BPA_ADD_DETAILS_OTHER_FEE_ADJUSTMENT_AMOUNT_KEY) != null) {
			log.info("Other Fee Adjustment Amount is present, proceeding with calculation");
			calculationService.addCalculationV2ForRevalidation(bpaRequest, feeType, typeDetails.getApplicationType(),
					typeDetails.getServiceType());
		} else {
			log.info("Other Fee Adjustment Amount is not present, skipping calculation");
		}
	}

	/**
	 * Inner class to hold application type and service type details.
	 */
	private static class ApplicationTypeDetails {

		private final String applicationType;
		private final String serviceType;

		public ApplicationTypeDetails(String applicationType, String serviceType) {
			this.applicationType = applicationType;
			this.serviceType = serviceType;
		}

		public String getApplicationType() {
			return applicationType;
		}

		public String getServiceType() {
			return serviceType;
		}
	}

	/**
	 * Prepares BPA data for Non-SUJOG applications.
	 * 
	 * <p>
	 * This method orchestrates the data preparation process for non-SUJOG
	 * applications:
	 * <ol>
	 * <li>Parses additional details to extract building information</li>
	 * <li>Builds occupancy list from block details</li>
	 * <li>Updates additional details with occupancy list and plot area</li>
	 * <li>Determines and sets the appropriate business service based on building
	 * parameters</li>
	 * <li>Calculates and sets the risk type</li>
	 * </ol>
	 * </p>
	 * 
	 * @param bpa The BPA application to prepare data for
	 */
	@SuppressWarnings("unchecked")
	public void prepareDataForNonSujog(BPA bpa) {

		// Step 1: Parse additional details JSON
		DocumentContext context = JsonPath.parse(bpa.getAdditionalDetails());

		// Step 2: Extract building flags and parameters
		NonSujogBuildingParams buildingParams = extractBuildingParams(context);

		// Step 3: Build occupancy list from block details
		List<Map<String, Object>> occupancyList = buildOccupancyListFromBlocks(context);

		// Step 4: Update additional details with occupancy list and plot area
		updateAdditionalDetailsWithOccupancy(bpa, context, occupancyList);

		// Step 5: Determine and set business service
		String businessService = getBusinessServiceForNonSujog(buildingParams.getMaxBuildingHeight(),
				buildingParams.getPlotArea(), buildingParams.isSpecialBuilding(), buildingParams.isSparit());
		bpa.setBusinessService(businessService);

		// Step 6: Calculate and set risk type
		bpa.setRiskType(getRiskType(context));
	}

	/**
	 * Extracts building parameters from the JSON context for non-SUJOG
	 * applications.
	 * 
	 * @param context The JSON document context
	 * @return NonSujogBuildingParams containing extracted parameters
	 */
	private NonSujogBuildingParams extractBuildingParams(DocumentContext context) {

		boolean isSpecialBuilding = context.read("$.RevalidationPlanNonSujog.isSpecialBuilding");
		boolean isSparit = context.read("$.RevalidationPlanNonSujog.isSparit");
		Double maxBuildingHeight = findMaxBuildingHeight(context);
		Double plotArea = getPlotArea(context);

		return new NonSujogBuildingParams(isSpecialBuilding, isSparit, maxBuildingHeight, plotArea);
	}

	/**
	 * Builds the occupancy list from block details in the JSON context.
	 * 
	 * <p>
	 * Extracts occupancy code, sub-occupancy code, floor area, and buildup area
	 * from each block in the block details.
	 * </p>
	 * 
	 * @param context The JSON document context
	 * @return List of occupancy maps containing occupancy details
	 */
	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> buildOccupancyListFromBlocks(DocumentContext context) {

		List<Map<String, Object>> occupancyList = new ArrayList<>();

		List<Map<String, Object>> blocksList = context.read("$.RevalidationPlanNonSujog.edcr.blockDetail[*].blocks[*]");

		for (Map<String, Object> block : blocksList) {
			Map<String, Object> occupancyMap = new HashMap<>();
			occupancyMap.put("OccupancyCode", block.get("occupancy"));
			occupancyMap.put("floorArea", block.get("Floor Area"));
			occupancyMap.put("subOccupancyCode", block.get("subOccupancy"));
			occupancyMap.put("builtUpArea", block.get("Buildup Area"));
			occupancyList.add(occupancyMap);
		}

		return occupancyList;
	}

	/**
	 * Updates the BPA additional details with occupancy list and plot area.
	 * 
	 * @param bpa           The BPA application
	 * @param context       The JSON document context
	 * @param occupancyList The occupancy list to add
	 */
	@SuppressWarnings("unchecked")
	private void updateAdditionalDetailsWithOccupancy(BPA bpa, DocumentContext context,
			List<Map<String, Object>> occupancyList) {

		Map<String, Object> additionalDetails = (Map<String, Object>) bpa.getAdditionalDetails();
		Map<String, Object> revalidationPlanNonSujog = (Map<String, Object>) additionalDetails
				.get("RevalidationPlanNonSujog");

		// Get plot area from context
		String plotAreaInSqrMt = context.read("$.RevalidationPlanNonSujog.edcr.plotDetails.plotAreaInSqrMt");

		// Update revalidation plan with occupancy list and plot area
		revalidationPlanNonSujog.put("occupancyList", occupancyList);
		revalidationPlanNonSujog.put("PLOT_AREA", plotAreaInSqrMt);
	}

	/**
	 * Inner class to hold building parameters for non-SUJOG applications.
	 */
	private static class NonSujogBuildingParams {

		private final boolean specialBuilding;
		private final boolean sparit;
		private final Double maxBuildingHeight;
		private final Double plotArea;

		public NonSujogBuildingParams(boolean specialBuilding, boolean sparit, Double maxBuildingHeight,
				Double plotArea) {
			this.specialBuilding = specialBuilding;
			this.sparit = sparit;
			this.maxBuildingHeight = maxBuildingHeight;
			this.plotArea = plotArea;
		}

		public boolean isSpecialBuilding() {
			return specialBuilding;
		}

		public boolean isSparit() {
			return sparit;
		}

		public Double getMaxBuildingHeight() {
			return maxBuildingHeight;
		}

		public Double getPlotArea() {
			return plotArea;
		}
	}

	/**
	 * Determines the business service code for non-SUJOG applications.
	 * 
	 * <p>
	 * Business service is determined based on:
	 * <ul>
	 * <li>Building height thresholds (10m, 15m, 30m)</li>
	 * <li>Plot area thresholds (500, 4047, 10000/20000 sq.m)</li>
	 * <li>Whether building is special building</li>
	 * <li>Whether SPARIT flag is set (affects plot area thresholds)</li>
	 * </ul>
	 * </p>
	 * 
	 * @param buildingHeight    The maximum building height
	 * @param plotArea          The plot area in square meters
	 * @param isSpecialBuilding Whether the building is a special building
	 * @param isSparitFlag      Whether SPARIT rules apply
	 * @return The business service code (BPA1_RV, BPA2_RV, BPA3_RV, or BPA4_RV)
	 */
	private String getBusinessServiceForNonSujog(Double buildingHeight, Double plotArea, boolean isSpecialBuilding,
			boolean isSparitFlag) {

		String businessService = null;

		if (null != buildingHeight && null != plotArea) {
			if (!isSpecialBuilding) {
				if ((buildingHeight <= 10) || (plotArea <= 500)) {
					businessService = "BPA1_RV";
				}
				if ((buildingHeight > 10 && buildingHeight <= 15) || (plotArea > 500 && plotArea <= 4047)) {
					businessService = "BPA2_RV";
				}
				if (isSparitFlag) {
					if ((buildingHeight > 15 && buildingHeight <= 30) || (plotArea > 4047 && plotArea <= 20000)) {
						businessService = "BPA3_RV";
					}
					if ((buildingHeight > 30) || (plotArea > 20000)) {
						businessService = "BPA4_RV";
					}
				} else {
					if ((buildingHeight > 15 && buildingHeight <= 30) || (plotArea > 4047 && plotArea <= 10000)) {
						businessService = "BPA3_RV";
					}
					if ((buildingHeight > 30) || (plotArea > 10000)) {
						businessService = "BPA4_RV";
					}
				}
			} else {
				if (buildingHeight <= 15) {
					businessService = "BPA2_RV";
				} else if (buildingHeight > 15 && buildingHeight <= 30) {
					businessService = "BPA3_RV";
				} else if (buildingHeight > 30) {
					businessService = "BPA4_RV";
				}
			}
		}
		return businessService;
	}

	/**
	 * Calculates the risk type for a non-SUJOG application.
	 * 
	 * <p>
	 * Risk type determination logic:
	 * <ul>
	 * <li><b>HIGH RISK (default):</b> Applied when any of the following conditions
	 * are true:
	 * <ul>
	 * <li>Building is an assembly building</li>
	 * <li>Building is under hazardous occupancy category</li>
	 * <li>Building is a special building</li>
	 * <li>Building requires Fire NOC</li>
	 * <li>Basement is present</li>
	 * <li>Plot area > 500 sq.m OR building height > 10m</li>
	 * <li>Building has high-risk occupancy codes</li>
	 * </ul>
	 * </li>
	 * <li><b>LOW RISK:</b> Applied only when ALL of the above conditions are
	 * false</li>
	 * </ul>
	 * </p>
	 * 
	 * @param context The JSON document context containing building details
	 * @return The risk type (RISK_TYPE_HIGH or RISK_TYPE_LOW)
	 */
	public String getRiskType(DocumentContext context) {

		// Default to high risk
		String riskType = RevalidationConstants.RISK_TYPE_HIGH;

		// Step 1: Extract building flags from context
		BuildingRiskFlags riskFlags = extractBuildingRiskFlags(context);

		// Step 2: Get building dimensions
		Double maxBuildingHeight = findMaxBuildingHeight(context);
		Double plotArea = getPlotArea(context);
		boolean isBasementPresent = isBasementPresent(context);

		// Step 3: Check if any high-risk indicators are present
		if (hasHighRiskIndicators(riskFlags, isBasementPresent)) {
			return riskType;
		}

		// Step 4: Check plot area and building height thresholds
		if (!isWithinLowRiskDimensions(plotArea, maxBuildingHeight)) {
			return riskType;
		}

		// Step 5: Check for high-risk occupancy codes
		List<String> allOccupancyCodes = extractAllOccupancyCodes(context);
		if (!containsHighRiskOccupancyCodes(allOccupancyCodes)) {
			riskType = RevalidationConstants.RISK_TYPE_LOW;
		}

		return riskType;
	}

	/**
	 * Extracts building risk-related flags from the JSON context.
	 * 
	 * @param context The JSON document context
	 * @return BuildingRiskFlags containing all risk-related boolean flags
	 */
	private BuildingRiskFlags extractBuildingRiskFlags(DocumentContext context) {

		boolean isSpecialBuilding = context.read("$.RevalidationPlanNonSujog.isSpecialBuilding");
		boolean isHazardous = context.read("$.RevalidationPlanNonSujog.isBuildingUnderHazardousOccupancyCategory");
		boolean isAssemblyBuilding = context.read("$.RevalidationPlanNonSujog.isAssemblyBuilding");
		boolean requiresFireNOC = context.read("$.RevalidationPlanNonSujog.requiresFireNOC");

		return new BuildingRiskFlags(isSpecialBuilding, isHazardous, isAssemblyBuilding, requiresFireNOC);
	}

	/**
	 * Checks if any high-risk indicators are present based on building flags.
	 * 
	 * @param flags             The building risk flags
	 * @param isBasementPresent Whether basement is present
	 * @return true if any high-risk indicator is present
	 */
	private boolean hasHighRiskIndicators(BuildingRiskFlags flags, boolean isBasementPresent) {

		return flags.isAssemblyBuilding() || flags.isHazardousOccupancy() || flags.isSpecialBuilding()
				|| flags.isRequiresFireNOC() || isBasementPresent;
	}

	/**
	 * Checks if the building dimensions are within low-risk thresholds.
	 * 
	 * <p>
	 * Low risk thresholds: Plot area <= 500 sq.m AND building height <= 10m
	 * </p>
	 * 
	 * @param plotArea       The plot area in square meters
	 * @param buildingHeight The maximum building height in meters
	 * @return true if within low-risk dimensions
	 */
	private boolean isWithinLowRiskDimensions(Double plotArea, Double buildingHeight) {

		return plotArea != null && buildingHeight != null && plotArea <= 500 && buildingHeight <= 10;
	}

	/**
	 * Extracts all occupancy and sub-occupancy codes from the building details.
	 * 
	 * @param context The JSON document context
	 * @return Combined list of occupancy and sub-occupancy codes
	 */
	private List<String> extractAllOccupancyCodes(DocumentContext context) {

		List<String> ocList = context.read("$.RevalidationPlanNonSujog.edcr.blockDetail[*].blocks[*].occupancy");
		List<String> socList = context.read("$.RevalidationPlanNonSujog.edcr.blockDetail[*].blocks[*].subOccupancy");

		List<String> allCodes = new ArrayList<>();
		allCodes.addAll(ocList);
		allCodes.addAll(socList);

		return allCodes;
	}

	/**
	 * Checks if the building has any high-risk occupancy codes.
	 * 
	 * <p>
	 * High-risk occupancy codes include:
	 * <ul>
	 * <li>Public/Semi-public/Institutional</li>
	 * <li>Industrial Zone</li>
	 * <li>Cold Storage and Ice Factory</li>
	 * <li>Gas Godown, Godowns, Good Storage</li>
	 * <li>Wholesale Storage (Perishable/Non-Perishable)</li>
	 * <li>Storage/Hangers/Terminal Depot</li>
	 * <li>Warehouse, Wholesale Market</li>
	 * </ul>
	 * </p>
	 * 
	 * @param occupancyCodes The list of occupancy codes to check
	 * @return true if any high-risk occupancy code is present
	 */
	private boolean containsHighRiskOccupancyCodes(List<String> occupancyCodes) {

		String[] highRiskCodes = { RevalidationConstants.OC_PUBLIC_SEMI_PUBLIC_OR_INSTITUTIONAL,
				RevalidationConstants.OC_INDUSTRIAL_ZONE, RevalidationConstants.COLD_STORAGE_AND_ICE_FACTORY,
				RevalidationConstants.GAS_GODOWN, RevalidationConstants.GODOWNS, RevalidationConstants.GOOD_STORAGE,
				RevalidationConstants.WHOLESALE_STORAGE_PERISHABLE,
				RevalidationConstants.WHOLESALE_STORAGE_NON_PERISHABLE,
				RevalidationConstants.STORAGE_OR_HANGERS_OR_TERMINAL_DEPOT, RevalidationConstants.WARE_HOUSE,
				RevalidationConstants.WHOLESALE_MARKET };

		for (String code : highRiskCodes) {
			if (occupancyCodes.contains(code)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Inner class to hold building risk-related flags.
	 */
	private static class BuildingRiskFlags {

		private final boolean specialBuilding;
		private final boolean hazardousOccupancy;
		private final boolean assemblyBuilding;
		private final boolean requiresFireNOC;

		public BuildingRiskFlags(boolean specialBuilding, boolean hazardousOccupancy, boolean assemblyBuilding,
				boolean requiresFireNOC) {
			this.specialBuilding = specialBuilding;
			this.hazardousOccupancy = hazardousOccupancy;
			this.assemblyBuilding = assemblyBuilding;
			this.requiresFireNOC = requiresFireNOC;
		}

		public boolean isSpecialBuilding() {
			return specialBuilding;
		}

		public boolean isHazardousOccupancy() {
			return hazardousOccupancy;
		}

		public boolean isAssemblyBuilding() {
			return assemblyBuilding;
		}

		public boolean isRequiresFireNOC() {
			return requiresFireNOC;
		}
	}

	private Double findMaxBuildingHeight(DocumentContext context) {
		List<String> buildingHeightsStr = context
				.read("$.RevalidationPlanNonSujog.edcr.blockDetail[*].actualBuildingHeight");

		List<Double> buildingHeights = buildingHeightsStr.stream().map(Double::parseDouble)
				.collect(Collectors.toList());

		Double max = 0d;
		for (Double height : buildingHeights) {
			if (height != null && Double.compare(height, max) > 0) {
				max = height;
			}
		}
		return max;
	}

	public boolean isBasementPresent(DocumentContext context) {

		List<String> levelsStr = context.read("$.RevalidationPlanNonSujog.edcr.blockDetail[*].blocks[*].Level");
		List<Integer> levels = levelsStr.stream().map(Integer::parseInt).collect(Collectors.toList());
		for (Integer level : levels) {
			if (level != null && level == -1) {
				return true;
			}
		}
		return false;
	}

	public Double getPlotArea(DocumentContext context) {
		String plotAreaStr = context.read("$.RevalidationPlanNonSujog.edcr.plotDetails.plotAreaInSqrMt");
		Double plotArea = null;
		if (plotAreaStr != null && !plotAreaStr.isEmpty()) {
			plotArea = Double.parseDouble(plotAreaStr);
		}
		return plotArea;
	}

	/**
	 * Checks if a permit application exists and is eligible for revalidation.
	 * 
	 * <p>
	 * This method performs the following validation checks:
	 * <ol>
	 * <li>Checks if revalidation already exists for the permit number</li>
	 * <li>Searches for the BPA application by permit/approval number</li>
	 * <li>Validates that the permit is not older than 3 years</li>
	 * </ol>
	 * </p>
	 * 
	 * @param info     The request info containing user context
	 * @param permitNo The permit number to check
	 * @return List of BPA applications if found and eligible, empty list for
	 *         non-SUJOG permits
	 * @throws CustomException if revalidation already exists or permit is expired
	 */
	public List<BPA> checkApplicationPresent(RequestInfo info, String permitNo) {

		// Step 1: Check if revalidation already exists for given permitNo
		validateNoExistingRevalidation(info, permitNo);

		// Step 2: Search for BPA by permit/approval number
		List<BPA> bpas = searchBpaByPermitNo(info, permitNo);
		
		// Return empty list for non-SUJOG permits
		if (bpas.isEmpty()) {
			return Collections.emptyList();
		}
		
		List<BPA> sortedBPAs = bpas.stream()
				.filter(obj -> BPAConstants.STATUS_APPROVED.equals(obj.getStatus()) && obj.getApprovalDate() != null
						&& obj.getApprovalDate() != 0L)
				.sorted(Comparator.comparing(BPA::getApprovalExpiryDate,
						Comparator.nullsLast(Comparator.reverseOrder())))
				.collect(Collectors.toList());
		return sortedBPAs;
	}

	/**
	 * Validates that no active revalidation exists for the given permit number.
	 * 
	 * <p>
	 * If a revalidation exists and its associated BPA is not deleted, throws an
	 * exception to prevent duplicate revalidation requests.
	 * </p>
	 * 
	 * @param info     The request info
	 * @param permitNo The permit number to check
	 * @throws CustomException if active revalidation already exists
	 */
//	old logic
//	private void validateNoExistingRevalidation(RequestInfo info, String permitNo) {
//
//		List<Revalidation> revalidations = repository
//				.getRevalidationData(RevalidationSearchCriteria.builder().permitNo(permitNo).build());
//
//		if (CollectionUtils.isEmpty(revalidations)) {
//			return;
//		}
//
//		// Check if associated BPA is active (not deleted)
//		String applicationNo = revalidations.get(0).getBpaApplicationNo();
//		List<BPA> bpas = bpaService.getBPAFromCriteria(BPASearchCriteria.builder().applicationNo(applicationNo).build(),
//				info, null);
//
//		if (!CollectionUtils.isEmpty(bpas) && !BPAConstants.BPA_DELETED.equalsIgnoreCase(bpas.get(0).getStatus())) {
//			throw new CustomException("Validation Error",
//					"Revalidation Application already exists for given PermitNo: " + permitNo);
//		}
//	}
	// updated logic
	private void validateNoExistingRevalidation(RequestInfo requestInfo, String permitNo) {

	    final String method = "validateNoExistingRevalidation";
	    log.info("[{}] Start :: permitNo={}", method, permitNo);

	    if (StringUtils.isEmpty(permitNo)) {
	        return;
	    }

	    List<Revalidation> revalidations;

	    try {
	        revalidations = repository.getRevalidationData(
	                RevalidationSearchCriteria.builder()
	                        .permitNo(permitNo)
	                        .build()
	        );
	    } catch (Exception ex) {
	        log.error("[{}] DB error", method, ex);
	        throw new CustomException("SYSTEM_ERROR",
	                "Unable to validate existing revalidation.");
	    }

	    if (CollectionUtils.isEmpty(revalidations)) {
	        return;
	    }

	    for (Revalidation revalidation : revalidations) {

	        if (revalidation == null) continue;

	        String applicationNo = revalidation.getBpaApplicationNo();
	        if (StringUtils.isEmpty(applicationNo)) continue;

	        if (isBPRVInProgress(applicationNo, requestInfo)) {

	            log.error("[{}] ACTIVE BPA exists → Blocking permitNo={}", method, permitNo);

	            throw new CustomException("Validation Error",
	                    "Revalidation is in-progress for permit no : " + permitNo);
	        }
	    }

	    log.info("[{}] All BPA DELETED → Allowed", method);
	}	
	/**
	 * Searches for BPA application by permit/approval number.
	 * 
	 * @param info     The request info
	 * @param permitNo The permit number to search for
	 * @return List of BPA applications, empty list if not found (non-SUJOG permit)
	 */
	private List<BPA> searchBpaByPermitNo(RequestInfo info, String permitNo) {

		BPASearchCriteria bpaSearchCriteria = BPASearchCriteria.builder().approvalNo(permitNo).build();

		List<BPA> bpas = bpaService.getBPAFromCriteria(bpaSearchCriteria, info, null);

		if (bpas == null || bpas.isEmpty()) {
			log.info("Found Non-SUJOG Permit: {}", permitNo);
			return Collections.emptyList();
		}

		return bpas;
	}

	/**
	 * Validates that the permit is not older than 3 years.
	 * 
	 * <p>
	 * Permits older than 3 years are considered expired and cannot be revalidated.
	 * Users must apply for a fresh application instead.
	 * </p>
	 * 
	 * @param bpa The BPA application to validate
	 * @throws CustomException if permit approval date is older than 3 years
	 */
	private void validatePermitNotExpired(BPA bpa) {

		Long approvalDate = bpa.getApprovalDate();
		boolean isPermitExpired = isThreeYearsOld(approvalDate);

		if (isPermitExpired) {
			throw new CustomException("Create Error",
					"Approval date is older than 3 years. Permit already expired. Please apply for fresh application.");
		}
	}
}
