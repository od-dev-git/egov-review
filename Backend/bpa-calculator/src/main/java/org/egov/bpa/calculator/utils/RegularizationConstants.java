package org.egov.bpa.calculator.utils;

import java.math.BigDecimal;

public class RegularizationConstants {
	
	public static final String APPLICATION_FEE = "ApplicationFee";
	public static final String SANCTION_FEE = "SanctionFee";
	
	public static final String APP_TYPE_KEY = "appType";
	public static final String APP_TYPE_LAND = "LAND";
	public static final String APP_TYPE_BUILDING = "BUILDING";
	public static final String APP_TYPE_LAND_BUILDING = "LAND_AND_BUILDING";
	
	public static final String RISK_TYPE_LOW = "LOW";
	public static final String RISK_TYPE_KEY = "riskType";
	public static final String IS_SPARIT_KEY = "isSparit";
	public static final String IS_REWORK_APP_KEY = "isReworkApp";
	public static final String LAND_DEV_FEE_PAID_KEY = "landDevelopmentFeePaid";
	public static final String BUILDING_OPR_FEE_PAID_KEY = "buildingOperationFeePaid";
	public static final String WALFARE_CESS_RATE = "welfareCessRate";
	public static final String CONSTRUCTION_COST = "constructionCost";
	
	

	//Tax Head for Land Regularization
	public static final String TAXHEAD_REG_LAND_DEVELOPMENT_FEE = "REG_LAND_DEV_FEE";
	public static final String TAXHEAD_REG_LAND_COMPOUNDING_FEE = "REG_LAND_COMP_FEE";
	
	//Tax Head for Building Regularization
	public static final String TAXHEAD_REG_BUILDING_OPPERATION_FEE = "REG_BLDNG_OPRN_FEE";
	public static final String TAXHEAD_REG_BUILDING_SANCTION_FEE = "REG_BLDNG_SANC_FEE";
	public static final String TAXHEAD_REG_CONSTRUCTION_WORKER_WELFARE_CESS = "REG_CONST_WORKER_WELFARE_CESS";
	public static final String TAXHEAD_REG_SHELTER_FEE = "REG_SHELTER_FEE";
	public static final String TAXHEAD_REG_TEMPORARY_RETENTION_FEE = "REG_TEMP_RETENTION_FEE";
	public static final String TAXHEAD_REG_SECURITY_DEPOSIT = "REG_SECURITY_DEPOSIT";
	public static final String TAXHEAD_REG_PURCHASABLE_FAR = "REG_PUR_FAR";
	public static final String TAXHEAD_REG_EIDP_FEE = "REG_EIDP_FEE";
	public static final String TAXHEAD_REG_COMPOUNDING_FAR_FEE = "REG_COMP_FAR_FEE";
	public static final String TAXHEAD_REG_COMPOUNDING_SETBACK_FEE = "REG_COMP_SETBACK_FEE";
	public static final String TAXHEAD_REG_ADJUSTMENT_AMOUNT_1 = "REG_SANC_ADJUSTMENT_AMOUNT_1";
	public static final String TAXHEAD_REG_ADJUSTMENT_AMOUNT_2 = "REG_SANC_ADJUSTMENT_AMOUNT_2";
	public static final String TAXHEAD_REG_ADJUSTMENT_AMOUNT_3 = "REG_SANC_ADJUSTMENT_AMOUNT_3";
	public static final String TAXHEAD_REG_ADJUSTMENT_AMOUNT_4 = "REG_SANC_ADJUSTMENT_AMOUNT_4";
	public static final String TAXHEAD_REG_ADJUSTMENT_AMOUNT_5 = "REG_SANC_ADJUSTMENT_AMOUNT_5";
	public static final String TAXHEAD_REG_LAND_DEVELOPMENT_FEE_REWORK_ADJUSTMENT = "REG_LAND_DEV_FEE_REWORK_ADJUSTMENT";
	public static final String TAXHEAD_REG_BUILDING_OPPERATION_FEE_REWORK_ADJUSTMENT = "REG_BLDNG_OPRN_FEE_REWORK_ADJUSTMENT";
	public static final String REG_SANC_FEE_OTHER_FEES_KEY = "otherFees";
	
	
	public static Long BILL_EXPIRY_TIME = 2629800000L;

	public static final BigDecimal SPARIT_RATE_FOR_LAND_DEV_FEE = new BigDecimal("1.00");
	
	public static final BigDecimal RATE_FOR_LAND_DEV_FEE = new BigDecimal("5.00");
	
	public static BigDecimal FIFTEEN_PERCENT = new BigDecimal("0.15");
	
	public static BigDecimal TEN_PERCENT = new BigDecimal("0.10");
	
	public static BigDecimal ONE_PERCENT = new BigDecimal("0.01");
	
	public static BigDecimal FIVE_PERCENT = new BigDecimal("0.05");
	
	public static BigDecimal FIVE_HUNDRED_SQFT = new BigDecimal("46.4515");
	
	public static BigDecimal FIVE_THOUSAND_SQFT = new BigDecimal("464.5152");
	
	public static final BigDecimal ONE_ACRE = new BigDecimal("4046.85");

	public static final String PLOTS_SUBDIVIDED_BEFORE_30th_MAY_2017 = "TYPE_A";

	public static final String PLOTS_SUBDIVIDED_AFTER_30th_MAY_2017_TILL_29th_SEP_2022 = "TYPE_B";
	
	public static final String LAND_BUSSINESS_SERVICE_1 = "LR1";
	
	public static final String LAND_BUSSINESS_SERVICE_2 = "LR2";
	
	public static final String LAND_BUSSINESS_SERVICE_3 = "LR3";
	
	public static final String LAND_BUSSINESS_SERVICE_4 = "LR4";
	
	public static final BigDecimal COMMON_CONSTANT_PRICE_PER_UNIT = new BigDecimal("5");// BigDecimal.valueOf(5);
	public static final Integer INT_TWENTY = 20;
	public static final Integer INT_FIFTY = 50;
	public static final Integer INT_HUNDRED = 100;
	public static final Integer INT_ONE_HUNDRED_FIFTY = 150;
	public static final Integer INT_THREE_HUNDRED = 300;
	
	public static final Double FOURTY_NINE = 49.99;
	
	public static final BigDecimal ZERO_TWO_FIVE = new BigDecimal("0.25");// BigDecimal.valueOf(0.25);
	public static final BigDecimal ZERO_POINT_FIVE = new BigDecimal("0.5");// BigDecimal.valueOf(0.5);
	public static final BigDecimal FIVE = new BigDecimal("5");// BigDecimal.valueOf(5);
	public static final BigDecimal TEN = new BigDecimal("10");// BigDecimal.valueOf(10);
	public static final BigDecimal FIFTEEN = new BigDecimal("15");// BigDecimal.valueOf(15);
	public static final BigDecimal TWENTY = new BigDecimal("20");// BigDecimal.valueOf(20);
	public static final BigDecimal TWENTY_FIVE = new BigDecimal("25");// BigDecimal.valueOf(25);
	public static final BigDecimal THIRTY = new BigDecimal("30");// BigDecimal.valueOf(30);
	public static final BigDecimal FIFTY = new BigDecimal("50");// BigDecimal.valueOf(50);
	public static final BigDecimal SIXTY = new BigDecimal("60");// BigDecimal.valueOf(60);
	public static final BigDecimal HUNDRED = new BigDecimal("100");// BigDecimal.valueOf(100);
	public static final BigDecimal TWO_HUNDRED_FIFTY = new BigDecimal("250");// BigDecimal.valueOf(250);
	public static final BigDecimal FIVE_HUNDRED = new BigDecimal("500");// BigDecimal.valueOf(500);
	public static final BigDecimal FIFTEEN_HUNDRED = new BigDecimal("1500");// BigDecimal.valueOf(1500);
//	public static final BigDecimal EIGHTEEN_FIFTY_SEVEN = new BigDecimal("1857.42");// BigDecimal.valueOf(1857.42);
	public static final BigDecimal SQMT_SQFT_MULTIPLIER = new BigDecimal("10.764");// BigDecimal.valueOf(10.764);
	public static final BigDecimal ACRE_SQMT_MULTIPLIER = new BigDecimal("4046.85");// BigDecimal.valueOf(4046.85);
	public static final BigDecimal TEN_LAC = new BigDecimal("1000000");// BigDecimal.valueOf(1000000);
	public static final BigDecimal WELFARE_CESS_RATE = new BigDecimal("18.57");// BigDecimal.valueOf(18.57);
	public static final BigDecimal ONE_FORTY_PERCENT = new BigDecimal("1.40");// BigDecimal.valueOf(1.40);
	public static final BigDecimal ONE_HUNDRED_FIFTY = new BigDecimal("150");// BigDecimal.valueOf(150);
	public static final BigDecimal THREE_HUNDRED_SEVENTY_FIVE = new BigDecimal("375");// BigDecimal.valueOf(375);
	public static final BigDecimal SIX_HUNDRED_TWENTY_FIVE = new BigDecimal("625");// BigDecimal.valueOf(625);
	public static final BigDecimal SIX_HUNDRED_SEVENTY_FIVE = new BigDecimal("675");// BigDecimal.valueOf(675);
	public static final BigDecimal THREE_HUNDRED = new BigDecimal("300");// BigDecimal.valueOf(300);
	public static final BigDecimal SEVEN_HUNDRED_FIFTY = new BigDecimal("750");// BigDecimal.valueOf(750);
	public static final BigDecimal TWELVE_POINT_FIVE = new BigDecimal("12.5");// BigDecimal.valueOf(12.5);
	public static final BigDecimal SEVEN_POINT_FIVE = new BigDecimal("7.5");// BigDecimal.valueOf(7.5);
	
}
