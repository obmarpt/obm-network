const { Router } = require('express');

const router = Router();

router.use(require('./debug'));
router.use(require('./auth'));
router.use(require('./stats'));
router.use(require('./players'));
router.use(require('./economy'));
router.use(require('./logs'));
router.use(require('./commands'));

module.exports = router;
