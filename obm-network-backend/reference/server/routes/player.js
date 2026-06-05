const express = require('express');
const { query } = require('../db');

const router = express.Router();

/**
 * GET /player/:uuid
 * Resposta: balance + coins (mesmo valor) para compatibilidade com o plugin.
 */
router.get('/:uuid', async (req, res) => {
  try {
    const uuid = String(req.params.uuid || '').toLowerCase();
    if (!uuid || uuid.length < 32) {
      return res.status(400).json({ error: 'invalid_uuid' });
    }

    const result = await query(
      'SELECT uuid, name, coins, emeralds, rank FROM players WHERE uuid = $1',
      [uuid]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'not_found' });
    }

    const row = result.rows[0];
    const balance = Number(row.coins) || 0;
    res.json({
      uuid: row.uuid,
      name: row.name,
      balance,
      money: balance,
      coins: balance,
      emeralds: Number(row.emeralds) || 0,
      rank: row.rank || 'bronze',
    });
  } catch (err) {
    console.error('[PLAYER] GET', err.message);
    res.status(500).json({ error: 'internal_error' });
  }
});

module.exports = router;
