const { Router } = require('express');
const { dbWriteGuard } = require('../middleware/dbWriteGuard');
const { globalReadLimiter, writeLimiter } = require('../middleware/rateLimit');
const { suspiciousRequestLogger } = require('../middleware/suspiciousRequest');

const router = Router();

router.use(globalReadLimiter);
router.use(writeLimiter);
router.use(suspiciousRequestLogger);
router.use(dbWriteGuard);
router.use(require('./webPublic'));
router.use(require('./webAuth'));
router.use(require('./gameCatalog'));
router.use(require('./support'));
router.use(require('./supportAdmin'));
router.use(require('./webAdmin'));
router.use(require('./store'));
router.use(require('./storeAdmin'));
router.use(require('./debug'));
router.use(require('./admin'));
router.use(require('./arenas'));
router.use(require('./moderation'));
router.use(require('./anticheat'));
router.use(require('./auth'));
router.use(require('./stats'));
router.use(require('./leaderboards'));
router.use(require('./playerData'));
router.use(require('./players'));
router.use(require('./economy'));
router.use(require('./logs'));
router.use(require('./commands'));
router.use(require('./security'));

module.exports = router;
