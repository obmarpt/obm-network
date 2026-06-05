const ROLES = { ADMIN: 'admin', MOD: 'mod' };

function roleLevel(role) {
  if (role === ROLES.ADMIN) return 2;
  if (role === ROLES.MOD) return 1;
  return 0;
}

function requireRole(minRole) {
  return (req, res, next) => {
    const userRole = req.user?.role;
    if (roleLevel(userRole) >= roleLevel(minRole)) {
      return next();
    }
    return res.status(403).json({ error: 'insufficient_permissions', required: minRole });
  };
}

const requireMod = requireRole(ROLES.MOD);
const requireAdmin = requireRole(ROLES.ADMIN);

module.exports = { ROLES, requireMod, requireAdmin, requireRole };
