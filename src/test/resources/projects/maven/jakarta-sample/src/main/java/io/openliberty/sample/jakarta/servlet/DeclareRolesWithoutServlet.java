package io.openliberty.sample.jakarta.servlet;

import jakarta.annotation.security.DeclareRoles;
import jakarta.servlet.http.HttpServlet;

@DeclareRoles("Administrator")
public class DeclareRolesWithoutServlet extends HttpServlet {

}
