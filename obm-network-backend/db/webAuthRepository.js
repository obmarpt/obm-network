const db = require('../database');

async function findByEmail(email) {
  const res = await db.query(
    'SELECT * FROM web_accounts WHERE LOWER(email) = LOWER($1)',
    [email.trim()]
  );
  return res.rows[0] || null;
}

async function findById(id) {
  const res = await db.query('SELECT * FROM web_accounts WHERE id = $1', [id]);
  return res.rows[0] || null;
}

async function findByLinkCode(code) {
  const res = await db.query(
    `SELECT * FROM web_accounts
     WHERE link_code = $1 AND link_code_expires > NOW()`,
    [code.trim().toUpperCase()]
  );
  return res.rows[0] || null;
}

async function createAccount({ email, passwordHash, minecraftUuid, minecraftUsername }) {
  const res = await db.query(
    `INSERT INTO web_accounts (email, password_hash, minecraft_uuid, minecraft_username)
     VALUES ($1,$2,$3,$4) RETURNING *`,
    [email.trim().toLowerCase(), passwordHash, minecraftUuid || null, minecraftUsername || null]
  );
  return res.rows[0];
}

async function updateMinecraftLink(accountId, uuid, username) {
  const res = await db.query(
    `UPDATE web_accounts SET minecraft_uuid = $2, minecraft_username = $3, link_code = NULL, link_code_expires = NULL, updated_at = NOW()
     WHERE id = $1 RETURNING *`,
    [accountId, uuid, username]
  );
  return res.rows[0] || null;
}

async function setLinkCode(accountId, code, expiresMinutes = 15) {
  const res = await db.query(
    `UPDATE web_accounts SET link_code = $2, link_code_expires = NOW() + ($3 || ' minutes')::interval, updated_at = NOW()
     WHERE id = $1 RETURNING *`,
    [accountId, code, String(expiresMinutes)]
  );
  return res.rows[0] || null;
}

async function confirmLinkByCode(code, uuid, username) {
  const account = await findByLinkCode(code);
  if (!account) return null;
  if (account.minecraft_username && account.minecraft_username.toLowerCase() !== username.toLowerCase()) {
    return { error: 'username_mismatch' };
  }
  const taken = await db.query(
    'SELECT id FROM web_accounts WHERE minecraft_uuid = $1 AND id != $2 LIMIT 1',
    [uuid, account.id]
  );
  if (taken.rows.length > 0) {
    return { error: 'uuid_already_linked' };
  }
  const updated = await updateMinecraftLink(account.id, uuid, username);
  return updated;
}

module.exports = {
  findByEmail,
  findById,
  findByLinkCode,
  createAccount,
  updateMinecraftLink,
  setLinkCode,
  confirmLinkByCode,
};
