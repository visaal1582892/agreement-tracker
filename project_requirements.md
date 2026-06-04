# Product Requirements Document: Commercial Agreement Tracker

## 1\. Project Overview

- **Objective:** Build a centralized, standalone application to track all pharma and non-pharma commercial agreements.
- **Problem to Solve:** Eliminate the manual, fragmented process of individual account managers holding onto their own commercial documents.
- **Deployment Strategy:** Initially launched as a standalone application with dedicated screens, laying the architectural groundwork for future integrations with external inventory and finance systems (Phase 2).

## 2\. Core Architecture: Agreement Group & Versioning

The system utilizes a top-down hierarchical architecture to manage commercial relationships and versioning, anchored by a centralized Company Master.

**Hierarchy Model:**

Company -> Agreement Group -> Agreement Version 1 -> Agreement Version 2

- **Company:** A centralized registry of all participating business entities.
- **Agreement Group:** The logical container visible to users, identified by a business-friendly Agreement Number (e.g., "AGR-2026-0001") mapped to a single Company.
- **Agreement Version:** The immutable snapshot of the commercial arrangement at a specific point in time.
- **Explicit Version Creation Workflow:**
  - User clicks "Edit" on the Current Version.
  - System creates a New Draft Version (e.g., V3) and copies all data.
  - User modifies the Draft.
  - User Submits for approval.
  - Approver Approves the version.
  - System updates current_version_id on the Group to point to V3.
- **Rejected Versions:** If a version (e.g., V2) is rejected during the approval workflow, it **cannot** be edited. The user must create a new version (V3) to submit corrections.
- **Immutability:** Old versions remain completely immutable.

## 3\. UI/UX Workflow: Vendor & Product Selection

When drafting an Agreement Version, the system guides the user through a specific, sequential selection process.

### Step 1: Company Selection

- **Action:** User searches for and selects a single **Company** from the CompanyMaster.

### Step 2: Vendor Selection

- **Action:** User selects multiple vendors associated with the agreement.
- **Features:**
  - Search and select via Vendor IDs or Vendor Names.
  - **Bulk Paste:** Users can paste a comma-separated list of Vendor IDs. The system will parse the input and automatically select/display the corresponding vendors.

### Step 3: Product Mapping (Dynamic Cascading UI)

Once vendors are selected, the system fetches all products mapped to those specific vendors from the external Product Master (UNS). The user then filters this list (as referenced in image_3f62aa.png) to pinpoint the exact products applicable to the agreement.

- **Filter Level 1: Manufacturer:** Dropdown to select a specific Manufacturer (e.g., Pfizer).
- **Filter Level 2: Divisions:**
  - Checkbox list of divisions belonging to the selected Manufacturer (e.g., Oncology, Vaccines, Internal Medicine).
  - **Operator:** An "Include Only" or "Exclude" dropdown determines whether the checked divisions are the _only_ ones applied, or the ones specifically _removed_ from the product pool.
- **Filter Level 3: Specific Products:**
  - Checkbox list of individual products, grouped by Division (e.g., Ibrance, Xtandi under Oncology).
  - **Operator:** An "Include Only" or "Exclude" dropdown provides granular control over which specific products within the filtered divisions are attached to the agreement.

## 4\. User Roles, Access Control & Security (CAS Ready)

The system enforces strict Role-Based Access Control (RBAC) to ensure data privacy. The security schema is explicitly designed to be Central Authentication Service (CAS) ready.

### 4.1 Core Roles & Dashboard Views

- **Account Managers:** Limited to specific assigned agreements and companies/vendors.
- **Leadership / Finance:** Full global visibility across all system records.
- **System Admin:** Full technical and data access, including Configurator management and historical data access.
- **Approver:** Access to review, approve, or reject pending agreements.

### 4.2 Security Schema (Database Entities)

- **User:** id (PK), employee_id, username, email, full_name, is_active, created_by, created_at, updated_at.
- **Role:** id (PK), name (e.g., ADMIN, ACCOUNT_MANAGER), description, is_active, created_by_user_id, created_at, updated_by_user_id, updated_at.
- **Right:** id (PK), code (e.g., CREATE_AGREEMENT), name, module, description, is_active, created_by_user_id, created_at, updated_by_user_id, updated_at.
- **UserRole:** id (PK), user_id, role_id, created_by_user_id, created_at, updated_by_user_id, updated_at.
- **RoleRight:** id (PK), role_id, right_id, created_by_user_id, created_at, updated_by_user_id, updated_at.
- **UserCompanyAssignment:** id (PK), user_id, company_id, assigned_at, created_by_user_id, created_at, updated_by_user_id, updated_at.

## 5\. Application Database Schema

This section outlines the streamlined, production-ready table structures. Optimistic locking is enforced via updated_at validation during saves.

### 5.1 Company & Group Core

**1\. CompanyMaster** (The centralized registry)

- id (PK)
- company_name
- is_active
- created_by_user_id, created_at, updated_by_user_id, updated_at

**2\. AgreementGroup** (The logical container)

- id (PK)
- company_id (FK -> CompanyMaster.id)
- agreement*number (VARCHAR(50) UNIQUE NOT NULL) *(e.g., "AGR-2026-0001")\_
- agreement_name
- current*version_id (FK -> Agreement.id) *(Single source of truth for the active version)\_
- is_active
- created_by_user_id, created_at, updated_by_user_id, updated_at

### 5.2 Agreement Version Core

**3\. Agreement** (The specific commercial snapshot)

- id (PK)
- agreement_group_id (FK)
- version_number
- owner_user_id
- income_type_id
- agreement_type_id
- commercial_structure (Enum: FLAT, SLAB)
- commercial_value
- calculation_formula (Nullable)
- start_date
- expiry*date *(Validation: Must be >= start*date)*
- approval_status (Enum: DRAFT, PENDING_APPROVAL, APPROVED, REJECTED)
- in*progress_flag (Boolean) *(Default: False)\_
- termination_reason
- termination_date
- approved_by_user_id, approval_date
- created_by_user_id, created_at, updated_by_user_id, updated_at
- **Constraint:** UNIQUE(agreement_group_id, version_number)

_Note on Status: Agreement Status is derived dynamically._

- IF termination_date is not null -> TERMINATED
- ELSE IF in_progress_flag is TRUE -> IN_PROGRESS
- ELSE IF expiry_date &lt; today -&gt; EXPIRED
- ELSE -> ACTIVE

### 5.3 Details, Products & Documents

**4\. AgreementVendor**

- id (PK), agreement_id (FK), vendor_id, vendor_name_snapshot
- created_by_user_id, created_at, updated_by_user_id, updated_at

**5\. AgreementProduct**

- id (PK), agreement_id (FK), product_id, manufacturer_id, division_id, product_name_snapshot, manufacturer_name_snapshot, division_name_snapshot
- created_by_user_id, created_at, updated_by_user_id, updated_at

**6\. AgreementSlab** (The Target Tiers)

- id (PK), agreement_id (FK), slab_name (e.g., "1000 - 5000"), from_value, to_value
- created_by_user_id, created_at, updated_by_user_id, updated_at

**7\. AgreementTimePeriod** (The Date Buckets)

- id (PK), agreement_id (FK), period_name (e.g., "Q1-2026"), start_date, end_date
- created_by_user_id, created_at, updated_by_user_id, updated_at

**8\. SlabPeriodValue** (The Mapping Table populated via Excel)

- id (PK), agreement_slab_id (FK), agreement_time_period_id (FK), commercial_value
- created_by_user_id, created_at, updated_by_user_id, updated_at

**9\. AgreementDocument**

- id (PK), agreement_id (FK), document_type (Enum: AGREEMENT, TERMINATION, SUPPORTING_DOC, EMAIL, OTHER), file_name, file_path
- created_by_user_id, created_at, updated_by_user_id, updated_at

### 5.4 Configuration & Lookups

**10\. IncomeType**

- id (PK), name, description, is_active
- created_by_user_id, created_at, updated_by_user_id, updated_at

**11\. AgreementType**

- id (PK), name, is_active
- created_by_user_id, created_at, updated_by_user_id, updated_at

### 5.5 Workflows, Audits & Notifications

**12\. AgreementApproval** (Timeline & Remarks History)

- id (PK), agreement_id (FK), action (Enum: SUBMITTED, APPROVED, REJECTED), remarks, approval_status_before, approval_status_after
- created_by_user_id, created_at, updated_by_user_id, updated_at

**13\. AgreementAudit**

- id (PK), agreement_group_id, agreement_id, entity_type, entity_id, action, old_value_json, new_value_json
- created*by_user_id, created_at *(Append only log, no updated*at needed)*

**14\. AgreementReminder**

- id (PK), agreement_id (FK), reminder_type (Enum: D_90, D_60, D_30, EXPIRED), sent_to_user_id, sent_at
- created_by_user_id, created_at, updated_by_user_id, updated_at

## 6\. System Logic & Constraints

### 6.1 Reminder Workflow & "IN PROGRESS" Status

The system sends automated alerts 90, 60, and 30 days prior to the expiry_date, and continuously sends alerts once expired.

- **The "IN PROGRESS" Override:** If discussions regarding a renewal or termination are actively occurring, the Account Manager can toggle the in_progress_flag to TRUE.
- **Effect:** This immediately pauses all automated reminders for that specific agreement, preventing notification fatigue. The agreement status is visually flagged as IN_PROGRESS on the UI.

### 6.2 Validation Rule: Product Mappings (CRITICAL)

Before any agreement can be created or approved, the system enforces a strict validation rule to prevent duplicate commercial setups. Drafts and Rejected versions are ignored during this check.

**BLOCK Creation/Approval IF:** Target is an APPROVED version AND NOT TERMINATED AND:

- Same Product AND
- Same Income Type AND
- Date Ranges Overlap

_Example:_ If "Crocin" has an APPROVED, non-terminated "Data Fee" active from Jan-Dec, a new agreement for "Crocin" with a "Data Fee" from Mar-Nov must be blocked.

### 6.3 Ownership Transfer Service

- A transfer updates the owner_user_id on the version directly. It **DOES NOT** create a new version number.
- Only the **current active version** is updated during a transfer. Historical versions retain their original owner records.

## 7\. UI/UX Workflows & Enhancements

- **User Visibility:** Users only see the logical agreement_number (e.g., "AGR-2026-0001"), the current active version, and an option to view historical versions. They never see database IDs.
- **Continuous Editing:** Users can update fields for Groups with a renewed/revised copy of the agreement. This initiates a new Version in a DRAFT state.
- **Bi-directional Dates:** Entering Start Date and Tenure calculates Expiry Date, and vice versa.

## 8\. Phase 2 Preparations

- **MinMax Exceptions:** Integration with Inventory systems.
- **Automated Finance Calculations:** Integration with GRN to calculate payouts based on the simplified Agreement structure.