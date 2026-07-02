package org.egov.bpa.calculator.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.egov.bpa.calculator.config.BPACalculatorConfig;
import org.egov.bpa.calculator.edcr.model.Occupancy;
import org.egov.bpa.calculator.repository.InstallmentRepository;
import org.egov.bpa.calculator.utils.BPACalculatorConstants;
import org.egov.bpa.calculator.web.models.Calculation;
import org.egov.bpa.calculator.web.models.CalculationReq;
import org.egov.bpa.calculator.web.models.CalulationCriteria;
import org.egov.bpa.calculator.web.models.ExemptionFeeDetails;
import org.egov.bpa.calculator.web.models.Installment;
import org.egov.bpa.calculator.web.models.InstallmentSearchCriteria;
import org.egov.bpa.calculator.web.models.OtherFeeDetails;
import org.egov.bpa.calculator.web.models.Revision;
import org.egov.bpa.calculator.web.models.bpa.BPA;
import org.egov.bpa.calculator.web.models.demand.Category;
import org.egov.bpa.calculator.web.models.demand.Demand;
import org.egov.bpa.calculator.web.models.demand.DemandDetail;
import org.egov.bpa.calculator.web.models.demand.TaxHeadEstimate;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AlterationCalculationService {
	
	@Autowired
	private DemandService demandService;
	
	@Autowired
	private CalculationService calculationService;
	
	@Autowired
	private ObjectMapper mapper;
	
	@Autowired
	private InstallmentRepository installmentRepository;
	
	@Autowired
	private BPACalculatorConfig config;
	
	private static final BigDecimal ZERO_TWO_FIVE = new BigDecimal("0.25");// BigDecimal.valueOf(0.25);
	private static final BigDecimal ZERO_FIVE = new BigDecimal("0.5");// BigDecimal.valueOf(0.5);
	private static final BigDecimal TEN = new BigDecimal("10");// BigDecimal.valueOf(10);
	private static final BigDecimal FIFTEEN = new BigDecimal("15");// BigDecimal.valueOf(15);
//	private static final BigDecimal SEVENTEEN_FIVE = new BigDecimal("17.50");// BigDecimal.valueOf(17.50);
	private static final BigDecimal SEVENTEEN_POINT_EIGHT_FIVE = new BigDecimal("17.85");// BigDecimal.valueOf(17.50);
	private static final BigDecimal EIGHTEEN_POINT_TWO_ONE = new BigDecimal("18.21");
	private static final BigDecimal EIGHTEEN_POINT_FIVE_SEVEN = new BigDecimal("18.57");
	private static final BigDecimal TWENTY = new BigDecimal("20");// BigDecimal.valueOf(20);
	private static final BigDecimal TWENTY_FIVE = new BigDecimal("25");// BigDecimal.valueOf(25);
	private static final BigDecimal THIRTY = new BigDecimal("30");// BigDecimal.valueOf(30);
	private static final BigDecimal FIFTY = new BigDecimal("50");// BigDecimal.valueOf(50);
	private static final BigDecimal HUNDRED = new BigDecimal("100");// BigDecimal.valueOf(100);
	private static final BigDecimal TWO_HUNDRED = new BigDecimal("200");// BigDecimal.valueOf(200);
	private static final BigDecimal TWO_HUNDRED_FIFTY = new BigDecimal("250");// BigDecimal.valueOf(250);
	private static final BigDecimal THREE_HUNDRED = new BigDecimal("300");// BigDecimal.valueOf(300);
	private static final BigDecimal FIVE_HUNDRED = new BigDecimal("500");// BigDecimal.valueOf(500);
	private static final BigDecimal FIFTEEN_HUNDRED = new BigDecimal("1500");// BigDecimal.valueOf(1500);
	private static final BigDecimal SEVENTEEN_FIFTY = new BigDecimal("1750");// BigDecimal.valueOf(1750);
	private static final BigDecimal EIGHTEEN_FIFTY_SEVEN = new BigDecimal("1857.42");// BigDecimal.valueOf(1857.42);
	private static final BigDecimal EIGHTEEN_NINTY_FOUR = new BigDecimal("1894");// BigDecimal.valueOf(1894);
	private static final BigDecimal TWO_THOUSAND = new BigDecimal("2000");// BigDecimal.valueOf(2000);
	private static final BigDecimal THOUSAND = new BigDecimal("1000");// BigDecimal.valueOf(2000);
	private static final BigDecimal TEN_LAC = new BigDecimal("1000000");// BigDecimal.valueOf(1000000);
	private static final BigDecimal SQMT_SQFT_MULTIPLIER = new BigDecimal("10.764");// BigDecimal.valueOf(10.764);
	private static final BigDecimal ACRE_SQMT_MULTIPLIER = new BigDecimal("4046.85");// BigDecimal.valueOf(4046.85);
	
	public BigDecimal calculateFeeForBuildingOperation(Map<String, Object> paramMap,
			ArrayList<TaxHeadEstimate> estimates) {
		BigDecimal feeForBuildingOperation = BigDecimal.ZERO;
		BigDecimal feesForBuildingOperation = BigDecimal.ZERO;
		Double totalexistingBuitUpArea=null;
		Double totalBuitUpArea = null;
		String occupancyType = null;
		List<Occupancy> occupancyies = null;
		String subservice = null;
		if (Objects.nonNull(paramMap.get(BPACalculatorConstants.BPA_ADD_DETAILS_SUBSERVICE_KEY))) {
			subservice = (String) paramMap.get(BPACalculatorConstants.BPA_ADD_DETAILS_SUBSERVICE_KEY);
		}
		Boolean isRefApplicationPresentInSujog = Boolean.FALSE;
		if (Objects.nonNull(paramMap.get(BPACalculatorConstants.IS_REF_APPLICATION_PRESENT_IN_SUJOG))) {
			isRefApplicationPresentInSujog = (Boolean) paramMap.get(BPACalculatorConstants.IS_REF_APPLICATION_PRESENT_IN_SUJOG);
		}
		Map<String, Object> paramMapBackup = new HashMap<>();
		if (BPACalculatorConstants.ALTERATION_SUBSERVICE_B.equals(subservice) && !isRefApplicationPresentInSujog
				&& !((boolean) paramMap.get(BPACalculatorConstants.IS_REVISED_AREA_LESS_OR_EQUAL_TO_OLD_AREA))) {
			paramMapBackup.putAll(paramMap);
			BPA bpa = (BPA) (paramMap.get("BPA"));
			CalulationCriteria calculationCriteria = CalulationCriteria.builder().applicationNo(bpa.getApplicationNo())
					.applicationType(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY).bpa(bpa)
					.feeType(BPACalculatorConstants.MDMS_CALCULATIONTYPE_APL_FEETYPE)
					.serviceType(BPACalculatorConstants.ALTERATION).tenantId(bpa.getTenantId()).build();
			calculationService.prepareParamMapForBpa1to4((RequestInfo) paramMap.get("requestInfo"), calculationCriteria,
					BPACalculatorConstants.MDMS_CALCULATIONTYPE_APL_FEETYPE, paramMap);
		}
		
		totalBuitUpArea = getProposedAreaParameterForAlteration(paramMap);
		log.info("totalBuitUpArea outside:"+totalBuitUpArea);
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCYLIST)) {
			occupancyies = (List<Occupancy>) paramMap.get(BPACalculatorConstants.OCCUPANCYLIST);
		}
		
		Occupancy subOccupancyWithMaxCoverage = occupancyies.stream()
		        .max((o1, o2) -> Double.compare(o1.getBuiltUpArea(), o2.getBuiltUpArea()))
		        .orElse(null);
		
		for(Occupancy occupancy : occupancyies) {
			totalBuitUpArea = occupancy.getBuiltUpArea();
			log.info("Alt App Fee: total builtup area before public washroom addition :" + totalBuitUpArea);
			
			// Add public Washroom area in Built Up Area if this subOccupanecy has max coverage
			if (occupancy.equals(subOccupancyWithMaxCoverage)) {
				totalBuitUpArea = addBuiltUpAreaForPublicWashroom(totalBuitUpArea, paramMap, occupancy);
				totalBuitUpArea = addBuiltUpAreaForICT(totalBuitUpArea, paramMap, occupancy);
			}
			
			totalexistingBuitUpArea = occupancy.getExistingBuiltUpArea();
			log.info("totalBuitUpArea inside:"+totalBuitUpArea);
			occupancyType = occupancy.getOccupancyCode();
			paramMap.put(BPACalculatorConstants.OCCUPANCY_TYPE, occupancyType);
			Double proposedBuiltUpArea = totalBuitUpArea - totalexistingBuitUpArea;
			paramMap.put(BPACalculatorConstants.ALTERATION_PROPOSED_BUILTUP_AREA, proposedBuiltUpArea);
			paramMap.put(BPACalculatorConstants.SUB_OCCUPANCY_TYPE,occupancy.getSubOccupancyCode());
			log.info("occupancy inside:"+occupancyType);
			log.info("suboccupancy inside:"+occupancy.getSubOccupancyCode());
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (totalBuitUpArea != null) {
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.A))) {
				feeForBuildingOperation = calculateBuildingOperationFeeForResidentialOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.B))) {
				feeForBuildingOperation = calculateBuildingOperationFeeForCommercialOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.C))) {
				feeForBuildingOperation = calculateBuildingOperationFeeForPublicSemiPublicInstitutionalOccupancy(
						paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.D))) {
				feeForBuildingOperation = calculateBuildingOperationFeeForPublicUtilityOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.E))) {
				feeForBuildingOperation = calculateBuildingOperationFeeForIndustrialZoneOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.F))) {
				feeForBuildingOperation = calculateBuildingOperationFeeForEducationOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.G))) {
				feeForBuildingOperation = calculateBuildingOperationFeeForTransportationOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.H))) {
				feeForBuildingOperation = calculateBuildingOperationFeeForAgricultureOccupancy(paramMap);
			}
			 feesForBuildingOperation = feesForBuildingOperation.add(feeForBuildingOperation) ;
		}
		
		}
		if (Objects.nonNull(paramMap.get(BPACalculatorConstants.IS_REVISED_AREA_LESS_OR_EQUAL_TO_OLD_AREA))
				&& (boolean) paramMap.get(BPACalculatorConstants.IS_REVISED_AREA_LESS_OR_EQUAL_TO_OLD_AREA)) {
			//using old area for half for subservice A-
			feesForBuildingOperation = feesForBuildingOperation.divide(new BigDecimal(2), 0, BigDecimal.ROUND_HALF_UP);
		}
		generateTaxHeadEstimate(estimates, feesForBuildingOperation,
				BPACalculatorConstants.TAXHEAD_BPA_BUILDING_OPERATION_FEE, Category.FEE);
		log.info("FeeForBuildingOperationAlteration:::::::::::" + feesForBuildingOperation);
		if (BPACalculatorConstants.ALTERATION_SUBSERVICE_B.equals(subservice) && !isRefApplicationPresentInSujog
				&& !((boolean) paramMap.get(BPACalculatorConstants.IS_REVISED_AREA_LESS_OR_EQUAL_TO_OLD_AREA))) {
			// revert back paramMap to its original state
			paramMap.putAll(paramMapBackup);
		}
		return feesForBuildingOperation;
	}
	
	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateBuildingOperationFeeForResidentialOccupancy(Map<String, Object> paramMap) {
		BigDecimal feeForBuildingOperation = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double totalBuitUpArea = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		totalBuitUpArea = getProposedAreaParameterForAlteration(paramMap);
		log.info("totalBuitUpArea residentialAlteration:"+totalBuitUpArea);
		
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.A))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION) || serviceType
									.equalsIgnoreCase(BPACalculatorConstants.ALTERATION_SERVICE_TYPE_FOR_BPA7)))) {
				feeForBuildingOperation = calculateVariableFee1(totalBuitUpArea);
			}

		}
		return feeForBuildingOperation;
	}
	
	/**
	 * @param totalBuitUpArea
	 * @return
	 */
	private BigDecimal calculateVariableFee1(Double totalBuitUpArea) {
		BigDecimal amount = BigDecimal.ZERO;
		if (null != totalBuitUpArea) {
			if (totalBuitUpArea <= 100) {
				amount = TWO_HUNDRED_FIFTY;
			} else if (totalBuitUpArea <= 300) {
				amount = (TWO_HUNDRED_FIFTY
						.add(FIFTEEN.multiply(BigDecimal.valueOf(totalBuitUpArea).subtract(HUNDRED)))).setScale(2,
								BigDecimal.ROUND_UP);
			} else if (totalBuitUpArea > 300) {
				amount = (TWO_HUNDRED_FIFTY.add(FIFTEEN.multiply(TWO_HUNDRED))
						.add(TEN.multiply(BigDecimal.valueOf(totalBuitUpArea).subtract(THREE_HUNDRED)))).setScale(2,
								BigDecimal.ROUND_UP);
			}

		}
		return amount;
	}
	
	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateBuildingOperationFeeForCommercialOccupancy(Map<String, Object> paramMap) {
		BigDecimal feeForBuildingOperation = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double totalBuitUpArea = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		totalBuitUpArea = getProposedAreaParameterForAlteration(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.B))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION) || serviceType
									.equalsIgnoreCase(BPACalculatorConstants.ALTERATION_SERVICE_TYPE_FOR_BPA7)))) {
				feeForBuildingOperation = calculateVariableFee2(totalBuitUpArea);
			}

		}
		return feeForBuildingOperation;
	}
	
	/**
	 * @param totalBuitUpArea
	 * @return
	 */
	private BigDecimal calculateVariableFee2(Double totalBuitUpArea) {
		BigDecimal amount = BigDecimal.ZERO;
		if (null != totalBuitUpArea) {
			if (totalBuitUpArea <= 20) {
				amount = FIVE_HUNDRED;
			} else if (totalBuitUpArea <= 50) {
				amount = (FIVE_HUNDRED.add(FIFTY.multiply(BigDecimal.valueOf(totalBuitUpArea).subtract(TWENTY))))
						.setScale(2, BigDecimal.ROUND_UP);
			} else if (totalBuitUpArea > 50) {
				amount = (FIVE_HUNDRED.add(FIFTY.multiply(THIRTY))
						.add(TWENTY.multiply(BigDecimal.valueOf(totalBuitUpArea).subtract(FIFTY)))).setScale(2,
								BigDecimal.ROUND_UP);
			}

		}
		return amount;
	}
	
	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateBuildingOperationFeeForPublicSemiPublicInstitutionalOccupancy(
			Map<String, Object> paramMap) {
		BigDecimal feeForBuildingOperation = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		String subOccupancyType = null;
		Double totalBuitUpArea = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE)) {
			subOccupancyType = (String) paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE);
		}
		totalBuitUpArea = getProposedAreaParameterForAlteration(paramMap);
		if (((occupancyType.equalsIgnoreCase(BPACalculatorConstants.C))) && (StringUtils.hasText(subOccupancyType))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION) || serviceType
									.equalsIgnoreCase(BPACalculatorConstants.ALTERATION_SERVICE_TYPE_FOR_BPA7)))) {
				if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_A))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_B))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_C))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_CL))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_MP))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_CH))) {
					feeForBuildingOperation = calculateVariableFee2(totalBuitUpArea);

				} else if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_O))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_OAH))) {
					// feeForBuildingOperation = calculateConstantFee(paramMap, 5);
					feeForBuildingOperation = calculateConstantFeeNew(totalBuitUpArea, 5);

				} else if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_SC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_C1H))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_C2H))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_SCC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_CC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_EC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_G))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_MH))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_ML))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_M))) {
					feeForBuildingOperation = calculateVariableFee2(totalBuitUpArea);

				} else if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_PW))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_PL))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_REB))) {
					// feeForBuildingOperation = calculateConstantFee(paramMap, 5);
					feeForBuildingOperation = calculateConstantFeeNew(totalBuitUpArea, 5);

				} else if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_SPC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_S))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_T))) {
					feeForBuildingOperation = calculateVariableFee2(totalBuitUpArea);

				} else if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_AB))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_GO))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_LSGO))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_P))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_RB))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_SWC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_CI))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_D))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_YC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_DC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_GSGH))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_RT))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_HC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_H))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_L))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_MTH))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_MB))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_NH))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_PLY))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_RC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_VHAB))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_RTI))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_PS))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_FS))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_J))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_PO))) {

					// feeForBuildingOperation = calculateConstantFee(paramMap, 5);
					feeForBuildingOperation = calculateConstantFeeNew(totalBuitUpArea, 5);

				}
			}

		}
		return feeForBuildingOperation;
	}
	
	/**
	 * @param effectiveArea
	 * @param multiplicationFactor
	 * @return
	 */
	private BigDecimal calculateConstantFeeNew(Double effectiveArea, int multiplicationFactor) {
		BigDecimal totalAmount = BigDecimal.ZERO;
		if (null != effectiveArea) {
			totalAmount = (BigDecimal.valueOf(effectiveArea).multiply(BigDecimal.valueOf(multiplicationFactor)))
					.setScale(2, BigDecimal.ROUND_UP);

		}
		return totalAmount;
	}
	
	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateBuildingOperationFeeForPublicUtilityOccupancy(Map<String, Object> paramMap) {
		BigDecimal feeForBuildingOperation = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double totalBuitUpArea = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		totalBuitUpArea = getProposedAreaParameterForAlteration(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.D))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION) || serviceType
									.equalsIgnoreCase(BPACalculatorConstants.ALTERATION_SERVICE_TYPE_FOR_BPA7)))) {
				// feeForBuildingOperation = calculateConstantFee(paramMap, 5);
				feeForBuildingOperation = calculateConstantFeeNew(totalBuitUpArea, 5);

			}
		}
		return feeForBuildingOperation;
	}
	
	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateBuildingOperationFeeForIndustrialZoneOccupancy(Map<String, Object> paramMap) {
		BigDecimal feeForBuildingOperation = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double totalBuitUpArea = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		totalBuitUpArea = getProposedAreaParameterForAlteration(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.E))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION) || serviceType
									.equalsIgnoreCase(BPACalculatorConstants.ALTERATION_SERVICE_TYPE_FOR_BPA7)))) {
				feeForBuildingOperation = calculateVariableFee3(totalBuitUpArea);
			}
		}
		return feeForBuildingOperation;
	}
	
	/**
	 * @param totalBuitUpArea
	 * @return
	 */
	private BigDecimal calculateVariableFee3(Double totalBuitUpArea) {
		BigDecimal amount = BigDecimal.ZERO;
		if (null != totalBuitUpArea) {
			if (totalBuitUpArea <= 100) {
				amount = FIFTEEN_HUNDRED;
			} else if (totalBuitUpArea <= 300) {
				amount = (FIFTEEN_HUNDRED
						.add(TWENTY_FIVE.multiply(BigDecimal.valueOf(totalBuitUpArea).subtract(HUNDRED)))).setScale(2,
								BigDecimal.ROUND_UP);
			} else if (totalBuitUpArea > 300) {
				amount = (FIFTEEN_HUNDRED.add(TWENTY_FIVE.multiply(TWO_HUNDRED))
						.add(FIFTEEN.multiply(BigDecimal.valueOf(totalBuitUpArea).subtract(THREE_HUNDRED)))).setScale(2,
								BigDecimal.ROUND_UP);
			}

		}
		return amount;
	}
	
	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateBuildingOperationFeeForEducationOccupancy(Map<String, Object> paramMap) {
		BigDecimal feeForBuildingOperation = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double totalBuitUpArea = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		totalBuitUpArea = getProposedAreaParameterForAlteration(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.F))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION) || serviceType
									.equalsIgnoreCase(BPACalculatorConstants.ALTERATION_SERVICE_TYPE_FOR_BPA7)))) {
				// feeForBuildingOperation = calculateConstantFee(paramMap, 5);
				feeForBuildingOperation = calculateConstantFeeNew(totalBuitUpArea, 5);
			}
		}
		return feeForBuildingOperation;
	}
	
	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateBuildingOperationFeeForTransportationOccupancy(Map<String, Object> paramMap) {
		BigDecimal feeForBuildingOperation = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double totalBuitUpArea = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		totalBuitUpArea = getProposedAreaParameterForAlteration(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.G))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION) || serviceType
									.equalsIgnoreCase(BPACalculatorConstants.ALTERATION_SERVICE_TYPE_FOR_BPA7)))) {
				// feeForBuildingOperation = calculateConstantFee(paramMap, 5);
				feeForBuildingOperation = calculateConstantFeeNew(totalBuitUpArea, 5);
			}
		}
		return feeForBuildingOperation;
	}
	
	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateBuildingOperationFeeForAgricultureOccupancy(Map<String, Object> paramMap) {
		BigDecimal feeForBuildingOperation = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double totalBuitUpArea = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		totalBuitUpArea = getProposedAreaParameterForAlteration(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.H))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION) || serviceType
									.equalsIgnoreCase(BPACalculatorConstants.ALTERATION_SERVICE_TYPE_FOR_BPA7)))) {
				feeForBuildingOperation = calculateVariableFee1(totalBuitUpArea);
			}
		}
		return feeForBuildingOperation;
	}
	
	//SANCTION FEES CALCULATIONS-
	
	public BigDecimal calculateTotalSanctionFeeForPermit(Map<String, Object> paramMap, ArrayList<TaxHeadEstimate> estimates) {
		BigDecimal calculatedTotalPermitFee = BigDecimal.ZERO;
		BigDecimal sanctionFee = calculateSanctionFee(paramMap, estimates);
		BigDecimal constructionWorkerWelfareCess = calculateConstructionWorkerWelfareCess(paramMap, estimates);
		BigDecimal shelterFee = calculateShelterFee(paramMap, estimates);
		BigDecimal temporaryRetentionFee = calculateTemporaryRetentionFee(paramMap, estimates);
		BigDecimal securityDeposit = calculateSecurityDeposit(paramMap, estimates);
		BigDecimal purchasableFAR = calculatePurchasableFAR(paramMap, estimates);
		BigDecimal eidpFee = calculateEIDPFee(paramMap, estimates);
		BigDecimal adjustmentAmount = calculateAdjustmentAmount(paramMap, estimates);
		BigDecimal totalOtherFeesAmount = calculateAllOtherFees(paramMap, estimates);
		// Add totalExcemptionFee for Outside Sujog applicaiton
		calculatedTotalPermitFee = (calculatedTotalPermitFee.add(sanctionFee).add(constructionWorkerWelfareCess)
				.add(shelterFee).add(temporaryRetentionFee).add(securityDeposit).add(purchasableFAR)
				.add(eidpFee).add(adjustmentAmount).add(totalOtherFeesAmount))
				.setScale(2, BigDecimal.ROUND_UP);
		
		// if revision application then calculate total amount after adjustments-
		BPA bpa = (BPA) paramMap.get("BPA");
		String subservice = null;
		Boolean isRevisionApplication = Boolean.FALSE;
		Boolean isRefApplicationPresentInSujog = Boolean.FALSE;
		if (Objects.nonNull(paramMap.get(BPACalculatorConstants.BPA_ADD_DETAILS_SUBSERVICE_KEY))) {
			subservice = (String) paramMap.get(BPACalculatorConstants.BPA_ADD_DETAILS_SUBSERVICE_KEY);
		}
		if (Objects.nonNull(paramMap.get(BPACalculatorConstants.IS_REVISION_APPLICATION))) {
			isRevisionApplication = (Boolean) paramMap.get(BPACalculatorConstants.IS_REVISION_APPLICATION);
		}
		if (Objects.nonNull(paramMap.get(BPACalculatorConstants.IS_REF_APPLICATION_PRESENT_IN_SUJOG))) {
			isRefApplicationPresentInSujog = (Boolean) paramMap
					.get(BPACalculatorConstants.IS_REF_APPLICATION_PRESENT_IN_SUJOG);
		}
		log.info("isRevisionApplication : "+isRevisionApplication);
		if (isRevisionApplication) {
			log.info("isRefApplicationPresentInSujog : "+isRefApplicationPresentInSujog);
			if (!isRefApplicationPresentInSujog) {
				Revision revision = (Revision) paramMap.get(BPACalculatorConstants.REVISION);
				calculateForRevisionNonSujogAppl(paramMap, estimates, revision);
				calculatedTotalPermitFee = calculateTotalAmountForRevision(estimates);
				return calculatedTotalPermitFee;
			} else {
				calculateForRevisionSujogExistingAppl(paramMap, estimates);
				calculatedTotalPermitFee = calculateTotalAmountForRevision(estimates);
			}
		}
		
		// calculate application fees again if reworkhistory is there and compare with
		// payment done and add calculations for adjustments in separate taxheads--
		calculationService.processApplicationFeesAfterRework(paramMap, estimates);
		
		return calculatedTotalPermitFee;
	}
	
	private void calculateForRevisionNonSujogAppl(Map<String, Object> paramMap, ArrayList<TaxHeadEstimate> estimates,
			Revision revision) {
		log.info("Inside calculateForRevisionNonSujogAppl method");
		BPA bpa = (BPA) paramMap.get("BPA");
		// set IsRevisionApplication to false for normal calculation-
		Map<String, Object>	additionalDetails = (Map<String, Object>) bpa.getAdditionalDetails();
		Object alterationServiceNode = additionalDetails.get(BPACalculatorConstants.BPA_ADD_DETAILS_SERVICE_KEY);
		additionalDetails.remove(BPACalculatorConstants.BPA_ADD_DETAILS_SERVICE_KEY);
		CalulationCriteria calculationCriteria = CalulationCriteria.builder()
				.applicationNo(revision.getBpaApplicationNo())
				.applicationType(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY).bpa(bpa)
				.feeType(BPACalculatorConstants.MDMS_CALCULATIONTYPE_SANC_FEETYPE)
				.serviceType(BPACalculatorConstants.ALTERATION).tenantId(bpa.getTenantId()).build();
		List<CalulationCriteria> calculationCriterias = new ArrayList<>();
		calculationCriterias.add(calculationCriteria);
		CalculationReq calculationReq = CalculationReq.builder().calulationCriteria(calculationCriterias)
				.requestInfo((RequestInfo) paramMap.get("requestInfo")).build();
		// calculate the estimates as per old parameters -
		List<Calculation> calculationsForCurrentApplicationWithoutRevision = calculationService.getEstimate(calculationReq);
		List<TaxHeadEstimate> estimatesForCurrentApplicationWithoutRevision = calculationsForCurrentApplicationWithoutRevision
				.get(0).getTaxHeadEstimates();
		for (TaxHeadEstimate estimateAsPerCurrentApplicationWORevision : estimatesForCurrentApplicationWithoutRevision) {
			//note: do not touch other fee(adjustment amount) as it might be required in new estimate
			Optional<TaxHeadEstimate> taxHeadEstimateSearchAsPerOldParameters = estimates.stream()
					.filter(estimate -> StringUtils.hasText(estimate.getTaxHeadCode())
							&& !estimate.getTaxHeadCode().contains(BPACalculatorConstants.BPA_SANC_ADJUSTMENT_AMOUNT)
							&& !BPACalculatorConstants.TAXHEAD_BPA_TEMP_RETENTION_FEE.equals(estimate.getTaxHeadCode())
							&& estimate.getTaxHeadCode().equals(estimateAsPerCurrentApplicationWORevision.getTaxHeadCode()))
					.findFirst();
			log.info("taxHeadEstimateSearchAsPerOldParameters : "+taxHeadEstimateSearchAsPerOldParameters);
			if (taxHeadEstimateSearchAsPerOldParameters.isPresent()
					&& estimateAsPerCurrentApplicationWORevision.getEstimateAmount()
							.compareTo(taxHeadEstimateSearchAsPerOldParameters.get().getEstimateAmount()) > 0) {
				estimateAsPerCurrentApplicationWORevision
						.setEstimateAmount(estimateAsPerCurrentApplicationWORevision.getEstimateAmount()
								.subtract(taxHeadEstimateSearchAsPerOldParameters.get().getEstimateAmount()));
			log.info("Inside substract of Current Revision : "+estimateAsPerCurrentApplicationWORevision.getTaxHeadCode()+" : "+estimateAsPerCurrentApplicationWORevision.getEstimateAmount());
			log.info("Substract Amount: "+taxHeadEstimateSearchAsPerOldParameters.get().getEstimateAmount());
			}
			else if (taxHeadEstimateSearchAsPerOldParameters.isPresent()) {
				estimateAsPerCurrentApplicationWORevision.setEstimateAmount(BigDecimal.ZERO);
			log.info("Inside else : "+taxHeadEstimateSearchAsPerOldParameters.get().getTaxHeadCode());
			}
		}
		estimates.clear();
		estimates.addAll(estimatesForCurrentApplicationWithoutRevision);
		additionalDetails.put(BPACalculatorConstants.BPA_ADD_DETAILS_SERVICE_KEY, alterationServiceNode);
		//bpa.setIsRevisionApplication(true);

	}
	
	private void calculateForRevisionSujogExistingAppl(Map<String, Object> paramMap, ArrayList<TaxHeadEstimate> estimates) {
		// if isSujogExistingApplication=true, then fetch old application demand details-
		    log.info("Inside calculateForRevisionSujogExistingAppl method");
			BPA bpa = (BPA) paramMap.get(BPACalculatorConstants.PARAM_MAP_BPA);
			RequestInfo requestInfo = (RequestInfo) paramMap.get("requestInfo");
			String subservice = (String) paramMap.get(BPACalculatorConstants.BPA_ADD_DETAILS_SUBSERVICE_KEY);
			
			String tenantId = String.valueOf(paramMap.get("tenantId"));
			Set<String> consumerCode = new HashSet<>();
			BPA refBpa = null;
			if (Objects.nonNull(paramMap.get(BPACalculatorConstants.REF_BPA_APPLICATION))) {
				refBpa = (BPA) paramMap.get(BPACalculatorConstants.REF_BPA_APPLICATION);
			}
			consumerCode.add(refBpa.getApplicationNo());
			InstallmentSearchCriteria installmentSearchCriteria = InstallmentSearchCriteria.builder()
					.tenantId(tenantId).consumerCode(refBpa.getApplicationNo()).installmentNos(Arrays.asList(-1)).build();
			List<Installment> installments= installmentRepository.getInstallments(installmentSearchCriteria);
			try {
				log.info("Installments Object: "+mapper.writeValueAsString(installments));
			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(CollectionUtils.isEmpty(installments))
				return;
			for (TaxHeadEstimate estimate : estimates) {
				Optional<Installment> installmentOptional = installments.stream()
						.filter(installment -> installment.getTaxHeadCode().equals(estimate.getTaxHeadCode())
								&& (!BPACalculatorConstants.TAXHEAD_BPA_TEMP_RETENTION_FEE
								.equals(estimate.getTaxHeadCode()) 
								|| !subservice.equals(BPACalculatorConstants.ALTERATION_SUBSERVICE_C)))
						.filter(installment -> StringUtils.hasText(estimate.getTaxHeadCode())
								&& installment.getTaxHeadCode().equals(estimate.getTaxHeadCode())
								&& !estimate.getTaxHeadCode().contains(BPACalculatorConstants.BPA_SANC_ADJUSTMENT_AMOUNT))
						.findFirst();
				if (installmentOptional.isPresent()) {
					Installment installment = installmentOptional.get();
					switch (estimate.getTaxHeadCode()) {
					default:
						if (estimate.getEstimateAmount().compareTo(installment.getTaxAmount()) > 0)
							estimate.setEstimateAmount(estimate.getEstimateAmount().subtract(installment.getTaxAmount()));
						else
							estimate.setEstimateAmount(BigDecimal.ZERO);
						break;
					}
					
				}
				log.info("**AlterationCalculationService method calculateForRevisionSujogExistingAppl**taxhead:"
						+ estimate.getTaxHeadCode() + ", taxAmount:" + estimate.getEstimateAmount());
			}

	}

	private BigDecimal calculateTotalAmountForRevision(ArrayList<TaxHeadEstimate> estimates) {
		BigDecimal calculatedTotalPermitFee = BigDecimal.ZERO;
		for (TaxHeadEstimate estimate : estimates) {
			calculatedTotalPermitFee.add(estimate.getEstimateAmount());
		}
		return calculatedTotalPermitFee;
	}
	
	/**
	 * @param paramMap
	 * @param estimates
	 * @return
	 */
	private BigDecimal calculateSanctionFee(Map<String, Object> paramMap, ArrayList<TaxHeadEstimate> estimates) {
		BigDecimal sanctionFee = BigDecimal.ZERO;
		BigDecimal sanctionFees = BigDecimal.ZERO;
		Double totalBuitUpArea = null;
		Double totalExistBuitUpArea = null;
		String occupancyType = null;
		List<Occupancy> occupancyies= new ArrayList<>();
		String subservice = null;
		if (Objects.nonNull(paramMap.get(BPACalculatorConstants.BPA_ADD_DETAILS_SUBSERVICE_KEY))) {
			subservice = (String) paramMap.get(BPACalculatorConstants.BPA_ADD_DETAILS_SUBSERVICE_KEY);
		}
		totalBuitUpArea = getProposedAreaParameterForAlteration(paramMap);
//		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
//			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
//		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCYLIST)) {
			occupancyies = (List<Occupancy>) paramMap.get(BPACalculatorConstants.OCCUPANCYLIST);
		}
		Map<Integer, BigDecimal> colorcodeVsApprovedAreaMap = new HashMap<>();
		if (null != paramMap.get(BPACalculatorConstants.APPROVED_CONSTRUCTION_OCCUPANCYWISE)) {
			colorcodeVsApprovedAreaMap = (Map<Integer, BigDecimal>) paramMap
					.get(BPACalculatorConstants.APPROVED_CONSTRUCTION_OCCUPANCYWISE);
		}
		
		Occupancy subOccupancyWithMaxCoverage = occupancyies.stream()
		        .max((o1, o2) -> Double.compare(o1.getBuiltUpArea(), o2.getBuiltUpArea()))
		        .orElse(null);
		
		Boolean isOutsideSujogApplExemptionEstimate = isOutsideSujogApplExcemptionEstimate(paramMap, estimates);
		if (isOutsideSujogApplExemptionEstimate && config.getCalculateExemptionForFeeOutsideSujogAppl()) {
			BPA bpa = (BPA) paramMap.get("BPA");
			List<ExemptionFeeDetails> exemptionFeeDetails = getAllExemptionFeesDetails(
					((Map) bpa.getAdditionalDetails()).get(BPACalculatorConstants.BPA_ADD_DETAILS_OUTSIDE_SUJOG_APPL),
					BPACalculatorConstants.TAXHEAD_BPA_SANCTION_FEE);
			BigDecimal totalAmountAfterExemption = BigDecimal.ZERO;
			for (ExemptionFeeDetails excmptionFeeDetail : exemptionFeeDetails) {
				try {
					totalAmountAfterExemption = totalAmountAfterExemption.add(excmptionFeeDetail.getTotalAmount());
				} catch (Exception e) {
					e.printStackTrace();
				}
				sanctionFees.add(totalAmountAfterExemption);
			}
		} else  for(Occupancy occupancy : occupancyies) {
			
			totalBuitUpArea = occupancy.getBuiltUpArea();
			
			log.info("Sanction Fee: builtup area before public washroom addition: " + totalBuitUpArea);
			
			// Add public Washroom area in Built Up Area if this subOccupanecy has max coverage
			if(occupancy.equals(subOccupancyWithMaxCoverage)) {
				totalBuitUpArea = addBuiltUpAreaForPublicWashroom(totalBuitUpArea, paramMap, occupancy);
				totalBuitUpArea = addBuiltUpAreaForICT(totalBuitUpArea, paramMap, occupancy);
			}

			log.info("BuitUpArea inside:" + totalBuitUpArea);
			totalExistBuitUpArea=occupancy.getExistingBuiltUpArea();
			log.info("totalBuitUpArea inside:"+totalBuitUpArea+			totalExistBuitUpArea);
			occupancyType = occupancy.getOccupancyCode();
			paramMap.put(BPACalculatorConstants.OCCUPANCY_TYPE, occupancyType);
			Double proposedBuiltUpArea = totalBuitUpArea-totalExistBuitUpArea;
			paramMap.put(BPACalculatorConstants.ALTERATION_PROPOSED_BUILTUP_AREA, proposedBuiltUpArea);
			paramMap.put(BPACalculatorConstants.SUB_OCCUPANCY_TYPE,occupancy.getSubOccupancyCode());
			log.info("occupancy inside:"+occupancyType);
			log.info("suboccupancy inside:"+occupancy.getSubOccupancyCode());
			log.info("subservice : "+subservice);
			//check and use delta for alterationSubService=ALTERATION_SERVICE_D only for sanction fee taxhead--
			if (Objects.nonNull(subservice)
					&& subservice.equalsIgnoreCase(BPACalculatorConstants.ALTERATION_SUBSERVICE_D)) {				
				String colorcodeString = occupancy.getTypeHelper().getOccupancySubType().getColor();
				log.info("colorcodeString : "+colorcodeString);
				Integer colorcode = Integer.parseInt(colorcodeString);
				BigDecimal builtupAreaApproved = BigDecimal.ZERO;
				if (colorcodeVsApprovedAreaMap.containsKey(colorcode)) {
					builtupAreaApproved = colorcodeVsApprovedAreaMap.get(colorcode);
					log.info("builtupAreaApproved : "+builtupAreaApproved);		
				}
				if (builtupAreaApproved.compareTo(BigDecimal.ZERO) > 0) {
					BigDecimal deltaBuiltupAreaFromPreviousConstruction = builtupAreaApproved
							.subtract(BigDecimal.valueOf(totalExistBuitUpArea));
					log.info("deltaBuiltupAreaFromPreviousConstruction : "+deltaBuiltupAreaFromPreviousConstruction);	
					if (deltaBuiltupAreaFromPreviousConstruction.compareTo(BigDecimal.ZERO) > 0) {
						log.info("proposedBuiltUpArea before calculation : "+proposedBuiltUpArea);				
						proposedBuiltUpArea = proposedBuiltUpArea
								- deltaBuiltupAreaFromPreviousConstruction.doubleValue();
						log.info("proposedBuiltUpArea after calculation : "+proposedBuiltUpArea);
						paramMap.put(BPACalculatorConstants.ALTERATION_PROPOSED_BUILTUP_AREA, proposedBuiltUpArea);
						
					}
				}
			}
			
		if (totalBuitUpArea != null) {
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.A))) {
				sanctionFee = calculateSanctionFeeForResidentialOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.B))) {
				sanctionFee = calculateSanctionFeeForCommercialOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.C))) {
				sanctionFee = calculateSanctionFeeForPublicSemiPublicInstitutionalOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.D))) {
				sanctionFee = calculateSanctionFeeForPublicUtilityOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.E))) {
				sanctionFee = calculateSanctionFeeForIndustrialZoneOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.F))) {
				sanctionFee = calculateSanctionFeeForEducationOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.G))) {
				sanctionFee = calculateSanctionFeeForTransportationOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.H))) {
				sanctionFee = calculateSanctionFeeForAgricultureOccupancy(paramMap);
			}

		}
		sanctionFees =  sanctionFees.add(sanctionFee);
        }
	
		if (sanctionFees.compareTo(BigDecimal.ZERO) == -1) {
			log.info("Negative sanctionFees::::::::::::::" + sanctionFees);
			sanctionFees = BigDecimal.ZERO;
		}

		generateTaxHeadEstimate(estimates, sanctionFees, BPACalculatorConstants.TAXHEAD_BPA_SANCTION_FEE, Category.FEE);

		// System.out.println("SanctionFee::::::::" + sanctionFee);
		log.info("SanctionFee::::::::" + sanctionFees);
		return sanctionFees;
	}
	
	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateSanctionFeeForResidentialOccupancy(Map<String, Object> paramMap) {
		BigDecimal sanctionFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		String subOccupancyType = null;
		Double totalBuitUpArea = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE)) {
			subOccupancyType = (String) paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE);
		}
		totalBuitUpArea = getProposedAreaParameterForAlteration(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.A) && StringUtils.hasText(subOccupancyType))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION) || serviceType
									.equalsIgnoreCase(BPACalculatorConstants.ALTERATION_SERVICE_TYPE_FOR_BPA7)))) {
				if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_P))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_S))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_R))) {
					// sanctionFee = calculateConstantFee(paramMap, 15);
					sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 15);

				} else {
					// sanctionFee = calculateConstantFee(paramMap, 50);
					sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 50);
				}

			}

		}
		return sanctionFee;
	}
	
	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateSanctionFeeForCommercialOccupancy(Map<String, Object> paramMap) {
		BigDecimal sanctionFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double totalBuitUpArea = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		totalBuitUpArea = getProposedAreaParameterForAlteration(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.B))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION) || serviceType
									.equalsIgnoreCase(BPACalculatorConstants.ALTERATION_SERVICE_TYPE_FOR_BPA7)))) {
				// sanctionFee = calculateConstantFee(paramMap, 60);
				sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 60);
			}
		}
		return sanctionFee;
	}
	
	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateSanctionFeeForPublicSemiPublicInstitutionalOccupancy(Map<String, Object> paramMap) {
		BigDecimal sanctionFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		String subOccupancyType = null;
		Double totalBuitUpArea = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE)) {
			subOccupancyType = (String) paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE);
		}
		totalBuitUpArea = getProposedAreaParameterForAlteration(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.C)) && (StringUtils.hasText(subOccupancyType))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION) || serviceType
									.equalsIgnoreCase(BPACalculatorConstants.ALTERATION_SERVICE_TYPE_FOR_BPA7)))) {
				if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_A))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_B))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_C))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_CL))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_MP))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_CH))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_O))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_OAH))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_SC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_C1H))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_C2H))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_SCC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_CC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_EC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_G))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_MH))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_ML))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_M))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_PW))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_PL))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_REB))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_SPC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_S))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_T))) {

					// sanctionFee = calculateConstantFee(paramMap, 30);
					sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 30);

				} else if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_AB))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_GO))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_LSGO))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_P))) {
					// sanctionFee = calculateConstantFee(paramMap, 10);
					sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 10);
				} else if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_SWC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_CI))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_D))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_YC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_DC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_GSGH))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_RT))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_HC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_H))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_L))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_MTH))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_MB))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_NH))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_PLY))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_RC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_VHAB))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_RTI))) {
					// sanctionFee = calculateConstantFee(paramMap, 30);
					sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 30);
				} else if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_PS))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_FS))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_J))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_PO))) {
					// sanctionFee = calculateConstantFee(paramMap, 10);
					sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 10);
				}
			}
		}
		return sanctionFee;
	}
	
	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateSanctionFeeForPublicUtilityOccupancy(Map<String, Object> paramMap) {
		BigDecimal sanctionFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double totalBuitUpArea = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		totalBuitUpArea = getProposedAreaParameterForAlteration(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.D))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION) || serviceType
									.equalsIgnoreCase(BPACalculatorConstants.ALTERATION_SERVICE_TYPE_FOR_BPA7)))) {
				// sanctionFee = calculateConstantFee(paramMap, 10);
				sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 10);
			}
		}
		return sanctionFee;
	}
	
	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateSanctionFeeForIndustrialZoneOccupancy(Map<String, Object> paramMap) {
		BigDecimal sanctionFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double totalBuitUpArea = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		totalBuitUpArea = getProposedAreaParameterForAlteration(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.E))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION) || serviceType
									.equalsIgnoreCase(BPACalculatorConstants.ALTERATION_SERVICE_TYPE_FOR_BPA7)))) {
				// sanctionFee = calculateConstantFee(paramMap, 60);
				sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 60);
			}
		}
		return sanctionFee;
	}
	
	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateSanctionFeeForEducationOccupancy(Map<String, Object> paramMap) {
		BigDecimal sanctionFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double totalBuitUpArea = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		totalBuitUpArea = getProposedAreaParameterForAlteration(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.F))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION) || serviceType
									.equalsIgnoreCase(BPACalculatorConstants.ALTERATION_SERVICE_TYPE_FOR_BPA7)))) {
				// sanctionFee = calculateConstantFee(paramMap, 30);
				sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 30);
			}
		}
		return sanctionFee;
	}
	
	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateSanctionFeeForTransportationOccupancy(Map<String, Object> paramMap) {
		BigDecimal sanctionFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double totalBuitUpArea = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		totalBuitUpArea = getProposedAreaParameterForAlteration(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.G))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION) || serviceType
									.equalsIgnoreCase(BPACalculatorConstants.ALTERATION_SERVICE_TYPE_FOR_BPA7)))) {
				// sanctionFee = calculateConstantFee(paramMap, 10);
				sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 10);
			}
		}
		return sanctionFee;
	}
	
	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateSanctionFeeForAgricultureOccupancy(Map<String, Object> paramMap) {
		BigDecimal sanctionFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double totalBuitUpArea = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		totalBuitUpArea = getProposedAreaParameterForAlteration(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.H))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION) || serviceType
									.equalsIgnoreCase(BPACalculatorConstants.ALTERATION_SERVICE_TYPE_FOR_BPA7)))) {
				// sanctionFee = calculateConstantFee(paramMap, 30);
				sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 30);
			}
		}
		return sanctionFee;
	}
	
	/**
	 * @param paramMap
	 * @param estimates
	 * @return
	 */
	private BigDecimal calculateConstructionWorkerWelfareCess(Map<String, Object> paramMap,
			ArrayList<TaxHeadEstimate> estimates) {
		log.info("Inside calculateConstructionWorkerWelfareCess");
		BigDecimal welfareCessRate = BigDecimal.ZERO;
		BigDecimal costOfConstruction = BigDecimal.ZERO;
		BigDecimal welfareCess = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		Double totalFloorArea = null;
		Double alterationTotalBuiltupAreaProposed = null;
		Double alterationTotalBuiltupArea = null;
		Double totalBuiltup =0.0;
		Double totalexistinguiltuparea =0.0;
		BigDecimal builtupAreaApproved = BigDecimal.ZERO;
		Double proposedBuiltUpArea = 0.0;
		Boolean isTheCwwcFeeAlreadyPaid = Boolean.FALSE;
		List<Occupancy> occupancyies = new ArrayList<>();
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCYLIST)) {
			occupancyies = (List<Occupancy>) paramMap.get(BPACalculatorConstants.OCCUPANCYLIST);
		}
		String subservice = null;
		if (Objects.nonNull(paramMap.get(BPACalculatorConstants.BPA_ADD_DETAILS_SUBSERVICE_KEY))) {
			subservice = (String) paramMap.get(BPACalculatorConstants.BPA_ADD_DETAILS_SUBSERVICE_KEY);
		}
		
		Map<Integer, BigDecimal> colorcodeVsApprovedAreaMap = new HashMap<>();
		if (null != paramMap.get(BPACalculatorConstants.APPROVED_CONSTRUCTION_OCCUPANCYWISE)) {
			colorcodeVsApprovedAreaMap = (Map<Integer, BigDecimal>) paramMap
					.get(BPACalculatorConstants.APPROVED_CONSTRUCTION_OCCUPANCYWISE);
		}
		
		Occupancy subOccupancyWithMaxCoverage = occupancyies.stream()
		        .max((o1, o2) -> Double.compare(o1.getBuiltUpArea(), o2.getBuiltUpArea()))
		        .orElse(null);
		
		for(Occupancy occupancy : occupancyies) {
			
			totalexistinguiltuparea += occupancy.getExistingBuiltUpArea();
			log.info("totalexistinguiltuparea inside:"+totalexistinguiltuparea);
			totalBuiltup += occupancy.getBuiltUpArea();
			
			log.info("CWC Fee: builtup area before public washroom addition: " + totalBuiltup);
			
			// Add public Washroom area in Built Up Area if this subOccupanecy has max coverage
			if(occupancy.equals(subOccupancyWithMaxCoverage)) {
				totalBuiltup = addBuiltUpAreaForPublicWashroom(totalBuiltup, paramMap, occupancy);
				totalBuiltup = addBuiltUpAreaForICT(totalBuiltup, paramMap, occupancy);

			}

			log.info("totalBuitUpArea inside:"+totalBuiltup);
			
			if(!ObjectUtils.isEmpty(occupancy.getTypeHelper()) && !ObjectUtils.isEmpty(occupancy.getTypeHelper().getOccupancySubType())
					&& !ObjectUtils.isEmpty(occupancy.getTypeHelper().getOccupancySubType().getColor())) {
				String colorcodeString = occupancy.getTypeHelper().getOccupancySubType().getColor();
				log.info("colorcodeString : " + colorcodeString);
				Integer colorcode = Integer.parseInt(colorcodeString);

				if (colorcodeVsApprovedAreaMap.containsKey(colorcode)) {
					builtupAreaApproved = builtupAreaApproved.add(colorcodeVsApprovedAreaMap.get(colorcode));
					log.info("builtupAreaApproved : " + builtupAreaApproved);
				}	
			}
		}
		
		proposedBuiltUpArea = totalBuiltup - totalexistinguiltuparea;
		// Substract delta area from previous construction for ADDITION_ALTERATION_D service
		if (Objects.nonNull(subservice)
				&& subservice.equalsIgnoreCase(BPACalculatorConstants.ALTERATION_SUBSERVICE_D)) {
			if (builtupAreaApproved.compareTo(BigDecimal.ZERO) > 0) {
				BigDecimal deltaBuiltupAreaFromPreviousConstruction = builtupAreaApproved
						.subtract(BigDecimal.valueOf(totalexistinguiltuparea));
				log.info("deltaBuiltupAreaFromPreviousConstruction : " + deltaBuiltupAreaFromPreviousConstruction);
				if (deltaBuiltupAreaFromPreviousConstruction.compareTo(BigDecimal.ZERO) > 0) {
					proposedBuiltUpArea = proposedBuiltUpArea - deltaBuiltupAreaFromPreviousConstruction.doubleValue();
					log.info("proposedBuiltUpArea after calculation : " + proposedBuiltUpArea);
				} 
			}
		}
		
		paramMap.put(BPACalculatorConstants.ALTERATION_PROPOSED_BUILTUP_AREA, proposedBuiltUpArea);
		paramMap.put(BPACalculatorConstants.ALTERATION_TOTAL_BUILTUP_AREA,totalBuiltup);
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.TOTAL_FLOOR_AREA)) {
			totalFloorArea = (Double) paramMap.get(BPACalculatorConstants.TOTAL_FLOOR_AREA);
		}
		if(null != paramMap.get(BPACalculatorConstants.CWCFEE)) {
			welfareCessRate = new BigDecimal((String)paramMap.get(BPACalculatorConstants.CWCFEE));
		}
		if(null != paramMap.get(BPACalculatorConstants.COST_OF_CONSTRUCTION)) {
			costOfConstruction = new BigDecimal((String)paramMap.get(BPACalculatorConstants.COST_OF_CONSTRUCTION));
		}
		// Get the variable to check if CWWC is already paid or not
		if (null != paramMap.get(BPACalculatorConstants.IS_THE_CWWC_FEE_ALREADY_PAID)) {
			isTheCwwcFeeAlreadyPaid = (Boolean) paramMap.get(BPACalculatorConstants.IS_THE_CWWC_FEE_ALREADY_PAID);
		}
		alterationTotalBuiltupAreaProposed = getProposedAreaParameterForAlteration(paramMap);
		log.info("alterationTotalBuiltupAreaProposed : "+alterationTotalBuiltupAreaProposed);
		alterationTotalBuiltupArea = getTotalAreaParameterForAlteration(paramMap);
		log.info("alterationTotalBuiltupArea : "+alterationTotalBuiltupArea);
		Boolean isOutsideSujogApplExemptionEstimate = isOutsideSujogApplExcemptionEstimate(paramMap, estimates);
		if (isOutsideSujogApplExemptionEstimate && config.getCalculateExemptionForFeeOutsideSujogAppl()) {
			BPA bpa = (BPA) paramMap.get("BPA");
			List<ExemptionFeeDetails> exemptionFeeDetails = getAllExemptionFeesDetails(
					((Map) bpa.getAdditionalDetails()).get(BPACalculatorConstants.BPA_ADD_DETAILS_OUTSIDE_SUJOG_APPL),
					BPACalculatorConstants.TAXHEAD_BPA_WORKER_WELFARE_CESS);
			BigDecimal totalAmountAfterExemption = BigDecimal.ZERO;
			for (ExemptionFeeDetails excmptionFeeDetail : exemptionFeeDetails) {
				try {
					totalAmountAfterExemption = totalAmountAfterExemption.add(excmptionFeeDetail.getTotalAmount());
				} catch (Exception e) {
					e.printStackTrace();
				}
				welfareCessRate.add(totalAmountAfterExemption);
			}
		} else if ((StringUtils.hasText(applicationType)
				&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
				&& (StringUtils.hasText(serviceType)
						&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION) || serviceType
								.equalsIgnoreCase(BPACalculatorConstants.ALTERATION_SERVICE_TYPE_FOR_BPA7)))) {
			// Double costOfConstruction = (1750 * totalBuitUpArea * 10.764);
			//Use builtup area instead of floor area to calculate totalCostOfConstruction and check if totalCostOfConstruction>10Lakh.If true,
			//then use builtup area instead of floor area to calculate ConstructionWorkerWelfareCess-
			
			log.info("CWWC already paid :" + isTheCwwcFeeAlreadyPaid);
			// if CWWC already paid, then CWWC fee is exempted
			if (!isTheCwwcFeeAlreadyPaid) {
				BigDecimal totalCostOfConstruction = (costOfConstruction
						.multiply(BigDecimal.valueOf(alterationTotalBuiltupArea)).multiply(SQMT_SQFT_MULTIPLIER))
						.setScale(2, BigDecimal.ROUND_UP);
				log.info("totalCostOfConstruction : " + totalCostOfConstruction);
				if (totalCostOfConstruction.compareTo(TEN_LAC) > 0) {
					welfareCess = (welfareCessRate.multiply(BigDecimal.valueOf(alterationTotalBuiltupAreaProposed))
							.multiply(SQMT_SQFT_MULTIPLIER)).setScale(2, BigDecimal.ROUND_UP);
				}
			}
		
		}
		
		
		// Avoid Negative Calculation
		if (welfareCess.compareTo(BigDecimal.ZERO) == -1) {
			log.info("Negative WelfareCess::::::::::::::" + welfareCess);
			welfareCess = BigDecimal.ZERO;
		}
		generateTaxHeadEstimate(estimates, welfareCess, BPACalculatorConstants.TAXHEAD_BPA_WORKER_WELFARE_CESS, Category.FEE);
		// System.out.println("WelfareCess::::::::::::::" + welfareCess);
		log.info("WelfareCess::::::::::::::" + welfareCess);
		return welfareCess;

	}
	
	/**
	 * @param paramMap
	 * @param estimates
	 * @return
	 */
	private BigDecimal calculateShelterFee(Map<String, Object> paramMap, ArrayList<TaxHeadEstimate> estimates) {
	    log.info("Inside calculateShelterFee");
	    BigDecimal shelterFee = BigDecimal.ZERO;
		BigDecimal shelterFees = BigDecimal.ZERO;
		Double totalBuitUpArea = null;
		Double totalExistBuitUpArea = null;
		String occupancyType = null;
		totalBuitUpArea = getProposedAreaParameterForAlteration(paramMap);
         List<Occupancy> occupancyies = new ArrayList<>();
		
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCYLIST)) {
			occupancyies = (List<Occupancy>) paramMap.get(BPACalculatorConstants.OCCUPANCYLIST);
		}
		for(Occupancy occupancy : occupancyies) {
		totalBuitUpArea = occupancy.getFloorArea();
		totalExistBuitUpArea =occupancy.getExistingBuiltUpArea();
		log.info("totalBuitUpArea inside:"+totalBuitUpArea);
		occupancyType = occupancy.getOccupancyCode();
		paramMap.put(BPACalculatorConstants.OCCUPANCY_TYPE, occupancyType);
		paramMap.put(BPACalculatorConstants.ALTERATION_PROPOSED_BUILTUP_AREA,(totalBuitUpArea-totalExistBuitUpArea));
		paramMap.put(BPACalculatorConstants.SUB_OCCUPANCY_TYPE,occupancy.getSubOccupancyCode());
		log.info("occupancy inside:"+occupancyType);
		log.info("suboccupancy inside:"+occupancy.getSubOccupancyCode());
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		Boolean isOutsideSujogApplExemptionEstimate = isOutsideSujogApplExcemptionEstimate(paramMap, estimates);
		if (isOutsideSujogApplExemptionEstimate && config.getCalculateExemptionForFeeOutsideSujogAppl()) {
			BPA bpa = (BPA) paramMap.get("BPA");
			List<ExemptionFeeDetails> exemptionFeeDetails = getAllExemptionFeesDetails(
					((Map) bpa.getAdditionalDetails()).get(BPACalculatorConstants.BPA_ADD_DETAILS_OUTSIDE_SUJOG_APPL),
					BPACalculatorConstants.TAXHEAD_BPA_SHELTER_FEE);
			BigDecimal totalAmountAfterExemption = BigDecimal.ZERO;
			for (ExemptionFeeDetails excmptionFeeDetail : exemptionFeeDetails) {
				try {
					totalAmountAfterExemption = totalAmountAfterExemption.add(excmptionFeeDetail.getTotalAmount());
				} catch (Exception e) {
					e.printStackTrace();
				}
				shelterFees.add(totalAmountAfterExemption);
			}
		} else if (totalBuitUpArea != null) {
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.A))) {
				shelterFee = calculateShelterFeeForResidentialOccupancy(paramMap);
			}
		}
		shelterFees = shelterFees.add(shelterFee);
		if(shelterFees.compareTo(BigDecimal.ZERO)>0) {
			break;
		}
		}
		generateTaxHeadEstimate(estimates, shelterFees, BPACalculatorConstants.TAXHEAD_BPA_SHELTER_FEE, Category.FEE);

		// System.out.println("ShelterFee::::::::::::::::" + shelterFee);
		log.info("ShelterFee::::::::::::::::" + shelterFees);
		return shelterFees;

	}
	
	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateShelterFeeForResidentialOccupancy(Map<String, Object> paramMap) {
		BigDecimal shelterFee = BigDecimal.ZERO;
		BigDecimal costOfConstruction = BigDecimal.ZERO;;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		String subOccupancyType = null;
		Double totalEWSArea = null;
		boolean isShelterFeeRequired = false;
		int totalNoOfDwellingUnits = 0;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE)) {
			subOccupancyType = (String) paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.EWS_AREA)) {
			totalEWSArea = (Double) paramMap.get(BPACalculatorConstants.EWS_AREA);
		}
		if (null != paramMap.get(BPACalculatorConstants.SHELTER_FEE)) {
			isShelterFeeRequired = (boolean) paramMap.get(BPACalculatorConstants.SHELTER_FEE);
		}
		if (null != paramMap.get(BPACalculatorConstants.TOTAL_NO_OF_DWELLING_UNITS)) {
			totalNoOfDwellingUnits = (int) paramMap.get(BPACalculatorConstants.TOTAL_NO_OF_DWELLING_UNITS);
		}
		if(null != paramMap.get(BPACalculatorConstants.COST_OF_CONSTRUCTION)) {
			costOfConstruction = new BigDecimal((String)paramMap.get(BPACalculatorConstants.COST_OF_CONSTRUCTION));
		}
		if (isShelterFeeRequired && totalNoOfDwellingUnits > 8) {
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.A) && StringUtils.hasText(subOccupancyType))) {
				if ((StringUtils.hasText(applicationType)
						&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
						&& (StringUtils.hasText(serviceType)
								&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION) || serviceType
										.equalsIgnoreCase(BPACalculatorConstants.ALTERATION_SERVICE_TYPE_FOR_BPA7)))) {

					if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_P))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_S))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_R))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_AB))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_HP))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_WCR))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_SA))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_MIH))) {

						shelterFee = (BigDecimal.valueOf(totalEWSArea).multiply(SQMT_SQFT_MULTIPLIER)
								.multiply(costOfConstruction).multiply(ZERO_TWO_FIVE)).setScale(2, BigDecimal.ROUND_UP);

					}

				}

			}

		}
		return shelterFee;
	}
	
	/**
	 * @param paramMap
	 * @param estimates
	 * @return
	 */
	private BigDecimal calculateTemporaryRetentionFee(Map<String, Object> paramMap,
			ArrayList<TaxHeadEstimate> estimates) {
		BigDecimal retentionFee = BigDecimal.ZERO;
		Double numberOfTemporaryStructures = null;
		String applicationType = null;
		String serviceType = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.NUMBER_OF_TEMP_STRUCTURES)) {
			numberOfTemporaryStructures = (Double) paramMap.get(BPACalculatorConstants.NUMBER_OF_TEMP_STRUCTURES);
		}
		boolean isRetentionFeeApplicable = false;
		if (null != paramMap.get(BPACalculatorConstants.IS_RETENTION_FEE_APPLICABLE)) {
			isRetentionFeeApplicable = (boolean) paramMap.get(BPACalculatorConstants.IS_RETENTION_FEE_APPLICABLE);
		}
		Boolean isOutsideSujogApplExemptionEstimate = isOutsideSujogApplExcemptionEstimate(paramMap, estimates);
		if (isOutsideSujogApplExemptionEstimate && config.getCalculateExemptionForFeeOutsideSujogAppl()) {
			BPA bpa = (BPA) paramMap.get("BPA");
			List<ExemptionFeeDetails> exemptionFeeDetails = getAllExemptionFeesDetails(
					((Map) bpa.getAdditionalDetails()).get(BPACalculatorConstants.BPA_ADD_DETAILS_OUTSIDE_SUJOG_APPL),
					BPACalculatorConstants.TAXHEAD_BPA_TEMP_RETENTION_FEE);
			BigDecimal totalAmountAfterExemption = BigDecimal.ZERO;
			for (ExemptionFeeDetails excmptionFeeDetail : exemptionFeeDetails) {
				try {
					totalAmountAfterExemption = totalAmountAfterExemption.add(excmptionFeeDetail.getTotalAmount());
				} catch (Exception e) {
					e.printStackTrace();
				}
				retentionFee.add(totalAmountAfterExemption);
			}
		} else if ((StringUtils.hasText(applicationType)
				&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
				&& (StringUtils.hasText(serviceType)
						&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION) || serviceType
								.equalsIgnoreCase(BPACalculatorConstants.ALTERATION_SERVICE_TYPE_FOR_BPA7)))
				&& isRetentionFeeApplicable && Objects.nonNull(numberOfTemporaryStructures)) {
			Object mdmsData = paramMap.get("mdmsData");
			String tenantId = String.valueOf(paramMap.get("tenantId"));
			List jsonOutput = JsonPath.read(mdmsData, BPACalculatorConstants.MDMS_RETENTION_FEE_PATH);
			String filterExp = "$.[?(@.ulb == '" + tenantId + "')]";
			List<Map<String, String>> retentionFeeForTenantJson = JsonPath.read(jsonOutput, filterExp);
			if (!CollectionUtils.isEmpty(retentionFeeForTenantJson)) {
				String retentionFeeForTenant = retentionFeeForTenantJson.get(0)
						.get(BPACalculatorConstants.MDMS_RETENTION_FEE);
				retentionFee = new BigDecimal(retentionFeeForTenant).multiply(new BigDecimal(numberOfTemporaryStructures));
			}
		}

		if (retentionFee.compareTo(BigDecimal.ZERO) == -1) {
			log.info("Negative WelfareCess::::::::::::::" + retentionFee);
			retentionFee = BigDecimal.ZERO;
		}
		generateTaxHeadEstimate(estimates, retentionFee, BPACalculatorConstants.TAXHEAD_BPA_TEMP_RETENTION_FEE, Category.FEE);

		// System.out.println("RetentionFee:::::::::::" + retentionFee);
		log.info("RetentionFee:::::::::::" + retentionFee);
		return retentionFee;

	}
	
	/**
	 * @param paramMap
	 * @param estimates
	 * @return Calculated SecurityDeposit
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private BigDecimal calculateSecurityDeposit(Map<String, Object> paramMap, ArrayList<TaxHeadEstimate> estimates) {
		BigDecimal securityDeposits = BigDecimal.ZERO;
		Double totalBuitUpArea = null;
		Double totalexistingBuiltUpArea =null;
		String occupancyType = null;
		boolean isSecurityDepositRequired = false;
		totalBuitUpArea = getProposedAreaParameterForAlteration(paramMap);
		List<Occupancy> occupancyies = new ArrayList<>();
		
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCYLIST)) {
			occupancyies = (List<Occupancy>) paramMap.get(BPACalculatorConstants.OCCUPANCYLIST);
		}
		
		Occupancy subOccupancyWithMaxCoverage = occupancyies.stream()
		        .max((o1, o2) -> Double.compare(o1.getBuiltUpArea(), o2.getBuiltUpArea()))
		        .orElse(null);
		
		for(Occupancy occupancy : occupancyies) {
			BigDecimal securityDeposit = BigDecimal.ZERO;
			totalBuitUpArea = occupancy.getBuiltUpArea();
			
			log.info("Security Deposit: builtup area before public washroom addition: " + totalBuitUpArea);
			
			// Add public Washroom area in Built Up Area if this subOccupanecy has max coverage
			if(occupancy.equals(subOccupancyWithMaxCoverage)) {
				totalBuitUpArea = addBuiltUpAreaForPublicWashroom(totalBuitUpArea, paramMap, occupancy);
				totalBuitUpArea = addBuiltUpAreaForICT(totalBuitUpArea, paramMap, occupancy);
			}
	
			totalexistingBuiltUpArea = occupancy.getExistingBuiltUpArea();
			log.info("totalBuitUpArea inside:"+totalBuitUpArea);
			occupancyType = occupancy.getOccupancyCode();
			paramMap.put(BPACalculatorConstants.OCCUPANCY_TYPE, occupancyType);
			paramMap.put(BPACalculatorConstants.ALTERATION_PROPOSED_BUILTUP_AREA,(totalBuitUpArea-totalexistingBuiltUpArea));
			paramMap.put(BPACalculatorConstants.SUB_OCCUPANCY_TYPE,occupancy.getSubOccupancyCode());
			log.info("occupancy inside:"+occupancyType);
			log.info("suboccupancy inside:"+occupancy.getSubOccupancyCode());
			log.info("Alternation Proposed BuitUpArea :" + (totalBuitUpArea - totalexistingBuiltUpArea));
			
			
			if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
				occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
			}
			if (null != paramMap.get(BPACalculatorConstants.SECURITY_DEPOSIT)) {
				isSecurityDepositRequired = (boolean) paramMap.get(BPACalculatorConstants.SECURITY_DEPOSIT);
			}
			
			Boolean isOutsideSujogApplExemptionEstimate = isOutsideSujogApplExcemptionEstimate(paramMap, estimates);
			if (isOutsideSujogApplExemptionEstimate && config.getCalculateExemptionForFeeOutsideSujogAppl()) {
				BPA bpa = (BPA) paramMap.get("BPA");
				List<ExemptionFeeDetails> exemptionFeeDetails = getAllExemptionFeesDetails(
						((Map) bpa.getAdditionalDetails()).get(BPACalculatorConstants.BPA_ADD_DETAILS_OUTSIDE_SUJOG_APPL),BPACalculatorConstants.TAXHEAD_BPA_SECURITY_DEPOSIT);
				BigDecimal totalAmountAfterExemption = BigDecimal.ZERO;
				for (ExemptionFeeDetails excmptionFeeDetail : exemptionFeeDetails) {
					try {
						totalAmountAfterExemption = totalAmountAfterExemption.add(excmptionFeeDetail.getTotalAmount());
					} catch (Exception e) {
						e.printStackTrace();
					}
					securityDeposits.add(totalAmountAfterExemption);
				}
			} else	if (totalBuitUpArea != null && isSecurityDepositRequired) {
				if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.A))) {
					securityDeposit = calculateSecurityDepositForResidentialOccupancy(paramMap);
				}
				if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.B))) {
					securityDeposit = calculateSecurityDepositForCommercialOccupancy(paramMap);
				}
				if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.C))) {
					securityDeposit = calculateSecurityDepositForPublicSemiPublicInstitutionalOccupancy(paramMap);
				}
				if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.F))) {
					securityDeposit = calculateSecurityDepositForEducationOccupancy(paramMap);
				}
			}
	        log.info("Security Deposit::::::::::::::" + securityDeposit);
            securityDeposits = securityDeposits.add(securityDeposit);
		}
		
		if (securityDeposits.compareTo(BigDecimal.ZERO) == -1) {
			log.info("Negative WelfareCess::::::::::::::" + securityDeposits);
			securityDeposits = BigDecimal.ZERO;
		}
		generateTaxHeadEstimate(estimates, securityDeposits, BPACalculatorConstants.TAXHEAD_BPA_SECURITY_DEPOSIT, Category.FEE);
		log.info("Total Security Deposit::::::::::::::" + securityDeposits);

		return securityDeposits;

	}
	
	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateSecurityDepositForResidentialOccupancy(Map<String, Object> paramMap) {
		BigDecimal securityDeposit = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		String subOccupancyType = null;
		Double totalBuitUpArea = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE)) {
			subOccupancyType = (String) paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE);
		}
		totalBuitUpArea = getProposedAreaParameterForAlteration(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.A) && StringUtils.hasText(subOccupancyType))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION) || serviceType
									.equalsIgnoreCase(BPACalculatorConstants.ALTERATION_SERVICE_TYPE_FOR_BPA7)))) {

				if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_AB))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_HP))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_WCR))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_SA))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_E))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_LIH))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_MIH))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_SQ))) {

					// securityDeposit = calculateConstantFee(paramMap, 100);
					securityDeposit = calculateConstantFeeNew(totalBuitUpArea, 100);

				}

			}

		}
		return securityDeposit;
	}
	
	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateSecurityDepositForCommercialOccupancy(Map<String, Object> paramMap) {
		BigDecimal securityDeposit = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		String subOccupancyType = null;
		Double totalBuitUpArea = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE)) {
			subOccupancyType = (String) paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE);
		}
		totalBuitUpArea = getProposedAreaParameterForAlteration(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.B) && StringUtils.hasText(subOccupancyType))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION) || serviceType
									.equalsIgnoreCase(BPACalculatorConstants.ALTERATION_SERVICE_TYPE_FOR_BPA7)))) {
				if (null != totalBuitUpArea && totalBuitUpArea >= 200) {
					// securityDeposit = calculateConstantFee(paramMap, 100);
					securityDeposit = calculateConstantFeeNew(totalBuitUpArea, 100);

				}
			}
		}
		return securityDeposit;
	}
	
	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateSecurityDepositForPublicSemiPublicInstitutionalOccupancy(Map<String, Object> paramMap) {
		BigDecimal securityDeposit = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		String subOccupancyType = null;
		Double totalBuitUpArea = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE)) {
			subOccupancyType = (String) paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE);
		}
		totalBuitUpArea = getProposedAreaParameterForAlteration(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.C) && StringUtils.hasText(subOccupancyType))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION) || serviceType
									.equalsIgnoreCase(BPACalculatorConstants.ALTERATION_SERVICE_TYPE_FOR_BPA7)))) {
				if (null != totalBuitUpArea && totalBuitUpArea >= 200) {
					// securityDeposit = calculateConstantFee(paramMap, 100);
					securityDeposit = calculateConstantFeeNew(totalBuitUpArea, 100);

				}
			}
		}
		return securityDeposit;
	}
	
	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateSecurityDepositForEducationOccupancy(Map<String, Object> paramMap) {
		BigDecimal securityDeposit = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		String subOccupancyType = null;
		Double totalBuitUpArea = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE)) {
			subOccupancyType = (String) paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE);
		}
		totalBuitUpArea = getProposedAreaParameterForAlteration(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.F) && StringUtils.hasText(subOccupancyType))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION) || serviceType
									.equalsIgnoreCase(BPACalculatorConstants.ALTERATION_SERVICE_TYPE_FOR_BPA7)))) {
				if (null != totalBuitUpArea && totalBuitUpArea >= 200) {
					// securityDeposit = calculateConstantFee(paramMap, 100);
					securityDeposit = calculateConstantFeeNew(totalBuitUpArea, 100);
				}
			}
		}
		return securityDeposit;
	}
	
	/**
	 * @param paramMap
	 * @param estimates
	 * @return
	 */
	private BigDecimal calculatePurchasableFAR(Map<String, Object> paramMap, ArrayList<TaxHeadEstimate> estimates) {
		BigDecimal purchasableFARFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		Double benchmarkValuePerAcre = null;
		Double baseFar = null;
		Double providedFar = null;
		Double maxPermissibleFar = null;
		Double tdrFarRelaxation = null;
		Double plotArea = null;
		Double proposedArea = null;
		Double existingArea = null;
		String subOccupancyType = null;
		String alterationSubservice = null;
		Double additionalTdr = null;
		Boolean isLayoutApproved = Boolean.FALSE;
		String riskType = null;
        List<Occupancy> occupancyies = new ArrayList<>();
		
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCYLIST)) {
			occupancyies = (List<Occupancy>) paramMap.get(BPACalculatorConstants.OCCUPANCYLIST);
		}
		Set<String>  occCode = occupancyies.stream().filter(o->o.getSubOccupancyCode()!=null).map(Occupancy::getSubOccupancyCode).collect(Collectors.toSet());
		String code = occCode.stream().filter(occ->occ.equalsIgnoreCase(BPACalculatorConstants.A_MIH)).findAny().orElse(null);
		if (code!=null) {
			paramMap.put(BPACalculatorConstants.SUB_OCCUPANCY_TYPE,code);
		}else {
			code =occCode.stream().findFirst().get();
			if(code!=null)
			paramMap.put(BPACalculatorConstants.SUB_OCCUPANCY_TYPE,code);
		}
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.BMV_ACRE)) {
			benchmarkValuePerAcre = (Double) paramMap.get(BPACalculatorConstants.BMV_ACRE);
		}
		if (null != paramMap.get(BPACalculatorConstants.BASE_FAR)) {
			baseFar = (Double) paramMap.get(BPACalculatorConstants.BASE_FAR);
		}
		if (null != paramMap.get(BPACalculatorConstants.PROVIDED_FAR)) {
			providedFar = (Double) paramMap.get(BPACalculatorConstants.PROVIDED_FAR);
		}
		if (null != paramMap.get(BPACalculatorConstants.PLOT_AREA)) {
			plotArea = (Double) paramMap.get(BPACalculatorConstants.PLOT_AREA);
		}
		if (null != paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE)) {
			subOccupancyType = (String) paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.PERMISSABLE_FAR)) {
			maxPermissibleFar = (Double) paramMap.get(BPACalculatorConstants.PERMISSABLE_FAR);
		}
		if (null != paramMap.get(BPACalculatorConstants.TDR_FAR_RELAXATION)) {
			tdrFarRelaxation = (Double) paramMap.get(BPACalculatorConstants.TDR_FAR_RELAXATION);
		}		
		if (Objects.nonNull(paramMap.get(BPACalculatorConstants.BPA_ADD_DETAILS_SUBSERVICE_KEY))) {
			alterationSubservice = (String) paramMap.get(BPACalculatorConstants.BPA_ADD_DETAILS_SUBSERVICE_KEY);
		}
		if (Objects.nonNull(paramMap.get(BPACalculatorConstants.ADDITIONAL_TDR))) {
			additionalTdr = (Double) paramMap.get(BPACalculatorConstants.ADDITIONAL_TDR);
		}
		if (null != paramMap.get(BPACalculatorConstants.IS_LAYOUT_APPROVED_UNDER_SEC_16)) {
			isLayoutApproved = (Boolean) paramMap.get(BPACalculatorConstants.IS_LAYOUT_APPROVED_UNDER_SEC_16);
		}
		if (null != paramMap.get(BPACalculatorConstants.RISK_TYPE)) {
			riskType = (String) paramMap.get(BPACalculatorConstants.RISK_TYPE);
		}
		
		if(riskType.equalsIgnoreCase(BPACalculatorConstants.RISK_TYPE_LOW)) {
			log.info("Risk Type Low, pur far fee 0 ...");
			purchasableFARFee = BigDecimal.ZERO;
			return purchasableFARFee;
		}
		Double cfar = Double.valueOf("0");
		Boolean isOutsideSujogApplExemptionEstimate = isOutsideSujogApplExcemptionEstimate(paramMap, estimates);
		if (isOutsideSujogApplExemptionEstimate && config.getCalculateExemptionForFeeOutsideSujogAppl()) {
			BPA bpa = (BPA) paramMap.get("BPA");
			List<ExemptionFeeDetails> exemptionFeeDetails = getAllExemptionFeesDetails(
					((Map) bpa.getAdditionalDetails()).get(BPACalculatorConstants.BPA_ADD_DETAILS_OUTSIDE_SUJOG_APPL),
					BPACalculatorConstants.TAXHEAD_BPA_PURCHASABLE_FAR);
			BigDecimal totalAmountAfterExemption = BigDecimal.ZERO;
			for (ExemptionFeeDetails excmptionFeeDetail : exemptionFeeDetails) {
				try {
					totalAmountAfterExemption = totalAmountAfterExemption.add(excmptionFeeDetail.getTotalAmount());
				} catch (Exception e) {
					e.printStackTrace();
				}
				purchasableFARFee.add(totalAmountAfterExemption);
			}
		} //calculation for all cases other than MIG sub-occupancy-
		else if ((null != providedFar) && (null != baseFar) && (providedFar > baseFar) && (null != plotArea)) { 
		      
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION) || serviceType
									.equalsIgnoreCase(BPACalculatorConstants.ALTERATION_SERVICE_TYPE_FOR_BPA7)))) {

				BigDecimal benchmarkValuePerSQM = BigDecimal.valueOf(benchmarkValuePerAcre).divide(ACRE_SQMT_MULTIPLIER,
						2, BigDecimal.ROUND_UP);

				BigDecimal purchasableFARRate = (benchmarkValuePerSQM.multiply(ZERO_TWO_FIVE)).setScale(2,
						BigDecimal.ROUND_UP);

				if(BPACalculatorConstants.ALTERATION_SUBSERVICE_B.equals(alterationSubservice)) {
					if (null != paramMap.get(BPACalculatorConstants.TOTAL_PROPOSED_FLOOR_AREA_EDCR)) {
						proposedArea = (Double) paramMap.get(BPACalculatorConstants.TOTAL_PROPOSED_FLOOR_AREA_EDCR);
					}
					if (null != paramMap.get(BPACalculatorConstants.TOTAL_EXISTING_FLOOR_AREA_EDCR)) {
						existingArea = (Double) paramMap.get(BPACalculatorConstants.TOTAL_EXISTING_FLOOR_AREA_EDCR);
					}
				}else {
					if (null != paramMap.get(BPACalculatorConstants.ALTERATION_PROPOSED_FLOOR_AREA)) {
						proposedArea = (Double) paramMap.get(BPACalculatorConstants.ALTERATION_PROPOSED_FLOOR_AREA);
					}
					if (null != paramMap.get(BPACalculatorConstants.ALTERATION_EXISTING_FLOOR_AREA)) {
						existingArea = (Double) paramMap.get(BPACalculatorConstants.ALTERATION_EXISTING_FLOOR_AREA);
					}
				}

//				// Purchasable FAR Calculation for Alteration- D 
//				if (!StringUtils.isEmpty(alterationSubservice)
//						&& alterationSubservice.equalsIgnoreCase(BPACalculatorConstants.ALTERATION_SUBSERVICE_D)) {
//					if (!ObjectUtils.isEmpty(additionalTdr)) {
//						proposedArea = proposedArea - additionalTdr;
//					}
//					purchasableFARFee = (purchasableFARRate.multiply(BigDecimal.valueOf(proposedArea))).setScale(2, BigDecimal.ROUND_UP);
//				} else {
				BigDecimal proposedFAR = BigDecimal.ZERO;
				BigDecimal existingFAR = BigDecimal.ZERO;
				log.info("proposedFAR - "+proposedArea +", plotArea -"+plotArea+", existingArea - "+existingArea);
				if(proposedArea !=null && plotArea !=null) {
					proposedFAR = BigDecimal.valueOf(proposedArea).divide(BigDecimal.valueOf(plotArea), 5, RoundingMode.HALF_UP);	
				}
				if(existingArea !=null && plotArea !=null) {
					existingFAR = BigDecimal.valueOf(existingArea).divide(BigDecimal.valueOf(plotArea), 5, RoundingMode.HALF_UP);
				}
				
				Double deltaFAR = baseFar-existingFAR.doubleValue();
				
				if (deltaFAR > 0) {
					cfar = proposedFAR.doubleValue() - deltaFAR;
				} else {
					cfar = proposedFAR.doubleValue();
				}
				// tdr relaxation- decrease deltaFar based on tdrFarRelaxation-
				if (null != tdrFarRelaxation) {
					cfar = cfar - tdrFarRelaxation.doubleValue();
				}
				
				log.info("Is Layout approved : " + isLayoutApproved  + " max far:"+ maxPermissibleFar);
				if (isLayoutApproved && maxPermissibleFar >= 3.00) {
					cfar = cfar - 1.0;
					log.info("applicable discount after Layout Approval applicable: " + cfar);
				} else {
					// calculation for MIG sub-occupancy-
					if (BPACalculatorConstants.A_MIH.equalsIgnoreCase(subOccupancyType)) {
						BigDecimal applicableDiscountFar = (BigDecimal.valueOf(maxPermissibleFar)
								.subtract(BigDecimal.valueOf(baseFar)).multiply(new BigDecimal("0.25")))
								.setScale(2, BigDecimal.ROUND_UP);
						cfar = cfar - applicableDiscountFar.doubleValue();
					}
				}
				if (cfar < 0) {
					cfar = Double.valueOf("0");
				}
                
				if (subOccupancyType != null && subOccupancyType.contains("E-")) {
					log.info("Sub Occupancy Type : " + subOccupancyType);
					purchasableFARFee = BigDecimal.ZERO;
				} else {
					purchasableFARFee = (purchasableFARRate.multiply(BigDecimal.valueOf(cfar))
							.multiply(BigDecimal.valueOf(plotArea))).setScale(2, BigDecimal.ROUND_UP);
				}
			}

		}
	
		if (purchasableFARFee.compareTo(BigDecimal.ZERO) == -1) {
			log.info("Negative PurchasableFARFee::::::::::::::" + purchasableFARFee);
			purchasableFARFee = BigDecimal.ZERO;
		}
		generateTaxHeadEstimate(estimates, purchasableFARFee, BPACalculatorConstants.TAXHEAD_BPA_PURCHASABLE_FAR, Category.FEE);
		
		log.info("PurchasableFARFee:::::::::::::::::" + purchasableFARFee);
		return purchasableFARFee;

	}
	
	private BigDecimal calculateEIDPFee(Map<String, Object> paramMap, ArrayList<TaxHeadEstimate> estimates) {
		BigDecimal eidpFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		Double projectValue = null;
		Double alterationTotalBuiltupAreaProposed = null;
		Double alterationTotalBuiltupArea = null;
		Double totalexistinguiltuparea = 0.0;
		Double totalBuiltup = 0.0;
		BigDecimal builtupAreaApproved = BigDecimal.ZERO;
		Double proposedBuiltUpArea = 0.0;
		Boolean isEidpFeeExemptedForTheProject = Boolean.FALSE;
		
		List<Occupancy> occupancyies = new ArrayList<>();
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCYLIST)) {
			occupancyies = (List<Occupancy>) paramMap.get(BPACalculatorConstants.OCCUPANCYLIST);
		}
		
		String subservice = null;
		if (Objects.nonNull(paramMap.get(BPACalculatorConstants.BPA_ADD_DETAILS_SUBSERVICE_KEY))) {
			subservice = (String) paramMap.get(BPACalculatorConstants.BPA_ADD_DETAILS_SUBSERVICE_KEY);
		}
		
		Map<Integer, BigDecimal> colorcodeVsApprovedAreaMap = new HashMap<>();
		if (null != paramMap.get(BPACalculatorConstants.APPROVED_CONSTRUCTION_OCCUPANCYWISE)) {
			colorcodeVsApprovedAreaMap = (Map<Integer, BigDecimal>) paramMap
					.get(BPACalculatorConstants.APPROVED_CONSTRUCTION_OCCUPANCYWISE);
		}
		
		if (null != paramMap.get(BPACalculatorConstants.IS_EIDP_EXEMPTED_FOR_THE_PROJECT)) {
			isEidpFeeExemptedForTheProject = (Boolean) paramMap
					.get(BPACalculatorConstants.IS_EIDP_EXEMPTED_FOR_THE_PROJECT);
		}
		
		for(Occupancy occupancy : occupancyies) {
			
			totalexistinguiltuparea += occupancy.getExistingBuiltUpArea();
			log.info("totalexistinguiltuparea inside:"+totalexistinguiltuparea);
			totalBuiltup += occupancy.getBuiltUpArea();
			log.info("totalBuitUpArea inside:"+totalBuiltup);
			
			if(!ObjectUtils.isEmpty(occupancy.getTypeHelper()) && !ObjectUtils.isEmpty(occupancy.getTypeHelper().getOccupancySubType())
					&& !ObjectUtils.isEmpty(occupancy.getTypeHelper().getOccupancySubType().getColor())) {
				String colorcodeString = occupancy.getTypeHelper().getOccupancySubType().getColor();
				log.info("colorcodeString : " + colorcodeString);
				Integer colorcode = Integer.parseInt(colorcodeString);
	
				if (colorcodeVsApprovedAreaMap.containsKey(colorcode)) {
					builtupAreaApproved = builtupAreaApproved.add(colorcodeVsApprovedAreaMap.get(colorcode));
					log.info("builtupAreaApproved : " + builtupAreaApproved);
				}
			}
		}
		proposedBuiltUpArea = totalBuiltup - totalexistinguiltuparea;
		// Substract delta area from previous construction for ADDITION_ALTERATION_D service
		if (Objects.nonNull(subservice)
				&& subservice.equalsIgnoreCase(BPACalculatorConstants.ALTERATION_SUBSERVICE_D)) {
			if (builtupAreaApproved.compareTo(BigDecimal.ZERO) > 0) {
				BigDecimal deltaBuiltupAreaFromPreviousConstruction = builtupAreaApproved
						.subtract(BigDecimal.valueOf(totalexistinguiltuparea));
				log.info("deltaBuiltupAreaFromPreviousConstruction : " + deltaBuiltupAreaFromPreviousConstruction);
				if (deltaBuiltupAreaFromPreviousConstruction.compareTo(BigDecimal.ZERO) > 0) {
					proposedBuiltUpArea = proposedBuiltUpArea - deltaBuiltupAreaFromPreviousConstruction.doubleValue();
					log.info("proposedBuiltUpArea after calculation : " + proposedBuiltUpArea);
				}
			}
		}
		paramMap.put(BPACalculatorConstants.ALTERATION_PROPOSED_BUILTUP_AREA, proposedBuiltUpArea);
		paramMap.put(BPACalculatorConstants.ALTERATION_TOTAL_BUILTUP_AREA,totalBuiltup);
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.PROJECT_VALUE_FOR_EIDP)) {
			projectValue = (Double) paramMap.get(BPACalculatorConstants.PROJECT_VALUE_FOR_EIDP);
		}
		alterationTotalBuiltupAreaProposed = getProposedAreaParameterForAlteration(paramMap);
		alterationTotalBuiltupArea = getTotalAreaParameterForAlteration(paramMap);
		Boolean isOutsideSujogApplExemptionEstimate = isOutsideSujogApplExcemptionEstimate(paramMap, estimates);
		if (isOutsideSujogApplExemptionEstimate && config.getCalculateExemptionForFeeOutsideSujogAppl()) {
			BPA bpa = (BPA) paramMap.get("BPA");
			List<ExemptionFeeDetails> exemptionFeeDetails = getAllExemptionFeesDetails(
					((Map) bpa.getAdditionalDetails()).get(BPACalculatorConstants.BPA_ADD_DETAILS_OUTSIDE_SUJOG_APPL),
					BPACalculatorConstants.TAXHEAD_BPA_EIDP_FEE);
			BigDecimal totalAmountAfterExemption = BigDecimal.ZERO;
			for (ExemptionFeeDetails excmptionFeeDetail : exemptionFeeDetails) {
				try {
					totalAmountAfterExemption = totalAmountAfterExemption.add(excmptionFeeDetail.getTotalAmount());
				} catch (Exception e) {
					e.printStackTrace();
				}
				eidpFee.add(totalAmountAfterExemption);
			}
		}else if ((StringUtils.hasText(applicationType)
				&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
				&& (StringUtils.hasText(serviceType)
				&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION) || serviceType
						.equalsIgnoreCase(BPACalculatorConstants.ALTERATION_SERVICE_TYPE_FOR_BPA7)))
				&& projectValue != null) {
			
			// If EIDP fee is exempted variable is true(Govt plot), then EIDP fee is not calculated
			if (!isEidpFeeExemptedForTheProject) {
				eidpFee = BigDecimal.valueOf(projectValue)
						.multiply(BigDecimal.valueOf(alterationTotalBuiltupAreaProposed))
						.divide(BigDecimal.valueOf(alterationTotalBuiltupArea), 2, RoundingMode.HALF_UP).divide(HUNDRED)
						.setScale(2, BigDecimal.ROUND_HALF_UP);
			}
		}
		
		
		if (eidpFee.compareTo(BigDecimal.ZERO) == -1) {
			log.info("Negative eidpFee::::::::::::::" + eidpFee);
			eidpFee = BigDecimal.ZERO;
		}
		generateTaxHeadEstimate(estimates, eidpFee, BPACalculatorConstants.TAXHEAD_BPA_EIDP_FEE, Category.FEE);
		
		log.info("EIDP Fee:::::::::::::::::" + eidpFee);
		return eidpFee;
	}
	
	private void generateTaxHeadEstimate(ArrayList<TaxHeadEstimate> estimates, BigDecimal feeAmount, String taxHeadCode,
			Category category) {
		TaxHeadEstimate estimate = new TaxHeadEstimate();
		estimate.setEstimateAmount(feeAmount.setScale(0, BigDecimal.ROUND_UP));
		estimate.setCategory(category);
		estimate.setTaxHeadCode(taxHeadCode);
		estimates.add(estimate);
	}
	
	private Double getProposedAreaParameterForAlteration(Map<String, Object> paramMap) {
		String applicableAreaParameterName = BPACalculatorConstants.ALTERATION_PROPOSED_BUILTUP_AREA;
		// TODO should return null or 0?
		return null != paramMap.get(applicableAreaParameterName) ? (Double) paramMap.get(applicableAreaParameterName)
				: Double.valueOf(0);
	}
	
	private Double getTotalAreaParameterForAlteration(Map<String, Object> paramMap) {
		String applicableAreaParameterName = BPACalculatorConstants.ALTERATION_TOTAL_BUILTUP_AREA;
		// TODO should return null or 0?
		return null != paramMap.get(applicableAreaParameterName) ? (Double) paramMap.get(applicableAreaParameterName)
				: Double.valueOf(0);
	}
	
	private BigDecimal calculateAdjustmentAmount(Map<String, Object> paramMap, ArrayList<TaxHeadEstimate> estimates) {
		BigDecimal adjustmentAmount = BigDecimal.ZERO;
		BPA bpa = null;
		if (null != paramMap.get("BPA")) {
			bpa = (BPA) paramMap.get("BPA");
		}
		Boolean isOutsideSujogApplExemptionEstimate = isOutsideSujogApplExcemptionEstimate(paramMap, estimates);
		if (isOutsideSujogApplExemptionEstimate && config.getCalculateExemptionForFeeOutsideSujogAppl()) {
			List<ExemptionFeeDetails> exemptionFeeDetails = getAllExemptionFeesDetails(
					((Map) bpa.getAdditionalDetails()).get(BPACalculatorConstants.BPA_ADD_DETAILS_OUTSIDE_SUJOG_APPL),
					BPACalculatorConstants.TAXHEAD_BPA_ADJUSTMENT_AMOUNT);
			BigDecimal totalAmountAfterExemption = BigDecimal.ZERO;
			for (ExemptionFeeDetails excmptionFeeDetail : exemptionFeeDetails) {
				try {
					totalAmountAfterExemption = totalAmountAfterExemption.add(excmptionFeeDetail.getTotalAmount());
				} catch (Exception e) {
					e.printStackTrace();
				}
				adjustmentAmount.add(totalAmountAfterExemption);
			}
		}else if (Objects.nonNull(bpa) && Objects.nonNull(bpa.getAdditionalDetails())
				&& !StringUtils.isEmpty(((Map) bpa.getAdditionalDetails())
						.get(BPACalculatorConstants.BPA_ADD_DETAILS_SANCTION_FEE_ADJUSTMENT_AMOUNT_KEY))) {
			adjustmentAmount = new BigDecimal(((Map) bpa.getAdditionalDetails())
					.get(BPACalculatorConstants.BPA_ADD_DETAILS_SANCTION_FEE_ADJUSTMENT_AMOUNT_KEY).toString());
		}
		if (adjustmentAmount.compareTo(BigDecimal.ZERO) == -1) {
			log.info("Negative PurchasableFARFee::::::::::::::" + adjustmentAmount);
			adjustmentAmount = BigDecimal.ZERO;
		}
		generateTaxHeadEstimate(estimates, adjustmentAmount, BPACalculatorConstants.TAXHEAD_BPA_ADJUSTMENT_AMOUNT,
				Category.FEE);
		log.info("Adjustment Amount:::::::::::::::::" + adjustmentAmount);
		return adjustmentAmount;
	}
	
	@SuppressWarnings("rawtypes")
	private BigDecimal calculateAllOtherFees(Map<String, Object> paramMap, ArrayList<TaxHeadEstimate> estimates) {
		BigDecimal adjustmentAmount = BigDecimal.ZERO;
		BigDecimal otherFeeAmount = BigDecimal.ZERO;
		BPA bpa = null;
		if (null != paramMap.get("BPA")) {
			bpa = (BPA) paramMap.get("BPA");
		}
		
		Boolean isOutsideSujogApplExemptionEstimate = isOutsideSujogApplExcemptionEstimate(paramMap, estimates);
		if (isOutsideSujogApplExemptionEstimate && config.getCalculateExemptionForFeeOutsideSujogAppl()) {
			List<ExemptionFeeDetails> exemptionFeeDetails = getAllExemptionFeesDetails(
					((Map) bpa.getAdditionalDetails()).get(BPACalculatorConstants.BPA_ADD_DETAILS_OUTSIDE_SUJOG_APPL),
					BPACalculatorConstants.BPA_ADD_DETAILS_OTHER_FEES_KEY);
			BigDecimal totalAmountAfterExemption = BigDecimal.ZERO;
			for (ExemptionFeeDetails excmptionFeeDetail : exemptionFeeDetails) {
				try {
					totalAmountAfterExemption = totalAmountAfterExemption.add(excmptionFeeDetail.getTotalAmount());
				} catch (Exception e) {
					e.printStackTrace();
				}
				otherFeeAmount.add(totalAmountAfterExemption);
			}
		} else	if (Objects.nonNull(bpa) && Objects.nonNull(bpa.getAdditionalDetails())
				&& !StringUtils.isEmpty(((Map) bpa.getAdditionalDetails()).get(BPACalculatorConstants.BPA_ADD_DETAILS_OTHER_FEES_KEY))) {
			List<OtherFeeDetails> allOtherFeesDetails = getAllOtherFeesDetails(((Map)bpa.getAdditionalDetails()).get(BPACalculatorConstants.BPA_ADD_DETAILS_OTHER_FEES_KEY));
			
			for(OtherFeeDetails otherFeeDetails : allOtherFeesDetails) {
				try {
					otherFeeAmount = new BigDecimal(otherFeeDetails.getAmount().toString());
				} catch(Exception e) {e.printStackTrace();}
				
				switch (otherFeeDetails.getOrder()) {
				case 1:
					
					adjustmentAmount = adjustmentAmount.add(otherFeeAmount);
					generateTaxHeadEstimate(estimates, otherFeeAmount, BPACalculatorConstants.TAXHEAD_BPA_ADJUSTMENT_AMOUNT_1, Category.FEE);
					break;

				case 2:
					adjustmentAmount = adjustmentAmount.add(otherFeeAmount);
					generateTaxHeadEstimate(estimates, otherFeeAmount, BPACalculatorConstants.TAXHEAD_BPA_ADJUSTMENT_AMOUNT_2, Category.FEE);
					break;

				case 3:
					adjustmentAmount = adjustmentAmount.add(otherFeeAmount);
					generateTaxHeadEstimate(estimates, otherFeeAmount, BPACalculatorConstants.TAXHEAD_BPA_ADJUSTMENT_AMOUNT_3, Category.FEE);
					break;
					
				case 4:
					adjustmentAmount = adjustmentAmount.add(otherFeeAmount);
					generateTaxHeadEstimate(estimates, otherFeeAmount, BPACalculatorConstants.TAXHEAD_BPA_ADJUSTMENT_AMOUNT_4, Category.FEE);
					break;
					
				case 5:
					adjustmentAmount = adjustmentAmount.add(otherFeeAmount);
					generateTaxHeadEstimate(estimates, otherFeeAmount, BPACalculatorConstants.TAXHEAD_BPA_ADJUSTMENT_AMOUNT_5, Category.FEE);
					break;

				default:
					throw new CustomException("INVALID_OTHER_FEE_DETAILS", "Kindly check the other Fee details !!!");
				}
			}
		}
		
		log.info("Adjustment Amount :::::::::::::::::" + adjustmentAmount);
		return adjustmentAmount;
	}
	
	
	private List<OtherFeeDetails> getAllOtherFeesDetails(Object object) {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		try {
			String jsonString = mapper.writeValueAsString(object);
			List<OtherFeeDetails> allOtherFeeDetails = mapper.readValue(jsonString, new TypeReference<List<OtherFeeDetails>>(){});
			
			allOtherFeeDetails = allOtherFeeDetails.stream()
					.filter(otherFee -> !ObjectUtils.isEmpty(otherFee.getAmount()))
					.collect(Collectors.toList());
			
			return allOtherFeeDetails;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new ArrayList<>();
		
	}
	
	private Boolean isOutsideSujogApplExcemptionEstimate(Map<String, Object> paramMap,
			ArrayList<TaxHeadEstimate> estimates) {
		Boolean isOutsideSujogAppl = Boolean.FALSE;
		Boolean isRefApplicationPresentInSujog = Boolean.FALSE;
		BPA bpa = null;
		if (null != paramMap.get("BPA")) {
			bpa = (BPA) paramMap.get("BPA");
		}

		if (Objects.nonNull(paramMap.get(BPACalculatorConstants.IS_REF_APPLICATION_PRESENT_IN_SUJOG))) {
			isRefApplicationPresentInSujog = (Boolean) paramMap
					.get(BPACalculatorConstants.IS_REF_APPLICATION_PRESENT_IN_SUJOG);
		}

		if (!isRefApplicationPresentInSujog) {
			if (Objects.nonNull(bpa) && Objects.nonNull(bpa.getAdditionalDetails()) && !StringUtils.isEmpty(
					((Map) bpa.getAdditionalDetails()).get(BPACalculatorConstants.BPA_ADD_DETAILS_OUTSIDE_SUJOG_APPL))) {
				isOutsideSujogAppl = Boolean.TRUE;
			}

		}
		return isOutsideSujogAppl;
	}
	
	
	private List<ExemptionFeeDetails> getAllExemptionFeesDetails(Object object, String taxHead) {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		try {
			String jsonString = mapper.writeValueAsString(object);
			List<ExemptionFeeDetails> allExemptionFeeDetails = mapper.readValue(jsonString, new TypeReference<List<ExemptionFeeDetails>>(){});
			
			allExemptionFeeDetails = allExemptionFeeDetails.stream()
					.filter(exemptionFee -> exemptionFee.getFeeType().equalsIgnoreCase(taxHead))
					.collect(Collectors.toList());
			
			return allExemptionFeeDetails;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new ArrayList<>();
	}

	private Double addBuiltUpAreaForPublicWashroom(Double builtUpArea, Map<String, Object> paramMap,
			Occupancy occupancy) {

		if (paramMap.containsKey(BPACalculatorConstants.IS_PUBLIC_WASHROOM_PRESENT)) {

			Double publicWashroomBuiltUpArea = (Double) paramMap
					.get(BPACalculatorConstants.PUBLIC_WASHROOM_BUILTUP_AREA);
			log.info("public washroom area added in subOccupancy.."
					+ occupancy.getTypeHelper().getOccupancySubType().getName());
			builtUpArea = builtUpArea + publicWashroomBuiltUpArea;

		}
		return builtUpArea;
	}
	
	private Double addBuiltUpAreaForICT(Double builtUpArea, Map<String, Object> paramMap,
			Occupancy occupancy) {

		if (paramMap.containsKey(BPACalculatorConstants.IS_ICT_PRESENT)) {

			Double ictBuiltUpArea = (Double) paramMap
					.get(BPACalculatorConstants.ICT_BUILTUP_AREA);
			log.info("ict area added in subOccupancy.."
					+ occupancy.getTypeHelper().getOccupancySubType().getName());
			builtUpArea = builtUpArea + ictBuiltUpArea;

		}
		return builtUpArea;
	}
}
