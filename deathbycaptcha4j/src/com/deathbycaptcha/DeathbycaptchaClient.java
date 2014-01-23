package com.deathbycaptcha;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;

public class DeathbycaptchaClient {
	public static final ObjectMapper mapper = new ObjectMapper();
	
	private final HttpRequestFactory requestFactory;
	private final Integer connectTimeout;
	private final Integer readTimeout;
	private final Endpoint endpoint;
	private final Login login;
	
	public static enum Endpoint {
		HTTPS("https://deathbycaptcha.com/api/"),
		HTTP("http://api.dbcapi.me/api/");
		
		public final String url;
		private Endpoint(String url) {
			this.url = url;
		}
	}
	
	
	public DeathbycaptchaClient(HttpRequestFactory requestFactory, Integer connectTimeout, Integer readTimeout,
			Endpoint endpoint, Login login) {
		this.requestFactory = requestFactory;
		this.connectTimeout = connectTimeout;
		this.readTimeout = readTimeout;
		this.endpoint = endpoint;
		this.login = login;
	}
	
	public CaptchaStatus submit(Captcha captcha) throws IOException, DeathbycaptchaException {
    	// Remove this when Google HTTP Java Client supports multipart/form-data
    	// @see https://code.google.com/p/google-http-java-client/issues/detail?id=107
        final String boundary = Long.toHexString(System.currentTimeMillis());
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
	        final PrintStream printer = new PrintStream(baos, false, "UTF-8");
	        try {
	        	if (login != null) {
	        		if (login.username != null) {
			            printer.println("--" + boundary);
			            printer.println("Content-Disposition: form-data; name=\"username\"");
			            printer.println();
			            printer.println(login.username);
	        		}
	
	        		if (login.password != null) {
			            printer.println("--" + boundary);
			            printer.println("Content-Disposition: form-data; name=\"password\"");
			            printer.println();
			            printer.println(login.password);
	        		}
	        	}

	        	if (captcha != null) {
		            printer.println("--" + boundary);
		            printer.println("Content-Disposition: form-data; name=\"captchafile\"; filename=\"" + captcha.filename + "\"");
		            printer.println("Content-Type: " + captcha.contentType);
		            printer.println("Content-Transfer-Encoding: binary");
		            printer.println();
		            printer.write(captcha.content);
		            printer.println();
	        	}
	
	            // End of multipart/form-data.
	            printer.println("--" + boundary + "--");
	        } finally {
	            printer.close();
	        }
        } finally {
        	baos.close();
        }
        
        final HttpContent content = new ByteArrayContent("multipart/form-data; boundary=" + boundary, baos.toByteArray());
		final HttpRequest request = requestFactory.buildPostRequest(new GenericUrl(endpoint.url + "captcha"), content);
        if (connectTimeout != null) {
        	request.setConnectTimeout(connectTimeout.intValue());
        }
        if (readTimeout != null) {
        	request.setReadTimeout(readTimeout);
        }
        request.getHeaders().setAccept("application/json");
        request.setThrowExceptionOnExecuteError(false);
        
        final HttpResponse response = request.execute();
        try {
        	if (!response.isSuccessStatusCode()) {
        		throw new DeathbycaptchaException(mapper.readValue(response.getContent(), Error.class));
        	}
        	return mapper.readValue(response.getContent(), CaptchaStatus.class);
        } finally {
        	response.ignore();
        }
	}
	
	public CaptchaStatus getStatus(Long id) throws IOException, DeathbycaptchaException {
		final HttpRequest request = requestFactory.buildGetRequest(new GenericUrl(endpoint.url + "captcha/" + id));
        if (connectTimeout != null) {
        	request.setConnectTimeout(connectTimeout.intValue());
        }
        if (readTimeout != null) {
        	request.setReadTimeout(readTimeout);
        }
        request.getHeaders().setAccept("application/json");
        request.setThrowExceptionOnExecuteError(false);
        
        final HttpResponse response = request.execute();
        try {
        	if (!response.isSuccessStatusCode()) {
        		throw new DeathbycaptchaException(mapper.readValue(response.getContent(), Error.class));
        	}
        	return mapper.readValue(response.getContent(), CaptchaStatus.class);
        } finally {
        	response.ignore();
        }
	}
}
