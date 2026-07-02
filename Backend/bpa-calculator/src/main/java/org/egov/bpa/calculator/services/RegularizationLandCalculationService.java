package org.egov.bpa.calculator.services;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import org.apache.commons.lang3.ObjectUtils;
import org.egov.bpa.calculator.utils.RegularizationConstants;
import org.egov.bpa.calculator.utils.RegularizationUtils;
import org.egov.bpa.calculator.web.models.demand.Category;
import org.egov.bpa.calculator.web.models.demand.TaxHeadEstimate;
import org.egov.bpa.calculator.web.models.regularization.LandRegularizationInfo;
import org.egov.bpa.calculator.web.models.regularization.PlotInfo;
import org.egov.bpa.calculator.web.models.regularization.RegularizationCalculationCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RegularizationLandCalculationService {
	
	@Autowired
	private RegularizationUtils utils;
	
	
	/**
	 * Calculate fee and Estimates here for Land Regularization
	 * 
	 * @param criteria
	 * @param estimates
	 * @param paramMap 
	 */
	public void calculateTotalFeeForLand(@Valid RegularizationCalculationCriteria criteria, List<TaxHeadEstimate> estimates, Map<String, Object> paramMap) {

		if (StringUtils.hasText(criteria.getFeeType()) && criteria.getFeeType().equalsIgnoreCase(RegularizationConstants.APPLICATION_FEE)) {
			BigDecimal landDevFee = calculateLandDevelopmentFee(criteria, estimates, paramMap);
			log.info("Total App FEE For:::" + criteria.getApplicationNo() + " is " + landDevFee);
		} else if (StringUtils.hasText(criteria.getFeeType()) && criteria.getFeeType().equalsIgnoreCase(RegularizationConstants.SANCTION_FEE)) {
			BigDecimal compoundingFee = calculateCompoundingFee(criteria, estimates);
			log.info("Total Sanc FEE For:::" + criteria.getApplicationNo() + " is " + compoundingFee);
		}

	}

	
	/**
	 * Calculate Land Development fee for Regularization applications here
	 * @param paramMap 
	 * 
	 * @param totalCompoundingFee
	 * @param plots
	 * @return Calculated Land Development Fee
	 */
	public BigDecimal calculateLandDevelopmentFee(RegularizationCalculationCriteria criteria, List<TaxHeadEstimate> estimates, Map<String, Object> paramMap) {
		BigDecimal totalLandDevelopmentFee = BigDecimal.ZERO;
		BigDecimal landDevelopmentPaid = BigDecimal.ZERO;
		String riskType = null;
		Boolean isSparit = null;
		Boolean isReworkAppliation = false;
		
		if (ObjectUtils.isNotEmpty(paramMap.get(RegularizationConstants.RISK_TYPE_KEY))) {
			riskType = (String) paramMap.get(RegularizationConstants.RISK_TYPE_KEY);
		}
		
		if (ObjectUtils.isNotEmpty(paramMap.get(RegularizationConstants.IS_SPARIT_KEY))) {
			isSparit = (Boolean) paramMap.get(RegularizationConstants.IS_SPARIT_KEY);
		}
		
		if (ObjectUtils.isNotEmpty(paramMap.get(RegularizationConstants.LAND_DEV_FEE_PAID_KEY))) {
			landDevelopmentPaid = (BigDecimal)paramMap.get(RegularizationConstants.LAND_DEV_FEE_PAID_KEY);
		}
		
		if (ObjectUtils.isNotEmpty(paramMap.get(RegularizationConstants.IS_REWORK_APP_KEY))) {
			isReworkAppliation = (Boolean) paramMap.get(RegularizationConstants.IS_REWORK_APP_KEY);
		}
		
		
		if(landDevelopmentPaid.compareTo(BigDecimal.ZERO) > 0 && !isReworkAppliation) {
			totalLandDevelopmentFee = landDevelopmentPaid;
			
		} else {
			if(!RegularizationConstants.RISK_TYPE_LOW.equalsIgnoreCase(riskType)) {
				BigDecimal totalPlotArea = utils.getNetPlotArea(criteria.getRegularization().getLandRegularizationInfo().getPlotInfo());
				log.info("Total Plot area on which land Development fee calculated :::" + totalPlotArea);
				if(isSparit) {
					totalLandDevelopmentFee = utils.multiplyWithRoundUp(totalPlotArea, RegularizationConstants.SPARIT_RATE_FOR_LAND_DEV_FEE);
				} else {
					totalLandDevelopmentFee = utils.multiplyWithRoundUp(totalPlotArea, RegularizationConstants.RATE_FOR_LAND_DEV_FEE);
				}
			}
		}
		
		utils.generateTaxHeadEstimate(estimates, totalLandDevelopmentFee, RegularizationConstants.TAXHEAD_REG_LAND_DEVELOPMENT_FEE, Category.FEE);
		log.info("Application Number :::" + criteria.getApplicationNo());
		log.info("Total Land Development FEE :::" + totalLandDevelopmentFee);
		return totalLandDevelopmentFee;
	}
	
	

	/**
	 * Calculate fee for Compounding fee tax head 
	 * 
	 * @param criteria
	 * @param estimates
	 * @return Calculated Compounding Fee
	 */
	public BigDecimal calculateCompoundingFee(@Valid RegularizationCalculationCriteria criteria,
			List<TaxHeadEstimate> estimates) {

		BigDecimal totalCompoundingFee = BigDecimal.ZERO;
		List<PlotInfo> plots = criteria.getRegularization().getLandRegularizationInfo().getPlotInfo();
		
		LandRegularizationInfo landInfo = criteria.getRegularization().getLandRegularizationInfo(); 

		if (criteria.getRegularization().getLandRegularizationInfo().getLandRegularizationType()
				.equalsIgnoreCase(RegularizationConstants.PLOTS_SUBDIVIDED_BEFORE_30th_MAY_2017)) {

			totalCompoundingFee = calculateCompoundingFeeForTypeA(totalCompoundingFee, plots);

		} else if (criteria.getRegularization().getLandRegularizationInfo().getLandRegularizationType()
				.equalsIgnoreCase(RegularizationConstants.PLOTS_SUBDIVIDED_AFTER_30th_MAY_2017_TILL_29th_SEP_2022)) {

			totalCompoundingFee = calculateCompundingFeeForTypeB(totalCompoundingFee, plots, landInfo);
		}
		
		utils.generateTaxHeadEstimate(estimates, totalCompoundingFee, RegularizationConstants.TAXHEAD_REG_LAND_COMPOUNDING_FEE, Category.FEE);
		log.info(" Total Compounding fee for Appl :" + criteria.getApplicationNo() + " is " + totalCompoundingFee);

		return totalCompoundingFee;
	}

	
	/**
	 * Calculate compouding fee for Type A applications here
	 * 
	 * @param totalCompoundingFee
	 * @param plots
	 * @return calculated Land Development Fee
	 */
	private BigDecimal calculateCompoundingFeeForTypeA(BigDecimal totalCompoundingFee, List<PlotInfo> plots) {
		for (PlotInfo plot : plots) {
			BigDecimal plotArea = utils.getNetPlotArea(plot);
			log.info("Net plot area : " + plotArea + " for plot: " + plot.getPlotNo());
			BigDecimal bmvValuePerAcre = utils.convertToBigDecimal(plot.getBmvValue());

			BigDecimal bmvValuePerSqmt = utils.divideWithRoundUp(bmvValuePerAcre, RegularizationConstants.ONE_ACRE);
			BigDecimal bmvValueForPlot = utils.multiplyWithRoundUp(bmvValuePerSqmt, plotArea);
			BigDecimal feeForThisPlot = BigDecimal.ZERO;
			
			if(plotArea.compareTo(RegularizationConstants.FIVE_HUNDRED_SQFT) <= 0) {
				log.info("Compunding Fee for plot: " + plot.getPlotNo() + " is " + feeForThisPlot);
				
			} else if (plotArea.compareTo(RegularizationConstants.FIVE_HUNDRED_SQFT) > 0
					&& plotArea.compareTo(RegularizationConstants.FIVE_THOUSAND_SQFT) <= 0) {
				
				feeForThisPlot = utils.multiplyWithRoundUp(bmvValueForPlot, RegularizationConstants.ONE_PERCENT);
				log.info("Compunding Fee for plot: " + plot.getPlotNo() + " is " + feeForThisPlot);
				totalCompoundingFee = totalCompoundingFee.add(feeForThisPlot);
				
			} else if (plotArea.compareTo(RegularizationConstants.FIVE_THOUSAND_SQFT) > 0) {
				
				feeForThisPlot = utils.multiplyWithRoundUp(bmvValueForPlot, RegularizationConstants.FIVE_PERCENT);
				log.info("Compunding Fee for plot: " + plot.getPlotNo() + " is " + feeForThisPlot);
				totalCompoundingFee = totalCompoundingFee.add(feeForThisPlot);
			}

		}
		return totalCompoundingFee;
	}

	
	/**
	 * Calculate compouding fee for Type B applications here
	 * 
	 * @param totalCompoundingFee
	 * @param plots
	 * @return Calculated Compounding Fee
	 */
	private BigDecimal calculateCompundingFeeForTypeB(BigDecimal totalCompoundingFee, List<PlotInfo> plots,
			LandRegularizationInfo landInfo) {

		BigDecimal totalPlotArea = landInfo.getTotalPlotArea();
		log.info("Total plot area : " + totalPlotArea);
		for (PlotInfo plot : plots) {
			if (plot.getActive()) {
				BigDecimal plotArea = utils.getNetPlotArea(plot);
				log.info("Net plot area : " + plotArea + " for plot: " + plot.getPlotNo());
				BigDecimal bmvValuePerAcre = utils.convertToBigDecimal(plot.getBmvValue());

				BigDecimal bmvValuePerSqmt = utils.divideWithRoundUp(bmvValuePerAcre, RegularizationConstants.ONE_ACRE);
				BigDecimal bmvValueForPlot = utils.multiplyWithRoundUp(bmvValuePerSqmt, plotArea);
				BigDecimal feeForThisPlot = null;
				if (totalPlotArea.compareTo(RegularizationConstants.FIVE_HUNDRED) > 0) {
					// If Total Plot Area is greater than 500 sqmt, then 10% of subplot bmv is
					// applicable
					feeForThisPlot = utils.multiplyWithRoundUp(bmvValueForPlot, RegularizationConstants.TEN_PERCENT);
				} else {
					// If Total Plot Area is less than or equal 500 sqmt, then 5% of subplot bmv is
					// applicable
					feeForThisPlot = utils.multiplyWithRoundUp(bmvValueForPlot, RegularizationConstants.FIVE_PERCENT);
				}
				log.info("Compunding Fee for plot: " + plot.getPlotNo() + " is " + feeForThisPlot);
				totalCompoundingFee = totalCompoundingFee.add(feeForThisPlot);
			}
		}
		return totalCompoundingFee;
	}
	
}
