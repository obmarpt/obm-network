async function api(path, options = {}) {
  const res = await fetch(path, {
    credentials: 'include',
    headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
    ...options,
  });
  const text = await res.text();
  let data = null;
  if (text) try { data = JSON.parse(text); } catch { data = {}; }
  if (!res.ok) throw new Error(data?.error || `HTTP ${res.status}`);
  return data;
}

function esc(s) {
  return String(s ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/"/g, '&quot;');
}

const BADGE_LABELS = {
  popular: '🔥 Most Popular',
  best_value: '💎 Best Value',
  limited: '⏳ Limited Offer',
  recommended: '⭐ Recommended',
};

const CATEGORIES = [
  { id: 'vip', label: 'VIPs', icon: '👑' },
  { id: 'bundle', label: 'Bundles', icon: '🎁' },
  { id: 'cosmetic', label: 'Cosméticos', icon: '✨' },
  { id: 'booster', label: 'Boosters', icon: '⚡' },
];

const BUNDLE_SLUGS = ['starter-pack', 'pro-pack', 'ultimate-pack'];

const TIER_CLASS = {
  'vip-30d': 'tier-vip',
  'vip-lifetime': 'tier-vip',
  'vip-plus-30d': 'tier-vip-plus',
  'vip-plus-lifetime': 'tier-vip-plus',
  'mvp-30d': 'tier-mvp',
  'mvp-lifetime': 'tier-mvp',
  'mvp-plus-30d': 'tier-mvp-plus',
  'mvp-plus-lifetime': 'tier-mvp-plus',
};

const BUNDLE_CLASS = {
  'starter-pack': 'tier-bundle-starter',
  'pro-pack': 'tier-bundle-pro',
  'ultimate-pack': 'tier-bundle-ultimate',
};

function tierClassFor(p, variant) {
  if (variant === 'bundle') return BUNDLE_CLASS[p.slug] || '';
  if (p.category !== 'vip') return '';
  return TIER_CLASS[p.slug] || '';
}

let allProducts = [];
let activeCategory = 'vip';
let appliedPromo = null;
let pendingProduct = null;

function formatCountdown(endsAt) {
  const end = new Date(endsAt).getTime();
  const diff = end - Date.now();
  if (diff <= 0) return null;
  const h = Math.floor(diff / 3600000);
  const m = Math.floor((diff % 3600000) / 60000);
  const s = Math.floor((diff % 60000) / 1000);
  return `${h}h ${m}m ${s}s`;
}

function productPrice(p) {
  if (appliedPromo?.productId === p.id) return appliedPromo.finalDisplay;
  return p.price_display;
}

function renderBadge(p) {
  if (!p.badge) return '';
  const cls = `store-badge store-badge-${p.badge}`;
  return `<span class="${cls}">${BADGE_LABELS[p.badge] || p.badge}</span>`;
}

function renderProductCard(p, variant = 'default') {
  const compare = p.compare_display
    ? `<span class="store-product-compare">€${esc(p.compare_display)}</span>` : '';
  const savings = p.savings_pct
    ? `<span class="store-product-save">-${p.savings_pct}%</span>` : '';
  const countdown = p.sale_ends_at ? formatCountdown(p.sale_ends_at) : null;
  const timer = countdown
    ? `<div class="store-timer">⏳ Limited — ${esc(countdown)}</div>` : '';
  const tierCls = tierClassFor(p, variant);
  const isBestValue = p.badge === 'best_value' || p.slug === 'mvp-plus-30d' || p.slug === 'mvp-plus-lifetime';
  const cardClass = variant === 'bundle'
    ? `store-bundle-card${p.slug === 'ultimate-pack' ? ' ultimate' : ''}${isBestValue ? ' best-value' : ''}${tierCls ? ` ${tierCls}` : ''}`
    : `store-product${p.featured ? ' is-featured' : ''}${isBestValue ? ' best-value' : ''}${tierCls ? ` ${tierCls}` : ''}`;

  return `
    <article class="${cardClass}" data-id="${p.id}" data-cat="${esc(p.category)}">
      ${renderBadge(p)}
      ${variant !== 'bundle' ? `<div class="store-product-cat">${esc(p.category)}</div>` : ''}
      <h3 class="store-product-name">${esc(p.name)}</h3>
      <p class="store-product-desc">${esc(p.description)}</p>
      ${timer}
      <div class="store-product-price-block">
        <div class="store-product-price-row">
          ${compare}
          <div class="store-product-price"><span>€</span>${esc(productPrice(p))}</div>
          ${savings}
        </div>
      </div>
      <ul class="store-product-perks">
        ${(p.perks || []).map((x) => `<li>${esc(x)}</li>`).join('')}
      </ul>
      <button type="button" class="store-buy-btn buy-btn" data-id="${p.id}">Comprar</button>
    </article>`;
}

function renderTierMatrix(tierData) {
  if (window.Portal?.renderTierMatrix) {
    return Portal.renderTierMatrix(tierData, { compact: false });
  }
  return '<p class="muted">Comparativo indisponível</p>';
}

function renderFeaturedBundles() {
  const bundles = allProducts.filter((p) => p.category === 'bundle' && BUNDLE_SLUGS.includes(p.slug));
  const section = document.getElementById('featuredSection');
  const grid = document.getElementById('featuredGrid');
  if (!bundles.length || !section || !grid) return;

  section.hidden = false;
  grid.innerHTML = bundles
    .sort((a, b) => BUNDLE_SLUGS.indexOf(a.slug) - BUNDLE_SLUGS.indexOf(b.slug))
    .map((p) => renderProductCard(p, 'bundle'))
    .join('');
  bindBuyButtons(grid);
}

function filterProducts() {
  return allProducts.filter((p) => {
    if (p.category === 'bundle' && BUNDLE_SLUGS.includes(p.slug)) return false;
    return p.category === activeCategory;
  });
}

function renderGrid() {
  const grid = document.getElementById('productsGrid');
  const items = filterProducts();

  if (!items.length) {
    grid.innerHTML = '<p class="store-empty">Nenhum produto nesta categoria.</p>';
    return;
  }

  grid.innerHTML = `<div class="store-products-grid">${items.map((p) => renderProductCard(p)).join('')}</div>`;
  bindBuyButtons(grid);
}

function bindBuyButtons(root) {
  root.querySelectorAll('.buy-btn').forEach((btn) => {
    btn.addEventListener('click', () => openCheckoutModal(parseInt(btn.dataset.id, 10)));
  });
}

async function ensureStoreAuth() {
  try {
    const { account } = await api('/api/web/auth/me');
    if (!account) {
      window.location.href = 'login.html?next=store.html';
      return null;
    }
    if (!account.linked) {
      alert('Liga a conta Minecraft em Conta antes de comprar. Usa /link no servidor.');
      window.location.href = 'account.html';
      return null;
    }
    return account;
  } catch {
    window.location.href = 'login.html?next=store.html';
    return null;
  }
}

async function openCheckoutModal(productId) {
  const account = await ensureStoreAuth();
  if (!account) return;

  const product = allProducts.find((p) => p.id === productId);
  if (!product) return;
  pendingProduct = product;

  const price = productPrice(product);
  const promoNote = appliedPromo?.productId === product.id
    ? `<p class="store-modal-product">Código <strong>${esc(appliedPromo.code)}</strong> aplicado</p>`
    : '';

  const mount = document.getElementById('checkoutModal');
  mount.innerHTML = `
    <div class="store-modal-backdrop" id="modalBackdrop" role="dialog" aria-modal="true">
      <div class="store-modal">
        <h3>Confirmar compra</h3>
        <p class="store-modal-product">${esc(product.name)}</p>
        ${promoNote}
        <div class="store-modal-price">€${esc(price)}</div>
        <ul class="store-modal-perks">
          ${(product.perks || []).slice(0, 6).map((x) => `<li>${esc(x)}</li>`).join('')}
        </ul>
        <p class="store-modal-product" style="margin-bottom:1rem;">Serás redirecionado para o Stripe. Entrega automática in-game.</p>
        <div class="store-modal-actions">
          <button type="button" class="btn btn-ghost" id="modalCancel">Cancelar</button>
          <button type="button" class="store-buy-btn" id="modalConfirm" style="flex:2;">Confirmar & Pagar</button>
        </div>
      </div>
    </div>`;

  document.getElementById('modalCancel').addEventListener('click', closeCheckoutModal);
  document.getElementById('modalBackdrop').addEventListener('click', (e) => {
    if (e.target.id === 'modalBackdrop') closeCheckoutModal();
  });
  document.getElementById('modalConfirm').addEventListener('click', confirmCheckout);
  document.addEventListener('keydown', onModalKeydown);
}

function onModalKeydown(e) {
  if (e.key === 'Escape') {
    closeCheckoutModal();
    document.removeEventListener('keydown', onModalKeydown);
  }
}

function closeCheckoutModal() {
  document.getElementById('checkoutModal').innerHTML = '';
  document.removeEventListener('keydown', onModalKeydown);
  pendingProduct = null;
}

async function confirmCheckout() {
  if (!pendingProduct) return;
  const btn = document.getElementById('modalConfirm');
  if (!btn || btn.disabled) return;

  btn.disabled = true;
  btn.innerHTML = '<span class="store-modal-loading"></span>A processar...';

  try {
    const body = { productId: pendingProduct.id };
    if (appliedPromo?.code) body.promoCode = appliedPromo.code;
    const { checkoutUrl } = await api('/api/store/checkout', {
      method: 'POST',
      body: JSON.stringify(body),
    });
    if (!checkoutUrl) throw new Error('checkout_failed');
    window.location.href = checkoutUrl;
  } catch (err) {
    const map = {
      unauthorized: 'Faz login primeiro.',
      payments_unavailable: 'Pagamentos indisponíveis.',
      email_not_verified: 'Verifica o email em Conta.',
      invalid_code: 'Código promocional inválido.',
      expired_or_exhausted: 'Código expirado ou esgotado.',
      already_used: 'Já usaste este código.',
      'liga a conta Minecraft primeiro': 'Liga a conta Minecraft.',
      minecraft_not_linked: 'Liga a conta Minecraft em Conta (/link in-game).',
      checkout_failed: 'Checkout indisponível — tenta novamente.',
      product_not_found: 'Produto indisponível.',
      too_many_checkouts: 'Demasiadas tentativas — aguarda 1 min.',
    };
    alert(map[err.message] || err.message);
    btn.disabled = false;
    btn.textContent = 'Confirmar & Pagar';
  }
}

function showSuccessOverlay(message, sub) {
  const mount = document.getElementById('successOverlay');
  mount.innerHTML = `
    <div class="store-success-overlay" id="successOverlayInner">
      <div class="store-success-card">
        <div class="store-success-icon">✓</div>
        <h2 style="margin-bottom:0.5rem;font-size:1.35rem;">${esc(message)}</h2>
        <p class="muted">${esc(sub || '')}</p>
      </div>
    </div>`;
  setTimeout(() => {
    mount.innerHTML = '';
    const url = new URL(location.href);
    url.searchParams.delete('success');
    url.searchParams.delete('session_id');
    history.replaceState({}, '', url.pathname + (url.search || ''));
  }, 5000);
}

function renderCheckoutStatus(params) {
  const el = document.getElementById('checkoutStatus');
  if (!el) return;

  if (params.get('success') === '1') {
    el.innerHTML = `
      <div class="store-status-banner success">
        <span style="font-size:1.5rem;">✅</span>
        <div>
          <strong>Compra realizada com sucesso!</strong>
          <div class="muted" style="font-size:0.88rem;">A entregar recompensas in-game...</div>
        </div>
      </div>`;
    showSuccessOverlay('Compra realizada com sucesso!', 'As recompensas serão entregues em breve no servidor.');

    const sid = params.get('session_id');
    if (sid) {
      api(`/api/store/order/status?session_id=${encodeURIComponent(sid)}`)
        .then((st) => {
          if (st.fulfilled) {
            el.innerHTML = `
              <div class="store-status-banner success">
                <span style="font-size:1.5rem;">🎉</span>
                <div>
                  <strong>Recompensa entregue in-game!</strong>
                  <div class="muted" style="font-size:0.88rem;">Entra no servidor para aproveitar.</div>
                </div>
              </div>`;
            showSuccessOverlay('Recompensa entregue!', 'Entra no MineSpace e aproveita os teus perks.');
          }
        })
        .catch(() => {});
    }
  } else if (params.get('cancelled') === '1') {
    el.innerHTML = `
      <div class="store-status-banner cancelled">
        <span>↩</span>
        <span>Compra cancelada — os teus produtos continuam disponíveis.</span>
      </div>`;
  }
}

document.addEventListener('DOMContentLoaded', async () => {
  const tabs = document.getElementById('categoryTabs');
  const promoForm = document.getElementById('promoForm');
  const params = new URLSearchParams(location.search);

  renderCheckoutStatus(params);

  tabs.innerHTML = CATEGORIES.map((c) =>
    `<button type="button" class="store-tab-btn ${c.id === activeCategory ? 'active' : ''}" data-cat="${c.id}">${c.icon} ${c.label}</button>`
  ).join('');

  tabs.querySelectorAll('.store-tab-btn').forEach((tab) => {
    tab.addEventListener('click', () => {
      activeCategory = tab.dataset.cat;
      tabs.querySelectorAll('.store-tab-btn').forEach((t) =>
        t.classList.toggle('active', t.dataset.cat === activeCategory));
      renderGrid();
    });
  });

  async function tryRedeemStarter(code) {
    document.getElementById('promoError').textContent = '';
    try {
      await api('/api/store/promo/redeem', { method: 'POST', body: JSON.stringify({ code }) });
      document.getElementById('promoSuccess').textContent = '✅ Recompensa NEWPLAYER entregue in-game!';
      return true;
    } catch {
      return false;
    }
  }

  promoForm?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const code = document.getElementById('promoInput').value.trim().toUpperCase();
    const productId = parseInt(document.getElementById('promoProductSelect').value, 10);
    const errEl = document.getElementById('promoError');
    const okEl = document.getElementById('promoSuccess');
    errEl.textContent = '';
    okEl.textContent = '';
    if (!code) return;

    if (code === 'NEWPLAYER' || code.startsWith('NEW')) {
      const ok = await tryRedeemStarter(code);
      if (!ok) errEl.textContent = 'Código inválido ou já usado. Login + Minecraft ligado.';
      return;
    }
    if (!productId) return;

    try {
      const data = await api('/api/store/promo/validate', {
        method: 'POST',
        body: JSON.stringify({ code, productId }),
      });
      appliedPromo = { ...data, productId, code: data.code || code };
      okEl.textContent = `✅ ${data.code}: -€${(data.discountCents / 100).toFixed(2)} → €${data.finalDisplay}`;
      renderFeaturedBundles();
      renderGrid();
    } catch (err) {
      if (err.message === 'use_redeem_endpoint') {
        await tryRedeemStarter(code);
      } else {
        errEl.textContent = err.message;
      }
    }
  });

  document.getElementById('redeemStarterBtn')?.addEventListener('click', async () => {
    document.getElementById('promoInput').value = 'NEWPLAYER';
    const ok = await tryRedeemStarter('NEWPLAYER');
    if (!ok) document.getElementById('promoError').textContent = 'Falha — login + conta Minecraft ligada.';
  });

  try {
    const [tierData, productsRes] = await Promise.all([
      api('/api/store/tiers').catch(() => null),
      api('/api/store/products'),
    ]);

    if (tierData) {
      document.getElementById('tierTableWrap').innerHTML = renderTierMatrix(tierData);
    }

    allProducts = productsRes.products || [];
    const sel = document.getElementById('promoProductSelect');
    if (sel) {
      sel.innerHTML = allProducts.map((p) =>
        `<option value="${p.id}">${esc(p.name)} — €${p.price_display}</option>`
      ).join('');
    }

    renderFeaturedBundles();
    renderGrid();

    setInterval(() => {
      if (allProducts.some((p) => p.sale_ends_at)) {
        renderFeaturedBundles();
        renderGrid();
      }
    }, 1000);
  } catch (err) {
    document.getElementById('productsGrid').innerHTML =
      `<p class="store-empty error">Loja indisponível: ${esc(err.message)}</p>`;
  }
});
