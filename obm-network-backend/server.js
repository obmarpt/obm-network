const http = require('http');
const express = require('express');
const cookieParser = require('cookie-parser');
const pool = require('./database');
const initDb = pool.initDb;
const routes = require('./routes');
const { initWebSocket } = require('./ws');
const backupService = require('./services/backupService');
const statsHistoryService = require('./services/statsHistoryService');
const { startExpirationScheduler } = require('./services/entitlementExpirationService');

const rateLimit = require('express-rate-limit');

const path = require('path');
const { handleStripeWebhook } = require('./routes/stripeWebhook');

const app = express();
const server = http.createServer(app);
const PORT = process.env.PORT || 3000;

app.set('trust proxy', 1);

app.post(
  '/api/stripe/webhook',
  express.raw({ type: 'application/json' }),
  handleStripeWebhook
);

app.use(express.json({ limit: '256kb' }));
app.use(cookieParser());

app.get('/player/:id', (_req, res) => {
  res.sendFile(path.join(__dirname, 'public', 'site', 'profile.html'));
});

app.use('/admin', express.static(path.join(__dirname, 'public')));
app.use(express.static(path.join(__dirname, 'public', 'site')));

const healthLimiter = rateLimit({
  windowMs: 60_000,
  max: 120,
  standardHeaders: true,
  legacyHeaders: false,
});

app.get('/health', healthLimiter, async (req, res) => {
  try {
    const dbOk = await pool.ping();
    const minimal = process.env.NODE_ENV === 'production';
    res.json(minimal
        ? { ok: dbOk, service: 'obm-backend' }
        : {
          ok: dbOk,
          service: 'obm-backend',
          databaseAvailable: pool.isDatabaseAvailable(),
          db: dbOk,
        });
  } catch (err) {
    console.error('Erro (health):', err);
    res.status(503).json({
      ok: false,
      error: process.env.NODE_ENV === 'production' ? 'database_unavailable' : err.message,
    });
  }
});

app.use(routes);

app.use((err, req, res, next) => {
  console.error(`Erro não tratado [${req.method} ${req.path}]:`, err);
  if (res.headersSent) {
    return next(err);
  }
  res.status(500).json({ error: 'internal error' });
});

async function start() {
  try {
    await initDb();
    backupService.startScheduledBackups();
    statsHistoryService.startSchedulers();
    initWebSocket(server);
    startExpirationScheduler(60_000);

    server.listen(PORT, () => {
      console.log(`🚀 Server running on port ${PORT}`);
      console.log(`✅ DB colunas: ${JSON.stringify(pool.getPlayerColumns())}`);

      if (process.env.DISCORD_WEBHOOK_URL) {
        console.log('✅ Discord webhooks configurados');
      }

      if (process.env.JWT_SECRET && process.env.ADMIN_USER && process.env.ADMIN_PASS) {
        console.log('✅ Auth JWT configurado');
      } else {
        console.log('❌ Login: variáveis ADMIN_USER, ADMIN_PASS ou JWT_SECRET em falta');
      }

      if (process.env.PLUGIN_API_KEY) {
        console.log('✅ Plugin API key configurada');
      } else {
        console.log('❌ PLUGIN_API_KEY em falta — plugin Minecraft não autentica');
      }

      const siteUrl = process.env.SITE_URL || '';
      if (process.env.NODE_ENV === 'production' && (!siteUrl || siteUrl.includes('localhost'))) {
        console.log('❌ SITE_URL em falta ou localhost — Stripe return URLs inválidos');
      } else if (siteUrl) {
        console.log(`✅ SITE_URL: ${siteUrl}`);
      }

      if (!process.env.STRIPE_SECRET_KEY || !process.env.STRIPE_WEBHOOK_SECRET) {
        console.log('⚠️ Stripe não configurado — loja em modo limitado');
      }
    });
  } catch (err) {
    console.error('❌ Falha ao iniciar:', err.message);
    process.exit(1);
  }
}

start();
