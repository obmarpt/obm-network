const { Router } = require('express');
const pool = require('../database');
const { authMiddleware } = require('../auth');

const router = Router();

router.get('/debug/columns', authMiddleware, async (req, res) => {
  try {
    const result = await pool.query(`
      SELECT column_name, data_type
      FROM information_schema.columns
      WHERE table_schema = 'public' AND table_name = 'players'
      ORDER BY ordinal_position
    `);

    res.json({
      columns: result.rows,
      mapped: pool.getPlayerColumns(),
      hint: 'Override via COINS_COLUMN / EMERALDS_COLUMN env vars',
    });
  } catch (err) {
    console.error('Erro (debug/columns):', err);
    res.status(500).json({ error: 'internal error' });
  }
});

module.exports = router;
