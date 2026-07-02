CREATE INDEX IF NOT EXISTS idx_egcl_payment_tenantid ON egcl_payment(tenantid);
CREATE INDEX IF NOT EXISTS idx_egcl_payment_transactiondate ON egcl_payment(transactiondate);
CREATE INDEX IF NOT EXISTS idx_egcl_payment_paymentmode ON egcl_payment(paymentmode);

CREATE INDEX IF NOT EXISTS idx_egcl_paymentDetail_tenantid ON egcl_paymentDetail(tenantid);
CREATE INDEX IF NOT EXISTS idx_egcl_paymentDetail_receiptdate ON egcl_paymentDetail(receiptdate);
CREATE INDEX IF NOT EXISTS idx_egcl_paymentDetail_businessservice ON egcl_paymentDetail(businessservice);