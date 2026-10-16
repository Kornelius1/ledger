# Database Schema & Data Dictionary

* **Database Engine**: PostgreSQL 16
* **Database Name**: `db_ledger`

---

## 1. ERD & Relasi Tabel

```text
[accounts] 1 --- * [account_holds]
   1                 |
   |                 |
   *                 *
[ledger_entries] * --- 1 [transactions]
```

---

## 2. Kamus Data

### Tabel: `accounts`
Menyimpan data saldo dan akun nasabah.

| Kolom | Tipe Data | Constraint | Keterangan |
| :--- | :--- | :--- | :--- |
| `id` | `BIGINT` | PRIMARY KEY | ID unik akun. |
| `account_number` | `VARCHAR(50)` | UNIQUE, NOT NULL | Nomor rekening akun. |
| `balance` | `NUMERIC(15,2)` | NOT NULL | Total saldo riil akun. |
| `hold_balance` | `NUMERIC(15,2)` | NOT NULL, DEFAULT 0 | Saldo yang sedang ditahan/dikunci. |
| `status` | `VARCHAR(20)` | NOT NULL | Status akun (`ACTIVE`, `FROZEN`, `CLOSED`). |

---

### Tabel: `account_holds`
Menyimpan instruksi penahanan saldo sementara.

| Kolom | Tipe Data | Constraint | Keterangan |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | PRIMARY KEY | ID penahanan saldo. |
| `account_id` | `BIGINT` | FOREIGN KEY | Referensi ke `accounts(id)`. |
| `amount` | `NUMERIC(15,2)` | NOT NULL | Jumlah saldo yang ditahan. |
| `status` | `VARCHAR(20)` | NOT NULL | Status hold (`HELD`, `COMMITTED`, `RELEASED`, `EXPIRED`). |
| `created_at` | `TIMESTAMP` | NOT NULL | Waktu pembuatan record hold. |
| `expires_at` | `TIMESTAMP` | NOT NULL | Waktu kadaluarsa penahanan saldo. |

---

### Tabel: `transactions`
Menyimpan header transaksi pemindahan dana.

| Kolom | Tipe Data | Constraint | Keterangan |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | PRIMARY KEY | ID unik transaksi. |
| `source_account_id` | `BIGINT` | FOREIGN KEY | Rekening pengirim. |
| `destination_account_id` | `BIGINT` | FOREIGN KEY | Rekening penerima. |
| `amount` | `NUMERIC(15,2)` | NOT NULL | Nominal transfer. |
| `idempotency_key` | `VARCHAR(100)` | UNIQUE, NOT NULL | Kunci idempotensi transaksi. |
| `status` | `VARCHAR(20)` | NOT NULL | Status transaksi (`SUCCESS`, `FAILED`). |
| `created_at` | `TIMESTAMP` | NOT NULL | Waktu transaksi dieksekusi. |

---

### Tabel: `ledger_entries`
Menyimpan pembukuan entri ganda (*double-entry*) untuk debit dan kredit.

| Kolom | Tipe Data | Constraint | Keterangan |
| :--- | :--- | :--- | :--- |
| `id` | `BIGSERIAL` | PRIMARY KEY | ID baris pembukuan. |
| `transaction_id` | `UUID` | FOREIGN KEY | Referensi ke `transactions(id)`. |
| `account_id` | `BIGINT` | FOREIGN KEY | Referensi ke `accounts(id)`. |
| `entry_type` | `VARCHAR(10)` | NOT NULL | Jenis entri (`DEBIT` atau `CREDIT`). |
| `amount` | `NUMERIC(15,2)` | NOT NULL | Nominal entri. |
| `created_at` | `TIMESTAMP` | NOT NULL | Waktu entri dicatat. |