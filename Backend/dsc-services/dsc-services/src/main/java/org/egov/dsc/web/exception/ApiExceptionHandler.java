package org.egov.dsc.web.exception;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.egov.common.contract.response.ResponseInfo;
import org.egov.dsc.web.contract.factory.ResponseInfoFactory;
import org.egov.tracer.model.Error;
import org.egov.tracer.model.ErrorRes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns request problems into DIGIT-style {@link ErrorRes} bodies instead of Spring's generic
 * error page, and logs unexpected failures while the request's MDC (correlation/user id) is still
 * set. Validation error codes follow the tracer convention, e.g. {@code NotBlank.dataSignRequest.channelId}.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatus status, WebRequest request) {
		List<Error> errors = new ArrayList<>();
		for (ObjectError objectError : ex.getBindingResult().getAllErrors()) {
			String[] codes = objectError.getCodes();
			String field = (objectError instanceof FieldError) ? ((FieldError) objectError).getField() : null;
			errors.add(new Error(codes == null ? null : codes[0], objectError.getDefaultMessage(), field,
					field == null ? null : Collections.singletonList(field)));
		}
		// Messages only, never rejected values: the body can carry the token passphrase
		log.warn("Rejected invalid request: {}",
				errors.stream().map(e -> e.getDescription() + " " + e.getMessage()).collect(Collectors.joining("; ")));
		return errorResponse(HttpStatus.BAD_REQUEST, errors);
	}

	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
			HttpHeaders headers, HttpStatus status, WebRequest request) {
		log.warn("Rejected unreadable request body: {}", ex.getMostSpecificCause().getClass().getSimpleName());
		return errorResponse(HttpStatus.BAD_REQUEST, Collections.singletonList(
				new Error("InvalidRequestBody", "Request body is missing or is not valid JSON", null, null)));
	}

	/** Remaining standard Spring MVC errors (405, 415, ...) keep their status but get an ErrorRes body. */
	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
			HttpStatus status, WebRequest request) {
		if (body == null) {
			ErrorRes errorRes = errorBody(Collections.singletonList(
					new Error(ex.getClass().getSimpleName(), status.getReasonPhrase(), null, null)));
			errorRes.getResponseInfo().setStatus(status.toString());
			body = errorRes;
		}
		return super.handleExceptionInternal(ex, body, headers, status, request);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Object> handleUnexpected(Exception ex) {
		log.error("Unhandled exception while processing request", ex);
		return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, Collections.singletonList(new Error("DSC_INTERNAL_ERROR",
				"Unexpected error. Quote the x-correlation-id response header when reporting it.", null, null)));
	}

	private ResponseEntity<Object> errorResponse(HttpStatus status, List<Error> errors) {
		ErrorRes body = errorBody(errors);
		body.getResponseInfo().setStatus(status.toString());
		return new ResponseEntity<>(body, status);
	}

	private ErrorRes errorBody(List<Error> errors) {
		ResponseInfo responseInfo = ResponseInfoFactory.createResponseInfoFromRequestInfo(null, false);
		ErrorRes errorRes = new ErrorRes();
		errorRes.setResponseInfo(responseInfo);
		errorRes.setErrors(errors);
		return errorRes;
	}
}
