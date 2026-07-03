package org.egov.edcr.service;

import static ar.com.fdvs.dj.domain.constants.Stretching.RELATIVE_TO_BAND_HEIGHT;
import static org.egov.edcr.utility.DcrConstants.DECIMALDIGITS_MEASUREMENTS;
import static org.egov.edcr.utility.DcrConstants.ROUNDMODE_MEASUREMENTS;
import static org.egov.infra.security.utils.SecureCodeUtils.generatePDF417Code;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.egov.common.entity.edcr.Block;
import org.egov.common.entity.edcr.Building;
import org.egov.common.entity.edcr.Floor;
import org.egov.common.entity.edcr.OcComparisonBlockDetail;
import org.egov.common.entity.edcr.OcComparisonReportFloorDetail;
import org.egov.common.entity.edcr.Occupancy;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.Result;
import org.egov.common.entity.edcr.ScrutinyDetail;
import org.egov.common.entity.edcr.ScrutinyDetail.ColumnHeadingDetail;
import org.egov.common.entity.edcr.SetBack;
import org.egov.edcr.constants.DxfFileConstants;
import org.egov.edcr.constants.OdishaUlbs;
import org.egov.edcr.entity.ApplicationType;
import org.egov.edcr.entity.EdcrApplication;
import org.egov.edcr.entity.EdcrApplicationDetail;
import org.egov.edcr.entity.OcComparisonDetail;
import org.egov.edcr.utility.DcrConstants;
import org.egov.edcr.utility.Util;
import org.egov.infra.admin.master.service.CityService;
import org.egov.infra.config.core.ApplicationThreadLocals;
import org.egov.infra.filestore.entity.FileStoreMapper;
import org.egov.infra.filestore.service.FileStoreService;
import org.egov.infra.reporting.util.ReportUtil;
import org.egov.infra.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import ar.com.fdvs.dj.core.DJConstants;
import ar.com.fdvs.dj.core.DynamicJasperHelper;
import ar.com.fdvs.dj.core.layout.ClassicLayoutManager;
import ar.com.fdvs.dj.domain.DJDataSource;
import ar.com.fdvs.dj.domain.DynamicReport;
import ar.com.fdvs.dj.domain.Style;
import ar.com.fdvs.dj.domain.builders.ColumnBuilderException;
import ar.com.fdvs.dj.domain.builders.FastReportBuilder;
import ar.com.fdvs.dj.domain.constants.Font;
import ar.com.fdvs.dj.domain.constants.HorizontalAlign;
import ar.com.fdvs.dj.domain.constants.Page;
import ar.com.fdvs.dj.domain.entities.Subreport;
import ar.com.fdvs.dj.domain.entities.columns.AbstractColumn;
import ar.com.fdvs.dj.domain.entities.conditionalStyle.ConditionalStyle;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

@Service
public class OcComparisonReportService {

	private static final Logger LOG = Logger.getLogger(OcComparisonReportService.class);
	public static final String STATUS = "Status";
	public static final String BLOCK = "Block";
	public static final BigDecimal DEVIATION_VALUE = BigDecimal.valueOf(5);
	public static final BigDecimal SETBACK_DEVIATION_VALUE = BigDecimal.valueOf(-10);
	@Autowired
	private CityService cityService;
	@Autowired
	private JasperReportService reportService;
	@Autowired
	private FileStoreService fileStoreService;

	public InputStream generateOcComparisonReport(EdcrApplicationDetail ocDcr, EdcrApplicationDetail permitDcr,
			OcComparisonDetail comparisonDetail) {

		FileStoreMapper ocPlanFileMapper = ocDcr.getPlanDetailFileStore();
		File ocFile = ocPlanFileMapper != null
				? fileStoreService.fetch(ocPlanFileMapper.getFileStoreId(), DcrConstants.APPLICATION_MODULE_TYPE)
				: null;
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		Plan ocPlan = null;
		try {
			ocPlan = mapper.readValue(ocFile, Plan.class);
		} catch (IOException e) {
			LOG.log(Level.ERROR, e);
		}

		FileStoreMapper permitFileMapper = permitDcr.getPlanDetailFileStore();
		File permitFile = permitFileMapper != null
				? fileStoreService.fetch(permitFileMapper.getFileStoreId(), DcrConstants.APPLICATION_MODULE_TYPE)
				: null;
		ObjectMapper permitMapper = new ObjectMapper();
		permitMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		Plan permitPlan = null;
		try {
			permitPlan = permitMapper.readValue(permitFile, Plan.class);
		} catch (IOException e) {
			LOG.log(Level.ERROR, e);
		}

		List<OcComparisonBlockDetail> ocComparison = buildOcComparison(permitPlan, ocPlan);
		List<ScrutinyDetail> scrutinyDetails = buildReportObject(ocComparison, permitPlan, ocPlan);

		boolean finalReportStatus = true;
		FastReportBuilder drb = new FastReportBuilder();

		final Style titleStyle = new Style("titleStyle");
		titleStyle.setFont(new Font(50, Font._FONT_TIMES_NEW_ROMAN, true));
		titleStyle.setHorizontalAlign(HorizontalAlign.CENTER);

		titleStyle.setFont(new Font(2, Font._FONT_TIMES_NEW_ROMAN, false));
		String applicationNumber = StringUtils.isNotBlank(ocDcr.getApplication().getApplicationNumber())
				? ocDcr.getApplication().getApplicationNumber()
				: "NA";
		String applicationDate = DateUtils.toDefaultDateFormat(ocDcr.getApplication().getApplicationDate());

		drb.setPageSizeAndOrientation(new Page(842, 595, true));
		final JRDataSource ds = new JRBeanCollectionDataSource(scrutinyDetails);
		
		OdishaUlbs odishaUlbs = null;
		odishaUlbs = OdishaUlbs.getUlb(ocDcr.getPlan().getThirdPartyUserTenantld());
		
		final Map<String, Object> valuesMap = new HashMap<>();
//		valuesMap.put("ulbName", ApplicationThreadLocals.getMunicipalityName());
		valuesMap.put("ulbName", odishaUlbs.getUlbName());
		valuesMap.put("applicationNumber", applicationNumber);
		valuesMap.put("ocdcrNo", ocDcr.getDcrNumber());
		valuesMap.put("dcrNo", permitDcr.getDcrNumber());
		valuesMap.put("applicationDate", applicationDate);
		valuesMap.put("applicantName", ocDcr.getApplication().getApplicantName());
		valuesMap.put("reportGeneratedDate", DateUtils.toDefaultDateTimeFormat(new Date()));

//		String imageURL = ReportUtil.getImageURL("/egi/resources/global/images/digit-logo-black.png");
//		valuesMap.put("egovLogo", imageURL);
//		valuesMap.put("cityLogo", cityService.getCityLogoURLByCurrentTenant());

		Set<String> common = new TreeSet<>();
		Map<String, ScrutinyDetail> allMap = new HashMap<>();
		Map<String, Set<String>> blocks = new TreeMap<>();
		//LOG.info("Generate Report.......");
		for (ScrutinyDetail sd : scrutinyDetails) {
			//LOG.info(sd.getKey());
			//LOG.info(sd.getHeading());
			String[] split = {};
			if (sd.getKey() != null)
				split = sd.getKey().split("_");
			if (split.length == 2) {
				common.add(split[1]);
				allMap.put(split[1], sd);

			} else if (split.length == 3) {
				if (blocks.get(split[1]) == null) {
					Set<String> features = new TreeSet<>();
					features.add(split[2]);
					blocks.put(split[1], features);
				} else {
					blocks.get(split[1]).add(split[2]);
				}
				allMap.put(split[1] + split[2], sd);
			}
		}
		/*
		 * int i = 0; List<String> cmnHeading = new ArrayList<>();
		 * cmnHeading.add("Common");
		 * drb.addConcatenatedReport(createHeaderSubreport("Common - Scrutiny Details",
		 * "Common")); valuesMap.put("Common", cmnHeading); for (String cmnFeature :
		 * common) { i++; drb.addConcatenatedReport(getSub(allMap.get(cmnFeature), i, i
		 * + "." + cmnFeature, allMap.get(cmnFeature).getHeading(),
		 * allMap.get(cmnFeature).getSubHeading(), cmnFeature));
		 * valuesMap.put(cmnFeature, allMap.get(cmnFeature).getDetail()); }
		 */

		for (String blkName : blocks.keySet()) {
			List blkHeading = new ArrayList();
			blkHeading.add(BLOCK + blkName);
			drb.addConcatenatedReport(createHeaderSubreport("Block " + blkName, BLOCK + blkName));
			valuesMap.put(BLOCK + blkName, blkHeading);
			int j = 0;

			// This is only for rest
			for (String blkFeature : blocks.get(blkName)) {
				j++;
				drb.addConcatenatedReport(getSub(allMap.get(blkName + blkFeature), j, j + "." + blkFeature,
						allMap.get(blkName + blkFeature).getHeading(), allMap.get(blkName + blkFeature).getSubHeading(),
						blkName + blkFeature));
				valuesMap.put(blkName + blkFeature, allMap.get(blkName + blkFeature).getDetail());

				List featureFooter = new ArrayList();
				if (allMap.get(blkName + blkFeature).getRemarks() != null) {
					drb.addConcatenatedReport(
							createFooterSubreport("Remarks :  " + allMap.get(blkName + blkFeature).getRemarks(),
									"Remarks_" + blkName + blkFeature));
					featureFooter.add(allMap.get(blkName + blkFeature).getRemarks());
					valuesMap.put("Remarks_" + blkName + blkFeature, featureFooter);

				}

			}

		}

		if (finalReportStatus)
			for (String cmnFeature : common) {
				for (Map<String, String> commonStatus : allMap.get(cmnFeature).getDetail()) {
					if (commonStatus.get(STATUS).equalsIgnoreCase(Result.Not_Accepted.getResultVal())) {
						finalReportStatus = false;
					}
				}
			}

		if (finalReportStatus)
			for (String blkName : blocks.keySet()) {
				for (String blkFeature : blocks.get(blkName)) {
					for (Map<String, String> blkStatus : allMap.get(blkName + blkFeature).getDetail()) {
						if (blkStatus.get(STATUS).equalsIgnoreCase(Result.Not_Accepted.getResultVal())) {
							finalReportStatus = false;
						}
					}
				}
			}
		drb.setTemplateFile("/reports/templates/oc_comparison_report.jrxml");
		drb.setMargins(5, 0, 33, 20);
		String endStatus = finalReportStatus ? "Accepted" : "Not Accepted";
		valuesMap.put("reportStatus", endStatus);
		comparisonDetail.setStatus(endStatus);

		valuesMap.put("qrCode", generatePDF417Code(buildQRCodeDetails(ocDcr.getApplication(), finalReportStatus)));

		final DynamicReport dr = drb.build();
		InputStream exportPdf = null;
		try {
			JasperPrint generateJasperPrint = DynamicJasperHelper.generateJasperPrint(dr, new ClassicLayoutManager(),
					ds, valuesMap);
			exportPdf = reportService.exportPdf(generateJasperPrint);
		} catch (IOException | JRException e) {
			LOG.error("Error occurred when generating Jasper report", e);
		}
		return exportPdf;

	}

	public InputStream generatePreOcComparisonReport(EdcrApplicationDetail ocDcr, EdcrApplicationDetail permitDcr,
			OcComparisonDetail comparisonDetail) {

		Plan ocPlan = ocDcr.getPlan();

		FileStoreMapper permitFileMapper = permitDcr.getPlanDetailFileStore();
		File permitFile = permitFileMapper != null
				? fileStoreService.fetch(permitFileMapper.getFileStoreId(), DcrConstants.APPLICATION_MODULE_TYPE)
				: null;
		ObjectMapper permitMapper = new ObjectMapper();
		permitMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		Plan permitPlan = null;
		try {
			permitPlan = permitMapper.readValue(permitFile, Plan.class);
		} catch (IOException e) {
			LOG.log(Level.ERROR, e);
		}

		List<ScrutinyDetail> scrutinyDetails = comparisonDetail.getScrutinyDetails();

		boolean finalReportStatus = true;
		FastReportBuilder drb = new FastReportBuilder();

		final Style titleStyle = new Style("titleStyle");
		titleStyle.setFont(new Font(50, Font._FONT_TIMES_NEW_ROMAN, true));
		titleStyle.setHorizontalAlign(HorizontalAlign.CENTER);

		titleStyle.setFont(new Font(2, Font._FONT_TIMES_NEW_ROMAN, false));
		String applicationNumber = StringUtils.isNotBlank(ocDcr.getApplication().getApplicationNumber())
				? ocDcr.getApplication().getApplicationNumber()
				: "NA";
		String applicationDate = DateUtils.toDefaultDateFormat(ocDcr.getApplication().getApplicationDate());

		drb.setPageSizeAndOrientation(new Page(842, 595, true));
		final JRDataSource ds = new JRBeanCollectionDataSource(scrutinyDetails);
		
		OdishaUlbs odishaUlbs = null;
		odishaUlbs = OdishaUlbs.getUlb(ocDcr.getPlan().getThirdPartyUserTenantld());
		
		final Map<String, Object> valuesMap = new HashMap<>();
//		valuesMap.put("ulbName", ApplicationThreadLocals.getMunicipalityName());
		valuesMap.put("ulbName", odishaUlbs.getUlbName());
		valuesMap.put("applicationNumber", applicationNumber);
		valuesMap.put("ocdcrNo", ocDcr.getDcrNumber());
		valuesMap.put("dcrNo", permitDcr.getDcrNumber());
		valuesMap.put("applicationDate", applicationDate);
		valuesMap.put("applicantName", ocDcr.getApplication().getApplicantName());
		valuesMap.put("reportGeneratedDate", DateUtils.toDefaultDateTimeFormat(new Date()));
//		String imageURL = ReportUtil.getImageURL("/egi/resources/global/images/digit-logo-black.png");
//		valuesMap.put("egovLogo", imageURL);
//		valuesMap.put("cityLogo", cityService.getCityLogoURLByCurrentTenant());

		Set<String> common = new TreeSet<>();
		Map<String, ScrutinyDetail> allMap = new HashMap<>();
		Map<String, Set<String>> blocks = new TreeMap<>();
		//LOG.info("Generate Report.......");
		for (ScrutinyDetail sd : scrutinyDetails) {
			//LOG.info(sd.getKey());
			//LOG.info(sd.getHeading());
			String[] split = {};
			if (sd.getKey() != null)
				split = sd.getKey().split("_");
			if (split.length == 2) {
				common.add(split[1]);
				allMap.put(split[1], sd);

			} else if (split.length == 3) {
				if (blocks.get(split[1]) == null) {
					Set<String> features = new TreeSet<>();
					features.add(split[2]);
					blocks.put(split[1], features);
				} else {
					blocks.get(split[1]).add(split[2]);
				}
				allMap.put(split[1] + split[2], sd);
			}
		}

		int i = 0;
		List<String> cmnHeading = new ArrayList<>();
		cmnHeading.add("Common");
		drb.addConcatenatedReport(createHeaderSubreport("Common - Scrutiny Details", "Common"));
		valuesMap.put("Common", cmnHeading);
		for (String cmnFeature : common) {
			i++;
			drb.addConcatenatedReport(getSub(allMap.get(cmnFeature), i, i + "." + cmnFeature,
					allMap.get(cmnFeature).getHeading(), allMap.get(cmnFeature).getSubHeading(), cmnFeature));
			valuesMap.put(cmnFeature, allMap.get(cmnFeature).getDetail());
		}

		for (String blkName : blocks.keySet()) {
			List blkHeading = new ArrayList();
			blkHeading.add(BLOCK + blkName);
			drb.addConcatenatedReport(createHeaderSubreport("Block " + blkName, BLOCK + blkName));
			valuesMap.put(BLOCK + blkName, blkHeading);
			int j = 0;

			// This is only for rest
			for (String blkFeature : blocks.get(blkName)) {
				j++;
				drb.addConcatenatedReport(getSub(allMap.get(blkName + blkFeature), j, j + "." + blkFeature,
						allMap.get(blkName + blkFeature).getHeading(), allMap.get(blkName + blkFeature).getSubHeading(),
						blkName + blkFeature));
				valuesMap.put(blkName + blkFeature, allMap.get(blkName + blkFeature).getDetail());

				List featureFooter = new ArrayList();
				if (allMap.get(blkName + blkFeature).getRemarks() != null) {
					drb.addConcatenatedReport(
							createFooterSubreport("Remarks :  " + allMap.get(blkName + blkFeature).getRemarks(),
									"Remarks_" + blkName + blkFeature));
					featureFooter.add(allMap.get(blkName + blkFeature).getRemarks());
					valuesMap.put("Remarks_" + blkName + blkFeature, featureFooter);

				}

			}

		}

		if (finalReportStatus)
			for (String cmnFeature : common) {
				for (Map<String, String> commonStatus : allMap.get(cmnFeature).getDetail()) {
					if (commonStatus.get(STATUS) != null
							&& commonStatus.get(STATUS).equalsIgnoreCase(Result.Not_Accepted.getResultVal())) {
						finalReportStatus = false;
					}
				}
			}

		if (finalReportStatus)
			for (String blkName : blocks.keySet()) {
				for (String blkFeature : blocks.get(blkName)) {
					for (Map<String, String> blkStatus : allMap.get(blkName + blkFeature).getDetail()) {
						if (blkStatus.get(STATUS) != null
								&& blkStatus.get(STATUS).equalsIgnoreCase(Result.Not_Accepted.getResultVal())) {
							finalReportStatus = false;
						}
					}
				}
			}
		drb.setTemplateFile("/reports/templates/oc_comparison_report.jrxml");
		drb.setMargins(5, 0, 33, 20);
		String endStatus = finalReportStatus ? "Accepted" : "Not Accepted";
		valuesMap.put("reportStatus", endStatus);
		comparisonDetail.setStatus(endStatus);

		valuesMap.put("qrCode", generatePDF417Code(buildQRCodeDetails(ocDcr.getApplication(), finalReportStatus)));

		final DynamicReport dr = drb.build();
		InputStream exportPdf = null;
		try {
			JasperPrint generateJasperPrint = DynamicJasperHelper.generateJasperPrint(dr, new ClassicLayoutManager(),
					ds, valuesMap);
			exportPdf = reportService.exportPdf(generateJasperPrint);
		} catch (IOException | JRException e) {
			LOG.error("Error occurred when generating Jasper report", e);
		}
		return exportPdf;

	}

	public InputStream generatePreOcComparisonReportAlt(EdcrApplicationDetail ocDcr, EdcrApplicationDetail permitDcr,
			OcComparisonDetail comparisonDetail) {

		Plan ocPlan = ocDcr.getPlan();
		
		FileStoreMapper permitFileMapper = null;
		
		//LOG.info("permitDcr in generatePreOcComparisonReportAlt = "+permitDcr);
		
		if(permitDcr!=null) {
			permitFileMapper = permitDcr.getPlanDetailFileStore();
		}
		
		File permitFile = permitFileMapper != null
				? fileStoreService.fetch(permitFileMapper.getFileStoreId(), DcrConstants.APPLICATION_MODULE_TYPE)
				: null;
				
		//LOG.info("permitFile = "+permitFile.toString());
		
		ObjectMapper permitMapper = new ObjectMapper();
		permitMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		permitMapper.configure(DeserializationFeature.READ_ENUMS_USING_TO_STRING, true);
		
		Plan permitPlan = null;
		try {
			permitPlan = permitMapper.readValue(permitFile, Plan.class);
		} catch (IOException e) {
			e.printStackTrace();
			LOG.log(Level.ERROR, e);
		}
		
		//LOG.info("comparisonDetail = "+comparisonDetail.toString());

		List<ScrutinyDetail> scrutinyDetails = comparisonDetail.getScrutinyDetails();
		
		//LOG.info("scrutinyDetails = "+scrutinyDetails.toString());

		boolean finalReportStatus = true;
		FastReportBuilder drb = new FastReportBuilder();

		final Style titleStyle = new Style("titleStyle");
		titleStyle.setFont(new Font(50, Font._FONT_TIMES_NEW_ROMAN, true));
		titleStyle.setHorizontalAlign(HorizontalAlign.CENTER);

		titleStyle.setFont(new Font(2, Font._FONT_TIMES_NEW_ROMAN, false));
		String applicationNumber = StringUtils.isNotBlank(ocDcr.getApplication().getApplicationNumber())
				? ocDcr.getApplication().getApplicationNumber()
				: "NA";
		String applicationDate = DateUtils.toDefaultDateFormat(ocDcr.getApplication().getApplicationDate());

		drb.setPageSizeAndOrientation(new Page(842, 595, true));
		final JRDataSource ds = new JRBeanCollectionDataSource(scrutinyDetails);
		OdishaUlbs odishaUlbs = null;
		odishaUlbs = OdishaUlbs.getUlb(ocDcr.getPlan().getThirdPartyUserTenantld());
		
		final Map<String, Object> valuesMap = new HashMap<>();
//		valuesMap.put("ulbName", ApplicationThreadLocals.getMunicipalityName());
		valuesMap.put("ulbName", odishaUlbs.getUlbName());
		valuesMap.put("applicationNumber", applicationNumber);
		valuesMap.put("oldedcrNo", permitDcr != null? permitDcr.getDcrNumber():"");
		valuesMap.put("dcrNo", ocDcr.getDcrNumber());
		valuesMap.put("applicationDate", applicationDate);
		valuesMap.put("applicantName", ocDcr.getApplication().getApplicantName());
		valuesMap.put("reportGeneratedDate", DateUtils.toDefaultDateTimeFormat(new Date()));
//		String imageURL = ReportUtil.getImageURL("/egi/resources/global/images/digit-logo-black.png");
//		valuesMap.put("egovLogo", imageURL);
//		valuesMap.put("cityLogo", cityService.getCityLogoURLByCurrentTenant());

		Set<String> common = new TreeSet<>();
		Map<String, ScrutinyDetail> allMap = new HashMap<>();
		Map<String, Set<String>> blocks = new TreeMap<>();
		//LOG.info("Generate Report.......");
		for (ScrutinyDetail sd : scrutinyDetails) {
			//LOG.info(sd.getKey());
			//LOG.info(sd.getHeading());
			String[] split = {};
			if (sd.getKey() != null)
				split = sd.getKey().split("_");
			if (split.length == 2) {
				common.add(split[1]);
				allMap.put(split[1], sd);

			} else if (split.length == 3) {
				if (blocks.get(split[1]) == null) {
					Set<String> features = new TreeSet<>();
					features.add(split[2]);
					blocks.put(split[1], features);
				} else {
					blocks.get(split[1]).add(split[2]);
				}
				allMap.put(split[1] + split[2], sd);
			}
		}

		int i = 0;
		List<String> cmnHeading = new ArrayList<>();
		cmnHeading.add("Common");
		drb.addConcatenatedReport(createHeaderSubreport("Common - Scrutiny Details", "Common"));
		valuesMap.put("Common", cmnHeading);
		for (String cmnFeature : common) {
			i++;
			drb.addConcatenatedReport(getSub(allMap.get(cmnFeature), i, i + "." + cmnFeature,
					allMap.get(cmnFeature).getHeading(), allMap.get(cmnFeature).getSubHeading(), cmnFeature));
			valuesMap.put(cmnFeature, allMap.get(cmnFeature).getDetail());
		}

		for (String blkName : blocks.keySet()) {
			List blkHeading = new ArrayList();
			blkHeading.add(BLOCK + blkName);
			drb.addConcatenatedReport(createHeaderSubreport("Block " + blkName, BLOCK + blkName));
			valuesMap.put(BLOCK + blkName, blkHeading);
			int j = 0;

			// This is only for rest
			for (String blkFeature : blocks.get(blkName)) {
				j++;
				drb.addConcatenatedReport(getSub(allMap.get(blkName + blkFeature), j, j + "." + blkFeature,
						allMap.get(blkName + blkFeature).getHeading(), allMap.get(blkName + blkFeature).getSubHeading(),
						blkName + blkFeature));
				valuesMap.put(blkName + blkFeature, allMap.get(blkName + blkFeature).getDetail());

				List featureFooter = new ArrayList();
				if (allMap.get(blkName + blkFeature).getRemarks() != null) {
					drb.addConcatenatedReport(
							createFooterSubreport("Remarks :  " + allMap.get(blkName + blkFeature).getRemarks(),
									"Remarks_" + blkName + blkFeature));
					featureFooter.add(allMap.get(blkName + blkFeature).getRemarks());
					valuesMap.put("Remarks_" + blkName + blkFeature, featureFooter);

				}

			}

		}
		if (finalReportStatus)
			for (String cmnFeature : common) {
				for (Map<String, String> commonStatus : allMap.get(cmnFeature).getDetail()) {
					if (commonStatus.get(STATUS) != null
							&& commonStatus.get(STATUS).equalsIgnoreCase(Result.Not_Accepted.getResultVal())) {
						finalReportStatus = false;
					}
				}
			}

		if (finalReportStatus)
			for (String blkName : blocks.keySet()) {
				for (String blkFeature : blocks.get(blkName)) {
					for (Map<String, String> blkStatus : allMap.get(blkName + blkFeature).getDetail()) {
						if (blkStatus.get(STATUS) != null
								&& blkStatus.get(STATUS).equalsIgnoreCase(Result.Not_Accepted.getResultVal())) {
							finalReportStatus = false;
						}
					}
				}
			}
		drb.setTemplateFile("/reports/templates/comparison_report.jrxml");
		drb.setMargins(5, 0, 33, 20);
		String endStatus = finalReportStatus ? "Accepted" : "Not Accepted";
		valuesMap.put("reportStatus", endStatus);
		comparisonDetail.setStatus(endStatus);

		valuesMap.put("qrCode", generatePDF417Code(buildQRCodeDetails(ocDcr.getApplication(), finalReportStatus)));

		final DynamicReport dr = drb.build();
		InputStream exportPdf = null;
		try {
			//LOG.info("dr : "+dr);
			//LOG.info("ds :"+ds);
			//LOG.info("valuesMap :"+valuesMap);
			JasperPrint generateJasperPrint = DynamicJasperHelper.generateJasperPrint(dr, new ClassicLayoutManager(),
					ds, valuesMap);
			exportPdf = reportService.exportPdf(generateJasperPrint);
			//LOG.info("exported successfully..");
		} catch (IOException | JRException e) {
			LOG.error("Error occurred when generating Jasper report", e);
		}
		return exportPdf;

	}
	
	public OcComparisonDetail getComparisonReportStatus(EdcrApplicationDetail ocDcr, EdcrApplicationDetail permitDcr,
			OcComparisonDetail comparisonDetail) {

		Plan ocPlan = ocDcr.getPlan();
		
		//LOG.info("permitDcr in getComparisonReportStatus = "+ permitDcr);
		
		FileStoreMapper permitFileMapper = null;
		
		if(permitDcr!=null) {
			permitFileMapper = permitDcr.getPlanDetailFileStore();
		}

		
		File permitFile = permitFileMapper != null
				? fileStoreService.fetch(permitFileMapper.getFileStoreId(), DcrConstants.APPLICATION_MODULE_TYPE)
				: null;
				
		//LOG.info("permitFile in getComparisonReportStatus = "+permitFile);
		
		ObjectMapper permitMapper = new ObjectMapper();
		permitMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		permitMapper.configure(DeserializationFeature.READ_ENUMS_USING_TO_STRING, true);
		
		Plan permitPlan = null;
		try {
			permitPlan = permitMapper.readValue(permitFile, Plan.class);
		} catch (IOException e) {
			e.printStackTrace();
			LOG.log(Level.ERROR, e);
			permitPlan=null;
		}
		
		List<OcComparisonBlockDetail> ocComparison = new ArrayList<>();
		List<ScrutinyDetail> scrutinyDetails = new ArrayList<>();
		boolean finalReportStatus=false;
		try {
			if(comparisonDetail.getAlterationSubService() == null) {
				ocComparison = buildOcComparison(permitPlan, ocPlan);
				scrutinyDetails = buildReportObject(ocComparison, permitPlan, ocPlan);
			}else {
				ocComparison = buildComparisonForAlt(permitPlan, ocPlan,comparisonDetail.getAlterationSubService());
				scrutinyDetails = buildReportObjectAlt(ocComparison, permitPlan, ocPlan,comparisonDetail.getAlterationSubService());
			}
			finalReportStatus = true;
		} catch (Exception e) {
			e.printStackTrace();
		}

		Set<String> common = new TreeSet<>();
		Map<String, ScrutinyDetail> allMap = new HashMap<>();
		Map<String, Set<String>> blocks = new TreeMap<>();
		//LOG.info("Generate Report.......");
		for (ScrutinyDetail sd : scrutinyDetails) {
			//LOG.info(sd.getKey());
			//LOG.info(sd.getHeading());
			String[] split = {};
			if (sd.getKey() != null)
				split = sd.getKey().split("_");
			if (split.length == 2) {
				common.add(split[1]);
				allMap.put(split[1], sd);

			} else if (split.length == 3) {
				if (blocks.get(split[1]) == null) {
					Set<String> features = new TreeSet<>();
					features.add(split[2]);
					blocks.put(split[1], features);
				} else {
					blocks.get(split[1]).add(split[2]);
				}
				allMap.put(split[1] + split[2], sd);
			}
		}
		/*
		 * int i = 0; List<String> cmnHeading = new ArrayList<>();
		 * cmnHeading.add("Common");
		 * drb.addConcatenatedReport(createHeaderSubreport("Common - Scrutiny Details",
		 * "Common")); valuesMap.put("Common", cmnHeading); for (String cmnFeature :
		 * common) { i++; drb.addConcatenatedReport(getSub(allMap.get(cmnFeature), i, i
		 * + "." + cmnFeature, allMap.get(cmnFeature).getHeading(),
		 * allMap.get(cmnFeature).getSubHeading(), cmnFeature));
		 * valuesMap.put(cmnFeature, allMap.get(cmnFeature).getDetail()); }
		 */
	
		if (finalReportStatus)
			for (String cmnFeature : common) {
				for (Map<String, String> commonStatus : allMap.get(cmnFeature).getDetail()) {
					if (commonStatus.get(STATUS) != null
							&& commonStatus.get(STATUS).equalsIgnoreCase(Result.Not_Accepted.getResultVal())) {
						finalReportStatus = false;
					}
				}
			}

		if (finalReportStatus)
			for (String blkName : blocks.keySet()) {
				for (String blkFeature : blocks.get(blkName)) {
					for (Map<String, String> blkStatus : allMap.get(blkName + blkFeature).getDetail()) {
						if (blkStatus.get(STATUS) != null
								&& blkStatus.get(STATUS).equalsIgnoreCase(Result.Not_Accepted.getResultVal())) {
							finalReportStatus = false;
						}
					}
				}
			}
		comparisonDetail.setScrutinyDetails(scrutinyDetails);
		comparisonDetail
				.setStatus(finalReportStatus ? Result.Accepted.getResultVal() : Result.Not_Accepted.getResultVal());
		return comparisonDetail;

	}

	private Subreport createHeaderSubreport(String title, String dataSourceName) {
		try {

			FastReportBuilder frb = new FastReportBuilder();
			frb.setUseFullPageWidth(true);
			frb.setTitle(title);

			frb.setShowDetailBand(false);
			frb.setMargins(0, 0, 0, 0);
			frb.setTitleStyle(reportService.getSubReportTitleStyle());
			frb.setPageSizeAndOrientation(Page.Page_A4_Portrait());
			DynamicReport build = frb.build();
			Subreport sub = new Subreport();
			sub.setDynamicReport(build);
			sub.setDatasource(new DJDataSource(dataSourceName, DJConstants.DATA_SOURCE_ORIGIN_PARAMETER, 0));
			sub.setLayoutManager(new ClassicLayoutManager());
			return sub;
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
		return null;
	}

	private Subreport createFooterSubreport(String title, String dataSourceName) {
		try {

			FastReportBuilder frb = new FastReportBuilder();
			frb.setUseFullPageWidth(true);
			frb.setTitleHeight(5);
			frb.setTitle(title);
			frb.setMargins(0, 0, 0, 0);
			frb.setTitleStyle(reportService.getTitleStyle());
			frb.setPageSizeAndOrientation(Page.Page_A4_Portrait());
			DynamicReport build = frb.build();
			Subreport sub = new Subreport();
			sub.setDynamicReport(build);
			sub.setDatasource(new DJDataSource(dataSourceName, DJConstants.DATA_SOURCE_ORIGIN_PARAMETER, 0));
			sub.setLayoutManager(new ClassicLayoutManager());
			return sub;
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
		return null;
	}

	private Subreport getSub(ScrutinyDetail detail, int j, String title, String heading, String subheading,
			String dataSourceName) {
		try {
			List<ConditionalStyle> listCondStyle = getConditonalStyles();
			FastReportBuilder frb = new FastReportBuilder();
			int size = detail.getColumnHeading().keySet().size();
			Double byeLawColumnSize = 40d;
			Double statusColumnSize = 60d;
			Double columnSize = (595d - (byeLawColumnSize + statusColumnSize)) / (size - 2);
			for (Integer s : detail.getColumnHeading().keySet()) {
				ColumnHeadingDetail columnHeading = detail.getColumnHeading().get(s);
				int columnWidth = columnSize.intValue();
				if ("Byelaw".equalsIgnoreCase(columnHeading.name)) {
					columnWidth = byeLawColumnSize.intValue();
				}
				if (STATUS.equalsIgnoreCase(columnHeading.name)) {
					if (size == 3) {
						columnWidth = 100;
					} else {
						columnWidth = statusColumnSize.intValue();
					}
				}
				frb.addColumn(columnHeading.name, columnHeading.name, String.class.getName(), columnWidth);
			}
			frb.setMargins(0, 0, 0, 0);
			frb.setUseFullPageWidth(true);
			List<AbstractColumn> columns = frb.getColumns();
			for (AbstractColumn col : columns) {
				if (STATUS.equalsIgnoreCase(col.getTitle()))
					col.setConditionalStyles(listCondStyle);

			}
			if (heading != null)
				frb.setTitle(j + "." + heading);
			else
				frb.setTitle(title);

			if (subheading != null)
				frb.setSubtitle("\t" + subheading);

			frb.setTitleStyle(reportService.getTitleStyle());
			frb.setHeaderHeight(5);
			frb.setDefaultStyles(reportService.getTitleStyle(), reportService.getSubTitleStyle(),
					reportService.getColumnHeaderStyle(), reportService.getDetailStyle());
			frb.setAllowDetailSplit(false);
			frb.setPageSizeAndOrientation(Page.Page_A4_Portrait());
			DynamicReport build = frb.build();
			Subreport sub = new Subreport();
			sub.setDynamicReport(build);
			Style style = new Style();
			style.setStretchWithOverflow(true);
			style.setStreching(RELATIVE_TO_BAND_HEIGHT);
			sub.setStyle(style);
			sub.setDatasource(new DJDataSource(dataSourceName, DJConstants.DATA_SOURCE_ORIGIN_PARAMETER, 0));
			sub.setLayoutManager(new ClassicLayoutManager());
			return sub;
		} catch (ColumnBuilderException |

				ClassNotFoundException e) {
			LOG.error(e.getMessage(), e);
		}
		return null;

	}

	private List<OcComparisonBlockDetail> buildOcComparison(Plan permit, Plan oc) {
		List<OcComparisonBlockDetail> blockDetails = new ArrayList<>();
		List<Block> permitBlks = permit.getBlocks();
		List<Block> ocBlks = oc.getBlocks();

		if (!permitBlks.isEmpty()) {

			for (Block permitBlk : permitBlks) {
				Block currentOcBlk = null;
				for (Block ocBlk : ocBlks) {
					if (ocBlk.getNumber().equalsIgnoreCase(permitBlk.getNumber())) {
						currentOcBlk = ocBlk;
					}
				}
				OcComparisonBlockDetail blockDetail = new OcComparisonBlockDetail();
				updateSetBackDetails(blockDetail, permitBlk, currentOcBlk);
				blockDetail.setNumber(Long.valueOf(permitBlk.getNumber()));
				Building permitBuilding = permitBlk.getBuilding();
				Building ocBuilding = null;
				if (currentOcBlk != null) {
					ocBuilding = currentOcBlk.getBuilding();
				}
				if (permitBuilding != null) {

					List<Floor> permitFloors = permitBuilding.getFloors();
					List<Floor> ocFloors = new ArrayList<>();
					if (ocBuilding != null) {
						ocFloors = ocBuilding.getFloors();
						BigDecimal noOfFloorsOc = ocBuilding.getFloorsAboveGround();
						BigDecimal buildingHeight = ocBuilding.getBuildingHeight();
						blockDetail.setNoOfFloorsOc(Long.valueOf(noOfFloorsOc.toString()));
						blockDetail.setHghtFromGroundOc(buildingHeight);
					}

					if (permitFloors != null && !permitFloors.isEmpty()) {
						List<OcComparisonReportFloorDetail> comparisonReportFloorDetails = new ArrayList<>();
						for (Floor permitFloor : permitFloors) {
							Floor ocFloor = null;

							if (ocFloors != null && !ocFloors.isEmpty()) {
								for (Floor floor : ocFloors) {
									if (floor.getNumber() == permitFloor.getNumber()) {
										ocFloor = floor;
									}
								}
							}
							OcComparisonReportFloorDetail comparisonReportFloorDetail = new OcComparisonReportFloorDetail();
							List<Occupancy> permitOccupancies = permitFloor.getOccupancies();
							BigDecimal permitBltUpArea = BigDecimal.ZERO;
							BigDecimal permitCarpetArea = BigDecimal.ZERO;
							BigDecimal permitFloorArea = BigDecimal.ZERO;
							if (!permitOccupancies.isEmpty()) {
								for (Occupancy occupancy : permitOccupancies) {
									permitBltUpArea = permitBltUpArea.add(occupancy.getBuiltUpArea());
									permitCarpetArea = permitCarpetArea.add(occupancy.getCarpetArea());
									permitFloorArea = permitFloorArea.add(occupancy.getFloorArea());
								}
							}
							comparisonReportFloorDetail.setPermitBltUpArea(Util.roundOffTwoDecimal(permitBltUpArea));
							comparisonReportFloorDetail.setPermitCarpetArea(Util.roundOffTwoDecimal(permitCarpetArea));
							comparisonReportFloorDetail.setPermitFloorArea(Util.roundOffTwoDecimal(permitFloorArea));

							if (ocFloor != null) {
								List<Occupancy> ocOccupancies = ocFloor.getOccupancies();
								BigDecimal ocBltUpArea = BigDecimal.ZERO;
								BigDecimal ocCarpetArea = BigDecimal.ZERO;
								BigDecimal ocFloorArea = BigDecimal.ZERO;
								if (!ocOccupancies.isEmpty()) {
									for (Occupancy ocOccupancy : ocOccupancies) {
										ocBltUpArea = ocBltUpArea
												.add(Util.roundOffTwoDecimal(ocOccupancy.getBuiltUpArea()));
										ocCarpetArea = ocCarpetArea
												.add(Util.roundOffTwoDecimal(ocOccupancy.getCarpetArea()));
										ocFloorArea = ocFloorArea
												.add(Util.roundOffTwoDecimal(ocOccupancy.getFloorArea()));
									}
								}
								comparisonReportFloorDetail.setOcBltUpArea(ocBltUpArea);
								comparisonReportFloorDetail.setOcCarpetArea(ocCarpetArea);
								comparisonReportFloorDetail.setOcFloorArea(ocFloorArea);
							}

							BigDecimal bltUpAreaDeviation = getDeviation(comparisonReportFloorDetail.getOcBltUpArea(),
									comparisonReportFloorDetail.getPermitBltUpArea());
							BigDecimal carpetAreaDeviation = getDeviation(comparisonReportFloorDetail.getOcCarpetArea(),
									comparisonReportFloorDetail.getPermitCarpetArea());
							BigDecimal floorAreaDeviation = getDeviation(comparisonReportFloorDetail.getOcFloorArea(),
									comparisonReportFloorDetail.getPermitFloorArea());

							comparisonReportFloorDetail.setBltUpAreaDeviation(bltUpAreaDeviation);
							comparisonReportFloorDetail.setCarpetAreaDeviation(carpetAreaDeviation);
							comparisonReportFloorDetail.setFloorAreaDeviation(floorAreaDeviation);
							comparisonReportFloorDetail.setNumber(Long.valueOf(permitFloor.getNumber()));
							comparisonReportFloorDetails.add(comparisonReportFloorDetail);
						}

						blockDetail.setComparisonReportFloorDetails(comparisonReportFloorDetails);
					}
					BigDecimal noOfFloorsPermit = permitBuilding.getFloorsAboveGround();
					BigDecimal buildingHeight = permitBuilding.getBuildingHeight();

					blockDetail.setNoOfFloorsPermit(Long.valueOf(noOfFloorsPermit.toString()));
					blockDetail.setHgtFromGroundPermit(buildingHeight);
				}

				blockDetails.add(blockDetail);
			}
		}

		return blockDetails;

	}
	
	private List<OcComparisonBlockDetail> buildComparisonForAlt(Plan permit, Plan oc,String alterationSubService) {
		List<OcComparisonBlockDetail> blockDetails = new ArrayList<>();
		List<Block> permitBlks = permit.getBlocks();
		List<Block> ocBlks = oc.getBlocks();

		if (!permitBlks.isEmpty()) {

			for (Block permitBlk : permitBlks) {
				Block currentOcBlk = null;
				for (Block ocBlk : ocBlks) {
					if (ocBlk.getNumber().equalsIgnoreCase(permitBlk.getNumber())) {
						currentOcBlk = ocBlk;
					}
				}
				OcComparisonBlockDetail blockDetail = new OcComparisonBlockDetail();
				updateSetBackDetails(blockDetail, permitBlk, currentOcBlk);
				blockDetail.setNumber(Long.valueOf(permitBlk.getNumber()));
				Building permitBuilding = permitBlk.getBuilding();
				Building ocBuilding = null;
				if (currentOcBlk != null) {
					ocBuilding = currentOcBlk.getBuilding();
				}
				if (permitBuilding != null) {

					List<Floor> permitFloors = permitBuilding.getFloors();
					List<Floor> ocFloors = new ArrayList<>();
					if (ocBuilding != null) {
						ocFloors = ocBuilding.getFloors();
						BigDecimal noOfFloorsOc = ocBuilding.getFloorsAboveGround();
						BigDecimal buildingHeight = ocBuilding.getBuildingHeight();
						blockDetail.setNoOfFloorsOc(Long.valueOf(noOfFloorsOc.toString()));
						blockDetail.setHghtFromGroundOc(buildingHeight);
					}

					if (permitFloors != null && !permitFloors.isEmpty()) {
						List<OcComparisonReportFloorDetail> comparisonReportFloorDetails = new ArrayList<>();
						for (Floor permitFloor : permitFloors) {
							Floor ocFloor = null;

							if (ocFloors != null && !ocFloors.isEmpty()) {
								for (Floor floor : ocFloors) {
									if (floor.getNumber() == permitFloor.getNumber()) {
										ocFloor = floor;
									}
								}
							}
							OcComparisonReportFloorDetail comparisonReportFloorDetail = new OcComparisonReportFloorDetail();
							List<Occupancy> permitOccupancies = permitFloor.getOccupancies();
							BigDecimal permitBltUpArea = BigDecimal.ZERO;
							BigDecimal permitCarpetArea = BigDecimal.ZERO;
							BigDecimal permitFloorArea = BigDecimal.ZERO;
							if (!permitOccupancies.isEmpty()) {
								for (Occupancy occupancy : permitOccupancies) {
									permitBltUpArea = permitBltUpArea.add(occupancy.getBuiltUpArea());
									permitCarpetArea = permitCarpetArea.add(occupancy.getCarpetArea());
									permitFloorArea = permitFloorArea.add(occupancy.getFloorArea());
								}
							}
							comparisonReportFloorDetail.setPermitBltUpArea(Util.roundOffTwoDecimal(permitBltUpArea));
							comparisonReportFloorDetail.setPermitCarpetArea(Util.roundOffTwoDecimal(permitCarpetArea));
							comparisonReportFloorDetail.setPermitFloorArea(Util.roundOffTwoDecimal(permitFloorArea));

							if (ocFloor != null) {
								List<Occupancy> ocOccupancies = ocFloor.getOccupancies();
								BigDecimal ocBltUpArea = BigDecimal.ZERO;
								BigDecimal ocCarpetArea = BigDecimal.ZERO;
								BigDecimal ocFloorArea = BigDecimal.ZERO;
								if (!ocOccupancies.isEmpty()) {
									for (Occupancy ocOccupancy : ocOccupancies) {
										ocBltUpArea = ocBltUpArea
												.add(Util.roundOffTwoDecimal(ocOccupancy.getBuiltUpArea()));
										ocCarpetArea = ocCarpetArea
												.add(Util.roundOffTwoDecimal(ocOccupancy.getCarpetArea()));
										ocFloorArea = ocFloorArea
												.add(Util.roundOffTwoDecimal(ocOccupancy.getFloorArea()));
									}
								}
								comparisonReportFloorDetail.setOcBltUpArea(ocBltUpArea);
								comparisonReportFloorDetail.setOcCarpetArea(ocCarpetArea);
								comparisonReportFloorDetail.setOcFloorArea(ocFloorArea);
							}

							BigDecimal bltUpAreaDeviation = getDeviation(comparisonReportFloorDetail.getOcBltUpArea(),
									comparisonReportFloorDetail.getPermitBltUpArea());
							BigDecimal carpetAreaDeviation = getDeviation(comparisonReportFloorDetail.getOcCarpetArea(),
									comparisonReportFloorDetail.getPermitCarpetArea());
							BigDecimal floorAreaDeviation = getDeviation(comparisonReportFloorDetail.getOcFloorArea(),
									comparisonReportFloorDetail.getPermitFloorArea());

							comparisonReportFloorDetail.setBltUpAreaDeviation(bltUpAreaDeviation);
							comparisonReportFloorDetail.setCarpetAreaDeviation(carpetAreaDeviation);
							comparisonReportFloorDetail.setFloorAreaDeviation(floorAreaDeviation);
							comparisonReportFloorDetail.setNumber(Long.valueOf(permitFloor.getNumber()));
							comparisonReportFloorDetails.add(comparisonReportFloorDetail);
						}

						blockDetail.setComparisonReportFloorDetails(comparisonReportFloorDetails);
					}
					BigDecimal noOfFloorsPermit = permitBuilding.getFloorsAboveGround();
					BigDecimal buildingHeight = permitBuilding.getBuildingHeight();

					blockDetail.setNoOfFloorsPermit(Long.valueOf(noOfFloorsPermit.toString()));
					blockDetail.setHgtFromGroundPermit(buildingHeight);
				}

				blockDetails.add(blockDetail);
			}
		}

		return blockDetails;

	}
	
	private void updateSetBackDetails(OcComparisonBlockDetail blockDetail,Block permitBlk,Block ocBlk) {
		
		SetBack setBackPermit = (permitBlk.getSetBacks()!=null && permitBlk.getSetBacks().size()>0)?permitBlk.getSetBacks().get(0):null;
		SetBack setBackOc = (ocBlk.getSetBacks()!=null && ocBlk.getSetBacks().size()>0)?ocBlk.getSetBacks().get(0):null;
		
		if(setBackPermit ==null || setBackOc ==null)
			return;
		
		BigDecimal minFrontPermit = BigDecimal.ZERO;
		BigDecimal minRearPermit = BigDecimal.ZERO;
		BigDecimal minSide1Permit = BigDecimal.ZERO;
		BigDecimal minSide2Permit = BigDecimal.ZERO;
		BigDecimal totalCumulativeFrontAndRearPermit = BigDecimal.ZERO;
		BigDecimal totalCumulativeSidePermit = BigDecimal.ZERO;
		
		if (setBackPermit.getFrontYard() != null)
			minFrontPermit = setBackPermit.getFrontYard().getMinimumDistance();
		if (setBackPermit.getRearYard() != null)
			minRearPermit = setBackPermit.getRearYard().getMinimumDistance();
		if (setBackPermit.getSideYard1() != null)
			minSide1Permit = setBackPermit.getSideYard1().getMinimumDistance();
		if (setBackPermit.getSideYard2() != null)
			minSide2Permit = setBackPermit.getSideYard2().getMinimumDistance();
		
		totalCumulativeFrontAndRearPermit = minFrontPermit.add(minRearPermit);
		totalCumulativeSidePermit = minSide1Permit.add(minSide2Permit);
		
		blockDetail.setMinFrontPermit(minFrontPermit);
		blockDetail.setMinRearPermit(minRearPermit);
		blockDetail.setMinSide1Permit(minSide1Permit);
		blockDetail.setMinSide2Permit(minSide2Permit);
		blockDetail.setTotalCumulativeFrontAndRearPermit(totalCumulativeFrontAndRearPermit);
		blockDetail.setTotalCumulativeSidePermit(totalCumulativeSidePermit);
		
		BigDecimal minFrontOc = BigDecimal.ZERO;
		BigDecimal minRearOc = BigDecimal.ZERO;
		BigDecimal minSide1Oc = BigDecimal.ZERO;
		BigDecimal minSide2Oc = BigDecimal.ZERO;
		BigDecimal totalCumulativeFrontAndRearOc = BigDecimal.ZERO;
		BigDecimal totalCumulativeSideOc = BigDecimal.ZERO;
		
		if (setBackOc.getFrontYard() != null)
			minFrontOc = setBackOc.getFrontYard().getMinimumDistance();
		if (setBackOc.getRearYard() != null)
			minRearOc = setBackOc.getRearYard().getMinimumDistance();
		if (setBackOc.getSideYard1() != null)
			minSide1Oc = setBackOc.getSideYard1().getMinimumDistance();
		if (setBackOc.getSideYard2() != null)
			minSide2Oc = setBackOc.getSideYard2().getMinimumDistance();
		
		totalCumulativeFrontAndRearOc = minFrontOc.add(minRearOc);
		totalCumulativeSideOc = minSide1Oc.add(minSide2Oc);
		
		blockDetail.setMinFrontOc(minFrontOc);
		blockDetail.setMinRearOc(minRearOc);
		blockDetail.setMinSide1Oc(minSide1Oc);
		blockDetail.setMinSide2Oc(minSide2Oc);
		blockDetail.setTotalCumulativeFrontAndRearOc(totalCumulativeFrontAndRearOc);
		blockDetail.setTotalCumulativeSideOc(totalCumulativeSideOc);
	}
	
	private BigDecimal getDeviation(BigDecimal ocValue, BigDecimal permitValue) {
		BigDecimal numerator = ocValue.subtract(permitValue).multiply(BigDecimal.valueOf(100));
		BigDecimal finalValue = permitValue.compareTo(BigDecimal.ZERO) > 0
				? numerator.divide(permitValue, DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS)
				: numerator;
		return finalValue;
	}

	private List<ScrutinyDetail> buildReportObject(List<OcComparisonBlockDetail> ocComparison, Plan permitPlan,
			Plan ocPlan) {

		List<ScrutinyDetail> scrutinyDetails = new ArrayList<>();

		scrutinyDetails.add(getFarBuildReport(permitPlan, ocPlan));
		scrutinyDetails.add(getCoverageBuildReport(permitPlan, ocPlan));
		for (OcComparisonBlockDetail blockDetail : ocComparison) {
			Map<String, String> floorNos = new HashMap<>();
			Map<String, String> bldngHgts = new HashMap<>();

			ScrutinyDetail bltUpAreaSd = new ScrutinyDetail();
			bltUpAreaSd.setKey("Block_" + blockDetail.getNumber() + "_" + "BuiltUp Area");
			bltUpAreaSd.addColumnHeading(1, "Floor");
			bltUpAreaSd.addColumnHeading(2, "Oc built up area");
			bltUpAreaSd.addColumnHeading(3, "Permit built up area");
			bltUpAreaSd.addColumnHeading(4, "Deviation in %");
			//bltUpAreaSd.addColumnHeading(5, "Status");

			ScrutinyDetail floorAreaSd = new ScrutinyDetail();
			floorAreaSd.setKey("Block_" + blockDetail.getNumber() + "_" + "Floor Area");
			floorAreaSd.addColumnHeading(1, "Floor");
			floorAreaSd.addColumnHeading(2, "Oc floor area");
			floorAreaSd.addColumnHeading(3, "Permit floor area");
			floorAreaSd.addColumnHeading(4, "Deviation in %");
			//floorAreaSd.addColumnHeading(5, "Status");

			ScrutinyDetail crptAreaSd = new ScrutinyDetail();
			crptAreaSd.setKey("Block_" + blockDetail.getNumber() + "_" + "Carpet Area");
			crptAreaSd.addColumnHeading(1, "Floor");
			crptAreaSd.addColumnHeading(2, "Oc carpet area");
			crptAreaSd.addColumnHeading(3, "Permit carpet area");
			crptAreaSd.addColumnHeading(4, "Deviation in %");
			//crptAreaSd.addColumnHeading(5, "Status");

			ScrutinyDetail floors = new ScrutinyDetail();
			floors.setKey("Block_" + blockDetail.getNumber() + "_" + "Number of Floors");
			floors.addColumnHeading(1, "Oc Floors");
			floors.addColumnHeading(2, "Permit Floors");
			floors.addColumnHeading(3, "Deviation in %");
			//floors.addColumnHeading(3, "Status");

			floorNos.put("Oc Floors", blockDetail.getNoOfFloorsOc().toString());
			floorNos.put("Permit Floors", blockDetail.getNoOfFloorsPermit().toString());
			floorNos.put("Deviation in %",DxfFileConstants.NA);
			floorNos.put("Status",
					blockDetail.getNoOfFloorsOc() > blockDetail.getNoOfFloorsPermit() ? Result.Verify.getResultVal()
							: Result.Verify.getResultVal());
			floors.getDetail().add(floorNos);

			ScrutinyDetail bldngHgt = new ScrutinyDetail();
			bldngHgt.setKey("Block_" + blockDetail.getNumber() + "_" + "Height of building");
			bldngHgt.addColumnHeading(1, "Oc building height");
			bldngHgt.addColumnHeading(2, "Permit building height");
			bldngHgt.addColumnHeading(3, "Deviation in %");
			//bldngHgt.addColumnHeading(3, "Status");

			bldngHgts.put("Oc building height", blockDetail.getHghtFromGroundOc().toString());
			bldngHgts.put("Permit building height", blockDetail.getHgtFromGroundPermit().toString());
			bldngHgts.put("Deviation in %", getDeviation(blockDetail.getHghtFromGroundOc(), blockDetail.getHgtFromGroundPermit()).toString());
			bldngHgts.put("Status",
					blockDetail.getHghtFromGroundOc().compareTo(blockDetail.getHgtFromGroundPermit()) > 0
							? Result.Verify.getResultVal()
							: Result.Verify.getResultVal());
			bldngHgt.getDetail().add(bldngHgts);
			
			scrutinyDetails.add(getSetbackBuildReport(blockDetail, permitPlan, ocPlan));
			
			List<OcComparisonReportFloorDetail> comparisonReportFloorDetails = blockDetail
					.getComparisonReportFloorDetails();
			if (!comparisonReportFloorDetails.isEmpty()) {
				for (OcComparisonReportFloorDetail floor : comparisonReportFloorDetails) {
					Map<String, String> bltUpAreaDetails = new HashMap<>();
					Map<String, String> flrAreaDetails = new HashMap<>();
					Map<String, String> crptAreaDetails = new HashMap<>();

					bltUpAreaDetails.put("Floor", floor.getNumber().toString());
					bltUpAreaDetails.put("Oc built up area", floor.getOcBltUpArea().toString());
					bltUpAreaDetails.put("Permit built up area", floor.getPermitBltUpArea().toString());
					BigDecimal bltUpAreaDeviation = floor.getBltUpAreaDeviation();
					bltUpAreaDetails.put("Deviation in %", bltUpAreaDeviation.toString());

					bltUpAreaDetails.put("Status",
							bltUpAreaDeviation.compareTo(DEVIATION_VALUE) > 0 ? Result.Verify.getResultVal()
									: Result.Verify.getResultVal());
					bltUpAreaSd.getDetail().add(bltUpAreaDetails);

					flrAreaDetails.put("Floor", floor.getNumber().toString());
					flrAreaDetails.put("Oc floor area", floor.getOcFloorArea().toString());
					flrAreaDetails.put("Permit floor area", floor.getPermitFloorArea().toString());
					BigDecimal flrAreaDeviation = floor.getFloorAreaDeviation();
					flrAreaDetails.put("Deviation in %", flrAreaDeviation.toString());
					flrAreaDetails.put("Status",
							flrAreaDeviation.compareTo(DEVIATION_VALUE) > 0 ? Result.Verify.getResultVal()
									: Result.Verify.getResultVal());
					floorAreaSd.getDetail().add(flrAreaDetails);

					crptAreaDetails.put("Floor", floor.getNumber().toString());
					crptAreaDetails.put("Oc carpet area", floor.getOcCarpetArea().toString());
					crptAreaDetails.put("Permit carpet area", floor.getPermitCarpetArea().toString());
					BigDecimal crptAreaDeviation = floor.getCarpetAreaDeviation();
					crptAreaDetails.put("Deviation in %", crptAreaDeviation.toString());
					crptAreaDetails.put("Status",
							crptAreaDeviation.compareTo(DEVIATION_VALUE) > 0 ? Result.Verify.getResultVal()
									: Result.Verify.getResultVal());
					crptAreaSd.getDetail().add(crptAreaDetails);
				}
			}
			scrutinyDetails.add(bltUpAreaSd);
			scrutinyDetails.add(floorAreaSd);
			scrutinyDetails.add(crptAreaSd);
			scrutinyDetails.add(floors);
			scrutinyDetails.add(bldngHgt);
		}
		return scrutinyDetails;
	}
	
	private List<ScrutinyDetail> buildReportObjectAlt(List<OcComparisonBlockDetail> ocComparison, Plan permitPlan,
			Plan ocPlan,String alterationSubService) {

		List<ScrutinyDetail> scrutinyDetails = new ArrayList<>();

		scrutinyDetails.add(getFarBuildReportAlt(permitPlan, ocPlan));
		scrutinyDetails.add(getCoverageBuildReportAlt(permitPlan, ocPlan));
		for (OcComparisonBlockDetail blockDetail : ocComparison) {
			Map<String, String> floorNos = new HashMap<>();
			Map<String, String> bldngHgts = new HashMap<>();

			ScrutinyDetail bltUpAreaSd = new ScrutinyDetail();
			bltUpAreaSd.setKey("Block_" + blockDetail.getNumber() + "_" + "BuiltUp Area");
			bltUpAreaSd.addColumnHeading(1, "Floor");
			bltUpAreaSd.addColumnHeading(2, "Previous built up area");
			bltUpAreaSd.addColumnHeading(3, "New built up area");
			bltUpAreaSd.addColumnHeading(4, "Deviation in %");
			//bltUpAreaSd.addColumnHeading(5, "Status");

			ScrutinyDetail floorAreaSd = new ScrutinyDetail();
			floorAreaSd.setKey("Block_" + blockDetail.getNumber() + "_" + "Floor Area");
			floorAreaSd.addColumnHeading(1, "Floor");
			floorAreaSd.addColumnHeading(2, "Previous floor area");
			floorAreaSd.addColumnHeading(3, "New floor area");
			floorAreaSd.addColumnHeading(4, "Deviation in %");
			//floorAreaSd.addColumnHeading(5, "Status");

			ScrutinyDetail crptAreaSd = new ScrutinyDetail();
			crptAreaSd.setKey("Block_" + blockDetail.getNumber() + "_" + "Carpet Area");
			crptAreaSd.addColumnHeading(1, "Floor");
			crptAreaSd.addColumnHeading(2, "Previous carpet area");
			crptAreaSd.addColumnHeading(3, "New carpet area");
			crptAreaSd.addColumnHeading(4, "Deviation in %");
			//crptAreaSd.addColumnHeading(5, "Status");

			ScrutinyDetail floors = new ScrutinyDetail();
			floors.setKey("Block_" + blockDetail.getNumber() + "_" + "Number of Floors");
			floors.addColumnHeading(1, "Previous Floors");
			floors.addColumnHeading(2, "New Floors");
			floors.addColumnHeading(3, "Deviation in %");
			//floors.addColumnHeading(3, "Status");

			floorNos.put("Previous Floors", blockDetail.getNoOfFloorsPermit().toString());
			floorNos.put("New Floors", blockDetail.getNoOfFloorsOc().toString());
			floorNos.put("Deviation in %",DxfFileConstants.NA);
			floorNos.put("Status",
					blockDetail.getNoOfFloorsOc() > blockDetail.getNoOfFloorsPermit() ? Result.Verify.getResultVal()
							: Result.Verify.getResultVal());
			floors.getDetail().add(floorNos);

			ScrutinyDetail bldngHgt = new ScrutinyDetail();
			bldngHgt.setKey("Block_" + blockDetail.getNumber() + "_" + "Height of building");
			bldngHgt.addColumnHeading(1, "Previous building height");
			bldngHgt.addColumnHeading(2, "New building height");
			bldngHgt.addColumnHeading(3, "Deviation in %");
			//bldngHgt.addColumnHeading(3, "Status");

			bldngHgts.put("Previous building height", blockDetail.getHgtFromGroundPermit().toString());
			bldngHgts.put("New building height",blockDetail.getHghtFromGroundOc().toString());
			bldngHgts.put("Deviation in %", getDeviation(blockDetail.getHghtFromGroundOc(), blockDetail.getHgtFromGroundPermit()).toString());
			bldngHgts.put("Status",
					blockDetail.getHghtFromGroundOc().compareTo(blockDetail.getHgtFromGroundPermit()) > 0
							? Result.Verify.getResultVal()
							: Result.Verify.getResultVal());
			bldngHgt.getDetail().add(bldngHgts);
			
			scrutinyDetails.add(getSetbackBuildReportAlt(blockDetail, permitPlan, ocPlan));
			
			List<OcComparisonReportFloorDetail> comparisonReportFloorDetails = blockDetail
					.getComparisonReportFloorDetails();
			if (!comparisonReportFloorDetails.isEmpty()) {
				for (OcComparisonReportFloorDetail floor : comparisonReportFloorDetails) {
					Map<String, String> bltUpAreaDetails = new HashMap<>();
					Map<String, String> flrAreaDetails = new HashMap<>();
					Map<String, String> crptAreaDetails = new HashMap<>();

					bltUpAreaDetails.put("Floor", floor.getNumber().toString());
					bltUpAreaDetails.put("Previous built up area", floor.getPermitBltUpArea().toString());
					bltUpAreaDetails.put("New built up area", floor.getOcBltUpArea().toString());
					BigDecimal bltUpAreaDeviation = floor.getBltUpAreaDeviation();
					bltUpAreaDetails.put("Deviation in %", bltUpAreaDeviation.toString());

					bltUpAreaDetails.put("Status",
							bltUpAreaDeviation.compareTo(DEVIATION_VALUE) > 0 ? Result.Verify.getResultVal()
									: Result.Verify.getResultVal());
					bltUpAreaSd.getDetail().add(bltUpAreaDetails);

					flrAreaDetails.put("Floor", floor.getNumber().toString());
					flrAreaDetails.put("Previous floor area", floor.getPermitFloorArea().toString());
					flrAreaDetails.put("New floor area", floor.getOcFloorArea().toString());
					BigDecimal flrAreaDeviation = floor.getFloorAreaDeviation();
					flrAreaDetails.put("Deviation in %", flrAreaDeviation.toString());
					flrAreaDetails.put("Status",
							flrAreaDeviation.compareTo(DEVIATION_VALUE) > 0 ? Result.Verify.getResultVal()
									: Result.Verify.getResultVal());
					floorAreaSd.getDetail().add(flrAreaDetails);

					crptAreaDetails.put("Floor", floor.getNumber().toString());
					crptAreaDetails.put("Previous carpet area", floor.getPermitCarpetArea().toString());
					crptAreaDetails.put("New carpet area", floor.getOcCarpetArea().toString());
					BigDecimal crptAreaDeviation = floor.getCarpetAreaDeviation();
					crptAreaDetails.put("Deviation in %", crptAreaDeviation.toString());
					crptAreaDetails.put("Status",
							crptAreaDeviation.compareTo(DEVIATION_VALUE) > 0 ? Result.Verify.getResultVal()
									: Result.Verify.getResultVal());
					crptAreaSd.getDetail().add(crptAreaDetails);
				}
			}
			scrutinyDetails.add(bltUpAreaSd);
			scrutinyDetails.add(floorAreaSd);
			scrutinyDetails.add(crptAreaSd);
			scrutinyDetails.add(floors);
			scrutinyDetails.add(bldngHgt);
		}
		return scrutinyDetails;
	}

	private ScrutinyDetail getFarBuildReport(Plan permitPlan, Plan ocPlan) {
		ScrutinyDetail scrutinyDetail = new ScrutinyDetail();
		scrutinyDetail.setKey("Common_FAR");
		scrutinyDetail.addColumnHeading(1, "Oc Far");
		scrutinyDetail.addColumnHeading(2, "Permit Far");
		scrutinyDetail.addColumnHeading(3, "Deviation in %");

		Map<String, String> details = new HashMap<>();

		details.put("Oc Far", ocPlan.getFarDetails().getProvidedFar().toString());
		details.put("Permit Far", permitPlan.getFarDetails().getProvidedFar().toString());
		BigDecimal farDeviation = getDeviation(new BigDecimal(ocPlan.getFarDetails().getProvidedFar()),
				new BigDecimal(permitPlan.getFarDetails().getProvidedFar()));
		details.put("Deviation in %", farDeviation.toString());

		scrutinyDetail.getDetail().add(details);
		return scrutinyDetail;
	}
	
	private ScrutinyDetail getFarBuildReportAlt(Plan permitPlan, Plan ocPlan) {
		ScrutinyDetail scrutinyDetail = new ScrutinyDetail();
		scrutinyDetail.setKey("Common_FAR");
		scrutinyDetail.addColumnHeading(1, "Previous Far");
		scrutinyDetail.addColumnHeading(2, "New Far");
		scrutinyDetail.addColumnHeading(3, "Deviation in %");

		Map<String, String> details = new HashMap<>();

		details.put("Previous Far", permitPlan.getFarDetails().getProvidedFar().toString());
		details.put("New Far", ocPlan.getFarDetails().getProvidedFar().toString());
		BigDecimal farDeviation = getDeviation(new BigDecimal(ocPlan.getFarDetails().getProvidedFar()),
				new BigDecimal(permitPlan.getFarDetails().getProvidedFar()));
		details.put("Deviation in %", farDeviation.toString());

		scrutinyDetail.getDetail().add(details);
		return scrutinyDetail;
	}
	
	private ScrutinyDetail getCoverageBuildReport(Plan permitPlan, Plan ocPlan) {
		ScrutinyDetail scrutinyDetail = new ScrutinyDetail();
		scrutinyDetail.setKey("Common_Coverage in Percentage");
		scrutinyDetail.addColumnHeading(1, "Oc Coverage");
		scrutinyDetail.addColumnHeading(2, "Permit Coverage");
		scrutinyDetail.addColumnHeading(3, "Deviation in %");

		Map<String, String> details = new HashMap<>();

		details.put("Oc Coverage", ocPlan.getCoverage().toString());
		details.put("Permit Coverage", permitPlan.getCoverage().toString());
		BigDecimal farDeviation = getDeviation(ocPlan.getCoverage(),permitPlan.getCoverage());
		details.put("Deviation in %", farDeviation.toString());

		scrutinyDetail.getDetail().add(details);
		return scrutinyDetail;
	}
	
	private ScrutinyDetail getCoverageBuildReportAlt(Plan permitPlan, Plan ocPlan) {
		ScrutinyDetail scrutinyDetail = new ScrutinyDetail();
		scrutinyDetail.setKey("Common_Coverage in Percentage");
		scrutinyDetail.addColumnHeading(1, "Previous Coverage");
		scrutinyDetail.addColumnHeading(2, "New Coverage");
		scrutinyDetail.addColumnHeading(3, "Deviation in %");

		Map<String, String> details = new HashMap<>();

		details.put("Previous Coverage", permitPlan.getCoverage().toString());
		details.put("New Coverage", ocPlan.getCoverage().toString());
		BigDecimal farDeviation = getDeviation(ocPlan.getCoverage(),permitPlan.getCoverage());
		details.put("Deviation in %", farDeviation.toString());

		scrutinyDetail.getDetail().add(details);
		return scrutinyDetail;
	}
	
	private ScrutinyDetail getSetbackBuildReport(OcComparisonBlockDetail blockDetail,Plan permitPlan, Plan ocPlan) {
		ScrutinyDetail setback = new ScrutinyDetail();
		setback.setKey("Block_" + blockDetail.getNumber() + "_" + "Setback");
		setback.addColumnHeading(1, "Description");
		setback.addColumnHeading(2, "OC");
		setback.addColumnHeading(3, "Permit");
		setback.addColumnHeading(4, "Deviation in %");
		setback.addColumnHeading(5, "Status");
		boolean isLowRisk=false;
		if(permitPlan.getPlanInformation().isLowRiskBuilding()!=ocPlan.getPlanInformation().isLowRiskBuilding()) {
			ocPlan.addError("Oc-setback", "Risk type of the project should be same as in buliding permit");
			return setback;
		}else {
			isLowRisk=ocPlan.getPlanInformation().isLowRiskBuilding();
		}
		
//		if(isLowRisk) {
//			
//			Map<String, String> details = new HashMap<>();
//			details.put("Description", "Min Front setback ");
//			details.put("OC", blockDetail.getMinFrontOc().toString());
//			details.put("Permit", blockDetail.getMinFrontPermit().toString());
//			BigDecimal deviation = getDeviation(blockDetail.getMinFrontOc(),blockDetail.getMinFrontPermit());
//			details.put("Deviation in %", deviation.toString());
//			details.put("Status",
//					deviation.compareTo(SETBACK_DEVIATION_VALUE) <= 0 ? Result.Accepted.getResultVal()
//							: Result.Not_Accepted.getResultVal());
//			setback.getDetail().add(details);
//			
//			Map<String, String> details1 = new HashMap<>();
//			details1.put("Description", "Total cumulative Front and rear Setback ");
//			details1.put("OC", blockDetail.getTotalCumulativeFrontAndRearOc().toString());
//			details1.put("Permit", blockDetail.getTotalCumulativeFrontAndRearPermit().toString());
//			BigDecimal deviation1 = getDeviation(blockDetail.getTotalCumulativeFrontAndRearOc(),blockDetail.getTotalCumulativeFrontAndRearPermit());
//			details1.put("Deviation in %", deviation1.toString());
//			details1.put("Status",
//					deviation1.compareTo(SETBACK_DEVIATION_VALUE) <= 0 ? Result.Accepted.getResultVal()
//							: Result.Not_Accepted.getResultVal());
//			setback.getDetail().add(details1);
//			
//			Map<String, String> details2 = new HashMap<>();
//			details2.put("Description", "Total cumulative side Setback ");
//			details2.put("OC", blockDetail.getTotalCumulativeSideOc().toString());
//			details2.put("Permit", blockDetail.getTotalCumulativeSidePermit().toString());
//			BigDecimal deviation2 = getDeviation(blockDetail.getTotalCumulativeSideOc(),blockDetail.getTotalCumulativeSidePermit());
//			details2.put("Deviation in %", deviation2.toString());
//			details2.put("Status",
//					deviation2.compareTo(SETBACK_DEVIATION_VALUE) <= 0 ? Result.Accepted.getResultVal()
//							: Result.Not_Accepted.getResultVal());
//			setback.getDetail().add(details2);
//
//		}else {
//			Map<String, String> details = new HashMap<>();
//			details.put("Description", "Min Front setback ");
//			details.put("OC", blockDetail.getMinFrontOc().toString());
//			details.put("Permit", blockDetail.getMinFrontPermit().toString());
//			BigDecimal deviation = getDeviation(blockDetail.getMinFrontOc(),blockDetail.getMinFrontPermit());
//			details.put("Deviation in %", deviation.toString());
//			details.put("Status",
//					deviation.compareTo(SETBACK_DEVIATION_VALUE) <= 0 ? Result.Accepted.getResultVal()
//							: Result.Not_Accepted.getResultVal());
//			setback.getDetail().add(details);
//			
//			Map<String, String> details1 = new HashMap<>();
//			details1.put("Description", "Min Rear setback ");
//			details1.put("OC", blockDetail.getMinRearOc().toString());
//			details1.put("Permit", blockDetail.getMinRearPermit().toString());
//			BigDecimal deviation1 = getDeviation(blockDetail.getMinRearOc(),blockDetail.getMinRearPermit());
//			details1.put("Deviation in %", deviation1.toString());
//			details1.put("Status",
//					deviation1.compareTo(SETBACK_DEVIATION_VALUE) <= 0 ? Result.Accepted.getResultVal()
//							: Result.Not_Accepted.getResultVal());
//			setback.getDetail().add(details1);
//			
//			Map<String, String> details2 = new HashMap<>();
//			details2.put("Description", "Min Side1 setback ");
//			details2.put("OC", blockDetail.getMinSide1Oc().toString());
//			details2.put("Permit", blockDetail.getMinSide1Permit().toString());
//			BigDecimal deviation2 = getDeviation(blockDetail.getMinSide1Oc(),blockDetail.getMinSide1Permit());
//			details2.put("Deviation in %", deviation2.toString());
//			details2.put("Status",
//					deviation2.compareTo(SETBACK_DEVIATION_VALUE) <= 0 ? Result.Accepted.getResultVal()
//							: Result.Not_Accepted.getResultVal());
//			setback.getDetail().add(details2);
//			
//			Map<String, String> details3 = new HashMap<>();
//			details3.put("Description", "Min Side2 setback ");
//			details3.put("OC", blockDetail.getMinSide1Oc().toString());
//			details3.put("Permit", blockDetail.getMinSide1Permit().toString());
//			BigDecimal deviation3 = getDeviation(blockDetail.getMinSide1Oc(),blockDetail.getMinSide1Permit());
//			details3.put("Deviation in %", deviation3.toString());
//			details3.put("Status",
//					deviation3.compareTo(SETBACK_DEVIATION_VALUE) <= 0 ? Result.Accepted.getResultVal()
//							: Result.Not_Accepted.getResultVal());
//			setback.getDetail().add(details3);
//		}
		
		
		Map<String, String> details = new HashMap<>();
		details.put("Description", "Min Front setback ");
		details.put("OC", blockDetail.getMinFrontOc().toString());
		details.put("Permit", blockDetail.getMinFrontPermit().toString());
		BigDecimal deviation = getDeviation(blockDetail.getMinFrontOc(),blockDetail.getMinFrontPermit());
		details.put("Deviation in %", deviation.abs().toString());
		details.put("Status",
				deviation.compareTo(SETBACK_DEVIATION_VALUE) >= 0 ? Result.Accepted.getResultVal()
						: Result.Not_Accepted.getResultVal());
		setback.getDetail().add(details);
		
		Map<String, String> details1 = new HashMap<>();
		details1.put("Description", "Min Rear setback ");
		details1.put("OC", blockDetail.getMinRearOc().toString());
		details1.put("Permit", blockDetail.getMinRearPermit().toString());
		BigDecimal deviation1 = getDeviation(blockDetail.getMinRearOc(),blockDetail.getMinRearPermit());
		details1.put("Deviation in %", deviation1.abs().toString());
		details1.put("Status",
				deviation1.compareTo(SETBACK_DEVIATION_VALUE) >= 0 ? Result.Accepted.getResultVal()
						: Result.Not_Accepted.getResultVal());
		setback.getDetail().add(details1);
		
		Map<String, String> details2 = new HashMap<>();
		details2.put("Description", "Min Side1 setback ");
		details2.put("OC", blockDetail.getMinSide1Oc().toString());
		details2.put("Permit", blockDetail.getMinSide1Permit().toString());
		BigDecimal deviation2 = getDeviation(blockDetail.getMinSide1Oc(),blockDetail.getMinSide1Permit());
		details2.put("Deviation in %", deviation2.abs().toString());
		details2.put("Status",
				deviation2.compareTo(SETBACK_DEVIATION_VALUE) >= 0 ? Result.Accepted.getResultVal()
						: Result.Not_Accepted.getResultVal());
		setback.getDetail().add(details2);
		
		Map<String, String> details3 = new HashMap<>();
		details3.put("Description", "Min Side2 setback ");
		details3.put("OC", blockDetail.getMinSide2Oc().toString());
		details3.put("Permit", blockDetail.getMinSide2Permit().toString());
		BigDecimal deviation3 = getDeviation(blockDetail.getMinSide2Oc(),blockDetail.getMinSide2Permit());
		details3.put("Deviation in %", deviation3.abs().toString());
		details3.put("Status",
				deviation3.compareTo(SETBACK_DEVIATION_VALUE) >= 0 ? Result.Accepted.getResultVal()
						: Result.Not_Accepted.getResultVal());
		setback.getDetail().add(details3);

		
		return setback;
	}
	
	private ScrutinyDetail getSetbackBuildReportAlt(OcComparisonBlockDetail blockDetail,Plan permitPlan, Plan ocPlan) {
		ScrutinyDetail setback = new ScrutinyDetail();
		setback.setKey("Block_" + blockDetail.getNumber() + "_" + "Setback");
		setback.addColumnHeading(1, "Description");
		setback.addColumnHeading(2, "Previous");
		setback.addColumnHeading(3, "New");
		setback.addColumnHeading(4, "Deviation in %");
		//setback.addColumnHeading(5, "Status");
		boolean isLowRisk=false;
		if(permitPlan.getPlanInformation().isLowRiskBuilding()!=ocPlan.getPlanInformation().isLowRiskBuilding()) {
			ocPlan.addError("Oc-setback", "Risk type of the project should be same as in buliding permit");
			return setback;
		}else {
			isLowRisk=ocPlan.getPlanInformation().isLowRiskBuilding();
		}
			
		Map<String, String> details = new HashMap<>();
		details.put("Description", "Min Front setback ");
		details.put("Previous", blockDetail.getMinFrontPermit().toString());
		details.put("New", blockDetail.getMinFrontOc().toString());
		BigDecimal deviation = getDeviation(blockDetail.getMinFrontOc(),blockDetail.getMinFrontPermit());
		details.put("Deviation in %", deviation.abs().toString());
		details.put("Status",
				deviation.compareTo(SETBACK_DEVIATION_VALUE) >= 0 ? Result.Verify.getResultVal()
						: Result.Verify.getResultVal());
		setback.getDetail().add(details);
		
		Map<String, String> details1 = new HashMap<>();
		details1.put("Description", "Min Rear setback ");
		details1.put("Previous", blockDetail.getMinRearPermit().toString());
		details1.put("New", blockDetail.getMinRearOc().toString());
		BigDecimal deviation1 = getDeviation(blockDetail.getMinRearOc(),blockDetail.getMinRearPermit());
		details1.put("Deviation in %", deviation1.abs().toString());
		details1.put("Status",
				deviation1.compareTo(SETBACK_DEVIATION_VALUE) >= 0 ? Result.Verify.getResultVal()
						: Result.Verify.getResultVal());
		setback.getDetail().add(details1);
		
		Map<String, String> details2 = new HashMap<>();
		details2.put("Description", "Min Side1 setback ");
		details2.put("Previous", blockDetail.getMinSide1Permit().toString());
		details2.put("New", blockDetail.getMinSide1Oc().toString());
		BigDecimal deviation2 = getDeviation(blockDetail.getMinSide1Oc(),blockDetail.getMinSide1Permit());
		details2.put("Deviation in %", deviation2.abs().toString());
		details2.put("Status",
				deviation2.compareTo(SETBACK_DEVIATION_VALUE) >= 0 ? Result.Verify.getResultVal()
						: Result.Verify.getResultVal());
		setback.getDetail().add(details2);
		
		Map<String, String> details3 = new HashMap<>();
		details3.put("Description", "Min Side2 setback ");
		details3.put("Previous", blockDetail.getMinSide2Permit().toString());
		details3.put("New", blockDetail.getMinSide2Oc().toString());
		BigDecimal deviation3 = getDeviation(blockDetail.getMinSide2Oc(),blockDetail.getMinSide2Permit());
		details3.put("Deviation in %", deviation3.abs().toString());
		details3.put("Status",
				deviation3.compareTo(SETBACK_DEVIATION_VALUE) >= 0 ? Result.Verify.getResultVal()
						: Result.Verify.getResultVal());
		setback.getDetail().add(details3);

		
		return setback;
	}

	private String buildQRCodeDetails(final EdcrApplication dcrApplication, boolean reportStatus) {
		StringBuilder qrCodeValue = new StringBuilder();
		qrCodeValue = !StringUtils.isEmpty(dcrApplication.getEdcrApplicationDetails().get(0).getDcrNumber())
				? qrCodeValue.append("OCDCR Number : ")
						.append(dcrApplication.getEdcrApplicationDetails().get(0).getDcrNumber()).append("\n")
				: qrCodeValue.append("OCDCR Number : ").append("N/A").append("\n");
		qrCodeValue = !StringUtils.isEmpty(dcrApplication.getApplicationNumber())
				? qrCodeValue.append("Application Number : ").append(dcrApplication.getApplicationNumber()).append("\n")
				: qrCodeValue.append("Application Number : ").append("N/A").append("\n");
		qrCodeValue = dcrApplication.getApplicationDate() != null
				? qrCodeValue.append("Application Date : ")
						.append(DateUtils.toDefaultDateFormat(dcrApplication.getApplicationDate())).append("\n")
				: qrCodeValue.append("Application Date : ").append("N/A").append("\n");
		String applicationTypeVal = dcrApplication.getApplicationType().getApplicationTypeVal();
		qrCodeValue = qrCodeValue.append("Application Type : ").append(applicationTypeVal).append("\n");
		if (ApplicationType.OCCUPANCY_CERTIFICATE.getApplicationTypeVal().equalsIgnoreCase(applicationTypeVal)) {
			qrCodeValue = qrCodeValue.append("Applicant Name : ").append(dcrApplication.getApplicantName())
					.append("\n");
		}
		// Map<String, String> serviceTypeList = DxfFileConstants.getServiceTypeList();
		Map<String, String> serviceTypeList = new ConcurrentHashMap<>();
		serviceTypeList.put("NEW_CONSTRUCTION", "New Construction");
		if (StringUtils.isNotBlank(dcrApplication.getServiceType())) {
			String serviceType = serviceTypeList.get(dcrApplication.getServiceType());
			qrCodeValue = qrCodeValue.append("Service Type : ").append(serviceType).append("\n");
		}

		qrCodeValue = qrCodeValue.append("Report Status :").append(reportStatus ? "Accepted" : "Not Accepted")
				.append("\n");
		return qrCodeValue.toString();
	}

	private List<ConditionalStyle> getConditonalStyles() {
		List<ConditionalStyle> conditionalStyles = new ArrayList<>();
		FetchCondition fc = new FetchCondition(STATUS, "Not Accepted");

		ConditionalStyle cs = new ConditionalStyle(fc, reportService.getDetailStyle(Color.RED));
		conditionalStyles.add(cs);

		fc = new FetchCondition(STATUS, "Accepted");

		cs = new ConditionalStyle(fc, reportService.getDetailStyle(new Color(0, 128, 0)));
		conditionalStyles.add(cs);

		fc = new FetchCondition(STATUS, "Verify");

		cs = new ConditionalStyle(fc, reportService.getDetailStyle(new Color(30, 144, 255)));
		conditionalStyles.add(cs);

		return conditionalStyles;
	}
}
