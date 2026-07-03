package org.egov.edcr.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.egov.common.entity.edcr.Block;
import org.egov.common.entity.edcr.DcrReportBlockDetail;
import org.egov.common.entity.edcr.DcrReportFloorDetail;
import org.egov.common.entity.edcr.Floor;
import org.egov.common.entity.edcr.Occupancy;
import org.egov.common.entity.edcr.OdishaParkingHelper;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.ScrutinyDetail;
import org.egov.edcr.constants.DxfFileConstants;
import org.egov.edcr.constants.NOCConstants;
import org.egov.edcr.constants.OdishaUlbs;
import org.egov.edcr.entity.Installment;
import org.egov.edcr.feature.AdditionalFeature;
import org.egov.edcr.feature.Parking;
import org.egov.edcr.od.OdishaUtill;
import org.egov.edcr.utility.DcrConstants;
import org.egov.infra.exception.ApplicationRuntimeException;
import org.egov.infra.microservice.models.RequestInfo;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

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
public class PermitOrderServiceBPA2 extends PermitOrderService {
	
	private static final Logger LOG = Logger.getLogger(PermitOrderServiceBPA2.class);

	public static String PARAGRAPH_ONE = "Permission Under Sub-Section (3) of the Section-16 of the Orissa Development Authorities Act’1982(Orissa Act,1982) is hereby granted in favour of Land Owner";
	public static String PARAGRAPH_ONE_OLD = "Permission Under Sub-Section (3) of the Section-16 of the Orissa Development Authorities Act’1982(Orissa Act,1982) is hereby granted in favour of ";
	public static String PARAGRAPH_ONE_CRZ = "Permission under claus 4(i)(d) of CRZ notification, is hereby granted in favour of Land Owner";
	
	public static String PARAGRAPH_ONE_SPARIT = "Permission/Licence under sub section(3) of section 31/ sub section (1) of section 33 of Odisha Town Planning & Improvement Trust Act 1956 is hereby granted in favour of Land Owner";
	public static String PARAGRAPH_ONE_SPARIT_OLD = "Permission/Licence under sub section(3) of section 31/ sub section (1) of section 33 of Odisha Town Planning & Improvement Trust Act 1956 is hereby granted in favour of ";
	
	public static String PARAGRAPH_ONE_BOLD = " KRJ PROJECT LLP authorized representated by Sri Sachin Kumar Singh (Authorized signatory) and SRI HARI INFRACON PVT.LTD authorized Representative Binod Kumar Agrawalla.";
	public static final String PERMITPRVIEW = "PERMIT LETTER PREVIEW";
	public static String PARAGRAPH_TWO_I = "For %s of ";
	public static String PARAGRAPH_TWO_II_OLD = "%s %s %s over Plot No. %s pertaining to Khata No. %s in Mouza-%s ";
	public static String PARAGRAPH_TWO_II = "%s %s %s over";
	public static String PARAGRAPH_TWO_II_NEW = " in Mouza- %s ";
	public static String PARAGRAPH_TWO_III = "in the Development Plan area of ";
	public static String PARAGRAPH_TWO_IV = "Bhubaneswar ";
	public static String PARAGRAPH_TWO_V = " with the following parameters and conditions;";

	public static String PARAGRAPH_THREE_I = "The building shall be used exclusively for ";
	public static String PARAGRAPH_THREE_II = "MultistoriedResidential Apartment with Community building ";
	public static String PARAGRAPH_THREE_III = "purpose and the use shall not be changed to any other use without prior approval of this Authority.\n\n";

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
	public static String PARAGRAPH_FOURTEEN_II_CRZ = "The approval has been considered on receipt of CRZ clearance of Odisha Coastal Zone Management Authority and the conditions there upon shall be strictly added.";
	
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
		String applicationNo = getValue(bpaApplication, "applicationNo");
		String tenantIdActual = getValue(bpaApplication, "tenantId");
		Document document = new Document();
		ByteArrayOutputStream outputBytes;
		outputBytes = new ByteArrayOutputStream();
		PdfWriter.getInstance(document, outputBytes);
//		PdfWriter.getInstance(document,
//				new FileOutputStream("C:\\Temp\\Odisha\\permitFile\\001_V3LongPermit_20220525.pdf"));
		document.open();

		Map<String, Object> additionalDetails = getAdditionalDetailsMap(bpaApplication);
		Image logo = getLogo();
		Parking parking = new Parking();
		OdishaParkingHelper parkingData = parking.prepareParkingData(plan);
		
		boolean isInCRZ = false;
		
		String crzNumber = plan.getPlanInformation().getCrzNumberForProjectsFallingUnderCrzArea();
		if (crzNumber!=null && crzNumber.matches("\\d") && (Integer.parseInt(crzNumber) == 1 || Integer.parseInt(crzNumber) == 2
				|| Integer.parseInt(crzNumber) == 3)) {
			isInCRZ = true;
		}

		Font font = FontFactory.getFont(FontFactory.HELVETICA, 16, Font.BOLD);
		String[] ulbGradeNameAndUlbArray = getUlbNameAndGradeFromMdms(requestInfo, tenantIdActual);
		String ulbGradeNameAndUlb = (ulbGradeNameAndUlbArray[0] + " " + ulbGradeNameAndUlbArray[1]);
		Paragraph para = new Paragraph(ulbGradeNameAndUlb, font);
		para.setAlignment(Paragraph.ALIGN_CENTER);
		
        Paragraph permitPreviewTitle = new Paragraph(PERMITPRVIEW, font);
        permitPreviewTitle.setAlignment(Paragraph.ALIGN_CENTER);
        
		Font font1 = FontFactory.getFont(FontFactory.HELVETICA, 12);
		Font fontBold = FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD);
		Font fontBoldUnderlined = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Font.UNDERLINE);
		Paragraph para1 = new Paragraph("Form-II (Order for Grant of Permission)", font1);
		para1.setAlignment(Paragraph.ALIGN_CENTER);

		
		
		String tenantId = StringUtils.capitalize(tenantIdActual.split("\\.")[1]);
		@SuppressWarnings("deprecation")
		String approvalDate = getApprovalDate();
		Paragraph para12 = new Paragraph(
				"Letter No. " + getValue(bpaApplication, "approvalNo") + ", " + tenantId + ", Dated: " + approvalDate,
				fontBold);
		
		Paragraph permitPreviewSubTitle2 = new Paragraph(
				"Letter No. " + DxfFileConstants.NA + ", " + tenantId + ", Dated: " + DxfFileConstants.NA, fontBold);
		permitPreviewSubTitle2.setAlignment(Paragraph.ALIGN_CENTER);

		para12.setAlignment(Paragraph.ALIGN_CENTER);
		Paragraph para13 = new Paragraph("Sujog-OBPS APPLICATION NO. " + applicationNo, fontBoldUnderlined);
		para13.setAlignment(Paragraph.ALIGN_CENTER);
		
		if (isPreview) {
			document.add(permitPreviewTitle);
			document.add(Chunk.NEWLINE);
			document.add(permitPreviewSubTitle2);

		} else {
			document.add(logo);
			document.add(para);
			document.add(para1);
			document.add(Chunk.NEWLINE);
			document.add(para12);
		}

		Font fontPara1 = FontFactory.getFont(FontFactory.HELVETICA, 12);
		Font fontPara1Bold = FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD);
		
		Chunk chunk1 = new Chunk(PARAGRAPH_ONE, fontPara1);
		Chunk chunk1_old = new Chunk(PARAGRAPH_ONE_OLD, fontPara1);
		Chunk chunk1_crz = new Chunk(PARAGRAPH_ONE_CRZ, fontPara1);
		
		Chunk chunk1_sparit = new Chunk(PARAGRAPH_ONE_SPARIT, fontPara1);
		Chunk chunk1_sparit_old = new Chunk(PARAGRAPH_ONE_SPARIT_OLD, fontPara1);
		
		String ownersCsv = getNameOfOwner(bpaApplication);
		Chunk chunk2 = new Chunk(ownersCsv, fontPara1Bold);
		Phrase phrasePara1 = new Phrase();
		
		OdishaUlbs ulb = OdishaUlbs.getUlb(plan.getThirdPartyUserTenantld());
		
		boolean isPlotAndKhataDetailsEmpty = CollectionUtils.isEmpty(getPlotAndKhataDetails(additionalDetails));

		if (!ulb.isSparitFlag()) {
		    if (isPlotAndKhataDetailsEmpty) {
		    	phrasePara1.add(chunk1_old);
				phrasePara1.add(chunk2);
		    } else {
		    	phrasePara1.add(isInCRZ ? chunk1_crz : chunk1);
		    }
		} else {
		    if (isPlotAndKhataDetailsEmpty) {
				phrasePara1.add(chunk1_sparit_old);
				phrasePara1.add(chunk2);
		    } else {
		    	phrasePara1.add(chunk1_sparit);
		    }
		}
		
		String serviceType = getServiceType(plan, additionalDetails);
		Chunk chunk21 = new Chunk(String.format(PARAGRAPH_TWO_I, serviceType), fontPara1);
		String floorInfo = plan.getPlanInformation().getFloorInfo() + " ";
		String occupancy = plan.getPlanInformation().getOccupancy();
		String subOccupancy = plan.getPlanInformation().getSubOccupancy() + " ";
		String plotNo = plan.getPlanInformation().getPlotNo() + " ";
		String khataNo = plan.getPlanInformation().getKhataNo() + " ";
		String localityName = getValue(bpaApplication, "$.landInfo.address.locality.name");

		String oldPermitNo = getPreviousPermitNo(additionalDetails);
		String alterationStatement = getAlterationSubserviceStatement(additionalDetails);
		alterationStatement = alterationStatement.replace("xxxxx", oldPermitNo);
		
		Phrase phrasePara2 = new Phrase();
		Phrase phrasePara2New = new Phrase();
		Phrase phrasePara2New2 = new Phrase();
		
		Chunk chunk23 = new Chunk(PARAGRAPH_TWO_III, fontPara1);
		Chunk chunk24 = new Chunk(tenantId, fontPara1Bold);
		Chunk chunk25 = new Chunk(PARAGRAPH_TWO_V, fontPara1);

		if (CollectionUtils.isEmpty(getPlotAndKhataDetails(additionalDetails))) {
			Chunk chunk22 = new Chunk(String.format(PARAGRAPH_TWO_II_OLD, floorInfo, occupancy + ", " + subOccupancy,
					alterationStatement, plotNo, khataNo, localityName), fontPara1Bold);
			
			phrasePara2.add(chunk21);
			phrasePara2.add(chunk22);
			phrasePara2.add(chunk23);
			phrasePara2.add(chunk24);
			phrasePara2.add(chunk25);

		} else {
			//table
			Chunk chunk22_new = new Chunk(
					String.format(PARAGRAPH_TWO_II, floorInfo, occupancy + ", " + subOccupancy, alterationStatement),
					fontPara1Bold);			
			Chunk chunk23_new = new Chunk(String.format(PARAGRAPH_TWO_II_NEW, localityName), fontPara1);
			
			phrasePara2New.add(chunk21);
			phrasePara2New.add(chunk22_new);
			phrasePara2New2.add(chunk23_new);
			phrasePara2New2.add(chunk23);
			phrasePara2New2.add(chunk24);
			phrasePara2New2.add(chunk25);
			
		}
		
		Paragraph cadTitle = new Paragraph();
		Phrase cadPhrase = new Phrase(CAD_STATEMENT, fontBold);
		Chunk bullet = new Chunk("\u2022"); // Unicode character for bullet point
        bullet.setFont(fontBold);
        cadTitle.add(bullet);
        cadTitle.add(Chunk.TABBING);
        cadTitle.add(cadPhrase);
		
		BigDecimal plotArea = plan.getPlot().getArea();
		BigDecimal totalRoadSurrenderArea = plan.getTotalSurrenderRoadArea();
		// As observed in format provided by BDA, net plot area =(total plot area-road
		// affected area)
		BigDecimal netPlotArea = plan.getPlot().getPlotBndryArea().setScale(DcrConstants.DECIMALDIGITS_MEASUREMENTS,
				DcrConstants.ROUNDMODE_MEASUREMENTS);
		BigDecimal roadWidth = plan.getPlanInformation().getTotalRoadWidth();
		if(roadWidth == null || roadWidth.compareTo(BigDecimal.ZERO)<=0) //for old scrutiny support
			roadWidth = plan.getPlanInformation().getRoadWidth();
		Font fontList1 = FontFactory.getFont("Bold", 12, Font.UNDERLINE);
		Phrase list1 = new Phrase("Parameters:\n", fontList1);
		Chunk chunk31 = new Chunk(Chunk.TABBING);
//		Chunk chunk31 = new Chunk(Chunk.TAB);
		Chunk chunk32 = new Chunk(getTotalPlotAreaValueV2(plan)+"\n", fontPara1Bold);
		Chunk chunk33 = new Chunk("	- CDP road affected area: " + totalRoadSurrenderArea + " Sqm.\n", fontPara1Bold);
		java.util.List<Chunk> affectedArea = getTotalCDPRoadAffectedArea(plan);
		Chunk chunk34 = new Chunk("	- Net plot area: " + netPlotArea +SQM +"\n", fontPara1Bold);
		Chunk chunk35 = new Chunk("	- Abutting road width: " + roadWidth + " Mtr\n", fontPara1Bold);

		Phrase list11 = new Phrase();
		list11.add(chunk31);
		list11.add(chunk32);
		Phrase list12 = new Phrase();
		//list11.add(chunk33);
		if(!affectedArea.isEmpty()) {
			list11.add(chunk31);
			list11.addAll(affectedArea);
		}
		Phrase list13 = new Phrase();
		list11.add(chunk31);
		list11.add(chunk34);
		Phrase list14 = new Phrase();
		list11.add(chunk31);
		list11.add(chunk35);

		String subService = null;
		if (Objects.nonNull(additionalDetails) && Objects.nonNull(additionalDetails.get(BPA_ADD_DETAILS_SERVICE_KEY))
				&& additionalDetails.get(BPA_ADD_DETAILS_SERVICE_KEY) instanceof Map
				&& Objects.nonNull(((Map) additionalDetails.get(BPA_ADD_DETAILS_SERVICE_KEY))
						.get(BPA_ADD_DETAILS_SUBSERVICE_KEY))) {
			subService = ((Map) additionalDetails.get(BPA_ADD_DETAILS_SERVICE_KEY)).get(BPA_ADD_DETAILS_SUBSERVICE_KEY)
					+ "";
		}
		
		PdfPTable table1 = null;
		if (Objects.nonNull(subService) && !ALTERATION_SUBSERVICE_A.equals(subService)) {
			// for existing area column
			table1 = new PdfPTable(5);
		} else {
			table1 = new PdfPTable(4);
		}
		table1.setWidthPercentage(100f);

		java.util.List<DcrReportBlockDetail> blockDetails = buildBlockWiseProposedInfo(plan);
		for (DcrReportBlockDetail block : blockDetails) {
			BigDecimal totalFloorArea = BigDecimal.ZERO;
			Block planBlock = plan.getBlockByName(block.getBlockNo());
			//Block planBlock = plan.getBlocks().get(Integer.parseInt(block.getBlockNo()));
			addTableHeader1(table1, block,plan, subService);
			java.util.List<DcrReportFloorDetail> floorDetails = block.getDcrReportFloorDetails();
			for (DcrReportFloorDetail floor : floorDetails) {
				totalFloorArea = addRowsPerFloorAndAggregateFlrAreas(table1, floor, totalFloorArea,
						planBlock.getBuilding().getFloorNumber(floor.getFloorNumberInteger()), subService);

			}
			addTotalRow(table1, totalFloorArea,planBlock, subService);

		}

		PdfPTable table3 = new PdfPTable(1);
		table3.setWidthPercentage(100f);
		addRows31(table3, plan);

		PdfPTable table5 = new PdfPTable(3);
		table5.setWidthPercentage(100f);
		addTableRow5(table5);

		PdfPTable table6 = new PdfPTable(3);
		table6.setWidthPercentage(100f);
		addTableHeader6(table6, plan, parkingData);

		PdfPTable table10 = new PdfPTable(1);
		table10.setWidthPercentage(100f);
		addTableHeader10(table10, plan);

		PdfPTable table11 = new PdfPTable(1);
		table11.setWidthPercentage(100f);
		addTableHeader11(table11, plan);

		PdfPTable table12 = new PdfPTable(3);
		table12.setWidthPercentage(100f);
		addRows121(table12, plan);

		PdfPTable table13 = new PdfPTable(2);
		table13.setWidthPercentage(100f);
		addRows131(table13, plan);

		PdfPTable table14 = new PdfPTable(2);
		table14.setWidthPercentage(100f);
		addRows141(table14, parkingData);

		PdfPTable table15 = new PdfPTable(3);
		table15.setWidthPercentage(100f);
		addTableHeader151(table15);
		addTableHeader152(table15, plan);

		Chunk chunk41 = new Chunk("	- Set backs approved to be provided", fontPara1Bold);
		Phrase list21 = new Phrase();
		list21.add(chunk31);
		list21.add(chunk41);

		Font fontList3 = FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD);
		//Phrase list3 = new Phrase("- NOCs/ Clearances submitted:\n", fontList3);
		
		List list1Payment = new List(List.ORDERED, List.ALPHABETICAL);
		List list1Payment2 = new List(List.ORDERED, List.ALPHABETICAL);//list1
		List payments = new List(List.UNORDERED); 
		List payments2 = new List(List.UNORDERED);
		ListItem list1Item11 = new ListItem(); 
		ListItem list1Item112 = new ListItem();
		
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
		Chunk chunk56 = new Chunk(parkingData.totalParkingProvided + " Sqm. ", fontPara1Bold);
		Chunk chunk57 = new Chunk(PARAGRAPH_FIVE_III, fontPara1);
		list6.add(chunk55);
		list6.add(chunk56);
		list6.add(chunk57);
		item3.add(list6);
		ListItem item4 = new ListItem();
		Phrase list7 = new Phrase();
		Chunk chunk58 = new Chunk(PARAGRAPH_SIX_I, fontPara1);
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
		String roadAffectedAreas = getRoadAffectedArea(plan);
		String roadAffectedArea = roadAffectedAreas.equalsIgnoreCase("0.00") ? totalRoadSurrenderArea+""
				: roadAffectedAreas;
		Chunk chunk63 = new Chunk(roadAffectedArea + " Sqm. ", fontPara1Bold);
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
		Chunk chunk87_2_crz = new Chunk(PARAGRAPH_FOURTEEN_II_CRZ, fontPara1);;
		
		if (!ulb.isSparitFlag()) {
			item26.add(chunk87);			
		} else {
			item26.add(chunk87_sparit);
		}
		
		ListItem item26_2 = new ListItem();
		if(isInCRZ)
			item26_2.add(chunk87_2_crz);	
		
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
		if(isInCRZ)
			topLevel.add(item26_2);
		topLevel.add(item27);
		topLevel.add(item30);
		topLevel.add(item31);
		topLevel.add(item34);
		topLevel.add(item35);

		document.add(para13);
		if(dxfToPdfFlag && (plan.getIsDxfToPdfEnabled() != null && plan.getIsDxfToPdfEnabled())) {		
			String drawingRefId= plan.getPlanInformation().getDxfToPdfCorrelationId();
			Paragraph headerSubTitle4 = new Paragraph("Drawing Reference ID. " + drawingRefId, fontBoldUnderlined);
			headerSubTitle4.setAlignment(Paragraph.ALIGN_CENTER);
			document.add(headerSubTitle4);
		}
		document.add(Chunk.NEWLINE);
		document.add(phrasePara1);
		document.add(Chunk.NEWLINE);

		if (CollectionUtils.isEmpty(getPlotAndKhataDetails(additionalDetails))) {

			document.add(Chunk.NEWLINE);
			document.add(phrasePara2);
		} else {
			PdfPTable plotTable = new PdfPTable(7);
			plotTable.setLockedWidth(false);
			plotTable.setWidthPercentage(100f);
			addTableHeaderPlotTable(plotTable);
			addRowsForPlotTable(requestInfo, plotTable, additionalDetails, tenantIdActual);
			
			document.add(plotTable);
			document.add(Chunk.NEWLINE);
			document.add(phrasePara2New);
			document.add(phrasePara2New2);
		}

		document.add(Chunk.NEWLINE);
		document.add(list1);
		document.add(Chunk.NEWLINE);
		document.add(list11);
		document.add(list12);
		document.add(list13);
		document.add(list14);
		document.add(Chunk.NEWLINE);
		document.add(table1);
		document.add(table3);
		document.add(table6);
		document.add(table10);
		document.add(table11);
		document.add(table12);
		document.add(table13);
		document.add(table14);
		document.add(Chunk.NEWLINE);
		document.add(list21);
		document.add(Chunk.NEWLINE);
		document.add(table15);
		document.add(Chunk.NEWLINE);
		document.add(cadTitle);
		document.add(Chunk.NEWLINE);
//		document.add(list3);
//		document.add(Chunk.NEWLINE);
		//addNocs(document, requestInfo, tenantIdActual, applicationNo);
		document.add(Chunk.NEWLINE);
		document.add(Chunk.NEWLINE);
		document.add(topLevel);
		document.add(Chunk.NEWLINE);

		boolean isInstallment = false;
		
		if (!isPreview) {
			
		Map<String,Object> installmentData = (Map<String, Object>) getAllInstallments(applicationNo, requestInfo);
		//check if installment applicable or not.If applicable,then check if full payment or not
		if (Objects.isNull(installmentData)
				|| (Objects.nonNull(installmentData) && Objects.nonNull(installmentData.get("installments"))
						&& installmentData.get("installments") instanceof java.util.List
						&& ((java.util.List) installmentData.get("installments")).isEmpty())
				|| (Objects.nonNull(installmentData) && Objects.nonNull(installmentData.get("fullPayment"))
						&& installmentData.get("fullPayment") instanceof java.util.List
						&& !((java.util.List) installmentData.get("fullPayment")).isEmpty()
						&& Objects.nonNull(((Map) ((java.util.List) installmentData.get("fullPayment")).get(0))
								.get("isPaymentCompletedInDemand"))
						&& (Boolean) ((Map) ((java.util.List) installmentData.get("fullPayment")).get(0))
								.get("isPaymentCompletedInDemand"))) {
//			LOG.info("Inside first if... ");
			//first condition in || = null response
			//second condition in || = empty installment response
			//third condition in || = full payment done
			//For all above 3 cases,full payment to be shown from payment details like before-
			//Map<String,String> paymentDetailsMap = getAllFeeDetailsMap(bpaApplication, requestInfo, applicationNo, tenantIdActual);
		}
		else if (Objects.nonNull(installmentData) && Objects.nonNull(installmentData.get("installments"))
				&& installmentData.get("installments") instanceof java.util.List
				&& !((java.util.List) installmentData.get("installments")).isEmpty()) {
			// if payment done in installments
			isInstallment = true;
			//LOG.info("Inside first else: isInstallment = "+ isInstallment);
		}
		
		
		//String[] feeDetails = getAllFeeDetails(requestInfo, applicationNo, tenantIdActual);
		Map<String,String> paymentDetailsMap = getAllFeeDetailsMap(bpaApplication, requestInfo, applicationNo, tenantIdActual);
		PdfPTable table16 = new PdfPTable(3);
		PdfPTable table17 = new PdfPTable(3);
		PdfPTable table18 = new PdfPTable(1);
		PdfPTable table19 = new PdfPTable(3);
		PdfPTable table20 = new PdfPTable(2);
		PdfPTable table21 = new PdfPTable(3);
		PdfPTable table22 = new PdfPTable(1);
		PdfPTable table23 = new PdfPTable(3);
		PdfPTable table24 = new PdfPTable(2);
		PdfPTable table25 = new PdfPTable(1);
		PdfPTable table26 = new PdfPTable(3);
		PdfPTable table27 = new PdfPTable(2);
		PdfPTable table28 = new PdfPTable(1);
		PdfPTable table29 = new PdfPTable(3);
		PdfPTable table30 = new PdfPTable(2);
		PdfPTable table31 = new PdfPTable(2);
		PdfPTable table32 = new PdfPTable(2);
		PdfPTable table33 = new PdfPTable(2);
		PdfPTable table34 = new PdfPTable(2);

		// Not Installment
		PdfPTable table35 = new PdfPTable(3);
		PdfPTable otherFeeReasonTable = new PdfPTable(1);

		java.util.List<PdfPTable> tablesForInstallments = new ArrayList<>();
		ObjectMapper mapper = new ObjectMapper();
		
	
		if (isInstallment) {
			Map<String, java.util.List<Installment>> installmentMapper = prepareTaxHeadCodeToInstallmentsMap(
					installmentData, mapper);
			// scrutiny fees tables-
			table16.setWidthPercentage(100f);
			addTableHeader16(table16);
			
			// permit fees tables-
			table17.setWidthPercentage(100f);
			addScrutinyFeeDetails(table17, otherFeeReasonTable, paymentDetailsMap, additionalDetails);
			String[] seq="B C D E F G H I J K L M N O P Q R S T U V W X Y Z".split("\\s");
			java.util.List<String> list = new ArrayList<>(Arrays.asList(seq));
			Iterator<String> sequence = list.iterator();
			// any change in this list and display name has to be done in method addTableRow35 also 
			java.util.List<String> taxHeadsToShowInOrder = Arrays.asList(new String[] { TAXHEAD_BPA_SANC_SANC_FEE_CODE,
					TAXHEAD_BPA_SANC_WORKER_WELFARE_CESS_CODE, TAXHEAD_BPA_SANC_SECURITY_DEPOSIT_CODE,TAXHEAD_BPA_SANC_TEMP_RETENTION_FEE_CODE,
					TAXHEAD_BPA_SANC_SHELTER_FEE_CODE, TAXHEAD_BPA_SANC_PUR_FAR_CODE, TAXHEAD_BPA_SANC_EIDP_FEE_CODE,
					TAXHEAD_BPA_SANC_ADJUSTMENT_AMOUNT_CODE, TAXHEAD_BPA_SANC_ADJUSTMENT_AMOUNT_CODE_1, TAXHEAD_BPA_SANC_ADJUSTMENT_AMOUNT_CODE_2,
					TAXHEAD_BPA_SANC_ADJUSTMENT_AMOUNT_CODE_3});
			
			BigDecimal totalPaidFromInstallments=BigDecimal.ZERO;
			BigDecimal totalRemainingFeesFromInstallments = BigDecimal.ZERO;
			for (String taxHeadCode : taxHeadsToShowInOrder) {
				
				//LOG.info("taxHeadCode for Currently:" + taxHeadCode);
				
				if (!installmentMapper.containsKey(taxHeadCode)) {
					continue;
				}
				if (installmentMapper.get(taxHeadCode).get(0).getTaxAmount().compareTo(BigDecimal.ZERO) == 0) {
					continue;
				}
				String feeComponentName = getFeeComponentNameFromTaxHeadCodeForPaymentTable(taxHeadCode);
				if (feeComponentName.equals("")) {
					// display name if not found from BDA provided format then use standard names
					
					if (taxHeadCode.equalsIgnoreCase(TAXHEAD_BPA_SANC_ADJUSTMENT_AMOUNT_CODE_1)
							|| taxHeadCode.equalsIgnoreCase(TAXHEAD_BPA_SANC_ADJUSTMENT_AMOUNT_CODE_2)
							|| taxHeadCode.equalsIgnoreCase(TAXHEAD_BPA_SANC_ADJUSTMENT_AMOUNT_CODE_3)) {

						java.util.List otherFees = (java.util.List) additionalDetails.get("otherFees");

						switch (taxHeadCode) {
						case TAXHEAD_BPA_SANC_ADJUSTMENT_AMOUNT_CODE_1:
							if (!otherFees.isEmpty())
								feeComponentName = (String) ((Map) otherFees.get(0)).get("reason");
							break;

						case TAXHEAD_BPA_SANC_ADJUSTMENT_AMOUNT_CODE_2:
							if (otherFees.size() > 1)
								feeComponentName = (String) ((Map) otherFees.get(1)).get("reason");
							break;

						case TAXHEAD_BPA_SANC_ADJUSTMENT_AMOUNT_CODE_3:
							if (otherFees.size() > 2)
								feeComponentName = (String) ((Map) otherFees.get(2)).get("reason");
							break;

						default:
							feeComponentName = getFeeComponentNameFromTaxHeadCode(taxHeadCode);
							break;

						}
					} else {
						feeComponentName = getFeeComponentNameFromTaxHeadCode(taxHeadCode);
					}

					
				}
				BaseColor backGroundColor = getBackGroundColorForTaxHeadcode(taxHeadCode);
				boolean isSingleInstallment = (installmentMapper.get(taxHeadCode).size() == 1) ? true : false;
				
				//LOG.info("isSingleInstallment = "+ isSingleInstallment);
				//LOG.info("inside installmentMapper.get(taxHeadCode) = " + installmentMapper.get(taxHeadCode));
				
				if (isSingleInstallment) {
					
					//LOG.info("Inside the isSingleInstallment if... ");
					//LOG.info("taxHeadCode is :" + taxHeadCode);
					// handling for single installment taxheads
					PdfPTable singleInstallmentTable = new PdfPTable(3);
					singleInstallmentTable.setWidthPercentage(100f);
					setTablesForSingleInstallmentTaxHeads(singleInstallmentTable,
							sequence.next() + ". " + feeComponentName,
							installmentMapper.get(taxHeadCode).get(0).getTaxAmount() + "", backGroundColor);
					tablesForInstallments.add(singleInstallmentTable);
					// maintain total paid and remaining-
					if (installmentMapper.get(taxHeadCode).get(0).isPaymentCompletedInDemand()) {
						totalPaidFromInstallments = totalPaidFromInstallments
								.add(installmentMapper.get(taxHeadCode).get(0).getTaxAmount());
					}
				} else {
					
					//LOG.info(" else: Not single installment... ");
					//LOG.info("taxHeadCode is :" + taxHeadCode);
					// handling for multiple installment taxheads

					// taxheadname header table-
					PdfPTable headerTable = getInstallmentCellForFeeComponentName(
							sequence.next() + ". " + feeComponentName, backGroundColor);
					tablesForInstallments.add(headerTable);

					// installment wise table rows-
					PdfPTable installmentTable = getTableForInstallmentBreakup();
					BigDecimal totalPayable = BigDecimal.ZERO;
					for (Installment installment : installmentMapper.get(taxHeadCode)) {
						addInstallmentCellToTable(installmentTable, backGroundColor, installment);
						totalPayable = totalPayable.add(installment.getTaxAmount());

						// maintain total paid and remaining-
						if (installment.isPaymentCompletedInDemand()) {
							totalPaidFromInstallments = totalPaidFromInstallments.add(installment.getTaxAmount());
						} else {
							totalRemainingFeesFromInstallments = totalRemainingFeesFromInstallments
									.add(installment.getTaxAmount());
						}
					}
					tablesForInstallments.add(installmentTable);
					// total payable table-
					PdfPTable totalPayableTable = getTableForTotalOfInstallment();
					setTableForTotalOfInstallment(totalPayableTable, feeComponentName, totalPayable + "",
							backGroundColor);
					tablesForInstallments.add(totalPayableTable);
				}
				// reason for other fee--
				if (taxHeadCode.equalsIgnoreCase(TAXHEAD_BPA_SANC_ADJUSTMENT_AMOUNT_CODE)) {
					PdfPCell header = new PdfPCell();
					header.setBackgroundColor(ORANGE);
					header.setBorderWidth(2);
					header.setVerticalAlignment(Element.ALIGN_LEFT);
					header.setPhrase(new Phrase(
							"Other Fee Detail: "
									+ additionalDetails.get("modificationReasonSanctionFeeAdjustmentAmount"),
							fontPara1Bold));
					otherFeeReasonTable.addCell(header);
				}
			}
			table31.setWidthPercentage(100f);
			Map<String,String> paymentDetailsMapFromInstallments = new HashMap<>();
			String totalApplicationFeeAmountPaid = paymentDetailsMap.get("totalApplicationFeeAmountPaid");
			paymentDetailsMapFromInstallments.put("totalApplicationAndPermitFee", totalPaidFromInstallments.add(new BigDecimal(totalApplicationFeeAmountPaid))+"");
			paymentDetailsMapFromInstallments.put("totalRemainingFeesFromInstallments", totalRemainingFeesFromInstallments+"");
			addTableHeader31(table31, paymentDetailsMapFromInstallments);
			addRemainingInstallmentAmountToBePaid(table31, paymentDetailsMapFromInstallments);
			
			
			/*
			table17.setWidthPercentage(100f);
			addTableHeader171(table17);
			addTableHeader172(table17);
			addTableHeader173(table17);
			table18.setWidthPercentage(100f);
			addTableHeader18(table18);
			table19.setWidthPercentage(100f);
			addTableHeader191(table19);
			addTableHeader192(table19);
			addTableHeader193(table19);
			table20.setWidthPercentage(100f);
			addTableHeader20(table20);
			table21.setWidthPercentage(100f);
			addTableHeader21(table21);
			table22.setWidthPercentage(100f);
			addTableHeader22(table22);
			table23.setWidthPercentage(100f);
			addTableHeader231(table23);
			addTableHeader232(table23);
			addTableHeader233(table23);
			addTableHeader234(table23);
			table24.setWidthPercentage(100f);
			addTableHeader24(table24);
			table25.setWidthPercentage(100f);
			addTableHeader25(table25);
			table26.setWidthPercentage(100f);
			addTableHeader261(table26);
			addTableHeader262(table26);
			addTableHeader263(table26);
			addTableHeader264(table26);
			table27.setWidthPercentage(100f);
			addTableHeader27(table27);
			table28.setWidthPercentage(100f);
			addTableHeader28(table28);
			table29.setWidthPercentage(100f);
			addTableHeader291(table29);
			addTableHeader292(table29);
			addTableHeader293(table29);
			addTableHeader294(table29);
			table30.setWidthPercentage(100f);
			addTableHeader30(table30);
			table31.setWidthPercentage(100f);
			addTableHeader31(table31, paymentDetailsMap);
			table32.setWidthPercentage(100f);
			addTableHeader32(table32);
			table33.setWidthPercentage(100f);
			addTableHeader33(table33);
			table34.setWidthPercentage(100f);
			addTableHeader34(table34);
			*/
		} else {
	
			table16.setWidthPercentage(100f);
			addTableHeader16(table16);
			table35.setWidthPercentage(100f);
			addTableRow35(table35,otherFeeReasonTable, paymentDetailsMap,additionalDetails);
			otherFeeReasonTable.setWidthPercentage(100f);
			table31.setWidthPercentage(100f);
			addTableHeader31(table31, paymentDetailsMap);

	}
		document.add(table16);
		document.add(table17);
		document.add(table18);
		document.add(table19);
		document.add(table20);
		document.add(table21);
		document.add(table22);
		document.add(table23);
		document.add(table24);
		document.add(table25);
		document.add(table26);
		document.add(table27);
		document.add(table28);
		document.add(table29);
		document.add(table30);
		document.add(table35);
		//payment table if installment is available-
		for(PdfPTable table:tablesForInstallments) {
			document.add(table);
		}
		//
		//if(!otherFeeReasonTable.getChunks().isEmpty())
			document.add(otherFeeReasonTable);
		document.add(table31);
		document.add(table32);
		document.add(table33);
		document.add(table34);
		
		document.add(Chunk.NEWLINE);
		// disabled till installment will go live
//		document.add(para3);
		document.add(Chunk.NEWLINE);
		document.add(Chunk.NEWLINE);
		
	} else {
		
		java.util.List<Map<String, Object>> estimatedApplicationFeeDetails = getEstimatedApplicationFeeDetails(
				requestInfo, bpaApplication);
		
		for (Map<String, Object> taxHeadEstimate : estimatedApplicationFeeDetails) {
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
			Chunk paymentSentencePostPreview = new Chunk("  - Paid", font1);
			paymentLine.add(paymentSentencePre);
			paymentLine.add(paymentSentence);
			paymentLine.add(paymentSentencePost);
			paymentLine.add(paymentSentencePostPreview);
			individualPaymentSentence.add(paymentLine);
			payments2.add(individualPaymentSentence);
			
		}
		list1Item112.add(payments2);
		list1Payment2.add(list1Item112);
		document.add(list1Payment2);
		document.add(Chunk.NEWLINE);
		
		
		java.util.List<Map<String, Object>> estimatedSanctionFeeDetails = getEstimatedSanctionFeeDetails(requestInfo,
				bpaApplication);

		for (Map<String, Object> taxHeadEstimate : estimatedSanctionFeeDetails) {

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
		
		list1Item11.add(payments);
		list1Payment.add(list1Item11);
		
		document.add(list1Payment);
		document.add(Chunk.NEWLINE);
	}
		//Phrase para3 = new Phrase(PARAGRAPH_NINETEEN_II, fontPara1Bold);

		List toplevel2 = new List(List.ORDERED);
		toplevel2.setFirst(18);
		ListItem item36 = new ListItem();
		Chunk chunk98 = new Chunk(PARAGRAPH_TWENTY_I, fontPara1Bold);
		item36.add(chunk98);
		// dynamic Other conditions from additionalDetails of application--
		if (Objects.nonNull(additionalDetails)
				&& Objects.nonNull(additionalDetails.get("otherConditionsForPermitCertificate"))) {
			Chunk otherConditions = new Chunk(
					String.valueOf(additionalDetails.get("otherConditionsForPermitCertificate")), fontPara1);
			item36.add(otherConditions);
		}
		
		//commented out code for hardcoded other conditions as per format provided by BDA--
		/*
		List secondlevel4 = new List(List.ORDERED, List.ALPHABETICAL);
		ListItem item37 = new ListItem();
		Chunk chunk99 = new Chunk(PARAGRAPH_TWENTY_I_A, fontPara1);
		item37.add(chunk99);
		ListItem item38 = new ListItem();
		Chunk chunk100 = new Chunk(PARAGRAPH_TWENTY_I_B, fontPara1);
		item38.add(chunk100);
		ListItem item39 = new ListItem();
		Chunk chunk101 = new Chunk(PARAGRAPH_TWENTY_I_C, fontPara1);
		item39.add(chunk101);
		ListItem item40 = new ListItem();
		Chunk chunk102 = new Chunk(PARAGRAPH_TWENTY_I_D, fontPara1);
		item40.add(chunk102);
		ListItem item41 = new ListItem();
		Chunk chunk103 = new Chunk(PARAGRAPH_TWENTY_I_E, fontPara1);
		item41.add(chunk103);
		ListItem item42 = new ListItem();
		Chunk chunk104 = new Chunk(PARAGRAPH_TWENTY_I_F, fontPara1);
		item42.add(chunk104);
		ListItem item43 = new ListItem();
		Chunk chunk105 = new Chunk(PARAGRAPH_TWENTY_I_G, fontPara1);
		item43.add(chunk105);
		ListItem item44 = new ListItem();
		Chunk chunk106 = new Chunk(PARAGRAPH_TWENTY_I_H, fontPara1);
		item44.add(chunk106);
		ListItem item45 = new ListItem();
		Chunk chunk107 = new Chunk(PARAGRAPH_TWENTY_I_I, fontPara1);
		item45.add(chunk107);
		ListItem item46 = new ListItem();
		Chunk chunk108 = new Chunk(PARAGRAPH_TWENTY_I_J, fontPara1);
		item46.add(chunk108);
		ListItem item47 = new ListItem();
		Chunk chunk109 = new Chunk(PARAGRAPH_TWENTY_I_K, fontPara1Bold);
		item47.add(chunk109);
		ListItem item48 = new ListItem();
		Chunk chunk110 = new Chunk(PARAGRAPH_TWENTY_I_L, fontPara1Bold);
		item48.add(chunk110);
		ListItem item49 = new ListItem();
		Chunk chunk111 = new Chunk(PARAGRAPH_TWENTY_I_M, fontPara1Bold);
		item49.add(chunk111);
		ListItem item50 = new ListItem();
		Chunk chunk112 = new Chunk(PARAGRAPH_TWENTY_I_N, fontPara1Bold);
		item50.add(chunk112);
		ListItem item51 = new ListItem();
		Chunk chunk113 = new Chunk(PARAGRAPH_TWENTY_I_O, fontPara1Bold);
		item51.add(chunk113);
		ListItem item52 = new ListItem();
		Chunk chunk114 = new Chunk(PARAGRAPH_TWENTY_I_P, fontPara1Bold);
		item52.add(chunk114);
		ListItem item53 = new ListItem();
		Chunk chunk115 = new Chunk(PARAGRAPH_TWENTY_I_Q, fontPara1Bold);
		item53.add(chunk115);
		ListItem item54 = new ListItem();
		Chunk chunk116 = new Chunk(PARAGRAPH_TWENTY_I_R, fontPara1Bold);
		item54.add(chunk116);
		ListItem item55 = new ListItem();
		Chunk chunk117 = new Chunk(PARAGRAPH_TWENTY_I_S, fontPara1Bold);
		item55.add(chunk117);
		ListItem item56 = new ListItem();
		Chunk chunk118 = new Chunk(PARAGRAPH_TWENTY_I_T, fontPara1Bold);
		item56.add(chunk118);
		ListItem item57 = new ListItem();
		Chunk chunk119 = new Chunk(PARAGRAPH_TWENTY_I_U, fontPara1Bold);
		item57.add(chunk119);
		ListItem item58 = new ListItem();
		Chunk chunk120 = new Chunk(PARAGRAPH_TWENTY_I_V, fontPara1Bold);
		item58.add(chunk120);
		secondlevel4.add(item37);
		secondlevel4.add(item38);
		secondlevel4.add(item39);
		secondlevel4.add(item40);
		secondlevel4.add(item41);
		secondlevel4.add(item42);
		secondlevel4.add(item43);
		secondlevel4.add(item44);
		secondlevel4.add(item45);
		secondlevel4.add(item46);
		secondlevel4.add(item47);
		secondlevel4.add(item48);
		secondlevel4.add(item49);
		secondlevel4.add(item50);
		secondlevel4.add(item51);
		secondlevel4.add(item52);
		secondlevel4.add(item53);
		secondlevel4.add(item54);
		secondlevel4.add(item55);
		secondlevel4.add(item56);
		secondlevel4.add(item57);
		secondlevel4.add(item58);
		item36.add(secondlevel4);
		*/
		
		//"Other conditions to be complied by the applicant are as per the following
		toplevel2.add(item36);

		document.add(toplevel2);

		Paragraph nocTitle = new Paragraph(NOC_TITLE, fontBoldUnderlined);

		Paragraph onlineTitle = new Paragraph(ONLINE_TITLE, fontBold);
		Paragraph otherNocTitle = new Paragraph(OTHERNOC_TITLE, fontBold);

		// online NOC table
		PdfPTable onlineNOCtable = new PdfPTable(2);
		onlineNOCtable.setLockedWidth(false);
		float[] onlineNOCtableWidths = { 60f, 40f };
		onlineNOCtable.setWidths(onlineNOCtableWidths);
		addTableHeaderForNOCTable(onlineNOCtable);
		addRowsForNocTable(onlineNOCtable, requestInfo, tenantIdActual, applicationNo,additionalDetails);

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
		
		
		// approved by section in the permit letter:

		// Date
		Phrase dateSection = new Phrase();
		Paragraph approvedByParagraph = new Paragraph();
		
		if (!isPreview) {
			
	
		Chunk dateSectionPart1 = new Chunk("Date: ",font1);
		Chunk dateSectionPart2 = new Chunk(approvalDate ,font1);
		dateSection.add(dateSectionPart1);
		dateSection.add(dateSectionPart2);
		Paragraph dateParagraph = new Paragraph();
		dateParagraph.add(dateSection);
		dateParagraph.setAlignment(Element.ALIGN_LEFT);
		
		//Approver details
		String approvedBy = getValue(bpaApplication, "$.dscDetails[0].approvedBy");
		Map<String, Object> approverDetailsMap = getApproverDetails(requestInfo, tenantId, "BPA2", approvedBy,
				"EMPLOYEE");
		
		String approverName = !org.springframework.util.StringUtils.isEmpty(approverDetailsMap.get("nameOfApprover"))
				? approverDetailsMap.get("nameOfApprover") + ""
				: "";
		String[] bpaPermitHeader = getUlbNameAndGradeFromMdms(requestInfo, tenantIdActual);
		
		Font fontParaApprovedBy = FontFactory.getFont(FontFactory.HELVETICA, 12);
		Phrase approvedBySection = new Phrase();
		Chunk approvedBySectionLine1 = new Chunk("BY ORDER OF ", fontParaApprovedBy);
		Chunk approvedBySectionLine2 = new Chunk(approverName + "\n", fontParaApprovedBy);
		Chunk approvedBySectionLine3 = new Chunk("Authorized Officer"+ "\n", fontParaApprovedBy);
		Chunk approvedBySectionLine4 = new Chunk(bpaPermitHeader[2]);
		approvedBySection.add(approvedBySectionLine1);
		approvedBySection.add(approvedBySectionLine2);
		approvedBySection.add(approvedBySectionLine3);
		approvedBySection.add(approvedBySectionLine4);
		
		approvedByParagraph.add(approvedBySection);
		approvedByParagraph.setAlignment(Element.ALIGN_RIGHT);
		
		document.add(Chunk.NEWLINE);
		document.add(Chunk.NEWLINE);
		document.add(Chunk.NEWLINE);
		document.add(dateParagraph);
		document.add(approvedByParagraph);
	}
		document.close();
		return new ByteArrayInputStream(outputBytes.toByteArray());
	}
	
	private void addScrutinyFeeDetails(PdfPTable table35, PdfPTable otherFeeReasonTable, Map<String, String> paymentDetailsMap,
			Map<String, Object> additionalDetails) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 13, Font.BOLD);
		if(StringUtils.isNotEmpty(paymentDetailsMap.get("BPA_BLDNG_OPRN_FEE")) && !paymentDetailsMap.get("BPA_BLDNG_OPRN_FEE").equals("0.0"))
		Stream.of("A. (i) Fee for building operation", paymentDetailsMap.get("BPA_BLDNG_OPRN_FEE"), "Paid").forEach(columnTitle -> {
			PdfPCell header = new PdfPCell();
			header.setBackgroundColor(ORANGE);
			header.setBorderWidth(2);
			header.setVerticalAlignment(Element.ALIGN_MIDDLE);
			header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
			table35.addCell(header);
		});
		if(StringUtils.isNotEmpty(paymentDetailsMap.get("BPA_LAND_DEV_FEE")) && !paymentDetailsMap.get("BPA_LAND_DEV_FEE").equals("0.0"))
			Stream.of("A. (ii) Development Fees", paymentDetailsMap.get("BPA_LAND_DEV_FEE"), "Paid").forEach(columnTitle -> {
				PdfPCell header = new PdfPCell();
				header.setBackgroundColor(ORANGE);
				header.setBorderWidth(2);
				header.setVerticalAlignment(Element.ALIGN_MIDDLE);
				header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
				table35.addCell(header);
			});
	}

	private void addTableRow35(PdfPTable table35, PdfPTable otherFeeReasonTable, Map<String, String> paymentDetailsMap,
			Map<String, Object> additionalDetails) {
		BaseColor grey = new BaseColor(216, 216, 216);
		BaseColor orange = new BaseColor(248, 203, 172);
		BaseColor lime = new BaseColor(226, 239, 217);
		BaseColor blue = new BaseColor(188, 214, 238);
		BaseColor pink = new BaseColor(251, 228, 213);
		BaseColor green = new BaseColor(197, 224, 178);
		BaseColor yellow = new BaseColor(255, 231, 154);
		BaseColor deepYellow = new BaseColor(255, 255, 0);
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 13, Font.BOLD);
		String[] seq="B C D E F G H I J K L M N O P Q R S T U V W X Y Z".split("\\s");
		java.util.List<String> list = new ArrayList<>(Arrays.asList(seq));
		Iterator<String> sequence = list.iterator();
		if(StringUtils.isNotEmpty(paymentDetailsMap.get("BPA_BLDNG_OPRN_FEE")) && !paymentDetailsMap.get("BPA_BLDNG_OPRN_FEE").equals("0.0"))
		Stream.of("A. (i) Fee for building operation", paymentDetailsMap.get("BPA_BLDNG_OPRN_FEE"), "Paid").forEach(columnTitle -> {
			PdfPCell header = new PdfPCell();
			header.setBackgroundColor(orange);
			header.setBorderWidth(2);
			header.setVerticalAlignment(Element.ALIGN_MIDDLE);
			header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
			table35.addCell(header);
		});
		if(StringUtils.isNotEmpty(paymentDetailsMap.get("BPA_LAND_DEV_FEE")) && !paymentDetailsMap.get("BPA_LAND_DEV_FEE").equals("0.0"))
			Stream.of("A. (ii) Development Fees", paymentDetailsMap.get("BPA_LAND_DEV_FEE"), "Paid").forEach(columnTitle -> {
				PdfPCell header = new PdfPCell();
				header.setBackgroundColor(orange);
				header.setBorderWidth(2);
				header.setVerticalAlignment(Element.ALIGN_MIDDLE);
				header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
				table35.addCell(header);
			});
		// the following sequence of tax head codes to be shown in permit letter, is
		// also used in the variable "taxHeadsToShowInOrder" in another method for
		// installment.Any change in this also should be done there.
		//Same for display names of taxheadcodes in method getFeeComponentNameFromTaxHeadCodeForPaymentTable.
		if(StringUtils.isNotEmpty(paymentDetailsMap.get("BPA_SANC_SANC_FEE")) && !paymentDetailsMap.get("BPA_SANC_SANC_FEE").equals("0.0")) {
		Stream.of(sequence.next() +". Sanction fees", paymentDetailsMap.get("BPA_SANC_SANC_FEE"), "Paid").forEach(columnTitle -> {
			PdfPCell header = new PdfPCell();
			header.setBackgroundColor(lime);
			header.setBorderWidth(2);
			header.setVerticalAlignment(Element.ALIGN_MIDDLE);
			header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
			table35.addCell(header);
		});
		}
	if (StringUtils.isNotEmpty(paymentDetailsMap.get("BPA_SANC_WORKER_WELFARE_CESS"))
			&& !paymentDetailsMap.get("BPA_SANC_WORKER_WELFARE_CESS").equals("0.0"))
			Stream.of(sequence.next() + ". Construction worker welfare Cess (CWWC)",
					paymentDetailsMap.get("BPA_SANC_WORKER_WELFARE_CESS"), "Paid").forEach(columnTitle -> {
			PdfPCell header = new PdfPCell();
			header.setBackgroundColor(blue);
			header.setBorderWidth(2);
			header.setVerticalAlignment(Element.ALIGN_MIDDLE);
			header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
			table35.addCell(header);
		});
		if (StringUtils.isNotEmpty(paymentDetailsMap.get("BPA_SANC_SECURITY_DEPOSIT"))
				&& !paymentDetailsMap.get("BPA_SANC_SECURITY_DEPOSIT").equals("0.0")) {
			Stream.of(sequence.next() + ". Security Deposit Fees", paymentDetailsMap.get("BPA_SANC_SECURITY_DEPOSIT"),
					"Paid").forEach(columnTitle -> {
						PdfPCell header = new PdfPCell();
						header.setBackgroundColor(lime);
						header.setBorderWidth(2);
						header.setVerticalAlignment(Element.ALIGN_MIDDLE);
						header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
						table35.addCell(header);
					});
		}
	if (StringUtils.isNotEmpty(paymentDetailsMap.get("BPA_SANC_TEMP_RETENTION_FEE"))
			&& !paymentDetailsMap.get("BPA_SANC_TEMP_RETENTION_FEE").equals("0.0"))
		Stream.of(sequence.next()+". Temporary Retention Fee", paymentDetailsMap.get("BPA_SANC_TEMP_RETENTION_FEE"), "Paid")
				.forEach(columnTitle -> {
			PdfPCell header = new PdfPCell();
			header.setBackgroundColor(pink);
			header.setBorderWidth(2);
			header.setVerticalAlignment(Element.ALIGN_MIDDLE);
			header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
			table35.addCell(header);
		});
		if(StringUtils.isNotEmpty(paymentDetailsMap.get("BPA_SANC_SHELTER_FEE")) && !paymentDetailsMap.get("BPA_SANC_SHELTER_FEE").equals("0.0"))
		Stream.of(
				sequence.next() + ". Shelter Fees for mandatory 10% EWS Housing (carpet area) @ 25% of construction cost of EWS housing ",
				paymentDetailsMap.get("BPA_SANC_SHELTER_FEE"), "Paid").forEach(columnTitle -> {
					PdfPCell header = new PdfPCell();
					header.setBackgroundColor(green);
					header.setBorderWidth(2);
					header.setVerticalAlignment(Element.ALIGN_MIDDLE);
					header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
					table35.addCell(header);
				});
		if(StringUtils.isNotEmpty(paymentDetailsMap.get("BPA_SANC_PUR_FAR")) && !paymentDetailsMap.get("BPA_SANC_PUR_FAR").equals("0.0"))
		Stream.of(sequence.next() + ". Charges for Purchasable FAR Area", paymentDetailsMap.get("BPA_SANC_PUR_FAR"), "Paid").forEach(columnTitle -> {
			PdfPCell header = new PdfPCell();
			header.setBackgroundColor(yellow);
			header.setBorderWidth(2);
			header.setVerticalAlignment(Element.ALIGN_MIDDLE);
			header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
			table35.addCell(header);
		});
		if(StringUtils.isNotEmpty(paymentDetailsMap.get("BPA_SANC_EIDP_FEE")) && !paymentDetailsMap.get("BPA_SANC_EIDP_FEE").equals("0.0"))
		Stream.of(sequence.next() + ". EIDP Fees ", paymentDetailsMap.get("BPA_SANC_EIDP_FEE"), "Paid").forEach(columnTitle -> {
			PdfPCell header = new PdfPCell();
			header.setBackgroundColor(orange);
			header.setBorderWidth(2);
			header.setVerticalAlignment(Element.ALIGN_MIDDLE);
			header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
			table35.addCell(header);
		});
	if (StringUtils.isNotEmpty(paymentDetailsMap.get(TAXHEAD_BPA_SANC_ADJUSTMENT_AMOUNT_CODE))
			&& !paymentDetailsMap.get(TAXHEAD_BPA_SANC_ADJUSTMENT_AMOUNT_CODE).equals("0.0")) {
			Stream.of(sequence.next() + ". Other Fee ", paymentDetailsMap.get(TAXHEAD_BPA_SANC_ADJUSTMENT_AMOUNT_CODE), "Paid")
					.forEach(columnTitle -> {
						PdfPCell header = new PdfPCell();
						header.setBackgroundColor(orange);
						header.setBorderWidth(2);
						header.setVerticalAlignment(Element.ALIGN_MIDDLE);
						header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
						table35.addCell(header);
					});
			PdfPCell header = new PdfPCell();
			header.setBackgroundColor(orange);
			header.setBorderWidth(2);
			header.setVerticalAlignment(Element.ALIGN_LEFT);
			header.setPhrase(new Phrase("Other Fee Detail: "+additionalDetails.get("modificationReasonSanctionFeeAdjustmentAmount"), fontPara1Bold));
			otherFeeReasonTable.addCell(header);
			otherFeeReasonTable.setWidthPercentage(100f);
		}
	
		if (StringUtils.isNotEmpty(paymentDetailsMap.get(TAXHEAD_BPA_SANC_ADJUSTMENT_AMOUNT_CODE_1))
				&& !paymentDetailsMap.get(TAXHEAD_BPA_SANC_ADJUSTMENT_AMOUNT_CODE_1).equals("0.0")) {
			 
			java.util.List otherFees = (java.util.List) additionalDetails.get("otherFees");
			String taxHeadName = (String) ((Map) otherFees.get(0)).get("reason");
			Stream.of(sequence.next() + ". "+ taxHeadName,
					paymentDetailsMap.get(TAXHEAD_BPA_SANC_ADJUSTMENT_AMOUNT_CODE_1), "Paid").forEach(columnTitle -> {
						PdfPCell header = new PdfPCell();
						header.setBackgroundColor(orange);
						header.setBorderWidth(2);
						header.setVerticalAlignment(Element.ALIGN_MIDDLE);
						header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
						table35.addCell(header);
					});
			PdfPCell header = new PdfPCell();
			header.setBackgroundColor(orange);
			header.setBorderWidth(2);
			header.setVerticalAlignment(Element.ALIGN_LEFT);
			header.setPhrase(new Phrase("Other Fee Detail: "
					+ additionalDetails.get("modificationReasonSanctionFeeAdjustmentAmount") != null
							? (String) additionalDetails.get("modificationReasonSanctionFeeAdjustmentAmount")
							: "NA",
					fontPara1Bold));
			otherFeeReasonTable.addCell(header);
			otherFeeReasonTable.setWidthPercentage(100f);
		}
		
		if (StringUtils.isNotEmpty(paymentDetailsMap.get(TAXHEAD_BPA_SANC_ADJUSTMENT_AMOUNT_CODE_2))
				&& !paymentDetailsMap.get(TAXHEAD_BPA_SANC_ADJUSTMENT_AMOUNT_CODE_2).equals("0.0")) {
			java.util.List otherFees = (java.util.List) additionalDetails.get("otherFees");
			String taxHeadName = (String) ((Map) otherFees.get(1)).get("reason");
			Stream.of(sequence.next() + ". "+ taxHeadName,
					paymentDetailsMap.get(TAXHEAD_BPA_SANC_ADJUSTMENT_AMOUNT_CODE_2), "Paid").forEach(columnTitle -> {
						PdfPCell header = new PdfPCell();
						header.setBackgroundColor(orange);
						header.setBorderWidth(2);
						header.setVerticalAlignment(Element.ALIGN_MIDDLE);
						header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
						table35.addCell(header);
					});
			PdfPCell header = new PdfPCell();
			header.setBackgroundColor(orange);
			header.setBorderWidth(2);
			header.setVerticalAlignment(Element.ALIGN_LEFT);
			header.setPhrase(new Phrase("Other Fee Detail: "
					+ additionalDetails.get("modificationReasonSanctionFeeAdjustmentAmount") != null
							? (String) additionalDetails.get("modificationReasonSanctionFeeAdjustmentAmount")
							: "NA",
					fontPara1Bold));
			otherFeeReasonTable.addCell(header);
			otherFeeReasonTable.setWidthPercentage(100f);
		}
		
		if (StringUtils.isNotEmpty(paymentDetailsMap.get(TAXHEAD_BPA_SANC_ADJUSTMENT_AMOUNT_CODE_3))
				&& !paymentDetailsMap.get(TAXHEAD_BPA_SANC_ADJUSTMENT_AMOUNT_CODE_3).equals("0.0")) {
			
			java.util.List otherFees = (java.util.List) additionalDetails.get("otherFees");
			String taxHeadName = (String) ((Map) otherFees.get(2)).get("reason");
			Stream.of(sequence.next() + ". "+ taxHeadName,
					paymentDetailsMap.get(TAXHEAD_BPA_SANC_ADJUSTMENT_AMOUNT_CODE_3), "Paid").forEach(columnTitle -> {
						PdfPCell header = new PdfPCell();
						header.setBackgroundColor(orange);
						header.setBorderWidth(2);
						header.setVerticalAlignment(Element.ALIGN_MIDDLE);
						header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
						table35.addCell(header);
					});
			PdfPCell header = new PdfPCell();
			header.setBackgroundColor(orange);
			header.setBorderWidth(2);
			header.setVerticalAlignment(Element.ALIGN_LEFT);
			header.setPhrase(new Phrase("Other Fee Detail: "
					+ additionalDetails.get("modificationReasonSanctionFeeAdjustmentAmount") != null
							? (String) additionalDetails.get("modificationReasonSanctionFeeAdjustmentAmount")
							: "NA",
					fontPara1Bold));
			otherFeeReasonTable.addCell(header);
			otherFeeReasonTable.setWidthPercentage(100f);
		}
			
	}

	private void addTableHeader34(PdfPTable table34) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 13, Font.BOLD);
		Stream.of("Remaining Fees Payable at DRDA, Khurda as per above", "30,33,750").forEach(columnTitle -> {
			PdfPCell header = new PdfPCell();
			header.setBackgroundColor(BaseColor.YELLOW);
			header.setBorderWidth(2);
			header.setVerticalAlignment(Element.ALIGN_MIDDLE);
			header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
			table34.addCell(header);
		});
	}

	private void addTableHeader33(PdfPTable table33) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 13, Font.BOLD);
		Stream.of("Remaining Fees Payable at BDA as per above", "1,34,50,448").forEach(columnTitle -> {
			PdfPCell header = new PdfPCell();
			header.setBackgroundColor(BaseColor.YELLOW);
			header.setBorderWidth(2);
			header.setVerticalAlignment(Element.ALIGN_MIDDLE);
			header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
			table33.addCell(header);
		});
	}

	private void addTableHeader32(PdfPTable table32) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 13, Font.BOLD);
		BaseColor deepYellow = new BaseColor(255, 255, 0);
		Stream.of("TOTAL FEES PAID AT DRDA, KHURDA", "10,11,250").forEach(columnTitle -> {
			PdfPCell header = new PdfPCell();
			header.setBackgroundColor(deepYellow);
			header.setBorderWidth(2);
			header.setVerticalAlignment(Element.ALIGN_MIDDLE);
			header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
			table32.addCell(header);
		});
	}

	private void addTableHeader31(PdfPTable table31, Map<String,String> paymentDetailsMap) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 13, Font.BOLD);
		BaseColor deepYellow = new BaseColor(255, 255, 0);
		Stream.of("TOTAL FEES PAID ", paymentDetailsMap.get("totalApplicationAndPermitFee")).forEach(columnTitle -> {
			PdfPCell header = new PdfPCell();
			header.setBackgroundColor(deepYellow);
			header.setBorderWidth(2);
			header.setVerticalAlignment(Element.ALIGN_MIDDLE);
			header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
			table31.addCell(header);
		});

	}
	
	private void addRemainingInstallmentAmountToBePaid(PdfPTable table31, Map<String, String> paymentDetailsMap) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 13, Font.BOLD);
		BaseColor deepYellow = new BaseColor(255, 255, 0);
		Stream.of("REMAINING FEES PAYABLE ", paymentDetailsMap.get("totalRemainingFeesFromInstallments"))
				.forEach(columnTitle -> {
					PdfPCell header = new PdfPCell();
					header.setBackgroundColor(deepYellow);
					header.setBorderWidth(2);
					header.setVerticalAlignment(Element.ALIGN_MIDDLE);
					header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
					table31.addCell(header);
				});
	}

	private void addTableHeader30(PdfPTable table30) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 13, Font.BOLD);
		Stream.of("Total payable fees towards EIDP", "40,45,000").forEach(columnTitle -> {
			PdfPCell header = new PdfPCell();
			header.setBackgroundColor(BaseColor.PINK);
			header.setBorderWidth(2);
			header.setVerticalAlignment(Element.ALIGN_MIDDLE);
			header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
			table30.addCell(header);
		});

	}

	private void addTableHeader294(PdfPTable table29) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 13, Font.BOLD);
		Stream.of("4th installment", "10,11,250",
				"To be paid at At DRDA, Khurda At the time of application of occupancy certificate")
				.forEach(columnTitle -> {
					PdfPCell header = new PdfPCell();
					header.setBackgroundColor(BaseColor.PINK);
					header.setBorderWidth(2);
					header.setVerticalAlignment(Element.ALIGN_MIDDLE);
					header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
					table29.addCell(header);
				});
	}

	private void addTableHeader293(PdfPTable table29) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 13, Font.BOLD);
		Stream.of("3rd installment", "10,11,250",
				"To be paid at At DRDA, KhurdaAt the time of Ground Floor Roof Casting").forEach(columnTitle -> {
					PdfPCell header = new PdfPCell();
					header.setBackgroundColor(BaseColor.PINK);
					header.setBorderWidth(2);
					header.setVerticalAlignment(Element.ALIGN_MIDDLE);
					header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
					table29.addCell(header);
				});
	}

	private void addTableHeader292(PdfPTable table29) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 13, Font.BOLD);
		Stream.of("2nd installment", "10,11,250", "To be paid at At DRDA, Khurda At the time of Plinth level")
				.forEach(columnTitle -> {
					PdfPCell header = new PdfPCell();
					header.setBackgroundColor(BaseColor.PINK);
					header.setBorderWidth(2);
					header.setVerticalAlignment(Element.ALIGN_MIDDLE);
					header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
					table29.addCell(header);
				});
	}

	private void addTableHeader291(PdfPTable table29) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 13, Font.BOLD);
		Stream.of("1st installment", "10,11,250", "Paid at At DRDA, Khurda").forEach(columnTitle -> {
			PdfPCell header = new PdfPCell();
			header.setBackgroundColor(BaseColor.PINK);
			header.setBorderWidth(2);
			header.setVerticalAlignment(Element.ALIGN_MIDDLE);
			header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
			table29.addCell(header);
		});

	}

	private void addTableHeader28(PdfPTable table28) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 13, Font.BOLD);
		Stream.of("G. EIDP Fees").forEach(columnTitle -> {
			PdfPCell header = new PdfPCell();
			header.setBackgroundColor(BaseColor.PINK);
			header.setBorderWidth(2);
			header.setVerticalAlignment(Element.ALIGN_MIDDLE);
			header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
			table28.addCell(header);
		});

	}

	private void addTableHeader27(PdfPTable table27) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 13, Font.BOLD);
		Stream.of("Total payable fees for purchasable FAR", "64,94,984").forEach(columnTitle -> {
			PdfPCell header = new PdfPCell();
			header.setBackgroundColor(BaseColor.LIGHT_GRAY);
			header.setBorderWidth(2);
			header.setVerticalAlignment(Element.ALIGN_MIDDLE);
			header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
			table27.addCell(header);
		});
	}

	private void addTableHeader264(PdfPTable table26) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 13, Font.BOLD);
		Stream.of("4th installment", "16,23,746", "To be paid At the time of application of occupancy certificate")
				.forEach(columnTitle -> {
					PdfPCell header = new PdfPCell();
					header.setBackgroundColor(BaseColor.LIGHT_GRAY);
					header.setBorderWidth(2);
					header.setVerticalAlignment(Element.ALIGN_MIDDLE);
					header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
					table26.addCell(header);
				});

	}

	private void addTableHeader263(PdfPTable table26) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 13, Font.BOLD);
		Stream.of("3rd installment", "16,23,746", "To be paid At the time of Ground Floor Roof Casting")
				.forEach(columnTitle -> {
					PdfPCell header = new PdfPCell();
					header.setBackgroundColor(BaseColor.LIGHT_GRAY);
					header.setBorderWidth(2);
					header.setVerticalAlignment(Element.ALIGN_MIDDLE);
					header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
					table26.addCell(header);
				});

	}

	private void addTableHeader262(PdfPTable table26) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 13, Font.BOLD);
		Stream.of("2nd installment", "16,23,746", "To be paid At the time of Plinth level").forEach(columnTitle -> {
			PdfPCell header = new PdfPCell();
			header.setBackgroundColor(BaseColor.LIGHT_GRAY);
			header.setBorderWidth(2);
			header.setVerticalAlignment(Element.ALIGN_MIDDLE);
			header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
			table26.addCell(header);
		});

	}

	private void addTableHeader261(PdfPTable table26) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 13, Font.BOLD);
		Stream.of("1st installment", "16,23,746", "Paid").forEach(columnTitle -> {
			PdfPCell header = new PdfPCell();
			header.setBackgroundColor(BaseColor.LIGHT_GRAY);
			header.setBorderWidth(2);
			header.setVerticalAlignment(Element.ALIGN_MIDDLE);
			header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
			table26.addCell(header);
		});

	}

	private void addTableHeader25(PdfPTable table25) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 13, Font.BOLD);
		Stream.of("F. Charges for Purchasable FAR Area").forEach(columnTitle -> {
			PdfPCell header = new PdfPCell();
			header.setBackgroundColor(BaseColor.LIGHT_GRAY);
			header.setBorderWidth(2);
			header.setVerticalAlignment(Element.ALIGN_MIDDLE);
			header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
			table25.addCell(header);
		});

	}

	private void addTableHeader24(PdfPTable table24) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 13, Font.BOLD);
		Stream.of("Total Payable Shelter Fees", "68,42,390").forEach(columnTitle -> {
			PdfPCell header = new PdfPCell();
			header.setBackgroundColor(BaseColor.GREEN);
			header.setBorderWidth(2);
			header.setVerticalAlignment(Element.ALIGN_MIDDLE);
			header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
			table24.addCell(header);
		});
	}

	private void addTableHeader234(PdfPTable table23) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 13, Font.BOLD);
		Stream.of("4th installment", "17,10,597", "To be paid before three years of issue of permission letter")
				.forEach(columnTitle -> {
					PdfPCell header = new PdfPCell();
					header.setBackgroundColor(BaseColor.GREEN);
					header.setBorderWidth(2);
					header.setVerticalAlignment(Element.ALIGN_MIDDLE);
					header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
					table23.addCell(header);
				});

	}

	private void addTableHeader233(PdfPTable table23) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 13, Font.BOLD);
		Stream.of("3rd installment", "17,10,597", "To be paid before two years of issue of permission letter")
				.forEach(columnTitle -> {
					PdfPCell header = new PdfPCell();
					header.setBackgroundColor(BaseColor.GREEN);
					header.setBorderWidth(2);
					header.setVerticalAlignment(Element.ALIGN_MIDDLE);
					header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
					table23.addCell(header);
				});

	}

	private void addTableHeader232(PdfPTable table23) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 13, Font.BOLD);
		Stream.of("2nd installment", "17,10,597", "To be paid before one year of issue of permission letter")
				.forEach(columnTitle -> {
					PdfPCell header = new PdfPCell();
					header.setBackgroundColor(BaseColor.GREEN);
					header.setBorderWidth(2);
					header.setVerticalAlignment(Element.ALIGN_MIDDLE);
					header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
					table23.addCell(header);
				});

	}

	private void addTableHeader231(PdfPTable table23) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 13, Font.BOLD);
		Stream.of("1st installment", "17,10,597", "Paid").forEach(columnTitle -> {
			PdfPCell header = new PdfPCell();
			header.setBackgroundColor(BaseColor.GREEN);
			header.setBorderWidth(2);
			header.setVerticalAlignment(Element.ALIGN_MIDDLE);
			header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
			table23.addCell(header);
		});

	}

	private void addTableHeader22(PdfPTable table22) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 13, Font.BOLD);
		Stream.of(
				"E. Shelter Fees for mandatory 10% EWS Housing (carpetarea) @ 25% of construction cost of EWS housing")
				.forEach(columnTitle -> {
					PdfPCell header = new PdfPCell();
					header.setBackgroundColor(BaseColor.GREEN);
					header.setBorderWidth(2);
					header.setVerticalAlignment(Element.ALIGN_MIDDLE);
					header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
					table22.addCell(header);
				});

	}

	private void addTableHeader21(PdfPTable table21) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 13, Font.BOLD);
		Stream.of("D. Rates of Compounding Charges for Unauthorized	Layouts", "5,82,270", "Paid")
				.forEach(columnTitle -> {
					PdfPCell header = new PdfPCell();
					header.setBackgroundColor(BaseColor.PINK);
					header.setBorderWidth(2);
					header.setVerticalAlignment(Element.ALIGN_MIDDLE);
					header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
					table21.addCell(header);
				});

	}

	private void addTableHeader20(PdfPTable table20) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 13, Font.BOLD);
		Stream.of("Total Payable CWWC Fees", "51,71,126").forEach(columnTitle -> {
			PdfPCell header = new PdfPCell();
			header.setBackgroundColor(BaseColor.CYAN);
			header.setBorderWidth(2);
			header.setVerticalAlignment(Element.ALIGN_MIDDLE);
			header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
			table20.addCell(header);
		});

	}

	private void addTableHeader193(PdfPTable table19) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 13, Font.BOLD);
		Stream.of("3rd installment", "17,23,709", "To be paid before two years of issue of permission letter")
				.forEach(columnTitle -> {
					PdfPCell header = new PdfPCell();
					header.setBackgroundColor(BaseColor.CYAN);
					header.setBorderWidth(2);
					header.setVerticalAlignment(Element.ALIGN_MIDDLE);
					header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
					table19.addCell(header);
				});

	}

	private void addTableHeader192(PdfPTable table19) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 13, Font.BOLD);
		Stream.of("2nd installment", "17,23,709", "To be paid before one year of issue of permission letter")
				.forEach(columnTitle -> {
					PdfPCell header = new PdfPCell();
					header.setBackgroundColor(BaseColor.CYAN);
					header.setBorderWidth(2);
					header.setVerticalAlignment(Element.ALIGN_MIDDLE);
					header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
					table19.addCell(header);
				});

	}

	private void addTableHeader191(PdfPTable table19) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 13, Font.BOLD);
		Stream.of("1st installment", "17,23,709", "Paid").forEach(columnTitle -> {
			PdfPCell header = new PdfPCell();
			header.setBackgroundColor(BaseColor.CYAN);
			header.setBorderWidth(2);
			header.setVerticalAlignment(Element.ALIGN_MIDDLE);
			header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
			table19.addCell(header);
		});

	}

	private void addTableHeader18(PdfPTable table18) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 13, Font.BOLD);
		Stream.of("C. Construction worker welfare Cess (CWWC)").forEach(columnTitle -> {
			PdfPCell header = new PdfPCell();
			header.setBackgroundColor(BaseColor.CYAN);
			header.setBorderWidth(2);
			header.setVerticalAlignment(Element.ALIGN_MIDDLE);
			header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
			table18.addCell(header);
		});

	}

	private void addTableHeader173(PdfPTable table17) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 13, Font.BOLD);
		Stream.of("B. Sanction fees", "13,72,598", "Paid").forEach(columnTitle -> {
			PdfPCell header = new PdfPCell();
			header.setBackgroundColor(BaseColor.GREEN);
			header.setBorderWidth(2);
			header.setVerticalAlignment(Element.ALIGN_MIDDLE);
			header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
			table17.addCell(header);
		});

	}

	private void addTableHeader172(PdfPTable table17) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 13, Font.BOLD);
		Stream.of("A (ii) Fee for building operation", "2,61,068", "Paid").forEach(columnTitle -> {
			PdfPCell header = new PdfPCell();
			header.setBackgroundColor(BaseColor.ORANGE);
			header.setBorderWidth(2);
			header.setVerticalAlignment(Element.ALIGN_MIDDLE);
			header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
			table17.addCell(header);
		});

	}

	private void addTableHeader171(PdfPTable table17) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 13, Font.BOLD);
		Stream.of("A. (i) Development Fees", "50,731", "Paid").forEach(columnTitle -> {
			PdfPCell header = new PdfPCell();
			header.setBackgroundColor(BaseColor.ORANGE);
			header.setBorderWidth(2);
			header.setVerticalAlignment(Element.ALIGN_MIDDLE);
			header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
			table17.addCell(header);
		});

	}

	private void addTableHeader16(PdfPTable table16) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 13, Font.BOLD);
		BaseColor grey = new BaseColor(216, 216, 216);
		Stream.of("Details of Fees and Charges", "Amount in Rupees", "Payment Status").forEach(columnTitle -> {
			PdfPCell header = new PdfPCell();
			header.setBackgroundColor(grey);
			header.setBorderWidth(2);
			header.setVerticalAlignment(Element.ALIGN_MIDDLE);
			header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
			table16.addCell(header);
		});

	}

	private void addTableHeader152(PdfPTable table15, Plan plan) {
		for(Block block:plan.getBlocks()) {
			Map<String, BigDecimal> setBackData = getSetBackData(plan,block);
			BigDecimal frontSetbackProvided = setBackData.get("frontSetbackProvided");
			BigDecimal rearSetbackProvided = setBackData.get("rearSetbackProvided");
			BigDecimal leftSetbackProvided = setBackData.get("leftSetbackProvided");
			BigDecimal rightSetbackProvided = setBackData.get("rightSetbackProvided");

			addCellForSetBackTable(table15, block.getName(), 4);
			addCellForSetBackTable(table15, "Front Set back", null);
			addCellForSetBackTable(table15, frontSetbackProvided + "", null);
			addCellForSetBackTable(table15, "Rear Set back", null);
			addCellForSetBackTable(table15, rearSetbackProvided + "", null);
			addCellForSetBackTable(table15, "Left side", null);
			addCellForSetBackTable(table15, leftSetbackProvided + "", null);
			addCellForSetBackTable(table15, "Right side", null);
			addCellForSetBackTable(table15, rightSetbackProvided + "", null);
		}
	}
	
	private void addCellForSetBackTable(PdfPTable table15, String displayContent,Integer rowspan) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 13, Font.BOLD);
		PdfPCell header = new PdfPCell();
		header.setVerticalAlignment(Element.ALIGN_MIDDLE);
		header.setPhrase(new Phrase(displayContent, fontPara1Bold));
		if(Objects.nonNull(rowspan))
			header.setRowspan(rowspan);
		table15.addCell(header);
	}

	private void addTableHeader151(PdfPTable table15) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 13, Font.BOLD);
		Stream.of("Block No.", "Item", "Provided (in Mtr)").forEach(columnTitle -> {
			PdfPCell header = new PdfPCell();
			header.setBorderWidth(2);
			header.setVerticalAlignment(Element.ALIGN_MIDDLE);
			header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
			table15.addCell(header);
		});
	}

	private void addRows141(PdfPTable table14, OdishaParkingHelper parkingData) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 12, Font.BOLD);
		PdfPCell pdfWordCell1 = new PdfPCell();
		Phrase firstLine = new Phrase("Parking", fontPara1Bold);
		pdfWordCell1.addElement(firstLine);
		pdfWordCell1.setVerticalAlignment(Element.ALIGN_MIDDLE);
		pdfWordCell1.setBorderWidth(2);
		table14.addCell(pdfWordCell1);
		PdfPCell pdfWordCell3 = new PdfPCell();

		BigDecimal stiltParking = parkingData.stiltParkingProvided;
		BigDecimal basementParking = parkingData.basementParkingProvided;
		BigDecimal openParking = parkingData.openParkingProvided;
		BigDecimal totalParking = parkingData.totalParkingProvided;
		Chunk chunk1 = new Chunk("Basement-" + basementParking + "+	  Stilt- " + stiltParking
				+ " + Ground (Open Parking )-" + openParking);
		Chunk chunk2 = new Chunk("   Total =" + totalParking + " Sqm.", fontPara1Bold);
		Phrase thirdLine = new Phrase();
		thirdLine.add(chunk1);
		thirdLine.add(chunk2);
		pdfWordCell3.addElement(thirdLine);
		pdfWordCell3.setBorderWidth(2);
		table14.addCell(pdfWordCell3);

	}

	private void addRows131(PdfPTable table13, Plan plan) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 13, Font.BOLD);
		// TODO what height
		Stream.of("Height (mtr.)", getHeight(plan)).forEach(columnTitle -> {
			PdfPCell header = new PdfPCell();
			header.setBorderWidth(2);
			header.setVerticalAlignment(Element.ALIGN_MIDDLE);
			header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
			table13.addCell(header);
		});

	}

	private void addRows121(PdfPTable table12, Plan plan) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 12, Font.BOLD);
		PdfPCell pdfWordCell1 = new PdfPCell();
		Phrase firstLine = new Phrase("F.A.R", fontPara1Bold);
		pdfWordCell1.addElement(firstLine);
		pdfWordCell1.setVerticalAlignment(Element.ALIGN_MIDDLE);
		pdfWordCell1.setBorderWidth(2);
		table12.addCell(pdfWordCell1);
		PdfPCell pdfWordCell2 = new PdfPCell();
		Phrase secondLine = new Phrase(plan.getFarDetails().getPermissableFar() + " (Max. Permissible)\n"
				+ plan.getFarDetails().getBaseFar() + " (Base FAR )");
		pdfWordCell2.addElement(secondLine);
		pdfWordCell2.setVerticalAlignment(Element.ALIGN_MIDDLE);
		pdfWordCell2.setBorderWidth(2);
		table12.addCell(pdfWordCell2);
		PdfPCell pdfWordCell3 = new PdfPCell();
//		BigDecimal deltaFAR = (BigDecimal.valueOf(plan.getFarDetails().getProvidedFar())
//				.subtract(BigDecimal.valueOf(plan.getFarDetails().getBaseFar()))).setScale(2, BigDecimal.ROUND_HALF_UP);
		
		//base far null validation added.
		BigDecimal providedFar = Optional.ofNullable(plan.getFarDetails()).map(details -> details.getProvidedFar())
				.map(BigDecimal::valueOf).orElse(BigDecimal.ZERO);

		BigDecimal baseFar = Optional.ofNullable(plan.getFarDetails()).map(details -> details.getBaseFar())
				.map(BigDecimal::valueOf).orElse(BigDecimal.ZERO);

		BigDecimal deltaFAR = providedFar.subtract(baseFar).setScale(2, BigDecimal.ROUND_HALF_UP);

		// tdr relaxation- decrease deltaFar based on tdrFarRelaxation-
		if (null != plan.getFarDetails().getTdrFarRelaxation()) {
			deltaFAR = deltaFAR.subtract(new BigDecimal(plan.getFarDetails().getTdrFarRelaxation())).setScale(2,
					BigDecimal.ROUND_UP);
		}
		if (deltaFAR.compareTo(BigDecimal.ZERO) < 0) {
			deltaFAR = BigDecimal.ZERO;
		}
		
		BigDecimal achievedFar = BigDecimal.valueOf(plan.getFarDetails().getProvidedFar())
				.setScale(DcrConstants.DECIMALDIGITS_MEASUREMENTS, DcrConstants.ROUNDMODE_MEASUREMENTS);

		Chunk chunk1 = new Chunk("ACHIEVED- " + achievedFar, fontPara1Bold);
		Chunk chunk2 = new Chunk("(" + deltaFAR + " Purchasable FAR)");
		Phrase thirdLine = new Phrase();
		thirdLine.add(chunk1);
		thirdLine.add(chunk2);
		pdfWordCell3.addElement(thirdLine);
		pdfWordCell3.setBorderWidth(2);
		table12.addCell(pdfWordCell3);
	}

	private void addTableHeader11(PdfPTable table11, Plan plan) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 13, Font.BOLD);
		BigDecimal grandTotalBuiltupArea = plan.getVirtualBuilding() != null
				? plan.getVirtualBuilding().getTotalBuitUpArea().setScale(DcrConstants.DECIMALDIGITS_MEASUREMENTS,
						DcrConstants.ROUNDMODE_MEASUREMENTS)
				: BigDecimal.ZERO;
		Stream.of("Grand Total BUA - " + grandTotalBuiltupArea + "" + " Sqm.").forEach(columnTitle -> {
			PdfPCell header = new PdfPCell();
			header.setBorderWidth(2);
			header.setVerticalAlignment(Element.ALIGN_MIDDLE);
			header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
			table11.addCell(header);
		});

	}

	private void addTableHeader10(PdfPTable table10, Plan plan) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 13, Font.BOLD);

		BigDecimal providedFar = BigDecimal.valueOf(plan.getFarDetails().getProvidedFar());
		//String grandTotalFarArea = plan.getPlot().getArea().multiply(providedFar).setScale(2, BigDecimal.ROUND_UP) + "";
		String grandTotalFarArea = plan.getVirtualBuilding().getTotalFloorArea().setScale(2, BigDecimal.ROUND_UP) + "";
		Stream.of("Grand Total FAR Area - " + grandTotalFarArea + " Sqm.").forEach(columnTitle -> {
			PdfPCell header = new PdfPCell();
			header.setBorderWidth(2);
			header.setVerticalAlignment(Element.ALIGN_MIDDLE);
			header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
			table10.addCell(header);
		});

	}

	private void addTableHeader9(PdfPTable table4) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 12, Font.BOLD);
		Stream.of("Plantation(no of tree per 80Sqm.)", "91Nos", "93Nos.").forEach(columnTitle -> {
			PdfPCell header = new PdfPCell();
			header.setBorderWidth(2);
			header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
			table4.addCell(header);
		});

	}

	private void addTableHeader8(PdfPTable table4, OdishaParkingHelper parkingData) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 12, Font.BOLD);
		Stream.of("Visitor parking", parkingData.visitorParkingRequired + "", parkingData.visitorParkingProvided + "")
				.forEach(columnTitle -> {
					PdfPCell header = new PdfPCell();
					header.setBorderWidth(2);
					header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
					table4.addCell(header);
				});

	}

	private void addTableHeader7(PdfPTable table4) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 12, Font.BOLD);
		Stream.of("E-vehicle charging station", "0", "0").forEach(columnTitle -> {
			PdfPCell header = new PdfPCell();
			header.setBorderWidth(2);
			header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
			table4.addCell(header);
		});

	}

	private void addTableHeader6(PdfPTable table4, Plan plan, OdishaParkingHelper parkingData) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 12, Font.BOLD);

		Stream.of("Bye Laws Provisions", "Required", "Proposed").forEach(columnTitle -> {
			PdfPCell header = new PdfPCell();
			header.setBorderWidth(2);
			header.setBackgroundColor(BaseColor.LIGHT_GRAY);
			header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
			table4.addCell(header);
		});
		Map<String, String> liftDetail = getLiftDetails(plan);
		//show provided stair count as required as the method .GeneralStair.requiredGenralStairPerFloor(plan, block) giving 0 count
		//Stream.of("No.of staircases", getRequiredStairCount(plan), getStairCount(plan))
		Stream.of("No.of staircases", getStairCount(plan), getStairCount(plan))
				.forEach(columnTitle -> {
					PdfPCell header = new PdfPCell();
					header.setBorderWidth(2);
					header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
					table4.addCell(header);
				});
		Stream.of("No.of Lifts", liftDetail.get(REQUIRED_LIFT_DETAIL), liftDetail.get(PROVIDED_LIFT_DETAIL))
				.forEach(columnTitle -> {
					PdfPCell header = new PdfPCell();
					header.setBorderWidth(2);
					header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
					table4.addCell(header);
				});
		Stream.of("E-vehicle charging station", getChargingStationsRequired(plan), getChargingStationsProvided(plan))
				.forEach(columnTitle -> {
					PdfPCell header = new PdfPCell();
					header.setBorderWidth(2);
					header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
					table4.addCell(header);
				});
		Stream.of("Visitor parking(in Sqm.)", parkingData.visitorParkingRequired + "",
				parkingData.visitorParkingProvided + "").forEach(columnTitle -> {
					PdfPCell header = new PdfPCell();
					header.setBorderWidth(2);
					header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
					table4.addCell(header);
				});
		Stream.of("Plantation(no of tree per 80Sqm.)", getNoOfTreesRequired(plan), getNoOfTreesProvided(plan))
				.forEach(columnTitle -> {
					PdfPCell header = new PdfPCell();
					header.setBorderWidth(2);
					header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
					table4.addCell(header);
				});
	}

	private void addTableRow5(PdfPTable table4) {
		Font fontPara1 = FontFactory.getFont(FontFactory.COURIER, 12);
		Stream.of("Basement Area (Envelope Basement Area)", "5304.53 Sqm.",
				"Parking Area =4787.55Sqm. Service Area=516.98 Sqm. ").forEach(columnTitle -> {
					PdfPCell header = new PdfPCell();
					header.setPhrase(new Phrase(columnTitle, fontPara1));
					table4.addCell(header);
				});

	}

	private void addRows45(PdfPTable table) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 12, Font.BOLD);
		PdfPCell pdfWordCell1 = new PdfPCell();
		Phrase firstLine = new Phrase("Total FAR Area", fontPara1Bold);
		pdfWordCell1.addElement(firstLine);
		pdfWordCell1.setVerticalAlignment(Element.ALIGN_MIDDLE);
		table.addCell(pdfWordCell1);
		PdfPCell pdfWordCell2 = new PdfPCell();
		Phrase secondLine = new Phrase("1051.50 Sqm.", fontPara1Bold);
		pdfWordCell2.addElement(secondLine);
		pdfWordCell2.setVerticalAlignment(Element.ALIGN_MIDDLE);
		table.addCell(pdfWordCell2);

	}

	private void addRows44(PdfPTable table) {
		table.addCell("Third Floor");
		table.addCell("258.47 Sqm");

	}

	private void addRows43(PdfPTable table) {
		table.addCell("Second floor");
		table.addCell("258.47 Sqm");

	}

	private void addRows42(PdfPTable table) {
		table.addCell("First floor");
		table.addCell("258.47 Sqm");
	}

	private void addRows41(PdfPTable table) {
		table.addCell("Ground floor");
		table.addCell("276.09 Sqm");

	}

	private void addTableHeader4(PdfPTable table4) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 12, Font.BOLD);
		Stream.of("Community Building (B+G+3)", "Covered area approved").forEach(columnTitle -> {
			PdfPCell header = new PdfPCell();
			header.setBackgroundColor(BaseColor.LIGHT_GRAY);
			header.setBorderWidth(2);
			header.setFixedHeight(25f);
			header.setVerticalAlignment(Element.ALIGN_MIDDLE);
			header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
			table4.addCell(header);

		});

	}

	private void addRows31(PdfPTable table, Plan plan) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 12, Font.BOLD);
		PdfPCell pdfWordCell1 = new PdfPCell();
		Phrase firstLine = new Phrase(
				"Total no. of Dwelling Units -" + plan.getPlanInformation().getTotalNoOfDwellingUnits(), fontPara1Bold);
		pdfWordCell1.addElement(firstLine);
		pdfWordCell1.setVerticalAlignment(Element.ALIGN_MIDDLE);
		table.addCell(pdfWordCell1);

	}

	private void addRows25(PdfPTable table) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 12, Font.BOLD);
		PdfPCell pdfWordCell1 = new PdfPCell();
		Phrase firstLine = new Phrase("Total FAR Area", fontPara1Bold);
		pdfWordCell1.addElement(firstLine);
		pdfWordCell1.setVerticalAlignment(Element.ALIGN_MIDDLE);
		table.addCell(pdfWordCell1);
		PdfPCell pdfWordCell2 = new PdfPCell();
		Phrase secondLine = new Phrase("10,246.05 Sqm.", fontPara1Bold);
		pdfWordCell2.addElement(secondLine);
		pdfWordCell2.setVerticalAlignment(Element.ALIGN_MIDDLE);
		table.addCell(pdfWordCell2);
		PdfPCell pdfWordCell3 = new PdfPCell();
		Phrase thirdLine = new Phrase(" ", fontPara1Bold);
		pdfWordCell3.addElement(thirdLine);
		pdfWordCell3.setVerticalAlignment(Element.ALIGN_MIDDLE);
		table.addCell(pdfWordCell3);
		PdfPCell pdfWordCell4 = new PdfPCell();
		Phrase fourthLine = new Phrase("74Nos.", fontPara1Bold);
		pdfWordCell4.addElement(fourthLine);
		pdfWordCell4.setVerticalAlignment(Element.ALIGN_MIDDLE);
		table.addCell(pdfWordCell4);

	}

	private void addRows24(PdfPTable table) {
		table.addCell("Ground floor");
		table.addCell("748.62 Sqm");
		table.addCell("Parking + Service + Residential");
		table.addCell("04(Four) Nos.");

	}

	private void addRows23(PdfPTable table) {
		table.addCell("Ground floor");
		table.addCell("748.62 Sqm");
		table.addCell("Parking + Service + Residential");
		table.addCell("04(Four) Nos.");

	}

	private void addRows22(PdfPTable table) {
		table.addCell("Ground floor");
		table.addCell("748.62 Sqm");
		table.addCell("Parking + Service + Residential");
		table.addCell("04(Four) Nos.");

	}

	private void addRows21(PdfPTable table) {
		table.addCell("Ground floor");
		table.addCell("748.62 Sqm");
		table.addCell("Parking + Service + Residential");
		table.addCell("04(Four) Nos.");

	}


	private BigDecimal addRowsPerFloorAndAggregateFlrAreas(PdfPTable table, DcrReportFloorDetail floor,
			BigDecimal totalFloorArea,Floor planFloor, String subService) {
		PdfPCell floorNameCell = new PdfPCell();
		Phrase floorNamephrase = new Phrase(floor.getFloorNo());
		floorNameCell.addElement(floorNamephrase);
		table.addCell(floorNameCell);
		PdfPCell floorAreaCell = new PdfPCell();
		Phrase floorAreaphrase = new Phrase(floor.getBuiltUpArea() + "");
		floorAreaCell.addElement(floorAreaphrase);
		table.addCell(floorAreaCell);
		if (Objects.nonNull(subService) && !ALTERATION_SUBSERVICE_A.equals(subService)) {
			PdfPCell floorExistingAreaCell = new PdfPCell();
			Phrase floorExistingAreaphrase = new Phrase(getExistingFloorBuiltupArea(floor) + "");
			floorExistingAreaCell.addElement(floorExistingAreaphrase);
			table.addCell(floorExistingAreaCell);
		}
		PdfPCell floorOccupancyCell = new PdfPCell();
		Phrase floorOccupancyphrase = new Phrase(floor.getOccupancy());
		floorOccupancyCell.addElement(floorOccupancyphrase);
		table.addCell(floorOccupancyCell);
		PdfPCell floorUnitsCell = new PdfPCell();
		Phrase floorUnitsphrase = new Phrase(totalDUForTable(planFloor, floor)+"");
		floorUnitsCell.addElement(floorUnitsphrase);
		table.addCell(floorUnitsCell);

		totalFloorArea = totalFloorArea.add(floor.getBuiltUpArea());
		return totalFloorArea;

	}
	
	private int totalDU(Floor floor) {
		return floor.getEwsUnit().size()+floor.getLigUnit().size()+floor.getMig1Unit().size()+floor.getMig2Unit().size()+floor.getOthersUnit().size()+floor.getRoomUnit().size();
	}
	
	private int totalDUForTable(Floor floor, DcrReportFloorDetail floorDetail) {

		java.util.List<String> validOccupancyList = Arrays.asList("Residential Plotted",
				"Plotted Detached or Individual Residential Building",
				"EWS", "Low Income Housing", "Semi Detached", "Row Housing", "Apartment Building", "Housing Project",
				"Work Cum Residential", "Studio Apartments", "Dharmasala", "Dormitory", "Medium Income Housing",
				"Hostel", "Shelter House", "Staff Qaurter", "Hotel", "Five Star Hotel", "Motels", "Resorts",
				"Lagoons and Lagoon Resort", "Guest Houses", "Holiday Resort", "Boarding and Lodging Houses", "Orphanage",
				"Old Age Home","Marriage Hall/Kalyan Mandap","Govt-Semi Govt. Hospital", "Hospital", "Medical Building", "Nursing Home", 
				"Veterinary Hospital for pet animals and birds", "Rehabilitaion Center", "Country Homes", "Farm House", "Hostel (Captive)");

		if (floorDetail.getOccupancy() != null && validOccupancyList.contains(floorDetail.getOccupancy())) {
			return floor.getEwsUnit().size() + floor.getLigUnit().size() + floor.getMig1Unit().size()
					+ floor.getMig2Unit().size() + floor.getOthersUnit().size() + floor.getRoomUnit().size();
		} else {
			return 0;
		}

	}

	private void addTotalRow(PdfPTable table, BigDecimal totalFloorArea,Block planBlock, String subService) {
		int totalDuInBlock = 0;
		for(Floor floor:planBlock.getBuilding().getFloors()) {
			totalDuInBlock +=totalDU(floor);
		}
		table.addCell("Total BUA Area");
		table.addCell(totalFloorArea + "");
		if (Objects.nonNull(subService) && !ALTERATION_SUBSERVICE_A.equals(subService)) {
			// add empty cell if extra column added for existing area in alteration subservice B,C,D-
			table.addCell(" ");
		}
		table.addCell(" ");
		table.addCell(totalDuInBlock+"");
	}

	private void addRows13(PdfPTable table) {
		table.addCell("Second floor");
		table.addCell("677.55 Sqm");
		table.addCell("Residential");
		table.addCell("05 (Five) Nos");
	}

	private void addRows14(PdfPTable table) {
		table.addCell("3rd Floor to 14th Floor");
		table.addCell("676.74 * 12 = 8120.88Sqm");
		table.addCell("Residential");
		table.addCell("05*12 =60(Sixty) Nos.");
	}

	private void addRows15(PdfPTable table) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 12, Font.BOLD);
		PdfPCell pdfWordCell1 = new PdfPCell();
		Phrase firstLine = new Phrase("Total FAR Area", fontPara1Bold);
		pdfWordCell1.addElement(firstLine);
		pdfWordCell1.setVerticalAlignment(Element.ALIGN_MIDDLE);
		table.addCell(pdfWordCell1);
		PdfPCell pdfWordCell2 = new PdfPCell();
		Phrase secondLine = new Phrase("10,246.05 Sqm.", fontPara1Bold);
		pdfWordCell2.addElement(secondLine);
		pdfWordCell2.setVerticalAlignment(Element.ALIGN_MIDDLE);
		table.addCell(pdfWordCell2);
		PdfPCell pdfWordCell3 = new PdfPCell();
		Phrase thirdLine = new Phrase(" ", fontPara1Bold);
		pdfWordCell3.addElement(thirdLine);
		pdfWordCell3.setVerticalAlignment(Element.ALIGN_MIDDLE);
		table.addCell(pdfWordCell3);
		PdfPCell pdfWordCell4 = new PdfPCell();
		Phrase fourthLine = new Phrase("74Nos.", fontPara1Bold);
		pdfWordCell4.addElement(fourthLine);
		pdfWordCell4.setVerticalAlignment(Element.ALIGN_MIDDLE);
		table.addCell(pdfWordCell4);
	}
	AdditionalFeature additionalFeature = new AdditionalFeature();
	private void addTableHeader1(PdfPTable table1, DcrReportBlockDetail block,Plan plan, String subService) {

		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 12, Font.BOLD);
		String noOfFloor = additionalFeature.getNoOfFloor(plan.getBlockByName(block.getBlockNo()));
		if(Objects.nonNull(subService)&&!ALTERATION_SUBSERVICE_A.equals(subService)) {
			// for alteration subservices other than subservice A, show one extra column for existing area-
			Stream.of("Block-No." + block.getBlockNo() + " (" + noOfFloor + ")", "Covered area approved -Proposed(Sqm.)",
					"Covered area approved -Existing(Sqm.)", "Proposed use", "No. of Dwelling Units")
			.forEach(columnTitle -> {
				PdfPCell header = new PdfPCell();
				header.setBackgroundColor(BaseColor.LIGHT_GRAY);
				header.setBorderWidth(2);
				header.setVerticalAlignment(Element.ALIGN_MIDDLE);
				header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
				table1.addCell(header);

			});
		}
		else {
			Stream.of("Block-No." + block.getBlockNo()+ " (" + noOfFloor + ")", "Covered area approved (Sqm.)", "Proposed use", "No. of Dwelling Units")
			.forEach(columnTitle -> {
				PdfPCell header = new PdfPCell();
				header.setBackgroundColor(BaseColor.LIGHT_GRAY);
				header.setBorderWidth(2);
				header.setVerticalAlignment(Element.ALIGN_MIDDLE);
				header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
				table1.addCell(header);

			});
		}
		
	}

	private void addNocs(Document document, RequestInfo requestInfo, String tenantId, String applicationNo)
			throws DocumentException {
		Set<String> nocNames = getNocsList(requestInfo, tenantId, applicationNo);
		if (nocNames.isEmpty()) {
			Phrase phrase = new Phrase();
			phrase.add(new Chunk(Chunk.TABBING));
//			phrase.add(new Chunk(Chunk.TAB));
			phrase.add(new Chunk("NA\n", FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD)));
			document.add(phrase);
		}
		for (String nocName : nocNames) {
			Phrase phrase = new Phrase();
			phrase.add(new Chunk(Chunk.TABBING));
//			phrase.add(new Chunk(Chunk.TAB));
			phrase.add(new Chunk("	- " + nocName + ".\n", FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD)));
			document.add(phrase);
		}
	}

	private String getChargingStationsRequired(Plan plan) {
		return "0";
	}

	private String getChargingStationsProvided(Plan plan) {
		return "0";
	}
	
	private  PdfPTable getInstallmentCellForFeeComponentName(String feeComponentName, BaseColor backgroundColor) {
		PdfPTable feeComponentNameTable = new PdfPTable(1);
		feeComponentNameTable.setWidthPercentage(100f);
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 13, Font.BOLD);
		Stream.of(feeComponentName).forEach(columnTitle -> {
			PdfPCell header = new PdfPCell();
			header.setBackgroundColor(backgroundColor);
			header.setBorderWidth(2);
			header.setVerticalAlignment(Element.ALIGN_MIDDLE);
			header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
			feeComponentNameTable.addCell(header);
		});
		return feeComponentNameTable;
	}
	
	private  PdfPTable getTableForInstallmentBreakup() {
		PdfPTable installmentBreakupTable = new PdfPTable(3);
		installmentBreakupTable.setWidthPercentage(100f);
		return installmentBreakupTable;
	}
	
	private  PdfPTable getTableForTotalOfInstallment() {
		PdfPTable totalOfInstallmentTable = new PdfPTable(2);
		totalOfInstallmentTable.setWidthPercentage(100f);
		return totalOfInstallmentTable;
	}
	
	private void setTableForTotalOfInstallment(PdfPTable totalTable, String feeComponentName, String totalFee,
			BaseColor backgroundColor) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 13, Font.BOLD);
		Stream.of("Total payable " + feeComponentName, totalFee).forEach(columnTitle -> {
			PdfPCell header = new PdfPCell();
			header.setBackgroundColor(backgroundColor);
			header.setBorderWidth(2);
			header.setVerticalAlignment(Element.ALIGN_MIDDLE);
			header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
			totalTable.addCell(header);
		});
	}
	
	private void addTableHeader(PdfPTable table,String headerName,Font fontStyle,BaseColor backgroundColor) {
		Stream.of(headerName).forEach(columnTitle -> {
			PdfPCell header = new PdfPCell();
			header.setBackgroundColor(backgroundColor);
			header.setBorderWidth(2);
			header.setVerticalAlignment(Element.ALIGN_MIDDLE);
			header.setPhrase(new Phrase(columnTitle, fontStyle));
			table.addCell(header);
		});

	}
	
	private void addInstallmentCellToTable(PdfPTable installmentTable, BaseColor backgroundColour,
			Installment installment) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 13, Font.BOLD);
		Stream.of(getInstallmentNoShowText(installment.getInstallmentNo()), installment.getTaxAmount() + "",
				getCommentForTaxHeadInstallment(installment)).forEach(columnTitle -> {
					PdfPCell header = new PdfPCell();
					header.setBackgroundColor(backgroundColour);
					header.setBorderWidth(2);
					header.setVerticalAlignment(Element.ALIGN_MIDDLE);
					header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
					installmentTable.addCell(header);
				});
	}
	
	private String getInstallmentNoShowText(int installmentNo) {
		switch (installmentNo) {
		case 1:
			return "1st installment";
		case 2:
			return "2nd installment";
		case 3:
			return "3rd installment";
		case 4:
			return "4th installment";
		case 5:
			return "5th installment";
		default:
			return installmentNo + " installment";
		}
	}
	
	private String getCommentForTaxHeadInstallment(Installment installment) {
		int installmentNo = installment.getInstallmentNo();
		String taxHeadCode = installment.getTaxHeadCode();
		if (installmentNo == 1 || installment.isPaymentCompletedInDemand())
			return "Paid";
		switch (taxHeadCode) {
		case TAXHEAD_BPA_SANC_WORKER_WELFARE_CESS_CODE:
		case TAXHEAD_BPA_SANC_SHELTER_FEE_CODE:
			switch (installmentNo) {
			case 2:
				return "To be paid before one year of issue of permission letter";
			case 3:
				return "To be paid before two years of issue of permission letter";
			case 4:
				return "To be paid before three years of issue of permission letter";
			}
			break;
		case TAXHEAD_BPA_SANC_PUR_FAR_CODE:
			switch (installmentNo) {
			case 2:
				return "To be paid At the time of Plinth level";
			case 3:
				return "To be paid At the time of Ground Floor Roof Casting";
			case 4:
				return "To be paid At the time of application of occupancy certificate";
			}
			break;
		case TAXHEAD_BPA_SANC_EIDP_FEE_CODE:
			switch (installmentNo) {
			case 2:
				return "To be paid at the time of Plinth level";
			case 3:
				return "To be paid at the time of Ground Floor Roof Casting";
			case 4:
				return "To be paid at the time of application of occupancy certificate";
			}
			break;
		}
		return "";
	}
	
	private void setTablesForSingleInstallmentTaxHeads(PdfPTable singleInstallmentTable, String feeComponentName,
			String feeAmount, BaseColor backgroundColor) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.COURIER, 13, Font.BOLD);
		Stream.of(feeComponentName, feeAmount, "Paid").forEach(columnTitle -> {
			PdfPCell header = new PdfPCell();
			header.setBackgroundColor(backgroundColor);
			header.setBorderWidth(2);
			header.setVerticalAlignment(Element.ALIGN_MIDDLE);
			header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
			singleInstallmentTable.addCell(header);
		});

	}
	
	private Map<String, java.util.List<Installment>> prepareTaxHeadCodeToInstallmentsMap(
			Map<String, Object> installmentData, ObjectMapper mapper) {
		Map<String, java.util.List<Installment>> installmentMapper = new HashMap<>();
		java.util.List<Object> allInstallments = (java.util.List<Object>) installmentData.get("installments");
		for (Object installmentsinOneInstallmentNoObject : allInstallments) {
			java.util.List<Object> installmentsinOneInstallmentNoList = (java.util.List<Object>) installmentsinOneInstallmentNoObject;
			for (Object installmentObject : installmentsinOneInstallmentNoList) {
				Installment installment = mapper.convertValue(installmentObject, Installment.class);
				if (installmentMapper.containsKey(installment.getTaxHeadCode())) {
					java.util.List<Installment> installmentsForTaxHead = installmentMapper
							.get(installment.getTaxHeadCode());
					installmentsForTaxHead.add(installment);
				} else {
					java.util.List<Installment> installmentsForTaxHead = new ArrayList<>();
					installmentsForTaxHead.add(installment);
					installmentMapper.put(installment.getTaxHeadCode(), installmentsForTaxHead);
				}
			}
		}
		// System.out.println(mapper.writeValueAsString(installmentMapper));
		return installmentMapper;
	}
	
	
}
