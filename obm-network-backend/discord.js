const WEBHOOKS = {
  default: () => process.env.DISCORD_WEBHOOK_URL,
  admin: () => process.env.DISCORD_WEBHOOK_ADMIN || process.env.DISCORD_WEBHOOK_URL,
  economy: () => process.env.DISCORD_WEBHOOK_ECONOMY || process.env.DISCORD_WEBHOOK_URL,
  anticheat: () => process.env.DISCORD_WEBHOOK_ANTICHEAT || process.env.DISCORD_WEBHOOK_URL,
  punish: () => process.env.DISCORD_WEBHOOK_PUNISH || process.env.DISCORD_WEBHOOK_URL,
};

function colorFor(type) {
  switch (type) {
    case 'ANTICHEAT': return 0xff0000;
    case 'PUNISH': return 0x992d22;
    case 'ECONOMY': return 0x2ecc71;
    default: return 0x3498db;
  }
}

function resolveCategory(type) {
  const upper = (type || '').toUpperCase();
  if (upper.includes('ANTICHEAT') || upper.includes('SPARTAN') || upper.includes('GRIM')) {
    return 'anticheat';
  }
  if (upper.includes('BAN') || upper.includes('KICK') || upper.includes('MUTE')) {
    return 'punish';
  }
  if (upper.includes('COIN') || upper.includes('EMERALD') || upper.includes('ECONOMY')) {
    return 'economy';
  }
  return 'admin';
}

async function sendDiscord(category, event) {
  const url = (WEBHOOKS[category] || WEBHOOKS.default)?.();
  if (!url) return;

  const payload = {
    embeds: [{
      title: event.title || `[OBM] ${event.type || 'Event'}`,
      description: event.description || '',
      color: colorFor(event.type),
      fields: (event.fields || []).map((f) => ({
        name: String(f.name ?? ''),
        value: String(f.value ?? '-'),
        inline: f.inline !== false,
      })),
      timestamp: new Date().toISOString(),
    }],
  };

  try {
    const res = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });
    if (!res.ok) {
      console.warn(`⚠️ Discord webhook status ${res.status}`);
    }
  } catch (err) {
    console.warn('⚠️ Discord webhook failed:', err.message);
  }
}

function routeEventToDiscord(event) {
  const category = resolveCategory(event.type);
  return sendDiscord(category, event);
}

function notifyAdminLog({ staff, action, target, value }) {
  return routeEventToDiscord({
    type: action,
    title: `Admin: ${action}`,
    description: `${staff} executou **${action}**`,
    fields: [
      { name: 'Staff', value: staff, inline: true },
      { name: 'Target', value: target || '-', inline: true },
      { name: 'Value', value: value || '-', inline: true },
    ],
  });
}

module.exports = { sendDiscord, routeEventToDiscord, notifyAdminLog };
