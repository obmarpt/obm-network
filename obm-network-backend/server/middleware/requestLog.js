/**
 * Logs estruturados (só com LOG_LEVEL=debug ou NODE_ENV=development).
 */
function requestLog(req, res, next) {
  const verbose =
      process.env.LOG_LEVEL === 'debug' || process.env.NODE_ENV === 'development';
  if (!verbose) {
    return next();
  }

  const start = Date.now();
  res.on('finish', () => {
    const ms = Date.now() - start;
    if (res.statusCode >= 400) {
      console.warn(`[API] ${req.method} ${req.originalUrl} ${res.statusCode} ${ms}ms`);
    }
  });
  next();
}

module.exports = { requestLog };
