package org.jpoxy;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.jpoxy.events.JSONRPCEventListener;
import org.jpoxy.events.JSONRPCMessage;
import org.jpoxy.events.JSONRPCMessageEvent;

import org.json.JSONObject;

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
		}
	}
}
