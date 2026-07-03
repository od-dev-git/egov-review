package org.egov.edcr.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static org.egov.edcr.utility.DcrConstants.ROUNDMODE_MEASUREMENTS;
import org.apache.log4j.Logger;
import org.egov.commons.mdms.config.MdmsConfiguration;
import org.egov.commons.service.RestCallService;
import org.egov.edcr.entity.Installment;
import org.egov.edcr.preapproved.helper.PlanPreApproved;
import org.egov.infra.filestore.service.FileStoreService;
import org.egov.infra.microservice.models.RequestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class BpaService {
	
	private Logger LOG = Logger.getLogger(BpaService.class);
	public static final String PLOT = "plot";
	public static final String TOTALPLOTAREA = "totalPlotArea";
	
	 @Autowired
	private FileStoreService fileStoreService;
	 
	private RestCallService serviceRequestRepository;
	private MdmsConfiguration mdmsConfiguration;


	public  BpaService(RestCallService serviceRequestRepository,MdmsConfiguration mdmsConfiguration) {
		this.serviceRequestRepository = serviceRequestRepository;
		this.mdmsConfiguration = mdmsConfiguration;
	}

	public StringBuilder getPreApprovedSearchUrl(String drawingNo) {
			String hostUrl = mdmsConfiguration.getBpaHost();

		String url = String.format("%s/bpa-services/v1/preapprovedplan/_search?drawingNo="+drawingNo, hostUrl);
		//LOG.info("url"+url);
		return new StringBuilder(url);
	}

	public  Object fetchPreApproved(RequestInfo requestInfo,  String drawingNo) {
		StringBuilder searchUrl = getPreApprovedSearchUrl(drawingNo);
		Map<String, Object> requestInfoPayload = new HashMap<>();
		requestInfoPayload.put("RequestInfo", requestInfo);
		Object result = serviceRequestRepository.fetchResult(searchUrl, requestInfoPayload);
	      Object preApprove =  ((Map) result).get("preapprovedPlan");
		return preApprove;


	}
	    

	    Object DrawingDetails =new Object();
	    
	    Object preApprovedPlanResponse = new Object();
	    
	    public PlanPreApproved loadPreApprovedData(String eDcrNumber,LinkedHashMap bpaApplication, RequestInfo requestInfo) {
	    	
	    	
	    	
	    	 preApprovedPlanResponse = fetchPreApproved(requestInfo,eDcrNumber);
	          //LOG.info(preApprovedPlanResponse);
	    	return buildDcrPreApprovedApplicationdetails(preApprovedPlanResponse,bpaApplication);
	    }
	    private PlanPreApproved buildDcrPreApprovedApplicationdetails(Object preApprovedPlanResponse,LinkedHashMap bpaApplication ) {
			// TODO Auto-generated method stub
	    	PlanPreApproved plan = new PlanPreApproved();
	    	Object obj = ((List)preApprovedPlanResponse).get(0);
	    	
	    	//LOG.info("obj: "+obj);
	        
	        Object additionalDetails = bpaApplication.get("additionalDetails");
	         DrawingDetails =((Map) obj).get("drawingDetail");
	         
	        // LOG.info("DrawingDetails: "+DrawingDetails);
	         
//	        List nocs = (List) ((Map) nocResponse).get("Noc");
	        //String plotNo = (String ) ((Map)) DrawingDetails).get("plotNo");roadWidth
	         //LOG.info("additionalDetails: "+additionalDetails);
	         //System.out.println((String ) ((Map) additionalDetails).get("plotNo"));
	         
				if (CollectionUtils.isEmpty(getPlotAndKhataDetails(additionalDetails))) {
					Object planDetails = ((Map) additionalDetails).get("planDetail");
					Object plot = ((Map) planDetails).get("plot");

					Object planInformation = ((Map) planDetails).get("planInformation");

					plan.setPlotNo((String) ((Map) plot).get("plotNo"));

					plan.setKhataNo((String) ((Map) planInformation).get("khataNo"));

					plan.setPlotArea(new BigDecimal((String) ((Map) plot).get("area")));

				} else {
					plan.setPlotNo("");
					plan.setKhataNo("");
					plan.setPlotArea(getTotalPlotArea(additionalDetails));
				}
    
	        plan.setServiceType((String ) ((Map) DrawingDetails).get("serviceType"));
	        
	        plan.setFloorInfo((String ) ((Map) DrawingDetails).get("floorDescription"));
	        
	       // plan.setSubOccupancy((String ) ((Map) DrawingDetails).get("subOccupancy"));
	        
	        Object subocc = ((Map) DrawingDetails).get("subOccupancy");
	        
	        plan.setSubOccupancy((String)  ((Map) subocc).get("label"));
	      
	        Object roadwidt= ((Map) obj).get("roadWidth");
	        BigDecimal roadwidth=  new BigDecimal(roadwidt.toString());
	        
	        plan.setRoadWidth(roadwidth);
	        
	        plan.setTotalBuitUpArea(new BigDecimal((Double)  ((Map) DrawingDetails).get("totalBuitUpArea")));
	        
	        setTotalFloorAreaAndFar(plan,DrawingDetails);	        
	       
	        return plan;
			
		}

		private void setTotalFloorAreaAndFar(PlanPreApproved plan, Object drawingDetails) {
			
			Double far = (Double) ((Map) DrawingDetails).get("totalFar");
			Double floorArea = (Double) ((Map) DrawingDetails).get("totalFloorArea");

			plan.setTotalfloorArea(new BigDecimal(floorArea));
		
			plan.setProvidedFar(far);
			}

		

		public List getblockDetails() {
			
			List blocks = ( List) ((Map) DrawingDetails).get("blocks");
		 
			
			return blocks;
		}

		public Object getServiceTypeBPA6(PlanPreApproved plan) {
			try {
				 Map<String, String> SERVICE_TYPE = new ConcurrentHashMap<>();
				 SERVICE_TYPE.put("NEW_CONSTRUCTION", "New Construction");
				 SERVICE_TYPE.put("ADDITION_AND_ALTERATION", "Addition and Alteration");
				return SERVICE_TYPE.get(plan.getServiceType());
			}catch (Exception e) {
				e.printStackTrace();
				throw e;
			}
		}
		
		public Map<String, String> getSetBackDatapreApproved(PlanPreApproved plan,
				java.util.List blocks,int i) {
			Map<String, String> setBackData = new HashMap<>();
			Object obj = blocks.get(i);
			Object building = ((Map)obj).get("building");
			List setBack = (List) ((Map)building).get("setBack");
			Object sbk = setBack.get(0);

				setBackData.put("frontSetbackRequired", (String)  ((Map) sbk).get("frontSetback"));
				setBackData.put("rearSetbackRequired", (String)  ((Map) sbk).get("rearSetback"));
				setBackData.put("leftSetbackRequired", (String)  ((Map) sbk).get("leftSetback"));
				setBackData.put("rightSetbackRequired", (String)  ((Map) sbk).get("rightSetback"));


		return setBackData;
		}
		
		
		public java.util.List getfloordetails(PlanPreApproved plan,
				java.util.List blocks, int i) {
			Object obj = blocks.get(i);
			Object building = ((Map)obj).get("building");
			List floors = (List) ((Map)building).get("floors");

	    	


			return floors;
		}
		
		public Map<String,String> getfloorinfo(java.util.List floorDetails, int k) {
			// TODO Auto-generated method stub  floorName
			
			Object objs = 	floorDetails.get(k);
		   String floorName = (String)  ((Map) objs).get("floorName");
		   String builtUpArea = (String)  ((Map) objs).get("builtUpArea"); 
			 Map<String,String>	flr  = new HashMap<>();
			 flr.put("floorName", floorName);
			 flr.put("builtUpArea", builtUpArea);
			
			return flr;
		}

		public String getDrawingCode(LinkedHashMap bpaApplication) {
			
			//preApprovedCode
			PlanPreApproved plan = new PlanPreApproved();
	    	Object obj = ((List)preApprovedPlanResponse).get(0);
			
	    	 Object code= ((Map) obj).get("preApprovedCode");
	    	 
	    	 return code.toString();
		}
		
		public Object getAllInstallments(String consumerCode, RequestInfo requestInfo) {
			String hostUrl = mdmsConfiguration.getBpaHost();
			String url = String.format("%s/bpa-services/v1/bpa/_getAllInstallments", hostUrl);
			//LOG.info("url" + url);
			Map<String, Object> requestPayload = new HashMap<>();
			requestPayload.put("RequestInfo", requestInfo);
			Map<String, Object> consumerCodeMap = new HashMap<>();
			consumerCodeMap.put("consumerCode", consumerCode);
			requestPayload.put("InstallmentSearchCriteria", consumerCodeMap);
			Object result = serviceRequestRepository.fetchResult(new StringBuilder(url), requestPayload);
			return result;
			/*
			if (Objects.nonNull(result) && result instanceof Map
					&& Objects.nonNull(((Map) result).get("installments"))) {
				return result;
			} else {
				Map<String, Object> response = new HashMap<>();
				response.put("installments", Collections.EMPTY_LIST);
				return response;
			}
			*/
		}
		
		public BigDecimal getTotalPlotArea(Object additionalDetailsObj) {

			Map<String, Object> additionalDetails = (Map<String, Object>) additionalDetailsObj;

			double totalPlotArea = 0.0;

			try {

				if (Objects.nonNull(additionalDetails) 
						&& Objects.nonNull(additionalDetails.get(TOTALPLOTAREA))) {

					totalPlotArea = (double) additionalDetails.get(TOTALPLOTAREA);

				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return new BigDecimal(totalPlotArea);

		}
		
		
		public List<Map<String, Object>> getPlotAndKhataDetails(Object additionalDetailsObj){
			
			Map<String, Object> additionalDetails = (Map<String, Object>) additionalDetailsObj;
			
			List<Map<String, Object>> plotList = new ArrayList<>();
			
			if(Objects.nonNull(additionalDetails) 
					&& Objects.nonNull(additionalDetails.get(PLOT)) 
					&& additionalDetails.get(PLOT) instanceof List
					&& !CollectionUtils.isEmpty((List) additionalDetails.get(PLOT))){
			
					plotList = (List)additionalDetails.get(PLOT);
			}
			
			return plotList;
			
		}
		
		public  Object getBPAShowCauseNoticeDetails(RequestInfo requestInfo, String businessId) {
			StringBuilder searchUrl = getBpaShowCauseURL(businessId);
			Map<String, Object> requestInfoPayload = new HashMap<>();
			requestInfoPayload.put("RequestInfo", requestInfo);
			Object result = serviceRequestRepository.fetchResult(searchUrl, requestInfoPayload);			  
			return ((Map) result).get("Notice");
		}
		
		public StringBuilder getBpaShowCauseURL(String businessId) {
		String hostUrl = mdmsConfiguration.getBpaHost();
		
		String url=new String();
		if(businessId.substring(0, 3).equals("BLR")) {
			url = String.format("%s/bpa-services/v1/regularization/notice/_search?businessid="+businessId, hostUrl);
		} else {
			url = String.format("%s/bpa-services/v1/notice/_search?businessid="+businessId, hostUrl);
		}
		
		return new StringBuilder(url);
	}

}
