const db = require('../database');

async function upsertSubscription({
  accountId,
  productId,
  uuid,
  username,
  stripeSubscriptionId,
  stripeCustomerId,
  status,
  currentPeriodEnd,
  cancelAtPeriodEnd,
}) {
  const res = await db.query(
    `INSERT INTO store_subscriptions
       (account_id, product_id, minecraft_uuid, minecraft_username, stripe_subscription_id, stripe_customer_id, status, current_period_end, cancel_at_period_end)
     VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9)
     ON CONFLICT (stripe_subscription_id) DO UPDATE SET
       status = EXCLUDED.status,
       current_period_end = EXCLUDED.current_period_end,
       cancel_at_period_end = EXCLUDED.cancel_at_period_end,
       updated_at = NOW()
     RETURNING *`,
    [
      accountId,
      productId,
      uuid,
      username,
      stripeSubscriptionId,
      stripeCustomerId,
      status,
      currentPeriodEnd,
      cancelAtPeriodEnd || false,
    ]
  );
  return res.rows[0];
}

async function findByStripeId(stripeSubscriptionId) {
  const res = await db.query(
    'SELECT * FROM store_subscriptions WHERE stripe_subscription_id = $1',
    [stripeSubscriptionId]
  );
  return res.rows[0] || null;
}

async function updateStatus(stripeSubscriptionId, status, extra = {}) {
  const res = await db.query(
    `UPDATE store_subscriptions SET status = $2,
      current_period_end = COALESCE($3, current_period_end),
      cancel_at_period_end = COALESCE($4, cancel_at_period_end),
      updated_at = NOW()
     WHERE stripe_subscription_id = $1 RETURNING *`,
    [
      stripeSubscriptionId,
      status,
      extra.currentPeriodEnd || null,
      extra.cancelAtPeriodEnd,
    ]
  );
  return res.rows[0] || null;
}

async function listAll({ status, page = 1, limit = 30 } = {}) {
  const lim = Math.min(100, limit);
  const offset = (page - 1) * lim;
  const params = [];
  let where = '';
  if (status) {
    params.push(status);
    where = `WHERE s.status = $${params.length}`;
  }
  params.push(lim, offset);
  const res = await db.query(
    `SELECT s.*, p.name AS product_name, p.slug AS product_slug
     FROM store_subscriptions s
     LEFT JOIN store_products p ON p.id = s.product_id
     ${where}
     ORDER BY s.updated_at DESC LIMIT $${params.length - 1} OFFSET $${params.length}`,
    params
  );
  return res.rows;
}

module.exports = {
  upsertSubscription,
  findByStripeId,
  updateStatus,
  listAll,
};
