package org.egov.edcr.service;

import java.io.InputStream;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.egov.common.entity.edcr.Block;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.SetBack;
import org.egov.infra.exception.ApplicationRuntimeException;
import org.egov.infra.microservice.models.RequestInfo;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.CollectionUtils;

import com.itextpdf.text.BadElementException;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.BarcodeQRCode;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

public abstract class OccupancyCertificateService {
	
	public static final String PAYMENTS_RESPONSE_FIELD = "Payments";

	private static final Logger LOG = Logger.getLogger(OccupancyCertificateService.class);

	public static final String PLOT = "plot";
	public Font font1 = FontFactory.getFont(FontFactory.HELVETICA, 12);
	
	public static final String TAXHEAD_BPA_OC_CERT_FEE_CODE = "BPA_OC_CERT_FEE";
	public static final String TAXHEAD_BPA_OC_CERT_FEE_NAME = "Occupancy Certificate Application Fee";
	
	public static final String TAXHEAD_BPA_OC_SANC_GRAND_OCCUPANCY_CERT_FEE_CODE = "BPA_OC_SANC_GRAND_OCCUPANCY_CERT_FEE";
	public static final String TAXHEAD_BPA_OC_SANC_GRAND_OCCUPANCY_CERT_FEE_NAME = "Fee for grant of Occupancy Certificate";
	
	public static final String TAXHEAD_BPA_OC_SANC_ADJUSTMENT_AMOUNT_1_CODE = "BPA_OC_SANC_ADJUSTMENT_AMOUNT_1";
	public static final String TAXHEAD_BPA_OC_SANC_ADJUSTMENT_AMOUNT_1_NAME = "Occupancy certificate adjustment amount";
	
	public static final String TAXHEAD_BPA_OC_SANC_COMPOUND_FAR_FEE_CODE = "BPA_OC_SANC_COMPOUND_FAR_FEE";
	public static final String TAXHEAD_BPA_OC_SANC_COMPOUND_FAR_FEE_NAME = "BPA OC Compounding Fee for FAR";
	
	public static final String TAXHEAD_BPA_OC_SANC_WORKER_WELFARE_CESS_CODE = "BPA_OC_SANC_WORKER_WELFARE_CESS";
	public static final String TAXHEAD_BPA_OC_SANC_WORKER_WELFARE_CESS_NAME = "BPA OC Construction worker welfare Cess (CWWC)";
	
	public static final String TAXHEAD_BPA_OC_SANC_COMPOUND_SETBACK_FEE_CODE = "BPA_OC_SANC_COMPOUND_SETBACK_FEE";
	public static final String TAXHEAD_BPA_OC_SANC_COMPOUND_SETBACK_FEE_NAME = "BPA OC Compounding Fee for Setback";
	
	public static final String TAXHEAD_BPA_OC_SCRUTINY_FEE_CODE = "BPA_OC_SCRUTINY_FEE";
	public static final String TAXHEAD_BPA_OC_SCRUTINY_FEE_NAME = "BPA OC Scrutiny Fee";

	public abstract InputStream generateReport(LinkedHashMap bpaList, RequestInfo requestInfo, Boolean isPreview)
			throws Exception;

	@Autowired
	private MdmsService mdmsService;

	@Autowired
	private PaymentService paymentService;

	@Autowired
	private UserInfoService userService;

	@Autowired
	private LocationVillageService locationVillageService;

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

	public Object getValue(Map dataMap, String key) {
		Object value = "";
		try {
		String jsonString = new JSONObject(dataMap).toString();
		DocumentContext context = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonString);
		
			value = context.read(key);
		} catch (PathNotFoundException p) {
//			p.printStackTrace();
			LOG.error("Path not found: "+key);
		} catch (Exception e) {
//			e.printStackTrace();
			LOG.error("Exception while parsing json string: "+key);
		}
		return value;
	}

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

	public String[] getUlbNameAndGradeFromMdms(RequestInfo requestInfo, String tenantId) {
		return mdmsService.getUlbNameAndGradeFromMdms(requestInfo, tenantId);
	}

	public String getApprovalDate() {
		Date date = new Date(Calendar.getInstance().getTimeInMillis());
		DateFormat format = new SimpleDateFormat("dd/MM/yyyy");
		String approvalDate = format.format(date);
		return approvalDate;
	}

	public Map<String, Object> getAdditionalDetailsMap(Map bpa) {
		String jsonString = new JSONObject(bpa).toString();
		DocumentContext context = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonString);
		return context.read("additionalDetails");
	}

	public List<Map<String, Object>> getPlotAndKhataDetails(Map<String, Object> additionalDetails) {

		List<Map<String, Object>> plotList = new ArrayList<>();

		if (Objects.nonNull(additionalDetails) && Objects.nonNull(additionalDetails.get(PLOT))
				&& additionalDetails.get(PLOT) instanceof List
				&& !CollectionUtils.isEmpty((List) additionalDetails.get(PLOT))) {

			plotList = (List) additionalDetails.get(PLOT);
		}

		return plotList;

	}

	public void addTableHeaderPlotTable(PdfPTable plotTable) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD);
		Stream.of("Plot No.", "Plot Area", "Khata No.", "Kisam", "Village", "Land Owner Name", "GPA Holder Name")
				.forEach(columnTitle -> {
					PdfPCell header = new PdfPCell();
					header.setBorderWidth(1);
					header.setVerticalAlignment(Element.ALIGN_MIDDLE);
					header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
					plotTable.addCell(header);

				});
	}

	public void addRowsForPlotTable(RequestInfo reqInfo, PdfPTable table1, List<Map<String, Object>> plotList,
			String tenantId) {

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
		
		case TAXHEAD_BPA_OC_CERT_FEE_CODE:
			taxHeadName = TAXHEAD_BPA_OC_CERT_FEE_NAME;
			break;

		case TAXHEAD_BPA_OC_SANC_GRAND_OCCUPANCY_CERT_FEE_CODE:
			taxHeadName = TAXHEAD_BPA_OC_SANC_GRAND_OCCUPANCY_CERT_FEE_NAME;
			break;
			
		case TAXHEAD_BPA_OC_SANC_COMPOUND_FAR_FEE_CODE:
			taxHeadName = TAXHEAD_BPA_OC_SANC_COMPOUND_FAR_FEE_NAME;
			break;
			
		case TAXHEAD_BPA_OC_SANC_WORKER_WELFARE_CESS_CODE:
			taxHeadName = TAXHEAD_BPA_OC_SANC_WORKER_WELFARE_CESS_NAME;
			break;
			
		case TAXHEAD_BPA_OC_SANC_COMPOUND_SETBACK_FEE_CODE:
			taxHeadName = TAXHEAD_BPA_OC_SANC_COMPOUND_SETBACK_FEE_NAME;
			break;
		
		case TAXHEAD_BPA_OC_SCRUTINY_FEE_CODE:
			taxHeadName = TAXHEAD_BPA_OC_SCRUTINY_FEE_NAME;
			break;
		
		default:
			taxHeadName = taxHeadCode;
		}
		return taxHeadName;
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
			String nameOfApprover = getValue((Map) userSearchResponseObject, "$.user[0].name").toString();
			String mobileNumberOfApprover = getValue((Map) userSearchResponseObject, "$.user[0].userName").toString();
			approverDetails.put("nameOfApprover", nameOfApprover);
			approverDetails.put("mobileNumberOfApprover", mobileNumberOfApprover);
		} catch (Exception ex) {
			LOG.error("error while extracting approver details", ex);
		}
		return approverDetails;
	}
	
	public void addTableHeaderPaymentTable(PdfPTable table1) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD);
		Stream.of("Sl No.", "Item", "Amount (Rs)").forEach(columnTitle -> {
			PdfPCell header = new PdfPCell();
			header.setPadding(3f);
			header.setVerticalAlignment(Element.ALIGN_MIDDLE);
			header.setHorizontalAlignment(Element.ALIGN_MIDDLE);
			header.setBackgroundColor(BaseColor.LIGHT_GRAY);
			header.setBorderWidth(2);
			header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
			table1.addCell(header);

		});
	}

	public void addRowsForPaymentsTable(RequestInfo requestInfo, PdfPTable paymentTable, String tenantId,
			String applicationNo) {

		Object ocPaymentDetails = paymentService.fetchOCPaymentDetails(requestInfo, applicationNo, tenantId);

		List<List<Map<String, Object>>> billAccountDetailsList = getBillAccountDetailsFromPaymentResponse(
				ocPaymentDetails);
		List<Map<String, Object>> finalBillAccountDetailsList = new ArrayList<>();
		billAccountDetailsList.forEach(finalBillAccountDetailsList::addAll);
		int count = 1;
		double totalAmountPaid = 0.0;

		for (Map<String, Object> bills : finalBillAccountDetailsList) {
			String adjustedAmount = String.valueOf(bills.get("adjustedAmount"));
			String taxHeadCode = String.valueOf(bills.get("taxHeadCode"));
			String taxHeadName = getFeeComponentNameFromTaxHeadCode(taxHeadCode);

			PdfPCell numberCell = new PdfPCell(new Phrase(toSmallRomanNumeral(count++)));
			numberCell.setPadding(6f);
			PdfPCell taxHeadCell = new PdfPCell(new Phrase(taxHeadName, font1));
			taxHeadCell.setPadding(6f);
			PdfPCell amountCell = new PdfPCell(new Phrase(adjustedAmount, font1));
			amountCell.setPadding(6f);

			paymentTable.addCell(numberCell);
			paymentTable.addCell(taxHeadCell);
			paymentTable.addCell(amountCell);

			totalAmountPaid += Double.parseDouble(adjustedAmount);
		}

		PdfPCell totalLabelCell = new PdfPCell(new Phrase("Total amount paid", font1));
		totalLabelCell.setColspan(2);
		totalLabelCell.setPadding(6f);
		PdfPCell totalAmountCell = new PdfPCell(new Phrase(String.valueOf(totalAmountPaid), font1));
		totalAmountCell.setPadding(6f);

		paymentTable.addCell(totalLabelCell);
		paymentTable.addCell(totalAmountCell);
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

	public void addRowsForEstimatedFees(RequestInfo requestInfo, PdfPTable paymentTable,
			LinkedHashMap bpaList) {

		List<Map<String, Object>> estimatedSancFeeDetails = getEstimatedSanctionFeeDetails(requestInfo,
				bpaList);
		int count = 1;
		double totalEstimatedAmount = 0.0;
		for (Map<String, Object> bills : estimatedSancFeeDetails) {

			String estimatedAmount = String.valueOf(bills.get("estimateAmount"));

			String taxHeadCode = String.valueOf(bills.get("taxHeadCode"));
			String taxHeadName = getFeeComponentNameFromTaxHeadCode(taxHeadCode);

			PdfPCell numberCell = new PdfPCell(new Phrase(toSmallRomanNumeral(count++)));
			numberCell.setPadding(6f);
			PdfPCell taxHeadCell = new PdfPCell(new Phrase(taxHeadName, font1));
			taxHeadCell.setPadding(6f);
			PdfPCell amountCell = new PdfPCell(new Phrase(estimatedAmount + " (Not Paid)", font1));
			amountCell.setPadding(6f);

			paymentTable.addCell(numberCell);
			paymentTable.addCell(taxHeadCell);
			paymentTable.addCell(amountCell);
			
			totalEstimatedAmount += Double.parseDouble(estimatedAmount);
		}

		List<Map<String, Object>> estimatedAppFeeDetails = getEstimatedApplicationFeeDetails(requestInfo,
				bpaList);

		for (Map<String, Object> bills : estimatedAppFeeDetails) {

			String estimatedAmount = String.valueOf(bills.get("estimateAmount"));

			String taxHeadCode = String.valueOf(bills.get("taxHeadCode"));
			String taxHeadName = getFeeComponentNameFromTaxHeadCode(taxHeadCode);

			PdfPCell numberCell = new PdfPCell(new Phrase(toSmallRomanNumeral(count++)));
			numberCell.setPadding(6f);
			PdfPCell taxHeadCell = new PdfPCell(new Phrase(taxHeadName, font1));
			taxHeadCell.setPadding(6f);
			PdfPCell amountCell = new PdfPCell(new Phrase(estimatedAmount + "(Paid)", font1));
			amountCell.setPadding(6f);

			paymentTable.addCell(numberCell);
			paymentTable.addCell(taxHeadCell);
			paymentTable.addCell(amountCell);
			
			totalEstimatedAmount += Double.parseDouble(estimatedAmount);
		}
		
		PdfPCell totalLabelCell = new PdfPCell(new Phrase("Total estimated amount", font1));
		totalLabelCell.setColspan(2);
		totalLabelCell.setPadding(6f);
		PdfPCell totalAmountCell = new PdfPCell(new Phrase(String.valueOf(totalEstimatedAmount), font1));
		totalAmountCell.setPadding(6f);

		paymentTable.addCell(totalLabelCell);
		paymentTable.addCell(totalAmountCell);
	}

	public void addRowsForEstimatedFeesOcInsideSujog(RequestInfo requestInfo, PdfPTable paymentTable,
			LinkedHashMap bpaList, String riskType) {

		List<Map<String, Object>> estimatedSancFeeDetails = getEstimatedSanctionFeeDetailsOCInsideSujog(requestInfo,
				bpaList, riskType);
		List<Map<String, Object>> estimatedAppFeeDetails = getEstimatedApplicationFeeDetailsOCInsideSujog(requestInfo,
				bpaList, riskType);

		int count = 1;
		double totalEstimatedAmount = 0.0;

		for (Map<String, Object> bills : estimatedSancFeeDetails) {
			String estimatedAmount = String.valueOf(bills.get("estimateAmount"));
			String taxHeadCode = String.valueOf(bills.get("taxHeadCode"));
			String taxHeadName = getFeeComponentNameFromTaxHeadCode(taxHeadCode);

			PdfPCell numberCell = new PdfPCell(new Phrase(toSmallRomanNumeral(count++)));
			numberCell.setPadding(6f);
			PdfPCell taxHeadCell = new PdfPCell(new Phrase(taxHeadName, font1));
			taxHeadCell.setPadding(6f);
			PdfPCell amountCell = new PdfPCell(new Phrase(estimatedAmount + " (Not Paid)", font1));
			amountCell.setPadding(6f);

			paymentTable.addCell(numberCell);
			paymentTable.addCell(taxHeadCell);
			paymentTable.addCell(amountCell);

			totalEstimatedAmount += Double.parseDouble(estimatedAmount);
		}

		for (Map<String, Object> bills : estimatedAppFeeDetails) {
			String estimatedAmount = String.valueOf(bills.get("estimateAmount"));
			String taxHeadCode = String.valueOf(bills.get("taxHeadCode"));
			String taxHeadName = getFeeComponentNameFromTaxHeadCode(taxHeadCode);

			PdfPCell numberCell = new PdfPCell(new Phrase(toSmallRomanNumeral(count++)));
			numberCell.setPadding(6f);
			PdfPCell taxHeadCell = new PdfPCell(new Phrase(taxHeadName, font1));
			taxHeadCell.setPadding(6f);
			PdfPCell amountCell = new PdfPCell(new Phrase(estimatedAmount + " (Paid)", font1));
			amountCell.setPadding(6f);

			paymentTable.addCell(numberCell);
			paymentTable.addCell(taxHeadCell);
			paymentTable.addCell(amountCell);

			totalEstimatedAmount += Double.parseDouble(estimatedAmount);
		}

		PdfPCell totalLabelCell = new PdfPCell(new Phrase("Total estimated amount", font1));
		totalLabelCell.setColspan(2);
		totalLabelCell.setPadding(6f);
		PdfPCell totalAmountCell = new PdfPCell(new Phrase(String.valueOf(totalEstimatedAmount), font1));
		totalAmountCell.setPadding(6f);

		paymentTable.addCell(totalLabelCell);
		paymentTable.addCell(totalAmountCell);
	}

	public List<Map<String, Object>> getEstimatedSanctionFeeDetails(RequestInfo requestInfo,
			LinkedHashMap bpaApplication) {
		Object estimatedFeeDetails = paymentService.fetchEstimatedSanctionFeePaymentOCOutsideSujog(requestInfo,
				bpaApplication);
		return getTaxHeadEstimatesfromPaymentResponse(estimatedFeeDetails);
	}
	
	public List<Map<String, Object>> getEstimatedSanctionFeeDetailsOCInsideSujog(RequestInfo requestInfo,
			LinkedHashMap bpaApplication, String riskType) {
		Object estimatedFeeDetails = paymentService.fetchEstimatedSanctionFeePaymentOCInsideSujog(requestInfo,
				bpaApplication, riskType);
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
	public List<Map<String, Object>> getEstimatedApplicationFeeDetails(RequestInfo requestInfo,
			LinkedHashMap bpaApplication) {
		Object estimatedFeeDetails = paymentService.fetchEstimatedApplicationFeePaymentForOCOutsideSujog(requestInfo,
				bpaApplication);
		return getTaxHeadEstimatesfromPaymentResponse(estimatedFeeDetails);
	}
	
	public List<Map<String, Object>> getEstimatedApplicationFeeDetailsOCInsideSujog(RequestInfo requestInfo,
			LinkedHashMap bpaApplication, String riskType) {
		Object estimatedFeeDetails = paymentService.fetchEstimatedApplicationFeePaymentForOCinsideSujog(requestInfo,
				bpaApplication, riskType);
		return getTaxHeadEstimatesfromPaymentResponse(estimatedFeeDetails);
	}
	
	public Map<String,String> getArchitectDetails(RequestInfo requestInfo, String ownerIds){
		Map<String,String> responseMap= new HashMap<>();
		Map<String,Object> architectUserInfo= (Map<String, Object>) userService.fetchArchitectInfo(requestInfo, ownerIds);
		if(architectUserInfo==null) {
			responseMap.put("architectName", "");
			responseMap.put("architectId", "");
			return responseMap;
		}
		List<Map<String,Object>> licenses= (List<Map<String, Object>>) architectUserInfo.get("Licenses");
		String architectName="";
		String architectId="";
		for(Map<String,Object> license:licenses) {
			String archIdfromResponse=getValue(license, "$.tradeLicenseDetail.additionalDetail.counsilForArchNo").toString();
			if(StringUtils.isNotEmpty(archIdfromResponse)) {
				architectName=getValue(license, "$.tradeLicenseDetail.owners[0].name").toString();
				architectId=archIdfromResponse;
			}
		}
		
		responseMap.put("architectName", architectName);
		responseMap.put("architectId", architectId);
		return responseMap;
	}

}
