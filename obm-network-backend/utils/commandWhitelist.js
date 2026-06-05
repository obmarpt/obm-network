const ALLOWED_PREFIXES = [
  /^say\s+\S.+$/i,
  /^broadcast\s+\S.+$/i,
  /^minecraft:broadcast\s+\S.+$/i,
  /^lp\s+user\s+[\w.-]{1,16}\s+(parent|permission|meta)\s+\S.+$/i,
];

const FORBIDDEN = /[;`|&<>$\\]/;

function isAllowedRemoteCommand(raw) {
  if (typeof raw !== 'string') return false;
  const cmd = raw.trim();
  if (!cmd || cmd.length > 256) return false;
  if (FORBIDDEN.test(cmd)) return false;
  return ALLOWED_PREFIXES.some((re) => re.test(cmd));
}

function sanitizeRemoteCommand(raw) {
  if (!isAllowedRemoteCommand(raw)) return null;
  return raw.trim();
}

module.exports = { isAllowedRemoteCommand, sanitizeRemoteCommand };
