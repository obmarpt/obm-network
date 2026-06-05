const RARITY_COLORS = {
  COMMON: '#8b9bb4',
  RARE: '#4ecdc4',
  EPIC: '#a78bfa',
  LEGENDARY: '#f5a623',
  MYTHIC: '#ff6b6b',
};

function rarityBadge(rarity) {
  const c = RARITY_COLORS[rarity] || RARITY_COLORS.COMMON;
  return `<span class="rarity" style="--rarity:${c}">${rarity || 'COMMON'}</span>`;
}

async function loadCatalog() {
  const res = await fetch('/api/public/catalog', { credentials: 'include' });
  if (!res.ok) throw new Error('catalog');
  return res.json();
}
