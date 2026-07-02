package org.egov.bpa.web.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.validation.Valid;

import org.egov.bpa.service.RegularizationNoticeService;
import org.egov.bpa.service.RegularizationShowCauseNoticeService;
import org.egov.bpa.util.RegularizationUtil;
import org.egov.bpa.util.ResponseInfoFactory;
import org.egov.bpa.web.model.Notice;
import org.egov.bpa.web.model.NoticeRequest;
import org.egov.bpa.web.model.NoticeResponse;
import org.egov.bpa.web.model.NoticeSearchCriteria;
import org.egov.bpa.web.model.RequestInfoWrapper;
import org.egov.bpa.web.model.regularization.RegularizationRequest;
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
@RequestMapping("/v1/regularization/notice")
public class RegularizationNoticeController {

	@Autowired
	private RegularizationNoticeService noticeService;

	@Autowired
	private RegularizationUtil utils;

	@Autowired
	private ResponseInfoFactory responseInfoFactory;

	@Autowired
	private RegularizationShowCauseNoticeService regularizationShowCauseNoticeService;
	
	@PostMapping(value = "/_create")
	public ResponseEntity<NoticeResponse> create(@Valid @RequestBody NoticeRequest request) {
		utils.defaultJsonPathConfig();
		Notice notices = noticeService.createNotice(request);
		List<Notice> notice = new ArrayList<Notice>();
		notice.add(notices);
		NoticeResponse response = NoticeResponse.builder().notice(notice)
				.responseInfo(responseInfoFactory.createResponseInfoFromRequestInfo(request.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PostMapping(value = "/_search")
	public ResponseEntity<NoticeResponse> search(@Valid @RequestBody RequestInfoWrapper requestInfoWrapper,
			@Valid @ModelAttribute NoticeSearchCriteria criteria) {
		List<Notice> notice = noticeService.searchNotice(criteria);
		NoticeResponse response = NoticeResponse.builder().notice(notice).responseInfo(
				responseInfoFactory.createResponseInfoFromRequestInfo(requestInfoWrapper.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);

	}

	@PostMapping(value = "/_update")
	public ResponseEntity<NoticeResponse> update(@Valid @RequestBody NoticeRequest request) {
		Notice notices = noticeService.updateNotice(request);
		List<Notice> notice = new ArrayList<Notice>();
		notice.add(notices);
		NoticeResponse response = NoticeResponse.builder().notice(notice)
				.responseInfo(responseInfoFactory.createResponseInfoFromRequestInfo(request.getRequestInfo(), true))
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
	public ResponseEntity<NoticeResponse> regularizationApplicationPullBack(
			@Valid @RequestBody RequestInfoWrapper requestInfoWrapper, @RequestParam String applicationNo,
			@RequestParam String noticeId) {
		Notice notice = regularizationShowCauseNoticeService.processRegularizationApplicationPullBack(requestInfoWrapper,
				applicationNo, noticeId);
		NoticeResponse response = NoticeResponse.builder().notice(Arrays.asList(notice)).responseInfo(
				responseInfoFactory.createResponseInfoFromRequestInfo(requestInfoWrapper.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	@PostMapping(value = "/refusal/scn/create")
	public ResponseEntity<NoticeResponse> refusalShowCauseNoticecreate(@Valid @RequestBody NoticeRequest request) {
		utils.defaultJsonPathConfig();
		Notice notices = regularizationShowCauseNoticeService.createRefusalShowCauseNotice(request.getnotice(), request.getRequestInfo());
		List<Notice> notice = new ArrayList<Notice>();
		notice.add(notices);
		NoticeResponse response = NoticeResponse.builder().notice(notice)
				.responseInfo(responseInfoFactory.createResponseInfoFromRequestInfo(request.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

}
