const { WebSocketServer } = require('ws');
const { verifyToken, extractToken } = require('./auth');

const clients = new Set();
let wss;

function initWebSocket(server) {
  wss = new WebSocketServer({ server, path: '/ws' });

  wss.on('connection', (ws, req) => {
    const mockReq = {
      cookies: parseCookies(req.headers.cookie),
      headers: req.headers,
      query: parseQuery(req.url),
    };

    const token = extractToken(mockReq);
    const user = verifyToken(token);
    if (!user) {
      ws.close(4401, 'Unauthorized');
      return;
    }

    clients.add(ws);
    ws.on('close', () => clients.delete(ws));
    ws.on('error', () => clients.delete(ws));
  });

  console.log('✅ WebSocket ativo em /ws (JWT obrigatório)');
}

function parseCookies(header) {
  const out = {};
  if (!header) return out;
  for (const part of header.split(';')) {
    const [k, ...v] = part.trim().split('=');
    if (k) out[k] = decodeURIComponent(v.join('='));
  }
  return out;
}

function parseQuery(url) {
  const out = {};
  if (!url) return out;
  const i = url.indexOf('?');
  if (i < 0) return out;
  for (const pair of url.slice(i + 1).split('&')) {
    const [k, v] = pair.split('=');
    if (k) out[decodeURIComponent(k)] = decodeURIComponent(v || '');
  }
  return out;
}

function broadcast(data) {
  if (!wss) return;

  const raw = JSON.stringify(data);
  for (const ws of clients) {
    if (ws.readyState === 1) {
      ws.send(raw);
    }
  }
}

module.exports = { initWebSocket, broadcast };
