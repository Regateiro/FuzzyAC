/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.messaging.client;

import it.av.fac.messaging.client.interfaces.IReply;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.xerial.snappy.Snappy;

/**
 *
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public class BDFISReply implements IReply {

    private final ReplyStatus status;

    private final String errorMsg;

    private final JSONArray data;

    public BDFISReply() {
        this.data = new JSONArray();
        this.errorMsg = "";
        this.status = ReplyStatus.OK;
    }

    public BDFISReply(ReplyStatus status, String errorMsg) {
        this.data = new JSONArray();
        this.errorMsg = errorMsg;
        this.status = status;
    }

    private BDFISReply(ReplyStatus status, String errorMsg, JSONArray data) {
        this.data = new JSONArray(data.toString());
        this.errorMsg = errorMsg;
        this.status = status;
    }

    @Override
    public ReplyStatus getStatus() {
        return status;
    }

    @Override
    public String getErrorMsg() {
        return errorMsg;
    }

    @Override
    public byte[] convertToBytes() throws IOException {
        JSONObject ret = new JSONObject();

        ret.put("status", this.status.name());
        ret.put("error_msg", this.errorMsg);
        ret.put("data", this.data);

        return Snappy.compress(ret.toString().getBytes("UTF-8"));
    }

    public static BDFISReply readFromBytes(byte[] bytes) throws IOException {
        String data = Snappy.uncompressString(bytes, "UTF-8");
        JSONObject obj = new JSONObject(data);

        BDFISReply reply = new BDFISReply(
                ReplyStatus.valueOf(obj.optString("status")),
                obj.optString("error_msg"),
                obj.getJSONArray("data")
        );

        return reply;
    }

    @Override
    public List<String> getData() {
        List<String> ret = new ArrayList<>();
        this.data.forEach((databit) -> {
            ret.add((String) databit);
        });
        return ret;
    }

    @Override
    public void addData(String data) {
        this.data.put(data);
    }
}
