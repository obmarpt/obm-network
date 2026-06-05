const fs = require('fs');
const path = require('path');
const adminRepo = require('../db/adminRepository');
const db = require('../database');

const DEFAULT_DIR = path.join(__dirname, '..', 'backups');

function getConfig() {
  return {
    enabled: process.env.BACKUP_ENABLED !== 'false',
    intervalSeconds: Math.max(60, parseInt(process.env.BACKUP_INTERVAL_SECONDS || '300', 10)),
    directory: process.env.BACKUP_DIR || DEFAULT_DIR,
    maxFiles: Math.max(5, parseInt(process.env.BACKUP_MAX_FILES || '48', 10)),
    format: process.env.BACKUP_FORMAT === 'sql' ? 'sql' : 'json',
  };
}

function ensureDir(dir) {
  if (!fs.existsSync(dir)) {
    fs.mkdirSync(dir, { recursive: true });
  }
}

function pruneOldBackups(dir, maxFiles) {
  if (!fs.existsSync(dir)) return;
  const files = fs
    .readdirSync(dir)
    .filter((f) => f.startsWith('obm-backup-'))
    .map((f) => ({ f, t: fs.statSync(path.join(dir, f)).mtimeMs }))
    .sort((a, b) => b.t - a.t);
  for (let i = maxFiles; i < files.length; i++) {
    try {
      fs.unlinkSync(path.join(dir, files[i].f));
    } catch (err) {
      console.warn('[Backup] prune:', err.message);
    }
  }
}

async function runBackup(manual = false) {
  const cfg = getConfig();
  if (!cfg.enabled && !manual) {
    return { skipped: true, reason: 'disabled' };
  }
  if (!db.isDatabaseAvailable()) {
    console.warn('[Backup] Ignorado — DB indisponível');
    return { ok: false, error: 'database_unavailable' };
  }

  const started = Date.now();
  try {
    const payload = await adminRepo.exportCriticalData();
    ensureDir(cfg.directory);
    const stamp = new Date().toISOString().replace(/[:.]/g, '-');
    const fileName = `obm-backup-${stamp}.${cfg.format === 'sql' ? 'json' : 'json'}`;
    const filePath = path.join(cfg.directory, fileName);
    fs.writeFileSync(filePath, JSON.stringify(payload, null, 2), 'utf8');
    pruneOldBackups(cfg.directory, cfg.maxFiles);
    const ms = Date.now() - started;
    console.log(`[Backup] OK ${fileName} (${ms}ms, ${payload.players?.length || 0} players)`);
    return { ok: true, file: fileName, path: filePath, ms, players: payload.players?.length || 0 };
  } catch (err) {
    console.error('[Backup] Falhou:', err.message);
    return { ok: false, error: err.message };
  }
}

let timer = null;

function startScheduledBackups() {
  const cfg = getConfig();
  if (!cfg.enabled) {
    console.log('[Backup] Desactivado (BACKUP_ENABLED=false)');
    return;
  }
  if (timer) {
    clearInterval(timer);
  }
  const intervalMs = cfg.intervalSeconds * 1000;
  timer = setInterval(() => {
    runBackup(false).catch((err) => console.error('[Backup]', err.message));
  }, intervalMs);
  console.log(`[Backup] Agendado a cada ${cfg.intervalSeconds}s → ${cfg.directory}`);
  setTimeout(() => runBackup(false).catch(() => {}), 15000);
}

function stopScheduledBackups() {
  if (timer) {
    clearInterval(timer);
    timer = null;
  }
}

module.exports = {
  getConfig,
  runBackup,
  startScheduledBackups,
  stopScheduledBackups,
};
