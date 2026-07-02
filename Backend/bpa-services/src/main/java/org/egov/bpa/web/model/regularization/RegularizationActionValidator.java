package org.egov.bpa.web.model.regularization;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.egov.bpa.service.RegularizationWorkflow;
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
public class RegularizationActionValidator {


	@Autowired
	private RegularizationWorkflow workflowService;

	/**
	 * Workflow actions related validations
	 * 
	 * @param request
	 * @param businessService
	 */
	public void validateUpdateRequest(RegularizationRequest request, BusinessService businessService) {
		validateRoleAction(request,businessService);
		validateIds(request, businessService);
	}

	/**
	 * Validate if the user is authorize to perform the specified action
	 * 
	 * @param request
	 * @param businessService
	 */
	private void validateRoleAction(RegularizationRequest request, BusinessService businessService) {

		Regularization regularization = request.getRegularization();
		Map<String, String> errorMap = new HashMap<>();
		RequestInfo requestInfo = request.getRequestInfo();

		State state = workflowService.getCurrentStateObj(regularization.getStatus(), businessService);
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

			if (!validActions.contains(regularization.getWorkflow().getAction())) {
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

	/**
	 * Validate the ids of application and documents here
	 * 
	 * @param request
	 * @param businessService
	 */
	private void validateIds(RegularizationRequest request, BusinessService businessService) {
		Map<String, String> errorMap = new HashMap<>();
		Regularization regularization = request.getRegularization();

		if (!workflowService.isStateUpdatable(regularization.getStatus(), businessService)) {
			if (regularization.getId() == null) {
				errorMap.put(RegularizationConstants.INVAILD_UPDATE, "Id of Application cannot be null");
			}

			if (!CollectionUtils.isEmpty(regularization.getDocuments())) {
				regularization.getDocuments().forEach(document -> {
					if (document.getId() == null)
						errorMap.put(RegularizationConstants.INVAILD_UPDATE,
								"Id of applicationDocument cannot be null");
				});
			}

		}
		if (!errorMap.isEmpty())
			throw new CustomException(errorMap);
	}

}