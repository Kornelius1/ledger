package com.bank.ledgerservice.service;

import com.bank.ledgerservice.entity.Account;
import com.bank.ledgerservice.entity.AccountHold;
import com.bank.ledgerservice.entity.HoldStatus;
import com.bank.ledgerservice.repository.AccountHoldRepository;
import com.bank.ledgerservice.repository.AccountRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class HoldService {

    private final AccountRepository accountRepository;
    private final AccountHoldRepository holdRepository;

    public HoldService(AccountRepository accountRepository, AccountHoldRepository holdRepository) {
        this.accountRepository = accountRepository;
        this.holdRepository = holdRepository;
    }

    @Transactional
    public AccountHold reserveHold(Long accountId, BigDecimal amount, Duration ttl) {
        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Rekening pengirim tidak ditemukan"));

        if (account.getAvailableBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Saldo tersedia tidak mencukupi");
        }

        account.setHoldBalance(account.getHoldBalance().add(amount));
        accountRepository.save(account);

        AccountHold hold = new AccountHold(
                UUID.randomUUID(),
                accountId,
                amount,
                HoldStatus.HELD,
                Instant.now().plus(ttl)
        );

        return holdRepository.save(hold);
    }

    @Transactional
    public void commitHold(UUID holdId, Long destinationAccountId) {
        AccountHold hold = holdRepository.findById(holdId)
                .orElseThrow(() -> new IllegalArgumentException("Data hold tidak ditemukan"));

        if (hold.getStatus() != HoldStatus.HELD) {
            throw new IllegalStateException("Status hold sudah tidak aktif");
        }

        Account source = accountRepository.findByIdForUpdate(hold.getAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Rekening pengirim tidak ditemukan"));

        Account destination = accountRepository.findByIdForUpdate(destinationAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Rekening penerima tidak ditemukan"));

        source.setBalance(source.getBalance().subtract(hold.getAmount()));
        source.setHoldBalance(source.getHoldBalance().subtract(hold.getAmount()));

        destination.setBalance(destination.getBalance().add(hold.getAmount()));

        accountRepository.save(source);
        accountRepository.save(destination);

        hold.setStatus(HoldStatus.COMMITTED);
        holdRepository.save(hold);
    }

    @Transactional
    public void releaseHold(UUID holdId) {
        AccountHold hold = holdRepository.findById(holdId)
                .orElseThrow(() -> new IllegalArgumentException("Data hold tidak ditemukan"));

        if (hold.getStatus() != HoldStatus.HELD) {
            return;
        }

        Account account = accountRepository.findByIdForUpdate(hold.getAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Rekening pengirim tidak ditemukan"));

        account.setHoldBalance(account.getHoldBalance().subtract(hold.getAmount()));
        accountRepository.save(account);

        hold.setStatus(HoldStatus.RELEASED);
        holdRepository.save(hold);
    }
}