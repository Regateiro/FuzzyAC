/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.messaging.client.interfaces;

import it.av.fac.messaging.client.ReplyStatus;
import java.io.IOException;
import java.util.List;

/**
 *
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public interface IReply {
    public ReplyStatus getStatus();
    public String getErrorMsg();
    public byte[] convertToBytes() throws IOException;
    public List<String> getData();
    public void addData(String document);
}
