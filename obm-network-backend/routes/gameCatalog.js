const { Router } = require('express');
const fs = require('fs');
const path = require('path');
const requestCache = require('../services/requestCache');

const router = Router();
const catalogPath = path.join(__dirname, '..', 'data', 'game-catalog.json');

function loadCatalog() {
  const raw = fs.readFileSync(catalogPath, 'utf8');
  return JSON.parse(raw);
}

router.get('/api/public/catalog', async (_req, res) => {
  try {
    const data = await requestCache.wrap('public:catalog', 300_000, async () => loadCatalog());
    res.json(data);
  } catch (err) {
    res.status(500).json({ error: 'catalog_unavailable' });
  }
});

router.get('/api/public/catalog/:section', async (req, res) => {
  const section = req.params.section;
  try {
    const catalog = await requestCache.wrap('public:catalog', 300_000, async () => loadCatalog());
    if (section === 'battlepass') return res.json({ battlepass: catalog.battlepass });
    if (catalog[section]) return res.json({ [section]: catalog[section] });
    return res.status(404).json({ error: 'not_found' });
  } catch (err) {
    res.status(500).json({ error: 'catalog_unavailable' });
  }
});

module.exports = router;
