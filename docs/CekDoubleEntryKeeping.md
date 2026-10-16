## Cara 1: Menampilkan Riwayat Entri Berdasarkan ID Akun Terkait
Jika Anda ingin melihat seluruh entri ledger yang melibatkan kedua akun tersebut (baik sebagai pengirim maupun penerima), jalankan query berikut:
```
docker exec -it ledger-postgres-ledger-1 psql -U ledger_admin -d db_ledger -c "
SELECT 
    transaction_id,
    account_id,
    entry_type,
    amount,
    created_at
FROM ledger_entries
WHERE account_id IN (1001, 1002)
ORDER BY created_at DESC, transaction_id;
"
```
## Cara 2: Menampilkan Pasangan Transaksi Langsung (Source $\rightarrow$ Destination)
Jika Anda ingin melihat format laporan mutasi berpasangan secara langsung (menampilkan baris akun sumber yang mendebit/mengkredit dan akun tujuannya dalam satu baris transaksi), gunakan query Self-Join berikut:
```
docker exec -it ledger-postgres-ledger-1 psql -U ledger_admin -d db_ledger -c "
SELECT 
    d.transaction_id,
    d.account_id AS debit_account,
    c.account_id AS credit_account,
    d.amount,
    d.created_at
FROM ledger_entries d
JOIN ledger_entries c ON d.transaction_id = c.transaction_id
WHERE d.entry_type = 'DEBIT' 
  AND c.entry_type = 'CREDIT'
  AND (d.account_id IN (1001, 1002) OR c.account_id IN (1001, 1002))
ORDER BY d.created_at DESC;
"
```



### Penjelasan Hasil:
- `transaction_id`: Menunjukkan ID unik yang mengikat kedua entri keuangan tersebut agar tetap dalam satu kesatuan transaksi.

- `entry_type`: Menunjukkan apakah akun tersebut bertindak sebagai DEBIT (biasanya saldo berkurang/penerima dana tergantung desain sistem) atau CREDIT (saldo bertambah/sumber dana).

- `amount`: Nominal uang yang dipindahkan, yang dipastikan selalu memiliki nilai yang sama besar pada kedua sisi entri.