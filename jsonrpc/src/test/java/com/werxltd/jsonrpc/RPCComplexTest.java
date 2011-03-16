package com.werxltd.jsonrpc;

import junit.framework.TestCase;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.testing.ServletTester;

public class RPCComplexTest extends TestCase {
	private static ServletTester tester;

	protected void setUp() throws Exception {
		tester = new ServletTester();
		tester.setContextPath("/");
		ServletHolder sh = tester.addServlet(RPC.class, "/api");
		sh.setInitParameter("rpcclasses", "com.werxltd.jsonrpc.ClassA,com.werxltd.jsonrpc.ClassB");
		sh.setInitParameter("use_full_classname", "true");
		sh.setInitParameter("expose_methods", "true");
		tester.start();
	}

	protected void tearDown() {
		// release objects under test here, if necessary
		tester = null;
	}

	private void checkHeader(String[] chunks) {
		assertEquals(5, chunks.length);
		assertEquals("HTTP/1.1 200 OK", chunks[0]);
		assertEquals("Content-Type: text/plain; charset=iso-8859-1", chunks[1]);
		// assertEquals("Content-Length: 877", chunks[2]);
		assertTrue(chunks[2].matches("^Content-Length: \\d+$"));
		assertEquals("", chunks[3]);
	}

	public void testListMethods() throws Exception {
		String requests = "GET /api HTTP/1.1\r\n" + "Host: tester\r\n" + "\r\n";

		String responses = tester.getResponses(requests);

		String chunks[] = responses.split("\\r\\n");

		checkHeader(chunks);

		JSONObject jsonObj = new JSONObject(chunks[4]);

		assertNotNull(jsonObj);
		assertTrue(jsonObj.has("jsonrpc"));
		assertEquals("2.0", jsonObj.getString("jsonrpc"));

		JSONObject resultObj = jsonObj.getJSONObject("result");
		assertNotNull(resultObj);

		JSONArray methodArr = resultObj.getJSONArray("method");
		assertNotNull(methodArr);
		assertEquals(3, methodArr.length());

		for (int i = 0; i < methodArr.length(); i++) {
			JSONObject methodObj = methodArr.getJSONObject(i);
			assertNotNull(methodObj);

			assertTrue(methodObj.has("class"));
			assertTrue(methodObj.has("name"));
			assertTrue(methodObj.has("params"));
			assertTrue(methodObj.has("returns"));
			assertTrue(methodObj.has("static"));
		}
	}

	public void testTestClassAString() throws Exception {

		String requests = "GET /api?method=com.werxltd.jsonrpc.ClassB.test HTTP/1.1\r\n"
				+ "Host: tester\r\n" + "\r\n";

		String responses = tester.getResponses(requests);

		String chunks[] = responses.split("\\r\\n");

		checkHeader(chunks);

		JSONObject jsonObj = new JSONObject(chunks[4]);

		assertNotNull(jsonObj);

		assertTrue(jsonObj.has("jsonrpc"));
		assertEquals("2.0", jsonObj.getString("jsonrpc"));

		assertFalse(jsonObj.has("error"));
		assertTrue(jsonObj.has("result"));

		assertEquals("test from class B successful", jsonObj.getString("result"));
	}
}
