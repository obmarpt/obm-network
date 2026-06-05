const db = require('../database');

const DEPRECATED_SLUGS = ['vip-7d', 'vip-90d', 'vip-monthly', 'starter-bundle'];

const DEFAULT_PRODUCTS = [
  {
    slug: 'mvp-plus-30d',
    name: 'MVP+ — 30 dias',
    description: '💎 Melhor valor — máximo QoL, cosméticos exclusivos e +50% money. Sem vantagem PvP.',
    price_cents: 1999,
    compare_price_cents: 2499,
    category: 'vip',
    badge: 'best_value',
    reward_type: 'rank_temp',
    reward_payload: { group: 'mvp_plus', days: 30 },
    perks: ['+50% money', '/repair instant (3/h)', '+8 homes', '5 keys/dia', 'Prefix animado', 'Cosméticos MVP+'],
    sort_order: 1,
    featured: true,
  },
  {
    slug: 'vip-30d',
    name: 'VIP — 30 dias',
    description: 'Entrada perfeita na network. QoL equilibrado.',
    price_cents: 499,
    category: 'vip',
    reward_type: 'rank_temp',
    reward_payload: { group: 'vip', days: 30 },
    perks: ['+10% money', '/repair (5min CD)', '+2 homes', '1 key/dia', 'Prefix §aVIP'],
    sort_order: 2,
  },
  {
    slug: 'vip-plus-30d',
    name: 'VIP+ — 30 dias',
    description: 'Mais homes, kits e boost de economia.',
    price_cents: 999,
    category: 'vip',
    reward_type: 'rank_temp',
    reward_payload: { group: 'vip_plus', days: 30 },
    perks: ['+20% money', '/repair (3min CD)', '+4 homes', '2 keys/dia', 'Kits básicos', 'Prefix §bVIP+'],
    sort_order: 3,
  },
  {
    slug: 'mvp-30d',
    name: 'MVP — 30 dias',
    description: 'Prioridade, warps e perks premium.',
    price_cents: 1499,
    category: 'vip',
    reward_type: 'rank_temp',
    reward_payload: { group: 'mvp', days: 30 },
    perks: ['+30% money', '/repair (90s CD)', '+6 homes', '3 keys/dia', 'Warps especiais', 'Prefix §dMVP'],
    sort_order: 4,
    featured: true,
  },
  {
    slug: 'vip-lifetime',
    name: 'VIP — Lifetime',
    description: 'VIP permanente. Uma compra, para sempre.',
    price_cents: 1999,
    compare_price_cents: 2994,
    category: 'vip',
    badge: 'best_value',
    reward_type: 'rank',
    reward_payload: { group: 'vip' },
    perks: ['VIP permanente', 'Todos perks VIP', 'Sem renovação'],
    sort_order: 10,
  },
  {
    slug: 'vip-plus-lifetime',
    name: 'VIP+ — Lifetime',
    description: 'VIP+ para sempre.',
    price_cents: 3999,
    category: 'vip',
    reward_type: 'rank',
    reward_payload: { group: 'vip_plus' },
    perks: ['VIP+ permanente', 'Todos perks VIP+'],
    sort_order: 11,
  },
  {
    slug: 'mvp-lifetime',
    name: 'MVP — Lifetime',
    description: 'MVP permanente com todos os perks.',
    price_cents: 5999,
    category: 'vip',
    reward_type: 'rank',
    reward_payload: { group: 'mvp' },
    perks: ['MVP permanente', 'Prioridade & warps'],
    sort_order: 12,
  },
  {
    slug: 'mvp-plus-lifetime',
    name: 'MVP+ — Lifetime',
    description: 'O topo absoluto. Lifetime MVP+.',
    price_cents: 7999,
    compare_price_cents: 9999,
    category: 'vip',
    badge: 'best_value',
    reward_type: 'rank',
    reward_payload: { group: 'mvp_plus' },
    perks: ['MVP+ permanente', 'Cosméticos exclusivos', 'Máximo QoL'],
    sort_order: 13,
    featured: true,
  },
  {
    slug: 'starter-pack',
    name: 'Starter Pack',
    description: 'Ideal para começar: VIP 7 dias + 50k coins + 3 keys. Poupa ~38% vs comprar separado.',
    price_cents: 799,
    compare_price_cents: 1297,
    category: 'bundle',
    badge: 'recommended',
    reward_type: 'bundle',
    reward_payload: {
      rewards: [
        { type: 'rank_temp', group: 'vip', days: 7 },
        { type: 'coins', amount: 50000 },
        { type: 'crate_keys', crate: 'COMMON', amount: 3 },
      ],
    },
    perks: ['VIP 7 dias', '50k Money', '3 Crate Keys', 'Poupa 38%'],
    sort_order: 5,
    featured: true,
  },
  {
    slug: 'pro-pack',
    name: 'Pro Pack',
    description: 'VIP+ 30 dias + Fire Aura + 100k coins + boosters 1h. Progressão acelerada sem P2W.',
    price_cents: 1499,
    compare_price_cents: 2197,
    category: 'bundle',
    badge: 'limited',
    reward_type: 'bundle',
    reward_payload: {
      rewards: [
        { type: 'rank_temp', group: 'vip_plus', days: 30 },
        { type: 'cosmetic', cosmetic: 'FIRE_AURA' },
        { type: 'coins', amount: 100000 },
        { type: 'booster_money', percent: 20, duration_minutes: 60 },
        { type: 'booster_bp', percent: 25, duration_minutes: 60 },
      ],
    },
    perks: ['VIP+ 30 dias', 'Fire Aura', '100k Money', 'Boosters 1h'],
    sort_order: 6,
    sale_ends_at: new Date(Date.now() + 14 * 86400000).toISOString(),
  },
  {
    slug: 'ultimate-pack',
    name: 'Ultimate Pack',
    description: '💎 Melhor bundle: MVP+ 30d + aura Mythic + 250k coins + 9 crate keys. Máximo valor.',
    price_cents: 2499,
    compare_price_cents: 3797,
    category: 'bundle',
    badge: 'best_value',
    reward_type: 'bundle',
    reward_payload: {
      rewards: [
        { type: 'rank_temp', group: 'mvp_plus', days: 30 },
        { type: 'cosmetic', cosmetic: 'SURVIVOR_AURA' },
        { type: 'coins', amount: 250000 },
        { type: 'crate_keys', crate: 'COMMON', amount: 5 },
        { type: 'crate_keys', crate: 'RARE', amount: 3 },
        { type: 'crate_keys', crate: 'EPIC', amount: 1 },
      ],
    },
    perks: ['MVP+ 30 dias', 'Cosmético Mythic', '250k Money', '9 Crate Keys'],
    sort_order: 7,
    featured: true,
  },
  {
    slug: 'booster-money-1h',
    name: 'Booster Money +20%',
    description: '+20% coins SMP durante 1 hora. Stack com VIP.',
    price_cents: 199,
    category: 'booster',
    reward_type: 'booster_money',
    reward_payload: { percent: 20, duration_minutes: 60 },
    perks: ['+20% coins SMP', '1 hora', 'Não afeta PvP'],
    sort_order: 40,
  },
  {
    slug: 'booster-bp-1h',
    name: 'Booster Battle Pass +25%',
    description: '+25% XP Battle Pass durante 1 hora.',
    price_cents: 249,
    category: 'booster',
    reward_type: 'booster_bp',
    reward_payload: { percent: 25, duration_minutes: 60 },
    perks: ['+25% BP XP', '1 hora', 'Progresso mais rápido'],
    sort_order: 41,
  },
  {
    slug: 'emerald-500',
    name: '500 Emeralds',
    description: 'Emeralds para cosméticos, crates e loja global.',
    price_cents: 299,
    category: 'cosmetic',
    reward_type: 'emeralds',
    reward_payload: { amount: 500 },
    perks: ['Entrega instantânea', 'Toda a network'],
    sort_order: 50,
  },
  {
    slug: 'cosmetic-heart-trail',
    name: 'Heart Trail',
    description: 'Cosmético Heart Trail permanente.',
    price_cents: 399,
    category: 'cosmetic',
    reward_type: 'cosmetic',
    reward_payload: { cosmetic: 'HEART_TRAIL' },
    perks: ['Trail visual', 'Permanente'],
    sort_order: 51,
  },
  {
    slug: 'smp-coins-50k',
    name: '50.000 Coins SMP',
    description: 'Boost economia Rush — ranks e loja SMP.',
    price_cents: 499,
    category: 'currency',
    reward_type: 'coins',
    reward_payload: { amount: 50000 },
    perks: ['Coins Rush', 'Entrega automática'],
    sort_order: 52,
  },
];

async function seedProducts() {
  for (const slug of DEPRECATED_SLUGS) {
    await db.query('UPDATE store_products SET active = false WHERE slug = $1', [slug]);
  }
  for (const p of DEFAULT_PRODUCTS) {
    await db.query(
      `INSERT INTO store_products
         (slug, name, description, price_cents, compare_price_cents, category, reward_type, reward_payload, perks,
          sort_order, badge, featured, is_subscription, sale_ends_at)
       VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14)
       ON CONFLICT (slug) DO UPDATE SET
         name = EXCLUDED.name,
         description = EXCLUDED.description,
         price_cents = EXCLUDED.price_cents,
         compare_price_cents = EXCLUDED.compare_price_cents,
         category = EXCLUDED.category,
         reward_type = EXCLUDED.reward_type,
         reward_payload = EXCLUDED.reward_payload,
         perks = EXCLUDED.perks,
         sort_order = EXCLUDED.sort_order,
         badge = EXCLUDED.badge,
         featured = EXCLUDED.featured,
         is_subscription = EXCLUDED.is_subscription,
         sale_ends_at = EXCLUDED.sale_ends_at,
         updated_at = NOW()`,
      [
        p.slug,
        p.name,
        p.description,
        p.price_cents,
        p.compare_price_cents || null,
        p.category,
        p.reward_type,
        JSON.stringify(p.reward_payload),
        JSON.stringify(p.perks),
        p.sort_order,
        p.badge || null,
        p.featured || false,
        p.is_subscription || false,
        p.sale_ends_at || null,
      ]
    );
  }
}

async function listActiveProducts() {
  const res = await db.query(
    `SELECT id, slug, name, description, price_cents, compare_price_cents, currency, category,
            reward_type, reward_payload, perks, sort_order, badge, featured, is_subscription, sale_ends_at
     FROM store_products WHERE active = true ORDER BY sort_order ASC, id ASC`
  );
  return res.rows.map(formatProduct);
}

async function listAllProducts() {
  const res = await db.query(
    `SELECT * FROM store_products ORDER BY sort_order ASC, id ASC`
  );
  return res.rows.map(formatProduct);
}

async function getProductById(id) {
  const res = await db.query('SELECT * FROM store_products WHERE id = $1', [id]);
  return res.rows[0] ? formatProduct(res.rows[0]) : null;
}

async function getProductBySlug(slug) {
  const res = await db.query('SELECT * FROM store_products WHERE slug = $1 AND active = true', [slug]);
  return res.rows[0] ? formatProduct(res.rows[0]) : null;
}

async function createOrder({
  accountId,
  productId,
  minecraftUuid,
  minecraftUsername,
  stripeSessionId,
  amountCents,
  currency,
  promoCodeId,
  discountCents,
  isSubscription,
}) {
  const res = await db.query(
    `INSERT INTO store_orders
       (account_id, product_id, minecraft_uuid, minecraft_username, stripe_session_id, amount_cents, currency,
        promo_code_id, discount_cents, is_subscription, status)
     VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,'pending')
     RETURNING *`,
    [
      accountId,
      productId,
      minecraftUuid,
      minecraftUsername,
      stripeSessionId,
      amountCents,
      currency,
      promoCodeId || null,
      discountCents || 0,
      isSubscription || false,
    ]
  );
  return res.rows[0];
}

async function attachSubscriptionToOrder(orderId, stripeSubscriptionId) {
  await db.query(
    `UPDATE store_orders SET stripe_subscription_id = $2, is_subscription = true, updated_at = NOW() WHERE id = $1`,
    [orderId, stripeSubscriptionId]
  );
}

async function getOrderByStripeSession(sessionId) {
  const res = await db.query('SELECT * FROM store_orders WHERE stripe_session_id = $1', [sessionId]);
  return res.rows[0] || null;
}

async function getOrderById(id) {
  const res = await db.query('SELECT * FROM store_orders WHERE id = $1', [id]);
  return res.rows[0] || null;
}

async function markOrderPaid(orderId, paymentIntent) {
  const res = await db.query(
    `UPDATE store_orders SET status = 'paid', stripe_payment_intent = $2, updated_at = NOW()
     WHERE id = $1 AND status = 'pending' RETURNING *`,
    [orderId, paymentIntent]
  );
  return res.rows[0] || null;
}

async function markOrderFulfilled(orderId, commands) {
  const res = await db.query(
    `UPDATE store_orders SET status = 'fulfilled', fulfillment_commands = $2, fulfilled_at = NOW(), updated_at = NOW()
     WHERE id = $1 AND status = 'paid' RETURNING *`,
    [orderId, JSON.stringify(commands)]
  );
  return res.rows[0] || null;
}

async function listOrdersByAccount(accountId, { limit = 20 } = {}) {
  const lim = Math.min(50, Math.max(1, parseInt(limit, 10) || 20));
  const res = await db.query(
    `SELECT o.*, p.name AS product_name, p.slug AS product_slug
     FROM store_orders o
     LEFT JOIN store_products p ON p.id = o.product_id
     WHERE o.account_id = $1
     ORDER BY o.created_at DESC
     LIMIT $2`,
    [accountId, lim]
  );
  return res.rows;
}

async function markOrderFailed(orderId, error) {
  await db.query(
    `UPDATE store_orders SET status = 'failed', fulfillment_error = $2, updated_at = NOW() WHERE id = $1`,
    [orderId, error]
  );
}

async function logFulfillment(orderId, command, success, error) {
  await db.query(
    `INSERT INTO store_fulfillment_log (order_id, command, success, error) VALUES ($1,$2,$3,$4)`,
    [orderId, command, success, error || null]
  );
}

async function listOrders({ page = 1, limit = 25, status } = {}) {
  const lim = Math.min(100, Math.max(1, parseInt(limit, 10) || 25));
  const pg = Math.max(1, parseInt(page, 10) || 1);
  const offset = (pg - 1) * lim;
  const params = [];
  let where = '';
  if (status) {
    params.push(status);
    where = `WHERE o.status = $${params.length}`;
  }
  const countRes = await db.query(`SELECT COUNT(*)::int AS c FROM store_orders o ${where}`, params);
  params.push(lim, offset);
  const res = await db.query(
    `SELECT o.*, p.name AS product_name, p.slug AS product_slug
     FROM store_orders o
     LEFT JOIN store_products p ON p.id = o.product_id
     ${where}
     ORDER BY o.created_at DESC
     LIMIT $${params.length - 1} OFFSET $${params.length}`,
    params
  );
  return {
    total: countRes.rows[0]?.c || 0,
    page: pg,
    limit: lim,
    orders: res.rows,
  };
}

async function updateProduct(id, fields) {
  const allowed = ['name', 'description', 'price_cents', 'compare_price_cents', 'active', 'sort_order', 'perks', 'badge', 'featured', 'sale_ends_at'];
  const sets = [];
  const params = [];
  for (const key of allowed) {
    if (fields[key] !== undefined) {
      params.push(key === 'perks' ? JSON.stringify(fields[key]) : fields[key]);
      sets.push(`${key} = $${params.length}`);
    }
  }
  if (sets.length === 0) return null;
  params.push(id);
  const res = await db.query(
    `UPDATE store_products SET ${sets.join(', ')}, updated_at = NOW() WHERE id = $${params.length} RETURNING *`,
    params
  );
  return res.rows[0] ? formatProduct(res.rows[0]) : null;
}

function formatProduct(row) {
  const compare = row.compare_price_cents;
  const price = row.price_cents;
  let savingsPct = null;
  if (compare && compare > price) {
    savingsPct = Math.round(((compare - price) / compare) * 100);
  }
  return {
    id: row.id,
    slug: row.slug,
    name: row.name,
    description: row.description,
    price_cents: price,
    price_display: (price / 100).toFixed(2),
    compare_price_cents: compare || null,
    compare_display: compare ? (compare / 100).toFixed(2) : null,
    savings_pct: savingsPct,
    currency: row.currency || 'eur',
    category: row.category,
    reward_type: row.reward_type,
    reward_payload: typeof row.reward_payload === 'object' ? row.reward_payload : JSON.parse(row.reward_payload || '{}'),
    perks: Array.isArray(row.perks) ? row.perks : JSON.parse(row.perks || '[]'),
    active: row.active,
    sort_order: row.sort_order,
    badge: row.badge || null,
    featured: Boolean(row.featured),
    is_subscription: Boolean(row.is_subscription),
    sale_ends_at: row.sale_ends_at || null,
  };
}

module.exports = {
  seedProducts,
  listActiveProducts,
  listAllProducts,
  getProductById,
  getProductBySlug,
  createOrder,
  getOrderByStripeSession,
  getOrderById,
  markOrderPaid,
  markOrderFulfilled,
  markOrderFailed,
  logFulfillment,
  listOrders,
  listOrdersByAccount,
  updateProduct,
  attachSubscriptionToOrder,
};
