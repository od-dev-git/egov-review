package org.egov.edcr.entity;

import java.math.BigDecimal;

public class PaymentTable {

	private String detailName;

	private BigDecimal demandAmount;

	private BigDecimal amountPaid;

	public PaymentTable(String detailName, BigDecimal demandAmount, BigDecimal amountPaid) {
		this.detailName = detailName;
		this.demandAmount = demandAmount;
		this.amountPaid = amountPaid;
	}

	public String getDetailName() {
		return detailName;
	}

	public void setDetailName(String detailName) {
		this.detailName = detailName;
	}

	public BigDecimal getDemandAmount() {
		return demandAmount;
	}

	public void setDemandAmount(BigDecimal demandAmount) {
		this.demandAmount = demandAmount;
	}

	public BigDecimal getAmountPaid() {
		return amountPaid;
	}

	public void setAmountPaid(BigDecimal amountPaid) {
		this.amountPaid = amountPaid;
	}

}
