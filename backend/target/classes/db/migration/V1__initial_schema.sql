-- V1__initial_schema.sql
-- ShopWave monolith — tek veritabanı, tek transaction boundary.
-- Tüm domain'ler (catalog, inventory, order, audit) aynı şemada.

-- ─────────────────────────────────────────────
-- CATALOG (ürün kataloğu)
-- ─────────────────────────────────────────────
CREATE TABLE categories (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE products (
    id           BIGSERIAL    PRIMARY KEY,
    sku          VARCHAR(50)  NOT NULL UNIQUE,
    name         VARCHAR(200) NOT NULL,
    description  TEXT,
    price        NUMERIC(12,2) NOT NULL,
    category_id  BIGINT       REFERENCES categories(id),
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_price CHECK (price >= 0)
);

-- ─────────────────────────────────────────────
-- INVENTORY (stok yönetimi)
-- Monolith'te catalog ile aynı DB'de.
-- İlerideki lab'da bu tablo ayrı bir servisin DB'sine taşınacak.
-- ─────────────────────────────────────────────
CREATE TABLE inventory (
    id           BIGSERIAL PRIMARY KEY,
    product_id   BIGINT    NOT NULL UNIQUE REFERENCES products(id),
    quantity     INT       NOT NULL DEFAULT 0,
    reserved     INT       NOT NULL DEFAULT 0,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_quantity CHECK (quantity >= 0),
    CONSTRAINT chk_reserved CHECK (reserved >= 0),
    CONSTRAINT chk_reserved_lte_quantity CHECK (reserved <= quantity)
);

-- ─────────────────────────────────────────────
-- CUSTOMERS
-- ─────────────────────────────────────────────
CREATE TABLE customers (
    id           BIGSERIAL    PRIMARY KEY,
    email        VARCHAR(150) NOT NULL UNIQUE,
    full_name    VARCHAR(100) NOT NULL,
    phone        VARCHAR(20),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ─────────────────────────────────────────────
-- ORDERS
-- ─────────────────────────────────────────────
CREATE TABLE orders (
    id              BIGSERIAL    PRIMARY KEY,
    order_ref       VARCHAR(30)  NOT NULL UNIQUE,
    customer_id     BIGINT       NOT NULL REFERENCES customers(id),
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    total_amount    NUMERIC(12,2) NOT NULL DEFAULT 0,
    shipping_address TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_order_status CHECK (
        status IN ('PENDING','CONFIRMED','SHIPPED','DELIVERED','CANCELLED')
    )
);

CREATE TABLE order_items (
    id           BIGSERIAL     PRIMARY KEY,
    order_id     BIGINT        NOT NULL REFERENCES orders(id),
    product_id   BIGINT        NOT NULL REFERENCES products(id),
    quantity     INT           NOT NULL,
    unit_price   NUMERIC(12,2) NOT NULL,
    CONSTRAINT chk_item_qty   CHECK (quantity > 0),
    CONSTRAINT chk_item_price CHECK (unit_price >= 0)
);

-- ─────────────────────────────────────────────
-- AUDIT LOG
-- Monolith'te her önemli işlem aynı transaction içinde yazılır.
-- Dağıtık mimaride bu garanti kaybolur (Lab konusu: Outbox Pattern).
-- ─────────────────────────────────────────────
CREATE TABLE audit_logs (
    id           BIGSERIAL   PRIMARY KEY,
    event_type   VARCHAR(60) NOT NULL,
    aggregate    VARCHAR(30),
    aggregate_id BIGINT,
    payload      TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ─────────────────────────────────────────────
-- SEED DATA
-- ─────────────────────────────────────────────
INSERT INTO categories (name, description) VALUES
    ('Electronics',  'Phones, laptops and gadgets'),
    ('Clothing',     'Apparel and accessories'),
    ('Books',        'Physical and digital books'),
    ('Home & Garden','Furniture and garden tools'),
    ('Sports',       'Sports and outdoor equipment');

INSERT INTO products (sku, name, description, price, category_id) VALUES
    ('ELEC-001', 'Wireless Headphones Pro',  'Noise-cancelling over-ear headphones', 129.99, 1),
    ('ELEC-002', 'USB-C Hub 7-in-1',         '4K HDMI, 100W PD, 3x USB-A',          49.99,  1),
    ('ELEC-003', 'Mechanical Keyboard TKL',  'RGB, hot-swap switches',               89.99,  1),
    ('CLTH-001', 'Classic Denim Jacket',     'Slim-fit, indigo wash',                74.99,  2),
    ('CLTH-002', 'Running Shorts',           'Lightweight, moisture-wicking',        29.99,  2),
    ('BOOK-001', 'Designing Data-Intensive Applications', 'Martin Kleppmann', 49.99, 3),
    ('BOOK-002', 'Clean Code',               'Robert C. Martin',                     34.99,  3),
    ('HOME-001', 'Standing Desk Converter',  'Height-adjustable, 60cm wide',         149.99, 4),
    ('SPRT-001', 'Yoga Mat Premium',         '6mm thick, non-slip',                  39.99,  5),
    ('SPRT-002', 'Resistance Band Set',      '5 levels, latex-free',                 24.99,  5);

INSERT INTO inventory (product_id, quantity, reserved) VALUES
    (1, 50,  0),
    (2, 200, 0),
    (3, 75,  0),
    (4, 40,  0),
    (5, 120, 0),
    (6, 30,  0),
    (7, 30,  0),
    (8, 15,  0),
    (9, 80,  0),
    (10,100, 0);

INSERT INTO customers (email, full_name, phone) VALUES
    ('alice@example.com',   'Alice Johnson',  '+90-532-111-0001'),
    ('bob@example.com',     'Bob Smith',      '+90-532-111-0002'),
    ('charlie@example.com', 'Charlie Brown',  '+90-532-111-0003'),
    ('diana@example.com',   'Diana Prince',   '+90-532-111-0004'),
    ('eve@example.com',     'Eve Turner',     '+90-532-111-0005');
