package org.egov.dsc.model;

import java.util.List;

public class StorageResponse {

	private List<FileReq> files;

    public List<FileReq> getFiles() {
        return files;
    }

    public void setFiles(List<FileReq> files) {
        this.files = files;
    }
}
