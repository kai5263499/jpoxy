package org.jpoxy;

import junit.framework.TestCase;

import org.json.JSONObject;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.testing.ServletTester;

public class RPCObjectMarshallingTest extends TestCase {

    private static ServletTester tester;

    @Override
    protected void setUp() throws Exception {
        tester = new ServletTester();
        tester.setContextPath("/");
        ServletHolder sh = tester.addServlet(org.jpoxy.RPC.class, "/api");
        sh.setInitParameter("rpcclasses", "org.jpoxy.ClassE");
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

    public void testEditUserMethod() throws Exception {

        String requests = "GET /api?method=edit&name=Thomas%20A.%20Anderson&age=48"+
                " HTTP/1.1\r\n" + "Host: tester\r\n" + "\r\n";

        String responses = tester.getResponses(requests);

        String chunks[] = responses.split("\\r\\n");

        checkHeader(chunks);

        JSONObject jsonObj = new JSONObject(chunks[4]);

        assertNotNull(jsonObj);

        assertTrue(jsonObj.has("jsonrpc"));
        assertEquals("2.0", jsonObj.getString("jsonrpc"));

        assertFalse(jsonObj.has("error"));
        assertTrue(jsonObj.has("result"));

        JSONObject user = jsonObj.getJSONObject("result");

        assertEquals("Neo", user.getString("name"));
        assertEquals(22, user.getInt("age"));
        assertFalse(user.has("class"));
    }
}
