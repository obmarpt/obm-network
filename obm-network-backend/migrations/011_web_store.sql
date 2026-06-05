-- Web accounts, store products, orders (Stripe)

CREATE TABLE IF NOT EXISTS web_accounts (
  id SERIAL PRIMARY KEY,
  email VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  minecraft_uuid VARCHAR(36),
  minecraft_username VARCHAR(16),
  link_code VARCHAR(8),
  link_code_expires TIMESTAMPTZ,
  email_verified BOOLEAN DEFAULT false,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_web_accounts_uuid ON web_accounts(minecraft_uuid);
CREATE INDEX IF NOT EXISTS idx_web_accounts_username ON web_accounts(LOWER(minecraft_username));

CREATE TABLE IF NOT EXISTS store_products (
  id SERIAL PRIMARY KEY,
  slug VARCHAR(64) UNIQUE NOT NULL,
  name VARCHAR(128) NOT NULL,
  description TEXT DEFAULT '',
  price_cents INT NOT NULL CHECK (price_cents > 0),
  currency VARCHAR(8) DEFAULT 'eur',
  category VARCHAR(32) DEFAULT 'rank',
  reward_type VARCHAR(32) NOT NULL,
  reward_payload JSONB NOT NULL DEFAULT '{}',
  perks JSONB DEFAULT '[]',
  active BOOLEAN DEFAULT true,
  sort_order INT DEFAULT 0,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS store_orders (
  id SERIAL PRIMARY KEY,
  account_id INT REFERENCES web_accounts(id) ON DELETE SET NULL,
  product_id INT REFERENCES store_products(id),
  minecraft_uuid VARCHAR(36),
  minecraft_username VARCHAR(16) NOT NULL,
  stripe_session_id VARCHAR(255) UNIQUE,
  stripe_payment_intent VARCHAR(255),
  status VARCHAR(32) DEFAULT 'pending',
  amount_cents INT NOT NULL,
  currency VARCHAR(8) DEFAULT 'eur',
  fulfillment_commands JSONB DEFAULT '[]',
  fulfillment_error TEXT,
  fulfilled_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_store_orders_status ON store_orders(status);
CREATE INDEX IF NOT EXISTS idx_store_orders_account ON store_orders(account_id);
CREATE INDEX IF NOT EXISTS idx_store_orders_created ON store_orders(created_at DESC);

CREATE TABLE IF NOT EXISTS store_fulfillment_log (
  id SERIAL PRIMARY KEY,
  order_id INT REFERENCES store_orders(id) ON DELETE CASCADE,
  command TEXT NOT NULL,
  success BOOLEAN DEFAULT false,
  error TEXT,
  created_at TIMESTAMPTZ DEFAULT NOW()
);
