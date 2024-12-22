package com.nantaaditya.example.interceptor;

import com.google.gson.Gson;
import com.nantaaditya.example.helper.MaskingHelper;
import com.nantaaditya.example.model.dto.ClientLogResponse;
import com.nantaaditya.example.properties.LogProperties;
import java.io.IOException;
import java.util.HashSet;
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

@Slf4j
@Component
public class ClientLogInterceptor implements ClientHttpRequestInterceptor{

	private final Gson gson;
	private final Set<String> maskingKeys = new HashSet<>();

	private static final String BREAKPOINT = "\n";

	public ClientLogInterceptor(Gson gson, LogProperties logProperties) {
		this.gson = gson;

		maskingKeys.addAll(logProperties.getSensitiveFields());
		log.debug("#SensitiveMasking - keys: {}", maskingKeys);
	}

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
			throws IOException {
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		logRequest(request, body);

		ClientHttpResponse response = execution.execute(request, body);
		ClientLogResponse clientLogResponse = new ClientLogResponse(response);

		logResponse(clientLogResponse, stopWatch);
		return clientLogResponse;
	}

	private void logRequest(HttpRequest request, byte[] body) throws IOException {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("%s %s", request.getMethod(), request.getURI()));
		maskHeaders(request.getHeaders(), sb);
		sb.append(BREAKPOINT);
		maskPayload(sb, new String(body));
		log.info(sb.toString());
	}

	private void logResponse(ClientLogResponse response, StopWatch stopWatch) throws IOException {
		stopWatch.stop();
		StringBuilder sb = new StringBuilder();
		sb.append(BREAKPOINT);
		sb.append(String.format("Duration : %d ms", (stopWatch.getTime(TimeUnit.MILLISECONDS))));
		sb.append(BREAKPOINT);
		sb.append(response.getStatusCode());
		maskHeaders(response.getHeaders(), sb);

		sb.append(BREAKPOINT);
		byte[] responseBytes = response.getBodyBytes();
		if (responseBytes != null) {
			maskPayload(sb, new String(responseBytes));
		}
		log.info(sb.toString());
	}

	private void maskHeaders(HttpHeaders headers, StringBuilder sb) {
		headers.forEach((k,v) -> {
			sb.append(BREAKPOINT);
			sb.append(k);
			sb.append(":");
			if (maskingKeys.contains(k)) {
				v.forEach(item -> sb.append(MaskingHelper.masking(item)));
			} else {
				sb.append(v);
			}
		});
	}

	private void maskPayload(StringBuilder sb, String body) {
		String content = MaskingHelper.maskingJson(gson, maskingKeys, body);
		sb.append(content);
	}

}
