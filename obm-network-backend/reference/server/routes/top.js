const express = require('express');
const { query } = require('../db');

const router = express.Router();

const STAT_COLUMNS = {
  coins: 'coins',
  emeralds: 'emeralds',
  kills: 'kills',
  playtime: 'playtime',
};

async function fetchTop(statKey, limit = 10) {
  const column = STAT_COLUMNS[statKey];
  if (!column) {
    return null;
  }
  const safeLimit = Math.min(Math.max(parseInt(limit, 10) || 10, 1), 50);
  const sql = `
    SELECT uuid, name, ${column} AS value
    FROM players
    WHERE ${column} > 0
    ORDER BY ${column} DESC
    LIMIT $1
  `;
  const result = await query(sql, [safeLimit]);
  return result.rows.map((row, index) => ({
    rank: index + 1,
    uuid: row.uuid,
    name: row.name || 'Unknown',
    value: Number(row.value) || 0,
  }));
}

function registerTopRoute(path, statKey) {
  router.get(path, async (req, res) => {
    try {
      const limit = req.query.limit;
      const entries = await fetchTop(statKey, limit);
      if (entries === null) {
        return res.status(400).json({ error: 'invalid_stat' });
      }
      res.json({ stat: statKey, entries });
    } catch (err) {
      console.error(`[TOP] ${statKey}`, err.message);
      res.status(500).json({ error: 'internal_error' });
    }
  });
}

registerTopRoute('/coins', 'coins');
registerTopRoute('/emeralds', 'emeralds');
registerTopRoute('/kills', 'kills');
registerTopRoute('/playtime', 'playtime');

module.exports = router;
