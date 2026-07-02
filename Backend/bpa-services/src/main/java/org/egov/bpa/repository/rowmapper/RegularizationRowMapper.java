package org.egov.bpa.repository.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.egov.bpa.web.model.AuditDetails;
import org.egov.bpa.web.model.Document;
import org.egov.bpa.web.model.landInfo.Institution;
import org.egov.bpa.web.model.landInfo.OwnerInfo;
import org.egov.bpa.web.model.landInfo.Relationship;
import org.egov.bpa.web.model.regularization.AppType;
import org.egov.bpa.web.model.regularization.BUADetails;
import org.egov.bpa.web.model.regularization.BuildingOtherDetails;
import org.egov.bpa.web.model.regularization.BuildingRegularizationInfo;
import org.egov.bpa.web.model.regularization.FARDetails;
import org.egov.bpa.web.model.regularization.LandRegularizationInfo;
import org.egov.bpa.web.model.regularization.PlotInfo;
import org.egov.bpa.web.model.regularization.Regularization;
import org.egov.bpa.web.model.regularization.RegularizationDscDetails;
import org.egov.tracer.model.CustomException;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class RegularizationRowMapper implements ResultSetExtractor<List<Regularization>> {

	@Autowired
	private ObjectMapper mapper;
	
	/**
	 * extract the data from the resultset and prepare the Regularization Object
	 * @see org.springframework.jdbc.core.ResultSetExtractor#extractData(java.sql.ResultSet)
	 */
	@Override
	public List<Regularization> extractData(ResultSet rs) throws SQLException, DataAccessException {
		Map<String, Regularization> regularizationMap = new LinkedHashMap<>();
       
		while (rs.next()) {
			try {
				String id = rs.getString("id");
				Regularization regularization = regularizationMap.get(id);

				String applicationNo = rs.getString("applicationno");
				String approvalNo = rs.getString("approvalno");
				String tenantId = rs.getString("tenantid");

				if (regularization == null) {
					Long lastModifiedTime = rs.getLong("regularization_lastmodifiedtime");
					if (rs.wasNull()) {
						lastModifiedTime = null;
					}
					Object additionalDetails = null;
					if(null != rs.getString("additionaldetails")) {
					additionalDetails = new Gson().fromJson(rs.getString("additionaldetails").equals("{}")
							|| rs.getString("additionaldetails").equals("null") ? null
									: rs.getString("additionaldetails"),
							Object.class);
					}
					AuditDetails auditdetails = AuditDetails.builder()
							.createdBy(rs.getString("regularization_createdby"))
							.createdTime(rs.getLong("regularization_createdtime"))
							.lastModifiedBy(rs.getString("regularization_lastmodifiedby"))
							.lastModifiedTime(lastModifiedTime).build();

					regularization = Regularization.builder().id(id).tenantId(tenantId)
							.appType(AppType.fromValue(rs.getString("apptype"))).applicationNo(applicationNo)
							.applicationDate(rs.getLong("applicationdate"))
							.applicationType(rs.getString("applicationtype")).approvalDate(rs.getLong("approvaldate"))
							.status(rs.getString("status")).ownershipCategory("INDIVIDUAL.SINGLEOWNER")
							.serviceType(rs.getString("servicetype")).serviceSubType(rs.getString("servicesubtype"))
							.businessService(rs.getString("businessservice")).accountId(rs.getString("accountid"))
							.approvalNo(approvalNo).permitExpiryDate(rs.getString("permitexpirydate"))
							.additionalDetails(additionalDetails).auditDetails(auditdetails).build();

					// Add Workflow to Regularization
					addWorkflowToRegularization(rs, regularization);

					// Add Institution to Regularization
					addInstitutionToRegularization(rs, regularization);

				}

				// Add LandRegularizationInfo to Regularization
				addLandRegularizationInfoToRegularization(rs, regularization);

				// Add BuildingRegularizationInfo to Regularization
				addBuildingRegularizationInfoToRegularization(rs, regularization);

				// Add documents to Regularization
				addDocumentsToRegularization(rs, regularization);

				// Add Dsc Details to Regularization
				addDscDetailsToRegularization(rs, regularization);

				// Add Owners to Regularization
				addOwnersToRegularization(rs, regularization);

				regularizationMap.put(id, regularization);

			} catch (Exception e) {
				log.info("Exception in RegularizationRowmapper for Application No " + rs.getString("applicationno"));
				e.printStackTrace();
				throw new CustomException("SQL ERROR", "Error occurred in Regularization Rowmapper " + e.getMessage());
			}
		}

		return new ArrayList<>(regularizationMap.values());
	}




	/**
	 * add LandRegularizationInfo objects to the Regularization from the results set
	 * @param rs
	 * @param regularization
	 * @throws SQLException
	 */
	private void addWorkflowToRegularization(ResultSet rs, Regularization regularization)  throws SQLException {
//		Workflow workflow = Workflow.builder()
//				.build();
//		regularization.setWorkflow(workflow);
	}

	

	/**
	 * add LandRegularizationInfo objects to the Regularization from the results set
	 * @param rs
	 * @param regularization
	 * @throws SQLException
	 */
	private void addLandRegularizationInfoToRegularization(ResultSet rs, Regularization regularization)  throws SQLException {
		
		String landId = rs.getString("landid");
		LandRegularizationInfo landRegularizationInfo = regularization.getLandRegularizationInfo();
		
		if (!ObjectUtils.isEmpty(landRegularizationInfo)) {
			if(landRegularizationInfo.getId().equals(landId)) {
				addPlotInfoToLandRegularizationInfo(rs, landRegularizationInfo);
			}
			
		} else {
			if(!ObjectUtils.isEmpty(landId)) {
				AuditDetails landAuditdetails = AuditDetails.builder()
						.createdBy(rs.getString("landcreatedby"))
						.createdTime(rs.getLong("landcreatedtime"))
						.lastModifiedBy(rs.getString("landlastmodifiedby"))
						.lastModifiedTime(rs.getLong("landlastmodifiedtime"))
						.build();
				
				Object additionalDetails = new Gson().fromJson(rs.getString("landadditionaldetails").equals("{}")
						|| rs.getString("landadditionaldetails").equals("null") ? null : rs.getString("landadditionaldetails"),
						Object.class);
				
				landRegularizationInfo = LandRegularizationInfo.builder()
						.id(rs.getString("landid"))
						.tenantId(rs.getString("landtenantid"))
						.regularizationId(rs.getString("regularizationid"))
						.landRegularizationType(rs.getString("regularizationtype"))
						.totalPlotArea(rs.getBigDecimal("totalplotarea"))
						.accessRoadWidth(rs.getBigDecimal("accessroadwidth"))
						.additionalDetails(additionalDetails)
						.auditDetails(landAuditdetails)
						.build();
				
				addPlotInfoToLandRegularizationInfo(rs, landRegularizationInfo);
			}
		}
		
		regularization.setLandRegularizationInfo(landRegularizationInfo);
		
	}


	private void addBuildingRegularizationInfoToRegularization(ResultSet rs, Regularization regularization)
			throws SQLException {

		String buildingId = rs.getString("buildingid");
		BuildingRegularizationInfo buildingRegularizationInfo = regularization.getBuildingRegularizationInfo();

		if (!ObjectUtils.isEmpty(buildingId)) {
			AuditDetails buildingAuditdetails = AuditDetails.builder()
					.createdBy(rs.getString("buildingcreatedby"))
					.createdTime(rs.getLong("buildingcreatedtime"))
					.lastModifiedBy(rs.getString("buildinglastmodifiedby"))
					.lastModifiedTime(rs.getLong("buildinglastmodifiedtime"))
					.build();

			Object additionalDetails = new Gson().fromJson(rs.getString("buildingadditionaldetails").equals("{}")
					|| rs.getString("buildingadditionaldetails").equals("null") ? null
							: rs.getString("buildingadditionaldetails"),
					Object.class);

			Object buildingBlocks = new Gson().fromJson(
					rs.getString("buildingblocks").equals("{}") || rs.getString("buildingblocks").equals("null") ? null
							: rs.getString("buildingblocks"),
					Object.class);
			
			
			FARDetails farDetails = FARDetails.builder()
					.baseFar(rs.getString("basefar"))
					.maxPermissibleFar(rs.getString("maxpermissiblefar"))
					.approvedFar(rs.getString("approvedFar"))
					.asBuiltFar(rs.getString("asbuiltfar"))
					.farStatus(rs.getString("farstatus"))
					.build();
			
			BUADetails buaDetails = BUADetails.builder()
					.totalProvidedBUA(rs.getString("totalprovidedbua"))
					.totalApprovedBUA(rs.getString("totalapprovedbua"))
					.totalUnauthorizedBUA(rs.getString("totalunauthorizedbua"))
					.totalUnauthAreaonSBWithinNorms(rs.getString("totalunauthareaonsbwithinnorms"))
					.totalUnauthAreaonSBBeyondNormsButUnder5(rs.getString("totalunauthareaonsbbeyondnormsbutunder5"))
					.totalUnauthAreaonSBBeyondNormsButUnder10(rs.getString("totalunauthareaonsbbeyondnormsbutunder10"))
					.build();
			
			BuildingOtherDetails otherDetails = BuildingOtherDetails.builder()
					.hasProjectProvidedMin10PercentBUAForEWSWithin5KmFromProjectSite(rs.getBoolean("hasprojectprovidedmin10percentbuaforewswithin5kmfromprojectsite"))
					.numberOfTemporaryStructures(rs.getString("numberoftemporarystructures"))
					.projectValueIfEIDPFeeApplicableForProject(rs.getString("projectvalueifeidpfeeapplicableforproject"))
					.totalNoOfDwellingUnits(rs.getString("totalnoofdwellingunits"))
					.isShelterFeeApplicable(rs.getBoolean("isshelterfeeapplicable"))
					.isEIDPFeeApplicable(rs.getBoolean("iseidpfeeapplicable"))
					.isCWWCFeeApplicable(rs.getBoolean("iscwwcfeeapplicable"))
					.effectiveEWSArea(rs.getString("effectiveewsarea"))
					.isSecurityDepositRequired(rs.getBoolean("issecuritydepositrequired"))
					.tdrFarRelaxation(rs.getString("tdrfarrelaxation"))
					.farFeeRate(rs.getString("farfeerate"))
					.unauthorizedSBWithinNormsRate(rs.getString("unauthorizedsbwithinnormsrate"))
					.unauthorizedSBBNUnder5Rate(rs.getString("unauthorizedsbbnunder5rate"))
					.unauthorizedSBBNUnder10Rate(rs.getString("unauthorizedsbbnunder10rate"))
					.build();
					
					
			buildingRegularizationInfo = BuildingRegularizationInfo.builder()
					.id(rs.getString("buildingid"))
					.tenantId(rs.getString("buildingtenantid"))
					.regularizationId(rs.getString("regularizationid"))
					.farDetails(farDetails)
					.buaDetails(buaDetails)
					.otherDetails(otherDetails)
					.buildingBlocks(buildingBlocks).additionalDetails(additionalDetails)
					.auditDetails(buildingAuditdetails).build();

		}

		regularization.setBuildingRegularizationInfo(buildingRegularizationInfo);

	}
	
	private void addPlotInfoToLandRegularizationInfo(ResultSet rs, LandRegularizationInfo landRegularizationInfo) throws SQLException {
		String plotId = rs.getString("plotinfoid");
		List<PlotInfo> plotsList = landRegularizationInfo.getPlotInfo();
		
		if (!ObjectUtils.isEmpty(plotId)) {
			
			if (!CollectionUtils.isEmpty(plotsList)) {
				for (PlotInfo plotInfo : plotsList) {
	                if (plotInfo.getId().equals(plotId))
	                    return;
	            }
			}
			
			AuditDetails landAuditdetails = AuditDetails.builder()
					.createdBy(rs.getString("plotinfocreatedby"))
					.createdTime(rs.getLong("plotinfocreatedtime"))
					.lastModifiedBy(rs.getString("plotinfolastmodifiedby"))
					.lastModifiedTime(rs.getLong("plotinfolastmodifiedtime"))
					.build();
			
			PlotInfo plotInfo = PlotInfo.builder()
					.id(plotId)
					.tenantId(rs.getString("landtenantid"))
					.landInfoId(landRegularizationInfo.getId())
					.district(rs.getString("district"))
					.tehsil(rs.getString("tehsil"))
					.village(rs.getString("village"))
					.plotNo(rs.getString("plotno"))
					.subPlotNo(rs.getString("subplotno"))
					.subSubPlotNo(rs.getString("subsubplotno"))
					.plotArea(rs.getString("plotarea"))
					.khata(rs.getString("khata"))
					.kisam(rs.getString("kisam"))
					.landOwnerName(rs.getString("landownername"))
					.gpaHolderName(rs.getString("gpaholdername"))
					.saleDeedNo(rs.getString("saledeedno"))
					.saleDeedDate(rs.getLong("saledeeddate"))
					.bmvValue(rs.getString("bmvvalue"))
					.isPlotToBeGifted(rs.getString("plottobegifted"))
					.areaToBeGifted(rs.getString("areatobegifted"))
					.reasonForGift(rs.getString("reasonforgift"))
					.isDeleted(rs.getBoolean("isactive"))
					.auditDetails(landAuditdetails)
					.build();
			
			
			landRegularizationInfo.addPlotInfo(plotInfo);
			
		}
		
	}



	/**
	 * add Institution objects to the Regularization from the results set
	 * @param rs
	 * @param regularization
	 * @throws SQLException
	 */
	private void addInstitutionToRegularization(ResultSet rs, Regularization regularization)  throws SQLException {
		
		String institutionId = rs.getString("institutionid");
		
		if (!ObjectUtils.isEmpty(institutionId)) {
			
			Object institutionadditionalDetails = new Gson().fromJson(rs.getString("institutionadditionaldetails").equals("{}")
					|| rs.getString("institutionadditionaldetails").equals("null") ? null : rs.getString("institutionadditionaldetails"),
					Object.class);
			
			Institution institution =  Institution.builder()
					.id(institutionId)
					.tenantId(rs.getString("institutiontenantid"))
					.type(rs.getString("type"))
					.designation(rs.getString("designation"))
					.nameOfAuthorizedPerson(rs.getString("nameofauthorizedperson"))
					.additionalDetails(institutionadditionalDetails)
					.build();
			regularization.setInstitution(institution);
		}
		
	}





	/**
	 * add Document objects to the Regularization from the results set
	 * @param rs
	 * @param regularization
	 * @throws SQLException
	 */
	private void addDocumentsToRegularization(ResultSet rs, Regularization regularization) throws SQLException {
		
		String documentId = rs.getString("documentid");
		List<Document> documents = regularization.getDocuments();
				
		if (!ObjectUtils.isEmpty(documentId)) {
			
			if (!CollectionUtils.isEmpty(documents)) {
				for (Document document : documents) {
	                if (document.getId().equals(documentId))
	                    return;
	            }
			}
			
			AuditDetails auditdetails = AuditDetails.builder()
					.createdBy(rs.getString("documentcreatedby"))
					.createdTime(rs.getLong("documentcreatedtime"))
					.lastModifiedBy(rs.getString("documentlastmodifiedby"))
					.lastModifiedTime(rs.getLong("documentlastmodifiedtime"))
					.build();
			
			Object docDetails = null;
			
			if(rs.getString("documentadditionaldetails") != null) {
				docDetails = new Gson().fromJson(rs.getString("documentadditionaldetails").equals("{}")
						|| rs.getString("documentadditionaldetails").equals("null") ? null : rs.getString("documentadditionaldetails"),
						Object.class);
			}
			
			Document document = Document.builder()
					.id(documentId)
					.documentType(rs.getString("documenttype"))
					.fileStoreId(rs.getString("filestoreid"))
					.documentUid(rs.getString("documentuid"))
					.additionalDetails(docDetails)
					.auditDetails(auditdetails)
					.build();
			regularization.addDocuments(document);
		}
	}
	
	
	
	/**
	 * add Owners objects to the Regularization from the results set
	 * @param rs
	 * @param regularization
	 * @throws SQLException
	 */
	private void addDscDetailsToRegularization(ResultSet rs, Regularization regularization) throws SQLException {
		String dscDetailsId = rs.getString("dscdetailsid");
		List<RegularizationDscDetails> dscDetailsList = regularization.getDscDetails();
				
		if (!ObjectUtils.isEmpty(dscDetailsId)) {
			
			if (!CollectionUtils.isEmpty(dscDetailsList)) {
				for (RegularizationDscDetails dscDetails : dscDetailsList) {
	                if (dscDetails.getId().equals(dscDetailsId))
	                    return;
	            }
			}
			
			AuditDetails auditdetails = AuditDetails.builder()
					.createdBy(rs.getString("dscdetailscreatedby"))
					.createdTime(rs.getLong("dscdetailscreatedtime"))
					.lastModifiedBy(rs.getString("dscdetailslastmodifiedby"))
					.lastModifiedTime(rs.getLong("dscdetailslastmodifiedtime"))
					.build();
			
			
			JsonNode additionalDetails = null;
			try {
				PGobject pgObj = (PGobject) rs.getObject("dscdetailsadditionaldetails");
				if (!ObjectUtils.isEmpty(pgObj)) {
					additionalDetails = mapper.readTree(pgObj.getValue());
				}
			} catch (Exception e) {
				throw new CustomException("PARSING ERROR", "The DSC Details additionalDetail json cannot be parsed");
			}
			
			RegularizationDscDetails dscDetails = RegularizationDscDetails.builder()
					.id(dscDetailsId)
					.documentType(rs.getString("dscdetailsdocumenttype"))
					.documentId(rs.getString("dscdetailsdocumentid"))
					.applicationNo(rs.getString("dscdetailsapplicationno"))
					.approvedBy(rs.getString("dscdetailsapprovedby"))
					.tenantId(rs.getString("dscdetailstenantid"))
					.additionalDetails(additionalDetails)
					.auditDetails(auditdetails)
					.build();
			
			regularization.addDscDetails(dscDetails);
		}
		
	}

	
	/**
	 * add Owners objects to the Regularization from the results set
	 * @param rs
	 * @param regularization
	 * @throws SQLException
	 */
	private void addOwnersToRegularization(ResultSet rs, Regularization regularization) throws SQLException {
		String ownerId = rs.getString("ownersid");
		List<OwnerInfo> owners = regularization.getOwners();
		
		if (!ObjectUtils.isEmpty(ownerId)) {
			
			if (!CollectionUtils.isEmpty(owners)) {
				for (OwnerInfo ownerInfo : owners) {
	                if (ownerInfo.getOwnerId().equals(ownerId))
	                    return;
	            }
			}
		
			AuditDetails ownersAuditDetails = AuditDetails.builder()
					.createdBy(rs.getString("ownerscreatedby"))
					.createdTime(rs.getLong("ownerscreatedtime"))
					.lastModifiedBy(rs.getString("ownerslastmodifiedby"))
					.lastModifiedTime(rs.getLong("ownerslastmodifiedtime"))
					.build();
			
			Object ownersAdditionalDetails = null;
			
			if(rs.getString("ownersadditionaldetails") != null) {
				ownersAdditionalDetails = new Gson().fromJson(rs.getString("ownersadditionaldetails").equals("{}")
						|| rs.getString("ownersadditionaldetails").equals("null") ? null : rs.getString("ownersadditionaldetails"),
						Object.class);
			}
		
			OwnerInfo ownerInfo = OwnerInfo.builder()
					.ownerId(ownerId)
					.uuid(rs.getString("uuid"))
					.isPrimaryOwner(rs.getBoolean("isprimaryowner"))
					.ownerShipPercentage(rs.getBigDecimal("ownershippercentage"))
					.institutionId(rs.getString("ownersinstitutionid"))
					.relationship(Relationship.fromValue(rs.getString("relationship")))
					.additionalDetails(ownersAdditionalDetails)
					.auditDetails(ownersAuditDetails)
					.build();
			regularization.addOwners(ownerInfo);
		}
	}

}
