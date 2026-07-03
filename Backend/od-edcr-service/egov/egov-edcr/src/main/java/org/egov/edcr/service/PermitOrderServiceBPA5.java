package org.egov.edcr.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang.StringUtils;
import org.egov.common.entity.edcr.Plan;
import org.egov.edcr.constants.DxfFileConstants;
import org.egov.edcr.constants.NOCConstants;
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
public class PermitOrderServiceBPA5 extends PermitOrderServiceBPA1 {
	public static String PERMITPRVIEW = "PERMIT LETTER PREVIEW";
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
		Document document = new Document();
		
		boolean isInCRZ = false;
		
		String crzNumber = plan.getPlanInformation().getCrzNumberForProjectsFallingUnderCrzArea();
		if (crzNumber != null && crzNumber.matches("\\d") && (Integer.parseInt(crzNumber) == 1 || Integer.parseInt(crzNumber) == 2
				|| Integer.parseInt(crzNumber) == 3)) {
			isInCRZ = true;
		}
		
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
		
		Paragraph paragraph1_sparit = new Paragraph(PARAGRAPH_ONE_SPARIT, font1);
		Paragraph paragraph1_sparit_old = new Paragraph(PARAGRAPH_ONE_SPARIT_OLD, font1);
		
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
		
		String plotNo = plan.getPlanInformation().getPlotNo() + " ";
		Chunk pltoNumber = new Chunk(plotNo, fontBold);
		Chunk chunk7 = new Chunk(PARAGRAPH_1_7, font1);
		String khataNo = plan.getPlanInformation().getKhataNo() + " ";
		Chunk khataNumber = new Chunk(khataNo, fontBold);

		Chunk village = new Chunk(PARAGRAPH_1_9, font1);
		Chunk locality = new Chunk(localityName + " ", fontBold);
		Chunk withinDevelopmentPlanArea = new Chunk(String.format(PARAGRAPH_1_11, tenantId), font1);

		Phrase paragraph2 = new Phrase();
		Phrase paragraph2_new = new Phrase();
		Phrase paragraph2_new_2 = new Phrase();
		
		if(CollectionUtils.isEmpty(getPlotAndKhataDetails(additionalDetails))) {
			
			paragraph2.add(forServiceType);
			paragraph2.add(floorInform);
			paragraph2.add(storeyed);
			paragraph2.add(subOccupanc);			
			Chunk buildingInRespectOf = new Chunk(PARAGRAPH_1_5_OLD, font1);
			paragraph2.add(buildingInRespectOf);
			paragraph2.add(pltoNumber);
			paragraph2.add(chunk7);
			paragraph2.add(khataNumber);
			paragraph2.add(village);
			paragraph2.add(locality);
			paragraph2.add(withinDevelopmentPlanArea);
			
		} else {
			Chunk buildingInRespectOf = new Chunk(PARAGRAPH_1_5, font1);
			paragraph2_new.add(forServiceType);
			paragraph2_new.add(floorInform);
			paragraph2_new.add(storeyed);
			paragraph2_new.add(subOccupanc);
			paragraph2_new.add(buildingInRespectOf);
			
			paragraph2_new_2.add(village);
			paragraph2_new_2.add(locality);
			paragraph2_new_2.add(withinDevelopmentPlanArea);
			
		}
		
		Paragraph secondPara = new Paragraph(paragraph2);
		Paragraph secondParaNew = new Paragraph(paragraph2_new);
		Paragraph secondParaNew2 = new Paragraph(paragraph2_new_2);

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
				list1Item8.add(chunk26_CRZ_2);
			} else {
				list1Item8.add(chunk26);
			}
		} else {
			list1Item8.add(chunk26_sparit);
		}
		
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
			java.util.List<Map<String, Object>> estimatedFeeDetails = getEstimatedSanctionFeeDetails(requestInfo, bpaApplication);

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
		
		//date section
		Phrase dateSection = new Phrase();
		Paragraph dateParagraph = new Paragraph();
		Paragraph approvedByParagraph = new Paragraph();
		
		if (!isPreview) {
		Chunk dateSectionPart1 = new Chunk("Date: ",font1);
		Chunk dateSectionPart2 = new Chunk(approvalDate ,font1);
		dateSection.add(dateSectionPart1);
		dateSection.add(dateSectionPart2);
		dateParagraph.add(dateSection);
		dateParagraph.setAlignment(Element.ALIGN_LEFT);
		
		// approved by -
		String approvedBy = getValue(bpaApplication, "$.dscDetails[0].approvedBy");
		
		//Assumption: accredited person is always a citizen
		
		Map<String, Object> approverDetails = getApproverDetails(requestInfo, tenantIdActual, "BPA2", approvedBy,
				"CITIZEN");
		String approverName = !org.springframework.util.StringUtils.isEmpty(approverDetails.get("nameOfApprover"))
				? approverDetails.get("nameOfApprover") + ""
				: "";
		String approverMobileNo = !org.springframework.util.StringUtils
				.isEmpty(approverDetails.get("mobileNumberOfApprover"))
						? approverDetails.get("mobileNumberOfApprover") + ""
						: "";
		Font fontParaApprovedBy = FontFactory.getFont(FontFactory.HELVETICA, 12);
		Phrase approvedBySection = new Phrase();
		Chunk approvedBySectionLine1 = new Chunk("Approved By ", fontParaApprovedBy);
		Chunk approvedBySectionLine2 = new Chunk(approverName + "\n", fontParaApprovedBy);
		Chunk approvedBySectionLine3 = new Chunk(approverMobileNo + "\n", fontParaApprovedBy);
		Chunk approvedBySectionLine4 = new Chunk("Accredited Person\n", fontParaApprovedBy);
		approvedBySection.add(approvedBySectionLine1);
		approvedBySection.add(approvedBySectionLine2);
		approvedBySection.add(approvedBySectionLine3);
		approvedBySection.add(approvedBySectionLine4);
		
		approvedByParagraph.add(approvedBySection);
		approvedByParagraph.setAlignment(Element.ALIGN_RIGHT);
		
		Image qrCode = getQrCode(ownersNamesCsv, getValue(bpaApplication, "approvalNo"), approvalDate, getValue(bpaApplication, "edcrNumber"));
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
		        document.add(paragraph1_sparit_old);
		    } else {
		        document.add(paragraph1_sparit);
		    }
		}
		
		document.add(Chunk.NEWLINE);

		if (CollectionUtils.isEmpty(getPlotAndKhataDetails(additionalDetails))) {
			document.add(Chunk.NEWLINE);
			document.add(addressed);
			document.add(applicants);
			document.add(Chunk.NEWLINE);
			document.add(address2);
			document.add(address3);
			
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
		document.add(Chunk.NEWLINE);
		document.add(dateParagraph);
		document.add(approvedByParagraph);
		
		document.close();
		return new ByteArrayInputStream(outputBytes.toByteArray());
	}

}