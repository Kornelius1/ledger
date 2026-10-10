package com.bank.ledgerservice.service;

import com.bank.ledgerservice.dto.TransferRequest;
import com.bank.ledgerservice.dto.TransferResponse;
import com.bank.ledgerservice.entity.Account;
import com.bank.ledgerservice.entity.LedgerEntry;
import com.bank.ledgerservice.entity.Transaction;
import com.bank.ledgerservice.repository.AccountRepository;
import com.bank.ledgerservice.repository.LedgerEntryRepository;
import com.bank.ledgerservice.repository.TransactionRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final RestTemplate restTemplate;

    public TransferService(AccountRepository accountRepository,
                           TransactionRepository transactionRepository,
                           LedgerEntryRepository ledgerEntryRepository,
                           StringRedisTemplate redisTemplate,
                           KafkaTemplate<String, String> kafkaTemplate) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.redisTemplate = redisTemplate;
        this.kafkaTemplate = kafkaTemplate;
        this.restTemplate = new RestTemplate();
    }

    @Transactional
    public TransferResponse processTransfer(TransferRequest request) {
        // 1. Redis Idempotency Check
        String redisKey = "idempotency:" + request.idempotencyKey();
        Boolean isNewKey = redisTemplate.opsForValue().setIfAbsent(redisKey, "PROCESSING", Duration.ofHours(24));
        if (Boolean.FALSE.equals(isNewKey)) {
            Transaction existingTx = transactionRepository.findByIdempotencyKey(request.idempotencyKey())
                    .orElseThrow(() -> new IllegalStateException("Transaksi sedang diproses"));
            return new TransferResponse(existingTx.getStatus(), existingTx.getId(), existingTx.getIdempotencyKey(),
                    request.sourceAccountId(), request.destinationAccountId(), existingTx.getAmount());
        }

        // 2. Validasi Status Keamanan Akun ke customer-service (Port 8081)
        validateAccountSecurity(request.sourceAccountId());
        validateAccountSecurity(request.destinationAccountId());

        // 3. Pessimistic Locking berurutan ID (Mencegah Deadlock)
        List<Long> accountIds = List.of(request.sourceAccountId(), request.destinationAccountId());
        List<Account> accounts = accountRepository.findAllByIdsForUpdate(accountIds);

        Account sourceAccount = accounts.stream()
                .filter(a -> a.getId().equals(request.sourceAccountId()))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Rekening pengirim tidak ditemukan"));

        Account destAccount = accounts.stream()
                .filter(a -> a.getId().equals(request.destinationAccountId()))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Rekening penerima tidak ditemukan"));

        // 4. Validasi Saldo
        if (sourceAccount.getBalance().compareTo(request.amount()) < 0) {
            redisTemplate.delete(redisKey);
            throw new IllegalArgumentException("Saldo tidak mencukupi");
        }

        // 5. Mutasi Saldo
        sourceAccount.setBalance(sourceAccount.getBalance().subtract(request.amount()));
        destAccount.setBalance(destAccount.getBalance().add(request.amount()));
        accountRepository.save(sourceAccount);
        accountRepository.save(destAccount);

        // 6. Simpan Transaksi & Ledger Entries (Double-Entry Bookkeeping)
        UUID txId = UUID.randomUUID();
        Transaction transaction = new Transaction(txId, request.idempotencyKey(),
                sourceAccount.getId(), destAccount.getId(), request.amount(), "SUCCESS");
        transactionRepository.save(transaction);

        ledgerEntryRepository.save(new LedgerEntry(txId, sourceAccount.getId(), "DEBIT", request.amount()));
        ledgerEntryRepository.save(new LedgerEntry(txId, destAccount.getId(), "CREDIT", request.amount()));

        // 7. Terbitkan Event ke Kafka
        kafkaTemplate.send("transaction-settlement", txId.toString(),
                "Transfer " + request.amount() + " dari " + sourceAccount.getId() + " ke " + destAccount.getId() + " Sukses");

        return new TransferResponse("SUCCESS", txId, request.idempotencyKey(),
                sourceAccount.getId(), destAccount.getId(), request.amount());
    }

    private void validateAccountSecurity(Long accountId) {
        String url = "http://localhost:8081/api/v1/internal/security/check-account/" + accountId;
        try {
            Map<?, ?> response = restTemplate.getForObject(url, Map.class);
            if (response != null && Boolean.TRUE.equals(response.get("isFrozen"))) {
                throw new IllegalStateException("Rekening ID " + accountId + " dalam status dibekukan");
            }
        } catch (Exception e) {
            throw new IllegalStateException("Gagal memverifikasi status keamanan akun: " + e.getMessage());
        }
    }
}