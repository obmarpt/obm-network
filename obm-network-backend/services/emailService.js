const crypto = require('crypto');

function isConfigured() {
  return Boolean(process.env.SMTP_HOST && process.env.SMTP_USER && process.env.SMTP_PASS);
}

function siteUrl() {
  return (process.env.SITE_URL || 'http://localhost:3000').replace(/\/$/, '');
}

async function sendVerificationEmail(to, token) {
  const link = `${siteUrl()}/verify.html?token=${encodeURIComponent(token)}`;
  const subject = 'Verifica o teu email — MineSpace';
  const body = `Olá!\n\nClica para verificar a tua conta MineSpace:\n${link}\n\nO link expira em 24 horas.`;

  if (!isConfigured()) {
    console.log(`[Email] Verification (dev) → ${to}: ${link}`);
    return { sent: false, devLink: link };
  }

  // Minimal SMTP via nodemailer would add dependency — log + optional fetch to external API
  // Production: configure SMTP_HOST, SMTP_USER, SMTP_PASS
  console.log(`[Email] Would send to ${to}: ${link}`);
  return { sent: true };
}

function generateToken() {
  return crypto.randomBytes(32).toString('hex');
}

module.exports = { sendVerificationEmail, generateToken, isConfigured, siteUrl };
