const express = require('express');
const { query } = require('../db');

const router = express.Router();

/**
 * POST /log
 * Body: { staff, action, target, value }
 */
router.post('/', async (req, res) => {
  try {
    const staff = (req.body?.staff && String(req.body.staff).trim().slice(0, 32)) || 'console';
    const action = (req.body?.action && String(req.body.action).trim().slice(0, 64)) || 'UNKNOWN';
    const target = (req.body?.target && String(req.body.target).trim().slice(0, 64)) || '';
    const value = (req.body?.value && String(req.body.value).trim().slice(0, 256)) || '';

    await query(
      `INSERT INTO audit_logs (staff, action, target, value, created_at)
       VALUES ($1, $2, $3, $4, NOW())`,
      [staff, action, target, value]
    ).catch(() => {
      // Tabela opcional — não falhar se não existir
      console.log(`[LOG] ${staff} ${action} ${target} ${value}`);
    });

    res.json({ ok: true });
  } catch (err) {
    console.log(`[LOG] ${req.body?.action || '?'} — ${err.message}`);
    res.json({ ok: true });
  }
});

module.exports = router;
