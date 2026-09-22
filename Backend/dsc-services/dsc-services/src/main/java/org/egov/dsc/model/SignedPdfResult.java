package org.egov.dsc.model;

public class SignedPdfResult {
	private final String fileStoreId;
	private final boolean dsc;

	public SignedPdfResult(String fileStoreId, boolean dsc) {
		this.fileStoreId = fileStoreId;
		this.dsc = dsc;
	}

	public String getFileStoreId() {
		return fileStoreId;
	}

	public boolean isDsc() {
		return dsc;
	}
}
