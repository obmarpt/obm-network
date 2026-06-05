const NAV_LINKS = [
  { href: 'index.html', nav: 'home', label: 'Home' },
  { href: 'modes.html', nav: 'modes', label: 'Modos' },
  { href: 'store.html', nav: 'store', label: 'Loja' },
  { href: 'stats.html', nav: 'stats', label: 'Stats' },
  { href: 'leaderboards.html', nav: 'leaderboards', label: 'Rankings' },
  { href: 'battlepass.html', nav: 'battlepass', label: 'Battle Pass' },
  { href: 'cosmetics.html', nav: 'cosmetics', label: 'Cosméticos' },
  { href: 'achievements.html', nav: 'achievements', label: 'Achievements' },
  { href: 'support.html', nav: 'support', label: 'Suporte' },
];

const BRAND_LOGO = `<span class="brand-logo" aria-hidden="true">◆</span>`;

function renderNav() {
  const mount = document.getElementById('app-nav');
  if (!mount) return;
  const page = document.body.dataset.page || '';
  const links = NAV_LINKS.map((l) =>
    `<a href="${l.href}" data-nav="${l.nav}" class="${page === l.nav ? 'active' : ''}">${l.label}</a>`
  ).join('');
  mount.innerHTML = `
    <nav class="nav">
      <a href="index.html" class="brand">${BRAND_LOGO}Mine<span>Space</span></a>
      <button class="nav-toggle" id="navToggle" type="button" aria-label="Menu">☰</button>
      <div class="nav-links" id="navLinks">
        ${links}
        <div class="nav-actions">
          <a href="join.html" class="btn btn-primary btn-sm" data-nav="join">▶ Jogar</a>
          <span id="navAuth"></span>
        </div>
      </div>
    </nav>`;
  document.getElementById('navToggle')?.addEventListener('click', () => {
    document.getElementById('navLinks')?.classList.toggle('open');
  });
}

function renderFooter() {
  const mount = document.getElementById('app-footer');
  if (!mount) return;
  mount.innerHTML = `
    <footer class="footer">
      <div class="footer-grid">
        <div class="footer-col">
          <div class="footer-brand"><strong class="brand-inline">◆ MineSpace</strong></div>
          <p class="muted">Network Minecraft premium — SMP, Hardcore e TierSpace competitivo.</p>
        </div>
        <div class="footer-col">
          <h4>Jogar</h4>
          <a href="join.html">Como entrar</a>
          <a href="modes.html">Modos</a>
          <a href="leaderboards.html">Rankings</a>
        </div>
        <div class="footer-col">
          <h4>Loja</h4>
          <a href="store.html">VIPs & Bundles</a>
          <a href="cosmetics.html">Cosméticos</a>
          <a href="battlepass.html">Battle Pass</a>
        </div>
        <div class="footer-col">
          <h4>Conta</h4>
          <a href="login.html">Entrar</a>
          <a href="register.html">Registar</a>
          <a href="support.html">Suporte</a>
        </div>
      </div>
      <div class="footer-bottom">
        <span>© ${new Date().getFullYear()} MineSpace Network</span>
        <span class="muted">Pagamentos seguros · Zero pay-to-win</span>
      </div>
    </footer>`;
}

let revealObserver = null;

function initScrollReveal(root) {
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;
  const scope = root || document;
  const candidates = scope.querySelectorAll('.section, .card, .store-compare, .store-featured, .auth-card, .page-header');
  candidates.forEach((el) => {
    if (!el.classList.contains('reveal')) el.classList.add('reveal');
  });
  if (!revealObserver) {
    revealObserver = new IntersectionObserver((entries) => {
      entries.forEach((e) => {
        if (e.isIntersecting) {
          e.target.classList.add('revealed');
          revealObserver.unobserve(e.target);
        }
      });
    }, { threshold: 0.06, rootMargin: '0px 0px -40px 0px' });
  }
  candidates.forEach((el) => {
    if (!el.classList.contains('revealed')) revealObserver.observe(el);
  });
}

window.msReveal = initScrollReveal;

document.addEventListener('DOMContentLoaded', () => {
  renderNav();
  renderFooter();
  initScrollReveal();
});
