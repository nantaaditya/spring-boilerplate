package com.nantaaditya.example.interceptor;

import com.google.gson.Gson;
import com.nantaaditya.example.helper.MaskingHelper;
import com.nantaaditya.example.model.constant.LogFormat;
import com.nantaaditya.example.model.dto.AppLogMessage;
import com.nantaaditya.example.model.dto.ClientLogResponse;
import com.nantaaditya.example.model.dto.JsonLogHttpRequest;
import com.nantaaditya.example.model.dto.JsonLogHttpResponse;
import com.nantaaditya.example.properties.LogProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

@Log4j2
@Component
public class ClientLogInterceptor implements ClientHttpRequestInterceptor {

	private static final String BREAKPOINT = "\n";

	private final Gson gson;
	private final Set<String> maskingKeys;
	private final LogFormat logFormat;

	public ClientLogInterceptor(Gson gson, LogProperties logProperties) {
		this.gson = gson;
		this.logFormat = logProperties.logFormat();
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
		if (LogFormat.TEXT == logFormat) {
			logHttpRequest(request, body);
		}

		if (LogFormat.JSON == logFormat) {
			logJsonRequest(request, body);
		}
	}

	private void logResponse(ClientLogResponse response, StopWatch stopWatch) throws IOException {
		stopWatch.stop();
		if (LogFormat.TEXT == logFormat) {
			logHttpResponse(response, stopWatch);
		}

		if (LogFormat.JSON == logFormat) {
			logJsonResponse(response, stopWatch);
		}
	}

	// --- HTTP FORMAT LOGGING ---

	private void logHttpRequest(HttpRequest request, byte[] body) {
		StringBuilder logBuilder = new StringBuilder();

		logBuilder.append(String.format("%s %s", request.getMethod(), request.getURI()));
		appendMaskedHeaders(logBuilder, request.getHeaders());
		appendMaskedBody(logBuilder, new String(body));

		log.info(AppLogMessage.create(logBuilder.toString()));
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

		log.info(AppLogMessage.create(logBuilder.toString()));
	}

	private void logJsonRequest(HttpRequest request, byte[] body) {
    JsonLogHttpRequest content = new JsonLogHttpRequest(
        request.getMethod().name(),
        request.getURI().toString(),
        getMaskedHeaders(request.getHeaders()),
        gson.fromJson(maskJsonBody(new String(body)), Map.class)
    );

		log.info(AppLogMessage.create("#Client", content));
	}

	private void logJsonResponse(ClientLogResponse response, StopWatch stopWatch) throws IOException {
    String body = new String(response.getBodyBytes(), StandardCharsets.UTF_8);
    JsonLogHttpResponse content = new JsonLogHttpResponse(
        null,
        null,
        response.getStatusCode().toString(),
        String.format("[%s] ms", stopWatch.getTime(TimeUnit.MILLISECONDS)),
        getMaskedHeaders(response.getHeaders()),
        !StringUtils.hasText(body) ? null : gson.fromJson(maskJsonBody(body), Map.class)
    );

		log.info(AppLogMessage.create("#Client", content));
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

	private MultiValueMap<String, String> getMaskedHeaders(HttpHeaders headers) {
		MultiValueMap<String, String> masked = new LinkedMultiValueMap<>();
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
