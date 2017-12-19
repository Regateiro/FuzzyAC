/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.webserver;

import it.av.fac.webserver.handlers.QueryString;
import it.av.fac.webserver.handlers.WikiHandler;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public class WikiAdminUsers extends HttpServlet {

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        WikiHandler handler = new WikiHandler();
        response.setContentType("text/html;charset=UTF-8");

        try (PrintWriter out = response.getWriter()) {
            String html = "";
            try {
                Map<String, String> params = QueryString.parseQueryString(request.getQueryString());
                if (params.containsKey("action") && params.get("action").equalsIgnoreCase("register")) {
                    // register from wiki
                    String username = params.get("username");
                    if (username != null) {
                        html = handler.registerUser(username);
                    } else {
                        html = handler.generateErrorPage(new RuntimeException("username key missing."));
                    }
                } else {
                    // get info and display
                    String token = params.get("token");
                    if (token != null) {
                        html = handler.getUser(token);
                    } else {
                        html = handler.generateErrorPage(new RuntimeException("token key missing."));
                    }
                }
            } catch (UnsupportedEncodingException ex) {
                html = handler.generateErrorPage(ex);
            }
            out.println(html);
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
