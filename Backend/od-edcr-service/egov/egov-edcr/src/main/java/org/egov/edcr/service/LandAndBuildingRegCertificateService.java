package org.egov.edcr.service;

import java.io.InputStream;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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
import org.egov.common.entity.edcr.Occupancy;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.ScrutinyDetail;
import org.egov.common.entity.edcr.SetBack;
import org.egov.edcr.config.properties.EdcrApplicationSettings;
import org.egov.edcr.constants.DxfFileConstants;
import org.egov.edcr.constants.NOCConstants;
import org.egov.edcr.contract.oc.BuildingBlockDetails;
import org.egov.edcr.contract.oc.Floor;
import org.egov.edcr.contract.oc.ScrutinyDetails;
import org.egov.edcr.contract.oc.Setback;
import org.egov.edcr.feature.AdditionalFeature;
import org.egov.edcr.od.FloorNumberToWord;
import org.egov.edcr.od.OdishaUtill;
import org.egov.infra.exception.ApplicationRuntimeException;
import org.egov.infra.microservice.models.RequestInfo;
import org.jfree.util.Log;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.text.BadElementException;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.ListItem;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.BarcodeQRCode;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

public abstract class LandAndBuildingRegCertificateService {

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

	private static final Logger LOG = Logger.getLogger(LandAndBuildingRegCertificateService.class);

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
	public static final String SQM = " SQM";
	public static final String PROVIDED_LIFT_DETAIL = "providedLiftDetail";
	public static final String REQUIRED_LIFT_DETAIL = "requiredLiftDetail";
	public static final String SCRUTINY_DETAIL_PROVIDED = "Provided";
	public static final String SCRUTINY_DETAIL_REQUIRED = "Required";
	public static final String GROUND_FLOOR_NO = "0";
	public static final String DETAIL = "Detail";
	public static final String DESCRIPTION_IN_SCRUTINY_DETAIL = "Description";
	public static final String NO_OF_TREE_PER_PLOT = "No of tree as per plot";
	public static final String PAYMENTS_RESPONSE_FIELD = "Payments";

	public static final String TAXHEAD_BPA_REG_BLDNG_SANC_FEE_CODE = "REG_BLDNG_SANC_FEE";
	public static final String TAXHEAD_BPA_REG_BLDNG_SANC_FEE_NAME = "Regularization Building Sanction Fee";
	public static final String TAXHEAD_BPA_REG_TEMP_RETENTION_FEE_CODE = "REG_TEMP_RETENTION_FEE";
	public static final String TAXHEAD_BPA_REG_TEMP_RETENTION_FEE_NAME = "Regularization Building Temporary Retention Fee";
	public static final String TAXHEAD_BPA_REG_SECURITY_DEPOSIT_CODE = "REG_SECURITY_DEPOSIT";
	public static final String TAXHEAD_BPA_REG_SECURITY_DEPOSIT_NAME = "Regularization Building Security Deposit";
	public static final String TAXHEAD_BPA_REG_SANC_WORKER_WELFARE_CESS_CODE = "REG_CONST_WORKER_WELFARE_CESS";
	public static final String TAXHEAD_BPA_REG_SANC_WORKER_WELFARE_CESS_NAME = "Regularization Building Construction Worker Welfare Cess (CWWC)";
	public static final String TAXHEAD_BPA_REG_PUR_FAR_CODE = "REG_PUR_FAR";
	public static final String TAXHEAD_BPA_REG_PUR_FAR_NAME = "Regularization Building Purchasable FAR";
	public static final String TAXHEAD_BPA_REG_SHELTER_FEE_CODE = "REG_SHELTER_FEE";
	public static final String TAXHEAD_BPA_REG_SHELTER_FEE_NAME = "Regularization Building Shelter Fee";
	public static final String TAXHEAD_BPA_SANC_SANC_FEE_CODE = "BPA_SANC_SANC_FEE";
	public static final String TAXHEAD_BPA_SANC_SANC_FEE_NAME = "BPA Sanction Fee";
	public static final String TAXHEAD_BPA_REG_EIDP_FEE_CODE = "REG_EIDP_FEE";
	public static final String TAXHEAD_BPA_REG_EIDP_FEE_NAME = "Regularization Building EIDP Fee";
	public static final String TAXHEAD_BPA_SANC_ADJUSTMENT_AMOUNT_CODE = "BPA_SANC_ADJUSTMENT_AMOUNT";
	public static final String TAXHEAD_BPA_SANC_ADJUSTMENT_AMOUNT_NAME = "BPA Sanction Fee Adjustment Amount";
	public static final String TAXHEAD_BPA_REG_BLDNG_OPRN_FEE_CODE = "REG_BLDNG_OPRN_FEE";
	public static final String TAXHEAD_BPA_REG_BLDNG_OPRN_FEE_NAME = "Regularization Building Operation Fee";
	public static final String TAXHEAD_BPA_REG_COMP_FAR_FEE_CODE = "REG_COMP_FAR_FEE";
	public static final String TAXHEAD_BPA_REG_COMP_FAR_FEE_NAME = "Regularization Building Compounding FAR Fee";
	public static final String TAXHEAD_BPA_REG_COMP_SETBACK_FEE_CODE = "REG_COMP_SETBACK_FEE";
	public static final String TAXHEAD_BPA_REG_COMP_SETBACK_FEE_NAME = "Regularization Building Compounding Setback Fee";
	public static final String TAXHEAD_BPA_REG_SANC_ADJUSTMENT_AMOUNT_1_CODE="REG_SANC_ADJUSTMENT_AMOUNT_1";
	public static final String TAXHEAD_BPA_REG_SANC_ADJUSTMENT_AMOUNT_1_NAME="Regularization Sanction Fee Adjustment Amount 1";
	public static final String TAXHEAD_BPA_REG_SANC_ADJUSTMENT_AMOUNT_2_CODE="REG_SANC_ADJUSTMENT_AMOUNT_2";
	public static final String TAXHEAD_BPA_REG_SANC_ADJUSTMENT_AMOUNT_2_NAME="Regularization Sanction Fee Adjustment Amount 2";
	public static final String TAXHEAD_BPA_REG_SANC_ADJUSTMENT_AMOUNT_3_CODE="REG_SANC_ADJUSTMENT_AMOUNT_3";
	public static final String TAXHEAD_BPA_REG_SANC_ADJUSTMENT_AMOUNT_3_NAME="Regularization Sanction Fee Adjustment Amount 3";
	public static final String TAXHEAD_BPA_REG_LAND_DEV_FEE_REWORK_ADJUSTMENT_CODE="REG_LAND_DEV_FEE_REWORK_ADJUSTMENT";
	public static final String TAXHEAD_BPA_REG_LAND_DEV_FEE_REWORK_ADJUSTMENT_NAME="Regularization Land Dev Fee Rework";
	public static final String TAXHEAD_BPA_REG_BLDNG_OPRN_FEE_REWORK_ADJUSTMENT_CODE="REG_BLDNG_OPRN_FEE_REWORK_ADJUSTMENT";
	public static final String TAXHEAD_BPA_REG_BLDNG_OPRN_FEE_REWORK_ADJUSTMENT_NAME="Regularization Building Operation Fee Rework";
	public static final String TAXHEAD_BPA_Layout_SANC_SECURITY_DEPOSIT_CODE="Layout_SANC_SECURITY_DEPOSIT";
	public static final String TAXHEAD_BPA_Layout_SANC_SECURITY_DEPOSIT_NAME="Layout Security Deposit";
	public static final String TAXHEAD_BPA_Layout_LAND_DEV_FEE_CODE="Layout_LAND_DEV_FEE";
	public static final String TAXHEAD_BPA_Layout_LAND_DEV_FEE_NAME="Layout Land Development Fee";
	public static final String TAXHEAD_BPA_Layout_SANC_SHELTER_FEE_CODE="Layout_SANC_SHELTER_FEE";
	public static final String TAXHEAD_BPA_Layout_SANC_SHELTER_FEE_NAME="Layout Shelter Fee";
	public static final String TAXHEAD_BPA_Layout_SANC_OSE_FEE_CODE="Layout_SANC_OSEFEE";
	public static final String TAXHEAD_BPA_Layout_SANC_OSE_FEE_NAME="Layout OSE Fee";
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

	public Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA, 16, Font.BOLD);
	public Font font1 = FontFactory.getFont(FontFactory.HELVETICA, 12);
	public Font fontBold = FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD);
	public Font fontBoldUnderlined = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Font.UNDERLINE);

	public static final String TAXHEAD_REG_LAND_DEV_FEE_CODE = "REG_LAND_DEV_FEE";
	public static final String TAXHEAD_REG_LAND_DEV_FEE_NAME = "Land Development Fee";
	
	public static final String TAXHEAD_REG_LAND_COMPOUNDING_FEE_CODE = "REG_LAND_COMP_FEE";
	public static final String TAXHEAD_REG_LAND_COMPOUNDING_FEE_NAME = "Compounding fee for regularization of sub-plots";

	public static final String TAXHEAD_REG_SANC_ADJUSTMENT_AMOUNT_FEE_CODE = "REG_SANC_ADJUSTMENT_AMOUNT";
	public static final String TAXHEAD_REG_SANC_ADJUSTMENT_AMOUNT_FEE_NAME = "Other Fee";

	public abstract InputStream generateReport(LinkedHashMap regularizations, RequestInfo requestInfo,
			Boolean isPreview);

	@Autowired
	private EdcrApplicationSettings edcrApplicationSettings;

	Map<String, String> nocMap = new HashMap<>();
	{
		nocMap.put("NOC_NOCCRZ", NOCConstants.NOC_NOCCRZ);
		nocMap.put("NOC_ENVCLEARANCE", NOCConstants.NOC_ENVCLEARANCE);
		nocMap.put("NOC_NOCULBHUD", NOCConstants.NOC_NOCULBHUD);
		nocMap.put("NOC_NOCPUBHLTHENGGORG", NOCConstants.NOC_NOCPUBHLTHENGGORG);
		nocMap.put("NOC_NOCELECTRICDISCOMP", NOCConstants.NOC_NOCELECTRICDISCOMP);
		nocMap.put("NOC_NOCPOLUNHOMEDEPT", NOCConstants.NOC_NOCPOLUNHOMEDEPT);
		nocMap.put("NOC_NOCODSPCB", NOCConstants.NOC_NOCODSPCB);
		nocMap.put("NOC_NA", NOCConstants.NOC_NA);
		nocMap.put("NOC_NOCDEPUTYFORESTOFFICER", NOCConstants.NOC_NOCDEPUTYFORESTOFFICER);
		nocMap.put("NOC_NOCWATERRESDEPT", NOCConstants.NOC_NOCWATERRESDEPT);
		nocMap.put("NOC_NOCTEHREVDISASMANG", NOCConstants.NOC_NOCTEHREVDISASMANG);
		nocMap.put("NOC_NOCNHAI", NOCConstants.NOC_NOCNHAI);
		nocMap.put("NOC_TEHSILDAR", NOCConstants.NOC_TEHSILDAR);
		nocMap.put("NOC_NOCCGWA", NOCConstants.NOC_NOCCGWA);
	}

	public static String image_logo;
	public Image logo = null;
	{
		try {
			ClassPathResource resource = new ClassPathResource("logo_base64.txt");
			InputStream inputStream = resource.getInputStream();
			// InputStream is = classloader.getResourceAsStream("logo_base64.txt");
			// FileInputStream fis = new
			// FileInputStream("classpath:config/logo_base64.txt");
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

	public String getServiceType(Map<String, Object> regularizations) {
		try {
			Map<String, String> SERVICE_TYPE = new ConcurrentHashMap<>();
			SERVICE_TYPE.put("BUILDING_REGULARIZATION", "And Building Regularization");
			// handling for subservice--
			if (Objects.nonNull(regularizations) && Objects.nonNull(regularizations.get("serviceType"))) {
				SERVICE_TYPE.put("BUILDING_REGULARIZATION", regularizations.get("serviceType").toString());
			}
			return SERVICE_TYPE.get("BUILDING_REGULARIZATION");
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	public String getValue(Map dataMap, String key) {
		String jsonString = new JSONObject(dataMap).toString();
		DocumentContext context = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonString);
		String value = "";
		try {
			value = context.read(key);
		} catch (PathNotFoundException p) {
			p.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return value;
	}

//	public Map<String, Object> getAdditionalDetailsMap(Map bpa) {
//		String jsonString = new JSONObject(bpa).toString();
//		DocumentContext context = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonString);
//		return context.read("additionalDetails");
//	}
//	
	public List<Map<String, Object>> getPlotAndKhataDetails(Map<String, Object> regularizations) {

		List<Map<String, Object>> plots = new ArrayList<>();

		if (Objects.nonNull(regularizations) && Objects.nonNull(regularizations.get("landRegularizationInfo"))
				&& regularizations.get("landRegularizationInfo") instanceof Map
				&& Objects.nonNull(((Map) regularizations.get("landRegularizationInfo")).get("plotInfo"))
				&& ((Map) regularizations.get("landRegularizationInfo")).get("plotInfo") instanceof List
				&& !CollectionUtils
						.isEmpty((List) ((Map) regularizations.get("landRegularizationInfo")).get("plotInfo"))) {

			plots = (List) ((Map) regularizations.get("landRegularizationInfo")).get("plotInfo");
		}

		return plots;

	}

	public List<Map<String, Object>> getBuildingBlockDetails(Map<String, Object> regularizations) {

		List<Map<String, Object>> buildingBlocks = new ArrayList<>();

		if (Objects.nonNull(regularizations) && Objects.nonNull(regularizations.get("buildingRegularizationInfo"))
				&& regularizations.get("buildingRegularizationInfo") instanceof Map
				&& Objects.nonNull(((Map) regularizations.get("buildingRegularizationInfo")).get("buildingBlocks"))
				&& ((Map) regularizations.get("buildingRegularizationInfo")).get("buildingBlocks") instanceof List
				&& !CollectionUtils.isEmpty(
						(List) ((Map) regularizations.get("buildingRegularizationInfo")).get("buildingBlocks"))) {

			buildingBlocks = (List) ((Map) regularizations.get("buildingRegularizationInfo")).get("buildingBlocks");
		}

		return buildingBlocks;

	}

	public Object fetchPaymentDetails(RequestInfo requestInfo, String consumercode, String tenantId) {
		paymentService.fetchApplicationFeePaymentDetails(requestInfo, consumercode, tenantId);
		paymentService.fetchPermitFeePaymentDetails(requestInfo, consumercode, tenantId);
		return null;
	}

	public List<Map<String, Object>> getEstimatedSanctionFeeDetails(RequestInfo requestInfo,
			LinkedHashMap bpaApplication) {
		Object estimatedFeeDetails = paymentService.fetchEstimatedFeePaymentForRegularization(requestInfo,
				bpaApplication, "SanctionFee");
		return getTaxHeadEstimatesfromPaymentResponse(estimatedFeeDetails);
	}

	public List<Map<String, Object>> getEstimatedApplicationFeeDetails(RequestInfo requestInfo,
			LinkedHashMap bpaApplication) {
		Object estimatedFeeDetails = paymentService.fetchEstimatedFeePaymentForRegularization(requestInfo,
				bpaApplication, "ApplicationFee");
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
		taxHeadDetails = context.read("$.Calculations[" + (calculationsLength - 1) + "].taxHeadEstimates");
		return taxHeadDetails;
	}

	public String[] getUlbNameAndGradeFromMdms(RequestInfo requestInfo, String tenantId) {
		return mdmsService.getUlbNameAndGradeFromMdms(requestInfo, tenantId);
	}

//	public String getBpaPermitHeader(RequestInfo requestInfo, String tenantId) {
//		return mdmsService.getBpaPermitHeader(requestInfo, tenantId);
//	}

	public Image getQrCode(String applicationNo, String approvalNo, String approvalDate) {
		String qrCodeInformation = "Application Number: %s, Approval Letter Number : %s, Approval Date : %s";
		qrCodeInformation = String.format(qrCodeInformation, applicationNo, approvalNo, approvalDate);
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
		if (totalPlotArea == null || totalPlotArea.compareTo(BigDecimal.ZERO) <= 0)
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
				Chunk chunk = new Chunk(" - " + affectedLandArea.getName() + " affected area: " + area + SQM + "\n",
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

	public BigDecimal getGiftedArea(List<Map<String, Object>> plotInfoList) {
		BigDecimal totalAreaToBeGifted = BigDecimal.ZERO;
		if (plotInfoList.contains("areaToBeGifted")) {
			for (Map<String, Object> plotInfo : plotInfoList) {
				totalAreaToBeGifted = totalAreaToBeGifted
						.add(BigDecimal.valueOf(Double.valueOf(plotInfo.get("areaToBeGifted").toString())));
			}
		}

		return totalAreaToBeGifted;
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
		// TODO required stair
		StringBuilder requiredStairCount = new StringBuilder();
		StringBuilder stairDetail = new StringBuilder();
		try {
			for (Block block : pl.getBlocks()) {
				Optional<Integer> maxStairCount = block.getBuilding().getFloors().stream()
						.map(floor -> floor.getGeneralStairs() == null ? 0 : floor.getGeneralStairs().size())
						.reduce(Integer::max);
				if (maxStairCount.isPresent())
					stairDetail.append(", " + "B" + block.getName() + "-" + maxStairCount.get());
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
		String detail = DxfFileConstants.NA;
		StringBuilder providedLiftDetail = new StringBuilder();
		StringBuilder requiredLiftDetail = new StringBuilder();
		Map<String, String> liftDetail = new HashMap<>();
		try {
			for (Block block : plan.getBlocks()) {
				java.util.List<ScrutinyDetail> scrutinyDetails = OdishaUtill.getScrutinyDetailsFromPlan(plan,
						"Block_" + block.getNumber() + "_" + "General Lift");
				if (!CollectionUtils.isEmpty(scrutinyDetails)) {
					// use ground floor detail-
					Optional<Map<String, String>> groundFloorDetail = scrutinyDetails.get(0).getDetail().stream()
							.filter(floorDetail -> floorDetail.get("Floor").equals(GROUND_FLOOR_NO)).findFirst();
					if (groundFloorDetail.isPresent()
							&& StringUtils.isNotEmpty(groundFloorDetail.get().get(SCRUTINY_DETAIL_REQUIRED))
							&& StringUtils.isNotEmpty(groundFloorDetail.get().get(SCRUTINY_DETAIL_PROVIDED))) {
						requiredLiftDetail.append(", " + "B" + block.getName() + "-"
								+ groundFloorDetail.get().get(SCRUTINY_DETAIL_REQUIRED));
						providedLiftDetail.append(", " + "B" + block.getName() + "-"
								+ groundFloorDetail.get().get(SCRUTINY_DETAIL_PROVIDED));
					}

				}
			}
		} catch (Exception ex) {
			LOG.error("error while extracting the lift count for permit letter", ex);
		}
		String addProvidedLiftDetail = !providedLiftDetail.toString().isEmpty()
				? liftDetail.put(PROVIDED_LIFT_DETAIL, providedLiftDetail.toString().replaceFirst(", ", ""))
				: liftDetail.put(PROVIDED_LIFT_DETAIL, detail);
		String addRequiredLiftDetail = !requiredLiftDetail.toString().isEmpty()
				? liftDetail.put(REQUIRED_LIFT_DETAIL, requiredLiftDetail.toString().replaceFirst(", ", ""))
				: liftDetail.put(REQUIRED_LIFT_DETAIL, detail);
		return liftDetail;
	}

	public String getNoOfTreesRequired(Plan plan) {
		String noOfTreesRequired = DxfFileConstants.NA;
		try {
			java.util.List<ScrutinyDetail> scrutinyDetails = OdishaUtill.getScrutinyDetailsFromPlan(plan,
					"Common_Plantation Tree Cover");
			if (!CollectionUtils.isEmpty(scrutinyDetails)) {
				ScrutinyDetail scrutinyDetail = scrutinyDetails.get(0);
				Optional<Map<String, String>> treeDetail = scrutinyDetail.getDetail().stream()
						.filter(detail -> StringUtils.isNotEmpty(detail.get(DESCRIPTION_IN_SCRUTINY_DETAIL))
								&& NO_OF_TREE_PER_PLOT.equalsIgnoreCase(detail.get(DESCRIPTION_IN_SCRUTINY_DETAIL)))
						.findFirst();
				if (treeDetail.isPresent() && StringUtils.isNotEmpty(treeDetail.get().get(SCRUTINY_DETAIL_REQUIRED)))
					noOfTreesRequired = treeDetail.get().get(SCRUTINY_DETAIL_REQUIRED);
			}
		} catch (Exception ex) {
			LOG.error("error while extracting no of trees required for permit letter", ex);
		}
		return noOfTreesRequired;
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
				else
					LOG.info("unsupported ownershipCategory:" + ownershipCategory);
			}
//			LOG.info("jsonPathForCorrespondenceAddress in getCorrespondenceAddress method:  "
//					+ jsonPathForCorrespondenceAddress);
			correspondenceAddress = getValue(bpaApplication, jsonPathForCorrespondenceAddress).replace("[", "")
					.replace("]", "").replace("\"", "").replace("\\u2013", "-").replace("\\/", "/");
//			LOG.info("CorrespondenceAddress in getCorrespondenceAddress method: " + correspondenceAddress);
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
					LOG.info("unsupported ownershipMajorType:" + ownershipMajorType);
				}
			} else {
				String ownershipCategory = getOwnershipCategory(bpaApplication);
				if (ownershipCategory.contains(OWNERSHIP_MAJOR_TYPE_INSTITUTIONAL_PRIVATE))
					jsonPathForOwnerName = "$.landInfo.additionalDetails.institutionName";
				else if (ownershipCategory.contains(OWNERSHIP_MAJOR_TYPE_INDIVIDUAL))
					jsonPathForOwnerName = "$.landInfo.owners.*.name";
				else if (ownershipCategory.contains(OWNERSHIP_MAJOR_TYPE_INSTITUTIONAL_GOVERNMENT))
					jsonPathForOwnerName = "$.landInfo.additionalDetails.institutionName";
				else
					LOG.info("unsupported ownershipCategory:" + ownershipCategory);
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
				LOG.info("user not found for approver");
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
		// approval date to be taken from the date on which permit letter
		// generated(actually dsc signature done)-
		// Date date = new Date(Long.valueOf(getValue(bpaApplication, "approvalDate")));
		Date date = new Date(Calendar.getInstance().getTimeInMillis());
		DateFormat format = new SimpleDateFormat("dd/MM/yyyy");
		String approvalDate = format.format(date);
		return approvalDate;
	}

	public Object getAllInstallments(String consumerCode, RequestInfo requestInfo) {
		return bpaService.getAllInstallments(consumerCode, requestInfo);
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
		BigDecimal existingFloorBuiltUpArea = floor.getBuiltUpArea();
		//LOG.info("getExistingFloorBuiltupArea method gives: " + existingFloorBuiltUpArea);
		if (existingFloorBuiltUpArea.compareTo(BigDecimal.ZERO) > 0) {
			return existingFloorBuiltUpArea;
		}
		// return null if no existing floor
		return null;
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

	public void addRowsForPlotTable(RequestInfo reqInfo, PdfPTable table1, Map<String, Object> regularizations,
			String tenantId) {
		List<Map<String, Object>> plotList = getPlotAndKhataDetails(regularizations);

		List<Object> boundaryList = getVillageList(reqInfo, tenantId);

		Font fontPara1 = FontFactory.getFont(FontFactory.HELVETICA, 12);
		if (!CollectionUtils.isEmpty(plotList)) {
			for (Map<String, Object> plots : plotList) {

				PdfPCell plotNumberCell = new PdfPCell();
				Phrase plotNumberCellPhrase = new Phrase(plots.get("plotNo") + "", fontPara1);
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
				Phrase villageCellPhrase = new Phrase(
						getVillageFromVillageList(boundaryList, (String) plots.get("village")) + "", fontPara1);
				villageCell.addElement(villageCellPhrase);

				PdfPCell landOwnerNameCell = new PdfPCell();
				Phrase landOwnerNameCellPhrase = new Phrase(plots.get("landOwnerName") + "", fontPara1);
				landOwnerNameCell.addElement(landOwnerNameCellPhrase);

				PdfPCell gpaHolderNameCell = new PdfPCell();
				Phrase gpaHolderNamePhrase = new Phrase(
						plots.get("gpaHolderName") != null ? plots.get("gpaHolderName") + "" : "N.A.", fontPara1);
				gpaHolderNameCell.addElement(gpaHolderNamePhrase);

				table1.addCell(plotNumberCell);
				table1.addCell(plotAreaCell);
				table1.addCell(khataCell);
				table1.addCell(kisamCell);
				table1.addCell(villageCell);
				table1.addCell(landOwnerNameCell);
				table1.addCell(gpaHolderNameCell);

			}
		}

	}

	public void addBlockwiseDetailsTableRow(PdfPTable blockTable, Map<String, Object> regularizations) {
		List<Map<String, Object>> buildingBlockDetails = getBuildingBlockDetails(regularizations);

		Font fontPara1 = FontFactory.getFont(FontFactory.HELVETICA, 12);
		int blockCount = 1;
		if (!CollectionUtils.isEmpty(buildingBlockDetails)) {
			for (Map<String, Object> buildingBlock : buildingBlockDetails) {

				PdfPCell phaseCell = new PdfPCell();
				Phrase phaseCellPhrase = new Phrase("N.A.", fontPara1);
				phaseCell.addElement(phaseCellPhrase);

				PdfPCell blockCell = new PdfPCell();
				Phrase blockCellPhrase = new Phrase("BLOCK- " + blockCount, fontPara1);
				blockCell.addElement(blockCellPhrase);
				blockCount++;

				PdfPCell approvedFarCell = new PdfPCell();
				String approvedFar = getValue(buildingBlock, "$.floors[0].asBuiltFARArea");
				Phrase approvedFarCellPhrase = new Phrase(approvedFar, fontPara1);
				approvedFarCell.addElement(approvedFarCellPhrase);

				PdfPCell existingFarCell = new PdfPCell();
				String existingFar = getValue(buildingBlock, "$.floors[0].asBuiltFARArea");
				Phrase existingFarCellPhrase = new Phrase(existingFar, fontPara1);
				existingFarCell.addElement(existingFarCellPhrase);

				PdfPCell deviationFarCell = new PdfPCell();
				Phrase deviationFarCellPhrase = new Phrase("-", fontPara1);
				deviationFarCell.addElement(deviationFarCellPhrase);

				blockTable.addCell(phaseCell);
				blockTable.addCell(blockCell);
				blockTable.addCell(approvedFarCell);
				blockTable.addCell(existingFarCell);
				blockTable.addCell(deviationFarCell);

			}
		}

	}

	public void addBlockwiseDetailsTableHeader(PdfPTable table1) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD);
		Stream.of("Phase", "Block", "Approved(FAR Area)", "Existing(FAR Area)", "Deviation(FAR Area)")
				.forEach(columnTitle -> {
					PdfPCell header = new PdfPCell();
					header.setBorderWidth(2);
					header.setVerticalAlignment(Element.ALIGN_MIDDLE);
					header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
					table1.addCell(header);

				});
	}

	public void addTableHeaderPlotTable(PdfPTable table1) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD);
		Stream.of("Plot Number", "Plot Area", "Khata No", "Kisam", "Village", "Land Owner Name", "GPA Holder Name")
				.forEach(columnTitle -> {
					PdfPCell header = new PdfPCell();
					header.setBorderWidth(2);
					header.setVerticalAlignment(Element.ALIGN_MIDDLE);
					header.setBackgroundColor(BaseColor.LIGHT_GRAY);
					header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
					table1.addCell(header);

				});
	}

	public void addRowsForNocTable(PdfPTable table1, RequestInfo requestInfo, String tenantId,
			String bpaApplicationNo) {

		LinkedHashMap<String, String> nocs = getOnlineNocsDetails(requestInfo, tenantId, bpaApplicationNo);

		if (!nocs.isEmpty()) {
			table1.addCell("NOC from Airport Authority of India");
			table1.addCell(nocs.containsKey("AAI_NOC") ? "Received" : "NA");

			table1.addCell("NOC from Fire Department");
			table1.addCell(nocs.containsKey("FIRE_NOC") ? "Received" : "NA");

			table1.addCell("NOC from National Monument Authority");
			table1.addCell(nocs.containsKey("NMA_NOC") ? "Received" : "NA");
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

		for (Object boundaryMap : boundaryList) {
			String boundaryCode = (String) ((Map) boundaryMap).get("code");
			if (boundaryCode.equalsIgnoreCase(code)) {
				villageName = (String) ((Map) boundaryMap).get("name");
				break;
			}

		}

		return villageName;
	}

	public void addTableHeaderPaymentTable(PdfPTable table1) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD);
		Stream.of("Sl No.", "Item", "Amount (Rs)").forEach(columnTitle -> {
			PdfPCell header = new PdfPCell();
			header.setPadding(3f);
			header.setVerticalAlignment(Element.ALIGN_MIDDLE);
			header.setHorizontalAlignment(Element.ALIGN_MIDDLE);
			header.setBackgroundColor(BaseColor.LIGHT_GRAY);
			header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
			table1.addCell(header);

		});
	}

	public void addRowsForPaymentsTable(RequestInfo requestInfo, PdfPTable landRegularizationPaymentTable, PdfPTable buildingRegularizationPaymentTable, String tenantId,
			String applicationNo) {

		Object regularizationPaymentDetails = paymentService.fetchBuildingRegularizationPaymentDetails(requestInfo,
				applicationNo, tenantId);

		List<List<Map<String, Object>>> billAccountDetailsList = getBillAccountDetailsFromPaymentResponse(
				regularizationPaymentDetails);
		List<Map<String, Object>> finalBillAccountDetailsList = new ArrayList<>();
		billAccountDetailsList.forEach(finalBillAccountDetailsList::addAll);
		int landRegCount = 1,buildingRegCount=1;
		for (Map<String, Object> bills : finalBillAccountDetailsList) {

			String adjustedAmount = String.valueOf(bills.get("adjustedAmount"));
			// skip those taxheadcodes for which adjustedAmount is 0-
//			if (StringUtils.isNotEmpty(adjustedAmount) && "0.0".equals(adjustedAmount)
//					&& !String.valueOf(bills.get("taxHeadCode")).equalsIgnoreCase("BLR_COMPOUNDING_FEE"))
//				continue;
			String taxHeadCode = String.valueOf(bills.get("taxHeadCode"));
			String taxHeadName = getFeeComponentNameFromTaxHeadCode(taxHeadCode);

//			PdfPCell numberCell = new PdfPCell(new Phrase(toSmallRomanNumeral(count++)));
//			numberCell.setPadding(6f);
			PdfPCell taxHeadCell = new PdfPCell(new Phrase(taxHeadName, font1));
			taxHeadCell.setPadding(6f);
			PdfPCell amountCell = new PdfPCell(new Phrase(adjustedAmount, font1));
			amountCell.setPadding(6f);
			
			if(taxHeadCode.equals(TAXHEAD_REG_LAND_COMPOUNDING_FEE_CODE)||taxHeadCode.equals(TAXHEAD_REG_LAND_DEV_FEE_CODE)) {
				PdfPCell numberCell = new PdfPCell(new Phrase(toSmallRomanNumeral(landRegCount++)));
				numberCell.setPadding(6f);
				landRegularizationPaymentTable.addCell(numberCell);
				landRegularizationPaymentTable.addCell(taxHeadCell);
				landRegularizationPaymentTable.addCell(amountCell);
			}
			else {
				PdfPCell numberCell = new PdfPCell(new Phrase(toSmallRomanNumeral(buildingRegCount++)));
				numberCell.setPadding(6f);
				buildingRegularizationPaymentTable.addCell(numberCell);
				buildingRegularizationPaymentTable.addCell(taxHeadCell);
				buildingRegularizationPaymentTable.addCell(amountCell);
			}
		}

	}

	public List<List<Map<String, Object>>> getBillAccountDetailsFromPaymentResponse(
			Object regularalizationPaymentDetails) {
		List<Map<String, Object>> billAccountDetails = new ArrayList<>();
		List<List<Map<String, Object>>> billAccountDetailsList = new ArrayList<>();

		int paymentsLength = 0;
		if (Objects.nonNull(regularalizationPaymentDetails) && regularalizationPaymentDetails instanceof Map
				&& ((Map) regularalizationPaymentDetails).get(PAYMENTS_RESPONSE_FIELD) instanceof List
				&& !CollectionUtils
						.isEmpty((List) ((Map) regularalizationPaymentDetails).get(PAYMENTS_RESPONSE_FIELD))) {
			List payments = (List) ((Map) regularalizationPaymentDetails).get(PAYMENTS_RESPONSE_FIELD);
			paymentsLength = payments.size();
		}

		for (int i = 0; i < paymentsLength; i++) {

			String jsonString = new JSONObject((Map) regularalizationPaymentDetails).toString();
			DocumentContext context = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonString);
			billAccountDetails = context
					.read("$.Payments[" + i + "].paymentDetails[0].bill.billDetails[0].billAccountDetails");
			billAccountDetailsList.add(billAccountDetails);
		}

		return billAccountDetailsList;
	}

	public String getFeeComponentNameFromTaxHeadCode(String taxHeadCode) {
		String taxHeadName = "";
		switch (taxHeadCode) {
		case TAXHEAD_REG_LAND_COMPOUNDING_FEE_CODE:
			taxHeadName = TAXHEAD_REG_LAND_COMPOUNDING_FEE_NAME;
			break;
		case TAXHEAD_REG_LAND_DEV_FEE_CODE:
			taxHeadName = TAXHEAD_REG_LAND_DEV_FEE_NAME;
			break;

		case TAXHEAD_REG_SANC_ADJUSTMENT_AMOUNT_FEE_CODE:
			taxHeadName = TAXHEAD_REG_SANC_ADJUSTMENT_AMOUNT_FEE_NAME;
			break;

		case TAXHEAD_BPA_REG_BLDNG_SANC_FEE_CODE:
			taxHeadName = TAXHEAD_BPA_REG_BLDNG_SANC_FEE_NAME;
			break;

		case TAXHEAD_BPA_REG_SANC_WORKER_WELFARE_CESS_CODE:
			taxHeadName = TAXHEAD_BPA_REG_SANC_WORKER_WELFARE_CESS_NAME;
			break;

		case TAXHEAD_BPA_REG_SHELTER_FEE_CODE:
			taxHeadName = TAXHEAD_BPA_REG_SHELTER_FEE_NAME;
			break;

		case TAXHEAD_BPA_REG_SECURITY_DEPOSIT_CODE:
			taxHeadName = TAXHEAD_BPA_REG_SECURITY_DEPOSIT_NAME;
			break;

		case TAXHEAD_BPA_REG_PUR_FAR_CODE:
			taxHeadName = TAXHEAD_BPA_REG_PUR_FAR_NAME;
			break;

		case TAXHEAD_BPA_REG_EIDP_FEE_CODE:
			taxHeadName = TAXHEAD_BPA_REG_EIDP_FEE_NAME;
			break;

		case TAXHEAD_BPA_REG_BLDNG_OPRN_FEE_CODE:
			taxHeadName = TAXHEAD_BPA_REG_BLDNG_OPRN_FEE_NAME;
			break;

		case TAXHEAD_BPA_REG_COMP_FAR_FEE_CODE:
			taxHeadName = TAXHEAD_BPA_REG_COMP_FAR_FEE_NAME;
			break;

		case TAXHEAD_BPA_REG_COMP_SETBACK_FEE_CODE:
			taxHeadName = TAXHEAD_BPA_REG_COMP_SETBACK_FEE_NAME;
			break;

		case TAXHEAD_BPA_REG_SANC_ADJUSTMENT_AMOUNT_1_CODE:
			taxHeadName = TAXHEAD_BPA_REG_SANC_ADJUSTMENT_AMOUNT_1_NAME;
			break;
			
		case TAXHEAD_BPA_REG_SANC_ADJUSTMENT_AMOUNT_2_CODE:
			taxHeadName = TAXHEAD_BPA_REG_SANC_ADJUSTMENT_AMOUNT_2_NAME;
			break;
			
		case TAXHEAD_BPA_REG_SANC_ADJUSTMENT_AMOUNT_3_CODE:
			taxHeadName = TAXHEAD_BPA_REG_SANC_ADJUSTMENT_AMOUNT_3_NAME;
			break;
			
		case TAXHEAD_BPA_REG_LAND_DEV_FEE_REWORK_ADJUSTMENT_CODE:
			taxHeadName = TAXHEAD_BPA_REG_LAND_DEV_FEE_REWORK_ADJUSTMENT_NAME;
			break;
			
		case TAXHEAD_BPA_REG_BLDNG_OPRN_FEE_REWORK_ADJUSTMENT_CODE:
			taxHeadName = TAXHEAD_BPA_REG_BLDNG_OPRN_FEE_REWORK_ADJUSTMENT_NAME;
			break;
			
		case TAXHEAD_BPA_REG_TEMP_RETENTION_FEE_CODE:
			taxHeadName = TAXHEAD_BPA_REG_TEMP_RETENTION_FEE_NAME;
			break;
			
		case TAXHEAD_BPA_Layout_LAND_DEV_FEE_CODE:
			taxHeadName = TAXHEAD_BPA_Layout_LAND_DEV_FEE_NAME;
			break;
			
		case TAXHEAD_BPA_Layout_SANC_OSE_FEE_CODE:
			taxHeadName = TAXHEAD_BPA_Layout_SANC_OSE_FEE_NAME;
			break;
			
		case TAXHEAD_BPA_Layout_SANC_SECURITY_DEPOSIT_CODE:
			taxHeadName = TAXHEAD_BPA_Layout_SANC_SECURITY_DEPOSIT_NAME;
			break;
			
		case TAXHEAD_BPA_Layout_SANC_SHELTER_FEE_CODE:
			taxHeadName = TAXHEAD_BPA_Layout_SANC_SHELTER_FEE_NAME;
			break;

		default:
			taxHeadName = taxHeadCode;
		}
		return taxHeadName;
	}

	private String toSmallRomanNumeral(int number) {
		if (number < 1 || number > 26) {
			return "";
		}

		String[] smallRomanNumerals = { "i", "ii", "iii", "iv", "v", "vi", "vii", "viii", "ix", "x", "xi", "xii",
				"xiii", "xiv", "xv", "xvi", "xvii", "xviii", "xix", "xx", "xxi", "xxii", "xxiii", "xxiv", "xxv",
				"xxvi" };
		return smallRomanNumerals[number - 1];
	}

	public void addRowsForEstimatedFees(RequestInfo requestInfo, PdfPTable landRegularizationPaymentTable, PdfPTable buildingRegularizationPaymentTable,
			LinkedHashMap regularizations) {

		List<Map<String, Object>> estimatedSancFeeDetails = getEstimatedSanctionFeeDetails(requestInfo,
				regularizations);
		int landRegCount = 1,buildingRegCount=1;
		for (Map<String, Object> bills : estimatedSancFeeDetails) {

			String estimatedAmount = String.valueOf(bills.get("estimateAmount"));

			String taxHeadCode = String.valueOf(bills.get("taxHeadCode"));
			String taxHeadName = getFeeComponentNameFromTaxHeadCode(taxHeadCode);

			
			PdfPCell taxHeadCell = new PdfPCell(new Phrase(taxHeadName, font1));
			taxHeadCell.setPadding(6f);
			PdfPCell amountCell = new PdfPCell(new Phrase(estimatedAmount + " (Not Paid)", font1));
			amountCell.setPadding(6f);

			if(taxHeadCode.equals(TAXHEAD_REG_LAND_COMPOUNDING_FEE_CODE)||taxHeadCode.equals(TAXHEAD_REG_LAND_DEV_FEE_CODE)) {
				PdfPCell numberCell = new PdfPCell(new Phrase(toSmallRomanNumeral(landRegCount++)));
				numberCell.setPadding(6f);
				landRegularizationPaymentTable.addCell(numberCell);
				landRegularizationPaymentTable.addCell(taxHeadCell);
				landRegularizationPaymentTable.addCell(amountCell);
			}
			else {
				PdfPCell numberCell = new PdfPCell(new Phrase(toSmallRomanNumeral(buildingRegCount++)));
				numberCell.setPadding(6f);
				buildingRegularizationPaymentTable.addCell(numberCell);
				buildingRegularizationPaymentTable.addCell(taxHeadCell);
				buildingRegularizationPaymentTable.addCell(amountCell);
			}
		}

		List<Map<String, Object>> estimatedAppFeeDetails = getEstimatedApplicationFeeDetails(requestInfo,
				regularizations);

		for (Map<String, Object> bills : estimatedAppFeeDetails) {

			String estimatedAmount = String.valueOf(bills.get("estimateAmount"));

			String taxHeadCode = String.valueOf(bills.get("taxHeadCode"));
			String taxHeadName = getFeeComponentNameFromTaxHeadCode(taxHeadCode);

			
			PdfPCell taxHeadCell = new PdfPCell(new Phrase(taxHeadName, font1));
			taxHeadCell.setPadding(6f);
			PdfPCell amountCell = new PdfPCell(new Phrase(estimatedAmount + "(Paid)", font1));
			amountCell.setPadding(6f);

			if(taxHeadCode.equals(TAXHEAD_REG_LAND_COMPOUNDING_FEE_CODE)||taxHeadCode.equals(TAXHEAD_REG_LAND_DEV_FEE_CODE)) {
				PdfPCell numberCell = new PdfPCell(new Phrase(toSmallRomanNumeral(landRegCount++)));
				numberCell.setPadding(6f);
				landRegularizationPaymentTable.addCell(numberCell);
				landRegularizationPaymentTable.addCell(taxHeadCell);
				landRegularizationPaymentTable.addCell(amountCell);
			}
			else {
				PdfPCell numberCell = new PdfPCell(new Phrase(toSmallRomanNumeral(buildingRegCount++)));
				numberCell.setPadding(6f);
				buildingRegularizationPaymentTable.addCell(numberCell);
				buildingRegularizationPaymentTable.addCell(taxHeadCell);
				buildingRegularizationPaymentTable.addCell(amountCell);
			}
		}
	}

	protected PdfPTable getBuildingBlocksTable(java.util.List<Map<String, Object>> buildingBlocksInfoList, Set<String> subOccupancies) {
		Integer blockCount = 0;
		PdfPTable buildingBlockTable = new PdfPTable(5);
		buildingBlockTable.setWidthPercentage(100f);
		for (Map<String, Object> block : buildingBlocksInfoList) {
			blockCount = blockCount + 1;
			BigDecimal totalExistingBuiltUpArea = BigDecimal.ZERO;
			BigDecimal totalApprovedBuiltUpArea=BigDecimal.ZERO;
			Map<String,BigDecimal> areas=new HashMap<>();
			areas.put("totalExistingBuiltUpArea", totalExistingBuiltUpArea);
			areas.put("totalApprovedBuiltUpArea", totalApprovedBuiltUpArea);
			addBuildingBlockTableHeader(buildingBlockTable, blockCount);
			java.util.List<Floor> floorDetails = getFloorDetails(block.get("floors"));
			//Log.info(floorDetails);
			for (Floor floor : floorDetails) {
				addRowsPerFloorAndAggregateFlrAreas(buildingBlockTable, floor,areas);
				subOccupancies.add(floor.getSubOcuupancy().getLabel());

			}
			addTotalRow(buildingBlockTable, areas);

		}
		return buildingBlockTable;
	}

	private PdfPTable getAreaStatementTable(String plotArea, String netPlotArea, String roadWidth)
			throws DocumentException {
		PdfPTable areaStatementTable = new PdfPTable(3);
		areaStatementTable.setLockedWidth(false);
//		areaStatementTable.setWidthPercentage(100f);
		float[] areaStatementTableWidths = { 5f, 50f, 15f };
		areaStatementTable.setWidths(areaStatementTableWidths);

		PdfPCell aCell = new PdfPCell();
		aCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
		aCell.setHorizontalAlignment(Element.ALIGN_MIDDLE);
		aCell.setPhrase(new Phrase("(a)", font1));

		PdfPCell bCell = new PdfPCell();
		bCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
		bCell.setHorizontalAlignment(Element.ALIGN_MIDDLE);
		bCell.setPhrase(new Phrase("(b)", font1));

		PdfPCell cCell = new PdfPCell();
		cCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
		cCell.setHorizontalAlignment(Element.ALIGN_MIDDLE);
		cCell.setPhrase(new Phrase("(c)", font1));

		areaStatementTable.addCell(aCell);
		areaStatementTable.addCell("Total Plot Area");
		areaStatementTable.addCell(plotArea + " sq. meter");

		areaStatementTable.addCell(bCell);
		areaStatementTable.addCell("Net Plot Area");
		areaStatementTable.addCell(netPlotArea + " sq. meter");

		areaStatementTable.addCell(cCell);
		areaStatementTable.addCell("Road Width");
		areaStatementTable.addCell(roadWidth + " sq. meter");

		ListItem areaStatementItem = new ListItem();
		areaStatementItem.add(new Chunk("Area statement of the plot", fontBold));
		areaStatementItem.setIndentationLeft(60);
		areaStatementItem.setSpacingAfter(10);

		return areaStatementTable;
	}

	private void addSetbackTableHeader(PdfPTable setbackTable) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 13, Font.BOLD);
		Stream.of("Block No.", "Item", "Provided (in Mtr)").forEach(columnTitle -> {
			PdfPCell header = new PdfPCell();
			header.setBorderWidth(2);
			header.setVerticalAlignment(Element.ALIGN_MIDDLE);
			header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
			setbackTable.addCell(header);
		});
	}

	private void addSetbackTableRows(PdfPTable setbackTable, List<Map<String, Object>> buildingBlocksInfoList) {
		Integer blockCount = 0;
		for (Map<String, Object> buildingBlockDetails : buildingBlocksInfoList) {
			blockCount = blockCount + 1;
			java.util.List<Setback> setbackList = getSetbackList(buildingBlockDetails.get("setback"));
			Map<String, String> setBackData = getSetBackData(setbackList);
			
			if(setBackData.size()==4) {
				String frontSetbackProvided = setBackData.get("front");
				String rearSetbackProvided = setBackData.get("rear");
				String leftSetbackProvided = setBackData.get("left");
				String rightSetbackProvided = setBackData.get("right");
	
				addCellForSetBackTable(setbackTable, blockCount.toString(), 4);
				addCellForSetBackTable(setbackTable, "Front Set back", null);
				addCellForSetBackTable(setbackTable, frontSetbackProvided + "", null);
				addCellForSetBackTable(setbackTable, "Rear Set back", null);
				addCellForSetBackTable(setbackTable, rearSetbackProvided + "", null);
				addCellForSetBackTable(setbackTable, "Left side", null);
				addCellForSetBackTable(setbackTable, leftSetbackProvided + "", null);
				addCellForSetBackTable(setbackTable, "Right side", null);
				addCellForSetBackTable(setbackTable, rightSetbackProvided + "", null);
			}
			else {
				String frontSetbackProvided = setBackData.get("front");
				String totalCumulativeFrontAndRear = setBackData.get("totalCumulativeFrontAndRear");
				String totalCumulativeSides = setBackData.get("totalCumulativeSides");
				
	
				addCellForSetBackTable(setbackTable, blockCount.toString(), 3);
				addCellForSetBackTable(setbackTable, "Front Set back", null);
				addCellForSetBackTable(setbackTable, frontSetbackProvided + "", null);
				addCellForSetBackTable(setbackTable, "Total Cumulative Front and Rear Set Back", null);
				addCellForSetBackTable(setbackTable, totalCumulativeFrontAndRear + "", null);
				addCellForSetBackTable(setbackTable, "Total Cumulative Side Set Back", null);
				addCellForSetBackTable(setbackTable, totalCumulativeSides + "", null);

			}

		}

	}

	private List<Setback> getSetbackList(Object object) {
		ObjectMapper objectMapper = new ObjectMapper();
		List<Setback> setback = objectMapper.convertValue(object, new TypeReference<List<Setback>>() {
		});
		return setback;
	}

	private Map<String, String> getSetBackData(java.util.List<Setback> setbackList) {

		Map<String, String> setBackDataMap = new HashMap<String, String>();
		setbackList.forEach(setback -> {
			setBackDataMap.put(setback.getName(), setback.getAsBuiltMeasurement());
		});
		return setBackDataMap;
	}

	private void addCellForSetBackTable(PdfPTable setbackTable, String displayContent, Integer rowspan) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 13, Font.BOLD);
		displayContent= StringUtils.isEmpty(displayContent)?"N.A." : displayContent;
		PdfPCell header = new PdfPCell();
		header.setVerticalAlignment(Element.ALIGN_MIDDLE);
		header.setPhrase(new Phrase(displayContent, fontPara1Bold));
		if (Objects.nonNull(rowspan))
			header.setRowspan(rowspan);
		setbackTable.addCell(header);
	}

	private void addBuildingBlockTableHeader(PdfPTable buildingBlockTable, Integer blockNumber) {

		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 12, Font.BOLD);

		// for alteration subservices other than subservice A, show one extra column for
		// existing area-
		Stream.of("Block-No." + blockNumber, "Approved (BUA Area)","Existing (BUA Area)","Deviation (BUA Area)", "Proposed use")
				.forEach(columnTitle -> {
					PdfPCell header = new PdfPCell();
					header.setBackgroundColor(BaseColor.LIGHT_GRAY);
					header.setBorderWidth(2);
					header.setVerticalAlignment(Element.ALIGN_MIDDLE);
					header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
					buildingBlockTable.addCell(header);

				});

	}

	private void addRowsPerFloorAndAggregateFlrAreas(PdfPTable buildingBlockTable, Floor floor,
			Map<String,BigDecimal> builtUpAreas) {
		
		BigDecimal totalExistingBuiltUpArea=builtUpAreas.get("totalExistingBuiltUpArea");
		BigDecimal totalApprovedBuiltUpArea=builtUpAreas.get("totalApprovedBuiltUpArea");
		
		String approvedBUA=floor.getApprovedBUA();
		String asBuiltBUA= floor.getAsBuiltBUA();
		
		BigDecimal deviationBUA = BigDecimal.ZERO;
		
		if(!StringUtils.isEmpty(approvedBUA) && !StringUtils.isEmpty(asBuiltBUA)) {
			deviationBUA= (BigDecimal.valueOf(Double.parseDouble(approvedBUA)).subtract(BigDecimal.valueOf(Double.parseDouble(asBuiltBUA)))).abs();
		}
		else if(StringUtils.isEmpty(approvedBUA)  && !StringUtils.isEmpty(asBuiltBUA)) {
			deviationBUA=BigDecimal.valueOf(Double.parseDouble(asBuiltBUA));
		}
		else if(!StringUtils.isEmpty(approvedBUA)  && StringUtils.isEmpty(asBuiltBUA)) {
			deviationBUA=BigDecimal.valueOf(Double.parseDouble(approvedBUA));
		}

		PdfPCell floorNameCell = new PdfPCell();
		Phrase floorNamephrase = new Phrase("Floor-" + floor.getFloorNumber());
		floorNameCell.addElement(floorNamephrase);
		buildingBlockTable.addCell(floorNameCell);

		PdfPCell floorApprovedAreaCell = new PdfPCell();
		Phrase floorApprovedAreaphrase = new Phrase(floor.getApprovedBUA() + "");
		floorApprovedAreaCell.addElement(floorApprovedAreaphrase);
		buildingBlockTable.addCell(floorApprovedAreaCell);
		
		PdfPCell floorExistingAreaCell = new PdfPCell();
		Phrase floorExistingAreaphrase = new Phrase(floor.getAsBuiltBUA() + "");
		floorExistingAreaCell.addElement(floorExistingAreaphrase);
		buildingBlockTable.addCell(floorExistingAreaCell);
		
		
		
		PdfPCell floorDeviationAreaCell = new PdfPCell();
		Phrase floorDeviationAreaphrase = new Phrase(deviationBUA.toString());
		floorDeviationAreaCell.addElement(floorDeviationAreaphrase);
		buildingBlockTable.addCell(floorDeviationAreaCell);

		PdfPCell floorOccupancyCell = new PdfPCell();
		Phrase floorOccupancyphrase = new Phrase(floor.getSubOcuupancy().getLabel());
		floorOccupancyCell.addElement(floorOccupancyphrase);
		buildingBlockTable.addCell(floorOccupancyCell);

		if(StringUtils.isNotEmpty(floor.getAsBuiltBUA()))
			totalExistingBuiltUpArea = totalExistingBuiltUpArea.add(BigDecimal.valueOf(Double.parseDouble(floor.getAsBuiltBUA())));
		if(StringUtils.isNotEmpty(floor.getApprovedBUA()))
			totalApprovedBuiltUpArea=totalApprovedBuiltUpArea.add(BigDecimal.valueOf(Double.parseDouble(floor.getApprovedBUA())));
		
		builtUpAreas.put("totalExistingBuiltUpArea", totalExistingBuiltUpArea);
		builtUpAreas.put("totalApprovedBuiltUpArea", totalApprovedBuiltUpArea);
		

	}

	private void addTotalRow(PdfPTable table, Map<String,BigDecimal>areas) {
		// long totalDuInBlock = totalNoOfDwellingUnits;
		table.addCell("Total BUA Area");
		table.addCell(areas.get("totalApprovedBuiltUpArea")+"");
		table.addCell(areas.get("totalExistingBuiltUpArea") + "");
		table.addCell(" ");
		table.addCell(" ");
		// table.addCell(totalDuInBlock+"");
	}

	private void addRowsTotalDewellingUnits(PdfPTable table, String totalNoOfDwellingUnits) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 12, Font.BOLD);
		PdfPCell pdfWordCell1 = new PdfPCell();
		Phrase firstLine = new Phrase("Total no. of Dwelling Units -" + totalNoOfDwellingUnits, fontPara1Bold);
		pdfWordCell1.addElement(firstLine);
		pdfWordCell1.setVerticalAlignment(Element.ALIGN_MIDDLE);
		table.addCell(pdfWordCell1);

	}

	private void addBuildingAccessoryDetailTableHeader(PdfPTable buildingAccessoryDetailTable,
			LinkedHashMap<String, Object> additionalInformation) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 12, Font.BOLD);

		Stream.of("Bye Laws Provisions", "Required", "Proposed").forEach(columnTitle -> {
			PdfPCell header = new PdfPCell();
			header.setBorderWidth(2);
			header.setBackgroundColor(BaseColor.LIGHT_GRAY);
			header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
			buildingAccessoryDetailTable.addCell(header);
		});

		// show provided stair count as required as the method
		// .GeneralStair.requiredGenralStairPerFloor(plan, block) giving 0 count
		// Stream.of("No.of staircases", getRequiredStairCount(plan),
		// getStairCount(plan))
		Stream.of("No.of staircases", additionalInformation.get("noOfStairCaseRequired").toString(),
				additionalInformation.get("noOfStairCaseProvided").toString()).forEach(columnTitle -> {
					PdfPCell header = new PdfPCell();
					header.setBorderWidth(2);
					header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
					buildingAccessoryDetailTable.addCell(header);
				});
		Stream.of("No.of Lifts", additionalInformation.get("noOfLiftRequired").toString(),
				additionalInformation.get("noOfLiftProvided").toString()).forEach(columnTitle -> {
					PdfPCell header = new PdfPCell();
					header.setBorderWidth(2);
					header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
					buildingAccessoryDetailTable.addCell(header);
				});
//		Stream.of("E-vehicle charging station", "0", "0")
//				.forEach(columnTitle -> {
//					PdfPCell header = new PdfPCell();
//					header.setBorderWidth(2);
//					header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
//					buildingAccessoryDetailTable.addCell(header);
//				});
		Stream.of("Visitor parking(in Sqm.)", additionalInformation.get("visitorParkingRequired").toString(),
				additionalInformation.get("visitorParkingProvided").toString()).forEach(columnTitle -> {
					PdfPCell header = new PdfPCell();
					header.setBorderWidth(2);
					header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
					buildingAccessoryDetailTable.addCell(header);
				});
		Stream.of("Plantation(no of tree per 80Sqm.)", additionalInformation.get("plantationRequired").toString(),
				additionalInformation.get("plantationProvided").toString()).forEach(columnTitle -> {
					PdfPCell header = new PdfPCell();
					header.setBorderWidth(2);
					header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
					buildingAccessoryDetailTable.addCell(header);
				});
	}

	private void addTotalAreaTableHeader(PdfPTable totalAreaTable, BigDecimal totalFloorArea, BigDecimal totalBUA) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 13, Font.BOLD);

		Stream.of("Grand Total FAR Area - " + totalFloorArea + " Sqm.").forEach(columnTitle -> {
			PdfPCell header = new PdfPCell();
			header.setBorderWidth(2);
			header.setVerticalAlignment(Element.ALIGN_MIDDLE);
			header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
			totalAreaTable.addCell(header);
		});

		Stream.of("Grand Total BUA - " + totalBUA + " Sqm.").forEach(columnTitle -> {
			PdfPCell header = new PdfPCell();
			header.setBorderWidth(2);
			header.setVerticalAlignment(Element.ALIGN_MIDDLE);
			header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
			totalAreaTable.addCell(header);
		});
	}

	private void addFarTableRows(PdfPTable farTable, LinkedHashMap<String, Object> buildingRegularizationInfo) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 12, Font.BOLD);
		PdfPCell pdfWordCell1 = new PdfPCell();
		Phrase firstLine = new Phrase("F.A.R", fontPara1Bold);
		pdfWordCell1.addElement(firstLine);
		pdfWordCell1.setVerticalAlignment(Element.ALIGN_MIDDLE);
		pdfWordCell1.setBorderWidth(2);
		farTable.addCell(pdfWordCell1);
		PdfPCell pdfWordCell2 = new PdfPCell();
		Phrase secondLine = new Phrase(
				getValue(buildingRegularizationInfo, "$.farDetails.maxPermissibleFar") + " (Max. Permissible)\n"
						+ getValue(buildingRegularizationInfo, "$.farDetails.baseFar") + " (Base FAR )");
		pdfWordCell2.addElement(secondLine);
		pdfWordCell2.setVerticalAlignment(Element.ALIGN_MIDDLE);
		pdfWordCell2.setBorderWidth(2);
		farTable.addCell(pdfWordCell2);
		PdfPCell pdfWordCell3 = new PdfPCell();
		String asBuiltFar= getValue(buildingRegularizationInfo, "$.farDetails.asBuiltFar");
		String baseFar = getValue(buildingRegularizationInfo, "$.farDetails.baseFar");
		BigDecimal deltaFAR = BigDecimal.ZERO;
		if(!StringUtils.isEmpty(asBuiltFar) && !StringUtils.isEmpty(baseFar)) {
		deltaFAR = (BigDecimal
				.valueOf(Double.parseDouble(asBuiltFar)))
				.subtract(BigDecimal
						.valueOf(Double.parseDouble(baseFar)))
				.setScale(2, BigDecimal.ROUND_HALF_UP);
		}

		// tdr relaxation- decrease deltaFar based on tdrFarRelaxation-
		if (!StringUtils.isEmpty(getValue(buildingRegularizationInfo, "$.otherDetails.tdrFarRelaxation"))) {
			deltaFAR = deltaFAR
					.subtract(new BigDecimal(Double
							.parseDouble(getValue(buildingRegularizationInfo, "$.otherDetails.tdrFarRelaxation"))))
					.setScale(2, BigDecimal.ROUND_UP);
		}
		if (deltaFAR.compareTo(BigDecimal.ZERO) < 0) {
			deltaFAR = BigDecimal.ZERO;
		}
		Chunk chunk1 = new Chunk("ACHIEVED- " + getValue(buildingRegularizationInfo, "$.farDetails.asBuiltFar"),
				fontPara1Bold);
		Chunk chunk2 = new Chunk("(" + deltaFAR + " Purchasable FAR)");
		Phrase thirdLine = new Phrase();
		thirdLine.add(chunk1);
		thirdLine.add(chunk2);
		pdfWordCell3.addElement(thirdLine);
		pdfWordCell3.setBorderWidth(2);
		farTable.addCell(pdfWordCell3);
	}

	private void addBuildingHeightAndParkingTableRows(PdfPTable buildingHeightAndParkingtable,
			LinkedHashMap<String, Object> buildingRegularizationAdditionalDetails,
			List<Map<String, Object>> buildingBlocksInfoList, BigDecimal totalParkingArea) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 13, Font.BOLD);
		// TODO what height
		Stream.of("Height (mtr.)", getBuildingBlockHeight(buildingBlocksInfoList)).forEach(columnTitle -> {
			PdfPCell header = new PdfPCell();
			header.setBorderWidth(2);
			header.setVerticalAlignment(Element.ALIGN_MIDDLE);
			header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
			buildingHeightAndParkingtable.addCell(header);
		});

		PdfPCell pdfWordCell1 = new PdfPCell();
		Phrase firstLine = new Phrase("Parking", fontPara1Bold);
		pdfWordCell1.addElement(firstLine);
		pdfWordCell1.setVerticalAlignment(Element.ALIGN_MIDDLE);
		pdfWordCell1.setBorderWidth(2);
		buildingHeightAndParkingtable.addCell(pdfWordCell1);
		PdfPCell pdfWordCell3 = new PdfPCell();
		totalParkingArea= BigDecimal.valueOf(Double.parseDouble(buildingRegularizationAdditionalDetails.get("parkingTotal").toString()));

		Chunk chunk1 = new Chunk("Basement-" + buildingRegularizationAdditionalDetails.get("parkingBasement")
				+ "+	  Stilt- " + buildingRegularizationAdditionalDetails.get("parkingStilt")
				+ " + Ground (Open Parking )-" + buildingRegularizationAdditionalDetails.get("parkingGround"));
		Chunk chunk2 = new Chunk("   Total =" + buildingRegularizationAdditionalDetails.get("parkingTotal") + " Sqm.",
				fontPara1Bold);
		Phrase thirdLine = new Phrase();
		thirdLine.add(chunk1);
		thirdLine.add(chunk2);
		pdfWordCell3.addElement(thirdLine);
		pdfWordCell3.setBorderWidth(2);
		buildingHeightAndParkingtable.addCell(pdfWordCell3);

	}

	private String getBuildingBlockHeight(List<Map<String, Object>> buildingBlocksInfoList) {
		String blockHeightString = "";
		Integer blockCount = 0;
		for (Map<String, Object> buildingBlockDetails : buildingBlocksInfoList) {
			blockCount = blockCount + 1;
			if (!StringUtils.isEmpty(blockHeightString)) {
				blockHeightString = blockHeightString + " , ";
			}
			blockHeightString = blockHeightString + "B" + blockCount + "-" + buildingBlockDetails.get("height");

		}
		return blockHeightString;
	}

	private List<Floor> getFloorDetails(Object objectFloorList) {
		ObjectMapper objectMapper = new ObjectMapper();

		java.util.List<Floor> floors = objectMapper.convertValue(objectFloorList, new TypeReference<List<Floor>>() {
		});
		return floors;
	}

	protected PdfPTable getTotalNoOfDwellingUnitsTable(LinkedHashMap<String, Object> buildingRegularizationInfo) {
		PdfPTable totalNoOfDwellingUnitsTable = new PdfPTable(1);
		totalNoOfDwellingUnitsTable.setWidthPercentage(100f);
		addRowsTotalDewellingUnits(totalNoOfDwellingUnitsTable,
				getValue(buildingRegularizationInfo, "$.otherDetails.totalNoOfDwellingUnits"));
		return totalNoOfDwellingUnitsTable;
	}

	protected PdfPTable getBuildingAccessoryDetailTable(LinkedHashMap regularizations,
			LinkedHashMap<String, Object> buildingRegularizationAdditionalDetails) {
		PdfPTable buildingAccessoryDetailTable = new PdfPTable(3);
		buildingAccessoryDetailTable.setWidthPercentage(100f);
		addBuildingAccessoryDetailTableHeader(buildingAccessoryDetailTable, buildingRegularizationAdditionalDetails);
		return buildingAccessoryDetailTable;
	}

	protected PdfPTable getTotalAreaTable(java.util.List<Map<String, Object>> buildingBlocksInfoList) {
		PdfPTable totalAreaTable = new PdfPTable(1);
		totalAreaTable.setWidthPercentage(100f);
		BigDecimal totalFloorArea = BigDecimal.ZERO;
		BigDecimal totalBUA = BigDecimal.ZERO;
		for (Map<String, Object> block : buildingBlocksInfoList) {
			java.util.List<Floor> floorDetails = getFloorDetails(block.get("floors"));
			for (Floor floor : floorDetails) {
				if(StringUtils.isNotEmpty(floor.getAsBuiltFARArea()))
					totalFloorArea = totalFloorArea.add(BigDecimal.valueOf(Double.parseDouble(floor.getAsBuiltFARArea())));
				if(StringUtils.isNotEmpty(floor.getAsBuiltBUA()))
					totalBUA = totalBUA.add(BigDecimal.valueOf(Double.parseDouble(floor.getAsBuiltBUA())));

			}

		}
		addTotalAreaTableHeader(totalAreaTable, totalFloorArea, totalBUA);
		return totalAreaTable;
	}

	protected PdfPTable getFarTable(LinkedHashMap<String, Object> buildingRegularizationInfo) {
		PdfPTable farTable = new PdfPTable(3);
		farTable.setWidthPercentage(100f);
		addFarTableRows(farTable, buildingRegularizationInfo);
		return farTable;
	}

	protected PdfPTable getBuildingHeightAndParkingtable(
			LinkedHashMap<String, Object> buildingRegularizationAdditionalDetails,
			List<Map<String, Object>> buildingBlocksInfoList, BigDecimal totalParkingArea) {
		PdfPTable buildingHeightAndParkingtable = new PdfPTable(2);
		buildingHeightAndParkingtable.setWidthPercentage(100f);
		addBuildingHeightAndParkingTableRows(buildingHeightAndParkingtable, buildingRegularizationAdditionalDetails,
				buildingBlocksInfoList, totalParkingArea);
		return buildingHeightAndParkingtable;
	}

	protected PdfPTable getSetbackTable(List<Map<String, Object>> buildingBlocksInfoList) {
		PdfPTable setbackTable = new PdfPTable(3);
		setbackTable.setWidthPercentage(100f);
		addSetbackTableHeader(setbackTable);
		addSetbackTableRows(setbackTable, buildingBlocksInfoList);
		return setbackTable;
	}
	
	public String getTotalPlotArea(LinkedHashMap regularizations) {

		List<Map<String, Object>> plotList = getPlotAndKhataDetails(regularizations);
		Double totalPlotArea = 0.0d;

		for(Map<String,Object> plot:plotList){
			totalPlotArea = totalPlotArea + Double.parseDouble(plot.get("plotArea") == null ? "0": plot.get("plotArea").toString());
		}

		return totalPlotArea.toString();
	}
	
	public String getAreaTobeGiftedSinglePlot(LinkedHashMap regularizations) {
		
		List<Map<String, Object>> plotList = getPlotAndKhataDetails(regularizations);	
		Map<String, Object> singlePlot = (Map) plotList.get(0);
		
		return (String) (singlePlot.get("areaToBeGifted") == null ? "0": singlePlot.get("areaToBeGifted"));
	}
	
public String getNetPlotAreaSinglePlot(LinkedHashMap regularizations) {
		
		Float giftedArea = Float.valueOf(0);
		Float totalArea = Float.valueOf(0);
		try {
			giftedArea = Float.parseFloat(getAreaTobeGiftedSinglePlot(regularizations));			
			totalArea = Float.parseFloat(getTotalPlotArea(regularizations));
	
		} catch (Exception e) {
			LOG.error("total plot area not found in the payload!");
			e.printStackTrace();
		}
		return (totalArea-giftedArea)+"";
	}

}
