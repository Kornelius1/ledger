package com.bank.customerservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bank.customerservice.entity.AccountSecuritySetting;

import java.util.Optional;

public interface AccountSecurityRepository extends JpaRepository<AccountSecuritySetting, Long> {
    Optional<AccountSecuritySetting> findByAccountId(Long accountId);
}   