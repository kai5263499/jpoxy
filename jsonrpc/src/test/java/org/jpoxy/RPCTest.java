package org.jpoxy;

import java.net.URLDecoder;
import java.net.URLEncoder;

import junit.framework.TestCase;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.testing.ServletTester;

public class RPCTest extends TestCase {

    private static ServletTester tester;

    @Override
    protected void setUp() throws Exception {
        tester = new ServletTester();
        tester.setContextPath("/");
        ServletHolder sh = tester.addServlet(org.jpoxy.RPC.class, "/api");
        sh.setInitParameter("rpcclasses", "org.jpoxy.Example");
        sh.setInitParameter("expose_methods", "true");
        sh.setInitParameter("detailed_errors", "true");
        tester.start();
    }

    @Override
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
        assertEquals(9, methodArr.length());

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

    public void testInvalidMethod() throws Exception {
        JSONObject requestObj = new JSONObject();
        requestObj.put("method", "invalidmethod");
        String requests = "GET /api?json="
                + URLEncoder.encode(requestObj.toString(), "UTF-8")
                + " HTTP/1.1\r\n" + "Host: tester\r\n" + "\r\n";

        String responses = tester.getResponses(requests);

        String chunks[] = responses.split("\\r\\n");

        checkHeader(chunks);

        JSONObject jsonObj = new JSONObject(chunks[4]);

        assertNotNull(jsonObj);
        assertTrue(jsonObj.has("jsonrpc"));
        assertEquals("2.0", jsonObj.getString("jsonrpc"));

        assertTrue(jsonObj.has("error"));
        assertFalse(jsonObj.has("result"));

        JSONObject errorObj = jsonObj.getJSONObject("error");
        assertNotNull(errorObj);

        assertTrue(errorObj.has("code"));
        assertEquals(-32601, errorObj.getLong("code"));
        assertTrue(errorObj.has("data"));
        JSONObject dataObj = errorObj.getJSONObject("data");
        assertNotNull(dataObj);
        assertTrue(dataObj.has("classname"));
        assertTrue(dataObj.has("hashcode"));
        assertTrue(dataObj.has("stacktrace"));

        assertTrue(errorObj.has("message"));
    }

    public void testTestMethodReg() throws Exception {

        JSONObject requestObj = new JSONObject();
        requestObj.put("method", "test");
        String requests = "GET /api?json=" + requestObj.toString()
                + " HTTP/1.1\r\n" + "Host: tester\r\n" + "\r\n";

        String responses = tester.getResponses(requests);

        String chunks[] = responses.split("\\r\\n");

        checkHeader(chunks);

        JSONObject jsonObj = new JSONObject(chunks[4]);

        assertNotNull(jsonObj);

        assertTrue(jsonObj.has("jsonrpc"));
        assertEquals("2.0", jsonObj.getString("jsonrpc"));

        assertFalse(jsonObj.has("error"));
        assertTrue(jsonObj.has("result"));

        assertEquals("test successful", jsonObj.getString("result"));
    }

    public void testTestMethodGet() throws Exception {

        String requests = "GET /api?method=test HTTP/1.1\r\n"
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

        assertEquals("test successful", jsonObj.getString("result"));
    }

    public void testTestMethodPost() throws Exception {

        String requests = "POST /api HTTP/1.1\r\n" + "Host: tester\r\n"
                + "Content-Length: 11\r\n"
                + "Content-Type: application/x-www-form-urlencoded\r\n"
                + "\r\n" + "method=test\r\n";

        String responses = tester.getResponses(requests);

        String chunks[] = responses.split("\\r\\n");

        checkHeader(chunks);

        JSONObject jsonObj = new JSONObject(chunks[4]);

        assertNotNull(jsonObj);

        assertTrue(jsonObj.has("jsonrpc"));
        assertEquals("2.0", jsonObj.getString("jsonrpc"));

        assertFalse(jsonObj.has("error"));
        assertTrue(jsonObj.has("result"));

        assertEquals("test successful", jsonObj.getString("result"));
    }

    public void testAddMethodGet() throws Exception {

        JSONArray jsonArr = new JSONArray("[1,2]");

        String requests = "GET /api?method=add&params=" + jsonArr.toString()
                + " HTTP/1.1\r\n" + "Host: tester\r\n" + "\r\n";

        String responses = tester.getResponses(requests);

        String chunks[] = responses.split("\\r\\n");

        checkHeader(chunks);

        JSONObject jsonObj = new JSONObject(chunks[4]);

        assertNotNull(jsonObj);

        assertTrue(jsonObj.has("jsonrpc"));
        assertEquals("2.0", jsonObj.getString("jsonrpc"));

        assertFalse(jsonObj.has("error"));
        assertTrue(jsonObj.has("result"));

        assertFalse(jsonObj.has("id"));

        assertEquals("3", jsonObj.getString("result"));
    }

    public void testAddMethodGetWithId() throws Exception {

        JSONArray jsonArr = new JSONArray("[1,2]");

        String requests = "GET /api?method=add&params=" + jsonArr.toString()
                + "&id=411 HTTP/1.1\r\n" + "Host: tester\r\n" + "\r\n";

        String responses = tester.getResponses(requests);

        String chunks[] = responses.split("\\r\\n");

        checkHeader(chunks);

        JSONObject jsonObj = new JSONObject(chunks[4]);

        assertNotNull(jsonObj);

        assertTrue(jsonObj.has("jsonrpc"));
        assertEquals("2.0", jsonObj.getString("jsonrpc"));

        assertFalse(jsonObj.has("error"));
        assertTrue(jsonObj.has("result"));

        assertTrue(jsonObj.has("id"));
        assertEquals("411", jsonObj.getString("id"));

        assertEquals("3", jsonObj.getString("result"));
    }

    public void testSubMethodGetWithId() throws Exception {

        JSONArray jsonArr = new JSONArray("[5,2]");

        String requests = "GET /api?method=sub&params=" + jsonArr.toString()
                + "&id=811 HTTP/1.1\r\n" + "Host: tester\r\n" + "\r\n";

        String responses = tester.getResponses(requests);

        String chunks[] = responses.split("\\r\\n");

        checkHeader(chunks);

        JSONObject jsonObj = new JSONObject(chunks[4]);

        assertNotNull(jsonObj);

        assertTrue(jsonObj.has("jsonrpc"));
        assertEquals("2.0", jsonObj.getString("jsonrpc"));

        assertFalse(jsonObj.has("error"));
        assertTrue(jsonObj.has("result"));

        assertTrue(jsonObj.has("id"));
        assertEquals("811", jsonObj.getString("id"));

        assertEquals("3", jsonObj.getString("result"));
    }

    public void testEchoJsonStringMethodGet() throws Exception {

        JSONObject paramObj = new JSONObject("{\"test\":true}");

        String requests = "GET /api?method=echoJsonString&params="
                + paramObj.toString() + " HTTP/1.1\r\n" + "Host: tester\r\n"
                + "\r\n";

        String responses = tester.getResponses(requests);

        String chunks[] = responses.split("\\r\\n");

        checkHeader(chunks);

        JSONObject jsonObj = new JSONObject(chunks[4]);
        assertNotNull(jsonObj);

        assertTrue(jsonObj.has("jsonrpc"));
        assertEquals("2.0", jsonObj.getString("jsonrpc"));

        assertFalse(jsonObj.has("error"));
        assertTrue(jsonObj.has("result"));
        String jsonStr = URLDecoder.decode(jsonObj.getString("result"), "UTF-8");

        JSONObject resultObj = new JSONObject(jsonStr);

        assertNotNull(resultObj);

        assertTrue(resultObj.has("test"));
        assertTrue(resultObj.getBoolean("test"));
    }

    public void testEchoJsonObjMethodGet() throws Exception {

        JSONObject paramObj = new JSONObject("{\"test\":true}");

        String requests = "GET /api?method=echoJsonObj&params="
                + paramObj.toString() + " HTTP/1.1\r\n" + "Host: tester\r\n"
                + "\r\n";

        String responses = tester.getResponses(requests);

        String chunks[] = responses.split("\\r\\n");

        checkHeader(chunks);

        JSONObject jsonObj = new JSONObject(chunks[4]);
        assertNotNull(jsonObj);

        assertTrue(jsonObj.has("jsonrpc"));
        assertEquals("2.0", jsonObj.getString("jsonrpc"));

        assertFalse(jsonObj.has("error"));
        assertTrue(jsonObj.has("result"));
        String jsonStr = jsonObj.getString("result");

        JSONObject resultObj = new JSONObject(jsonStr);

        assertNotNull(resultObj);

        assertTrue(resultObj.has("test"));
        assertTrue(resultObj.getBoolean("test"));

    }
    
    public void testGetlistMethodGet() throws Exception {
        String requests = "GET /api?method=getlist"
                + " HTTP/1.1\r\n" + "Host: tester\r\n"
                + "\r\n";

        String responses = tester.getResponses(requests);
        
        String chunks[] = responses.split("\\r\\n");

        checkHeader(chunks);

        JSONObject jsonObj = new JSONObject(chunks[4]);
        assertNotNull(jsonObj);

        assertTrue(jsonObj.has("jsonrpc"));
        assertEquals("2.0", jsonObj.getString("jsonrpc"));

        assertFalse(jsonObj.has("error"));
        assertTrue(jsonObj.has("result"));
        JSONArray jarr = jsonObj.getJSONArray("result");


        assertNotNull(jarr);

        assertEquals(jarr.get(0), 1);
        assertEquals(jarr.get(1), 2);
        assertEquals(jarr.get(2), 3);
        assertEquals(jarr.get(3), 4);
    }
}
