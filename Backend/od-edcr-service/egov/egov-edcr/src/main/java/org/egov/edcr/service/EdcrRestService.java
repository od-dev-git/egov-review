/*
 * eGov  SmartCity eGovernance suite aims to improve the internal efficiency,transparency,
 * accountability and the service delivery of the government  organizations.
 *
 *  Copyright (C) <2017>  eGovernments Foundation
 *
 *  The updated version of eGov suite of products as by eGovernments Foundation
 *  is available at http://www.egovernments.org
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see http://www.gnu.org/licenses/ or
 *  http://www.gnu.org/licenses/gpl.html .
 *
 *  In addition to the terms of the GPL license to be adhered to in using this
 *  program, the following additional terms are to be complied with:
 *
 *      1) All versions of this program, verbatim or modified must carry this
 *         Legal Notice.
 *      Further, all user interfaces, including but not limited to citizen facing interfaces,
 *         Urban Local Bodies interfaces, dashboards, mobile applications, of the program and any
 *         derived works should carry eGovernments Foundation logo on the top right corner.
 *
 *      For the logo, please refer http://egovernments.org/html/logo/egov_logo.png.
 *      For any further queries on attribution, including queries on brand guidelines,
 *         please contact contact@egovernments.org
 *
 *      2) Any misrepresentation of the origin of the material is prohibited. It
 *         is required that all modified versions of this material be marked in
 *         reasonable ways as different from the original version.
 *
 *      3) This license does not grant any rights to any user of the program
 *         with regards to rights under trademark law for use of the trade names
 *         or trademarks of eGovernments Foundation.
 *
 *  In case of any queries, you can reach eGovernments Foundation at contact@egovernments.org.
 */

package org.egov.edcr.service;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.egov.common.entity.dcr.helper.EdcrApplicationInfo;
import org.egov.common.entity.dcr.helper.ErrorDetail;
import org.egov.common.entity.dcr.helper.OccupancyHelperDetail;
import org.egov.common.entity.edcr.Block;
import org.egov.common.entity.edcr.Building;
import org.egov.common.entity.edcr.Floor;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.PlanInformation;
import org.egov.common.entity.edcr.Plot;
import org.egov.common.entity.edcr.VirtualBuilding;
import org.egov.commons.mdms.config.MdmsConfiguration;
import org.egov.edcr.config.properties.EdcrApplicationSettings;
import org.egov.edcr.constants.DxfFileConstants;
import org.egov.edcr.contract.BuildingRegRequest;
import org.egov.edcr.contract.EdcrDetail;
import org.egov.edcr.contract.EdcrDetailV2;
import org.egov.edcr.contract.EdcrRequest;
import org.egov.edcr.contract.LandRegRequest;
import org.egov.edcr.contract.LayoutRequest;
import org.egov.edcr.contract.OccupancyCertiRequest;
import org.egov.edcr.contract.PermitOrderRequest;
import org.egov.edcr.entity.ApplicationType;
import org.egov.edcr.entity.EdcrApplication;
import org.egov.edcr.entity.EdcrApplicationDetail;
import org.egov.edcr.entity.dto.plan.BlockDTO;
import org.egov.edcr.entity.dto.plan.BuildingDTO;
import org.egov.edcr.entity.dto.plan.FloorDTO;
import org.egov.edcr.entity.dto.plan.OccupancyDTO;
import org.egov.edcr.entity.dto.plan.OccupancyHelperDetailDTO;
import org.egov.edcr.entity.dto.plan.OccupancyTypeDTO;
import org.egov.edcr.entity.dto.plan.OccupancyTypeHelperDTO;
import org.egov.edcr.entity.dto.plan.PlanResponseDTO;
import org.egov.edcr.entity.dto.plan.PlotDTO;
import org.egov.edcr.entity.dto.plan.VirtualBuildingDTO;
import org.egov.edcr.utility.DcrConstants;
import org.egov.infra.admin.master.entity.City;
import org.egov.infra.admin.master.service.CityService;
import org.egov.infra.config.core.ApplicationThreadLocals;
import org.egov.infra.custom.CustomImplProvider;
import org.egov.infra.exception.ApplicationRuntimeException;
import org.egov.infra.filestore.entity.FileStoreMapper;
import org.egov.infra.filestore.repository.FileStoreMapperRepository;
import org.egov.infra.filestore.service.FileStoreService;
import org.egov.infra.microservice.contract.RequestInfoWrapper;
import org.egov.infra.microservice.contract.ResponseInfo;
import org.egov.infra.microservice.models.RequestInfo;
import org.egov.infra.microservice.models.UserInfo;
import org.egov.infra.security.utils.SecurityUtils;
import org.egov.infra.utils.FileStoreUtils;
import org.egov.infra.utils.TenantUtils;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Transactional(readOnly = true)
public class EdcrRestService {
	private static final String MSG_UNQ_TRANSACTION_NUMBER = "Transaction Number should be unique";

	private static final String REQ_BODY_REQUIRED = "Required request body is missing";

	private static final String USER_ID_IS_MANDATORY = "User id is mandatory";

	private static final String BPA_01 = "BPA-01";

	private static final String BPA_07 = "BPA-07";

	private static final String BPA_05 = "BPA-05";

	private static Logger LOG = Logger.getLogger(EdcrApplicationService.class);

	public static final String FILE_DOWNLOAD_URL = "%s/edcr/rest/dcr/downloadfile/";

	@Autowired
	protected SecurityUtils securityUtils;

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private EdcrApplicationSettings edcrApplicationSettings;

	@Autowired
	private EdcrApplicationService edcrApplicationService;

	@Autowired
	private FileStoreService fileStoreService;

	@Autowired
	private TenantUtils tenantUtils;

	@Autowired
	private CityService cityService;

	@Autowired
	private MdmsConfiguration mdmsConfiguration;

	@Autowired
	private OcComparisonService ocComparisonService;

	@Autowired
	private EdcrApplicationDetailService applicationDetailService;

	@Autowired
	private EdcrExternalService edcrExternalService;
	
	@Autowired
	private ApplicationContext applicationContext;

	@Value("${download.url.support.flage}")
	private boolean downloadUrlSupportFlage;

	@Autowired
	private CustomImplProvider specificRuleService;

	@Autowired
	private FileStoreMapperRepository fileStoreMapperRepository;


	public Session getCurrentSession() {
		return entityManager.unwrap(Session.class);
	}

	@Transactional
	public EdcrDetail createEdcr(final EdcrRequest edcrRequest, final MultipartFile file,
			Map<String, List<Object>> masterData) {
		EdcrApplication edcrApplication = new EdcrApplication();
		edcrApplication.setMdmsMasterData(masterData);
		EdcrApplicationDetail edcrApplicationDetail = new EdcrApplicationDetail();
		if (ApplicationType.OCCUPANCY_CERTIFICATE.toString().equalsIgnoreCase(edcrRequest.getAppliactionType())) {
			edcrApplicationDetail.setComparisonDcrNumber(edcrRequest.getComparisonEdcrNumber());
		}else if(edcrRequest.getComparisonEdcrNumber() != null)
			edcrApplicationDetail.setComparisonDcrNumber(edcrRequest.getComparisonEdcrNumber());
		List<EdcrApplicationDetail> edcrApplicationDetails = new ArrayList<>();
		edcrApplicationDetails.add(edcrApplicationDetail);
		edcrApplication.setTransactionNumber(edcrRequest.getTransactionNumber());
		if (isNotBlank(edcrRequest.getApplicantName()))
			edcrApplication.setApplicantName(edcrRequest.getApplicantName());
		else
			edcrApplication.setApplicantName(DxfFileConstants.ANONYMOUS_APPLICANT);
		edcrApplication.setArchitectInformation(DxfFileConstants.ANONYMOUS_APPLICANT);
		edcrApplication.setServiceType(edcrRequest.getApplicationSubType());
		if (edcrRequest.getAppliactionType() == null)
			edcrApplication.setApplicationType(ApplicationType.PERMIT);
		else
			edcrApplication.setApplicationType(ApplicationType.valueOf(edcrRequest.getAppliactionType()));
		if (edcrRequest.getPermitNumber() != null)
			edcrApplication.setPlanPermitNumber(edcrRequest.getPermitNumber());

		if (edcrRequest.getPermitDate() != null) {
			edcrApplication.setApplicationDate(edcrRequest.getPermitDate());
			edcrApplication.setPermitApplicationDate(edcrRequest.getPermitDate());
		}
		
		if(edcrRequest.getPermitNumber() != null) {
			edcrApplication.setPlanPermitNumber(edcrRequest.getPermitNumber());
		}

		edcrApplication.setEdcrApplicationDetails(edcrApplicationDetails);
		edcrApplication.setDxfFile(file);

		if (edcrRequest.getRequestInfo() != null && edcrRequest.getRequestInfo().getUserInfo() != null) {
			edcrApplication.setThirdPartyUserCode(isNotBlank(edcrRequest.getRequestInfo().getUserInfo().getId())
					? edcrRequest.getRequestInfo().getUserInfo().getId()
					: StringUtils.EMPTY);
			edcrApplication.setThirdPartyUserTenant(
					StringUtils.isNotBlank(edcrRequest.getTenantId()) ? edcrRequest.getTenantId()
							: edcrRequest.getRequestInfo().getUserInfo().getTenantId());
		}
		
		if(edcrRequest.getIsRevisionApplication()!=null)
			edcrApplication.setIsRevisionApplication(edcrRequest.getIsRevisionApplication());
		
		if(edcrRequest.getIsApplicationPersentInSujogSystem()!=null) {
			edcrApplication.setIsApplicationPersentInSujogSystem(edcrRequest.getIsApplicationPersentInSujogSystem());
		}
		
		if(edcrRequest.getIsPermitLetterExpried()!=null) {
			edcrApplication.setIsPermitLetterExpried(edcrRequest.getIsPermitLetterExpried());
		}
		
		if(edcrRequest.getAlterationSubService()!=null) {
			edcrApplication.setAlterationSubService(edcrRequest.getAlterationSubService());
		}
		
		if(edcrRequest.getGisData()!=null) {
			edcrApplicationDetail.setGisData(edcrRequest.getGisData());
		}
		
		if (!CollectionUtils.isEmpty(edcrRequest.getRoles())) {
			edcrApplication.setRoles(edcrRequest.getRoles());
		}
		
		if(edcrRequest.getIsCadToPdfEnabled()!=null) {
			edcrApplication.setIsCadToPdfEnabled(edcrRequest.getIsCadToPdfEnabled());
		}
		
		edcrApplication = edcrApplicationService.createRestEdcr(edcrApplication);
		FileStoreUtils.removeFileFromPath(edcrApplication.getSavedDxfFile());
		return setEdcrResponse(edcrApplication.getEdcrApplicationDetails().get(0), edcrRequest);
	}
	

	@Transactional
	public List<EdcrDetail> edcrDetailsResponse(List<EdcrApplicationDetail> edcrApplications, EdcrRequest edcrRequest) {
		List<EdcrDetail> edcrDetails = new ArrayList<>();
		for (EdcrApplicationDetail edcrApp : edcrApplications)
			edcrDetails.add(setEdcrResponse(edcrApp, edcrRequest));

		return edcrDetails;
	}

	public EdcrDetail setEdcrResponse(EdcrApplicationDetail edcrApplnDtl, EdcrRequest edcrRequest) {
		EdcrDetail edcrDetail = new EdcrDetail();
		List<String> planPdfs = new ArrayList<>();
		edcrDetail.setTransactionNumber(edcrApplnDtl.getApplication().getTransactionNumber());
		LOG.info("edcr number == " + edcrApplnDtl.getDcrNumber());
		edcrDetail.setEdcrNumber(edcrApplnDtl.getDcrNumber());
		edcrDetail.setStatus(edcrApplnDtl.getStatus());
		LOG.info("application number ==" + edcrApplnDtl.getApplication().getApplicationNumber());
		edcrDetail.setApplicationNumber(edcrApplnDtl.getApplication().getApplicationNumber());
		edcrDetail.setApplicationDate(edcrApplnDtl.getApplication().getApplicationDate());

		if (edcrApplnDtl.getApplication().getPlanPermitNumber() != null) {
			edcrDetail.setPermitNumber(edcrApplnDtl.getApplication().getPlanPermitNumber());
		}
		if (edcrApplnDtl.getApplication().getPermitApplicationDate() != null) {
			edcrDetail.setPermitDate(edcrApplnDtl.getApplication().getPermitApplicationDate());
		}
		ApplicationType applicationType = edcrApplnDtl.getApplication().getApplicationType();
		if (applicationType != null) {
			Boolean mdmsEnabled = mdmsConfiguration.getMdmsEnabled();
			if (mdmsEnabled != null && mdmsEnabled) {
				if (ApplicationType.PERMIT.getApplicationTypeVal()
						.equalsIgnoreCase(edcrApplnDtl.getApplication().getApplicationType().getApplicationTypeVal())) {
					edcrDetail.setAppliactionType("BUILDING_PLAN_SCRUTINY");
				} else {
					edcrDetail.setAppliactionType("BUILDING_OC_PLAN_SCRUTINY");
				}
			} else
				edcrDetail.setAppliactionType(applicationType.getApplicationTypeVal());

		}
		if (edcrApplnDtl.getApplication().getServiceType() != null)
			edcrDetail.setApplicationSubType(edcrApplnDtl.getApplication().getServiceType());

		if (edcrApplnDtl.getDxfFileId() != null)
			edcrDetail.setDxfFile(format(getFileDownloadUrl(edcrApplnDtl.getDxfFileId().getFileStoreId(),
					ApplicationThreadLocals.getTenantID())));

		if (edcrApplnDtl.getScrutinizedDxfFileId() != null)
			edcrDetail.setUpdatedDxfFile(format(getFileDownloadUrl(
					edcrApplnDtl.getScrutinizedDxfFileId().getFileStoreId(), ApplicationThreadLocals.getTenantID())));

		if (edcrApplnDtl.getReportOutputId() != null)
			edcrDetail.setPlanReport(format(getFileDownloadUrl(edcrApplnDtl.getReportOutputId().getFileStoreId(),
					ApplicationThreadLocals.getTenantID())));

		if (edcrApplnDtl.getShortenedreportOutputId() != null) {
			edcrDetail.setShortenedPlanReport(
					format(getFileDownloadUrl(edcrApplnDtl.getShortenedreportOutputId().getFileStoreId(),
							ApplicationThreadLocals.getTenantID())));
		} else {
			edcrDetail.setShortenedPlanReport(edcrDetail.getPlanReport());
		}
		
		if(edcrApplnDtl.getApplication() != null && edcrApplnDtl.getApplication().getAlterationSubService() != null)
			edcrDetail.setAlterationSubService(edcrApplnDtl.getApplication().getAlterationSubService());

		File file = edcrApplnDtl.getPlanDetailFileStore() != null
				? fileStoreService.fetch(edcrApplnDtl.getPlanDetailFileStore().getFileStoreId(),
						DcrConstants.APPLICATION_MODULE_TYPE)
				: null;

//		if (LOG.isInfoEnabled())
//			LOG.info("**************** End - Reading Plan detail file **************" + file);
		try {
			if (file == null) {
				Plan pl1 = new Plan();
				PlanInformation pi = new PlanInformation();
				pi.setApplicantName(edcrApplnDtl.getApplication().getApplicantName());
				pl1.setPlanInformation(pi);
				edcrDetail.setPlanDetail(pl1);
			} else {
				ObjectMapper mapper = new ObjectMapper();
				mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
				Plan pl1 = mapper.readValue(file, Plan.class);
				pl1.getPlanInformation().setApplicantName(edcrApplnDtl.getApplication().getApplicantName());
//				if (LOG.isInfoEnabled())
//					LOG.info("**************** Plan detail object **************" + pl1);
				edcrDetail.setPlanDetail(pl1);
			}
		} catch (IOException e) {
			LOG.log(Level.ERROR, e);
		} catch (Exception e) {
			LOG.log(Level.ERROR, e);
		} 

		if (edcrApplnDtl.getDxfFileId() != null)
			planPdfs.add(format(getFileDownloadUrl(edcrApplnDtl.getDxfFileId().getFileStoreId(),
					ApplicationThreadLocals.getTenantID())));
		
		for (org.egov.edcr.entity.EdcrPdfDetail planPdf : edcrApplnDtl.getEdcrPdfDetails()) {
			if (planPdf.getConvertedPdf() != null) {
				String downloadURL = format(getFileDownloadUrl(planPdf.getConvertedPdf().getFileStoreId(),
						edcrRequest.getTenantId()));
				planPdfs.add(downloadURL);
				if(edcrDetail.getPlanDetail().getEdcrPdfDetails() != null)
					for (org.egov.common.entity.edcr.EdcrPdfDetail pdf : edcrDetail.getPlanDetail().getEdcrPdfDetails()) {
						if (planPdf.getLayer().equalsIgnoreCase(pdf.getLayer()))
							pdf.setDownloadURL(downloadURL);
					}
				if(edcrDetail.getPlanDetail().getEdcrPdfDetails1() != null)
					for (org.egov.common.entity.edcr.EdcrPdfDetail pdf : edcrDetail.getPlanDetail().getEdcrPdfDetails1()) {
						if (planPdf.getLayer().equalsIgnoreCase(pdf.getLayer()))
							pdf.setDownloadURL(downloadURL);
					}
				
				if(edcrDetail.getPlanDetail().getEdcrPdfDetails2() != null)
					for (org.egov.common.entity.edcr.EdcrPdfDetail pdf : edcrDetail.getPlanDetail().getEdcrPdfDetails2()) {
						if (planPdf.getLayer().equalsIgnoreCase(pdf.getLayer()))
							pdf.setDownloadURL(downloadURL);
					}
				
				if (planPdf.getLayer().equalsIgnoreCase("BASE_LAYERS")) {
		            edcrDetail.setDxfToPdfBase(planPdf.getConvertedPdf().getFileStoreId());
		        } else if (planPdf.getLayer().equalsIgnoreCase("BASE_AND_OBPAS_LAYERS")) {
		            edcrDetail.setDxfToPdfBasePlusObpas(planPdf.getConvertedPdf().getFileStoreId());
		        }
			}
		}		

		if (edcrApplnDtl.getReportOutputId() != null)
			planPdfs.add(format(getFileDownloadUrl(edcrApplnDtl.getReportOutputId().getFileStoreId(),
					ApplicationThreadLocals.getTenantID())));

		edcrDetail.setPlanPdfs(planPdfs);
		edcrDetail.setTenantId(edcrRequest.getTenantId());

		if (StringUtils.isNotBlank(edcrRequest.getComparisonEdcrNumber()))
			edcrDetail.setComparisonEdcrNumber(edcrRequest.getComparisonEdcrNumber());

		if (!edcrApplnDtl.getStatus().equalsIgnoreCase("Accepted"))
			edcrDetail.setStatus(edcrApplnDtl.getStatus());
		try {
			Files.deleteIfExists(file.toPath());
		} catch (IOException e) {
			LOG.log(Level.ERROR, e);
		}
		
		if(edcrApplnDtl.getGisData()!=null) {
			ObjectMapper objectMapper = new ObjectMapper();
			try {
				edcrDetail.setGisData(objectMapper.readTree(edcrApplnDtl.getGisData()));
			} catch (IOException e) {
				LOG.log(Level.ERROR, e);
			}
		}
		return edcrDetail;
	}

	public EdcrDetail setEdcrResponseForAcrossTenants(Object[] applnDtls, String stateCityCode) {
		EdcrDetail edcrDetail = new EdcrDetail();
		edcrDetail.setTransactionNumber(String.valueOf(applnDtls[1]));
		edcrDetail.setEdcrNumber(String.valueOf(applnDtls[2]));
		edcrDetail.setStatus(String.valueOf(applnDtls[3]));
		edcrDetail.setApplicationDate(new LocalDate(String.valueOf(applnDtls[9])).toDate());
		edcrDetail.setApplicationNumber(String.valueOf(applnDtls[10]));

		if (String.valueOf(applnDtls[5]) != null)
			edcrDetail
					.setDxfFile(format(getFileDownloadUrl(String.valueOf(applnDtls[5]), String.valueOf(applnDtls[0]))));

		if (String.valueOf(applnDtls[6]) != null)
			edcrDetail.setUpdatedDxfFile(
					format(getFileDownloadUrl(String.valueOf(applnDtls[6]), String.valueOf(applnDtls[0]))));

		if (String.valueOf(applnDtls[7]) != null)
			edcrDetail.setPlanReport(
					format(getFileDownloadUrl(String.valueOf(applnDtls[7]), String.valueOf(applnDtls[0]))));
		Plan pl1 = new Plan();
		PlanInformation pi = new PlanInformation();
		pi.setApplicantName(String.valueOf(applnDtls[4]));
		pl1.setPlanInformation(pi);
		edcrDetail.setPlanDetail(pl1);

		edcrDetail.setTenantId(stateCityCode.concat(".").concat(String.valueOf(applnDtls[0])));

		if (!String.valueOf(applnDtls[3]).equalsIgnoreCase("Accepted"))
			edcrDetail.setStatus(String.valueOf(applnDtls[3]));

		return edcrDetail;
	}

	public List<EdcrDetail> fetchEdcr(final EdcrRequest edcrRequest, final RequestInfoWrapper reqInfoWrapper) {
		List<EdcrApplicationDetail> edcrApplications = new ArrayList<>();
		UserInfo userInfo = reqInfoWrapper.getRequestInfo() == null ? null
				: reqInfoWrapper.getRequestInfo().getUserInfo();
		boolean onlyTenantId = edcrRequest != null && isBlank(edcrRequest.getEdcrNumber())
				&& isBlank(edcrRequest.getTransactionNumber()) && isBlank(edcrRequest.getAppliactionType())
				&& isBlank(edcrRequest.getApplicationSubType()) && isNotBlank(edcrRequest.getTenantId());

		City stateCity = cityService.fetchStateCityDetails();
		if (edcrRequest != null && edcrRequest.getTenantId().equalsIgnoreCase(stateCity.getCode())) {
			final Map<String, String> params = new ConcurrentHashMap<>();
			Map<String, String> tenants = tenantUtils.tenantsMap();
			StringBuilder queryStr = new StringBuilder();
			Iterator<Map.Entry<String, String>> tenantItr = tenants.entrySet().iterator();
			while (tenantItr.hasNext()) {
				Map.Entry<String, String> value = tenantItr.next();
				queryStr.append("(select '").append(value.getKey()).append(
						"' as tenantId,appln.transactionNumber,dtl.dcrNumber,dtl.status,appln.applicantName,dxf.fileStoreId as dxfFileId,scrudxf.fileStoreId as scrutinizedDxfFileId,rofile.fileStoreId as reportOutputId,pdfile.fileStoreId as planDetailFileStore,appln.applicationDate,appln.applicationNumber from ")
						.append(value.getKey()).append(".edcr_application appln, ").append(value.getKey())
						.append(".edcr_application_detail dtl, ").append(value.getKey())
						.append(".eg_filestoremap dxf, ").append(value.getKey()).append(".eg_filestoremap scrudxf, ")
						.append(value.getKey()).append(".eg_filestoremap rofile, ").append(value.getKey())
						.append(".eg_filestoremap pdfile ")
						.append("where appln.id = dtl.application and dtl.dxfFileId=dxf.id and dtl.scrutinizedDxfFileId=scrudxf.id and dtl.reportOutputId=rofile.id and dtl.planDetailFileStore=pdfile.id ");

				if (isNotBlank(edcrRequest.getEdcrNumber())) {
					queryStr.append("and dtl.dcrNumber=:dcrNumber ");
					params.put("dcrNumber", edcrRequest.getEdcrNumber());
				}

				if (isNotBlank(edcrRequest.getTransactionNumber())) {
					queryStr.append("and appln.transactionNumber=:transactionNumber ");
					params.put("transactionNumber", edcrRequest.getTransactionNumber());
				}

				if (onlyTenantId && userInfo != null && isNotBlank(userInfo.getId())) {
					queryStr.append("and appln.thirdPartyUserCode=:thirdPartyUserCode ");
					params.put("thirdPartyUserCode", userInfo.getId());
				}

				String appliactionType = edcrRequest.getAppliactionType();
				if (isNotBlank(appliactionType)) {
					ApplicationType applicationType = null;
					if ("BUILDING_PLAN_SCRUTINY".equalsIgnoreCase(appliactionType)) {
						applicationType = ApplicationType.PERMIT;
					} else if ("BUILDING_OC_PLAN_SCRUTINY".equalsIgnoreCase(appliactionType)) {
						applicationType = ApplicationType.OCCUPANCY_CERTIFICATE;
					} else if ("Permit".equalsIgnoreCase(appliactionType)) {
						applicationType = ApplicationType.PERMIT;
					} else if ("Occupancy certificate".equalsIgnoreCase(appliactionType)) {
						applicationType = ApplicationType.OCCUPANCY_CERTIFICATE;
					}
					queryStr.append("and appln.applicationType=:applicationtype ");
					params.put("applicationtype", applicationType.toString());
				}

				if (isNotBlank(edcrRequest.getApplicationSubType())) {
					queryStr.append("and appln.serviceType=:servicetype ");
					params.put("servicetype", edcrRequest.getApplicationSubType());
				}

				queryStr.append(" order by appln.createddate desc)");
				if (tenantItr.hasNext()) {
					queryStr.append(" union ");
				}
			}

			final Query query = getCurrentSession().createSQLQuery(queryStr.toString());
			for (final Map.Entry<String, String> param : params.entrySet())
				query.setParameter(param.getKey(), param.getValue());
			List<Object[]> applns = query.list();
			if (applns.isEmpty()) {
				EdcrDetail edcrDetail = new EdcrDetail();
				edcrDetail.setErrors("No Record Found");
				return Arrays.asList(edcrDetail);
			} else {
				List<EdcrDetail> edcrDetails2 = new ArrayList<>();
				for (Object[] appln : applns)
					edcrDetails2.add(setEdcrResponseForAcrossTenants(appln, stateCity.getCode()));
				return edcrDetails2;
			}
		} else {
			final Criteria criteria = getCurrentSession().createCriteria(EdcrApplicationDetail.class,
					"edcrApplicationDetail");
			criteria.createAlias("edcrApplicationDetail.application", "application");
			if (edcrRequest != null && isNotBlank(edcrRequest.getEdcrNumber())) {
				criteria.add(Restrictions.eq("edcrApplicationDetail.dcrNumber", edcrRequest.getEdcrNumber()));
			}
			if (edcrRequest != null && isNotBlank(edcrRequest.getTransactionNumber())) {
				criteria.add(Restrictions.eq("application.transactionNumber", edcrRequest.getTransactionNumber()));
			}

			String appliactionType = edcrRequest.getAppliactionType();

			if (edcrRequest != null && isNotBlank(appliactionType)) {
				ApplicationType applicationType = null;
				if ("BUILDING_PLAN_SCRUTINY".equalsIgnoreCase(appliactionType)) {
					applicationType = ApplicationType.PERMIT;
				} else if ("BUILDING_OC_PLAN_SCRUTINY".equalsIgnoreCase(appliactionType)) {
					applicationType = ApplicationType.OCCUPANCY_CERTIFICATE;
				}
				if ("Permit".equalsIgnoreCase(appliactionType)) {
					applicationType = ApplicationType.PERMIT;
				} else if ("Occupancy certificate".equalsIgnoreCase(appliactionType)) {
					applicationType = ApplicationType.OCCUPANCY_CERTIFICATE;
				}
				criteria.add(Restrictions.eq("application.applicationType", applicationType));
			}

			if (edcrRequest != null && isNotBlank(edcrRequest.getApplicationSubType())) {
				criteria.add(Restrictions.eq("application.serviceType", edcrRequest.getApplicationSubType()));
			}

			if (onlyTenantId && userInfo != null && isNotBlank(userInfo.getId())) {
				criteria.add(Restrictions.eq("application.thirdPartyUserCode", userInfo.getId()));
			}

			criteria.addOrder(Order.asc("edcrApplicationDetail.createdDate"));
			criteria.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
			edcrApplications = criteria.list();
		}

		//LOG.info("The number of records = " + edcrApplications.size());
		if (edcrApplications.isEmpty()) {
			EdcrDetail edcrDetail = new EdcrDetail();
			edcrDetail.setErrors("No Record Found");
			return Arrays.asList(edcrDetail);
		} else {
			return edcrDetailsResponse(edcrApplications, edcrRequest);
		}
	}

	public ErrorDetail validatePlanFile(final MultipartFile file) {
		List<String> dcrAllowedExtenstions = new ArrayList<>(
				Arrays.asList(edcrApplicationSettings.getValue("dcr.dxf.allowed.extenstions").split(",")));

		String fileSize = edcrApplicationSettings.getValue("dcr.dxf.max.size");
		final String maxAllowSizeInMB = fileSize;
		String extension;
		if (file != null && !file.isEmpty()) {
			extension = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf('.') + 1);
			if (extension != null && !extension.isEmpty()) {

				if (!dcrAllowedExtenstions.contains(extension.toLowerCase())) {
					return new ErrorDetail("BPA-02", "Please upload " + dcrAllowedExtenstions + " format file only");
				} else if (file.getSize() > (Long.valueOf(maxAllowSizeInMB) * 1024 * 1024)) {
					return new ErrorDetail("BPA-04", "File size should not exceed 300 MB");
				} /*
					 * else if (allowedExtenstions.contains(extension.toLowerCase()) &&
					 * (!mimeTypes.contains(mimeType) ||
					 * StringUtils.countMatches(file.getOriginalFilename(), ".") > 1 ||
					 * file.getOriginalFilename().contains("%00"))) { return new
					 * ErrorDetail("BPA-03", "Malicious file upload"); }
					 */
			}
		} else {
			return new ErrorDetail(BPA_05, "Please upload plan file, It is mandatory");
		}

		return null;
	}
	
	public List<ErrorDetail> validateAlterationCase(EdcrRequest edcr) {
		List<ErrorDetail> errors = new ArrayList<>();
		
		if (edcr.getApplicationSubType().equalsIgnoreCase("Alteration")) {
			if (edcr.getAlterationSubService() == null) {
				errors.add(new ErrorDetail("BPA-29", "alterationSubService cannot be null for alteration service type"));
			}
		}
		
		return errors;
		
	}

	public ErrorDetail validateEdcrRequest(final EdcrRequest edcrRequest, final MultipartFile planFile) {
		if (edcrRequest.getRequestInfo() == null)
			return new ErrorDetail(BPA_07, REQ_BODY_REQUIRED);
		else if (edcrRequest.getRequestInfo().getUserInfo() == null
				|| (edcrRequest.getRequestInfo().getUserInfo() != null
						&& isBlank(edcrRequest.getRequestInfo().getUserInfo().getId())))
			return new ErrorDetail(BPA_07, USER_ID_IS_MANDATORY);

		if (isBlank(edcrRequest.getTransactionNumber()))
			return new ErrorDetail(BPA_07, "Please enter transaction number");
		if (isNotBlank(edcrRequest.getTransactionNumber())
				&& edcrApplicationService.findByTransactionNumber(edcrRequest.getTransactionNumber()) != null) {
			return new ErrorDetail(BPA_01, MSG_UNQ_TRANSACTION_NUMBER);
		}

		return validatePlanFile(planFile);
	}

	public ErrorDetail validateEdcrOcRequest(final EdcrRequest edcrRequest, final MultipartFile planFile) {
		if (edcrRequest.getRequestInfo() == null)
			return new ErrorDetail(BPA_07, REQ_BODY_REQUIRED);
		else if (edcrRequest.getRequestInfo().getUserInfo() == null
				|| (edcrRequest.getRequestInfo().getUserInfo() != null
						&& isBlank(edcrRequest.getRequestInfo().getUserInfo().getId())))
			return new ErrorDetail(BPA_07, USER_ID_IS_MANDATORY);

		if (isBlank(edcrRequest.getTransactionNumber()))
			return new ErrorDetail(BPA_07, "Transaction number is mandatory");

		if (null == edcrRequest.getPermitDate())
			return new ErrorDetail("BPA-08", "Permit Date is mandatory");
		if (isNotBlank(edcrRequest.getTransactionNumber())
				&& edcrApplicationService.findByTransactionNumber(edcrRequest.getTransactionNumber()) != null) {
			return new ErrorDetail(BPA_01, MSG_UNQ_TRANSACTION_NUMBER);

		}

		return validatePlanFile(planFile);
	}

	public List<ErrorDetail> validateScrutinizeOcRequest(final EdcrRequest edcrRequest, final MultipartFile planFile) {
		List<ErrorDetail> errorDetails = new ArrayList<>();

		if (edcrRequest.getRequestInfo() == null)
			errorDetails.add(new ErrorDetail(BPA_07, REQ_BODY_REQUIRED));
		else if (edcrRequest.getRequestInfo().getUserInfo() == null
				|| (edcrRequest.getRequestInfo().getUserInfo() != null
						&& isBlank(edcrRequest.getRequestInfo().getUserInfo().getId())))
			errorDetails.add(new ErrorDetail("BPA-08", USER_ID_IS_MANDATORY));

		if (isBlank(edcrRequest.getTransactionNumber()))
			errorDetails.add(new ErrorDetail("BPA-09", "Transaction number is mandatory"));

		if (null == edcrRequest.getPermitDate())
			errorDetails.add(new ErrorDetail("BPA-10", "Permit Date is mandatory"));
		if (isNotBlank(edcrRequest.getTransactionNumber())
				&& edcrApplicationService.findByTransactionNumber(edcrRequest.getTransactionNumber()) != null) {
			errorDetails.add(new ErrorDetail("BPA-11", MSG_UNQ_TRANSACTION_NUMBER));

		}

		String dcrNo = edcrRequest.getComparisonEdcrNumber();
		if (StringUtils.isBlank(dcrNo)) {
			errorDetails.add(new ErrorDetail("BPA-29", "Comparison eDcr number is mandatory"));
		} else {
			EdcrApplicationDetail permitDcr = applicationDetailService.findByDcrNumberAndTPUserTenant(dcrNo,
					edcrRequest.getTenantId());

			if (permitDcr != null && permitDcr.getApplication() != null
					&& StringUtils.isBlank(permitDcr.getApplication().getServiceType())) {
				errorDetails.add(new ErrorDetail("BPA-26", "No service type found for dcr number " + dcrNo));
			}

			if (permitDcr == null) {
				errorDetails.add(new ErrorDetail("BPA-24", "No record found with dcr number " + dcrNo));
			}

			if (permitDcr != null && permitDcr.getApplication() != null && edcrRequest.getAppliactionType()
					.equalsIgnoreCase(permitDcr.getApplication().getApplicationType().toString())) {
				errorDetails.add(new ErrorDetail("BPA-27", "Application types are same"));
			}

			if (permitDcr != null && permitDcr.getApplication() != null && !edcrRequest.getApplicationSubType()
					.equalsIgnoreCase(permitDcr.getApplication().getServiceType())) {
				errorDetails.add(new ErrorDetail("BPA-28", "Service types are not mathing"));
			}
		}

		ErrorDetail validatePlanFile = validatePlanFile(planFile);
		if (validatePlanFile != null)
			errorDetails.add(validatePlanFile);

		return errorDetails;
	}

	public List<ErrorDetail> validateEdcrMandatoryFields(final EdcrRequest edcrRequest) {
		List<ErrorDetail> errors = new ArrayList<>();
		if (StringUtils.isBlank(edcrRequest.getAppliactionType())) {
			errors.add(new ErrorDetail("BPA-10", "Application type is missing"));
		}

		if (StringUtils.isBlank(edcrRequest.getApplicationSubType())) {
			errors.add(new ErrorDetail("BPA-11", "Service type is missing"));
		}
		
		if(CollectionUtils.isEmpty(edcrRequest.getRoles())) {
			errors.add(new ErrorDetail("BPA-12", "Technical Person roles cannot be empty"));
		}

		return errors;
	}
	
	public List<ErrorDetail> validateTechnicalRoles(final EdcrRequest edcrRequest) {
		String[] allowedRoleCodes = { "BPA_ARCHITECT", "BPA_CIVILENGINEERTP", "BPA_SUPERVISORTP" };
		List<ErrorDetail> errors = new ArrayList<>();

		List<String> allRoles = edcrRequest.getRoles().stream().map(role -> role.getCode())
				.collect(Collectors.toList());

		boolean var = allRoles.stream().anyMatch(role -> Arrays.asList(allowedRoleCodes).contains(role));

		if (var) {
			return errors;
		} else {
			errors.add(new ErrorDetail("BPA-13", "Provided role is not allowed to use scrutiny!"));
		}

		return errors;
	}
	

	public ErrorDetail validateSearchRequest(final String edcrNumber, final String transactionNumber) {
		ErrorDetail errorDetail = null;
		if (isBlank(edcrNumber) && isBlank(transactionNumber))
			return new ErrorDetail(BPA_07, "Please enter valid edcr number or transaction number");
		return errorDetail;
	}

	/*
	 * public String getMimeType(final MultipartFile file) {
	 * MimeUtil.registerMimeDetector(
	 * "eu.medsea.mimeutil.detector.MagicMimeMimeDetector");
	 * eu.medsea.mimeutil.MimeType mimeType = null; try { mimeType =
	 * MimeUtil.getMostSpecificMimeType(MimeUtil.getMimeTypes(file.getInputStream())
	 * ); } catch (MimeException | IOException e) { LOG.error(e); }
	 * MimeUtil.unregisterMimeDetector(
	 * "eu.medsea.mimeutil.detector.MagicMimeMimeDetector"); return
	 * String.valueOf(mimeType); }
	 */

	public ErrorDetail validateParam(List<String> allowedExtenstions, List<String> mimeTypes, MultipartFile file,
			final String maxAllowSizeInMB) {
		String extension;
		String mimeType;
		if (file != null && !file.isEmpty()) {
			extension = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf('.') + 1);
			if (extension != null && !extension.isEmpty()) {
				// mimeType = getMimeType(file);
				if (!allowedExtenstions.contains(extension.toLowerCase())) {
					return new ErrorDetail("BPA-02", "Please upload " + allowedExtenstions + " format file only");
				} else if (file.getSize() > (Long.valueOf(maxAllowSizeInMB) * 1024 * 1024)) {
					return new ErrorDetail("BPA-04", "File size should not exceed 30 MB");
				} /*
					 * else if (allowedExtenstions.contains(extension.toLowerCase()) &&
					 * (!mimeTypes.contains(mimeType) ||
					 * StringUtils.countMatches(file.getOriginalFilename(), ".") > 1 ||
					 * file.getOriginalFilename().contains("%00"))) { return new
					 * ErrorDetail("BPA-03", "Malicious file upload"); }
					 */
			}
		} else {
			return new ErrorDetail(BPA_05, "Please, upload plan file is mandatory");
		}

		return null;
	}

	public ResponseInfo createResponseInfoFromRequestInfo(RequestInfo requestInfo, Boolean success) {
		String apiId = null;
		String ver = null;
		String ts = null;
		String resMsgId = "";
		String msgId = null;
		if (requestInfo != null) {
			apiId = requestInfo.getApiId();
			ver = requestInfo.getVer();
			if (requestInfo.getTs() != null)
				ts = requestInfo.getTs().toString();
			msgId = requestInfo.getMsgId();
		}
		String responseStatus = success ? "successful" : "failed";

		return new ResponseInfo(apiId, ver, ts, resMsgId, msgId, responseStatus);
	}

	public String getFileDownloadUrl(final String fileStoreId, final String tenantId) {
		String dUrl = String.format(FILE_DOWNLOAD_URL, ApplicationThreadLocals.getDomainURL()) + fileStoreId
				+ "?tenantId=" + tenantId;
		if (downloadUrlSupportFlage) {
			dUrl = dUrl.replace("http:", "https:");
		}
		return dUrl;
	}
	
	@Transactional
	public List<String> generateBaseLayerId(PermitOrderRequest permitOrderRequest) {

		List<String> fileStoreIds = new ArrayList<>();
		try {
			String edcrNo = null;
			if (permitOrderRequest.getBpaList().get(0).get("edcrNumber") != null) {
				edcrNo = permitOrderRequest.getBpaList().get(0).get("edcrNumber").toString();
			}
			String businessService = permitOrderRequest.getBpaList().get(0).get("businessService").toString();
			if (edcrNo != null)
				edcrNo.trim();

			EdcrApplicationInfo edcrApplicationInfo = null;
			try {
				if (!DxfFileConstants.BPA_PRE_APPROVED_CODE.equals(businessService)) {
					edcrApplicationInfo = edcrExternalService.loadEdcrApplicationDetails(edcrNo);
				}
			} catch (Exception e) {
				LOG.error("Error while feating EDCR data");
			}

			String baseLayerFileStoreId = edcrApplicationInfo.getBaseLayerFileStoreId();

			fileStoreIds.add(baseLayerFileStoreId);

		} catch (ApplicationRuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new ApplicationRuntimeException("Error while generating base layer filestore Id", e);
		}

		return fileStoreIds;
	}

	/**
	 * generate permit order pdf using iText
	 * 
	 * @param permitOrderRequest
	 * @param permitReportType   eg: V1, V2
	 * @return
	 */
	@Transactional
	public List<String> genratePermitOrder(PermitOrderRequest permitOrderRequest, Boolean isPreview) {
		List<String> fileStoreIds = null;		
		try {
			String edcrNo = UUID.randomUUID().toString();
			if (permitOrderRequest.getBpaList().get(0).get("edcrNumber") != null) {
				edcrNo = permitOrderRequest.getBpaList().get(0).get("edcrNumber").toString();
			}
			if(edcrNo!=null)
				edcrNo.trim();
			String bpaApplication = permitOrderRequest.getBpaList().get(0).get("applicationNo").toString();
			String fileName = bpaApplication + "-" + edcrNo;
			String bpaAppTenantId = permitOrderRequest.getBpaList().get(0).get("tenantId").toString();
			String businessService = permitOrderRequest.getBpaList().get(0).get("businessService").toString();
			
			//List<FileStoreMapper> fileStoreMappers = fileStoreMapperRepository.findByFileNameStartsWith(fileName);
			
			List<FileStoreMapper> fileStoreMappers = new ArrayList<>();
//			if (fileStoreMappers == null) {
//				fileStoreMappers = new ArrayList<>();
//			}
			fileName = fileName + "-V-" + fileStoreMappers.size() + ".pdf";
			Plan plan = null;
			boolean flage = false;
			try {
				if (!DxfFileConstants.BPA_PRE_APPROVED_CODE.equals(businessService) || flage) {
					EdcrApplicationInfo edcrApplicationInfo = edcrExternalService.loadEdcrApplicationDetails(edcrNo);
					plan = edcrApplicationInfo.getPlan();
				}
			} catch (Exception e) {
				LOG.error("Error while feating EDCR data");
				plan = null;
			}
			PermitOrderService permitOrderService = getPermitOrderServiceBean(businessService);
			InputStream reportStream = permitOrderService.generateReport(plan, permitOrderRequest.getBpaList().get(0),
					permitOrderRequest.getRequestInfo(), isPreview);
			FileStoreMapper fileStoreMapper = storePermitOrder(reportStream, fileName, bpaAppTenantId);
			fileStoreMappers.add(fileStoreMapper);
			fileStoreIds = fileStoreMappers.stream()
					.sorted((fm1, fm2) -> fm2.getCreatedDate().compareTo(fm1.getCreatedDate()))
					.map(fm -> fm.getFileStoreId()).collect(Collectors.toList());
		}catch (ApplicationRuntimeException e) {
			throw e;
		} 
		catch (Exception e) {
			throw new ApplicationRuntimeException("Error while generating permit order pdf", e);
		}
		return fileStoreIds;
	}

	private PermitOrderService getPermitOrderServiceBean(String businessService) {
		PermitOrderService permitOrderService = null;
		if(businessService.equalsIgnoreCase("BPA7")) {
			permitOrderService = (PermitOrderService) specificRuleService
					.find("permitOrderService" + "BPA1");
		} else {
			permitOrderService = (PermitOrderService) specificRuleService
					.find("permitOrderService" + businessService);
		}		
		if(permitOrderService == null)
			throw new ApplicationRuntimeException("Permit order not supported for businessService: "+businessService);
		return permitOrderService;
	}

	@Transactional
	private FileStoreMapper storePermitOrder(InputStream reportStream, String fileName, String bpaAppTenantId) {
		String edcrTenantId = ApplicationThreadLocals.getTenantID();
		ApplicationThreadLocals.setTenantID(bpaAppTenantId);
		final FileStoreMapper fileStoreMapper = fileStoreService.store(reportStream, fileName, "application/pdf",
				DcrConstants.FILESTORE_MODULECODE);
		ApplicationThreadLocals.setTenantID(edcrTenantId);
		if (fileStoreMapper != null) {
			fileStoreMapperRepository.save(fileStoreMapper);
		}
		return fileStoreMapper;
	}
	
	
	@Transactional
	public List<String> genrateLandRegularizationCertificate(LandRegRequest landRegRequest, Boolean isPreview) {

		List<String> fileStoreIds = null;

		try {
			String LRApplicationNo = landRegRequest.getRegularizations().get(0).get("applicationNo").toString();
			String bpaAppTenantId = landRegRequest.getRegularizations().get(0).get("tenantId").toString();
			String serviceSubType = landRegRequest.getRegularizations().get(0).get("serviceSubType").toString();

			String fileName = LRApplicationNo + "";
			List<FileStoreMapper> fileStoreMappers = new ArrayList<>();
			fileName = fileName + "-V-" + fileStoreMappers.size() + ".pdf";

			LandRegCertificateService landRegCertificateService = null;
			// if true, single plot regularization
			if (serviceSubType.equalsIgnoreCase("Single Plot Regularization")) {
				landRegCertificateService = (LandRegCertificateService) specificRuleService
						.find("LandRegCertificateService" + "SinglePlot");
				if (landRegCertificateService == null)
					throw new ApplicationRuntimeException(
							"Error while generating Land regularization certificate for single plot.");
			} else {
				landRegCertificateService = (LandRegCertificateService) specificRuleService
						.find("LandRegCertificateService" + "MultiPlot");
				if (landRegCertificateService == null)
					throw new ApplicationRuntimeException(
							"Error while generating Land regularization certificate for Multiple plots.");

			}
			
			InputStream reportStream = landRegCertificateService.generateReport(landRegRequest.getRegularizations().get(0),
					landRegRequest.getRequestInfo(), isPreview);
			
			FileStoreMapper fileStoreMapper = storePermitOrder(reportStream, fileName, bpaAppTenantId);
			fileStoreMappers.add(fileStoreMapper);
			fileStoreIds = fileStoreMappers.stream()
					.sorted((fm1, fm2) -> fm2.getCreatedDate().compareTo(fm1.getCreatedDate()))
					.map(fm -> fm.getFileStoreId()).collect(Collectors.toList());
			

		} catch (Exception e) {
			e.printStackTrace();
		}

		return fileStoreIds;
	}
	
	/**
	 * 
	 * @param buildingRegRequest
	 * @param isPreview
	 * @return
	 */
	@Transactional
	public List<String> generateBuildingRegularizationCertificate(BuildingRegRequest buildingRegRequest, Boolean isPreview) {

		List<String> fileStoreIds = null;

		try {
			String LRApplicationNo = buildingRegRequest.getRegularizations().get(0).get("applicationNo").toString();
			String bpaAppTenantId = buildingRegRequest.getRegularizations().get(0).get("tenantId").toString();
			String businessService = buildingRegRequest.getRegularizations().get(0).get("businessService").toString();

			String fileName = LRApplicationNo + "";
			List<FileStoreMapper> fileStoreMappers = new ArrayList<>();
			fileName = fileName + "-V-" + fileStoreMappers.size() + ".pdf";

			BuildingRegCertificateService buildingRegCertificateService = null;
			// if true, single plot regularization
			if (businessService.equalsIgnoreCase("BLR1")) {
				buildingRegCertificateService = (BuildingRegCertificateService) specificRuleService
						.find("BuildingRegCertificateServiceLowRisk");
				if (buildingRegCertificateService == null)
					throw new ApplicationRuntimeException(
							"Error while generating Builiding regularization certificate for low risk.");
			} else {
				buildingRegCertificateService = (BuildingRegCertificateService) specificRuleService
						.find("BuildingRegCertificateServiceOtherThanLowRisk");
				if (buildingRegCertificateService == null)
					throw new ApplicationRuntimeException(
							"Error while generating Builiding regularization certificate for other than low risk.");

			}
			
			InputStream reportStream = buildingRegCertificateService.generateReport(buildingRegRequest.getRegularizations().get(0),
					buildingRegRequest.getRequestInfo(), isPreview);
			
			FileStoreMapper fileStoreMapper = storePermitOrder(reportStream, fileName, bpaAppTenantId);
			fileStoreMappers.add(fileStoreMapper);
			fileStoreIds = fileStoreMappers.stream()
					.sorted((fm1, fm2) -> fm2.getCreatedDate().compareTo(fm1.getCreatedDate()))
					.map(fm -> fm.getFileStoreId()).collect(Collectors.toList());
			

		} catch (Exception e) {
			e.printStackTrace();
		}

		return fileStoreIds;
	}
	
	/**
	 * 
	 * @param buildingRegRequest
	 * @param isPreview
	 * @return
	 */
	@Transactional
	public List<String> generateLandAndBuildingRegularizationCertificate(BuildingRegRequest buildingRegRequest, Boolean isPreview) {

		List<String> fileStoreIds = null;

		try {
			String LRApplicationNo = buildingRegRequest.getRegularizations().get(0).get("applicationNo").toString();
			String bpaAppTenantId = buildingRegRequest.getRegularizations().get(0).get("tenantId").toString();
			String businessService = buildingRegRequest.getRegularizations().get(0).get("businessService").toString();

			String fileName = LRApplicationNo + "";
			List<FileStoreMapper> fileStoreMappers = new ArrayList<>();
			fileName = fileName + "-V-" + fileStoreMappers.size() + ".pdf";

			LandAndBuildingRegCertificateService landAndBuildingRegCertificateService = null;
			// if true, single plot regularization
			if (businessService.equalsIgnoreCase("BLR1")) {
				landAndBuildingRegCertificateService = (LandAndBuildingRegCertificateService) applicationContext
						.getBean("landAndBuildingRegCertificateServiceImpl");
				if (landAndBuildingRegCertificateService == null)
					throw new ApplicationRuntimeException(
							"Error while generating Builiding regularization certificate for low risk.");
			} else {
				landAndBuildingRegCertificateService = (LandAndBuildingRegCertificateService) applicationContext
						.getBean("landAndBuildingRegCertificateServiceImpl");
				if (landAndBuildingRegCertificateService == null)
					throw new ApplicationRuntimeException(
							"Error while generating Builiding regularization certificate for other than low risk.");

			}
			
			InputStream reportStream = landAndBuildingRegCertificateService.generateReport(buildingRegRequest.getRegularizations().get(0),
					buildingRegRequest.getRequestInfo(), isPreview);
			
			FileStoreMapper fileStoreMapper = storePermitOrder(reportStream, fileName, bpaAppTenantId);
			fileStoreMappers.add(fileStoreMapper);
			fileStoreIds = fileStoreMappers.stream()
					.sorted((fm1, fm2) -> fm2.getCreatedDate().compareTo(fm1.getCreatedDate()))
					.map(fm -> fm.getFileStoreId()).collect(Collectors.toList());
			

		} catch (Exception e) {
			e.printStackTrace();
		}

		return fileStoreIds;
	}
	
	/**
	 * 
	 * @param occupancyCertiRequest
	 * @param isPreview
	 * @return
	 */
	@Transactional
	public List<String> generateOccupancyCertificate(OccupancyCertiRequest occupancyCertiRequest, Boolean isPreview) {

		List<String> fileStoreIds = null;

		try {
			String LRApplicationNo = occupancyCertiRequest.getBpaList().get(0).get("applicationNo").toString();
			String bpaAppTenantId = occupancyCertiRequest.getBpaList().get(0).get("tenantId").toString();
			String businessService = occupancyCertiRequest.getBpaList().get(0).get("businessService").toString();

			String fileName = LRApplicationNo + "";
			List<FileStoreMapper> fileStoreMappers = new ArrayList<>();
			fileName = fileName + "-V-" + fileStoreMappers.size() + ".pdf";

			OccupancyCertificateService occupancyCertificateService = null;

			Set<String> insideSujogBS = new HashSet<>(Arrays.asList("BPA_OC1", "BPA_OC2", "BPA_OC3", "BPA_OC4"));

			if (businessService.equalsIgnoreCase("BPA_OC_OS1") || businessService.equalsIgnoreCase("BPA_OC_OS2")
					|| businessService.equalsIgnoreCase("BPA_OC_OS3")
					|| businessService.equalsIgnoreCase("BPA_OC_OS4")) {

				occupancyCertificateService = (OccupancyCertificateService) specificRuleService
						.find("OccupancyCertificateServiceBPA_OC");
				if (occupancyCertificateService == null)
					throw new ApplicationRuntimeException("Error while generating Occupation certificate.");

			} else if (insideSujogBS.contains(businessService.toUpperCase())) {
				occupancyCertificateService = (OccupancyCertificateService) specificRuleService
						.find("OccupancyCertificateServiceInsideSujog");
				if (occupancyCertificateService == null)
					throw new ApplicationRuntimeException("Error while generating Occupation certificate.");
			} else {
				throw new ApplicationRuntimeException(
						"Invalid business service while generating occupancy certificate.");
			}
			
			InputStream reportStream = occupancyCertificateService.generateReport(occupancyCertiRequest.getBpaList().get(0),
					occupancyCertiRequest.getRequestInfo(), isPreview);
			
			FileStoreMapper fileStoreMapper = storePermitOrder(reportStream, fileName, bpaAppTenantId);
			fileStoreMappers.add(fileStoreMapper);
			fileStoreIds = fileStoreMappers.stream()
					.sorted((fm1, fm2) -> fm2.getCreatedDate().compareTo(fm1.getCreatedDate()))
					.map(fm -> fm.getFileStoreId()).collect(Collectors.toList());
			

		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if(fileStoreIds != null && !fileStoreIds.isEmpty()) {		
			LOG.info("Occupancy certificate generated successfully with filestore id: "+ fileStoreIds.get(0));
		}

		return fileStoreIds;
	}

	@Transactional
	public List<String> generateRejectionLetter(PermitOrderRequest permitOrderRequest, Boolean isPreview) {

		List<String> fileStoreIds = null;

		try {
			String bpaApplication = permitOrderRequest.getBpaList().get(0).get("applicationNo").toString();
			String fileName = bpaApplication + "-" + "Rejection";
			String bpaAppTenantId = permitOrderRequest.getBpaList().get(0).get("tenantId").toString();
			String edcrNo = "";
			String businessService = permitOrderRequest.getBpaList().get(0).get("businessService").toString();
			
			List<FileStoreMapper> fileStoreMappers = new ArrayList<>();
			fileName = fileName + "-V-" + fileStoreMappers.size() + ".pdf";

			if (permitOrderRequest.getBpaList().get(0).get("edcrNumber") != null) {
				edcrNo = permitOrderRequest.getBpaList().get(0).get("edcrNumber").toString();
			}

			if (edcrNo != null)
				edcrNo.trim();

			Plan plan = null;
			PermitOrderService permitOrderService = null;

			String pattern = "^BPA_OC(\\d+|_OS\\d+)$";
			Pattern regex = Pattern.compile(pattern);
			Matcher matcher = regex.matcher(businessService);
			if (matcher.matches()) {
				permitOrderService = (PermitOrderService) applicationContext.getBean("rejectionLetterServiceOC");
			} else {

				if (!bpaApplication.substring(0, 3).equals("BLR")) {

					try {

						EdcrApplicationInfo edcrApplicationInfo = edcrExternalService
								.loadEdcrApplicationDetails(edcrNo);
						plan = edcrApplicationInfo.getPlan();

					} catch (Exception e) {
						LOG.error("Error while feating EDCR data");
						plan = null;
					}

					permitOrderService = (PermitOrderService) applicationContext.getBean("rejectionLetterService");

				} else {
					permitOrderService = (PermitOrderService) applicationContext.getBean("rejectionLetterServiceBLR");
				}
			}

			InputStream reportStream = permitOrderService.generateReport(plan, permitOrderRequest.getBpaList().get(0),
					permitOrderRequest.getRequestInfo(), isPreview);

			FileStoreMapper fileStoreMapper = storePermitOrder(reportStream, fileName, bpaAppTenantId);
			fileStoreMappers.add(fileStoreMapper);
			fileStoreIds = fileStoreMappers.stream()
					.sorted((fm1, fm2) -> fm2.getCreatedDate().compareTo(fm1.getCreatedDate()))
					.map(fm -> fm.getFileStoreId()).collect(Collectors.toList());

		} catch (Exception e) {
			e.printStackTrace();
		}

		return fileStoreIds;
	}
	
	public Plan trimPlanDetails(Plan originalPlan) {
	    if (originalPlan == null) {
	        return null;
	    }

	    Plan trimmedPlan = new Plan();
	    
	    trimmedPlan.setApplicationDate(originalPlan.getApplicationDate());
	    trimmedPlan.setApplicationType(originalPlan.getApplicationType());
	    trimmedPlan.setPlanInformation(originalPlan.getPlanInformation());
	    
	    if (originalPlan.getPlot() != null) { 
	        Plot trimmedPlot = new Plot();
	        trimmedPlot.setArea(originalPlan.getPlot().getArea());
	        trimmedPlan.setPlot(trimmedPlot);
	    }
	    
	    if (originalPlan.getBlocks() != null) {
	        List<Block> trimmedBlocks = originalPlan.getBlocks().stream().map(block -> {
	            Block trimmedBlock = new Block();
	            if (block.getBuilding() != null) {
	                Building trimmedBuilding = new Building();
	                trimmedBuilding.setWidth(block.getBuilding().getWidth());
	                trimmedBuilding.setArea(block.getBuilding().getArea());
	                trimmedBuilding.setBuildingHeight(block.getBuilding().getBuildingHeight());
	                trimmedBuilding.setTotalFloors(block.getBuilding().getTotalFloors());
	                
	                if (block.getBuilding().getFloors() != null) {
	                    List<Floor> trimmedFloors = block.getBuilding().getFloors().stream().map(floor -> {
	                        Floor trimmedFloor = new Floor();
	                        trimmedFloor.setNumber(floor.getNumber());
	                        trimmedFloor.setOccupancies(floor.getOccupancies());
	                        return trimmedFloor;
	                    }).collect(Collectors.toList());
	                    trimmedBuilding.setFloors(trimmedFloors);
	                }
	                
	                trimmedBuilding.setOccupancies(block.getBuilding().getOccupancies());
	                trimmedBlock.setBuilding(trimmedBuilding);
	            }
	            return trimmedBlock;
	        }).collect(Collectors.toList());
	        trimmedPlan.setBlocks(trimmedBlocks);
	    }
	    
	    if (originalPlan.getVirtualBuilding() != null) {
	        VirtualBuilding trimmedVirtualBuilding = new VirtualBuilding();
	        trimmedVirtualBuilding.setBuildingHeight(originalPlan.getVirtualBuilding().getBuildingHeight());
	        trimmedVirtualBuilding.setOccupancyTypes(originalPlan.getVirtualBuilding().getOccupancyTypes());
	        trimmedVirtualBuilding.setTotalBuitUpArea(originalPlan.getVirtualBuilding().getTotalBuitUpArea());
	        trimmedVirtualBuilding.setMostRestrictiveFarHelper(originalPlan.getVirtualBuilding().getMostRestrictiveFarHelper());
	        trimmedPlan.setVirtualBuilding(trimmedVirtualBuilding);
	    }

	    return trimmedPlan;
	}
	

	
	public PlanResponseDTO trimPlanDetailsV2(Plan originalPlan) {
	    if (originalPlan == null) {
	        return null;
	    }

	    PlanResponseDTO trimmedPlan = new PlanResponseDTO();

	    // Copy required fields
	    trimmedPlan.setApplicationDate(originalPlan.getApplicationDate());
	    trimmedPlan.setApplicationType(originalPlan.getApplicationType());

	    // Map PlanInformation to PlanInformationDTO
	    trimmedPlan.setPlanInformation(originalPlan.getPlanInformation());

	    // Map Plot to PlotDTO
	    if (originalPlan.getPlot() != null) {
	        PlotDTO plotDTO = new PlotDTO(originalPlan.getPlot().getArea());
	        trimmedPlan.setPlot(plotDTO);
	    }

	 // Map Blocks to BlockDTOs
	    if (originalPlan.getBlocks() != null) {
	        List<BlockDTO> trimmedBlocks = originalPlan.getBlocks().stream().map(block -> {
	            BlockDTO blockDTO = new BlockDTO();

	            if (block.getBuilding() != null) {
	                BuildingDTO buildingDTO = new BuildingDTO(
	                        block.getBuilding().getWidth(),
	                        block.getBuilding().getArea(),
	                        block.getBuilding().getBuildingHeight(),
	                        block.getBuilding().getTotalFloors(),
	                        null, // Floors will be set separately
	                        null  // Occupancies will be set separately
	                );

	                // Map Floors to FloorDTOs
	                if (block.getBuilding().getFloors() != null) {
	                    List<FloorDTO> floorDTOs = block.getBuilding().getFloors().stream().map(floor -> {
	                        List<OccupancyDTO> occupancyDTOs = floor.getOccupancies().stream().map(occupancy -> {
	                            OccupancyHelperDetailDTO type = occupancy.getTypeHelper().getType() != null
	                                    ? new OccupancyHelperDetailDTO(
	                                            occupancy.getTypeHelper().getType().getColor(),
	                                            occupancy.getTypeHelper().getType().getCode(),
	                                            occupancy.getTypeHelper().getType().getName())
	                                    : null;

	                            OccupancyHelperDetailDTO subtype = occupancy.getTypeHelper().getSubtype() != null
	                                    ? new OccupancyHelperDetailDTO(
	                                            occupancy.getTypeHelper().getSubtype().getColor(),
	                                            occupancy.getTypeHelper().getSubtype().getCode(),
	                                            occupancy.getTypeHelper().getSubtype().getName())
	                                    : null;

	                            OccupancyHelperDetailDTO usage = occupancy.getTypeHelper().getUsage() != null
	                                    ? new OccupancyHelperDetailDTO(
	                                            occupancy.getTypeHelper().getUsage().getColor(),
	                                            occupancy.getTypeHelper().getUsage().getCode(),
	                                            occupancy.getTypeHelper().getUsage().getName())
	                                    : null;

	                            return new OccupancyDTO(
	                                    new OccupancyTypeHelperDTO(type, subtype, usage)	                                    
	                            );
	                        }).collect(Collectors.toList());
	                        return new FloorDTO(occupancyDTOs);
	                    }).collect(Collectors.toList());
	                    buildingDTO.setFloors(floorDTOs);
	                }

	                // Map Occupancies to OccupancyTypeHelperDTO
	                if (block.getBuilding().getOccupancies() != null) {
	                    List<OccupancyTypeHelperDTO> occupancyDTOs = block.getBuilding().getOccupancies().stream().map(occupancy -> {
	                        OccupancyHelperDetailDTO type = occupancy.getTypeHelper().getType() != null
	                                ? new OccupancyHelperDetailDTO(
	                                        occupancy.getTypeHelper().getType().getColor(),
	                                        occupancy.getTypeHelper().getType().getCode(),
	                                        occupancy.getTypeHelper().getType().getName())
	                                : null;

	                        OccupancyHelperDetailDTO subtype = occupancy.getTypeHelper().getSubtype() != null
	                                ? new OccupancyHelperDetailDTO(
	                                        occupancy.getTypeHelper().getSubtype().getColor(),
	                                        occupancy.getTypeHelper().getSubtype().getCode(),
	                                        occupancy.getTypeHelper().getSubtype().getName())
	                                : null;

	                        OccupancyHelperDetailDTO usage = occupancy.getTypeHelper().getUsage() != null
	                                ? new OccupancyHelperDetailDTO(
	                                        occupancy.getTypeHelper().getUsage().getColor(),
	                                        occupancy.getTypeHelper().getUsage().getCode(),
	                                        occupancy.getTypeHelper().getUsage().getName())
	                                : null;

	                        return new OccupancyTypeHelperDTO(type, subtype, usage);
	                    }).collect(Collectors.toList());
	                    buildingDTO.setOccupancies(occupancyDTOs);
	                }

	                blockDTO.setBuilding(buildingDTO);
	            }
	            return blockDTO;
	        }).collect(Collectors.toList());
	        trimmedPlan.setBlocks(trimmedBlocks);
	    }




	    // Map VirtualBuilding to VirtualBuildingDTO
	    if (originalPlan.getVirtualBuilding() != null) {
	        VirtualBuilding originalVirtualBuilding = originalPlan.getVirtualBuilding();

	        // Map OccupancyTypeDTOs (instead of OccupancyTypeHelperDTOs)
	        Set<OccupancyTypeDTO> occupancyTypeDTOs = originalVirtualBuilding.getOccupancyTypes().stream().map(occupancyTypeHelper -> {
	            OccupancyTypeDTO occupancyTypeDTO = new OccupancyTypeDTO(
	                    occupancyTypeHelper.getType() != null
	                            ? new OccupancyHelperDetailDTO(
	                                    occupancyTypeHelper.getType().getColor(),
	                                    occupancyTypeHelper.getType().getCode(),
	                                    occupancyTypeHelper.getType().getName())
	                            : null,
	                    occupancyTypeHelper.getSubtype() != null
	                            ? new OccupancyHelperDetailDTO(
	                                    occupancyTypeHelper.getSubtype().getColor(),
	                                    occupancyTypeHelper.getSubtype().getCode(),
	                                    occupancyTypeHelper.getSubtype().getName())
	                            : null,
	                    occupancyTypeHelper.getUsage() != null
	                            ? new OccupancyHelperDetailDTO(
	                                    occupancyTypeHelper.getUsage().getColor(),
	                                    occupancyTypeHelper.getUsage().getCode(),
	                                    occupancyTypeHelper.getUsage().getName())
	                            : null,
	                    occupancyTypeHelper.getConvertedType() != null
	                            ? new OccupancyHelperDetailDTO(
	                                    occupancyTypeHelper.getConvertedType().getColor(),
	                                    occupancyTypeHelper.getConvertedType().getCode(),
	                                    occupancyTypeHelper.getConvertedType().getName())
	                            : null,
	                    occupancyTypeHelper.getConvertedSubtype() != null
	                            ? new OccupancyHelperDetailDTO(
	                                    occupancyTypeHelper.getConvertedSubtype().getColor(),
	                                    occupancyTypeHelper.getConvertedSubtype().getCode(),
	                                    occupancyTypeHelper.getConvertedSubtype().getName())
	                            : null,
	                    occupancyTypeHelper.getConvertedUsage() != null
	                            ? new OccupancyHelperDetailDTO(
	                                    occupancyTypeHelper.getConvertedUsage().getColor(),
	                                    occupancyTypeHelper.getConvertedUsage().getCode(),
	                                    occupancyTypeHelper.getConvertedUsage().getName())
	                            : null
	                            
	            );
	            return occupancyTypeDTO;
	        }).collect(Collectors.toSet());

	        // Map MostRestrictiveFarHelper
	        OccupancyTypeHelperDTO mostRestrictiveFarHelperDTO = null;
	        if (originalVirtualBuilding.getMostRestrictiveFarHelper() != null) {
	            OccupancyHelperDetailDTO type = originalVirtualBuilding.getMostRestrictiveFarHelper().getType() != null
	                    ? new OccupancyHelperDetailDTO(
	                            originalVirtualBuilding.getMostRestrictiveFarHelper().getType().getColor(),
	                            originalVirtualBuilding.getMostRestrictiveFarHelper().getType().getCode(),
	                            originalVirtualBuilding.getMostRestrictiveFarHelper().getType().getName())
	                    : null;

	            OccupancyHelperDetailDTO subtype = originalVirtualBuilding.getMostRestrictiveFarHelper().getSubtype() != null
	                    ? new OccupancyHelperDetailDTO(
	                            originalVirtualBuilding.getMostRestrictiveFarHelper().getSubtype().getColor(),
	                            originalVirtualBuilding.getMostRestrictiveFarHelper().getSubtype().getCode(),
	                            originalVirtualBuilding.getMostRestrictiveFarHelper().getSubtype().getName())
	                    : null;

	            OccupancyHelperDetailDTO usage = originalVirtualBuilding.getMostRestrictiveFarHelper().getUsage() != null
	                    ? new OccupancyHelperDetailDTO(
	                            originalVirtualBuilding.getMostRestrictiveFarHelper().getUsage().getColor(),
	                            originalVirtualBuilding.getMostRestrictiveFarHelper().getUsage().getCode(),
	                            originalVirtualBuilding.getMostRestrictiveFarHelper().getUsage().getName())
	                    : null;

	            mostRestrictiveFarHelperDTO = new OccupancyTypeHelperDTO(type, subtype, usage);
	        }

	        // Create VirtualBuildingDTO
	        VirtualBuildingDTO virtualBuildingDTO = new VirtualBuildingDTO(
	                originalVirtualBuilding.getBuildingHeight(),
	                occupancyTypeDTOs,  // Use Set<OccupancyTypeDTO>
	                originalVirtualBuilding.getTotalBuitUpArea(),
	                mostRestrictiveFarHelperDTO
	        );

	        trimmedPlan.setVirtualBuilding(virtualBuildingDTO);
	    }



	    return trimmedPlan;
	}
	
	public void mapEdcrToEdcrDetailV2(EdcrDetail edcr, EdcrDetailV2 edcrDetailV2) {
	    edcrDetailV2.setDxfFile(edcr.getDxfFile());
	    edcrDetailV2.setUpdatedDxfFile(edcr.getUpdatedDxfFile());
	    edcrDetailV2.setPlanReport(edcr.getPlanReport());
	    edcrDetailV2.setShortenedPlanReport(edcr.getShortenedPlanReport());
	    edcrDetailV2.setTransactionNumber(edcr.getTransactionNumber());
	    edcrDetailV2.setApplicationDate(edcr.getApplicationDate());
	    edcrDetailV2.setApplicationNumber(edcr.getApplicationNumber());
	    edcrDetailV2.setStatus(edcr.getStatus());
	    edcrDetailV2.setEdcrNumber(edcr.getEdcrNumber());
	    edcrDetailV2.setTenantId(edcr.getTenantId());
	    edcrDetailV2.setErrors(edcr.getErrors());
	    edcrDetailV2.setPlanPdfs(edcr.getPlanPdfs());
	    edcrDetailV2.setPlanDetail(trimPlanDetailsV2(edcr.getPlanDetail()));
	    edcrDetailV2.setPermitNumber(edcr.getPermitNumber());
	    edcrDetailV2.setPermitDate(edcr.getPermitDate());
	    edcrDetailV2.setAppliactionType(edcr.getAppliactionType());
	    edcrDetailV2.setApplicationSubType(edcr.getApplicationSubType());
	    edcrDetailV2.setComparisonEdcrNumber(edcr.getComparisonEdcrNumber());
	    edcrDetailV2.setAlterationSubService(edcr.getAlterationSubService());
	    edcrDetailV2.setGisData(edcr.getGisData());
	    edcrDetailV2.setDxfToPdfBase(edcr.getDxfToPdfBase());
	    edcrDetailV2.setDxfToPdfBasePlusObpas(edcr.getDxfToPdfBasePlusObpas());
	}
	

}