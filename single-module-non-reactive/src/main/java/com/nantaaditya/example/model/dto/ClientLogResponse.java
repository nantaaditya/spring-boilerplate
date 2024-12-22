package com.nantaaditya.example.model.dto;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

@Slf4j
public class ClientLogResponse implements ClientHttpResponse {

	private ClientHttpResponse parent;
	private byte[] bytes;
	private ByteArrayInputStream bis;

	public ClientLogResponse(ClientHttpResponse response) {
		this.parent = response;
		try {
			this.bytes = StreamUtils.copyToByteArray(response.getBody());
			this.bis = new ByteArrayInputStream(this.bytes);
		} catch (IOException ioe) {
			log.error("#RestClient - error http client response, ", ioe);
		}
	}

	@Override
	public InputStream getBody() throws IOException {
		return this.bis;
	}
	
	public byte[] getBodyBytes() {
		return this.bytes;
	}

	@Override
	public HttpHeaders getHeaders() {
		return this.parent.getHeaders();
	}

	@Override
	public HttpStatusCode getStatusCode() throws IOException {
		return this.parent.getStatusCode();
	}

	@Override
	public int getRawStatusCode() throws IOException {
		return this.parent.getRawStatusCode();
	}

	@Override
	public String getStatusText() throws IOException {
		return this.parent.getStatusText();
	}

	@Override
	public void close() {
		this.parent.close();		
	}

}
