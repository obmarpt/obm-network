const PlayerProfile = (() => {
  const RARITY_CLASS = {
    COMMON: 'rarity-common',
    RARE: 'rarity-rare',
    EPIC: 'rarity-epic',
    LEGENDARY: 'rarity-legendary',
    MYTHIC: 'rarity-mythic',
  };

  function raritySpan(rarity) {
    const cls = RARITY_CLASS[rarity] || 'rarity-common';
    return `<span class="${cls}">${esc(rarity || 'COMMON')}</span>`;
  }

  function badgeTierClass(tier) {
    return tier ? `tier-${tier}` : '';
  }

  let chartIdSeq = 0;

  function renderLineChart(series, opts = {}) {
    const gradId = `chartGrad-${chartIdSeq++}`;
    const data = Array.isArray(series) ? series.filter((p) => p && p.date) : [];
    if (data.length < 2) {
      return '<div class="profile-chart-empty">Dados insuficientes — joga mais para ver evolução</div>';
    }

    const w = 320;
    const h = 120;
    const pad = { t: 8, r: 8, b: 20, l: 36 };
    const innerW = w - pad.l - pad.r;
    const innerH = h - pad.t - pad.b;

    const values = data.map((d) => Number(d.value) || 0);
    const minV = opts.invertY ? Math.min(...values) : Math.min(...values);
    const maxV = opts.invertY ? Math.max(...values) : Math.max(...values, 1);
    const range = maxV - minV || 1;

    const points = data.map((d, i) => {
      const x = pad.l + (i / (data.length - 1)) * innerW;
      let norm = (Number(d.value) - minV) / range;
      if (opts.invertY) norm = 1 - norm;
      const y = pad.t + (1 - norm) * innerH;
      return { x, y, ...d };
    });

    const pathD = points.map((p, i) => `${i === 0 ? 'M' : 'L'}${p.x.toFixed(1)},${p.y.toFixed(1)}`).join(' ');
    const areaD = `${pathD} L${points[points.length - 1].x},${h - pad.b} L${points[0].x},${h - pad.b} Z`;
    const stroke = opts.color || '#9d4edd';
    const last = data[data.length - 1];
    const first = data[0];

    return `
      <svg class="profile-chart-svg" viewBox="0 0 ${w} ${h}" preserveAspectRatio="none" role="img" aria-label="Gráfico">
        <defs>
          <linearGradient id="${gradId}" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stop-color="${stroke}" stop-opacity="0.35"/>
            <stop offset="100%" stop-color="${stroke}" stop-opacity="0"/>
          </linearGradient>
        </defs>
        <path d="${areaD}" fill="url(#${gradId})"/>
        <path d="${pathD}" fill="none" stroke="${stroke}" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        <text x="${pad.l}" y="${h - 4}" fill="#8a8a96" font-size="9">${esc(first.date.slice(5))}</text>
        <text x="${w - pad.r - 28}" y="${h - 4}" fill="#8a8a96" font-size="9">${esc(last.date.slice(5))}</text>
        <text x="4" y="${pad.t + 8}" fill="#8a8a96" font-size="9">${fmt(last.value)}</text>
      </svg>`;
  }

  function renderHero(p, badges) {
    const avatar = Portal.avatarUrl(p.uuid, 96);
    const memberSince = p.memberSince
      ? new Date(p.memberSince).toLocaleDateString('pt-PT')
      : '—';

    const badgeHtml = (badges || []).map((b) =>
      `<span class="profile-badge ${badgeTierClass(b.tier)}">${b.icon} ${esc(b.name)}</span>`
    ).join('');

    return `
      <div class="profile-hero-adv card">
        <img class="profile-avatar" src="${avatar}" alt="" width="96" height="96" onerror="this.style.display='none'">
        <div class="profile-hero-info">
          <h1>${esc(p.username)}</h1>
          <p class="muted-sm">Membro desde ${esc(memberSince)}</p>
          <div class="profile-hero-meta">
            <span class="badge badge-warn">${esc(p.rankDisplay || p.rank || 'Bronze')}</span>
            <span class="badge">BP Lv ${fmt(p.bpLevel)}</span>
            <span class="badge">SMP Lv ${fmt(p.smpLevel)}</span>
          </div>
          ${badgeHtml ? `<div class="profile-highlight-badges">${badgeHtml}</div>` : ''}
        </div>
      </div>`;
  }

  function renderMainStats(p) {
    return `
      <section class="profile-section">
        <h2 class="profile-section-title">Estatísticas principais</h2>
        <div class="profile-stat-row">
          <div class="profile-stat-card card"><div class="val">${fmt(p.coins)}</div><div class="lbl">Money SMP</div></div>
          <div class="profile-stat-card card"><div class="val">${fmt(p.smp?.kills)}</div><div class="lbl">Kills</div></div>
          <div class="profile-stat-card card"><div class="val">${fmt(p.smp?.deaths)}</div><div class="lbl">Deaths</div></div>
          <div class="profile-stat-card card"><div class="val">${esc(p.smp?.kd)}</div><div class="lbl">K/D</div></div>
          <div class="profile-stat-card card"><div class="val">${fmt(p.bpLevel)}</div><div class="lbl">Battle Pass</div></div>
          <div class="profile-stat-card card"><div class="val">${esc(p.rankDisplay)}</div><div class="lbl">Rank SMP</div></div>
        </div>
      </section>`;
  }

  function renderRankings(rankings) {
    if (!rankings) return '';
    const r = rankings;
    return `
      <section class="profile-section">
        <h2 class="profile-section-title">Rankings por modo</h2>
        <div class="profile-rank-grid">
          <div class="profile-rank-item card"><span>SMP Money</span><strong>#${fmt(r.smp?.money ?? '—')}</strong></div>
          <div class="profile-rank-item card"><span>SMP Kills</span><strong>#${fmt(r.smp?.kills ?? '—')}</strong></div>
          <div class="profile-rank-item card"><span>TierSpace ELO</span><strong>#${fmt(r.tierspace?.rating ?? '—')}</strong></div>
          <div class="profile-rank-item card"><span>Geral</span><strong>#${fmt(r.overall ?? '—')}</strong></div>
        </div>
      </section>`;
  }

  function renderSeason(season) {
    if (!season) {
      return `<section class="profile-section"><div class="card"><p class="muted">Season indisponível</p></div></section>`;
    }
    const ends = season.endsAt ? new Date(season.endsAt).toLocaleDateString('pt-PT') : '—';
    return `
      <section class="profile-section">
        <h2 class="profile-section-title">Season atual</h2>
        <div class="card profile-season-card">
          <div class="profile-season-header">
            <div>
              <h3>${esc(season.name)}</h3>
              <p class="muted-sm">Termina ${esc(ends)}</p>
            </div>
            <div class="profile-season-pos">#${fmt(season.position ?? '—')}</div>
          </div>
          <p class="muted-sm" style="margin-bottom:0.5rem;">${fmt(season.points)} pts · Progresso season</p>
          <div class="progress-bar"><span style="width:${season.progress || 0}%"></span></div>
        </div>
      </section>`;
  }

  function renderCharts(charts) {
    const c = charts || {};
    return `
      <section class="profile-section">
        <h2 class="profile-section-title">Evolução</h2>
        <div class="profile-chart-grid">
          <div class="card profile-chart-card">
            <h4>Money ao longo do tempo</h4>
            ${renderLineChart(c.money, { color: '#ffc300' })}
          </div>
          <div class="card profile-chart-card">
            <h4>Kills ao longo do tempo</h4>
            ${renderLineChart(c.kills, { color: '#00b4ff' })}
          </div>
          <div class="card profile-chart-card">
            <h4>Ranking geral</h4>
            ${renderLineChart(c.ranking, { color: '#9d4edd', invertY: true })}
          </div>
        </div>
      </section>`;
  }

  function renderSeasonHistory(history) {
    const rows = (history || []).map((s) => {
      const start = s.startsAt ? new Date(s.startsAt).toLocaleDateString('pt-PT') : '—';
      const cls = s.active ? 'profile-season-row current' : 'profile-season-row';
      return `
        <div class="${cls}">
          <div>
            <strong>${esc(s.name)}</strong>
            <span class="muted-sm"> · ${esc(start)}</span>
          </div>
          <span>#${fmt(s.position ?? '—')} · ${fmt(s.points)} pts</span>
        </div>`;
    }).join('');

    return `
      <section class="profile-section">
        <h2 class="profile-section-title">Histórico de seasons</h2>
        <div class="profile-season-history">
          ${rows || '<p class="muted">Sem temporadas anteriores</p>'}
        </div>
      </section>`;
  }

  function renderCosmetics(cosmetics) {
    const items = cosmetics?.catalog || [];
    const grid = items.map((c) => {
      const cls = ['profile-cosmetic-item', 'card'];
      if (c.unlocked) cls.push('unlocked');
      if (c.active) cls.push('active');
      const status = c.active ? 'Ativo' : (c.unlocked ? 'Desbloqueado' : 'Bloqueado');
      return `
        <div class="${cls.join(' ')}">
          <div style="display:flex;justify-content:space-between;align-items:center;gap:0.5rem;">
            <strong>${esc(c.name)}</strong>
            ${raritySpan(c.rarity)}
          </div>
          <p class="muted-sm" style="margin:0.35rem 0;">${esc(c.type)} · ${esc(status)}</p>
          <p class="muted-sm">${esc(c.unlock)}</p>
        </div>`;
    }).join('');

    return `
      <section class="profile-section">
        <h2 class="profile-section-title">Cosméticos (${fmt(cosmetics?.unlocked?.length ?? 0)} desbloqueados)</h2>
        <div class="profile-cosmetic-grid">${grid || '<p class="muted">Sem cosméticos</p>'}</div>
      </section>`;
  }

  function renderAchievements(achievements) {
    const items = achievements?.items || [];
    const grid = items.map((a) => {
      const cls = a.completed ? 'profile-achievement-item completed card' : 'profile-achievement-item card';
      return `
        <div class="${cls}">
          <div style="display:flex;justify-content:space-between;gap:0.5rem;">
            <strong>${esc(a.name)}</strong>
            ${raritySpan(a.rarity)}
          </div>
          <p class="muted-sm" style="margin:0.35rem 0;">${esc(a.description)}</p>
          ${a.completed
            ? '<span class="badge" style="margin-top:0.35rem;">✔ Completo</span>'
            : `<p class="muted-sm">${fmt(a.current)} / ${fmt(a.target)}</p><div class="progress-bar"><span style="width:${a.progress}%"></span></div>`
          }
        </div>`;
    }).join('');

    return `
      <section class="profile-section">
        <h2 class="profile-section-title">Achievements (${fmt(achievements?.completed ?? 0)}/${fmt(achievements?.total ?? 0)})</h2>
        <div class="profile-achievement-grid">${grid}</div>
      </section>`;
  }

  function render(data) {
    const p = data.profile;
    return `
      <div class="profile-page">
        ${renderHero(p, data.badges)}
        ${renderMainStats(p)}
        ${renderRankings(data.rankings)}
        ${renderSeason(data.season)}
        ${renderCharts(data.charts)}
        ${renderSeasonHistory(data.seasonHistory)}
        ${renderCosmetics(data.cosmetics)}
        ${renderAchievements(data.achievements)}
      </div>`;
  }

  return { render, renderLineChart };
})();

window.PlayerProfile = PlayerProfile;
