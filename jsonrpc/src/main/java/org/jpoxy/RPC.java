package org.jpoxy;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.SimpleTimeZone;
import java.util.logging.Level;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.jpoxy.events.JSONRPCEventListener;
import org.jpoxy.events.JSONRPCMessage;
import org.jpoxy.events.JSONRPCMessageEvent;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.codehaus.jackson.map.ObjectMapper;
import org.jpoxy.annotate.JpoxyIgnore;
import org.reflections.Reflections;
import org.reflections.scanners.TypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

/**
 * This class creates a servlet which implements the JSON-RPC 2.0 specification.
 * 
 * @author Wes Widner
 * 
 */
@SuppressWarnings("serial")
public class RPC extends HttpServlet {

    protected final static Logger LOG = Logger.getLogger(RPC.class);
    private boolean PERSIST_CLASS = true;
    private boolean EXPOSE_METHODS = false;
    private boolean DETAILED_ERRORS = false;
    private boolean USE_FULL_CLASSNAME = false;
    private HashMap<String, Object> rpcobjects;
    private HashMap<String, Method> rpcmethods;
    private List<JSONRPCEventListener> listeners = new ArrayList<JSONRPCEventListener>();
    private ServletConfig servletconfig;
    private ServletContext servletcontext;
    private ObjectMapper objectmapper;
    JpoxyIgnore jpoxyignoreannotation;

    private void processClass(Class c) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        int classmodifiers = c.getModifiers();

        /*
         * Class must be public and cannot be an abstract or interface
         */
        if (Modifier.isAbstract(classmodifiers)
                || !Modifier.isPublic(classmodifiers)
                || Modifier.isInterface(classmodifiers)) {
            LOG.debug("skipping abstract class");
            return;
        }

        if (!Modifier.isStatic(classmodifiers)) {
            try {
                if (PERSIST_CLASS) {
                    Object obj = c.newInstance();
                    rpcobjects.put(c.getName(), obj);

                    if (implementsRPCEventListener(c)) {
                        addMessageListener((JSONRPCEventListener) obj);
                    }
                }
            } catch (InstantiationException ie) {
                LOG.error("Caught InstantiationException");
                return;
            }
        }

        Method methods[] = c.getDeclaredMethods();

        for (int i = 0; i < methods.length; i++) {
            int methodmodifiers = methods[i].getModifiers();

            // Auto-prune non-public methods and any methods that return data types we can't serialize
            if (!Modifier.isPublic(methodmodifiers) || (
                    !objectmapper.canSerialize(methods[i].getReturnType()) &&
                    !methods[i].getReturnType().equals(JSONObject.class) &&
                    !methods[i].getReturnType().isPrimitive()
                 )
               ) {
                continue;
            }

            jpoxyignoreannotation = methods[i].getAnnotation(JpoxyIgnore.class);
            if(jpoxyignoreannotation != null && !jpoxyignoreannotation.value()) {
                continue;
            }

            String methodsig = generateMethodSignature(methods[i]);

            LOG.debug("methodsig:" + methodsig);

            if (methodsig == null) {
                continue;
            }

            if (rpcmethods.containsKey(methodsig)) {
                LOG.debug("Skipping duplicate method name: [" + methodsig + "]");
                continue;
            }

            LOG.debug("Adding method sig: [" + methodsig + "]");

            rpcmethods.put(methodsig, methods[i]);
        }
    }

    /**
     * This method reads the servlet configuration for a list of classes it
     * should scan for acceptable Method objects that can be called remotely.
     * Acceptable methods are methods that do not have a {@link Modifier}
     * marking them as abstract or interface methods. Static methods are fine.
     * <P>
     * Valid methods are gathered into a {@link HashMap} and instances of
     * non-static classes are created and reused for subsequent RPC calls.
     * <P>
     * Class is marked as final to prevent overriding and possible interference
     * upstream.
     *
     * @see <a href=
     *      "http://java.sun.com/j2se/1.5.0/docs/api/java/lang/reflect/Method.html"
     *      >java.lang.reflect.Method</>
     * @see <a
     *      href="http://java.sun.com/j2se/1.5.0/docs/api/java/lang/reflect/Modifier.html">java.lang.reflect.Modifier</a>
     * @see <a
     *      href="http://java.sun.com/j2se/1.4.2/docs/api/java/util/HashMap.html">java.util.HashMap</a>
     * @param config
     *            ServletConfig passed from container upon initialization
     */
    @Override
    public final void init(ServletConfig config) throws ServletException {
        super.init(config);
        LOG.debug("RPC init!");

        objectmapper = new ObjectMapper();

        servletconfig = config;
        servletcontext = getServletContext();
        
        try {
            String classnames[] = servletconfig.getInitParameter("rpcclasses").replaceAll("\\s*", "").split(",");

            if (servletconfig.getInitParameter("expose_methods") != null) {
                EXPOSE_METHODS = servletconfig.getInitParameter("expose_methods").equalsIgnoreCase("true");
            }
            if (servletconfig.getInitParameter("detailed_errors") != null) {
                DETAILED_ERRORS = servletconfig.getInitParameter("detailed_errors").equalsIgnoreCase("true");
            }
            if (servletconfig.getInitParameter("persist_class") != null) {
                PERSIST_CLASS = servletconfig.getInitParameter("persist_class").equalsIgnoreCase("true");
            }
            if (servletconfig.getInitParameter("use_full_classname") != null) {
                USE_FULL_CLASSNAME = servletconfig.getInitParameter("use_full_classname").equalsIgnoreCase("true");
            }

            if (classnames.length < 1) {
                throw new JSONRPCException("No RPC classes specified.");
            }

            rpcmethods = new HashMap<String, Method>();
            rpcobjects = new HashMap<String, Object>();

            for (int o = 0; o < classnames.length; o++) {
                LOG.debug("examining class: " + classnames[o]);

                String patternStr = "(a(b*))+(c*)";

                // Compile and use regular expression
                Pattern pattern = Pattern.compile("^(.*)\\.(.*)\\*$");
                Matcher matcher = pattern.matcher(classnames[o]);
                boolean matchFound = matcher.find();

                if(matchFound) {
                    LOG.debug("Looking for classes in package: "+classnames[o]);

                    Reflections reflections = new Reflections(new ConfigurationBuilder()
                            .setUrls(ClasspathHelper.getUrlsForPackagePrefix(matcher.group(1)))
                            .setScanners(new TypesScanner()));

                    Set<String> stringSet = reflections.getStore().get(TypesScanner.class).keySet();
                    for (String key_str : stringSet) {
                        String additional = "";
                        if(matcher.group(2).length() > 0) additional = "."+matcher.group(2);
                        if(key_str.matches(matcher.group(1)+additional+".*") && !key_str.matches(".*\\$.*")) {
                            LOG.debug("Processing: "+key_str);
                            processClass(Class.forName(key_str));
                        }
                    }

                } else {
                    LOG.debug("Processing: "+classnames[o]);
                    processClass(Class.forName(classnames[o]));
                }


            }

            if(EXPOSE_METHODS) {
                Class<?> rpcclass = this.getClass();
                Method infoMethod = rpcclass.getMethod("listrpcmethods", (Class<?>[]) null);
                rpcmethods.put("listrpcmethods:0", infoMethod);
                rpcobjects.put("listrpcmethods", this);
            }

            if (rpcmethods.isEmpty()) {
                throw new JSONRPCException("No valid RPC methods found.");
            }

            JSONRPCMessage msg = generateMessage(JSONRPCMessage.INIT, null, null);
            msg.setServletConfig(servletconfig);
            msg.setServletContext(servletcontext);
            fireMessageEvent(msg);
        } catch (Exception e) {
            long now = System.currentTimeMillis();
            Date date = new Date(now);

            SimpleDateFormat sdf = new SimpleDateFormat();
            sdf.setTimeZone(new SimpleTimeZone(0, "GMT"));
            sdf.applyPattern("dd MMM yyyy HH:mm:ss z");

            LOG.error("Exception caught at [" + sdf.format(date) + "]");
            LOG.error("Stack trace:");
            LOG.error(e.getStackTrace());
            LOG.error("End exception code");
        }
    }

    /**
     * Adds a class that implements the JSONRPCEventListener interface to the internal
     * list of listeners
     * @param l
     */
    public synchronized void addMessageListener(JSONRPCEventListener l) {
        listeners.add(l);
    }

    /**
     * Removes a class that implements the JSONRPCEventListener interface from the
     * internal list of listeners
     * @param l
     */
    public synchronized void removeMessageListener(JSONRPCEventListener l) {
        listeners.remove(l);
    }

    /**
     * Walks through the listeners list, firing the messageRecieved method on
     * all classes that implement the JSONRPCEventListener
     * @param m
     */
    private synchronized void fireMessageEvent(JSONRPCMessage m) {
        LOG.info("Firing message with code: " + m.getCode());

        JSONRPCMessageEvent me = new JSONRPCMessageEvent(this, m);
        Iterator<JSONRPCEventListener> ilisteners = listeners.iterator();
        while (ilisteners.hasNext()) {
            ilisteners.next().messageReceived(me);
        }
    }

    /**
     * This method returns true or false depending on whether the supplied class implements JSONRPCEventListener or not
     * @param c Class to test
     * @return boolean indicating whether supplied class implements JSONRPCEventListener or not
     */
    private boolean implementsRPCEventListener(Class<?> c) {
        Class<?>[] theInterfaces = c.getInterfaces();
        for (int i = 0; i < theInterfaces.length; i++) {

            String interfaceName = theInterfaces[i].getName();

            LOG.info("Class: " + c.getName() + " implents interface: " + interfaceName);

            if (theInterfaces[i].equals(JSONRPCEventListener.class)) {
                return true;
            }
        }

        return false;
    }

    /**
     * This method generates a string containing the "signature" of a
     * {@link java.util.Method} object in the form of methodname:paramcount or
     * test:3
     * <p>
     * The output of this method is used to generate keys for Method objects
     * stored in a {@link HashMap} for easy retrieval.
     *
     * @param Method
     *            The Method object you would like to generate a signature for.
     * @return String Contains the "signature" of a Method object in the form of
     *         methodname:paramcount or test:3
     */
    private String generateMethodSignature(Method m) throws ClassNotFoundException {
        Class<?> paramclasses[] = m.getParameterTypes();

        String mname;
        if (USE_FULL_CLASSNAME) {
            mname = m.getDeclaringClass().getName() + "." + m.getName();
        } else {
            mname = m.getName();
        }

        if(paramclasses.length == 0) {
            return mname + ":0";
        } else if(paramclasses.length == 1) {
            if(paramclasses[0].getName().matches("org.jpoxy.events.JSONRPCMessageEvent")) {
                return null;
            } else if (paramclasses[0].getName().matches("org.json.JSONObject")) {
                return mname + ":JSONObject";
            } else if (paramclasses[0].getName().matches("java.util.HashMap")) {
                return mname + ":HashMap";
            } else if (paramclasses[0].getName().matches(".+\\..+") &&
                    !paramclasses[0].getName().matches("java.lang.String") &&
                    objectmapper.canSerialize(paramclasses[0])
              ) {
                return mname + ":Object";
            } else {
                return mname + ":1";
            }
        } else {
            int parmscount = 0;
            for (int j = 0; j < paramclasses.length; j++) {
                if (paramclasses[j].getName().matches("java.lang.String")
                        || paramclasses[j].isPrimitive()) {
                    parmscount++;
                } else {
                    return null;
                }
            }
            return mname + ":" + parmscount;
        }
    }

    /**
     * Method lists available RPC methods loaded from configured classes.
     *
     * @return JSONObject Containing available method information.
     * @throws JSONException
     */
    public JSONObject listrpcmethods() throws JSONException {
        LOG.info("listing rpc methods");
        JSONObject result = new JSONObject();
        Iterator<String> iterator = rpcmethods.keySet().iterator();
        while (iterator.hasNext()) {
            String methodsig = iterator.next();
            Method m = rpcmethods.get(methodsig);
            int modifiers = m.getModifiers();
            JSONObject methodObj = new JSONObject();
            methodObj.put("name", m.getName());
            methodObj.put("static", Modifier.isStatic(modifiers));
            methodObj.put("class", m.getDeclaringClass().getName());
            methodObj.put("returns", m.getReturnType().getName());
            Class<?> paramclasses[] = m.getParameterTypes();
            for (int i = 0; i < paramclasses.length; i++) {
                methodObj.append("params", paramclasses[i].getName());
            }
            if (!methodObj.has("params")) {
                methodObj.put("params", new JSONArray());
            }

            result.append("method", methodObj);
        }

        return result;
    }

    /**
     * This method attempts to turn generic exceptions into valid JSONRPC
     * exceptions.
     *
     * @see <a
     *      href="http://groups.google.com/group/json-rpc/web/json-rpc-1-2-proposal#error-object">JSON-RPC
     *      Error Specification</a>
     *
     * @param e
     *            Generic exception thrown, for better, more readable
     *            exceptions, use JSONRPCExcepton class.
     * @throws JSONException
     */
    private Response handleException(Exception e) throws JSONException {
        Response response = new Response(e);
        if (!DETAILED_ERRORS) {
            response.clearErrorData();
        }

        fireMessageEvent(generateMessage(JSONRPCMessage.EXCEPTION, null, null));

        return response;
    }

    /**
     * This method attempts to take a Throwable object and turn it into a
     * valid JSONRPC error.
     *
     * @param t
     * @throws JSONException
     */
    private Response handleException(Throwable t, Response response) throws JSONException {
        response = new Response(t);
        if (!DETAILED_ERRORS) {
            response.clearErrorData();
        }

        fireMessageEvent(generateMessage(JSONRPCMessage.EXCEPTION, null, null));

        return response;
    }

    /**
     * Unified method for outputting the internal jsonresponse (error or not).
     * Method checks for a "debug" parameter and, if it is set to "true", prints
     * the JSONObject in a more human-readable fashion.
     *
     * @param req
     * @param res
     * @throws IOException
     * @throws JSONException
     */
    private void writeResponse(HttpServletRequest req, HttpServletResponse res, Response response)
            throws IOException, JSONException {
        String jsonStr = "";

        res.setContentType("text/plain");

        PrintWriter writer = res.getWriter();

        if (req.getParameter("debug") != null
                && req.getParameter("debug").matches("true")) {
            jsonStr = response.getJSONString(2);
        } else {
            jsonStr = response.getJSONString();
        }

        if (req.getParameter("callback") != null) {
            if (req.getParameter("callback").matches("^\\?")) {
                writer.println("(" + jsonStr + ")");
            } else if(req.getParameter("callback").matches("^(\\d|\\w)+$")) {
                writer.println(req.getParameter("callback") + "(" + jsonStr + ")");
            } else {
                writer.println("{\"error\":\"Invalid callback parameter specified.\"}");
            }
        } else {
            writer.println(jsonStr);
        }
    }

    /**
     * Method to generate a JSONRPCMessage, usually used in connection with a fireMessageEvent
     * @param code
     * @param req
     * @param res
     * @return JSONRPCMessage
     * @see fireMessageEvent
     */
    private JSONRPCMessage generateMessage(int code, HttpServletRequest req, HttpServletResponse res) {
        JSONRPCMessage msg = new JSONRPCMessage(code);
        msg.setServletConfig(getServletConfig());
        //msg.setRPCResponse(response);
        return msg;
    }

    /**
     * Forwards request to doGet method for consistency
     * @see doGet
     */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        doGet(req, res);
    }

    /**
     * Called by servlet container when a request is made.
     */
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        boolean handled = false;
        res.setContentType("text/plain");
        Response response = null;
        try {
            response = handleRequest(req, res);
            handled = true;
        } catch (Exception e) {
            try {
                response = handleException(e);
                handled = true;
            } catch (JSONException ex) {
                java.util.logging.Logger.getLogger(RPC.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        try {
            if (response != null) {
                writeResponse(req, res, response);
            }
        } catch (JSONException ex) {
            java.util.logging.Logger.getLogger(RPC.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (!handled) {
            PrintWriter writer = res.getWriter();
            writer.println("{\"error\":\"unrecoverable error\"}");
        }
    }

    /**
     * Converts HttpServletRequest POST/GET parameters into a HashMap
     */
    private HashMap reqParamsToHashMap(HttpServletRequest request) {
        HashMap<String, Object> retmap = new HashMap();

        Enumeration paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramName = (String) paramNames.nextElement();
            String paramValue = request.getParameter(paramName);

            retmap.put(paramName, paramValue);
        }

        retmap.put("request", request);

        return retmap;
    }

    /**
     *
     * @param req
     *            HttpServletRequest given to us from doGet or doPost
     * @param res
     *            HttpServletResponse we can output information to, This method
     *            passes res to the
     *            {@link #writeResponse(HttpServletRequest, HttpServletResponse)}
     *            method.
     * @throws ServletException
     * @throws IOException
     * @throws JSONException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws JSONRPCException
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalArgumentException
     */
    private Response handleRequest(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException, JSONException,
            IllegalAccessException, InvocationTargetException,
            JSONRPCException, ClassNotFoundException, IllegalArgumentException, InstantiationException, NoSuchMethodException {

        Request request = new Request();
        Response response = new Response();

        if (req.getParameter("json") != null) {
            request.parseJSON(req.getParameter("json"));
        } else if (req.getParameter("data") != null) {
            request.parseJSON(req.getParameter("data"));
        } else {
        }

        if (request.getId() == null) {
            if (req.getParameter("id") != null) {
                response.setId(req.getParameter("id"));
            }
        }

        String method = request.getMethod();
        if (method == null) {
            if (req.getParameter("method") != null) {
                method = req.getParameter("method");
                request.setMethod(method);
            } else {
                if (EXPOSE_METHODS) {
                    response.setResult(listrpcmethods());
                    return response;
                } else {
                    throw new JSONRPCException("Unspecified JSON-RPC method.", -32600);
                }
            }
        }

        if (request.getParamtype() == Request.ParamType.NONE) {
            if (req.getParameter("params") != null) {
                request.parseParams(req.getParameter("params"));
            }
        }

        /*
         * throw new JSONRPCException(
         * "JSON parameter failed to parse into valid JSONObject.", -32700);
         *
         * if (!request.has("method")) throw new
         * JSONRPCException("Unspecified JSON-RPC method.", -32600);
         *
         * throw new JSONRPCException(
         * "Invalid params data given. Must be object or array.", -32602);
         */

        // Generate a string which includes the method name and the number of
        // parameters provided
        int param_count = request.getParamCount();
        String methodsig = method + ":" + param_count;

        LOG.error("looking for methodsig: " + methodsig);

        Object result = new Object();
        Method m = null;

        HashMap<String, ?> paramstohashmap = reqParamsToHashMap(req);

        Object methparams[] = null;
        if (rpcmethods.containsKey(method + ":JSONObject")) {
            m = rpcmethods.get(method + ":JSONObject");

            if (m == null) {
                init(servletconfig);
                m = rpcmethods.get(method + ":JSONObject");
                if (m == null) {
                    throw new JSONRPCException("JSON-RPC method [" + method + "] with "
                            + param_count + " parameters not found. Re-init attempted.", -32601);
                }
            }

            param_count = 1;
            methparams = new Object[1];
            methparams[0] = request.getParamObj();
        } else if (rpcmethods.containsKey(method + ":HashMap")) {
            m = rpcmethods.get(method + ":HashMap");

            if (m == null) {
                init(servletconfig);
                m = rpcmethods.get(method + ":HashMap");
                if (m == null) {
                    throw new JSONRPCException("JSON-RPC method [" + method + "] with "
                            + param_count + " parameters not found. Re-init attempted.", -32601);
                }
            }

            param_count = 1;
            methparams = new Object[1];
            methparams[0] = paramstohashmap;
        } else if (rpcmethods.containsKey(method + ":Object")) {
            param_count = 1;
            methparams = new Object[1];
            m = rpcmethods.get(method + ":Object");
            Class<?> paramclasses[] = m.getParameterTypes();

            JSONObject mobj = request.getParamObj();
            if(mobj == null) {
                paramstohashmap.remove("request");
                paramstohashmap.remove("method");
                paramstohashmap.remove("debug");
                paramstohashmap.remove("callback");
                mobj = new JSONObject(paramstohashmap);
            }

            if(mobj == null) {
                throw new JSONRPCException("Could not convert parameters to valid JSONObject. JSON-RPC method [" + method + "].", -32601);
            }

            Object obj = objectmapper.readValue(mobj.toString(), paramclasses[0]);

            methparams[0] = obj;
        } else if (rpcmethods.containsKey(methodsig)) {
            m = rpcmethods.get(methodsig);
            if (param_count > 0) {
                methparams = new Object[param_count];
                Class<?> paramtypes[] = m.getParameterTypes();
                for (int i = 0; i < paramtypes.length; i++) {
                    if (paramtypes[i].getName().matches("float")) {
                        methparams[i] = Float.parseFloat(request.getParamAt(i));
                    } else if (paramtypes[i].getName().matches("int")) {
                        methparams[i] = Integer.parseInt(request.getParamAt(i));
                    } else if (paramtypes[i].getName().matches("long")) {
                        methparams[i] = Long.getLong(request.getParamAt(i));
                    } else if (paramtypes[i].getName().matches(
                            "java.lang.String")) {
                        methparams[i] = request.getParamAt(i);
                    } else if (paramtypes[i].getName().matches("double")) {
                        methparams[i] = Double.parseDouble(request.getParamAt(i));
                    } else if (paramtypes[i].getName().matches("boolean")) {
                        methparams[i] = Boolean.parseBoolean(request.getParamAt(i));
                    }
                }
            }
        } else {
            throw new JSONRPCException("JSON-RPC method [" + method + "] with "
                    + param_count + " parameters not found.", -32601);
        }

        LOG.debug("running methodsig: " + methodsig + " param_count:" + param_count);

        try {
            result = runMethod(m, param_count, methparams);
            response.setResult(result);
        } catch (InvocationTargetException ite) {
            if (ite.getCause() != null) {
                response = handleException(ite.getCause(), response);
            } else {
                throw ite;
            }
        }

        return response;
    }

    public static String ucfirst(String string) {
        String retval;

        retval = string.toUpperCase(Locale.getDefault()).substring(0, 1)
                + string.substring(1);

        return retval;
    }

    private Object runMethod(Method m, int param_count, Object[] methparams) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException, JSONException, IOException, JSONRPCException {
        int modifiers = m.getModifiers();
        Object result = new Object();
        if (Modifier.isStatic(modifiers)) {
            if (param_count > 0) {
                result = (Object) m.invoke(null, methparams);
            } else {
                result = (Object) m.invoke(null);
            }
        } else {
            Object obj;

            if (PERSIST_CLASS) {
                obj = rpcobjects.get(m.getDeclaringClass().getName());
            } else {
                Class<?> c = Class.forName(m.getDeclaringClass().getName());

                obj = c.newInstance();
            }

            if (param_count > 0) {
                result = (Object) m.invoke(obj, methparams);
            } else {
                result = (Object) m.invoke(obj);
            }

            if (!PERSIST_CLASS) {
                obj = null;
            }
        }

        String resultclassname = result.getClass().getName();
        if(resultclassname.matches(".+\\..+") && !resultclassname.matches("^org\\.json\\..+") && !resultclassname.matches("^java\\.lang\\..+") && !result.getClass().isPrimitive()) {
            String jsonstr = objectmapper.writeValueAsString(result);
            if(jsonstr.matches("^\\[.*\\]$")) {
                result = new JSONArray(jsonstr);
            } else if(jsonstr.matches("^\\{.*\\}$")) {
                result = new JSONObject(jsonstr);
            } else {
                throw new JSONRPCException("could not reserialize data returned from method, "+jsonstr, -32601);
            }
        }
        
        return result;
    }
}
