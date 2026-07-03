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
import org.egov.infra.exception.ApplicationRuntimeException;
import org.egov.infra.microservice.models.RequestInfo;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.CollectionUtils;

import com.itextpdf.text.BadElementException;
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
import com.jayway.jsonpath.JsonPathException;
import com.jayway.jsonpath.PathNotFoundException;

public abstract class LandRegCertificateService {
	
	private static final Logger LOG = Logger.getLogger(LandRegCertificateService.class);
	
	public Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA, 16, Font.BOLD);
	public Font font1 = FontFactory.getFont(FontFactory.HELVETICA, 12);
	public Font fontBold = FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD);
	public Font fontBoldUnderlined = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Font.UNDERLINE);
	
	public static final String PAYMENTS_RESPONSE_FIELD = "Payments";
	public static final String TAXHEAD_REG_COMPOUNDING_FEE_CODE = "REG_LAND_COMP_FEE";
	public static final String TAXHEAD_REG_COMPOUNDING_FEE_NAME = "Compounding fee for regularization of sub-plots";
	
	public static final String TAXHEAD_REG_LAND_DEV_FEE_CODE =  "REG_LAND_DEV_FEE";
	public static final String TAXHEAD_REG_LAND_DEV_FEE_NAME = "Scrutiny-fee (Land development fee)";
	
	public static final String TAXHEAD_REG_SANC_ADJUSTMENT_AMOUNT_FEE_CODE =  "REG_SANC_ADJUSTMENT_AMOUNT";
	public static final String TAXHEAD_REG_SANC_ADJUSTMENT_AMOUNT_FEE_NAME = "Other Fee";
	
	public static String OTHER_CONDITIONS ="Other conditions to be complied by the applicant are as per the following:";
	
	
	@Autowired
	private MdmsService mdmsService;
	
	@Autowired
	private PaymentService paymentService;
	
	@Autowired
	private UserInfoService userService;
	
	@Autowired
	private LocationVillageService locationVillageService;
	
	public abstract InputStream generateReport(LinkedHashMap regularizations, RequestInfo requestInfo, Boolean isPreview);
	
	
	public String[] getUlbNameAndGradeFromMdms(RequestInfo requestInfo, String tenantId) {
		return mdmsService.getUlbNameAndGradeFromMdms(requestInfo, tenantId);
	}
	
	
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
	
	public void addTableHeaderPlotTable(PdfPTable table1) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD);
		Stream.of("Revenue Plot No.","Plot Area", "Khata No", "Kisam", "Village",
				"Land Owner Name", "GPA Holder Name").forEach(columnTitle -> {
					PdfPCell header = new PdfPCell();
					header.setBorderWidth(1);
					header.setVerticalAlignment(Element.ALIGN_MIDDLE);
					header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
					table1.addCell(header);

				});
	}
	
	
	public void addRowsForPlotTable(RequestInfo reqInfo, PdfPTable table1, LinkedHashMap regularizations, String tenantId) {
		List<Map<String, Object>> plotList = getPlotAndKhataDetails(regularizations);
		
		List<Object> boundaryList = getVillageList(reqInfo, tenantId);
		
		Font fontPara1 = FontFactory.getFont(FontFactory.HELVETICA, 12);
		if(!CollectionUtils.isEmpty(plotList)) {
			for(Map<String, Object> plots : plotList) {
				
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
				Phrase villageCellPhrase = new Phrase(getVillageFromVillageList(boundaryList, (String) plots.get("village"))  + "", fontPara1);
				villageCell.addElement(villageCellPhrase);
				
				PdfPCell landOwnerNameCell = new PdfPCell();
				Phrase landOwnerNameCellPhrase = new Phrase(plots.get("landOwnerName") + "", fontPara1);
				landOwnerNameCell.addElement(landOwnerNameCellPhrase);
				
				PdfPCell gpaHolderNameCell = new PdfPCell();
				Phrase gpaHolderNamePhrase = new Phrase(plots.get("gpaHolderName") != null ? plots.get("gpaHolderName")+ "":"N.A.", fontPara1);
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
	
	public void addTableHeaderPaymentTable(PdfPTable table1) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD);
		Stream.of("Sl No.","Item", "Amount (Rs)").forEach(columnTitle -> {
					PdfPCell header = new PdfPCell();
					header.setPadding(3f);
					header.setVerticalAlignment(Element.ALIGN_MIDDLE);
					header.setHorizontalAlignment(Element.ALIGN_MIDDLE);
					header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
					table1.addCell(header);

				});
	}
	
	public List<Map<String, Object>> getPlotAndKhataDetails(LinkedHashMap regularizations) {

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
	
	public List<Map<String, Object>> getEstimatedSanctionFeeDetails(RequestInfo requestInfo,
			LinkedHashMap regularizations) {
		Object estimatedFeeDetails = paymentService.fetchEstimatedFeePaymentForRegularization(requestInfo,
				regularizations, "SanctionFee");
		return getTaxHeadEstimatesfromPaymentResponse(estimatedFeeDetails);
	}
	
	public List<Map<String, Object>> getEstimatedApplicationFeeDetails(RequestInfo requestInfo,
			LinkedHashMap regularizations) {
		Object estimatedFeeDetails = paymentService.fetchEstimatedFeePaymentForRegularization(requestInfo,
				regularizations, "ApplicationFee");
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
	
	public void addRowsForEstimatedFees(RequestInfo requestInfo,PdfPTable paymentTable, LinkedHashMap regularizations){
		
		List<Map<String, Object>> estimatedSancFeeDetails =  getEstimatedSanctionFeeDetails(requestInfo, regularizations);
		int count=1;
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
		}
		
		List<Map<String, Object>> estimatedAppFeeDetails =  getEstimatedApplicationFeeDetails(requestInfo, regularizations);
		
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
		}	
	}
	
	
	public String getValue(Map dataMap, String key) {
		if(dataMap==null)
			return "";
		try {
		String jsonString = new JSONObject(dataMap).toString();
		DocumentContext context = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonString);
		return context.read(key) + "";
		}
		catch (PathNotFoundException p) {
			p.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}
	
	public String getAreaTobeGiftedSinglePlot(LinkedHashMap regularizations) {
		
		List<Map<String, Object>> plotList = getPlotAndKhataDetails(regularizations);	
		Map<String, Object> singlePlot = (Map) plotList.get(0);
		
		return (String) (singlePlot.get("areaToBeGifted") == null ? "0": singlePlot.get("areaToBeGifted"));
	}

	public String getAreaTobeGifted(LinkedHashMap regularizations) {

		List<Map<String, Object>> plotList = getPlotAndKhataDetails(regularizations);
		Double totalGiftedArea = 0.0d;

		for(Map<String,Object> plot:plotList){
			totalGiftedArea = totalGiftedArea + Double.parseDouble(plot.get("areaToBeGifted") == null ? "0": plot.get("areaToBeGifted").toString());
		}

		return totalGiftedArea.toString();
	}
	
	public String getTotalPlotArea(LinkedHashMap regularizations) {

		List<Map<String, Object>> plotList = getPlotAndKhataDetails(regularizations);
		Double totalPlotArea = 0.0d;

		for(Map<String,Object> plot:plotList){
			totalPlotArea = totalPlotArea + Double.parseDouble(plot.get("plotArea") == null ? "0": plot.get("plotArea").toString());
		}

		return totalPlotArea.toString();
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

	public String getNetPlotArea(LinkedHashMap regularizations) {

		Float giftedArea = Float.valueOf(0);
		Float totalArea = Float.valueOf(0);
		try {
			giftedArea = Float.parseFloat(getAreaTobeGifted(regularizations));
			totalArea = Float.parseFloat(getTotalPlotArea(regularizations));

		} catch (Exception e) {
			LOG.error("total plot area not found in the payload!");
			e.printStackTrace();
		}
		return (totalArea-giftedArea)+"";
	}


	
	public void addRowsForPaymentsTable(RequestInfo requestInfo, PdfPTable paymentTable,
			String tenantId, String consumercode) {

		Object regularizationPaymentDetails = paymentService.fetchRegularizationPaymentDetails(requestInfo,
				consumercode, tenantId);

		List<List<Map<String, Object>>> billAccountDetailsList = getBillAccountDetailsFromPaymentResponse(
				regularizationPaymentDetails);
		List<Map<String, Object>> finalBillAccountDetailsList = new ArrayList<>();
		billAccountDetailsList.forEach(finalBillAccountDetailsList::addAll);
		int count=1;
		for (Map<String, Object> bills : finalBillAccountDetailsList) {

			String adjustedAmount = String.valueOf(bills.get("adjustedAmount"));
			// skip those taxheadcodes for which adjustedAmount is 0-
//			if (StringUtils.isNotEmpty(adjustedAmount) && "0.0".equals(adjustedAmount)
//					&& !String.valueOf(bills.get("taxHeadCode")).equalsIgnoreCase("BLR_COMPOUNDING_FEE"))
//				continue;
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
		}

	}
	
	public List<List<Map<String, Object>>> getBillAccountDetailsFromPaymentResponse(Object regularalizationPaymentDetails) {
		List<Map<String, Object>> billAccountDetails = new ArrayList<>();
		List<List<Map<String, Object>>> billAccountDetailsList = new ArrayList<>();

		int paymentsLength = 1;
		if (Objects.nonNull(regularalizationPaymentDetails) && regularalizationPaymentDetails instanceof Map
				&& ((Map) regularalizationPaymentDetails).get(PAYMENTS_RESPONSE_FIELD) instanceof List
				&& !CollectionUtils.isEmpty((List) ((Map) regularalizationPaymentDetails).get(PAYMENTS_RESPONSE_FIELD))) {
			List payments = (List) ((Map) regularalizationPaymentDetails).get(PAYMENTS_RESPONSE_FIELD);
			paymentsLength = payments.size();
		}
		
		for(int i=0; i<paymentsLength; i++) {
			
			String jsonString = new JSONObject((Map) regularalizationPaymentDetails).toString();
			DocumentContext context = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonString);
			billAccountDetails = context.read(
					"$.Payments[" + i + "].paymentDetails[0].bill.billDetails[0].billAccountDetails");
			billAccountDetailsList.add(billAccountDetails);
		}
		
		return billAccountDetailsList;
	}
	
	public String getFeeComponentNameFromTaxHeadCode(String taxHeadCode) {
		String taxHeadName = "";
		switch (taxHeadCode) {
		case TAXHEAD_REG_COMPOUNDING_FEE_CODE:
			taxHeadName = TAXHEAD_REG_COMPOUNDING_FEE_NAME;
			break;
		case TAXHEAD_REG_LAND_DEV_FEE_CODE:
			taxHeadName = TAXHEAD_REG_LAND_DEV_FEE_NAME;
			break;
			
		case TAXHEAD_REG_SANC_ADJUSTMENT_AMOUNT_FEE_CODE:
			taxHeadName = TAXHEAD_REG_SANC_ADJUSTMENT_AMOUNT_FEE_NAME;
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

        String[] smallRomanNumerals = {"i", "ii", "iii", "iv", "v", "vi", "vii", "viii", "ix", "x", "xi", "xii", "xiii", "xiv", "xv", "xvi", "xvii", "xviii", "xix", "xx", "xxi", "xxii", "xxiii", "xxiv", "xxv", "xxvi"};
        return smallRomanNumerals[number - 1];
    }
	
	//approval date is the date at which land reg certificate is signed.
	public String getApprovalDate() {
		Date date = new Date(Calendar.getInstance().getTimeInMillis());
		DateFormat format = new SimpleDateFormat("dd/MM/yyyy");
		String approvalDate = format.format(date);
		return approvalDate;
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
	
	public Map<String, Object> getAdditionalDetailsMap(Map regularizations) {
		String jsonString = new JSONObject(regularizations).toString();
		DocumentContext context = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonString);
		return context.read("additionalDetails");
	}

}
