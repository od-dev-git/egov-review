package org.egov.bpa.service;

import java.util.HashMap;
import java.util.Map;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.web.model.landInfo.LandRecordDTO;
import org.egov.tracer.model.CustomException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class LandRecordService {
	
	private String bhulekhUrl = "https://bhulekh.ori.nic.in/sujagaService.svc/json/RoRDetails";
	private String igrAuthUrl22 = "https://erp.igrodisha.gov.in/igrone/api/User/GetUserAuthencate"; 
	private String igrUrl22 = "https://erp.igrodisha.gov.in/igrone/api/Deed/GetBmvPerAcer";
//	private String igrAuthUrl = "https://164.100.141.142/igrone/api/User/GetUserAuthencate"; 
//	private String igrUrl = "https://164.100.141.142/igrone/api/Deed/GetBmvPerAcer";
	private String igrAuthUrl = "/igrone/api/User/GetUserAuthencate"; 
	private String igrUrl = "/igrone/api/Deed/GetBmvPerAcer";
	private String BHULEKH_API_ERROR = "BHULEKH_API_ERROR";
	private String IGR_API_ERROR	 = "IGR_API_ERROR";
	private String IGR_AUTH_TOKEN_ERROR = "IGR_AUTH_TOKEN_ERROR";
	
	@Autowired
	private BPAConfiguration config;
	
	public LandRecordDTO searchLandRecords(LandRecordDTO landRecordInput) {

		String url = bhulekhUrl + "?districtLgdCode=%s&tahasilLgdCode=%s&villLgdCode=%s&PlotNo=%s";
		url = String.format(url, landRecordInput.getDistrictLgdCode(), landRecordInput.getTehsilLgdCode(),
				landRecordInput.getVillageLgdCode(), landRecordInput.getPlotNumber());

		RestTemplate restTemplate = new RestTemplate();
		String response = restTemplate.getForObject(url, String.class);
		
		log.info("Print response from bhulekh api !!!"+response);
		
		String errorMessage = "Error while calling Bhulekh API";
		if (response == null || response.trim().isEmpty()) {
			throw new CustomException(BHULEKH_API_ERROR, errorMessage);
		}

		JSONArray dataArray = new JSONArray();
		JSONObject root = new JSONObject(response);
		JSONObject result = root.optJSONObject("GetRoRDetailsResult");

		if (result == null || !result.optBoolean("success")) {
			if (result.optString("error") != null) {
				errorMessage = result.optString("error");
			}
			throw new CustomException(BHULEKH_API_ERROR, errorMessage);
		}

		dataArray = result.optJSONArray("data");
		if (dataArray == null || dataArray.length() == 0) {
			throw new CustomException(BHULEKH_API_ERROR, "No Data Found from Bhulekh API");
		}

		JSONObject data = dataArray.getJSONObject(0);

		LandRecordDTO landRecord = LandRecordDTO.builder().khata(data.optString("KhataNo"))
				.kisam(data.optString("kissam")).village(data.optString("villName"))
				.plotArea(String.valueOf(data.optDouble("Plot_area_acre"))).plotNumber(data.optString("Plot_No"))
				.applicantName(data.optString("Owner_name")).landOwnershipType(data.optString("Swatwa")).build();

		return landRecord;
	}

	public LandRecordDTO searchLandRevenueDetail(LandRecordDTO landRecordInput) {

		String authToken = getIGRAuthToken();
		Map<String, Object> request = new HashMap<>();
		request.put("districtCode", landRecordInput.getDistrictLgdCode());
		request.put("tehsilCode", landRecordInput.getTehsilLgdCode());
		request.put("mouzaCode", landRecordInput.getVillageLgdCode());
		request.put("plotNo", landRecordInput.getPlotNumber());

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBearerAuth(authToken);

		HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(request, headers);

		RestTemplate restTemplate = new RestTemplate();

		String url = config.getIgrHost() + igrUrl;
		log.info("Print url inside searchLandRevenueDetail !!!" + url);

		String response = restTemplate.postForObject(url, httpEntity, String.class);

		log.info("Print response from IGR Search API !!!" + response);

		JSONObject root = new JSONObject(response);
		if (root.optInt("code") != 0) {
			throw new CustomException(IGR_API_ERROR, "Error while fetching BMV details from IGR");
		}

		JSONObject data = root.optJSONObject("data");
		if (data == null) {
			throw new CustomException(IGR_API_ERROR, "No Data Found from IGR");
		}
		landRecordInput.setPerAcreBmvValue(data.optString("bmvPerAcer"));

		return landRecordInput;
	}
	
	public String getIGRAuthToken() {

		RestTemplate restTemplate = new RestTemplate();
		String token = "";
			Map<String, String> request = new HashMap<>();
			request.put("useR_LOGIN_ID", "sujog@ocac");
			request.put("useR_PWD", "sujog@12345");
			request.put("useR_TYPE", "SU");

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);

			HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);
			
			String url = config.getIgrHost()+igrAuthUrl;
			log.info("Print url inside getIGRAuthToken !!!"+url);
			String response = restTemplate.postForObject(url, entity, String.class);
			log.info("Print response from IGR Authentication API !!!"+response);
			JSONObject root = new JSONObject(response);

			if (root.optInt("code") != 0) {
				throw new CustomException(IGR_AUTH_TOKEN_ERROR, "Error in fetching auth token from IGR");
			}

			JSONObject data = root.optJSONObject("information");
			if (data == null || !data.has("token")) {
				throw new CustomException(IGR_AUTH_TOKEN_ERROR, "Token not present in IGR response");
			}
			
			token = data.getString("token");
			
			log.info("Print Authtoken ::::::::::::" + token);
		
		return token;
	}
}
