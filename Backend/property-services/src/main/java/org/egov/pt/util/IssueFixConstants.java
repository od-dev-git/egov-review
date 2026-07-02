package org.egov.pt.util;

import java.util.Arrays;
import java.util.List;

import org.egov.pt.models.enums.CreationReason;

public class IssueFixConstants {

    public IssueFixConstants() {
    }

    public static final String PENDING_FOR_PAYMENT = "PENDING_FOR_PAYMENT";

    public static final String IS_DEMAND_DETAILS_UPDATE_NEEDED = "IS_DEMAND_DETAILS_UPDATE_NEEDED";

    public static final String IS_DEMAND_UPDATE_NEEDED = "IS_DEMAND_UPDATE_NEEDED";
    
    public static final String IS_BILL_EXPIRATION_NEEDED = "IS_BILL_EXPIRATION_NEEDED";

    public static final String IS_WORKFLOW_UPDATE_NEEDED = "IS_WORKFLOW_UPDATE_NEEDED";
    
    public static final String IS_WORKFLOW_DELETE_NEEDED = "IS_WORKFLOW_DELETE_NEEDED";

    public static final String IS_APPLICATION_UPDATE_NEEDED = "IS_APPLICATION_UPDATE_NEEDED";

    public static final String PT ="PT";

    public static final String APPLY = "SUBMIT_APPLICATION";
    
    public static final String APPROVE = "APPROVE";

    public static final String PAY = "PAY";

    public static final String PAY_STATUS ="045950d9-b814-4ac7-8f60-c7aca38c4ac8";

    public static final String STATUS_ACTIVE ="ACTIVE";
    
    public static final String STATUS_INWORKFLOW = "INWORKFLOW";
    
    public static final String STATUS_DEACTIVATED = "DEACTIVATED";
	
	public static final String STATUS_INACTIVE = "INACTIVE";
	
	public static final String ACTION_APPROVE = "APPROVE";
	
	public static final String ACTION_FORWARD = "FORWARD";
	
	public static final String ACTION_SEND_BACK = "SEND_BACK";
	
	public static final String ACTION_SEND_BACK_TO_CITIZEN = "SEND_BACK_TO_CITIZEN";
	
	public static final String ACTION_VERIFY = "VERIFY";
	
	public static final String FIXED = "FIXED";
	
	// Allowed creation reasons for issue fix process
    public static final List<String> ALLOWED_CREATION_REASONS = Arrays.asList(
        CreationReason.CREATE.toString(), 
        CreationReason.UPDATE.toString(), 
        CreationReason.MUTATION.toString()
    );

    // Allowed statuses for 'CREATE' and 'UPDATE'
    public static final List<String> ALLOWED_ACTION_CREATE_UPDATE = Arrays.asList(
        "VERIFY", "FORWARD", "REJECT"
    );

    // Allowed statuses for 'MUTATION'
    public static final List<String> ALLOWED_ACTION_MUTATE = Arrays.asList(
        "VERIFY", "FORWARD", "REJECT", "APPROVE"
    );
	
}
