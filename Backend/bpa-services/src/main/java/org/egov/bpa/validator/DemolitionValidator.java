package org.egov.bpa.validator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import javax.validation.Valid;

import org.egov.bpa.util.BPAConstants;
import org.egov.bpa.util.BPAErrorConstants;
import org.egov.bpa.util.DemolitionConstants;
import org.egov.bpa.util.DemolitionUtil;
import org.egov.bpa.util.RegularizationConstants;
import org.egov.bpa.web.model.AuditDetails;
import org.egov.bpa.web.model.Document;
import org.egov.bpa.web.model.VillageSearchCriteria;
import org.egov.bpa.web.model.demolition.Demolition;
import org.egov.bpa.web.model.demolition.DemolitionFieldInspection;
import org.egov.bpa.web.model.demolition.DemolitionFieldInspectionRequest;
import org.egov.bpa.web.model.demolition.DemolitionRequest;
import org.egov.bpa.web.model.demolition.DemolitionSearchCriteria;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import com.jayway.jsonpath.JsonPath;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class DemolitionValidator {
	
	@Autowired
	private DemolitionUtil util;
	
	public void validateDemolitionRequest(@Valid DemolitionRequest request) {
		
		Demolition demolition = request.getDemolition();
		
		if (StringUtils.isEmpty(demolition.getTenantId())) {
			throw new CustomException("create_error", "Please enter TenantId to create Demolition application !");
		}

	}

	public void validateUpdateRequest(@Valid DemolitionRequest request, List<Demolition> searchResult, Object mdmsData,
			Object currentState) {
		
		Demolition demolition = request.getDemolition();

		validateApplicationDocuments(request, mdmsData, currentState);
		validateAllIds(searchResult, demolition);
		validateDuplicateDocuments(request);
		setFieldsFromSearch(request, searchResult);
		
	}

	private void setFieldsFromSearch(@Valid DemolitionRequest request, List<Demolition> searchResult) {
		
		Map<String, Demolition> idToDemolitionFromSearch = new HashMap<>();

		searchResult.forEach(item -> {
			idToDemolitionFromSearch.put(item.getId(), item);
		});

		request.getDemolition().getAuditDetails().setCreatedBy(idToDemolitionFromSearch
				.get(request.getDemolition().getId()).getAuditDetails().getCreatedBy());
		request.getDemolition().getAuditDetails().setCreatedTime(idToDemolitionFromSearch
				.get(request.getDemolition().getId()).getAuditDetails().getCreatedTime());
		request.getDemolition().setStatus(
				idToDemolitionFromSearch.get(request.getDemolition().getId()).getStatus());
		
	}

	private void validateDuplicateDocuments(@Valid DemolitionRequest request) {
		
		if (request.getDemolition().getDocuments() != null) {
			List<String> documentFileStoreIds = new LinkedList<String>();
			request.getDemolition().getDocuments().forEach(document -> {
				if (documentFileStoreIds.contains(document.getFileStoreId()))
					throw new CustomException(RegularizationConstants.DUPLICATE_DOCUMENT,
							"Same document cannot be used multiple times");
				else
					documentFileStoreIds.add(document.getFileStoreId());
			});
		}
		
	}

	private void validateAllIds(List<Demolition> searchResult, Demolition demolition) {
		
		Map<String, Demolition> idToDemolitionFromSearch = new HashMap<>();
		searchResult.forEach(item -> {
			idToDemolitionFromSearch.put(item.getId(), item);
		});

		Map<String, String> errorMap = new HashMap<>();
		Demolition searchedDemolition = idToDemolitionFromSearch.get(demolition.getId());

		if (!searchedDemolition.getApplicationNo().equalsIgnoreCase(demolition.getApplicationNo()))
			errorMap.put("INVALID UPDATE",
					"The application number from search: " + searchedDemolition.getApplicationNo()
							+ " and from update: " + demolition.getApplicationNo() + " does not match");

		if (!searchedDemolition.getId().equalsIgnoreCase(demolition.getId()))
			errorMap.put("INVALID UPDATE", "The id " + demolition.getId() + " does not exist");

		if (!CollectionUtils.isEmpty(errorMap))
			throw new CustomException(errorMap);
		
	}

	private void validateApplicationDocuments(@Valid DemolitionRequest request, Object mdmsData, Object currentState) {
		
		Map<String, List<String>> masterData = util.getAttributeValues(mdmsData);

		Demolition demolition = request.getDemolition();

		if (!demolition.getWorkflow().getAction().equalsIgnoreCase(BPAConstants.ACTION_REJECT)
				&& !demolition.getWorkflow().getAction().equalsIgnoreCase(BPAConstants.ACTION_ADHOC)
				&& !demolition.getWorkflow().getAction().equalsIgnoreCase(BPAConstants.ACTION_PAY)) {

			String filterExp = "$.[?(@.WFState=='" + currentState + "')].docTypes";

			List<Object> docTypeMappings = JsonPath.read(masterData.get(DemolitionConstants.DOCUMENT_TYPE_MAPPING),
					filterExp);

			List<Document> allDocuments = new ArrayList<Document>();
			if (demolition.getDocuments() != null) {
				allDocuments.addAll(demolition.getDocuments());
			}

			if (CollectionUtils.isEmpty(docTypeMappings)) {
				return;
			}

			filterExp = "$.[?(@.required==true)].code";
			List<String> requiredDocTypes = JsonPath.read(docTypeMappings.get(0), filterExp);

			List<String> validDocumentTypes = masterData.get(DemolitionConstants.DOCUMENT_TYPE);

			
			if (!CollectionUtils.isEmpty(allDocuments)) {

				allDocuments.forEach(document -> {

					if (!validDocumentTypes.contains(document.getDocumentType())) {
						throw new CustomException("update_error", document.getDocumentType() + " is Unkown");
					}
				});

				if (requiredDocTypes.size() > 0) {

					List<String> addedDocTypes = new ArrayList<String>();
					allDocuments.forEach(document -> {

						String docType = document.getDocumentType();
						int lastIndex = docType.lastIndexOf(".");
						String documentNs = "";
						if (lastIndex > 1) {
							documentNs = docType.substring(0, lastIndex);
							if(requiredDocTypes.contains(documentNs)) {
								requiredDocTypes.remove(documentNs);
							}
						} else if (lastIndex == 1) {
							throw new CustomException("update_error", document.getDocumentType() + " is Invalid");
						} else {
							documentNs = docType;
						}

						addedDocTypes.add(documentNs);
					});
				}
			} else if (requiredDocTypes.size() > 0) {
				throw new CustomException(RegularizationConstants.MANADATORY_DOCUMENTPYE_MISSING,
						"Atleast " + requiredDocTypes.size() + " Documents are requied ");
			}
			if(requiredDocTypes.size() > 0) {
				throw new CustomException(RegularizationConstants.MANADATORY_DOCUMENTPYE_MISSING,
						"Either one or more mandatory documents are missing. Kindly upload them to proceed !!");
			}
			demolition.setDocuments(allDocuments);
		}	
	}

	public void validateSearch(RequestInfo requestInfo, @Valid DemolitionSearchCriteria criteria) {

		if (!requestInfo.getUserInfo().getType().equalsIgnoreCase(BPAConstants.CITIZEN) && criteria.isEmpty())
			throw new CustomException(BPAErrorConstants.INVALID_SEARCH, "Search without any paramters is not allowed");

		if (!requestInfo.getUserInfo().getType().equalsIgnoreCase(BPAConstants.CITIZEN) && !criteria.tenantIdOnly()
				&& criteria.getTenantId() == null)
			throw new CustomException(BPAErrorConstants.INVALID_SEARCH, "TenantId is mandatory in search");

		if (requestInfo.getUserInfo().getType().equalsIgnoreCase(BPAConstants.CITIZEN) && !criteria.isEmpty()
				&& !criteria.tenantIdOnly() && criteria.getTenantId() == null)
			throw new CustomException(BPAErrorConstants.INVALID_SEARCH, "TenantId is mandatory in search");

	}
	
	/**
	 * Validator of Village Search Request
	 * 
	 * @param criteria
	 */
	public void validateVillageSearchRequest(@Valid VillageSearchCriteria criteria) {

		// Don't Allow the request to process, if Application Number is missing
		if (CollectionUtils.isEmpty(criteria.getApplicationNos())) {

			throw new CustomException("INVALID_SEARCH_PARAM",
					"Kindly provide Application Number to Search the Villages !!");
		}

	}

	/**
	 * Validate field inspection request
	 * 
	 * @param fieldInspection
	 */
	public void validatefieldInspectionRequest(DemolitionFieldInspection fieldInspection) {
		validateDocs(fieldInspection);
		Map<Integer, List<String>> docTypes = getAlldocTypeforphotos(fieldInspection);
		log.info("docs list:" + docTypes);
		validateAllImagesUpload(fieldInspection, docTypes);
		log.info("docs list:" + docTypes);
	}

	/**
	 * Validate Documents
	 * 
	 * @param fieldInspection
	 */
	private void validateDocs(DemolitionFieldInspection fieldInspection) {
		Object reportDetails = fieldInspection.getReportDetails();
		if (ObjectUtils.isEmpty(reportDetails))
			throw new CustomException("create error",
					"Failed to create feild inspection report, reportdetails can't be empty !");
		Object details = ((List<Object>) reportDetails).get(0);
		Object docs = (((Map) details).get(BPAConstants.DOCS));
		if (ObjectUtils.isEmpty(docs)) {
			throw new CustomException("create error",
					"Failed to create feild inspection report, docs, questions is mandatory!");
		}
	}

	private void validateAllImagesUpload(DemolitionFieldInspection fieldInspection,
			Map<Integer, List<String>> docTypes) {
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
		if (!Objects.isNull(buildingSituation) && !String.valueOf(buildingSituation).isEmpty()) {
			Object noOfLifts = ((Map) (((List) buildingSituation).get(0))).get(BPAConstants.NO_OF_LIFTS);
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

		Object demolitionSetback = fieldInspection.getDemolitionSetback();
		if (!Objects.isNull(demolitionSetback)
				&& !String.valueOf(demolitionSetback).isEmpty()) {
			Object setBackFront = ((Map) (((List) demolitionSetback).get(0)))
					.get(BPAConstants.SET_BACK_FRONT);
			if (Objects.isNull(setBackFront) && String.valueOf(setBackFront).isEmpty()) {

				throw new CustomException("create error", "Set Back Front is mandatory field !");

			}
		}

		if (!Objects.isNull(demolitionSetback)
				&& !String.valueOf(demolitionSetback).isEmpty()) {
			Object setBackRear = ((Map) (((List) demolitionSetback).get(0)))
					.get(BPAConstants.SET_BACK_REAR);
			if (Objects.isNull(setBackRear) && String.valueOf(setBackRear).isEmpty()) {

				throw new CustomException("create error", "Set Back Rear is mandatory field !");

			}
		}

		if (!Objects.isNull(demolitionSetback)
				&& !String.valueOf(demolitionSetback).isEmpty()) {
			Object setBackLeft = ((Map) (((List) demolitionSetback).get(0)))
					.get(BPAConstants.SET_BACK_LEFT);
			if (Objects.isNull(setBackLeft) && String.valueOf(setBackLeft).isEmpty()) {

				throw new CustomException("create error", "Set Back Left is mandatory field !");

			}
		}

		if (!Objects.isNull(demolitionSetback)
				&& !String.valueOf(demolitionSetback).isEmpty()) {
			Object setBackRight = ((Map) (((List) demolitionSetback).get(0)))
					.get(BPAConstants.SET_BACK_RIGHT);
			if (Objects.isNull(setBackRight) && String.valueOf(setBackRight).isEmpty()) {

				throw new CustomException("create error", "Set Back Right is mandatory field !");

			}
		}

	}
	private Map<Integer, List<String>> getAlldocTypeforphotos(DemolitionFieldInspection fieldInspection) {
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
	/**
	 * Enrich create field inspection report
	 * 
	 * @param request
	 */
	public void enrichCreateFieldInspectionReport(@Valid DemolitionFieldInspectionRequest request) {
		request.getFieldInspection().setId(UUID.randomUUID().toString());
		RequestInfo requestInfo = request.getRequestInfo();
		AuditDetails auditDetails = util.getAuditDetails(requestInfo.getUserInfo().getUuid(), true);
		request.getFieldInspection().setAuditDetails(auditDetails);
	}
	
	
}
