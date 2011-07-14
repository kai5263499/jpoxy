/*
 * From http://www.java2s.com/Code/Java/Servlets/javaxservletrequestX509Certificate.htm
 */
package org.jpoxy.filter;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.cert.X509Certificate;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This class
 */
public class CertAuth implements Filter {

    private FilterConfig filterConfigObj = null;

    @Override
    public void init(FilterConfig fc) throws ServletException {
        filterConfigObj = fc;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain fc) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        //fc.doFilter(request, response);

        res.setContentType("text/plain");
        PrintWriter out = res.getWriter();

        X509Certificate[] certs = (X509Certificate[]) req.getAttribute("javax.servlet.request.X509Certificate");
        if (certs != null) {
            for (int i = 0; i < certs.length; i++) {
                out.println("Client Certificate [" + i + "] = " + certs[i].toString());
            }
        } else {
            if ("https".equals(req.getScheme())) {
                out.println("This was an HTTPS request, " + "but no client certificate is available");
            } else {
                out.println("This was not an HTTPS request, " + "so no client certificate is available");
            }
        }

    }

    @Override
    public void destroy() {
    }
}
