-- ============================================================
-- V007: Project Module Extensions — Industrial Supply & EPC Vendor
-- Extends existing projects table dengan kebutuhan EPC/vendor
-- ============================================================

-- Tambah kolom ke tabel projects yang sudah ada
ALTER TABLE projects
    ADD COLUMN IF NOT EXISTS project_type      VARCHAR(30)  DEFAULT 'SUPPLY',
    ADD COLUMN IF NOT EXISTS contract_number   VARCHAR(100),
    ADD COLUMN IF NOT EXISTS po_number_owner   VARCHAR(100),   -- nomor PO dari owner/customer
    ADD COLUMN IF NOT EXISTS site_location     VARCHAR(500),
    ADD COLUMN IF NOT EXISTS retention_pct     DECIMAL(5,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS retention_amount  DECIMAL(19,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS warranty_months   INT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS warranty_end_date DATE,
    ADD COLUMN IF NOT EXISTS project_manager   VARCHAR(200),
    ADD COLUMN IF NOT EXISTS site_manager      VARCHAR(200);

-- Index baru
CREATE INDEX IF NOT EXISTS idx_projects_type     ON projects(project_type);
CREATE INDEX IF NOT EXISTS idx_projects_contract ON projects(contract_number);

-- ============================================================
-- Project Cost Tracking (biaya langsung per proyek)
-- Untuk menghitung real profit vs estimasi
-- ============================================================
CREATE TABLE project_cost_entries (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_project      UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    entry_date      DATE NOT NULL,
    cost_category   VARCHAR(50) NOT NULL,   -- MATERIAL, LABOR, SUBCON, EQUIPMENT, TRANSPORT, OTHER
    description     VARCHAR(500) NOT NULL,
    quantity        DECIMAL(19,4),
    unit            VARCHAR(50),
    unit_cost       DECIMAL(19,2),
    total_cost      DECIMAL(19,2) NOT NULL,
    reference_type  VARCHAR(30),            -- PO, GR, MANUAL
    reference_id    UUID,                   -- id PO atau GR terkait
    notes           TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100),

    CONSTRAINT chk_cost_category CHECK (cost_category IN (
        'MATERIAL','LABOR','SUBCON','EQUIPMENT','TRANSPORT','OVERHEAD','OTHER'
    ))
);

CREATE INDEX idx_pce_project  ON project_cost_entries(id_project);
CREATE INDEX idx_pce_date     ON project_cost_entries(entry_date);
CREATE INDEX idx_pce_category ON project_cost_entries(cost_category);

-- ============================================================
-- Project Revenue Tracking (pendapatan per proyek / termin)
-- ============================================================
CREATE TABLE project_revenue_entries (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_project      UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    entry_date      DATE NOT NULL,
    revenue_type    VARCHAR(50) NOT NULL,   -- MATERIAL, SERVICE, ENGINEERING, RETENTION
    description     VARCHAR(500) NOT NULL,
    amount          DECIMAL(19,2) NOT NULL,
    tax_amount      DECIMAL(19,2) DEFAULT 0,
    reference_type  VARCHAR(30),            -- INVOICE, SO, MANUAL
    reference_id    UUID,
    termin_no       INT,                    -- nomor termin pembayaran
    notes           TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100),

    CONSTRAINT chk_revenue_type CHECK (revenue_type IN (
        'MATERIAL','SERVICE','ENGINEERING','INSTALLATION','RETENTION','OTHER'
    ))
);

CREATE INDEX idx_pre_project ON project_revenue_entries(id_project);
CREATE INDEX idx_pre_date    ON project_revenue_entries(entry_date);

-- ============================================================
-- Project Documents (dokumen proyek)
-- ============================================================
CREATE TABLE project_documents (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_project      UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    doc_type        VARCHAR(50) NOT NULL,   -- RFQ, QUOTATION, PO_OWNER, DO, BAST, MTC, dll
    doc_number      VARCHAR(100),
    doc_date        DATE,
    description     VARCHAR(500),
    file_path       VARCHAR(500),
    uploaded_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    uploaded_by     VARCHAR(100),

    CONSTRAINT chk_doc_type CHECK (doc_type IN (
        'RFQ','QUOTATION','SALES_ORDER','PO_OWNER','DELIVERY_ORDER',
        'BAST','INVOICE','PURCHASE_ORDER','GOODS_RECEIPT',
        'MTC','DRAWING','SPECIFICATION','CONTRACT','OTHER'
    ))
);

CREATE INDEX idx_pd_project  ON project_documents(id_project);
CREATE INDEX idx_pd_doc_type ON project_documents(doc_type);
