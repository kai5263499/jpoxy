package com.werxltd.jsonrpc;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.JSONObject;

import com.werxltd.jsonrpc.events.JSONRPCEventListener;
import com.werxltd.jsonrpc.events.JSONRPCMessage;
import com.werxltd.jsonrpc.events.JSONRPCMessageEvent;

public class Example implements JSONRPCEventListener {
	private ServletConfig config;
	private HttpSession session;
	private HttpServletRequest request;
	private HttpServletResponse response;
	private Response lastresponse;
	
	public Example() {
		System.out.println("Query init");
	}
	
	public static String test() {
		return "test successful";
	}

	public float add(float a, float b) {
		return a + b;
	}

	public float sub(float a, float b) {
		return a - b;
	}
	
	public String list(int a, int b, int c) {
		return "a:"+a+" b:"+b+" c:"+c;
	}
	
	public String echo(String s) {
		return s;
	}

	public String echoJson(JSONObject obj) throws UnsupportedEncodingException {
		return URLEncoder.encode(obj.toString(), "UTF-8");
	}
	
	public Integer sessionCounter() {
		Integer count = (Integer) session.getAttribute("count");
		
		if(count == null) count = 1;
		else count += 1;
		
		System.out.println("count is: "+count);
		
		session.setAttribute("count", count);
		
		return (Integer) session.getAttribute("count");
	}
	
	public String getLastResponse() {
		if(lastresponse != null) return (String) lastresponse.getResult();
		return "";
	}
	
	public String getConfig() {
		if(config != null) return config.getInitParameter("rpcclasses");
		return "";
	}
	
	public void messageReceived(JSONRPCMessageEvent me) {
		switch(me.message().getCode()) {
			case JSONRPCMessage.INIT:
				config = me.message().getServletConfig();
			break;
			case JSONRPCMessage.BEFOREREQUEST:
				request = me.message().getRequest();
				session = request.getSession(true);
			break;
			case JSONRPCMessage.BEFORERESPONSE:
				response = me.message().getHttpResponse();
				response.setContentType("text/html");
			break;
			case JSONRPCMessage.AFTERRESPONSE:
				lastresponse = me.message().getRPCResponse();
			break;
		}
	}
}
