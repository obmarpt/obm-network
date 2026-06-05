const https = require('https');

function fetchJson(url) {
  return new Promise((resolve, reject) => {
    const req = https.get(url, { timeout: 8000 }, (res) => {
      let data = '';
      res.on('data', (chunk) => { data += chunk; });
      res.on('end', () => {
        if (res.statusCode === 204 || res.statusCode === 404) {
          return resolve(null);
        }
        if (res.statusCode < 200 || res.statusCode >= 300) {
          return reject(new Error(`Mojang HTTP ${res.statusCode}`));
        }
        try {
          resolve(JSON.parse(data));
        } catch (e) {
          reject(e);
        }
      });
    });
    req.on('error', reject);
    req.on('timeout', () => {
      req.destroy();
      reject(new Error('Mojang timeout'));
    });
  });
}

function formatUuid(raw) {
  if (!raw || raw.includes('-')) return raw;
  const s = raw.replace(/-/g, '');
  if (s.length !== 32) return raw;
  return `${s.slice(0, 8)}-${s.slice(8, 12)}-${s.slice(12, 16)}-${s.slice(16, 20)}-${s.slice(20)}`;
}

async function lookupUsername(username) {
  const name = String(username || '').trim();
  if (!/^[a-zA-Z0-9_]{3,16}$/.test(name)) {
    return null;
  }
  const encoded = encodeURIComponent(name);
  const profile = await fetchJson(`https://api.mojang.com/users/profiles/minecraft/${encoded}`);
  if (!profile || !profile.id) return null;
  return {
    uuid: formatUuid(profile.id),
    username: profile.name || name,
  };
}

module.exports = { lookupUsername, formatUuid };
