package org.jpoxy;

import org.json.JSONException;
import org.json.JSONObject;

public class Response {
	private static final String jsonrpc = "2.0";
	private String id;

        private Object result;

	private JSONObject response;

	public Response(Exception e) throws JSONException {
		handleException(e);
	}

	public Response(Throwable t) throws JSONException {
		handleThrowable(t);
	}

	public Response() throws JSONException {
		response = new JSONObject();
		setVersion();
	}

	private void handleException(Exception e) throws JSONException {
		response = new JSONObject();
		setVersion();

		JSONObject error = new JSONObject();
		if (e.getClass().getName().endsWith("JSONRPCException")) {
			JSONRPCException je = (JSONRPCException) e;
			error.put("code", je.getCode());
		} else {
			error.put("code", -32603);
		}
		error.put("message", e.getMessage());

		JSONObject errorData = new JSONObject();
		errorData.put("classname", e.getClass().getName());
		errorData.put("hashcode", e.hashCode());
		errorData.put("stacktrace", e.getStackTrace());
		error.put("data", errorData);
		response.put("error", error);

		e.printStackTrace();
	}

	private void handleThrowable(Throwable t) throws JSONException {
		response = new JSONObject();
		setVersion();

		JSONObject error = new JSONObject();
		if (t.getClass().getName().endsWith("JSONRPCException")) {
			JSONRPCException je = (JSONRPCException) t;
			error.put("code", je.getCode());
		} else {
			error.put("code", -32603);
		}
		error.put("message", t.getMessage());

		JSONObject errorData = new JSONObject();
		errorData.put("classname", t.getClass().getName());
		errorData.put("hashcode", t.hashCode());
		errorData.put("stacktrace", t.getStackTrace());
		error.put("data", errorData);
		response.put("error", error);

		t.printStackTrace();
	}

	public void clearErrorData() throws JSONException {
		if (response.has("error")
				&& response.getJSONObject("error").has("data"))
			response.getJSONObject("error").remove("data");
	}

	private void setVersion() throws JSONException {
		response.put("jsonrpc", jsonrpc);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) throws JSONException {
		this.id = id;
		response.put("id", this.id);
	}

	public Object getResult() {
		return result;
	}

	public void setResult(Object result) throws JSONException {
		this.result = result;
		response.put("result", this.result);
	}

	public JSONObject getJSON() {
		return response;
	}

	public String getJSONString() {
                return response.toString();
	}

	public String getJSONString(int i) {
		try {
			return response.toString(i);
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
	}

}
