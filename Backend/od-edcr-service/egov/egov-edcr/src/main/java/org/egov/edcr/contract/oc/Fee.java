package org.egov.edcr.contract.oc;

import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;


public class Fee {

    String feeType;

    BigDecimal feeAmount;

    BigDecimal paidAmount;

    BigDecimal amountNotPaid;

	public Fee(String feeType, BigDecimal feeAmount, BigDecimal paidAmount, BigDecimal amountNotPaid) {
		super();
		this.feeType = feeType;
		this.feeAmount = feeAmount;
		this.paidAmount = paidAmount;
		this.amountNotPaid = amountNotPaid;
	}

	public Fee() {
		super();
	}

	public String getFeeType() {
		return feeType;
	}

	public void setFeeType(String feeType) {
		this.feeType = feeType;
	}

	public BigDecimal getFeeAmount() {
		return feeAmount;
	}

	public void setFeeAmount(BigDecimal feeAmount) {
		this.feeAmount = feeAmount;
	}

	public BigDecimal getPaidAmount() {
		return paidAmount;
	}

	public void setPaidAmount(BigDecimal paidAmount) {
		this.paidAmount = paidAmount;
	}

	public BigDecimal getAmountNotPaid() {
		return amountNotPaid;
	}

	public void setAmountNotPaid(BigDecimal amountNotPaid) {
		this.amountNotPaid = amountNotPaid;
	}
    
    
}
