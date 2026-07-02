package org.egov.collection.service;

import static java.util.Objects.isNull;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.egov.collection.config.ApplicationProperties;
import org.egov.collection.model.AuditDetails;
import org.egov.collection.model.Payment;
import org.egov.collection.model.PaymentRequest;
import org.egov.collection.model.PaymentSearchCriteria;
import org.egov.collection.model.enums.PaymentStatusEnum;
import org.egov.collection.producer.CollectionProducer;
import org.egov.collection.repository.PaymentRepository;
import org.egov.collection.util.PaymentEnricher;
import org.egov.collection.util.PaymentValidator;
import org.egov.collection.util.Utils;
import org.egov.collection.web.contract.Bill;
import org.egov.collection.web.contract.BillAccountDetail;
import org.egov.collection.web.contract.BillDetail;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j
public class PaymentService {

    private ApportionerService apportionerService;

    private PaymentEnricher paymentEnricher;

    private ApplicationProperties applicationProperties;

    private UserService userService;

    private PaymentValidator paymentValidator;

    private PaymentRepository paymentRepository;

    private CollectionProducer producer;
    
    @Autowired
    private Utils utils;
    
    public static String advanceTaxCode = "PT_ADVANCE_CARRYFORWARD";
    
    public static String paymentSearchBusinessService = "PT";

    @Autowired
    private WhatsAppNotificationService whatsAppNotificationService;


    @Autowired
    public PaymentService(ApportionerService apportionerService, PaymentEnricher paymentEnricher, ApplicationProperties applicationProperties,
                          UserService userService, PaymentValidator paymentValidator, PaymentRepository paymentRepository, CollectionProducer producer) {
        this.apportionerService = apportionerService;
        this.paymentEnricher = paymentEnricher;
        this.applicationProperties = applicationProperties;
        this.userService = userService;
        this.paymentValidator = paymentValidator;
        this.paymentRepository = paymentRepository;
        this.producer = producer;
    }



    /**
     * Fetch all receipts matching the given criteria, enrich receipts with instruments
     *
     * @param requestInfo           Request info of the search
     * @param paymentSearchCriteria Criteria against which search has to be performed
     * @return List of matching receipts
     */
    public List<Payment> getPayments(RequestInfo requestInfo, PaymentSearchCriteria paymentSearchCriteria, String moduleName) {
    	
        paymentValidator.validateAndUpdateSearchRequestFromConfig(paymentSearchCriteria, requestInfo, moduleName);
        if (applicationProperties.isPaymentsSearchPaginationEnabled()) {
            paymentSearchCriteria.setOffset(isNull(paymentSearchCriteria.getOffset()) ? 0 : paymentSearchCriteria.getOffset());
            paymentSearchCriteria.setLimit(isNull(paymentSearchCriteria.getLimit()) ? applicationProperties.getReceiptsSearchDefaultLimit() :
                    paymentSearchCriteria.getLimit());
        } else {
            paymentSearchCriteria.setOffset(0);
            paymentSearchCriteria.setLimit(applicationProperties.getReceiptsSearchDefaultLimit());
        }
        /*if(requestInfo.getUserInfo().getType().equals("CITIZEN")) {
            List<String> payerIds = new ArrayList<>();
            payerIds.add(requestInfo.getUserInfo().getUuid());
            paymentSearchCriteria.setPayerIds(payerIds);
        }*/
        List<Payment> payments = paymentRepository.fetchPayments(paymentSearchCriteria);
        
        
        setPTDemandAndCollectionDetails(payments);

        return payments;
    }



	private void setPTDemandAndCollectionDetails(List<Payment> payments) {
		if(!CollectionUtils.isEmpty(payments)) {
        	try {
        		setDemandAndCollectionDetails(payments);
        	}catch (Exception e) {
        		log.info("Error while setting Demand And Collection Amount : " + e.toString());
        	}
        }
	}



	private void setDemandAndCollectionDetails(List<Payment> payments) {
		for (Payment payment : payments) {
            BigDecimal currentDemand = BigDecimal.ZERO;
            BigDecimal currentCollectionWithoutAdvance = BigDecimal.ZERO;
            BigDecimal arrearCollectionWithoutAdvance = BigDecimal.ZERO;
            if (payment.getPaymentDetails().get(0).getBusinessService().equals(paymentSearchBusinessService)) {

                Long startingdate = Utils.getStartingDateOfCurrentFy();

                BigDecimal advanceAmount = BigDecimal.ZERO;
                List<BillDetail> billDetails = payment.getPaymentDetails().get(0).getBill().getBillDetails();

                for (BillDetail billDetail : billDetails) {

                    if (billDetail.getToPeriod() > startingdate) {

                        currentDemand = billDetail.getAmount();
                        payment.setCurrentDue(currentDemand);
                        currentCollectionWithoutAdvance = extractSumFromBillAccountDetails(billDetail);

                        payment.setCurrentAmountPaid(currentCollectionWithoutAdvance);
                    } else {
                        if (payment.getArrearDue() == null && payment.getArrearAmountPaid() == null) {
                            payment.setArrearDue(BigDecimal.ZERO);
                            payment.setArrearAmountPaid(BigDecimal.ZERO);
                        }
                        payment.setArrearDue(payment.getArrearDue().add(billDetail.getAmount()));
                        arrearCollectionWithoutAdvance = extractSumFromBillAccountDetails(billDetail);
                        payment.setArrearAmountPaid(payment.getArrearAmountPaid().add(arrearCollectionWithoutAdvance));
                    }
                }

                if (payment.getArrearDue() == null && payment.getArrearAmountPaid() == null) {
                    payment.setArrearDue(BigDecimal.ZERO);
                    payment.setArrearAmountPaid(BigDecimal.ZERO);
                }
                if (payment.getTotalAmountPaid().compareTo(payment.getTotalDue()) == 1) {
                    advanceAmount = payment.getTotalAmountPaid().subtract(payment.getTotalDue());
                    payment.setAdvanceAmountPaid(advanceAmount);
                }

            }
            if (currentDemand == null) {
                currentDemand = BigDecimal.ZERO;
            }
            payment.setCurrentDue(currentDemand);
            if (currentCollectionWithoutAdvance == null) {
                currentCollectionWithoutAdvance = BigDecimal.ZERO;
            }
            payment.setCurrentAmountPaid(currentCollectionWithoutAdvance);
            try {
                List<BillDetail> billDetails = payment.getPaymentDetails().get(0).getBill().getBillDetails();
                DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
                formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
                if (!CollectionUtils.isEmpty(billDetails)) {
                    BillDetail billDetailFrom = payment.getPaymentDetails().get(0).getBill().getBillDetails().get(payment.getPaymentDetails().get(0).getBill().getBillDetails().size()-1);
                    BillDetail billDetailTo = payment.getPaymentDetails().get(0).getBill().getBillDetails().get(0);
                    payment.setDemandPeriod(formatter.format(billDetailFrom.getFromPeriod()) + " to " + formatter.format(billDetailTo.getToPeriod()));
                    calculateCollectionPeriod(payment, formatter, billDetailFrom);
                    calculateArrearDemandDuration(payment, billDetailFrom, billDetailTo, formatter);

                }
            }
            catch (Exception e){
                log.error(e.getMessage());
                e.printStackTrace();
            }
        }
	}

    private void calculateArrearDemandDuration(Payment payment, BillDetail billDetailFrom, BillDetail billDetailTo, DateFormat formatter) {
        Calendar demandPeriodToCalendar = Calendar.getInstance();
        demandPeriodToCalendar.setTimeInMillis(billDetailTo.getToPeriod());
        int toYear = demandPeriodToCalendar.get(Calendar.YEAR);
        long currentTime = System.currentTimeMillis();
        Calendar currentCalendar = Calendar.getInstance();
        currentCalendar.setTimeInMillis(currentTime);
        int currentMonth = currentCalendar.get(Calendar.MONTH);
        int currentYear = currentCalendar.get(Calendar.YEAR);
        int currentFiscalToYear = currentMonth>= Calendar.MARCH ? currentYear+1:currentYear;
        int arrearToYear = currentFiscalToYear-1;
        if(payment.getPaymentDetails().get(0).getBill().getBillDetails().size()>1) {
	        if(payment.getPaymentDetails().get(0).getBill().getBillDetails().get(1)!=null){
		            demandPeriodToCalendar.add(Calendar.YEAR,(arrearToYear-toYear));
		            payment.setArrearDemandDuration(formatter.format(billDetailFrom.getFromPeriod()) +" to "+ formatter.format(demandPeriodToCalendar.getTimeInMillis()));
		        }
        }
        else {
        	if(arrearToYear==toYear) {
        		payment.setArrearDemandDuration(formatter.format(billDetailFrom.getFromPeriod()) +" - "+ formatter.format(demandPeriodToCalendar.getTimeInMillis()));
        	}
        }
    }

    private void calculateCollectionPeriod(Payment payment, DateFormat formatter, BillDetail billDetailFrom) {
        List<BillDetail> billDetails = payment.getPaymentDetails().get(0).getBill().getBillDetails();
        if(CollectionUtils.isEmpty(billDetails)) {
            return;
        }
        String toPeriod=null,fromPeriod=null;
        boolean isFromPeriodCalculated=false ;
        for (BillDetail billDetail:billDetails){
            if(billDetail.getAmountPaid().compareTo(BigDecimal.ZERO)!=0){
                if(!isFromPeriodCalculated){
                    fromPeriod = formatter.format(billDetail.getFromPeriod());
                    isFromPeriodCalculated=true;
                }
                toPeriod=formatter.format(billDetail.getToPeriod());
                break;
            }
        }
        payment.setCollectionPeriod(fromPeriod + " to "+ toPeriod );
    }


    private BigDecimal extractSumFromBillAccountDetails(BillDetail billDetail) {
		BigDecimal arrearCollectionWithoutAdvance;
		arrearCollectionWithoutAdvance = billDetail.getBillAccountDetails().stream()
				.filter(billAccountDetail -> !billAccountDetail.getTaxHeadCode().equals(advanceTaxCode)) 
				.map(BillAccountDetail::getAdjustedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
		return arrearCollectionWithoutAdvance;
	}

    /**
     * Fetch all receipts matching the given criteria, enrich receipts with instruments
     *
     * @param requestInfo           Request info of the search
     * @param paymentSearchCriteria Criteria against which search has to be performed
     * @return List of matching receipts
     */
    public List<Payment> getPayments(RequestInfo requestInfo, PaymentSearchCriteria paymentSearchCriteria) {
    	
        Map<String, String> errorMap = new HashMap<>();
        paymentValidator.validateUserInfo(requestInfo, errorMap);
        if (!errorMap.isEmpty())
            throw new CustomException(errorMap);

        if (applicationProperties.isPaymentsSearchPaginationEnabled()) {
            paymentSearchCriteria.setOffset(isNull(paymentSearchCriteria.getOffset()) ? 0 : paymentSearchCriteria.getOffset());
            paymentSearchCriteria.setLimit(isNull(paymentSearchCriteria.getLimit()) ? applicationProperties.getReceiptsSearchDefaultLimit() :
                    paymentSearchCriteria.getLimit());
        } else {
            paymentSearchCriteria.setOffset(0);
            paymentSearchCriteria.setLimit(applicationProperties.getReceiptsSearchDefaultLimit());
        }
        /*if(requestInfo.getUserInfo().getType().equals("CITIZEN")) {
            List<String> payerIds = new ArrayList<>();
            payerIds.add(requestInfo.getUserInfo().getUuid());
            paymentSearchCriteria.setPayerIds(payerIds);
        }*/
        List<Payment> payments = paymentRepository.fetchPayments(paymentSearchCriteria);
        
        return payments;
    }
    
    @Transactional
    public List<Payment> updatePaymentStatus(RequestInfo requestInfo, PaymentSearchCriteria paymentSearchCriteria, PaymentStatusEnum paymentStatus) {

        List<Payment> validatedPayments = getPayments(requestInfo, paymentSearchCriteria);

        for (Payment payment : validatedPayments) {
        	payment.setPaymentStatus(paymentStatus);
        }

        AuditDetails auditDetails = AuditDetails.builder()
				.lastModifiedBy(requestInfo.getUserInfo().getId().toString())
				.lastModifiedTime(System.currentTimeMillis()).build();

        paymentRepository.updatePaymentStatus(validatedPayments,auditDetails);
        return validatedPayments;
    }
    
    
    /**
     * Handles creation of a receipt, including multi-service, involves the following steps, - Enrich receipt from billing service
     * using bill id - Validate the receipt object - Enrich receipt with receipt numbers, coll type etc - Apportion paid amount -
     * Persist the receipt object - Create instrument
     *
     * @param paymentRequest payment request for which receipt has to be created
     * @return Created receipt
     */
    @Transactional
    public Payment createPayment(PaymentRequest paymentRequest) {
    	
        paymentEnricher.enrichPaymentPreValidate(paymentRequest);
        paymentValidator.validatePaymentForCreate(paymentRequest);
        paymentEnricher.enrichPaymentPostValidate(paymentRequest);

        Payment payment = paymentRequest.getPayment();
        Map<String, Bill> billIdToApportionedBill = apportionerService.apportionBill(paymentRequest);
        paymentEnricher.enrichAdvanceTaxHead(new LinkedList<>(billIdToApportionedBill.values()));
        setApportionedBillsToPayment(billIdToApportionedBill,payment);

        String payerId = createUser(paymentRequest);
        if(!StringUtils.isEmpty(payerId))
            payment.setPayerId(payerId);
        paymentRepository.savePayment(payment);
        
        log.info("create payment topic name: "+applicationProperties.getCreatePaymentTopicName());
        log.info("paymentRequest :"+paymentRequest);
        
        producer.producer(applicationProperties.getCreatePaymentTopicName(), paymentRequest);

        whatsAppNotificationService.pushWhatsappNotification(payment);
        
		whatsAppNotificationService.pushWhatsappNotificationv2(payment);
		
        return payment;
    }


    /**
     * If Citizen is paying, the id of the logged in user becomes payer id.
     * If Employee is paying, 
     * 1. the id of the owner of the bill will be attached as payer id.
     * 2. In case the bill is for a misc payment, payer id is empty.
     * 
     * @param paymentRequest
     * @return
     */
    public String createUser(PaymentRequest paymentRequest) {
    	
        String id = null;
        if(paymentRequest.getRequestInfo().getUserInfo().getType().equals("CITIZEN")) {
            id = paymentRequest.getRequestInfo().getUserInfo().getUuid();
        }else {
            if(applicationProperties.getIsUserCreateEnabled()) {
                Payment payment = paymentRequest.getPayment();
                Map<String, String> res = userService.getUser(paymentRequest.getRequestInfo(), payment.getMobileNumber(), payment.getTenantId());
                if(CollectionUtils.isEmpty(res.keySet())) {
                    id = userService.createUser(paymentRequest);
                }else {
                    id = res.get("id");
                }
            }
        }
        return id;
    }


    private void setApportionedBillsToPayment(Map<String, Bill> billIdToApportionedBill,Payment payment){
        Map<String,String> errorMap = new HashMap<>();
        payment.getPaymentDetails().forEach(paymentDetail -> {
            if(billIdToApportionedBill.get(paymentDetail.getBillId())!=null)
                paymentDetail.setBill(billIdToApportionedBill.get(paymentDetail.getBillId()));
            else errorMap.put("APPORTIONING_ERROR","The bill id: "+paymentDetail.getBillId()+" not present in apportion response");
        });
        if(!errorMap.isEmpty())
            throw new CustomException(errorMap);
    }


    @Transactional
    public List<Payment> updatePayment(PaymentRequest paymentRequest) {

        List<Payment> validatedPayments = paymentValidator.validateAndEnrichPaymentsForUpdate(Collections.singletonList(paymentRequest.getPayment()),
                paymentRequest.getRequestInfo());

        paymentRepository.updatePayment(validatedPayments);
        producer.producer(applicationProperties.getUpdatePaymentTopicName(), new PaymentRequest(paymentRequest.getRequestInfo(), paymentRequest.getPayment()));

        return validatedPayments;
    }
    
    
    
    /**
     * Used by payment gateway to validate provisional receipts of the payment
     * 
     * @param paymentRequest
     * @return
     */
    @Transactional
    public Payment vaidateProvisonalPayment(PaymentRequest paymentRequest) {
        paymentEnricher.enrichPaymentPreValidate(paymentRequest);
        paymentValidator.validatePaymentForCreate(paymentRequest);
        
        return paymentRequest.getPayment();
    }

    public List<Payment> plainSearch(PaymentSearchCriteria paymentSearchCriteria) {
        PaymentSearchCriteria searchCriteria = new PaymentSearchCriteria();

        if (applicationProperties.isPaymentsSearchPaginationEnabled()) {
            searchCriteria.setOffset(isNull(paymentSearchCriteria.getOffset()) ? 0 : paymentSearchCriteria.getOffset());
            searchCriteria.setLimit(isNull(paymentSearchCriteria.getLimit()) ? applicationProperties.getReceiptsSearchDefaultLimit() : paymentSearchCriteria.getLimit());
        } else {
            searchCriteria.setOffset(0);
            searchCriteria.setLimit(applicationProperties.getReceiptsSearchDefaultLimit());
        }

        List<String> ids = paymentRepository.fetchPaymentIds(searchCriteria);
        if (ids.isEmpty())
            return Collections.emptyList();

        PaymentSearchCriteria criteria = PaymentSearchCriteria.builder().ids(new HashSet<String>(ids)).build();
        return paymentRepository.fetchPaymentsForPlainSearch(criteria);
    }


}
