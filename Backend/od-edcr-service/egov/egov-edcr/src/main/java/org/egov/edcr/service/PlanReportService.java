package org.egov.edcr.service;

import static ar.com.fdvs.dj.domain.constants.Stretching.RELATIVE_TO_BAND_HEIGHT;
import static org.egov.edcr.utility.DcrConstants.DECIMALDIGITS_MEASUREMENTS;
import static org.egov.edcr.utility.DcrConstants.ROUNDMODE_MEASUREMENTS;
import static org.egov.infra.security.utils.SecureCodeUtils.generatePDF417Code;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.egov.common.entity.edcr.Block;
import org.egov.common.entity.edcr.Building;
import org.egov.common.entity.edcr.DcrReportBlockDetail;
import org.egov.common.entity.edcr.DcrReportDeclarationsDetail;
import org.egov.common.entity.edcr.DcrReportErrorDetail;
import org.egov.common.entity.edcr.DcrReportFloorDetail;
import org.egov.common.entity.edcr.DcrReportOutput;
import org.egov.common.entity.edcr.DcrReportPlanDetail;
import org.egov.common.entity.edcr.ElectricLine;
import org.egov.common.entity.edcr.Floor;
import org.egov.common.entity.edcr.Occupancy;
import org.egov.common.entity.edcr.OccupancyPercentage;
import org.egov.common.entity.edcr.OccupancyReport;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.Result;
import org.egov.common.entity.edcr.ScrutinyDetail;
import org.egov.common.entity.edcr.ScrutinyDetail.ColumnHeadingDetail;
import org.egov.common.entity.edcr.ShopOrWorkCumResidencialDetails;
import org.egov.common.entity.edcr.VirtualBuilding;
import org.egov.common.entity.edcr.VirtualBuildingReport;
import org.egov.edcr.autonumber.DcrApplicationNumberGenerator;
import org.egov.edcr.autonumber.OCPlanScrutinyNumberGenerator;
import org.egov.edcr.constants.DxfFileConstants;
import org.egov.edcr.constants.OdishaUlbs;
import org.egov.edcr.entity.ApplicationType;
import org.egov.edcr.entity.EdcrApplication;
import org.egov.edcr.entity.EdcrApplicationDetail;
import org.egov.edcr.utility.DcrConstants;
import org.egov.infra.admin.master.service.CityService;
import org.egov.infra.reporting.util.ReportUtil;
import org.egov.infra.utils.DateUtils;
import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ar.com.fdvs.dj.core.DJConstants;
import ar.com.fdvs.dj.core.DynamicJasperHelper;
import ar.com.fdvs.dj.core.layout.ClassicLayoutManager;
import ar.com.fdvs.dj.core.layout.HorizontalBandAlignment;
import ar.com.fdvs.dj.domain.AutoText;
import ar.com.fdvs.dj.domain.DJCalculation;
import ar.com.fdvs.dj.domain.DJDataSource;
import ar.com.fdvs.dj.domain.DynamicReport;
import ar.com.fdvs.dj.domain.Style;
import ar.com.fdvs.dj.domain.builders.ColumnBuilder;
import ar.com.fdvs.dj.domain.builders.ColumnBuilderException;
import ar.com.fdvs.dj.domain.builders.FastReportBuilder;
import ar.com.fdvs.dj.domain.constants.Font;
import ar.com.fdvs.dj.domain.constants.HorizontalAlign;
import ar.com.fdvs.dj.domain.constants.Page;
import ar.com.fdvs.dj.domain.constants.VerticalAlign;
import ar.com.fdvs.dj.domain.entities.Subreport;
import ar.com.fdvs.dj.domain.entities.columns.AbstractColumn;
import ar.com.fdvs.dj.domain.entities.conditionalStyle.ConditionalStyle;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

@Service
public class PlanReportService {
	private static final String TOTAL = "Total";
	private static final String DESCRIPTION = "description";
	private static final String RULE_NO = "RuleNo";
	private static final Logger LOG = Logger.getLogger(PlanReportService.class);
	public static final String BLOCK = "Block";
	public static final String STATUS = "Status";
	//public static  OdishaUlbs odishaUlbs = null;
	@Value("${edcr.client.subreport}")
	private boolean clientSpecificSubReport;
	@Autowired
	private CityService cityService;
	@Autowired
	private DcrApplicationNumberGenerator dcrApplicationNumberGenerator;
	@Autowired
	private OCPlanScrutinyNumberGenerator ocPlanScrutinyNumberGenerator;
	@Autowired
	private JasperReportService reportService;
	
	@Value("${rules.subrules.in.report.flag}")
	private boolean rulesSubrulesFlag;
	
	@Value("${dxf.to.pdf.flag:false}")
	protected boolean dxfToPdfFlag;

	public static final String FRONT_YARD_DESC = "Front Setback";
	public static final String REAR_YARD_DESC = "Rear Setback";
	public static final String SIDE_YARD1_DESC = "Side Setback 1";
	public static final String SIDE_YARD2_DESC = "Side Setback 2";
	public static final String BSMT_FRONT_YARD_DESC = "Basement Front Setback";
	public static final String BSMT_REAR_YARD_DESC = "Basement Rear Setback";
	public static final String BSMT_SIDE_YARD1_DESC = "Basement Side Setback 1";
	public static final String BSMT_SIDE_YARD2_DESC = "Basement Side Setback 2";
	public static final String BSMT_SIDE_YARD_DESC = "Basement Side Setback";
	public static final String SIDE_YARD_DESC = "Side Setback";
	private static final String SIDENUMBER = "Side Number";
	private static final String SIDENUMBER_NAME = "Setback";
	private static final String LEVEL = "Level";
	private static final String COMBINED_BLOCKS_SUMMARY_DETAILS = "Overall Summary";
	private static final String BLOCK_WISE_SUMMARY = "Block Wise Summary";
	private static final String ERROR_SUMMARY = "Error Summary";

	public InputStream generateDynamicReport(Plan plan, EdcrApplication dcrApplication) {
		FastReportBuilder drb = new FastReportBuilder();
		List<Map> inputData = new ArrayList<>();
		try {
			List<ScrutinyDetail> scrutinyDetails = plan.getReportOutput().getScrutinyDetails();

			Set<String> common = new TreeSet<>();
			Map<String, ScrutinyDetail> allMap = new HashMap<>();
			Map<String, Set<String>> blocks = new TreeMap<>();
			for (ScrutinyDetail sd : scrutinyDetails) {
				String[] split = sd.getKey().split("_");
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
			Map paramMap = new HashMap<>();
			int i = 0;
			for (String cmnFeature : common) {
				i++;
				drb.addConcatenatedReport(getSub(allMap.get(cmnFeature), i, i + "." + cmnFeature,
						allMap.get(cmnFeature).getHeading(), allMap.get(cmnFeature).getSubHeading(), cmnFeature));
				paramMap.put(cmnFeature, allMap.get(cmnFeature).getDetail());
			}

			for (String blkName : blocks.keySet()) {
				List blkHeading = new ArrayList();
				blkHeading.add(BLOCK + blkName);
				drb.addConcatenatedReport(createHeaderSubreport("Block " + blkName, BLOCK + blkName));
				paramMap.put(BLOCK + blkName, blkHeading);
				int j = 0;
				for (String blkFeature : blocks.get(blkName)) {
					j++;
					drb.addConcatenatedReport(getSub(allMap.get(blkName + blkFeature), j, j + "." + blkFeature,
							allMap.get(blkName + blkFeature).getHeading(),
							allMap.get(blkName + blkFeature).getSubHeading(), blkName + blkFeature));
					paramMap.put(blkName + blkFeature, allMap.get(blkName + blkFeature).getDetail());

					List featureFooter = new ArrayList();
					if (allMap.get(blkName + blkFeature).getRemarks() != null) {
						drb.addConcatenatedReport(
								createFooterSubreport("Remarks :  " + allMap.get(blkName + blkFeature).getRemarks(),
										"Remarks_" + blkName + blkFeature));
						featureFooter.add(allMap.get(blkName + blkFeature).getRemarks());
						paramMap.put("Remarks_" + blkName + blkFeature, featureFooter);
					}
				}
			}

			drb.addColumn(RULE_NO, RULE_NO, String.class.getName(), 100).addColumn(DESCRIPTION, DESCRIPTION,
					String.class.getName(), 100);
			drb.setTitle("FAR");

			Map<String, String> details = new HashMap<>();
			details.put(RULE_NO, "31-1");
			details.put(DESCRIPTION, "far");

			Map<String, String> details21 = new HashMap<>();
			details21.put(RULE_NO, "4444");
			details21.put(DESCRIPTION, "fffff");

			inputData.add(details);
			inputData.add(details21);

			JasperPrint generateJasperPrint = DynamicJasperHelper.generateJasperPrint(drb.build(),
					new ClassicLayoutManager(), new JRBeanCollectionDataSource(inputData), paramMap);
			ByteArrayOutputStream outputBytes;
			outputBytes = new ByteArrayOutputStream();
			JasperExportManager.exportReportToPdfStream(generateJasperPrint, outputBytes);
			return new ByteArrayInputStream(outputBytes.toByteArray());
			// closeStream(reportStream);
		} catch (ColumnBuilderException | JRException | ClassNotFoundException e) {
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
			Double byeLawColumnSize = 100d;
			Double statusColumnSize = 60d;
			Double columnSize = (595d - (byeLawColumnSize + statusColumnSize)) / (size - 2);
			for (Integer s : detail.getColumnHeading().keySet()) {
				ColumnHeadingDetail columnHeading = detail.getColumnHeading().get(s);
				int columnWidth = columnSize.intValue();
				if ("Byelaw".equalsIgnoreCase(columnHeading.name)) {
					columnWidth = byeLawColumnSize.intValue();
				}
				if (STATUS.equalsIgnoreCase(columnHeading.name)) {
					columnWidth = statusColumnSize.intValue();
				}
				if(rulesSubrulesFlag) {
					frb.addColumn(columnHeading.name, columnHeading.name, String.class.getName(), columnWidth);
				} else {
					if (!"Byelaw".equalsIgnoreCase(columnHeading.name))
						frb.addColumn(columnHeading.name, columnHeading.name, String.class.getName(), columnWidth);
				}
				//frb.addColumn(columnHeading.name, columnHeading.name, String.class.getName(), columnWidth);
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
		} catch (ColumnBuilderException | ClassNotFoundException e) {
			LOG.error(e.getMessage(), e);
		}
		return null;

	}

	private Subreport getBlkDetails(DcrReportBlockDetail dcrReportBlockDetail, boolean isProposed) {
		try {

			FastReportBuilder frb = new FastReportBuilder();

			AbstractColumn floor = ColumnBuilder.getNew().setColumnProperty("floorNo", String.class.getName())
					.setTitle("Floor").setWidth(45).setHeaderStyle(reportService.getBldgDetlsHeaderStyle()).build();

			AbstractColumn occupancy = ColumnBuilder.getNew().setColumnProperty("occupancy", String.class.getName())
					.setTitle("Occupancy/Sub Occupancy").setWidth(125)
					.setHeaderStyle(reportService.getBldgDetlsHeaderStyle()).build();

			AbstractColumn builtUpArea = ColumnBuilder.getNew()
					.setColumnProperty("builtUpArea", BigDecimal.class.getName()).setTitle("Built Up Area in m²")
					.setWidth(120).setStyle(reportService.getNumberStyle()).build();
			frb.addGlobalFooterVariable(builtUpArea, DJCalculation.SUM, reportService.getTotalNumberStyle());

			AbstractColumn floorArea = ColumnBuilder.getNew().setColumnProperty("floorArea", BigDecimal.class.getName())
					.setTitle("Floor Area in m²").setWidth(120).setStyle(reportService.getNumberStyle()).build();
			frb.addGlobalFooterVariable(floorArea, DJCalculation.SUM, reportService.getTotalNumberStyle());

			AbstractColumn carpetArea = ColumnBuilder.getNew()
					.setColumnProperty("carpetArea", BigDecimal.class.getName()).setTitle("Carpet Area in m²")
					.setWidth(120).setStyle(reportService.getNumberStyle()).build();
			frb.addGlobalFooterVariable(carpetArea, DJCalculation.SUM, reportService.getTotalNumberStyle());

			frb.addColumn(floor);
			frb.addColumn(occupancy);
			frb.addColumn(builtUpArea);
			frb.addColumn(floorArea);
			frb.addColumn(carpetArea);

			if (dcrReportBlockDetail.getBlockNo() != null) {
				if (isProposed) {
					frb.setTitle("Block No " + dcrReportBlockDetail.getBlockNo() + " - Proposed Details");

					StringBuilder text = new StringBuilder();

					String coveredAreaText = "1. Covered area : " + (dcrReportBlockDetail.getCoverageArea() != null
							? dcrReportBlockDetail.getCoverageArea().setScale(DcrConstants.DECIMALDIGITS_MEASUREMENTS,
									DcrConstants.ROUNDMODE_MEASUREMENTS)
							: BigDecimal.ZERO) + " m²";

					String blgHgtText = "2. Height of building before Deduction/Addition : "
							+ (dcrReportBlockDetail.getDeclaredBuildingHeight() != null ? dcrReportBlockDetail
									.getDeclaredBuildingHeight().setScale(DcrConstants.DECIMALDIGITS_MEASUREMENTS,
											DcrConstants.ROUNDMODE_MEASUREMENTS)
									: BigDecimal.ZERO)
							+ " m";

					text = text.append(coveredAreaText).append("\\n").append(blgHgtText);
					// Building height before deduction
					String blgHgtAfterText = "3. Height of building after Deduction/Addition : "
							+ (dcrReportBlockDetail.getBuildingHeight() != null ? dcrReportBlockDetail
									.getBuildingHeight().setScale(DcrConstants.DECIMALDIGITS_MEASUREMENTS,
											DcrConstants.ROUNDMODE_MEASUREMENTS)
									: BigDecimal.ZERO)
							+ " m";

					text = text.append("\\n").append(blgHgtAfterText);

					if (dcrReportBlockDetail.getConstructedArea().compareTo(BigDecimal.ZERO) > 0) {
						String constructedAreaText = "4. Already constructed area is "
								+ (dcrReportBlockDetail.getConstructedArea() != null ? dcrReportBlockDetail
										.getConstructedArea().setScale(DcrConstants.DECIMALDIGITS_MEASUREMENTS,
												DcrConstants.ROUNDMODE_MEASUREMENTS)
										: BigDecimal.ZERO)
								+ " m²";
						text = text.append("\\n").append(constructedAreaText);
					}

					AutoText autoText = new AutoText(text.toString(), AutoText.POSITION_FOOTER,
							HorizontalBandAlignment.LEFT, 530);

					autoText.setHeight(40);
					autoText.setStyle(reportService.getTotalNumberStyle());

					frb.addAutoText(autoText);
				} else
					frb.setTitle("Block No " + dcrReportBlockDetail.getBlockNo() + " - Existing Details");
			}

			frb.setTitleStyle(reportService.getTitleStyle());
			frb.setHeaderHeight(5);
			frb.setTopMargin(10);
			frb.setBottomMargin(0);
			frb.setDefaultStyles(reportService.getTitleStyle(), reportService.getSubTitleStyle(),
					reportService.getColumnHeaderStyle(), reportService.getDetailStyle());
			frb.setAllowDetailSplit(false);
			frb.setPageSizeAndOrientation(Page.Page_A4_Portrait());
			frb.setGrandTotalLegend(TOTAL);
			frb.setGrandTotalLegendStyle(reportService.getNumberStyle());
			DynamicReport build = frb.build();
			Subreport sub = new Subreport();
			sub.setDynamicReport(build);
			Style style = new Style();
			style.setStretchWithOverflow(true);
			style.setStreching(RELATIVE_TO_BAND_HEIGHT);
			sub.setStyle(style);
			if (isProposed) {
				sub.setDatasource(new DJDataSource("Block No " + dcrReportBlockDetail.getBlockNo(),
						DJConstants.DATA_SOURCE_ORIGIN_PARAMETER, 0));
			} else
				sub.setDatasource(new DJDataSource("Existing Block No " + dcrReportBlockDetail.getBlockNo(),
						DJConstants.DATA_SOURCE_ORIGIN_PARAMETER, 0));
			sub.setLayoutManager(new ClassicLayoutManager());
			return sub;
		} catch (ColumnBuilderException e) {
			LOG.error(e.getMessage(), e);
		}
		return null;
	}

	private Subreport getAreaDetails(boolean isProposed) {
		try {

			FastReportBuilder frb = new FastReportBuilder();

			AbstractColumn builtUpArea = ColumnBuilder.getNew()
					.setColumnProperty(isProposed ? "proposedBuitUpArea" : "totalExistingBuiltUpArea",
							BigDecimal.class.getName())
					.setTitle("Built Up Area in m²").setWidth(120).setStyle(reportService.getTotalNumberStyle())
					.build();

			AbstractColumn floorArea = ColumnBuilder.getNew()
					.setColumnProperty(isProposed ? "proposedFloorArea" : "totalExistingFloorArea",
							BigDecimal.class.getName())
					.setTitle("Floor Area in m²").setWidth(120).setStyle(reportService.getTotalNumberStyle()).build();

			AbstractColumn carpetArea = ColumnBuilder.getNew()
					.setColumnProperty(isProposed ? "proposedCarpetArea" : "totalExistingCarpetArea",
							BigDecimal.class.getName())
					.setTitle("Carpet Area in m²").setWidth(120).setStyle(reportService.getTotalNumberStyle()).build();

			frb.addColumn(builtUpArea);
			frb.addColumn(floorArea);
			frb.addColumn(carpetArea);

			if (isProposed)
				frb.setTitle("Total Proposed Areas");
			else
				frb.setTitle("Total Existing Areas");

			frb.setTitleStyle(reportService.getTitleStyle());
			frb.setHeaderHeight(5);
			frb.setTopMargin(5);
			frb.setDefaultStyles(reportService.getTitleStyle(), reportService.getSubTitleStyle(),
					reportService.getColumnHeaderStyle(), reportService.getDetailStyle());
			frb.setAllowDetailSplit(false);
			frb.setPageSizeAndOrientation(Page.Page_A4_Portrait());
			frb.setGrandTotalLegend(TOTAL);
			frb.setGrandTotalLegendStyle(reportService.getNumberStyle());
			DynamicReport build = frb.build();
			Subreport sub = new Subreport();
			sub.setDynamicReport(build);
			Style style = new Style();
			style.setStretchWithOverflow(true);
			style.setStreching(RELATIVE_TO_BAND_HEIGHT);
			sub.setStyle(style);
			if (isProposed) {
				sub.setDatasource(
						new DJDataSource("Total Proposed Details", DJConstants.DATA_SOURCE_ORIGIN_PARAMETER, 0));
			} else
				sub.setDatasource(
						new DJDataSource("Total Existing Details", DJConstants.DATA_SOURCE_ORIGIN_PARAMETER, 0));
			sub.setLayoutManager(new ClassicLayoutManager());
			return sub;
		} catch (ColumnBuilderException e) {
			LOG.error(e.getMessage(), e);
		}
		return null;
	}

	private Subreport getTotalAreaDetails(VirtualBuildingReport virtualBuildingReport) {
		try {

			FastReportBuilder frb = new FastReportBuilder();

			if (virtualBuildingReport.getTotalConstructedArea() != null
					&& virtualBuildingReport.getTotalConstructedArea().compareTo(BigDecimal.ZERO) > 0) {
				AbstractColumn builtUpArea = ColumnBuilder.getNew()
						.setColumnProperty("totalBuitUpArea", BigDecimal.class.getName())
						.setTitle("Built Up Area in m²").setWidth(100).setStyle(reportService.getTotalNumberStyle())
						.build();

				AbstractColumn floorArea = ColumnBuilder.getNew()
						.setColumnProperty("totalFloorArea", BigDecimal.class.getName()).setTitle("Floor Area in m²")
						.setWidth(100).setStyle(reportService.getTotalNumberStyle()).build();

				AbstractColumn carpetArea = ColumnBuilder.getNew()
						.setColumnProperty("totalCarpetArea", BigDecimal.class.getName()).setTitle("Carpet Area in m²")
						.setWidth(100).setStyle(reportService.getTotalNumberStyle()).build();

				AbstractColumn coverageArea = ColumnBuilder.getNew()
						.setColumnProperty("totalCoverageArea", BigDecimal.class.getName())
						.setTitle("Covered Area in m²").setWidth(100).setStyle(reportService.getTotalNumberStyle())
						.build();

				AbstractColumn constructedArea = ColumnBuilder.getNew()
						.setColumnProperty("totalConstructedArea", BigDecimal.class.getName())
						.setTitle("Already Constructed Area in m²").setWidth(100)
						.setStyle(reportService.getTotalNumberStyle()).build();

				frb.addColumn(builtUpArea);
				frb.addColumn(floorArea);
				frb.addColumn(carpetArea);
				frb.addColumn(coverageArea);
				frb.addColumn(constructedArea);
			} else {
				AbstractColumn builtUpArea = ColumnBuilder.getNew()
						.setColumnProperty("totalBuitUpArea", BigDecimal.class.getName())
						.setTitle("Built Up Area in m²").setWidth(120).setStyle(reportService.getTotalNumberStyle())
						.build();

				AbstractColumn floorArea = ColumnBuilder.getNew()
						.setColumnProperty("totalFloorArea", BigDecimal.class.getName()).setTitle("Floor Area in m²")
						.setWidth(120).setStyle(reportService.getTotalNumberStyle()).build();

				AbstractColumn carpetArea = ColumnBuilder.getNew()
						.setColumnProperty("totalCarpetArea", BigDecimal.class.getName()).setTitle("Carpet Area in m²")
						.setWidth(120).setStyle(reportService.getTotalNumberStyle()).build();

				AbstractColumn coverageArea = ColumnBuilder.getNew()
						.setColumnProperty("totalCoverageArea", BigDecimal.class.getName())
						.setTitle("Covered Area in m²").setWidth(120).setStyle(reportService.getTotalNumberStyle())
						.build();

				frb.addColumn(builtUpArea);
				frb.addColumn(floorArea);
				frb.addColumn(carpetArea);
				frb.addColumn(coverageArea);
			}

			frb.setTitle("Total Area");
			frb.setTitleStyle(reportService.getTitleStyle());
			frb.setHeaderHeight(5);
			frb.setTopMargin(5);
			frb.setDefaultStyles(reportService.getTitleStyle(), reportService.getSubTitleStyle(),
					reportService.getColumnHeaderStyle(), reportService.getDetailStyle());
			frb.setAllowDetailSplit(false);
			frb.setPageSizeAndOrientation(Page.Page_A4_Portrait());
			frb.setGrandTotalLegend(TOTAL);
			frb.setGrandTotalLegendStyle(reportService.getNumberStyle());
			DynamicReport build = frb.build();
			Subreport sub = new Subreport();
			sub.setDynamicReport(build);
			Style style = new Style();
			style.setStretchWithOverflow(true);
			style.setStreching(RELATIVE_TO_BAND_HEIGHT);
			sub.setStyle(style);

			sub.setDatasource(new DJDataSource("Total Area Details", DJConstants.DATA_SOURCE_ORIGIN_PARAMETER, 0));

			sub.setLayoutManager(new ClassicLayoutManager());
			return sub;
		} catch (ColumnBuilderException e) {
			LOG.error(e.getMessage(), e);
		}
		return null;
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

	protected void closeStream(final InputStream stream) {
		if (stream != null)
			try {
				stream.close();
			} catch (final IOException e) {
				if (LOG.isDebugEnabled())
					LOG.debug("Error" + e.getMessage());

			}
	}

	public InputStream generateReport(Plan plan, EdcrApplication dcrApplication) {
		
		//dcrApplic

		FastReportBuilder drb = new FastReportBuilder();
		StringBuilder reportBuilder = new StringBuilder();

		final Style titleStyle = new Style("titleStyle");
		titleStyle.setFont(new Font(50, Font._FONT_TIMES_NEW_ROMAN, true));
		titleStyle.setHorizontalAlign(HorizontalAlign.CENTER);

		titleStyle.setFont(new Font(2, Font._FONT_TIMES_NEW_ROMAN, false));
		String applicationNumber = StringUtils.isNotBlank(dcrApplication.getApplicationNumber())
				? dcrApplication.getApplicationNumber()
				: "NA";
		String applicationDate = DateUtils.toDefaultDateFormat(dcrApplication.getApplicationDate());

		if (plan.getVirtualBuilding() != null && !plan.getVirtualBuilding().getOccupancyTypes().isEmpty()) {
			List<String> occupancies = new ArrayList<>();
			plan.getVirtualBuilding().getOccupancyTypes().forEach(occ -> {
				if (occ.getType() != null)
					occupancies.add(occ.getType().getName());
			});
			Set<String> distinctOccupancies = new HashSet<>(occupancies);
//			plan.getPlanInformation()
//					.setOccupancy(distinctOccupancies.stream().map(String::new).collect(Collectors.joining(",")));
			if (plan.getVirtualBuilding().getMostRestrictiveFarHelper() != null
					&& plan.getVirtualBuilding().getMostRestrictiveFarHelper().getType() != null) {

				if (plan.getPlanInformation().getIsProjectWorkCumResidential()
						|| plan.getPlanInformation().getIsProjectShopCumResidential()) {

					plan.getPlanInformation().setOccupancy(
							plan.getPlanInformation().getIsProjectWorkCumResidential() ? "Residential" : "Commercial");

				} else {
					plan.getPlanInformation()
							.setOccupancy(plan.getVirtualBuilding().getMostRestrictiveFarHelper().getType().getName());
				}

			}
				
		}
		boolean reportStatus = false;
		boolean finalReportStatus = true;
		StringBuilder errors = new StringBuilder();
		StringBuilder nocs = new StringBuilder();
		if (plan.getNoObjectionCertificates() != null && plan.getNoObjectionCertificates().size() > 0) {
			int i = 1;
			for (Map.Entry<String, String> entry : plan.getNoObjectionCertificates().entrySet()) {
				nocs.append(String.valueOf(i)).append(". ");
				nocs.append(entry.getValue());
				nocs.append("\n");
				i++;
			}
		}

//		if (plan.getErrors() != null && plan.getErrors().size() > 0) {
//			int i = 1;
//			for (Map.Entry<String, String> entry : plan.getErrors().entrySet()) {
//				errors.append(String.valueOf(i)).append(". ");
//				errors.append(entry.getValue());
//				errors.append("\n");
//				i++;
//				finalReportStatus = false;
//			}
//		}

		drb.setPageSizeAndOrientation(new Page(842, 595, true));
		final JRDataSource ds = new JRBeanCollectionDataSource(Collections.singletonList(plan));
		String voltages = "";
		StringBuilder voltageString = new StringBuilder();

		if (!plan.getElectricLine().isEmpty()) {
			for (ElectricLine electricLine : plan.getElectricLine()) {
				voltageString.append(String.valueOf(electricLine.getVoltage() == null ? 0 : electricLine.getVoltage()))
						.append(",");
			}
			if (voltageString.length() > 1) {
				voltages = voltageString.deleteCharAt(voltageString.length() - 1).toString() + " KV";
			}
		}
		if (StringUtils.isBlank(voltages)) {
			voltages = String.valueOf(BigDecimal.ZERO) + " KV";
		}

		final Map<String, Object> valuesMap = new HashMap<>();
//		String ulbName = ApplicationThreadLocals.getMunicipalityName();
		String ulbName = null;
		OdishaUlbs odishaUlbs= null;
		if (ulbName == null || ulbName.trim().isEmpty())
		//ulbName = UlbNameConstants.ulbName(plan.getThirdPartyUserTenantld());
		//LOG.info(OdishaUlbs.getUlb(plan.getThirdPartyUserTenantld()));
		 odishaUlbs = OdishaUlbs.getUlb(plan.getThirdPartyUserTenantld());
		 //LOG.info("odisha ulbs:"+odishaUlbs.getUlbName());
		String subTitle = getSubTitle(dcrApplication.getApplicationType(), dcrApplication.getAlterationSubService(), dcrApplication);
		valuesMap.put("subTitle",subTitle);
		valuesMap.put("ulbName",odishaUlbs.getUlbName());
		valuesMap.put("applicantName", dcrApplication.getApplicantName());
		valuesMap.put("licensee", plan.getArchitectInformation());
		valuesMap.put("applicationNumber", applicationNumber);
		valuesMap.put("applicationDate", applicationDate);
//		valuesMap.put("errors", plan.getErrors());
//		valuesMap.put("errorString", errors.toString());
		valuesMap.put("nocString", nocs.toString());
		valuesMap.put("nocs", plan.getNoObjectionCertificates());
		valuesMap.put("reportGeneratedDate", DateUtils.toDefaultDateTimeFormat(new Date()));
		valuesMap.put("currentYear", new LocalDate().getYear());
		valuesMap.put("far", plan.getFarDetails() != null ? plan.getFarDetails().getProvidedFar() : "");
		valuesMap.put("coverage", plan.getCoverage());
		valuesMap.put("totalFloorArea",
				plan.getVirtualBuilding() != null ? plan.getVirtualBuilding().getTotalFloorArea()
						: BigDecimal.valueOf(0));
		valuesMap.put("totalBuiltUpArea",
				plan.getVirtualBuilding() != null ? plan.getVirtualBuilding().getTotalBuitUpArea()
						: BigDecimal.valueOf(0));
		valuesMap.put("electricLineVoltage", voltages);
		valuesMap.put("blockCount",
				plan.getBlocks() != null && !plan.getBlocks().isEmpty() ? plan.getBlocks().size() : 0);
		valuesMap.put("surrenderRoadArea", plan.getTotalSurrenderRoadArea());
		String imageURL = ReportUtil.getImageURL("/egi/resources/global/images/digit-logo-black.png");
		// valuesMap.put("egovLogo", imageURL);
		// valuesMap.put("cityLogo", cityService.getCityLogoURLByCurrentTenant());
		// valuesMap.put("cityLogo",
		// "https://digitaldesksujog051120.blob.core.windows.net/assets/Logos/Cuttack.jpg");
		// valuesMap.put("cityLogo",
		// cityService.getCityLogoURLNew(dcrApplication.getThirdPartyUserTenant()));

		if (clientSpecificSubReport) {

			List<DcrReportBlockDetail> blockDetails = new ArrayList<>();

			List<DcrReportBlockDetail> existingBlockDetails = buildBlockWiseExistingInfo(plan);
			VirtualBuildingReport virtualBuildingReport = buildVirtualBuilding(plan.getVirtualBuilding());
			List<OccupancyReport> occupanciesReport = buildSubOccupanciesReport(plan.getPlanInformation().getOccupancyPercentages());

			List<String> combinedSummary = new ArrayList<>();
			combinedSummary.add(COMBINED_BLOCKS_SUMMARY_DETAILS);
			drb.addConcatenatedReport(
					createHeaderSubreport(COMBINED_BLOCKS_SUMMARY_DETAILS, COMBINED_BLOCKS_SUMMARY_DETAILS));
			valuesMap.put(COMBINED_BLOCKS_SUMMARY_DETAILS, combinedSummary);
			
			//TODO: 11 City level customization for Puri code starts here.
			// Adds table in report		
			if (plan.getThirdPartyUserTenantld() != null && (plan.getThirdPartyUserTenantld().equals("od.puri")
					|| plan.getThirdPartyUserTenantld().equals("od.purikonarkdevelopmentauthority")
					|| plan.getThirdPartyUserTenantld().equals("od.konark")
					|| plan.getThirdPartyUserTenantld().equals("od.nimapara"))) {
				drb.addConcatenatedReport(getDeclarations());
				valuesMap.put("Declarations", getDcrReportDeclarationsDetails(plan));
				
			}
			// City level customization for Puri code ends here.

			// Add total area details
			drb.addConcatenatedReport(getTotalAreaDetails(virtualBuildingReport));
			valuesMap.put("Total Area Details", Arrays.asList(virtualBuildingReport));
			
			// Add shop/work cum residendial details
			if(plan.getPlanInformation().getIsProjectShopCumResidential()
					|| plan.getPlanInformation().getIsProjectWorkCumResidential()) {
				ShopOrWorkCumResidencialDetails shopOrWorkCumResidencialDetails=new ShopOrWorkCumResidencialDetails();
				
				shopOrWorkCumResidencialDetails.setOccupancy(plan.getPlanInformation().getOccupancy());
				shopOrWorkCumResidencialDetails.setOccupancySubType(
						plan.getPlanInformation().getOccupancy().equalsIgnoreCase("Commercial") ? "Shop Cum Residential"
								: "Work Cum Residential");
			
				List<ShopOrWorkCumResidencialDetails> shopWork = Arrays.asList(new ShopOrWorkCumResidencialDetails[] {shopOrWorkCumResidencialDetails});
				
				drb.addConcatenatedReport(getShopOrWorkCumResidialDetails());
				valuesMap.put("ShopOrWorkDetails", shopWork);
			
			}
			

			// Add Occupancy percentage details
			drb.addConcatenatedReport(getOccupanciesPercentageDetails());
			valuesMap.put("Occupancy Percentage Details", occupanciesReport);
			
			
			//gisLandUseZones
			GISLandUseZone gisLandUseZone=new GISLandUseZone();
			if(rulesSubrulesFlag) {
				gisLandUseZone.setByeLaw1("ODA Rule 27(1), 45");
				gisLandUseZone.setByeLaw2("BDA Rule 25(5), 57(3)");
			}			
			gisLandUseZone.setLandUseZoneAsPerDeclaration(plan.getPlanInformation().getLandUseZone());
			gisLandUseZone.setLandUseZoneAsPerGIS(plan.getPlanInformation().getGisLandUseZone());
			gisLandUseZone.setDpbpCommitteeReviewDecription("To Be reviewd by DPBP-committee");
			gisLandUseZone.setDpbpCommitteeReviewOc(plan.getPlanInformation().getDpbpCommitteeReviewOc());

			List<GISLandUseZone> gisLandUseZones = Arrays.asList(new GISLandUseZone[] {gisLandUseZone});
			List<GISLandUseZone> gisLandUseZonesRestriction = null;
			try {
				gisLandUseZonesRestriction = Arrays.asList(new GISLandUseZone[] {gisLandUseZone.clone()});
			}catch (Exception e) {
				e.printStackTrace();
			}
			drb.addConcatenatedReport(getLandUseZoneReport());
			valuesMap.put("LandUseZone", gisLandUseZones);

			drb.addConcatenatedReport(getLandUseZoneReportRestriction());
			valuesMap.put("Land Use Zone - Restriction", gisLandUseZonesRestriction);
			
			List<String> blockSummary = new ArrayList<>();
			blockSummary.add(BLOCK_WISE_SUMMARY);
			drb.addConcatenatedReport(createHeaderSubreport(BLOCK_WISE_SUMMARY, BLOCK_WISE_SUMMARY));
			valuesMap.put(BLOCK_WISE_SUMMARY, blockSummary);

			// Add existing block details
			if (existingBlockDetails != null && !existingBlockDetails.isEmpty()) {
				for (DcrReportBlockDetail existingBlockDetail : existingBlockDetails) {
					blockDetails.add(existingBlockDetail);
					drb.addConcatenatedReport(getBlkDetails(existingBlockDetail, false));
					valuesMap.put("Existing Block No " + existingBlockDetail.getBlockNo(),
							existingBlockDetail.getDcrReportFloorDetails());
				}
				drb.addConcatenatedReport(getAreaDetails(false));
				valuesMap.put("Total Existing Details", Arrays.asList(virtualBuildingReport));
			}

			List<DcrReportBlockDetail> proposedBlockDetails = buildBlockWiseProposedInfo(plan);

			// Add proposed block details
			for (DcrReportBlockDetail dcrReportBlockDetail : proposedBlockDetails) {
				blockDetails.add(dcrReportBlockDetail);
				drb.addConcatenatedReport(getBlkDetails(dcrReportBlockDetail, true));
				valuesMap.put("Block No " + dcrReportBlockDetail.getBlockNo(),
						dcrReportBlockDetail.getDcrReportFloorDetails());
			}

			if (existingBlockDetails != null && !existingBlockDetails.isEmpty()) {
				drb.addConcatenatedReport(getAreaDetails(true));
				valuesMap.put("Total Proposed Details", Arrays.asList(virtualBuildingReport));
			}

			DcrReportPlanDetail dcrReportPlanDetail = new DcrReportPlanDetail();
			dcrReportPlanDetail.setVirtualBuildingReport(virtualBuildingReport);
			dcrReportPlanDetail.setDcrReportBlockDetailList(blockDetails);

			List<ScrutinyDetail> scrutinyDetails = plan.getReportOutput().getScrutinyDetails();

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
				drb.addConcatenatedReport(
						createHeaderSubreport("Block " + blkName + " - Scrutiny Details", BLOCK + blkName));
				valuesMap.put(BLOCK + blkName, blkHeading);
				int j = 0;
				// This is only for set back
				ScrutinyDetail front = null;
				ScrutinyDetail rear = null;
				ScrutinyDetail side = null;

				for (String blkFeature : blocks.get(blkName)) {
					if (blkFeature.equals(FRONT_YARD_DESC) || blkFeature.equals(REAR_YARD_DESC)
							|| blkFeature.equals(SIDE_YARD_DESC)) {

						if (blkFeature.equals(FRONT_YARD_DESC)) {
							front = allMap.get(blkName + blkFeature);
							front.getDetail().get(0).put(SIDENUMBER_NAME, "Front");
							continue;
						}
						if (blkFeature.equals(REAR_YARD_DESC)) {
							rear = allMap.get(blkName + blkFeature);
							rear.getDetail().get(0).put(SIDENUMBER_NAME, "Rear");
							continue;
						}

						side = allMap.get(blkName + blkFeature);
						// List<Map<String, String>> detail = allMap.get(blkName +
						// blkFeature).getDetail();
						List<Map<String, String>> detail = side.getDetail();

						if (front != null)
							detail.add(0, front.getDetail().get(0));
						if (rear != null)
							detail.add(1, rear.getDetail().get(0));

						for (Map<String, String> d : detail) {
							String sideNumber = d.get(SIDENUMBER);
							if (StringUtils.isNotBlank(sideNumber)) {
								d.remove(SIDENUMBER);
								d.put(SIDENUMBER_NAME, sideNumber);
							}
						}
						side.addColumnHeading(2, SIDENUMBER_NAME);
						side.addColumnHeading(4, LEVEL);
						// allMap.get(blkName + blkFeature).setHeading(SIDENUMBER_NMAE);

						j++;
						drb.addConcatenatedReport(
								getSub(allMap.get(blkName + blkFeature), j, j + "." + blkFeature, SIDENUMBER_NAME,
										allMap.get(blkName + blkFeature).getSubHeading(), blkName + blkFeature));
						valuesMap.put(blkName + blkFeature, allMap.get(blkName + blkFeature).getDetail());

						List featureFooter = new ArrayList();
						if (allMap.get(blkName + blkFeature).getRemarks() != null) {
							drb.addConcatenatedReport(
									createFooterSubreport("Remarks :  " + allMap.get(blkName + blkFeature).getRemarks(),
											"Remarks_" + blkName + blkFeature));
							featureFooter.add(allMap.get(blkName + blkFeature).getRemarks());
							valuesMap.put("Remarks_" + blkName + blkFeature, featureFooter);
						}
					} else {
						continue;
					}

				}
				// This is only for rest
				for (String blkFeature : blocks.get(blkName)) {
					if (blkFeature.equals(FRONT_YARD_DESC) || blkFeature.equals(REAR_YARD_DESC)
							|| blkFeature.equals(SIDE_YARD_DESC)) {
						continue;
					} else {
						j++;
						drb.addConcatenatedReport(getSub(allMap.get(blkName + blkFeature), j, j + "." + blkFeature,
								allMap.get(blkName + blkFeature).getHeading(),
								allMap.get(blkName + blkFeature).getSubHeading(), blkName + blkFeature));
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

			}
			
			// Errors in Scrutiny report
			if(plan != null && !plan.getErrors().isEmpty()) {
				List<String> errorSummary = new ArrayList<>();
				errorSummary.add(ERROR_SUMMARY);
				drb.addConcatenatedReport(
						createHeaderSubreport(ERROR_SUMMARY, ERROR_SUMMARY));
				valuesMap.put(ERROR_SUMMARY, errorSummary); 
				
				drb.addConcatenatedReport(getErrors());
				valuesMap.put("Errors", getDcrReportErrorDetails(plan.getErrors()));
				finalReportStatus = false;
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
							if (blkStatus.get(STATUS)!=null && blkStatus.get(STATUS).equalsIgnoreCase(Result.Not_Accepted.getResultVal())) {
								finalReportStatus = false;
							}
						}
					}
				}

		} else {
			/*
			 * List<PlanRule> planRules = planRuleService.findRulesByPlanDetail(planDetail);
			 * for (PlanRule pl : planRules) { String rules = pl.getRules(); String[]
			 * ruleSet = rules.split(","); for (String s : ruleSet) { String ruleName =
			 * "rule" + s; if(LOG.isDebugEnabled()) LOG.debug(s); Object ruleBean =
			 * getRuleBean(ruleName); if (ruleBean != null) { GeneralRule bean =
			 * (GeneralRule) ruleBean; if (bean != null){ reportStatus =
			 * bean.generateRuleReport(planDetail, drb, valuesMap, reportStatus);
			 * if(!reportStatus) { finalReportStatus=false; } } } else
			 * LOG.error("Skipping rule " + ruleName + "Since rule cannot be injected"); } }
			 */
		}

		if (ApplicationType.OCCUPANCY_CERTIFICATE.equals(dcrApplication.getApplicationType())
				&& StringUtils.isNotBlank(dcrApplication.getEdcrApplicationDetails().get(0).getComparisonDcrNumber())) {
			plan.setMainDcrPassed(finalReportStatus);
			finalReportStatus = finalReportStatus && (dcrApplication.getDeviationStatus().equalsIgnoreCase("Accepted"));
		}else {
			plan.setMainDcrPassed(finalReportStatus);
			if(dcrApplication.getDeviationStatus() != null)
				finalReportStatus = finalReportStatus && (dcrApplication.getDeviationStatus().equalsIgnoreCase("Accepted"));
		}

		reportBuilder.append("Report Status : " + (finalReportStatus ? "Accepted" : "Not Accepted")).append("\\n")
				.append("\\n");
		reportBuilder.append("Rules Verified : ").append("\\n");
		valuesMap.put("reportStatus", (finalReportStatus ? "Accepted" : "Not Accepted"));
		if(dxfToPdfFlag) {
			drb.setTemplateFile("/reports/templates/edcr_report.jrxml");
		} else {
			drb.setTemplateFile("/reports/templates/edcr_report_old.jrxml");
		}
		
		drb.setMargins(5, 0, 33, 20);
		if (ApplicationType.OCCUPANCY_CERTIFICATE.equals(dcrApplication.getApplicationType())) {
			valuesMap.put("planPermissionNumber", dcrApplication.getPlanPermitNumber());
			valuesMap.put("bpaApplicationDate",
					DateUtils.toDefaultDateFormat(dcrApplication.getPermitApplicationDate()));
		}
		if (finalReportStatus) {
			String dcrApplicationNumber = "";
			if (ApplicationType.OCCUPANCY_CERTIFICATE.equals(dcrApplication.getApplicationType()))
				dcrApplicationNumber = ocPlanScrutinyNumberGenerator.generateEdcrApplicationNumber();
			else
				dcrApplicationNumber = dcrApplicationNumberGenerator.generateEdcrApplicationNumber(dcrApplication);
			EdcrApplicationDetail edcrApplicationDetail = dcrApplication.getEdcrApplicationDetails().get(0);
			edcrApplicationDetail.setDcrNumber(dcrApplicationNumber);
			valuesMap.put("dcrNo", dcrApplicationNumber);
		}
		if (finalReportStatus) {
			valuesMap.put("qrCode", generatePDF417Code(buildQRCodeDetails(dcrApplication, finalReportStatus)));
		}
		valuesMap.put("applicationType", dcrApplication.getApplicationType().getApplicationTypeVal());
		//Map<String, String> serviceTypeList = DxfFileConstants.getServiceTypeList();
		Map<String, String> serviceTypeList = new ConcurrentHashMap<>();
		serviceTypeList.put("NEW_CONSTRUCTION", "New Construction");
		serviceTypeList.put("ALTERATION", "Addition and Alteration");

		if (StringUtils.isNotBlank(dcrApplication.getServiceType())) {
			String serviceType = serviceTypeList.get(dcrApplication.getServiceType());
			valuesMap.put("serviceType", serviceType);
		}

		if (StringUtils.isNotBlank(dcrApplication.getServiceType())) {
			String serviceType = serviceTypeList.get(dcrApplication.getServiceType());
			valuesMap.put("serviceType", serviceType);
		}
		final DynamicReport dr = drb.build();
		plan.setEdcrPassed(finalReportStatus);
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

	private Subreport getOccupanciesPercentageDetails() {

		try {

			FastReportBuilder frb = new FastReportBuilder();
			
			if(rulesSubrulesFlag) {
				AbstractColumn byeLaw = ColumnBuilder.getNew()
						.setColumnProperty("byeLaw", String.class.getName())
						.setTitle("Byelaw").setWidth(70).setStyle(reportService.getTotalNumberStyle())
						.build();
				frb.addColumn(byeLaw);
			}
						
			AbstractColumn occupancy = ColumnBuilder.getNew()
					.setColumnProperty("occupancy", String.class.getName())
					.setTitle("Occupancy").setWidth(rulesSubrulesFlag?60:80).setStyle(reportService.getTotalNumberStyle())
					.build();
			
			AbstractColumn subOccupancy = ColumnBuilder.getNew()
					.setColumnProperty("subOccupancy", String.class.getName())
					.setTitle("Sub-Occupancy").setWidth(rulesSubrulesFlag?80:100).setStyle(reportService.getTotalNumberStyle())
					.build();

			AbstractColumn buaPercentage = ColumnBuilder.getNew()
					.setColumnProperty("percentage", BigDecimal.class.getName()).setTitle("Build Up Area in %")
					.setWidth(rulesSubrulesFlag?75:90).setStyle(reportService.getTotalNumberStyle()).build();
			
			AbstractColumn buaToatlArea = ColumnBuilder.getNew()
					.setColumnProperty("buildUpArea", BigDecimal.class.getName()).setTitle("Built Up Area in m²")
					.setWidth(rulesSubrulesFlag?75:90).setStyle(reportService.getTotalNumberStyle()).build();
			
			AbstractColumn floorToatlArea= ColumnBuilder.getNew()
					.setColumnProperty("floorArea", BigDecimal.class.getName()).setTitle("Floor Area in m²")
					.setWidth(rulesSubrulesFlag?75:90).setStyle(reportService.getTotalNumberStyle()).build();
			
			AbstractColumn carpetToatlArea = ColumnBuilder.getNew()
					.setColumnProperty("carpetArea", BigDecimal.class.getName()).setTitle("Carpet Area in m²")
					.setWidth(rulesSubrulesFlag?75:90).setStyle(reportService.getTotalNumberStyle()).build();
			
			
			frb.addColumn(occupancy);
			frb.addColumn(subOccupancy);
			frb.addColumn(buaPercentage);
			frb.addColumn(buaToatlArea);
			frb.addColumn(floorToatlArea);
			frb.addColumn(carpetToatlArea);

			frb.setTitle("Sub-Occupancy occupied Percentage");
			frb.setTitleStyle(reportService.getTitleStyle());
			frb.setHeaderHeight(5);
			frb.setTopMargin(5);
			frb.setDefaultStyles(reportService.getTitleStyle(), reportService.getSubTitleStyle(),
					reportService.getColumnHeaderStyle(), reportService.getDetailStyle());
			frb.setAllowDetailSplit(false);
			frb.setPageSizeAndOrientation(Page.Page_A4_Portrait());
			frb.setGrandTotalLegend(TOTAL);
			frb.setGrandTotalLegendStyle(reportService.getNumberStyle());
			DynamicReport build = frb.build();
			Subreport sub = new Subreport();
			sub.setDynamicReport(build);
			Style style = new Style();
			style.setStretchWithOverflow(true);
			style.setStreching(RELATIVE_TO_BAND_HEIGHT);
			sub.setStyle(style);

			sub.setDatasource(new DJDataSource("Occupancy Percentage Details", DJConstants.DATA_SOURCE_ORIGIN_PARAMETER, 0));

			sub.setLayoutManager(new ClassicLayoutManager());
			return sub;
		} catch (ColumnBuilderException e) {
			LOG.error(e.getMessage(), e);
		}
		return null;
	
	}
	
	private Subreport getShopOrWorkCumResidialDetails() {
		try {

			FastReportBuilder frb = new FastReportBuilder();
			AbstractColumn occupancy = ColumnBuilder.getNew()
					.setColumnProperty("occupancy", String.class.getName()).setTitle("Occupancy")
					.setWidth(180).setStyle(reportService.getTotalNumberStyle()).build();

			AbstractColumn occupancySubType = ColumnBuilder.getNew()
					.setColumnProperty("occupancySubType", String.class.getName())
					.setTitle("Occupancy Sub Type").setWidth(180).setStyle(reportService.getTotalNumberStyle())
					.build();

			frb.addColumn(occupancy);
			frb.addColumn(occupancySubType);

			frb.setTitle("Occupancy Sub Type Details");
			frb.setTitleStyle(reportService.getTitleStyle());
			frb.setHeaderHeight(5);
			frb.setTopMargin(5);
			frb.setDefaultStyles(reportService.getTitleStyle(), reportService.getSubTitleStyle(),
					reportService.getColumnHeaderStyle(), reportService.getDetailStyle());
			frb.setAllowDetailSplit(false);
			frb.setPageSizeAndOrientation(Page.Page_A4_Portrait());
			frb.setGrandTotalLegend(TOTAL);
			frb.setGrandTotalLegendStyle(reportService.getNumberStyle());
			DynamicReport build = frb.build();
			Subreport sub = new Subreport();
			sub.setDynamicReport(build);
			Style style = new Style();
			style.setStretchWithOverflow(true);
			style.setStreching(RELATIVE_TO_BAND_HEIGHT);
			sub.setStyle(style);

			sub.setDatasource(new DJDataSource("ShopOrWorkDetails", DJConstants.DATA_SOURCE_ORIGIN_PARAMETER, 0));

			sub.setLayoutManager(new ClassicLayoutManager());
			return sub;
		} catch (ColumnBuilderException e) {
			LOG.error(e.getMessage(), e);
		}
		return null;
	}
	
	private Subreport getLandUseZoneReport() {
		try {

			FastReportBuilder frb = new FastReportBuilder();
			
			if(rulesSubrulesFlag) {
				AbstractColumn byeLaw = ColumnBuilder.getNew()
						.setColumnProperty("byeLaw1", String.class.getName())
						.setTitle("Byelaw").setWidth(180).setStyle(reportService.getTotalNumberStyle()).build();
				
				frb.addColumn(byeLaw);
			}
						
			AbstractColumn asPerGis = ColumnBuilder.getNew()
					.setColumnProperty("landUseZoneAsPerGIS", String.class.getName())
					.setTitle("As per GIS Data").setWidth(180).setStyle(reportService.getTotalNumberStyle()).build();

			AbstractColumn asPerDeclaration = ColumnBuilder.getNew()
					.setColumnProperty("landUseZoneAsPerDeclaration", String.class.getName())
					.setTitle("As per drawing declaration").setWidth(180).setStyle(reportService.getTotalNumberStyle()).build();
			
			frb.addColumn(asPerGis);
			frb.addColumn(asPerDeclaration);

			frb.setTitle("Land Use Zone");
			frb.setTitleStyle(reportService.getTitleStyle());
			frb.setHeaderHeight(5);
			frb.setTopMargin(5);
			frb.setDefaultStyles(reportService.getTitleStyle(), reportService.getSubTitleStyle(),
					reportService.getColumnHeaderStyle(), reportService.getDetailStyle());
			frb.setAllowDetailSplit(false);
			frb.setPageSizeAndOrientation(Page.Page_A4_Portrait());
			frb.setGrandTotalLegend(TOTAL);
			frb.setGrandTotalLegendStyle(reportService.getNumberStyle());
			DynamicReport build = frb.build();
			Subreport sub = new Subreport();
			sub.setDynamicReport(build);
			Style style = new Style();
			style.setStretchWithOverflow(true);
			style.setStreching(RELATIVE_TO_BAND_HEIGHT);
			sub.setStyle(style);

			sub.setDatasource(
					new DJDataSource("LandUseZone", DJConstants.DATA_SOURCE_ORIGIN_PARAMETER, 0));

			sub.setLayoutManager(new ClassicLayoutManager());
			return sub;
		} catch (ColumnBuilderException e) {
			LOG.error(e.getMessage(), e);
		}
		return null;

	}

	private Subreport getLandUseZoneReportRestriction() {
		try {

			FastReportBuilder frb = new FastReportBuilder();
			AbstractColumn title = ColumnBuilder.getNew().setColumnProperty("dpbpCommitteeReviewDecription", String.class.getName())
					.setTitle("Title").setWidth(160).setStyle(reportService.getTotalNumberStyle()).build();
			
			if(rulesSubrulesFlag) {
				AbstractColumn byeLaw = ColumnBuilder.getNew().setColumnProperty("byeLaw2", String.class.getName())
						.setTitle("Byelaw").setWidth(160).setStyle(reportService.getTotalNumberStyle()).build();
				frb.addColumn(byeLaw);
			}
			

			AbstractColumn occupancy = ColumnBuilder.getNew()
					.setColumnProperty("dpbpCommitteeReviewOc", String.class.getName()).setTitle("Occupancy").setWidth(100)
					.setStyle(reportService.getTotalNumberStyle()).build();

			
			
			frb.addColumn(title);
			frb.addColumn(occupancy);

			frb.setTitle("Land Use Zone - Restriction");
			frb.setTitleStyle(reportService.getTitleStyle());
			frb.setHeaderHeight(5);
			frb.setTopMargin(5);
			frb.setDefaultStyles(reportService.getTitleStyle(), reportService.getSubTitleStyle(),
					reportService.getColumnHeaderStyle(), reportService.getDetailStyle());
			frb.setAllowDetailSplit(false);
			frb.setPageSizeAndOrientation(Page.Page_A4_Portrait());
			frb.setGrandTotalLegend(TOTAL);
			frb.setGrandTotalLegendStyle(reportService.getNumberStyle());
			DynamicReport build = frb.build();
			Subreport sub = new Subreport();
			sub.setDynamicReport(build);
			Style style = new Style();
			style.setStretchWithOverflow(true);
			style.setStreching(RELATIVE_TO_BAND_HEIGHT);
			sub.setStyle(style);

			sub.setDatasource(
					new DJDataSource("Land Use Zone - Restriction", DJConstants.DATA_SOURCE_ORIGIN_PARAMETER, 0));

			sub.setLayoutManager(new ClassicLayoutManager());
			return sub;
		} catch (ColumnBuilderException e) {
			LOG.error(e.getMessage(), e);
		}
		return null;

	}

	private List<OccupancyReport> buildSubOccupanciesReport(Map<String, OccupancyPercentage> occupancyPercentage) {
		List<OccupancyReport> occupanciesReport = new ArrayList<OccupancyReport>();
		if(occupancyPercentage!=null) {
			for(String oc : occupancyPercentage.keySet()) {
				OccupancyPercentage ocp = occupancyPercentage.get(oc);
				OccupancyReport ort = new OccupancyReport();
				if(rulesSubrulesFlag)
					ort.setByeLaw("ODA Rule 62, 64(1,2,3), 65(2,3,4), 75(4), 76 (4), 84(1,2,5)");
				ort.setOccupancy(ocp.getOccupancy());
				ort.setSubOccupancy(ocp.getSubOccupancy());
				ort.setPercentage(ocp.getPercentage());
				
				// used setScale to get precision of 2 decimal digits in report only.
				
				ort.setBuildUpArea(ocp.getTotalBuildUpArea().setScale(DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS));
				ort.setCarpetArea(ocp.getTotalCarpetArea().setScale(DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS));
				ort.setFloorArea(ocp.getTotalFloorArea().setScale(DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS));
				occupanciesReport.add(ort);
			}
		}
		return occupanciesReport;
	}

	private List<DcrReportErrorDetail> getDcrReportErrorDetails(Map<String, String> errors) {
		List<DcrReportErrorDetail> dcrReportErrorDetails = new ArrayList<>();
		int slNo = 0;
		for(String key:errors.keySet()) {
			slNo++;
			DcrReportErrorDetail dred = new DcrReportErrorDetail();
			dred.setSlNo(String.valueOf(slNo));
			dred.setDescription(errors.get(key));
			dcrReportErrorDetails.add(dred);
		}
		return dcrReportErrorDetails;
	}

	private Subreport getErrors() {
		try {
			FastReportBuilder frb = new FastReportBuilder();
			
			AbstractColumn errorSlNo = ColumnBuilder.getNew()
					.setColumnProperty("slNo", String.class.getName())
					.setTitle("S.No").setWidth(30).setStyle(reportService.getTotalNumberStyle())
					.build();
			
			AbstractColumn errorDesc = ColumnBuilder.getNew()
					.setColumnProperty("description", String.class.getName())
					.setTitle("Error Description").setWidth(510).setStyle(reportService.getDetailStyle())
					.build();
			
			frb.addColumn(errorSlNo);
			frb.addColumn(errorDesc);
			frb.setHeaderHeight(5);
			frb.setTopMargin(5);
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

			sub.setDatasource(new DJDataSource("Errors", DJConstants.DATA_SOURCE_ORIGIN_PARAMETER, 0));

			sub.setLayoutManager(new ClassicLayoutManager());
			return sub;
		} catch (ColumnBuilderException e) {
			LOG.error(e.getMessage(), e);
		}
		return null;
	}

	public Subreport generateDcrSubReport(final List<DcrReportOutput> dcrReportOutputs) {
		FastReportBuilder drb = new FastReportBuilder();

		final Style titleStyle = new Style("titleStyle");
		titleStyle.setFont(Font.ARIAL_MEDIUM_BOLD);
		titleStyle.setHorizontalAlign(HorizontalAlign.CENTER);
		titleStyle.setVerticalAlign(VerticalAlign.BOTTOM);

		final Style columnStyle = reportService.getColumnStyle();
		final Style columnHeaderStyle = reportService.getColumnHeaderStyle();
		drb.setTitle("Building Rule Scrutiny");
		drb.setTitleStyle(titleStyle);
		try {
			drb.addColumn("KMBR Rule No.", "key", String.class.getName(), 50, columnStyle, columnHeaderStyle);
			drb.addColumn("Rule description", DESCRIPTION, String.class.getName(), 120, columnStyle, columnHeaderStyle);
			drb.addColumn("Required by Rule", "expectedResult", String.class.getName(), 120, columnStyle,
					columnHeaderStyle);
			drb.addColumn("Provided as per drawings", "actualResult", String.class.getName(), 120, columnStyle,
					columnHeaderStyle);
			drb.addColumn("Accepted / Not Accepted ", "status", String.class.getName(), 50, columnStyle,
					columnHeaderStyle);
		} catch (ColumnBuilderException | ClassNotFoundException e) {
			LOG.error(e.getMessage(), e);
		}
		drb.setUseFullPageWidth(true);
		drb.setPageSizeAndOrientation(Page.Page_Legal_Landscape());

		if (LOG.isDebugEnabled()) {
			for (DcrReportOutput dcrReportOutput : dcrReportOutputs) {
				LOG.debug("********* Key " + dcrReportOutput.getKey());
				LOG.debug("********* Description " + dcrReportOutput.getDescription());
				LOG.debug("********* Expected result " + dcrReportOutput.getExpectedResult());
				LOG.debug("********* Actual result " + dcrReportOutput.getActualResult());
				LOG.debug("********* Status " + dcrReportOutput.getStatus());
			}
		}

		new JRBeanCollectionDataSource(dcrReportOutputs);
		final DJDataSource djds = new DJDataSource("subreportds", DJConstants.DATA_SOURCE_ORIGIN_PARAMETER,
				DJConstants.DATA_SOURCE_TYPE_JRDATASOURCE);

		final Subreport subRep = new Subreport();
		subRep.setLayoutManager(new ClassicLayoutManager());
		subRep.setDynamicReport(drb.build());
		subRep.setDatasource(djds);
		subRep.setUseParentReportParameters(true);

		return subRep;
	}
	
	
	// TODO:11 City level customization for Puri code starts here.
	private List<DcrReportDeclarationsDetail> getDcrReportDeclarationsDetails(Plan plan) {

		List<DcrReportDeclarationsDetail> dcrReportDeclarationsDetails = new ArrayList<>();

		if (plan.getPlanInformation().getCrzNumberForProjectsFallingUnderCrzArea() != null
				&& plan.getPlanInformation().getWhetherPlotFallsUnder100mOfGrantRoad() != null
				&& plan.getPlanInformation()
						.getWhetherPlotFallsWithin3KMArialRadiusDistanceOfSriJagannathTemple() != null) {
			for (int i = 1; i <= 3; i++) {
				DcrReportDeclarationsDetail decl = new DcrReportDeclarationsDetail();
				decl.setSlNo("" + i + "");
				decl.setDeclarations(plan.getPlanInformation().getDeclsList()[i - 1]);

				if (i == 1) {
					decl.setResponse(plan.getPlanInformation().getCrzNumberForProjectsFallingUnderCrzArea());
				} else if (i == 2) {
					decl.setResponse(plan.getPlanInformation().getWhetherPlotFallsUnder100mOfGrantRoad());
				} else if (i == 3) {
					decl.setResponse(plan.getPlanInformation()
							.getWhetherPlotFallsWithin3KMArialRadiusDistanceOfSriJagannathTemple());
				}
				dcrReportDeclarationsDetails.add(decl);
			}
		} else if (plan.getPlanInformation().getCrzNumberForProjectsFallingUnderCrzArea() == null
				&& plan.getPlanInformation().getWhetherPlotFallsUnder100mOfGrantRoad() == null
				&& plan.getPlanInformation()
						.getWhetherPlotFallsWithin3KMArialRadiusDistanceOfSriJagannathTemple() == null) {

			for (int i = 1; i <= 3; i++) {
				DcrReportDeclarationsDetail decl = new DcrReportDeclarationsDetail();
				decl.setSlNo("" + i + "");
				decl.setDeclarations(plan.getPlanInformation().getDeclsList()[i - 1]);

				if (i == 1) {
					decl.setResponse(" ");
				} else if (i == 2) {
					decl.setResponse(" ");
				} else if (i == 3) {
					decl.setResponse(" ");
				}
				dcrReportDeclarationsDetails.add(decl);
			}

		} else if (plan.getPlanInformation().getCrzNumberForProjectsFallingUnderCrzArea() == null
				&& plan.getPlanInformation().getWhetherPlotFallsUnder100mOfGrantRoad() == null
				&& plan.getPlanInformation()
						.getWhetherPlotFallsWithin3KMArialRadiusDistanceOfSriJagannathTemple() != null) {

			for (int i = 1; i <= 3; i++) {
				DcrReportDeclarationsDetail decl = new DcrReportDeclarationsDetail();
				decl.setSlNo("" + i + "");
				decl.setDeclarations(plan.getPlanInformation().getDeclsList()[i - 1]);

				if (i == 1) {
					decl.setResponse(" ");
				} else if (i == 2) {
					decl.setResponse(" ");
				} else if (i == 3) {
					decl.setResponse(plan.getPlanInformation()
							.getWhetherPlotFallsWithin3KMArialRadiusDistanceOfSriJagannathTemple());
				}
				dcrReportDeclarationsDetails.add(decl);
			}

		} else if (plan.getPlanInformation().getCrzNumberForProjectsFallingUnderCrzArea() == null
				&& plan.getPlanInformation().getWhetherPlotFallsUnder100mOfGrantRoad() != null
				&& plan.getPlanInformation()
						.getWhetherPlotFallsWithin3KMArialRadiusDistanceOfSriJagannathTemple() == null) {

			for (int i = 1; i <= 3; i++) {
				DcrReportDeclarationsDetail decl = new DcrReportDeclarationsDetail();
				decl.setSlNo("" + i + "");
				decl.setDeclarations(plan.getPlanInformation().getDeclsList()[i - 1]);

				if (i == 1) {
					decl.setResponse(" ");
				} else if (i == 2) {
					decl.setResponse(plan.getPlanInformation().getWhetherPlotFallsUnder100mOfGrantRoad());
				} else if (i == 3) {
					decl.setResponse(" ");
				}
				dcrReportDeclarationsDetails.add(decl);
			}

		} else if (plan.getPlanInformation().getCrzNumberForProjectsFallingUnderCrzArea() == null
				&& plan.getPlanInformation().getWhetherPlotFallsUnder100mOfGrantRoad() != null
				&& plan.getPlanInformation()
						.getWhetherPlotFallsWithin3KMArialRadiusDistanceOfSriJagannathTemple() != null) {

			for (int i = 1; i <= 3; i++) {
				DcrReportDeclarationsDetail decl = new DcrReportDeclarationsDetail();
				decl.setSlNo("" + i + "");
				decl.setDeclarations(plan.getPlanInformation().getDeclsList()[i - 1]);

				if (i == 1) {
					decl.setResponse(" ");
				} else if (i == 2) {
					decl.setResponse(plan.getPlanInformation().getWhetherPlotFallsUnder100mOfGrantRoad());
				} else if (i == 3) {
					decl.setResponse(plan.getPlanInformation()
							.getWhetherPlotFallsWithin3KMArialRadiusDistanceOfSriJagannathTemple());
				}
				dcrReportDeclarationsDetails.add(decl);
			}

		} else if (plan.getPlanInformation().getCrzNumberForProjectsFallingUnderCrzArea() != null
				&& plan.getPlanInformation().getWhetherPlotFallsUnder100mOfGrantRoad() == null
				&& plan.getPlanInformation()
						.getWhetherPlotFallsWithin3KMArialRadiusDistanceOfSriJagannathTemple() == null) {

			for (int i = 1; i <= 3; i++) {
				DcrReportDeclarationsDetail decl = new DcrReportDeclarationsDetail();
				decl.setSlNo("" + i + "");
				decl.setDeclarations(plan.getPlanInformation().getDeclsList()[i - 1]);

				if (i == 1) {
					decl.setResponse(plan.getPlanInformation().getCrzNumberForProjectsFallingUnderCrzArea());
				} else if (i == 2) {
					decl.setResponse(" ");
				} else if (i == 3) {
					decl.setResponse(" ");
				}
				dcrReportDeclarationsDetails.add(decl);
			}

		} else if (plan.getPlanInformation().getCrzNumberForProjectsFallingUnderCrzArea() != null
				&& plan.getPlanInformation().getWhetherPlotFallsUnder100mOfGrantRoad() == null
				&& plan.getPlanInformation()
						.getWhetherPlotFallsWithin3KMArialRadiusDistanceOfSriJagannathTemple() != null) {

			for (int i = 1; i <= 3; i++) {
				DcrReportDeclarationsDetail decl = new DcrReportDeclarationsDetail();
				decl.setSlNo("" + i + "");
				decl.setDeclarations(plan.getPlanInformation().getDeclsList()[i - 1]);

				if (i == 1) {
					decl.setResponse(plan.getPlanInformation().getCrzNumberForProjectsFallingUnderCrzArea());
				} else if (i == 2) {
					decl.setResponse(" ");
				} else if (i == 3) {
					decl.setResponse(plan.getPlanInformation()
							.getWhetherPlotFallsWithin3KMArialRadiusDistanceOfSriJagannathTemple());
				}
				dcrReportDeclarationsDetails.add(decl);
			}

		} else if (plan.getPlanInformation().getCrzNumberForProjectsFallingUnderCrzArea() != null
				&& plan.getPlanInformation().getWhetherPlotFallsUnder100mOfGrantRoad() != null
				&& plan.getPlanInformation()
						.getWhetherPlotFallsWithin3KMArialRadiusDistanceOfSriJagannathTemple() == null) {

			for (int i = 1; i <= 3; i++) {
				DcrReportDeclarationsDetail decl = new DcrReportDeclarationsDetail();
				decl.setSlNo("" + i + "");
				decl.setDeclarations(plan.getPlanInformation().getDeclsList()[i - 1]);

				if (i == 1) {
					decl.setResponse(plan.getPlanInformation().getCrzNumberForProjectsFallingUnderCrzArea());
				} else if (i == 2) {
					decl.setResponse(plan.getPlanInformation().getWhetherPlotFallsUnder100mOfGrantRoad());
				} else if (i == 3) {
					decl.setResponse(" ");
				}
				dcrReportDeclarationsDetails.add(decl);
			}

		}

		return dcrReportDeclarationsDetails;
	}

	private Subreport getDeclarations() {
		try {
			FastReportBuilder frb = new FastReportBuilder();

			AbstractColumn declarationSlNo = ColumnBuilder.getNew().setColumnProperty("slNo", String.class.getName())
					.setTitle("S.No").setWidth(30).setStyle(reportService.getTotalNumberStyle()).build();

			AbstractColumn declaration = ColumnBuilder.getNew()
					.setColumnProperty("declarations", String.class.getName()).setTitle("Declarations").setWidth(300)
					.setStyle(reportService.getDetailStyle()).build();

			AbstractColumn response = ColumnBuilder.getNew().setColumnProperty("response", String.class.getName())
					.setTitle("Responses").setWidth(210).setStyle(reportService.getDetailStyle()).build();

			frb.addColumn(declarationSlNo);
			frb.addColumn(declaration);
			frb.addColumn(response);

			frb.setTitle("CRZ & Other Project Specific Declarations");
			frb.setTitleStyle(reportService.getTitleStyle());
			frb.setHeaderHeight(5);
			frb.setTopMargin(5);
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

			sub.setDatasource(new DJDataSource("Declarations", DJConstants.DATA_SOURCE_ORIGIN_PARAMETER, 0));

			sub.setLayoutManager(new ClassicLayoutManager());
			return sub;

		} catch (ColumnBuilderException e) {
			e.printStackTrace();
			LOG.error(e.getMessage(), e);
		}

		return null;
	}
	// City level customization for Puri code ends here.

	private String buildQRCodeDetails(final EdcrApplication dcrApplication, boolean reportStatus) {
		StringBuilder qrCodeValue = new StringBuilder();
		qrCodeValue = !StringUtils.isEmpty(dcrApplication.getEdcrApplicationDetails().get(0).getDcrNumber())
				? qrCodeValue.append("DCR Number : ")
						.append(dcrApplication.getEdcrApplicationDetails().get(0).getDcrNumber()).append("\n")
				: qrCodeValue.append("DCR Number : ").append("N/A").append("\n");
		qrCodeValue = !StringUtils.isEmpty(dcrApplication.getApplicationNumber())
				? qrCodeValue.append("Application Number : ").append(dcrApplication.getApplicationNumber()).append("\n")
				: qrCodeValue.append("Application Number : ").append("N/A").append("\n");
		qrCodeValue = dcrApplication.getApplicationDate() != null
				? qrCodeValue.append("Application Date : ").append(dcrApplication.getApplicationDate()).append("\n")
				: qrCodeValue.append("Application Date : ").append("N/A").append("\n");
		qrCodeValue = qrCodeValue.append("Report Status :").append(reportStatus ? "Accepted" : "Not Accepted")
				.append("\n");
		return qrCodeValue.toString();
	}

	private List<DcrReportBlockDetail> buildBlockWiseProposedInfo(Plan plan) {
		List<DcrReportBlockDetail> dcrReportBlockDetails = new ArrayList<>();

//		List<Block> blocks = plan.getBlocks();
		List<Block> totalBlocksInPlan=new ArrayList<>();
		totalBlocksInPlan.addAll(plan.getBlocks());
		totalBlocksInPlan.addAll(plan.getOuthouse());

		if (!totalBlocksInPlan.isEmpty()) {

			for (Block block : totalBlocksInPlan) {

				Building building = block.getBuilding();
				if (building != null) {
					DcrReportBlockDetail dcrReportBlockDetail = new DcrReportBlockDetail();
					dcrReportBlockDetail.setBlockNo(block.getNumber());
					dcrReportBlockDetail.setCoverageArea(building.getCoverageArea());
					dcrReportBlockDetail.setBuildingHeight(building.getBuildingHeight());
					dcrReportBlockDetail.setDeclaredBuildingHeight(building.getDeclaredBuildingHeight());
					dcrReportBlockDetail.setConstructedArea(building.getTotalConstructedArea());
					List<Floor> floors = building.getFloors();

					if (!floors.isEmpty()) {
						List<DcrReportFloorDetail> dcrReportFloorDetails = new ArrayList<>();
						for (Floor floor : floors) {

							List<Occupancy> occupancies = floor.getOccupancies();

							if (!occupancies.isEmpty()) {

								for (Occupancy occupancy : occupancies) {
									String occupancyName = "";
									if (occupancy.getTypeHelper() != null)
										if (occupancy.getTypeHelper().getSubtype() != null)
											occupancyName = occupancy.getTypeHelper().getSubtype().getName();
										else {
											if (occupancy.getTypeHelper().getType() != null)
												occupancyName = occupancy.getTypeHelper().getType().getName();
										}
									DcrReportFloorDetail dcrReportFloorDetail = new DcrReportFloorDetail();
									String floorNo;
									if (floor.getTerrace())
										floorNo = "Terrace";
									else if (occupancy.getIsMezzanine())
										floorNo = floor.getNumber() + " (Mezzanine " + floor.getNumber() + ")";
									else
										floorNo = String.valueOf(floor.getNumber());
									dcrReportFloorDetail.setFloorNo(floorNo);
									dcrReportFloorDetail.setOccupancy(occupancyName);
									
									// used setScale to get precision of 2 decimal digits in report only
									
									dcrReportFloorDetail.setBuiltUpArea(
											occupancy.getExistingBuiltUpArea().compareTo(BigDecimal.ZERO) > 0
													? occupancy.getBuiltUpArea()
															.subtract(occupancy.getExistingBuiltUpArea())
															.setScale(DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS)
													: occupancy.getBuiltUpArea().setScale(DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS));
									dcrReportFloorDetail.setFloorArea(
											occupancy.getExistingFloorArea().compareTo(BigDecimal.ZERO) > 0
													? occupancy.getFloorArea()
															.subtract(occupancy.getExistingFloorArea())
															.subtract(occupancy.getFarAreaDeductExisting())
															.setScale(DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS)
													: occupancy.getFloorArea().setScale(DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS));
									dcrReportFloorDetail.setCarpetArea(
											occupancy.getExistingCarpetArea().compareTo(BigDecimal.ZERO) > 0
													? occupancy.getCarpetArea()
															.subtract(occupancy.getExistingCarpetArea())
															.setScale(DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS)
													: occupancy.getCarpetArea().setScale(DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS));
									if (dcrReportFloorDetail.getBuiltUpArea().compareTo(BigDecimal.ZERO) > 0) {
										dcrReportFloorDetails.add(dcrReportFloorDetail);
									}
								}

							}

						}
						
						dcrReportFloorDetails = dcrReportFloorDetails.stream()
							    .sorted(Comparator.comparingInt(d -> Integer.parseInt(d.getFloorNo())))
							    .collect(Collectors.toList());

						dcrReportBlockDetail.setDcrReportFloorDetails(dcrReportFloorDetails);
					}
					dcrReportBlockDetails.add(dcrReportBlockDetail);
				}

			}

		}
		return dcrReportBlockDetails;
	}

	private List<DcrReportBlockDetail> buildBlockWiseExistingInfo(Plan plan) {
		List<DcrReportBlockDetail> dcrReportBlockDetails = new ArrayList<>();

		//List<Block> blocks = plan.getBlocks();
		List<Block> totalBlocksInPlan=new ArrayList<>();
		totalBlocksInPlan.addAll(plan.getBlocks());
		totalBlocksInPlan.addAll(plan.getOuthouse());
		if (!totalBlocksInPlan.isEmpty()) {

			for (Block block : totalBlocksInPlan) {

				Building building = block.getBuilding();
				if (building != null && building.getTotalExistingBuiltUpArea() != null
						&& building.getTotalExistingBuiltUpArea().compareTo(BigDecimal.ZERO) > 0
						&& building.getTotalExistingFloorArea().compareTo(BigDecimal.ZERO) > 0) {
					DcrReportBlockDetail dcrReportBlockDetail = new DcrReportBlockDetail();
					dcrReportBlockDetail.setBlockNo(block.getNumber());

					List<Floor> floors = building.getFloors();

					if (!floors.isEmpty()) {
						List<DcrReportFloorDetail> dcrReportFloorDetails = new ArrayList<>();
						for (Floor floor : floors) {

							List<Occupancy> occupancies = floor.getOccupancies();

							if (!occupancies.isEmpty()) {

								for (Occupancy occupancy : occupancies) {
									String occupancyName = "";
									if (occupancy.getTypeHelper() != null)
										if (occupancy.getTypeHelper().getSubtype() != null)
											occupancyName = occupancy.getTypeHelper().getSubtype().getName();
										else if (occupancy.getTypeHelper().getType() != null)
											occupancyName = occupancy.getTypeHelper().getType().getName();
									if (occupancy != null
											&& occupancy.getExistingBuiltUpArea().compareTo(BigDecimal.ZERO) > 0) {
										DcrReportFloorDetail dcrReportFloorDetail = new DcrReportFloorDetail();
										dcrReportFloorDetail.setFloorNo(
												floor.getTerrace() ? "Terrace" : floor.getNumber().toString());
										dcrReportFloorDetail.setOccupancy(occupancyName);
										
										// used setScale to get precision of 2 decimal digits in report only
										
										dcrReportFloorDetail.setBuiltUpArea(occupancy.getExistingBuiltUpArea().setScale(DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS));
										dcrReportFloorDetail.setFloorArea(occupancy.getExistingFloorArea().setScale(DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS));
										dcrReportFloorDetail.setCarpetArea(occupancy.getExistingCarpetArea().setScale(DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS));
										dcrReportFloorDetails.add(dcrReportFloorDetail);
									}
								}

							}

						}
						dcrReportFloorDetails = dcrReportFloorDetails.stream()
							    .sorted(Comparator.comparingInt(d -> Integer.parseInt(d.getFloorNo())))
							    .collect(Collectors.toList());

						dcrReportBlockDetail.setDcrReportFloorDetails(dcrReportFloorDetails);
					}
					dcrReportBlockDetails.add(dcrReportBlockDetail);
				}

			}

		}
		return dcrReportBlockDetails;
	}

	private VirtualBuildingReport buildVirtualBuilding(VirtualBuilding virtualBuilding) {
		VirtualBuildingReport virtualBuildingReport = new VirtualBuildingReport();

		if (virtualBuilding != null) {
			if (virtualBuilding.getTotalExistingBuiltUpArea() != null) {
				virtualBuildingReport.setProposedBuitUpArea(
						virtualBuilding.getTotalBuitUpArea().subtract(virtualBuilding.getTotalExistingBuiltUpArea()));
				virtualBuildingReport.setProposedFloorArea(
						virtualBuilding.getTotalFloorArea().subtract(virtualBuilding.getTotalExistingFloorArea()));
				virtualBuildingReport.setProposedCarpetArea(
						virtualBuilding.getTotalCarpetArea().subtract(virtualBuilding.getTotalExistingCarpetArea()));
			}
			virtualBuildingReport.setTotalExistingBuiltUpArea(virtualBuilding.getTotalExistingBuiltUpArea());

			virtualBuildingReport.setTotalExistingFloorArea(virtualBuilding.getTotalExistingFloorArea());
			virtualBuildingReport.setTotalExistingCarpetArea(virtualBuilding.getTotalExistingCarpetArea());

			virtualBuildingReport.setTotalCoverageArea(virtualBuilding.getTotalCoverageArea());

			virtualBuildingReport.setTotalBuitUpArea(virtualBuilding.getTotalBuitUpArea());
			virtualBuildingReport.setTotalFloorArea(virtualBuilding.getTotalFloorArea());
			virtualBuildingReport.setTotalCarpetArea(virtualBuilding.getTotalCarpetArea());

			virtualBuildingReport.setTotalConstructedArea(virtualBuilding.getTotalConstructedArea());
		}
		return virtualBuildingReport;
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
	
	private String getSubTitle(ApplicationType applicationType,String alterationSubService,EdcrApplication dcrApplication) {
		String subTitle = null;
		
		if(ApplicationType.OCCUPANCY_CERTIFICATE.getApplicationTypeVal()
				.equalsIgnoreCase(applicationType.getApplicationType())) {
			String planPermissionNumber = dcrApplication.getPlanPermitNumber();
			String bpaApplicationDate = DateUtils.toDefaultDateFormat(dcrApplication.getPermitApplicationDate());
			subTitle = "The occupancy certificate plan is scrutinised with the by-laws which were prevailing on the date "+bpaApplicationDate+" on which the eDCR scrutiny of the building permit application with plan permission number "+planPermissionNumber+" was done";
		}else if(ApplicationType.PERMIT.getApplicationTypeVal()
				.equalsIgnoreCase(applicationType.getApplicationType())) {
			if(DxfFileConstants.ALTERATION_SERVICE_A.equalsIgnoreCase(alterationSubService)) {
				subTitle = "Modification in the received permit letter (No Construction present at site)";
			}else if(DxfFileConstants.ALTERATION_SERVICE_B.equalsIgnoreCase(alterationSubService)) {
				subTitle = "Modification in the received permit letter (With Existing Construction present at site)";
			}
		}
		
		return subTitle;
	}
}
