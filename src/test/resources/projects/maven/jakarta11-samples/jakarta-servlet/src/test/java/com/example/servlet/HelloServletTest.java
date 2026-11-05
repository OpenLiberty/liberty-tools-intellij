package com.example.servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;

class HelloServletTest {

    private String call(String name) throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        StringWriter out = new StringWriter();
        when(req.getParameter("name")).thenReturn(name);
        when(resp.getWriter()).thenReturn(new PrintWriter(out, true));

        new HelloServlet().doGet(req, resp);
        return out.toString();
    }

    @Test
    void greetsByName() throws Exception {
        assertEquals("Hello, Bob!", call("Bob"));
    }

    @Test
    void usesDefaultWhenNameMissing() throws Exception {
        assertEquals("Hello, Jakarta EE 11!", call(null));
    }
}
