const db = require('../database');

async function createToken(accountId, token, hoursValid = 24) {
  await db.query('DELETE FROM email_verification_tokens WHERE account_id = $1 AND used_at IS NULL', [accountId]);
  const res = await db.query(
    `INSERT INTO email_verification_tokens (account_id, token, expires_at)
     VALUES ($1, $2, NOW() + ($3 || ' hours')::interval) RETURNING *`,
    [accountId, token, String(hoursValid)]
  );
  return res.rows[0];
}

async function consumeToken(token) {
  const res = await db.query(
    `UPDATE email_verification_tokens SET used_at = NOW()
     WHERE token = $1 AND used_at IS NULL AND expires_at > NOW()
     RETURNING account_id`,
    [token]
  );
  if (!res.rows[0]) return null;
  const accountId = res.rows[0].account_id;
  await db.query('UPDATE web_accounts SET email_verified = true, updated_at = NOW() WHERE id = $1', [accountId]);
  return accountId;
}

module.exports = { createToken, consumeToken };
