package org.egov.bpa.util;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class RegularizationConstants {
	
	//MDMS constants
	public static final String ODA_ULBS = "OdaUlbs";
	
	public static final String STATE_TENANTID = "od";
	
	public static final String ODA_ULBS_JSONPATH_CODE = "$.MdmsRes.BPA.OdaUlbs";
	
	public static final String BPA_JSON_CODE = "$.MdmsRes.BPA";
	
	public static final String COMMON_MASTER_JSONPATH_CODE = "$.MdmsRes.common-masters";
	
	public static final String DOCUMENT_TYPE_MAPPING = "RegularizationDocType";
	
	public static final String OWNERSHIP_CATEGORY = "OwnerShipCategory";
	
	public static final String CITIZEN = "CITIZEN";
	
	//regularization constants
	
	public static final String PLOTS_SUBDIVIDED_BEFORE_30th_MAY_2017 = "TYPE_A";
	
	public static final String PLOTS_SUBDIVIDED_AFTER_30th_MAY_2017_TILL_29th_SEP_2022 = "TYPE_B";
	
	public static final Long THIRTY_DECEMBER_2022 = 1672424999000L;
	
	public static final BigDecimal FIVE_HUNDRED_SQMT = new BigDecimal("500");
	
	public static final BigDecimal ONE_ACRE = new BigDecimal("4046.85");
	
	public static final BigDecimal ONE_HECTARE = new BigDecimal("10000");
	
	public static final String SANC_FEE_STATE = "PENDING_SANC_FEE_PAYMENT";

	public static final String APPL_FEE_STATE = "PENDING_APPL_FEE";

	public static final String APPLICATION_FEE_KEY = "ApplicationFee";

	public static final String SANCTION_FEE_KEY = "SanctionFee";
	
	public static final String APPROVED = "APPROVED";

	public static final String DOCUMENT_TYPE = "DocumentType";
	
	public static final String SHOW_CAUSE_KEY ="regularization-show-cause";
	
	public static final String REFUSAL_SHOW_CAUSE_KEY ="regularization-refusal-showcause";
	
	public static final String BUILDING_HEIGHT ="height";
	
	public static final String IS_SPECIAL_BUILDING ="isSpecialBuilding";
	
	public static final String SITE_PLAN_LAYOUT_SIGN ="sitePlanLayoutSignature";
	
	public static final String APPLICATION_TYPE ="applicationType";
	
	public static final String SITE_PLAN_LAYOUT_IS_SIGNED ="sitePlanLayoutIsSigned";
	
	public static final String DOC_TYPE_SITE_PLAN ="APPL.SITEPLAN.SITEPLANLAYOUT";
	
	public static final String UNSIGNED_SITE_PLAN_LAYOUT_DETAILS = "unsignedSitePlanLayoutDetails";
	
	public static final String BUILDING_PLAN_LAYOUT_SIGN ="buildingPlanLayoutSignature";
	
	public static final String BUILDING_PLAN_LAYOUT_IS_SIGNED ="buildingPlanLayoutIsSigned";
	
	public static final String DOC_TYPE_BUILDING_PLAN ="APPL.BLP.BLP";
	
	public static final String UNSIGNED_BUILDING_PLAN_LAYOUT_DETAILS = "unsignedBuildingPlanLayoutDetails";
	
	public static final String LAND_AND_BUILDING_PLAN_LAYOUT_SIGN ="landAndBuildingPlanLayoutSignature";
	
	public static final String LAND_AND_BUILDING_PLAN_LAYOUT_IS_SIGNED ="landAndBuildingPlanLayoutIsSigned";
	
	public static final String UNSIGNED_LAND_AND_BUILDING_PLAN_LAYOUT_DETAILS = "unsignedLandAndBuildingPlanLayoutDetails";
	
	public static final String CODE = "documentType";
	
	public static final String FILESTOREID = "fileStoreId";
	
	public static final String ACTION_DELETE = "DELETE";
	
	public static final String  STATUS_CITIZEN_APPROVAL_INPROCESS = "CITIZEN_APPROVAL_INPROCESS";
	
	public static final String  INPROGRESS_STATUS = "INPROGRESS";
	
	public static final String  INITIATED = "INITIATED";
	
	public static final String DELETED = "DELETED";
	
	public static final String ACTION_UPDATE_DATA = "UPDATE_DATA";
	
	public static final String FLOORS ="floors";
	
	public static final String SETBACK ="setback";
	
	public static final BigDecimal TEN_METERS = new BigDecimal("10");
	
	public static final BigDecimal FIFTEEN_METERS = new BigDecimal("15");
	
	public static final BigDecimal THIRTY_METERS = new BigDecimal("30");
	
	public static final String ANSWER_YES = "YES";
	
	//Bussiness Service Constants
	
	public static final String LAND_BUSSINESS_SERVICE_1 = "LR1";
	
	public static final String LAND_BUSSINESS_SERVICE_2 = "LR2";
	
	public static final String LAND_BUSSINESS_SERVICE_3 = "LR3";
	
	public static final String LAND_BUSSINESS_SERVICE_4 = "LR4";
	
	public static final String BUILDING_BUSSINESS_SERVICE_1 = "BLR1";

	public static final String BUILDING_BUSSINESS_SERVICE_2 = "BLR2";

	public static final String BUILDING_BUSSINESS_SERVICE_3 = "BLR3";

	public static final String BUILDING_BUSSINESS_SERVICE_4 = "BLR4";
	
	
	//Error Constants
	public static final String INVALID_TENANT_ID_MDMS_KEY = "INVALID TENANTID";

	public static final String INVALID_TENANT_ID_MDMS_MSG = "No data found for this tenentID";
	
	public static final String INVAILD_OWNER = "INVAILD_OWNER";
	
	public static final String WF_KEY_NOT_FOUND = "WF_ERROR_KEY_NOT_FOUND";
	
	public static final String EG_WF_ERROR = "EG WF ERROR";
	
	public static final String UPDATE_ERROR = "UPDATE_ERROR";

	public static final String INVAILD_UPDATE = "INVAILD_UPDATE";

	public static final String UNAUTHORIZED_UPDATE = "UNAUTHORIZED_UPDATE";

	public static final String UPDATE_ERROR_COMMENT_REQUIRED = "REGULARIZATION UPDATE ERROR COMMENT REQUIRED";

	public static final String DUPLICATE_DOCUMENT = "DUPLICATE DOCUMENT";

	public static final String MANADATORY_DOCUMENTPYE_MISSING = "MANADATORY DOCUMENTPYE MISSING";

	public static final String INVALID_DOCUMENTTYPE = "INVALID DOCUMENTTYPE";

	public static final String UNKNOWN_DOCUMENTTYPE = "UNKNOWN DOCUMENTTYPE";
	
	public static final String SEARCH_MODULE ="rainmaker-common";
	
	public static final String DRAFT_NUMBER ="draftNo";
	
	public static final String INDUSTRIAL_BUILDING_CODE ="E-IB";
	
	public static final List<String> OBPS_ALL_DA_LIST = Arrays.asList("od.berhampurdevelopmentauthority",
			"od.cuttackdevelopmentauthority", "od.kalinganagardevelopmentauthority", "od.paradipdevelopmentauthority",
			"od.purikonarkdevelopmentauthority", "od.rourkeladevelopmentauthority", "od.sambalpurdevelopmentauthority",
			"od.talcherangulmeramandalidevelopmentauthority", "od.bhubaneswardevelopmentauthority");
	
}
