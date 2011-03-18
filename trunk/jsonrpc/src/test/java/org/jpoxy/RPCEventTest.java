package org.jpoxy;

import junit.framework.TestCase;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.testing.ServletTester;

public class RPCEventTest extends TestCase {

    private static ServletTester tester;

    @Override
    protected void setUp() throws Exception {
        tester = new ServletTester();
        tester.setContextPath("/");

        ServletHolder sh = tester.addServlet(RPC.class, "/api");
        sh.setInitParameter("rpcclasses", "org.jpoxy.ClassC");
        sh.setInitParameter("use_full_classname", "false");
        sh.setInitParameter("expose_methods", "true");
        tester.start();
    }

    @Override
    protected void tearDown() {
        // release objects under test here, if necessary
        tester = null;
    }

    private void checkSessionHeader(String[] chunks) {
        assertEquals(7, chunks.length);
        assertEquals("HTTP/1.1 200 OK", chunks[0]);
        assertEquals("Content-Type: text/plain; charset=iso-8859-1", chunks[1]);
        assertTrue(chunks[2].matches("^Expires:.*$"));
        assertTrue(chunks[3].matches("^Set-Cookie:.*$"));
        assertTrue(chunks[4].matches("^Content-Length: \\d+$"));
        assertEquals("", chunks[5]);
    }

    public void testListMethods() throws Exception {
        String requests = "GET /api HTTP/1.1\r\n" + "Host: tester\r\n" + "\r\n";

        String responses = tester.getResponses(requests);

        String chunks[] = responses.split("\\r\\n");

        JSONObject jsonObj = new JSONObject(chunks[4]);

        assertNotNull(jsonObj);
        assertTrue(jsonObj.has("jsonrpc"));
        assertEquals("2.0", jsonObj.getString("jsonrpc"));

        JSONObject resultObj = jsonObj.getJSONObject("result");
        assertNotNull(resultObj);

        JSONArray methodArr = resultObj.getJSONArray("method");
        assertNotNull(methodArr);
        assertEquals(4, methodArr.length());

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

    public void testTestClassCGetConfig() throws Exception {

        String requests = "GET /api?method=getConfig HTTP/1.1\r\n"
                + "Host: tester\r\n" + "\r\n";

        String responses = tester.getResponses(requests);

        String chunks[] = responses.split("\\r\\n");

        checkSessionHeader(chunks);

        JSONObject jsonObj = new JSONObject(chunks[6]);

        assertNotNull(jsonObj);

        assertTrue(jsonObj.has("jsonrpc"));
        assertEquals("2.0", jsonObj.getString("jsonrpc"));

        assertFalse(jsonObj.has("error"));
        assertTrue(jsonObj.has("result"));

        assertEquals("org.jpoxy.ClassC", jsonObj.getString("result"));
    }

    /*
    public void testLastResponse() throws Exception {
    String requests = "GET /api?method=test HTTP/1.1\r\n"
    + "Host: tester\r\n" + "\r\n";

    String responses = tester.getResponses(requests);

    System.out.println(responses);

    String chunks[] = responses.split("\\r\\n");

    checkSessionHeader(chunks);

    JSONObject jsonObj = new JSONObject(chunks[6]);

    assertNotNull(jsonObj);

    assertTrue(jsonObj.has("jsonrpc"));
    assertEquals("2.0", jsonObj.getString("jsonrpc"));

    assertFalse(jsonObj.has("error"));
    assertTrue(jsonObj.has("result"));

    assertEquals("test from class C successful", jsonObj.getString("result"));

    requests = "GET /api?method=getLastResponse HTTP/1.1\r\n"
    + "Host: tester\r\n" + "\r\n";

    responses = tester.getResponses(requests);

    System.out.println(responses);

    chunks = responses.split("\\r\\n");

    checkSessionHeader(chunks);

    jsonObj = new JSONObject(chunks[6]);

    assertNotNull(jsonObj);

    assertTrue(jsonObj.has("jsonrpc"));
    assertEquals("2.0", jsonObj.getString("jsonrpc"));

    assertFalse(jsonObj.has("error"));
    assertTrue(jsonObj.has("result"));

    assertEquals("test from class C successful", jsonObj.getString("result"));
    }
     */
}
