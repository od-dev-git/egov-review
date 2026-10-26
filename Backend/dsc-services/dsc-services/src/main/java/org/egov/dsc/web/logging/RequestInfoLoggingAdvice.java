package org.egov.dsc.web.logging;

import java.lang.reflect.Type;

import org.egov.common.contract.request.RequestInfo;
import org.egov.dsc.model.CertificateRequest;
import org.egov.dsc.model.DataSignRequest;
import org.egov.dsc.model.TokenRequest;
import org.slf4j.MDC;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

@ControllerAdvice
public class RequestInfoLoggingAdvice extends RequestBodyAdviceAdapter {

	@Override
	public boolean supports(MethodParameter methodParameter, Type targetType,
			Class<? extends HttpMessageConverter<?>> converterType) {
		return true;
	}

	@Override
	public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter,
			Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
		RequestInfo requestInfo = null;
		if (body instanceof DataSignRequest) {
			requestInfo = ((DataSignRequest) body).getRequestInfo();
		} else if (body instanceof TokenRequest) {
			requestInfo = ((TokenRequest) body).getRequestInfo();
		} else if (body instanceof CertificateRequest) {
			requestInfo = ((CertificateRequest) body).getRequestInfo();
		}

		if (requestInfo != null) {
			if (requestInfo.getUserInfo() != null && requestInfo.getUserInfo().getId() != null) {
				MDC.put(LoggingContextFilter.MDC_USER_ID, String.valueOf(requestInfo.getUserInfo().getId()));
			}
			if (requestInfo.getMsgId() != null) {
				MDC.put(LoggingContextFilter.MDC_MSG_ID, requestInfo.getMsgId());
			}
		}
		return body;
	}
}
