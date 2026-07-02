package org.egov.bpa.service.job;

import org.egov.bpa.config.BPAConfiguration;
import org.quartz.JobDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;

@Configuration
//@ConditionalOnProperty(
//	    value="bpa.auto.escalation.enable", 
//	    havingValue = "true", 
//	    matchIfMissing = false)
public class BPAAutoEscalationJobConfig {
	
	@Autowired
	private BPAConfiguration bpaConfiguration;

	@Bean
	JobDetailFactoryBean processStatusUpdateJob() {
		JobDetailFactoryBean jobDetailFactory = new JobDetailFactoryBean();
		jobDetailFactory.setJobClass(BPAAutoEscalationJob.class);
		jobDetailFactory.setGroup("auto-escalation");
		jobDetailFactory.setDurability(true);
		return jobDetailFactory;
	}

	@Bean
	@Autowired
	CronTriggerFactoryBean processStatusUpdateTrigger(JobDetail processStatusUpdateJob) {
		CronTriggerFactoryBean cronTriggerFactoryBean = new CronTriggerFactoryBean();
		cronTriggerFactoryBean.setJobDetail(processStatusUpdateJob);
//		cronTriggerFactoryBean.setCronExpression("0/20 * * * * ?");
		cronTriggerFactoryBean.setCronExpression(bpaConfiguration.getBpaAutoEscalationCronexpression());
		cronTriggerFactoryBean.setGroup("auto-escalation");
		return cronTriggerFactoryBean;
	}

}
