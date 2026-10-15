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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TransferService {

    private static final String IDEMPOTENCY_PREFIX = "idempotency:";
    private static final String KAFKA_TOPIC = "transaction-settlement";
    private static final String PROCESSING = "PROCESSING";
    private static final String SUCCESS = "SUCCESS";
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final RestTemplate restTemplate;

    public TransferService(
            AccountRepository accountRepository,
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

    @Transactional(readOnly = true)
    public TransferResponse getTransferStatus(UUID id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaksi tidak ditemukan"));

        return new TransferResponse(
                transaction.getStatus(),
                transaction.getId(),
                transaction.getIdempotencyKey(),
                transaction.getSourceAccountId(),
                transaction.getDestinationAccountId(),
                transaction.getAmount());
    }

    @Transactional
    public TransferResponse processTransfer(TransferRequest request) {
        validateRequest(request);

        String key = request.idempotencyKey();
        String redisKey = IDEMPOTENCY_PREFIX + key;

        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, PROCESSING, IDEMPOTENCY_TTL);

        if (Boolean.FALSE.equals(acquired)) {
            return getExistingTransfer(request);
        }

        if (acquired == null) {
            throw new IllegalStateException("Tidak dapat memastikan status idempotency");
        }

        try {
            // 1. Periksa keamanan rekening
            validateAccountSecurity(request.sourceAccountId());
            validateAccountSecurity(request.destinationAccountId());

            // 2. Kunci kedua rekening dengan urutan ID konsisten
            List<Long> accountIds = List.of(
                    request.sourceAccountId(),
                    request.destinationAccountId()).stream().sorted().toList();

            List<Account> accounts = accountRepository.findAllByIdsForUpdate(accountIds);

            Account source = accounts.stream()
                    .filter(a -> a.getId().equals(request.sourceAccountId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Rekening pengirim tidak ditemukan"));

            Account destination = accounts.stream()
                    .filter(a -> a.getId().equals(request.destinationAccountId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Rekening penerima tidak ditemukan"));

            // 3. Pastikan saldo mencukupi
            if (source.getBalance().compareTo(request.amount()) < 0) {
                throw new IllegalArgumentException("Saldo tidak mencukupi");
            }

            // 4. Mutasi saldo dalam satu transaksi database
            source.setBalance(source.getBalance().subtract(request.amount()));
            destination.setBalance(destination.getBalance().add(request.amount()));

            accountRepository.saveAll(List.of(source, destination));

            // 5. Simpan transaksi dan double-entry ledger
            UUID transactionId = UUID.randomUUID();

            Transaction transaction = new Transaction(
                    transactionId,
                    key,
                    source.getId(),
                    destination.getId(),
                    request.amount(),
                    SUCCESS);

            transactionRepository.save(transaction);

            ledgerEntryRepository.saveAll(List.of(
                    new LedgerEntry(
                            transactionId,
                            source.getId(),
                            "DEBIT",
                            request.amount()),
                    new LedgerEntry(
                            transactionId,
                            destination.getId(),
                            "CREDIT",
                            request.amount())));

            // 6. Kirim event hanya setelah database berhasil commit
            String event = String.format(
                    "Transfer %s dari %d ke %d sukses",
                    request.amount(),
                    source.getId(),
                    destination.getId());

            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            kafkaTemplate.send(
                                    KAFKA_TOPIC,
                                    transactionId.toString(),
                                    event);
                        }
                    });

            return new TransferResponse(
                    SUCCESS,
                    transactionId,
                    key,
                    source.getId(),
                    destination.getId(),
                    request.amount());

        } catch (Exception e) {
            // Hapus kunci Redis jika proses gagal agar tidak tersangkut berstatus PROCESSING
            redisTemplate.delete(redisKey);
            throw e;
        }
    }

    private TransferResponse getExistingTransfer(TransferRequest request) {
        Transaction existing = transactionRepository
                .findByIdempotencyKey(request.idempotencyKey())
                .orElseThrow(() -> new IllegalStateException(
                        "Transaksi sedang diproses. Coba kembali nanti."));

        boolean sameRequest = existing.getSourceAccountId().equals(request.sourceAccountId())
                && existing.getDestinationAccountId().equals(request.destinationAccountId())
                && existing.getAmount().compareTo(request.amount()) == 0;

        if (!sameRequest) {
            throw new IllegalArgumentException("Idempotency key sudah digunakan untuk request berbeda");
        }

        return new TransferResponse(
                existing.getStatus(),
                existing.getId(),
                existing.getIdempotencyKey(),
                existing.getSourceAccountId(),
                existing.getDestinationAccountId(),
                existing.getAmount());
    }

    private void validateRequest(TransferRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request wajib diisi");
        }

        if (request.idempotencyKey() == null || request.idempotencyKey().isBlank()) {
            throw new IllegalArgumentException("Idempotency key wajib diisi");
        }

        if (request.sourceAccountId() == null || request.destinationAccountId() == null) {
            throw new IllegalArgumentException("Rekening wajib diisi");
        }

        if (request.sourceAccountId().equals(request.destinationAccountId())) {
            throw new IllegalArgumentException("Rekening pengirim dan penerima tidak boleh sama");
        }

        BigDecimal amount = request.amount();

        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Nominal transfer harus lebih besar dari nol");
        }

        if (amount.scale() > 2) {
            throw new IllegalArgumentException("Nominal maksimal dua angka desimal");
        }
    }

    private void validateAccountSecurity(Long accountId) {
        // Port diubah dari 8081 ke 8082 untuk langsung menembak customer-service
        String url = "http://localhost:8082/api/v1/internal/security/check-account/" + accountId;

        final Map<?, ?> response;

        try {
            response = restTemplate.getForObject(url, Map.class);
        } catch (Exception e) {
            throw new IllegalStateException("Gagal memverifikasi keamanan rekening " + accountId, e);
        }

        if (response == null || !(response.get("isFrozen") instanceof Boolean frozen)) {
            throw new IllegalStateException("Respons keamanan rekening tidak valid");
        }

        if (frozen) {
            throw new IllegalStateException("Rekening " + accountId + " sedang dibekukan");
        }
    }
}