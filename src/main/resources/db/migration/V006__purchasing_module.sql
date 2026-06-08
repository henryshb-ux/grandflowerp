-- ============================================================
-- V006: Purchasing Module — Industrial Supply & EPC Vendor
-- FIX: Hapus FK ke bills(id) di goods_receipts
--      (bills sudah ada di V002, tapi FK cross-version bisa
--       menyebabkan masalah jika urutan migration berubah.
--       Relasi ke bill ditangani di application layer saja.)
-- ============================================================
 
-- Nomor urut dokumen purchasing
INSERT INTO transaction_sequences (id, sequence_type, prefix, year, last_number)
VALUES
  (gen_random_uuid(), 'PURCHASE_REQUEST', 'PR',  EXTRACT(YEAR FROM NOW())::INT, 0),
  (gen_random_uuid(), 'PURCHASE_ORDER',   'PO',  EXTRACT(YEAR FROM NOW())::INT, 0),
  (gen_random_uuid(), 'GOODS_RECEIPT',    'GR',  EXTRACT(YEAR FROM NOW())::INT, 0)
ON CONFLICT (sequence_type, year) DO NOTHING;
 
-- ============================================================
-- PURCHASE REQUEST
-- ============================================================
CREATE TABLE purchase_requests (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    row_version     BIGINT NOT NULL DEFAULT 0,
    pr_number       VARCHAR(50)  NOT NULL UNIQUE,
    id_project      UUID REFERENCES projects(id),
    id_so           UUID,
    request_date    DATE         NOT NULL,
    required_date   DATE,
    status          VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    priority        VARCHAR(20)  NOT NULL DEFAULT 'NORMAL',
    requested_by    VARCHAR(200),
    approved_by     VARCHAR(200),
    approved_at     TIMESTAMP,
    subject         VARCHAR(500),
    notes           TEXT,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    deleted_at      TIMESTAMP,
 
    CONSTRAINT chk_pr_status   CHECK (status IN ('DRAFT','SUBMITTED','APPROVED','REJECTED','ORDERED','CLOSED','CANCELLED')),
    CONSTRAINT chk_pr_priority CHECK (priority IN ('LOW','NORMAL','HIGH','URGENT'))
);
 
CREATE INDEX idx_pr_status   ON purchase_requests(status);
CREATE INDEX idx_pr_date     ON purchase_requests(request_date);
CREATE INDEX idx_pr_project  ON purchase_requests(id_project);
 
CREATE TABLE purchase_request_lines (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_pr           UUID NOT NULL REFERENCES purchase_requests(id) ON DELETE CASCADE,
    id_product      UUID REFERENCES products(id),
    id_so_line      UUID,
    line_order      INT  NOT NULL DEFAULT 1,
    description     VARCHAR(1000) NOT NULL,
    quantity        DECIMAL(19,4) NOT NULL,
    unit            VARCHAR(50),
    estimated_price DECIMAL(19,2),
    notes           TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
 
CREATE INDEX idx_pr_lines_pr ON purchase_request_lines(id_pr);
 
-- ============================================================
-- PURCHASE ORDER
-- ============================================================
CREATE TABLE purchase_orders (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    row_version         BIGINT NOT NULL DEFAULT 0,
    po_number           VARCHAR(50)  NOT NULL UNIQUE,
    id_vendor           UUID NOT NULL REFERENCES vendors(id),
    id_pr               UUID REFERENCES purchase_requests(id),
    id_project          UUID REFERENCES projects(id),
    po_date             DATE NOT NULL,
    expected_delivery   DATE,
    status              VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    payment_terms       VARCHAR(200),
    delivery_address    TEXT,
    currency            VARCHAR(10) NOT NULL DEFAULT 'IDR',
    subtotal            DECIMAL(19,2) NOT NULL DEFAULT 0,
    discount_amount     DECIMAL(19,2) NOT NULL DEFAULT 0,
    tax_amount          DECIMAL(19,2) NOT NULL DEFAULT 0,
    total_amount        DECIMAL(19,2) NOT NULL DEFAULT 0,
    notes               TEXT,
    vendor_ref          VARCHAR(100),
    approved_by         VARCHAR(200),
    approved_at         TIMESTAMP,
    sent_at             TIMESTAMP,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    deleted_at          TIMESTAMP,
 
    CONSTRAINT chk_po_status CHECK (status IN ('DRAFT','SENT','CONFIRMED','PARTIALLY_RECEIVED','RECEIVED','BILLED','CANCELLED'))
);
 
CREATE INDEX idx_po_vendor  ON purchase_orders(id_vendor);
CREATE INDEX idx_po_status  ON purchase_orders(status);
CREATE INDEX idx_po_date    ON purchase_orders(po_date);
CREATE INDEX idx_po_pr      ON purchase_orders(id_pr);
 
CREATE TABLE purchase_order_lines (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_po           UUID NOT NULL REFERENCES purchase_orders(id) ON DELETE CASCADE,
    id_pr_line      UUID REFERENCES purchase_request_lines(id),
    id_product      UUID REFERENCES products(id),
    line_order      INT  NOT NULL DEFAULT 1,
    description     VARCHAR(1000) NOT NULL,
    quantity        DECIMAL(19,4) NOT NULL,
    qty_received    DECIMAL(19,4) NOT NULL DEFAULT 0,
    unit            VARCHAR(50),
    unit_price      DECIMAL(19,2) NOT NULL DEFAULT 0,
    discount_pct    DECIMAL(5,2)  NOT NULL DEFAULT 0,
    subtotal        DECIMAL(19,2) NOT NULL DEFAULT 0,
    tax_pct         DECIMAL(5,2)  NOT NULL DEFAULT 11,
    tax_amount      DECIMAL(19,2) NOT NULL DEFAULT 0,
    total           DECIMAL(19,2) NOT NULL DEFAULT 0,
    notes           TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
 
CREATE INDEX idx_po_lines_po ON purchase_order_lines(id_po);
 
-- ============================================================
-- GOODS RECEIPT
-- FIX: Tidak ada FK ke bills(id) — relasi ditangani di app layer
-- ============================================================
CREATE TABLE goods_receipts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    row_version     BIGINT NOT NULL DEFAULT 0,
    gr_number       VARCHAR(50)  NOT NULL UNIQUE,
    id_po           UUID NOT NULL REFERENCES purchase_orders(id),
    id_vendor       UUID NOT NULL REFERENCES vendors(id),
    id_bill         UUID,            -- ← NO FK CONSTRAINT, app layer only
    receipt_date    DATE NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    received_by     VARCHAR(200),
    delivery_note   VARCHAR(100),
    notes           TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    deleted_at      TIMESTAMP,
 
    CONSTRAINT chk_gr_status CHECK (status IN ('DRAFT','CONFIRMED','BILLED','CANCELLED'))
);
 
CREATE INDEX idx_gr_po     ON goods_receipts(id_po);
CREATE INDEX idx_gr_vendor ON goods_receipts(id_vendor);
CREATE INDEX idx_gr_status ON goods_receipts(status);
 
CREATE TABLE goods_receipt_lines (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_gr          UUID NOT NULL REFERENCES goods_receipts(id) ON DELETE CASCADE,
    id_po_line     UUID REFERENCES purchase_order_lines(id),
    id_product     UUID REFERENCES products(id),
    line_order     INT  NOT NULL DEFAULT 1,
    description    VARCHAR(1000) NOT NULL,
    quantity       DECIMAL(19,4) NOT NULL,
    unit           VARCHAR(50),
    unit_price     DECIMAL(19,2),
    serial_numbers TEXT,
    notes          TEXT,
    created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP NOT NULL DEFAULT NOW()
);
 
CREATE INDEX idx_gr_lines_gr ON goods_receipt_lines(id_gr);
 
-- Tambah referensi ke bills (tanpa FK constraint)
ALTER TABLE bills ADD COLUMN IF NOT EXISTS id_po UUID;
ALTER TABLE bills ADD COLUMN IF NOT EXISTS id_gr UUID;