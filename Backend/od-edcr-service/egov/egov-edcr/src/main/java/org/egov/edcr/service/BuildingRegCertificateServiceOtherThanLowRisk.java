package org.egov.edcr.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
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
public class BuildingRegCertificateServiceOtherThanLowRisk extends BuildingRegCertificateService{
	
	private static final Logger LOG = Logger.getLogger(BuildingRegCertificateServiceLowRisk.class);

	public static String PARAGRAPH_ONE = "Permission under sub-section (3) of the Section-16 of the Odisha Development Authorities Act, 1982 (Act 14 of 1982) is hereby granted in favour of Land Owner";
	public static String PARAGRAPH_ONE_SPARIT = "Permission under sub section(3) of section 31/ sub section (1) of section 33 of Odisha Town Planning & Improvement Trust Act 1956 is hereby granted in favour of Land Owner";
	
	// public static String PARAGRAPH_ONE_OLD = "Permission under sub-section (3) of
	// the Section-16 of the Odisha Development Authorities Act, 1982 is hereby
	// granted in favour of";

	//public static String PARAGRAPH_ONE_SPARIT = "Permission/Licence under sub section(3) of section 31/ sub section (1) of section 33 of Odisha Town Planning & Improvement Trust Act 1956 is hereby granted in favour of Land Owner";
	//public static String PARAGRAPH_ONE_SPARIT_OLD = "Permission/Licence under sub section(3) of section 31/ sub section (1) of section 33 of Odisha Town Planning & Improvement Trust Act 1956 is hereby granted in favour of";

	public static String ADDRESS = "LAXMIPRIYA PANDA AND OTHERS,";
	public static String ADDRESS2 = "KALARAHANGA";
	public static String ADDRESS1 = "INJANA,";
	public static String PERMITPRVIEW = "Building Regularization Certificate Preview";
	public static String PARAGRAPH_1_1 = "%1$s of a %2$s storeyed building within the Development Plan Area of %3$s subject to following parameter and conditions/restrictions.";


	public static String PARAGRAPH_1_5_OLD = "Building in respect of plot No.";

	public static String PARAGRAPH_1_5 = "Building in respect of";
//	public static String PARAGRAPH_1_6 = "1337, ";
	public static String PARAGRAPH_1_7 = "Khata No.";
//	public static String PARAGRAPH_1_8 = "180 ";
	public static String PARAGRAPH_1_9 = "Village/Mouza. ";
//	public static String PARAGRAPH_1_10 = "Injana ";
	public static String PARAGRAPH_1_11 = "of %s within the Development Plan Area subject to following conditions/ restrictions: ";

	public static String PARAGRAPH_2_1 = "The Building shall be used exclusively for %s purpose and the uses shall not be changed to any other use without prior approval of this Authority.\n\n";
	public static String PARAGRAPH_2_2 = "The development shall be undertaken strictly according to plans enclosed with necessary permission endorsement.\n\n";
	public static String PARAGRAPH_2_3_1 = "Parking space measuring ";
	public static String PARAGRAPH_2_3_3 = "sq. mtr. as shown in the approved plan shall be exclusively used for parking and no part of it will be used for any other purpose.\n\n";
	public static String PARAGRAPH_2_4 = "The land over which construction is proposed is accessible by an approved means of access of %s mtr. width.\n\n";
	public static String PARAGRAPH_2_5 = "The land in question must be in lawful ownership and peaceful possession of the applicant.\n\n";
	public static String PARAGRAPH_2_6 = "The applicant shall free gift %s sq.mtr. of located in the %s for the widening of the road/construction of new roads and other public amenities prior to completion of the development as indicated in the plan.\n\n";
	
	public static String PARAGRAPH_2_7_1 = "The permission granted under these regulations shall remain valid upto ";
	public static String PARAGRAPH_2_7_2 = "three years ";
	public static String PARAGRAPH_2_7_3 = "from the date of issue.However the permission shall have to be revalidated before the expiry of the "
			+ "above period on payment of such fee as may be prescribed under rules and such revalidation "
			+ "shall be valid for one year.\n\n";
	
	
	public static String PARAGRAPH_2_8_A = "Approval of plans and acceptance of any statement or document pertaining to such plan "
			+ "shall not exempt the owner or person or persons under whose supervision the building is "
			+ "constructed from their responsibilities imposed under ODA (Planning & Building Standards) "
			+ "Rules 2020, or under any other law for the time being in force.\n\n";
	public static String PARAGRAPH_2_8_B = "Approval of plan would mean granting of permission to construct under these regulations in\r\n"
			+ "force only and shall not mean among other things";
	public static String PARAGRAPH_2_8_B_1 = "The title over the land or building\n\n";
	public static String PARAGRAPH_2_8_B_2 = "Easement rights\n\n";
	public static String PARAGRAPH_2_8_B_3 = "Variation in area from recorded area of a plot or a building\n\n";
	public static String PARAGRAPH_2_8_B_4 = "Structural stability\n\n";
	public static String PARAGRAPH_2_8_B_5 = "Workmanship and soundness of materials used in the construction of the buildings\n\n";
	public static String PARAGRAPH_2_8_B_6 = "Quality of building services and amenities in the construction of the building";
	public static String PARAGRAPH_2_8_B_7 = "The site/area liable to flooding as a result of not taking proper drainage arrangement as "
			+ "per the natural lay of the land, etc\n\n";
	public static String PARAGRAPH_2_8_B_8 = "Other requirements or licenses or clearances required to be obtained for the site "
			+ "/premises or activity under various other laws.\n\n";
	

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

	
	public static String PARAGRAPH_THREE_I = "The building shall be used exclusively for ";
	public static String PARAGRAPH_THREE_II = "MultistoriedResidential Apartment with Community building ";
	public static String PARAGRAPH_THREE_III = " purpose and the use shall not be changed to any other use without prior approval of this Authority.\n\n";

	public static String PARAGRAPH_FOUR = "The development shall be undertaken strictly according to plans enclosed with necessary permission endorsement.\n\n";

	public static String PARAGRAPH_FIVE_I = "Total Parking space measuring ";
	public static String PARAGRAPH_FIVE_II = "6798.03Sqm (in Basement/ Ground and Open) ";
	public static String PARAGRAPH_FIVE_III = " as shown in the approved plan shall be left for parking of vehicles and no part of it will be used for any other purpose.\n\n";

	public static String PARAGRAPH_SIX_I = "The land over which construction is proposed is accessible by an approved means of access of ";
	public static String PARAGRAPH_SIX_II = "%s Mtr. ";
	public static String PARAGRAPH_SIX_III = "in width.\n\n";

	public static String PARAGRAPH_SEVEN = "The land in question must be in lawful ownership and peaceful possession of the applicant.\n\n";

	public static String PARAGRAPH_EIGHT_I = "The applicant shall free gift ";
	public static String PARAGRAPH_EIGHT_II = "143.41sqm ";
	public static String PARAGRAPH_EIGHT_III = " wide strip of land to %s for further widening of the road to the standard width as per ";
	public static String PARAGRAPH_EIGHT_IV = "CDP-2010, BDA.\n\n";
	
	public static String PARAGRAPH_EIGHT_III_SPARIT = " wide strip of land to %s for further widening of the road to the standard width if less than 20 ft or provision as per master plan.";

	public static String PARAGRAPH_NINE_I = "The permission granted under these regulations shall remain valid upto ";
	public static String PARAGRAPH_NINE_II = "three years ";
	public static String PARAGRAPH_NINE_III = "from the date of issue.However the permission shall have to be revalidated before the expiry of the above period on payment of such fee as may be prescribed under rules and such revalidation shall be valid for one year.\n\n";

	public static String PARAGRAPH_TEN_I = "Approval of plans and acceptance of any statement or document pertaining to such plan shall not exempt the owner or person or persons under whose supervision the building is constructed from their responsibilities imposed under ODA (Planning & Building Standards) Rules 2020, or under any other law for the time being in force.\n\n";
	public static String PARAGRAPH_TEN_I_SPARIT = "Approval of plans and acceptance of any statement or document pertaining to such plan shall not exempt the owner or person or persons under whose supervision the building is constructed from their responsibilities imposed under OTP&IT (Planning & Building Standards) Rules 2021, or under any other law for the time being in force.\n\n";
	
	public static String PARAGRAPH_TEN_II = "Approval of plan would mean granting of permission to construct under these regulations in force only and shall not mean among other things-\n\n";
	public static String PARAGRAPH_TEN_II_A = "The title over the land or building\n\n";
	public static String PARAGRAPH_TEN_II_B = "Easement rights\n\n";
	public static String PARAGRAPH_TEN_II_C = "Variation in area from recorded area of a plot or a building\n\n";
	public static String PARAGRAPH_TEN_II_D = "Structural stability\n\n";
	public static String PARAGRAPH_TEN_II_E = "Workmanship and soundness of materials used in the construction of the buildings\n\n";
	public static String PARAGRAPH_TEN_II_F = "Quality of building services and amenities in the construction of the building,\n\n";
	public static String PARAGRAPH_TEN_II_G = "The site/area liable to flooding as a result of not taking proper drainage arrangement as per the natural lay of the land, etc and\n\n";
	public static String PARAGRAPH_TEN_II_H = "Other requirements or licenses or clearances required to be obtained for the site /premises or activity under various other laws.\n\n";

	public static String PARAGRAPH_ELEVEN = "In case of any dispute arising out of land record or in respect of right, title, interest after this permission is granted, the permission so granted shall be treated as automatically cancelled during the period of dispute.\n\n";

	public static String PARAGRAPH_TWELEVE = "Neither granting of the permit nor the approval of the drawing and specifications, nor inspections made by the Authority during erection of the building shall in any way relieve the owner of such building from full responsibility for carrying out the work in accordance with the requirements of NBC 2005 and these regulations.\n\n";

	public static String PARAGRAPH_THIRTEEN_I = "The owner /applicant shall:\n\n";
	public static String PARAGRAPH_THIRTEEN_I_A = "Permit the Authority to enter the building or premises, for which the permission has been granted at any reasonable time for the purpose of enforcing the regulations;\n\n";
	public static String PARAGRAPH_THIRTEEN_I_B = "Obtain, wherever applicable, from the competent Authority permissions/clearance required in connection with the proposed work;\n\n";
	public static String PARAGRAPH_THIRTEEN_I_C = "Give written notice to the Authority before commencement of work on building site in Form-V,periodic progress report in Form-VIII, notice of completion in Form-VI and notice in case of termination of services of Technical persons engaged by him.\n\n";
	public static String PARAGRAPH_THIRTEEN_I_D = "Obtain an Occupancy Certificate from the Authority prior to occupation of building in full or part.\n\n";

	public static String PARAGRAPH_FOURTEEN = "The applicant shall abide by the provisions of Rule no.15 of ODA (P&BS) Rules, 2020 with regard to third party verification at plinth level, ground level & roof level. Any deviation to the above shall attract penalty as per the provision of the same.\n\n";
	public static String PARAGRAPH_FOURTEEN_SPARIT = "The applicant shall abide by the provisions of Rule no.84 of OTP&IT Rules 2021 with regard to third party verification at plinth level, ground level & roof level. Any deviation to the above shall attract penalty as per the provision of the same.\n\n";

	public static String PARAGRAPH_FIFTEEN_A = "In case the full plot or part thereof on which permission is accorded is agricultural kisam, the same may be converted to non-agricultural kisam under section- 8 of OLR Act before commencement of construction.\n\n";
	public static String PARAGRAPH_FIFTEEN_B = "The owner/applicant shall get the structural plan and design vetted by the institutions identified by the Authority for buildings more than 30 mtr height before commencement of construction.\n\n";

	public static String PARAGRAPH_SIXTEEN = "Wherever tests of any material are made to ensure conformity of the requirements of the regulations in force,records of the test data shall be kept available for inspection during the construction of building and for such period thereafter as required by the Authority.\n\n";

	public static String PARAGRAPH_SEVENTEEN_I = "The persons to whom a permit is issued during construction shall keep pasted in a conspicuous place on the property in respect of which the permit was issued;\n\n";
	public static String PARAGRAPH_SEVENTEEN_I_A = "A copy of the building permit; and\n\n";
	public static String PARAGRAPH_SEVENTEEN_I_B = "A copy of approved drawings and specifications.\n\n";

	public static String PARAGRAPH_EIGHTEEN_I = "If the Authority finds at any stage that the construction is not being carried on according to the sanctioned plan or is in violations of any of the provisions of these regulations, it shall notify the owner and no further construction shall beallowed until necessary corrections in the plan are made and the corrected plan is approved. ";
	public static String PARAGRAPH_EIGHTEEN_II = "The applicant during the course of construction and till issue of occupancy certificate shall place a display board on his site with details and declaration.\n\n";

	public static String PARAGRAPH_NINETEEN = "This permission is accorded on deposit /submission of the following:\n\n";
	public static String PARAGRAPH_NINETEEN_II = "If not paid within such time as mentioned above, then interest rate of SBI PLR shall be Imposed and occupancy certificate shall not be issued without realizing the total amount including interest.";

	public static String PARAGRAPH_TWENTY_I = "Other conditions to be complied by the applicant are as per the following;\n\n";
	public static String PARAGRAPH_TWENTY_I_A = "The owner/applicant/Technical Person shall strictly adhere to the terms and conditions imposed in the NOC/Clearances given by Fire Prevention officer/National Airport Authority/SEIAA, Ministry of Forest & Environment/PHED etc wherever applicable.";
	public static String PARAGRAPH_TWENTY_I_B = "Storm water from the premises of roof top shall be conveyed and discharged to the rain water recharging pits as per Rule 47 of ODA (Planning & Building Standards) Rules 2020.";
	public static String PARAGRAPH_TWENTY_I_C = "The space which is meant for parking shall not be changed to any other use and shall not be partitioned/ closed in any manner.";
	public static String PARAGRAPH_TWENTY_I_D = "30% of the parking space in group housing/apartment building shall be exclusively earmarked for ambulance, fire tender, physically handicapped persons andoutside visitors withsignage as per norms under Rule 37 of ODA (Planning & Building Standards) Rules 2020.";
	public static String PARAGRAPH_TWENTY_I_E = "Plantation for one tree per 80 sqm of plot area made by the applicant as per provision under Rule 30 of ODA (Planning & Building Standards) Rules 2020.";
	public static String PARAGRAPH_TWENTY_I_F = "If the construction / development are not as per the approved plan / deviated beyond permissible norms, the performance security shall be forfeited and action shall be initiated against the applicant/builder / developer as per the provisions of the ODA Act, 1982 Rules and Regulations made there under";
	public static String PARAGRAPH_TWENTY_I_G = "The Owner/ Applicant/Architect/Structural Engineer are fully and jointly responsible for any structural failure of building due to any structural/construction defects, Authority will be no way be held responsible for the same in what so ever manner.";
	public static String PARAGRAPH_TWENTY_I_H = "The concerned Architect / Applicant / Developer are fully responsible for any deviations additions & alternations beyond approved plan/ defective construction etc.shall be liable for action as per the provisions of the Regulation.";
	public static String PARAGRAPH_TWENTY_I_I = "The applicant shall obtain infrastructural specification and subsequent clearance with regard to development of infrastructure from BMC/BDA before commencement of construction.The applicant shall obtain infrastructural specification and subsequent clearance with regard to development of infrastructure from BMC/BDA before commencement of construction.";
	public static String PARAGRAPH_TWENTY_I_J = "All the stipulated conditions of the NOC/Clearances given by CE-Cum- Engineer Member, BDA& PHED shall be adhered to strictly. All the fire fighting installation etc are to be ensured and maintained by the applicant as per NBC 2016.";
	public static String PARAGRAPH_TWENTY_I_K = "No storm water/water shall be discharged to the public road/public premises and other adjoining plots.";
	public static String PARAGRAPH_TWENTY_I_L = "The applicant shall abide by the terms and conditions of the NOC given by CGWA, Airport Authority, SEIAA and Fire Safety Recommendations, EIDP vetting by CE-cum-EM, BDA as well as structural vetting.";
	public static String PARAGRAPH_TWENTY_I_M = "Adhere to the provisions of BDA (Planning & Building Standards) Regulation strictly and conditions thereto.";
	public static String PARAGRAPH_TWENTY_I_N = "All the passages around the building shall be developed with permeable pavers block for absorption of rain water and seepage in to the ground.";
	public static String PARAGRAPH_TWENTY_I_O = "Rain water harvesting structure and recharging pits of adequate capacity shall be developed to minimize the storm water runoff to the drain.";
	public static String PARAGRAPH_TWENTY_I_P = "The applicant shall make own arrangement of solid waste management through micro compost plant within the project premises";
	public static String PARAGRAPH_TWENTY_I_Q = "The applicant shall register this project before the ORERA as per affidavit submitted before commencement of work.";
	public static String PARAGRAPH_TWENTY_I_R = "The applicant shall install Rooftop P.V. system as per BDA Regulations.";
	public static String PARAGRAPH_TWENTY_I_S = "The applicant shall free gift the road affected area to Government/BDA as and when required by the government for development of road.";
	public static String PARAGRAPH_TWENTY_I_T = "The Authority shall in no way be held responsible for any structural failure and damage due to earthquake/cyclone/any other natural disaster.";
	public static String PARAGRAPH_TWENTY_I_U = "The number of dwelling units so approved shall not be changed in any manner.";
	public static String PARAGRAPH_TWENTY_I_V = "Lift shall be provided as per the provision of NBCI, 2016 in pursuance with note(ii) of sub-rule (2) of Rule 42 of ODA Rules, 2020. If the same isn’t provided by the applicant, appropriate action shall be taken as per law.";

	public Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA, 16, Font.BOLD);
	public Font font1 = FontFactory.getFont(FontFactory.HELVETICA, 12);
	public Font fontBold = FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD);
	public Font fontBoldUnderlined = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Font.UNDERLINE);

	@Override
	public InputStream generateReport(LinkedHashMap regularizations, RequestInfo requestInfo,
			Boolean isPreview) {
		try {
			return createPdf(regularizations, requestInfo, isPreview);
		} catch (Exception e) {
			e.printStackTrace();
			throw new ApplicationRuntimeException("Error while generating permit order pdf", e);
		}
	}

	public InputStream createPdf(LinkedHashMap regularizations, RequestInfo requestInfo, Boolean isPreview)
			throws Exception {


		LinkedHashMap<String, Object> buildingRegularizationInfo = (LinkedHashMap<String, Object>)regularizations.get("buildingRegularizationInfo");
		LinkedHashMap<String, Object> landRegularizationInfo = (LinkedHashMap<String, Object>)regularizations.get("landRegularizationInfo");
		java.util.List<Map<String, Object>> plotInfoList =  getPlotAndKhataDetails(regularizations);
		java.util.List<Map<String, Object>> buildingBlocksInfoList =  getBuildingBlockDetails(regularizations);
		LinkedHashMap<String, Object> buildingRegularizationAdditionalDetails = (LinkedHashMap<String, Object>) buildingRegularizationInfo
				.get("additionalDetails");
		LinkedHashMap<String, Object> additionalDetails = (LinkedHashMap<String, Object>) regularizations
				.get("additionalDetails");
		String tenantIdActual = regularizations.get("tenantId").toString();
		OdishaUlbs ulb = OdishaUlbs.getUlb(regularizations.get("tenantId").toString());
		BigDecimal totalParkingArea = BigDecimal.valueOf(Double.parseDouble(buildingRegularizationAdditionalDetails.get("parkingTotal").toString()));


		Document document = new Document();
		ByteArrayOutputStream outputBytes;
		outputBytes = new ByteArrayOutputStream();
		PdfWriter.getInstance(document, outputBytes);
		document.open();
		Image logo = getLogo();
		String[] ulbGradeNameAndUlbArray = getUlbNameAndGradeFromMdms(requestInfo, tenantIdActual);
		String ulbGradeNameAndUlb = (ulbGradeNameAndUlbArray[0] + " " + ulbGradeNameAndUlbArray[1]);

		//Map<String, Object> additionalDetails = getAdditionalDetailsMap(regularizations);
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
		
		Paragraph paragraph1_Sparit = new Paragraph(PARAGRAPH_ONE_SPARIT, font1);

		Paragraph addressed = new Paragraph("Smt. /Shri,", fontBold);

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

		PdfPTable buildingHeightAndParkingtable = getBuildingHeightAndParkingtable(buildingRegularizationAdditionalDetails, buildingBlocksInfoList, totalParkingArea);
		
		PdfPTable setbackTable = getSetbackTable(buildingBlocksInfoList);


		
		Chunk paragraph2Chunk = new Chunk(String.format(PARAGRAPH_1_1,getServiceType(regularizations), buildingRegularizationAdditionalDetails.get("noOfStoreys"),tenantId),font1);

		java.util.List<Map<String,Object>> floors= (java.util.List<Map<String, Object>>)buildingBlocksInfoList.get(0).get("floors");
		String subOccupancy = String.join(",", subOccupancies);
		Chunk subOccupanc = new Chunk(subOccupancy, fontBold);


		

		Phrase paragraph2 = new Phrase();
		Phrase secondPhraseNew = new Phrase();
		Phrase secondPhraseNew2 = new Phrase();

		if (CollectionUtils.isEmpty(getPlotAndKhataDetails(regularizations))) {
			paragraph2.add(paragraph2Chunk);

		} else {
			secondPhraseNew.add(paragraph2Chunk);
		}

		Paragraph secondPara = new Paragraph(paragraph2);
		Paragraph secondParaNew = new Paragraph(secondPhraseNew);
		Paragraph secondParaNew2 = new Paragraph(secondPhraseNew2);

		List conditionPointsList = getConditionPointListData(regularizations, requestInfo, isPreview, ulb, ulbGradeNameAndUlb, tenantIdActual,
				applicationNo, subOccupancy,totalParkingArea);
		
		PdfPTable paymentTable = new PdfPTable(3);
        paymentTable.setLockedWidth(false);
        float[] paymentTableWidths = { 15f, 60f, 25f };
        paymentTable.setWidths(paymentTableWidths);
        addTableHeaderPaymentTable(paymentTable);
        if (!isPreview) {
            addRowsForPaymentsTable(requestInfo, paymentTable, tenantIdActual, applicationNo);
        } else {
            addRowsForEstimatedFees(requestInfo, paymentTable, regularizations);
        }

        ListItem paymentTableItem = new ListItem();
        //paymentTableItem.add(paymentItem);
        paymentTableItem.add(paymentTable);
        paymentTableItem.setSpacingAfter(10);
        
        conditionPointsList.add(paymentTableItem);

		BigDecimal plotAreaAsPerDeclaration =BigDecimal.valueOf(Double.parseDouble(landRegularizationInfo.get("totalPlotArea").toString()));
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

		String otherConditionsParaString = getValue(additionalDetails, "$.approverNote").toString();
		Paragraph otherConditionsPara=null,otherConditionsParaCondition=null;
		
		if(StringUtils.isNotEmpty(otherConditionsParaString)) {
			otherConditionsPara=new Paragraph(OTHER_CONDITIONS, fontBoldUnderlined);
			otherConditionsPara.setAlignment(Paragraph.ALIGN_LEFT);
			otherConditionsParaCondition= new Paragraph(otherConditionsParaString,font1);
		}
		

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

			//("user (approvedBy):" + approvedBy);
			Map<String, Object> approverDetailsMap = getApproverDetails(requestInfo, tenantId,regularizations.get("businessService").toString(), approvedBy,
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

			Image qrCode = getQrCode(applicationNo,approvalNo,approvalDate);
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

		
		if (!ulb.isSparitFlag()) {
			document.add(paragraph1);
		} else {
			document.add(paragraph1_Sparit);
		}
		

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
		document.add(conditionPointsList);
		document.add(Chunk.NEWLINE);
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

	private List getConditionPointListData(LinkedHashMap regularizations, RequestInfo requestInfo, Boolean isPreview, OdishaUlbs ulb,
			String ulbGradeNameAndUlb, String tenantIdActual, String applicationNo, String subOccupancy, BigDecimal totalParkingArea) {
		
		LinkedHashMap<String, Object> landRegularizationInfo = (LinkedHashMap<String, Object>)regularizations.get("landRegularizationInfo");
		java.util.List<Map<String, Object>> plotInfo =  getPlotAndKhataDetails(regularizations);
		
		Font fontPara1 = FontFactory.getFont(FontFactory.HELVETICA, 12);
		Font fontPara1Bold = FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD);
		
		List topLevel = new List(List.ORDERED);
		ListItem item1 = new ListItem();
		Phrase list4 = new Phrase();
		Chunk chunk51 = new Chunk(PARAGRAPH_THREE_I, fontPara1);
		Chunk chunk52 = new Chunk(subOccupancy, fontPara1Bold);
		Chunk chunk53 = new Chunk(PARAGRAPH_THREE_III, fontPara1);
		list4.add(chunk51);
		list4.add(chunk52);
		list4.add(chunk53);
		item1.add(list4);
		ListItem item2 = new ListItem();
		Phrase list5 = new Phrase();
		Chunk chunk54 = new Chunk(PARAGRAPH_FOUR, fontPara1);
		list5.add(chunk54);
		item2.add(list5);
		ListItem item3 = new ListItem();
		Phrase list6 = new Phrase();
		Chunk chunk55 = new Chunk(PARAGRAPH_FIVE_I, fontPara1);
		Chunk chunk56 = new Chunk( totalParkingArea+" Sqm. ", fontPara1Bold);
		Chunk chunk57 = new Chunk(PARAGRAPH_FIVE_III, fontPara1);
		list6.add(chunk55);
		list6.add(chunk56);
		list6.add(chunk57);
		item3.add(list6);
		ListItem item4 = new ListItem();
		Phrase list7 = new Phrase();
		Chunk chunk58 = new Chunk(PARAGRAPH_SIX_I, fontPara1);
		BigDecimal roadWidth = BigDecimal.valueOf(Double.parseDouble(landRegularizationInfo.get("accessRoadWidth").toString()));
		Chunk chunk59 = new Chunk(String.format(PARAGRAPH_SIX_II, roadWidth + ""), fontPara1Bold);
		Chunk chunk60 = new Chunk(PARAGRAPH_SIX_III, fontPara1);
		list7.add(chunk58);
		list7.add(chunk59);
		list7.add(chunk60);
		item4.add(list7);
		ListItem item5 = new ListItem();
		Phrase list8 = new Phrase();
		Chunk chunk61 = new Chunk(PARAGRAPH_SEVEN, fontPara1);
		list8.add(chunk61);
		item5.add(list8);
		ListItem item6 = new ListItem();
		Phrase list9 = new Phrase();
		Chunk chunk62 = new Chunk(PARAGRAPH_EIGHT_I, fontPara1);
//		String roadAffectedAreas = getRoadAffectedArea(plan);
//		String roadAffectedArea = roadAffectedAreas.equalsIgnoreCase("0.00") ? totalRoadSurrenderArea+""
//				: roadAffectedAreas;
		Chunk chunk63 = new Chunk( getGiftedArea(plotInfo).toString()+" Sqm. ", fontPara1Bold);
		Chunk chunk64 = new Chunk(String.format(PARAGRAPH_EIGHT_III, ulbGradeNameAndUlb), fontPara1);
		Chunk chunk64_sparit = new Chunk(String.format(PARAGRAPH_EIGHT_III_SPARIT, ulbGradeNameAndUlb), fontPara1);
		Chunk chunk65 = new Chunk(PARAGRAPH_EIGHT_IV, fontPara1Bold);
		list9.add(chunk62);
		list9.add(chunk63);
		if (!ulb.isSparitFlag()) {
			list9.add(chunk64);
			list9.add(chunk65);
		} else {
			list9.add(chunk64_sparit);
		}
		list9.add(Chunk.NEWLINE);
		item6.add(list9);
		ListItem item7 = new ListItem();
		Phrase list10 = new Phrase();
		Chunk chunk66 = new Chunk(PARAGRAPH_NINE_I, fontPara1);
		Chunk chunk67 = new Chunk(PARAGRAPH_NINE_II, fontPara1Bold);
		Chunk chunk68 = new Chunk(PARAGRAPH_NINE_III, fontPara1);
		list10.add(chunk66);
		list10.add(chunk67);
		list10.add(chunk68);
		item7.add(list10);
		ListItem item8 = new ListItem();
		Phrase list111 = new Phrase();
		Chunk chunk69 = new Chunk(" ", fontPara1);
		list111.add(chunk69);
		item8.add(list111);
		List secondLevel = new List(List.ORDERED, List.ALPHABETICAL);
		secondLevel.setPreSymbol("(");
		secondLevel.setPostSymbol(") ");
		ListItem item9 = new ListItem();
		Phrase chunk70 = new Phrase(PARAGRAPH_TEN_I, fontPara1);
		
		Phrase chunk70_sparit = new Phrase(PARAGRAPH_TEN_I_SPARIT, fontPara1);
		if (!ulb.isSparitFlag()) {
			item9.add(chunk70);
		} else {
			item9.add(chunk70_sparit);
		}
		
		Phrase chunk71 = new Phrase(PARAGRAPH_TEN_II, fontPara1);
		ListItem item10 = new ListItem();
		item10.add(chunk71);
		List ThirdLevel = new List(List.ORDERED, List.NUMERICAL);
		Phrase chunk72 = new Phrase(PARAGRAPH_TEN_II_A, fontPara1);
		ListItem item11 = new ListItem();
		item11.add(chunk72);
		Phrase chunk73 = new Phrase(PARAGRAPH_TEN_II_B, fontPara1);
		ListItem item12 = new ListItem();
		item12.add(chunk73);
		Phrase chunk74 = new Phrase(PARAGRAPH_TEN_II_C, fontPara1);
		ListItem item13 = new ListItem();
		item13.add(chunk74);
		Phrase chunk75 = new Phrase(PARAGRAPH_TEN_II_D, fontPara1);
		ListItem item14 = new ListItem();
		item14.add(chunk75);
		Phrase chunk76 = new Phrase(PARAGRAPH_TEN_II_E, fontPara1);
		ListItem item15 = new ListItem();
		item15.add(chunk76);
		Phrase chunk77 = new Phrase(PARAGRAPH_TEN_II_F, fontPara1);
		ListItem item16 = new ListItem();
		item16.add(chunk77);
		Phrase chunk78 = new Phrase(PARAGRAPH_TEN_II_G, fontPara1);
		ListItem item17 = new ListItem();
		item17.add(chunk78);
		Phrase chunk79 = new Phrase(PARAGRAPH_TEN_II_H, fontPara1);
		ListItem item18 = new ListItem();
		item18.add(chunk79);
		ThirdLevel.add(item11);
		ThirdLevel.add(item12);
		ThirdLevel.add(item13);
		ThirdLevel.add(item14);
		ThirdLevel.add(item15);
		ThirdLevel.add(item16);
		ThirdLevel.add(item17);
		ThirdLevel.add(item18);
		item10.add(ThirdLevel);
		secondLevel.add(item9);
		secondLevel.add(item10);
		item8.add(secondLevel);
		ListItem item19 = new ListItem();
		Phrase list112 = new Phrase();
		Chunk chunk80 = new Chunk(PARAGRAPH_ELEVEN, fontPara1);
		list112.add(chunk80);
		item19.add(list112);
		ListItem item20 = new ListItem();
		Phrase list113 = new Phrase();
		Chunk chunk81 = new Chunk(PARAGRAPH_TWELEVE, fontPara1);
		list113.add(chunk81);
		item20.add(list113);
		ListItem item21 = new ListItem();
		Phrase list114 = new Phrase();
		Chunk chunk82 = new Chunk(PARAGRAPH_THIRTEEN_I, fontPara1Bold);
		list114.add(chunk82);
		item21.add(list114);
		List secondLevel1 = new List(List.ORDERED, List.ALPHABETICAL);
		ListItem item22 = new ListItem();
		Chunk chunk83 = new Chunk(PARAGRAPH_THIRTEEN_I_A, fontPara1);
		item22.add(chunk83);
		ListItem item23 = new ListItem();
		Chunk chunk84 = new Chunk(PARAGRAPH_THIRTEEN_I_B, fontPara1);
		item23.add(chunk84);
		ListItem item24 = new ListItem();
		Chunk chunk85 = new Chunk(PARAGRAPH_THIRTEEN_I_C, fontPara1);
		item24.add(chunk85);
		ListItem item25 = new ListItem();
		Chunk chunk86 = new Chunk(PARAGRAPH_THIRTEEN_I_D, fontPara1);
		item25.add(chunk86);
		secondLevel1.add(item22);
		secondLevel1.add(item23);
		secondLevel1.add(item24);
		secondLevel1.add(item25);
		item21.add(secondLevel1);
		ListItem item26 = new ListItem();
		Chunk chunk87 = new Chunk(PARAGRAPH_FOURTEEN, fontPara1Bold);
		Chunk chunk87_sparit = new Chunk(PARAGRAPH_FOURTEEN_SPARIT, fontPara1Bold);
		
		if (!ulb.isSparitFlag()) {
			item26.add(chunk87);
		} else {
			item26.add(chunk87_sparit);
		}
		
		
		ListItem item27 = new ListItem();
		Chunk chunk88 = new Chunk(" ", fontPara1);
		item27.add(chunk88);
		List secondLevel2 = new List(List.ORDERED, List.ALPHABETICAL);
		ListItem item28 = new ListItem();
		Chunk chunk89 = new Chunk(PARAGRAPH_FIFTEEN_A, fontPara1);
		item28.add(chunk89);
		ListItem item29 = new ListItem();
		Chunk chunk90 = new Chunk(PARAGRAPH_FIFTEEN_B, fontPara1);
		item29.add(chunk90);
		secondLevel2.add(item28);
		secondLevel2.add(item29);
		item27.add(secondLevel2);
		ListItem item30 = new ListItem();
		Chunk chunk91 = new Chunk(PARAGRAPH_SIXTEEN, fontPara1);
		item30.add(chunk91);
		ListItem item31 = new ListItem();
		Chunk chunk92 = new Chunk(PARAGRAPH_SEVENTEEN_I, fontPara1);
		item31.add(chunk92);
		List secondLevel3 = new List(List.ORDERED, List.ALPHABETICAL);
		ListItem item32 = new ListItem();
		Chunk chunk93 = new Chunk(PARAGRAPH_SEVENTEEN_I_A, fontPara1);
		item32.add(chunk93);
		ListItem item33 = new ListItem();
		Chunk chunk94 = new Chunk(PARAGRAPH_SEVENTEEN_I_B, fontPara1);
		item33.add(chunk94);
		secondLevel3.add(item32);
		secondLevel3.add(item33);
		item31.add(secondLevel3);
		ListItem item34 = new ListItem();
		Phrase list115 = new Phrase();
		Chunk chunk95 = new Chunk(PARAGRAPH_EIGHTEEN_I, fontPara1);
		Chunk chunk96 = new Chunk(PARAGRAPH_EIGHTEEN_II, fontPara1Bold);
		list115.add(chunk95);
		list115.add(chunk96);
		item34.add(list115);
		ListItem item35 = new ListItem();
		Chunk chunk97 = new Chunk(PARAGRAPH_NINETEEN, fontPara1Bold);
		item35.add(chunk97);
		topLevel.add(item1);
		topLevel.add(item2);
		topLevel.add(item3);
		topLevel.add(item4);
		topLevel.add(item5);
		topLevel.add(item6);
		topLevel.add(item7);
		topLevel.add(item8);
		topLevel.add(item19);
		topLevel.add(item20);
		topLevel.add(item21);
		topLevel.add(item26);
		topLevel.add(item27);
		topLevel.add(item30);
		topLevel.add(item31);
		topLevel.add(item34);
		topLevel.add(item35);
		
		return topLevel;
	}




}
