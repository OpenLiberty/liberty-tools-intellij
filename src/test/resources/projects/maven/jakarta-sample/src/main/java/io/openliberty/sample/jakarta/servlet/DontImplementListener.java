package io.openliberty.sample.jakarta.servlet;

import jakarta.servlet.annotation.WebServlet;

@WebServlet(name = "listenerdemo", urlPatterns = { "/listener" })
public class DontImplementListener {

}