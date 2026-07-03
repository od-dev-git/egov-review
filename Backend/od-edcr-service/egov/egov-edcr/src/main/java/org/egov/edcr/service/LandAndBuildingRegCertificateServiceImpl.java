package org.egov.edcr.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import javax.print.DocFlavor.STRING;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.egov.common.entity.edcr.DcrReportBlockDetail;
import org.egov.common.entity.edcr.DcrReportFloorDetail;
import org.egov.common.entity.edcr.OdishaParkingHelper;
import org.egov.common.entity.edcr.Plan;
import org.egov.edcr.constants.DxfFileConstants;
import org.egov.edcr.constants.NOCConstants;
import org.egov.edcr.constants.OdishaUlbs;
import org.egov.edcr.feature.Parking;
import org.egov.edcr.od.OdishaUtill;
import org.egov.infra.exception.ApplicationRuntimeException;
import org.egov.infra.microservice.models.RequestInfo;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
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

@Service
public class LandAndBuildingRegCertificateServiceImpl extends LandAndBuildingRegCertificateService {

	private static final Logger LOG = Logger.getLogger(LandAndBuildingRegCertificateServiceImpl.class);

	public static String PARAGRAPH_ONE = "Permission under Scheme for regularization of unauthorised layout/subdivided plots, notified in the Extraordinary Gazette no.1034 dated 30 May 2017 and Permission under sub-section (3) of the Section-16 of the Odisha Development Authorities Act, 1982 (Act 14 of 1982) is hereby granted in favour of Land Owner";
	// public static String PARAGRAPH_ONE_OLD = "Permission under sub-section (3) of
	// the Section-16 of the Odisha Development Authorities Act, 1982 is hereby
	// granted in favour of";

	// public static String PARAGRAPH_ONE_SPARIT = "Permission/Licence under sub
	// section(3) of section 31/ sub section (1) of section 33 of Odisha Town
	// Planning & Improvement Trust Act 1956 is hereby granted in favour of Land
	// Owner";
	// public static String PARAGRAPH_ONE_SPARIT_OLD = "Permission/Licence under sub
	// section(3) of section 31/ sub section (1) of section 33 of Odisha Town
	// Planning & Improvement Trust Act 1956 is hereby granted in favour of";

	public static String ADDRESS = "LAXMIPRIYA PANDA AND OTHERS,";
	public static String ADDRESS2 = "KALARAHANGA";
	public static String ADDRESS1 = "INJANA,";
	public static String PERMITPRVIEW = "Land & Building Regularization Certificate Preview";
	public static String PARAGRAPH_0_1 = "And Building Regularization of a %1$s storeyed building within the Development Plan Area of %2$s subject to following parameter and conditions/restrictions.";


	public static String PARAGRAPH_1_5_OLD = "Building in respect of plot No.";
	
	public static String PARAGRAPH_1_1 = "The land in question must be in lawful ownership and peaceful possession of the applicant.";
	public static String PARAGRAPH_1_2 = "The permission accorded for regularization of the unauthorizedly sub-divided plot cannot be construed as an evidence to claim right, title and interest on the plot on which the permission has been granted.";
	public static String PARAGRAPH_1_3 = "If any dispute arises with respect to right, title, interest on the land on which the permission has been granted, the permission so granted shall be automatically treated as cancelled during the period of dispute; and any construction and development made by the applicant or owner on the disputed land will be at his risk without any legal or financial liability on the Authority.";
	public static String PARAGRAPH_1_4 = "Application for building plan approval over the regularized sub-plot shall be considered in accordance with the provisions of the Scheme for regularization of unauthorised layout/subdivided plots, land-use specified in the Development Plan and the Planning & Building Standards Rules/regulations, in operation; and any such provisions applicable at the time of such application.";

	public static String PARAGRAPH_2_1_1 = "The sub-divided plot shall be accessible by an approved means of access of %s meter in width;";
	public static String PARAGRAPH_2_1_2 = " and the %s sq. meter strip of land free-gifted to the local body vide deed no. dtd. shall be merged with the existing road for future widening of the road to at least 6.00 meter wide.";

	public static String PARAGRAPH_2_2 = "The road area surrendered/free-gifted for widening of the approach road shall be in the nature of public thoroughfare and shall not be blocked by any boundary wall/gate/physical obstruction.";
	public static String PARAGRAPH_2_3 = "The sub-plot shall not be further sub-divided or amalgamated without prior approval u/s-16 of ODA Act, 1982.";
	public static String PARAGRAPH_2_4 = "No storm water shall be discharged to the public road/public premises and other adjoining plots and shall be discharged to the road side storm water drain.";
	public static String PARAGRAPH_2_5 = "The plinth level of the buildings to be constructed over the regularized plot shall be raised 0.6 meters above the high flood level (HFL).";
	public static String PARAGRAPH_2_6 = "Building construction shall not be permitted, in case, the Kisam of the plot has not been converted to Gharabari. The approval for regularization granted herewith shall neither be construed as approval for conversion of Kisam nor shall confer any right for conversion of Kisam; and the same shall be duly converted to Gharabari kisam in accordance with relevant Rules and Regulations prescribed in this regard.";
	public static String PARAGRAPH_2_7 = "Regularization of the sub-divided plot shall not confer any right for construction of building and the same shall be considered as per the provision of ODA Act, 1982, Rules/Regulations framed thereunder, provisions of Development Plan, Zoning Regulation and subject to applicable statutory provisions of the State/ Central Government.";
	public static String PARAGRAPH_2_8 = "Regularization of the sub-divided plot shall not give any right for means of access/easement right to the plot.";

	public static String PARAGRAPH_1_5 = "Building in respect of";
	public static String PARAGRAPH_1_7 = "Khata No.";
//	public static String PARAGRAPH_1_8 = "180 ";
	public static String PARAGRAPH_1_9 = "Village/Mouza. ";
//	public static String PARAGRAPH_1_10 = "Injana ";
	public static String PARAGRAPH_1_11 = "of %s within the Development Plan Area subject to following conditions/ restrictions: ";

	public static String PARAGRAPH_3_1 = "The Building shall be used exclusively for %s purpose and the uses shall not be changed to any other use without prior approval of this Authority.\n\n";
	public static String PARAGRAPH_3_2 = "The development shall be undertaken strictly according to plans enclosed with necessary permission endorsement.\n\n";
	public static String PARAGRAPH_3_3_1 = "Parking space measuring ";
	public static String PARAGRAPH_3_3_2 = "sq. mtr. as shown in the approved plan shall be exclusively used for parking and no part of it will be used for any other purpose.\n\n";
	public static String PARAGRAPH_3_4 = "The land over which construction is proposed is accessible by an approved means of access of %s mtr. width.\n\n";
	public static String PARAGRAPH_3_5 = "The land in question must be in lawful ownership and peaceful possession of the applicant.\n\n";
	public static String PARAGRAPH_3_6 = "The applicant shall free gift %s sq.mtr. of located in the %s for the widening of the road/construction of new roads and other public amenities prior to completion of the development as indicated in the plan.\n\n";
	public static String PARAGRAPH_3_7_1 = "The permission is valid for period of ";
	public static String PARAGRAPH_3_7_2 = "three years ";
	public static String PARAGRAPH_3_7_3 = "with effect from the date of issue.\n\n";
	public static String PARAGRAPH_3_8 = "Permission accorded under the provision of Section 16 of ODA Act, cannot be construed as an evidence to claim right title interest on the plot on which the permission has been granted.\n\n";
	public static String PARAGRAPH_2_8_SPARIT = "Permission accorded under the provision of Section 31 and 33 of OTPIT Act, cannot be construed as an evidence to claim right title interest on the plot on which the permission has been granted.\n\n";

	public static String PARAGRAPH_3_9 = "If any dispute arises with respect to right, title interest on the land on which the permission has been granted, the permission so granted shall be automatically treated as canceled during the period of dispute.\n\n";
	public static String PARAGRAPH_3_10 = "Any construction and development made by the applicant or owner on the disputed land will be at his risk without any legal or financial liability on the Authority.\n\n";
	public static String OTHER_CONDITIONS = "Other conditions to be complied by the applicant are as per the following:\n";
	public static String PARAGRAPH_3_11_1 = "The ";
	public static String PARAGRAPH_3_11_2 = "is approved on payment following fees:\n\n";
//	public static String PARAGRAPH_2_11_3_A = "Sanction fee : Rs 6,689.00/-only";
	// public static String PARAGRAPH_2_11_3_B = "Construction Workers Welfare Cess
	// : Rs 83,967.00/-only";
	public static String PARAGRAPH_4_1 = "Total plot area (As per document) : ";
	
	public static String PARAGRAPH_5_1 = "Total plot area (As per Possession) : ";	

	public Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA, 16, Font.BOLD);
	public Font font1 = FontFactory.getFont(FontFactory.HELVETICA, 12);
	public Font fontBold = FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD);
	public Font fontBoldUnderlined = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Font.UNDERLINE);

	@Override
	public InputStream generateReport(LinkedHashMap regularizations, RequestInfo requestInfo, Boolean isPreview) {
		try {
			return createPdf(regularizations, requestInfo, isPreview);
		} catch (Exception e) {
			e.printStackTrace();
			throw new ApplicationRuntimeException("Error while generating permit order pdf", e);
		}
	}

	public InputStream createPdf(LinkedHashMap regularizations, RequestInfo requestInfo, Boolean isPreview)
			throws Exception {

		LinkedHashMap<String, Object> buildingRegularizationInfo = (LinkedHashMap<String, Object>) regularizations
				.get("buildingRegularizationInfo");
		LinkedHashMap<String, Object> landRegularizationInfo = (LinkedHashMap<String, Object>) regularizations
				.get("landRegularizationInfo");
		java.util.List<Map<String, Object>> plotInfoList = getPlotAndKhataDetails(regularizations);
		java.util.List<Map<String, Object>> buildingBlocksInfoList = getBuildingBlockDetails(regularizations);

		LinkedHashMap<String, Object> buildingRegularizationAdditionalDetails = (LinkedHashMap<String, Object>) buildingRegularizationInfo
				.get("additionalDetails");
		LinkedHashMap<String, Object> additionalDetails = (LinkedHashMap<String, Object>) regularizations
				.get("additionalDetails");
		String tenantIdActual = regularizations.get("tenantId").toString();
		OdishaUlbs ulb = OdishaUlbs.getUlb(regularizations.get("tenantId").toString());
		
		BigDecimal totalParkingArea = BigDecimal.ZERO;
		
		

		Document document = new Document();
		ByteArrayOutputStream outputBytes;
		outputBytes = new ByteArrayOutputStream();
		PdfWriter.getInstance(document, outputBytes);
		document.open();
		Image logo = getLogo();
		String[] ulbGradeNameAndUlbArray = getUlbNameAndGradeFromMdms(requestInfo, tenantIdActual);
		String ulbGradeNameAndUlb = (ulbGradeNameAndUlbArray[0] + " " + ulbGradeNameAndUlbArray[1]);

		// Map<String, Object> additionalDetails =
		// getAdditionalDetailsMap(regularizations);
		// Cuttack Municipal Corporation
		Paragraph headerTitle = new Paragraph(ulbGradeNameAndUlb, fontHeader);
		headerTitle.setAlignment(Paragraph.ALIGN_CENTER);

		Paragraph permitPreviewTitle = new Paragraph(PERMITPRVIEW, fontHeader);
		permitPreviewTitle.setAlignment(Paragraph.ALIGN_CENTER);

		// Form-II (Order for Grant of Permission)
		Paragraph headerSubTitle = new Paragraph("FORM-II ", font1);
		headerSubTitle.setAlignment(Paragraph.ALIGN_CENTER);

		Paragraph headerSubTitle1 = new Paragraph("[See Rule - 10 (5) of ODA (P&BS) Rules, 2020]");
		headerSubTitle1.setAlignment(Paragraph.ALIGN_CENTER);

		String tenantId = StringUtils.capitalize(tenantIdActual.split("\\.")[1]);
		@SuppressWarnings("deprecation")
		String approvalNo = getValue(regularizations, "approvalNo");
		String approvalDate = getApprovalDate();
		Paragraph headerSubTitle2 = new Paragraph(
				"Letter No. " + approvalNo + ", " + tenantId + ", Dated: " + approvalDate, fontBold);
		headerSubTitle2.setAlignment(Paragraph.ALIGN_CENTER);

		Paragraph permitPreviewSubTitle2 = new Paragraph(
				"Letter No. " + DxfFileConstants.NA + ", " + tenantId + ", Dated: " + DxfFileConstants.NA, fontBold);
		permitPreviewSubTitle2.setAlignment(Paragraph.ALIGN_CENTER);

		String applicationNo = getValue(regularizations, "applicationNo");
		Paragraph headerSubTitle3 = new Paragraph("Sujog-OBPS APPLICATION NO. " + applicationNo, fontBoldUnderlined);
		headerSubTitle3.setAlignment(Paragraph.ALIGN_CENTER);

		Paragraph paragraph1 = new Paragraph(PARAGRAPH_ONE, font1);

		// Paragraph paragraph1_Sparit = new Paragraph(PARAGRAPH_ONE_SPARIT, font1);

		Paragraph addressed = new Paragraph("Smt. /Shri,", fontBold);

//		String ownersNamesCsv = getNameOfOwner(regularizations);
//
//		Paragraph applicants = new Paragraph(ownersNamesCsv + ",", fontBold);
//		applicants.setIndentationLeft(30);

		String localityName = getValue(regularizations, "$.landRegularizationInfo.plotInfo[0].village");
		Paragraph address2 = new Paragraph(localityName + ",", fontBold);
		address2.setIndentationLeft(30);

		// to show correspondenceAddress from owner details--
		String primaryOwnerCorrespondenceAddress = getCorrespondenceAddress(regularizations);
		Paragraph address3 = new Paragraph(primaryOwnerCorrespondenceAddress, fontBold);
		address3.setIndentationLeft(30);
		
		
		Set<String> subOccupancies = new HashSet<String>();
		PdfPTable buildingBlockTable = getBuildingBlocksTable(buildingBlocksInfoList,subOccupancies);

		PdfPTable totalNoOfDwellingUnitsTable = getTotalNoOfDwellingUnitsTable(buildingRegularizationInfo);

		PdfPTable buildingAccessoryDetailTable = getBuildingAccessoryDetailTable(regularizations,buildingRegularizationAdditionalDetails);

		PdfPTable totalAreaTable = getTotalAreaTable(buildingBlocksInfoList);

		PdfPTable farTable = getFarTable(buildingRegularizationInfo);

		PdfPTable buildingHeightAndParkingtable = getBuildingHeightAndParkingtable(buildingRegularizationAdditionalDetails,
				buildingBlocksInfoList,totalParkingArea);

		PdfPTable setbackTable = getSetbackTable(buildingBlocksInfoList);
		
		

//		Chunk forServiceType = new Chunk(String.format("%s of a ", getServiceType(regularizations)), font1);
//		Chunk storeyed = new Chunk(PARAGRAPH_1_3, font1);
		Chunk paragraph2Chunk = new Chunk(String.format(PARAGRAPH_0_1, buildingRegularizationAdditionalDetails.get("noOfStoreys"),tenantId),font1);
		
		
		String subOccupancy = String.join(",", subOccupancies);
		


		Phrase paragraph2 = new Phrase();
		Phrase secondPhraseNew = new Phrase();
		Phrase secondPhraseNew2 = new Phrase();

		if (CollectionUtils.isEmpty(getPlotAndKhataDetails(regularizations))) {
			paragraph2.add(paragraph2Chunk);
			


		} else {
			// table
			secondPhraseNew.add(paragraph2Chunk);
			

		}

		Paragraph secondPara = new Paragraph(paragraph2);
		Paragraph secondParaNew = new Paragraph(secondPhraseNew);
		Paragraph secondParaNew2 = new Paragraph(secondPhraseNew2);
		
		String otherConditionsParaString = getValue(additionalDetails, "$.approverNote").toString();
		Paragraph otherConditionsPara=null,otherConditionsParaCondition=null;
		
		if(StringUtils.isNotEmpty(otherConditionsParaString)) {
			otherConditionsPara=new Paragraph(OTHER_CONDITIONS, fontBoldUnderlined);
			otherConditionsPara.setAlignment(Paragraph.ALIGN_LEFT);
			otherConditionsParaCondition= new Paragraph(otherConditionsParaString,font1);
		}

		List dataList = getListData(regularizations, requestInfo, isPreview, ulb, ulbGradeNameAndUlb, tenantIdActual,
				applicationNo, subOccupancy,totalParkingArea,otherConditionsParaString);
		
		//Area Statement
		
		generateAreaStatementTable(regularizations, dataList);

		PdfPTable landRegularizationPaymentTable = new PdfPTable(3);
		landRegularizationPaymentTable.setLockedWidth(false);
		float[] paymentTableWidths = { 15f, 60f, 25f };
		landRegularizationPaymentTable.setWidths(paymentTableWidths);
		addTableHeaderPaymentTable(landRegularizationPaymentTable);
		
		PdfPTable buildingRegularizationPaymentTable = new PdfPTable(3);
		buildingRegularizationPaymentTable.setLockedWidth(false);
		buildingRegularizationPaymentTable.setWidths(paymentTableWidths);
		addTableHeaderPaymentTable(buildingRegularizationPaymentTable);
		
		if (!isPreview) {
			addRowsForPaymentsTable(requestInfo, landRegularizationPaymentTable,buildingRegularizationPaymentTable, tenantIdActual, applicationNo);
		} else {
			addRowsForEstimatedFees(requestInfo, landRegularizationPaymentTable,buildingRegularizationPaymentTable, regularizations);
		}

		List landRegularizationPaymentList = new List(List.UNORDERED);
		landRegularizationPaymentList.setIndentationLeft(30);
		ListItem landRegularizationPaymentTableItem = new ListItem();
		landRegularizationPaymentTableItem.add(landRegularizationPaymentTable);
		landRegularizationPaymentList.add(landRegularizationPaymentTableItem);
		
		List buildingRegularizationPaymentList = new List(List.UNORDERED);
		buildingRegularizationPaymentList.setIndentationLeft(30);
		ListItem buildingRegularizationPaymentTableItem = new ListItem();
		buildingRegularizationPaymentTableItem.add(buildingRegularizationPaymentTable);
		buildingRegularizationPaymentList.add(buildingRegularizationPaymentTableItem);
		
		

		dataList.add(new ListItem("The sub-plot is regularised on payment of following fees - \n\n", fontBold));
		dataList.add(landRegularizationPaymentList);
		
		dataList.add(new ListItem("The Building is regularised on payment of following fees - \n\n", fontBold));
		dataList.add(buildingRegularizationPaymentList);
		

		BigDecimal plotAreaAsPerDeclaration = BigDecimal
				.valueOf(Double.parseDouble(landRegularizationInfo.get("totalPlotArea").toString()));
		Chunk chunk35 = new Chunk(PARAGRAPH_4_1, font1);
		Chunk chunk36 = new Chunk(plotAreaAsPerDeclaration + SQM, fontBold);
		Paragraph chunk37 = new Paragraph();
		chunk37.add(chunk35);
		chunk37.add(chunk36);

		BigDecimal plotAreaAsPerPossession = plotAreaAsPerDeclaration;
		Chunk chunk38 = new Chunk(PARAGRAPH_5_1, font1);
		// TODO: to check which parameter in scrutiny gives occupied plot area
		Chunk chunk39 = new Chunk(plotAreaAsPerPossession + SQM, fontBold);
		Paragraph chunk40 = new Paragraph();
		chunk40.add(chunk38);
		chunk40.add(chunk39);

		
		

		Paragraph approvedByParagraph = new Paragraph();
		Paragraph dateParagraph = new Paragraph();

		if (!isPreview) {

			Chunk dateSectionPart1 = new Chunk("Date: ", font1);
			Chunk dateSectionPart2 = new Chunk(approvalDate, font1);
			Phrase dateSection = new Phrase();
			dateSection.add(dateSectionPart1);
			dateSection.add(dateSectionPart2);

			dateParagraph.add(dateSection);
			dateParagraph.setAlignment(Element.ALIGN_LEFT);

			String approvedBy = getValue(regularizations, "$.dscDetails[0].approvedBy");

			//LOG.info("user (approvedBy):" + approvedBy);
			Map<String, Object> approverDetailsMap = getApproverDetails(requestInfo, tenantId, "BLR1", approvedBy,
					"EMPLOYEE");

			String approverName = !org.springframework.util.StringUtils
					.isEmpty(approverDetailsMap.get("nameOfApprover")) ? approverDetailsMap.get("nameOfApprover") + ""
							: "";
			//LOG.info("approverName: " + approverName);
			// String bpaPermitHeader = getBpaPermitHeader(requestInfo, tenantId);
			String[] bpaPermitHeader = getUlbNameAndGradeFromMdms(requestInfo, tenantIdActual);

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

			Image qrCode = getQrCode(applicationNo, approvalNo, approvalDate);
			document.add(qrCode);

		}
		if (isPreview) {
			document.add(permitPreviewTitle);
			document.add(Chunk.NEWLINE);
			document.add(permitPreviewSubTitle2);
		} else {

			document.add(logo);
			document.add(headerTitle);
			document.add(Chunk.NEWLINE);
			document.add(headerSubTitle);
			document.add(headerSubTitle1);
			document.add(headerSubTitle2);
		}
		document.add(headerSubTitle3);
		document.add(Chunk.NEWLINE);
		document.add(Chunk.NEWLINE);

		document.add(paragraph1);

		document.add(Chunk.NEWLINE);

		if (CollectionUtils.isEmpty(getPlotAndKhataDetails(regularizations))) {
			document.add(Chunk.NEWLINE);
			document.add(addressed);
			document.add(address2);
			document.add(Chunk.NEWLINE);
			document.add(address3);
			document.add(Chunk.NEWLINE);
			document.add(Chunk.NEWLINE);
			document.add(secondPara);
			document.add(Chunk.NEWLINE);

		} else {
			PdfPTable plotTable = new PdfPTable(7);
			plotTable.setLockedWidth(false);
			plotTable.setWidthPercentage(100f);
			addTableHeaderPlotTable(plotTable);
			addRowsForPlotTable(requestInfo, plotTable, regularizations, tenantIdActual);

			document.add(plotTable);
			document.add(Chunk.NEWLINE);
			document.add(Chunk.NEWLINE);
			document.add(secondParaNew);
			document.add(secondParaNew2);
		}

		document.add(Chunk.NEWLINE);
		document.add(Chunk.NEWLINE);
		document.add(chunk37);
		document.add(chunk40);
		document.add(Chunk.NEWLINE);
		document.add(buildingBlockTable);
		document.add(totalNoOfDwellingUnitsTable);
		document.add(buildingAccessoryDetailTable);
		document.add(farTable);
		document.add(buildingHeightAndParkingtable);
		document.add(totalAreaTable);
		document.add(Chunk.NEWLINE);
		document.add(setbackTable);
		document.add(Chunk.NEWLINE);
		document.add(Chunk.NEWLINE);
		document.add(dataList);
		document.add(Chunk.NEWLINE);
		document.add(Chunk.NEWLINE);
		document.add(Chunk.NEWLINE);
		document.add(Chunk.NEWLINE);
		document.add(dateParagraph);
		document.add(approvedByParagraph);
		document.close();
		return new ByteArrayInputStream(outputBytes.toByteArray());
	}

	private void generateAreaStatementTable(LinkedHashMap regularizations, List dataList) throws DocumentException {
		String totalPlotArea = getTotalPlotArea(regularizations);
		String giftedPlotArea = getAreaTobeGiftedSinglePlot(regularizations);
		
		PdfPTable areaStatementTable = new PdfPTable(3);
		areaStatementTable.setLockedWidth(false);
//		areaStatementTable.setWidthPercentage(100f);
		float[] areaStatementTableWidths = { 5f, 50f, 15f };
		areaStatementTable.setWidths(areaStatementTableWidths);

		PdfPCell aCell = new PdfPCell();
		aCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
		aCell.setHorizontalAlignment(Element.ALIGN_MIDDLE);
		aCell.setPhrase(new Phrase("(a)", fontBold));

		PdfPCell bCell = new PdfPCell();
		bCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
		bCell.setHorizontalAlignment(Element.ALIGN_MIDDLE);
		bCell.setPhrase(new Phrase("(b)", fontBold));

		PdfPCell cCell = new PdfPCell();
		cCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
		cCell.setHorizontalAlignment(Element.ALIGN_MIDDLE);
		cCell.setPhrase(new Phrase("(c)", fontBold));

		areaStatementTable.addCell(aCell);
		areaStatementTable.addCell("Total Plot Area");
		areaStatementTable.addCell(totalPlotArea+" sq. meter");

		areaStatementTable.addCell(bCell);
		areaStatementTable.addCell("Area free-gifted for road widening purpose");
		areaStatementTable.addCell(giftedPlotArea+" sq. meter");

		areaStatementTable.addCell(cCell);
		areaStatementTable.addCell("Net Plot Area");
		areaStatementTable.addCell(getNetPlotAreaSinglePlot(regularizations)+" sq. meter");

		ListItem areaStatementItem = new ListItem();
		//areaStatementItem.add(new Chunk("Area statement of the sub-plot", fontBold));
		areaStatementItem.setIndentationLeft(60);
		areaStatementItem.setSpacingAfter(10);

		areaStatementItem.add(areaStatementTable);
		
		List tableItemList = new List(List.UNORDERED);
		tableItemList.add(areaStatementItem);
		//tableItem.setSpacingAfter(10);

		dataList.add(new ListItem("Area statement of the sub-plot\n\n", fontBold));
		dataList.add(tableItemList);
	}

	private List getListData(LinkedHashMap regularizations, RequestInfo requestInfo, Boolean isPreview, OdishaUlbs ulb,
			String ulbGradeNameAndUlb, String tenantIdActual, String applicationNo, String subOccupancy, BigDecimal totalParkingArea, String otherConditionsParaString) {

		LinkedHashMap<String, Object> landRegularizationInfo = (LinkedHashMap<String, Object>) regularizations
				.get("landRegularizationInfo");
		java.util.List<Map<String, Object>> plotInfo = getPlotAndKhataDetails(regularizations);
		String accessRoadWidth = getValue(regularizations, "$.landRegularizationInfo.accessRoadWidth");
		String giftedPlotArea = getAreaTobeGiftedSinglePlot(regularizations);
		String totalPlotArea = getTotalPlotArea(regularizations);
		
		//Land Points
		
		List mainList = new List(List.ORDERED, List.NUMERICAL);

		mainList.add(new ListItem(" ", fontBold));

		List list1 = new List(List.ORDERED, List.ALPHABETICAL);
		list1.setLowercase(true);

		ListItem list1Item1 = new ListItem();
		Phrase landInQuestion = new Phrase(PARAGRAPH_1_1, font1);
		list1Item1.add(landInQuestion);
		list1Item1.add(Chunk.NEWLINE);
		list1Item1.setAlignment(List.ALIGN_JUSTIFIED);
		list1Item1.setSpacingAfter(10);

		ListItem list1Item2 = new ListItem();
		Phrase thePermissionAccorded = new Phrase(PARAGRAPH_1_2, font1);
		list1Item2.add(thePermissionAccorded);
		list1Item2.add(Chunk.NEWLINE);
		list1Item2.setAlignment(List.ALIGN_JUSTIFIED);
		list1Item2.setSpacingAfter(10);

		ListItem list1Item3 = new ListItem();
		Phrase ifAnyDisputes = new Phrase(PARAGRAPH_1_3, font1);
		list1Item3.add(ifAnyDisputes);
		list1Item3.add(Chunk.NEWLINE);
		list1Item3.setAlignment(List.ALIGN_JUSTIFIED);
		list1Item3.setSpacingAfter(10);

		ListItem list1Item4 = new ListItem();
		Phrase applicationForBuilding = new Phrase(PARAGRAPH_1_4, font1);
		list1Item4.add(applicationForBuilding);
		list1Item4.add(Chunk.NEWLINE);
		list1Item4.setAlignment(List.ALIGN_JUSTIFIED);
		list1Item4.setSpacingAfter(10);

		list1.setIndentationLeft(30);
		list1.add(list1Item1);
		list1.add(list1Item2);
		list1.add(list1Item3);
		list1.add(list1Item4);

		mainList.add(list1);

		mainList.add(new ListItem(" ", fontBold));
		List list2 = new List(List.ORDERED, List.ALPHABETICAL);
		list2.setLowercase(true);
		list2.setIndentationLeft(30);

		ListItem list2Item1 = new ListItem();
		list2Item1.add(new Chunk(String.format(PARAGRAPH_2_1_1, accessRoadWidth), font1));
		list2Item1.add(new Chunk(String.format(PARAGRAPH_2_1_2, giftedPlotArea), font1));
		list2Item1.add(Chunk.NEWLINE);
		list2Item1.setAlignment(List.ALIGN_JUSTIFIED);
		list2Item1.setSpacingAfter(10);
		list2.add(list2Item1);

		ListItem list2Item2 = new ListItem();
		list2Item2.add(new Phrase(PARAGRAPH_2_2, font1));
		list2Item2.add(Chunk.NEWLINE);
		list2Item2.setAlignment(List.ALIGN_JUSTIFIED);
		list2Item2.setSpacingAfter(10);
		list2.add(list2Item2);

		ListItem list2Item3 = new ListItem();
		list2Item3.add(new Phrase(PARAGRAPH_2_3, font1));
		list2Item3.add(Chunk.NEWLINE);
		list2Item3.setAlignment(List.ALIGN_JUSTIFIED);
		list2Item3.setSpacingAfter(10);
		list2.add(list2Item3);

		ListItem list2Item4 = new ListItem();
		list2Item4.add(new Phrase(PARAGRAPH_2_4, font1));
		list2Item4.add(Chunk.NEWLINE);
		list2Item4.setAlignment(List.ALIGN_JUSTIFIED);
		list2Item4.setSpacingAfter(10);
		list2.add(list2Item4);

		ListItem list2Item5 = new ListItem();
		list2Item5.add(new Phrase(PARAGRAPH_2_5, font1));
		list2Item5.add(Chunk.NEWLINE);
		list2Item5.setAlignment(List.ALIGN_JUSTIFIED);
		list2Item5.setSpacingAfter(10);
		list2.add(list2Item5);

		ListItem list2Item6 = new ListItem();
		list2Item6.add(new Phrase(PARAGRAPH_2_6, font1));
		list2Item6.add(Chunk.NEWLINE);
		list2Item6.setAlignment(List.ALIGN_JUSTIFIED);
		list2Item6.setSpacingAfter(10);
		list2.add(list2Item6);

		ListItem list2Item7 = new ListItem();
		list2Item7.add(new Phrase(PARAGRAPH_2_7, font1));
		list2Item7.add(Chunk.NEWLINE);
		list2Item7.setAlignment(List.ALIGN_JUSTIFIED);
		list2Item7.setSpacingAfter(10);
		list2.add(list2Item7);

		ListItem list2Item8 = new ListItem();
		list2Item8.add(new Phrase(PARAGRAPH_2_8, font1));
		list2Item8.add(Chunk.NEWLINE);
		list2Item8.setAlignment(List.ALIGN_JUSTIFIED);
		list2Item8.setSpacingAfter(10);
		list2.add(list2Item8);

		mainList.add(list2);
		
		
		//Building
		mainList.add(new ListItem(" ", fontBold));
		List list3 = new List(List.ORDERED, List.ALPHABETICAL);
		list3.setLowercase(true);
		list3.setIndentationLeft(30);
		
		ListItem list3Item1 = new ListItem();
		Chunk subOccupancyChunk = new Chunk(subOccupancy, fontBold);
		Phrase landUsePhrase = new Phrase(String.format(PARAGRAPH_3_1, subOccupancyChunk), font1);
		list3Item1.add(landUsePhrase);
		list3Item1.add(Chunk.NEWLINE);
		list3Item1.setAlignment(List.ALIGN_JUSTIFIED);
		list3Item1.setSpacingAfter(10);
		list3.add(list3Item1);

		ListItem list3Item2 = new ListItem();
		Phrase developmentUndertakenPhrase = new Phrase(PARAGRAPH_2_2, font1);
		list3Item2.add(developmentUndertakenPhrase);
		list3Item2.add(Chunk.NEWLINE);
		list3Item2.setAlignment(List.ALIGN_JUSTIFIED);
		list3Item2.setSpacingAfter(10);
		list3.add(list3Item2);

		ListItem list3Item3 = new ListItem();
		Chunk chunk15 = new Chunk(PARAGRAPH_3_3_1, font1);
		Chunk chunk16 =new Chunk(totalParkingArea.toString()+" ",fontBold);
		Chunk chunk17 = new Chunk(PARAGRAPH_3_3_2, font1);
		Phrase chunk18 = new Phrase();
		chunk18.add(chunk15);
		chunk18.add(chunk16);
		chunk18.add(chunk17);
		list3Item3.add(chunk18);
		list3Item3.add(Chunk.NEWLINE);
		list3Item3.setAlignment(List.ALIGN_JUSTIFIED);
		list3Item3.setSpacingAfter(10);
		list3.add(list3Item3);

		ListItem list3Item4 = new ListItem();
		BigDecimal roadWidth = BigDecimal
				.valueOf(Double.parseDouble(landRegularizationInfo.get("accessRoadWidth").toString()));
		Phrase chunk19 = new Phrase(String.format(PARAGRAPH_3_4, roadWidth), font1);
		list3Item4.add(chunk19);
		list3Item4.add(Chunk.NEWLINE);
		list3Item4.setAlignment(List.ALIGN_JUSTIFIED);
		list3Item4.setSpacingAfter(10);
		list3.add(list3Item4);

		ListItem list3Item5 = new ListItem();
		Phrase chunk20 = new Phrase(PARAGRAPH_3_5, font1);
		list3Item5.add(chunk20);
		list3Item5.add(Chunk.NEWLINE);
		list3Item5.setAlignment(List.ALIGN_JUSTIFIED);
		list3Item5.setSpacingAfter(10);
		list3.add(list3Item5);

		ListItem list3Item6 = new ListItem();
		String fContent = String.format(PARAGRAPH_3_6, getGiftedArea(plotInfo).toString(), ulbGradeNameAndUlb);
		Phrase chunk21 = new Phrase(fContent, font1);
		list3Item6.add(chunk21);
		list3Item6.add(Chunk.NEWLINE);
		list3Item6.setAlignment(List.ALIGN_JUSTIFIED);
		list3Item6.setSpacingAfter(10);
		list3.add(list3Item6);

		ListItem list3Item7 = new ListItem();
		Chunk chunk22 = new Chunk(PARAGRAPH_3_7_1, font1);
		Chunk chunk23 = new Chunk(PARAGRAPH_3_7_2, fontBold);
		Chunk chunk24 = new Chunk(PARAGRAPH_3_7_3, font1);
		Phrase chunk25 = new Phrase();
		chunk25.add(chunk22);
		chunk25.add(chunk23);
		chunk25.add(chunk24);
		list3Item7.add(chunk25);
		list3Item7.add(Chunk.NEWLINE);
		list3Item7.setAlignment(List.ALIGN_JUSTIFIED);
		list3Item7.setSpacingAfter(10);
		list3.add(list3Item7);

		ListItem list3Item8 = new ListItem();
		Phrase chunk26 = new Phrase(PARAGRAPH_3_8, font1);
		Phrase chunk26_sparit = new Phrase(PARAGRAPH_2_8_SPARIT, font1);
		if (!ulb.isSparitFlag()) {
			list3Item8.add(chunk26);
		} else {
			list3Item8.add(chunk26_sparit);
		}
		list3Item8.add(Chunk.NEWLINE);
		list3Item8.setAlignment(List.ALIGN_JUSTIFIED);
		list3Item8.setSpacingAfter(10);
		list3.add(list3Item8);

		ListItem list3Item9 = new ListItem();
		Phrase chunk27 = new Phrase(PARAGRAPH_3_9, font1);
		list3Item9.add(chunk27);
		list3Item9.add(Chunk.NEWLINE);
		list3Item9.setAlignment(List.ALIGN_JUSTIFIED);
		list3Item9.setSpacingAfter(10);
		list3.add(list3Item9);

		ListItem list3Item10 = new ListItem();
		Phrase chunk28 = new Phrase(PARAGRAPH_3_10, font1);
		list3Item10.add(chunk28);
		list3Item10.add(Chunk.NEWLINE);
		list3Item10.setAlignment(List.ALIGN_JUSTIFIED);
		list3Item10.setSpacingAfter(10);
		list3.add(list3Item10);

		if(StringUtils.isNotEmpty(otherConditionsParaString)) {
			ListItem otherConditionsItem = new ListItem();
			Phrase OtherConditionsPhrase = new Phrase(OTHER_CONDITIONS+"\n\n"+otherConditionsParaString, font1);
			otherConditionsItem.add(OtherConditionsPhrase);	
			otherConditionsItem.add(Chunk.NEWLINE);
			otherConditionsItem.setAlignment(List.ALIGN_JUSTIFIED);
			otherConditionsItem.setSpacingAfter(10);
			list3.add(otherConditionsItem);
		}
		

//		ListItem list3Item11 = new ListItem();
//		Chunk chunk29 = new Chunk(PARAGRAPH_3_11_1, font1);
//		Chunk chunk31 = new Chunk(PARAGRAPH_3_11_2, font1);
//		Phrase chunk32 = new Phrase();
//		chunk32.add(chunk29);
//		chunk32.add(chunk31);
//		list3Item11.add(chunk32);
//		List payments = new List(List.UNORDERED);
//		list3.add(list3Item11);
//
//
//		list3Item11.add(payments);
		
			
		mainList.add(list3);
		return mainList;
	}

}
