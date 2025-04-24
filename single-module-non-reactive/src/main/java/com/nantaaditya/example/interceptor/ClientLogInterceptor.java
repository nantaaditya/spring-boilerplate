package com.nantaaditya.example.interceptor;

import com.google.gson.Gson;
import com.nantaaditya.example.helper.MaskingHelper;
import com.nantaaditya.example.model.constant.ClientLogFormat;
import com.nantaaditya.example.model.dto.ClientLogResponse;
import com.nantaaditya.example.properties.ClientProperties;
import com.nantaaditya.example.properties.LogProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class ClientLogInterceptor implements ClientHttpRequestInterceptor {

	private static final String BREAKPOINT = "\n";

	private final Gson gson;
	private final Set<String> maskingKeys;
	private final ClientLogFormat logFormat;

	public ClientLogInterceptor(Gson gson, LogProperties logProperties, ClientProperties clientProperties) {
		this.gson = gson;
		this.logFormat = clientProperties.logFormat();
		this.maskingKeys = new HashSet<>(logProperties.getSensitiveFields());
	}

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
			throws IOException {

		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		logRequest(request, body);

		ClientHttpResponse response = execution.execute(request, body);
		ClientLogResponse wrappedResponse = new ClientLogResponse(response);

		logResponse(wrappedResponse, stopWatch);
		return wrappedResponse;
	}

	private void logRequest(HttpRequest request, byte[] body) {
		if (ClientLogFormat.HTTP == logFormat) {
			logHttpRequest(request, body);
		}

		if (ClientLogFormat.JSON == logFormat) {
			logJsonRequest(request, body);
		}
	}

	private void logResponse(ClientLogResponse response, StopWatch stopWatch) throws IOException {
		stopWatch.stop();
		if (ClientLogFormat.HTTP == logFormat) {
			logHttpResponse(response, stopWatch);
		}

		if (ClientLogFormat.JSON == logFormat) {
			logJsonResponse(response, stopWatch);
		}
	}

	// --- HTTP FORMAT LOGGING ---

	private void logHttpRequest(HttpRequest request, byte[] body) {
		StringBuilder logBuilder = new StringBuilder();

		logBuilder.append(String.format("%s %s", request.getMethod(), request.getURI()));
		appendMaskedHeaders(logBuilder, request.getHeaders());
		appendMaskedBody(logBuilder, new String(body));

		log.info(logBuilder.toString());
	}

	private void logHttpResponse(ClientLogResponse response, StopWatch stopWatch) throws IOException {
		StringBuilder logBuilder = new StringBuilder();

		logBuilder.append(BREAKPOINT)
				.append("Duration : ").append(stopWatch.getTime(TimeUnit.MILLISECONDS)).append(" ms")
				.append(BREAKPOINT)
				.append(response.getStatusCode());

		appendMaskedHeaders(logBuilder, response.getHeaders());

		byte[] responseBody = response.getBodyBytes();
		if (responseBody != null) {
			appendMaskedBody(logBuilder, new String(responseBody));
		}

		log.info(logBuilder.toString());
	}

	private void logJsonRequest(HttpRequest request, byte[] body) {
		Map<String, Object> logMap = new LinkedHashMap<>();
		logMap.put("method", request.getMethod());
		logMap.put("uri", request.getURI());
		logMap.put("headers", getMaskedHeaders(request.getHeaders()));
		logMap.put("body", maskJsonBody(new String(body)));

		log.info(gson.toJson(logMap));
	}

	private void logJsonResponse(ClientLogResponse response, StopWatch stopWatch) throws IOException {
		Map<String, Object> logMap = new LinkedHashMap<>();
		logMap.put("duration", stopWatch.getTime(TimeUnit.MILLISECONDS) + " ms");
		logMap.put("http code", response.getStatusCode());
		logMap.put("headers", getMaskedHeaders(response.getHeaders()));

		String body = new String(response.getBodyBytes(), StandardCharsets.UTF_8);
		if (StringUtils.hasText(body)) {
			logMap.put("body", maskJsonBody(body));
		}

		log.info(gson.toJson(logMap));
	}

	// --- UTILS ---

	private void appendMaskedHeaders(StringBuilder sb, HttpHeaders headers) {
		headers.forEach((key, values) -> {
			sb.append(BREAKPOINT).append(key).append(": ");
			if (maskingKeys.contains(key)) {
				values.forEach(value -> sb.append(MaskingHelper.masking(value)));
			} else {
				sb.append(values);
			}
		});
	}

	private Map<String, List<String>> getMaskedHeaders(HttpHeaders headers) {
		Map<String, List<String>> masked = new HashMap<>();
		headers.forEach((key, values) -> {
			List<String> processed = new ArrayList<>();
			for (String value : values) {
				processed.add(maskingKeys.contains(key) ? MaskingHelper.masking(value) : value);
			}
			masked.put(key, processed);
		});
		return masked;
	}

	private void appendMaskedBody(StringBuilder sb, String body) {
		sb.append(BREAKPOINT).append(maskJsonBody(body));
	}

	private String maskJsonBody(String body) {
		return MaskingHelper.maskingJson(gson, maskingKeys, body);
	}
}
