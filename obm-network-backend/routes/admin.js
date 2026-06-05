const { Router } = require('express');
const { authMiddleware } = require('../auth');
const { requireMod, requireAdmin } = require('../middleware/permissions');
const adminRepo = require('../db/adminRepository');
const bansRepo = require('../db/bansRepository');
const reportsRepo = require('../db/reportsRepository');
const anticheatRepo = require('../db/anticheatRepository');
const backupService = require('../services/backupService');
const statsHistory = require('../services/statsHistoryService');
const requestCache = require('../services/requestCache');
const db = require('../database');
const { broadcast } = require('../ws');
const { validateUuid, badRequest, internalError, isNonEmptyString, parseInteger } = require('./validation');
const { sanitizeRemoteCommand } = require('../utils/commandWhitelist');

const router = Router();

function requireDb(_req, res, next) {
  if (!db.isDatabaseAvailable()) {
    return res.status(503).json({ error: 'database_unavailable' });
  }
  next();
}

router.use(authMiddleware);

router.get('/admin/me', (req, res) => {
  res.json({ username: req.user?.username, role: req.user?.role || 'admin' });
});

router.get('/admin/stats', requireMod, requireDb, async (_req, res) => {
  try {
    const data = await requestCache.wrap('admin:stats', 5000, () => adminRepo.getGlobalStats());
    res.json(data);
  } catch (err) {
    return internalError(res, err, 'admin/stats');
  }
});

router.get('/admin/stats/history', requireMod, requireDb, async (req, res) => {
  try {
    const days = req.query.days || 14;
    const data = await requestCache.wrap(`admin:history:${days}`, 12000, () =>
      statsHistory.getHistory(days)
    );
    res.json(data);
  } catch (err) {
    return internalError(res, err, 'admin/stats/history');
  }
});

router.get('/admin/monitor', requireMod, requireDb, async (_req, res) => {
  try {
    const data = await requestCache.wrap('admin:monitor', 4000, () => adminRepo.getMonitor());
    res.json(data);
  } catch (err) {
    return internalError(res, err, 'admin/monitor');
  }
});

router.get('/admin/staff/online', requireMod, requireDb, async (_req, res) => {
  try {
    res.json(await adminRepo.getOnlinePlayers());
  } catch (err) {
    return internalError(res, err, 'admin/staff/online');
  }
});

router.get('/admin/players', requireMod, requireDb, async (req, res) => {
  try {
    const data = await adminRepo.listPlayers({
      q: req.query.q,
      page: req.query.page,
      limit: req.query.limit,
    });
    res.json(data);
  } catch (err) {
    return internalError(res, err, 'admin/players');
  }
});

router.get('/admin/player/:uuid', requireMod, requireDb, async (req, res) => {
  const uuid = validateUuid(res, req.params.uuid);
  if (!uuid) return;
  try {
    const profile = await adminRepo.getPlayerProfileFull(uuid);
    if (!profile) return res.status(404).json({ error: 'not_found' });
    res.json(profile);
  } catch (err) {
    return internalError(res, err, 'admin/player');
  }
});

router.get('/admin/bans', requireMod, requireDb, async (req, res) => {
  try {
    res.json(
      await bansRepo.listBans({
        page: req.query.page,
        limit: req.query.limit,
        activeOnly: req.query.active === 'true',
      })
    );
  } catch (err) {
    return internalError(res, err, 'admin/bans');
  }
});

router.post('/admin/bans', requireMod, requireDb, async (req, res) => {
  const uuid = validateUuid(res, req.body?.uuid);
  if (!uuid) return;
  if (!isNonEmptyString(req.body?.reason)) return badRequest(res, 'reason obrigatório');

  try {
    let expiresAt = null;
    const hours = parseInteger(req.body?.hours ?? req.body?.duration_hours);
    if (hours && hours > 0) {
      expiresAt = new Date(Date.now() + hours * 3600 * 1000).toISOString();
    }

    const ban = await bansRepo.createBan({
      uuid,
      username: req.body?.username || req.body?.name,
      reason: req.body.reason.trim(),
      bannedBy: req.user?.username || 'staff',
      expiresAt,
    });

    await adminRepo.insertServerEvent({
      type: 'ban',
      actorUuid: uuid,
      actorName: req.body?.username,
      detail: req.body.reason,
    });
    await adminRepo.queueAdminCommand(`kick ${req.body?.username || uuid} Banido`, req.user?.username);
    requestCache.invalidate('admin:');
    broadcast({ type: 'update' });
    res.json({ ok: true, ban });
  } catch (err) {
    return internalError(res, err, 'admin/bans');
  }
});

router.post('/admin/bans/unban', requireMod, requireDb, async (req, res) => {
  const uuid = validateUuid(res, req.body?.uuid);
  if (!uuid) return;
  try {
    const rows = await bansRepo.unban(uuid, req.user?.username);
    requestCache.invalidate('admin:');
    res.json({ ok: true, count: rows.length });
  } catch (err) {
    return internalError(res, err, 'admin/bans/unban');
  }
});

router.get('/admin/reports', requireMod, requireDb, async (req, res) => {
  try {
    res.json(
      await reportsRepo.listReports({
        status: req.query.status || 'open',
        page: req.query.page,
        limit: req.query.limit,
      })
    );
  } catch (err) {
    return internalError(res, err, 'admin/reports');
  }
});

router.post('/admin/reports/:id/close', requireMod, requireDb, async (req, res) => {
  try {
    const row = await reportsRepo.closeReport(req.params.id, req.user?.username);
    if (!row) return res.status(404).json({ error: 'not_found' });
    res.json({ ok: true, report: row });
  } catch (err) {
    return internalError(res, err, 'admin/reports/close');
  }
});

router.post('/admin/economy/update', requireAdmin, requireDb, async (req, res) => {
  const uuid = validateUuid(res, req.body?.uuid);
  if (!uuid) return;

  const body = req.body || {};
  const hasCoins =
    body.coins_delta !== undefined ||
    body.coinsDelta !== undefined ||
    body.set_coins !== undefined ||
    body.setCoins !== undefined;
  const hasEmeralds =
    body.emeralds_delta !== undefined ||
    body.emeraldsDelta !== undefined ||
    body.set_emeralds !== undefined ||
    body.setEmeralds !== undefined;

  if (!hasCoins && !hasEmeralds) {
    return badRequest(res, 'coins_delta ou emeralds_delta obrigatório');
  }

  try {
    const profile = await adminRepo.updateEconomy({
      uuid,
      username: body.username || body.name,
      coinsDelta: parseInteger(body.coins_delta ?? body.coinsDelta),
      emeraldsDelta: parseInteger(body.emeralds_delta ?? body.emeraldsDelta),
      setCoins: body.set_coins ?? body.setCoins,
      setEmeralds: body.set_emeralds ?? body.setEmeralds,
    });

    await db.query(
      'INSERT INTO logs (staff, action, target, value) VALUES ($1, $2, $3, $4)',
      [req.user?.username, 'economy_update', uuid, JSON.stringify(body).slice(0, 500)]
    ).catch(() => {});

    requestCache.invalidate('admin:');
    broadcast({ type: 'update' });
    res.json({ ok: true, player: profile });
  } catch (err) {
    return internalError(res, err, 'admin/economy/update');
  }
});

router.get('/admin/leaderboards', requireMod, requireDb, async (req, res) => {
  try {
    const boards = await requestCache.wrap('admin:leaderboards', 10000, () =>
      adminRepo.getAllLeaderboards(req.query.limit)
    );
    res.json(boards);
  } catch (err) {
    return internalError(res, err, 'admin/leaderboards');
  }
});

router.get('/admin/logs', requireMod, requireDb, async (req, res) => {
  try {
    res.json(
      await adminRepo.getAdminLogs({
        type: req.query.type,
        page: req.query.page,
        limit: req.query.limit,
      })
    );
  } catch (err) {
    return internalError(res, err, 'admin/logs');
  }
});

router.get('/admin/commands', requireMod, requireDb, async (req, res) => {
  try {
    res.json({ commands: await adminRepo.getCommandHistory(req.query.limit) });
  } catch (err) {
    return internalError(res, err, 'admin/commands');
  }
});

router.post('/admin/command', requireMod, requireDb, async (req, res) => {
  const { command } = req.body || {};
  if (!isNonEmptyString(command)) return badRequest(res, 'command inválido');

  const safe = sanitizeRemoteCommand(command);
  if (!safe) return res.status(400).json({ error: 'command not allowed' });

  if (req.user?.role === 'mod' && /^(lp|minecraft:tp)/i.test(safe)) {
    return res.status(403).json({ error: 'command_requires_admin' });
  }

  try {
    const row = await adminRepo.queueAdminCommand(safe, req.user?.username);
    await db.query(
      'INSERT INTO logs (staff, action, target, value) VALUES ($1, $2, $3, $4)',
      [req.user?.username, 'remote_command', safe, '']
    ).catch(() => {});
    broadcast({ type: 'update' });
    res.json({ ok: true, id: row.id, command: row.command, created_at: row.created_at });
  } catch (err) {
    return internalError(res, err, 'admin/command');
  }
});

router.post('/admin/reset', requireAdmin, requireDb, async (req, res) => {
  const uuid = validateUuid(res, req.body?.uuid);
  if (!uuid) return;
  try {
    await adminRepo.resetPlayerStats(uuid, req.body?.scope || 'all');
    requestCache.invalidate('admin:');
    broadcast({ type: 'update' });
    res.json({ ok: true, uuid, scope: req.body?.scope || 'all' });
  } catch (err) {
    return internalError(res, err, 'admin/reset');
  }
});

router.get('/admin/anticheat', requireMod, requireDb, async (_req, res) => {
  try {
    const [stats, timeline, suspects, alerts] = await Promise.all([
      anticheatRepo.getDashboardStats(),
      anticheatRepo.getViolationsTimeline(24),
      anticheatRepo.listSuspects(40),
      anticheatRepo.listAlerts({ page: 1, limit: 25, openOnly: true }),
    ]);
    res.json({ stats, timeline, suspects, alerts: alerts.alerts });
  } catch (err) {
    return internalError(res, err, 'admin/anticheat');
  }
});

router.get('/admin/anticheat/violations', requireMod, requireDb, async (req, res) => {
  try {
    const data = await anticheatRepo.listViolations({
      page: req.query.page,
      limit: req.query.limit,
      uuid: req.query.uuid,
      severity: req.query.severity,
    });
    res.json(data);
  } catch (err) {
    return internalError(res, err, 'admin/anticheat/violations');
  }
});

router.get('/admin/anticheat/alerts', requireMod, requireDb, async (req, res) => {
  try {
    const openOnly = req.query.open !== 'false';
    const data = await anticheatRepo.listAlerts({
      page: req.query.page,
      limit: req.query.limit,
      openOnly,
    });
    res.json(data);
  } catch (err) {
    return internalError(res, err, 'admin/anticheat/alerts');
  }
});

router.post('/admin/anticheat/alerts/:id/ack', requireMod, requireDb, async (req, res) => {
  try {
    const row = await anticheatRepo.acknowledgeAlert(req.params.id);
    if (!row) return res.status(404).json({ error: 'not_found' });
    broadcast({ type: 'update' });
    res.json({ ok: true, alert: row });
  } catch (err) {
    return internalError(res, err, 'admin/anticheat/ack');
  }
});

router.post('/admin/anticheat/suspects/:uuid/clear', requireMod, requireDb, async (req, res) => {
  const uuid = validateUuid(res, req.params.uuid);
  if (!uuid) return;
  try {
    await anticheatRepo.clearSuspect(uuid);
    broadcast({ type: 'update' });
    res.json({ ok: true, uuid });
  } catch (err) {
    return internalError(res, err, 'admin/anticheat/clear');
  }
});

router.get('/admin/anticheat/mining/heatmap', requireMod, requireDb, async (req, res) => {
  try {
    const rows = await anticheatRepo.getMiningHeatmap({
      uuid: req.query.uuid,
      world: req.query.world,
      hours: req.query.hours,
      limit: req.query.limit,
    });
    res.json({ heatmap: rows });
  } catch (err) {
    return internalError(res, err, 'admin/anticheat/mining/heatmap');
  }
});

router.get('/admin/anticheat/mining/:uuid', requireMod, requireDb, async (req, res) => {
  const uuid = validateUuid(res, req.params.uuid);
  if (!uuid) return;
  try {
    const [logs, stats] = await Promise.all([
      anticheatRepo.listMiningLogs({ uuid, limit: req.query.limit }),
      anticheatRepo.getMiningStats(uuid),
    ]);
    res.json({ uuid, stats, logs });
  } catch (err) {
    return internalError(res, err, 'admin/anticheat/mining');
  }
});

router.post('/admin/backup', requireAdmin, async (req, res) => {
  try {
    const result = await backupService.runBackup(true);
    if (!result.ok) {
      return res.status(result.error === 'database_unavailable' ? 503 : 500).json(result);
    }
    res.json(result);
  } catch (err) {
    return internalError(res, err, 'admin/backup');
  }
});

module.exports = router;
