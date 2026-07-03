package org.egov.commons.mdms.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MdmsConfiguration {

    @Value("${mdms.host:}")
    private String mdmsHost;

    @Value("${mdms.searchurl:}")
    private String mdmsSearchUrl;

    @Value("${mdms.enable:false}")
    private Boolean mdmsEnabled;
    
    @Value("${egov.collection.service.host:}")
    private String collectionServiceHost;
    
    @Value("${egov.noc.service.host:}")
    private String nocHost;
    
    @Value("${egov.bpa.service.host:}")
    private String bpaHost;


    @Value("${egov.user.service.host:}")
    private String userServiceHost;
    
    @Value("${egov.location.service.host:}")
    private String locationServiceHost;
    
    @Value("${egov.tl.bpareg.service.host:}")
    private String tlBpaRegServiceHost;

	public String getUserServiceHost() {
		return userServiceHost;
	}

	public void setUserServiceHost(String userServiceHost) {
		this.userServiceHost = userServiceHost;
	}


    public String getBpaHost() {
		return bpaHost;
	}

	public void setBpaHost(String bpaHost) {
		this.bpaHost = bpaHost;
	}

    public String getMdmsHost() {
        return mdmsHost;
    }

    public void setMdmsHost(String mdmsHost) {
        this.mdmsHost = mdmsHost;
    }

    public String getMdmsSearchUrl() {
        return mdmsSearchUrl;
    }

    public void setMdmsSearchUrl(String mdmsSearchUrl) {
        this.mdmsSearchUrl = mdmsSearchUrl;
    }

    public Boolean getMdmsEnabled() {
        return mdmsEnabled;
    }

    public void setMdmsEnabled(Boolean mdmsEnabled) {
        this.mdmsEnabled = mdmsEnabled;
    }

	public String getCollectionServiceHost() {
		return collectionServiceHost;
	}

	public void setCollectionServiceHost(String collectionServiceHost) {
		this.collectionServiceHost = collectionServiceHost;
	}

	public String getNocHost() {
		return nocHost;
	}

	public void setNocHost(String nocHost) {
		this.nocHost = nocHost;
	}

	public String getLocationServiceHost() {
		return locationServiceHost;
	}

	public void setLocationServiceHost(String locationServiceHost) {
		this.locationServiceHost = locationServiceHost;
	}

	public String getTlBpaRegServiceHost() {
		return tlBpaRegServiceHost;
	}

	public void setTlBpaRegServiceHost(String tlBpaRegServiceHost) {
		this.tlBpaRegServiceHost = tlBpaRegServiceHost;
	}

    
}
