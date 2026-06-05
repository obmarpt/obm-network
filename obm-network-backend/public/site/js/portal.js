/** MineSpace portal — tiers, leaderboards, stats (partilhado) */
const Portal = (() => {
  function avatarUrl(uuid, size = 32) {
    if (!uuid) return '';
    return `https://mc-heads.net/avatar/${uuid}/${size}`;
  }

  function playerLink(uuid) {
    if (!uuid) return '#';
    return `/player/${encodeURIComponent(uuid)}`;
  }

  function renderTierMatrix(tierData, opts = {}) {
    const matrix = tierData?.matrix;
    if (!matrix?.rows?.length) return '';

    const hi = matrix.highlightIndex ?? 3;
    const compact = opts.compact === true;
    const rows = compact
      ? matrix.rows.filter((r) => !['30 dias', 'Lifetime'].includes(r.label))
      : matrix.rows;

    const headCells = matrix.columns.map((col, i) => {
      if (i === hi) {
        return `<th class="tier-col-highlight tier-mvp-plus-col">${esc(col)}<span class="tier-pop-badge">🔥 MOST POPULAR</span></th>`;
      }
      return `<th>${esc(col)}</th>`;
    }).join('');

    const bodyRows = rows.map((row) => {
      const cells = row.values.map((val, i) => {
        const cls = [
          i === hi ? 'tier-col-highlight' : '',
          row.bestIndex === i ? 'best-val' : '',
        ].filter(Boolean).join(' ');
        return `<td class="${cls}">${esc(val)}</td>`;
      }).join('');
      return `<tr><td class="feature-col">${esc(row.label)}</td>${cells}</tr>`;
    }).join('');

    const cta = opts.showCta
      ? `<div class="tier-compare-cta"><a href="store.html" class="btn btn-primary">Ver planos VIP →</a></div>`
      : '';

    return `
      <div class="tier-compare-block">
        <div class="store-matrix-wrap">
          <table class="store-matrix portal-matrix">
            <thead>
              <tr>
                <th class="feature-col">Feature</th>
                ${headCells}
              </tr>
            </thead>
            <tbody>${bodyRows}</tbody>
          </table>
        </div>
        ${tierData.rules ? `<p class="tier-compare-note">${esc(tierData.rules)}</p>` : ''}
        ${cta}
      </div>`;
  }

  function rankClass(rank) {
    if (rank === 1) return 'lb-rank-gold';
    if (rank === 2) return 'lb-rank-silver';
    if (rank === 3) return 'lb-rank-bronze';
    return '';
  }

  function renderLbRow(entry, index) {
    const rank = entry.rank ?? index + 1;
    const uuid = entry.uuid || '';
    const name = entry.name || entry.username || '?';
    const value = entry.value ?? entry.score ?? 0;
    const rc = rankClass(rank);
    const podium = rank <= 3 ? ' lb-podium' : '';

    return `
      <li class="lb-row${podium} ${rc}">
        <span class="lb-pos">#${rank}</span>
        <a class="lb-player" href="${playerLink(uuid)}">
          <img class="lb-avatar" src="${avatarUrl(uuid, 36)}" alt="" loading="lazy" width="36" height="36" onerror="this.classList.add('lb-avatar-fallback')">
          <span class="lb-name">${esc(name)}</span>
        </a>
        <span class="lb-value">${fmt(value)}</span>
      </li>`;
  }

  function renderLeaderboardCard(title, icon, entries, emptyMsg = 'Sem dados') {
    const list = (entries || []).slice(0, 10);
    const body = list.length
      ? `<ol class="lb-list">${list.map((e, i) => renderLbRow(e, i)).join('')}</ol>`
      : `<p class="muted lb-empty">${esc(emptyMsg)}</p>`;

    return `
      <article class="card lb-card">
        <h3 class="lb-card-title">${icon} ${esc(title)}</h3>
        ${body}
      </article>`;
  }

  async function fetchTiers() {
    const res = await fetch('/api/store/tiers', { credentials: 'include' });
    if (!res.ok) throw new Error('tiers_unavailable');
    return res.json();
  }

  async function fetchLeaderboards() {
    const res = await fetch('/api/public/leaderboards', { credentials: 'include' });
    if (!res.ok) throw new Error('leaderboards_unavailable');
    return res.json();
  }

  async function fetchLeaderboard(metric) {
    const res = await fetch(`/api/public/leaderboard/${metric}`, { credentials: 'include' });
    if (!res.ok) throw new Error('leaderboard_unavailable');
    return res.json();
  }

  async function fetchGlobalStats() {
    const res = await fetch('/api/public/stats', { credentials: 'include' });
    if (!res.ok) throw new Error('stats_unavailable');
    return res.json();
  }

  function renderGlobalStatsBar(totals, onlineNow) {
    const t = totals || {};
    return `
      <div class="global-stats-bar">
        <div class="global-stat">
          <span class="global-stat-val live-stat">${fmt(onlineNow ?? 0)}</span>
          <span class="global-stat-lbl">Online agora</span>
        </div>
        <div class="global-stat">
          <span class="global-stat-val">${fmt(t.players ?? 0)}</span>
          <span class="global-stat-lbl">Registados</span>
        </div>
        <div class="global-stat">
          <span class="global-stat-val">${fmt(t.totalCoins ?? 0)}</span>
          <span class="global-stat-lbl">Coins totais</span>
        </div>
        <div class="global-stat">
          <span class="global-stat-val">${fmt(t.totalKills ?? 0)}</span>
          <span class="global-stat-lbl">Kills SMP</span>
        </div>
      </div>`;
  }

  async function mountGlobalStats(targetId) {
    const el = document.getElementById(targetId);
    if (!el) return;
    try {
      const s = await fetchGlobalStats();
      el.innerHTML = renderGlobalStatsBar(s.totals, s.onlineNow);
    } catch {
      el.innerHTML = '<p class="muted">Stats indisponíveis</p>';
    }
  }

  function renderProfile(p) {
    const avatar = avatarUrl(p.uuid, 80);
    const kd = p.smp?.kd ?? '0';
    const memberSince = p.memberSince
      ? new Date(p.memberSince).toLocaleDateString('pt-PT')
      : '—';

    return `
      <div class="profile-hero card">
        <img class="profile-avatar" src="${avatar}" alt="" width="80" height="80" onerror="this.style.display='none'">
        <div class="profile-hero-info">
          <h1>${esc(p.username)}</h1>
          <p class="muted-sm">Membro desde ${esc(memberSince)}</p>
          <div class="profile-badges">
            <span class="badge badge-warn">Level ${fmt(p.globalLevel ?? 1)}</span>
            <span class="badge">SMP Lv ${fmt(p.smpLevel ?? 1)}</span>
          </div>
        </div>
      </div>

      <div class="stat-grid profile-stats">
        <div class="stat-box"><div class="val">${fmt(p.coins)}</div><div class="lbl">Coins</div></div>
        <div class="stat-box"><div class="val">${fmt(p.emeralds)}</div><div class="lbl">Emeralds</div></div>
        <div class="stat-box"><div class="val">${fmt(p.smp?.kills)}</div><div class="lbl">SMP Kills</div></div>
        <div class="stat-box"><div class="val">${kd}</div><div class="lbl">K/D SMP</div></div>
        <div class="stat-box"><div class="val">${fmt(p.tierspace?.rating)}</div><div class="lbl">ELO TierSpace</div></div>
        <div class="stat-box"><div class="val">${fmt(p.hc?.kills)}</div><div class="lbl">HC Kills</div></div>
      </div>

      <div class="grid-2 profile-detail-grid">
        <div class="card">
          <h3>⚔ SMP Rush</h3>
          <ul class="profile-detail-list">
            <li><span>Kills</span><strong>${fmt(p.smp?.kills)}</strong></li>
            <li><span>Deaths</span><strong>${fmt(p.smp?.deaths)}</strong></li>
            <li><span>Playtime</span><strong>${fmt(p.smp?.playtime)} min</strong></li>
            <li><span>Level</span><strong>${fmt(p.smpLevel)}</strong></li>
          </ul>
          <div class="progress-bar" title="Progressão SMP"><span style="width:${Math.min(100, (p.smpLevel || 1) * 4)}%"></span></div>
        </div>
        <div class="card">
          <h3>🏆 TierSpace</h3>
          <ul class="profile-detail-list">
            <li><span>Rating</span><strong>${fmt(p.tierspace?.rating)}</strong></li>
            <li><span>Wins</span><strong>${fmt(p.tierspace?.wins)}</strong></li>
            <li><span>Losses</span><strong>${fmt(p.tierspace?.losses)}</strong></li>
            <li><span>Winstreak</span><strong>${fmt(p.tierspace?.winstreak)}</strong></li>
          </ul>
        </div>
        <div class="card">
          <h3>☠ Hardcore</h3>
          <ul class="profile-detail-list">
            <li><span>Kills</span><strong>${fmt(p.hc?.kills)}</strong></li>
            <li><span>Deaths</span><strong>${fmt(p.hc?.deaths)}</strong></li>
            <li><span>Wins</span><strong>${fmt(p.hc?.wins)}</strong></li>
            <li><span>Level</span><strong>${fmt(p.hc?.level)}</strong></li>
          </ul>
        </div>
        <div class="card">
          <h3>💎 Economia</h3>
          <ul class="profile-detail-list">
            <li><span>Coins</span><strong>${fmt(p.coins)}</strong></li>
            <li><span>Emeralds</span><strong>${fmt(p.emeralds)}</strong></li>
            <li><span>Global Level</span><strong>${fmt(p.globalLevel)}</strong></li>
          </ul>
        </div>
      </div>`;
  }

  return {
    avatarUrl,
    playerLink,
    renderTierMatrix,
    renderLbRow,
    renderLeaderboardCard,
    renderGlobalStatsBar,
    renderProfile,
    fetchTiers,
    fetchLeaderboards,
    fetchLeaderboard,
    fetchGlobalStats,
    mountGlobalStats,
  };
})();

window.Portal = Portal;
