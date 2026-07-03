package org.egov.edcr.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.egov.common.entity.dcr.helper.EdcrApplicationInfo;
import org.egov.common.entity.edcr.Block;
import org.egov.common.entity.edcr.Plan;
import org.egov.edcr.constants.DxfFileConstants;
import org.egov.infra.exception.ApplicationRuntimeException;
import org.egov.infra.microservice.models.RequestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
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
public class OccupancyCertificateServiceInsideSujog extends OccupancyCertificateServiceBPA_OC {
	
	private static Logger LOG = Logger.getLogger(OccupancyCertificateServiceInsideSujog.class);

	public static String OCCUPANCYERTIFICATEPREVIEW = "Occupancy Certificate Preview";
	public static String HEADER_TITLE = "FORM-V";
	public static String HEADER_SUBTITLE1 = "See Rule 18(4)";
	public static String HEADER_TITLE1 = "Occupancy Certificate";
	public static String HEADER_SUBTITLE4 = "Sujog- OBPS APPLICATION NO. ";

	public static String PARA_1_1 = "The work of erection, re-erection and or material alteration undertakes in respect of ";
	public static String PARA_1_2 =	" building has been completed under the supervision of %1$s (Empanellment No. %2$s) as per the completion certificate submitted.";

	public static String PARA_2 = "On inspection it is observed that the erection, reerection and or alteration undertaken with respect to above building is in accordance with approved plan and the conditions imposed vide Permission Letter No. %1$s, Date %2$s. The building is permitted for ";
	public static String PARA_2_1 =	" subjected to the following parameters and conditions:";

	public static String CONDITIONS = "Conditions:";
	public static String OTHER_CONDITIONS = "Other Conditions:";
	
	public static final String PROVIDED_LIFT_DETAIL = "providedLiftDetail";
	public static final String REQUIRED_LIFT_DETAIL = "requiredLiftDetail";
	public static final String SCRUTINY_DETAIL_PROVIDED = "Provided";
	public static final String SCRUTINY_DETAIL_REQUIRED = "Required";
	public static final String GROUND_FLOOR_NO = "0";
	public static final String DETAIL = "Detail";
	public static final String DESCRIPTION_IN_SCRUTINY_DETAIL = "Description";
	public static final String NO_OF_TREE_PER_PLOT = "No of tree as per plot";
	
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

	@Autowired
	private EdcrExternalService edcrExternalService;

	@Override
	public InputStream generateReport(LinkedHashMap bpaApplication, RequestInfo requestInfo, Boolean isPreview)
			throws Exception {

		Plan plan = null;

		String edcrNo = "";

		if (bpaApplication.get("edcrNumber") != null) {
			edcrNo = bpaApplication.get("edcrNumber").toString();
		}

		if (edcrNo != null)
			edcrNo.trim();

		try {
			EdcrApplicationInfo edcrApplicationInfo = edcrExternalService.loadEdcrApplicationDetails(edcrNo);
			plan = edcrApplicationInfo.getPlan();

		} catch (Exception e) {
			e.printStackTrace();
			throw new ApplicationRuntimeException("Unable to get plan info!", e);
		}
		
		

		String tenantIdActual = (String) bpaApplication.get("tenantId");
		String applicationNo = (String) bpaApplication.get("applicationNo");
		String[] ulbGradeNameAndUlbArray = getUlbNameAndGradeFromMdms(requestInfo, tenantIdActual);
		String ulbGradeNameAndUlb = (ulbGradeNameAndUlbArray[0] + " " + ulbGradeNameAndUlbArray[1]);
		String approvalNo = (String) bpaApplication.get("approvalNo");
		String approvalDate = getApprovalDate();
		
		String tenantId = StringUtils.capitalize(tenantIdActual.split("\\.")[1]);

		Map<String, Object> additionalDetails = getAdditionalDetailsMap(bpaApplication);
		
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
		
		String riskType = plan.getPlanInformation().getRiskType();

		String ownerIds = bpaApplication.get("accountId").toString();
		Map<String, String> architectDetails = getArchitectDetails(requestInfo, ownerIds);
		java.util.List<Map<String, Object>> plotList = getPlotAndKhataDetails(additionalDetails);

		Paragraph approvedByParagraph = new Paragraph();
		Paragraph dateParagraph = new Paragraph();

		
		String architectName = StringUtils.isEmpty(architectDetails.get("architectName")) ? "N.A."
				: architectDetails.get("architectName");
		String empanellmentNumber = StringUtils.isEmpty(architectDetails.get("architectId")) ? "N.A."
				: architectDetails.get("architectId");

		Document document = new Document();
		ByteArrayOutputStream outputBytes;
		outputBytes = new ByteArrayOutputStream();
		PdfWriter.getInstance(document, outputBytes);
		document.open();

		document.setMargins(36, 72, 36, 72);

		Image qrCode = getQrCode(applicationNo, approvalNo, approvalDate);

		// Image Logo
		Image logo = getLogo();

		// Certificate Preview
		Paragraph occupancyCertPreview = new Paragraph(OCCUPANCYERTIFICATEPREVIEW, fontHeader);
		occupancyCertPreview.setAlignment(Paragraph.ALIGN_CENTER);

		Paragraph occupancyCertPreviewSubTitle2 = new Paragraph(
				"Letter No. " + DxfFileConstants.NA + ", " + tenantId + ", Dated: " + DxfFileConstants.NA, fontBold);
		occupancyCertPreviewSubTitle2.setAlignment(Paragraph.ALIGN_CENTER);

		Paragraph headerTitle = new Paragraph(HEADER_TITLE, fontBold);
		headerTitle.setAlignment(Paragraph.ALIGN_CENTER);

		Paragraph headerSubtitle1 = new Paragraph(HEADER_SUBTITLE1, fontBold);
		headerSubtitle1.setAlignment(Paragraph.ALIGN_CENTER);

		Paragraph headerSubtitle2 = new Paragraph(ulbGradeNameAndUlb + ", " + tenantId, font1);
		headerSubtitle2.setAlignment(Paragraph.ALIGN_CENTER);

		Paragraph headerTitle1 = new Paragraph(HEADER_TITLE1, fontBold);
		headerTitle1.setAlignment(Paragraph.ALIGN_CENTER);
		
		approvalNo = approvalNo!=null?approvalNo:"NA";
				
		Paragraph headerSubTitle3 = new Paragraph("LETTER NO. " + approvalNo + ", Date: " + approvalDate, font1);
		headerSubTitle3.setAlignment(Paragraph.ALIGN_CENTER);

		Paragraph headerSubTitle4 = new Paragraph(HEADER_SUBTITLE4 + applicationNo, fontBoldUnderlined);
		headerSubTitle4.setAlignment(Paragraph.ALIGN_CENTER);

		Phrase phrase1 = new Phrase();
		
		String floorInfo = plan.getPlanInformation().getFloorInfo();		
		String allOccupancies = getAllOccupancies(plan);		
		String[] floors = floorInfo.replaceAll("[\\[\\]]", "").split(",\\s*");
		String[] occupancies = allOccupancies.split(",\\s*");
		
		StringBuilder floorAndOccupancies = new StringBuilder();
		
		for (int i = 0; i < Math.min(floors.length, occupancies.length); i++) {
		    if (i > 0) {
		        if (i == Math.min(floors.length, occupancies.length) - 1) {
		            floorAndOccupancies.append(" and ");
		        } else {
		            floorAndOccupancies.append(", ");
		        }
		    }
		    floorAndOccupancies.append(floors[i].trim())
		          .append(" ")
		          .append(occupancies[i].trim());
		}
		
		Chunk floorInfoChunk = new Chunk(floorAndOccupancies.toString(), fontBold);
		
		Chunk firstLine = new Chunk(PARA_1_1, font1);
		Chunk secondLine = new Chunk(String.format(PARA_1_2, architectName, empanellmentNumber), font1);
		
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
		addRowsForPlotTableInsideSujog(requestInfo, plotTable, plotList, tenantIdActual);
		
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

		getApproverSection(bpaApplication, requestInfo, tenantIdActual, approvalDate, tenantId, approvedByParagraph,
				dateParagraph);

		String otherConditionsParaString = getValue(additionalDetails, "$.otherConditionsForPermitCertificate")
				.toString();
		Paragraph otherConditionsPara = null, otherConditionsParaCondition = null;

		if (StringUtils.isNotEmpty(otherConditionsParaString)) {
			otherConditionsPara = new Paragraph(OTHER_CONDITIONS, fontBoldUnderlined);
			otherConditionsPara.setAlignment(Paragraph.ALIGN_LEFT);
			otherConditionsParaCondition = new Paragraph(otherConditionsParaString, font1);
		}

		PdfPTable paymentTable = getPaymentTableOCInsideSujog(bpaApplication, requestInfo, isPreview, tenantIdActual, applicationNo, riskType);

		if (isPreview) {
			document.add(occupancyCertPreview);
			document.add(Chunk.NEWLINE);
			document.add(occupancyCertPreviewSubTitle2);
		} else {
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
		if (StringUtils.isNotEmpty(otherConditionsParaString)) {
			document.add(otherConditionsPara);
			document.add(Chunk.NEWLINE);
			document.add(otherConditionsParaCondition);
			document.add(Chunk.NEWLINE);
		}
		document.add(paymentTable);
		document.add(Chunk.NEWLINE);

		if (!isPreview) {
			document.add(dateParagraph);
			document.add(approvedByParagraph);
		}
		document.close();

		return new ByteArrayInputStream(outputBytes.toByteArray());
	}

	
	private String getAllOccupancies(Plan plan) {
		
		java.util.List<String> allOccupancies = new ArrayList<>();
		
		for (Block block : plan.getBlocks()) {

			String occupancies = block.getBuilding().getFloors().stream().flatMap(flr -> flr.getOccupancies().stream())
					.map(occupancy -> occupancy.getTypeHelper().getType().getName())
					.collect(Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new),
							set -> String.join(" cum ", set)));
			allOccupancies.add(occupancies);
		}
		
		return String.join(", ", allOccupancies);
		
	}


	private void addRowsForPlotTableInsideSujog(RequestInfo reqInfo, PdfPTable table1, java.util.List<Map<String, Object>> plotList,
			String tenantId) {

		java.util.List<Object> boundaryList = getVillageList(reqInfo, tenantId);

		Font fontPara1 = FontFactory.getFont(FontFactory.HELVETICA, 12);
		if (!CollectionUtils.isEmpty(plotList)) {
			for (Map<String, Object> plots : plotList) {

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
				Phrase villageCellPhrase = new Phrase(
						getVillageFromVillageList(boundaryList, (String) plots.get("village")) + "", fontPara1);
				villageCell.addElement(villageCellPhrase);

				PdfPCell landOwnerNameCell = new PdfPCell();
				Phrase landOwnerNameCellPhrase = new Phrase(plots.get("applicantName") + "", fontPara1);
				landOwnerNameCell.addElement(landOwnerNameCellPhrase);

				PdfPCell gpaHolderNameCell = new PdfPCell();
				Phrase gpaHolderNamePhrase = new Phrase(
						plots.get("gpaHoldername") != null ? plots.get("gpaHoldername") + "" : "N.A.", fontPara1);
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


}
