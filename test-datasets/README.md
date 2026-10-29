# FRWSS Test Datasets

## Purpose

This directory contains sample datasets for validating the Financial Reconciliation and Workflow Support System (FRWSS). The files are intended for academic development, demonstration, and manual testing of data ingestion, validation, duplicate detection, and reconciliation.

All datasets should contain synthetic or anonymized data. Do not use real financial records or personal information.

## Dataset Inventory

| File | Data Type | Records | Intended Use |
|---|---|---:|---|
| `Payroll Dataset.csv` | Payroll | 1,000 | Test payroll data ingestion and validation. |
| `Accounting Dataset.csv` | Accounting records | 1,000 | Test accounting data ingestion and reconciliation. |
| `Stock_purchases Dataset.csv` | Stock purchases | 1,000 | Test stock-purchase ingestion and reconciliation. |
| `Receipts_Valid Dataset.csv` | Receipts | 1,000 | Test ingestion and validation of receipt records with valid-format data. |
| `Receipts_Invalid Dataset.csv` | Receipts | 7 | Test validation of missing reference numbers and negative amounts, along with other invalid receipt records. |

## Recommended Testing Sequence

1. Upload the payroll, receipt, and stock-purchase datasets through the ingestion interface.
2. Upload the accounting records dataset.
3. Review the ingestion preview and validation results before saving.
4. Execute the supported reconciliation operations.
5. Inspect exact matches, partial matches, unmatched records, and bulk-matching outcomes where applicable.
6. Upload the validation dataset separately to inspect invalid-row handling and duplicate detection.

The actual results depend on the contents of the datasets and the current database state. Reset or prepare the test database between runs when necessary to avoid existing records affecting duplicate detection.

## Usage Notes

- Use the application's ingestion interface and follow its validation and save workflow.
- Confirm that column headers and date formats are accepted by the current implementation.
- Do not assume that every row will be accepted; some datasets are intentionally designed to exercise validation failures.
- Record actual outcomes when using these files for test evidence.
- Do not upload these datasets to a production financial environment.

## Scope

These datasets support testing of FRWSS reconciliation and workflow functionality. They do not initiate real payments, transfers, or other financial transactions.
