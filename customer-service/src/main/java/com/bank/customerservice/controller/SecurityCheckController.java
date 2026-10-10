package com.bank.customerservice.controller;

import com.bank.customerservice.repository.AccountSecurityRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/internal/security")
public class SecurityCheckController {

    private final AccountSecurityRepository securityRepository;

    public SecurityCheckController(AccountSecurityRepository securityRepository) {
        this.securityRepository = securityRepository;
    }

    @GetMapping("/check-account/{accountId}")
    public ResponseEntity<Map<String, Object>> checkAccountSecurity(@PathVariable Long accountId) {
        return securityRepository.findByAccountId(accountId)
                .map(setting -> ResponseEntity.ok(Map.<String, Object>of(
                        "accountId", setting.getAccountId(),
                        "isFrozen", setting.getIsFrozen(),
                        "isTwoFactorEnabled", setting.getIsTwoFactorEnabled()
                )))
                .orElse(ResponseEntity.notFound().build());
    }
}