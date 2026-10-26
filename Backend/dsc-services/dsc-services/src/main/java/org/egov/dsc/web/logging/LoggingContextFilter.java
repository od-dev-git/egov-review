package org.egov.dsc.web.logging;

import java.io.IOException;
import java.util.UUID;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Puts a per-request correlation id into the logging MDC so every log line of a request,
 * including ones from emBridge, Axis and Spring, can be grouped. Uses the gateway's
 * {@code x-correlation-id} when present, otherwise generates one, and echoes it back so the
 * caller can quote it. {@link RequestInfoLoggingAdvice} adds the user id once the body is read.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LoggingContextFilter extends OncePerRequestFilter {

	public static final String CORRELATION_ID_HEADER = "x-correlation-id";
	public static final String MDC_CORRELATION_ID = "CORRELATION_ID";
	public static final String MDC_USER_ID = "USER_ID";
	public static final String MDC_MSG_ID = "MSG_ID";

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String correlationId = request.getHeader(CORRELATION_ID_HEADER);
		if (!StringUtils.hasText(correlationId)) {
			correlationId = UUID.randomUUID().toString();
		}
		MDC.put(MDC_CORRELATION_ID, correlationId);
		response.setHeader(CORRELATION_ID_HEADER, correlationId);
		try {
			chain.doFilter(request, response);
		} finally {
			MDC.remove(MDC_CORRELATION_ID);
			MDC.remove(MDC_USER_ID);
			MDC.remove(MDC_MSG_ID);
		}
	}
}
