package org.jpoxy;

/**
 * A standardized exception class that conforms with JSON-RPC specifications.
 * 
 * @author Wes Widner
 * @see <a
 *      href="http://groups.google.com/group/json-rpc/web/json-rpc-1-2-proposal#error-object">JSON-RPC
 *      Error Specification</a>
 */

@SuppressWarnings("serial")
public class JSONRPCException extends Exception {
	private long code;

	public JSONRPCException() {
		super();
		setCode(-32603); // Generic JSON-RPC error code.
	}

	public JSONRPCException(String message) {
		super(message);
		setCode(-32603); // Generic JSON-RPC error code.
	}

	public JSONRPCException(String message, long code) {
		super(message);
		setCode(code);
	}

	/**
	 * Set the JSON-RPC error code for this exception
	 * 
	 * @param code The JSON-RPC error code, usually negative in the range of -32768 to -32000 inclusive
	 */
	public void setCode(long code) {
		this.code = code;
	}

	/**
	 * Get the JSON-RPC error code of this exception.
	 * @return long Error code, usually negative in the range of -32768 to -32000 inclusive
	 */
	public long getCode() {
		return code;
	}
}
