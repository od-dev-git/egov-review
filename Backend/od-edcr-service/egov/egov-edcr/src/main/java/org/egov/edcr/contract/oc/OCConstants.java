package org.egov.edcr.contract.oc;

import java.math.BigDecimal;

public class OCConstants {

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

    public static final String BUILDING_HEIGHT ="height";

    public static final String IS_SPECIAL_BUILDING ="isSpecialBuilding";

    public static final String SITE_PLAN_LAYOUT_SIGN ="sitePlanLayoutSignature";

    public static final String APPLICATION_TYPE ="applicationType";

    public static final String SITE_PLAN_LAYOUT_IS_SIGNED ="sitePlanLayoutIsSigned";

    public static final String DOC_TYPE_SITE_PLAN ="APPL.SITEPLAN.SITEPLANLAYOUT";

    public static final String UNSIGNED_SITE_PLAN_LAYOUT_DETAILS = "unsignedSitePlanLayoutDetails";

    public static final String CODE = "documentType";

    public static final String FILESTOREID = "fileStoreId";

    public static final String ACTION_DELETE = "DELETE";

    public static final String  STATUS_CITIZEN_APPROVAL_INPROCESS = "CITIZEN_APPROVAL_INPROCESS";

    public static final String  INPROGRESS_STATUS = "INPROGRESS";

    public static final String  INITIATED = "INITIATED";

    public static final String DELETED = "DELETED";

    public static final String FLOORS ="floors";

    public static final String SETBACK ="setback";

    public static final BigDecimal TEN_METERS = new BigDecimal("10");

    public static final BigDecimal FIFTEEN_METERS = new BigDecimal("15");

    public static final BigDecimal THIRTY_METERS = new BigDecimal("30");

    public static final String ANSWER_YES = "YES";

    //Bussiness Service Constants

    public static final String OC_BUSSINESS_SERVICE_1 = "BPA_OC1";

    public static final String OC_BUSSINESS_SERVICE_2 = "BPA_OC2";

    public static final String OC_BUSSINESS_SERVICE_3 = "BPA_OC3";

    public static final String OC_BUSSINESS_SERVICE_4 = "BPA_OC4";


}
