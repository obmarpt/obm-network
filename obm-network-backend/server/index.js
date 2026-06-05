require('dotenv').config();

const express = require('express');
const { pluginAuth } = require('./middleware/pluginAuth');
const topRoutes = require('./routes/top');
const statsRoutes = require('./routes/stats');
const logRoutes = require('./routes/log');
const playerRoutes = require('./routes/player');
const moneyRoutes = require('./routes/money');
const { query } = require('./db');

const { requestLog } = require('./middleware/requestLog');

const app = express();
const PORT = process.env.PORT || 3000;
const JSON_LIMIT = process.env.JSON_LIMIT || '64kb';

app.disable('x-powered-by');
app.use(express.json({ limit: JSON_LIMIT }));
app.use(requestLog);

app.get('/health', async (_req, res) => {
  try {
    await query('SELECT 1');
    res.json({ ok: true });
  } catch (err) {
    res.status(503).json({ ok: false, error: err.message });
  }
});

app.use(pluginAuth);

app.use('/top', topRoutes);
app.use('/stats', statsRoutes);
app.use('/log', logRoutes);
app.use('/player', playerRoutes);
app.use('/money', moneyRoutes);

app.use((err, _req, res, _next) => {
  console.error('[API]', err.message);
  res.status(500).json({ error: 'internal_error' });
});

app.listen(PORT, () => {
  console.log(`[OBM Backend] listening on :${PORT}`);
});
