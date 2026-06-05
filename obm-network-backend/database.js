const { Pool } = require('pg');

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

const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: resolveSsl(),
  max: 20,
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 5000,
});

pool.on('error', (err) => {
  console.error('PostgreSQL error:', err);
});

pool.connect()
  .then((client) => {
    client.release();
    console.log('✅ DB conectada');
  })
  .catch((err) => console.error('❌ Erro DB conexão:', err.message));

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

async function resolvePlayerColumns() {
  const result = await pool.query(`
    SELECT column_name
    FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'players'
    ORDER BY ordinal_position
  `);

  const existing = result.rows.map((r) => r.column_name.toLowerCase());

  playerColumns.coins = pickColumn(existing, COINS_CANDIDATES, process.env.COINS_COLUMN);
  playerColumns.emeralds = pickColumn(existing, EMERALDS_CANDIDATES, process.env.EMERALDS_COLUMN);

  if (!playerColumns.coins) {
    await pool.query('ALTER TABLE players ADD COLUMN IF NOT EXISTS coins INT NOT NULL DEFAULT 0');
    playerColumns.coins = 'coins';
    console.log('⚠️ Coluna coins criada (não existia balance/money/coins)');
  }

  if (!playerColumns.emeralds) {
    await pool.query('ALTER TABLE players ADD COLUMN IF NOT EXISTS emeralds INT NOT NULL DEFAULT 0');
    playerColumns.emeralds = 'emeralds';
    console.log('⚠️ Coluna emeralds criada');
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
  return `id, uuid, name, world, ${c} AS coins, ${e} AS emeralds, rank, created_at`;
}

async function initDb() {
  await pool.query(`
    CREATE TABLE IF NOT EXISTS players (
      id SERIAL PRIMARY KEY,
      uuid TEXT NOT NULL UNIQUE,
      name TEXT,
      world TEXT,
      coins INT NOT NULL DEFAULT 0,
      emeralds INT NOT NULL DEFAULT 0,
      rank TEXT NOT NULL DEFAULT 'bronze',
      created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );
  `);

  await pool.query(`
    CREATE TABLE IF NOT EXISTS logs (
      id SERIAL PRIMARY KEY,
      staff TEXT,
      action TEXT,
      target TEXT,
      value TEXT,
      created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );
  `);

  await pool.query(`
    CREATE TABLE IF NOT EXISTS commands (
      id SERIAL PRIMARY KEY,
      command TEXT NOT NULL,
      executed BOOLEAN NOT NULL DEFAULT false,
      created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );
  `);

  await pool.query(`
    CREATE INDEX IF NOT EXISTS idx_commands_pending
    ON commands (executed)
    WHERE executed = false;
  `);

  await pool.query('ALTER TABLE players ADD COLUMN IF NOT EXISTS kills INT NOT NULL DEFAULT 0');
  await pool.query('ALTER TABLE players ADD COLUMN IF NOT EXISTS playtime INT NOT NULL DEFAULT 0');
  await pool.query('ALTER TABLE players ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT NOW()');
  await pool.query(`ALTER TABLE players ADD COLUMN IF NOT EXISTS rank VARCHAR(50) DEFAULT 'bronze'`);

  await resolvePlayerColumns();
  console.log('✅ Database COMPLETA pronta');
}

module.exports = pool;
module.exports.initDb = initDb;
module.exports.resolvePlayerColumns = resolvePlayerColumns;
module.exports.getPlayerColumns = () => ({ ...playerColumns });
module.exports.col = col;
module.exports.sqlPlayerSelect = sqlPlayerSelect;
