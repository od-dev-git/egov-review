package org.egov.bpa.calculator.utils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.egov.bpa.calculator.edcr.model.Occupancy;
import org.egov.bpa.calculator.edcr.model.OccupancytypeHelper;
import org.egov.bpa.calculator.web.models.bpa.BPA;
import org.egov.bpa.calculator.web.models.bpa.edcr.*;
import org.egov.bpa.calculator.web.models.oc.ScrutinyDetails;
import org.egov.tracer.model.CustomException;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

@Component
@Slf4j
public class EdcrHelperUtils {
	
	public static List<Occupancy> getOccupancieswiseDetails(LinkedHashMap edcrResponse){
		
		List<Occupancy> finalList = new ArrayList<>();
	
		try {
	
		List edcrdetails = (List) edcrResponse.get("edcrDetail");
		
		Object planDetails  = ((Map)(edcrdetails.get(0))).get("planDetail");
		
		List blocks  = (List) (((Map) planDetails).get("blocks"));
		
		List<Occupancy> occupancyHelper = new ArrayList<>();
		
		Map<String,Map<String,Occupancy>> occupancyMap = new HashMap<>();
 		
		
for(int i=0; i<blocks.size();i++) {
		log.info("inside block info");	
			Object block =  blocks.get(i);
			
			Object building = ((Map)block).get("building");
			
			List floors = (List) ((Map)building).get("floors");
			
			for(int j=0;j<floors.size();j++) {
			
				Object fllor = floors.get(j);
				
				List  Occupanc = (List) ((Map)fllor).get("occupancies");
				
			for(int k=0;k<Occupanc.size();k++) {
				
				log.info("inside occupancy info");
				Object occ = Occupanc.get(k);
				
				Occupancy occupancy = new  Occupancy() ; 
				
				Object typehelper = ((Map)occ).get("typeHelper");
		    
				occupancy.setType((String)((Map)occ).get("type"));
				ObjectMapper mapper = new ObjectMapper();
				OccupancytypeHelper occupancytypeHelper  = mapper.convertValue(
						typehelper, new TypeReference<OccupancytypeHelper>() {});
				
				occupancy.setTypeHelper(occupancytypeHelper);
				
				//occupancy.setDeduction((double)(Integer)(((Map)occ).get("deduction")));
				Object builtArea=((Map)occ).get("builtUpArea");
				
				BigDecimal builtUpArea = new BigDecimal(builtArea.toString());
				
				occupancy.setBuiltUpArea(builtUpArea.doubleValue());
				
				Object floorArea=((Map)occ).get("floorArea");
				
				BigDecimal fllrarea=  new BigDecimal(floorArea.toString());
				
				occupancy.setFloorArea(fllrarea.doubleValue());
			
				Object carpetAreaObject = ((Map)occ).get("carpetArea");
				
				BigDecimal carpetArea = new BigDecimal(carpetAreaObject.toString());
				
                occupancy.setCarpetArea(carpetArea.doubleValue());
				
				//occupancy.setCarpetAreaDeduction((double)(Integer)(((Map)occ).get("carpetAreaDeduction")));
				
				Object existbuiltup=((Map)occ).get("existingBuiltUpArea");
				
				BigDecimal existBuiltUpArea=  new BigDecimal(existbuiltup.toString());
				occupancy.setExistingBuiltUpArea(existBuiltUpArea.doubleValue());
				
                Object existFllorup=((Map)occ).get("existingFloorArea");
				
				BigDecimal existfloorArea=  new BigDecimal(existFllorup.toString());
				
				occupancy.setExistingFloorArea(existfloorArea.doubleValue());
			
               // occupancy.setExistingBuiltUpArea((double)(Integer)(((Map)occ).get("existingCarpetArea")));
				
				//occupancy.setExistingCarpetAreaDeduction((double)(Integer)(((Map)occ).get("existingCarpetAreaDeduction")));
				

               // occupancy.setExistingDeduction((double)(Integer)(((Map)occ).get("existingDeduction")));
                
                occupancy.setSubOccupancyCode(occupancy.getTypeHelper().getOccupancySubType().getCode());
                
                
                occupancy.setOccupancyCode(occupancy.getTypeHelper().getOccupancytype().getCode());
				
                occupancyHelper.add(occupancy);
				
				
		        
			}
			}}


Set<String> typehelper = occupancyHelper.stream().filter(o->o.getSubOccupancyCode()!=null).map(Occupancy::getSubOccupancyCode).collect(Collectors.toSet());	
//Set<OccupancytypeHelper> subType = occupancyHelper.stream().filter(o->o.getTypeHelper()!=null).map(Occupancy::getTypeHelper).collect(Collectors.toSet());	

for(String code:typehelper) {
	
	log.info("inside final cal info");
	
	Occupancy occ = new Occupancy();
	List<Double> builtupArea = occupancyHelper.stream().filter(o->o.getSubOccupancyCode().equals(code)).map(Occupancy::getBuiltUpArea).collect(Collectors.toList());
	List<Double> floorArea   = occupancyHelper.stream().filter(o->o.getSubOccupancyCode().equals(code)).map(Occupancy::getFloorArea).collect(Collectors.toList());
	List<Double> existbuiltupArea = occupancyHelper.stream().filter(o->o.getSubOccupancyCode().equals(code)).map(Occupancy::getExistingBuiltUpArea).collect(Collectors.toList());
	List<Double> existfloorArea   = occupancyHelper.stream().filter(o->o.getSubOccupancyCode().equals(code)).map(Occupancy::getExistingFloorArea).collect(Collectors.toList());
	List<Double> carpetArea = occupancyHelper.stream().filter(o->o.getSubOccupancyCode().equals(code)).map(Occupancy::getCarpetArea).collect(Collectors.toList());
	
	Set<OccupancytypeHelper> subType =occupancyHelper.stream().filter(o->o.getSubOccupancyCode().equals(code)).map(Occupancy::getTypeHelper).collect(Collectors.toSet());

Double finalBuiltupArea = builtupArea.stream().mapToDouble(a -> a)
		    .sum();
	Double finalfloorArea = floorArea.stream().mapToDouble(a -> a)
		    .sum();
	Double existbuiltUpArea =existbuiltupArea.stream().mapToDouble(a -> a)
		    .sum();
	Double existFloorArea = existfloorArea.stream().mapToDouble(a -> a)
		    .sum();
	Double finalCarpetArea = carpetArea.stream().mapToDouble(a -> a)
			.sum();
   occ.setBuiltUpArea(finalBuiltupArea);
   System.out.println("final"+finalfloorArea);
   occ.setFloorArea(finalfloorArea);
   occ.setExistingBuiltUpArea(existbuiltUpArea);
   occ.setExistingFloorArea(existFloorArea);
   occ.setCarpetArea(finalCarpetArea);
 // occ.setType(code);
  // occ.setTypeHelper(null);
  occ.setSubOccupancyCode(code);
  occ.setTypeHelper(subType.stream().findFirst().get());
  occ.setOccupancyCode(occ.getTypeHelper().getOccupancytype().getCode());
  
   finalList.add(occ);
  
}	
return finalList;
		}catch(Exception ex) {
			throw new CustomException("Drwaing Detail Error","Error while fetching drawing Detail :"+ex);
		}
		//return finalList;
		
	}

	public List<Occupancy> getOccupancieswiseDetailsforpreApproved(Object drawingDetail) {
		
		try {
			
			log.info("inside preApproved info");
		List<Occupancy> finalList = new ArrayList<>();
		
		Occupancy occ = new Occupancy();
		
		Object subocc = ((Map) drawingDetail).get("subOccupancy");
		
		occ.setSubOccupancyCode((String)  ((Map) subocc).get("value"));
		
		log.info("inside preAprroved occupancy info"+occ.getSubOccupancyCode());
		
		Object builtArea = ((Map)drawingDetail).get("totalBuitUpArea");
		
		BigDecimal builtUpArea = new BigDecimal(builtArea.toString());
		
		occ.setBuiltUpArea(builtUpArea.doubleValue());
		
     Object fllrArea = ((Map)drawingDetail).get("totalFloorArea");
		
		BigDecimal floorArea = new BigDecimal(fllrArea.toString());
		occ.setFloorArea(floorArea.doubleValue());
		occ.setOccupancyCode("A");
         
		
		finalList.add(occ);
		
		return finalList;
		
		}catch(Exception ex) {
			throw new CustomException("Drwaing Detail Error","Error while fetching drawing Detail :"+ex);
		}
		
		
	}
	
	@SuppressWarnings("unchecked")
	public Map<Integer, BigDecimal> prepareApprovedConstructionData(LinkedHashMap edcrResponse) {
		Map<Integer, BigDecimal> colourcodeVsAreaMap = new HashMap<>();
		List<Object> edcrdetails = (List) edcrResponse.get("edcrDetail");

		Map<String, Object> planDetails = (Map<String, Object>) ((Map<String, ?>) (edcrdetails.get(0)))
				.get("planDetail");

		List<Object> blocks = (List<Object>) (planDetails.get("blocks"));
		for (Object block : blocks) {
			log.info("inside block info");

			Map<String, Object> building = (Map<String, Object>) ((Map<String, ?>) block).get("building");

			List<Object> floors = (List<Object>) building.get("floors");

			for (Object floorInfo : floors) {
				Map<String, Object> floor = (Map<String, Object>) floorInfo;
				Map<String, Object> approvedConstruction = new HashMap<>();
				if (Objects.nonNull(floor) && Objects.nonNull(floor.get("approvedConstruction"))
						&& floor.get("approvedConstruction") instanceof List
						&& !((List) floor.get("approvedConstruction")).isEmpty()
						&& Objects.nonNull(((List) floor.get("approvedConstruction")).get(0))
						&& ((List) floor.get("approvedConstruction")).get(0) instanceof Map) {
					for (Object approvedConstructionObject : (List) floor.get("approvedConstruction")) {
						approvedConstruction = (Map<String, Object>) approvedConstructionObject;
						addToColourcodeVsAreaMap(colourcodeVsAreaMap,
								Integer.valueOf(approvedConstruction.get("colorCode") + ""),
								new BigDecimal(approvedConstruction.get("area") + ""));
					}
				}
			}
		}
		return colourcodeVsAreaMap;
	}
	
	private void addToColourcodeVsAreaMap(Map<Integer, BigDecimal> colourcodeVsAreaMap, Integer colourCode,
			BigDecimal areaToAdd) {
		if (colourcodeVsAreaMap.containsKey(colourCode)) {
			BigDecimal addedArea = colourcodeVsAreaMap.get(colourCode).add(areaToAdd);
			colourcodeVsAreaMap.put(colourCode, addedArea);
		} else {
			colourcodeVsAreaMap.put(colourCode, areaToAdd);
		}
	}

	/**
	 *
	 * @param bpa
	 *
	 * Purpose: To enrich permit and oc edcr detail from user input scrutiny details for OC outside Sujog
	 */
	public void enrichOCOutsideEdcrDetailsFromRequest(BPA bpa){
		EdcrDetail edcrDetailForPermit = new EdcrDetail();
		EdcrDetail edcrDetailForOC = new EdcrDetail();

		CustomEdcrDetail permitEdcrDetail = new CustomEdcrDetail();
		CustomEdcrDetail ocEdcrDetail = new CustomEdcrDetail();

		List<ScrutinyDetails> scrutinyDetailsList=bpa.getOutsideOCDetails().getScrutinyDetails();
		enrichUUIDInScrutinyDetails(scrutinyDetailsList);

		List<ScrutinyDetails> permitScrutinyDetailsList=scrutinyDetailsList.stream().filter(scrutinyDetails1 -> scrutinyDetails1.getScrutinyType().equalsIgnoreCase("PERMIT")).collect(Collectors.toList());
		List<ScrutinyDetails> ocScrutinyDetailsList=scrutinyDetailsList.stream().filter(scrutinyDetails1 -> scrutinyDetails1.getScrutinyType().equalsIgnoreCase("OC")).collect(Collectors.toList());

		populateEDCRDetail(permitScrutinyDetailsList.get(0), edcrDetailForPermit);
		permitEdcrDetail.setEdcrDetail(Arrays.asList(edcrDetailForPermit));


		populateEDCRDetail(ocScrutinyDetailsList.get(0),edcrDetailForOC);
		ocEdcrDetail.setEdcrDetail(Arrays.asList(edcrDetailForOC));

		bpa.setPermitEdcrDetail(permitEdcrDetail);
		bpa.setOcEdcrDetail(ocEdcrDetail);

	}

	/**
	 *
	 * @param scrutinyDetailsList
	 *
	 * Purpose: To add UUID in Scrutiny Details Coming from Request
	 */
	private void enrichUUIDInScrutinyDetails(List<ScrutinyDetails> scrutinyDetailsList) {
		scrutinyDetailsList.forEach(scrutinyDetails -> {
			if(StringUtils.isEmpty(scrutinyDetails.getId()))
				scrutinyDetails.setId(UUID.randomUUID().toString());
		});
	}

	/**
	 *
	 * @param scrutinyDetails
	 * @param edcrDetail
	 *
	 * Purpose: To create EdcrDetail from ScrutinyDetail
	 */
	private void populateEDCRDetail(ScrutinyDetails scrutinyDetails, EdcrDetail edcrDetail){
		Plan plan = new Plan();

		enrichPlotDetails(plan,scrutinyDetails);
		enrichPlanInformationDetails(plan,scrutinyDetails);
		enrichFarDetails(plan,scrutinyDetails);
		enrichVirtualBuildingDetails(plan,scrutinyDetails);
		
		edcrDetail.setPlanDetail(plan);
	}

	private void enrichVirtualBuildingDetails(Plan plan, ScrutinyDetails scrutinyDetails) {

		VirtualBuilding virtualBuilding = VirtualBuilding.builder()
				.totalBuitUpArea(scrutinyDetails.getTotalBuiltUpArea())
				.totalCarpetArea(scrutinyDetails.getTotalCarpetArea())
				.totalFloorArea(scrutinyDetails.getTotalFloorArea()).build();
		enrichMostRestrictiveFarHelper(virtualBuilding,scrutinyDetails);
		plan.setVirtualBuilding(virtualBuilding);
	}

	private void enrichMostRestrictiveFarHelper(VirtualBuilding virtualBuilding, ScrutinyDetails scrutinyDetails) {

		OccupancyHelperDetail occupancyTypeHelperDetail = OccupancyHelperDetail.builder()
				.code(scrutinyDetails.getOccupancyTypeHelperCode()).build();
		OccupancyHelperDetail occpancySubTypeHelperDetail = OccupancyHelperDetail.builder()
				.code(scrutinyDetails.getOccupancySubTypeHelperCode()).build();

		OccupancyTypeHelper occupancyTypeHelper = OccupancyTypeHelper.builder()
				.type(occupancyTypeHelperDetail)
				.subtype(occpancySubTypeHelperDetail).build();

		virtualBuilding.setMostRestrictiveFarHelper(occupancyTypeHelper);
	}

	private void enrichFarDetails(Plan plan, ScrutinyDetails scrutinyDetails) {
		FarDetails farDetails = FarDetails.builder()
				.baseFar(scrutinyDetails.getBaseFar())
				.providedFar(scrutinyDetails.getProvidedFar())
				.permissableFar(scrutinyDetails.getMaxPermissibleFar())
				.tdrFarRelaxation(scrutinyDetails.getTdrFarRelaxation()).build();
		plan.setFarDetails(farDetails);
	}

	private void enrichPlanInformationDetails(Plan plan, ScrutinyDetails scrutinyDetails) {

		PlanInformation planInformation = PlanInformation.builder()
				.totalNoOfDwellingUnits(scrutinyDetails.getTotalNoOfDwellingUnits())
				.benchmarkValuePerAcre(scrutinyDetails.getBenchmarkValuePerAcre())
				.isSecurityDepositRequired(scrutinyDetails.isSecurityDepositRequired())
				.shelterFeeRequired(scrutinyDetails.isShelterFeeRequired())
				.projectValueForEIDP(scrutinyDetails.getProjectValueForEIDP())
				.isRetentionFeeApplicable(scrutinyDetails.isRetentionFeeApplicable())
				.numberOfTemporaryStructures(scrutinyDetails.getNumberOfTemporaryStructures())
				.isProjectUndertakingByGovt(scrutinyDetails.getIsProjectUndertakingByGovt()).build();

		plan.setPlanInformation(planInformation);


	}

	private void enrichPlotDetails(Plan plan, ScrutinyDetails scrutinyDetails) {
		Plot plot = Plot.builder().build();
		plot.setArea(scrutinyDetails.getPlotDetails().getNetPlotArea());
		plan.setPlot(plot);
	}

	@SuppressWarnings("unchecked")
	public void getPublicWashroomDetails(Map<String, Object> paramMap, JSONArray occupancyPercentages) {
		
		for (Object object : occupancyPercentages) {
			
			LinkedHashMap<String, Object> occupancy = (LinkedHashMap<String, Object>) object;
			
			if(occupancy.get("subOccupancy").toString().equalsIgnoreCase("Public Washrooms")) {
				paramMap.put(BPACalculatorConstants.IS_PUBLIC_WASHROOM_PRESENT, BPACalculatorConstants.YES);
				Double builtupArea = getDoubleValue(occupancy.get("totalBuildUpArea"));
				paramMap.put(BPACalculatorConstants.PUBLIC_WASHROOM_BUILTUP_AREA,builtupArea);
			}
			
		}
		
	}
	
	@SuppressWarnings("unchecked")
	public void getICTDetails(Map<String, Object> paramMap, JSONArray occupancyPercentages) {
		
		for (Object object : occupancyPercentages) {
			
			LinkedHashMap<String, Object> occupancy = (LinkedHashMap<String, Object>) object;
			
			if(occupancy.get("subOccupancy").toString().equalsIgnoreCase("Information and Communication Technology")) {
				paramMap.put(BPACalculatorConstants.IS_ICT_PRESENT, BPACalculatorConstants.YES);
				Double builtupArea = getDoubleValue(occupancy.get("totalBuildUpArea"));
				paramMap.put(BPACalculatorConstants.ICT_BUILTUP_AREA, builtupArea);
			}
			
		}
		
	}


	/**
	 * @param value
	 * @return DoubleValue
	 */
	public Double getDoubleValue(Object value) {
		Double doubleValue = 0d;
		if(ObjectUtils.isNotEmpty(value)) {
			try {
				doubleValue = Double.valueOf(String.valueOf(value));
			} catch (Exception e) {}
		}
		return doubleValue;
	}

}
