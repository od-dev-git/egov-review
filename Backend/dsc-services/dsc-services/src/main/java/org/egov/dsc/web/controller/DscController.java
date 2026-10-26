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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.validation.Valid;

import org.apache.commons.codec.binary.Base64;
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
import org.egov.dsc.model.SignedPdfResult;
import org.egov.dsc.model.StorageResponse;
import org.egov.dsc.model.TokenInputResponse;
import org.egov.dsc.model.TokenRequest;
import org.egov.dsc.model.TokenResponse;
import org.egov.dsc.service.PdfSigningArtifactStore;
import org.egov.dsc.web.contract.factory.ResponseInfoFactory;
import org.egov.tracer.model.CustomException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

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
	private static final Logger log = LoggerFactory.getLogger(DscController.class);
	private static final String DEFAULT_PDF_SIGNATURE_COORDINATES = "All-215,32,325,72";
	private static final String LICENSE_FILE_PATH = "/OdishaUrban.lic";
	private static final String EMAS_STATUS_SUCCESS = "Success";

	@PostConstruct
	public void initDirectoriesAndValidateLicense() throws Exception {
		File logDir = new File(logPath);
		File licDir = new File(licPath);

		if (!logDir.exists() && !logDir.mkdirs()) {
			log.warn("Failed to create log directory: {}", logPath);
		}
		if (!licDir.exists() && !licDir.mkdirs()) {
			log.warn("Failed to create license directory: {}", licPath);
		}
		File licenseFile = new File(licPath + LICENSE_FILE_PATH);
		if (!licenseFile.exists()) {
			log.error("CRITICAL: License file not found at path: {}", licenseFile.getAbsolutePath());
			throw new FileNotFoundException("Required license file missing: " + licenseFile.getAbsolutePath());
		}

		log.info("Successfully initialized directories and verified license file.");
	}

	@Autowired
	private ApplicationProperties applicationProperties;

	@Autowired
	private PdfSigningArtifactStore pdfSigningArtifactStore;

	@Autowired
	private RestTemplate restTemplate;

	private String logPath = System.getProperty("user.dir") + "/DS/Log";
	private String licPath = System.getProperty("user.dir") + "/" + "DS/Lic";

	
	private final java.util.Queue<emBridge> bridgePool = new java.util.concurrent.ConcurrentLinkedQueue<>();

	private emBridge acquireBridge() throws IOException {
		emBridge bridge = bridgePool.poll();
		if (bridge != null) {
			return bridge;
		}
		File logFile = new File(logPath);
		return new emBridge(licPath + LICENSE_FILE_PATH, logFile.getCanonicalPath());
	}

	private void releaseBridge(emBridge bridge) {
		if (bridge != null) {
			bridgePool.offer(bridge);
		}
	}

	private void cleanupPdfSigningSession(String sigId, String correlationId) {
		if (sigId == null) {
			return;
		}
		try {
			pdfSigningArtifactStore.deleteSession(sigId);
		} catch (IOException e) {
			log.warn("[correlationId={}] Failed to delete PDF signing session", correlationId, e);
		}
	}

	@GetMapping(value = "/_getCheck")
	public String test() {
		return applicationProperties.getEmasWsUrl();
	}

	@RequestMapping(value = "/_getTokenInput", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<TokenInputResponse> token(@Valid @RequestBody TokenRequest tokenRequest) {
		RequestInfo requestInfo = tokenRequest.getRequestInfo();
		String correlationId = (requestInfo != null) ? requestInfo.getMsgId() : "N/A";
		emBridge bridge = null;
		ListTokenRequest listTokenRequest = new ListTokenRequest();
		Request data = null;
		DetailRequestPojo input = new DetailRequestPojo();
		try {
			bridge = acquireBridge();
			listTokenRequest.setTokenStatus(Token_Status.CONNECTED);
			listTokenRequest.setTokenType(Token_Type.HARDWARE);
		} catch (IOException e) {
			input.setDscErrorCode(applicationProperties.getDSC_ERR_01());
			log.error("Failed to initialize DSC bridge for getTokenInput operation for correlationId={}", correlationId, e);
			return getSuccessTokenInputResponse(data, input, tokenRequest.getRequestInfo());
		}

		try {
			data = bridge.encListToken(listTokenRequest);
			log.debug("[correlationId={}] Data Error Code: {}", correlationId, data.getErrorCode());
			log.debug("[correlationId={}] Encrypted Data: {}", correlationId, data.getEncryptedData());
			log.debug("[correlationId={}] Encrypted Key ID: {}", correlationId, data.getEncryptionKeyID());
		} catch (Exception e) {
			input.setDscErrorCode(applicationProperties.getDSC_ERR_02());
			log.error("Token input encryption failed for correlationId={}", correlationId, e);
			return getSuccessTokenInputResponse(data, input, tokenRequest.getRequestInfo());
		} finally {
			releaseBridge(bridge);
		}
		return getSuccessTokenInputResponse(data, input, tokenRequest.getRequestInfo());

	}

	@RequestMapping(value = "/_getTokens", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<TokenResponse> tokens(@Validated(TokenRequest.Decrypt.class) @RequestBody TokenRequest tokenRequest) {
		RequestInfo requestInfo = tokenRequest.getRequestInfo();
		String correlationId = (requestInfo != null) ? requestInfo.getMsgId() : "N/A";
		List<String> tokens = new ArrayList<String>();
		emBridge bridge = null;
		ResponseDataListProviderToken responseDataListProviderToken = null;
		String emudhraErrorCode = "";
		try {
			bridge = acquireBridge();
		} catch (IOException e) {
			log.error("Failed to initialize DSC bridge for getTokens operation for correlationId={}", correlationId, e);
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
						log.debug("[correlationId={}] Token: {}", correlationId, token.getKeyStoreDisplayName());
						tokens.add(token.getKeyStoreDisplayName());
					}
				}
			}

		} catch (Exception e) {
			log.error("Token list decryption failed for correlationId={}", correlationId, e);
			return getSuccessTokenResponse(tokens, tokenRequest.getRequestInfo(), applicationProperties.getDSC_ERR_13(),
					null);
		} finally {
			releaseBridge(bridge);
		}

		return getSuccessTokenResponse(tokens, tokenRequest.getRequestInfo(), null, emudhraErrorCode);

	}

	@RequestMapping(value = "/_getInputCertificate", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<TokenInputResponse> cert(@Valid @RequestBody CertificateRequest certificateRequest) {
		RequestInfo requestInfo = certificateRequest.getRequestInfo();
		String correlationId = (requestInfo != null) ? requestInfo.getMsgId() : "N/A";
		emBridge bridge = null;
		ListCertificateRequest listCertRequest = new ListCertificateRequest();
		DetailRequestPojo input = new DetailRequestPojo();
		Request data = null;
		try {
			bridge = acquireBridge();
			listCertRequest.setKeyStoreDisplayName(certificateRequest.getTokenDisplayName());// Microsoft Windows
																								// Store//ePass V
		} catch (IOException e) {
			input.setDscErrorCode(applicationProperties.getDSC_ERR_01());
			log.error("Failed to initialize DSC bridge for getInputCertificate operation for correlationId={}", correlationId, e);
			return getSuccessTokenInputResponse(data, input, certificateRequest.getRequestInfo());
		}
		try {
			data = bridge.encListCertificate(listCertRequest);
			log.debug("[correlationId={}] Encrypted Data: {}", correlationId, data.getEncryptedData());
			log.debug("[correlationId={}] Encrypted Key ID: {}", correlationId, data.getEncryptionKeyID());
		} catch (Exception e) {
			input.setDscErrorCode(applicationProperties.getDSC_ERR_03());
			log.error("Certificate input encryption failed for correlationId={}", correlationId, e);
			return getSuccessTokenInputResponse(data, input, certificateRequest.getRequestInfo());
		} finally {
			releaseBridge(bridge);
		}
		return getSuccessTokenInputResponse(data, input, certificateRequest.getRequestInfo());
	}

	@RequestMapping(value = "/_getCertificate", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<CertificateResponse> certa(@Validated(CertificateRequest.Decrypt.class) @RequestBody CertificateRequest certificateRequest) {
		RequestInfo requestInfo = certificateRequest.getRequestInfo();
		String correlationId = (requestInfo != null) ? requestInfo.getMsgId() : "N/A";
		emBridge bridge = null;
		ResponseDataListPKCSCertificate responseDataListPKCSCertificate = null;
		List<PKCSCertificate> certificates = null;
		List<CertificateResponsePojo> certificatesList = null;
		CertificateResponsePojo certificate = null;
		String emudhraErrorCode = "";
		try {
			bridge = acquireBridge();
		} catch (IOException e) {
			log.error("Failed to initialize DSC bridge for getCertificate operation for correlationId={}", correlationId, e);
			return getSuccessCertResponse(certificatesList, certificateRequest.getRequestInfo(),
					applicationProperties.getDSC_ERR_01(), null);
		}
		try {
			try {
				responseDataListPKCSCertificate = bridge.decListCertificate(certificateRequest.getResponseData(), null);
			} catch (Exception e) {
				log.error("Certificate list decryption failed for correlationId={}", correlationId, e);
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
							log.debug("[correlationId={}] Certificate Key ID: {}", correlationId, cert.getKeyId());
							log.debug("[correlationId={}] Certificate Common Name: {}", correlationId, cert.getCommonName());
							certificate = new CertificateResponsePojo();
							certificate.setKeyId(cert.getKeyId());
							certificate.setCommonName(cert.getCommonName());
							certificate.setCertificateDate(cert.getCertificateData());
							certificatesList.add(certificate);
						}
					}
				}
			} catch (Exception e) {
				log.error("Error processing certificate response for correlationId={}", correlationId, e);
				return getSuccessCertResponse(certificatesList, certificateRequest.getRequestInfo(),
						applicationProperties.getDSC_ERR_14(), null);
			}
		} finally {
			releaseBridge(bridge);
		}
		return getSuccessCertResponse(certificatesList, certificateRequest.getRequestInfo(), null, emudhraErrorCode);

	}

	@RequestMapping(value = "/_dataSignInput", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<TokenInputResponse> dataSignInput(@Validated(DataSignRequest.DataSignInput.class) @RequestBody DataSignRequest dataSignRequest) {
		RequestInfo requestInfo = dataSignRequest.getRequestInfo();
		// RequestInfo.userInfo.id is guaranteed by DataSignRequest validation
		Long userId = requestInfo.getUserInfo().getId();
		String correlationId = (requestInfo != null) ? requestInfo.getMsgId() : "N/A";

		emBridge bridge = null;
		String randomNumber = "";
		DetailRequestPojo input = new DetailRequestPojo();
		PKCSSignRequest pKCSSignRequest = new PKCSSignRequest();
		Request data = null;
		try {
			bridge = acquireBridge();
			pKCSSignRequest.setKeyStoreDisplayName(dataSignRequest.getTokenDisplayName());// token
			pKCSSignRequest.setKeyStorePassphrase(dataSignRequest.getKeyStorePassPhrase());
			pKCSSignRequest.setKeyId(dataSignRequest.getKeyId());
			pKCSSignRequest.setDataType(ContentType.TextPKCS7ATTACHED);
		} catch (IOException e) {
			input.setDscErrorCode(applicationProperties.getDSC_ERR_01());
			log.error("Directory initialization failed in dataSign for correlationId={}", correlationId, e);
			return getSuccessTokenInputResponse(data, input, dataSignRequest.getRequestInfo());
		}

		try {
			// emas random number execute
			try {
				randomNumber = populateRandom(userId,
						dataSignRequest.getChannelId(), correlationId);
				if (randomNumber.contains("~")) {
					randomNumber = randomNumber.split("~")[1];
				} else {
					input.setEmudhraErrorCode(randomNumber);
					return getSuccessTokenInputResponse(data, input, dataSignRequest.getRequestInfo());
				}
				pKCSSignRequest.setDataToSign(userId + "~" + randomNumber);
				pKCSSignRequest.setTimeStamp("");
			} catch (DSCException e) {
				input.setDscErrorCode(applicationProperties.getDSC_ERR_06());
				return getSuccessTokenInputResponse(data, input, dataSignRequest.getRequestInfo());
			}

			try {
				data = bridge.encPKCSSign(pKCSSignRequest);
				log.debug("[correlationId={}] Encrypted Data: {}", correlationId, data.getEncryptedData());
				log.debug("[correlationId={}] Encrypted Key ID: {}", correlationId, data.getEncryptionKeyID());
			} catch (Exception e) {
				input.setDscErrorCode(applicationProperties.getDSC_ERR_05());
				log.error("PKCS encryption failed during emBridge call for correlationId={}", correlationId, e);
				return getSuccessTokenInputResponse(data, input, dataSignRequest.getRequestInfo());
			}
		} finally {
			releaseBridge(bridge);
		}
		return getSuccessTokenInputResponse(data, input, dataSignRequest.getRequestInfo());

	}

	@RequestMapping(value = "/_dataSign", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<DataSignResponse> dataSign(@Validated(DataSignRequest.DataSign.class) @RequestBody DataSignRequest dataSignRequest) {
		RequestInfo requestInfo = dataSignRequest.getRequestInfo();
		// RequestInfo.userInfo.id is guaranteed by DataSignRequest validation
		Long userId = requestInfo.getUserInfo().getId();
		String correlationId = (requestInfo != null) ? requestInfo.getMsgId() : "N/A";

		emBridge bridge = null;
		String response = "";
		ResponseDataPKCSSign responseDataPKCSSign = null;
		try {
			bridge = acquireBridge();
		} catch (IOException e) {
			log.error("[correlationId={}] Directory initialization failed in dataSign", correlationId, e);
			return getErrorDataSignResponse(response, null, dataSignRequest.getRequestInfo(),
					applicationProperties.getDSC_ERR_01(), null, HttpStatus.INTERNAL_SERVER_ERROR);
		}

		try {
			try {
				responseDataPKCSSign = bridge.decPKCSSign(dataSignRequest.getResponseData());
			} catch (EMBLException e) {
				log.error("[correlationId={}] PKCS decryption failed during emBridge call", correlationId, e);
				return getErrorDataSignResponse(response, null, dataSignRequest.getRequestInfo(),
						applicationProperties.getDSC_ERR_07(), null, HttpStatus.BAD_REQUEST);
			}
			if (responseDataPKCSSign == null || responseDataPKCSSign.getSignedText() == null
					|| responseDataPKCSSign.getSignedText().trim().isEmpty()) {
				log.error("[correlationId={}] emBridge returned no signed text", correlationId);
				return getErrorDataSignResponse(response, null, dataSignRequest.getRequestInfo(),
						applicationProperties.getDSC_ERR_07(), null, HttpStatus.BAD_GATEWAY);
			}
			// register
			try {
				response = populateRegisterSoapCall(userId, dataSignRequest.getChannelId(),
						responseDataPKCSSign.getSignedText(), correlationId);
				if (!isEmasSuccess(response)) {
					// eMAS rejected the registration; its reply is the eMudhra error code
					return getErrorDataSignResponse(response, null, dataSignRequest.getRequestInfo(),
							null, response, HttpStatus.BAD_REQUEST);
				}

			} catch (DSCException e) {
				log.error("[correlationId={}] eMAS register call failed in dataSign", correlationId, e);
				return getErrorDataSignResponse(response, null, dataSignRequest.getRequestInfo(),
						applicationProperties.getDSC_ERR_08(), null, HttpStatus.BAD_GATEWAY);
			}
		} finally {
			releaseBridge(bridge);
		}

		return buildDataSignResponse(response, null, dataSignRequest.getRequestInfo(), null, null);

	}

	private String populateRegisterSoapCall(Long id, String channel, String signedText, String correlationId) throws DSCException {
		String result = "";
		DSAuthenticateWS authenticateWS = new DSAuthenticateWSProxy(applicationProperties.getEmasWsUrl());
		try {
			result = authenticateWS.register(id + "~" + channel, signedText, "registration", "registration", true);
			log.debug("[correlationId={}] eMAS register result: {}", correlationId, result);
		} catch (RemoteException e) {
			log.error("Register API call failed for correlationId={}", correlationId, e);
			throw new DSCException("Error in register api from Emas");
		}
		return result;
	}

	private String populateRandom(Long id, String channel, String correlationId) throws DSCException {
		String result = "";
		DSAuthenticateWS authenticateWS = null;
		try {
			authenticateWS = new DSAuthenticateWSProxy(applicationProperties.getEmasWsUrl());
			result = authenticateWS.generateRandomNumber(id + "~" + channel);
			log.debug("[correlationId={}] eMAS random number result: {}", correlationId, result);
		} catch (RemoteException e) {
			log.error("Random number generation failed for correlationId={}", correlationId, e);
			throw new DSCException("Error in populating random number from Emas");
		}
		return result;
	}

	private ResponseEntity<TokenResponse> getSuccessTokenResponse(List<String> tokens, RequestInfo requestInfo,
			String dscErrorCode, String emudhraErrorCode) {
		HttpStatus status = hasError(dscErrorCode, emudhraErrorCode) ? HttpStatus.BAD_GATEWAY : HttpStatus.OK;
		final ResponseInfo responseInfo = ResponseInfoFactory.createResponseInfoFromRequestInfo(requestInfo,
				status == HttpStatus.OK);
		responseInfo.setStatus(status.toString());

		TokenResponse tokenResponse = new TokenResponse(responseInfo, tokens, dscErrorCode, emudhraErrorCode);
		return new ResponseEntity<>(tokenResponse, status);
	}

	private ResponseEntity<TokenInputResponse> getSuccessTokenInputResponse(Request data, DetailRequestPojo input,
			RequestInfo requestInfo) {
		if (data != null && data.getEncryptedData() != null) {
			input.setEncryptedRequest(data.getEncryptedData());
		}
		if (data != null && data.getEncryptionKeyID() != null) {
			input.setEncryptionKeyId(data.getEncryptionKeyID());
		}
		if (data != null && hasError(data.getErrorCode())) {
			input.setEmudhraErrorCode(data.getErrorCode() + " - " + data.getErrorMsg());
		}
		HttpStatus status = hasError(input.getDscErrorCode(), input.getEmudhraErrorCode())
				? HttpStatus.BAD_GATEWAY : HttpStatus.OK;
		final ResponseInfo responseInfo = ResponseInfoFactory.createResponseInfoFromRequestInfo(requestInfo,
				status == HttpStatus.OK);
		responseInfo.setStatus(status.toString());
		TokenInputResponse tokenResponse = new TokenInputResponse(responseInfo, input);
		return new ResponseEntity<>(tokenResponse, status);
	}

	private ResponseEntity<CertificateResponse> getSuccessCertResponse(List<CertificateResponsePojo> certs,
			RequestInfo requestInfo, String dscErrorCode, String emudhraErrorCode) {
		HttpStatus status = hasError(dscErrorCode, emudhraErrorCode) ? HttpStatus.BAD_GATEWAY : HttpStatus.OK;
		final ResponseInfo responseInfo = ResponseInfoFactory.createResponseInfoFromRequestInfo(requestInfo,
				status == HttpStatus.OK);
		responseInfo.setStatus(status.toString());

		CertificateResponse certificateResponse = new CertificateResponse(responseInfo, certs, dscErrorCode,
				emudhraErrorCode);
		return new ResponseEntity<>(certificateResponse, status);
	}

	ResponseEntity<DataSignResponse> buildDataSignResponse(String responseString, String fileStoreId,
			RequestInfo requestInfo, String dscErrorCode, String emudhraErrorCode) {
		HttpStatus status = hasError(dscErrorCode, emudhraErrorCode) ? HttpStatus.BAD_GATEWAY : HttpStatus.OK;
		final ResponseInfo responseInfo = ResponseInfoFactory.createResponseInfoFromRequestInfo(requestInfo,
				status == HttpStatus.OK);
		responseInfo.setStatus(status.toString());

		DataSignResponse datasign = new DataSignResponse(responseInfo, responseString, fileStoreId, dscErrorCode,
				emudhraErrorCode);
		return new ResponseEntity<>(datasign, status);
	}

	private boolean hasError(String... values) {
		for (String value : values) {
			if (value != null && !value.trim().isEmpty()) {
				return true;
			}
		}
		return false;
	}
	
	private ResponseEntity<DataSignResponse> getErrorDataSignResponse(String responseString, String fileStoreId,
			RequestInfo requestInfo, String dscErrorCode, String emudhraErrorCode, HttpStatus httpStatus) {
		final ResponseInfo responseInfo = ResponseInfoFactory.createResponseInfoFromRequestInfo(requestInfo, false);
		responseInfo.setStatus(httpStatus.toString());

		DataSignResponse datasign = new DataSignResponse(responseInfo, responseString, fileStoreId, dscErrorCode,
				emudhraErrorCode);
		return new ResponseEntity<>(datasign, httpStatus);
	}

	@RequestMapping(value = "/_pdfSignInput", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<TokenInputResponse> dpdfSignInput(@Validated(DataSignRequest.PdfSignInput.class) @RequestBody DataSignRequest dataSignRequest) {
		RequestInfo requestInfo = dataSignRequest.getRequestInfo();
		String correlationId = (requestInfo != null) ? requestInfo.getMsgId() : "N/A";
		emBridge bridge = null;
		String pdfStr = "";
		List<emBridgeSignerInput> inputs = new ArrayList<>();
		PKCSBulkPdfHashSignRequest pKCSBulkPdfHashSignRequest = new PKCSBulkPdfHashSignRequest();
		Request bulkPKCSSignRequest = null;
		String tempFilePath = null;
		String sigId = null;
		boolean keepSigSession = false;
		DetailRequestPojo pojo = new DetailRequestPojo();
		File pdfSig = null;
		try {
			bridge = acquireBridge();
		} catch (IOException e) {
			pojo.setDscErrorCode(applicationProperties.getDSC_ERR_01());
			log.error("Failed to initialize DSC bridge for pdfSignInput operation for correlationId={}", correlationId, e);
			return getSuccessTokenInputResponse(bulkPKCSSignRequest, pojo, dataSignRequest.getRequestInfo());
		}

		try {
			try {
				pdfStr = getPdfBytes(dataSignRequest.getFileBytes(), dataSignRequest.getTenantId(), correlationId);
			} catch (DSCException e) {
				pojo.setDscErrorCode(e.getMessage());
				log.error("Failed to fetch PDF bytes for pdfSignInput operation for correlationId={}", correlationId, e);
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
						dataSignRequest.getRequestInfo().getUserInfo().getName(), true, DEFAULT_PDF_SIGNATURE_COORDINATES,
						applicationProperties.getPdfProprty4(), false);
				inputs.add(input);
				pKCSBulkPdfHashSignRequest.setBulkInput(inputs);
				sigId = pdfSigningArtifactStore.createSession(
						String.valueOf(dataSignRequest.getRequestInfo().getUserInfo().getId()),
						dataSignRequest.getTenantId(), dataSignRequest.getChannelId());
				pKCSBulkPdfHashSignRequest.setTempFolder(
						pdfSigningArtifactStore.getSessionDirectory(sigId).toFile().getCanonicalPath());
				pKCSBulkPdfHashSignRequest.setKeyStoreDisplayName(dataSignRequest.getTokenDisplayName());
				pKCSBulkPdfHashSignRequest.setKeyStorePassphrase(dataSignRequest.getKeyStorePassPhrase());
				pKCSBulkPdfHashSignRequest.setKeyId(dataSignRequest.getKeyId());
			} catch (IOException e) {
				pojo.setDscErrorCode(applicationProperties.getDSC_ERR_09());
				log.error("Failed to prepare PDF signing input for correlationId={}", correlationId, e);
				return getSuccessTokenInputResponse(bulkPKCSSignRequest, pojo, dataSignRequest.getRequestInfo());
			}

			// 1 = single attempt (no retry); values below 1 are treated as 1
			int maxAttempts = Math.max(1, applicationProperties.getSigMaxAttempts());
			long backoffMillis = Math.max(0, applicationProperties.getSigInitialBackoffMs());
			long maxBackoffMillis = Math.max(backoffMillis, applicationProperties.getSigMaxBackoffMs());
			int attempt = 0;

			try {
				while (++attempt <= maxAttempts) {
					log.debug("[correlationId={}] Executing embridge call - attempt {}", correlationId, attempt);
					bulkPKCSSignRequest = bridge.encPKCSBulkSign(pKCSBulkPdfHashSignRequest);

					if (bulkPKCSSignRequest != null && bulkPKCSSignRequest.getTempFilePath() != null) {
						tempFilePath = pdfSigningArtifactStore
								.validateGeneratedSig(sigId, bulkPKCSSignRequest.getTempFilePath()).toString();
						pdfSig = new File(tempFilePath);

						if (pdfSig.exists()) {
							break;
						}
					}

					if (attempt < maxAttempts) {
						log.warn("[correlationId={}] .sig not created on attempt {}/{}, retrying in {} ms", correlationId,
								attempt, maxAttempts, backoffMillis);
						try {
							Thread.sleep(backoffMillis);
							backoffMillis = Math.min(backoffMillis * 2, maxBackoffMillis);
						} catch (InterruptedException ie) {
							Thread.currentThread().interrupt();
							break;
						}
					}
				}

				if (pdfSig == null || !pdfSig.exists()) {
					log.error("[correlationId={}] .sig not created after {} attempt(s)", correlationId,
							Math.min(attempt, maxAttempts));
					pojo.setDscErrorCode(applicationProperties.getDSC_ERR_28());
					return getSuccessTokenInputResponse(bulkPKCSSignRequest, pojo, dataSignRequest.getRequestInfo());
				}

				log.debug("[correlationId={}] Bulk Sign Encrypted Data: {}", correlationId, bulkPKCSSignRequest.getEncryptedData());
				log.debug("[correlationId={}] Bulk Sign Encrypted Key ID: {}", correlationId, bulkPKCSSignRequest.getEncryptionKeyID());
			} catch (Exception e) {
				pojo.setDscErrorCode(applicationProperties.getDSC_ERR_10());
				log.error("Bulk PDF sign request failed for correlationId={}", correlationId, e);
				return getSuccessTokenInputResponse(bulkPKCSSignRequest, pojo, dataSignRequest.getRequestInfo());
			}

			pojo.setFileName(dataSignRequest.getFileName());
			if (tempFilePath == null || !tempFilePath.endsWith(".sig")) {
				pojo.setDscErrorCode(applicationProperties.getDSC_ERR_11());
				return getSuccessTokenInputResponse(bulkPKCSSignRequest, pojo, dataSignRequest.getRequestInfo());
			}
			try {
				Files.setPosixFilePermissions(pdfSig.toPath(),
						EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
			} catch (Exception e) {
				log.error("Failed to set signed PDF file permissions for correlationId={}", correlationId, e);
				pojo.setDscErrorCode(applicationProperties.getDSC_ERR_27());
				return getSuccessTokenInputResponse(bulkPKCSSignRequest, pojo, dataSignRequest.getRequestInfo());
			}

			pojo.setSigId(sigId);
			keepSigSession = true;
			return getSuccessTokenInputResponse(bulkPKCSSignRequest, pojo, dataSignRequest.getRequestInfo());
		} finally {
			if (sigId != null && !keepSigSession) {
				try {
					pdfSigningArtifactStore.deleteSession(sigId);
				} catch (IOException e) {
					log.warn("[correlationId={}] Failed to clean up PDF signing session", correlationId, e);
				}
			}
			releaseBridge(bridge);
		}
	}

	/**
	 * Downloads the unsigned PDF from DIGIT filestore. Kept in memory only - no temp
	 * file, so nothing can leak on disk and the client-supplied fileStoreId never
	 * becomes part of a filesystem path.
	 */
	private byte[] fetchUnsignedPdfBytes(String fileStoreId, String tenantId, String correlationId)
			throws DSCException {
		ResponseEntity<byte[]> responseEntity;
		try {
			responseEntity = fetchFilesFromDigitService(fileStoreId, tenantId);
		} catch (Exception e) {
			log.error("[correlationId={}] Failed to fetch file from DIGIT for fileStoreId={}", correlationId,
					fileStoreId, e);
			throw new DSCException(applicationProperties.getDSC_ERR_12());
		}

		byte[] pdfBytes = (responseEntity != null) ? responseEntity.getBody() : null;
		if (pdfBytes == null || pdfBytes.length == 0) {
			log.error("[correlationId={}] Empty file received from DIGIT for fileStoreId={}", correlationId,
					fileStoreId);
			throw new DSCException(applicationProperties.getDSC_ERR_15());
		}
		return pdfBytes;
	}

	/**
	 * Encodes raw PDF byte array to Base64 String (CPU-bound operation).
	 */
	private String encodeToBase64(byte[] pdfBytes, String correlationId) {
		if (pdfBytes == null || pdfBytes.length == 0) {
			return null;
		}
		String pdfStr = Base64.encodeBase64String(pdfBytes);
		log.debug("[correlationId={}] pdfStr.length before sign: {}", correlationId, pdfStr.length());
		return pdfStr;
	}

	/**
	 * Main coordinator method.
	 */
	private String getPdfBytes(String fileStoreId, String tenantId, String correlationId) throws DSCException {
		try {
			byte[] pdfBytes = fetchUnsignedPdfBytes(fileStoreId, tenantId, correlationId);
			return encodeToBase64(pdfBytes, correlationId);
		} catch (DSCException e) {
			throw e;
		} catch (Exception e) {
			log.error("[correlationId={}] Unexpected error while processing PDF for fileStoreId={}", correlationId,
					fileStoreId, e);
			throw new DSCException(applicationProperties.getDSC_ERR_17());
		}
	}

	@RequestMapping(value = "/_pdfSign", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<DataSignResponse> pdfSign(@Validated(DataSignRequest.PdfSign.class) @RequestBody DataSignRequest dataSignRequest) {
		RequestInfo requestInfo = dataSignRequest.getRequestInfo();
		// RequestInfo.userInfo.id is guaranteed by DataSignRequest validation
		Long userId = requestInfo.getUserInfo().getId();
		String correlationId = (requestInfo != null) ? requestInfo.getMsgId() : "N/A";
		String sigId = dataSignRequest.getSigId();
		Path sigPath;
		try {
			sigPath = pdfSigningArtifactStore.resolveSigFile(sigId, String.valueOf(userId),
					dataSignRequest.getTenantId(), dataSignRequest.getChannelId());
		} catch (SecurityException e) {
			log.warn("[correlationId={}] PDF signing session ownership check failed", correlationId);
			return getErrorDataSignResponse("", null, requestInfo, applicationProperties.getDSC_ERR_28(), null,
					HttpStatus.FORBIDDEN);
		} catch (IOException | IllegalArgumentException e) {
			log.warn("[correlationId={}] Invalid or expired PDF signing session", correlationId, e);
			return getErrorDataSignResponse("", null, requestInfo, applicationProperties.getDSC_ERR_28(), null,
					HttpStatus.BAD_REQUEST);
		}

		boolean dsc = false;
		emBridge bridge = null;
		String tempFilePath = sigPath.toString();
		ResponseDataPKCSBulkSign apiResponse = null;
		String fileId = null;
		String result = "";
		try {
			bridge = acquireBridge();
		} catch (IOException e) {
			log.error("Failed to initialize DSC bridge for pdfSign operation for correlationId={}", correlationId, e);
			cleanupPdfSigningSession(sigId, correlationId);
			return buildDataSignResponse(result, fileId, dataSignRequest.getRequestInfo(),
					applicationProperties.getDSC_ERR_01(), null);
		}
		try {
			try {
				apiResponse = bridge.decPKCSBulkSign(dataSignRequest.getResponseData(), tempFilePath);
				log.debug("[correlationId={}] decPKCSBulkSign error code: {}", correlationId, apiResponse.getErrorCode());
			} catch (Exception e) {
				log.error("Bulk PDF sign decryption failed for correlationId={}", correlationId, e);
				return buildDataSignResponse(result, fileId, dataSignRequest.getRequestInfo(),
						applicationProperties.getDSC_ERR_18(), null);
			}
			if (apiResponse != null && apiResponse.getErrorCode() != null) {
				return buildDataSignResponse(result, fileId, dataSignRequest.getRequestInfo(), null,
						apiResponse.getErrorCode() + " - " + apiResponse.getErrorMsg());
			}
			try {
				SignedPdfResult pdfResult = populateSignedPdfFileStoreId(apiResponse, dataSignRequest.getFileName(),
						userId, dataSignRequest.getTenantId(),
						dataSignRequest.getModuleName(), dataSignRequest.getChannelId(), dsc, correlationId);

				fileId = pdfResult.getFileStoreId();
				dsc = pdfResult.isDsc();

			} catch (DSCException e) {
				log.error("Signed PDF result processing failed for correlationId={}", correlationId, e);
				dsc = e.isDsc();
				if (dsc) {
					return buildDataSignResponse(result, fileId, dataSignRequest.getRequestInfo(), e.getMessage(),
							null);
				} else {
					return buildDataSignResponse(result, fileId, dataSignRequest.getRequestInfo(), null,
							e.getMessage());
				}

			}

			if (fileId != null && !fileId.isEmpty() && !fileId.equalsIgnoreCase("0")) {
				result = "Success";
				log.debug("[correlationId={}] File ID: {}, Result: {}", correlationId, fileId, result);
			} else {
				fileId = null;
				result = "Failure";
				return buildDataSignResponse(result, fileId, dataSignRequest.getRequestInfo(),
						applicationProperties.getDSC_ERR_26(), null);
			}
			return buildDataSignResponse(result, fileId, dataSignRequest.getRequestInfo(), null, null);
		} finally {
			cleanupPdfSigningSession(sigId, correlationId);
			releaseBridge(bridge);
		}
	}

	private SignedPdfResult populateSignedPdfFileStoreId(ResponseDataPKCSBulkSign apiResponse, String fileName,
			Long userId, String tenantId, String moduleName, String channelId, boolean dsc, String correlationId)
			throws DSCException {
		String fileStoreId = "0";
		boolean check = false;
		byte[] signedDocBytes = null;

		if (apiResponse != null) {
			if (apiResponse.getBulkSignItems() != null && !apiResponse.getBulkSignItems().isEmpty()) {
				for (BulkSignOutput doc : apiResponse.getBulkSignItems()) {
					log.debug("[correlationId={}] PDF sign completed, authentication pending", correlationId);
					try {
						boolean[] dscHolder = new boolean[] { dsc };
						check = checkPdfAuthentication(String.valueOf(userId), doc.getSignedData(), channelId, dscHolder, correlationId);
						dsc = dscHolder[0];
					} catch (DSCException e) {
						dsc = e.isDsc();
						throw new DSCException(e.getMessage(), dsc);
					}

					if (!check) {
						break;
					}
					signedDocBytes = Base64.decodeBase64(doc.getSignedData());
					if (signedDocBytes == null || signedDocBytes.length == 0) {
						dsc = true;
						log.error("[correlationId={}] Signed PDF returned by emBridge is empty", correlationId);
						throw new DSCException(applicationProperties.getDSC_ERR_23(), dsc);
					}

				}
			} else {
				dsc = true;
				throw new DSCException(applicationProperties.getDSC_ERR_22(), dsc);
			}

		}

		if (signedDocBytes != null) {
			try {
				fileStoreId = store(signedDocBytes, fileName + "_signed.pdf", moduleName, tenantId);
			} catch (DSCException e) {
				dsc = true;
				log.error("Failed to store signed PDF file for correlationId={}", correlationId, e);
				throw new DSCException(applicationProperties.getDSC_ERR_21(), dsc);
			}
		}
		return new SignedPdfResult(fileStoreId, dsc);
	}

	/**
	 * Uploads the signed PDF to DIGIT filestore straight from memory. The bytes are
	 * already held in full, so staging them on disk (DiskFileItem/temp file) only
	 * added I/O and files to clean up.
	 */
	private String store(byte[] content, String fileName, String moduleName, String tenantId) throws DSCException {
		String fileStoreId = null;
		try {
			StorageResponse storageRes = getFileStorageService(content, fileName, moduleName, tenantId);

			for (FileReq filesId : storageRes.getFiles()) {
				fileStoreId = filesId.getFileStoreId();
			}
		} catch (Exception e) {
			throw new DSCException(applicationProperties.getDSC_ERR_21(), e);
		}
		return fileStoreId;
	}

	private StorageResponse getFileStorageService(byte[] content, String fileName, String modulename,
			String tenantId) {
		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_UTF8_VALUE);
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);

		MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
		map.add("module", modulename);
		map.add("tenantId", tenantId);
		// filestore needs a filename on the multipart part, which ByteArrayResource lacks by default
		map.add("file", new ByteArrayResource(content) {
			@Override
			public String getFilename() {
				return fileName;
			}
		});

		StringBuilder uri = new StringBuilder(applicationProperties.getEgovFileStoreSerHost())
				.append(applicationProperties.getEgovFileStoreUploadFile());

		HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(map, headers);

		return restTemplate.postForObject(uri.toString(), request, StorageResponse.class);
	}

	private boolean checkPdfAuthentication(String userId, String signedData, String channelId, boolean[] dsc, String correlationId) throws DSCException {
		boolean result = false;
		DSAuthenticateWS authenticateWS = new DSAuthenticateWSProxy(applicationProperties.getEmasWsUrl());
		String authenticatePDF = null;
		String uniqueId = userId + "~" + channelId;
		try {
			authenticatePDF = authenticateWS.authenticatePDF(uniqueId, signedData, null, "authenticate");
		} catch (RemoteException e) {
			log.error("RemoteException occurred while authenticating PDF for correlationId={}", correlationId, e);
			dsc[0] = true;
			throw new DSCException(applicationProperties.getDSC_ERR_19(), dsc[0]);
		} catch (CustomException e) {
			log.error("Exception occurred while authenticating PDF for correlationId={}", correlationId, e);
			dsc[0] = true;
			throw new DSCException(applicationProperties.getDSC_ERR_19(), dsc[0]);
		}
		log.debug("[correlationId={}] eMAS authenticatePDF result: {}", correlationId, authenticatePDF);

		if (isEmasSuccess(authenticatePDF)) {
			result = true;
		} else if (authenticatePDF != null && !authenticatePDF.isEmpty()) {
			dsc[0] = false;
			throw new DSCException(authenticatePDF, dsc[0]);
		}
		return result;
	}

	/**
	 * eMAS replies are "<status>~<payload>" (or just "<status>"). Only the status
	 * token is compared, so free text such as "Signature verification unsuccessful"
	 * is not mistaken for success.
	 */
	private static boolean isEmasSuccess(String emasResponse) {
		if (emasResponse == null || emasResponse.isEmpty()) {
			return false;
		}
		String status = emasResponse.split("~", 2)[0].trim();
		return EMAS_STATUS_SUCCESS.equalsIgnoreCase(status);
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
	public ResponseEntity<DataSignResponse> dataSignDeregister(@Validated(DataSignRequest.Deregister.class) @RequestBody DataSignRequest dataSignRequest) {
		RequestInfo requestInfo = dataSignRequest.getRequestInfo();
		String correlationId = (requestInfo != null) ? requestInfo.getMsgId() : "N/A";
		String response = "";
		String emudhraErrorCode = "";
		// deregister
		try {
			
			response = populateDeregisterSoapCall(dataSignRequest.getRequestInfo().getUserInfo().getId(), dataSignRequest.getChannelId(), correlationId);
			if (!isEmasSuccess(response)) {
				emudhraErrorCode = response;
			}

		} catch (DSCException e) {
			log.error("Deregistration failed for correlationId={}", correlationId, e);
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
	private String populateDeregisterSoapCall(Long userId, String channel, String correlationId) throws DSCException {
		String result = "";
		DSAuthenticateWS authenticateWS = new DSAuthenticateWSProxy(applicationProperties.getEmasWsUrl());
		try {
			String response = authenticateWS.userExists(userId + "~" + channel);
			if (isEmasSuccess(response)) {
				result = authenticateWS.deregister(userId + "~" + channel, "degistration");
				log.debug("[correlationId={}] eMAS deregister result: {}", correlationId, result);
				if (!isEmasSuccess(result)) {
					throw new DSCException("eMAS rejected deregistration");
				}
			} else {
				throw new DSCException("Error in deregistration api from Emas" + response);
			}
		} catch (RemoteException e) {
			log.error("Deregistration API call failed for correlationId={}", correlationId, e);
			throw new DSCException("Error in deregistration api from Emas");
		}
		return result;
	}
	
	
	private ResponseEntity<DataSignResponse> getSuccessDataSignDeregisterResponse(String responseString,
			RequestInfo requestInfo, String dscErrorCode, String emudhraErrorCode) {
		HttpStatus status = hasError(dscErrorCode, emudhraErrorCode) ? HttpStatus.BAD_GATEWAY : HttpStatus.OK;
		final ResponseInfo responseInfo = ResponseInfoFactory.createResponseInfoFromRequestInfo(requestInfo,
				status == HttpStatus.OK);
		responseInfo.setStatus(status.toString());

		DataSignResponse datasign = new DataSignResponse(responseInfo, responseString, null, dscErrorCode,
				emudhraErrorCode);
		return new ResponseEntity<>(datasign, status);
	}
}
