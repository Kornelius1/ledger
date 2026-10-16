# 🧪 Concurrency & Deadlock Testing Guide (`ledger-service`)

Dokumen ini memuat panduan praktis dan ringkas untuk mengeksekusi dua skenario pengujian konkurensi utama pada `ledger-service` menggunakan Apache JMeter CLI.

---

## 📌 Ringkasan Skenario Pengujian

| Parameter | Skenario 1: Race Condition Test | Skenario 2: Deadlock Prevention Test |
| :--- | :--- | :--- |
| **File Skrip** | `Ledger.jmx` | `LedgerDeadlock.jmx` |
| **Fokus Pengujian** | Mencegah *Double Spending* & *Data Corruption* pada satu akun yang sama. | Mencegah *Circular Wait / Deadlock* (`40P01`) pada transfer dua arah (*cross-transfer*). |
| **Target Akun** | Akun Tunggal (misal: ID `1001`) | Akun Ganda (Transfer Silang ID `1001` $\leftrightarrow$ `1002`) |
| **Mekanisme Validasi** | *Pessimistic Locking* (`FOR UPDATE`) & *Idempotency Filter* | *Deterministic Lock Ordering* ($\min(\text{ID}), \max(\text{ID})$) |

---

## 🟢 Skenario 1: Race Condition Test (`Ledger.jmx`)

### 1. Tujuan
Memastikan saat multiple *request* penahanan saldo (`/api/holds`) atau *commit* dikirimkan secara serentak pada milidetik yang sama (*spike test*), saldo akun tidak terpotong melebihi batas saldo tersedia (*overdraft*) dan tidak terjadi pemrosesan ganda.

### 2. Reset Data Uji di PostgreSQL
```bash
docker exec -it ledger-postgres-ledger-1 psql -U ledger_admin -d db_ledger -c "
UPDATE accounts SET balance = 500.00, hold_balance = 0.00 WHERE id = 1001;
DELETE FROM account_holds WHERE account_id = 1001;
"
```

### 3. Eksekusi JMeter CLI
```powershell
Remove-Item -Path "D:\result_race_condition.jtl" -ErrorAction SilentlyContinue
cd C:\apache-jmeter-5.6.3\bin
.\jmeter -n -t "C:\apache-jmeter-5.6.3\bin\Ledger.jmx" -l "D:\result_race_condition.jtl"
```

### 4. Kriteria Kelulusan
* **Ketersediaan Saldo**: Saldo `balance` dan `hold_balance` pasca-pengujian sesuai dengan kalkulasi matematika transaksi yang sah.
* **Integritas Record**: Hanya *request* yang memenuhi syarat kuota saldo yang berhasil (`200 OK`), sisanya ditolak dengan aman (`400 Bad Request` / `409 Conflict`).

---

## 🔴 Skenario 2: Deadlock Prevention Test (`LedgerDeadlock.jmx`)

### 1. Tujuan
Memastikan dua transaksi yang mengeksekusi transfer silang secara bersamaan (Akun A $\rightarrow$ Akun B dan Akun B $\rightarrow$ Akun A) dapat berjalan sekuensial tanpa memicu kebuntuan transaksi (*deadlock*) di PostgreSQL.

### 2. Reset Data Uji di PostgreSQL
```bash
docker exec -it ledger-postgres-ledger-1 psql -U ledger_admin -d db_ledger -c "
UPDATE accounts SET balance = 1000.00, hold_balance = 300.00 WHERE id IN (1001, 1002);
DELETE FROM account_holds;
INSERT INTO account_holds (id, account_id, amount, created_at, expires_at, status) 
VALUES 
  ('11111111-1111-1111-1111-111111111111', 1001, 300.00, NOW(), NOW() + INTERVAL '10 minutes', 'HELD'),
  ('22222222-2222-2222-2222-222222222222', 1002, 300.00, NOW(), NOW() + INTERVAL '10 minutes', 'HELD');"
```

### 3. Eksekusi JMeter CLI
```powershell
Remove-Item -Path "D:\result_deadlock.jtl" -ErrorAction SilentlyContinue
cd C:\apache-jmeter-5.6.3\bin
.\jmeter -n -t "C:\apache-jmeter-5.6.3\bin\LedgerDeadlock.jmx" -l "D:\result_deadlock.jtl"
```

### 4. Kriteria Kelulusan
* **Error Rate**: Output konsol JMeter menunjukkan `Err: 0 (0.00%)`.
* **Database Logs**: Log PostgreSQL (`docker logs ledger-postgres-ledger-1`) bersih dari error `SQLSTATE 40P01` (`deadlock detected`).
* **Status Akhir Hold**: Status kedua record di tabel `account_holds` berubah menjadi `COMMITTED` dan `hold_balance` kedua akun kembali ke `0.00`.

---

## 🔍 Command Verifikasi Cepat Pasca-Pengujian

Jalankan perintah SQL ini di terminal untuk memeriksa saldo dan status data *hold* secara langsung:

```bash
docker exec -it ledger-postgres-ledger-1 psql -U ledger_admin -d db_ledger -c "
SELECT id, balance, hold_balance FROM accounts WHERE id IN (1001, 1002);
SELECT id, account_id, status FROM account_holds;
"
```