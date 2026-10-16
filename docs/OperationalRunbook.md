# Operational Runbook & Troubleshooting

Dokumen ini berisi panduan diagnostik dan pemulihan cepat untuk tim Operasional/DevOps jika terjadi masalah pada `ledger-service`.

---

## 1. Kendala: `HttpMediaTypeNotSupportedException` (HTTP 415/500)

### Gejala
Response API mengembalikan status error, dan log Spring Boot menampilkan:
`Invalid mime type "application\json": does not contain '/'`

### Penyebab
Format header `Content-Type` yang dikirim oleh klien atau konfigurasi JMeter menggunakan penulisan *backslash* (`application\json`).

### Solusi Pemulihan
Ubah konfigurasi *Header Manager* atau *HTTP Client* agar menggunakan *forward slash* yang baku: `application/json`.

---

## 2. Kendala: Kafka Producer Disconnected / Timeout

### Gejala
Aplikasi tertahan (*hanging*) saat eksekusi `commitHold`, kemudian melempar `TimeoutException` dan log menampilkan:
`[Producer clientId=producer-1] Node -1 disconnected.`

### Solusi Pemulihan
1. Periksa status *container* Kafka:
   ```bash
   docker ps | grep kafka
   ```
2. Jika *container* tidak *healthy*, lakukan *restart*:
   ```bash
   docker restart ledger-kafka-1
   ```
3. Pastikan port `9092` dapat diakses dari host tempat `ledger-service` berjalan.

---

## 3. Kendala: Invalid Null Value on Constraint `created_at`

### Gejala
Perintah SQL `INSERT` gagal pada tabel `account_holds` dengan error:
`null value in column "created_at" of relation "account_holds" violates not-null constraint`

### Solusi Pemulihan
Pastikan skrip SQL atau penulisan entity JPA menyertakan nilai `NOW()` atau `@CreationTimestamp` pada kolom `created_at`:

```sql
INSERT INTO account_holds (id, account_id, amount, created_at, expires_at, status) 
VALUES ('11111111-1111-1111-1111-111111111111', 1001, 300.00, NOW(), NOW() + INTERVAL '10 minutes', 'HELD');
```