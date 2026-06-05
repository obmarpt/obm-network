const REFRESH_INTERVAL = 10_000;

const $ = (id) => document.getElementById(id);

const loginView = $('login-view');
const appView = $('app-view');
const loginUser = $('loginUser');
const loginPass = $('loginPass');
const loginBtn = $('loginBtn');
const loginError = $('loginError');
const logoutBtn = $('logoutBtn');
const statusDot = $('statusDot');
const statusText = $('statusText');
const playerCount = $('playerCount');
const tableContainer = $('tableContainer');
const toastContainer = $('toastContainer');
const manualCommandInput = $('manualCommand');
const broadcastInput = $('broadcastMsg');
const btnManualCommand = $('btnManualCommand');
const btnBroadcast = $('btnBroadcast');
const statTotalPlayers = $('statTotalPlayers');
const statTotalCoins = $('statTotalCoins');
const statAvgCoins = $('statAvgCoins');
const topCoinsEl = $('topCoins');
const topEmeraldsEl = $('topEmeralds');

let authenticated = false;
let refreshTimer = null;
let ws = null;
let playersCache = [];

const ALLOWED_CMD = [
  /^say\s+\S.+$/i,
  /^broadcast\s+\S.+$/i,
];

function isAllowedCommand(cmd) {
  const t = cmd.trim();
  if (!t || t.length > 256 || /[;`|&<>$\\]/.test(t)) return false;
  return ALLOWED_CMD.some((re) => re.test(t));
}

async function api(path, options = {}) {
  const res = await fetch(path, {
    ...options,
    credentials: 'include',
    headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
  });

  if (res.status === 401 || res.status === 403) {
    await logout();
    throw new Error('Sessão expirada');
  }

  if (!res.ok) {
    const data = await res.json().catch(() => ({}));
    throw new Error(data.error || `HTTP ${res.status}`);
  }

  if (res.status === 204 || res.headers.get('content-length') === '0') {
    return null;
  }

  const text = await res.text();
  return text ? JSON.parse(text) : null;
}

function showLogin(show) {
  loginView.classList.toggle('hidden', !show);
  appView.classList.toggle('hidden', show);
}

async function login() {
  loginError.textContent = '';
  loginBtn.disabled = true;

  try {
    await fetch('/login', {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username: loginUser.value.trim(),
        password: loginPass.value,
      }),
    }).then(async (res) => {
      const body = await res.json().catch(() => ({}));
      if (!res.ok) throw new Error(body.error || 'Login falhou');
      return body;
    });

    authenticated = true;
    loginPass.value = '';
    showLogin(false);
    connectWebSocket();
    await refreshAll();
  } catch (err) {
    loginError.textContent = err.message;
  } finally {
    loginBtn.disabled = false;
  }
}

async function logout() {
  authenticated = false;
  try {
    await fetch('/logout', { method: 'POST', credentials: 'include' });
  } catch { /* ignore */ }
  if (ws) { ws.close(); ws = null; }
  if (refreshTimer) { clearInterval(refreshTimer); refreshTimer = null; }
  showLogin(true);
}

function connectWebSocket() {
  if (ws) ws.close();

  const proto = location.protocol === 'https:' ? 'wss:' : 'ws:';
  ws = new WebSocket(`${proto}//${location.host}/ws`);

  ws.onmessage = (event) => {
    try {
      const msg = JSON.parse(event.data);
      if (msg.type === 'update') {
        loadPlayers();
        loadStats();
      }
    } catch { /* ignore */ }
  };

  ws.onclose = (ev) => {
    if (authenticated && ev.code !== 4401) {
      setTimeout(connectWebSocket, 3000);
    }
  };
}

function escapeHtml(str) {
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

function formatRank(rank) {
  const safe = (rank || 'bronze').toLowerCase();
  const known = ['bronze', 'silver', 'gold', 'platinum'];
  const cls = known.includes(safe) ? `rank-${safe}` : 'rank-default';
  return `<span class="rank ${cls}">${safe}</span>`;
}

function formatNumber(n) {
  return Number(n ?? 0).toLocaleString('pt-PT');
}

function setStatus(state, message) {
  statusDot.className = `dot ${state}`;
  statusText.textContent = message;
}

function showToast(message, type = 'success') {
  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  toast.textContent = message;
  toastContainer.appendChild(toast);
  setTimeout(() => toast.remove(), 4000);
}

function parsePositiveAmount(raw) {
  const n = Number(raw);
  if (!Number.isFinite(n) || !Number.isInteger(n) || n <= 0) return null;
  return n;
}

function renderTopList(el, items, field) {
  if (!items.length) {
    el.innerHTML = '<li>Nenhum registo</li>';
    return;
  }
  el.innerHTML = items.map((p) =>
    `<li><span>${escapeHtml(p.name || '—')}</span><strong class="coins">${formatNumber(p[field])}</strong></li>`
  ).join('');
}

async function giveCoins(uuid, amount, name) {
  await api('/money/add', {
    method: 'POST',
    body: JSON.stringify({ uuid, amount, name }),
  });
}

async function executeCommand(cmd) {
  const trimmed = cmd?.trim();
  if (!trimmed) throw new Error('Comando vazio');
  if (!isAllowedCommand(trimmed)) throw new Error('Comando não permitido');
  await api('/command', {
    method: 'POST',
    body: JSON.stringify({ command: trimmed }),
  });
}

async function handleGiveCoins(uuid, name) {
  const raw = prompt(`💰 Quantas coins dar a ${name}?`);
  if (raw === null) return;
  const amount = parsePositiveAmount(raw);
  if (amount === null) return showToast('❌ Valor inválido (inteiro > 0)', 'error');
  try {
    await giveCoins(uuid, amount, name);
    showToast(`✅ +${amount} coins → ${name}`);
    await loadPlayers();
    await loadStats();
  } catch (err) {
    showToast(`❌ Erro: ${err.message}`, 'error');
  }
}

async function handleRemoveCoins(uuid, name) {
  const raw = prompt(`💸 Quantas coins remover de ${name}?`);
  if (raw === null) return;
  const amount = parsePositiveAmount(raw);
  if (amount === null) return showToast('❌ Valor inválido (inteiro > 0)', 'error');
  try {
    await giveCoins(uuid, -amount, name);
    showToast(`✅ -${amount} coins → ${name}`);
    await loadPlayers();
    await loadStats();
  } catch (err) {
    showToast(`❌ Erro: ${err.message}`, 'error');
  }
}

async function handleManualCommand() {
  const cmd = manualCommandInput.value.trim();
  if (!cmd) return showToast('❌ Comando vazio', 'error');
  if (!isAllowedCommand(cmd)) {
    return showToast('❌ Apenas say ou broadcast são permitidos', 'error');
  }
  try {
    await executeCommand(cmd);
    showToast('✅ Comando enviado');
    manualCommandInput.value = '';
  } catch (err) {
    showToast(`❌ Erro: ${err.message}`, 'error');
  }
}

async function handleBroadcast() {
  const msg = broadcastInput.value.trim();
  if (!msg) return showToast('❌ Mensagem vazia', 'error');
  try {
    await executeCommand(`broadcast ${msg}`);
    showToast('✅ Broadcast enviado');
    broadcastInput.value = '';
  } catch (err) {
    showToast(`❌ Erro: ${err.message}`, 'error');
  }
}

function renderTable(players) {
  if (!players.length) {
    tableContainer.innerHTML = '<p class="empty">Nenhum player registado.</p>';
    return;
  }

  const rows = players.map((p) => {
    const name = p.name || 'Unknown';
    const uuid = p.uuid || '';
    return `
      <tr>
        <td>${escapeHtml(name)}</td>
        <td><code>${escapeHtml(uuid || '—')}</code></td>
        <td>${escapeHtml(p.world || '—')}</td>
        <td class="coins">${formatNumber(p.coins)}</td>
        <td class="emeralds">${formatNumber(p.emeralds)}</td>
        <td>${formatRank(p.rank)}</td>
        <td>
          <div class="actions">
            <button class="btn btn-give"   data-action="give"   data-uuid="${escapeHtml(uuid)}" data-name="${escapeHtml(name)}">💰 Coins</button>
            <button class="btn btn-remove" data-action="remove" data-uuid="${escapeHtml(uuid)}" data-name="${escapeHtml(name)}">💸 Remover</button>
          </div>
        </td>
      </tr>
    `;
  }).join('');

  tableContainer.innerHTML = `
    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Nome</th><th>UUID</th><th>World</th>
            <th>Coins</th><th>Emeralds</th><th>Rank</th><th>Ações</th>
          </tr>
        </thead>
        <tbody>${rows}</tbody>
      </table>
    </div>
  `;
}

async function loadPlayers() {
  try {
    playersCache = await api('/players');
    playerCount.textContent = `${playersCache.length} registo${playersCache.length !== 1 ? 's' : ''}`;
    renderTable(playersCache);
    const now = new Date().toLocaleTimeString('pt-PT');
    setStatus('online', `Atualizado às ${now} · live + 10s`);
  } catch (err) {
    if (authenticated) {
      tableContainer.innerHTML = `<p class="error-msg">${escapeHtml(err.message)}</p>`;
      setStatus('error', 'Erro de ligação');
    }
  }
}

async function loadStats() {
  try {
    const stats = await api('/stats/global');
    statTotalPlayers.textContent = formatNumber(stats.totalPlayers);
    statTotalCoins.textContent = formatNumber(stats.totalCoins);
    statAvgCoins.textContent = formatNumber(stats.avgCoins);

    const [topCoins, topEmeralds] = await Promise.all([
      api('/top/coins'),
      api('/top/emeralds'),
    ]);
    renderTopList(topCoinsEl, topCoins, 'coins');
    renderTopList(topEmeraldsEl, topEmeralds, 'emeralds');
  } catch (err) {
    console.error('Stats error:', err);
  }
}

async function refreshAll() {
  await Promise.all([loadPlayers(), loadStats()]);
  if (!refreshTimer) {
    refreshTimer = setInterval(refreshAll, REFRESH_INTERVAL);
  }
}

loginBtn.addEventListener('click', login);
logoutBtn.addEventListener('click', logout);
loginPass.addEventListener('keydown', (e) => { if (e.key === 'Enter') login(); });

tableContainer.addEventListener('click', async (e) => {
  const btn = e.target.closest('[data-action]');
  if (!btn) return;
  const { action, uuid, name } = btn.dataset;
  btn.disabled = true;
  try {
    if (action === 'give') await handleGiveCoins(uuid, name);
    else if (action === 'remove') await handleRemoveCoins(uuid, name);
  } finally {
    btn.disabled = false;
  }
});

btnManualCommand.addEventListener('click', handleManualCommand);
btnBroadcast.addEventListener('click', handleBroadcast);
manualCommandInput.addEventListener('keydown', (e) => { if (e.key === 'Enter') handleManualCommand(); });
broadcastInput.addEventListener('keydown', (e) => { if (e.key === 'Enter') handleBroadcast(); });

async function tryRestoreSession() {
  try {
    await api('/players');
    authenticated = true;
    showLogin(false);
    connectWebSocket();
    await refreshAll();
  } catch {
    showLogin(true);
  }
}

tryRestoreSession();
