-- Monetização: promo codes, subscrições, entitlements, produto UX

ALTER TABLE store_products ADD COLUMN IF NOT EXISTS badge VARCHAR(32);
ALTER TABLE store_products ADD COLUMN IF NOT EXISTS compare_price_cents INT;
ALTER TABLE store_products ADD COLUMN IF NOT EXISTS sale_ends_at TIMESTAMPTZ;
ALTER TABLE store_products ADD COLUMN IF NOT EXISTS is_subscription BOOLEAN DEFAULT false;
ALTER TABLE store_products ADD COLUMN IF NOT EXISTS subscription_interval VARCHAR(16) DEFAULT 'month';
ALTER TABLE store_products ADD COLUMN IF NOT EXISTS featured BOOLEAN DEFAULT false;

ALTER TABLE store_orders ADD COLUMN IF NOT EXISTS promo_code_id INT;
ALTER TABLE store_orders ADD COLUMN IF NOT EXISTS discount_cents INT DEFAULT 0;
ALTER TABLE store_orders ADD COLUMN IF NOT EXISTS stripe_subscription_id VARCHAR(255);
ALTER TABLE store_orders ADD COLUMN IF NOT EXISTS is_subscription BOOLEAN DEFAULT false;

CREATE TABLE IF NOT EXISTS promo_codes (
  id SERIAL PRIMARY KEY,
  code VARCHAR(32) UNIQUE NOT NULL,
  type VARCHAR(16) NOT NULL,
  value INT NOT NULL,
  max_uses INT,
  uses_count INT DEFAULT 0,
  per_account_limit INT DEFAULT 1,
  valid_from TIMESTAMPTZ DEFAULT NOW(),
  expires_at TIMESTAMPTZ,
  active BOOLEAN DEFAULT true,
  free_product_id INT REFERENCES store_products(id),
  created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS promo_redemptions (
  id SERIAL PRIMARY KEY,
  promo_code_id INT NOT NULL REFERENCES promo_codes(id),
  account_id INT REFERENCES web_accounts(id),
  order_id INT REFERENCES store_orders(id),
  minecraft_uuid VARCHAR(36),
  created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_promo_redemptions_code ON promo_redemptions(promo_code_id);
CREATE INDEX IF NOT EXISTS idx_promo_redemptions_account ON promo_redemptions(account_id);

CREATE TABLE IF NOT EXISTS store_subscriptions (
  id SERIAL PRIMARY KEY,
  account_id INT REFERENCES web_accounts(id),
  product_id INT REFERENCES store_products(id),
  minecraft_uuid VARCHAR(36),
  minecraft_username VARCHAR(16) NOT NULL,
  stripe_subscription_id VARCHAR(255) UNIQUE NOT NULL,
  stripe_customer_id VARCHAR(255),
  status VARCHAR(32) DEFAULT 'active',
  current_period_end TIMESTAMPTZ,
  cancel_at_period_end BOOLEAN DEFAULT false,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_store_subs_status ON store_subscriptions(status);
CREATE INDEX IF NOT EXISTS idx_store_subs_account ON store_subscriptions(account_id);

CREATE TABLE IF NOT EXISTS store_entitlements (
  id SERIAL PRIMARY KEY,
  minecraft_uuid VARCHAR(36) NOT NULL,
  minecraft_username VARCHAR(16) NOT NULL,
  entitlement_type VARCHAR(32) NOT NULL,
  payload JSONB NOT NULL DEFAULT '{}',
  order_id INT REFERENCES store_orders(id),
  subscription_id INT REFERENCES store_subscriptions(id),
  expires_at TIMESTAMPTZ,
  revoked_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_entitlements_uuid ON store_entitlements(minecraft_uuid);
CREATE INDEX IF NOT EXISTS idx_entitlements_expires ON store_entitlements(expires_at) WHERE revoked_at IS NULL;

CREATE TABLE IF NOT EXISTS store_stripe_events (
  event_id VARCHAR(255) PRIMARY KEY,
  event_type VARCHAR(64),
  processed_at TIMESTAMPTZ DEFAULT NOW()
);
