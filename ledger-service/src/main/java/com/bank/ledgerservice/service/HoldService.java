package com.bank.ledgerservice.service;

import com.bank.ledgerservice.entity.Account;
import com.bank.ledgerservice.entity.AccountHold;
import com.bank.ledgerservice.entity.HoldStatus;
import com.bank.ledgerservice.repository.AccountHoldRepository;
import com.bank.ledgerservice.repository.AccountRepository;
import com.bank.ledgerservice.exception.InsufficientBalanceException;

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
            throw new InsufficientBalanceException("Saldo tersedia tidak mencukupi");
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

    // Menandai method agar seluruh proses berjalan dalam satu transaksi database atomik
    @Transactional
    public void commitHold(UUID holdId, Long destinationAccountId) {
        // 1. Cari data tahanan dana berdasarkan ID
        AccountHold hold = holdRepository.findById(holdId)
                .orElseThrow(() -> new IllegalArgumentException("Data hold tidak ditemukan"));

        // 2. Validasi status hold, pastikan masih HELD
        if (hold.getStatus() != HoldStatus.HELD) {
            throw new IllegalStateException("Status hold sudah tidak aktif");
        }

        // 3. Periksa apakah masa tahanan dana sudah lewat batas waktu
        if (Instant.now().isAfter(hold.getExpiresAt())) {
            // Ambil data rekening pengirim dengan kunci baris SELECT FOR UPDATE
            Account source = accountRepository.findByIdForUpdate(hold.getAccountId())
                    .orElseThrow(() -> new IllegalArgumentException("Rekening pengirim tidak ditemukan"));
            
            // Lepaskan nominal yang tertahan kembali ke akun pengirim
            source.setHoldBalance(source.getHoldBalance().subtract(hold.getAmount()));
            accountRepository.save(source);

            // Ubah status hold menjadi kadaluwarsa
            hold.setStatus(HoldStatus.EXPIRED);
            holdRepository.save(hold);

            throw new IllegalStateException("Hold balance sudah kadaluwarsa dan transaksi dibatalkan");
        }

        // 4. Urutkan ID rekening dari kecil ke besar untuk urutan penguncian agar terhindar dari deadlock
        Long sourceAccountId = hold.getAccountId();
        Long firstLockId = Math.min(sourceAccountId, destinationAccountId);
        Long secondLockId = Math.max(sourceAccountId, destinationAccountId);

        // 5. Kunci baris rekening pertama di database
        Account firstAccount = accountRepository.findByIdForUpdate(firstLockId)
                .orElseThrow(() -> new IllegalArgumentException("Rekening tidak ditemukan: " + firstLockId));

        // 6. Kunci baris rekening kedua, atau gunakan referensi rekening pertama jika ID pengirim dan penerima sama
        Account secondAccount = sourceAccountId.equals(destinationAccountId) 
                ? firstAccount 
                : accountRepository.findByIdForUpdate(secondLockId)
                        .orElseThrow(() -> new IllegalArgumentException("Rekening tidak ditemukan: " + secondLockId));

        // 7. Tentukan variabel pengirim dan penerima berdasarkan objek yang sudah terkunci
        Account source = sourceAccountId.equals(firstAccount.getId()) ? firstAccount : secondAccount;
        Account destination = destinationAccountId.equals(firstAccount.getId()) ? firstAccount : secondAccount;

        // 8. Potong saldo utama dan saldo tertahan pengirim, lalu tambahkan ke saldo penerima
        source.setBalance(source.getBalance().subtract(hold.getAmount()));
        source.setHoldBalance(source.getHoldBalance().subtract(hold.getAmount()));
        destination.setBalance(destination.getBalance().add(hold.getAmount()));

        // 9. Simpan perubahan data saldo kedua rekening ke database
        accountRepository.save(source);
        accountRepository.save(destination);

        // 10. Perbarui status hold menjadi COMMITTED dan simpan
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