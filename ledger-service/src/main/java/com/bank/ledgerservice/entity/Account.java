package com.bank.ledgerservice.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false, unique = true)
    private String accountNumber;

    @Column(nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "hold_balance", nullable = false)
    private BigDecimal holdBalance = BigDecimal.ZERO;

    @Column(nullable = false)
    private String status = "ACTIVE";

    public Account() {}

    public Long getId() { return id; }
    
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public BigDecimal getHoldBalance() { 
        return holdBalance == null ? BigDecimal.ZERO : holdBalance; 
    }
    public void setHoldBalance(BigDecimal holdBalance) { this.holdBalance = holdBalance; }

    public BigDecimal getAvailableBalance() {
        return getBalance().subtract(getHoldBalance());
    }
}