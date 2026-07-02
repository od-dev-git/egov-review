package org.egov.bpa.repository.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.egov.bpa.web.model.AuditDetails;
import org.egov.bpa.web.model.Document;
import org.egov.bpa.web.model.demolition.BlockInfo;
import org.egov.bpa.web.model.demolition.Demolition;
import org.egov.bpa.web.model.demolition.DemolitionLandInfo;
import org.egov.bpa.web.model.landInfo.Address;
import org.egov.bpa.web.model.landInfo.GeoLocation;
import org.egov.bpa.web.model.landInfo.OwnerInfo;
import org.egov.bpa.web.model.landInfo.Relationship;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import com.google.gson.Gson;

@Component
public class DemolitionRowMapper implements ResultSetExtractor<List<Demolition>> {

	@Override
	public List<Demolition> extractData(ResultSet rs) throws SQLException {
		Map<String, Demolition> demolitionMap = new HashMap<>();

		while (rs.next()) {
			String demolitionId = rs.getString("app_id");

			// Fetching or creating a Demolition instance
			Demolition demolition = demolitionMap.get(demolitionId);
			if (demolition == null) {
				demolition = Demolition.builder().id(demolitionId).tenantId(rs.getString("app_tenantid"))
						.applicationNo(rs.getString("app_applicationno")).status(rs.getString("app_status"))
						.applicationType(rs.getString("app_applicationtype"))
						.serviceType(rs.getString("app_servicetype"))
						.plotDetails(Optional.ofNullable(rs.getString("app_plotdetails"))
								.filter(s -> !s.isEmpty() && !"null".equals(s))
								.<Object>map(add -> new Gson().fromJson(add, Object.class)).orElse(null))
						.additionalDetails(Optional.ofNullable(rs.getString("app_additionaldetails"))
								.filter(s -> !s.isEmpty() && !"null".equals(s))
								.<Object>map(add -> new Gson().fromJson(add, Object.class)).orElse(null))
						.applicationDate(rs.getLong("app_applicationdate")).approvalNo(rs.getString("app_approvalno"))
						.approvalDate(rs.getLong("app_approvaldate"))
						.businessService(rs.getString("app_businessservice")).accountId(rs.getString("app_accountid"))
						.auditDetails(AuditDetails.builder().createdBy(rs.getString("app_createdby"))
								.lastModifiedBy(rs.getString("app_lastmodifiedby"))
								.createdTime(rs.getLong("app_createdtime"))
								.lastModifiedTime(rs.getLong("app_lastmodifiedtime")).build())
						.workflow(null) // Workflow mapping can be done here
						.landInfo(DemolitionLandInfo.builder().id(rs.getString("landinfo_id"))
								.tenantId(rs.getString("landinfo_tenantid"))
								.demolitionId(rs.getString("landinfo_demolitionid"))
								.totalLandArea(rs.getString("landinfo_totallandarea"))
								.plotNumber(rs.getString("landinfo_plotnumber")).mauza(rs.getString("landinfo_mauza"))
								.landOwnerName(rs.getString("landinfo_landownername"))
								.auditDetails(AuditDetails.builder().createdBy(rs.getString("landinfo_createdby"))
										.lastModifiedBy(rs.getString("landinfo_lastmodifiedby"))
										.createdTime(rs.getLong("landinfo_createdtime"))
										.lastModifiedTime(rs.getLong("landinfo_lastmodifiedtime")).build())
								.build())
						.owners(new ArrayList<>()) // Initial empty owner list
						.build();

				demolitionMap.put(demolitionId, demolition);
			}

			// Fetch and add owner information
			addOwnersToDemolitions(rs, demolition);

			// Fetch and add document information
			addDocumentsToDemolitions(rs, demolition);

			addBlockInfoToDemolitions(rs, demolition);

			String addressId = rs.getString("addr_id");
			if (addressId != null) {
				Address address = Address.builder().id(addressId).tenantId(rs.getString("addr_tenantid"))
						.doorNo(rs.getString("addr_doorno")).plotNo(rs.getString("addr_plotno"))
						.landmark(rs.getString("addr_landmark")).city(rs.getString("addr_city"))
						.district(rs.getString("addr_district")).region(rs.getString("addr_region"))
						.state(rs.getString("addr_state")).country(rs.getString("addr_country"))
						.pincode(rs.getString("addr_pincode")).buildingName(rs.getString("addr_buildingname"))
						.street(rs.getString("addr_street"))
						.auditDetails(AuditDetails.builder().createdBy(rs.getString("addr_createdby"))
								.lastModifiedBy(rs.getString("addr_lastmodifiedby"))
								.createdTime(rs.getLong("addr_createdtime"))
								.lastModifiedTime(rs.getLong("addr_lastmodifiedtime")).build())
						.build();

				// Set the address to the land info object
				if (demolition.getLandInfo() != null) {
					demolition.getLandInfo().setAddress(address);
				}
			}

			// Fetch and add geolocation information
			String geoId = rs.getString("geo_id");
			if (geoId != null) {
				GeoLocation geoLocation = GeoLocation.builder().latitude(rs.getDouble("geo_latitude"))
						.longitude(rs.getDouble("geo_longitude"))
						.additionalDetails(Optional.ofNullable(rs.getString("geo_additionaldetails"))
								.filter(s -> !s.isEmpty() && !"null".equals(s))
								.<Object>map(add -> new Gson().fromJson(add, Object.class)).orElse(null))
						.build();
				demolition.getLandInfo().getAddress().setGeoLocation(geoLocation);
			}
		}

		return new ArrayList<>(demolitionMap.values());
	}

	/**
	 * @param rs
	 * @param demolition
	 * @throws SQLException
	 */
	private void addDocumentsToDemolitions(ResultSet rs, Demolition demolition) throws SQLException {
		String documentId = rs.getString("document_id");
		List<Document> documents = demolition.getDocuments();

		if (!ObjectUtils.isEmpty(documentId)) {

			if (!CollectionUtils.isEmpty(documents)) {
				for (Document document : documents) {
					if (document.getId().equals(documentId))
						return;
				}
			}

			AuditDetails auditdetails = AuditDetails.builder().createdBy(rs.getString("document_createdby"))
					.createdTime(rs.getLong("document_createdtime"))
					.lastModifiedBy(rs.getString("document_lastmodifiedby"))
					.lastModifiedTime(rs.getLong("document_lastmodifiedtime")).build();

			Document document = Document.builder().id(documentId).documentType(rs.getString("document_type"))
					.fileStoreId(rs.getString("document_filestoreid")).documentUid(rs.getString("document_uid"))
					.additionalDetails(Optional.ofNullable(rs.getString("document_additionaldetails"))
							.filter(s -> !s.isEmpty() && !"null".equals(s))
							.<Object>map(add -> new Gson().fromJson(add, Object.class)).orElse(null))
					.auditDetails(auditdetails).build();
			demolition.addDocuments(document);
		}

	}

	/**
	 * @param rs
	 * @param demolition
	 * @throws SQLException
	 */
	private void addBlockInfoToDemolitions(ResultSet rs, Demolition demolition) throws SQLException {
		String blockInfoId = rs.getString("blockinfo_id");
		List<BlockInfo> blockInfoList = demolition.getLandInfo().getBlockInfo();

		if (!ObjectUtils.isEmpty(blockInfoId)) {
			if (!CollectionUtils.isEmpty(blockInfoList)) {
				for (BlockInfo blockInfo : blockInfoList) {
					if (blockInfo.getId().equals(blockInfoId))
						return;
				}
			}

			AuditDetails auditDetails = AuditDetails.builder().createdBy(rs.getString("blockinfo_createdby"))
					.createdTime(rs.getLong("blockinfo_createdtime"))
					.lastModifiedBy(rs.getString("blockinfo_lastmodifiedby"))
					.lastModifiedTime(rs.getLong("blockinfo_lastmodifiedtime")).build();

			BlockInfo blockInfo = BlockInfo.builder().id(blockInfoId).tenantId(rs.getString("blockinfo_tenantid"))
					.demolitionLandInfoId(rs.getString("blockinfo_demolitionlandinfoid"))
					.anyApprovedArea(rs.getString("blockinfo_anyapprovedarea"))
					.occupancy(rs.getString("blockinfo_occupancy")).totalBUA(rs.getString("blockinfo_totalbua"))
					.noOfFloors(rs.getString("blockinfo_nooffloors"))
					.setbackDetails(Optional.ofNullable(rs.getString("blockinfo_setbackdetails"))
							.filter(s -> !s.isEmpty() && !"null".equals(s))
							.<Object>map(add -> new Gson().fromJson(add, Object.class)).orElse(null))
					.buildingDistance(rs.getString("blockinfo_buildingdistance"))
					.buildingHeight(rs.getString("blockinfo_buildingheight"))
					.totalBUAToBeDemolished(rs.getString("blockinfo_totalbuatobedemolished")).auditDetails(auditDetails)
					.build();
			demolition.getLandInfo().addBlockInfo(blockInfo);
		}

	}

	/**
	 * @param rs
	 * @param demolition
	 * @throws SQLException
	 */
	private void addOwnersToDemolitions(ResultSet rs, Demolition demolition) throws SQLException {
		String ownerId = rs.getString("owner_id");
		List<OwnerInfo> owners = demolition.getOwners();

		if (!ObjectUtils.isEmpty(ownerId)) {
			if (!CollectionUtils.isEmpty(owners)) {
				for (OwnerInfo ownerInfo : owners) {
					if (ownerInfo.getOwnerId().equals(ownerId))
						return;
				}
			}

			AuditDetails ownersAuditDetails = AuditDetails.builder().createdBy(rs.getString("owner_createdby"))
					.createdTime(rs.getLong("owner_createdtime")).lastModifiedBy(rs.getString("owner_lastmodifiedby"))
					.lastModifiedTime(rs.getLong("owner_lastmodifiedtime")).build();

			Object ownersAdditionalDetails = null;

			if (rs.getString("owner_additionaldetails") != null) {
				ownersAdditionalDetails = new Gson().fromJson(rs.getString("owner_additionaldetails").equals("{}")
						|| rs.getString("owner_additionaldetails").equals("null") ? null
								: rs.getString("owner_additionaldetails"),
						Object.class);
			}

			OwnerInfo ownerInfo = OwnerInfo.builder().ownerId(ownerId).uuid(rs.getString("owner_uuid"))
					.isPrimaryOwner(rs.getBoolean("owner_isprimaryowner"))
					.ownerShipPercentage(rs.getBigDecimal("owner_ownershippercentage"))
					.institutionId(rs.getString("owner_institutionid"))
					.relationship(Relationship.fromValue(rs.getString("owner_relationship")))
					.additionalDetails(ownersAdditionalDetails).auditDetails(ownersAuditDetails).build();

			demolition.addOwners(ownerInfo);

		}
	}

}
