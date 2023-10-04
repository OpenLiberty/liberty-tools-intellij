package io.openliberty.sample.jakarta.servlet;

import jakarta.servlet.annotation.WebServlet;

@WebServlet(name = "filterdemo", urlPatterns = { "/filter" })
public class DontImplementFilter {

}