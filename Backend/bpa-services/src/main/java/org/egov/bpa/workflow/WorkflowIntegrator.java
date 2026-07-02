package org.egov.bpa.workflow;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import java.util.LinkedList;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.util.BPAErrorConstants;
import org.egov.bpa.web.model.BPA;
import org.egov.bpa.web.model.BPARequest;
import org.egov.bpa.web.model.Notice;
import org.egov.bpa.web.model.NoticeRequest;
import org.egov.bpa.web.model.regularization.Regularization;
import org.egov.bpa.web.model.regularization.RegularizationRequest;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

@Service
@Slf4j
public class WorkflowIntegrator {

	private static final String TENANTIDKEY = "tenantId";

	private static final String BUSINESSSERVICEKEY = "businessService";

	private static final String ACTIONKEY = "action";

	private static final String COMMENTKEY = "comment";

	private static final String MODULENAMEKEY = "moduleName";

	private static final String BUSINESSIDKEY = "businessId";

	private static final String DOCUMENTSKEY = "documents";

	private static final String ASSIGNEEKEY = "assignes";

	private static final String MODULENAMEVALUE = "BPA";
	
	private static final String UUIDKEY = "uuid";

	private static final String WORKFLOWREQUESTARRAYKEY = "ProcessInstances";

	private static final String REQUESTINFOKEY = "RequestInfo";

	private static final String PROCESSINSTANCESJOSNKEY = "$.ProcessInstances";

	private static final String BUSINESSIDJOSNKEY = "$.businessId";

	private static final String STATUSJSONKEY = "$.state.applicationStatus";
	
	//private static final String ACTIONJSONKEY = "$.state.applicationStatus";

	private RestTemplate rest;

	private BPAConfiguration config;

	@Autowired
	public WorkflowIntegrator(RestTemplate rest, BPAConfiguration config) {
		this.rest = rest;
		this.config = config;
	}

	/**
	 * Method to integrate with workflow
	 *
	 * takes the bpa request as parameter constructs the work-flow request
	 *
	 * and sets the resultant status from wf-response back to bpa object
	 *
	 * @param bpaRequest
	 */
	public void callWorkFlow(BPARequest bpaRequest) {
		String wfTenantId = bpaRequest.getBPA().getTenantId();
		JSONArray array = new JSONArray();
		BPA bpa = bpaRequest.getBPA();
		JSONObject obj = new JSONObject();
		obj.put(BUSINESSIDKEY, bpa.getApplicationNo());
		obj.put(TENANTIDKEY, wfTenantId);
		obj.put(BUSINESSSERVICEKEY, bpa.getBusinessService());
		obj.put(MODULENAMEKEY, MODULENAMEVALUE);
		obj.put(ACTIONKEY, bpa.getWorkflow().getAction());
		obj.put(COMMENTKEY, bpa.getWorkflow().getComments());
		
		if (!CollectionUtils.isEmpty(bpa.getWorkflow().getAssignes())) {
			List<Map<String, String>> uuidmaps = new LinkedList<>();
			bpa.getWorkflow().getAssignes().forEach(assignee -> {
				Map<String, String> uuidMap = new HashMap<>();
				uuidMap.put(UUIDKEY, assignee);
				uuidmaps.add(uuidMap);
			});
			obj.put(ASSIGNEEKEY, uuidmaps);
		}
		
		obj.put(DOCUMENTSKEY, bpa.getWorkflow().getVarificationDocuments());
		array.add(obj);
		JSONObject workFlowRequest = new JSONObject();
		workFlowRequest.put(REQUESTINFOKEY, bpaRequest.getRequestInfo());
		workFlowRequest.put(WORKFLOWREQUESTARRAYKEY, array);
		String response = null;
		try {
			response = rest.postForObject(config.getWfHost().concat(config.getWfTransitionPath()), workFlowRequest,
					String.class);
		} catch (HttpClientErrorException e) {

			/*
			 * extracting message from client error exception
			 */
			DocumentContext responseContext = JsonPath.parse(e.getResponseBodyAsString());
			List<Object> errros = null;
			try {
				errros = responseContext.read("$.Errors");
			} catch (PathNotFoundException pnfe) {
				log.error(BPAErrorConstants.EG_BPA_WF_ERROR_KEY_NOT_FOUND,
						" Unable to read the json path in error object : " + pnfe.getMessage());
				throw new CustomException(BPAErrorConstants.EG_BPA_WF_ERROR_KEY_NOT_FOUND,
						" Unable to read the json path in error object : " + pnfe.getMessage());
			}
			throw new CustomException(BPAErrorConstants.EG_WF_ERROR, errros.toString());
		} catch (Exception e) {
			throw new CustomException(BPAErrorConstants.EG_WF_ERROR,
					" Exception occured while integrating with workflow : " + e.getMessage());
		}

		/*
		 * on success result from work-flow read the data and set the status
		 * back to BPA object
		 */
		DocumentContext responseContext = JsonPath.parse(response);
		List<Map<String, Object>> responseArray = responseContext.read(PROCESSINSTANCESJOSNKEY);
		Map<String, String> idStatusMap = new HashMap<>();
		responseArray.forEach(object -> {

			DocumentContext instanceContext = JsonPath.parse(object);
			idStatusMap.put(instanceContext.read(BUSINESSIDJOSNKEY), instanceContext.read(STATUSJSONKEY));
		});
		// setting the status back to BPA object from wf response
		bpa.setStatus(idStatusMap.get(bpa.getApplicationNo()));

	}

	public void callWorkFlowfornotice(@Valid NoticeRequest request) {
		// TODO Auto-generated method stub
		
		String wfTenantId = request.getnotice().getTenantid();
		JSONArray array = new JSONArray();
		Notice  notice  = request.getnotice();
		JSONObject obj = new JSONObject();
		obj.put(BUSINESSIDKEY, notice.getLetterNo());
		obj.put(TENANTIDKEY, wfTenantId);
		obj.put(BUSINESSSERVICEKEY, notice.getBusinessService());
		obj.put(MODULENAMEKEY, MODULENAMEVALUE);
		obj.put(ACTIONKEY, notice.getWorkflow().getAction());
		obj.put(COMMENTKEY, notice.getWorkflow().getComments());
		
		if (!CollectionUtils.isEmpty(notice.getWorkflow().getAssignes())) {
			List<Map<String, String>> uuidmaps = new LinkedList<>();
			notice.getWorkflow().getAssignes().forEach(assignee -> {
				Map<String, String> uuidMap = new HashMap<>();
				uuidMap.put(UUIDKEY, assignee);
				uuidmaps.add(uuidMap);
			});
			obj.put(ASSIGNEEKEY, uuidmaps);
		}
		
		obj.put(DOCUMENTSKEY, notice.getWorkflow().getVarificationDocuments());
		array.add(obj);
		JSONObject workFlowRequest = new JSONObject();
		workFlowRequest.put(REQUESTINFOKEY, request.getRequestInfo());
		workFlowRequest.put(WORKFLOWREQUESTARRAYKEY, array);
		String response = null;
		try {
			response = rest.postForObject(config.getWfHost().concat(config.getWfTransitionPath()), workFlowRequest,
					String.class);
		} catch (HttpClientErrorException e) {

			/*
			 * extracting message from client error exception
			 */
			DocumentContext responseContext = JsonPath.parse(e.getResponseBodyAsString());
			List<Object> errros = null;
			try {
				errros = responseContext.read("$.Errors");
			} catch (PathNotFoundException pnfe) {
				log.error(BPAErrorConstants.EG_BPA_WF_ERROR_KEY_NOT_FOUND,
						" Unable to read the json path in error object : " + pnfe.getMessage());
				throw new CustomException(BPAErrorConstants.EG_BPA_WF_ERROR_KEY_NOT_FOUND,
						" Unable to read the json path in error object : " + pnfe.getMessage());
			}
			throw new CustomException(BPAErrorConstants.EG_WF_ERROR, errros.toString());
		} catch (Exception e) {
			throw new CustomException(BPAErrorConstants.EG_WF_ERROR,
					" Exception occured while integrating with workflow : " + e.getMessage());
		}

		/*
		 * on success result from work-flow read the data and set the status
		 * back to notice object
		 */
		DocumentContext responseContext = JsonPath.parse(response);
		List<Map<String, Object>> responseArray = responseContext.read(PROCESSINSTANCESJOSNKEY);
		Map<String, String> idStatusMap = new HashMap<>();
		responseArray.forEach(object -> {

			DocumentContext instanceContext = JsonPath.parse(object);
			idStatusMap.put(instanceContext.read(BUSINESSIDJOSNKEY), instanceContext.read(STATUSJSONKEY));
		});
		// setting the status and action back to Notice  object from wf response
//		notice.setStatus(idStatusMap.get(notice.getLetterNo()));
//		notice.setAction(request.getnotice().getWorkflow().getAction());
//		log.info("status set duting create notice api:"+notice.getStatus());

		
	}
	
	
	/**
	 * Method to integrate with workflow
	 *
	 * takes the bpa request as parameter constructs the work-flow request
	 *
	 * and sets the resultant status from wf-response back to Regularization object
	 *
	 * @param bpaRequest
	 */
	public void callWorkFlowForRegularization(RegularizationRequest regularizationRequest) {
		Regularization regularization = regularizationRequest.getRegularization();
		JSONArray array = new JSONArray();
		
		JSONObject obj = new JSONObject();
		obj.put(BUSINESSIDKEY, regularization.getApplicationNo());
		obj.put(TENANTIDKEY, regularization.getTenantId());
		obj.put(BUSINESSSERVICEKEY, regularization.getBusinessService());
		obj.put(MODULENAMEKEY, MODULENAMEVALUE);
		obj.put(ACTIONKEY, regularization.getWorkflow().getAction());
		obj.put(COMMENTKEY, regularization.getWorkflow().getComments());
		
		if (!CollectionUtils.isEmpty(regularization.getWorkflow().getAssignes())) {
			List<Map<String, String>> uuidmaps = new LinkedList<>();
			regularization.getWorkflow().getAssignes().forEach(assignee -> {
				Map<String, String> uuidMap = new HashMap<>();
				uuidMap.put(UUIDKEY, assignee);
				uuidmaps.add(uuidMap);
			});
			obj.put(ASSIGNEEKEY, uuidmaps);
		}
		
		obj.put(DOCUMENTSKEY, regularization.getWorkflow().getVarificationDocuments());
		array.add(obj);
		
		JSONObject workFlowRequest = new JSONObject();
		workFlowRequest.put(REQUESTINFOKEY, regularizationRequest.getRequestInfo());
		workFlowRequest.put(WORKFLOWREQUESTARRAYKEY, array);
		String response = null;
		try {
			response = rest.postForObject(config.getWfHost().concat(config.getWfTransitionPath()), workFlowRequest,String.class);
		
		} catch (HttpClientErrorException e) {
			/*
			 * extracting message from client error exception
			 */
			DocumentContext responseContext = JsonPath.parse(e.getResponseBodyAsString());
			List<Object> errros = null;
			try {
				errros = responseContext.read("$.Errors");
			} catch (PathNotFoundException pnfe) {
				log.error(BPAErrorConstants.EG_BPA_WF_ERROR_KEY_NOT_FOUND,
						" Unable to read the json path in error object : " + pnfe.getMessage());
				throw new CustomException(BPAErrorConstants.EG_BPA_WF_ERROR_KEY_NOT_FOUND,
						" Unable to read the json path in error object : " + pnfe.getMessage());
			}
			throw new CustomException(BPAErrorConstants.EG_WF_ERROR, errros.toString());
		} catch (Exception e) {
			throw new CustomException(BPAErrorConstants.EG_WF_ERROR,
					" Exception occured while integrating with workflow : " + e.getMessage());
		}

		/*
		 * on success result from work-flow read the data and set the status
		 * back to BPA object
		 */
		DocumentContext responseContext = JsonPath.parse(response);
		List<Map<String, Object>> responseArray = responseContext.read(PROCESSINSTANCESJOSNKEY);
		Map<String, String> idStatusMap = new HashMap<>();
		
		responseArray.forEach(object -> {
			DocumentContext instanceContext = JsonPath.parse(object);
			idStatusMap.put(instanceContext.read(BUSINESSIDJOSNKEY), instanceContext.read(STATUSJSONKEY));
		});
		
		// setting the status back to BPA object from wf response
		regularization.setStatus(idStatusMap.get(regularization.getApplicationNo()));

	}
}
