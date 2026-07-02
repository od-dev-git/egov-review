/*
 * eGov suite of products aim to improve the internal efficiency,transparency,
 * accountability and the service delivery of the government  organizations.
 *
 *  Copyright (C) 2016  eGovernments Foundation
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
 *
 *      2) Any misrepresentation of the origin of the material is prohibited. It
 *         is required that all modified versions of this material be marked in
 *         reasonable ways as different from the original version.
 *
 *      3) This license does not grant any rights to any  of the program
 *         with regards to rights under trademark law for use of the trade names
 *         or trademarks of eGovernments Foundation.
 *
 *  In case of any queries, you can reach eGovernments Foundation at contact@egovernments.org.
 */
package org.egov.dsc.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@Getter
@Setter
public class ApplicationProperties {
    
    @Value("${dsc.emas.url}")
    private String emasWsUrl;
    
    @Value("${dsc.pdf.embridge.property.one}")
    private String pdfProperty1;
    
    @Value("${dsc.pdf.embridge.property.two}")
    private String pdfProperty2;
    
    @Value("${dsc.pdf.embridge.property.three}")
    private String pdfProprty3;
    
    @Value("${dsc.pdf.embridge.property.four}")
    private String pdfProprty4;
    
    @Value("${egov.filestore.service.endpoint}")
    private String egovFileStoreSerHost;
    
    @Value("${egov.services.filestore.service.upload.file}")
    private String egovFileStoreUploadFile;
    
    @Value("${egov.services.filestore.service.download.file}")
    private String egovFileStoreDownloadFile;
    
    @Value("${DSC_ERR_01}")
    private String DSC_ERR_01;
    
    @Value("${DSC_ERR_02}")
    private String DSC_ERR_02;
    
    @Value("${DSC_ERR_03}")
    private String DSC_ERR_03;
    
    @Value("${DSC_ERR_04}")
    private String DSC_ERR_04;
    
    @Value("${DSC_ERR_05}")
    private String DSC_ERR_05;
    
    @Value("${DSC_ERR_06}")
    private String DSC_ERR_06;
    
    @Value("${DSC_ERR_07}")
    private String DSC_ERR_07;
    
    @Value("${DSC_ERR_08}")
    private String DSC_ERR_08;
    
    @Value("${DSC_ERR_09}")
    private String DSC_ERR_09;
    
    @Value("${DSC_ERR_10}")
    private String DSC_ERR_10;
    
    @Value("${DSC_ERR_11}")
    private String DSC_ERR_11;
    
    @Value("${DSC_ERR_12}")
    private String DSC_ERR_12;
    
    @Value("${DSC_ERR_13}")
    private String DSC_ERR_13;
    
    @Value("${DSC_ERR_14}")
    private String DSC_ERR_14;
    
    @Value("${DSC_ERR_15}")
    private String DSC_ERR_15;
    
    @Value("${DSC_ERR_16}")
    private String DSC_ERR_16;
    
    @Value("${DSC_ERR_17}")
    private String DSC_ERR_17;
    
    @Value("${DSC_ERR_18}")
    private String DSC_ERR_18;
    
    @Value("${DSC_ERR_19}")
    private String DSC_ERR_19;
    
    @Value("${DSC_ERR_20}")
    private String DSC_ERR_20;
    
    @Value("${DSC_ERR_21}")
    private String DSC_ERR_21;
    
    @Value("${DSC_ERR_22}")
    private String DSC_ERR_22;
    
    @Value("${DSC_ERR_23}")
    private String DSC_ERR_23;
    		
    @Value("${DSC_ERR_24}")
    private String DSC_ERR_24;
    
    @Value("${DSC_ERR_25}")
    private String DSC_ERR_25; 
    
    @Value("${DSC_ERR_26}")
    private String DSC_ERR_26; 
    
    @Value("${DSC_ERR_27}")
    private String DSC_ERR_27; 
    
    @Value("${DSC_ERR_28}")
    private String DSC_ERR_28; 
    
    @Value("${DSC_ERR_29}")
    private String DSC_ERR_29; 
}
