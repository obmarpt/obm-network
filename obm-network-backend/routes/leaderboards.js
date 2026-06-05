const { Router } = require('express');
const { pluginOrAuthMiddleware } = require('../auth');
const playerRepo = require('../db/playerRepository');
const { internalError } = require('./validation');

const router = Router();

async function topHandler(metric, res) {
  try {
    const data = await playerRepo.getLeaderboard(metric, 10);
    if (data.error) {
      return res.status(400).json(data);
    }
    res.json(data);
  } catch (err) {
    return internalError(res, err, 'tops/' + metric);
  }
}

router.get('/tops/smp', pluginOrAuthMiddleware, (req, res) => topHandler('kills', res));
router.get('/tops/money', pluginOrAuthMiddleware, (req, res) => topHandler('coins', res));
router.get('/tops/hc', pluginOrAuthMiddleware, (req, res) => topHandler('hc_kills', res));

router.get('/tops/tierspace', pluginOrAuthMiddleware, (req, res) => topHandler('rating', res));
router.get('/tops/emeralds', pluginOrAuthMiddleware, (req, res) => topHandler('emeralds', res));
router.get('/tops/hc-totems', pluginOrAuthMiddleware, (req, res) => topHandler('hc_totems_used', res));

module.exports = router;
