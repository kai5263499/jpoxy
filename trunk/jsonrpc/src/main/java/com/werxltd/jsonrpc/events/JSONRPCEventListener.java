package com.werxltd.jsonrpc.events;

import java.util.EventListener;

public interface JSONRPCEventListener extends EventListener {
	public void messageReceived( JSONRPCMessageEvent me );
}
