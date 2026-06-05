module.exports = {
  enabled: process.env.BACKUP_ENABLED !== 'false',
  intervalSeconds: Math.max(60, parseInt(process.env.BACKUP_INTERVAL_SECONDS || '300', 10)),
};
