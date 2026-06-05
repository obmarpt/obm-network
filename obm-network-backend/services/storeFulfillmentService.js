const pool = require('../database');
const storeRepo = require('../db/storeRepository');
const entitlementRepo = require('../db/entitlementRepository');
const { sanitizeRemoteCommand } = require('../utils/commandWhitelist');

function buildSingleReward(type, payload, player) {
  switch (type) {
    case 'rank': {
      const group = payload.group || payload.rank;
      if (!group) throw new Error('missing_rank_group');
      return [`obmstore grant rank ${player} ${group}`];
    }
    case 'rank_temp': {
      const group = payload.group || payload.rank;
      const days = parseInt(payload.days, 10);
      if (!group || !days || days <= 0) throw new Error('invalid_temp_rank');
      return [`lp user ${player} parent addtemp ${group} ${days}d`];
    }
    case 'rank_sub': {
      const group = payload.group || payload.rank;
      const days = parseInt(payload.days, 10) || 30;
      if (!group) throw new Error('missing_rank_group');
      return [`lp user ${player} parent addtemp ${group} ${days}d`];
    }
    case 'emeralds': {
      const amount = parseInt(payload.amount, 10);
      if (!amount || amount <= 0) throw new Error('invalid_emerald_amount');
      return [`obmstore grant emeralds ${player} ${amount}`];
    }
    case 'coins': {
      const amount = parseInt(payload.amount, 10);
      if (!amount || amount <= 0) throw new Error('invalid_coin_amount');
      return [`obmstore grant coins ${player} ${amount}`];
    }
    case 'cosmetic': {
      const cosmetic = payload.cosmetic || payload.id;
      if (!cosmetic) throw new Error('missing_cosmetic');
      return [`obmstore grant cosmetic ${player} ${cosmetic}`];
    }
    case 'booster_money': {
      const percent = parseInt(payload.percent, 10);
      const minutes = parseInt(payload.duration_minutes || payload.minutes, 10);
      if (!percent || !minutes) throw new Error('invalid_booster');
      return [`obmstore grant booster money ${player} ${percent} ${minutes}`];
    }
    case 'booster_bp': {
      const percent = parseInt(payload.percent, 10);
      const minutes = parseInt(payload.duration_minutes || payload.minutes, 10);
      if (!percent || !minutes) throw new Error('invalid_booster');
      return [`obmstore grant booster bp ${player} ${percent} ${minutes}`];
    }
    case 'crate_keys': {
      const crate = String(payload.crate || 'COMMON').toUpperCase();
      const amount = parseInt(payload.amount, 10);
      if (!amount || amount <= 0) throw new Error('invalid_key_amount');
      return [`obmstore grant keys ${player} ${crate} ${amount}`];
    }
    default:
      throw new Error(`unknown_reward_type:${type}`);
  }
}

function buildCommands(product, username) {
  const player = String(username || '').trim();
  if (!/^[a-zA-Z0-9_]{3,16}$/.test(player)) {
    throw new Error('invalid_username');
  }
  const payload = product.reward_payload || {};
  const type = product.reward_type;

  if (type === 'bundle') {
    const rewards = Array.isArray(payload.rewards) ? payload.rewards : [];
    if (rewards.length === 0) throw new Error('empty_bundle');
    const cmds = [];
    for (const r of rewards) {
      cmds.push(...buildSingleReward(r.type, r, player));
    }
    return { commands: cmds, entitlements: buildEntitlementsFromRewards(rewards, player) };
  }

  const cmds = buildSingleReward(type, payload, player);
  return {
    commands: cmds,
    entitlements: buildEntitlementsFromRewards([{ type, ...payload }], player),
  };
}

function expiresFromReward(reward) {
  if (reward.type === 'rank_temp' || reward.type === 'rank_sub') {
    const days = parseInt(reward.days, 10);
    if (days > 0) return new Date(Date.now() + days * 86400000);
  }
  if (reward.type === 'booster_money' || reward.type === 'booster_bp') {
    const minutes = parseInt(reward.duration_minutes || reward.minutes, 10);
    if (minutes > 0) return new Date(Date.now() + minutes * 60000);
  }
  return null;
}

function buildEntitlementsFromRewards(rewards, username) {
  const out = [];
  for (const r of rewards) {
    const exp = expiresFromReward(r);
    if (!exp) continue;
    out.push({
      type: r.type,
      payload: { ...r, group: r.group || r.rank },
      username,
      expiresAt: exp,
    });
  }
  return out;
}

async function queueCommand(command) {
  const safe = sanitizeRemoteCommand(command);
  if (!safe) {
    throw new Error(`command_blocked:${command}`);
  }
  await pool.query('INSERT INTO commands (command) VALUES ($1)', [safe]);
  return safe;
}

async function recordEntitlements(order, entitlements, subscriptionId) {
  for (const ent of entitlements) {
    await entitlementRepo.createEntitlement({
      uuid: order.minecraft_uuid,
      username: ent.username || order.minecraft_username,
      type: ent.type,
      payload: ent.payload,
      orderId: order.id,
      subscriptionId: subscriptionId || null,
      expiresAt: ent.expiresAt,
    });
  }
}

async function fulfillOrder(orderId, { subscriptionId } = {}) {
  const order = await storeRepo.getOrderById(orderId);
  if (!order) throw new Error('order_not_found');
  if (order.status === 'fulfilled') return { already: true, order };
  if (order.status !== 'paid') throw new Error('order_not_paid');

  const product = await storeRepo.getProductById(order.product_id);
  if (!product) throw new Error('product_not_found');

  const { commands: rawCommands, entitlements } = buildCommands(product, order.minecraft_username);
  const queued = [];

  try {
    for (const cmd of rawCommands) {
      const safe = await queueCommand(cmd);
      await storeRepo.logFulfillment(orderId, safe, true, null);
      queued.push(safe);
    }
    await recordEntitlements(order, entitlements, subscriptionId);
    const updated = await storeRepo.markOrderFulfilled(orderId, queued);
    if (!updated) {
      const current = await storeRepo.getOrderById(orderId);
      if (current?.status === 'fulfilled') {
        return { already: true, order: current, commands: queued };
      }
      throw new Error('fulfill_state_conflict');
    }
    return { success: true, order: updated, commands: queued };
  } catch (err) {
    await storeRepo.markOrderFailed(orderId, err.message);
    for (const cmd of queued) {
      await storeRepo.logFulfillment(orderId, cmd, true, null);
    }
    if (queued.length < rawCommands.length) {
      await storeRepo.logFulfillment(orderId, rawCommands[queued.length] || '?', false, err.message);
    }
    throw err;
  }
}

module.exports = {
  buildCommands,
  buildSingleReward,
  queueCommand,
  fulfillOrder,
  recordEntitlements,
};
