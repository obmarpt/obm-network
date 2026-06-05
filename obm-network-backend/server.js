const http = require('http');
const express = require('express');
const cookieParser = require('cookie-parser');
const pool = require('./database');
const initDb = pool.initDb;
const routes = require('./routes');
const { initWebSocket } = require('./ws');

const app = express();
const server = http.createServer(app);
const PORT = process.env.PORT || 3000;

app.set('trust proxy', 1);
app.use(express.json());
app.use(cookieParser());
app.use(express.static('public'));

app.get('/health', async (req, res) => {
  try {
    await pool.query('SELECT 1');
    res.json({ ok: true, service: 'obm-backend' });
  } catch (err) {
    console.error('Erro (health):', err);
    res.status(503).json({ ok: false, error: 'internal error' });
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
    initWebSocket(server);

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
    });
  } catch (err) {
    console.error('❌ Falha ao iniciar:', err.message);
    process.exit(1);
  }
}

start();
