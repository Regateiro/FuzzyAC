/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.messaging.client;

import com.alibaba.fastjson.JSONObject;
import it.av.fac.messaging.client.interfaces.IReply;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.xerial.snappy.Snappy;

/**
 *
 * @author Diogo Regateiro
 */
public class DecisionReply implements IReply<DecisionReply> {

    private final Map<String, Map<String, Boolean>> decisions;

    private ReplyStatus status;

    private String errorMsg;

    public DecisionReply() {
        this.decisions = new HashMap<>();
        this.errorMsg = "";
        this.status = ReplyStatus.OK;
    }

    /**
     * Adds a decision to this reply.
     *
     * @param securityLabel The security label for which the decision applies.
     * @param permission The permission for which the decision applies.
     * @param isGranted Whether or not the permission for the given security
     * label was granted to the user.
     */
    public void addDecision(String securityLabel, String permission, boolean isGranted) {
        this.decisions.putIfAbsent(securityLabel, new HashMap<>());
        this.decisions.get(securityLabel).put(permission, isGranted);
    }

    /**
     * Gets the list of security labels within this reply.
     *
     * @return A set of security labels.
     */
    public Set<String> getSecurityLabels() {
        return Collections.unmodifiableSet(this.decisions.keySet());
    }

    /**
     * Returns the decision made for each permission within a security label.
     *
     * @param securityLabel The security label to obtain the access control
     * decision to.
     * @return A mapping of permissions to decisions, true being granted and
     * false being denied.
     */
    public Map<String, Boolean> getSecurityLabelDecision(String securityLabel) {
        return Collections.unmodifiableMap(this.decisions.get(securityLabel));
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
    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    @Override
    public void setStatus(ReplyStatus status) {
        this.status = status;
    }

    @Override
    public byte[] convertToBytes() throws IOException {
        JSONObject ret = new JSONObject();

        ret.put("status", status.name());
        ret.put("error_msg", errorMsg);

        JSONObject jsonDecisions = new JSONObject();
        decisions.keySet().stream().forEach((label) -> {
            JSONObject jsonLabelDecisions = new JSONObject();
            decisions.get(label).keySet().stream().forEach((permission) -> {
                jsonLabelDecisions.put(permission, decisions.get(label).get(permission));
            });
            jsonDecisions.put(label, jsonLabelDecisions);
        });

        ret.put("decisions", jsonDecisions);

        return Snappy.compress(ret.toJSONString().getBytes("UTF-8"));
    }

    @Override
    public DecisionReply readFromBytes(byte[] bytes) throws IOException {
        String data = Snappy.uncompressString(bytes, "UTF-8");
        JSONObject obj = JSONObject.parseObject(data);

        setStatus(ReplyStatus.valueOf(obj.getString("status")));
        setErrorMsg(obj.getString("error_msg"));

        this.decisions.clear();
        JSONObject jsonDecisions = obj.getJSONObject("decisions");
        if (jsonDecisions != null) {
            jsonDecisions.keySet().stream().forEach((label) -> {
                JSONObject jsonLabelDecisions = jsonDecisions.getJSONObject(label);
                jsonLabelDecisions.keySet().stream().forEach((permission) -> {
                    addDecision(label, permission, jsonLabelDecisions.getBooleanValue(permission));
                });
            });
        }

        return this;
    }
}
