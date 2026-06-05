-- Production hardening: unique constraints for promos and account linking

CREATE UNIQUE INDEX IF NOT EXISTS idx_promo_redemptions_account_unique
  ON promo_redemptions (promo_code_id, account_id);

CREATE UNIQUE INDEX IF NOT EXISTS idx_web_accounts_uuid_unique
  ON web_accounts (minecraft_uuid)
  WHERE minecraft_uuid IS NOT NULL;
