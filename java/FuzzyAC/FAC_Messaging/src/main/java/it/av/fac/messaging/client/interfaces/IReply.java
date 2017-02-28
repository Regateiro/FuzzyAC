/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.messaging.client.interfaces;

import it.av.fac.messaging.client.ReplyStatus;
import java.io.IOException;

/**
 *
 * @author Diogo Regateiro
 * @param <R>
 */
public interface IReply<R extends IReply> {
    public void setStatus(ReplyStatus status);
    public byte[] convertToBytes() throws IOException;
    public R readFromBytes(byte[] bytes) throws IOException;
}
