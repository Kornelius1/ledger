package com.bank.ledgerservice.dto;

import java.util.UUID;

public class CommitHoldRequest {
    private UUID holdId;
    private Long destinationAccountId;

    public CommitHoldRequest() {}

    public CommitHoldRequest(UUID holdId, Long destinationAccountId) {
        this.holdId = holdId;
        this.destinationAccountId = destinationAccountId;
    }

    public UUID getHoldId() {
        return holdId;
    }

    public void setHoldId(UUID holdId) {
        this.holdId = holdId;
    }

    public Long getDestinationAccountId() {
        return destinationAccountId;
    }

    public void setDestinationAccountId(Long destinationAccountId) {
        this.destinationAccountId = destinationAccountId;
    }
}