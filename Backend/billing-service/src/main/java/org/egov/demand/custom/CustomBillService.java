package org.egov.demand.custom;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.stream.Collectors;

import org.egov.common.contract.request.RequestInfo;
import org.egov.demand.model.BillAccountDetailV2;
import org.egov.demand.model.BillDetailV2;
import org.egov.demand.model.GenerateBillCriteria;
import org.egov.demand.service.BillServicev2;
import org.egov.demand.web.contract.BillResponseV2;
import org.egov.demand.web.contract.RequestInfoWrapper;
import org.egov.demand.web.contract.factory.ResponseFactory;
import org.egov.mdms.model.MdmsCriteriaReq;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.egov.demand.util.Util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;

import io.micrometer.core.instrument.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CustomBillService {

	@Autowired
	private BillServicev2 billService;
	
	@Autowired
	private ResponseFactory responseFactory;

	@Autowired
	private CustomBillValidator billValidator;
	
	@Autowired
	private Util util;
	
	@Autowired
	private ObjectMapper mapper;
	
	public static final String MODULE_NAME = "ws-services-calculation";
	public static final String REBATE_MDMS_MASTER_NAME = "Rebate";
	public static final String PENALTY_MDMS_MASTER_NAME = "Penalty";
	public static final List<String> MDMS_MASTER_NAMES = Collections
			.unmodifiableList(Arrays.asList(REBATE_MDMS_MASTER_NAME,PENALTY_MDMS_MASTER_NAME));
	public static final List<String> ALLOWED_BUSINESS_SERVICE_FOR_REBATE_PENALTY_DATA = Collections
			.unmodifiableList(Arrays.asList("WS.ONE_TIME_FEE","WS"));
	public static final String MDMS_CODE_FILTER = "";
	public static final String LATEST_FY = "latest_FY";
	public static final String MDMS_WS_REBATE = "$.MdmsRes.ws-services-calculation.Rebate";
	public static final String MDMS_WS_PENALTY = "$.MdmsRes.ws-services-calculation.Penalty";
	
	DecimalFormat decimalFormat = new DecimalFormat("#.##");

	public CustomBillResponse fetchBill(BillCriteria criteria, RequestInfoWrapper requestInfoWrapper) {
		BigDecimal totalAmount = BigDecimal.ZERO;
		RequestInfo requestInfo = requestInfoWrapper.getRequestInfo();
		CustomBill response = new CustomBill();
		billValidator.validateBillGenRequest(criteria, requestInfo);
		GenerateBillCriteria billCriteria = new GenerateBillCriteria();
		enrichBillGenRequest(criteria, billCriteria);
		BillResponseV2 billResponseV2 = billService.fetchBill(billCriteria, requestInfoWrapper);
		if (billResponseV2 != null) {
			totalAmount = billResponseV2.getBill().get(0).getTotalAmount();
			enrichBillResponse(billResponseV2, response);
			List<BillDetailV2> billDetails = billResponseV2.getBill().get(0).getBillDetails();

			// Sort the billDetails list in descending order based on fromPeriod and toPeriod
            billDetails.sort(Comparator.comparingLong(
					detail -> Math.max(((BillDetailV2) detail).getFromPeriod(), ((BillDetailV2) detail).getToPeriod()))
					.reversed());

			response.setTotalAmount(totalAmount);
			if (!CollectionUtils.isEmpty(billDetails)) {
				response.setArrear(totalAmount.subtract(billDetails.get(0).getAmount()));
				response.setCurrentAmount(billDetails.get(0).getAmount());
				response.setBillDetail(billDetails.get(0));
			}

		}
		
		//It is only for WS 
		if(ALLOWED_BUSINESS_SERVICE_FOR_REBATE_PENALTY_DATA.contains(criteria.getBusinessService())) {
			try {
				List<Map<String, Object>> mdmsDataLatestConfig =  fetchDataFromMDMS(response);
				Map<String, Object> LatestRebateConfigMap = mdmsDataLatestConfig.get(0);
				Integer noOfDaysFromMDMS = Integer.valueOf((String) LatestRebateConfigMap.get("endingDay"));
				
				List<Map<String, Object>> mdmsRebateAndPenaltyData =  fetchRebateDataFromMDMS(response);
				if(!mdmsRebateAndPenaltyData.isEmpty()) {
					processMDMSData(mdmsRebateAndPenaltyData,response,noOfDaysFromMDMS);
				}
			}catch(Exception ex) {
				log.info("Error while setting Rebate And Penalty Data : " + ex.toString());
			}
			
			setAdvanceAdjustedAmount(response);
			
		}

		setPaymentLink(response,criteria);

		
		return CustomBillResponse.builder().resposneInfo(responseFactory.getResponseInfo(requestInfo, HttpStatus.OK))
				.customBill(response).build();

		// return BillResponse.builder().customBillResponse(response).build();
	}

	private void setPaymentLink(CustomBill response, BillCriteria criteria) {

		if(StringUtils.isNotEmpty(criteria.getWhatsAppNumber())) {
			String shortenedURL= util.callUrlShortener(criteria.getTenantId(), criteria.getConsumerCode(), criteria.getBusinessService(), criteria.getWhatsAppNumber());
			response.setPaymentLink(shortenedURL);
		}
		
		if(StringUtils.isNotEmpty(criteria.getWhatsAppNumberv2())) {
			String shortenedURL= util.callUrlShortenerv2(criteria.getTenantId(), criteria.getConsumerCode(), criteria.getBusinessService(), criteria.getWhatsAppNumberv2());
			response.setPaymentLink(shortenedURL);
		}
		
	}


	private List<Map<String, Object>> fetchDataFromMDMS(CustomBill response) {
		
		MdmsCriteriaReq mdmsReq = util.prepareMdMsRequest(response.getTenantId(), MODULE_NAME,Collections
				.unmodifiableList(Arrays.asList(REBATE_MDMS_MASTER_NAME)), MDMS_CODE_FILTER,
				new RequestInfo());

		
		DocumentContext mdmsData;
		try {
			mdmsData = util.getAttributeValues(mdmsReq);
			List<Map<String, Object>> mdmsDataRebate = mapper.convertValue(mdmsData.read(MDMS_WS_REBATE) , new TypeReference<List<Map<String, Object>>>(){});
			return Arrays.asList(mdmsDataRebate.get(0));
		}catch (Exception e) {
			log.info(e.toString());
			return new ArrayList<Map<String, Object>>();
		}
		
	}


	private void setAdvanceAdjustedAmount(CustomBill response) {
		Double roundOff = 0d;
		Double taxCharge = 0d;
		Double waterCess = 0d;
		Double timeInterest = 0d;
		Double timePenalty = 0d;
		Double timeRebate = 0d;
		Double specialRebate = 0d;

		for (BillAccountDetailV2 tax : response.getBillDetail().getBillAccountDetails()) {
			String taxHeadCode = tax.getTaxHeadCode();
			switch (taxHeadCode) {
			case "WS_Round_Off":case "SW_ROUNDOFF":
				roundOff =roundOff.doubleValue() + tax.getAmount().doubleValue();
				break;
			case "WS_CHARGE":case "SW_CHARGE":
				taxCharge = taxCharge.doubleValue()+ tax.getAmount().doubleValue();
				break;
			case "WS_WATER_CESS":case "SW_SEWERAGE_CESS":
				waterCess = waterCess.doubleValue()+ tax.getAmount().doubleValue();
				break;
			case "WS_TIME_INTEREST":case "SW_TIME_INTEREST":
				timeInterest = timeInterest.doubleValue()+ tax.getAmount().doubleValue();
				break;
			case "WS_TIME_PENALTY":case "SW_TIME_PENALTY":
				timePenalty = timePenalty.doubleValue()+ tax.getAmount().doubleValue();
				break;
			case "WS_TIME_REBATE":case "SW_TIME_REBATE":
				timeRebate = timeRebate.doubleValue()+ tax.getAmount().doubleValue();
				break;
			case "WS_SPECIAL_REBATE":case "SW_SPECIAL_REBATE":
				specialRebate = specialRebate.doubleValue()+ tax.getAmount().doubleValue();
				break;

			}

		}
		log.info("roundOff Amount : " + roundOff.toString());
		log.info("taxCharge Amount : " + taxCharge.toString());
		log.info("waterCess : " + waterCess.toString());
		log.info("timeInterest : " + timeInterest.toString());
		log.info("timePenalty : " + timePenalty.toString());
		log.info("timeRebate : " + timeRebate.toString());
		log.info("specialRebate : " + specialRebate.toString());
		Double demandTotalAmount = roundOff.doubleValue() + taxCharge+ waterCess+timeInterest+timePenalty+timeRebate+specialRebate;

		Double billTotalAmount = response.getTotalAmount().doubleValue();
		Double advanceAdjusted;
		
		log.info("Bill Total Amount : " + billTotalAmount.toString());
		log.info("Demand Total Amount : " + demandTotalAmount.toString());

		if (billTotalAmount >= 0) {
			if (billTotalAmount < demandTotalAmount) {
				advanceAdjusted = (billTotalAmount - demandTotalAmount);
			} else {
				advanceAdjusted = 0d;
			}
		} else {
			advanceAdjusted = -demandTotalAmount;
		}
		
		response.setAdvanceAdjusted(new BigDecimal( decimalFormat.format(advanceAdjusted)).setScale(2));
	}


	public void enrichBillGenRequest(BillCriteria criteria, GenerateBillCriteria billCriteria) {
		billCriteria.setTenantId(criteria.getTenantId());
		billCriteria.setBusinessService(criteria.getBusinessService());
		billCriteria.setConsumerCode(
				Arrays.stream(criteria.getConsumerCode().split("\\,")).collect(Collectors.toCollection(HashSet::new)));

	}

	public CustomBill enrichBillResponse(BillResponseV2 billResponseV2, CustomBill response) {

		response.setId(billResponseV2.getBill().get(0).getId());
		response.setAdditionalDetails(billResponseV2.getBill().get(0).getAdditionalDetails());
		response.setAuditDetails(billResponseV2.getBill().get(0).getAuditDetails());
		response.setBillDate(billResponseV2.getBill().get(0).getBillDate());
		response.setBillNumber(billResponseV2.getBill().get(0).getBillNumber());
		response.setBusinessService(billResponseV2.getBill().get(0).getBusinessService());
		response.setConsumerCode(billResponseV2.getBill().get(0).getConsumerCode());
		response.setFileStoreId(billResponseV2.getBill().get(0).getFileStoreId());
		response.setMobileNumber(billResponseV2.getBill().get(0).getMobileNumber());
		response.setPayerName(billResponseV2.getBill().get(0).getPayerName());
		response.setPayerAddress(billResponseV2.getBill().get(0).getPayerAddress());
		response.setPayerEmail(billResponseV2.getBill().get(0).getPayerEmail());
		response.setPayerAddress(billResponseV2.getBill().get(0).getPayerAddress());
		response.setStatus(String.valueOf(billResponseV2.getBill().get(0).getStatus()));
		response.setTenantId(billResponseV2.getBill().get(0).getTenantId());

		return response;

	}
	
	
	public List<Map<String, Object>> fetchRebateDataFromMDMS(CustomBill response) {

		Long billToDate = response.getBillDetail().getToPeriod();
		
		MdmsCriteriaReq mdmsReq = util.prepareMdMsRequest(response.getTenantId(), MODULE_NAME, MDMS_MASTER_NAMES, MDMS_CODE_FILTER,
				new RequestInfo());
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(billToDate);
		
		String latest_FY = getFinancialYear(calendar);
		log.info("financialYear : " + latest_FY);
		
		String filterCode = "[?(@.fromFY=='latest_FY')]";
		String fyReplaced = filterCode.replaceAll(LATEST_FY, latest_FY);
		mdmsReq.getMdmsCriteria().getModuleDetails().get(0).getMasterDetails().forEach(name ->{
			name.setFilter(fyReplaced);
		});
		
		DocumentContext mdmsData;
		try {
			mdmsData = util.getAttributeValues(mdmsReq);
			List<Map<String, Object>> mdmsDataRebate = mapper.convertValue(mdmsData.read(MDMS_WS_REBATE) , new TypeReference<List<Map<String, Object>>>(){});
			List<Map<String, Object>> mdmsDataPenalty = mapper.convertValue(mdmsData.read(MDMS_WS_PENALTY) , new TypeReference<List<Map<String, Object>>>(){});
			mdmsDataRebate.addAll(mdmsDataPenalty);
			return mdmsDataRebate;//It has both Rebate and Penalty
		}catch (Exception e) {
			log.info(e.toString());
			return new ArrayList<Map<String, Object>>();
		}
		
		
	}


	private String getFinancialYear(Calendar calendar) {
		int year = calendar.get(calendar.YEAR);
		int month = calendar.get(calendar.MONTH);
		
		String financialYear;
		if(month >=Calendar.APRIL) {
			financialYear = year + "-" + (year+1L)%100L;
		}else {
			financialYear = (year-1L) + "-" + (year % 100L);
		}
		return financialYear;
	}
	
	private void processMDMSData( List<Map<String, Object>> mdmsRebateAndPenaltyData,CustomBill response , Integer daysFromMDMS) {
		Map<String, Object> combinedMap = mdmsRebateAndPenaltyData.get(0);
		Integer noOfDaysFromMDMS = daysFromMDMS;
		log.info("noOfDaysFromMDMS : " + String.valueOf(noOfDaysFromMDMS));
		long epochTimeBillingPeriodMonth = response.getBillDetail().getToPeriod();
		log.info("Billing Period Last Date : " + String.valueOf(epochTimeBillingPeriodMonth));
		
		LocalDate date = LocalDate.ofEpochDay(epochTimeBillingPeriodMonth/(24*60*60*1000));
		YearMonth nextMonth = YearMonth.from(date).plusMonths(1);
		LocalDate lastDateOfNextMonth = nextMonth.atEndOfMonth();
		ZonedDateTime zonedDateTime = lastDateOfNextMonth.atTime(23,59,59).atZone(ZoneOffset.ofHours(0));
		long nextMonthLastDayEpochTime = zonedDateTime.toInstant().toEpochMilli();
		
		log.info("nextMonth Last Date: " + String.valueOf(nextMonthLastDayEpochTime));
		
		Calendar calendarNextMonth = Calendar.getInstance();
		calendarNextMonth.setTimeInMillis(nextMonthLastDayEpochTime);
		TimeZone gmtTimezone = TimeZone.getTimeZone("GMT");
		calendarNextMonth.setTimeZone(gmtTimezone);
		int daysInNextMonth = calendarNextMonth.getActualMaximum(calendarNextMonth.DAY_OF_MONTH);
		
		log.info("daysInNextMonth : " + String.valueOf(daysInNextMonth));
		
		//Default Value
		Long DueDate = nextMonthLastDayEpochTime;
		if(daysInNextMonth>noOfDaysFromMDMS) {
			LocalDate	lastMDMSDateOfNextMonth = nextMonth.atDay(noOfDaysFromMDMS);
			ZonedDateTime	zonedMDMSDateTime = lastMDMSDateOfNextMonth.atTime(23,59,59).atZone(ZoneOffset.ofHours(0));
			long nextMonthLastMDMSDayEpochTime = zonedMDMSDateTime.toInstant().toEpochMilli();
			 DueDate = nextMonthLastMDMSDayEpochTime;
			 log.info("DueDate : " + String.valueOf(DueDate));
			log.info("nextMonthLastMDMSDayEpochTime : " + String.valueOf(nextMonthLastMDMSDayEpochTime));
		}
		
		BigDecimal arrear = response.getArrear();
		BigDecimal currentAmount = response.getCurrentAmount();
		
		//REBATE
		BigDecimal currentFinalAmountWithRebate = getCurrentFinalAmountWithRebate(mdmsRebateAndPenaltyData,
				response, currentAmount);
		BigDecimal totalAmountWithRebate = new BigDecimal(arrear.toString()).add(currentFinalAmountWithRebate);
		
		//PENALTY

		BigDecimal currentFinalAmountWithPenalty = getCurrentFinalAmountWithPenalty(mdmsRebateAndPenaltyData,
				response, currentAmount);
		BigDecimal totalAmountWithPenalty = new BigDecimal(arrear.toString()).add(currentFinalAmountWithPenalty);
		
		//due date set to one month after the bill date
		DueDate = Instant.ofEpochMilli(response.getBillDate()).plus(30, ChronoUnit.DAYS).toEpochMilli();  
		
		response.setBillAfterDueDate(totalAmountWithPenalty);
		response.setBillBeforeDueDate(totalAmountWithRebate);
		response.setBillDueDate(DueDate);
	}


	private BigDecimal getCurrentFinalAmountWithPenalty(List<Map<String, Object>> mdmsRebateAndPenaltyData,//WITH PENALTY
			CustomBill response, BigDecimal currentAmount) {
		

		Map<String, Object> penaltyData = mdmsRebateAndPenaltyData.get(1);
		Long penaltyFromMDMS = Optional.ofNullable(penaltyData.get("rate")).map(value -> Long.parseLong(value.toString())).orElse(0L);
		log.info("penalty From MDMS : " + String.valueOf(penaltyFromMDMS));
		
		BigDecimal timeRebate = response.getBillDetail().getBillAccountDetails().stream().filter(obj -> obj.getTaxHeadCode().equals("WS_TIME_REBATE")).map(BillAccountDetailV2::getAmount).reduce(BigDecimal.ZERO,BigDecimal::add);
		BigDecimal timePenalty = response.getBillDetail().getBillAccountDetails().stream().filter(obj -> obj.getTaxHeadCode().equals("WS_TIME_PENALTY")).map(BillAccountDetailV2::getAmount).reduce(BigDecimal.ZERO,BigDecimal::add);
		BigDecimal currentAmountModifiedWithPenalty = new BigDecimal(currentAmount.toString()).subtract(timePenalty).subtract(timeRebate);
		BigDecimal penalty = new BigDecimal(penaltyFromMDMS.toString()).divide(new BigDecimal(String.valueOf(100)));//Change to Penalty
		BigDecimal penaltyInDecimal = new BigDecimal(String.valueOf(1)).add(penalty);
		BigDecimal currentFinalAmountAfterPenalty = new BigDecimal(currentAmountModifiedWithPenalty.toString()).multiply(penaltyInDecimal);
		return currentFinalAmountAfterPenalty.setScale(2,BigDecimal.ROUND_HALF_EVEN);
		
	}


	private BigDecimal getCurrentFinalAmountWithRebate(List<Map<String, Object>> mdmsRebateAndPenaltyData,
			CustomBill response, BigDecimal currentAmount) {
		
		Map<String, Object> combinedMap = mdmsRebateAndPenaltyData.get(0);
		Long rebateFromMDMS = Optional.ofNullable(combinedMap.get("rate")).map(value -> Long.parseLong(value.toString())).orElse(0L);
		log.info("rebate From MDMS : " + String.valueOf(rebateFromMDMS));

		BigDecimal timePenalty = response.getBillDetail().getBillAccountDetails().stream().filter(obj -> obj.getTaxHeadCode().equals("WS_TIME_PENALTY")).map(BillAccountDetailV2::getAmount).reduce(BigDecimal.ZERO,BigDecimal::add);
		BigDecimal timeRebate = response.getBillDetail().getBillAccountDetails().stream().filter(obj -> obj.getTaxHeadCode().equals("WS_TIME_REBATE")).map(BillAccountDetailV2::getAmount).reduce(BigDecimal.ZERO,BigDecimal::add);
		BigDecimal currentAmountModifiedWithRebate = new BigDecimal(currentAmount.toString()).subtract(timeRebate).subtract(timePenalty);
		BigDecimal rebate = new BigDecimal(rebateFromMDMS.toString()).divide(new BigDecimal(String.valueOf(100)));
		BigDecimal rebateInDecimal = new BigDecimal(String.valueOf(1)).subtract(rebate);
		BigDecimal currentFinalAmountAfterRebate = new BigDecimal(currentAmountModifiedWithRebate.toString()).multiply(rebateInDecimal);
		return currentFinalAmountAfterRebate.setScale(2,BigDecimal.ROUND_HALF_EVEN);
	}

}
