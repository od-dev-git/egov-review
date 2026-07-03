package org.egov.edcr.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.egov.common.entity.edcr.Plan;
import org.egov.edcr.constants.DxfFileConstants;
import org.egov.edcr.constants.OdishaUlbs;
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
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

@Service
public class RejectionLetterService extends PermitOrderService {

	public Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA, 16, Font.BOLD);
	public Font font1 = FontFactory.getFont(FontFactory.HELVETICA, 12);
	public Font fontBold = FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD);
	public Font fontBoldUnderlined = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Font.UNDERLINE);
	
	public static String REJECTIONLETTERPREVIEW = "Refusal Letter Preview";
	
	public static String PARAGRAPH_0_0 = "Form III";
	public static String PARAGRAPH_0_1 = "[see rule 10(3) of ODA (CAF) Rules 2016]";
	public static String PARAGRAPH_0_2 = "ORDER FOR REFUSAL OF PERMISSION";
	
	public static String PARAGRAPH_1_0 = "Ref: %s in respect of %s .";
	public static String PARAGRAPH_1_0_1 = "Plot Number ";
	public static String PARAGRAPH_1_1 = "in Village/Mouza. ";
	public static String PARAGRAPH_1_2 = "under the Scheme notified vide Gazette no. 1034 dated 30.05.2017";
	public static String PARAGRAPH_1_3 = "Your reply to this office letter No %s dated %s";
	public static String PARAGRAPH_1_4 = " has not been found satisfactory and in compliance to the provisions of building and development norms in force / you have failed to show any cause in response to this office letter No %s Dated %s within the prescribed time stipulated in the above referred letter. ";
	public static String PARAGRAPH_1_5 = " Hence, in exercise of the powers under sub-section (3) of section 16 of the Odisha Development Authority Act, 1982, your application for permission to undertake development on ";
	public static String PARAGRAPH_1_6 = "of %s within the Development Plan is hereby refused on the following grounds - ";
	public static String PARAGRAPH_1_7 = "Khata No.";
	
	public static String NEWCONSTRUCTION_SUBJECT="New Construction of a %s storied %s building";
	public static String ADDITIONALTERATION_SUBJECT="Addition/Alteration of a %s storied %s building";
	public static String LANDREG_SUBJECT="regularization of unauthorizedly sub-divided plot" ;
	public static String BUILDINGREG_SUBJECT="Building Regularization of a %s storied %s";

	
	

	@Override
	public InputStream generateReport(Plan plan, LinkedHashMap bpaApplication, RequestInfo requestInfo,
			Boolean isPreview) {
		try {
			return createPdf(plan, bpaApplication, requestInfo, isPreview);
		} catch (Exception e) {
			e.printStackTrace();
			throw new ApplicationRuntimeException("Error while generating permit order pdf", e);
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
		String ownersNamesCsv = getNameOfOwner(bpaApplication);
		
		String localityName = getValue(bpaApplication, "$.landInfo.address.locality.name");

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

		Map<String, Object> additionalDetails = getAdditionalDetailsMap(bpaApplication);
		
		String subject="", floorInfo="", subOccupancy="";
		if(plan!=null) {
			if(plan.getPlanInformation()!=null) {
				floorInfo = plan.getPlanInformation().getFloorInfo() ==null? "" : plan.getPlanInformation().getFloorInfo()+" ";
				subOccupancy = plan.getPlanInformation().getSubOccupancy()==null? "" : plan.getPlanInformation().getSubOccupancy() + " ";
			}
		}
		
		if(additionalDetails.get("serviceType") !=null) {
			if(additionalDetails.get("serviceType").toString().equalsIgnoreCase("NEW_CONSTRUCTION")) {
				subject= String.format(PARAGRAPH_1_0, "Sujog-OBPS application No. " + applicationNo, String.format(NEWCONSTRUCTION_SUBJECT, floorInfo, subOccupancy));
			}
			else if(additionalDetails.get("serviceType").toString().equalsIgnoreCase("ALTERATION")) {
				subject= String.format(PARAGRAPH_1_0, "Sujog-OBPS application No. " + applicationNo, String.format(ADDITIONALTERATION_SUBJECT, floorInfo, subOccupancy));
			}
		}
		
		if (plan != null && StringUtils.isEmpty(subject)) {
			if (plan.getPlanInformation().getServiceType() != null) {

				if (plan.getPlanInformation().getServiceType().equalsIgnoreCase("NEW_CONSTRUCTION")) {
					subject = String.format(PARAGRAPH_1_0, "Sujog-OBPS application No. " + applicationNo,
							String.format(NEWCONSTRUCTION_SUBJECT, floorInfo, subOccupancy));
				} else if (plan.getPlanInformation().getServiceType().equalsIgnoreCase("ALTERATION")) {
					subject = String.format(PARAGRAPH_1_0, "Sujog-OBPS application No. " + applicationNo,
							String.format(ADDITIONALTERATION_SUBJECT, floorInfo, subOccupancy));
				}
			} else {
				subject = String.format(PARAGRAPH_1_0, "Sujog-OBPS application No. " + applicationNo,
						String.format(NEWCONSTRUCTION_SUBJECT, floorInfo, subOccupancy));
			}
		} 
		Paragraph subjectPara = new Paragraph(subject, fontBold);		
		
		Paragraph sirMadam = new Paragraph("Sir/Madam, ", fontBold);
		Paragraph withRefPara = new Paragraph();
		Chunk withRef = new Chunk(String.format(PARAGRAPH_1_3, noticeDetails.get("LetterNo"), noticeDetails.get("createdTime")), font1);
		Chunk satisfactory = new Chunk(String.format(PARAGRAPH_1_4, noticeDetails.get("LetterNo"), noticeDetails.get("createdTime")), font1);
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
		
		for(String cause : rejectionMap1) {
			if(rejectionMap.containsKey(cause)) {
				Phrase newCause = new Phrase(rejectionMap.get(cause)+"\n\n", font1);
				ListItem listItemCause = new ListItem(newCause);
				rejectList.add(listItemCause);
			} else {
				Phrase newCause = new Phrase(cause+"\n\n", font1);
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

		if(!isPreview) {
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
	
			Image qrCode = getQrCode(ownersNamesCsv, applicationNo, approvalDate,
					getValue(bpaApplication, "edcrNumber"));
		
		
		if(isPreview) {
			document.add(rejectionLetterPreview);
			document.add(headerSubTitle2_1);
			document.add(headerSubTitle3);
			document.add(headerSubTitle4);
			document.add(Chunk.NEWLINE);
			document.add(rejectionLetterPreviewSubTitle2);
		}
		else {
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
		
		if (CollectionUtils.isEmpty(getPlotAndKhataDetails(additionalDetails))) {
			String plotNo = plan.getPlanInformation().getPlotNo() + " ";
			Chunk pltoNumber = new Chunk("Plot No. ", font1);
			Chunk pltoNumber2 = new Chunk(plotNo+",", fontBold);
			Chunk chunk7 = new Chunk(PARAGRAPH_1_7, font1);
			String khataNo = plan.getPlanInformation().getKhataNo() + " ";
			Chunk khataNumber = new Chunk(khataNo, fontBold);
			Chunk village = new Chunk(PARAGRAPH_1_1, font1);
			Chunk locality = new Chunk(localityName + " ", fontBold);
			
			hencePara.add(pltoNumber);
			hencePara.add(pltoNumber2);
			hencePara.add(chunk7);
			hencePara.add(khataNumber);
			hencePara.add(village);
			hencePara.add(locality);
			document.add(hencePara);
			
		} else {
			document.add(hencePara);			
			PdfPTable plotTable = new PdfPTable(7);
			plotTable.setLockedWidth(false);
			plotTable.setWidthPercentage(100f);
			addTableHeaderPlotTable(plotTable);
			addRowsForPlotTable(requestInfo, plotTable, additionalDetails, tenantIdActual);
			document.add(Chunk.NEWLINE);
			document.add(plotTable);
			document.add(Chunk.NEWLINE);
		}
		
		
		
		document.add(hencePara2);
		document.add(Chunk.NEWLINE);
		document.add(rejectListPara);
		document.add(Chunk.NEWLINE);
		
		if(!isPreview) {
			document.add(dateParagraph);
			document.add(approvedByParagraph);
		}
		
		
		
		document.close();
		return new ByteArrayInputStream(outputBytes.toByteArray());
	}

}
