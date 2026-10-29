# FRWSS test data

Use on an **empty database** so the expected numbers below hold.

| File | Upload as (Data Type) | Rows |
|---|---|---|
| `payroll.csv` | PAYROLL | 29 |
| `receipts.csv` | RECEIPT | 35 |
| `stock_purchases.csv` | STOCK | 18 |
| `accounting_records.csv` | ACCOUNTING | 64 |
| `accounting_invalid_and_duplicates.csv` | ACCOUNTING | 9 (validation test) |

## Step 1: Ingestion

1. Go to Ingestion > Upload, pick the data type, upload the file, check the preview, then click Save.
2. Upload order: payroll, receipts, stock, **then** accounting_records.
3. Every file should show **0 invalid** and save every row.
4. Re-upload any file: every row should be skipped as a duplicate.

### Validation test (`accounting_invalid_and_duplicates.csv`)
Upload it **after** `accounting_records.csv`. Expected preview: 9 rows, with these outcomes:

| Row | Problem | Expected |
|---|---|---|
| ACC-V002 | empty reference_no | Missing reference |
| ACC-V003 | amount -120.00 | Invalid amount |
| ACC-V004 | date 2026-13-45 | Invalid date |
| ACC-V005 | amount `abc` | Parsing error |
| ACC-V001 (twice) | repeated in file | saved once, 1 duplicate skipped |
| ACC-0008 | already in DB | skipped as duplicate on save |
| ACC-V006, ACC-V007 | `15/10/2026` date, `1,250.00` amount | valid |

Expected save message: 3 saved (V001, V006, V007), 2 duplicates skipped, 4 invalid skipped.

## Step 2: Reconciliation

Each run only processes the **10 newest** unreconciled/orphan records, so click the button several times.

**Order matters:**

1. **One-to-many first** (tolerance 10%), before any exact run. The bulk records are only detected while they have no exact-run result.
2. **Exact**: click about 7 times until nothing is left.
3. **Partial** (tolerance 3 days): click about 3 times.

### Expected results

| Check | Expected |
|---|---|
| Exact | 49 (16 payroll, 14 receipt, 10 stock, 9 bulk children matched to receipts) |
| Partial | 8, with confidence 90 (1 day), 80 (2 days), 70 (3 days) |
| Orphan | 7: `RC-2026-FAR1` (6 days off, outside tolerance), `ORP-2026-001/002/003`, and the 3 bulk parent records |
| Bulk A01 (`PO-2026-A01-BULK`) | EXACT, 100% (1000 = 400+350+250) |
| Bulk B01 (`PO-2026-B01-BULK`) | PARTIAL, 85% (2000 vs 1900, 5% diff) |
| Bulk C01 (`PO-2026-C01-BULK`) | ORPHAN (3000 vs 1500, 50% diff) |

## Step 3: Rest of the system

- **Manual resolution:** open an orphan (e.g. `ORP-2026-001`), resolve it with an existing receipt ID such as `RCT-0001`, send it to workflow review, then approve or reject it as a different user.
- **Audit log:** every run and resolution should appear. Use "verify chain" afterwards; it should report valid.
- **Messaging/notifications:** send a message between two users, check the unread count and notification bell.
- **Payments:** process a payment for a record such as `ACC-0008` with amount `114893.74`. The simulated gateway fails about 5% of the time, so retry on failure. Then run payment reconciliation. The amount must equal the accounting record's amount to match.
- **Tax estimation / manual entry:** not covered by these files.
