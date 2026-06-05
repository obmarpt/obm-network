const { Router } = require('express');
const bcrypt = require('bcryptjs');
const crypto = require('crypto');
const { loginLimiter } = require('../middleware/rateLimit');
const webAuthRepo = require('../db/webAuthRepository');
const mojang = require('../services/mojangService');
const {
  signPlayerToken,
  setPlayerCookie,
  clearPlayerCookie,
  playerAuthMiddleware,
} = require('../middleware/playerAuth');
const { pluginOnlyAuth } = require('../auth');
const {
  badRequest, internalError, validateUuid, sanitizeUsername, sanitizeLinkCode,
} = require('./validation');

const emailService = require('../services/emailService');
const emailVerifyRepo = require('../db/emailVerifyRepository');

const router = Router();
const BCRYPT_ROUNDS = 12;

function validEmail(email) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(String(email || '').trim());
}

function isStrongPassword(password) {
  const p = String(password || '');
  return p.length >= 8 && /[A-Z]/.test(p) && /[0-9]/.test(p);
}

function publicAccount(account) {
  return {
    id: account.id,
    email: account.email,
    username: account.minecraft_username,
    uuid: account.minecraft_uuid,
    linked: Boolean(account.minecraft_uuid),
    emailVerified: Boolean(account.email_verified),
  };
}

router.post('/api/web/auth/register', loginLimiter, async (req, res) => {
  const { email, password, minecraftUsername } = req.body || {};
  if (!validEmail(email)) return badRequest(res, 'email inválido');
  if (!isStrongPassword(password)) {
    return badRequest(res, 'password: mín. 8 chars, 1 maiúscula e 1 número');
  }
  if (!minecraftUsername) return badRequest(res, 'minecraft username obrigatório');

  try {
    const existing = await webAuthRepo.findByEmail(email);
    if (existing) return res.status(409).json({ error: 'email_exists' });

    const profile = await mojang.lookupUsername(minecraftUsername);
    if (!profile) return res.status(404).json({ error: 'minecraft_user_not_found' });

    const hash = await bcrypt.hash(String(password), BCRYPT_ROUNDS);
    const account = await webAuthRepo.createAccount({
      email,
      passwordHash: hash,
      minecraftUuid: null,
      minecraftUsername: profile.username,
    });

    const verifyToken = emailService.generateToken();
    await emailVerifyRepo.createToken(account.id, verifyToken);
    const mail = await emailService.sendVerificationEmail(account.email, verifyToken);

    const token = signPlayerToken(account);
    setPlayerCookie(res, token);
    res.status(201).json({
      account: publicAccount(account),
      verification: { sent: mail.sent, devLink: mail.devLink || null },
    });
  } catch (err) {
    return internalError(res, err, 'web/register');
  }
});

router.get('/api/web/auth/verify', async (req, res) => {
  const token = String(req.query.token || '').trim();
  if (!token) return badRequest(res, 'token obrigatório');
  try {
    const accountId = await emailVerifyRepo.consumeToken(token);
    if (!accountId) return res.status(400).json({ error: 'invalid_or_expired_token' });
    res.json({ ok: true, accountId });
  } catch (err) {
    return internalError(res, err, 'web/verify');
  }
});

router.post('/api/web/auth/verify/resend', playerAuthMiddleware, async (req, res) => {
  try {
    const account = await webAuthRepo.findById(req.player.accountId);
    if (!account) return res.status(404).json({ error: 'not_found' });
    if (account.email_verified) return res.json({ ok: true, already: true });

    const verifyToken = emailService.generateToken();
    await emailVerifyRepo.createToken(account.id, verifyToken);
    const mail = await emailService.sendVerificationEmail(account.email, verifyToken);
    res.json({ ok: true, sent: mail.sent, devLink: mail.devLink || null });
  } catch (err) {
    return internalError(res, err, 'web/verify/resend');
  }
});

router.post('/api/web/auth/login', loginLimiter, async (req, res) => {
  const { email, password } = req.body || {};
  if (!validEmail(email) || !password) return badRequest(res, 'credenciais inválidas');

  try {
    const account = await webAuthRepo.findByEmail(email);
    if (!account) return res.status(401).json({ error: 'invalid_credentials' });
    const ok = await bcrypt.compare(String(password), account.password_hash);
    if (!ok) return res.status(401).json({ error: 'invalid_credentials' });

    const token = signPlayerToken(account);
    setPlayerCookie(res, token);
    res.json({ account: publicAccount(account) });
  } catch (err) {
    return internalError(res, err, 'web/login');
  }
});

router.post('/api/web/auth/logout', (_req, res) => {
  clearPlayerCookie(res);
  res.json({ ok: true });
});

router.get('/api/web/auth/me', playerAuthMiddleware, async (req, res) => {
  try {
    const account = await webAuthRepo.findById(req.player.accountId);
    if (!account) return res.status(404).json({ error: 'not_found' });
    res.json({ account: publicAccount(account) });
  } catch (err) {
    return internalError(res, err, 'web/me');
  }
});

router.post('/api/web/auth/link/start', playerAuthMiddleware, async (req, res) => {
  const { username } = req.body || {};
  if (!username) return badRequest(res, 'username obrigatório');
  try {
    const profile = await mojang.lookupUsername(username);
    if (!profile) return res.status(404).json({ error: 'minecraft_user_not_found' });

    const code = crypto.randomBytes(3).toString('hex').toUpperCase();
    await webAuthRepo.setLinkCode(req.player.accountId, code, 15);
    await require('../database').query(
      'UPDATE web_accounts SET minecraft_username = $2, updated_at = NOW() WHERE id = $1',
      [req.player.accountId, profile.username]
    );

    res.json({
      code,
      username: profile.username,
      expiresMinutes: 15,
      instruction: `Entra no servidor e usa /link ${code}`,
    });
  } catch (err) {
    return internalError(res, err, 'web/link/start');
  }
});

router.post('/api/web/link/confirm', pluginOnlyAuth, async (req, res) => {
  const { code, uuid, username } = req.body || {};
  const safeCode = sanitizeLinkCode(code);
  const safeUuid = validateUuid(res, uuid);
  if (!safeUuid) return;
  const safeName = sanitizeUsername(username);
  if (!safeCode || !safeName) return badRequest(res, 'dados inválidos');
  try {
    const result = await webAuthRepo.confirmLinkByCode(safeCode, safeUuid, safeName);
    if (!result) return res.status(404).json({ error: 'invalid_or_expired_code' });
    if (result.error) return res.status(400).json({ error: result.error });
    res.json({ ok: true, uuid: result.minecraft_uuid, username: result.minecraft_username });
  } catch (err) {
    return internalError(res, err, 'web/link/confirm');
  }
});

module.exports = router;
