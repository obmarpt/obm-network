const REFRESH_MS = 12_000;
const charts = {};

const state = {
  authenticated: false,
  role: 'admin',
  page: 'dashboard',
  playersPage: 1,
  playersQuery: '',
  logsPage: 1,
  logsType: 'all',
  bansPage: 1,
  acViolPage: 1,
  acViolSeverity: '',
  selectedUuid: null,
  refreshTimer: null,
  ws: null,
};

const $ = (id) => document.getElementById(id);

async function api(path, options = {}) {
  const res = await fetch(path, {
    ...options,
    credentials: 'include',
    headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
  });
  const text = await res.text();
  let data = null;
  if (text) {
    try { data = JSON.parse(text); } catch { data = { raw: text }; }
  }
  if (res.status === 401 || res.status === 403) {
    if (data?.error === 'insufficient_permissions' || data?.error === 'admin_required') {
      showToast('Sem permissão para esta acção', 'error');
      throw new Error('forbidden');
    }
    await logout();
    throw new Error('Sessão expirada');
  }
  if (!res.ok) throw new Error(data?.error || `HTTP ${res.status}`);
  return data;
}

const esc = (s) => String(s ?? '').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
const fmt = (n) => Number(n ?? 0).toLocaleString('pt-PT');
const fmtDate = (d) => { try { return new Date(d).toLocaleString('pt-PT'); } catch { return '—'; } };

function showToast(msg, type = 'success') {
  const el = document.createElement('div');
  el.className = `toast toast-${type}`;
  el.textContent = msg;
  $('toastContainer').appendChild(el);
  setTimeout(() => el.remove(), 4500);
}

function setStatus(kind, text) {
  $('statusDot').className = `dot ${kind}`;
  $('statusText').textContent = text;
}

function isAdmin() { return state.role === 'admin'; }

function applyRoleUi() {
  $('roleBadge').textContent = state.role.toUpperCase();
  $('roleBadge').className = `role-badge role-${state.role}`;
  document.querySelectorAll('.admin-only').forEach((el) => {
    el.classList.toggle('hidden', !isAdmin());
  });
  document.querySelectorAll('.mod-only').forEach((el) => {
    el.classList.remove('hidden');
  });
}

function showLogin(show) {
  $('login-view').classList.toggle('hidden', !show);
  $('app-view').classList.toggle('hidden', show);
}

async function login() {
  $('loginError').textContent = '';
  $('loginBtn').disabled = true;
  try {
    const res = await api('/login', {
      method: 'POST',
      body: JSON.stringify({ username: $('loginUser').value.trim(), password: $('loginPass').value }),
    });
    state.authenticated = true;
    state.role = res.role || 'admin';
    $('loginPass').value = '';
    showLogin(false);
    applyRoleUi();
    connectWs();
    navigate('dashboard');
    startRefresh();
  } catch (err) {
    $('loginError').textContent = err.message;
  } finally {
    $('loginBtn').disabled = false;
  }
}

async function logout() {
  state.authenticated = false;
  try { await fetch('/logout', { method: 'POST', credentials: 'include' }); } catch {}
  if (state.ws) { state.ws.close(); state.ws = null; }
  if (state.refreshTimer) { clearInterval(state.refreshTimer); state.refreshTimer = null; }
  showLogin(true);
}

function connectWs() {
  if (state.ws) state.ws.close();
  const proto = location.protocol === 'https:' ? 'wss:' : 'ws:';
  state.ws = new WebSocket(`${proto}//${location.host}/ws`);
  state.ws.onmessage = (ev) => {
    try {
      const msg = JSON.parse(ev.data);
      if (msg.type === 'anticheat_alert' && state.page === 'anticheat') {
        showToast(`Alerta AC: ${msg.alert?.username || 'jogador'}`, 'error');
      }
    } catch { /* ignore */ }
    refreshCurrentPage();
  };
  state.ws.onclose = (ev) => { if (state.authenticated && ev.code !== 4401) setTimeout(connectWs, 4000); };
}

const titles = {
  dashboard: 'Dashboard', players: 'Players', staff: 'Staff Tools',
  leaderboards: 'Leaderboards', bans: 'Bans', reports: 'Reports', logs: 'Logs', monitor: 'Monitor',
  anticheat: 'Anticheat',
  store: 'Loja Web',
  webaccounts: 'Contas Web',
  support: 'Suporte Web',
};

function navigate(page) {
  state.page = page;
  document.querySelectorAll('.nav-item').forEach((b) => b.classList.toggle('active', b.dataset.page === page));
  document.querySelectorAll('.page').forEach((s) => s.classList.toggle('hidden', s.id !== `page-${page}`));
  $('pageTitle').textContent = titles[page] || page;
  refreshCurrentPage();
}

function refreshCurrentPage() {
  if (!state.authenticated) return;
  const map = {
    dashboard: loadDashboard,
    players: loadPlayers,
    staff: loadStaff,
    leaderboards: loadLeaderboards,
    bans: loadBans,
    reports: loadReports,
    logs: loadLogs,
    monitor: loadMonitor,
    anticheat: loadAnticheat,
    store: loadStore,
    webaccounts: loadWebAccounts,
    support: loadSupport,
  };
  map[state.page]?.();
  loadMonitorStatus();
}

function startRefresh() {
  refreshCurrentPage();
  if (!state.refreshTimer) state.refreshTimer = setInterval(refreshCurrentPage, REFRESH_MS);
}

async function loadMonitorStatus() {
  try {
    const m = await api('/admin/monitor');
    setStatus(m.databaseAvailable ? 'online' : 'error',
      `DB ${m.dbPingMs ?? '—'}ms · ${m.playersOnlineEstimate} online`);
  } catch { setStatus('error', 'Offline'); }
}

function renderStats(el, items) {
  el.innerHTML = items.map((s) => `
    <div class="stat-card"><span>${esc(s.label)}</span><strong>${esc(s.value)}</strong></div>`).join('');
}

function upsertChart(id, type, labels, data, label) {
  const canvas = $(id);
  if (!canvas || typeof Chart === 'undefined') return;
  if (charts[id]) charts[id].destroy();
  charts[id] = new Chart(canvas, {
    type,
    data: { labels, datasets: [{ label, data, borderColor: '#3b82f6', backgroundColor: 'rgba(59,130,246,0.15)', fill: type === 'line', tension: 0.3 }] },
    options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true } } },
  });
}

async function loadDashboard() {
  try {
    const [stats, hist] = await Promise.all([
      api('/admin/stats'),
      api('/admin/stats/history?days=14'),
    ]);
    renderStats($('dashStats'), [
      { label: 'Jogadores', value: fmt(stats.totalPlayers) },
      { label: 'Online agora', value: fmt(hist.onlineNow) },
      { label: 'Coins total', value: fmt(stats.totalCoins) },
      { label: 'Kills total', value: fmt(stats.totalKills) },
      { label: 'Matches', value: fmt(stats.totalMatches) },
      { label: 'DB', value: stats.databaseAvailable ? 'OK' : 'DOWN' },
    ]);
    const onlineLabels = hist.online.map((p) => new Date(p.t).toLocaleTimeString('pt-PT', { hour: '2-digit', minute: '2-digit' }));
    const onlineData = hist.online.map((p) => p.count);
    upsertChart('chartOnline', 'line', onlineLabels.length ? onlineLabels : ['—'], onlineData.length ? onlineData : [0], 'Online');
    upsertChart('chartGrowth', 'bar', hist.playerGrowth.map((d) => d.date), hist.playerGrowth.map((d) => d.count), 'Players');
    upsertChart('chartKills', 'bar', hist.killsPerDay.map((d) => d.date), hist.killsPerDay.map((d) => d.kills), 'Kills');
    upsertChart('chartEconomy', 'line', hist.economy.map((d) => d.date), hist.economy.map((d) => d.coins), 'Coins');
  } catch (err) { if (err.message !== 'forbidden') showToast(err.message, 'error'); }
}

function renderPagination(el, page, pages, onPage) {
  if (pages <= 1) { el.innerHTML = ''; return; }
  let html = page > 1 ? `<button class="btn btn-ghost btn-sm" data-p="${page-1}">‹</button>` : '';
  html += `<span class="muted small"> ${page}/${pages} </span>`;
  if (page < pages) html += `<button class="btn btn-ghost btn-sm" data-p="${page+1}">›</button>`;
  el.innerHTML = html;
  el.querySelectorAll('[data-p]').forEach((b) => b.addEventListener('click', () => onPage(+b.dataset.p)));
}

async function loadPlayers() {
  try {
    const q = state.playersQuery ? `&q=${encodeURIComponent(state.playersQuery)}` : '';
    const data = await api(`/admin/players?page=${state.playersPage}&limit=25${q}`);
    $('playersMeta').textContent = `${data.total} · pág ${data.page}/${data.pages}`;
    if (!data.players.length) {
      $('playersTableWrap').innerHTML = '<p class="empty">Nenhum resultado.</p>';
    } else {
      $('playersTableWrap').innerHTML = `<div class="table-wrap"><table>
        <thead><tr><th>Nome</th><th>UUID</th><th>Coins</th><th>Emeralds</th><th>SMP</th><th>HC</th><th>Tier</th></tr></thead>
        <tbody>${data.players.map((p) => `<tr>
          <td><button class="link-btn" data-uuid="${esc(p.uuid)}">${esc(p.username)}</button></td>
          <td><code>${esc(p.uuid)}</code></td>
          <td class="coins">${fmt(p.coins)}</td>
          <td class="emeralds">${fmt(p.emeralds)}</td>
          <td>${fmt(p.smp_kills)}/${fmt(p.smp_deaths)}</td>
          <td>${fmt(p.hc_kills)}</td>
          <td>${fmt(p.tierspace_rating)}</td>
        </tr>`).join('')}</tbody></table></div>`;
    }
    renderPagination($('playersPagination'), data.page, data.pages, (p) => { state.playersPage = p; loadPlayers(); });
  } catch (err) { $('playersTableWrap').innerHTML = `<p class="empty">${esc(err.message)}</p>`; }
}

async function openProfile(uuid) {
  state.selectedUuid = uuid;
  try {
    const p = await api(`/admin/player/${uuid}`);
    $('profileTitle').textContent = `${p.username} ${p.active_ban ? '§ [BANIDO]' : ''}`;
    const banWarn = p.active_ban ? `<div class="alert alert-danger">Banido: ${esc(p.active_ban.reason)}</div>` : '';
    $('profileContent').innerHTML = banWarn + `
      <div class="profile-grid">
        <div class="profile-item"><span>Coins</span><strong class="coins">${fmt(p.coins)}</strong></div>
        <div class="profile-item"><span>Emeralds</span><strong class="emeralds">${fmt(p.emeralds)}</strong></div>
        <div class="profile-item"><span>SMP Lv</span><strong>${fmt(p.smp_level)}</strong></div>
        <div class="profile-item"><span>HC Lv</span><strong>${fmt(p.hc_level)}</strong></div>
        <div class="profile-item"><span>SMP K/D</span><strong>${fmt(p.smp?.kills)}/${fmt(p.smp?.deaths)}</strong></div>
        <div class="profile-item"><span>HC K/D/T</span><strong>${fmt(p.hc?.hc_kills)}/${fmt(p.hc?.hc_deaths)}/${fmt(p.hc?.hc_totems_used)}</strong></div>
        <div class="profile-item"><span>Tier</span><strong>${fmt(p.tierspace?.rating)} (${fmt(p.tierspace?.wins)}W)</strong></div>
      </div>
      <h4 class="section-title">Histórico recente</h4>
      <div class="timeline mini">${(p.history?.events || []).slice(0, 8).map((e) => timelineItem(e.event_type || 'event', e.actor_name, e.target_name, e.detail, e.created_at)).join('') || '<p class="muted">Sem eventos</p>'}</div>
      <h4 class="section-title">Bans</h4>
      <ul class="list">${(p.history?.bans || []).slice(0, 5).map((b) => `<li><span>${esc(b.reason)}</span><span>${b.active ? 'activo' : 'hist'}</span></li>`).join('') || '<li>—</li>'}</ul>`;
    $('playerProfilePanel').classList.remove('hidden');
    applyRoleUi();
  } catch (err) { showToast(err.message, 'error'); }
}

function timelineItem(type, actor, target, detail, at) {
  return `<div class="tl-item tl-${esc(type)}">
    <div class="tl-dot"></div>
    <div class="tl-body">
      <strong>${esc(type)}</strong> · ${esc(actor || '—')} → ${esc(target || '—')}
      <div class="muted small">${esc(detail || '')} · ${fmtDate(at)}</div>
    </div></div>`;
}

async function applyEconomy(coins, emeralds) {
  if (!state.selectedUuid) return showToast('Selecciona jogador', 'error');
  try {
    const body = { uuid: state.selectedUuid };
    if (coins != null) body.coins_delta = coins;
    if (emeralds != null) body.emeralds_delta = emeralds;
    await api('/admin/economy/update', { method: 'POST', body: JSON.stringify(body) });
    showToast('OK');
    openProfile(state.selectedUuid);
    loadPlayers();
  } catch (err) { showToast(err.message, 'error'); }
}

async function resetPlayer(scope) {
  if (!state.selectedUuid) return;
  try {
    await api('/admin/reset', { method: 'POST', body: JSON.stringify({ uuid: state.selectedUuid, scope }) });
    showToast('Reset ' + scope);
    openProfile(state.selectedUuid);
  } catch (err) { showToast(err.message, 'error'); }
}

async function banPlayer(uuid, name, reason, hours) {
  try {
    await api('/admin/bans', {
      method: 'POST',
      body: JSON.stringify({ uuid, username: name, reason, hours: hours || 0 }),
    });
    showToast('Ban aplicado');
    loadBans();
  } catch (err) { showToast(err.message, 'error'); }
}

async function unbanPlayer(uuid) {
  try {
    await api('/admin/bans/unban', { method: 'POST', body: JSON.stringify({ uuid }) });
    showToast('Unban OK');
    loadBans();
    if (state.selectedUuid === uuid) openProfile(uuid);
  } catch (err) { showToast(err.message, 'error'); }
}

async function loadStaff() {
  try {
    const data = await api('/admin/staff/online');
    $('staffOnlineCount').textContent = `${data.count} jogadores`;
    $('staffOnlineList').innerHTML = data.players.length
      ? `<ul class="list">${data.players.map((p) => `
        <li><button class="link-btn" data-goto="${esc(p.uuid)}">${esc(p.username)}</button>
        <span class="muted">${esc(p.world || '')}</span></li>`).join('')}</ul>`
      : '<p class="empty">Ninguém online (15m)</p>';
    $('staffOnlineList').querySelectorAll('[data-goto]').forEach((b) => {
      b.addEventListener('click', () => { navigate('players'); openProfile(b.dataset.goto); });
    });
  } catch (err) { $('staffOnlineList').innerHTML = `<p class="empty">${esc(err.message)}</p>`; }
}

async function loadLeaderboards() {
  try {
    const b = await api('/admin/leaderboards?limit=10');
    const card = (title, entries) => {
      const rows = (entries || []).map((e, i) => `<li><span>#${e.rank || i+1} ${esc(e.name)}</span><strong>${fmt(e.value)}</strong></li>`).join('');
      return `<div class="card lb-card"><div class="card-head"><strong>${esc(title)}</strong></div><ul class="list">${rows || '<li>—</li>'}</ul></div>`;
    };
    $('leaderboardsGrid').innerHTML = [
      card('SMP Kills', b.smp?.kills?.entries),
      card('SMP Coins', b.smp?.coins?.entries),
      card('HC Kills', b.hc?.kills?.entries),
      card('HC Totems', b.hc?.totems?.entries),
      card('Tier Rating', b.tierspace?.entries),
    ].join('');
  } catch (err) { $('leaderboardsGrid').innerHTML = `<p class="empty">${esc(err.message)}</p>`; }
}

async function loadBans() {
  try {
    const active = $('bansActiveOnly').checked ? '&active=true' : '';
    const data = await api(`/admin/bans?page=${state.bansPage}&limit=30${active}`);
    $('bansTableWrap').innerHTML = `<div class="table-wrap"><table>
      <thead><tr><th>UUID</th><th>Nome</th><th>Motivo</th><th>Por</th><th>Expira</th><th></th></tr></thead>
      <tbody>${data.bans.map((b) => `<tr>
        <td><code>${esc(b.uuid)}</code></td>
        <td>${esc(b.username)}</td>
        <td>${esc(b.reason)}</td>
        <td>${esc(b.banned_by)}</td>
        <td>${b.expires_at ? fmtDate(b.expires_at) : 'Permanente'}</td>
        <td>${b.active ? `<button class="btn btn-ghost btn-sm" data-unban="${esc(b.uuid)}">Unban</button>` : '—'}</td>
      </tr>`).join('')}</tbody></table></div>`;
    $('bansTableWrap').querySelectorAll('[data-unban]').forEach((btn) => {
      btn.addEventListener('click', () => unbanPlayer(btn.dataset.unban));
    });
    renderPagination($('bansPagination'), data.page, Math.ceil(data.total / data.limit) || 1, (p) => {
      state.bansPage = p; loadBans();
    });
  } catch (err) { $('bansTableWrap').innerHTML = `<p class="empty">${esc(err.message)}</p>`; }
}

async function loadReports() {
  try {
    const st = $('reportFilter').value;
    const data = await api(`/admin/reports?status=${st}&limit=50`);
    $('reportsTableWrap').innerHTML = `<div class="table-wrap"><table>
      <thead><tr><th>#</th><th>Reporter</th><th>Alvo</th><th>Motivo</th><th>Estado</th><th></th></tr></thead>
      <tbody>${data.reports.map((r) => `<tr>
        <td>${r.id}</td>
        <td>${esc(r.reporter_name)}</td>
        <td>${esc(r.target_name)}</td>
        <td>${esc(r.reason)}</td>
        <td>${esc(r.status)}</td>
        <td>${r.status === 'open' ? `<button class="btn btn-primary btn-sm" data-close="${r.id}">Fechar</button>` : fmtDate(r.closed_at)}</td>
      </tr>`).join('')}</tbody></table></div>`;
    $('reportsTableWrap').querySelectorAll('[data-close]').forEach((btn) => {
      btn.addEventListener('click', async () => {
        try {
          await api(`/admin/reports/${btn.dataset.close}/close`, { method: 'POST', body: '{}' });
          showToast('Report fechado');
          loadReports();
        } catch (e) { showToast(e.message, 'error'); }
      });
    });
  } catch (err) { $('reportsTableWrap').innerHTML = `<p class="empty">${esc(err.message)}</p>`; }
}

async function loadLogs() {
  try {
    const data = await api(`/admin/logs?type=${state.logsType}&page=${state.logsPage}&limit=40`);
    $('logsTimeline').innerHTML = data.entries.length
      ? data.entries.map((e) => timelineItem(e.type, e.actor, e.target, e.detail, e.created_at)).join('')
      : '<p class="empty">Sem logs</p>';
    renderPagination($('logsPagination'), data.page, Math.ceil(data.total / data.limit) || 1, (p) => {
      state.logsPage = p; loadLogs();
    });
  } catch (err) { $('logsTimeline').innerHTML = `<p class="empty">${esc(err.message)}</p>`; }
}

async function loadMonitor() {
  try {
    const [mon, stats] = await Promise.all([api('/admin/monitor'), api('/admin/stats')]);
    renderStats($('monitorStats'), [
      { label: 'Backend', value: mon.backend },
      { label: 'PostgreSQL', value: mon.databaseAvailable ? 'Online' : 'Offline' },
      { label: 'Latência DB', value: `${mon.dbPingMs ?? '—'} ms` },
      { label: 'Online (15m)', value: fmt(mon.playersOnlineEstimate) },
      { label: 'Comandos pendentes', value: fmt(mon.pendingCommands) },
      { label: 'Uptime', value: `${fmt(mon.uptimeSeconds)}s` },
      { label: 'Total players', value: fmt(stats.totalPlayers) },
      { label: 'Total kills', value: fmt(stats.totalKills) },
    ]);
  } catch (err) { showToast(err.message, 'error'); }
}

async function loadHealth() {
  try {
    const res = await fetch('/health');
    $('healthJson').textContent = JSON.stringify(await res.json(), null, 2);
  } catch (e) { $('healthJson').textContent = e.message; }
}

document.querySelectorAll('.nav-item').forEach((b) => b.addEventListener('click', () => navigate(b.dataset.page)));
$('loginBtn').addEventListener('click', login);
$('loginPass').addEventListener('keydown', (e) => { if (e.key === 'Enter') login(); });
$('logoutBtn').addEventListener('click', logout);
$('playerSearchBtn').addEventListener('click', () => { state.playersQuery = $('playerSearch').value.trim(); state.playersPage = 1; loadPlayers(); });
$('playersTableWrap').addEventListener('click', (e) => { const b = e.target.closest('[data-uuid]'); if (b) openProfile(b.dataset.uuid); });
$('closeProfile').addEventListener('click', () => { $('playerProfilePanel').classList.add('hidden'); state.selectedUuid = null; });
$('ecoApplyBtn')?.addEventListener('click', () => {
  const c = $('ecoCoinsDelta').value; const e = $('ecoEmeraldsDelta').value;
  applyEconomy(c !== '' ? +c : null, e !== '' ? +e : null);
});
$('ecoResetSmp')?.addEventListener('click', () => resetPlayer('smp'));
$('ecoResetAll')?.addEventListener('click', () => { if (confirm('Reset ALL?')) resetPlayer('all'); });
$('banPlayerBtn')?.addEventListener('click', () => {
  if (!state.selectedUuid) return;
  banPlayer(state.selectedUuid, $('profileTitle').textContent, $('banReason').value, +$('banHours').value || 0);
});
$('unbanPlayerBtn')?.addEventListener('click', () => { if (state.selectedUuid) unbanPlayer(state.selectedUuid); });
$('staffCmdRun')?.addEventListener('click', async () => {
  try {
    const res = await api('/admin/command', { method: 'POST', body: JSON.stringify({ command: $('staffCmd').value.trim() }) });
    showToast(`Comando #${res.id} na fila`);
  } catch (e) { showToast(e.message, 'error'); }
});
$('bansRefresh').addEventListener('click', loadBans);
$('bansActiveOnly').addEventListener('change', loadBans);
$('banSubmit').addEventListener('click', () => banPlayer($('banUuid').value, $('banName').value, $('banReasonGlobal').value, +$('banHoursGlobal').value || 0));
$('reportsRefresh').addEventListener('click', loadReports);
$('reportFilter').addEventListener('change', loadReports);
$('logsRefreshBtn').addEventListener('click', loadLogs);
$('logTypeFilter').addEventListener('change', () => { state.logsType = $('logTypeFilter').value; state.logsPage = 1; loadLogs(); });
$('healthRefreshBtn').addEventListener('click', loadHealth);
$('acRefreshBtn')?.addEventListener('click', loadAnticheat);
$('acViolFilter')?.addEventListener('change', () => {
  state.acViolSeverity = $('acViolFilter').value;
  state.acViolPage = 1;
  loadAnticheatViolations();
});

function severityBadge(sev) {
  const s = String(sev || 'low').toLowerCase();
  return `<span class="badge badge-${s}">${esc(s)}</span>`;
}

async function loadAnticheatViolations() {
  const sev = state.acViolSeverity ? `&severity=${encodeURIComponent(state.acViolSeverity)}` : '';
  const data = await api(`/admin/anticheat/violations?page=${state.acViolPage}&limit=40${sev}`);
  const pages = Math.max(1, Math.ceil(data.total / data.limit));
  if (!data.violations.length) {
    $('acViolationsWrap').innerHTML = '<p class="empty">Sem violações.</p>';
  } else {
    $('acViolationsWrap').innerHTML = `<div class="table-wrap"><table>
      <thead><tr><th>Jogador</th><th>Check</th><th>Tipo</th><th>VL</th><th>Severidade</th><th>Quando</th></tr></thead>
      <tbody>${data.violations.map((v) => `<tr>
        <td>${esc(v.username)}</td>
        <td><code>${esc(v.check_name)}</code></td>
        <td>${esc(v.cheat_type)}</td>
        <td>${Number(v.vl).toFixed(1)}</td>
        <td>${severityBadge(v.severity)}</td>
        <td>${fmtDate(v.created_at)}</td>
      </tr>`).join('')}</tbody></table></div>`;
  }
  renderPagination($('acViolPagination'), data.page, pages, (p) => {
    state.acViolPage = p;
    loadAnticheatViolations();
  });
}

async function loadAnticheat() {
  try {
    const overview = await api('/admin/anticheat');
    const st = overview.stats || {};
    renderStats($('acStats'), [
      { label: 'Flags 24h', value: fmt(st.violations24h) },
      { label: 'Alertas abertos', value: fmt(st.alertsOpen) },
      { label: 'Suspeitos', value: fmt(st.suspectsActive) },
    ]);
    const tl = overview.timeline || [];
    upsertChart('chartAcTimeline', 'line',
      tl.map((p) => new Date(p.t).toLocaleTimeString('pt-PT', { hour: '2-digit', minute: '2-digit' })),
      tl.map((p) => p.count),
      'Flags');
    const sevRows = st.bySeverity || [];
    upsertChart('chartAcSeverity', 'doughnut',
      sevRows.map((r) => r.severity),
      sevRows.map((r) => r.c),
      'Severidade');
    const alerts = overview.alerts || [];
    $('acAlertsWrap').innerHTML = alerts.length ? `<ul class="list">${alerts.map((a) => `
      <li class="list-row">
        <span><strong>${esc(a.username)}</strong> — ${esc(a.reason)}</span>
        <span class="muted">${fmtDate(a.created_at)}</span>
        <button class="btn btn-ghost btn-sm" data-ack="${a.id}">ACK</button>
      </li>`).join('')}</ul>` : '<p class="empty">Nenhum alerta aberto.</p>';
    $('acAlertsWrap').querySelectorAll('[data-ack]').forEach((btn) => {
      btn.addEventListener('click', async () => {
        try {
          await api(`/admin/anticheat/alerts/${btn.dataset.ack}/ack`, { method: 'POST', body: '{}' });
          showToast('Alerta reconhecido');
          loadAnticheat();
        } catch (e) { showToast(e.message, 'error'); }
      });
    });
    const suspects = overview.suspects || [];
    $('acSuspectsWrap').innerHTML = suspects.length ? `<ul class="list">${suspects.map((s) => `
      <li class="list-row">
        <span><strong>${esc(s.username)}</strong> — ${fmt(s.total_flags)} flags</span>
        <span class="muted">${esc(s.last_check || '—')}</span>
        <button class="btn btn-ghost btn-sm" data-clear="${esc(s.uuid)}">Limpar</button>
      </li>`).join('')}</ul>` : '<p class="empty">Nenhum suspeito activo.</p>';
    $('acSuspectsWrap').querySelectorAll('[data-clear]').forEach((btn) => {
      btn.addEventListener('click', async () => {
        try {
          await api(`/admin/anticheat/suspects/${btn.dataset.clear}/clear`, { method: 'POST', body: '{}' });
          showToast('Suspeito removido');
          loadAnticheat();
        } catch (e) { showToast(e.message, 'error'); }
      });
    });
    await loadAnticheatViolations();
  } catch (err) {
    if (err.message !== 'forbidden') showToast(err.message, 'error');
  }
}

async function loadStore() {
  if (!isAdmin()) return;
  const status = $('storeOrderFilter')?.value || '';
  try {
    const [{ products }, ordersData, promoData, subData] = await Promise.all([
      api('/admin/store/products'),
      api(`/admin/store/orders${status ? `?status=${encodeURIComponent(status)}` : ''}`),
      api('/admin/store/promos').catch(() => ({ promos: [], redemptions: [] })),
      api('/admin/store/subscriptions').catch(() => ({ subscriptions: [] })),
    ]);
    $('storeProductsWrap').innerHTML = (products || []).map((p) => `
      <div class="panel-row" style="margin-bottom:0.75rem">
        <div><strong>${esc(p.name)}</strong> · €${esc(p.price_display)}
        ${p.badge ? ` · <span class="muted">${esc(p.badge)}</span>` : ''}<br>
        <span class="muted">${esc(p.slug)} · ${esc(p.category)} · ${p.active ? 'activo' : 'inactivo'}</span></div>
      </div>`).join('') || '<p class="empty">Sem produtos</p>';

    if ($('storePromosWrap')) {
      $('storePromosWrap').innerHTML = (promoData.promos || []).map((p) =>
        `<div class="panel-row"><strong>${esc(p.code)}</strong> ${esc(p.type)} ${p.value}
        · ${p.uses_count}/${p.max_uses ?? '∞'} usos</div>`
      ).join('') || '<p class="empty">Sem promos</p>';
    }
    if ($('storeSubsWrap')) {
      $('storeSubsWrap').innerHTML = (subData.subscriptions || []).map((s) =>
        `<div class="panel-row"><strong>${esc(s.minecraft_username)}</strong> · ${esc(s.product_name || s.product_slug)}
        · ${esc(s.status)}</div>`
      ).join('') || '<p class="empty">Sem subscrições</p>';
    }

    $('storeOrdersMeta').textContent = `${ordersData.total || 0} total`;
    $('storeOrdersWrap').innerHTML = `<table><thead><tr>
      <th>ID</th><th>Jogador</th><th>Produto</th><th>€</th><th>Estado</th><th></th>
    </tr></thead><tbody>${(ordersData.orders || []).map((o) => `
      <tr>
        <td>#${o.id}</td>
        <td>${esc(o.minecraft_username)}</td>
        <td>${esc(o.product_name || o.product_slug)}</td>
        <td>${(o.amount_cents / 100).toFixed(2)}</td>
        <td>${esc(o.status)}</td>
        <td>${o.status !== 'fulfilled' ? `<button class="btn btn-sm btn-primary store-fulfill" data-id="${o.id}">Entregar</button>` : '✓'}</td>
      </tr>`).join('')}</tbody></table>` || '<p class="empty">Sem encomendas</p>';

    document.querySelectorAll('.store-fulfill').forEach((btn) => {
      btn.addEventListener('click', async () => {
        try {
          await api(`/admin/store/orders/${btn.dataset.id}/fulfill`, { method: 'POST' });
          showToast('Entrega enviada ao servidor');
          loadStore();
        } catch (e) { showToast(e.message, 'error'); }
      });
    });
  } catch (err) {
    if (err.message !== 'forbidden') showToast(err.message, 'error');
  }
}

$('storeRefreshBtn')?.addEventListener('click', () => loadStore());
$('storeOrderFilter')?.addEventListener('change', () => loadStore());

async function loadWebAccounts() {
  if (!isAdmin()) return;
  try {
    const data = await api('/admin/web/accounts');
    $('webAccountsWrap').innerHTML = `<table><thead><tr>
      <th>ID</th><th>Email</th><th>Minecraft</th><th>Verificado</th><th>Registo</th>
    </tr></thead><tbody>${(data.accounts || []).map((a) => `
      <tr>
        <td>${a.id}</td>
        <td>${esc(a.email)}</td>
        <td>${esc(a.minecraft_username || '—')}</td>
        <td>${a.email_verified ? '✓' : '—'}</td>
        <td>${fmtDate(a.created_at)}</td>
      </tr>`).join('')}</tbody></table>` || '<p class="empty">Sem contas</p>';
  } catch (err) {
    if (err.message !== 'forbidden') showToast(err.message, 'error');
  }
}

$('webAccountsRefresh')?.addEventListener('click', () => loadWebAccounts());

let selectedSupportTicket = null;

async function loadSupport() {
  const status = $('supportStatusFilter')?.value || '';
  try {
    const data = await api(`/admin/support/tickets${status ? `?status=${encodeURIComponent(status)}` : ''}`);
    $('supportTicketsWrap').innerHTML = (data.tickets || []).map((t) => `
      <div class="panel-row support-ticket-row" data-id="${t.id}" style="cursor:pointer;margin-bottom:0.5rem">
        <strong>#${t.id}</strong> ${esc(t.subject)}<br>
        <span class="muted">${esc(t.email)} · ${esc(t.status)}</span>
      </div>`).join('') || '<p class="empty">Sem tickets</p>';

    document.querySelectorAll('.support-ticket-row').forEach((row) => {
      row.addEventListener('click', () => openSupportTicket(parseInt(row.dataset.id, 10)));
    });
  } catch (err) {
    if (err.message !== 'forbidden') showToast(err.message, 'error');
  }
}

async function openSupportTicket(id) {
  selectedSupportTicket = id;
  try {
    const data = await api(`/admin/support/tickets/${id}`);
    const msgs = (data.messages || []).map((m) =>
      `<p><strong>${esc(m.sender_type)}</strong> (${fmtDate(m.created_at)}): ${esc(m.message)}</p>`
    ).join('');
    $('supportDetailWrap').innerHTML = `
      <p><strong>${esc(data.ticket.subject)}</strong> · ${esc(data.ticket.email)}</p>
      <div style="margin:0.75rem 0;">${msgs}</div>
      <textarea class="input" id="supportReplyText" rows="3" placeholder="Resposta..."></textarea>
      <button class="btn btn-primary btn-sm" id="supportReplyBtn">Responder</button>
      <button class="btn btn-ghost btn-sm" id="supportCloseBtn">Fechar</button>`;

    $('supportReplyBtn')?.addEventListener('click', async () => {
      const msg = $('supportReplyText').value.trim();
      if (!msg) return;
      await api(`/admin/support/tickets/${id}/reply`, { method: 'POST', body: JSON.stringify({ message: msg }) });
      showToast('Resposta enviada');
      openSupportTicket(id);
      loadSupport();
    });
    $('supportCloseBtn')?.addEventListener('click', async () => {
      await api(`/admin/support/tickets/${id}/close`, { method: 'POST' });
      showToast('Ticket fechado');
      loadSupport();
      $('supportDetailWrap').innerHTML = '<p class="muted">Ticket fechado</p>';
    });
  } catch (err) {
    showToast(err.message, 'error');
  }
}

$('supportRefreshBtn')?.addEventListener('click', () => loadSupport());
$('supportStatusFilter')?.addEventListener('change', () => loadSupport());

async function tryRestoreSession() {
  try {
    const me = await api('/admin/me');
    state.authenticated = true;
    state.role = me.role || 'admin';
    showLogin(false);
    applyRoleUi();
    connectWs();
    navigate('dashboard');
    startRefresh();
  } catch { showLogin(true); }
}

tryRestoreSession();
