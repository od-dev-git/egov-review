package org.egov.bpa.service.job;

import java.util.Collections;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.service.BPAAutoEscalationService;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class BPAAutoEscalationJob implements Job, InitializingBean {
	
	private static final RequestInfo requestInfo;
	
	@Autowired
    private BPAConfiguration bpaConfiguration;
	@Autowired
	private BPAAutoEscalationService autoEscalationService;
	
	static {
		User userInfo = User.builder().uuid("DAILY_AUTO_ESCALATION").type("SYSTEM").roles(Collections.emptyList()).id(0L)
				.build();
//		requestInfo = new RequestInfo("", "", 0L, "", "", "", "", "", "", userInfo);
		/* FIX: Show cause notice fix. Remove ts from RequestInfo */
		requestInfo = RequestInfo.builder().userInfo(userInfo).apiId("").action("").did("").key("").msgId("").ver("").correlationId("").build();
		requestInfo.setAuthToken("90cd13b4-e2bd-4100-9544-5f4e8b3d39dc");
	}

	@Override
	public void afterPropertiesSet() throws Exception {
    	User userInfo = User.builder()
                .uuid(bpaConfiguration.getBpaAutoEscalationUserUuid())
                .type(bpaConfiguration.getBpaAutoEscalationUserType())
                .roles(Collections.emptyList()).id(0L).build();

        requestInfo.setUserInfo(userInfo);
	}

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		if (bpaConfiguration.isBpaAutoEscalationSchedularEnable()) {
			log.info("BPA Escalation job execution started ....");
			autoEscalationService.processEscalation(requestInfo);
			log.info("BPA Escalation job execution END ....");
		}
		
	}

}
