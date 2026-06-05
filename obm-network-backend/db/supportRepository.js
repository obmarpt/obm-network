const db = require('../database');

async function createTicket({ accountId, email, subject, message }) {
  const client = await db.getPool().connect();
  try {
    await client.query('BEGIN');
    const ticketRes = await client.query(
      `INSERT INTO support_tickets (account_id, email, subject) VALUES ($1,$2,$3) RETURNING *`,
      [accountId, email, subject.slice(0, 200)]
    );
    const ticket = ticketRes.rows[0];
    await client.query(
      `INSERT INTO support_messages (ticket_id, sender_type, sender_name, message) VALUES ($1,'player',$2,$3)`,
      [ticket.id, email, message.slice(0, 4000)]
    );
    await client.query('COMMIT');
    return ticket;
  } catch (e) {
    await client.query('ROLLBACK');
    throw e;
  } finally {
    client.release();
  }
}

async function listTicketsForAccount(accountId) {
  const res = await db.query(
    `SELECT t.*, (SELECT COUNT(*)::int FROM support_messages m WHERE m.ticket_id = t.id) AS msg_count
     FROM support_tickets t WHERE t.account_id = $1 ORDER BY t.updated_at DESC LIMIT 50`,
    [accountId]
  );
  return res.rows;
}

async function getTicket(id, accountId) {
  const res = await db.query('SELECT * FROM support_tickets WHERE id = $1', [id]);
  const ticket = res.rows[0];
  if (!ticket) return null;
  if (accountId && ticket.account_id !== accountId) return null;
  const msgs = await db.query(
    'SELECT * FROM support_messages WHERE ticket_id = $1 ORDER BY created_at ASC',
    [id]
  );
  return { ticket, messages: msgs.rows };
}

async function listAllTickets({ status, page = 1, limit = 30 } = {}) {
  const lim = Math.min(100, Math.max(1, limit));
  const pg = Math.max(1, page);
  const offset = (pg - 1) * lim;
  const params = [];
  let where = '';
  if (status) {
    params.push(status);
    where = `WHERE status = $${params.length}`;
  }
  const count = await db.query(`SELECT COUNT(*)::int AS c FROM support_tickets ${where}`, params);
  params.push(lim, offset);
  const res = await db.query(
    `SELECT * FROM support_tickets ${where} ORDER BY updated_at DESC LIMIT $${params.length - 1} OFFSET $${params.length}`,
    params
  );
  return { total: count.rows[0]?.c || 0, page: pg, tickets: res.rows };
}

async function addStaffReply(ticketId, staffName, message) {
  await db.query(
    `INSERT INTO support_messages (ticket_id, sender_type, sender_name, message) VALUES ($1,'staff',$2,$3)`,
    [ticketId, staffName, message.slice(0, 4000)]
  );
  const res = await db.query(
    `UPDATE support_tickets SET status = 'answered', updated_at = NOW() WHERE id = $1 RETURNING *`,
    [ticketId]
  );
  return res.rows[0];
}

async function closeTicket(ticketId) {
  const res = await db.query(
    `UPDATE support_tickets SET status = 'closed', updated_at = NOW() WHERE id = $1 RETURNING *`,
    [ticketId]
  );
  return res.rows[0];
}

module.exports = {
  createTicket,
  listTicketsForAccount,
  getTicket,
  listAllTickets,
  addStaffReply,
  closeTicket,
};
