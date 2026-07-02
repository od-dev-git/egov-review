package org.egov.bpa.web.model.demolition;

import java.util.ArrayList;
import java.util.List;

import org.egov.bpa.web.model.AuditDetails;
import org.egov.bpa.web.model.landInfo.Address;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minidev.json.JSONObject;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DemolitionLandInfo {
	
	@JsonProperty("id")
	@Default
	private String id = null;

	@JsonProperty("tenantId")
	@Default
	private String tenantId = null;
	
	@JsonProperty("demolitionId")
	@Default
	private String demolitionId = null;
	
	@JsonProperty("totalLandArea")
	@Default
	private String totalLandArea = null;
	
	@JsonProperty("plotNumber")
	@Default
	private String plotNumber = null;
	
	@JsonProperty("mauza")
	@Default
	private String mauza = null;
	
	@JsonProperty("landOwnerName")
	@Default
	private String landOwnerName = null;
	
	@JsonProperty("address")
	@Default
	private Address address = null;
	
	@JsonProperty("blockInfo")
	@Default
	private List<BlockInfo> blockInfo = null;
	
	@JsonProperty("newBlockInfo")
	@Default
	private List<BlockInfo> newBlockInfos = null;
	
	@JsonProperty("auditDetails")
	@Default
	private AuditDetails auditDetails = null;
	
	 public DemolitionLandInfo addBlockInfo(BlockInfo blockInfo) {
	        if (this.blockInfo == null) {
	            this.blockInfo = new ArrayList<>();
	        }

	        if (blockInfo != null) {
	            this.blockInfo.add(blockInfo);
	        }
	        return this;
	    }

}
