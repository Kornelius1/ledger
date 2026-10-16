# API Specification: Ledger Service

Dokumen ini memuat spesifikasi teknis antarmuka REST API untuk penanganan transaksi penahanan saldo.

---

## 1. Commit Hold

Mengonfirmasi penahanan saldo yang ada dan memindahkan dana dari akun asal ke akun tujuan.

* **URL Path**: `/api/holds/commit`
* **HTTP Method**: `POST`
* **Content-Type**: `application/json`

### Request Body

```json
{
  "holdId": "11111111-1111-1111-1111-111111111111",
  "destinationAccountId": 1002
}
```

#### Deskripsi Field Request

| Field | Tipe Data | Wajib | Keterangan |
| :--- | :--- | :--- | :--- |
| `holdId` | `String (UUID)` | Ya | Identifier unik dari baris `account_holds` yang berstatus `HELD`. |
| `destinationAccountId` | `Long` | Ya | ID akun penerima transfer dana. |

---

### Response Body

#### 1. Success (HTTP 200 OK)

```json
{
  "status": "SUCCESS",
  "message": "Hold committed successfully"
}
```

#### 2. Bad Request (HTTP 400 Bad Request)

Dikembalikan jika format JSON tidak valid atau header `Content-Type` salah.

```json
{
  "timestamp": "2026-10-16T10:45:47.081Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid mime type \"application\\json\": does not contain '/'"
}
```

#### 3. Internal Server Error (HTTP 500 Internal Server Error)

Dikembalikan jika terjadi kesalahan internal di sisi server atau kendala konektivitas database/Kafka.

```json
{
  "timestamp": "2026-10-16T10:28:31.243Z",
  "status": 500,
  "error": "Internal Server Error",
  "message": "Terjadi kesalahan internal pada server"
}
```