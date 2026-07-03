package org.egov.edcr.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import org.egov.common.entity.edcr.SetBack;
import org.egov.common.entity.edcr.TypicalFloor;
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
public class PermitOrderServiceBPA1 extends PermitOrderService {
	
	private static final Logger LOG = Logger.getLogger(PermitOrderServiceBPA1.class);

	public static String PARAGRAPH_ONE = "Permission under sub-section (3) of the Section-16 of the Odisha Development Authorities Act, 1982 is hereby granted in favour of Land Owner";
	public static String PARAGRAPH_ONE_CRZ = "Permission under claus 4(i)(d) of CRZ notification, is hereby granted in favour of";
	public static String PARAGRAPH_ONE_OLD = "Permission under sub-section (3) of the Section-16 of the Odisha Development Authorities Act, 1982 is hereby granted in favour of";
	
	public static String PARAGRAPH_ONE_SPARIT = "Permission/Licence under sub section(3) of section 31/ sub section (1) of section 33 of Odisha Town Planning & Improvement Trust Act 1956 is hereby granted in favour of Land Owner";
	public static String PARAGRAPH_ONE_SPARIT_OLD = "Permission/Licence under sub section(3) of section 31/ sub section (1) of section 33 of Odisha Town Planning & Improvement Trust Act 1956 is hereby granted in favour of";
	
	public static String ADDRESS = "LAXMIPRIYA PANDA AND OTHERS,";
	public static String ADDRESS2 = "KALARAHANGA";
	public static String ADDRESS1 = "INJANA,";
	public static String PERMITPRVIEW = "PERMIT LETTER PREVIEW";
	public static String PARAGRAPH_1_1 = "For %s of a ";
//	public static String PARAGRAPH_1_2 = "S+2 ";
	public static String PARAGRAPH_1_3 = "storeyed ";
//	public static String PARAGRAPH_1_4 = "Residential ";
	
	public static String PARAGRAPH_1_5_OLD = "Building in respect of plot No.";
	public static String PARAGRAPH_1_5_ALTERATION_OLD = "Building %s in respect of plot No.";
	
	public static String PARAGRAPH_1_5 = "Building in respect of";
	public static String PARAGRAPH_1_5_ALTERATION = "Building %s in respect of";
//	public static String PARAGRAPH_1_6 = "1337, ";
	public static String PARAGRAPH_1_7 = "Khata No.";
//	public static String PARAGRAPH_1_8 = "180 ";
	public static String PARAGRAPH_1_9 = "Village/Mouza. ";
//	public static String PARAGRAPH_1_10 = "Injana ";
	public static String PARAGRAPH_1_11 = "of %s within the Development Plan Area subject to following conditions/ restrictions: ";

	public static String PARAGRAPH_2_1 = "The land/ Building shall be used exclusively for %s purpose and the uses shall not be changed to any other use without prior approval of this Authority.\n\n";
	public static String PARAGRAPH_2_2 = "The development shall be undertaken strictly according to plans enclosed with necessary permission endorsement.\n\n";
	public static String PARAGRAPH_2_3_1 = "Parking space measuring ";
//	public static String PARAGRAPH_2_3_2 = "134.51 ";
	public static String PARAGRAPH_2_3_3 = "sq. mtr. as shown in the approved plan shall be exclusively used for parking and no part of it will be used for any other purpose.\n\n";
	public static String PARAGRAPH_2_4 = "The land over which construction is proposed is accessible by an approved means of access of %s mtr. width.\n\n";
	public static String PARAGRAPH_2_5 = "The land in question must be in lawful ownership and peaceful possession of the applicant.\n\n";
	public static String PARAGRAPH_2_6 = "The applicant shall free gift %s sq.mtr. of located in the %s for the widening of the road/construction of new roads and other public amenities prior to completion of the development as indicated in the plan.\n\n";
	public static String PARAGRAPH_2_7_1 = "The permission is valid for period of ";
	public static String PARAGRAPH_2_7_2 = "three years ";
	public static String PARAGRAPH_2_7_3 = "with effect from the date of issue.\n\n";
	public static String PARAGRAPH_2_8 = "Permission accorded under the provision of Section 16 of ODA Act, cannot be construed as an evidence to claim right title interest on the plot on which the permission has been granted.\n\n";
	public static String PARAGRAPH_2_8_SPARIT = "Permission accorded under the provision of Section 31 and 33 of OTPIT Act, cannot be construed as an evidence to claim right title interest on the plot on which the permission has been granted.\n\n";
	public static String PARAGRAPH_2_8_CRZ = "Permission accorded under the provision of claus 4(i)(d) of CRZ notification cannot be construed as an evidence to claim right title interest on the plot on which the permission has been granted.\n\n";
	public static String PARAGRAPH_2_8_CRZ_2 = "The approval has been considered on receipt of CRZ clearance of Odisha Coastal Zone Management Authority and the conditions there upon shall be strictly added.\n\n";
	
	public static String PARAGRAPH_2_9 = "If any dispute arises with respect to right, title interest on the land on which the permission has been granted, the permission so granted shall be automatically treated as canceled during the period of dispute.\n\n";
	public static String PARAGRAPH_2_10 = "Any construction and development made by the applicant or owner on the disputed land will be at his risk without any legal or financial liability on the Authority.\n\n";
	public static String OTHER_CONDITIONS = "Other conditions to be complied by the applicant are as per the following:\n";
	public static String PARAGRAPH_2_11_1 = "The ";
//	public static String PARAGRAPH_2_11_2 = "S+2 residential building ";
	public static String PARAGRAPH_2_11_3 = "is approved on payment following fees:\n\n";
//	public static String PARAGRAPH_2_11_3_A = "Sanction fee : Rs 6,689.00/-only";
	// public static String PARAGRAPH_2_11_3_B = "Construction Workers Welfare Cess
	// : Rs 83,967.00/-only";
	public static String PARAGRAPH_3_1 = "Total plot area (As per document) : ";
	// public static String PARAGRAPH_3_2 = "607.24 sqmt.";
	public static String PARAGRAPH_4_1 = "Total plot area (As per Possession) : ";
	// public static String PARAGRAPH_4_2 = "563.11 sqmt.";
	public static String PARAGRAPH_5_1 = "Total built up area : ";
	// public static String PARAGRAPH_5_2 = "445.92 sqmt.";
	public static String PARAGRAPH_6_1 = "Total FAR area : ";
	// public static String PARAGRAPH_6_2 = "311.41 sqmt.";
	public static String PARAGRAPH_7_1 = "FAR : ";
	// public static String PARAGRAPH_7_2 = "0.55";

	public Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA, 16, Font.BOLD);
	public Font font1 = FontFactory.getFont(FontFactory.HELVETICA, 12);
	public Font fontBold = FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD);
	public Font fontBoldUnderlined = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Font.UNDERLINE);
	
	@Override
	public InputStream generateReport(Plan plan, LinkedHashMap bpaApplication, RequestInfo requestInfo, Boolean isPreview) {
		try {
			return createPdf(plan, bpaApplication, requestInfo, isPreview);
		} catch (Exception e) {
			e.printStackTrace();
			throw new ApplicationRuntimeException("Error while generating permit order pdf", e);
		}
	}

	public InputStream createPdf(Plan plan, LinkedHashMap bpaApplication, RequestInfo requestInfo, Boolean isPreview) throws Exception {
		
		String tenantIdActual = getValue(bpaApplication, "tenantId");
		
		OdishaUlbs ulb = OdishaUlbs.getUlb(plan.getThirdPartyUserTenantld());
		
		boolean isInCRZ = false;
		
		String crzNumber = plan.getPlanInformation().getCrzNumberForProjectsFallingUnderCrzArea();
		if (crzNumber != null && crzNumber.matches("\\d") && (Integer.parseInt(crzNumber) == 1 || Integer.parseInt(crzNumber) == 2
				|| Integer.parseInt(crzNumber) == 3)) {
			isInCRZ = true;
		}
		
		Document document = new Document();
		ByteArrayOutputStream outputBytes;
		outputBytes = new ByteArrayOutputStream();
		PdfWriter.getInstance(document, outputBytes);
		document.open();
		Image logo = getLogo();
		String[] ulbGradeNameAndUlbArray = getUlbNameAndGradeFromMdms(requestInfo, tenantIdActual);
		String ulbGradeNameAndUlb = (ulbGradeNameAndUlbArray[0] + " " + ulbGradeNameAndUlbArray[1]);
		
		
		Map<String, Object> additionalDetails = getAdditionalDetailsMap(bpaApplication);
		//Cuttack Municipal Corporation
		Paragraph headerTitle = new Paragraph(ulbGradeNameAndUlb, fontHeader);
		headerTitle.setAlignment(Paragraph.ALIGN_CENTER);
		
		Paragraph permitPreviewTitle = new Paragraph(PERMITPRVIEW, fontHeader);
        permitPreviewTitle.setAlignment(Paragraph.ALIGN_CENTER);
		
		//Form-II (Order for Grant of Permission)
		Paragraph headerSubTitle = new Paragraph("Form-II (Order for Grant of Permission)", font1);
		headerSubTitle.setAlignment(Paragraph.ALIGN_CENTER);
		
		String tenantId = StringUtils.capitalize(tenantIdActual.split("\\.")[1]);
		@SuppressWarnings("deprecation")
		String approvalNo = getValue(bpaApplication, "approvalNo");
		String approvalDate = getApprovalDate();
		Paragraph headerSubTitle2 = new Paragraph(
				"Letter No. " + approvalNo + ", " + tenantId + ", Dated: " + approvalDate,
				fontBold);
		headerSubTitle2.setAlignment(Paragraph.ALIGN_CENTER);
		
		Paragraph permitPreviewSubTitle2 = new Paragraph(
				"Letter No. " + DxfFileConstants.NA + ", " + tenantId + ", Dated: " + DxfFileConstants.NA, fontBold);
		permitPreviewSubTitle2.setAlignment(Paragraph.ALIGN_CENTER);
		
		String applicationNo = getValue(bpaApplication, "applicationNo");
		Paragraph headerSubTitle3 = new Paragraph("Sujog-OBPS APPLICATION NO. " + applicationNo, fontBoldUnderlined);
		headerSubTitle3.setAlignment(Paragraph.ALIGN_CENTER);
		

		

		
	
		Paragraph paragraph1 = new Paragraph(PARAGRAPH_ONE, font1);
		Paragraph paragraph1_old = new Paragraph(PARAGRAPH_ONE_OLD, font1);
		Paragraph paragraph1_CRZ = new Paragraph(PARAGRAPH_ONE_CRZ, font1);
		
		Paragraph paragraph1_Sparit = new Paragraph(PARAGRAPH_ONE_SPARIT, font1);		
		Paragraph paragraph1_Sparit_old = new Paragraph(PARAGRAPH_ONE_SPARIT_OLD, font1);
		
		Paragraph addressed = new Paragraph("Smt. /Shri,", fontBold);

		String ownersNamesCsv = getNameOfOwner(bpaApplication);
		
		Paragraph applicants = new Paragraph(ownersNamesCsv + ",", fontBold);
		applicants.setIndentationLeft(30);

		String localityName = getValue(bpaApplication, "$.landInfo.address.locality.name");
		Paragraph address2 = new Paragraph(localityName + ",", fontBold);
		address2.setIndentationLeft(30);

		// to show correspondenceAddress from owner details--
		String primaryOwnerCorrespondenceAddress = getCorrespondenceAddress(bpaApplication);
		Paragraph address3 = new Paragraph(primaryOwnerCorrespondenceAddress, fontBold);
		address3.setIndentationLeft(30);
		
		Chunk forServiceType = new Chunk(String.format("For %s of a ", getServiceType(plan, additionalDetails)), font1);
		String floorInfo = plan.getPlanInformation().getFloorInfo() + " ";
		Chunk floorInform = new Chunk(floorInfo, fontBold);
		Chunk storeyed = new Chunk(PARAGRAPH_1_3, font1);
		String subOccupancy = plan.getPlanInformation().getSubOccupancy() + " ";
		Chunk subOccupanc = new Chunk(subOccupancy, fontBold);
		String oldPermitNo = getPreviousPermitNo(additionalDetails);
		
		String buildingInRespectOfPlusAlteration="";
		
		if(CollectionUtils.isEmpty(getPlotAndKhataDetails(additionalDetails))) {
			buildingInRespectOfPlusAlteration = PARAGRAPH_1_5_ALTERATION_OLD;
		} else {
			buildingInRespectOfPlusAlteration = PARAGRAPH_1_5_ALTERATION;
		}
		buildingInRespectOfPlusAlteration = String.format(buildingInRespectOfPlusAlteration,
				getAlterationSubserviceStatement(additionalDetails));
		buildingInRespectOfPlusAlteration = buildingInRespectOfPlusAlteration.replace("xxxxx", oldPermitNo);
		Chunk buildingInRespectOf = new Chunk(buildingInRespectOfPlusAlteration, font1);
		
		
		
		String plotNo = plan.getPlanInformation().getPlotNo() + " ";
		Chunk pltoNumber = new Chunk(plotNo, fontBold);
		Chunk chunk7 = new Chunk(PARAGRAPH_1_7, font1);
		String khataNo = plan.getPlanInformation().getKhataNo() + " ";
		Chunk khataNumber = new Chunk(khataNo, fontBold);
		Chunk village = new Chunk(PARAGRAPH_1_9, font1);
		Chunk locality = new Chunk(localityName + " ", fontBold);
		Chunk withinDevelopmentPlanArea = new Chunk(String.format(PARAGRAPH_1_11, tenantId), font1);
	

		Phrase paragraph2 = new Phrase();
		Phrase secondPhraseNew = new Phrase();
		Phrase secondPhraseNew2 = new Phrase();
		
		
		if(CollectionUtils.isEmpty(getPlotAndKhataDetails(additionalDetails))) {
			paragraph2.add(forServiceType);
			paragraph2.add(floorInform);
			paragraph2.add(storeyed);
			paragraph2.add(subOccupanc);
			paragraph2.add(buildingInRespectOf);
			paragraph2.add(pltoNumber);
			paragraph2.add(chunk7);
			paragraph2.add(khataNumber);
			paragraph2.add(village);
			paragraph2.add(locality);
			paragraph2.add(withinDevelopmentPlanArea);
			
		} else {
			//table
			secondPhraseNew.add(forServiceType);
			secondPhraseNew.add(floorInform);
			secondPhraseNew.add(storeyed);
			secondPhraseNew.add(subOccupanc);
			secondPhraseNew.add(buildingInRespectOf);
			secondPhraseNew2.add(village);
			secondPhraseNew2.add(locality);
			secondPhraseNew2.add(withinDevelopmentPlanArea);
			
		}

		Paragraph secondPara = new Paragraph(paragraph2);	
		Paragraph secondParaNew = new Paragraph(secondPhraseNew);
		Paragraph secondParaNew2 = new Paragraph(secondPhraseNew2);
		

		List list1 = new List(List.ORDERED, List.ALPHABETICAL);
		ListItem list1Item1 = new ListItem();
		Phrase landUsePhrase = new Phrase(String.format(PARAGRAPH_2_1, subOccupancy), font1);
		list1Item1.add(landUsePhrase);
		ListItem list1Item2 = new ListItem();
		Phrase developmentUndertakenPhrase = new Phrase(PARAGRAPH_2_2, font1);
		list1Item2.add(developmentUndertakenPhrase);
		ListItem list1Item3 = new ListItem();
		Chunk chunk15 = new Chunk(PARAGRAPH_2_3_1, font1);
		Chunk chunk16 = new Chunk(plan.getPlanInformation().getTotalParking() + " ", fontBold);
		Chunk chunk17 = new Chunk(PARAGRAPH_2_3_3, font1);
		Phrase chunk18 = new Phrase();
		chunk18.add(chunk15);
		chunk18.add(chunk16);
		chunk18.add(chunk17);
		list1Item3.add(chunk18);
		ListItem list1Item4 = new ListItem();
		BigDecimal roadWidth = plan.getPlanInformation().getTotalRoadWidth();
		if(roadWidth == null || roadWidth.compareTo(BigDecimal.ZERO)<=0) // for old scrutiny support
			roadWidth = plan.getPlanInformation().getRoadWidth();
		Phrase chunk19 = new Phrase(String.format(PARAGRAPH_2_4, roadWidth), font1);
		list1Item4.add(chunk19);
		ListItem list1Item5 = new ListItem();
		Phrase chunk20 = new Phrase(PARAGRAPH_2_5, font1);
		list1Item5.add(chunk20);
		ListItem list1Item6 = new ListItem();
		String fContent = String.format(PARAGRAPH_2_6, getGiftedArea(plan).toString(),ulbGradeNameAndUlb);
		Phrase chunk21 = new Phrase(fContent, font1);
		list1Item6.add(chunk21);
		ListItem list1Item7 = new ListItem();
		Chunk chunk22 = new Chunk(PARAGRAPH_2_7_1, font1);
		Chunk chunk23 = new Chunk(PARAGRAPH_2_7_2, fontBold);
		Chunk chunk24 = new Chunk(PARAGRAPH_2_7_3, font1);
		Phrase chunk25 = new Phrase();
		chunk25.add(chunk22);
		chunk25.add(chunk23);
		chunk25.add(chunk24);
		list1Item7.add(chunk25);
		ListItem list1Item8 = new ListItem();
		Phrase chunk26 = new Phrase(PARAGRAPH_2_8, font1);
		Phrase chunk26_sparit = new Phrase(PARAGRAPH_2_8_SPARIT, font1);
		Phrase chunk26_CRZ = new Phrase(PARAGRAPH_2_8_CRZ, font1);
		Phrase chunk26_CRZ_2 = new Phrase(PARAGRAPH_2_8_CRZ_2, font1);
		if (!ulb.isSparitFlag()) {
			if (isInCRZ) {
				list1Item8.add(chunk26_CRZ);
			} else {
				list1Item8.add(chunk26);
			}
		} else {
			list1Item8.add(chunk26_sparit);
		}
		
		ListItem list1Item8_2 = new ListItem();
		
		if(isInCRZ)
			list1Item8_2.add(chunk26_CRZ_2);
		
		ListItem list1Item9 = new ListItem();
		Phrase chunk27 = new Phrase(PARAGRAPH_2_9, font1);
		list1Item9.add(chunk27);
		ListItem list1Item10 = new ListItem();
		Phrase chunk28 = new Phrase(PARAGRAPH_2_10, font1);
		list1Item10.add(chunk28);
		ListItem otherConditionsItem = new ListItem();
		Phrase OtherConditionsPhrase = new Phrase(OTHER_CONDITIONS, font1);
		// dynamic Other conditions from additionalDetails of application--
		if (Objects.nonNull(additionalDetails)
				&& Objects.nonNull(additionalDetails.get("otherConditionsForPermitCertificate"))) {
			Chunk otherConditions = new Chunk(
					String.valueOf(additionalDetails.get("otherConditionsForPermitCertificate"))+"\n\n", font1);
			otherConditionsItem.add(OtherConditionsPhrase);
			otherConditionsItem.add(otherConditions);
		}
		ListItem list1Item11 = new ListItem();
		Chunk chunk29 = new Chunk(PARAGRAPH_2_11_1, font1);
		Chunk chunk30 = new Chunk(
				plan.getPlanInformation().getFloorInfo() + " " + plan.getPlanInformation().getSubOccupancy() + " ",
				fontBold);
		Chunk chunk31 = new Chunk(PARAGRAPH_2_11_3, font1);
		Phrase chunk32 = new Phrase();
		chunk32.add(chunk29);
		chunk32.add(chunk30);
		chunk32.add(chunk31);
		list1Item11.add(chunk32);
		List payments = new List(List.UNORDERED);
		
		if (isPreview) {
			// Call bpa estimate service (As the collection service cannot be called on
			// account of it being null at this stage.)
			java.util.List<Map<String, Object>> estimatedFeeDetails = getEstimatedSanctionFeeDetails(requestInfo,
					bpaApplication);

			for (Map<String, Object> taxHeadEstimate : estimatedFeeDetails) {

				String estimateAmount = String.valueOf(taxHeadEstimate.get("estimateAmount"));
				if (StringUtils.isNotEmpty(estimateAmount) && "0".equals(estimateAmount))
					continue;

				String taxHeadCode = String.valueOf(taxHeadEstimate.get("taxHeadCode"));
				String taxHeadName = "";

				if (taxHeadCode.equalsIgnoreCase(TAXHEAD_BPA_SANC_ADJUSTMENT_AMOUNT_CODE_1)
						|| taxHeadCode.equalsIgnoreCase(TAXHEAD_BPA_SANC_ADJUSTMENT_AMOUNT_CODE_2)
						|| taxHeadCode.equalsIgnoreCase(TAXHEAD_BPA_SANC_ADJUSTMENT_AMOUNT_CODE_3)) {

					java.util.List otherFees = (java.util.List) additionalDetails.get("otherFees");

					switch (taxHeadCode) {
					case TAXHEAD_BPA_SANC_ADJUSTMENT_AMOUNT_CODE_1:
						if (!otherFees.isEmpty())
							taxHeadName = (String) ((Map) otherFees.get(0)).get("reason");
						break;

					case TAXHEAD_BPA_SANC_ADJUSTMENT_AMOUNT_CODE_2:
						if (otherFees.size() > 1)
							taxHeadName = (String) ((Map) otherFees.get(1)).get("reason");
						break;

					case TAXHEAD_BPA_SANC_ADJUSTMENT_AMOUNT_CODE_3:
						if (otherFees.size() > 2)
							taxHeadName = (String) ((Map) otherFees.get(2)).get("reason");
						break;

					default:
						taxHeadName = getFeeComponentNameFromTaxHeadCode(taxHeadCode);
						break;

					}
				} else {
					taxHeadName = getFeeComponentNameFromTaxHeadCode(taxHeadCode);
				}

				
				ListItem individualPaymentSentence = new ListItem();
				Phrase paymentLine = new Phrase();
				Chunk paymentSentencePre = new Chunk(taxHeadName + " : Rs ", font1);
				Chunk paymentSentence = new Chunk(estimateAmount, font1);
				Chunk paymentSentencePost = new Chunk("/-Only", font1);
				Chunk paymentSentencePostPreview = new Chunk("  - Not Paid", font1);
				paymentLine.add(paymentSentencePre);
				paymentLine.add(paymentSentence);
				paymentLine.add(paymentSentencePost);
				paymentLine.add(paymentSentencePostPreview);

				individualPaymentSentence.add(paymentLine);
				payments.add(individualPaymentSentence);
			}
		} else {
		
		// call collection-services to fetch payment details
		java.util.List<Map<String,Object>> permitFeeBillAccountDetails = getPermitFeeBillAccountDetails(requestInfo, applicationNo, tenantIdActual);
		for(Map<String,Object> billAccountDetail:permitFeeBillAccountDetails) {
			String adjustedAmount = String.valueOf(billAccountDetail.get("adjustedAmount"));
			//skip those taxheadcodes for which adjustedAmount is 0-
			if(StringUtils.isNotEmpty(adjustedAmount) && "0.0".equals(adjustedAmount))
				continue;
			String taxHeadCode = String.valueOf(billAccountDetail.get("taxHeadCode"));
			String taxHeadName = "";

			if (taxHeadCode.equalsIgnoreCase(TAXHEAD_BPA_SANC_ADJUSTMENT_AMOUNT_CODE_1)
					|| taxHeadCode.equalsIgnoreCase(TAXHEAD_BPA_SANC_ADJUSTMENT_AMOUNT_CODE_2)
					|| taxHeadCode.equalsIgnoreCase(TAXHEAD_BPA_SANC_ADJUSTMENT_AMOUNT_CODE_3)) {

				java.util.List otherFees = (java.util.List) additionalDetails.get("otherFees");

				switch (taxHeadCode) {
				case TAXHEAD_BPA_SANC_ADJUSTMENT_AMOUNT_CODE_1:
					if (!otherFees.isEmpty())
						taxHeadName = (String) ((Map) otherFees.get(0)).get("reason");
					break;

				case TAXHEAD_BPA_SANC_ADJUSTMENT_AMOUNT_CODE_2:
					if (otherFees.size() > 1)
						taxHeadName = (String) ((Map) otherFees.get(1)).get("reason");
					break;

				case TAXHEAD_BPA_SANC_ADJUSTMENT_AMOUNT_CODE_3:
					if (otherFees.size() > 2)
						taxHeadName = (String) ((Map) otherFees.get(2)).get("reason");
					break;

				default:
					taxHeadName = getFeeComponentNameFromTaxHeadCode(taxHeadCode);
					break;

				}
			} else {
				taxHeadName = getFeeComponentNameFromTaxHeadCode(taxHeadCode);
			}			
			ListItem individualPaymentSentence = new ListItem();
			Phrase paymentLine = new Phrase();
			Chunk paymentSentencePre = new Chunk(taxHeadName+" : Rs ", font1);
			Chunk paymentSentence = new Chunk(adjustedAmount, font1);
			Chunk paymentSentencePost = new Chunk("/-Only", font1);
			paymentLine.add(paymentSentencePre);
			paymentLine.add(paymentSentence);
			paymentLine.add(paymentSentencePost);
			// add reason for adjustment/other fee-
			if (TAXHEAD_BPA_SANC_ADJUSTMENT_AMOUNT_CODE.equalsIgnoreCase(taxHeadCode)) {
				String modificationReasonSanctionFeeAdjustmentAmount = getValue(bpaApplication,
						"$.additionalDetails.modificationReasonSanctionFeeAdjustmentAmount");
				paymentLine.add(new Chunk(" (" + modificationReasonSanctionFeeAdjustmentAmount + ")", font1));
			}
			individualPaymentSentence.add(paymentLine);
			payments.add(individualPaymentSentence);
		}
		}
		list1Item11.add(payments);
		list1.add(list1Item1);
		list1.add(list1Item2);
		list1.add(list1Item3);
		list1.add(list1Item4);
		list1.add(list1Item5);
		list1.add(list1Item6);
		list1.add(list1Item7);
		list1.add(list1Item8);
		if(isInCRZ)
			list1.add(list1Item8_2);
		list1.add(list1Item9);
		list1.add(list1Item10);
		list1.add(otherConditionsItem);
		list1.add(list1Item11);

		BigDecimal plotAreaAsPerDeclaration = plan.getPlot().getArea();
		Chunk chunk35 = new Chunk(PARAGRAPH_3_1, font1);
		Chunk chunk36 = new Chunk(plotAreaAsPerDeclaration + SQM, fontBold);
		Paragraph chunk37 = new Paragraph();
		chunk37.add(chunk35);
		chunk37.add(chunk36);

		BigDecimal plotAreaAsPerPossession = plotAreaAsPerDeclaration;
		Chunk chunk38 = new Chunk(PARAGRAPH_4_1, font1);
		// TODO: to check which parameter in scrutiny gives occupied plot area
		Chunk chunk39 = new Chunk(plotAreaAsPerPossession + SQM, fontBold);
		Paragraph chunk40 = new Paragraph();
		chunk40.add(chunk38);
		chunk40.add(chunk39);

		Chunk chunk41 = new Chunk(PARAGRAPH_5_1, font1);
		Chunk chunk42 = new Chunk(plan.getVirtualBuilding() != null
				? plan.getVirtualBuilding().getTotalBuitUpArea().setScale(2, BigDecimal.ROUND_UP) + SQM
				: "0", fontBold);
		Paragraph chunk43 = new Paragraph();
		chunk43.add(chunk41);
		chunk43.add(chunk42);

		Chunk chunk44 = new Chunk(PARAGRAPH_6_1, font1);
		BigDecimal providedFar = BigDecimal.valueOf(plan.getFarDetails().getProvidedFar());
		Chunk chunk45 = new Chunk(
				plan.getVirtualBuilding().getTotalFloorArea().setScale(2, BigDecimal.ROUND_UP) + SQM,
				fontBold);
		Paragraph chunk46 = new Paragraph();
		chunk46.add(chunk44);
		chunk46.add(chunk45);

		Chunk chunk47 = new Chunk(PARAGRAPH_7_1, font1);
		Chunk chunk48 = new Chunk(plan.getFarDetails().getProvidedFar() + "", fontBold);
		Paragraph chunk49 = new Paragraph();
		chunk49.add(chunk47);
		chunk49.add(chunk48);
		
		PdfPTable table1 = new PdfPTable(6);
		table1.setLockedWidth(false);
		table1.setWidthPercentage(100f);
		addTableHeader1(table1);
		addRows1(table1, plan, additionalDetails);
				
		Paragraph cadTitle = new Paragraph();
		Phrase cadPhrase = new Phrase(CAD_STATEMENT, fontBold);
		Chunk bullet = new Chunk("\u2022"); // Unicode character for bullet point
        bullet.setFont(fontBold);
        cadTitle.add(bullet);
        cadTitle.add(Chunk.TABBING);
        cadTitle.add(cadPhrase);

		Paragraph nocTitle = new Paragraph(NOC_TITLE, fontBoldUnderlined);
		
		Paragraph onlineTitle = new Paragraph(ONLINE_TITLE, fontBold);
		Paragraph otherNocTitle = new Paragraph(OTHERNOC_TITLE, fontBold);

		// online NOC table
		PdfPTable onlineNOCtable = new PdfPTable(2);
		onlineNOCtable.setLockedWidth(false);
		float[] onlineNOCtableWidths = {60f, 40f};
		onlineNOCtable.setWidths(onlineNOCtableWidths);
		addTableHeaderForNOCTable(onlineNOCtable);
		addRowsForNocTable(onlineNOCtable, requestInfo, tenantIdActual, applicationNo, additionalDetails);

		Paragraph nocStaticPara = new Paragraph(NOC_PARA);
		Paragraph nocListPara = new Paragraph();
		List nocList1 = new List(List.ORDERED, List.NUMERICAL);

		java.util.List<String> nocList = getNocDetails(additionalDetails);

		if (!nocList.contains(NOCConstants.NOC_NA)) {
			for (String noc : nocList) {

				Phrase newNoc = new Phrase(nocMap.get(noc), font1);
				ListItem listItemNoc = new ListItem(newNoc);
				nocList1.add(listItemNoc);

			}
			nocListPara.add(nocList1);
		}

		// Date to be added on the left side.
		
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

			String approvedBy = getValue(bpaApplication, "$.dscDetails[0].approvedBy");

			//LOG.info("user (approvedBy):" + approvedBy);
			Map<String, Object> approverDetailsMap = getApproverDetails(requestInfo, tenantId, "BPA1", approvedBy,
					"EMPLOYEE");

			String approverName = !org.springframework.util.StringUtils
					.isEmpty(approverDetailsMap.get("nameOfApprover")) ? approverDetailsMap.get("nameOfApprover") + ""
							: "";
			//LOG.info("approverName: " + approverName);
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

			Image qrCode = getQrCode(ownersNamesCsv, getValue(bpaApplication, "approvalNo"), approvalDate,
					getValue(bpaApplication, "edcrNumber"));
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
			document.add(headerSubTitle2);
		}
		document.add(headerSubTitle3);
		
		if(dxfToPdfFlag && (plan.getIsDxfToPdfEnabled() != null && plan.getIsDxfToPdfEnabled())) {			
			String drawingRefId= plan.getPlanInformation().getDxfToPdfCorrelationId();
			Paragraph headerSubTitle4 = new Paragraph("Drawing Reference ID. " + drawingRefId, fontBoldUnderlined);
			headerSubTitle4.setAlignment(Paragraph.ALIGN_CENTER);
			document.add(headerSubTitle4);
		}
		
		document.add(Chunk.NEWLINE);
		document.add(Chunk.NEWLINE);		
		
		boolean isPlotAndKhataDetailsEmpty = CollectionUtils.isEmpty(getPlotAndKhataDetails(additionalDetails));

		if (!ulb.isSparitFlag()) {
		    if (isPlotAndKhataDetailsEmpty) {
		        document.add(paragraph1_old);
		    } else {
		        document.add(isInCRZ ? paragraph1_CRZ : paragraph1);
		    }
		} else {
		    if (isPlotAndKhataDetailsEmpty) {
		        document.add(paragraph1_Sparit_old);
		    } else {
		        document.add(paragraph1_Sparit);
		    }
		}
		
		document.add(Chunk.NEWLINE);

		if (CollectionUtils.isEmpty(getPlotAndKhataDetails(additionalDetails))) {
			document.add(Chunk.NEWLINE);
			document.add(addressed);
			document.add(applicants);
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
			addRowsForPlotTable(requestInfo, plotTable, additionalDetails, tenantIdActual);
			
			document.add(plotTable);
			document.add(Chunk.NEWLINE);
			document.add(Chunk.NEWLINE);
			document.add(secondParaNew);
			document.add(secondParaNew2);
		}

		document.add(Chunk.NEWLINE);
		document.add(list1);
		document.add(Chunk.NEWLINE);
		document.add(Chunk.NEWLINE);
		document.add(chunk37);
		document.add(chunk40);
		document.add(Chunk.NEWLINE);
		document.add(Chunk.NEWLINE);
		document.add(chunk43);
		document.add(chunk46);
		addProvidedAreasForAlteration(additionalDetails, plan, document);
		document.add(chunk49);
		document.add(Chunk.NEWLINE);
		document.add(Chunk.NEWLINE);
		document.add(table1);
		document.add(Chunk.NEWLINE);
		document.add(cadTitle);
		document.add(Chunk.NEWLINE);
		document.add(nocTitle);
		document.add(Chunk.NEWLINE);
		document.add(onlineTitle);
		document.add(Chunk.NEWLINE);	
		document.add(onlineNOCtable);
		document.add(Chunk.NEWLINE);
		document.add(otherNocTitle);
		document.add(Chunk.NEWLINE);
		if(!CollectionUtils.isEmpty(getNocDetails(additionalDetails))) {
			document.add(nocStaticPara);
		}
		
		document.add(Chunk.NEWLINE);
		document.add(nocListPara);
		document.add(Chunk.NEWLINE);
		document.add(Chunk.NEWLINE);
		document.add(dateParagraph);
		document.add(approvedByParagraph);		
		document.close();
		return new ByteArrayInputStream(outputBytes.toByteArray());
	}

	public void addRows1(PdfPTable table1, Plan plan, Map<String, Object> additionalDetails) {

		//TODO:This method is called from BPA5.java also.So handle specifically for subservice and alteration.
		String subService = null;
		if(Objects.nonNull(additionalDetails) && Objects.nonNull(additionalDetails.get(BPA_ADD_DETAILS_SERVICE_KEY))
				&& additionalDetails.get(BPA_ADD_DETAILS_SERVICE_KEY) instanceof Map
				&& Objects.nonNull(((Map) additionalDetails.get(BPA_ADD_DETAILS_SERVICE_KEY))
						.get(BPA_ADD_DETAILS_SUBSERVICE_KEY))) {
			subService = ((Map) additionalDetails.get(BPA_ADD_DETAILS_SERVICE_KEY)).get(BPA_ADD_DETAILS_SUBSERVICE_KEY)
					+ "";
		}
		java.util.List<DcrReportBlockDetail> blockDetails = buildBlockWiseProposedInfo(plan);		
		//buildBlockWiseProposedInfo method builds both existing and proposed info.

		int rowSpanHelper = 0;
		for (DcrReportBlockDetail block : blockDetails) {

			java.util.List<DcrReportFloorDetail> floorDetails = block.getDcrReportFloorDetails();
			for (DcrReportFloorDetail floor : floorDetails) {
				rowSpanHelper++;
				if (Objects.nonNull(subService) && !ALTERATION_SUBSERVICE_A.equals(subService)
						&& Objects.nonNull(getExistingFloorBuiltupArea(floor))) {
					// if existing builtup area information of that floor is present, we need to add
					// that row in table-
					rowSpanHelper++;
				}
			}
			rowSpanHelper++;
		}

		Font fontPara1 = FontFactory.getFont(FontFactory.HELVETICA, 12);
		PdfPCell cell1 = new PdfPCell();
		Phrase cellValue1 = new Phrase("Stilt (Parking)", fontPara1);
		cell1.addElement(cellValue1);
		// String stiltParkingProvided=plan.getpa
		Parking parking = new Parking();
		OdishaParkingHelper parkingData = parking.prepareParkingData(plan);
		BigDecimal stiltParking = parkingData.stiltParkingProvided;
		PdfPCell cell2 = new PdfPCell();
		Phrase cellValue2 = new Phrase(stiltParking + "", fontPara1);
		cell2.addElement(cellValue2);
		PdfPCell cell3 = new PdfPCell();
		
		Phrase cellValue3 = new Phrase("", fontPara1);
		int rowSpanParking = 2;
		//set rowspanhelper as rowspan has to be only 2 and then for each block-
		rowSpanHelper=0;
		cell3.setRowspan(rowSpanParking + rowSpanHelper);
		cell3.addElement(cellValue3);
		PdfPCell cell4 = new PdfPCell();
		Phrase cellValue4 = new Phrase("", fontPara1);
		cell4.setRowspan(rowSpanParking + rowSpanHelper);
		cell4.addElement(cellValue4);
		PdfPCell cell5 = new PdfPCell();
		Phrase cellValue5 = new Phrase("", fontPara1);
		cell5.setRowspan(rowSpanParking + rowSpanHelper);
		cell5.addElement(cellValue5);
		PdfPCell cell6 = new PdfPCell();
		Phrase cellValue6 = new Phrase("", fontPara1);
		cell6.setRowspan(rowSpanParking + rowSpanHelper);
		cell6.addElement(cellValue6);
		PdfPCell cell7 = new PdfPCell();
		Phrase cellValue7 = new Phrase("Stilt (Services)", fontPara1);
		BigDecimal totalAreaStiltFloor = OdishaUtill.getStiltArea(plan);
		cell7.addElement(cellValue7);
		PdfPCell cell8 = new PdfPCell();
		Phrase cellValue8 = new Phrase(totalAreaStiltFloor + "", fontPara1);
		cell8.addElement(cellValue8);

		table1.addCell(cell1);
		table1.addCell(cell2);
		table1.addCell(cell3);
		table1.addCell(cell4);
		table1.addCell(cell5);
		table1.addCell(cell6);
		table1.addCell(cell7);
		table1.addCell(cell8);

		for (DcrReportBlockDetail block : blockDetails) {
			PdfPCell blockNameCell = new PdfPCell();
			Phrase blockNamephrase = new Phrase("Block " + block.getBlockNo() + " Details", fontPara1);
			blockNameCell.setColspan(2);
			blockNameCell.addElement(blockNamephrase);
			table1.addCell(blockNameCell);
			
			//my code starts-
			Map<String, BigDecimal> setBackData = getSetBackData(plan,plan.getBlockByName(block.getBlockNo()));
			BigDecimal frontSetbackProvided = setBackData.get("frontSetbackProvided");
			BigDecimal rearSetbackProvided = setBackData.get("rearSetbackProvided");
			BigDecimal leftSetbackProvided = setBackData.get("leftSetbackProvided");
			BigDecimal rightSetbackProvided = setBackData.get("rightSetbackProvided");

			int noOfFloors=0;
			java.util.List<DcrReportFloorDetail> floorDetails = block.getDcrReportFloorDetails();
			for (DcrReportFloorDetail floor : floorDetails) {
				noOfFloors++;
				if (Objects.nonNull(subService) && !ALTERATION_SUBSERVICE_A.equals(subService)
						&& Objects.nonNull(getExistingFloorBuiltupArea(floor))) {
					// if existing builtup area information of that floor is present, we need to add
					// that row in table-
					noOfFloors++;
				}
			}
			// add one row for block no-
			noOfFloors++;
			PdfPCell frontSetBackCell = new PdfPCell();
			Phrase frontSetBackPhrase = new Phrase(frontSetbackProvided + "", fontPara1);
			frontSetBackCell.setRowspan(noOfFloors);
			frontSetBackCell.addElement(frontSetBackPhrase);
			PdfPCell rearSetBackCell = new PdfPCell();
			Phrase rearSetBackPhrase = new Phrase(rearSetbackProvided + "", fontPara1);
			rearSetBackCell.setRowspan(noOfFloors);
			rearSetBackCell.addElement(rearSetBackPhrase);
			PdfPCell leftSetBackCell = new PdfPCell();
			Phrase leftSetBackPhrase = new Phrase(leftSetbackProvided + "", fontPara1);
			leftSetBackCell.setRowspan(noOfFloors);
			leftSetBackCell.addElement(leftSetBackPhrase);
			PdfPCell rightSetBackCell = new PdfPCell();
			Phrase rightSetBackPhrase = new Phrase(rightSetbackProvided + "", fontPara1);
			rightSetBackCell.setRowspan(noOfFloors);
			rightSetBackCell.addElement(rightSetBackPhrase);
			table1.addCell(frontSetBackCell);
			table1.addCell(rearSetBackCell);
			table1.addCell(leftSetBackCell);
			table1.addCell(rightSetBackCell);
			// my code ends
			
			for (DcrReportFloorDetail floor : floorDetails) {
				PdfPCell floorNameCell = new PdfPCell();
				Phrase floorNamephrase = new Phrase(floor.getFloorNo(), fontPara1);
				floorNameCell.addElement(floorNamephrase);
				table1.addCell(floorNameCell);
				PdfPCell floorAreaCell = new PdfPCell();
				Phrase floorAreaphrase = new Phrase(floor.getBuiltUpArea() != null ? floor.getBuiltUpArea() + "" : "0" , fontPara1);
				//LOG.info("Built up area below without existing field in pdf" + floor.getBuiltUpArea());
				
				floorAreaCell.addElement(floorAreaphrase);
				table1.addCell(floorAreaCell);
				if (Objects.nonNull(subService) && !ALTERATION_SUBSERVICE_A.equals(subService)
						&& Objects.nonNull(getExistingFloorBuiltupArea(floor))) {					
					
					// add one row for existing area -
					PdfPCell floorNameExistingCell = new PdfPCell();
					Phrase floorNameExistingphrase = new Phrase(floor.getFloorNo() + " (Existing)", fontPara1);
					floorNameExistingCell.addElement(floorNameExistingphrase);
					table1.addCell(floorNameExistingCell);
					PdfPCell floorAreaExistingCell = new PdfPCell();
					Phrase floorAreaExistingPhrase = new Phrase(floor.getExistingBuiltUpArea() != null ? floor.getExistingBuiltUpArea() + "" : "0", fontPara1);
					//LOG.info("Built up area below with Existing field in pdf" + floor.getExistingBuiltUpArea());
					floorAreaExistingCell.addElement(floorAreaExistingPhrase);
					table1.addCell(floorAreaExistingCell);
				}
			}
		}

	}

	public void addTableHeader1(PdfPTable table1) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD);
		Stream.of("Category", "Area(sqmt)", "Front set back(mt)", "Rear set back(mt)", "Left setback(mt)",
				"Right setback(mt)").forEach(columnTitle -> {
					PdfPCell header = new PdfPCell();
					header.setBorderWidth(2);
					header.setVerticalAlignment(Element.ALIGN_MIDDLE);
					header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
					table1.addCell(header);

				});
	}

}
