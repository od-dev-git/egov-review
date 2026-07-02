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
 *      3) This license does not grant any rights to any user of the program
 *         with regards to rights under trademark law for use of the trade names
 *         or trademarks of eGovernments Foundation.
 *
 *  In case of any queries, you can reach eGovernments Foundation at contact@egovernments.org.
 */

package org.egov.dsc.web.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.dsc.config.ApplicationProperties;
import org.egov.dsc.emas.ws.ds.DSAuthenticateWS;
import org.egov.dsc.emas.ws.ds.DSAuthenticateWSProxy;
import org.egov.dsc.model.CertificateRequest;
import org.egov.dsc.model.CertificateResponse;
import org.egov.dsc.model.CertificateResponsePojo;
import org.egov.dsc.model.DSCException;
import org.egov.dsc.model.DataSignRequest;
import org.egov.dsc.model.DataSignResponse;
import org.egov.dsc.model.DetailRequestPojo;
import org.egov.dsc.model.FileReq;
import org.egov.dsc.model.StorageResponse;
import org.egov.dsc.model.TokenInputResponse;
import org.egov.dsc.model.TokenRequest;
import org.egov.dsc.model.TokenResponse;
import org.egov.dsc.web.contract.factory.ResponseInfoFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import emh.Enum.ContentType;
import emh.Enum.Token_Status;
import emh.Enum.Token_Type;
import emh.Model.RequestData.ListCertificateRequest;
import emh.Model.RequestData.ListTokenRequest;
import emh.Model.RequestData.PKCSBulkPdfHashSignRequest;
import emh.Model.RequestData.PKCSSignRequest;
import emh.Model.RequestData.Request;
import emh.Model.ResponseData.BulkSignOutput;
import emh.Model.ResponseData.PKCSCertificate;
import emh.Model.ResponseData.ProviderToken;
import emh.Model.ResponseData.ResponseDataListPKCSCertificate;
import emh.Model.ResponseData.ResponseDataListProviderToken;
import emh.Model.ResponseData.ResponseDataPKCSBulkSign;
import emh.Model.ResponseData.ResponseDataPKCSSign;
import emh.emBridgeLib.EMBLException;
import emh.emBridgeLib.emBridge;
import emh.emBridgeLib.emBridgeSignerInput;

@RestController
@RequestMapping("/dsc")
public class DscController {

	@Autowired
	private ApplicationProperties applicationProperties;

	@Autowired
	private RestTemplate restTemplate;

	private String logPath = System.getProperty("user.dir") + "/DS/Log";
	private String tempPath = System.getProperty("user.dir") + "/" + "DS/Temp";
	private String licPath = System.getProperty("user.dir") + "/" + "DS/Lic";
	private String tempFilePath = "/DS/TempFiles/";
	private boolean dsc = false;

	@GetMapping(value = "/_getCheck")
	public String test() {
		return applicationProperties.getEmasWsUrl();
	}

	@RequestMapping(value = "/_getTokenInput", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<TokenInputResponse> token(@RequestBody TokenRequest tokenRequest) {
		emBridge bridge = null;
		File logFile = new File(logPath);
		File licFile = new File(licPath);
		ListTokenRequest listTokenRequest = new ListTokenRequest();
		Request data = null;
		DetailRequestPojo input = new DetailRequestPojo();
		try {
			if (!logFile.exists()) {
				logFile.mkdirs();
			}
			if (!licFile.exists()) {
				licFile.mkdirs();
			}

			bridge = new emBridge(licPath + "/OdishaUrban.lic", logFile.getCanonicalPath());
			listTokenRequest.setTokenStatus(Token_Status.CONNECTED);
			listTokenRequest.setTokenType(Token_Type.HARDWARE);
		} catch (IOException e) {
			input.setDscErrorCode(applicationProperties.getDSC_ERR_01());
			e.printStackTrace();
			return getSuccessTokenInputResponse(data, input, tokenRequest.getRequestInfo());
		}

		try {
			data = bridge.encListToken(listTokenRequest);
			System.out.println("data.getErrorCode() :::" + data.getErrorCode());
			System.out.println("Encrypted Data :" + data.getEncryptedData());
			System.out.println("Encrypted Key ID :" + data.getEncryptionKeyID());
		} catch (Exception e) {
			input.setDscErrorCode(applicationProperties.getDSC_ERR_02());
			e.printStackTrace();
			return getSuccessTokenInputResponse(data, input, tokenRequest.getRequestInfo());
		}
		return getSuccessTokenInputResponse(data, input, tokenRequest.getRequestInfo());

	}

	@RequestMapping(value = "/_getTokens", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<TokenResponse> tokens(@RequestBody TokenRequest tokenRequest) {
		List<String> tokens = new ArrayList<String>();
		emBridge bridge = null;
		File logFile = new File(logPath);
		File licFile = new File(licPath);
		ResponseDataListProviderToken responseDataListProviderToken = null;
		List<ProviderToken> tokens1 = null;
		String emudhraErrorCode = "";
		try {
			if (!logFile.exists()) {
				logFile.mkdirs();
			}
			if (!licFile.exists()) {
				licFile.mkdirs();
			}

			bridge = new emBridge(licPath + "/OdishaUrban.lic", logFile.getCanonicalPath());
		} catch (IOException e) {
			e.printStackTrace();
			return getSuccessTokenResponse(tokens, tokenRequest.getRequestInfo(), applicationProperties.getDSC_ERR_01(),
					null);
		}
		try {
			responseDataListProviderToken = bridge.decListToken(tokenRequest.getResponseData());
			if (responseDataListProviderToken != null) {
				if (responseDataListProviderToken.getErrorCode() != null) {
					emudhraErrorCode = responseDataListProviderToken.getErrorCode() + " - "
							+ responseDataListProviderToken.getErrorMsg();
				}

				if (responseDataListProviderToken.getTokens() != null) {
					for (ProviderToken token : responseDataListProviderToken.getTokens()) {
						System.out.println(token.getKeyStoreDisplayName());
						tokens.add(token.getKeyStoreDisplayName());
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			return getSuccessTokenResponse(tokens, tokenRequest.getRequestInfo(), applicationProperties.getDSC_ERR_13(),
					null);
		}

		return getSuccessTokenResponse(tokens, tokenRequest.getRequestInfo(), null, emudhraErrorCode);

	}

	@RequestMapping(value = "/_getInputCertificate", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<TokenInputResponse> cert(@RequestBody CertificateRequest certificateRequest) {
		emBridge bridge = null;
		File logFile = new File(logPath);
		File licFile = new File(licPath);
		ListCertificateRequest listCertRequest = new ListCertificateRequest();
		DetailRequestPojo input = new DetailRequestPojo();
		Request data = null;
		try {
			if (!logFile.exists()) {
				logFile.mkdirs();
			}
			if (!licFile.exists()) {
				licFile.mkdirs();
			}

			bridge = new emBridge(licPath + "/OdishaUrban.lic", logFile.getCanonicalPath());
			listCertRequest.setKeyStoreDisplayName(certificateRequest.getTokenDisplayName());// Microsoft Windows
																								// Store//ePass V
		} catch (IOException e) {
			input.setDscErrorCode(applicationProperties.getDSC_ERR_01());
			e.printStackTrace();
			return getSuccessTokenInputResponse(data, input, certificateRequest.getRequestInfo());
		}
		try {
			data = bridge.encListCertificate(listCertRequest);
			System.out.println("Encrypted Data :" + data.getEncryptedData());
			System.out.println("Encrypted Key ID :" + data.getEncryptionKeyID());
		} catch (Exception e) {
			input.setDscErrorCode(applicationProperties.getDSC_ERR_03());
			e.printStackTrace();
			return getSuccessTokenInputResponse(data, input, certificateRequest.getRequestInfo());
		}
		return getSuccessTokenInputResponse(data, input, certificateRequest.getRequestInfo());
	}

	@RequestMapping(value = "/_getCertificate", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<CertificateResponse> certa(@RequestBody CertificateRequest certificateRequest) {
		emBridge bridge = null;
		File logFile = new File(logPath);
		File licFile = new File(licPath);
		ResponseDataListPKCSCertificate responseDataListPKCSCertificate = null;
		List<PKCSCertificate> certificates = null;
		List<CertificateResponsePojo> certificatesList = null;
		CertificateResponsePojo certificate = null;
		String emudhraErrorCode = "";
		try {
			if (!logFile.exists()) {
				logFile.mkdirs();
			}
			if (!licFile.exists()) {
				licFile.mkdirs();
			}
			bridge = new emBridge(licPath + "/OdishaUrban.lic", logFile.getCanonicalPath());
		} catch (IOException e) {
			e.printStackTrace();
			return getSuccessCertResponse(certificatesList, certificateRequest.getRequestInfo(),
					applicationProperties.getDSC_ERR_01(), null);
		}
		try {
			responseDataListPKCSCertificate = bridge.decListCertificate(certificateRequest.getResponseData(), null);
		} catch (Exception e) {
			e.printStackTrace();
			return getSuccessCertResponse(certificatesList, certificateRequest.getRequestInfo(),
					applicationProperties.getDSC_ERR_04(), null);
		}
		try {
			if (responseDataListPKCSCertificate != null) {
				if (responseDataListPKCSCertificate.getErrorCode() != null) {
					emudhraErrorCode = responseDataListPKCSCertificate.getErrorCode() + " - "
							+ responseDataListPKCSCertificate.getErrorMsg();
				}
				if (responseDataListPKCSCertificate.getCertificates() != null) {
					certificates = responseDataListPKCSCertificate.getCertificates();
					certificatesList = new ArrayList<CertificateResponsePojo>();
					for (PKCSCertificate cert : certificates) {
						System.out.println("value of keyId :" + cert.getKeyId());
						System.out.println("value of common name :" + cert.getCommonName());
						certificate = new CertificateResponsePojo();
						certificate.setKeyId(cert.getKeyId());
						certificate.setCommonName(cert.getCommonName());
						certificate.setCertificateDate(cert.getCertificateData());
						certificatesList.add(certificate);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return getSuccessCertResponse(certificatesList, certificateRequest.getRequestInfo(),
					applicationProperties.getDSC_ERR_14(), null);
		}
		return getSuccessCertResponse(certificatesList, certificateRequest.getRequestInfo(), null, emudhraErrorCode);

	}

	@RequestMapping(value = "/_dataSignInput", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<TokenInputResponse> dataSignInput(@RequestBody DataSignRequest dataSignRequest) {
		emBridge bridge = null;
		File logFile = new File(logPath);
		File licFile = new File(licPath);
		String randomNumber = "";
		DetailRequestPojo input = new DetailRequestPojo();
		PKCSSignRequest pKCSSignRequest = new PKCSSignRequest();
		Request data = null;
		try {
			if (!logFile.exists()) {
				logFile.mkdirs();
			}
			if (!licFile.exists()) {
				licFile.mkdirs();
			}
			bridge = new emBridge(licPath + "/OdishaUrban.lic", logFile.getCanonicalPath());
			pKCSSignRequest.setKeyStoreDisplayName(dataSignRequest.getTokenDisplayName());// token
			pKCSSignRequest.setKeyStorePassphrase(dataSignRequest.getKeyStorePassPhrase());
			pKCSSignRequest.setKeyId(dataSignRequest.getKeyId());
			pKCSSignRequest.setDataType(ContentType.TextPKCS7ATTACHED);
		} catch (IOException e) {
			input.setDscErrorCode(applicationProperties.getDSC_ERR_01());
			e.printStackTrace();
			return getSuccessTokenInputResponse(data, input, dataSignRequest.getRequestInfo());
		}

		// emas random number execute
		try {
			randomNumber = populateRandom(dataSignRequest.getRequestInfo().getUserInfo().getId(),
					dataSignRequest.getChannelId());
			if (randomNumber.contains("~")) {
				randomNumber = randomNumber.split("~")[1];
			} else {
				input.setEmudhraErrorCode(randomNumber);
				return getSuccessTokenInputResponse(data, input, dataSignRequest.getRequestInfo());
			}
			pKCSSignRequest.setDataToSign(dataSignRequest.getRequestInfo().getUserInfo().getId() + "~" + randomNumber);
			pKCSSignRequest.setTimeStamp("");
		} catch (DSCException e) {
			input.setDscErrorCode(applicationProperties.getDSC_ERR_06());
			return getSuccessTokenInputResponse(data, input, dataSignRequest.getRequestInfo());
		}

		try {
			data = bridge.encPKCSSign(pKCSSignRequest);
			System.out.println("Encrypted Data :" + data.getEncryptedData());
			System.out.println("Encrypted Key ID :" + data.getEncryptionKeyID());
		} catch (Exception e) {
			input.setDscErrorCode(applicationProperties.getDSC_ERR_05());
			e.printStackTrace();
			return getSuccessTokenInputResponse(data, input, dataSignRequest.getRequestInfo());
		}
		return getSuccessTokenInputResponse(data, input, dataSignRequest.getRequestInfo());

	}

	@RequestMapping(value = "/_dataSign", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<DataSignResponse> dataSign(@RequestBody DataSignRequest dataSignRequest) {
		emBridge bridge = null;
		File logFile = new File(logPath);
		File licFile = new File(licPath);
		String repsonse = "";
		ResponseDataPKCSSign responseDataPKCSSign = null;
		String emudhraErrorCode = "";
		try {
			if (!logFile.exists()) {
				logFile.mkdirs();
			}
			if (!licFile.exists()) {
				licFile.mkdirs();
			}
			bridge = new emBridge(licPath + "/OdishaUrban.lic", logFile.getCanonicalPath());
		} catch (IOException e) {
			e.printStackTrace();
			return getSuccessDataSignResponse(repsonse, null, dataSignRequest.getRequestInfo(),
					applicationProperties.getDSC_ERR_01(), null);
		}

		try {
			responseDataPKCSSign = bridge.decPKCSSign(dataSignRequest.getResponseData());
		} catch (EMBLException e) {
			e.printStackTrace();
			return getSuccessDataSignResponse(repsonse, null, dataSignRequest.getRequestInfo(),
					applicationProperties.getDSC_ERR_07(), null);
		}
		// register
		try {
			if (responseDataPKCSSign != null) {
				if (responseDataPKCSSign.getSignedText() != null) {
					repsonse = populateRegisterSoapCall(dataSignRequest.getRequestInfo().getUserInfo().getId(),
							dataSignRequest.getChannelId(), responseDataPKCSSign.getSignedText());
					if (!(repsonse.toLowerCase().contains("success"))) {
						emudhraErrorCode = repsonse;
					}
				}
			}

		} catch (DSCException e) {
			return getSuccessDataSignResponse(repsonse, null, dataSignRequest.getRequestInfo(),
					applicationProperties.getDSC_ERR_08(), null);
		}

		return getSuccessDataSignResponse(repsonse, null, dataSignRequest.getRequestInfo(), null, emudhraErrorCode);

	}

	private String populateRegisterSoapCall(Long id, String channel, String signedText) throws DSCException {
		String result = "";
		DSAuthenticateWS authenticateWS = new DSAuthenticateWSProxy(applicationProperties.getEmasWsUrl());
		try {
			result = authenticateWS.register(id + "~" + channel, signedText, "registration", "registration", true);
			System.out.println("result after registration ::::" + result);
		} catch (RemoteException e) {
			e.printStackTrace();
			throw new DSCException("Error in register api from Emas");
		}
		return result;
	}

	private String populateRandom(Long id, String channel) throws DSCException {
		String result = "";
		DSAuthenticateWS authenticateWS = null;
		try {
			authenticateWS = new DSAuthenticateWSProxy(applicationProperties.getEmasWsUrl());
			result = authenticateWS.generateRandomNumber(id + "~" + channel);
			System.out.println("after result ::::" + result);
		} catch (RemoteException e) {
			e.printStackTrace();
			throw new DSCException("Error in populating random number from Emas");
		}
		return result;
	}

	private ResponseEntity<TokenResponse> getSuccessTokenResponse(List<String> tokens, RequestInfo requestInfo,
			String dscErrorCode, String emudhraErrorCode) {
		final ResponseInfo responseInfo = ResponseInfoFactory.createResponseInfoFromRequestInfo(requestInfo, true);
		responseInfo.setStatus(HttpStatus.OK.toString());

		TokenResponse tokenResponse = new TokenResponse(responseInfo, tokens, dscErrorCode, emudhraErrorCode);
		return new ResponseEntity<>(tokenResponse, HttpStatus.OK);
	}

	private ResponseEntity<TokenInputResponse> getSuccessTokenInputResponse(Request data, DetailRequestPojo input,
			RequestInfo requestInfo) {
		final ResponseInfo responseInfo = ResponseInfoFactory.createResponseInfoFromRequestInfo(requestInfo, true);
		responseInfo.setStatus(HttpStatus.OK.toString());
		if (data != null && data.getEncryptedData() != null) {
			input.setEncryptedRequest(data.getEncryptedData());
		}
		if (data != null && data.getEncryptionKeyID() != null) {
			input.setEncryptionKeyId(data.getEncryptionKeyID());
		}
		if (data != null && data.getErrorCode() != null) {
			input.setEmudhraErrorCode(data.getErrorCode() + " - " + data.getErrorMsg());
		}
		TokenInputResponse tokenResponse = new TokenInputResponse(responseInfo, input);
		return new ResponseEntity<>(tokenResponse, HttpStatus.OK);
	}

	private ResponseEntity<CertificateResponse> getSuccessCertResponse(List<CertificateResponsePojo> certs,
			RequestInfo requestInfo, String dscErrorCode, String emudhraErrorCode) {
		final ResponseInfo responseInfo = ResponseInfoFactory.createResponseInfoFromRequestInfo(requestInfo, true);
		responseInfo.setStatus(HttpStatus.OK.toString());

		CertificateResponse certificateResponse = new CertificateResponse(responseInfo, certs, dscErrorCode,
				emudhraErrorCode);
		return new ResponseEntity<>(certificateResponse, HttpStatus.OK);
	}

	private ResponseEntity<DataSignResponse> getSuccessDataSignResponse(String responseString, String fileStoreId,
			RequestInfo requestInfo, String dscErrorCode, String emudhraErrorCode) {
		final ResponseInfo responseInfo = ResponseInfoFactory.createResponseInfoFromRequestInfo(requestInfo, true);
		responseInfo.setStatus(HttpStatus.OK.toString());

		DataSignResponse datasign = new DataSignResponse(responseInfo, responseString, fileStoreId, dscErrorCode,
				emudhraErrorCode);
		return new ResponseEntity<>(datasign, HttpStatus.OK);
	}

	@RequestMapping(value = "/_pdfSignInput", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<TokenInputResponse> dpdfSignInput(@RequestBody DataSignRequest dataSignRequest) {
		emBridge bridge = null;
		File logFile = new File(logPath);
		File licFile = new File(licPath);
		File tempFile = new File(tempPath);
		File tempFilePathfile = new File(tempFilePath);
		String pdfStr = "";
		List<emBridgeSignerInput> inputs = new ArrayList<>();
		PKCSBulkPdfHashSignRequest pKCSBulkPdfHashSignRequest = new PKCSBulkPdfHashSignRequest();
		Request bulkPKCSSignRequest = null;
		String tempFilePath = null;
		DetailRequestPojo pojo = new DetailRequestPojo();
		//Added to set file permission
		File pdfSig = null;
		Set<PosixFilePermission> pdfSigPermissions = null;
		try {
			if (!logFile.exists()) {
				logFile.mkdirs();
			}
			if (!tempFile.exists()) {
				tempFile.mkdirs();
			}
			if (!licFile.exists()) {
				licFile.mkdirs();
			}
			if (!tempFilePathfile.exists()) {
				tempFilePathfile.mkdirs();
			}
			bridge = new emBridge(licPath + "/OdishaUrban.lic", logFile.getCanonicalPath());
		} catch (IOException e) {
			pojo.setDscErrorCode(applicationProperties.getDSC_ERR_01());
			e.printStackTrace();
			return getSuccessTokenInputResponse(bulkPKCSSignRequest, pojo, dataSignRequest.getRequestInfo());
		}

		try {
			pdfStr = getPdfBytes(dataSignRequest.getFileBytes(), dataSignRequest.getTenantId());
		} catch (DSCException e) {
			pojo.setDscErrorCode(e.getMessage());
			e.printStackTrace();
			return getSuccessTokenInputResponse(bulkPKCSSignRequest, pojo, dataSignRequest.getRequestInfo());
		}
		if (pdfStr == null || pdfStr.isEmpty()) {
			pojo.setDscErrorCode(applicationProperties.getDSC_ERR_12());
			return getSuccessTokenInputResponse(bulkPKCSSignRequest, pojo, dataSignRequest.getRequestInfo());
		}
		try {
			// emBridgeSignerInput input = new
			// emBridgeSignerInput(pdfStr,dataSignRequest.getFileName(),applicationProperties.getPdfProperty1(),
			// applicationProperties.getPdfProperty2(),dataSignRequest.getRequestInfo().getUserInfo().getName(),true,
			// PageTobeSigned.All, Coordinates.BottomMiddle,
			// applicationProperties.getPdfProprty4(), false);
			emBridgeSignerInput input = new emBridgeSignerInput(pdfStr, dataSignRequest.getFileName(),
					applicationProperties.getPdfProperty1(), applicationProperties.getPdfProperty2(),
					dataSignRequest.getRequestInfo().getUserInfo().getName(), true, "All-215,32,325,72",
					applicationProperties.getPdfProprty4(), false);
			inputs.add(input);
			pKCSBulkPdfHashSignRequest.setBulkInput(inputs);
			pKCSBulkPdfHashSignRequest.setTempFolder(tempFile.getCanonicalPath());
			pKCSBulkPdfHashSignRequest.setKeyStoreDisplayName(dataSignRequest.getTokenDisplayName());
			pKCSBulkPdfHashSignRequest.setKeyStorePassphrase(dataSignRequest.getKeyStorePassPhrase());
			pKCSBulkPdfHashSignRequest.setKeyId(dataSignRequest.getKeyId());
		} catch (IOException e) {
			pojo.setDscErrorCode(applicationProperties.getDSC_ERR_09());
			e.printStackTrace();
			return getSuccessTokenInputResponse(bulkPKCSSignRequest, pojo, dataSignRequest.getRequestInfo());
		}

		try {
			System.out.println("Retrying temp embridge call - 1st attempt");
			bulkPKCSSignRequest = bridge.encPKCSBulkSign(pKCSBulkPdfHashSignRequest);
			tempFilePath = bulkPKCSSignRequest.getTempFilePath();
			pdfSig = new File(tempFilePath);
			 if(!pdfSig.exists())
			{
				 System.out.println("Retrying temp embridge call - 2nd attempt");
				 bulkPKCSSignRequest = bridge.encPKCSBulkSign(pKCSBulkPdfHashSignRequest);
				 tempFilePath = bulkPKCSSignRequest.getTempFilePath();
				 pdfSig = new File(tempFilePath);
				 if(!pdfSig.exists())
				 {
					 System.out.println("Retrying temp embridge call - 3rd attempt");
					 bulkPKCSSignRequest = bridge.encPKCSBulkSign(pKCSBulkPdfHashSignRequest);
					 tempFilePath = bulkPKCSSignRequest.getTempFilePath();
					 pdfSig = new File(tempFilePath);
					 if(!pdfSig.exists())
					 {
						 System.out.println("Retrying temp embridge call - Failed");
						 pojo.setDscErrorCode(applicationProperties.getDSC_ERR_28());
							return getSuccessTokenInputResponse(bulkPKCSSignRequest, pojo, dataSignRequest.getRequestInfo());
					 }
				 }
			}
			System.out.println("Encrypted Data :" + bulkPKCSSignRequest.getEncryptedData());
			System.out.println("Encrypted Key ID :" + bulkPKCSSignRequest.getEncryptionKeyID());
			System.out.println("tempFilePath :" + tempFilePath);// if contains .sig
		} catch (Exception e) {
			pojo.setDscErrorCode(applicationProperties.getDSC_ERR_10());
			e.printStackTrace();
			return getSuccessTokenInputResponse(bulkPKCSSignRequest, pojo, dataSignRequest.getRequestInfo());
		}

		pojo.setFileName(dataSignRequest.getFileName());
		pojo.setTempFilePath(tempFilePath);
		if (!(tempFilePath != null && tempFilePath.contains(".sig"))) {
			pojo.setDscErrorCode(applicationProperties.getDSC_ERR_11());
			return getSuccessTokenInputResponse(bulkPKCSSignRequest, pojo, dataSignRequest.getRequestInfo());
		}
		else
		{
			System.out.println("File permission started for ::: "+tempFilePath);
			try {
				
				pdfSig.setExecutable(true, false);
				pdfSig.setReadable(true, false);
				pdfSig.setWritable(true, false);
				pdfSigPermissions = new HashSet<PosixFilePermission>();
				pdfSigPermissions.add(PosixFilePermission.OWNER_READ);
				pdfSigPermissions.add(PosixFilePermission.OWNER_WRITE);
				pdfSigPermissions.add(PosixFilePermission.OWNER_EXECUTE);
				pdfSigPermissions.add(PosixFilePermission.GROUP_READ);
				pdfSigPermissions.add(PosixFilePermission.GROUP_WRITE);
				pdfSigPermissions.add(PosixFilePermission.GROUP_EXECUTE);
				pdfSigPermissions.add(PosixFilePermission.OTHERS_READ);
				pdfSigPermissions.add(PosixFilePermission.OTHERS_WRITE);
				pdfSigPermissions.add(PosixFilePermission.OTHERS_EXECUTE);
				Files.setPosixFilePermissions(Paths.get(tempFilePath), pdfSigPermissions);
				System.out.println("File permission set successfully...");
			} catch (Exception e) {
				e.printStackTrace();
				pojo.setDscErrorCode(applicationProperties.getDSC_ERR_27());
				return getSuccessTokenInputResponse(bulkPKCSSignRequest, pojo, dataSignRequest.getRequestInfo());
			}
		}

		return getSuccessTokenInputResponse(bulkPKCSSignRequest, pojo, dataSignRequest.getRequestInfo());

	}

	private String getPdfBytes(String fileStoreId, String tenantId) throws DSCException {
		String pdfStr = null;

		InputStream unsignedFileStrm = null;
		File unsignedFile = fetchAsDigitPath(fileStoreId, tenantId).toFile();

		try {
			unsignedFileStrm = new FileInputStream(unsignedFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new DSCException(applicationProperties.getDSC_ERR_15());
		}
		byte[] pdfBytes = new byte[(int) unsignedFile.length()];
		try {
			unsignedFileStrm.read(pdfBytes, 0, pdfBytes.length);
		} catch (IOException e) {
			throw new DSCException(applicationProperties.getDSC_ERR_16());
		}
		try {
			unsignedFileStrm.close();
			if(unsignedFile != null) {
				unsignedFile.delete();
			}
		} catch (IOException e) {
			throw new DSCException(applicationProperties.getDSC_ERR_17());
		}
		if (pdfBytes != null) {
			pdfStr = Base64.encodeBase64String(pdfBytes);
			//System.out.println("pdfStr before sign:::" + pdfStr);
			System.out.println("pdfStr.length before sign:::::" + pdfStr.length());
		}
		return pdfStr;
	}

	@RequestMapping(value = "/_pdfSign", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<DataSignResponse> pdfSign(@RequestBody DataSignRequest dataSignRequest) {
		emBridge bridge = null;
		File logFile = new File(logPath);
		File licFile = new File(licPath);
		String tempFilePath = dataSignRequest.getTempFilePath();
		File tempSigFile=new File(tempFilePath);
		File tempFile = new File(tempPath);
		ResponseDataPKCSBulkSign apiResponse = null;
		String fileId = null;
		String result = "";
		dsc = false;
		try {
			if (!logFile.exists()) {
				logFile.mkdirs();
			}
			if (!licFile.exists()) {
				licFile.mkdirs();
			}
			if (!tempFile.exists()) {
				tempFile.mkdirs();
			}

			bridge = new emBridge(licPath + "/OdishaUrban.lic", logFile.getCanonicalPath());
		} catch (IOException e) {
			e.printStackTrace();
			return getSuccessDataSignResponse(result, fileId, dataSignRequest.getRequestInfo(),
					applicationProperties.getDSC_ERR_01(), null);
		}
		try {
			apiResponse = bridge.decPKCSBulkSign(dataSignRequest.getResponseData(), tempFilePath);
			System.out.println("apiResponse.getErrorCode() - " + apiResponse.getErrorCode());
		} catch (Exception e) {
			if(tempSigFile != null)
			{
				tempSigFile.delete();
			}
			e.printStackTrace();
			return getSuccessDataSignResponse(result, fileId, dataSignRequest.getRequestInfo(),
					applicationProperties.getDSC_ERR_18(), null);
		}
		if (apiResponse != null && apiResponse.getErrorCode() != null) {
			if(tempSigFile != null)
			{
				tempSigFile.delete();
			}
			return getSuccessDataSignResponse(result, fileId, dataSignRequest.getRequestInfo(), null,
					apiResponse.getErrorCode() + " - " + apiResponse.getErrorMsg());
		}
		try {
			fileId = populateSignedPdfFileStoreId(apiResponse, tempFilePath, dataSignRequest.getFileName(),
					dataSignRequest.getRequestInfo().getUserInfo().getId(), dataSignRequest.getTenantId(),
					dataSignRequest.getModuleName(), dataSignRequest.getChannelId());
		} catch (DSCException e) {
			if(tempSigFile != null)
			{
				tempSigFile.delete();
			}
			e.printStackTrace();
			if (dsc) {
				return getSuccessDataSignResponse(result, fileId, dataSignRequest.getRequestInfo(), e.getMessage(),
						null);
			} else {
				return getSuccessDataSignResponse(result, fileId, dataSignRequest.getRequestInfo(), null,
						e.getMessage());
			}

		}

		if (fileId != null && !fileId.isEmpty() && !fileId.equalsIgnoreCase("0")) {
			result = "Success";
			System.out.println("fileID - " + fileId);
			System.out.println("result - " + result);
			if(tempSigFile != null)
			{
				tempSigFile.delete();
			}
		} else {
			fileId = null;
			result = "Failure";
			if(tempSigFile != null)
			{
				tempSigFile.delete();
			}
			return getSuccessDataSignResponse(result, fileId, dataSignRequest.getRequestInfo(),
					applicationProperties.getDSC_ERR_26(), null);
		}
		return getSuccessDataSignResponse(result, fileId, dataSignRequest.getRequestInfo(), null, null);
	}

	private String populateSignedPdfFileStoreId(ResponseDataPKCSBulkSign apiResponse, String serverTempPath,
		String fileName, Long userId, String tenantId, String moduleName, String channelId) throws DSCException {
		String fileStoreId = "0";
		File file = null;
		File tempFile = new File(serverTempPath);
		boolean check = false;
		byte[] signedDocBytes = null;
		String finalFilePath = null;
		OutputStream os = null;

		if (apiResponse != null) {
			if (apiResponse.getBulkSignItems() != null && !apiResponse.getBulkSignItems().isEmpty()) {
				for (BulkSignOutput doc : apiResponse.getBulkSignItems()) {
					//System.out.println("pdfStr after sign before authentication ::::: " + doc.getSignedData());
					System.out.println("PDF sign completed, Authentication pending::::: ");
					try {
						check = checkPdfAuthentication(String.valueOf(userId), doc.getSignedData(), channelId);
					} catch (DSCException e) {
						throw new DSCException(e.getMessage());
					}

					if (!check) {
						break;
					}
					try {
						signedDocBytes = Base64.decodeBase64(doc.getSignedData());
						finalFilePath = tempPath + File.separatorChar + fileName + "_signed.pdf";
						file = new File(finalFilePath);
						os = new FileOutputStream(file);
						os.write(signedDocBytes);
						os.close();
					} catch (Exception e) {
						try {
							if(file != null)
							{
								file.delete();
							}
						} catch (Exception ex) {
							dsc = true;
							ex.printStackTrace();
							throw new DSCException(applicationProperties.getDSC_ERR_24());
						}
						dsc = true;
						e.printStackTrace();
						throw new DSCException(applicationProperties.getDSC_ERR_23());
					}

				}
			} else {
				dsc = true;
				throw new DSCException(applicationProperties.getDSC_ERR_22());
			}

		}

		if (file != null) {
			try {
				fileStoreId = store(new FileInputStream(file), file.getName(), "application/pdf", moduleName, true,
						tenantId);
			} catch (Exception e) {
				try {
					file.delete();
				} catch (Exception ex) {
					dsc = true;
					ex.printStackTrace();
					throw new DSCException(applicationProperties.getDSC_ERR_24());
				}
				dsc = true;
				e.printStackTrace();
				throw new DSCException(applicationProperties.getDSC_ERR_21());
			}
			try {
				file.delete();
			} catch (Exception e) {
				dsc = true;
				e.printStackTrace();
				throw new DSCException(applicationProperties.getDSC_ERR_24());
			}

		}
		try {
			//tempFile.delete(); //commented for testing purpose on dev
		} catch (Exception e) {
			dsc = true;
			e.printStackTrace();
			throw new DSCException(applicationProperties.getDSC_ERR_25());
		}

		return fileStoreId;
	}

	public String store(InputStream fileStream, String fileName, String mimeType, String moduleName,
			boolean closeStream, String tenantId) throws DSCException {
		String fileStoreId = null;
		try {
			byte[] fileSize = fileName.getBytes();
			DiskFileItem fileItem = new DiskFileItem("file", mimeType, false, fileName, fileSize.length, null);
			OutputStream os = fileItem.getOutputStream();
			int ret = fileStream.read();
			while (ret != -1) {
				os.write(ret);
				ret = fileStream.read();
			}
			os.flush();
			MultipartFile multipartFile = new CommonsMultipartFile(fileItem);
			fileStoreId = storeFiles(Arrays.asList(multipartFile), fileName, mimeType, moduleName, false, tenantId);
		} catch (Exception e) {
			throw new DSCException(applicationProperties.getDSC_ERR_21());
		}
		return fileStoreId;
	}

	private String storeFiles(List<MultipartFile> files, String fileName, String mimeType, String moduleName,
			boolean deleteFile, String tenantId) throws DSCException {
		String fileStoreId = null;
		try {
			StorageResponse storageRes = getFileStorageService(files, moduleName, tenantId);

			List<FileReq> filesList = storageRes.getFiles();
			for (FileReq filesId : filesList) {
				fileStoreId = filesId.getFileStoreId();
			}
		} catch (Exception e) {
			throw new DSCException(applicationProperties.getDSC_ERR_21());
		}
		return fileStoreId;
	}

	public StorageResponse getFileStorageService(final List<MultipartFile> files, String modulename, String tenantId)
			throws IOException {
		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_UTF8_VALUE);
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);

		MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
		map.add("module", modulename);
		map.add("tenantId", tenantId);

		ByteArrayResource contentsAsResource = null;
		for (MultipartFile file : files) {
			contentsAsResource = new ByteArrayResource(file.getBytes()) {
				@Override
				public String getFilename() {
					return file.getOriginalFilename();
				}
			};
		}
		;
		map.add("file", contentsAsResource);
		// map.add("file", contentsAsResource);

		StringBuilder uri = new StringBuilder(applicationProperties.getEgovFileStoreSerHost())
				.append(applicationProperties.getEgovFileStoreUploadFile());

		HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(map, headers);

		return restTemplate.postForObject(uri.toString(), request, StorageResponse.class);
	}

	private boolean checkPdfAuthentication(String userId, String signedData, String channelId) throws DSCException {
		boolean result = false;
		DSAuthenticateWS authenticateWS = new DSAuthenticateWSProxy(applicationProperties.getEmasWsUrl());
		String authenticatePDF = null;
		String uniqueId = userId + "~" + channelId;
		try {
			authenticatePDF = authenticateWS.authenticatePDF(uniqueId, signedData, null, "authenticate");
		} catch (RemoteException e) {
			e.printStackTrace();
			dsc = true;
			throw new DSCException(applicationProperties.getDSC_ERR_19());
		}
		System.out.println("authenticatePDF:: " + authenticatePDF);

		if (authenticatePDF != null && !authenticatePDF.isEmpty()
				&& (authenticatePDF.contains("Success") || authenticatePDF.contains("success"))) {
			result = true;
		} else if (authenticatePDF != null && !authenticatePDF.isEmpty()) {
			dsc = false;
			throw new DSCException(authenticatePDF);
		}
		return result;
	}

	private Path fetchAsDigitPath(String fileStoreId, String tenantId) throws DSCException {
		ResponseEntity<byte[]> responseEntity = null;
		Path fileDirPath = null;
		Path path = null;
		try {
			responseEntity = fetchFilesFromDigitService(fileStoreId, tenantId);
//			fileDirPath = Paths.get(fileStoreId);
			fileDirPath = Paths.get(tempFilePath+fileStoreId);
			path = Files.write(fileDirPath, responseEntity.getBody());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DSCException(applicationProperties.getDSC_ERR_12());
		}
		return path;

	}

	public ResponseEntity<byte[]> fetchFilesFromDigitService(String fileStoreId, String tenantId) throws Exception {
		Map<String, String> request = new HashMap<String, String>();
		request.put("tenantId", tenantId);
		request.put("fileStoreId", fileStoreId);

		StringBuilder uri = new StringBuilder(applicationProperties.getEgovFileStoreSerHost())
				.append(applicationProperties.getEgovFileStoreDownloadFile()).append("?");

		if (tenantId != null && !tenantId.isEmpty()) {
			uri.append("tenantId=").append(tenantId);
		}
		if (fileStoreId != null && !fileStoreId.isEmpty()) {
			uri.append("&fileStoreId=").append(fileStoreId);
		}
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM));
		HttpEntity<String> entity = new HttpEntity<>(headers);
		ResponseEntity<byte[]> response = restTemplate.exchange(uri.toString(), HttpMethod.GET, entity, byte[].class);
		return response;
	}
	
	
	/**
	 * DataSign Deregister API
	 * @param dataSignRequest
	 * @return Deregister response
	 */
	@RequestMapping(value = "/_dataSignDeregister", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<DataSignResponse> dataSignDeregister(@RequestBody DataSignRequest dataSignRequest) {
		String response = "";
		String emudhraErrorCode = "";
		// deregister
		try {
			
			response = populateDeregisterSoapCall(dataSignRequest.getUserId(), dataSignRequest.getChannelId());
			if (!(response.toLowerCase().contains("success"))) {
				emudhraErrorCode = response;
			}

		} catch (DSCException e) {
			e.printStackTrace();
			return getSuccessDataSignDeregisterResponse(response, dataSignRequest.getRequestInfo(),
					applicationProperties.getDSC_ERR_29(), null);
		}

		return getSuccessDataSignDeregisterResponse(response, dataSignRequest.getRequestInfo(), null, emudhraErrorCode);

	}
	
	
	/**
	 * 
	 * @param id
	 * @param channel
	 * @return Result after deregistration
	 * @throws DSCException
	 */
	private String populateDeregisterSoapCall(Long userId, String channel) throws DSCException {
		String result = "";
		DSAuthenticateWS authenticateWS = new DSAuthenticateWSProxy(applicationProperties.getEmasWsUrl());
		try {
			String response = authenticateWS.userExists(userId + "~" + channel);
			if (response.toLowerCase().contains("success")) {
				result = authenticateWS.deregister(userId + "~" + channel, "degistration");
				System.out.println("result after deregistration ::::" + result);
			} else {
				throw new DSCException("Error in deregistration api from Emas" + response);
			}
		} catch (RemoteException e) {
			e.printStackTrace();
			throw new DSCException("Error in deregistration api from Emas");
		}
		return result;
	}
	
	
	private ResponseEntity<DataSignResponse> getSuccessDataSignDeregisterResponse(String responseString,
			RequestInfo requestInfo, String dscErrorCode, String emudhraErrorCode) {
		final ResponseInfo responseInfo = ResponseInfoFactory.createResponseInfoFromRequestInfo(requestInfo, true);
		responseInfo.setStatus(HttpStatus.OK.toString());

		DataSignResponse datasign = new DataSignResponse(responseInfo, responseString, null, dscErrorCode,
				emudhraErrorCode);
		return new ResponseEntity<>(datasign, HttpStatus.OK);
	}
}
