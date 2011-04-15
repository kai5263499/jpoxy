package org.jpoxy;

import javax.sound.midi.SysexMessage;
import junit.framework.TestCase;
import org.json.JSONArray;

import org.json.JSONObject;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.testing.ServletTester;

public class RPCSecurityTest extends TestCase {

    private static ServletTester tester;

    @Override
    protected void setUp() throws Exception {
        tester = new ServletTester();
        tester.setContextPath("/");
        ServletHolder sh = tester.addServlet(org.jpoxy.RPC.class, "/api");
        sh.setInitParameter("rpcclasses", "org.jpoxy.ClassE");
        sh.setInitParameter("expose_methods", "true");
        sh.setInitParameter("detailed_errors", "true");
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

        JSONObject jsonObj = new JSONObject(chunks[4]);

        assertNotNull(jsonObj);
        assertTrue(jsonObj.has("jsonrpc"));
        assertEquals("2.0", jsonObj.getString("jsonrpc"));

        JSONObject resultObj = jsonObj.getJSONObject("result");
        assertNotNull(resultObj);

        JSONArray methodArr = resultObj.getJSONArray("method");
        assertNotNull(methodArr);
        assertEquals(2, methodArr.length());

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

    public void testValidJSONPCallbackGet() throws Exception {

        String requests = "GET /api?method=edit&fullname=Thomas%20Anderson&age=48&callback=cb"+
                " HTTP/1.1\r\n" + "Host: tester\r\n" + "\r\n";

        String responses = tester.getResponses(requests);

        String chunks[] = responses.split("\\r\\n");

        checkHeader(chunks);

        String expected_ret = "cb({\"result\":{\"alias\":\"Neo\",\"verified\":false,\"age\":28,\"name\":{\"last\":\"Anderson\",\"first\":\"Thomas\"},\"userImage\":null,\"gender\":null},\"jsonrpc\":\"2.0\"})\n";

        assertEquals(expected_ret, chunks[4]);
    }

    public void testInvalidJSONPCallbackGet() throws Exception {

        String requests = "GET /api?method=edit&fullname=Thomas%20Anderson&age=48&callback=cb<script>alert('test');</script>"+
                " HTTP/1.1\r\n" + "Host: tester\r\n" + "\r\n";

        String responses = tester.getResponses(requests);

        String chunks[] = responses.split("\\r\\n");

        checkHeader(chunks);

        JSONObject jsonObj = new JSONObject(chunks[4]);

        assertNotNull(jsonObj);

        assertTrue(jsonObj.has("error"));
        assertEquals("Invalid callback parameter specified.", jsonObj.getString("error"));
    }

    public void testValidJSONPCallbackWithDotsGet() throws Exception {

        String requests = "GET /api?method=edit&fullname=Thomas%20Anderson&age=48&callback=cb.cb"+
                " HTTP/1.1\r\n" + "Host: tester\r\n" + "\r\n";

        String responses = tester.getResponses(requests);

        String chunks[] = responses.split("\\r\\n");

        checkHeader(chunks);

        String expected_ret = "cb.cb({\"result\":{\"alias\":\"Neo\",\"verified\":false,\"age\":28,\"name\":{\"last\":\"Anderson\",\"first\":\"Thomas\"},\"userImage\":null,\"gender\":null},\"jsonrpc\":\"2.0\"})\n";

        assertEquals(expected_ret, chunks[4]);
    }
}
