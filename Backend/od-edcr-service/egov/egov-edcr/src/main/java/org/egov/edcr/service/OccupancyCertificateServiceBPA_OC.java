package org.egov.edcr.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.egov.edcr.constants.DxfFileConstants;
import org.egov.edcr.contract.oc.BuildingBlockDetails;
import org.egov.edcr.contract.oc.Floor;
import org.egov.edcr.contract.oc.OutsideOCDetails;
import org.egov.edcr.contract.oc.ScrutinyDetails;
import org.egov.infra.microservice.models.RequestInfo;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.text.BaseColor;
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
public class OccupancyCertificateServiceBPA_OC extends OccupancyCertificateService{
	
	public static String OCCUPANCYERTIFICATEPREVIEW = "Occupancy Certificate Preview";
	public static String HEADER_TITLE = "FORM-V";
	public static String HEADER_SUBTITLE1 = "See Rule 18(4)";
	public static String HEADER_TITLE1 = "Occupancy Certificate";
	public static String HEADER_SUBTITLE4 = "Sujog- OBPS APPLICATION NO. ";
	
	public static String PARA_1_1 ="The work of erection, re-erection and or material alteration undertaken in respect of ";
	public static String PARA_1_2 =" building has been completed under the supervision of %1$s (Empanellment No. %2$s) as per the completion certificate submitted.";
	
	public static String PARA_2 = "On inspection it is observed that the erection, reerection and or alteration undertaken with respect to above building is in accordance with approved plan and the conditions imposed vide Permission Letter No. %1$s, Date %2$s. The building is permitted for ";
	public static String PARA_2_1 =	" subjected to the following parameters and conditions:";

	public static String CONDITIONS = "Conditions:";
	public static String OTHER_CONDITIONS ="Other Conditions:";
	
	public static String CONDITION_1 = "All physical infrastructures like water supply, sewerage and drainage system and water harvesting structures shall be maintained.\n\n";
	public static String CONDITION_2 = "At least 1 tree per 80 sqm of the land area shall be developed for plantation and landscaping and maintained.\n\n";
	public static String CONDITION_3 = "Periodic inspection shall be made by the Fire Authority to ensure the fire safety of the building and compliance with the provisions of fire and life safety requirement (Part -4 of NBC).\n\n";
	public static String CONDITION_4 = "The building shall not be put to any other use other than the purpose for which the permission is accorded.\n\n";
	public static String CONDITION_5 = "A copy of permission letter and occupancy certificate shall be displayed in a conspicuous place on the property after occupancy certificate is issued.\n\n";
	public static String CONDITION_6 = "All the stipulated conditions given by any public agency shall be strictly adhered to.\n\n";

	
	public Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA, 16, Font.BOLD);
	public Font font1 = FontFactory.getFont(FontFactory.HELVETICA, 12);
	public Font fontBold = FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD);
	public Font fontBoldUnderlined = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Font.UNDERLINE);

	

	@Override
	public InputStream generateReport(LinkedHashMap bpaList, RequestInfo requestInfo, Boolean isPreview) throws Exception {
		
		String tenantIdActual = (String) bpaList.get("tenantId");
		String applicationNo = (String) bpaList.get("applicationNo");
		String[] ulbGradeNameAndUlbArray = getUlbNameAndGradeFromMdms(requestInfo, tenantIdActual);
		String ulbGradeNameAndUlb = (ulbGradeNameAndUlbArray[0] + " " + ulbGradeNameAndUlbArray[1]);
		String approvalNo = (String) bpaList.get("approvalNo");
		String approvalDate = getApprovalDate();
		String tenantId = StringUtils.capitalize(tenantIdActual.split("\\.")[1]);
		
		ScrutinyDetails scrutinyDetailsOC = getScrutinyDetailsOC(bpaList);
		Map<String, Object> additionalDetails = getAdditionalDetailsMap(bpaList);
		String ownerIds= bpaList.get("accountId").toString();
		Map<String,String> architectDetails=getArchitectDetails(requestInfo, ownerIds);
		java.util.List<Map<String,Object>> plotList = getPlotAndKhataDetails(additionalDetails);
		
		Paragraph approvedByParagraph = new Paragraph();
		Paragraph dateParagraph = new Paragraph();		
		String architectName = StringUtils.isEmpty(architectDetails.get("architectName"))?"N.A.":architectDetails.get("architectName");
		String empanellmentNumber =StringUtils.isEmpty(architectDetails.get("architectId"))?"N.A.":architectDetails.get("architectId");
				
		
		Document document = new Document();
		ByteArrayOutputStream outputBytes;
		outputBytes = new ByteArrayOutputStream();
		PdfWriter.getInstance(document, outputBytes);
		document.open();

		document.setMargins(36, 72, 36, 72);
		
		Image qrCode = getQrCode(applicationNo, approvalNo, approvalDate);

		//Image Logo 
		Image logo = getLogo();
		
		//Certificate Preview
		Paragraph occupancyCertPreview = new Paragraph(OCCUPANCYERTIFICATEPREVIEW, fontHeader);
		occupancyCertPreview.setAlignment(Paragraph.ALIGN_CENTER);
		
		Paragraph occupancyCertPreviewSubTitle2 = new Paragraph(
				"Letter No. " + DxfFileConstants.NA + ", " + tenantId + ", Dated: " + DxfFileConstants.NA, fontBold);
		occupancyCertPreviewSubTitle2.setAlignment(Paragraph.ALIGN_CENTER);
		
		Paragraph headerTitle = new Paragraph(HEADER_TITLE, fontBold);
		headerTitle.setAlignment(Paragraph.ALIGN_CENTER);
		
		Paragraph headerSubtitle1 = new Paragraph(HEADER_SUBTITLE1,fontBold);
		headerSubtitle1.setAlignment(Paragraph.ALIGN_CENTER);
		
		Paragraph headerSubtitle2 = new Paragraph(ulbGradeNameAndUlb+", "+tenantId, font1);
		headerSubtitle2.setAlignment(Paragraph.ALIGN_CENTER);
		
		Paragraph headerTitle1 = new Paragraph(HEADER_TITLE1, fontBold);
		headerTitle1.setAlignment(Paragraph.ALIGN_CENTER);
		
		Paragraph headerSubTitle3 = new Paragraph("LETTER NO. " + approvalNo + ", Date: "+approvalDate, font1);
		headerSubTitle3.setAlignment(Paragraph.ALIGN_CENTER);
		
		Paragraph headerSubTitle4 = new Paragraph(HEADER_SUBTITLE4 + applicationNo, fontBoldUnderlined);
		headerSubTitle4.setAlignment(Paragraph.ALIGN_CENTER);
		
		Chunk firstLine = new Chunk(PARA_1_1, font1);
		Chunk secondLine = new Chunk(String.format(PARA_1_2, architectName, empanellmentNumber), font1);
		
		Phrase phrase1 = new Phrase();

		String floorInfo = additionalDetails.get("noOfStoreys").toString();
		String occupancyList = extractOccupancies(scrutinyDetailsOC, floorInfo);	
		
		
		Chunk floorInfoChunk = new Chunk(occupancyList, fontBold);
		
		phrase1.add(firstLine);
		phrase1.add(floorInfoChunk);
		phrase1.add(secondLine);
				
		Paragraph para1 = new Paragraph();	
		para1.add(phrase1);
		para1.setAlignment(Paragraph.ALIGN_JUSTIFIED);
		
		PdfPTable plotTable = new PdfPTable(7);
        plotTable.setLockedWidth(false);
        plotTable.setWidthPercentage(100f);
        addTableHeaderPlotTable(plotTable);
        addRowsForPlotTable(requestInfo ,plotTable, plotList, tenantIdActual);
        
        String oldApprovalNo = null;
        Long oldApprovalDate1 = null;
        String oldApprovalDate = null;

        if (additionalDetails != null) {
            Map oldApplicationDetail = (Map)additionalDetails.get("oldApplicationDetail");
            if (oldApplicationDetail != null) {
                oldApprovalNo = (String)oldApplicationDetail.get("permitNumber");
                oldApprovalDate1 = (Long)oldApplicationDetail.get("oldPermitDate");
                
                if (oldApprovalDate1 != null) {
                    long timestamp = Double.valueOf(oldApprovalDate1).longValue();
                    Date date = new Date(timestamp);
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                    oldApprovalDate = sdf.format(date);
                }
            }
        }
		
        Phrase phrase2 = new Phrase();
		
		String occupancyType = null;
		if (additionalDetails != null) {
		    occupancyType = (String)additionalDetails.get("occupancyType");
		}
		
		Chunk firstline2 = new Chunk(String.format(PARA_2, oldApprovalNo, oldApprovalDate), font1);
		Chunk secondline2 = new Chunk(occupancyType!=null?occupancyType:"", fontBold);
		Chunk thirdline2 = new Chunk(PARA_2_1, font1);
		
		phrase2.add(firstline2);
		phrase2.add(secondline2);
		phrase2.add(thirdline2);
		

		Paragraph para2 = new Paragraph();
		para2.add(phrase2);
		para2.setAlignment(Paragraph.ALIGN_JUSTIFIED);
		
		List list = new List(com.itextpdf.text.List.ORDERED);
		list.setIndentationLeft(20);

		list.add(new ListItem(CONDITION_1, font1));
		list.add(new ListItem(CONDITION_2, font1));
		list.add(new ListItem(CONDITION_3, font1));
		list.add(new ListItem(CONDITION_4, font1));
		list.add(new ListItem(CONDITION_5, font1));
		list.add(new ListItem(CONDITION_6, font1));
		
		getApproverSection(bpaList, requestInfo, tenantIdActual, approvalDate, tenantId, approvedByParagraph,
				dateParagraph);

		//Conditions Paragraph
		Paragraph conditionsPara = new Paragraph(CONDITIONS, fontBoldUnderlined);
		conditionsPara.setAlignment(Paragraph.ALIGN_LEFT);
		
		String otherConditionsParaString = getValue(additionalDetails, "$.otherConditionsForPermitCertificate").toString();
		Paragraph otherConditionsPara=null,otherConditionsParaCondition=null;
		
		if(StringUtils.isNotEmpty(otherConditionsParaString)) {
			otherConditionsPara=new Paragraph(OTHER_CONDITIONS, fontBoldUnderlined);
			otherConditionsPara.setAlignment(Paragraph.ALIGN_LEFT);
			otherConditionsParaCondition= new Paragraph(otherConditionsParaString,font1);
		}
		
		PdfPTable paymentTable = getPaymentTable(bpaList, requestInfo, isPreview, tenantIdActual, applicationNo);

		
		if(isPreview) {
			document.add(occupancyCertPreview);
			document.add(Chunk.NEWLINE);
			document.add(occupancyCertPreviewSubTitle2);
		}
		else {
			document.add(qrCode);
			document.add(logo);
			document.add(Chunk.NEWLINE);
			document.add(Chunk.NEWLINE);
			document.add(headerTitle);
			document.add(headerSubtitle1);
			

		}
		document.add(headerSubtitle2);
		document.add(Chunk.NEWLINE);
		document.add(headerTitle1);
		document.add(headerSubTitle3);
		document.add(headerSubTitle4);
		document.add(Chunk.NEWLINE);
		document.add(Chunk.NEWLINE);
		document.add(para1);
		document.add(Chunk.NEWLINE);
		document.add(plotTable);
		document.add(Chunk.NEWLINE);
		document.add(para2);
		document.add(Chunk.NEWLINE);
		document.add(list);
		document.add(Chunk.NEWLINE);
		if(StringUtils.isNotEmpty(otherConditionsParaString)) {
			document.add(otherConditionsPara);
			document.add(Chunk.NEWLINE);
			document.add(otherConditionsParaCondition);
			document.add(Chunk.NEWLINE);
		}
		document.add(paymentTable);
		document.add(Chunk.NEWLINE);
		
		if(!isPreview) {
			document.add(dateParagraph);
			document.add(approvedByParagraph);
		}
		document.close();
		
		return new ByteArrayInputStream(outputBytes.toByteArray());
	}




	protected PdfPTable getPaymentTable(LinkedHashMap bpaList, RequestInfo requestInfo, Boolean isPreview,
			String tenantIdActual, String applicationNo) throws DocumentException {
		PdfPTable paymentTable = new PdfPTable(3);
		paymentTable.setLockedWidth(false);
		float[] paymentTableWidths = { 15f, 60f, 25f };
		paymentTable.setWidths(paymentTableWidths);
		addTableHeaderPaymentTable(paymentTable);
		if (!isPreview) {
			addRowsForPaymentsTable(requestInfo, paymentTable, tenantIdActual, applicationNo);
		} else {
			addRowsForEstimatedFees(requestInfo, paymentTable, bpaList);
		}
		return paymentTable;
	}
	
	protected PdfPTable getPaymentTableOCInsideSujog(LinkedHashMap bpaList, RequestInfo requestInfo, Boolean isPreview,
			String tenantIdActual, String applicationNo, String riskType) throws DocumentException {
		PdfPTable paymentTable = new PdfPTable(3);
		paymentTable.setLockedWidth(false);
		float[] paymentTableWidths = { 15f, 60f, 25f };
		paymentTable.setWidths(paymentTableWidths);
		addTableHeaderPaymentTable(paymentTable);
		if (!isPreview) {
			addRowsForPaymentsTable(requestInfo, paymentTable, tenantIdActual, applicationNo);
		} else {
			addRowsForEstimatedFeesOcInsideSujog(requestInfo, paymentTable, bpaList, riskType);
		}
		return paymentTable;
	}

	protected void getApproverSection(LinkedHashMap bpaList, RequestInfo requestInfo, String tenantIdActual,
			String approvalDate, String tenantId, Paragraph approvedByParagraph, Paragraph dateParagraph) {
		String approvedBy = getValue(bpaList, "$.dscDetails[0].approvedBy").toString();
		
		Map<String, Object> approverDetailsMap = getApproverDetails(requestInfo, tenantId, "BPA1", approvedBy,
				"EMPLOYEE");

		String approverName = !org.springframework.util.StringUtils
				.isEmpty(approverDetailsMap.get("nameOfApprover")) ? approverDetailsMap.get("nameOfApprover") + ""
						: "";

		
		Chunk dateSectionPart1 = new Chunk("Date: ", font1);
		Chunk dateSectionPart2 = new Chunk(approvalDate, font1);
		Phrase dateSection = new Phrase();
		dateSection.add(dateSectionPart1);
		dateSection.add(dateSectionPart2);

		dateParagraph.add(dateSection);
		dateParagraph.setAlignment(Element.ALIGN_LEFT);
		String[] bpaPermitHeader = getUlbNameAndGradeFromMdms(requestInfo, tenantIdActual);

		Font fontParaApprovedBy = FontFactory.getFont(FontFactory.HELVETICA, 12);
		Phrase approvedBySection = new Phrase();
		Chunk approvedBySectionLine1 = new Chunk("BY ORDER" + "\n", fontParaApprovedBy);
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

	private ScrutinyDetails getScrutinyDetailsOC(LinkedHashMap bpaList) {
		ObjectMapper objectMapper=new ObjectMapper();
		OutsideOCDetails outsideOCDetails= objectMapper.convertValue(bpaList.get("outsideOCDetails"), OutsideOCDetails.class) ;
		java.util.List<ScrutinyDetails> scrutinyDetailsList=outsideOCDetails.getScrutinyDetails();
		java.util.List<ScrutinyDetails> scrutinyDetailsOC =	scrutinyDetailsList.stream().filter(scrutinyDetail->scrutinyDetail.getScrutinyType().equalsIgnoreCase("OC")).collect(Collectors.toList());
		return scrutinyDetailsOC.get(0);
	}

	protected PdfPTable getAreaStatementTable(String plotArea, String netPlotArea, String roadWidth) throws DocumentException {
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
		areaStatementTable.addCell(plotArea+" sq. meter");

		areaStatementTable.addCell(bCell);
		areaStatementTable.addCell("Net Plot Area");
		areaStatementTable.addCell(netPlotArea+" sq. meter");

		areaStatementTable.addCell(cCell);
		areaStatementTable.addCell("Road Width");
		areaStatementTable.addCell(roadWidth+" meter");

		ListItem areaStatementItem = new ListItem();
		areaStatementItem.add(new Chunk("Area statement of the plot", fontBold));
		areaStatementItem.setIndentationLeft(60);
		areaStatementItem.setSpacingAfter(10);
		
		return areaStatementTable;
	}

	protected List getOCConditionList() {
		List conditionList = new List(List.ORDERED, List.NUMERICAL);
		
		ListItem conditionListItem1 = new ListItem();
		Phrase conditionPhrase1 = new Phrase(CONDITION_1, font1);
		conditionListItem1.add(conditionPhrase1);
		conditionListItem1.setAlignment(List.ALIGN_JUSTIFIED);
		conditionListItem1.setSpacingAfter(5);
		
		ListItem conditionListItem2 = new ListItem();
		Phrase conditionPhrase2 = new Phrase(CONDITION_2, font1);
		conditionListItem2.add(conditionPhrase2);
		conditionListItem2.setAlignment(List.ALIGN_JUSTIFIED);
		conditionListItem2.setSpacingAfter(5);
		
		ListItem conditionListItem3 = new ListItem();
		Phrase conditionPhrase3 = new Phrase(CONDITION_3, font1);
		conditionListItem3.add(conditionPhrase3);
		conditionListItem3.setAlignment(List.ALIGN_JUSTIFIED);
		conditionListItem3.setSpacingAfter(5);
		
		ListItem conditionListItem4 = new ListItem();
		Phrase conditionPhrase4 = new Phrase(CONDITION_4, font1);
		conditionListItem4.add(conditionPhrase4);
		conditionListItem4.setAlignment(List.ALIGN_JUSTIFIED);
		conditionListItem4.setSpacingAfter(5);
		
		conditionList.setIndentationLeft(30);
		conditionList.add(conditionListItem1);
		conditionList.add(conditionListItem2);
		conditionList.add(conditionListItem3);
		conditionList.add(conditionListItem4);
		
		return conditionList;
	}
	
	
	
	

	
	protected void addBuildingBlockTableHeader(PdfPTable buildingBlockTable, Integer blockNumber) {

		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 12, Font.BOLD);
		
		
			// for alteration subservices other than subservice A, show one extra column for existing area-
			Stream.of("Block-No."+blockNumber , "Covered area approved -Proposed(Sqm.)", "Proposed use")
			.forEach(columnTitle -> {
				PdfPCell header = new PdfPCell();
				header.setBackgroundColor(BaseColor.LIGHT_GRAY);
				header.setBorderWidth(2);
				header.setVerticalAlignment(Element.ALIGN_MIDDLE);
				header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
				buildingBlockTable.addCell(header);

			});
		
		
	}
	
	public String extractOccupancies(ScrutinyDetails scrutinyDetails, String floorInfos) {
	    String[] floorInfoArray = floorInfos.split(",\\s*");
	    
	    Map<String, String> occupancyMap = new HashMap<>();
	    occupancyMap.put("A", "Residential");
	    occupancyMap.put("B", "Commercial");
	    occupancyMap.put("C", "Public-Semi Public/Institutional");
	    occupancyMap.put("D", "Public Utility");
	    occupancyMap.put("E", "Industrial");
	    occupancyMap.put("F", "Educational");
	    occupancyMap.put("G", "Transportation");
	    occupancyMap.put("H", "Agriculture");
	    occupancyMap.put("M", "Mixed Use");
	    occupancyMap.put("AF", "Additional Feature");
	    
	    java.util.List<String> blockDetailsWithFloorInfo = new ArrayList<>();
	    
	    for (int i = 0; i < scrutinyDetails.getBuildingBlockDetails().size(); i++) {
	        BuildingBlockDetails blockDetail = scrutinyDetails.getBuildingBlockDetails().get(i);
	        String floorInfo = (i < floorInfoArray.length && floorInfoArray[i] != null) ? floorInfoArray[i] : "";
	        
	        Set<String> uniqueOccupancies = new LinkedHashSet<>();
	        for (Floor floor : blockDetail.getFloors()) {
	            String subOccupancyValue = floor.getSubOcuupancy().getValue();
	            if (subOccupancyValue != null && !subOccupancyValue.isEmpty()) {
	                String code = subOccupancyValue.split("-")[0];
	                if (occupancyMap.containsKey(code)) {
	                    uniqueOccupancies.add(occupancyMap.get(code));
	                }
	            }
	        }
	        
	        // Convert Set to List to handle the elements more easily
	        java.util.List<String> occupancyList = new ArrayList<>(uniqueOccupancies);
	        String combinedOccupancies = floorInfo;
	        
	        if (!occupancyList.isEmpty()) {
	            combinedOccupancies += " " + String.join(" cum ", occupancyList);
	        }
	        
	        blockDetailsWithFloorInfo.add(combinedOccupancies);
	    }
	    
	    return String.join(" and ", blockDetailsWithFloorInfo);
	}
}
