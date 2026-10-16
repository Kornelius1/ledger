# Concurrency & Deadlock Testing Guide: ledger-service

---

## Standar Prosedur Reset Data (DRY Setup)

Gunakan prosedur berikut untuk menyiapkan kondisi database sebelum mengeksekusi pengujian.

### [PROC-RESET-SINGLE] Setup Akun Single Transfer (Race Condition & Overdraft)

Menyiapkan saldo Akun `1001` sebesar 500.00 dan membersihkan riwayat penahanan saldo.

```bash
docker exec -it ledger-postgres-ledger-1 psql -U ledger_admin -d db_ledger -c "
UPDATE accounts SET balance = 500.00, hold_balance = 0.00 WHERE id = 1001;
DELETE FROM account_holds WHERE account_id = 1001;"
```

### [PROC-RESET-CROSS] Setup Transfer Silang (Cross-Transfer Deadlock Test)

Menyiapkan dua akun berimbang dengan saldo 1000.00 dan dua record penahanan saldo berstatus `HELD` sebesar 300.00.

```bash
docker exec -it ledger-postgres-ledger-1 psql -U ledger_admin -d db_ledger -c "
UPDATE accounts SET balance = 1000.00, hold_balance = 300.00 WHERE id IN (1001, 1002);
DELETE FROM account_holds;
INSERT INTO account_holds (id, account_id, amount, created_at, expires_at, status) 
VALUES 
  ('11111111-1111-1111-1111-111111111111', 1001, 300.00, NOW(), NOW() + INTERVAL '10 minutes', 'HELD'),
  ('22222222-2222-2222-2222-222222222222', 1002, 300.00, NOW(), NOW() + INTERVAL '10 minutes', 'HELD');"
```

---

## Skenario 1: Spike Concurrency & Race Condition Test (`Ledger.jmx`)

### 1. Detail Konfigurasi JMeter

- **File Script**: `Ledger.jmx`
- **Thread Group**: `10 Users`, Ramp-up: `1 detik`, Loop Count: `1`
- **Synchronizing Timer**: `Number of Simulated Users to Group by: 10`, `Timeout: 5000 ms`
- **HTTP Header Manager**: `Content-Type: application/json`
- **HTTP Request**:
  - **Method**: `POST`
  - **Endpoint**: `http://localhost:8083/api/v1/transfers`
- **Request Payload**:
  ```json
  {
    "sourceAccountId": 1001,
    "destinationAccountId": 1002,
    "amount": 300.00,
    "idempotencyKey": "TRX-JMETER-${__UUID()}"
  }
  ```

### 2. Eksekusi
1. Jalankan **`[PROC-RESET-SINGLE]`**.
2. Jalankan skrip via PowerShell CLI:
   ```powershell
   .\jmeter -n -t "C:\apache-jmeter-5.6.3\bin\Ledger.jmx" -l "D:\result_race_condition.jtl"
   ```

### 3. Kriteria Kelulusan
- **JMeter Output**: Tepat **1 request** berhasil (`200 OK`), sedangkan **9 request** selebihnya ditolak (`400 Bad Request` / `422 Unprocessable Entity`) karena saldo 500.00 hanya cukup untuk 1 kali transfer bernilai 300.00.
- **Integritas Database**: Saldo Akun `1001` tidak pernah minus (*overdraft*).

---

## Skenario 2: Deadlock Prevention Test (`LedgerDeadlock.jmx`)

### 1. Detail Konfigurasi JMeter

- **File Script**: `LedgerDeadlock.jmx`
- **Synchronizing Timer Global**: `Number of Simulated Users to Group by: 2`, `Timeout: 5000 ms`
- **HTTP Header Manager**: `Content-Type: application/json`

File ini dikonfigurasi dengan **2 Thread Group terpisah** di dalam satu Test Plan:

#### **Thread Group 1: Commit Hold Akun 1001 ke 1002**
- **Number of Threads (users)**: `1`
- **Ramp-up period**: `0 detik`
- **Loop Count**: `1`
- **HTTP Request**: `POST http://localhost:8083/api/holds/commit`
- **Request Payload**:
  ```json
  {
    "holdId": "11111111-1111-1111-1111-111111111111",
    "destinationAccountId": 1002
  }
  ```

#### **Thread Group 2: Commit Hold Akun 1002 ke 1001**
- **Number of Threads (users)**: `1`
- **Ramp-up period**: `0 detik`
- **Loop Count**: `1`
- **HTTP Request**: `POST http://localhost:8083/api/holds/commit`
- **Request Payload**:
  ```json
  {
    "holdId": "22222222-2222-2222-2222-222222222222",
    "destinationAccountId": 1001
  }
  ```

### 2. Eksekusi
1. Jalankan **`[PROC-RESET-CROSS]`**.
2. Jalankan skrip via PowerShell CLI:
   ```powershell
   .\jmeter -n -t "C:\apache-jmeter-5.6.3\bin\LedgerDeadlock.jmx" -l "D:\result_deadlock.jtl"
   ```

### 3. Kriteria Kelulusan
- **JMeter Output**: `summary = 2 in ... Err: 0 (0.00%)` (kedua request sukses terproses).
- **Log Database**: Log PostgreSQL (`docker logs ledger-postgres-ledger-1`) bersih dari error `SQLSTATE 40P01` (*deadlock detected*).
- **Status Database**: Status kedua record di tabel `account_holds` berubah dari `HELD` menjadi `COMMITTED`.

---

## 🔍 Verifikasi Database Pasca-Pengujian

```bash
docker exec -it ledger-postgres-ledger-1 psql -U ledger_admin -d db_ledger -c "
SELECT id, balance, hold_balance FROM accounts WHERE id IN (1001, 1002);
SELECT id, account_id, status FROM account_holds;"
```