const fs = require('fs');
const path = require('path');
const { Pool } = require('pg');
const { wrapPool, isDatabaseAvailable, getLastDbError, ping } = require('./db/dbMonitor');

const COINS_CANDIDATES = ['coins', 'balance', 'money'];
const EMERALDS_CANDIDATES = ['emeralds', 'emerald'];

function resolveSsl() {
  const url = process.env.DATABASE_URL || '';
  if (url.includes('localhost') || url.includes('127.0.0.1')) {
    return false;
  }
  if (process.env.DATABASE_SSL === 'false') {
    return false;
  }
  return { rejectUnauthorized: true };
}

const rawPool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: resolveSsl(),
  max: parseInt(process.env.DB_POOL_MAX || '20', 10),
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: parseInt(process.env.DB_CONNECT_TIMEOUT_MS || '5000', 10),
});

rawPool.on('error', (err) => {
  console.error('[DB] Pool error:', err.message);
});

const { query, getPool } = wrapPool(rawPool);

rawPool
  .connect()
  .then((client) => {
    client.release();
    console.log('✅ DB conectada');
  })
  .catch((err) => console.error('❌ Erro DB conexão:', err.message));

setInterval(() => {
  ping().catch(() => {});
}, Math.max(15000, parseInt(process.env.DB_PING_INTERVAL_MS || '30000', 10)));

const playerColumns = { coins: null, emeralds: null };

function pickColumn(existing, candidates, envOverride) {
  if (envOverride) {
    const key = envOverride.toLowerCase();
    if (!existing.includes(key)) {
      throw new Error(`Coluna configurada "${envOverride}" não existe na tabela players`);
    }
    return key;
  }
  return candidates.find((c) => existing.includes(c)) || null;
}

async function getPlayerTableColumns() {
  const result = await query(`
    SELECT column_name
    FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'players'
    ORDER BY ordinal_position
  `);
  return result.rows.map((r) => r.column_name.toLowerCase());
}

async function resolvePlayerColumns() {
  const existing = await getPlayerTableColumns();
  if (existing.length === 0) {
    playerColumns.coins = 'coins';
    playerColumns.emeralds = 'emeralds';
    return { ...playerColumns, allColumns: existing };
  }

  playerColumns.coins = pickColumn(existing, COINS_CANDIDATES, process.env.COINS_COLUMN);
  playerColumns.emeralds = pickColumn(existing, EMERALDS_CANDIDATES, process.env.EMERALDS_COLUMN);

  if (!playerColumns.coins) {
    await query('ALTER TABLE players ADD COLUMN IF NOT EXISTS coins INT NOT NULL DEFAULT 0');
    playerColumns.coins = 'coins';
  }
  if (!playerColumns.emeralds) {
    await query('ALTER TABLE players ADD COLUMN IF NOT EXISTS emeralds INT NOT NULL DEFAULT 0');
    playerColumns.emeralds = 'emeralds';
  }

  console.log(`✅ DB colunas: coins→${playerColumns.coins}, emeralds→${playerColumns.emeralds}`);
  return { ...playerColumns, allColumns: existing };
}

function col(field) {
  const name = playerColumns[field];
  if (!name || !/^[a-z_][a-z0-9_]*$/i.test(name)) {
    throw new Error(`Coluna inválida: ${field}`);
  }
  return name;
}

function sqlPlayerSelect() {
  const c = col('coins');
  const e = col('emeralds');
  const cols = getPlayerTableColumnsSync();
  const nameCol = cols.includes('username') ? 'username' : 'name';
  const idPart = cols.includes('id') ? 'id, ' : '';
  return `${idPart}uuid, ${nameCol} AS name, world, ${c} AS coins, ${e} AS emeralds, rank, created_at`;
}

let cachedCols = [];

function getPlayerTableColumnsSync() {
  return cachedCols;
}

async function runMigrationFile(fileName) {
  const file = path.join(__dirname, 'migrations', fileName);
  if (!fs.existsSync(file)) {
    return;
  }
  const sql = fs.readFileSync(file, 'utf8');
  const statements = sql
    .split(';')
    .map((s) => s.trim())
    .filter((s) => s.length > 0 && !s.startsWith('--'));
  for (const statement of statements) {
    try {
      await query(statement);
    } catch (err) {
      if (!/already exists/i.test(err.message)) {
        console.warn('[DB migration]', err.message.slice(0, 120));
      }
    }
  }
}

async function migrateLegacyColumns() {
  const existing = await getPlayerTableColumns();
  cachedCols = existing;

  if (existing.includes('name') && !existing.includes('username')) {
    await query('ALTER TABLE players ADD COLUMN IF NOT EXISTS username VARCHAR(32)');
    await query(`UPDATE players SET username = COALESCE(NULLIF(TRIM(name), ''), 'Unknown') WHERE username IS NULL`);
  }
  await query('ALTER TABLE players ADD COLUMN IF NOT EXISTS smp_level INT NOT NULL DEFAULT 1');
  await query('ALTER TABLE players ADD COLUMN IF NOT EXISTS hc_level INT NOT NULL DEFAULT 1');
  await query('ALTER TABLE players ADD COLUMN IF NOT EXISTS global_level INT NOT NULL DEFAULT 1');
  await query('ALTER TABLE players ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT NOW()');
  await query('ALTER TABLE players ADD COLUMN IF NOT EXISTS world VARCHAR(64)');
  await query(`ALTER TABLE players ADD COLUMN IF NOT EXISTS rank VARCHAR(50) DEFAULT 'bronze'`);

  if (existing.includes('kills') || existing.includes('playtime')) {
    await query(`
      INSERT INTO smp_stats (uuid, kills, playtime, last_seen)
      SELECT p.uuid::varchar(36),
             COALESCE(p.kills, 0),
             COALESCE(p.playtime, 0),
             NOW()
      FROM players p
      WHERE p.uuid IS NOT NULL
      ON CONFLICT (uuid) DO UPDATE SET
        kills = GREATEST(smp_stats.kills, EXCLUDED.kills),
        playtime = GREATEST(smp_stats.playtime, EXCLUDED.playtime)
    `).catch((err) => console.warn('[DB] legacy smp migrate:', err.message));
  }
}

async function initDb() {
  await runMigrationFile('000_simple_essential.sql');
  await runMigrationFile('001_normalized_schema.sql');
  await runMigrationFile('002_performance_indexes.sql');
  await runMigrationFile('003_hc_wins.sql');
  await runMigrationFile('004_admin_panel.sql');
  await runMigrationFile('005_enterprise_panel.sql');
  await runMigrationFile('006_grim_anticheat.sql');
  await runMigrationFile('007_performance_indexes.sql');
  await runMigrationFile('008_tierspace_arenas.sql');
  await runMigrationFile('009_security_logs.sql');
  await runMigrationFile('010_mining_logs.sql');
  await runMigrationFile('011_web_store.sql');
  await runMigrationFile('012_web_support.sql');
  await runMigrationFile('013_monetization.sql');
  await runMigrationFile('014_promo_starter.sql');
  await runMigrationFile('015_player_profile.sql');
  await runMigrationFile('016_production_hardening.sql');
  await runMigrationFile('017_leaderboard_indexes.sql');

  try {
    const storeRepo = require('./db/storeRepository');
    await storeRepo.seedProducts();
    const promoRepo = require('./db/promoRepository');
    await promoRepo.seedPromos();
    const profileRepo = require('./db/playerProfileRepository');
    await profileRepo.ensureDefaultSeason();
  } catch (err) {
    console.warn('[DB] store seed:', err.message);
  }

  const existing = await getPlayerTableColumns();
  if (existing.length === 0) {
    await query(`
      CREATE TABLE players (
        uuid VARCHAR(36) PRIMARY KEY,
        username VARCHAR(32) NOT NULL DEFAULT 'Unknown',
        coins INT NOT NULL DEFAULT 0,
        emeralds INT NOT NULL DEFAULT 0,
        smp_level INT NOT NULL DEFAULT 1,
        hc_level INT NOT NULL DEFAULT 1,
        global_level INT NOT NULL DEFAULT 1,
        rank VARCHAR(32) NOT NULL DEFAULT 'bronze',
        world VARCHAR(64),
        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
        updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
      );
    `);
  }

  await query(`
    CREATE TABLE IF NOT EXISTS logs (
      id SERIAL PRIMARY KEY,
      staff TEXT,
      action TEXT,
      target TEXT,
      value TEXT,
      created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );
  `);

  await query(`
    CREATE TABLE IF NOT EXISTS commands (
      id SERIAL PRIMARY KEY,
      command TEXT NOT NULL,
      executed BOOLEAN NOT NULL DEFAULT false,
      created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );
  `);

  await query(`
    CREATE INDEX IF NOT EXISTS idx_commands_pending
    ON commands (executed)
    WHERE executed = false;
  `);

  await migrateLegacyColumns();
  await resolvePlayerColumns();
  cachedCols = await getPlayerTableColumns();
  await ping();
  console.log('✅ Database normalizada pronta');
}

const pool = rawPool;
pool.query = query;

module.exports = pool;
module.exports.query = query;
module.exports.getPool = getPool;
module.exports.ping = ping;
module.exports.isDatabaseAvailable = isDatabaseAvailable;
module.exports.getLastDbError = getLastDbError;
module.exports.initDb = initDb;
module.exports.resolvePlayerColumns = resolvePlayerColumns;
module.exports.getPlayerColumns = () => ({ ...playerColumns });
module.exports.col = col;
module.exports.sqlPlayerSelect = sqlPlayerSelect;
