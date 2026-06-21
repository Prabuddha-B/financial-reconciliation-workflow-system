package com.frwss.system.audit.service;

public class AuditChainResult {
    private boolean valid;
    private Long brokenAtId;
    private String reason;

    public AuditChainResult(boolean valid, Long brokenAtId, String reason){
        this.valid = valid;
        this.brokenAtId = brokenAtId;
        this.reason = reason;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public Long getBrokenAtId() {
        return brokenAtId;
    }

    public void setBrokenAtId(Long brokenAtId) {
        this.brokenAtId = brokenAtId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
