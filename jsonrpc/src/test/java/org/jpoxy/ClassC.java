package org.jpoxy;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.jpoxy.events.JSONRPCEventListener;
import org.jpoxy.events.JSONRPCMessage;
import org.jpoxy.events.JSONRPCMessageEvent;
import java.util.HashMap;

public class ClassC implements JSONRPCEventListener {

    public boolean init = false;
    private ServletConfig config;

    public ClassC() {
        init = true;
    }

    public String getConfig(HashMap params) {
        HttpServletRequest request = (HttpServletRequest) params.get("request");
        HttpSession session = request.getSession(true);

        if (config != null) {
            return config.getInitParameter("rpcclasses");
        }
        return "";
    }

    public Integer sessionCounter(HashMap params) {
        HttpServletRequest request = (HttpServletRequest) params.get("request");
        HttpSession session = request.getSession(true);

        Integer count = (Integer) session.getAttribute("count");

        if (count == null) {
            count = 1;
        } else {
            count += 1;
        }

        System.out.println("count is: " + count);

        session.setAttribute("count", count);

        return (Integer) session.getAttribute("count");
    }

    public String test() {
        return "test from class C successful";
    }

    public void messageReceived(JSONRPCMessageEvent me) {
        switch (me.message().getCode()) {
            case JSONRPCMessage.INIT:
                config = me.message().getServletConfig();
                break;
        }
    }
}
