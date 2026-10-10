package com.bank.ledgerservice.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ledger_entries")
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "entry_type", nullable = false)
    private String entryType;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public LedgerEntry() {}

    public LedgerEntry(UUID transactionId, Long accountId, String entryType, BigDecimal amount) {
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.entryType = entryType;
        this.amount = amount;
        this.createdAt = OffsetDateTime.now();
    }
}