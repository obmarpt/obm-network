const API = '';

async function api(path, options = {}) {
  const res = await fetch(API + path, {
    credentials: 'include',
    headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
    ...options,
  });
  const text = await res.text();
  let data = null;
  if (text) {
    try { data = JSON.parse(text); } catch { data = { raw: text }; }
  }
  if (!res.ok) throw new Error(data?.error || `HTTP ${res.status}`);
  return data;
}

function esc(s) {
  return String(s ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

function fmt(n) {
  return Number(n ?? 0).toLocaleString('pt-PT');
}

function setActiveNav() {
  const page = document.body.dataset.page;
  document.querySelectorAll('.nav-links a[data-nav]').forEach((a) => {
    a.classList.toggle('active', a.dataset.nav === page);
  });
}

let onlinePollTimer = null;

function startOnlinePoll(targetId, intervalMs = 15000) {
  const tick = () => loadServerStats(targetId);
  tick();
  if (onlinePollTimer) clearInterval(onlinePollTimer);
  onlinePollTimer = setInterval(tick, intervalMs);
}

async function loadServerStats(targetId) {
  const el = document.getElementById(targetId);
  if (!el) return;
  try {
    const s = await api('/api/public/server');
    el.innerHTML = `
      <span class="stat-pill"><strong>${fmt(s.online)}</strong> online</span>
      <span class="stat-pill"><strong>${fmt(s.totalPlayers)}</strong> registados</span>
      <span class="stat-pill">IP: <strong>${esc(s.ip)}</strong></span>
    `;
    const ipEl = document.getElementById('serverIp');
    if (ipEl) ipEl.textContent = s.ip;
  } catch {
    el.innerHTML = '<span class="muted">Stats indisponíveis</span>';
  }
}

async function refreshAuthNav() {
  const navAuth = document.getElementById('navAuth');
  if (!navAuth) return;
  try {
    const { account } = await api('/api/web/auth/me');
    const profileHref = account.uuid ? `profile.html?uuid=${encodeURIComponent(account.uuid)}` : 'account.html';
    navAuth.innerHTML = `
      <a href="${profileHref}" class="nav-user">${esc(account.username || account.email)}</a>
      <a href="account.html" class="btn btn-ghost btn-sm">Conta</a>
      <button class="btn btn-ghost btn-sm" id="logoutBtn" type="button">Sair</button>
    `;
    document.getElementById('logoutBtn')?.addEventListener('click', async () => {
      await api('/api/web/auth/logout', { method: 'POST' });
      location.reload();
    });
  } catch {
    navAuth.innerHTML = '<a href="login.html" class="btn btn-secondary btn-sm">Entrar</a>';
  }
}

document.addEventListener('DOMContentLoaded', () => {
  setActiveNav();
  refreshAuthNav();
});
