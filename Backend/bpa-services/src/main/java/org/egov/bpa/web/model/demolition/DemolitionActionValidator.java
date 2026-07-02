package org.egov.bpa.web.model.demolition;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import org.egov.bpa.service.DemolitionWorkflowService;
import org.egov.bpa.util.DemolitionConstants;
import org.egov.bpa.util.RegularizationConstants;
import org.egov.bpa.web.model.workflow.Action;
import org.egov.bpa.web.model.workflow.BusinessService;
import org.egov.bpa.web.model.workflow.State;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.Role;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class DemolitionActionValidator {
	
	@Autowired
	private DemolitionWorkflowService workflowService;

	public void validateUpdateRequest(@Valid DemolitionRequest request, BusinessService businessService) {

		validateRoleAction(request, businessService);
		validateIds(request, businessService);

	}

	private void validateIds(@Valid DemolitionRequest request, BusinessService businessService) {

		Map<String, String> errorMap = new HashMap<>();
		Demolition demolition = request.getDemolition();

		if (!workflowService.isStateUpdatable(demolition.getStatus(), businessService)) {
			if (demolition.getId() == null) {
				errorMap.put("update_error", "Id of Application cannot be null");
			}

			if (!CollectionUtils.isEmpty(demolition.getDocuments())) {
				demolition.getDocuments().forEach(document -> {
					if (document.getId() == null)
						errorMap.put("update_error", "Id of applicationDocument cannot be null");
				});
			}

		}
		if (!errorMap.isEmpty())
			throw new CustomException(errorMap);
	}

	private void validateRoleAction(@Valid DemolitionRequest request, BusinessService businessService) {
		
		Demolition demolition = request.getDemolition();
		Map<String, String> errorMap = new HashMap<>();
		RequestInfo requestInfo = request.getRequestInfo();

		State state = workflowService.getCurrentStateObj(demolition.getStatus(), businessService);
		if (state != null) {
			List<Action> actions = state.getActions();
			List<Role> roles = requestInfo.getUserInfo().getRoles();
			List<String> validActions = new LinkedList<>();

			roles.forEach(role -> {
				actions.forEach(action -> {
					if (action.getRoles().contains(role.getCode())) {
						validActions.add(action.getAction());
					}
				});
			});

			if (!validActions.contains(demolition.getWorkflow().getAction())) {
				errorMap.put(RegularizationConstants.UNAUTHORIZED_UPDATE,
						"The action cannot be performed by this user");
			}
		} else {
			errorMap.put(RegularizationConstants.UNAUTHORIZED_UPDATE,
					"No workflow state configured for the current status of the application");
		}

		if (!errorMap.isEmpty()) {
			throw new CustomException(errorMap);
		}
	}
	
	public void validateDemolitionDeletion(Demolition demolition) {

		if (!demolition.getStatus().equalsIgnoreCase(DemolitionConstants.INITIATED)) {
			throw new CustomException("Delete Demolition Error",
					"Application is not allowed to delete after Submission");
		}

	}

}
