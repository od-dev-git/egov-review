package org.egov.edcr.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.egov.edcr.constants.DxfFileConstants;
import org.egov.infra.exception.ApplicationRuntimeException;
import org.egov.infra.microservice.models.RequestInfo;
import org.springframework.stereotype.Service;

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

@Service
public class LandRegCertificateServiceSinglePlot extends LandRegCertificateService {

	private static final Logger LOG = Logger.getLogger(LandRegCertificateServiceSinglePlot.class);

	public static String LANDREGCERTIFICATEPREVIEW = "Land Regularization Certificate Preview";

	public static String PARAGRAPH_1 = "Permission under Scheme for regularization of unauthorised layout/subdivided plots, notified in the Extraordinary Gazette no.1034 dated 30 May 2017 is hereby granted in favour of Land Owner, ";
	public static String PARAGRAPH_1_2 = "for regularization of unauthorizedly sub-divided plot";
	public static String PARAGRAPH_1_3 = "in the Development Plan area of ";
	public static String PARAGRAPH_1_4 = " subject to the following conditions/restrictions :-";

	public static String PARAGRAPH_2_1 = "The land in question must be in lawful ownership and peaceful possession of the applicant.";
	public static String PARAGRAPH_2_2 = "The permission accorded for regularization of the unauthorizedly sub-divided plot cannot be construed as an evidence to claim right, title and interest on the plot on which the permission has been granted.";
	public static String PARAGRAPH_2_3 = "If any dispute arises with respect to right, title, interest on the land on which the permission has been granted, the permission so granted shall be automatically treated as cancelled during the period of dispute; and any construction and development made by the applicant or owner on the disputed land will be at his risk without any legal or financial liability on the Authority.";
	public static String PARAGRAPH_2_4 = "Application for building plan approval over the regularized sub-plot shall be considered in accordance with the provisions of the Scheme for regularization of unauthorised layout/subdivided plots, land-use specified in the Development Plan and the Planning & Building Standards Rules/regulations, in operation; and any such provisions applicable at the time of such application.";

	public static String PARAGRAPH_3_1 = "The sub-divided plot shall be accessible by an approved means of access of %s meter in width;";
	public static String PARAGRAPH_3_1_2 = " and the %s sq. meter strip of land free-gifted to the local body vide deed no. dtd. shall be merged with the existing road for future widening of the road to at least 6.00 meter wide.";

	public static String PARAGRAPH_3_2 = "The road area surrendered/free-gifted for widening of the approach road shall be in the nature of public thoroughfare and shall not be blocked by any boundary wall/gate/physical obstruction.";
	public static String PARAGRAPH_3_3 = "The sub-plot shall not be further sub-divided or amalgamated without prior approval u/s-16 of ODA Act, 1982.";
	public static String PARAGRAPH_3_4 = "No storm water shall be discharged to the public road/public premises and other adjoining plots and shall be discharged to the road side storm water drain.";
	public static String PARAGRAPH_3_5 = "The plinth level of the buildings to be constructed over the regularized plot shall be raised 0.6 meters above the high flood level (HFL).";
	public static String PARAGRAPH_3_6 = "Building construction shall not be permitted, in case, the Kisam of the plot has not been converted to Gharabari. The approval for regularization granted herewith shall neither be construed as approval for conversion of Kisam nor shall confer any right for conversion of Kisam; and the same shall be duly converted to Gharabari kisam in accordance with relevant Rules and Regulations prescribed in this regard.";
	public static String PARAGRAPH_3_7 = "Regularization of the sub-divided plot shall not confer any right for construction of building and the same shall be considered as per the provision of ODA Act, 1982, Rules/Regulations framed thereunder, provisions of Development Plan, Zoning Regulation and subject to applicable statutory provisions of the State/ Central Government.";
	public static String PARAGRAPH_3_8 = "Regularization of the sub-divided plot shall not give any right for means of access/easement right to the plot.";

	@Override
	public InputStream generateReport(LinkedHashMap regularizations, RequestInfo requestInfo, Boolean isPreview) {

		try {
			return createPdf(regularizations, requestInfo, isPreview);
		} catch (Exception e) {
			e.printStackTrace();
			throw new ApplicationRuntimeException("Error while generating land regularization certificate pdf", e);
		}

	}

	public InputStream createPdf(LinkedHashMap regularizations, RequestInfo requestInfo, Boolean isPreview)
			throws Exception {

		Document document = new Document();
		ByteArrayOutputStream outputBytes;
		outputBytes = new ByteArrayOutputStream();
		PdfWriter.getInstance(document, outputBytes);
		document.open();

		document.setMargins(36, 72, 36, 72);

		Image logo = getLogo();

		Paragraph landRegCertPreview = new Paragraph(LANDREGCERTIFICATEPREVIEW, fontHeader);
		landRegCertPreview.setAlignment(Paragraph.ALIGN_CENTER);

		String tenantIdActual = (String) regularizations.get("tenantId");
		String applicationNo = (String) regularizations.get("applicationNo");
		String[] ulbGradeNameAndUlbArray = getUlbNameAndGradeFromMdms(requestInfo, tenantIdActual);
		String ulbGradeNameAndUlb = (ulbGradeNameAndUlbArray[0] + " " + ulbGradeNameAndUlbArray[1]);

		Paragraph headerTitle = new Paragraph(ulbGradeNameAndUlb, fontHeader);
		headerTitle.setAlignment(Paragraph.ALIGN_CENTER);

		String tenantId = StringUtils.capitalize(tenantIdActual.split("\\.")[1]);

		String approvalNo = (String) regularizations.get("approvalNo");
		String approvalDate = getApprovalDate();
		Paragraph headerSubTitle2 = new Paragraph(
				"Letter No. " + approvalNo + ", " + tenantId + ", Dated: " + approvalDate, fontBold);
		headerSubTitle2.setAlignment(Paragraph.ALIGN_CENTER);

		Paragraph headerSubTitle3 = new Paragraph("APPLICATION NO. " + applicationNo, fontBoldUnderlined);
		headerSubTitle3.setAlignment(Paragraph.ALIGN_CENTER);

		Paragraph landRegCertPreviewSubTitle2 = new Paragraph(
				"Letter No. " + DxfFileConstants.NA + ", " + tenantId + ", Dated: " + DxfFileConstants.NA, fontBold);
		landRegCertPreviewSubTitle2.setAlignment(Paragraph.ALIGN_CENTER);

		String accessRoadWidth = getValue(regularizations, "$.landRegularizationInfo.accessRoadWidth");
		String totalPlotArea = getTotalPlotArea(regularizations);
		String giftedPlotArea = getAreaTobeGiftedSinglePlot(regularizations);
		// String areaTobeGifted = getValue(regularizations,
		// "$.landRegularizationInfo.plotInfo");

		// String village = getValue(regularizations,
		// "$.landRegularizationInfo.plotInfo");
		
		Map<String, Object> additionalDetails = getAdditionalDetailsMap(regularizations);

		Paragraph paragraph1 = new Paragraph();
		Chunk permissionUnder = new Chunk(PARAGRAPH_1, font1);
		Chunk forRegularization = new Chunk(PARAGRAPH_1_2, font1);

		paragraph1.add(permissionUnder);
		paragraph1.add(forRegularization);
		paragraph1.setFirstLineIndent(30);
		paragraph1.setAlignment(Paragraph.ALIGN_JUSTIFIED);

		PdfPTable plotTable = new PdfPTable(7);
		plotTable.setLockedWidth(false);
		plotTable.setWidthPercentage(100f);
		addTableHeaderPlotTable(plotTable);
		addRowsForPlotTable(requestInfo ,plotTable, regularizations, tenantIdActual);

		Paragraph paragraph1_3 = new Paragraph();
		// Chunk inMouza = new Chunk(String.format("in Mouza - %s", ));
		Chunk inTheDevlPlanAreaOf = new Chunk(PARAGRAPH_1_3 + tenantId, font1);
		Chunk subjectTo = new Chunk(PARAGRAPH_1_4, font1);

		paragraph1_3.add(inTheDevlPlanAreaOf);
		paragraph1_3.add(subjectTo);

		List mainList = new List(List.ORDERED, List.NUMERICAL);

		mainList.add(new ListItem(" ", fontBold));

		List list1 = new List(List.ORDERED, List.ALPHABETICAL);
		list1.setLowercase(true);

		ListItem list1Item1 = new ListItem();
		Phrase landInQuestion = new Phrase(PARAGRAPH_2_1, font1);
		list1Item1.add(landInQuestion);
		list1Item1.add(Chunk.NEWLINE);
		list1Item1.setAlignment(List.ALIGN_JUSTIFIED);
		list1Item1.setSpacingAfter(10);

		ListItem list1Item2 = new ListItem();
		Phrase thePermissionAccorded = new Phrase(PARAGRAPH_2_2, font1);
		list1Item2.add(thePermissionAccorded);
		list1Item2.add(Chunk.NEWLINE);
		list1Item2.setAlignment(List.ALIGN_JUSTIFIED);
		list1Item2.setSpacingAfter(10);

		ListItem list1Item3 = new ListItem();
		Phrase ifAnyDisputes = new Phrase(PARAGRAPH_2_3, font1);
		list1Item3.add(ifAnyDisputes);
		list1Item3.add(Chunk.NEWLINE);
		list1Item3.setAlignment(List.ALIGN_JUSTIFIED);
		list1Item3.setSpacingAfter(10);

		ListItem list1Item4 = new ListItem();
		Phrase applicationForBuilding = new Phrase(PARAGRAPH_2_4, font1);
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
		list2Item1.add(new Chunk(String.format(PARAGRAPH_3_1, accessRoadWidth), font1));
		list2Item1.add(new Chunk(String.format(PARAGRAPH_3_1_2, giftedPlotArea), font1));
		list2Item1.add(Chunk.NEWLINE);
		list2Item1.setAlignment(List.ALIGN_JUSTIFIED);
		list2Item1.setSpacingAfter(10);
		list2.add(list2Item1);

		ListItem list2Item2 = new ListItem();
		list2Item2.add(new Phrase(PARAGRAPH_3_2, font1));
		list2Item2.add(Chunk.NEWLINE);
		list2Item2.setAlignment(List.ALIGN_JUSTIFIED);
		list2Item2.setSpacingAfter(10);
		list2.add(list2Item2);

		ListItem list2Item3 = new ListItem();
		list2Item3.add(new Phrase(PARAGRAPH_3_3, font1));
		list2Item3.add(Chunk.NEWLINE);
		list2Item3.setAlignment(List.ALIGN_JUSTIFIED);
		list2Item3.setSpacingAfter(10);
		list2.add(list2Item3);

		ListItem list2Item4 = new ListItem();
		list2Item4.add(new Phrase(PARAGRAPH_3_4, font1));
		list2Item4.add(Chunk.NEWLINE);
		list2Item4.setAlignment(List.ALIGN_JUSTIFIED);
		list2Item4.setSpacingAfter(10);
		list2.add(list2Item4);

		ListItem list2Item5 = new ListItem();
		list2Item5.add(new Phrase(PARAGRAPH_3_5, font1));
		list2Item5.add(Chunk.NEWLINE);
		list2Item5.setAlignment(List.ALIGN_JUSTIFIED);
		list2Item5.setSpacingAfter(10);
		list2.add(list2Item5);

		ListItem list2Item6 = new ListItem();
		list2Item6.add(new Phrase(PARAGRAPH_3_6, font1));
		list2Item6.add(Chunk.NEWLINE);
		list2Item6.setAlignment(List.ALIGN_JUSTIFIED);
		list2Item6.setSpacingAfter(10);
		list2.add(list2Item6);

		ListItem list2Item7 = new ListItem();
		list2Item7.add(new Phrase(PARAGRAPH_3_7, font1));
		list2Item7.add(Chunk.NEWLINE);
		list2Item7.setAlignment(List.ALIGN_JUSTIFIED);
		list2Item7.setSpacingAfter(10);
		list2.add(list2Item7);

		ListItem list2Item8 = new ListItem();
		list2Item8.add(new Phrase(PARAGRAPH_3_8, font1));
		list2Item8.add(Chunk.NEWLINE);
		list2Item8.setAlignment(List.ALIGN_JUSTIFIED);
		list2Item8.setSpacingAfter(10);
		list2.add(list2Item8);

		mainList.add(list2);

		// table 1
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
		areaStatementItem.add(new Chunk("Area statement of the sub-plot", fontBold));
		areaStatementItem.setIndentationLeft(60);
		areaStatementItem.setSpacingAfter(10);

		ListItem tableItem = new ListItem();
		tableItem.add(areaStatementItem);
		tableItem.add(areaStatementTable);
		tableItem.setSpacingAfter(10);

		mainList.add(new ListItem(" ", fontBold));
		mainList.add(tableItem);
		
		String otherConditionsParaString = getValue(additionalDetails, "$.approverNote").toString();
		Paragraph otherConditionsPara=null,otherConditionsParaCondition=null;
		
		if(StringUtils.isNotEmpty(otherConditionsParaString)) {
			otherConditionsPara=new Paragraph(OTHER_CONDITIONS, fontBoldUnderlined);
			otherConditionsPara.setAlignment(Paragraph.ALIGN_LEFT);
			otherConditionsParaCondition= new Paragraph(otherConditionsParaString,font1);
		}

		// fee table
		ListItem paymentItem = new ListItem();
		paymentItem.add(new Chunk("The sub-plot is regularised on payment of following fees - ", fontBold));
		paymentItem.setIndentationLeft(60);
		paymentItem.setSpacingAfter(10);

		PdfPTable paymentTable = new PdfPTable(3);
		paymentTable.setLockedWidth(false);
		float[] paymentTableWidths = { 7f, 50f, 15f };
		paymentTable.setWidths(paymentTableWidths);
		addTableHeaderPaymentTable(paymentTable);
		if (!isPreview) {
			addRowsForPaymentsTable(requestInfo, paymentTable, tenantIdActual, applicationNo);
		} else {
			addRowsForEstimatedFees(requestInfo, paymentTable, regularizations);
		}

		ListItem paymentTableItem = new ListItem();
		paymentTableItem.add(paymentItem);
		paymentTableItem.add(paymentTable);
		paymentTableItem.setSpacingAfter(10);

		mainList.setFirst(-2);
		mainList.add(new ListItem(" ", fontBold));
		mainList.add(paymentTableItem);

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
			Map<String, Object> approverDetailsMap = getApproverDetails(requestInfo, tenantId, "LR1", approvedBy,
					"EMPLOYEE");

			String approverName = !org.springframework.util.StringUtils
					.isEmpty(approverDetailsMap.get("nameOfApprover")) ? approverDetailsMap.get("nameOfApprover") + ""
							: "";
			//LOG.info("approverName: " + approverName);

			String[] permitHeader = getUlbNameAndGradeFromMdms(requestInfo, tenantIdActual);

			Phrase approvedBySection = new Phrase();
			Chunk approvedBySectionLine1 = new Chunk("BY ORDER OF" + "\n", font1);
			Chunk approvedBySectionLine2 = new Chunk(approverName + "\n", font1);
			Chunk approvedBySectionLine3 = new Chunk("Authorized Officer" + "\n", font1);
			Chunk approvedBySectionLine4 = new Chunk(permitHeader[2]);
			approvedBySection.add(approvedBySectionLine1);
			approvedBySection.add(approvedBySectionLine2);
			approvedBySection.add(approvedBySectionLine3);
			approvedBySection.add(approvedBySectionLine4);
			approvedByParagraph.add(approvedBySection);
			approvedByParagraph.setAlignment(Element.ALIGN_RIGHT);

		}

		if (isPreview) {
			document.add(landRegCertPreview);
			document.add(Chunk.NEWLINE);
			document.add(landRegCertPreviewSubTitle2);
		} else {

			Image qrCode = getQrCode(applicationNo, approvalNo, approvalDate);
			document.add(qrCode);
			document.add(logo);
			document.add(headerTitle);
			document.add(headerSubTitle2);
		}
		document.add(headerSubTitle3);
		document.add(Chunk.NEWLINE);
		document.add(paragraph1);
		document.add(Chunk.NEWLINE);
		document.add(plotTable);
		document.add(Chunk.NEWLINE);
		document.add(paragraph1_3);
		document.add(Chunk.NEWLINE);
		document.add(mainList);
		document.add(Chunk.NEWLINE);
		if(StringUtils.isNotEmpty(otherConditionsParaString)) {
			document.add(otherConditionsPara);
			document.add(Chunk.NEWLINE);
			document.add(otherConditionsParaCondition);
			document.add(Chunk.NEWLINE);
		}
		document.add(Chunk.NEWLINE);
		document.add(Chunk.NEWLINE);
		document.add(dateParagraph);
		document.add(approvedByParagraph);
		document.close();

		return new ByteArrayInputStream(outputBytes.toByteArray());

	}

}
