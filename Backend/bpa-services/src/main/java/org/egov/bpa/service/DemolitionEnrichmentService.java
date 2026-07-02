package org.egov.bpa.service;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.apache.commons.lang3.ObjectUtils;
import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.repository.IdGenRepository;
import org.egov.bpa.service.issuefix.RevenueVillageService;
import org.egov.bpa.util.BPAConstants;
import org.egov.bpa.util.DemolitionConstants;
import org.egov.bpa.util.DemolitionUtil;
import org.egov.bpa.util.RegularizationConstants;
import org.egov.bpa.web.model.AuditDetails;
import org.egov.bpa.web.model.Workflow;
import org.egov.bpa.web.model.demolition.BlockInfo;
import org.egov.bpa.web.model.demolition.Demolition;
import org.egov.bpa.web.model.demolition.DemolitionFieldInspectionRequest;
import org.egov.bpa.web.model.demolition.DemolitionLandInfo;
import org.egov.bpa.web.model.demolition.DemolitionRequest;
import org.egov.bpa.web.model.idgen.IdResponse;
import org.egov.bpa.web.model.issuefix.TenantBoundary;
import org.egov.bpa.web.model.workflow.BusinessService;
import org.egov.common.contract.request.RequestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DemolitionEnrichmentService {

	@Autowired
	private IdGenRepository idGenRepository;

	@Autowired
	private BPAConfiguration config;

	@Autowired
	private DemolitionUtil util;

	@Autowired
	private DemolitionUserService userService;

	@Autowired
	private DemolitionWorkflowService workflowService;

	@Autowired
	private RevenueVillageService revenueVillageService;
	
	@Autowired
	private MDMSService mdmsService;

	/**
	 * Enriches a demolition application create request with all required data for
	 * persistence.
	 * 
	 * <p>
	 * This method orchestrates the complete enrichment lifecycle for new demolition
	 * applications:
	 * <ul>
	 * <li>Creates audit details for tracking who created the record and when</li>
	 * <li>Generates unique identifiers (UUIDs) for demolition and land info</li>
	 * <li>Enriches demolition, land info, and block info with required
	 * metadata</li>
	 * <li>Generates application number via IdGen service</li>
	 * <li>Enriches owner details with user information</li>
	 * <li>Enriches village data from boundary service</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Business Logic:</strong>
	 * <ul>
	 * <li>All entities must have unique IDs before persistence</li>
	 * <li>Audit details track who created/modified records for compliance</li>
	 * <li>Application number follows configured format and is human-readable</li>
	 * <li>Owner data must be validated against user service</li>
	 * <li>Village names are enriched from boundary master data for reporting</li>
	 * </ul>
	 * 
	 * @param request the demolition create request containing application data
	 */
	public void enrichDemolitionCreateRequest(DemolitionRequest request) {

		Demolition demolition = request.getDemolition();
		RequestInfo requestInfo = request.getRequestInfo();

		// Step 1: Create audit details from user information
		AuditDetails auditDetails = util.getAuditDetails(requestInfo.getUserInfo().getUuid(), true);

		// Step 2: Enrich demolition and land info with base data
		enrichDemolitionBaseData(demolition, auditDetails);

		// Step 3: Generate and set unique identifiers
		generateAndSetUniqueIdentifiers(demolition, auditDetails);

		// Step 4: Generate application number
		generateApplicationNumber(request);

		// Step 5: Enrich owner details with user information
		enrichOwnerDetails(request);

		// Step 6: Enrich village data from boundary service
		enrichVillageData(request);
		
		// Step 7: Enrich Business Service for Demolition
		enrichBusinessService(request);
	}

	private void enrichBusinessService(DemolitionRequest request) {
		Boolean isSparit = mdmsService.getMdmsSparitValue(request.getRequestInfo(),
				request.getDemolition().getTenantId());
		log.info(request.getDemolition().getApplicationNo()+" : Is SPARIT : "+isSparit);
		if (isSparit) {
		    enrichBusinessServiceForSPARIT(request);
		} else {
		    enrichBusinessServiceForODA(request);
		}
	}

	private void enrichBusinessServiceForODA(DemolitionRequest request) {
		Demolition demolition = request.getDemolition();
		String buildingHeight = demolition.getLandInfo().getBlockInfo().get(0).getBuildingHeight();
		String plotArea = Optional.ofNullable(demolition.getAdditionalDetails()).filter(Map.class::isInstance)
				.map(Map.class::cast).map(map -> map.get("totalPlotArea")).map(Object::toString).orElse(null);
		Boolean isSpecialBuilding = Boolean.FALSE;
		log.info(demolition.getApplicationNo()+" : BuildingHeight : "+buildingHeight+" ,plotArea : "+plotArea);
        String businessService = determineBusinessServiceForODA(Double.valueOf(buildingHeight),
				Double.valueOf(plotArea), isSpecialBuilding);
        log.info(demolition.getApplicationNo()+" : BusinessService : "+businessService);
		request.getDemolition().setBusinessService(businessService);
	}

	private void enrichBusinessServiceForSPARIT(DemolitionRequest request) {
		Demolition demolition = request.getDemolition();
		String buildingHeight = demolition.getLandInfo().getBlockInfo().get(0).getBuildingHeight();
		String plotArea = Optional.ofNullable(demolition.getAdditionalDetails()).filter(Map.class::isInstance)
				.map(Map.class::cast).map(map -> map.get("totalPlotArea")).map(Object::toString).orElse(null);
		log.info(demolition.getApplicationNo()+" : BuildingHeight : "+buildingHeight+" ,plotArea : "+plotArea);
		String businessService = determineBusinessServiceForSPARIT(Double.valueOf(buildingHeight),
				Double.valueOf(plotArea));
		log.info(demolition.getApplicationNo()+" : BusinessService : "+businessService);
		request.getDemolition().setBusinessService(businessService);
	}
	
	
	public static String determineBusinessServiceForSPARIT(Double buildingHeight, Double plotArea) {
		if (buildingHeight == null) {
			throw new IllegalArgumentException("Building height cannot be null");
		}

		final double FOUR_TENTH_HECTARE_SQM = 4000.0;
		final double TWO_HECTARE_SQM = 20000.0;

		int heightRank;

		if (buildingHeight <= 10) {
			heightRank = 1; // BD1
		} else if (buildingHeight <= 15) {
			heightRank = 2; // BD2
		} else if (buildingHeight <= 30) {
			heightRank = 3; // BD3
		} else {
			heightRank = 4; // BD4
		}

		int plotRank = 1; // Default BD1

		if (plotArea != null) {
			if (plotArea <= 500) {
				plotRank = 1;
			} else if (plotArea <= FOUR_TENTH_HECTARE_SQM) {
				plotRank = 2;
			} else if (plotArea <= TWO_HECTARE_SQM) {
				plotRank = 3;
			} else {
				plotRank = 4;
			}
		}

		int finalRank = Math.max(heightRank, plotRank);

		switch (finalRank) {
		case 1:
			return DemolitionConstants.DEMOLITION_BS_BD1;
		case 2:
			return DemolitionConstants.DEMOLITION_BS_BD2;
		case 3:
			return DemolitionConstants.DEMOLITION_BS_BD3;
		case 4:
			return DemolitionConstants.DEMOLITION_BS_BD4;
		default:
			return DemolitionConstants.DEMOLITION_BS_BD1;
		}
	}

	public static String determineBusinessServiceForODA(Double buildingHeight, Double plotArea,
			Boolean specialBuilding) {

		if (buildingHeight == null) {
			throw new IllegalArgumentException("Building height cannot be null");
		}

		final double ONE_ACRE_SQM = 4046.86;
		final double ONE_HECTARE_SQM = 10000.0;

		// Special Building Logic
		if (Boolean.TRUE.equals(specialBuilding)) {

			if (buildingHeight <= 15) {
				return DemolitionConstants.DEMOLITION_BS_BD2;
			} else if (buildingHeight <= 30) {
				return DemolitionConstants.DEMOLITION_BS_BD3;
			} else {
				return DemolitionConstants.DEMOLITION_BS_BD4;
			}
		}

		int heightRank;
		if (buildingHeight <= 10) {
			heightRank = 1; // BD1
		} else if (buildingHeight <= 15) {
			heightRank = 2; // BD2
		} else if (buildingHeight <= 30) {
			heightRank = 3; // BD3
		} else {
			heightRank = 4; // BD4
		}

		int plotRank = 1; // Default BD1

		if (plotArea != null) {
			if (plotArea <= 500) {
				plotRank = 1;
			} else if (plotArea <= ONE_ACRE_SQM) {
				plotRank = 2;
			} else if (plotArea <= ONE_HECTARE_SQM) {
				plotRank = 3;
			} else {
				plotRank = 4;
			}
		}

		int finalRank = Math.max(heightRank, plotRank);

		switch (finalRank) {
		case 1:
			return DemolitionConstants.DEMOLITION_BS_BD1;
		case 2:
			return DemolitionConstants.DEMOLITION_BS_BD2;
		case 3:
			return DemolitionConstants.DEMOLITION_BS_BD3;
		case 4:
			return DemolitionConstants.DEMOLITION_BS_BD4;
		default:
			return DemolitionConstants.DEMOLITION_BS_BD1;
		}
	}

	/**
	 * Enriches demolition and land info with base audit details.
	 * 
	 * <p>
	 * Audit details track who created the record and when, which is required for
	 * compliance, auditing, and data lineage purposes.
	 * 
	 * @param demolition   the demolition application to enrich
	 * @param auditDetails the audit details to set
	 */
	private void enrichDemolitionBaseData(Demolition demolition, AuditDetails auditDetails) {
		demolition.setAuditDetails(auditDetails);
		demolition.getLandInfo().setAuditDetails(auditDetails);
	}

	/**
	 * Generates and sets unique identifiers for demolition, land info, and blocks.
	 * 
	 * <p>
	 * This method performs hierarchical ID generation:
	 * <ul>
	 * <li><strong>Demolition ID:</strong> Top-level UUID for the application</li>
	 * <li><strong>Land Info ID:</strong> UUID for land details (linked to
	 * demolition)</li>
	 * <li><strong>Block IDs:</strong> UUIDs for each block (linked to land
	 * info)</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Relationships:</strong>
	 * 
	 * <pre>
	 * Demolition (1) --&gt; Land Info (1) --&gt; Blocks (N)
	 * </pre>
	 * 
	 * @param demolition   the demolition application to enrich with IDs
	 * @param auditDetails the audit details to apply to blocks
	 */
	private void generateAndSetUniqueIdentifiers(Demolition demolition, AuditDetails auditDetails) {

		// Generate top-level UUIDs
		String demolitionUUID = UUID.randomUUID().toString();
		String landInfoUUID = UUID.randomUUID().toString();

		// Set demolition ID
		demolition.setId(demolitionUUID);

		// Enrich land info with IDs and relationships
		enrichLandInfoIdentifiers(demolition.getLandInfo(), demolition.getTenantId(), landInfoUUID, demolitionUUID);

		// Enrich each block with IDs and relationships
		enrichBlockInfoIdentifiers(demolition.getLandInfo().getBlockInfo(), demolition.getTenantId(), landInfoUUID,
				auditDetails);
	}

	/**
	 * Enriches land info with unique identifiers and relationship links.
	 * 
	 * @param landInfo       the land information to enrich
	 * @param tenantId       the tenant ID to set
	 * @param landInfoUUID   the unique ID for the land info
	 * @param demolitionUUID the parent demolition ID to link to
	 */
	private void enrichLandInfoIdentifiers(DemolitionLandInfo landInfo, String tenantId, String landInfoUUID,
			String demolitionUUID) {

		landInfo.setTenantId(tenantId);
		landInfo.setId(landInfoUUID);
		landInfo.setDemolitionId(demolitionUUID);
	}

	/**
	 * Enriches block info with unique identifiers, relationships, and audit
	 * details.
	 * 
	 * <p>
	 * Each block represents a building or structure unit within the land parcel.
	 * Blocks must have unique IDs and must be linked to their parent land info.
	 * 
	 * @param blockInfoList the list of blocks to enrich
	 * @param tenantId      the tenant ID to set on each block
	 * @param landInfoUUID  the parent land info ID to link to
	 * @param auditDetails  the audit details to apply to each block
	 */
	private void enrichBlockInfoIdentifiers(List<BlockInfo> blockInfoList, String tenantId, String landInfoUUID,
			AuditDetails auditDetails) {

		if (CollectionUtils.isEmpty(blockInfoList)) {
			return;
		}

		blockInfoList.forEach(block -> {
			block.setId(UUID.randomUUID().toString());
			block.setTenantId(tenantId);
			block.setDemolitionLandInfoId(landInfoUUID);
			block.setAuditDetails(auditDetails);
		});
	}

	/**
	 * Generates a human-readable application number via IdGen service.
	 * 
	 * <p>
	 * <strong>Application Number Format:</strong> The format is configured in
	 * application properties (e.g., "DM/2024/001234") and ensures uniqueness within
	 * the tenant for easier tracking and citizen reference.
	 * 
	 * <p>
	 * <strong>Business Purpose:</strong> Application numbers serve as:
	 * <ul>
	 * <li>User-friendly reference for citizens and officials</li>
	 * <li>Unique identifier for tracking across systems</li>
	 * <li>Sequential numbering for audit and reporting</li>
	 * </ul>
	 * 
	 * @param request the demolition request containing tenant and request info
	 */
	private void generateApplicationNumber(DemolitionRequest request) {

		Demolition demolition = request.getDemolition();

		// Call IdGen service to generate application number
		List<IdResponse> idResponses = idGenRepository.getId(request.getRequestInfo(), demolition.getTenantId(),
				config.getDemolitionApplicationName(), config.getDemolitionApplicationFormat(), 1).getIdResponses();

		// Extract and set the generated application number
		String applicationNo = idResponses.get(0).getId().trim();
		demolition.setApplicationNo(applicationNo);
	}

	/**
	 * Enriches demolition application with village name from boundary service.
	 * 
	 * <p>
	 * This method performs the following operations:
	 * <ul>
	 * <li>Retrieves boundary/village data from the boundary service</li>
	 * <li>Matches the mauza code from land info with boundary codes</li>
	 * <li>Extracts the human-readable village name</li>
	 * <li>Stores village name in additional details for display purposes</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Business Logic:</strong>
	 * <ul>
	 * <li>Mauza code is the system identifier for revenue villages</li>
	 * <li>Village name is user-friendly and used in reports and displays</li>
	 * <li>Enrichment is optional - failures don't block application processing</li>
	 * <li>Village name is stored in additional details for flexibility</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Note:</strong> This method is error-tolerant. If village lookup
	 * fails, the application can still proceed without the village name enrichment.
	 * 
	 * @param request the demolition request containing land info with mauza code
	 */
	private void enrichVillageData(DemolitionRequest request) {

		Demolition demolition = request.getDemolition();
		RequestInfo requestInfo = request.getRequestInfo();

		try {
			// Step 1: Validate land info and mauza code are present
			DemolitionLandInfo landInfo = demolition.getLandInfo();
			if (landInfo == null || StringUtils.isEmpty(landInfo.getMauza())) {
				return;
			}

			// Step 2: Fetch boundary data from boundary service
			List<TenantBoundary> tenantBoundary = revenueVillageService.boundaryDataSearch(requestInfo,
					demolition.getTenantId());

			if (CollectionUtils.isEmpty(tenantBoundary)) {
				return;
			}

			// Step 3: Extract village name by matching mauza code with boundary code
			String villageName = extractVillageNameFromBoundary(tenantBoundary, landInfo.getMauza());

			// Step 4: Store village name in additional details if found
			if (!StringUtils.isEmpty(villageName)) {
				storeVillageNameInAdditionalDetails(demolition, villageName);
			}

		} catch (Exception e) {
			// Log error but don't fail the enrichment process
			log.error("Error enriching village data for application {}: {}", demolition.getApplicationNo(),
					e.getMessage(), e);
		}
	}

	/**
	 * Extracts village name from boundary data by matching mauza code.
	 * 
	 * <p>
	 * This method searches through the boundary list to find a boundary whose code
	 * matches the provided mauza code (case-insensitive).
	 * 
	 * @param tenantBoundary the list of tenant boundaries from boundary service
	 * @param mauzaCode      the mauza code to match
	 * @return the village name if found, null otherwise
	 */
	private String extractVillageNameFromBoundary(List<TenantBoundary> tenantBoundary, String mauzaCode) {

		// Extract boundary data from the response
		Map<String, Object> boundaryData = (Map<String, Object>) tenantBoundary.get(0);
		List<LinkedHashMap<String, String>> boundaryList = (List<LinkedHashMap<String, String>>) boundaryData
				.get("boundary");

		// Search for matching mauza code
		for (LinkedHashMap<String, String> boundary : boundaryList) {
			if (boundary.get("code").equalsIgnoreCase(mauzaCode)) {
				return boundary.get("name");
			}
		}

		return null;
	}

	/**
	 * Stores village name in the demolition's additional details.
	 * 
	 * <p>
	 * Additional details is a flexible Map structure that can hold extra
	 * information not part of the core data model. This allows village name to be
	 * stored without schema changes.
	 * 
	 * @param demolition  the demolition application to update
	 * @param villageName the village name to store
	 */
	private void storeVillageNameInAdditionalDetails(Demolition demolition, String villageName) {

		// Get existing additional details or create new map
		Map<String, Object> additionalDetails = Optional.ofNullable(demolition.getAdditionalDetails())
				.map(details -> (Map<String, Object>) details).orElse(new HashMap<>());

		// Add village name to additional details
		additionalDetails.put("villageName", villageName);

		// Set back to demolition
		demolition.setAdditionalDetails(additionalDetails);
	}

	/**
	 * Method to Enrich Owner details
	 * 
	 * @param requestInfo
	 * @param demolition
	 */
	private void enrichOwnerDetails(DemolitionRequest request) {
		userService.manageUser(request);
		enrichRequiredDataForUser(request);
	}

	/**
	 * Enrich the required User Data in Request
	 * 
	 * @param requestInfo
	 * @param regularization
	 * @param isUpdate
	 */
	private void enrichRequiredDataForUser(DemolitionRequest request) {

		RequestInfo requestInfo = request.getRequestInfo();

		Demolition demolition = request.getDemolition();

		AuditDetails auditDetails = util.getAuditDetails(requestInfo.getUserInfo().getUuid(), true);

		// Owners data
		if (!CollectionUtils.isEmpty(demolition.getOwners())) {
			demolition.getOwners().forEach(owner -> {
				if (StringUtils.isEmpty(owner.getOwnerId()))
					owner.setOwnerId(UUID.randomUUID().toString());
				owner.setAuditDetails(auditDetails);
			});
		}
	}

	/**
	 * Enriches a demolition application update request with required metadata and
	 * identifiers.
	 * 
	 * <p>
	 * This method orchestrates the complete enrichment lifecycle for demolition
	 * updates:
	 * <ul>
	 * <li>Updates audit details to track who modified the record and when</li>
	 * <li>Enriches workflow assignees based on action type</li>
	 * <li>Generates UUIDs for new documents (application and workflow
	 * documents)</li>
	 * <li>Enriches block information (both existing and newly added blocks)</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Business Logic:</strong>
	 * <ul>
	 * <li>Original creation audit details are preserved (createdBy,
	 * createdTime)</li>
	 * <li>Only modification details are updated (lastModifiedBy,
	 * lastModifiedTime)</li>
	 * <li>New documents receive UUIDs, existing documents retain their IDs</li>
	 * <li>Block info is separated into existing blocks (update) and new blocks
	 * (insert)</li>
	 * <li>Assignees are enriched based on workflow action (e.g.,
	 * SEND_BACK_TO_CITIZEN)</li>
	 * </ul>
	 * 
	 * @param request the demolition update request containing application data
	 */
	public void enrichDemolitionUpdateRequest(@Valid DemolitionRequest request) {

		Demolition demolition = request.getDemolition();
		RequestInfo requestInfo = request.getRequestInfo();

		// Step 1: Update audit details for modification tracking
		AuditDetails auditDetails = enrichUpdateAuditDetails(demolition, requestInfo);

		// Step 2: Enrich workflow assignees based on action
		enrichAssignes(demolition);

		// Step 3: Generate IDs for new application documents
		enrichApplicationDocuments(demolition);

		// Step 4: Generate IDs for new workflow verification documents
		enrichWorkflowDocuments(demolition);

		// Step 5: Enrich block information (existing and new blocks)
		enrichBlockInfoDetails(request, auditDetails);
	}

	/**
	 * Enriches audit details for update operations while preserving original
	 * creation details.
	 * 
	 * <p>
	 * <strong>Audit Trail Logic:</strong>
	 * <ul>
	 * <li>Preserves who created the record and when (immutable)</li>
	 * <li>Updates who last modified the record and when (mutable)</li>
	 * <li>Ensures complete audit trail for compliance and tracking</li>
	 * </ul>
	 * 
	 * @param demolition  the demolition application being updated
	 * @param requestInfo the request information containing current user details
	 * @return updated audit details with preserved creation info
	 */
	private AuditDetails enrichUpdateAuditDetails(Demolition demolition, RequestInfo requestInfo) {

		// Generate new audit details with current user as modifier
		AuditDetails auditDetails = util.getAuditDetails(requestInfo.getUserInfo().getUuid(), false);

		// Preserve original creation details
		AuditDetails existingAudit = demolition.getAuditDetails();
		auditDetails.setCreatedBy(existingAudit.getCreatedBy());
		auditDetails.setCreatedTime(existingAudit.getCreatedTime());

		// Update modification timestamp on the demolition
		existingAudit.setLastModifiedTime(auditDetails.getLastModifiedTime());

		return auditDetails;
	}

	/**
	 * Enriches application documents with unique identifiers for new documents.
	 * 
	 * <p>
	 * <strong>Document ID Logic:</strong>
	 * <ul>
	 * <li>Existing documents already have IDs (retained)</li>
	 * <li>New documents added during update receive new UUIDs</li>
	 * <li>ID presence check determines if document is new or existing</li>
	 * </ul>
	 * 
	 * @param demolition the demolition application with documents
	 */
	private void enrichApplicationDocuments(Demolition demolition) {

		if (CollectionUtils.isEmpty(demolition.getDocuments())) {
			return;
		}

		demolition.getDocuments().forEach(document -> {
			if (document.getId() == null) {
				document.setId(UUID.randomUUID().toString());
			}
		});
	}

	/**
	 * Enriches workflow verification documents with unique identifiers for new
	 * documents.
	 * 
	 * <p>
	 * Workflow verification documents are additional documents required by
	 * officials during the approval process (e.g., site inspection reports,
	 * additional certificates, verification proofs).
	 * 
	 * @param demolition the demolition application with workflow documents
	 */
	private void enrichWorkflowDocuments(Demolition demolition) {

		if (demolition.getWorkflow() == null
				|| CollectionUtils.isEmpty(demolition.getWorkflow().getVarificationDocuments())) {
			return;
		}

		demolition.getWorkflow().getVarificationDocuments().forEach(document -> {
			if (document.getId() == null) {
				document.setId(UUID.randomUUID().toString());
			}
		});
	}

	/**
	 * Enriches block information by separating existing and newly added blocks.
	 * 
	 * <p>
	 * This method performs two distinct operations:
	 * <ul>
	 * <li><strong>Existing Blocks (with IDs):</strong> Updates audit details
	 * only</li>
	 * <li><strong>New Blocks (without IDs):</strong> Generates UUIDs and sets all
	 * metadata</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Database Operation Logic:</strong>
	 * <ul>
	 * <li>Existing blocks trigger UPDATE queries in the database</li>
	 * <li>New blocks trigger INSERT queries in the database</li>
	 * <li>Separation is done by checking ID presence (null = new, non-null =
	 * existing)</li>
	 * <li>Two separate lists enable the persister to handle them differently</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Business Logic:</strong> Citizens may add additional
	 * structures/blocks after initial submission (e.g., adding a shed or additional
	 * floor to demolition request). These need to be tracked separately for proper
	 * database operations.
	 * 
	 * @param request      the demolition request containing block information
	 * @param auditDetails the audit details to apply to blocks
	 */
	private void enrichBlockInfoDetails(@Valid DemolitionRequest request, AuditDetails auditDetails) {

		List<BlockInfo> allBlocks = request.getDemolition().getLandInfo().getBlockInfo();

		// Step 1: Separate existing blocks (have IDs) for UPDATE operation
		List<BlockInfo> existingBlocks = allBlocks.stream().filter(block -> StringUtils.hasText(block.getId()))
				.collect(Collectors.toList());

		// Step 2: Update audit details on existing blocks
		existingBlocks.forEach(block -> {
			block.setAuditDetails(auditDetails);
		});

		// Set existing blocks to main block list (for UPDATE queries)
		request.getDemolition().getLandInfo().setBlockInfo(existingBlocks);

		// Step 3: Separate new blocks (no IDs) for INSERT operation
		List<BlockInfo> newBlocks = allBlocks.stream().filter(block -> StringUtils.isEmpty(block.getId()))
				.collect(Collectors.toList());

		// Step 4: Enrich new blocks with all required metadata
		newBlocks.forEach(block -> {
			// Generate unique identifier
			block.setId(UUID.randomUUID().toString());

			// Set tenant and parent relationship
			block.setTenantId(request.getDemolition().getTenantId());
			block.setDemolitionLandInfoId(request.getDemolition().getLandInfo().getId());

			// Set audit details
			block.setAuditDetails(auditDetails);
		});

		// Set new blocks to separate list (for INSERT queries)
		request.getDemolition().getLandInfo().setNewBlockInfos(newBlocks);
	}

	/**
	 * Enriches workflow assignees based on the workflow action.
	 * 
	 * <p>
	 * This method manages the list of users who should be assigned to the workflow
	 * task:
	 * <ul>
	 * <li>Preserves any assignees already specified in the workflow</li>
	 * <li>For SEND_BACK_TO_CITIZEN action, adds all owners and account holders</li>
	 * <li>Ensures all citizen users are notified when application is sent back</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Business Logic for SEND_BACK_TO_CITIZEN:</strong>
	 * <ul>
	 * <li>Application is returned to citizens for corrections or additional
	 * information</li>
	 * <li>All owners listed in the application should be notified</li>
	 * <li>Account holder (if different from owners) should be notified</li>
	 * <li>System looks up registered UUIDs to ensure valid user accounts exist</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Note:</strong> This method creates a workflow object if it doesn't
	 * exist to ensure assignees list is always available for downstream processing.
	 * 
	 * @param demolition the demolition application with workflow information
	 */
	public void enrichAssignes(Demolition demolition) {

		// Step 1: Collect existing assignees
		Set<String> assignees = collectExistingAssignees(demolition.getWorkflow());

		// Step 2: Add citizen users if action is SEND_BACK_TO_CITIZEN
		if (isSendBackToCitizenAction(demolition.getWorkflow())) {
			addCitizenAssignees(demolition, assignees);
		}

		// Step 3: Update workflow with enriched assignees
		updateWorkflowWithAssignees(demolition, assignees);
	}

	/**
	 * Collects existing assignees from the workflow.
	 * 
	 * <p>
	 * This method preserves any assignees that were already specified in the
	 * workflow request, ensuring they are not lost during enrichment.
	 * 
	 * @param workflow the workflow containing existing assignees (may be null)
	 * @return set of existing assignee UUIDs (empty set if none exist)
	 */
	private Set<String> collectExistingAssignees(Workflow workflow) {
		Set<String> assignees = new HashSet<>();

		if (workflow != null && !CollectionUtils.isEmpty(workflow.getAssignes())) {
			assignees.addAll(workflow.getAssignes());
		}

		return assignees;
	}

	/**
	 * Checks if the workflow action is SEND_BACK_TO_CITIZEN.
	 * 
	 * @param workflow the workflow to check
	 * @return true if action is SEND_BACK_TO_CITIZEN, false otherwise
	 */
	private boolean isSendBackToCitizenAction(Workflow workflow) {
		return workflow != null && workflow.getAction() != null
				&& workflow.getAction().equalsIgnoreCase(BPAConstants.ACTION_SENDBACKTOCITIZEN);
	}

	/**
	 * Adds all citizen users (owners and account holders) as assignees.
	 * 
	 * <p>
	 * This method performs three enrichment operations:
	 * <ul>
	 * <li>Adds all owner UUIDs from the demolition application</li>
	 * <li>Adds the account holder UUID if present</li>
	 * <li>Looks up and adds registered user UUIDs based on mobile numbers</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Business Logic:</strong> When an application is sent back to
	 * citizens, all involved parties (owners, account holder, registered users)
	 * must be notified so they can take corrective action.
	 * 
	 * @param demolition the demolition application containing owner and account
	 *                   info
	 * @param assignees  the set to add citizen assignees to (modified in place)
	 */
	private void addCitizenAssignees(Demolition demolition, Set<String> assignees) {

		// Add all owner UUIDs
		addOwnerUUIDs(demolition, assignees);

		// Add account holder UUID
		addAccountHolderUUID(demolition, assignees);

		// Add registered user UUIDs from user service lookup
		addRegisteredUserUUIDs(demolition, assignees);
	}

	/**
	 * Adds all owner UUIDs to the assignees set.
	 * 
	 * @param demolition the demolition application with owners
	 * @param assignees  the set to add owner UUIDs to
	 */
	private void addOwnerUUIDs(Demolition demolition, Set<String> assignees) {
		if (!CollectionUtils.isEmpty(demolition.getOwners())) {
			demolition.getOwners().forEach(owner -> {
				if (owner.getUuid() != null) {
					assignees.add(owner.getUuid());
				}
			});
		}
	}

	/**
	 * Adds the account holder UUID to the assignees set if present.
	 * 
	 * <p>
	 * The account holder may be different from the owners (e.g., a property manager
	 * or legal representative handling the application on behalf of owners).
	 * 
	 * @param demolition the demolition application with account holder info
	 * @param assignees  the set to add account holder UUID to
	 */
	private void addAccountHolderUUID(Demolition demolition, Set<String> assignees) {
		if (demolition.getAccountId() != null) {
			assignees.add(demolition.getAccountId());
		}
	}

	/**
	 * Retrieves and adds registered user UUIDs from user service.
	 * 
	 * <p>
	 * This method queries the user service to find all registered users associated
	 * with the demolition (based on mobile numbers). This ensures that all citizen
	 * accounts that have access to the application are included in the notification
	 * list.
	 * 
	 * @param demolition the demolition application to look up users for
	 * @param assignees  the set to add registered user UUIDs to
	 */
	private void addRegisteredUserUUIDs(Demolition demolition, Set<String> assignees) {
		Set<String> registeredUUIDs = userService.getUUidFromUserName(demolition);

		if (!CollectionUtils.isEmpty(registeredUUIDs)) {
			assignees.addAll(registeredUUIDs);
		}
	}

	/**
	 * Updates the workflow with the enriched assignees list.
	 * 
	 * <p>
	 * If the workflow object doesn't exist, this method creates a new one to ensure
	 * the assignees list is always available for downstream processing.
	 * 
	 * @param demolition the demolition application to update
	 * @param assignees  the enriched set of assignee UUIDs
	 */
	private void updateWorkflowWithAssignees(Demolition demolition, Set<String> assignees) {

		// Convert set to list for workflow
		List<String> assigneeList = new LinkedList<>(assignees);

		if (demolition.getWorkflow() == null) {
			// Create new workflow object with assignees
			Workflow newWorkflow = new Workflow();
			newWorkflow.setAssignes(assigneeList);
			demolition.setWorkflow(newWorkflow);
		} else {
			// Update existing workflow with enriched assignees
			demolition.getWorkflow().setAssignes(assigneeList);
		}
	}

	/**
	 * Enriches demolition application after workflow progression with
	 * status-specific data.
	 * 
	 * <p>
	 * This method is called AFTER workflow transitions to enrich data based on the
	 * new status:
	 * <ul>
	 * <li>Sets application date when application reaches approval pending
	 * stage</li>
	 * <li>Generates approval number and date when application is approved</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Business Logic:</strong>
	 * <ul>
	 * <li><strong>Application Date:</strong> Timestamp when application officially
	 * enters the system (set when it reaches APPROVAL_PENDING after initial
	 * submission)</li>
	 * <li><strong>Approval Number:</strong> Human-readable permit number generated
	 * when approved (e.g., "DMP/2024/001234")</li>
	 * <li><strong>Approval Date:</strong> Timestamp when application was officially
	 * approved</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Note:</strong> These enrichments are status-dependent and happen only
	 * once to preserve the original timestamps for audit and reporting purposes.
	 * 
	 * @param request the demolition request after workflow progression
	 */
	public void postStatusEnrichment(@Valid DemolitionRequest request) {

		Demolition demolition = request.getDemolition();

		// Step 1: Retrieve business service configuration for workflow states
		BusinessService businessService = workflowService.getBusinessService(demolition, request.getRequestInfo(),
				demolition.getApplicationNo());

		log.info("Application status is: {}", demolition.getStatus());

		// Step 2: Get current workflow state name from status
		String currentState = workflowService.getCurrentState(demolition.getStatus(), businessService);
		log.info("Application state is: {}", currentState);

		// Step 3: Set application date if entering approval pending stage for first
		// time
		if (currentState.equalsIgnoreCase(DemolitionConstants.APPROVAL_PENDING)
				&& (demolition.getApplicationDate() == null || demolition.getApplicationDate() == 0)) {

			// Set current timestamp as application date
			demolition.setApplicationDate(Calendar.getInstance().getTimeInMillis());
		}

		// Step 4: Generate approval number if application is approved
		generateApprovalNumberIfApproved(request);
	}

	/**
	 * Generates approval number and sets approval date when demolition is approved.
	 * 
	 * <p>
	 * The approval number is a human-readable permit identifier that:
	 * <ul>
	 * <li>Follows a configured format (e.g., "DMP/2024/001234")</li>
	 * <li>Is generated via IdGen service for uniqueness and sequential
	 * numbering</li>
	 * <li>Serves as the official permit reference for citizens and authorities</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Note:</strong> Approval date is set only once to preserve the
	 * original approval timestamp even if application is modified later.
	 * 
	 * @param request the demolition request to enrich with approval details
	 */
	private void generateApprovalNumberIfApproved(@Valid DemolitionRequest request) {

		Demolition demolition = request.getDemolition();

		// Check if status is APPROVED
		if (demolition.getStatus().equalsIgnoreCase(RegularizationConstants.APPROVED)) {

			// Generate approval number via IdGen service
			List<IdResponse> idResponses = idGenRepository.getId(request.getRequestInfo(), demolition.getTenantId(),
					config.getDemolitionPermitName(), config.getDemolitionPermitFormat(), 1).getIdResponses();

			// Set approval number
			demolition.setApprovalNo(idResponses.get(0).getId());

			// Set approval date if not already set
			if (demolition.getApprovalDate() == null || demolition.getApprovalDate() == 0) {
				demolition.setApprovalDate(Calendar.getInstance().getTimeInMillis());
			}
		}
	}
	
	public void enrichCreateFieldInspectionReport(@Valid DemolitionFieldInspectionRequest request) {
		request.getFieldInspection().setId(UUID.randomUUID().toString());
		RequestInfo requestInfo = request.getRequestInfo();
		AuditDetails auditDetails = util.getAuditDetails(requestInfo.getUserInfo().getUuid(), true);
		request.getFieldInspection().setAuditDetails(auditDetails);
	}

	public void enrichUpdateFieldInspectionReport(@Valid DemolitionFieldInspectionRequest request) {
		RequestInfo requestInfo = request.getRequestInfo();
		AuditDetails auditDetails = util.getAuditDetails(requestInfo.getUserInfo().getUuid(), false);
		request.getFieldInspection().setAuditDetails(auditDetails);
	}

	
}
