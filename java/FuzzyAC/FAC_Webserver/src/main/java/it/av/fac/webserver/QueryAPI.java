/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.webserver;

import it.av.fac.enforcement.handlers.FACEnforcer;
import it.av.fac.messaging.client.QueryReply;
import it.av.fac.messaging.client.QueryRequest;
import it.av.fac.messaging.client.ReplyStatus;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.codec.binary.Base64;

/**
 * Entry point to the architecture.
 * @author Diogo Regateiro
 */
public class QueryAPI extends HttpServlet {

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
        response.setContentType("text/application;base64");
        QueryRequest queryRequest = new QueryRequest();
        queryRequest.readFromBytes(Base64.decodeBase64((String) request.getParameter("request")));
        System.out.println("Received query: " + queryRequest.getQuery());
        
        try (PrintWriter out = response.getWriter()) {
            QueryReply reply;
            try {
                reply = FACEnforcer.getInstance().handle(queryRequest);
            } catch (Exception ex) {
                reply = new QueryReply();
                reply.setStatus(ReplyStatus.ERROR);
                reply.setErrorMsg(ex.getMessage());
            }
            out.println(Base64.encodeBase64URLSafeString(reply.convertToBytes()));
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
