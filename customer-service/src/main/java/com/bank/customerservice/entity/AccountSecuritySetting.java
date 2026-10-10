package com.bank.customerservice.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "account_security_settings")
public class AccountSecuritySetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "is_two_factor_enabled", nullable = false)
    private Boolean isTwoFactorEnabled;

    @Column(name = "is_frozen", nullable = false)
    private Boolean isFrozen;

    @Column(name = "security_question", nullable = false)
    private String securityQuestion;

    @Column(name = "security_answer", nullable = false)
    private String securityAnswer;

    // Getters and Setters
    public Long getId() { return id; }
    public Long getAccountId() { return accountId; }
    public Boolean getIsTwoFactorEnabled() { return isTwoFactorEnabled; }
    public Boolean getIsFrozen() { return isFrozen; }
    public String getSecurityQuestion() { return securityQuestion; }
    public String getSecurityAnswer() { return securityAnswer; }
    
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public void setIsTwoFactorEnabled(Boolean isTwoFactorEnabled) { this.isTwoFactorEnabled = isTwoFactorEnabled; }
    public void setIsFrozen(Boolean isFrozen) { this.isFrozen = isFrozen; }
    public void setSecurityQuestion(String securityQuestion) { this.securityQuestion = securityQuestion; }
    public void setSecurityAnswer(String securityAnswer) { this.securityAnswer = securityAnswer; }
}