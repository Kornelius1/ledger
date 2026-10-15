package com.bank.ledgerservice.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "account_holds")
public class AccountHold {

    @Id
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HoldStatus status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public AccountHold() {}

    public AccountHold(UUID id, Long accountId, BigDecimal amount, HoldStatus status, Instant expiresAt) {
        this.id = id;
        this.accountId = accountId;
        this.amount = amount;
        this.status = status;
        this.expiresAt = expiresAt;
    }

    public UUID getId() { return id; }
    public Long getAccountId() { return accountId; }
    public BigDecimal getAmount() { return amount; }
    public HoldStatus getStatus() { return status; }
    public void setStatus(HoldStatus status) { this.status = status; }
    public Instant getExpiresAt() { return expiresAt; }
}