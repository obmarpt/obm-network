const SLOW_MS = Math.max(10, parseInt(process.env.DB_SLOW_QUERY_MS || '100', 10));

let databaseAvailable = true;
let lastError = null;
let lastOkAt = Date.now();

function setAvailable(ok, err) {
  const was = databaseAvailable;
  databaseAvailable = ok;
  if (ok) {
    lastOkAt = Date.now();
    lastError = null;
    if (!was) {
      console.log('[DB] Conexão restaurada');
    }
  } else if (err) {
    lastError = err;
    if (was) {
      console.error('[DB] Base de dados indisponível:', err);
    }
  }
}

function wrapPool(pool) {
  async function query(text, params) {
    const start = Date.now();
    try {
      const result = await pool.query(text, params);
      const ms = Date.now() - start;
      if (ms >= SLOW_MS) {
        console.log(`[DB] Query took ${ms}ms`);
      }
      setAvailable(true);
      return result;
    } catch (err) {
      const ms = Date.now() - start;
      console.error(`[DB] Query failed (${ms}ms): ${err.message}`);
      setAvailable(false, err.message);
      throw err;
    }
  }

  async function ping() {
    try {
      await query('SELECT 1');
      return true;
    } catch {
      return false;
    }
  }

  return { query, ping, getPool: () => pool };
}

module.exports = {
  wrapPool,
  isDatabaseAvailable: () => databaseAvailable,
  getLastDbError: () => lastError,
  getLastOkAt: () => lastOkAt,
  setAvailable,
};
