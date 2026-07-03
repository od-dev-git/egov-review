package org.egov.edcr.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.egov.common.entity.edcr.Plan;
import org.egov.edcr.constants.DxfFileConstants;
import org.egov.infra.exception.ApplicationRuntimeException;
import org.egov.infra.microservice.models.RequestInfo;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

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
public class RejectionLetterServiceOC extends PermitOrderService{
	
	public Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA, 16, Font.BOLD);
	public Font font1 = FontFactory.getFont(FontFactory.HELVETICA, 12);
	public Font fontBold = FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD);
	public Font fontBoldUnderlined = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Font.UNDERLINE);

	public static String REJECTIONLETTERPREVIEW = "Refusal Letter Preview";
	public static String PARAGRAPH_0_0 = "Form III";
	public static String PARAGRAPH_0_1 = "[see rule 10(3) of ODA (CAF) Rules 2016]";
	public static String PARAGRAPH_0_2 = "ORDER FOR REFUSAL OF PERMISSION";

	public static String PARAGRAPH_1_1 = "Ref: : Sujog-OBPS application No. %s in respect of Occupancy Certificate.";

	public static String PARAGRAPH_1_3 = "Your reply to this office letter No %s dated %s";
	public static String PARAGRAPH_1_4 = " has not been found satisfactory and in compliance to the provisions of building and development norms in force / you have failed to show any cause in response to this office letter No %s Dated %s within the prescribed time stipulated in the above referred letter. ";
	public static String PARAGRAPH_1_5 = " Hence, in exercise of the powers under sub-section (3) of section 16 of the Odisha Development Authority Act, 1982, your application for permission to undertake development on ";
	public static String PARAGRAPH_1_6 = "of %s within the Development Plan is hereby refused on the following grounds - ";

	@Override
	public InputStream generateReport(Plan plan, LinkedHashMap bpaApplication, RequestInfo requestInfo,
			Boolean isPreview) {
		try {
			return createPdf(plan, bpaApplication, requestInfo, isPreview);
		} catch (Exception e) {
			e.printStackTrace();
			throw new ApplicationRuntimeException("Error while generating rejection letter pdf", e);
		}
	}
	
	public InputStream createPdf(Plan plan, LinkedHashMap bpaApplication, RequestInfo requestInfo, Boolean isPreview)
			throws Exception {
		String tenantIdActual = getValue(bpaApplication, "tenantId");

		String approvalDate = getApprovalDate();

		Document document = new Document();
		ByteArrayOutputStream outputBytes;
		outputBytes = new ByteArrayOutputStream();
		PdfWriter.getInstance(document, outputBytes);
		document.open();

		Image logo = getLogo();
		String[] ulbGradeNameAndUlbArray = getUlbNameAndGradeFromMdms(requestInfo, tenantIdActual);
		String ulbGradeNameAndUlb = (ulbGradeNameAndUlbArray[0] + " " + ulbGradeNameAndUlbArray[1]);

		Paragraph headerTitle = new Paragraph(ulbGradeNameAndUlb, fontHeader);
		headerTitle.setAlignment(Paragraph.ALIGN_CENTER);

		Paragraph headerSubTitle = new Paragraph("Refusal Letter", fontHeader);
		headerSubTitle.setAlignment(Paragraph.ALIGN_CENTER);
		Paragraph headerSubTitle2_1 = new Paragraph(PARAGRAPH_0_0, fontBold);
		headerSubTitle2_1.setAlignment(Paragraph.ALIGN_CENTER);
		Paragraph headerSubTitle3 = new Paragraph(PARAGRAPH_0_1, fontBold);
		headerSubTitle3.setAlignment(Paragraph.ALIGN_CENTER);
		Paragraph headerSubTitle4 = new Paragraph(PARAGRAPH_0_2, fontBold);
		headerSubTitle4.setAlignment(Paragraph.ALIGN_CENTER);

		String tenantId = StringUtils.capitalize(tenantIdActual.split("\\.")[1]);

		Paragraph headerSubTitle2 = new Paragraph(tenantId + ", Dated: " + approvalDate, fontBold);
		headerSubTitle2.setAlignment(Paragraph.ALIGN_CENTER);

		String applicationNo = getValue(bpaApplication, "applicationNo");

		Map<String, String> noticeDetails = getShowCauseNoticeNumber(requestInfo, applicationNo);

		Paragraph rejectionLetterPreview = new Paragraph(REJECTIONLETTERPREVIEW, fontHeader);
		rejectionLetterPreview.setAlignment(Paragraph.ALIGN_CENTER);

		Paragraph rejectionLetterPreviewSubTitle2 = new Paragraph(
				"Letter No. " + DxfFileConstants.NA + ", " + tenantId + ", Dated: " + DxfFileConstants.NA, fontBold);
		rejectionLetterPreviewSubTitle2.setAlignment(Paragraph.ALIGN_CENTER);
		String subject = "";
		
		subject = String.format(PARAGRAPH_1_1, applicationNo);

		Paragraph subjectPara = new Paragraph(subject, fontBold);

		Paragraph sirMadam = new Paragraph("Sir/Madam, ", fontBold);
		Paragraph withRefPara = new Paragraph();
		Chunk withRef = new Chunk(
				String.format(PARAGRAPH_1_3, noticeDetails.get("LetterNo"), noticeDetails.get("createdTime")), font1);
		Chunk satisfactory = new Chunk(
				String.format(PARAGRAPH_1_4, noticeDetails.get("LetterNo"), noticeDetails.get("createdTime")), font1);
		Chunk henceInExecise = new Chunk(PARAGRAPH_1_5, font1);

		withRefPara.add(withRef);
		withRefPara.add(satisfactory);

		withRefPara.setFirstLineIndent(30);

		Paragraph hencePara = new Paragraph();
		hencePara.add(henceInExecise);

		Chunk developmentPlan = new Chunk(String.format(PARAGRAPH_1_6, tenantId), font1);

		Paragraph hencePara2 = new Paragraph();
		hencePara2.add(developmentPlan);

		Paragraph rejectListPara = new Paragraph();
		List rejectList = new List(List.ORDERED, List.NUMERICAL);

		java.util.List<String> rejectionMap1 = getRejectionListFromMap(bpaApplication);

		for (String cause : rejectionMap1) {
			if (rejectionMap.containsKey(cause)) {
				Phrase newCause = new Phrase(rejectionMap.get(cause) + "\n\n", font1);
				ListItem listItemCause = new ListItem(newCause);
				rejectList.add(listItemCause);
			} else {
				Phrase newCause = new Phrase(cause + "\n\n", font1);
				ListItem listItemCause = new ListItem(newCause);
				rejectList.add(listItemCause);
			}

		}
		rejectListPara.add(rejectList);

		Paragraph approvedByParagraph = new Paragraph();
		Paragraph dateParagraph = new Paragraph();

		Chunk dateSectionPart1 = new Chunk("Date: ", font1);
		Chunk dateSectionPart2 = new Chunk(approvalDate, font1);
		Phrase dateSection = new Phrase();
		dateSection.add(dateSectionPart1);
		dateSection.add(dateSectionPart2);

		dateParagraph.add(dateSection);
		dateParagraph.setAlignment(Element.ALIGN_LEFT);

		if (!isPreview) {
			String approvedBy = getValue(bpaApplication, "$.dscDetails[0].approvedBy");

			Map<String, Object> approverDetailsMap = getApproverDetails(requestInfo, tenantId, "BPA1", approvedBy,
					"EMPLOYEE");

			String approverName = !org.springframework.util.StringUtils
					.isEmpty(approverDetailsMap.get("nameOfApprover")) ? approverDetailsMap.get("nameOfApprover") + ""
							: "";
			// String bpaPermitHeader = getBpaPermitHeader(requestInfo, tenantId);
			String[] bpaPermitHeader = getUlbNameAndGradeFromMdms(requestInfo, tenantIdActual);

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
		}

		Image qrCode = getQrCode("", applicationNo, approvalDate,
				getValue(bpaApplication, "status"));

		if (isPreview) {
			document.add(rejectionLetterPreview);
			document.add(headerSubTitle2_1);
			document.add(headerSubTitle3);
			document.add(headerSubTitle4);
			document.add(Chunk.NEWLINE);
			document.add(rejectionLetterPreviewSubTitle2);
		} else {
			document.add(qrCode);
			document.add(logo);
			document.add(headerTitle);
			document.add(Chunk.NEWLINE);
			document.add(headerSubTitle);
			document.add(headerSubTitle2_1);
			document.add(headerSubTitle3);
			document.add(headerSubTitle4);
			document.add(headerSubTitle2);
		}

		document.add(Chunk.NEWLINE);
		document.add(Chunk.NEWLINE);
		document.add(subjectPara);
		document.add(Chunk.NEWLINE);
		document.add(sirMadam);
		document.add(withRefPara);
		document.add(Chunk.NEWLINE);

		document.add(hencePara);
		PdfPTable plotTable = new PdfPTable(7);
		plotTable.setLockedWidth(false);
		plotTable.setWidthPercentage(100f);
		addTableHeaderPlotTable(plotTable);
		if(((String)bpaApplication.get("businessService")).equalsIgnoreCase("BPA_OC_OS1") 
				|| ((String)bpaApplication.get("businessService")).equalsIgnoreCase("BPA_OC_OS2")
				|| ((String)bpaApplication.get("businessService")).equalsIgnoreCase("BPA_OC_OS3")
				|| ((String)bpaApplication.get("businessService")).equalsIgnoreCase("BPA_OC_OS4")) {
			addRowsForPlotTableForOutsideSujog(requestInfo, plotTable, getAdditionalDetailsMap(bpaApplication), tenantIdActual);
		} else
			addRowsForPlotTable(requestInfo, plotTable, getAdditionalDetailsMap(bpaApplication), tenantIdActual);
		document.add(Chunk.NEWLINE);
		document.add(plotTable);
		document.add(Chunk.NEWLINE);
		document.add(hencePara2);
		document.add(Chunk.NEWLINE);
		document.add(rejectListPara);
		document.add(Chunk.NEWLINE);

		if (!isPreview) {
			document.add(dateParagraph);
			document.add(approvedByParagraph);
		}
		document.close();
		return new ByteArrayInputStream(outputBytes.toByteArray());
	}
	
	public void addRowsForPlotTableForOutsideSujog(RequestInfo reqInfo,PdfPTable table1, Map<String, Object> additionalDetails, String tenantId) {
		java.util.List<Map<String, Object>> plotList = getPlotAndKhataDetails(additionalDetails);
		
		java.util.List<Object> boundaryList = getVillageList(reqInfo, tenantId);
		
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
				Phrase gpaHolderNamePhrase = new Phrase(plots.get("gpaHolderName") != null ? plots.get("gpaHolderName")+ "":"NA", fontPara1);
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

}
