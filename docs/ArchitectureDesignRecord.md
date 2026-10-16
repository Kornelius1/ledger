# ADR 0001: Lock Ordering Strategy for Deadlock Prevention in Cross-Transfers

* **Status**: Accepted
* **Tanggal**: 2026-10-16
* **Pengambil Keputusan**: Software Architecture & Engineering Team
* **Komponen**: `ledger-service`

---

## 1. Konteks dan Masalah
Pada sistem perbankan/ledger mikroservis, skenario transfer dana secara bersamaan antar dua akun (misal: Akun A menransfer ke Akun B, dan secara bersamaan Akun B menransfer ke Akun A) berpotensi tinggi memicu **Database Deadlock** (`SQLSTATE 40P01` di PostgreSQL).

Kondisi *deadlock* terjadi karena *circular wait*:
* Transaksi 1 mengunci Akun A (`FOR UPDATE`) dan mencoba mengunci Akun B.
* Transaksi 2 mengunci Akun B (`FOR UPDATE`) dan mencoba mengunci Akun A.

Kedua transaksi saling menunggu sumber daya yang dikunci lawan hingga database mendeteksi *deadlock* dan menggagalkan salah satu transaksi secara paksa.

---

## 2. Keputusan
Kami memutuskan untuk menerapkan strategi **Deterministic Lock Ordering** pada layer aplikasi/service saat mengeksekusi operasi yang melibatkan dua akun atau lebih.

### Aturan Utama:
1. Setiap operasi yang membutuhkan penguncian *Pessimistic Lock* (`FOR NO KEY UPDATE` / `FOR UPDATE`) pada dua akun **wajib mengurutkan penguncian berdasarkan Primary Key (`account_id`) terkecil terlebih dahulu**.
2. Formula urutan penguncian:
   $$\text{FirstLock} = \min(\text{AccountId}_A, \text{AccountId}_B)$$
   $$\text{SecondLock} = \max(\text{AccountId}_A, \text{AccountId}_B)$$

```java
Long firstLockId = Math.min(sourceAccountId, destinationAccountId);
Long secondLockId = Math.max(sourceAccountId, destinationAccountId);

Account firstAccount = accountRepository.findByIdForUpdate(firstLockId);
Account secondAccount = accountRepository.findByIdForUpdate(secondLockId);
```

---

## 3. Konsekuensi

### Dampak Positif (+)
* **Bebas Deadlock (100% Deadlock-Free)**: Menghilangkan kemungkinan *circular wait* secara teoritis dan praktis di tingkat database.
* **Prediktabilitas Latensi**: Transaksi kedua akan mengantre secara tertib (*blocking/queued*) hingga transaksi pertama melepaskan *lock* melalui `COMMIT` atau `ROLLBACK`.
* **Sederhana & Teruji**: Tidak memerlukan pustaka eksternal atau logika *retry loop* yang kompleks di sisi aplikasi.

### Dampak Negatif (-)
* **Pengantrean Transaksi**: Mengurangi throughput maksimum pada akun yang sangat aktif (hot-account) karena transaksi diproses secara sekuensial.

---

## 4. Alternatif yang Dipertimbangkan

1. **Optimistic Locking (`@Version`) with Retry**:
   * *Alasan Ditolak*: Pada beban konkurensi tinggi, angka kegagalan (*retry exhaustion*) sangat tinggi sehingga meningkatkan latensi dan penggunaan CPU.
2. **Distributed Lock via Redis (Redlock)**:
   * *Alasan Ditolak*: Menambah kompleksitas infrastruktur dan berisiko mengalami *split-brain* atau kegagalan jaringan antarservis.