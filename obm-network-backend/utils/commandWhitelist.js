const ALLOWED_PREFIXES = [
  /^say\s+\S.+$/i,
  /^broadcast\s+\S.+$/i,
  /^minecraft:broadcast\s+\S.+$/i,
  /^lp\s+user\s+[\w.-]{1,16}\s+(parent|permission|meta)\s+\S.+$/i,
  /^lp\s+user\s+[\w.-]{1,16}\s+parent\s+addtemp\s+\S+\s+\d+d$/i,
  /^lp\s+user\s+[\w.-]{1,16}\s+parent\s+remove\s+\S+$/i,
  /^tp\s+[\w.-]{1,16}(\s+[\w.-]{1,16})?$/i,
  /^minecraft:tp\s+[\w.-]{1,16}(\s+[\w.-]{1,16})?$/i,
  /^kick\s+[\w.-]{1,16}(\s+.+)?$/i,
  /^obmstore\s+grant\s+(rank|emeralds|coins|cosmetic)\s+[\w.-]{1,16}\s+\S+$/i,
  /^obmstore\s+grant\s+keys\s+[\w.-]{1,16}\s+(COMMON|RARE|EPIC|LEGENDARY)\s+\d+$/i,
  /^obmstore\s+grant\s+booster\s+(money|bp)\s+[\w.-]{1,16}\s+\d+\s+\d+$/i,
  /^obmstore\s+revoke\s+booster\s+[\w.-]{1,16}\s+(money|bp)$/i,
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
