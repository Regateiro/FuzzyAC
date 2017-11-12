/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.webserver;

import it.av.fac.webserver.handlers.WikiHandler;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Entry point to the architecture.
 *
 * @author Diogo Regateiro
 */
public class WikiAPI extends HttpServlet {

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
        response.setContentType("text/html");

        try (PrintWriter out = response.getWriter()) {
            String html = "";
            try {
                if (request.getPathInfo() != null){// && request.getParameter("token") != null) {
                    String page = URLDecoder.decode((String) request.getPathInfo().substring(1), "UTF-8");
                    //String userToken = URLDecoder.decode((String) request.getParameter("token"), "UTF-8");
                    System.out.println("Received query for page: " + page);
                    html = handler.fetch("", page);
                    html = html.replaceAll("\\[\\[([^]]*)\\|([^]]*)\\]\\]", "<a href=\"/FAC_Webserver/WikiAPI/$1\">$2</a>");
                    html = html.replaceAll("\\[\\[([^]]*)\\]\\]", "<a href=\"/FAC_Webserver/WikiAPI/$1\">$1</a>");
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
