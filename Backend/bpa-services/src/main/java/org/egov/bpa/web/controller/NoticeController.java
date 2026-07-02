package org.egov.bpa.web.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.validation.Valid;

import org.egov.bpa.service.BPAShowCauseNoticeService;
import org.egov.bpa.service.Noticeservice;

import org.egov.bpa.util.BPAUtil;
import org.egov.bpa.util.ResponseInfoFactory;
import org.egov.bpa.web.model.BPARequest;
import org.egov.bpa.web.model.Notice;
import org.egov.bpa.web.model.NoticeRequest;
import org.egov.bpa.web.model.NoticeResponse;
import org.egov.bpa.web.model.NoticeSearchCriteria;
import org.egov.bpa.web.model.RequestInfoWrapper;
import org.egov.bpa.workflow.WorkflowIntegrator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/notice")
public class NoticeController {
	
	
	@Autowired
	private Noticeservice noticeService;
	
	
	
	@Autowired
	private BPAUtil bpaUtil;

	@Autowired
	private ResponseInfoFactory responseInfoFactory;

	@Autowired
	private BPAShowCauseNoticeService bpaShowCauseNoticeService;
	
	
	@PostMapping(value = "/_create")
	public ResponseEntity<NoticeResponse> create(
			@Valid @RequestBody NoticeRequest request) {
		bpaUtil.defaultJsonPathConfig();
		Notice notices = noticeService.create(request);
		List<Notice> notice = new ArrayList<Notice>();
		notice.add(notices);
		NoticeResponse response = NoticeResponse.builder().notice(notice)
				.responseInfo(responseInfoFactory
						.createResponseInfoFromRequestInfo(request.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	@PostMapping(value = "/_search")
	public ResponseEntity<NoticeResponse> search(@Valid @RequestBody RequestInfoWrapper requestInfoWrapper,
			@Valid @ModelAttribute NoticeSearchCriteria criteria) {
		List<Notice> notice = noticeService.searchNotice(criteria);
		NoticeResponse response = NoticeResponse.builder().notice(notice)
				.responseInfo(responseInfoFactory
						.createResponseInfoFromRequestInfo(requestInfoWrapper.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);
		
		
	}
	
	@PostMapping(value = "/_update")
	public ResponseEntity<NoticeResponse> update(
			@Valid @RequestBody NoticeRequest request) {
		Notice notices = noticeService.update(request);
		List<Notice> notice = new ArrayList<Notice>();
		notice.add(notices);
		NoticeResponse response = NoticeResponse.builder().notice(notice)
				.responseInfo(responseInfoFactory
						.createResponseInfoFromRequestInfo(request.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);

}
	
	@PostMapping(value = "/_noticeUpdate")
	public ResponseEntity<NoticeResponse> showCauseReply(@Valid @RequestBody NoticeRequest request) {
		Notice notices = noticeService.showCauseReply(request);
		List<Notice> notice = new ArrayList<Notice>();
		notice.add(notices);
		NoticeResponse response = NoticeResponse.builder().notice(notice)
				.responseInfo(responseInfoFactory.createResponseInfoFromRequestInfo(request.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);

	}
	
	@PostMapping(value = "/_pullApplication")
	public ResponseEntity<NoticeResponse> bpaApplicationPullBack(
			@Valid @RequestBody RequestInfoWrapper requestInfoWrapper, @RequestParam String applicationNo,
			@RequestParam String noticeId) {
		Notice notice = bpaShowCauseNoticeService.processBpaApplicationPullBack(requestInfoWrapper, applicationNo,
				noticeId);
		NoticeResponse response = NoticeResponse.builder().notice(Arrays.asList(notice)).responseInfo(
				responseInfoFactory.createResponseInfoFromRequestInfo(requestInfoWrapper.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	@PostMapping(value = "/refusal/scn/create")
	public ResponseEntity<NoticeResponse> refusalScnCreate(
			@Valid @RequestBody NoticeRequest request) {
		bpaUtil.defaultJsonPathConfig();
		NoticeRequest noticeRequest = bpaShowCauseNoticeService.createRefusalShowCauseNotice(request.getnotice(), request.getRequestInfo());
		List<Notice> notices = new ArrayList<Notice>();
		notices.add(noticeRequest.getnotice());
		NoticeResponse response = NoticeResponse.builder().notice(notices)
				.responseInfo(responseInfoFactory
						.createResponseInfoFromRequestInfo(request.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
}
