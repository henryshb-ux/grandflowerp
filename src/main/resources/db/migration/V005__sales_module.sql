-- ============================================================
-- V005: Sales Module — Industrial Supply & EPC Vendor
-- Modul: RFQ → Quotation → Sales Order → Delivery Order
-- ============================================================

-- Nomor urut untuk dokumen sales
INSERT INTO transaction_sequences (id, sequence_type, prefix, year, last_number)
VALUES
  (gen_random_uuid(), 'RFQ',            'RFQ',  EXTRACT(YEAR FROM NOW())::INT, 0),
  (gen_random_uuid(), 'QUOTATION',      'QT',   EXTRACT(YEAR FROM NOW())::INT, 0),
  (gen_random_uuid(), 'SALES_ORDER',    'SO',   EXTRACT(YEAR FROM NOW())::INT, 0),
  (gen_random_uuid(), 'DELIVERY_ORDER', 'DO',   EXTRACT(YEAR FROM NOW())::INT, 0)
ON CONFLICT (sequence_type, year) DO NOTHING;

-- ============================================================
-- RFQ (Request For Quotation dari Customer)
-- ============================================================
CREATE TABLE rfq (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    row_version     BIGINT NOT NULL DEFAULT 0,
    rfq_number      VARCHAR(50) NOT NULL UNIQUE,
    id_client       UUID NOT NULL REFERENCES clients(id),
    id_project      UUID REFERENCES projects(id),
    rfq_date        DATE NOT NULL,
    response_date   DATE,                          -- deadline balas quotation
    status          VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    subject         VARCHAR(500),
    notes           TEXT,
    attachment_path VARCHAR(500),                  -- path file RFQ asli dari customer
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    deleted_at      TIMESTAMP,

    CONSTRAINT chk_rfq_status CHECK (status IN ('OPEN','QUOTED','CLOSED','CANCELLED'))
);

CREATE INDEX idx_rfq_client   ON rfq(id_client);
CREATE INDEX idx_rfq_status   ON rfq(status);
CREATE INDEX idx_rfq_date     ON rfq(rfq_date);

-- Item-item dalam RFQ
CREATE TABLE rfq_lines (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_rfq         UUID NOT NULL REFERENCES rfq(id) ON DELETE CASCADE,
    line_order     INT NOT NULL DEFAULT 1,
    id_product     UUID REFERENCES products(id),
    description    VARCHAR(1000) NOT NULL,
    quantity       DECIMAL(19,4) NOT NULL,
    unit           VARCHAR(50),
    notes          TEXT,
    created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_rfq_lines_rfq ON rfq_lines(id_rfq);

-- ============================================================
-- Quotation (Penawaran Harga ke Customer)
-- ============================================================
CREATE TABLE quotations (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    row_version         BIGINT NOT NULL DEFAULT 0,
    quotation_number    VARCHAR(50) NOT NULL UNIQUE,
    id_rfq              UUID REFERENCES rfq(id),
    id_client           UUID NOT NULL REFERENCES clients(id),
    id_project          UUID REFERENCES projects(id),
    quotation_date      DATE NOT NULL,
    valid_until         DATE NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    subject             VARCHAR(500),
    payment_terms       VARCHAR(200),              -- misal: NET 30, DP 30% NET 60
    delivery_terms      VARCHAR(200),              -- misal: DDP, FOB Surabaya
    delivery_days       INT,                       -- estimasi hari pengiriman
    currency            VARCHAR(10) NOT NULL DEFAULT 'IDR',
    subtotal            DECIMAL(19,2) NOT NULL DEFAULT 0,
    discount_amount     DECIMAL(19,2) NOT NULL DEFAULT 0,
    tax_amount          DECIMAL(19,2) NOT NULL DEFAULT 0,
    total_amount        DECIMAL(19,2) NOT NULL DEFAULT 0,
    notes               TEXT,
    internal_notes      TEXT,
    sent_at             TIMESTAMP,
    won_at              TIMESTAMP,
    lost_at             TIMESTAMP,
    lost_reason         TEXT,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    deleted_at          TIMESTAMP,

    CONSTRAINT chk_quotation_status CHECK (status IN ('DRAFT','SENT','WON','LOST','CANCELLED'))
);

CREATE INDEX idx_quotation_client  ON quotations(id_client);
CREATE INDEX idx_quotation_status  ON quotations(status);
CREATE INDEX idx_quotation_date    ON quotations(quotation_date);
CREATE INDEX idx_quotation_rfq     ON quotations(id_rfq);

-- Baris item dalam Quotation
CREATE TABLE quotation_lines (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_quotation    UUID NOT NULL REFERENCES quotations(id) ON DELETE CASCADE,
    id_rfq_line     UUID REFERENCES rfq_lines(id),
    id_product      UUID REFERENCES products(id),
    line_order      INT NOT NULL DEFAULT 1,
    description     VARCHAR(1000) NOT NULL,
    quantity        DECIMAL(19,4) NOT NULL,
    unit            VARCHAR(50),
    unit_price      DECIMAL(19,2) NOT NULL DEFAULT 0,
    discount_pct    DECIMAL(5,2) NOT NULL DEFAULT 0,
    subtotal        DECIMAL(19,2) NOT NULL DEFAULT 0,
    tax_pct         DECIMAL(5,2) NOT NULL DEFAULT 11,  -- PPN 11%
    tax_amount      DECIMAL(19,2) NOT NULL DEFAULT 0,
    total           DECIMAL(19,2) NOT NULL DEFAULT 0,
    notes           TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_quotation_lines_quotation ON quotation_lines(id_quotation);

-- ============================================================
-- Sales Order (Konfirmasi Order dari Customer)
-- ============================================================
CREATE TABLE sales_orders (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    row_version         BIGINT NOT NULL DEFAULT 0,
    so_number           VARCHAR(50) NOT NULL UNIQUE,
    id_quotation        UUID REFERENCES quotations(id),
    id_client           UUID NOT NULL REFERENCES clients(id),
    id_project          UUID REFERENCES projects(id),
    po_number_customer  VARCHAR(100),              -- Nomor PO dari customer
    so_date             DATE NOT NULL,
    expected_delivery   DATE,
    status              VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
    payment_terms       VARCHAR(200),
    delivery_address    TEXT,
    subtotal            DECIMAL(19,2) NOT NULL DEFAULT 0,
    discount_amount     DECIMAL(19,2) NOT NULL DEFAULT 0,
    tax_amount          DECIMAL(19,2) NOT NULL DEFAULT 0,
    total_amount        DECIMAL(19,2) NOT NULL DEFAULT 0,
    notes               TEXT,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    deleted_at          TIMESTAMP,

    CONSTRAINT chk_so_status CHECK (status IN ('CONFIRMED','IN_PROGRESS','PARTIALLY_DELIVERED','DELIVERED','INVOICED','CANCELLED'))
);

CREATE INDEX idx_so_client ON sales_orders(id_client);
CREATE INDEX idx_so_status ON sales_orders(status);
CREATE INDEX idx_so_quotation ON sales_orders(id_quotation);

-- Baris item dalam Sales Order
CREATE TABLE sales_order_lines (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_so           UUID NOT NULL REFERENCES sales_orders(id) ON DELETE CASCADE,
    id_quotation_line UUID REFERENCES quotation_lines(id),
    id_product      UUID REFERENCES products(id),
    line_order      INT NOT NULL DEFAULT 1,
    description     VARCHAR(1000) NOT NULL,
    quantity        DECIMAL(19,4) NOT NULL,
    qty_delivered   DECIMAL(19,4) NOT NULL DEFAULT 0,
    unit            VARCHAR(50),
    unit_price      DECIMAL(19,2) NOT NULL DEFAULT 0,
    discount_pct    DECIMAL(5,2) NOT NULL DEFAULT 0,
    subtotal        DECIMAL(19,2) NOT NULL DEFAULT 0,
    tax_pct         DECIMAL(5,2) NOT NULL DEFAULT 11,
    tax_amount      DECIMAL(19,2) NOT NULL DEFAULT 0,
    total           DECIMAL(19,2) NOT NULL DEFAULT 0,
    notes           TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_so_lines_so ON sales_order_lines(id_so);

-- ============================================================
-- Delivery Order (Surat Jalan / Pengiriman Barang)
-- ============================================================
CREATE TABLE delivery_orders (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    row_version     BIGINT NOT NULL DEFAULT 0,
    do_number       VARCHAR(50) NOT NULL UNIQUE,
    id_so           UUID NOT NULL REFERENCES sales_orders(id),
    id_client       UUID NOT NULL REFERENCES clients(id),
    delivery_date   DATE NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    shipped_by      VARCHAR(200),                  -- ekspedisi / kurir
    tracking_no     VARCHAR(100),
    delivery_address TEXT,
    received_by     VARCHAR(200),                  -- nama penerima
    received_at     TIMESTAMP,
    bast_number     VARCHAR(50),                   -- nomor BAST
    bast_signed_at  DATE,                          -- tanggal BAST ditandatangani
    notes           TEXT,
    id_invoice      UUID REFERENCES invoices(id),  -- terhubung ke invoice setelah dibuatkan
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    deleted_at      TIMESTAMP,

    CONSTRAINT chk_do_status CHECK (status IN ('DRAFT','SHIPPED','DELIVERED','BAST_SIGNED','CANCELLED'))
);

CREATE INDEX idx_do_so     ON delivery_orders(id_so);
CREATE INDEX idx_do_client ON delivery_orders(id_client);
CREATE INDEX idx_do_status ON delivery_orders(status);

-- Baris item dalam Delivery Order
CREATE TABLE delivery_order_lines (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_do           UUID NOT NULL REFERENCES delivery_orders(id) ON DELETE CASCADE,
    id_so_line      UUID REFERENCES sales_order_lines(id),
    id_product      UUID REFERENCES products(id),
    line_order      INT NOT NULL DEFAULT 1,
    description     VARCHAR(1000) NOT NULL,
    quantity        DECIMAL(19,4) NOT NULL,
    unit            VARCHAR(50),
    serial_numbers  TEXT,                          -- serial number dipisah koma
    notes           TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_do_lines_do ON delivery_order_lines(id_do);

-- Tambah kolom referensi ke invoices (terhubung ke DO)
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS id_so UUID REFERENCES sales_orders(id);
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS id_do UUID REFERENCES delivery_orders(id);
