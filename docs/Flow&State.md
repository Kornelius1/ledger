# Flow & State Machine Diagrams - Ledger Service

Dokumen ini menjelaskan alur data transaksi dan perubahan status penahanan saldo (*account holds*).

---

## 1. State Machine: Status Penahanan Saldo (`AccountHold`)

Setiap penahanan saldo memiliki siklus hidup status sebagai berikut:

```mermaid
stateDiagram-v2
    [*] --> HELD : Hold Created (POST /api/holds)
    
    HELD --> COMMITTED : Commit Hold (POST /api/holds/commit)
    HELD --> RELEASED : Cancel Hold / Rollback
    HELD --> EXPIRED : Expired Time Exceeded (expires_at)

    COMMITTED --> [*]
    RELEASED --> [*]
    EXPIRED --> [*]
```

### Keterangan Status:
* **`HELD`**: Saldo berhasil dikunci pada `hold_balance` akun asal. Total saldo tersedia (*available balance*) berkurang.
* **`COMMITTED`**: Dana penahanan berhasil dipindahkan ke akun tujuan secara eksekusi entri ganda (*double-entry ledger*).
* **`RELEASED`**: Penahanan dibatalkan secara manual; `hold_balance` dikembalikan ke akun asal.
* **`EXPIRED`**: Batas waktu penahanan habis sebelum di-*commit*.

---

## 2. Sequence Diagram: Commit Hold Request (`POST /api/holds/commit`)

```mermaid
sequenceDiagram
    autonumber
    actor Client as Client / API Gateway
    participant Ctrl as HoldController
    participant Svc as HoldService
    participant DB as PostgreSQL DB
    participant Kafka as Kafka Broker

    Client->>Ctrl: POST /api/holds/commit {holdId, destinationAccountId}
    Ctrl->>Svc: commitHold(holdId, destinationAccountId)
    
    rect rgb(230, 245, 255)
        Note over Svc, DB: Transaksi Database (Transactional)
        Svc->>DB: SELECT * FROM account_holds WHERE id = holdId
        DB-->>Svc: Return AccountHold (Status: HELD)
        
        Note over Svc: Hitung Lock Ordering (Min ID, Max ID)
        Svc->>DB: SELECT FOR UPDATE accounts WHERE id = Min(Source, Dest)
        Svc->>DB: SELECT FOR UPDATE accounts WHERE id = Max(Source, Dest)
        
        Svc->>DB: INSERT INTO transactions & ledger_entries
        Svc->>DB: UPDATE accounts (balance & hold_balance)
        Svc->>DB: UPDATE account_holds SET status = 'COMMITTED'
    end

    Svc->>Kafka: Publish Event: TransactionCommittedEvent
    Svc-->>Ctrl: Success Response
    Ctrl-->>Client: HTTP 200 OK { status: "SUCCESS" }
```