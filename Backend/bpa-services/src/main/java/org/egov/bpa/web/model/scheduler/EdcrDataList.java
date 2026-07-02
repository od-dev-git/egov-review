package org.egov.bpa.web.model.scheduler;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class EdcrDataList {

	@JsonProperty("edcrDataList")
	private List<EdcrData> edcrDataList;
}
