const db = require('../database');

const DEFAULT_PROMOS = [
  { code: 'LAUNCH10', type: 'percent', value: 10, max_uses: 1000, per_account_limit: 1 },
  {
    code: 'NEWPLAYER',
    type: 'starter',
    value: 0,
    max_uses: 5000,
    per_account_limit: 1,
    reward_grants: [
      { type: 'coins', amount: 15000 },
      { type: 'crate_keys', crate: 'COMMON', amount: 1 },
      { type: 'booster_money', percent: 10, duration_minutes: 30 },
    ],
  },
  {
    code: 'EVENT',
    type: 'percent',
    value: 20,
    max_uses: 300,
    per_account_limit: 1,
    expires_at: new Date(Date.now() + 7 * 86400000).toISOString(),
  },
];

async function seedPromos() {
  for (const p of DEFAULT_PROMOS) {
    await db.query(
      `INSERT INTO promo_codes (code, type, value, max_uses, per_account_limit, expires_at, reward_grants)
       VALUES ($1,$2,$3,$4,$5,$6,$7)
       ON CONFLICT (code) DO UPDATE SET
         type = EXCLUDED.type,
         value = EXCLUDED.value,
         max_uses = EXCLUDED.max_uses,
         per_account_limit = EXCLUDED.per_account_limit,
         expires_at = COALESCE(EXCLUDED.expires_at, promo_codes.expires_at),
         reward_grants = EXCLUDED.reward_grants`,
      [
        p.code,
        p.type,
        p.value,
        p.max_uses,
        p.per_account_limit,
        p.expires_at || null,
        JSON.stringify(p.reward_grants || []),
      ]
    );
  }
}

async function findByCode(code) {
  const res = await db.query(
    `SELECT * FROM promo_codes WHERE UPPER(code) = UPPER($1) AND active = true`,
    [code.trim()]
  );
  return res.rows[0] || null;
}

async function countAccountUses(promoId, accountId) {
  const res = await db.query(
    'SELECT COUNT(*)::int AS c FROM promo_redemptions WHERE promo_code_id = $1 AND account_id = $2',
    [promoId, accountId]
  );
  return res.rows[0]?.c || 0;
}

function isPromoValid(promo) {
  if (!promo || !promo.active) return false;
  if (promo.expires_at && new Date(promo.expires_at) < new Date()) return false;
  if (promo.valid_from && new Date(promo.valid_from) > new Date()) return false;
  if (promo.max_uses != null && promo.uses_count >= promo.max_uses) return false;
  return true;
}

function parseGrants(promo) {
  const raw = promo.reward_grants;
  if (!raw) return [];
  return Array.isArray(raw) ? raw : JSON.parse(raw || '[]');
}

function calcDiscount(promo, priceCents) {
  if (!promo) return 0;
  if (promo.type === 'percent') {
    return Math.min(priceCents, Math.floor((priceCents * promo.value) / 100));
  }
  if (promo.type === 'fixed') {
    return Math.min(priceCents, promo.value);
  }
  return 0;
}

async function validateForCheckout({ code, accountId, priceCents }) {
  const promo = await findByCode(code);
  if (!promo) return { valid: false, error: 'invalid_code' };
  if (promo.type === 'starter') return { valid: false, error: 'use_redeem_endpoint' };
  if (!isPromoValid(promo)) return { valid: false, error: 'expired_or_exhausted' };

  if (accountId) {
    const uses = await countAccountUses(promo.id, accountId);
    if (uses >= (promo.per_account_limit || 1)) {
      return { valid: false, error: 'already_used' };
    }
  }

  const discountCents = calcDiscount(promo, priceCents);
  const finalCents = Math.max(50, priceCents - discountCents);
  return { valid: true, promo, discountCents, finalCents };
}

async function validateStarterRedeem({ code, accountId }) {
  const promo = await findByCode(code);
  if (!promo || promo.type !== 'starter') return { valid: false, error: 'invalid_code' };
  if (!isPromoValid(promo)) return { valid: false, error: 'expired_or_exhausted' };
  if (!accountId) return { valid: false, error: 'unauthorized' };
  const uses = await countAccountUses(promo.id, accountId);
  if (uses >= (promo.per_account_limit || 1)) {
    return { valid: false, error: 'already_used' };
  }
  return { valid: true, promo, grants: parseGrants(promo) };
}

async function recordRedemption({ promoId, accountId, orderId, uuid }) {
  const result = await tryClaimRedemption({ promoId, accountId, orderId, uuid });
  if (!result.claimed) {
    throw new Error(result.error || 'redemption_failed');
  }
  return result;
}

async function tryClaimRedemption({ promoId, accountId, orderId, uuid }) {
  const client = await db.getPool().connect();
  try {
    await client.query('BEGIN');
    const promoRes = await client.query('SELECT * FROM promo_codes WHERE id = $1 FOR UPDATE', [promoId]);
    const promo = promoRes.rows[0];
    if (!promo || !isPromoValid(promo)) {
      await client.query('ROLLBACK');
      return { claimed: false, error: 'expired_or_exhausted' };
    }

    const usesRes = await client.query(
      'SELECT COUNT(*)::int AS c FROM promo_redemptions WHERE promo_code_id = $1 AND account_id = $2',
      [promoId, accountId]
    );
    if ((usesRes.rows[0]?.c || 0) >= (promo.per_account_limit || 1)) {
      await client.query('ROLLBACK');
      return { claimed: false, error: 'already_used' };
    }
    if (promo.max_uses != null && promo.uses_count >= promo.max_uses) {
      await client.query('ROLLBACK');
      return { claimed: false, error: 'expired_or_exhausted' };
    }

    const ins = await client.query(
      `INSERT INTO promo_redemptions (promo_code_id, account_id, order_id, minecraft_uuid)
       VALUES ($1,$2,$3,$4)
       ON CONFLICT (promo_code_id, account_id) DO NOTHING RETURNING id`,
      [promoId, accountId, orderId || null, uuid]
    );
    if (!ins.rows.length) {
      await client.query('ROLLBACK');
      return { claimed: false, error: 'already_used' };
    }

    await client.query('UPDATE promo_codes SET uses_count = uses_count + 1 WHERE id = $1', [promoId]);
    await client.query('COMMIT');
    return { claimed: true };
  } catch (err) {
    await client.query('ROLLBACK').catch(() => {});
    throw err;
  } finally {
    client.release();
  }
}

async function listAll() {
  const res = await db.query('SELECT * FROM promo_codes ORDER BY created_at DESC');
  return res.rows;
}

async function createPromo(fields) {
  const res = await db.query(
    `INSERT INTO promo_codes (code, type, value, max_uses, per_account_limit, expires_at, active, reward_grants)
     VALUES ($1,$2,$3,$4,$5,$6,$7,$8) RETURNING *`,
    [
      fields.code.toUpperCase(),
      fields.type,
      fields.value,
      fields.max_uses ?? null,
      fields.per_account_limit ?? 1,
      fields.expires_at ?? null,
      fields.active !== false,
      JSON.stringify(fields.reward_grants || []),
    ]
  );
  return res.rows[0];
}

async function listRedemptions({ page = 1, limit = 50 } = {}) {
  const lim = Math.min(100, limit);
  const offset = (page - 1) * lim;
  const res = await db.query(
    `SELECT r.*, p.code AS promo_code, p.type AS promo_type
     FROM promo_redemptions r
     JOIN promo_codes p ON p.id = r.promo_code_id
     ORDER BY r.created_at DESC LIMIT $1 OFFSET $2`,
    [lim, offset]
  );
  return res.rows;
}

module.exports = {
  seedPromos,
  findByCode,
  validateForCheckout,
  validateStarterRedeem,
  recordRedemption,
  tryClaimRedemption,
  listAll,
  createPromo,
  listRedemptions,
  isPromoValid,
  calcDiscount,
  parseGrants,
};
