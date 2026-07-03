package org.egov.edcr.service;

import static org.egov.edcr.utility.DcrConstants.DECIMALDIGITS_MEASUREMENTS;
import static org.egov.edcr.utility.DcrConstants.ROUNDMODE_MEASUREMENTS;

import java.io.InputStream;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.egov.common.entity.edcr.AdditionalReportDetail;
import org.egov.common.entity.edcr.Block;
import org.egov.common.entity.edcr.Building;
import org.egov.common.entity.edcr.DcrReportBlockDetail;
import org.egov.common.entity.edcr.DcrReportFloorDetail;
import org.egov.common.entity.edcr.Floor;
import org.egov.common.entity.edcr.Lift;
import org.egov.common.entity.edcr.Measurement;
import org.egov.common.entity.edcr.Occupancy;
import org.egov.common.entity.edcr.OccupancyTypeHelper;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.ScrutinyDetail;
import org.egov.common.entity.edcr.SetBack;
import org.egov.edcr.config.properties.EdcrApplicationSettings;
import org.egov.edcr.constants.DxfFileConstants;
import org.egov.edcr.constants.NOCConstants;
import org.egov.edcr.feature.AdditionalFeature;
import org.egov.edcr.od.FloorNumberToWord;
import org.egov.edcr.od.OdishaUtill;
import org.egov.edcr.utility.DcrConstants;
import org.egov.infra.exception.ApplicationRuntimeException;
import org.egov.infra.microservice.models.RequestInfo;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.CollectionUtils;

import com.itextpdf.text.BadElementException;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.BarcodeQRCode;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

public abstract class PermitOrderService {

	@Autowired
	private JasperReportService reportService;

	@Autowired
	private PaymentService paymentService;

	@Autowired
	private MdmsService mdmsService;

	@Autowired
	private NocService nocService;
	
	@Autowired
	private LocationVillageService locationVillageService;
	
	@Autowired
	private UserInfoService userService;
	
	@Autowired
	private BpaService bpaService;
	
	@Value("${dxf.to.pdf.flag:false}")
	protected boolean dxfToPdfFlag;

	private static final Logger LOG = Logger.getLogger(PermitOrderService.class);

	public static final String FRONT_YARD_DESC = "Front Setback";
	public static final String REAR_YARD_DESC = "Rear Setback";
	public static final String SIDE_YARD1_DESC = "Side Setback 1";
	public static final String SIDE_YARD2_DESC = "Side Setback 2";
	public static final String BSMT_FRONT_YARD_DESC = "Basement Front Setback";
	public static final String BSMT_REAR_YARD_DESC = "Basement Rear Setback";
	public static final String BSMT_SIDE_YARD1_DESC = "Basement Side Setback 1";
	public static final String BSMT_SIDE_YARD2_DESC = "Basement Side Setback 2";
	public static final String BSMT_SIDE_YARD_DESC = "Basement Side Setback";
	public static final String SIDE_YARD_DESC = "Side Setback";
	private static final String SIDENUMBER = "Side Number";
	private static final String SIDENUMBER_NAME = "Setback";
	private static final String LEVEL = "Level";
	private static final String BLOCK_WISE_SUMMARY = "Block Wise Summary";
	private static final String TOTAL = "Total";
	private static final String DESCRIPTION = "description";
	private static final String RULE_NO = "RuleNo";
	public static final String BLOCK = "Block";
	public static final String STATUS = "Status";
	public static final String SQM =" SQM";
	public static final String PROVIDED_LIFT_DETAIL = "providedLiftDetail";
	public static final String REQUIRED_LIFT_DETAIL = "requiredLiftDetail";
	public static final String SCRUTINY_DETAIL_PROVIDED = "Provided";
	public static final String SCRUTINY_DETAIL_REQUIRED = "Required";
	public static final String GROUND_FLOOR_NO = "0";
	public static final String DETAIL = "Detail";
	public static final String DESCRIPTION_IN_SCRUTINY_DETAIL = "Description";
	public static final String NO_OF_TREE_PER_PLOT = "No of tree as per plot";
	public static final String PAYMENTS_RESPONSE_FIELD = "Payments";
	
	public static final String TAXHEAD_BPA_SANC_FEES_CODE = "BPA_SANC_FEES";
	public static final String TAXHEAD_BPA_SANC_FEES_NAME = "Sanction Fee";
	public static final String TAXHEAD_BPA_SANC_TEMP_RETENTION_FEE_CODE = "BPA_SANC_TEMP_RETENTION_FEE";
	public static final String TAXHEAD_BPA_SANC_TEMP_RETENTION_FEE_NAME = "Temporary Retention Fee";
	public static final String TAXHEAD_BPA_SANC_SECURITY_DEPOSIT_CODE = "BPA_SANC_SECURITY_DEPOSIT";
	public static final String TAXHEAD_BPA_SANC_SECURITY_DEPOSIT_NAME = "Security Deposit";
	public static final String TAXHEAD_BPA_SANC_WORKER_WELFARE_CESS_CODE = "BPA_SANC_WORKER_WELFARE_CESS";
	public static final String TAXHEAD_BPA_SANC_WORKER_WELFARE_CESS_NAME = "Construction Workers Welfare Cess";
	public static final String TAXHEAD_BPA_SANC_PUR_FAR_CODE = "BPA_SANC_PUR_FAR";
	public static final String TAXHEAD_BPA_SANC_PUR_FAR_NAME = "Purchasable FAR";
	public static final String TAXHEAD_BPA_SANC_SHELTER_FEE_CODE = "BPA_SANC_SHELTER_FEE";
	public static final String TAXHEAD_BPA_SANC_SHELTER_FEE_NAME = "Shelter Fee";
	public static final String TAXHEAD_BPA_SANC_SANC_FEE_CODE = "BPA_SANC_SANC_FEE";
	public static final String TAXHEAD_BPA_SANC_SANC_FEE_NAME = "Sanction Fee";
	public static final String TAXHEAD_BPA_SANC_EIDP_FEE_CODE = "BPA_SANC_EIDP_FEE";
	public static final String TAXHEAD_BPA_SANC_EIDP_FEE_NAME = "EIDP FEE";
	public static final String TAXHEAD_BPA_SANC_ADJUSTMENT_AMOUNT_CODE = "BPA_SANC_ADJUSTMENT_AMOUNT";
	public static final String TAXHEAD_BPA_SANC_ADJUSTMENT_AMOUNT_NAME = "Other Fee";
	
	public static final String TAXHEAD_BPA_SANC_ADJUSTMENT_AMOUNT_CODE_1 = "BPA_SANC_ADJUSTMENT_AMOUNT_1";
	public static final String TAXHEAD_BPA_SANC_ADJUSTMENT_AMOUNT_CODE_2 = "BPA_SANC_ADJUSTMENT_AMOUNT_2";
	public static final String TAXHEAD_BPA_SANC_ADJUSTMENT_AMOUNT_CODE_3 = "BPA_SANC_ADJUSTMENT_AMOUNT_3";
	
	public static final String TAXHEAD_BPA_SANC_ADJUSTMENT_AMOUNT_1_NAME = "Other Fee 1";
	public static final String TAXHEAD_BPA_SANC_ADJUSTMENT_AMOUNT_2_NAME = "Other Fee 2";
	public static final String TAXHEAD_BPA_SANC_ADJUSTMENT_AMOUNT_3_NAME = "Other Fee 3";
	
	public static final String TAXHEAD_BPA_BLDNG_OPRN_FEE_REWORK_ADJUSTMENT_CODE = 
			"BPA_BLDNG_OPRN_FEE_REWORK_ADJUSTMENT";
	public static final String TAXHEAD_BPA_BLDNG_OPRN_FEE_REWORK_ADJUSTMENT_NAME = 
			"Building Operation Fee Rework Adjustment Amount";
	public static final String TAXHEAD_BPA_LAND_DEV_FEE_REWORK_ADJUSTMENT_CODE="BPA_LAND_DEV_FEE_REWORK_ADJUSTMENT";
	public static final String TAXHEAD_BPA_LAND_DEV_FEE_REWORK_ADJUSTMENT_NAME=
			"Land Development Fee Rework Adjustment Amount";
	public static final String TAXHEAD_BPA_LAND_DEV_FEE_CODE = "BPA_LAND_DEV_FEE";
	public static final String TAXHEAD_BPA_LAND_DEV_FEE_NAME = "Development Fee";
	public static final String TAXHEAD_BPA_BLDNG_OPRN_FEE_CODE = "BPA_BLDNG_OPRN_FEE";
	public static final String TAXHEAD_BPA_BLDNG_OPRN_FEE_NAME = "Fee for Building Operation";
	public static final String ADJUSTED_AMOUNT_ZERO = "0.0";
	public static final String OWNERSHIP_MAJOR_TYPE_INDIVIDUAL = "INDIVIDUAL";
	public static final String OWNERSHIP_MAJOR_TYPE_INSTITUTIONAL_PRIVATE = "INSTITUTIONALPRIVATE";
	public static final String OWNERSHIP_MAJOR_TYPE_INSTITUTIONAL_GOVERNMENT = "INSTITUTIONALGOVERNMENT";
	public static final String PLOT = "plot";
	
	public static final String NOC_TITLE = "NOCs/ Clearances submitted:";
	public static final String ONLINE_TITLE = "Fire, NMA and AAI";
	public static final String OTHERNOC_TITLE = "Other NOCs";
	public static final String NOC_PARA = "The Permit letter is being granted provisionally on the condition that the following list of No Objection Certificates (NOCs) are to be submitted mandatorily before the commencement of any construction work and that the applicant will commit to fulfilling all the prerequisites for the construction project.";
	public static final String NOCRELAXATIONCHECKLIST = "nocRelaxationCheckList";
	
	public static final String BPA_ADD_DETAILS_SERVICE_KEY = "alterationService";
	public static final String BPA_ADD_DETAILS_SUBSERVICE_KEY = "alterationSubService";
	public static final String BPA_ADD_DETAILS_PERMIT_NO_KEY = "permitNumber";
	public static final String ALTERATION_SUBSERVICE_D = "ALTERATION_SERVICE_D";
	public static final String ALTERATION_SUBSERVICE_C = "ALTERATION_SERVICE_C";
	public static final String ALTERATION_SUBSERVICE_B = "ALTERATION_SERVICE_B";
	public static final String ALTERATION_SUBSERVICE_A = "ALTERATION_SERVICE_A";
	
	public static final String CAD_STATEMENT = "In case there is any discrepancy found with numbers/figures provided in CAD drawing and Plan PDF, then the numbers/figures provided in the CAD drawing will prevail and will be considered valid.";
	
	public static BaseColor GREY = new BaseColor(216, 216, 216);
	public static BaseColor ORANGE = new BaseColor(248, 203, 172);
	public static BaseColor LIME = new BaseColor(226, 239, 217);
	public static BaseColor BLUE = new BaseColor(188, 214, 238);
	public static BaseColor PINK = new BaseColor(251, 228, 213);
	public static BaseColor GREEN = new BaseColor(197, 224, 178);
	public static BaseColor YELLOW = new BaseColor(255, 231, 154);
	public static BaseColor DEEPYELLOW = new BaseColor(255, 255, 0);
	
	public static String PARAGRAPH_EXISTING_BUILTUP_AREA = "Total built up area (existing) : ";
	public static String PARAGRAPH_EXISTING_FLOOR_AREA = "Total FAR area (existing) : ";

	public abstract InputStream generateReport(Plan plan, LinkedHashMap bpaApplication, RequestInfo requestInfo, Boolean isPreview);
	@Autowired
	private EdcrApplicationSettings edcrApplicationSettings;
	
	Map<String, String> nocMap = new HashMap<>();
	{
		nocMap.put("NOC.NOCODSPCB.NOC", NOCConstants.NOC_NOCCRZ); //As per mapping in the frontend and localization. 
		
		nocMap.put("NOC.ENVCLEARANCE.NOC", NOCConstants.NOC_ENVCLEARANCE);
		
		nocMap.put("NOC.NOCULBHUD.NOC", NOCConstants.NOC_NOCULBHUD);
		
		nocMap.put("NOC.NOCPUBHLTHENGGORG.NOC", NOCConstants.NOC_NOCPUBHLTHENGGORG);
		
		nocMap.put("NOC.NOCELECTRICDISCOMP.NOC", NOCConstants.NOC_NOCELECTRICDISCOMP);
		
		nocMap.put("NOC.NOCPOLUNHOMEDEPT.NOC", NOCConstants.NOC_NOCPOLUNHOMEDEPT);
		
//		nocMap.put("NOC.NOCODSPCB.NOC", NOCConstants.NOC_NOCODSPCB);
		
		nocMap.put("NOC.NA", NOCConstants.NOC_NA);
		
		nocMap.put("NOC.NOCDEPUTYFORESTOFFICER.NOC", NOCConstants.NOC_NOCDEPUTYFORESTOFFICER);
		
		nocMap.put("NOC.NOCWATERRESDEPT.NOC", NOCConstants.NOC_NOCWATERRESDEPT);
		
		nocMap.put("NOC.NOCTEHREVDISASMANG.NOC", NOCConstants.NOC_NOCTEHREVDISASMANG);
		
		nocMap.put("NOC.NOCNHAI.NOC", NOCConstants.NOC_NOCNHAI);
		
		nocMap.put("NOC.RDMTEHSILDAR.CERTIFICATE", NOCConstants.NOC_TEHSILDAR);
		
		nocMap.put("NOC.CGWA.CERTIFICATE", NOCConstants.NOC_NOCCGWA);
	}
	
	Map<String, String> rejectionMap = new HashMap<>();
	
	{ // to be done
		rejectionMap.put("NOT_PERMISSIBLE_IN_ZONING_REGULATIONS", "The plot is located in the Open Space use / Transport use / Agriculture Use / Forest Use / Water bodies use / Environmentally Sensitive Zone; and proposed use is not permissible under Zoning Regularions. ");
		rejectionMap.put("PROPOSED_CPD_ROAD", "The plot is affected by Proposed CDP road / drain/ water body use");
		rejectionMap.put("ASI_PROTECTED_MONUMENT", "The plot is located in Prohibited/Protected zone of ASI protected monument and you have not submitted NOC from the competent authority.");		
		rejectionMap.put("LOCATED_IN_ECO_SENSITIVE_ZONE", "The plot is located within Eco-sensitive zone and you have not submitted NOC from the competent authority.");		
		rejectionMap.put("OVER_WATER_BODIES", "The plot is over water bodies like river, nala, tank, lake, pond, etc. and/or recorded as Jalasaya/ Forest in the revenue records.");	
		rejectionMap.put("SUB_DIVIDED_AND_SOLD", "The open space earmarked in the approved layout has been sub-divided and sold and/or transferred.");
		rejectionMap.put("NOT_COMPLYING_WITH_SCHEME_PROVISIONS", "The approach road width is less than 4.5 meters and hence not complying with the Scheme provisions.");
		rejectionMap.put("SUB_DIVIDED_AFTER_30_05_2017", "The plot has been sub-divided after the cut-off date of the scheme, i.e. after 30.05.2017.");		
		rejectionMap.put("OWNERSHIP_NOT_ESTABLISHED", "As per the submitted ownership documents, the ownership is not established in your favour.");
		
	}
	

	
	public static String image_logo;
	public Image logo = null;
	{
		try {
			ClassPathResource resource = new ClassPathResource("logo_base64.txt");
			InputStream inputStream = resource.getInputStream();
			//InputStream is = classloader.getResourceAsStream("logo_base64.txt");
			//FileInputStream fis = new FileInputStream("classpath:config/logo_base64.txt");
			String stringTooLong = IOUtils.toString(inputStream, "UTF-8");
			byte[] b = org.apache.commons.codec.binary.Base64.decodeBase64(stringTooLong);
			logo = Image.getInstance(b);
			logo.scaleToFit(90, 90);
			logo.setAlignment(Image.MIDDLE);
			logo.setAlignment(Image.TOP);
			logo.setAlignment(Image.ALIGN_JUSTIFIED);
		} catch (Exception e) {
			throw new ApplicationRuntimeException("Error while loding logo", e);
		}
	}

	public Image getLogo() throws Exception {
		return logo;
	}
	
	public String getServiceType(Plan pl, Map<String, Object> additionalDetails) {
		try {
			 Map<String, String> SERVICE_TYPE = new ConcurrentHashMap<>();
			 SERVICE_TYPE.put("NEW_CONSTRUCTION", "New Construction");
			 SERVICE_TYPE.put("ALTERATION", "Addition and Alteration");
				// handling for subservice--
				if (Objects.nonNull(additionalDetails)
						&& Objects.nonNull(additionalDetails.get(BPA_ADD_DETAILS_SERVICE_KEY))
						&& additionalDetails.get(BPA_ADD_DETAILS_SERVICE_KEY) instanceof Map) {
					SERVICE_TYPE.put("NEW_CONSTRUCTION", "Addition and Alteration");
				}
			return SERVICE_TYPE.get(pl.getPlanInformation().getServiceType());
		}catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}
	
	public String getApplicationTypeForRejection(Map<String, Object> additionalDetails, Plan plan) {
		String applicationType="";
		try {
			 Map<String, String> SERVICE_TYPE = new ConcurrentHashMap<>();
			 SERVICE_TYPE.put("NEW_CONSTRUCTION", "New Construction");
			 SERVICE_TYPE.put("ALTERATION", "Addition and Alteration");
			
			 if(additionalDetails.get("serviceType")!=null) {
				 applicationType= SERVICE_TYPE.get(additionalDetails.get("serviceType"));
			 }
			 else if(plan!=null) {
				 applicationType= SERVICE_TYPE.get(plan.getPlanInformation().getServiceType());
			 }
			 else {
				 applicationType= SERVICE_TYPE.get("NEW_CONSTRUCTION");
			 }
		}catch (Exception e) {
			e.printStackTrace();
		}
		return applicationType;
	}
	
	public String getValue(Map dataMap, String key) {
		String jsonString = new JSONObject(dataMap).toString();
		DocumentContext context = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonString);
		return context.read(key) + "";
	}
	
	public Map<String, Object> getAdditionalDetailsMap(Map bpa) {
		String jsonString = new JSONObject(bpa).toString();
		DocumentContext context = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonString);
		return context.read("additionalDetails");
	}
	
	public Map<String, String> getRejectionMap(Map bpa) {
		String jsonString = new JSONObject(bpa).toString();
		DocumentContext context = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonString);
		return context.read("rejectionDetails");
	}
	
	public List<Map<String, Object>> getPlotAndKhataDetails(Map<String, Object> additionalDetails){
		
		List<Map<String, Object>> plotList = new ArrayList<>();
		
		if(Objects.nonNull(additionalDetails) 
				&& Objects.nonNull(additionalDetails.get(PLOT)) 
				&& additionalDetails.get(PLOT) instanceof List
				&& !CollectionUtils.isEmpty((List) additionalDetails.get(PLOT))){
		
				plotList = (List)additionalDetails.get(PLOT);
		}
		
		return plotList;
		
	}

	public Object fetchPaymentDetails(RequestInfo requestInfo, String consumercode, String tenantId) {
		paymentService.fetchApplicationFeePaymentDetails(requestInfo, consumercode, tenantId);
		paymentService.fetchPermitFeePaymentDetails(requestInfo, consumercode, tenantId);
		return null;
	}
	
	public List<Map<String, Object>> getEstimatedSanctionFeeDetails(RequestInfo requestInfo, LinkedHashMap bpaApplication) {
		Object estimatedFeeDetails = paymentService.fetchEstimatedSanctionFeePayment(requestInfo, bpaApplication);
		return getTaxHeadEstimatesfromPaymentResponse(estimatedFeeDetails);
	}
	
	public List<Map<String, Object>> getEstimatedApplicationFeeDetails(RequestInfo requestInfo, LinkedHashMap bpaApplication) {
		Object estimatedFeeDetails = paymentService.fetchEstimatedApplicationFeePayment(requestInfo, bpaApplication);
		return getTaxHeadEstimatesfromPaymentResponse(estimatedFeeDetails);
	}
	
	
	public List<Map<String, Object>> getTaxHeadEstimatesfromPaymentResponse(Object estimatedFeeDetails) {
		List<Map<String, Object>> taxHeadDetails = new ArrayList<>();
		if (estimatedFeeDetails == null) {
			return taxHeadDetails;
		}
		int calculationsLength = 1;
		if (Objects.nonNull(estimatedFeeDetails) && estimatedFeeDetails instanceof Map
				&& ((Map) estimatedFeeDetails).get("Calculations") instanceof List
				&& !CollectionUtils.isEmpty((List) ((Map) estimatedFeeDetails).get("Calculations"))) {
			List payments = (List) ((Map) estimatedFeeDetails).get("Calculations");
			calculationsLength = payments.size();
		}
		
		String jsonString = new JSONObject((Map) estimatedFeeDetails).toString();
		DocumentContext context = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonString);
		taxHeadDetails = context.read(
				"$.Calculations[" + (calculationsLength - 1) + "].taxHeadEstimates");
		return taxHeadDetails;
	}
	
	public List<Map<String, Object>> getPermitFeeBillAccountDetails(RequestInfo requestInfo, String consumercode,
			String tenantId) {
		Object permitFeePaymentDetails = paymentService.fetchPermitFeePaymentDetails(requestInfo, consumercode,
				tenantId);
		return getBillAccountDetailsFromPaymentResponse(permitFeePaymentDetails);
	}
	
	public List<Map<String, Object>> getBillAccountDetailsFromPaymentResponse(Object permitFeePaymentDetails) {
		List<Map<String, Object>> billAccountDetails = new ArrayList<>();

		int paymentsLength = 1;
		if (Objects.nonNull(permitFeePaymentDetails) && permitFeePaymentDetails instanceof Map
				&& ((Map) permitFeePaymentDetails).get(PAYMENTS_RESPONSE_FIELD) instanceof List
				&& !CollectionUtils.isEmpty((List) ((Map) permitFeePaymentDetails).get(PAYMENTS_RESPONSE_FIELD))) {
			List payments = (List) ((Map) permitFeePaymentDetails).get(PAYMENTS_RESPONSE_FIELD);
			paymentsLength = payments.size();
		}
		String jsonString = new JSONObject((Map) permitFeePaymentDetails).toString();
		DocumentContext context = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonString);
		billAccountDetails = context.read(
				"$.Payments[" + (paymentsLength - 1) + "].paymentDetails[0].bill.billDetails[0].billAccountDetails");
		return billAccountDetails;
	}
	
	public String getFeeComponentNameFromTaxHeadCode(String taxHeadCode) {
		String taxHeadName = "";
		switch (taxHeadCode) {
		case TAXHEAD_BPA_SANC_FEES_CODE:
			taxHeadName = TAXHEAD_BPA_SANC_FEES_NAME;
			break;
		case TAXHEAD_BPA_SANC_TEMP_RETENTION_FEE_CODE:
			taxHeadName = TAXHEAD_BPA_SANC_TEMP_RETENTION_FEE_NAME;
			break;
		case TAXHEAD_BPA_SANC_SECURITY_DEPOSIT_CODE:
			taxHeadName = TAXHEAD_BPA_SANC_SECURITY_DEPOSIT_NAME;
			break;
		case TAXHEAD_BPA_SANC_WORKER_WELFARE_CESS_CODE:
			taxHeadName = TAXHEAD_BPA_SANC_WORKER_WELFARE_CESS_NAME;
			break;
		case TAXHEAD_BPA_SANC_PUR_FAR_CODE:
			taxHeadName = TAXHEAD_BPA_SANC_PUR_FAR_NAME;
			break;
		case TAXHEAD_BPA_SANC_SHELTER_FEE_CODE:
			taxHeadName = TAXHEAD_BPA_SANC_SHELTER_FEE_NAME;
			break;
		case TAXHEAD_BPA_SANC_SANC_FEE_CODE:
			taxHeadName = TAXHEAD_BPA_SANC_SANC_FEE_NAME;
			break;
		case TAXHEAD_BPA_SANC_EIDP_FEE_CODE:
			taxHeadName = TAXHEAD_BPA_SANC_EIDP_FEE_NAME;
			break;
		case TAXHEAD_BPA_SANC_ADJUSTMENT_AMOUNT_CODE:
			taxHeadName = TAXHEAD_BPA_SANC_ADJUSTMENT_AMOUNT_NAME;
			break;
		case TAXHEAD_BPA_BLDNG_OPRN_FEE_REWORK_ADJUSTMENT_CODE:
			taxHeadName = TAXHEAD_BPA_BLDNG_OPRN_FEE_REWORK_ADJUSTMENT_NAME;
			break;
		case TAXHEAD_BPA_LAND_DEV_FEE_REWORK_ADJUSTMENT_CODE:
			taxHeadName = TAXHEAD_BPA_LAND_DEV_FEE_REWORK_ADJUSTMENT_NAME;
			break;
		case TAXHEAD_BPA_LAND_DEV_FEE_CODE:
			taxHeadName = TAXHEAD_BPA_LAND_DEV_FEE_NAME;
			break;
		case TAXHEAD_BPA_BLDNG_OPRN_FEE_CODE:
			taxHeadName = TAXHEAD_BPA_BLDNG_OPRN_FEE_NAME;
			break;						
		default:
			taxHeadName = taxHeadCode;
		}
		return taxHeadName;
	}
	
	public String getFeeComponentNameFromTaxHeadCodeForPaymentTable(String taxHeadCode) {
		//these are BDA provided format display names in their payment table
		switch (taxHeadCode) {
		case TAXHEAD_BPA_SANC_SANC_FEE_CODE:
			return "Sanction fees";
		case TAXHEAD_BPA_SANC_WORKER_WELFARE_CESS_CODE:
			return "Construction worker welfare Cess (CWWC)";
		case TAXHEAD_BPA_SANC_SECURITY_DEPOSIT_CODE:
			return "Security Deposit Fees";
		case TAXHEAD_BPA_SANC_TEMP_RETENTION_FEE_CODE:
			return "Temporary Retention Fee";
		case TAXHEAD_BPA_SANC_SHELTER_FEE_CODE:
			return "Shelter Fees for mandatory 10% EWS Housing (carpet area) @ 25% of construction cost of EWS housing ";
		case TAXHEAD_BPA_SANC_PUR_FAR_CODE:
			return "Charges for Purchasable FAR Area";
		case TAXHEAD_BPA_SANC_EIDP_FEE_CODE:
			return "EIDP Fees ";
		case TAXHEAD_BPA_SANC_ADJUSTMENT_AMOUNT_CODE:
			return "Other Fee";
		default:
			return "";
		}
	}
	
	public Map<String, String> getAllFeeDetailsMap(LinkedHashMap bpaApplication, RequestInfo requestInfo,
			String consumercode, String tenantId) {

		Map<String, String> paymentDetailsMap = new HashMap<>();
		Object permitFeePaymentDetails = paymentService.fetchPermitFeePaymentDetails(requestInfo, consumercode,
				tenantId);
		int PermitFeePaymentsLength = 1;
		if (Objects.nonNull(permitFeePaymentDetails) && permitFeePaymentDetails instanceof Map
				&& ((Map) permitFeePaymentDetails).get(PAYMENTS_RESPONSE_FIELD) instanceof List
				&& !CollectionUtils.isEmpty((List) ((Map) permitFeePaymentDetails).get(PAYMENTS_RESPONSE_FIELD))) {
			List payments = (List) ((Map) permitFeePaymentDetails).get(PAYMENTS_RESPONSE_FIELD);
			PermitFeePaymentsLength = payments.size();
		}
		List<Map<String, Object>> permitFeeBillAccountDetails = getBillAccountDetailsFromPaymentResponse(
				permitFeePaymentDetails);
		for (Map<String, Object> billAccountDetail : permitFeeBillAccountDetails) {
			String adjustedAmount = String.valueOf(billAccountDetail.get("adjustedAmount"));
			String taxHeadCode = String.valueOf(billAccountDetail.get("taxHeadCode"));
			paymentDetailsMap.put(taxHeadCode, adjustedAmount);
			if (TAXHEAD_BPA_SANC_ADJUSTMENT_AMOUNT_CODE.equalsIgnoreCase(taxHeadCode)
					&& !ADJUSTED_AMOUNT_ZERO.equals(adjustedAmount)) {	
				paymentDetailsMap.put("modificationReasonSanctionFeeAdjustmentAmount",
					getValue(bpaApplication, "$.additionalDetails.modificationReasonSanctionFeeAdjustmentAmount"));
			}
		}
		String totalPermitFeeAmountPaid = getValue((Map) permitFeePaymentDetails,
				"$.Payments[" + (PermitFeePaymentsLength - 1) + "].paymentDetails[0].totalAmountPaid");
		paymentDetailsMap.put("totalPermitFeeAmountPaid", totalPermitFeeAmountPaid);

		Object applicationFeePaymentDetails = paymentService.fetchApplicationFeePaymentDetails(requestInfo,
				consumercode, tenantId);
		int applicationFeePaymentsLength = 1;
		if (Objects.nonNull(applicationFeePaymentDetails) && applicationFeePaymentDetails instanceof Map
				&& ((Map) applicationFeePaymentDetails).get(PAYMENTS_RESPONSE_FIELD) instanceof List
				&& !CollectionUtils.isEmpty((List) ((Map) applicationFeePaymentDetails).get(PAYMENTS_RESPONSE_FIELD))) {
			List payments = (List) ((Map) applicationFeePaymentDetails).get(PAYMENTS_RESPONSE_FIELD);
			applicationFeePaymentsLength = payments.size();
		}
		List<Map<String, Object>> applicationFeeBillAccountDetails = getBillAccountDetailsFromPaymentResponse(
				applicationFeePaymentDetails);
		for (Map<String, Object> billAccountDetail : applicationFeeBillAccountDetails) {
			String adjustedAmount = String.valueOf(billAccountDetail.get("adjustedAmount"));
			String taxHeadCode = String.valueOf(billAccountDetail.get("taxHeadCode"));
			paymentDetailsMap.put(taxHeadCode, adjustedAmount);
		}
		String totalApplicationFeeAmountPaid = getValue((Map) applicationFeePaymentDetails,
				"$.Payments[" + (applicationFeePaymentsLength - 1) + "].paymentDetails[0].totalAmountPaid");
		paymentDetailsMap.put("totalApplicationFeeAmountPaid", totalApplicationFeeAmountPaid);

		BigDecimal totalApplicationAndPermitFee = new BigDecimal(totalPermitFeeAmountPaid)
				.add(new BigDecimal(totalApplicationFeeAmountPaid)).setScale(2, BigDecimal.ROUND_HALF_UP);
		paymentDetailsMap.put("totalApplicationAndPermitFee", totalApplicationAndPermitFee + "");
		return paymentDetailsMap;
	}

	public String[] getUlbNameAndGradeFromMdms(RequestInfo requestInfo, String tenantId) {
		return mdmsService.getUlbNameAndGradeFromMdms(requestInfo, tenantId);
	}
	
//	public String getBpaPermitHeader(RequestInfo requestInfo, String tenantId) {
//		return mdmsService.getBpaPermitHeader(requestInfo, tenantId);
//	}

	public Set<String> getNocsList(RequestInfo requestInfo, String tenantId, String bpaApplicationNo) {
		Object nocResponse = nocService.fetchNocs(requestInfo, tenantId, bpaApplicationNo);
		Set<String> nocviewableNames = new HashSet<>();
		if (Objects.nonNull(nocResponse) && nocResponse instanceof Map && ((Map) nocResponse).get("Noc") instanceof List
				&& !CollectionUtils.isEmpty((List) ((Map) nocResponse).get("Noc"))) {
			List nocs = (List) ((Map) nocResponse).get("Noc");
			Set<String> nocNames = (Set<String>) nocs.stream().map(noc -> String.valueOf(((Map) noc).get("nocType")))
					.collect(Collectors.toSet());
			for (String nocName : nocNames) {
				switch (nocName) {
				case "FIRE_NOC":
					nocviewableNames.add("Fire recommendations from Fire Prevention Wing");
					break;
				case "NMA_NOC":
					nocviewableNames.add("NOC from National Monuments Authority");
					break;
				case "AAI_NOC":
					nocviewableNames.add("NOC from Airports Authority of India");
					break;
				default:
					nocviewableNames.add(nocName);
				}
			}
		}
		return nocviewableNames;
	}

	private List<AdditionalReportDetail> buildAdditionalReport(Plan plan) {

		List<AdditionalReportDetail> additionalReportDetails = new ArrayList<>();

		AdditionalReportDetail additionalReportDetail1 = new AdditionalReportDetail();
		additionalReportDetail1.setDescription("No.of staircases");
		additionalReportDetail1.setNoDescription("10");
		additionalReportDetail1.setNoOfItems(new BigDecimal(10));
		AdditionalReportDetail additionalReportDetail2 = new AdditionalReportDetail();
		additionalReportDetail2.setDescription("No.of Lifts");
		additionalReportDetail2.setNoDescription("5");
		additionalReportDetail2.setNoOfItems(new BigDecimal(5));
		AdditionalReportDetail additionalReportDetail3 = new AdditionalReportDetail();
		additionalReportDetail3.setDescription("E-vehicle charging station(30%)");
		additionalReportDetail3.setNoDescription("30% of total car parks provided ");
		additionalReportDetail3.setNoOfItems(new BigDecimal(45));
		AdditionalReportDetail additionalReportDetail4 = new AdditionalReportDetail();
		additionalReportDetail4.setDescription("Visitor parking(20% of required car parking)");
		additionalReportDetail4.setNoDescription("29.4");
		additionalReportDetail4.setNoOfItems(new BigDecimal(30));
		AdditionalReportDetail additionalReportDetail5 = new AdditionalReportDetail();
		additionalReportDetail5.setDescription("Plantation(1no of tree per 80Sqm.)");
		additionalReportDetail5.setNoDescription("91");
		additionalReportDetail5.setNoOfItems(new BigDecimal(93));

		additionalReportDetails.add(additionalReportDetail1);
		additionalReportDetails.add(additionalReportDetail2);
		additionalReportDetails.add(additionalReportDetail3);
		additionalReportDetails.add(additionalReportDetail4);
		additionalReportDetails.add(additionalReportDetail5);

		return additionalReportDetails;

	}

	public List<DcrReportBlockDetail> buildBlockWiseExistingInfo(Plan plan) {
		List<DcrReportBlockDetail> dcrReportBlockDetails = new ArrayList<>();

		// List<Block> blocks = plan.getBlocks();
		List<Block> totalBlocksInPlan = new ArrayList<>();
		totalBlocksInPlan.addAll(plan.getBlocks());
		totalBlocksInPlan.addAll(plan.getOuthouse());
		if (!totalBlocksInPlan.isEmpty()) {

			for (Block block : totalBlocksInPlan) {
				Building building = block.getBuilding();
				if (building != null && building.getTotalExistingBuiltUpArea() != null
						&& building.getTotalExistingBuiltUpArea().compareTo(BigDecimal.ZERO) > 0
						&& building.getTotalExistingFloorArea().compareTo(BigDecimal.ZERO) > 0) {
					DcrReportBlockDetail dcrReportBlockDetail = new DcrReportBlockDetail();
					dcrReportBlockDetail.setBlockNo(block.getNumber());

					List<Floor> floors = building.getFloors();

					if (!floors.isEmpty()) {
						List<DcrReportFloorDetail> dcrReportFloorDetails = new ArrayList<>();
						for (Floor floor : floors) {

							List<Occupancy> occupancies = floor.getOccupancies();

							if (!occupancies.isEmpty()) {

								for (Occupancy occupancy : occupancies) {

									DcrReportFloorDetail dcrReportFloorDetail = new DcrReportFloorDetail();

									String floorNo;
									if (floor.getTerrace())
										floorNo = "Terrace";
									else if (occupancy.getIsMezzanine())
										floorNo = FloorNumberToWord.floorName(floor.getNumber(),
												floor.getIsStiltFloor(), floor.getIsServiceFloor()) + " (Mezzanine "
												+ floor.getNumber() + ")";
									else
										floorNo = FloorNumberToWord.floorName(floor.getNumber(),
												floor.getIsStiltFloor(), floor.getIsServiceFloor());
									dcrReportFloorDetail.setFloorNo(floorNo);

									String occupancyName = "";
									if (occupancy.getTypeHelper() != null)
										if (occupancy.getTypeHelper().getSubtype() != null)
											occupancyName = occupancy.getTypeHelper().getSubtype().getName();
										else if (occupancy.getTypeHelper().getType() != null)
											occupancyName = occupancy.getTypeHelper().getType().getName();
									if (occupancy != null
											&& occupancy.getExistingBuiltUpArea().compareTo(BigDecimal.ZERO) > 0) {
										dcrReportFloorDetail.setOccupancy(occupancyName);
										// dcrReportFloorDetail.setBuiltUpArea(occupancy.getExistingBuiltUpArea());
										dcrReportFloorDetail.setFloorArea(occupancy.getExistingFloorArea());
										dcrReportFloorDetail.setCarpetArea(occupancy.getExistingCarpetArea());

										dcrReportFloorDetail.setExistingBuiltUpArea(occupancy.getExistingBuiltUpArea());

										dcrReportFloorDetails.add(dcrReportFloorDetail);
									}
								}

							}

						}
						dcrReportFloorDetails = dcrReportFloorDetails.stream()
								.sorted(Comparator.comparing(DcrReportFloorDetail::getFloorNumberInteger))
								.collect(Collectors.toList());

						dcrReportBlockDetail.setDcrReportFloorDetails(dcrReportFloorDetails);
					}
					dcrReportBlockDetails.add(dcrReportBlockDetail);
				}

			}

		}
		return dcrReportBlockDetails;
	}

	public List<DcrReportBlockDetail> buildBlockWiseProposedInfo(Plan plan) {
		List<DcrReportBlockDetail> dcrReportBlockDetails = new ArrayList<>();
		AdditionalFeature additionalFeature = new AdditionalFeature();
//		List<Block> blocks = plan.getBlocks();
		List<Block> totalBlocksInPlan = new ArrayList<>();
		totalBlocksInPlan.addAll(plan.getBlocks());
		totalBlocksInPlan.addAll(plan.getOuthouse());

		if (!totalBlocksInPlan.isEmpty()) {

			for (Block block : totalBlocksInPlan) {

				Building building = block.getBuilding();
				if (building != null) {
					DcrReportBlockDetail dcrReportBlockDetail = new DcrReportBlockDetail();
					String noOfFloor = additionalFeature.getNoOfFloor(block);
					// TODO: NA to be replaced by sub occupancy of that block-
					dcrReportBlockDetail.setBlockNo(block.getNumber());
					// dcrReportBlockDetail.setBlockNo(block.getNumber() + " (" + noOfFloor + "
					// NA)");//raza
					dcrReportBlockDetail.setCoverageArea(building.getCoverageArea());
					dcrReportBlockDetail.setBuildingHeight(building.getBuildingHeight());
					dcrReportBlockDetail.setDeclaredBuildingHeight(building.getDeclaredBuildingHeight());
					dcrReportBlockDetail.setConstructedArea(building.getTotalConstructedArea());
					List<Floor> floors = building.getFloors();

					if (!floors.isEmpty()) {
						List<DcrReportFloorDetail> dcrReportFloorDetails = new ArrayList<>();
						for (Floor floor : floors) {

							List<Occupancy> occupancies = floor.getOccupancies();

							if (!occupancies.isEmpty()) {

								for (Occupancy occupancy : occupancies) {
									String occupancyName = "";
									if (occupancy.getTypeHelper() != null)
										if (occupancy.getTypeHelper().getSubtype() != null)
											occupancyName = occupancy.getTypeHelper().getSubtype().getName();
										else {
											if (occupancy.getTypeHelper().getType() != null)
												occupancyName = occupancy.getTypeHelper().getType().getName();
										}
									DcrReportFloorDetail dcrReportFloorDetail = new DcrReportFloorDetail();
									String floorNo;
									dcrReportFloorDetail.setFloorNumberInteger(floor.getNumber());
									if (floor.getTerrace())
										floorNo = "Terrace";
									else if (occupancy.getIsMezzanine())
										floorNo = FloorNumberToWord.floorName(floor.getNumber(),
												floor.getIsStiltFloor(), floor.getIsServiceFloor()) + " (Mezzanine "
												+ floor.getNumber() + ")";
									else
										floorNo = FloorNumberToWord.floorName(floor.getNumber(),
												floor.getIsStiltFloor(), floor.getIsServiceFloor());
									dcrReportFloorDetail.setFloorNo(floorNo);
									dcrReportFloorDetail.setOccupancy(occupancyName);
									dcrReportFloorDetail.setBuiltUpArea(
											occupancy.getBuiltUpArea().setScale(DcrConstants.DECIMALDIGITS_MEASUREMENTS,
													DcrConstants.ROUNDMODE_MEASUREMENTS));

									dcrReportFloorDetail.setFloorArea(
											occupancy.getExistingFloorArea().compareTo(BigDecimal.ZERO) > 0
													? occupancy.getFloorArea()
															.subtract(occupancy.getExistingFloorArea())
															.subtract(occupancy.getFarAreaDeductExisting())
															.setScale(DcrConstants.DECIMALDIGITS_MEASUREMENTS,
																	DcrConstants.ROUNDMODE_MEASUREMENTS)
													: occupancy.getFloorArea().setScale(
															DcrConstants.DECIMALDIGITS_MEASUREMENTS,
															DcrConstants.ROUNDMODE_MEASUREMENTS));
									dcrReportFloorDetail.setCarpetArea(
											occupancy.getExistingCarpetArea().compareTo(BigDecimal.ZERO) > 0
													? occupancy.getCarpetArea()
															.subtract(occupancy.getExistingCarpetArea())
															.setScale(DcrConstants.DECIMALDIGITS_MEASUREMENTS,
																	DcrConstants.ROUNDMODE_MEASUREMENTS)
													: occupancy.getCarpetArea().setScale(
															DcrConstants.DECIMALDIGITS_MEASUREMENTS,
															DcrConstants.ROUNDMODE_MEASUREMENTS));

									dcrReportFloorDetail.setExistingBuiltUpArea(occupancy.getExistingBuiltUpArea()
											.setScale(DcrConstants.DECIMALDIGITS_MEASUREMENTS,
													DcrConstants.ROUNDMODE_MEASUREMENTS));

									if (dcrReportFloorDetail.getBuiltUpArea().compareTo(BigDecimal.ZERO) > 0
											|| dcrReportFloorDetail.getExistingBuiltUpArea().compareTo(BigDecimal.ZERO) > 0) {
										dcrReportFloorDetails.add(dcrReportFloorDetail);

									}

								}

							}

						}
						dcrReportFloorDetails = dcrReportFloorDetails.stream()
								.sorted(Comparator.comparing(DcrReportFloorDetail::getFloorNumberInteger))
								.collect(Collectors.toList());

						dcrReportBlockDetail.setDcrReportFloorDetails(dcrReportFloorDetails);
					}
					dcrReportBlockDetails.add(dcrReportBlockDetail);
				}

			}

		}
		return dcrReportBlockDetails;
	}

	public Image getQrCode(String ownersCsv, String permitNo, String approvalDate, String edcrNo) {
		String qrCodeInformation = "Applicant Name: %s, Permit Order Number : %s, Permit Order Date : %s, eDCR Scrutiny Number: %s";
		qrCodeInformation = String.format(qrCodeInformation, ownersCsv, permitNo, approvalDate, edcrNo);
		BarcodeQRCode qrCode = new BarcodeQRCode(qrCodeInformation, 1, 1, null);
		Image codeQrImage = null;
		try {
			codeQrImage = qrCode.getImage();
		} catch (BadElementException e) {
			LOG.error("BadElementException while generating qr code image", e);
		}
		codeQrImage.scaleToFit(90, 90);
		codeQrImage.setAlignment(Image.MIDDLE);
		codeQrImage.setAlignment(Image.TOP);
		codeQrImage.setAlignment(Image.ALIGN_JUSTIFIED);
		return codeQrImage;
	}

	public String getTotalPlotAreaValueV2(Plan pl) {
		// " - Total plot area: Ac1.830Dec. (" + plotArea + " Sqm.)\n", fontPara1Bold
		StringBuilder result = new StringBuilder(" - Total plot area: ");
		BigDecimal totalPlotArea = pl.getPlanInformation().getTotalPlotArea();
		if(totalPlotArea == null || totalPlotArea.compareTo(BigDecimal.ZERO)<=0)
			totalPlotArea = pl.getPlot().getPlotBndryArea();
		BigDecimal totalPlotAreaInAcr = totalPlotArea.divide(new BigDecimal("4046.2"), 3, BigDecimal.ROUND_HALF_UP);
		result.append(totalPlotAreaInAcr + " Acre ( " + totalPlotArea + SQM + " ) ");
		return result.toString();
	}

	public List<Chunk> getTotalCDPRoadAffectedArea(Plan pl) {
		List<Chunk> affectedAreas = new ArrayList<>();
		Font fontPara1Bold = FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD);
		for (org.egov.common.entity.edcr.AffectedLandArea affectedLandArea : pl.getAffectedLandAreas()) {
			// affected area
			if (affectedLandArea.getMeasurements() != null && !affectedLandArea.getMeasurements().isEmpty()) {
				BigDecimal area = affectedLandArea.getMeasurements().stream().map(l -> l.getArea())
						.reduce(BigDecimal::add).orElse(BigDecimal.ZERO).setScale(2, BigDecimal.ROUND_HALF_UP);
				Chunk chunk = new Chunk(
						" - " + affectedLandArea.getName() + " affected area: " + area + SQM + "\n",
						fontPara1Bold);
				affectedAreas.add(chunk);
			}
		}
		return affectedAreas;
	}

	public String getRoadAffectedArea(Plan pl) {
		String roadAffectedArea = BigDecimal.ZERO + "";
		try {
			for (org.egov.common.entity.edcr.AffectedLandArea affectedLandArea : pl.getAffectedLandAreas()) {
				// affected area
				if (affectedLandArea.getMeasurements() != null && !affectedLandArea.getMeasurements().isEmpty()) {
					BigDecimal area = affectedLandArea.getMeasurements().stream().map(l -> l.getArea())
							.reduce(BigDecimal::add).orElse(BigDecimal.ZERO).setScale(2, BigDecimal.ROUND_HALF_UP);
					roadAffectedArea = area + "";
				}
			}
		} catch (Exception ex) {
			LOG.error("error while extracting road affected area for permit letter");
		}
		return roadAffectedArea;
	}
	
	public BigDecimal getGiftedArea(Plan pl) {
		return pl.getPlot().getPlotBndryDeductionArea();
	}
	
	public static Map<String, BigDecimal> getSetBackData(Plan plan, Block block) {
		SetBack setBack = block.getSetBacks().get(0);

		// these are provided setbacks-
		BigDecimal frontSetbackProvided = BigDecimal.ZERO;
		BigDecimal rearSetbackProvided = BigDecimal.ZERO;
		BigDecimal leftSetbackProvided = BigDecimal.ZERO;
		BigDecimal rightSetbackProvided = BigDecimal.ZERO;

		if (setBack != null) {
			
			frontSetbackProvided = (setBack.getFrontYardOverride() != null)
			        ? setBack.getFrontYardOverride().getMinimumDistance()
			        : (setBack.getFrontYard() != null ? setBack.getFrontYard().getMinimumDistance() : BigDecimal.ZERO);

			rearSetbackProvided = (setBack.getRearYardOverride() != null)
			        ? setBack.getRearYardOverride().getMinimumDistance()
			        : (setBack.getRearYard() != null ? setBack.getRearYard().getMinimumDistance() : BigDecimal.ZERO);
			
			leftSetbackProvided = (setBack.getSideYard1Override() != null)
			        ? setBack.getSideYard1Override().getMinimumDistance()
			        : (setBack.getSideYard1() != null ? setBack.getSideYard1().getMinimumDistance() : BigDecimal.ZERO);
			
			
			rightSetbackProvided = (setBack.getSideYard2Override() != null)
			        ? setBack.getSideYard2Override().getMinimumDistance()
			        : (setBack.getSideYard2() != null ? setBack.getSideYard2().getMinimumDistance() : BigDecimal.ZERO);
		}

		Map<String, BigDecimal> setBackData = new HashMap<>();
		setBackData.put("frontSetbackProvided", frontSetbackProvided);
		setBackData.put("rearSetbackProvided", rearSetbackProvided);
		setBackData.put("leftSetbackProvided", leftSetbackProvided);
		setBackData.put("rightSetbackProvided", rightSetbackProvided);

		// these are required setbacks-
		BigDecimal frontSetbackRequired = BigDecimal.ZERO;
		BigDecimal rearSetbackRequired = BigDecimal.ZERO;
		BigDecimal leftSetbackRequired = BigDecimal.ZERO;
		BigDecimal rightSetbackRequired = BigDecimal.ZERO;
		setBackData.put("frontSetbackRequired", frontSetbackRequired);
		setBackData.put("rearSetbackRequired", rearSetbackRequired);
		setBackData.put("leftSetbackRequired", leftSetbackRequired);
		setBackData.put("rightSetbackRequired", rightSetbackRequired);
		return setBackData;
	}
	
	public String getStairCount(Plan pl) {
		String count = DxfFileConstants.NA;
		//TODO required stair
		StringBuilder requiredStairCount = new StringBuilder();
		StringBuilder stairDetail = new StringBuilder();
		try {
			for (Block block : pl.getBlocks()) {
				Optional<Integer> maxStairCount = block.getBuilding().getFloors().stream()
						.map(floor -> floor.getGeneralStairs() == null ? 0 : floor.getGeneralStairs().size())
						.reduce(Integer::max);
				if (maxStairCount.isPresent())
					stairDetail.append( ", "+"B"+block.getName() + "-" + maxStairCount.get());
			}
		} catch (Exception ex) {
			LOG.error("error while extracting the stair count for permit letter", ex);
		}
		if (!stairDetail.toString().isEmpty()) {
			count = stairDetail.toString();
			count = count.replaceFirst(", ", "");
		}
		return count;
	}
	
	public String getRequiredStairCount(Plan plan) {
		String count = DxfFileConstants.NA;
		StringBuilder requiredStairs = new StringBuilder();
		try {
			for (Block block : plan.getBlocks()) {
				int requiredStair = org.egov.edcr.feature.GeneralStair.requiredGenralStairPerFloor(plan, block);
				requiredStairs.append(", B" + block.getName() + "-" + requiredStair);
			}
			if (!requiredStairs.toString().isEmpty())
				count = requiredStairs.toString().replaceFirst(", ", "");
		} catch (Exception ex) {
			LOG.error("error while extracting required no of stairs for permit letter", ex);
		}
		return count;
	}

	public Map<String, String> getLiftDetails(Plan plan) {
	    String defaultDetail = DxfFileConstants.NA;
	    StringBuilder providedLiftDetail = new StringBuilder();
	    StringBuilder requiredLiftDetail = new StringBuilder();
	    Map<String, String> liftDetail = new HashMap<>();

	    try {
	        for (Block block : plan.getBlocks()) {
	            int maxProvidedLifts = 0;
	            int totalRequiredLifts = calculateRequiredLifts(plan, block);

	            for (Floor floor : block.getBuilding().getFloors()) {
	                int floorLiftCount = getLifts(floor, 1).size()
	                                   + getLifts(floor, 2).size();
	                maxProvidedLifts = Math.max(maxProvidedLifts, floorLiftCount);
	            }

	            if (block.getBuilding().getFloors().size() >= 2) {
	                Floor lastFloor = block.getBuilding().getFloors()
	                                        .get(block.getBuilding().getFloors().size() - 1);
	                Floor secondLastFloor = block.getBuilding().getFloors()
	                                             .get(block.getBuilding().getFloors().size() - 2);

	                BigDecimal lastFloorBuiltUpArea = lastFloor.getOccupancies().stream()
	                	    .map(occupancy -> occupancy.getBuiltUpArea().compareTo(BigDecimal.ZERO) > 0 
	                	        ? occupancy.getBuiltUpArea() 
	                	        : occupancy.getExistingBuiltUpArea())
	                	    .reduce(BigDecimal.ZERO, BigDecimal::add);

	                	BigDecimal secondLastFloorBuiltUpArea = secondLastFloor.getOccupancies().stream()
	                	    .map(occupancy -> occupancy.getBuiltUpArea().compareTo(BigDecimal.ZERO) > 0 
	                	        ? occupancy.getBuiltUpArea() 
	                	        : occupancy.getExistingBuiltUpArea())
	                	    .reduce(BigDecimal.ZERO, BigDecimal::add);

	                if (lastFloorBuiltUpArea.compareTo(secondLastFloorBuiltUpArea) < 0) {
	                    // If the last floor has less built-up area, required lifts should be NA
	                    totalRequiredLifts = 0;
	                }
	                
	                if (plan.getPlanInformation().getLiftCountRelaxation() != null 
	                	    && plan.getPlanInformation().getLiftCountRelaxation().equalsIgnoreCase("YES")) {
	                	totalRequiredLifts = 0; 
	                }
	            }

	            if (maxProvidedLifts > 0) {
	                providedLiftDetail.append(", ").append("B").append(block.getName()).append("-").append(maxProvidedLifts);
	            }
	            if (totalRequiredLifts > 0) {
	                requiredLiftDetail.append(", ").append("B").append(block.getName()).append("-").append(totalRequiredLifts);
	            } else {
	                requiredLiftDetail.append(", ").append("B").append(block.getName()).append("-").append(defaultDetail);
	            }
	        }
	    } catch (Exception ex) {
	        LOG.error("Error while extracting lift details for permit letter", ex);
	    }

	    liftDetail.put(PROVIDED_LIFT_DETAIL,
	        providedLiftDetail.length() > 0 
	            ? providedLiftDetail.substring(2) 
	            : defaultDetail);
	    liftDetail.put(REQUIRED_LIFT_DETAIL,
	        requiredLiftDetail.length() > 0 
	            ? requiredLiftDetail.substring(2) 
	            : defaultDetail);

	    return liftDetail;
	}


	public static List<Lift> getLifts(Floor floor, int colorCode) {
		List<Lift> lifts = new ArrayList<>();

		for (Lift lift : floor.getLifts()) {
			Measurement measurement = lift.getLifts().get(0);
			if (measurement.getColorCode() == colorCode) {
				lifts.add(lift);
			}
		}

		return lifts;
	}
	
	private int calculateRequiredLifts(Plan plan, Block block) {
	    OccupancyTypeHelper typeHelper = plan.getVirtualBuilding().getMostRestrictiveFarHelper();
	    BigDecimal buildingHeight = block.getBuilding().getBuildingHeight();
	    int requiredCount = 0;

	   
	    if (Arrays.asList(
	            DxfFileConstants.PLOTTED_DETACHED_OR_INDIVIDUAL_RESIDENTIAL_BUILDING,
	            DxfFileConstants.SEMI_DETACHED,
	            DxfFileConstants.ROW_HOUSING,
	            DxfFileConstants.DHARMASALA,
	            DxfFileConstants.DORMITORY,
	            DxfFileConstants.HOSTEL,
	            DxfFileConstants.SHELTER_HOUSE,
	            DxfFileConstants.STAFF_QAURTER).contains(typeHelper.getSubtype().getCode())) {

	        requiredCount = calculateRequiredCountForResidential(block, buildingHeight);

	    } else if (Arrays.asList(
	            DxfFileConstants.APARTMENT_BUILDING,
	            DxfFileConstants.HOUSING_PROJECT,
	            DxfFileConstants.STUDIO_APARTMENTS,
	            DxfFileConstants.MEDIUM_INCOME_HOUSING).contains(typeHelper.getSubtype().getCode())) {

	        requiredCount = calculateRequiredCountForApartments(block, buildingHeight);

	    } else if (Arrays.asList(DxfFileConstants.EWS, DxfFileConstants.LOW_INCOME_HOUSING).contains(typeHelper.getSubtype().getCode())) {

	        requiredCount = calculateRequiredCountForEWS(block, buildingHeight);

	    } else if (!DxfFileConstants.OC_RESIDENTIAL.equals(typeHelper.getType().getCode())) {
	        requiredCount = calculateRequiredCountForNonResidential(block, buildingHeight);
	    }

	    return requiredCount;
	}
	
	private int calculateRequiredCountForResidential(Block block, BigDecimal buildingHeight) {
	    int requiredCount = buildingHeight.compareTo(new BigDecimal("21")) >= 0 ? 2 : 0;
	    int dwellingUnitsAbove2Floors = getTotalDUAbove2Floor(block);
	    int duRequired = dwellingUnitsAbove2Floors / 20 + (dwellingUnitsAbove2Floors % 20 != 0 ? 1 : 0);
	    return Math.max(requiredCount, duRequired);
	}

	private int calculateRequiredCountForApartments(Block block, BigDecimal buildingHeight) {
	    int requiredCount = buildingHeight.compareTo(new BigDecimal("10")) > 0 ? 1 : 0;
	    if (buildingHeight.compareTo(new BigDecimal("21")) >= 0) {
	        requiredCount = 2;
	    }
	    int dwellingUnitsAbove2Floors = getTotalDUAbove2Floor(block);
	    int duRequired = dwellingUnitsAbove2Floors / 20 + (dwellingUnitsAbove2Floors % 20 != 0 ? 1 : 0);
	    return Math.max(requiredCount, duRequired);
	}

	private int calculateRequiredCountForEWS(Block block, BigDecimal buildingHeight) {
	    int requiredCount = 0;
	    if (buildingHeight.compareTo(new BigDecimal("15")) > 0) {
	        requiredCount = 1;
	        if (buildingHeight.compareTo(new BigDecimal("21")) >= 0) {
	            requiredCount = 2;
	        }
	    }
	    return requiredCount;
	}

	private int calculateRequiredCountForNonResidential(Block block, BigDecimal buildingHeight) {
	    int requiredCount = buildingHeight.compareTo(new BigDecimal("10")) > 0 ? 1 : 0;
	    if (buildingHeight.compareTo(new BigDecimal("21")) >= 0) {
	        requiredCount = 2;
	    }
	    return requiredCount;
	}
	
	private int getTotalDUAbove2Floor(Block block) {
		int count = 0;
		for (Floor floor : block.getBuilding().getFloors()) {
			if (floor.getNumber() > 2) {
				count = count + floor.getEwsUnit().size() + floor.getLigUnit().size() + floor.getMig1Unit().size()
						+ floor.getMig2Unit().size() + floor.getOthersUnit().size() + floor.getRoomUnit().size();
			}
		}
		return count;
	}
	
	public String getNoOfTreesRequired(Plan plan) {
		int totalRequiredCountAsPerArea = 0;
		
		if (plan.getPlot().getArea().compareTo(new BigDecimal("115")) > 0) {
			totalRequiredCountAsPerArea = plan.getPlot().getArea()
					.divide(new BigDecimal("80"), DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS).intValue();

		}
		
		return totalRequiredCountAsPerArea!=0?totalRequiredCountAsPerArea+"":DxfFileConstants.NA;
	}

	public String getNoOfTreesProvided(Plan plan) {
		String noOfTreesProvided = DxfFileConstants.NA;
		try {
			int noOfCutTree = plan.getPlantation().getCutTreeCount();
			int noOfExistingTree = plan.getPlantation().getExistingTreeCount();
			int noOfPlantedTree = plan.getPlantation().getPlantedTreeCount();
			noOfTreesProvided = noOfExistingTree - noOfCutTree + noOfPlantedTree + "";
		} catch (Exception ex) {
			LOG.error("error while extracting no of trees provided for permit letter", ex);
		}
		return noOfTreesProvided;
	}
	
	public String getHeight(Plan plan) {
		String height = DxfFileConstants.NA;
		StringBuilder heights = new StringBuilder();
		try {
			for (Block block : plan.getBlocks()) {
				heights.append(", B" + block.getName() + "-" + block.getBuilding().getBuildingHeight());
			}
			if (!heights.toString().isEmpty())
				height = heights.toString().replaceFirst(", ", "");
		} catch (Exception ex) {
			LOG.error("error while extracting height og blocks for permit letter", ex);
		}
		return height;
	}
	
	public String getCorrespondenceAddress(LinkedHashMap bpaApplication) {
		String correspondenceAddress = "";
		try {
			String ownershipMajorType = getOwnershipMajorType(bpaApplication);
			String jsonPathForCorrespondenceAddress = "";
			if (StringUtils.isNotEmpty(ownershipMajorType)) {
				switch (ownershipMajorType) {
				case OWNERSHIP_MAJOR_TYPE_INSTITUTIONAL_PRIVATE:
					// only one owner allowed from UI-
					jsonPathForCorrespondenceAddress = "$.landInfo.owners[0].correspondenceAddress";
					break;
				case OWNERSHIP_MAJOR_TYPE_INDIVIDUAL:
					jsonPathForCorrespondenceAddress = "$.landInfo.owners[?(@.isPrimaryOwner==true)].correspondenceAddress";
					break;
				case OWNERSHIP_MAJOR_TYPE_INSTITUTIONAL_GOVERNMENT:
					// only one owner allowed from UI-
					jsonPathForCorrespondenceAddress = "$.landInfo.owners[0].correspondenceAddress";
					break;
				default:
					//LOG.info("unsupported ownershipMajorType:" + ownershipMajorType);
				}
			} else {
				String ownershipCategory = getOwnershipCategory(bpaApplication);
				if (ownershipCategory.contains(OWNERSHIP_MAJOR_TYPE_INSTITUTIONAL_PRIVATE))
					jsonPathForCorrespondenceAddress = "$.landInfo.owners[0].correspondenceAddress";
				else if (ownershipCategory.contains(OWNERSHIP_MAJOR_TYPE_INDIVIDUAL))
					jsonPathForCorrespondenceAddress = "$.landInfo.owners[?(@.isPrimaryOwner==true)].correspondenceAddress";
				else if (ownershipCategory.contains(OWNERSHIP_MAJOR_TYPE_INSTITUTIONAL_GOVERNMENT))
					jsonPathForCorrespondenceAddress = "$.landInfo.owners[0].correspondenceAddress";
//				else
//					LOG.info("unsupported ownershipCategory:" + ownershipCategory);
			}
			//LOG.info("jsonPathForCorrespondenceAddress in getCorrespondenceAddress method:  "+ jsonPathForCorrespondenceAddress);
			correspondenceAddress = getValue(bpaApplication, jsonPathForCorrespondenceAddress).replace("[", "")
					.replace("]", "").replace("\"", "")
					.replace("\\u2013", "-")
					.replace("\\/", "/");
			//LOG.info("CorrespondenceAddress in getCorrespondenceAddress method: "+correspondenceAddress);
		} catch (Exception ex) {
			LOG.error("error while extracting corresponding address of owner", ex);
		}
		return correspondenceAddress;
	}

	public String getNameOfOwner(LinkedHashMap bpaApplication) {
		String ownerName = "";
		try {
			// first look for ownershipMajorType.If not available then look for
			// ownershipCategory
			String ownershipMajorType = getOwnershipMajorType(bpaApplication);
			String jsonPathForOwnerName = "";
			if (StringUtils.isNotEmpty(ownershipMajorType)) {
				switch (ownershipMajorType) {
				case OWNERSHIP_MAJOR_TYPE_INSTITUTIONAL_PRIVATE:
					// only one owner allowed from UI-
					jsonPathForOwnerName = "$.landInfo.additionalDetails.institutionName";
					break;
				case OWNERSHIP_MAJOR_TYPE_INDIVIDUAL:
					jsonPathForOwnerName = "$.landInfo.owners.*.name";
					break;
				case OWNERSHIP_MAJOR_TYPE_INSTITUTIONAL_GOVERNMENT:
					// only one owner allowed from UI-
					jsonPathForOwnerName = "$.landInfo.additionalDetails.institutionName";
					break;
				default:
					//LOG.info("unsupported ownershipMajorType:" + ownershipMajorType);
				}
			} else {
				String ownershipCategory = getOwnershipCategory(bpaApplication);
				if (ownershipCategory.contains(OWNERSHIP_MAJOR_TYPE_INSTITUTIONAL_PRIVATE))
					jsonPathForOwnerName = "$.landInfo.additionalDetails.institutionName";
				else if (ownershipCategory.contains(OWNERSHIP_MAJOR_TYPE_INDIVIDUAL))
					jsonPathForOwnerName = "$.landInfo.owners.*.name";
				else if (ownershipCategory.contains(OWNERSHIP_MAJOR_TYPE_INSTITUTIONAL_GOVERNMENT))
					jsonPathForOwnerName = "$.landInfo.additionalDetails.institutionName";
//				else
//					LOG.info("unsupported ownershipCategory:" + ownershipCategory);
			}
			ownerName = getValue(bpaApplication, jsonPathForOwnerName).replace("[", "").replace("]", "").replace("\"",
					"");

		} catch (Exception ex) {
			LOG.error("error while extracting owner name", ex);
		}
		return ownerName;
	}
	
	public Map<String, Object> getApproverDetails(RequestInfo requestInfo, String tenantId, String businessService,
			String userUUID, String userType) {
		Map<String, Object> approverDetails = new HashMap<>();
		try {
			if (StringUtils.isNotEmpty(userType) && "CITIZEN".equals(userType) && tenantId.contains(".")) {
				tenantId = tenantId.split("\\.")[0];
			}
			Object userSearchResponseObject = userService.fetchUserInfo(requestInfo, tenantId, businessService,
					userUUID);
			//LOG.info("userSearchResponseObject: ");
			if (Objects.isNull(userSearchResponseObject) || !(userSearchResponseObject instanceof Map)
					|| Objects.isNull(((Map) userSearchResponseObject).get("user"))
					|| !(((Map) userSearchResponseObject).get("user") instanceof List)
					|| CollectionUtils.isEmpty((List) ((Map) userSearchResponseObject).get("user"))) {
				//LOG.info("user not found for approver");
			}
			String nameOfApprover = getValue((Map) userSearchResponseObject, "$.user[0].name");
			String mobileNumberOfApprover = getValue((Map) userSearchResponseObject, "$.user[0].userName");
			approverDetails.put("nameOfApprover", nameOfApprover);
			approverDetails.put("mobileNumberOfApprover", mobileNumberOfApprover);
		} catch (Exception ex) {
			LOG.error("error while extracting approver details", ex);
		}
		return approverDetails;
	}
	
	public String getApprovalDate() {
		// approval date to be taken from the date on which permit letter generated(actually dsc signature done)-
		// Date date = new Date(Long.valueOf(getValue(bpaApplication, "approvalDate")));
		Date date = new Date(Calendar.getInstance().getTimeInMillis());
		DateFormat format = new SimpleDateFormat("dd/MM/yyyy");
		String approvalDate = format.format(date);
		return approvalDate;
	}
	
	public Object getAllInstallments(String consumerCode, RequestInfo requestInfo) {
		return bpaService.getAllInstallments(consumerCode, requestInfo);
	}
	
	public BaseColor getBackGroundColorForTaxHeadcode(String taxHeadCode) {
		switch (taxHeadCode) {
		case TAXHEAD_BPA_SANC_FEES_CODE:
			return GREEN;
		case TAXHEAD_BPA_SANC_TEMP_RETENTION_FEE_CODE:
			return PINK;
		case TAXHEAD_BPA_SANC_SECURITY_DEPOSIT_CODE:
			return BLUE;
		case TAXHEAD_BPA_SANC_WORKER_WELFARE_CESS_CODE:
			return BLUE;
		case TAXHEAD_BPA_SANC_PUR_FAR_CODE:
			return YELLOW;
		case TAXHEAD_BPA_SANC_SHELTER_FEE_CODE:
			return GREEN;
		case TAXHEAD_BPA_SANC_SANC_FEE_CODE:
			return LIME;
		case TAXHEAD_BPA_SANC_EIDP_FEE_CODE:
			return ORANGE;
		case TAXHEAD_BPA_SANC_ADJUSTMENT_AMOUNT_CODE:
			return ORANGE;
		case TAXHEAD_BPA_BLDNG_OPRN_FEE_REWORK_ADJUSTMENT_CODE:
			return YELLOW;
		case TAXHEAD_BPA_LAND_DEV_FEE_REWORK_ADJUSTMENT_CODE:
			return YELLOW;
		case TAXHEAD_BPA_LAND_DEV_FEE_CODE:
			return ORANGE;
		case TAXHEAD_BPA_BLDNG_OPRN_FEE_CODE:
			return ORANGE;
		default:
			return YELLOW;
		}
	}
	
	public String getAlterationSubserviceStatement(Map<String, Object> additionalDetails) {
		String subService = null;
		String alterationSubServiceStatement = "";
		if (Objects.nonNull(additionalDetails) && Objects.nonNull(additionalDetails.get(BPA_ADD_DETAILS_SERVICE_KEY))
				&& additionalDetails.get(BPA_ADD_DETAILS_SERVICE_KEY) instanceof Map
				&& Objects.nonNull(((Map) additionalDetails.get(BPA_ADD_DETAILS_SERVICE_KEY))
						.get(BPA_ADD_DETAILS_SUBSERVICE_KEY))) {
			subService = ((Map) additionalDetails.get(BPA_ADD_DETAILS_SERVICE_KEY)).get(BPA_ADD_DETAILS_SUBSERVICE_KEY)
					+ "";
			switch (subService) {
			case ALTERATION_SUBSERVICE_A:
				alterationSubServiceStatement = " (modification of the previously received permit letter xxxxx with No Construction present at site) ";
				break;
			case ALTERATION_SUBSERVICE_B:
				alterationSubServiceStatement = " (modification of the previously received permit letter xxxxx with existing Construction present at site) ";
				break;
			case ALTERATION_SUBSERVICE_C:
			case ALTERATION_SUBSERVICE_D:
				alterationSubServiceStatement = " with previously received permit letter xxxxx ";
				break;
			}
		}
		return alterationSubServiceStatement;
	}
	
	public String getPreviousPermitNo(Map<String, Object> additionalDetails) {
		String previousPermitNo = "";
		if (Objects.nonNull(additionalDetails) && Objects.nonNull(additionalDetails.get(BPA_ADD_DETAILS_SERVICE_KEY))
				&& additionalDetails.get(BPA_ADD_DETAILS_SERVICE_KEY) instanceof Map
				&& Objects.nonNull(((Map) additionalDetails.get(BPA_ADD_DETAILS_SERVICE_KEY))
						.get(BPA_ADD_DETAILS_SUBSERVICE_KEY))) {
			previousPermitNo = ((Map) additionalDetails.get(BPA_ADD_DETAILS_SERVICE_KEY))
					.get(BPA_ADD_DETAILS_PERMIT_NO_KEY) + "";
		}
		return previousPermitNo;
	}
	
	public BigDecimal getExistingFloorBuiltupArea(DcrReportFloorDetail floor) {
		// TODO: use correct parameter in below line-
		BigDecimal existingFloorBuiltUpArea = floor.getExistingBuiltUpArea();
		//LOG.info("getExistingFloorBuiltupArea method gives: " + existingFloorBuiltUpArea);
		if (existingFloorBuiltUpArea.compareTo(BigDecimal.ZERO) > 0) {
			return existingFloorBuiltUpArea;
		}
		// return null if no existing floor
		return BigDecimal.ZERO;
	}
	
	public void addProvidedAreasForAlteration(Map<String, Object> additionalDetails, Plan plan, Document document)
			throws DocumentException {
		String subService = null;
		if (Objects.nonNull(additionalDetails) && Objects.nonNull(additionalDetails.get(BPA_ADD_DETAILS_SERVICE_KEY))
				&& additionalDetails.get(BPA_ADD_DETAILS_SERVICE_KEY) instanceof Map
				&& Objects.nonNull(((Map) additionalDetails.get(BPA_ADD_DETAILS_SERVICE_KEY))
						.get(BPA_ADD_DETAILS_SUBSERVICE_KEY))) {
			subService = ((Map) additionalDetails.get(BPA_ADD_DETAILS_SERVICE_KEY)).get(BPA_ADD_DETAILS_SUBSERVICE_KEY)
					+ "";
			if (!ALTERATION_SUBSERVICE_A.equals(subService)) {
				Font font1 = FontFactory.getFont(FontFactory.HELVETICA, 12);
				Font fontBold = FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD);
				Chunk existingBuiltupAreaTextChunk = new Chunk(PARAGRAPH_EXISTING_BUILTUP_AREA, font1);
				Chunk existingBuiltupAreaValueChunk = new Chunk(plan.getVirtualBuilding() != null
						? plan.getVirtualBuilding().getTotalExistingBuiltUpArea().setScale(2, BigDecimal.ROUND_UP) + SQM
						: "0", fontBold);
				Paragraph existingBuiltupAreaParagraph = new Paragraph();
				existingBuiltupAreaParagraph.add(existingBuiltupAreaTextChunk);
				existingBuiltupAreaParagraph.add(existingBuiltupAreaValueChunk);

				Chunk existingFloorAreaTextChunk = new Chunk(PARAGRAPH_EXISTING_FLOOR_AREA, font1);
				Chunk existingFloorAreaValueChunk = new Chunk(
						plan.getVirtualBuilding().getTotalExistingFloorArea().setScale(2, BigDecimal.ROUND_UP) + SQM,
						fontBold);
				Paragraph existingFloorAreaParagraph = new Paragraph();
				existingFloorAreaParagraph.add(existingFloorAreaTextChunk);
				existingFloorAreaParagraph.add(existingFloorAreaValueChunk);

				document.add(existingBuiltupAreaParagraph);
				document.add(existingFloorAreaParagraph);
			}
		}
	}

	private String getOwnershipMajorType(LinkedHashMap bpaApplication) {
		String ownershipMajorType = "";
		try {
			ownershipMajorType = getValue(bpaApplication, "$.landInfo.ownerShipMajorType");
		} catch (Exception ex) {
			LOG.error("exception while extracting ownershipMajorType");
		}
		return ownershipMajorType;
	}

	private String getOwnershipCategory(LinkedHashMap bpaApplication) {
		String ownershipCategory = "";
		try {
			ownershipCategory = getValue(bpaApplication, "$.landInfo.ownershipCategory");
		} catch (Exception ex) {
			LOG.error("exception while extracting ownerShipCategory");
		}
		return ownershipCategory;
	}
	
	public void addRowsForPlotTable(RequestInfo reqInfo,PdfPTable table1, Map<String, Object> additionalDetails, String tenantId) {
		List<Map<String, Object>> plotList = getPlotAndKhataDetails(additionalDetails);
		
		List<Object> boundaryList = getVillageList(reqInfo, tenantId);
		
		Font fontPara1 = FontFactory.getFont(FontFactory.HELVETICA, 12);
		if(!CollectionUtils.isEmpty(plotList)) {
			for(Map<String, Object> plots : plotList) {
				
				PdfPCell plotNumberCell = new PdfPCell();
				Phrase plotNumberCellPhrase = new Phrase(plots.get("plotNumber") + "", fontPara1);
				plotNumberCell.addElement(plotNumberCellPhrase);
				
				PdfPCell plotAreaCell = new PdfPCell();
				Phrase plotAreaCellPhrase = new Phrase(plots.get("plotArea") + "", fontPara1);
				plotAreaCell.addElement(plotAreaCellPhrase);
				
				PdfPCell khataCell = new PdfPCell();
				Phrase khataCellPhrase = new Phrase(plots.get("khata") + "", fontPara1);
				khataCell.addElement(khataCellPhrase);
				
				PdfPCell kisamCell = new PdfPCell();
				Phrase kisamCellPhrase = new Phrase(plots.get("kisam") + "", fontPara1);
				kisamCell.addElement(kisamCellPhrase);
				
				PdfPCell villageCell = new PdfPCell();
				Phrase villageCellPhrase = new Phrase(getVillageFromVillageList(boundaryList, (String) plots.get("village"))  + "", fontPara1);
				villageCell.addElement(villageCellPhrase);
				
				PdfPCell landOwnerNameCell = new PdfPCell();
				Phrase landOwnerNameCellPhrase = new Phrase(plots.get("applicantName") + "", fontPara1);
				landOwnerNameCell.addElement(landOwnerNameCellPhrase);
				
				PdfPCell gpaHolderNameCell = new PdfPCell();
				Phrase gpaHolderNamePhrase = new Phrase(plots.get("gpaHoldername") != null ? plots.get("gpaHoldername")+ "":"NA", fontPara1);
				gpaHolderNameCell.addElement(gpaHolderNamePhrase);
				
				table1.addCell(plotNumberCell);
				table1.addCell(plotAreaCell);
				table1.addCell(khataCell);
				table1.addCell(kisamCell );	
				table1.addCell(villageCell);
				table1.addCell(landOwnerNameCell);
				table1.addCell(gpaHolderNameCell);
				
			}
		}
		
	}
	
	public void addTableHeaderPlotTable(PdfPTable table1) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD);
		Stream.of("Plot Number","Plot Area", "Khata No", "Kisam", "Village",
				"Land Owner Name", "GPA Holder Name").forEach(columnTitle -> {
					PdfPCell header = new PdfPCell();
					header.setBorderWidth(2);
					header.setVerticalAlignment(Element.ALIGN_MIDDLE);
					header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
					table1.addCell(header);

				});
	}

	public void addRowsForNocTable(PdfPTable table1, RequestInfo requestInfo, String tenantId,
			String bpaApplicationNo, Map<String, Object> additionalDetails) {

		LinkedHashMap<String, String> nocs = getOnlineNocsDetails(requestInfo, tenantId, bpaApplicationNo);
		Map<String, String> nocBypassDetails = (Map) additionalDetails.get("nocBypassDetails");
		
		if (nocs == null) {
	        nocs = new LinkedHashMap<>();
	    }
	    if (nocBypassDetails == null) {
	        nocBypassDetails = new HashMap<>();
	    }

		if (!nocs.isEmpty() || !nocBypassDetails.isEmpty()) {
			table1.addCell("NOC from Airport Authority of India");
			table1.addCell(nocBypassDetails.containsKey("AAI_NOC") 
					&& "Yes".equals(nocBypassDetails.get("AAI_NOC")) 
                    ? "Received" 
                    : nocs.containsKey("AAI_NOC") ? "Received" : "NA");

			table1.addCell("NOC from Fire Department");
			table1.addCell(nocBypassDetails.containsKey("FIRE_NOC") 
					&& "Yes".equals(nocBypassDetails.get("FIRE_NOC")) 
                    ? "Received" 
                    : nocs.containsKey("FIRE_NOC") ? "Received" : "NA");

			table1.addCell("NOC from National Monument Authority");
			table1.addCell(nocBypassDetails.containsKey("NMA_NOC") 
					&& "Yes".equals(nocBypassDetails.get("NMA_NOC")) 
                    ? "Received" 
                    : nocs.containsKey("NMA_NOC") ? "Received" : "NA");
		} else {
			table1.addCell("NOC from Airport Authority of India");
			table1.addCell("NA");

			table1.addCell("NOC from Fire Department");
			table1.addCell("NA");

			table1.addCell("NOC from National Monument Authority");
			table1.addCell("NA");
		}
	}

	public void addTableHeaderForNOCTable(PdfPTable table1) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD);
		Stream.of("Name", "Status").forEach(columnTitle -> {
			PdfPCell header = new PdfPCell();
			header.setPadding(3f);
			header.setVerticalAlignment(Element.ALIGN_MIDDLE);
			header.setHorizontalAlignment(Element.ALIGN_MIDDLE);
			header.setBorderWidth(1);
			header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
			table1.addCell(header);

		});
	}

	public List<String> getNocDetails(Map<String, Object> additionalDetails) {

		List<String> nocList = new ArrayList<>();

		if (Objects.nonNull(additionalDetails) && Objects.nonNull(additionalDetails.get(NOCRELAXATIONCHECKLIST))
				&& additionalDetails.get(NOCRELAXATIONCHECKLIST) instanceof List
				&& !CollectionUtils.isEmpty((List) additionalDetails.get(NOCRELAXATIONCHECKLIST))) {

			nocList = (List) additionalDetails.get(NOCRELAXATIONCHECKLIST);
		}

		return nocList;

	}
	
	public LinkedHashMap<String, String> getOnlineNocsDetails(RequestInfo requestInfo, String tenantId,
			String bpaApplicationNo) {
		Object nocResponse = nocService.fetchNocs(requestInfo, tenantId, bpaApplicationNo);
		LinkedHashMap<String, String> resultMap = new LinkedHashMap<>();

		if (Objects.nonNull(nocResponse) && nocResponse instanceof Map && ((Map) nocResponse).get("Noc") instanceof List
				&& !CollectionUtils.isEmpty((List) ((Map) nocResponse).get("Noc"))) {
			List<Map<String, Object>> nocs = (List) ((Map) nocResponse).get("Noc");

			for (Map<String, Object> noc : nocs) {
				String nocType = (String) noc.get("nocType");
				String applicationStatus = (String) noc.get("applicationStatus");

				resultMap.put(nocType, applicationStatus);
			}
		}
		return resultMap;
	}
	
	public List<Object> getVillageList(RequestInfo reqInfo, String tenantId) {
		
		Object locationRes = locationVillageService.getLocation(reqInfo, tenantId);
		
		Map<String, Object> locationMap = new HashMap<>();
		
		if (Objects.nonNull(locationRes) && locationRes instanceof Map 
				&& ((Map) locationRes).get("TenantBoundary") instanceof List 
				&& !CollectionUtils.isEmpty((List) ((Map) locationRes).get("TenantBoundary"))
				&& Objects.nonNull(((List) ((Map) locationRes).get("TenantBoundary")).get(0))) {
			
			locationMap = (Map) ((List) ((Map) locationRes).get("TenantBoundary")).get(0);
			
		}
		
		List<Object> boundaryList = (List) locationMap.get("boundary");
		
		return boundaryList;
	}
	
	public String getVillageFromVillageList(List<Object> boundaryList, String code) {
		
		String villageName = "";
		
		if(boundaryList!=null)	
			for(Object boundaryMap: boundaryList) {
				String boundaryCode = (String) ((Map) boundaryMap).get("code");
				if(boundaryCode.equalsIgnoreCase(code)) {
					villageName = (String) ((Map)boundaryMap).get("name");
					break;
				}
				
			}
		
		
		return villageName;
	}
	
	public List<String> getRejectionListFromMap(LinkedHashMap bpaApplication) {
		List<String> rejectionList = new ArrayList<>();

		Map<String, String> rejectionCauseMap = getRejectionMap(bpaApplication);
		String manualOtherReason ="";

		for (Map.Entry<String, String> entry : rejectionCauseMap.entrySet()) {

			if (!"false".equals(entry.getValue()) && StringUtils.isNotBlank(entry.getValue())) {
				if(!entry.getKey().equals("MANUAL_OTHER_REASON")) {
					
					rejectionList.add(entry.getKey());
				} else {
					manualOtherReason=entry.getValue();
				}
				
			}
		}
		if(StringUtils.isNotBlank(manualOtherReason))
			rejectionList.add(manualOtherReason);

		return rejectionList;
	}
	

	protected Map<String, String> getShowCauseNoticeNumber(RequestInfo requestInfo, String businessId) {

		Map<String, String> noticeDetails = new HashMap<>();
		
		List<Map<String, Object>> notice = (List<Map<String, Object>>) bpaService
				.getBPAShowCauseNoticeDetails(requestInfo, businessId);
		
		
	    if (notice.isEmpty()) {
	        throw new RuntimeException("Show cause notice does not exist for the given application");
	    }

		Long createdTime = ((Long) ((Map) notice.get(0).get("auditDetails")).get("createdTime"));
		
		String formattedDate = DateTimeFormatter.ofPattern("dd-MM-yyyy")
                .withZone(ZoneId.systemDefault())
                .format(Instant.ofEpochMilli(createdTime));

		noticeDetails.put("LetterNo", (String) notice.get(0).get("LetterNo"));
		noticeDetails.put("createdTime", formattedDate);

		return noticeDetails;
	}
	
}