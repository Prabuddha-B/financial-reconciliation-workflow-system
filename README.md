# Nikhila Eranda — Developer Branch

## Overview
This branch contains my individual contribution to the **Financial Reconciliation Workflow System (FRWSS)**.  
My primary focus was improving the **Manual Data Entry (Ingestion UI)** flow, including UI behavior, navigation, and template/controller alignment to support adding and updating records across multiple dataset types.

---

## Frontend Contributions (Manual Entry UI)
- Implemented **Add New** and **Update** mode switching in the manual entry screen
- Added JavaScript to **toggle the correct sections/fields** based on the selected **Data Type** (Receipt / Payroll / Stock Purchase / Accounting)
- Added and wired **Search record to update** panel visibility based on **Update mode**
- Improved user flow with **scroll-to-relevant-section** behavior when switching modes
- Updated result page buttons (**Add Another / Update Another**) to navigate back to the manual entry route

---

## Backend / Controller Contributions
- Aligned Spring MVC routing and view returns for:
  - `GET /manual` → renders the manual entry page
  - `POST /manual/update/search` → loads an existing record for update and renders the manual entry page
  - `POST /manual/add` and `POST /manual/update` → renders the result page
- Ensured model attributes required by the UI flow are set consistently:
  - `dataType`, `mode`, `record`, `updateReady`, `successMessage`, `errorMessage`

---

## Notes
- This README documents **my own work only**.
- Core ingestion (CSV/XLSX processing) and other modules were handled by other developers.
- If Spring Security CSRF is enabled, POST forms may require adding CSRF tokens in the templates.
