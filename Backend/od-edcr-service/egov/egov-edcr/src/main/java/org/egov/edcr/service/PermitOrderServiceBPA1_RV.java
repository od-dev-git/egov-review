package org.egov.edcr.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.egov.common.entity.edcr.Block;
import org.egov.common.entity.edcr.DcrReportBlockDetail;
import org.egov.common.entity.edcr.DcrReportFloorDetail;
import org.egov.common.entity.edcr.OdishaParkingHelper;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.PlanInformation;
import org.egov.common.entity.edcr.SetBack;
import org.egov.common.entity.edcr.TypicalFloor;
import org.egov.edcr.constants.DxfFileConstants;
import org.egov.edcr.feature.Parking;
import org.egov.edcr.od.OdishaUtill;
import org.egov.infra.exception.ApplicationRuntimeException;
import org.egov.infra.microservice.models.RequestInfo;
import org.springframework.stereotype.Service;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.List;
import com.itextpdf.text.ListItem;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

@Service
public class PermitOrderServiceBPA1_RV extends PermitOrderService {
	public Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA, 16, Font.BOLD);
	public Font font1 = FontFactory.getFont(FontFactory.HELVETICA, 12);
	public Font fontBold = FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD);
	public Font fontBoldUnderlined = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Font.UNDERLINE);

	public static String PARAGRAPH_1_7 = "Khata No.";
	public static String PARAGRAPH_1_9 = "Village/Mouza.";
	public static String PARAGRAPH_1_3 = "storeyed ";
	public static String PARAGRAPH_1_11 = "of %s within the Development Plan Area subject to following conditions/ restrictions: ";
	public static String PARAGRAPH_1_5 = "Building in respect of plot No.";
	
	public static String PERMITPRVIEW = "PERMIT LETTER PREVIEW";

	private static final Logger LOG = Logger.getLogger(PermitOrderServiceBPA1.class);
	
	@Override
	public InputStream generateReport(Plan plan, LinkedHashMap bpaApplication, RequestInfo requestInfo,
			Boolean isPreview) {
		try {
			return createPdf(plan, bpaApplication, requestInfo, isPreview);
		} catch (Exception e) {
			e.printStackTrace();
			throw new ApplicationRuntimeException("Error while generating permit order pdf ", e);
		}
	}

	public InputStream createPdf(Plan plan, LinkedHashMap bpaApplication, RequestInfo requestInfo, Boolean isPreview)
			throws Exception {

		if (plan == null) {
			plan = new Plan();

			Boolean nonSujogRevalidation = Boolean.TRUE;

			Map<String, Object> additionalDetails = (Map<String, Object>) bpaApplication.get("additionalDetails");
			DocumentContext additionalDetailsContext = JsonPath.parse(additionalDetails);

			String plotNo = additionalDetailsContext.read("$.plot[0].plotNumber");
			String khataNo = additionalDetailsContext.read("$.plot[0].khata");
			java.util.Set<String> ocList = new LinkedHashSet<>(additionalDetailsContext
					.read("$.RevalidationPlanNonSujog.edcr.blockDetail[*].blocks[*].occupancyName"));

			String ocString = String.join(",", ocList);
			PlanInformation planInformation = new PlanInformation();
			String floorInfo = additionalDetailsContext.read("$.RevalidationPlanNonSujog.noOfFloors"); // floor
																										// description
																										// in form G + 1
			planInformation.setFloorInfo(floorInfo);
			planInformation.setSubOccupancy(ocString); // occupancy csv ex : Residential, Commercial
			planInformation.setKhataNo(khataNo);
			planInformation.setPlotNo(plotNo);

			plan.setPlanInformation(planInformation);

			bpaApplication.put("edcrNumber", "NA");
		}
		String tenantIdActual = getValue(bpaApplication, "tenantId");
		Document document = new Document();
		ByteArrayOutputStream outputBytes;
		outputBytes = new ByteArrayOutputStream();
		PdfWriter.getInstance(document, outputBytes);
		document.open();
		Image logo = getLogo();
		String[] ulbGradeNameAndUlbArray = getUlbNameAndGradeFromMdms(requestInfo, tenantIdActual);
		String ulbGradeNameAndUlb = (ulbGradeNameAndUlbArray[0] + " " + ulbGradeNameAndUlbArray[1]);
		String applicationNo = getValue(bpaApplication, "applicationNo");

		Map<String, Object> additionalDetails = getAdditionalDetailsMap(bpaApplication);
		// Cuttack Municipal Corporation
		Paragraph headerTitle = new Paragraph(ulbGradeNameAndUlb, fontHeader);
		headerTitle.setAlignment(Paragraph.ALIGN_CENTER);
		
		Paragraph permitPreviewTitle = new Paragraph(PERMITPRVIEW, fontHeader);
        permitPreviewTitle.setAlignment(Paragraph.ALIGN_CENTER);

		Paragraph headerSubTitle = new Paragraph("REVALIDATION OF PERMISSION UNDER SECTION-20 OF THE ODISHA \n"
				+ "DEVELOPMENT AUTHORITIES ACT, 1982 (ODISHA ACT-14 OF 1982)", font1);
		headerSubTitle.setAlignment(Paragraph.ALIGN_CENTER);

		String tenantId = StringUtils.capitalize(tenantIdActual.split("\\.")[1]);

		String approvalNo = getValue(bpaApplication, "approvalNo");
		String approvalDate = getApprovalDate();
		String applicationDateString = getValue(bpaApplication, "applicationDate");
		long timestamp = Long.parseLong(applicationDateString);
		Date applicationDateEpoch = new Date(timestamp);
		DateFormat format = new SimpleDateFormat("dd/MM/yyyy");
		String applicationDate = format.format(applicationDateEpoch);
		String ownersNamesCsv = getNameOfOwner(bpaApplication);
		
		String oldApprovalNo = null;
		Long oldApprovalDate1 = null;
		String oldApprovalDate2 = "";

		if (additionalDetails != null && additionalDetails.get("oldApplicationDetail") instanceof Map) {
		    Map<String, Object> oldApplicationDetail = (Map<String, Object>) additionalDetails.get("oldApplicationDetail");

		    if (oldApplicationDetail != null) {
		        oldApprovalNo = (String) oldApplicationDetail.get("permitNumber");
		        oldApprovalDate1 = (Long) oldApplicationDetail.get("oldPermitDate");
		        
		        
		        oldApprovalDate2 = DateTimeFormatter.ofPattern("dd-MM-yyyy")
		                .withZone(ZoneId.systemDefault())
		                .format(Instant.ofEpochMilli(oldApprovalDate1));
		    }
		}
		
		Paragraph headerSubTitle2 = new Paragraph(
				"Letter No. " + approvalNo + ", " + tenantId + ", Dated: " + approvalDate, fontBold);
		headerSubTitle2.setAlignment(Paragraph.ALIGN_CENTER);

		Paragraph permitPreviewSubTitle2 = new Paragraph(
				"Letter No. " + DxfFileConstants.NA + ", " + tenantId + ", Dated: " + DxfFileConstants.NA, fontBold);
		permitPreviewSubTitle2.setAlignment(Paragraph.ALIGN_CENTER);
		
		Paragraph headerSubTitle4 = new Paragraph("Sujog-OBPS APPLICATION NO. " + applicationNo, fontBoldUnderlined);
		headerSubTitle4.setAlignment(Paragraph.ALIGN_CENTER);

		String localityName = getValue(bpaApplication, "$.landInfo.address.locality.name");
		Paragraph address2 = new Paragraph(localityName + ",", fontBold);
		address2.setIndentationLeft(30);
		
		// dynamic Other conditions from additionalDetails of application--
		Chunk otherConditions = new Chunk("", font1);
		if (Objects.nonNull(additionalDetails)
				&& Objects.nonNull(additionalDetails.get("otherConditionsForPermitCertificate"))) {
			otherConditions = new Chunk(String.valueOf("\n\n"+additionalDetails.get("otherConditionsForPermitCertificate"))+"\n\n", font1);
		}
		
		//permission given by
		Paragraph permissionBy = new Paragraph("Permission given by the " + ulbGradeNameAndUlb + " under the provision of Odisha\n" + "Development Authorities Act - 1982 vide no. " + (oldApprovalNo!=null?oldApprovalNo:"") + " dated " + oldApprovalDate2 + " (refer Annexure)", font1);
				
		String floorInfo = plan.getPlanInformation().getFloorInfo() + " ";
		Chunk floorInform = new Chunk(floorInfo, fontBold);
		Chunk storeyed = new Chunk(PARAGRAPH_1_3, font1);
		String subOccupancy = plan.getPlanInformation().getSubOccupancy() + " ";
		Chunk subOccupanc = new Chunk(subOccupancy, fontBold);
		
		Chunk withinDevelopmentPlanArea = new Chunk(String.format(PARAGRAPH_1_11, tenantId), font1);
		Phrase paragraph2 = new Phrase();
		paragraph2.add("For construction of a ");
		paragraph2.add(floorInform);
		paragraph2.add(storeyed);
		paragraph2.add(subOccupanc);
		paragraph2.add(" Building for " + ownersNamesCsv +" used in respect of");
		
		PdfPTable plotTable = new PdfPTable(7);
		plotTable.setLockedWidth(false);
		plotTable.setWidthPercentage(100f);
		addTableHeaderPlotTable(plotTable);
		addRowsForPlotTable(requestInfo, plotTable, additionalDetails, tenantIdActual);
		
		Phrase paragraph2_1 = new Phrase();
		paragraph2_1.add("In the Master Plan area ");
		paragraph2_1.add(withinDevelopmentPlanArea);
		paragraph2_1.add(otherConditions);
		paragraph2_1.add(
				" is revalidated under section-20 of the Odisha Development Authorities Act-1982 for a period of one year from the date of approval subject to the same conditions and restrictions as indicated in the above referred letter.");
		Paragraph secondPara = new Paragraph(paragraph2);
		
		Paragraph secondPara1 = new Paragraph(paragraph2_1);

		//officer section
		Paragraph approvedByParagraph = new Paragraph();
		String approvedBy = "isPreview "+isPreview;
		
		if(!isPreview) {
			approvedBy = getValue(bpaApplication, "$.dscDetails[0].approvedBy");
		}

		//LOG.info("user (approvedBy):" + approvedBy);
		Map<String, Object> approverDetailsMap = getApproverDetails(requestInfo, tenantId, "BPA1", approvedBy,
				"EMPLOYEE");
		
		String[] bpaPermitHeader = getUlbNameAndGradeFromMdms(requestInfo, tenantIdActual);
		
		String approverName = !org.springframework.util.StringUtils
				.isEmpty(approverDetailsMap.get("nameOfApprover")) ? approverDetailsMap.get("nameOfApprover") + ""
						: "";
		//LOG.info("Right side of the Permit Letter");
		Font fontParaApprovedBy = FontFactory.getFont(FontFactory.HELVETICA, 12);
		Phrase approvedBySection = new Phrase();
		Chunk approvedBySectionLine1 = new Chunk("BY ORDER OF" + "\n", fontParaApprovedBy);
		Chunk approvedBySectionLine2 = new Chunk(approverName + "\n", fontParaApprovedBy);
		Chunk approvedBySectionLine3 = new Chunk("Authorized Officer" + "\n", fontParaApprovedBy);
		Chunk approvedBySectionLine4 = new Chunk(bpaPermitHeader[2]);
		approvedBySection.add(approvedBySectionLine1);
		approvedBySection.add(approvedBySectionLine2);
		approvedBySection.add(approvedBySectionLine3);
		approvedBySection.add(approvedBySectionLine4);
		approvedByParagraph.add(approvedBySection);
		approvedByParagraph.setAlignment(Element.ALIGN_RIGHT);

		Image qrCode = getQrCode(ownersNamesCsv, getValue(bpaApplication, "approvalNo"), approvalDate,
				getValue(bpaApplication, "edcrNumber"));
		
		if (isPreview) {
			document.add(permitPreviewTitle);
			document.add(Chunk.NEWLINE);
			document.add(permitPreviewSubTitle2);
		} else {
		document.add(qrCode);
		document.add(logo);
		document.add(headerTitle);
		document.add(headerSubTitle);
		document.add(Chunk.NEWLINE);
		document.add(headerSubTitle2);
		}
		document.add(headerSubTitle4);
		document.add(Chunk.NEWLINE);
		document.add(Chunk.NEWLINE);
		document.add(permissionBy);
		document.add(Chunk.NEWLINE);
		document.add(secondPara);
		document.add(Chunk.NEWLINE);
		document.add(plotTable);
		document.add(Chunk.NEWLINE);
		document.add(secondPara1);
		document.add(Chunk.NEWLINE);
		document.add(approvedByParagraph);	
		document.close();
		return new ByteArrayInputStream(outputBytes.toByteArray());
	}
}