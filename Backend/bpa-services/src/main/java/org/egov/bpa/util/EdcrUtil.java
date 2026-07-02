package org.egov.bpa.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.egov.bpa.service.PreapprovedPlanService;
import org.egov.bpa.web.model.BPA;
import org.egov.bpa.web.model.BPARequest;
import org.egov.bpa.web.model.PreapprovedPlan;
import org.egov.bpa.web.model.PreapprovedPlanSearchCriteria;
import org.egov.bpa.web.model.scheduler.EdcrData;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import com.google.gson.JsonArray;
import com.google.gson.internal.LinkedTreeMap;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONArray;


@Component
@Slf4j
public class EdcrUtil {
	
	private static final String BPA_ADD_DETAILS_SERVICE_KEY = "alterationService";
    private static final String BPA_ADD_DETAILS_SUBSERVICE_KEY = "alterationSubService";
	@Autowired
	private PreapprovedPlanService preapprovedPlanService;
	
	
	
	public void setEdcrDetails(LinkedHashMap<String, Object> edcr, BPARequest request) {
		BPA bpa = request.getBPA();
		String businessService = bpa.getBusinessService();

		String mauza = bpa.getLandInfo().getAddress().getLocality().getName();
		String riskType="";
		String serviceType="";
		String plotNumber="";
		Boolean isBUAAbove500 = Boolean.FALSE;
		String totalBuiltupArea = "";
		String totalPlotArea = "";
		String maxBuildingHeight = "";
		String alterationSubService = "";
		String occupancyType = "";
		Boolean basementPresent = Boolean.FALSE;
		String affordableUnitsPresent = "";
		String noOfDwellingUnits = "";
		Boolean publicWashRoomProvided = Boolean.FALSE;
		String noOfFloors = "";
		String approachRoadWidth = "";
		String proposedFar = "";
		String correlationId = "";
		String noOfStorey="";
		String subOccupancyType="";
		if (!StringUtils.isEmpty(businessService) && "BPA6".equals(businessService)) {
			PreapprovedPlan preapprovedPlan = getPreapprovedPlan(bpa);
			Map<String, Object> drawingDetail = (Map<String, Object>) preapprovedPlan.getDrawingDetail();
			serviceType=getServiceTypeForPreapprovedPlan(drawingDetail);
			riskType=getRiskTypeForPreapprovedPlan(drawingDetail);
			plotNumber=getPlotNoForPreapprovedPlan(bpa);
			isBUAAbove500=getIsBUAAbove500ForPreapprovedPlan(drawingDetail);
			totalBuiltupArea=getTotalBuiltUpAreaForPreApprovedPlan(drawingDetail);
			totalPlotArea=getPlotAreaForPreApprovedPlan(drawingDetail);
			maxBuildingHeight=getMaxBuildingHeightForPreApprovedPlan(drawingDetail);
			occupancyType = getOccupancyTypeForPreApprovedPlan(drawingDetail);
			basementPresent = isBasementPresentForPreApprovedPlan(drawingDetail);
			affordableUnitsPresent = isAffordableUnitsPresentForPreApprovedPlan(drawingDetail);
			noOfDwellingUnits = getNoOfDwellingUnitsForPreApprovedPlan(drawingDetail);
			publicWashRoomProvided = isPublicWashRoomProvidedForPreApprovedPlan(drawingDetail);
			noOfFloors = getNoOfFloorsForPreApprovedPlan(drawingDetail);
			approachRoadWidth = getApproachRoadWidthForPreApprovedPlan(request);
			proposedFar = getProposedFarForPreApprovedPlan(drawingDetail);
			noOfStorey = getNoOfStroreysForPreApprovedPan(drawingDetail);
			subOccupancyType = getSubOccupancyForPreApprovedPlan(drawingDetail);
		}else {
		String jsonString = new JSONObject(edcr).toString();
		DocumentContext context = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonString);
	 serviceType=getServiceTypeFromEdcr(context);
	 riskType=getRiskTypeFromEdcr(context);
	 plotNumber = getPlotNoFromEdcr(context);
	 isBUAAbove500 = getisBUAAbove500FromEdcr(context);
	 totalBuiltupArea = getTotalBuiltUpAreaFromEdcr(context);
	 totalPlotArea = getTotalPlotAreaFromEdcr(context);
	 maxBuildingHeight = getMaxBuildingHeightFromEdcr(context);
	 alterationSubService = getAlterationSubService(context);
	 occupancyType = getOccupancyType(context);
	 basementPresent = isBasementPresent(context);
	 affordableUnitsPresent = isAffordableUnitsPresent(context);
	 noOfDwellingUnits = getNoOfDwellingUnits(context);
	 publicWashRoomProvided = isPublicWashRoomProvided(context);
	 correlationId = getCorrelationIdFromEdcr(context);
	 noOfStorey = getNoOfStoreysFromEDCR(context);
	 subOccupancyType = getSubOccupancyFromEDCR(context);
	 if(!bpa.getOCOutsideSujogApplication()) {
		 noOfFloors = getNoOfFloors(context);
	 	approachRoadWidth = getApproachRoadWidth(context);
	 }
	 proposedFar = getProposedFar(context);
		}
		//prepare edcr data-
		
		EdcrData edcrData = EdcrData.builder().id(UUID.randomUUID().toString())
				.applicationId(bpa.getId())
				.applicationNo(bpa.getApplicationNo())
				.mauza(mauza)
				.riskType(riskType)
				.serviceType(getServiceType(serviceType, bpa))
				.plotNumber(plotNumber)
				.isBUAAbove500(isBUAAbove500)
				.workflowName(getWorkflowName(bpa))
				.maxBuildingHeight(maxBuildingHeight)
				.totalBuiltupArea(totalBuiltupArea)
				.totalPlotArea(totalPlotArea)
				.alterationSubService(alterationSubService)
				.occupancyType(occupancyType)
				.basementPresent(basementPresent)
				.affordableUnitsPresent(affordableUnitsPresent)
				.noOfDwellingUnits(noOfDwellingUnits)
				.publicWashRoomProvided(publicWashRoomProvided)
				.noOfFloors(noOfFloors)
				.approachRoadWidth(approachRoadWidth)
				.proposedFar(proposedFar)
				.correlationId(correlationId)
				.noOfStorey(noOfStorey)
				.subOccupancy(subOccupancyType)
				.build();
		
		//return edcrData;
		bpa.setEdcrData(edcrData);
		bpa.setCorrelationId(correlationId);
				
	}

	private String getWorkflowName(BPA bpa) {
		String workflowName="";
		switch(bpa.getBusinessService()) {
		case "BPA1":
			workflowName="BPA (Category-A)";
			break;
		case "BPA2":
			workflowName="BPA (Category-B)";
			break;
		case "BPA3":
			workflowName="BPA (Category-C)";
			break;
		case "BPA4":
			workflowName="BPA (Category-D)";
			break;
		case "BPA5":
			workflowName="BPA (A - Accredited Person)";
			break;
		case "BPA6":
			workflowName="BPA (A - Preapproved Plan)";
			break;
		case "BPA_OC_OS1":
			workflowName="OC Outside SUJOG (Category-A)";
			break;
		case "BPA_OC_OS2":
			workflowName="OC Outside SUJOG (Category-B)";
			break;
		case "BPA_OC_OS3":
			workflowName="OC Outside SUJOG (Category-C)";
			break;
		case "BPA_OC_OS4":
			workflowName="OC Outside SUJOG (Category-D)";
			break;
		case "BPA7":
			workflowName="BPA (Category-A)";
			break;	
		
		}
		return workflowName;
	}
	
	
	private String getServiceType(String serviceTypeFromEdcrOrPAP, BPA bpa) {
		String serviceType = "";
		String[] usualBusinessServices = new String[] { "BPA1", "BPA2", "BPA3", "BPA4","BPA7" };
		List<String> businessServices = new ArrayList<>();
		Collections.addAll(businessServices, usualBusinessServices);
		if (Objects.nonNull(serviceTypeFromEdcrOrPAP) && "NEW_CONSTRUCTION".equals(serviceTypeFromEdcrOrPAP)
				&& businessServices.contains(bpa.getBusinessService())) {
			serviceType = "New Construction";
		} else if (Objects.nonNull(serviceTypeFromEdcrOrPAP) && "ALTERATION".equals(serviceTypeFromEdcrOrPAP)
				&& businessServices.contains(bpa.getBusinessService())) {
			if (Objects.nonNull(getSubService(bpa))) {
				String subservice = getSubService(bpa);
				serviceType = "Addition & Alteration (" + subservice + ")";
			} else {
				serviceType = "Addition & Alteration";
			}
		} else if ("BPA5".equals(bpa.getBusinessService())) {
			serviceType = "Accredited Person";
		} else if ("BPA6".equals(bpa.getBusinessService())) {
			serviceType = "Pre-Approved Plan";
		}
		return serviceType;
	}
		
	private String getServiceTypeFromEdcr(DocumentContext edcrResponseDocumentContext) {
		List<String> serviceType = edcrResponseDocumentContext
				.read("edcrDetail.*.planDetail.planInformation.serviceType");
		if (CollectionUtils.isEmpty(serviceType)) {
			serviceType.add("NEW_CONSTRUCTION");
		}
		return serviceType.get(0);
	}
	private String getRiskTypeFromEdcr(DocumentContext edcrResponseDocumentContext) {
		List<String> riskType = edcrResponseDocumentContext.read("edcrDetail.*.planDetail.planInformation.riskType");
		if (CollectionUtils.isEmpty(riskType)) {
			riskType.add("LOW");
		}
		return riskType.get(0);
	}
	
	private PreapprovedPlan getPreapprovedPlan(BPA bpa) {
		PreapprovedPlanSearchCriteria preapprovedPlanSearchCriteria = new PreapprovedPlanSearchCriteria();
		preapprovedPlanSearchCriteria.setDrawingNo(bpa.getEdcrNumber());
		List<PreapprovedPlan> preapprovedPlans = preapprovedPlanService
				.getPreapprovedPlanFromCriteria(preapprovedPlanSearchCriteria);
		if (CollectionUtils.isEmpty(preapprovedPlans)) {
			log.error("no preapproved plan found for provided drawingNo:" + bpa.getEdcrNumber());
			throw new CustomException("no preapproved plan found for provided drawingNo",
					"no preapproved plan found for provided drawingNo");
		}
		PreapprovedPlan preapprovedPlanFromDb = preapprovedPlans.get(0);
		return preapprovedPlanFromDb;
	}	

	private String getServiceTypeForPreapprovedPlan(Map<String, Object> drawingDetail) {
		String serviceType = "";
		serviceType = drawingDetail.get("serviceType") + "";
		return serviceType;
	}

	private String getPlotNoForPreapprovedPlan(BPA bpa) {
		String plotNo = "";
		try {
			Map<String, Object> additionalDetails = (Map<String, Object>) bpa.getAdditionalDetails();
			String jsonString = new JSONObject(additionalDetails).toString();
			DocumentContext context = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonString);
			List<String> plotNoFromContext = context.read("planDetail.plot.plotNo");
			if (!CollectionUtils.isEmpty(plotNoFromContext)) {
				plotNo = plotNoFromContext.get(0);
			}

		} catch (Exception ex) {
			log.error("error while extracting plotNo for preapproved plan application", ex);
		}
		return plotNo;
	}
	private String getRiskTypeForPreapprovedPlan(Map<String, Object> drawingDetail) {
		String riskType = "LOW";// TODO
		return riskType;
	}

	private Boolean getIsBUAAbove500ForPreapprovedPlan(Map<String, Object> drawingDetail) {
		Boolean isBuaABove500 = false;
		String totalBUAString = String.valueOf(drawingDetail.get("totalBuitUpArea"));
		Double totalBUA = Double.valueOf(totalBUAString);
		if (totalBUA > 500)
			isBuaABove500 = true;
		return isBuaABove500;
	}
	
	private String getSubService(BPA bpa) {
		Map<String,Object> additionalDetails = new HashMap<>();
		if (Objects.nonNull(bpa) && Objects.nonNull(bpa.getAdditionalDetails())){
			additionalDetails = (Map<String, Object>) bpa.getAdditionalDetails();
		}
		String subservice = null;
		if (Objects.nonNull(additionalDetails.get(BPA_ADD_DETAILS_SERVICE_KEY))
				&& additionalDetails.get(BPA_ADD_DETAILS_SERVICE_KEY) instanceof Map
				&& !StringUtils
						.isEmpty(((Map) additionalDetails.get(BPA_ADD_DETAILS_SERVICE_KEY))
								.get(BPA_ADD_DETAILS_SUBSERVICE_KEY))) {
			subservice = String
					.valueOf(((Map) additionalDetails.get(BPA_ADD_DETAILS_SERVICE_KEY))
							.get(BPA_ADD_DETAILS_SUBSERVICE_KEY));
		}
		return subservice;
	}
	
	private String getPlotNoFromEdcr(DocumentContext edcrResponseDocumentContext) {
		String plotNo = "";
		List<String> plotNoList = edcrResponseDocumentContext.read("edcrDetail.*.planDetail.planInformation.plotNo");
		if (!CollectionUtils.isEmpty(plotNoList)) {
			plotNo = plotNoList.get(0);
		}
		return plotNo;
	}
	
	
	private Boolean getisBUAAbove500FromEdcr(DocumentContext edcrResponseDocumentContext) {
		Double totalBuiltUpArea = 0.0;
		if (edcrResponseDocumentContext
				.read("edcrDetail.*.planDetail.virtualBuilding.totalBuitUpArea") instanceof JSONArray) {
			JSONArray edcrTotalBuiltUpAreas = edcrResponseDocumentContext
					.read("edcrDetail.*.planDetail.virtualBuilding.totalBuitUpArea");
			if (!CollectionUtils.isEmpty(edcrTotalBuiltUpAreas)) {
				if (null != edcrTotalBuiltUpAreas.get(0)) {
					String edcrTotalBuiltUpAreaString = edcrTotalBuiltUpAreas.get(0).toString();
					totalBuiltUpArea = Double.parseDouble(edcrTotalBuiltUpAreaString);
				}
			}
		} else if (edcrResponseDocumentContext
				.read("edcrDetail.*.planDetail.virtualBuilding.totalBuitUpArea") instanceof LinkedList) {
			LinkedList edcrTotalBuiltUpAreas = edcrResponseDocumentContext
					.read("edcrDetail.*.planDetail.virtualBuilding.totalBuitUpArea");
			if (!CollectionUtils.isEmpty(edcrTotalBuiltUpAreas)) {
				if (null != edcrTotalBuiltUpAreas.get(0)) {
					String edcrTotalBuiltUpAreaString = edcrTotalBuiltUpAreas.get(0).toString();
					totalBuiltUpArea = Double.parseDouble(edcrTotalBuiltUpAreaString);
				}
			}
		}
		else {
			log.info("total Builtup area not readable from edcr context");
		}
		Boolean isBUAAbove500 = false;
		if (totalBuiltUpArea > 500)
			isBUAAbove500 = true;
		return isBUAAbove500;
	}
	
	private String getMaxBuildingHeightForPreApprovedPlan(Map<String, Object> drawingDetail) {
		List<Object> blocks = (List<Object>) drawingDetail.get("blocks");
		LinkedTreeMap<Object,Object> building = (LinkedTreeMap<Object,Object>) blocks.get(0);
		Map<String,Object> buildingMap = (Map<String, Object>) building.get("building");
		return (String.valueOf(buildingMap.get("buildingHeight")));
	}


	private String getPlotAreaForPreApprovedPlan(Map<String, Object> drawingDetail) {
		return String.valueOf(drawingDetail.get("plotArea"));
		
	}
	
	private String getLiftCountRelaxationForPreApprovedPlan(Map<String, Object> drawingDetail) {
		return String.valueOf(drawingDetail.get("liftCountRelaxation"));
		
	}


	private String getTotalBuiltUpAreaForPreApprovedPlan(Map<String, Object> drawingDetail) {
		return String.valueOf(drawingDetail.get("totalBuitUpArea"));
	}

	private String getTotalBuiltUpAreaFromEdcr(DocumentContext edcrResponseDocumentContext) {
		
		String edcrTotalBuiltUpAreaString = "";
		if (edcrResponseDocumentContext
				.read("edcrDetail.*.planDetail.virtualBuilding.totalBuitUpArea") instanceof JSONArray) {
			JSONArray edcrTotalBuiltUpAreas = edcrResponseDocumentContext
					.read("edcrDetail.*.planDetail.virtualBuilding.totalBuitUpArea");
			if (!CollectionUtils.isEmpty(edcrTotalBuiltUpAreas)) {
				if (null != edcrTotalBuiltUpAreas.get(0)) {
					edcrTotalBuiltUpAreaString = edcrTotalBuiltUpAreas.get(0).toString();
				}
			}
		} else if (edcrResponseDocumentContext
				.read("edcrDetail.*.planDetail.virtualBuilding.totalBuitUpArea") instanceof LinkedList) {
			LinkedList edcrTotalBuiltUpAreas = edcrResponseDocumentContext
					.read("edcrDetail.*.planDetail.virtualBuilding.totalBuitUpArea");
			if (!CollectionUtils.isEmpty(edcrTotalBuiltUpAreas)) {
				if (null != edcrTotalBuiltUpAreas.get(0)) {
					edcrTotalBuiltUpAreaString = edcrTotalBuiltUpAreas.get(0).toString();
				}
			}
		}
		else {
			log.info("total Builtup area not readable from edcr context");
		}
		return edcrTotalBuiltUpAreaString;
	}
	
	private String getTotalPlotAreaFromEdcr(DocumentContext edcrResponseDocumentContext) {
		String totalPlotAreaString = "";
		List<String> totalPlotArea = edcrResponseDocumentContext.read("edcrDetail.*.planDetail.planInformation.totalPlotArea");		
		if(!CollectionUtils.isEmpty(totalPlotArea)) {
			totalPlotAreaString = String.valueOf(totalPlotArea.get(0));
		}
		return totalPlotAreaString;
	}
	
	private String getCorrelationIdFromEdcr(DocumentContext edcrResponseDocumentContext) {
		String correlationId = "";
		List<String> correlationIds = edcrResponseDocumentContext.read("edcrDetail.*.planDetail.dxfToPdfCorrelationId");
		if(!CollectionUtils.isEmpty(correlationIds)) {
			correlationId = String.valueOf(correlationIds.get(0));
		}
		log.info("Correlation Id : "+correlationId);
		return correlationId;
	}
	
	private String getMaxBuildingHeightFromEdcr(DocumentContext edcrResponseDocumentContext) {
		
		String edcrMaxBuildingHeightString = "";
		if (edcrResponseDocumentContext
				.read("edcrDetail.*.planDetail.virtualBuilding.buildingHeight") instanceof JSONArray) {
			JSONArray edcrMaxBuilding = edcrResponseDocumentContext
					.read("edcrDetail.*.planDetail.virtualBuilding.buildingHeight");
			if (!CollectionUtils.isEmpty(edcrMaxBuilding)) {
				if (null != edcrMaxBuilding.get(0)) {
					edcrMaxBuildingHeightString = edcrMaxBuilding.get(0).toString();
				}
			}
		} else if (edcrResponseDocumentContext
				.read("edcrDetail.*.planDetail.virtualBuilding.buildingHeight") instanceof LinkedList) {
			LinkedList edcrMaxBuilding = edcrResponseDocumentContext
					.read("edcrDetail.*.planDetail.virtualBuilding.buildingHeight");
			if (!CollectionUtils.isEmpty(edcrMaxBuilding)) {
				if (null != edcrMaxBuilding.get(0)) {
					edcrMaxBuildingHeightString = edcrMaxBuilding.get(0).toString();
				}
			}
		}
		else {
			log.info("Max Building Height not readable from edcr context");
		}
		return edcrMaxBuildingHeightString;
	}
	private String getAlterationSubService(DocumentContext edcrResponseDocumentContext) {
		
		String alterationSubServiceString = "";
		List<String> alterationSubService = edcrResponseDocumentContext.read("edcrDetail.*.planDetail.planInformation.alterationSubService");		
		if(!CollectionUtils.isEmpty(alterationSubService)) {
			alterationSubServiceString = String.valueOf(alterationSubService.get(0));
		}
		return alterationSubServiceString;
	}
	
	private String getLiftCountRelaxationFromEdcr(DocumentContext edcrResponseDocumentContext) {
		String liftCountRelaxationString = "";
		List<String> liftCountRelaxation = edcrResponseDocumentContext.read("edcrDetail.*.planDetail.planInformation.liftCountRelaxation");		
		if(!CollectionUtils.isEmpty(liftCountRelaxation)) {
			liftCountRelaxationString = String.valueOf(liftCountRelaxation.get(0));
		}
		return liftCountRelaxationString;
	}
	

	private String getNoOfFloors(DocumentContext edcrResponseDocumentContext) {
		List<Integer> floorNumbers = edcrResponseDocumentContext
				.read("edcrDetail.*.planDetail.blocks[*].building.floorsAboveGround");
		String maxNoOfFloorsInABuildingOtherThanBasement = String
				.valueOf((Integer.valueOf(Collections.max(floorNumbers))));
		System.out.println("Max Floors: " + Collections.max(floorNumbers));
		System.out.println("Total Floors: " + maxNoOfFloorsInABuildingOtherThanBasement);
		return maxNoOfFloorsInABuildingOtherThanBasement;
	}

	private Boolean isBasementPresent(DocumentContext edcrResponseDocumentContext) {
		List<Integer> floorNumbers = edcrResponseDocumentContext
				.read("edcrDetail.*.planDetail.blocks[*].building.floors[*].number");

		Boolean isBasementPresent = floorNumbers.stream().anyMatch(number -> number.equals(-1));

		return isBasementPresent;
	}

	private String getApproachRoadWidth(DocumentContext edcrResponseDocumentContext) {
		String approachRoadWidthString = "";
		List<String> approachRoadWidth = edcrResponseDocumentContext
				.read("edcrDetail.*.planDetail.planInfoProperties.ADJACENT_ROAD_WIDTH");
		if (!org.springframework.util.ObjectUtils.isEmpty(approachRoadWidth.get(0))) {
			approachRoadWidthString = String.valueOf(approachRoadWidth.get(0));
		}
		return approachRoadWidthString;
	}

	private String getProposedFar(DocumentContext edcrResponseDocumentContext) {
		String proposedFARString = "";
		List<String> proposedFAR = edcrResponseDocumentContext.read("edcrDetail.*.planDetail.farDetails.providedFar");
		if (!org.springframework.util.ObjectUtils.isEmpty(proposedFAR.get(0))) {
			proposedFARString = String.valueOf(proposedFAR.get(0));
		}
		return proposedFARString;
	}

	private String getNoOfDwellingUnits(DocumentContext edcrResponseDocumentContext) {
		String noOfDwellingUnitsString = "";
		List<String> noOfDwellingUnits = edcrResponseDocumentContext
				.read("edcrDetail.*.planDetail.planInformation.totalNoOfDwellingUnits");
		if (!org.springframework.util.ObjectUtils.isEmpty(noOfDwellingUnits.get(0))) {
			noOfDwellingUnitsString = String.valueOf(noOfDwellingUnits.get(0));
		}
		return noOfDwellingUnitsString;
	}

	private String isAffordableUnitsPresent(DocumentContext edcrResponseDocumentContext) {

		List<List<String>> ewsUnitsPresentString = edcrResponseDocumentContext
				.read("edcrDetail.*.planDetail.blocks[*].building.floors[*].ewsUnit");

		List<List<String>> ligUnitsPresentString = edcrResponseDocumentContext
				.read("edcrDetail.*.planDetail.blocks[*].building.floors[*].ligUnit");

		boolean containsEWSElements = false;
		boolean containsLIHElements = false;
		
		for (List item : ewsUnitsPresentString) {
			if(!CollectionUtils.isEmpty(item))
				containsEWSElements =  true;
		}
		for (List item : ligUnitsPresentString) {
			if(!CollectionUtils.isEmpty(item))
				containsLIHElements =  true;
		}

		if (containsEWSElements || containsLIHElements) {
			System.out.println("At least one sub-array contains elements.");
			return "Yes";
		} else {
			System.out.println("None of the sub-arrays contain elements.");
			return "No";
		}
	}

	private Boolean isPublicWashRoomProvided(DocumentContext edcrResponseDocumentContext) {
		List<List<String>> publicWashroomJson = edcrResponseDocumentContext.read("edcrDetail.*.planDetail.publicWashroom");

		if(CollectionUtils.isEmpty(publicWashroomJson)) 
			return false;
		if (CollectionUtils.isEmpty(publicWashroomJson.get(0)))
			return false;

		return true;
	}

	private String getOccupancyType(DocumentContext edcrResponseDocumentContext) {
		String occupancyTypeString = "";
		List<String> occupancyType = edcrResponseDocumentContext
				.read("edcrDetail.*.planDetail.planInformation.occupancy");
		if (!CollectionUtils.isEmpty(occupancyType)) {
			occupancyTypeString = String.valueOf(occupancyType.get(0));
		}
		return occupancyTypeString;
	}
	
	private String getNoOfStoreysFromEDCR(DocumentContext edcrResponseDocumentContext) {
		String noOfStoreysString = "";
		List<String> noOfStoreys = edcrResponseDocumentContext
				.read("edcrDetail.*.planDetail.planInformation.floorInfo");
		if (!CollectionUtils.isEmpty(noOfStoreys)) {
			noOfStoreysString = String.valueOf(noOfStoreys.get(0));
		}
		return noOfStoreysString;
	}
	
	private String getProposedFarForPreApprovedPlan(Map<String, Object> drawingDetail) {
		return String.valueOf(drawingDetail.get("totalFar"));
	}


	private String getApproachRoadWidthForPreApprovedPlan(BPARequest request) {
		Map<String, Object> additionalDetails = request.getBPA().getAdditionalDetails() != null ? (Map) request.getBPA().getAdditionalDetails()
				: new HashMap<String, Object>();
		Map<String, String> landDetails = (Map<String, String>) additionalDetails.get("landDetails");
		String roadWidth = String.valueOf(landDetails.get("abuttingRoadWidth"));
		return roadWidth;
	}


	private String getNoOfFloorsForPreApprovedPlan(Map<String, Object> drawingDetail) {
		List<Integer> maxFloorBlockWise = new ArrayList<>();
		List<Object> blocks = (List<Object>) drawingDetail.get("blocks");
		for (Object block : blocks) {
			List<String> floorNumbers = new ArrayList<>();
			LinkedTreeMap<Object, Object> building = (LinkedTreeMap<Object, Object>) block;
			Map<String, Object> buildingMap = (Map<String, Object>) building.get("building");
			List<Map<String, Object>> floorList = (List<Map<String, Object>>) buildingMap.get("floors");
			floorList.stream().forEach(floor -> {
				floor.forEach((key, value) -> {
					if (key.equalsIgnoreCase("floorNo"))
						if (!value.equals(String.valueOf("-1"))) {
							floorNumbers.add((String) value);
						}
				});
			});
			maxFloorBlockWise.add((Integer) floorNumbers.size());
		}
		String maxNoOfFloorsInABuildingOtherThanBasement = String.valueOf((Collections.max(maxFloorBlockWise)));
		System.out.println("Max Floor No: " + Collections.max(maxFloorBlockWise));
		System.out.println("Total Max Floors in a Building: " + maxNoOfFloorsInABuildingOtherThanBasement);
		return maxNoOfFloorsInABuildingOtherThanBasement;

	}


	private Boolean isPublicWashRoomProvidedForPreApprovedPlan(Map<String, Object> drawingDetail) {
		return false;
	}


	private String getNoOfDwellingUnitsForPreApprovedPlan(Map<String, Object> drawingDetail) {
		return String.valueOf("NA");
	}


	private String isAffordableUnitsPresentForPreApprovedPlan(Map<String, Object> drawingDetail) {
		return "NA";
	}


	private Boolean isBasementPresentForPreApprovedPlan(Map<String, Object> drawingDetail) {
		List<String> floorNumbers = new ArrayList<>();
		List<Object> blocks = (List<Object>) drawingDetail.get("blocks");
		LinkedTreeMap<Object,Object> building = (LinkedTreeMap<Object,Object>) blocks.get(0);
		Map<String,Object> buildingMap = (Map<String, Object>) building.get("building");
		List<Map<String,Object>> floorList = (List<Map<String,Object>>) buildingMap.get("floors");
		floorList.stream().forEach(floor -> {
			floor.forEach((key, value) -> {
				if (key.equalsIgnoreCase("floorNo"))
					floorNumbers.add((String) value);
			});
		});
		
		Boolean isBasementPresent = floorNumbers.stream().anyMatch(number -> number.equals("-1"));
		
		return isBasementPresent;
	}


	private String getOccupancyTypeForPreApprovedPlan(Map<String, Object> drawingDetail) {
		return String.valueOf(drawingDetail.get("occupancy"));
	}
	
	private String getNoOfStroreysForPreApprovedPan(Map<String, Object> drawingDetail) {
		return String.valueOf(drawingDetail.get("floorDescription"));
	}
	
	private String getSubOccupancyFromEDCR(DocumentContext edcrResponseDocumentContext) {
		String subOccupancyTypeString = "";
		List<String> subOccupancyType = edcrResponseDocumentContext
				.read("edcrDetail.*.planDetail.planInformation.subOccupancy");
		if (!CollectionUtils.isEmpty(subOccupancyType)) {
			subOccupancyTypeString = String.valueOf(subOccupancyType.get(0));
		}
		return subOccupancyTypeString;
	}
	
	private String getSubOccupancyForPreApprovedPlan(Map<String, Object> drawingDetail) {
		return "Residential Plotted";
	}

}
