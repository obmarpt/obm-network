const db = require('../database');

/** Bloqueia writes quando PostgreSQL está em baixo (plugin recebe 503 e usa cache). */
function dbWriteGuard(req, res, next) {
  const writePaths = ['/player/save', '/stats/sync', '/money/add', '/playerJoin', '/admin/economy/update'];
  const path = req.originalUrl || req.url || '';
  const isWrite =
    req.method === 'POST' &&
    (writePaths.some((p) => path.endsWith(p) || path.includes(p)) || path.includes('/save'));

  if (isWrite && !db.isDatabaseAvailable()) {
    return res.status(503).json({
      error: 'database_unavailable',
      databaseAvailable: false,
    });
  }
  next();
}

module.exports = { dbWriteGuard };
