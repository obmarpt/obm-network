/**
 * Stress test controlado do backend OBM.
 * Uso: node scripts/stress-test.js [url] [concurrency] [requests]
 * Exemplo: node scripts/stress-test.js http://localhost:3000 20 200
 */
const http = require('http');
const https = require('https');

const baseUrl = process.argv[2] || process.env.BACKEND_URL || 'http://localhost:3000';
const concurrency = Math.min(50, Math.max(1, parseInt(process.argv[3], 10) || 10));
const totalRequests = Math.min(500, Math.max(10, parseInt(process.argv[4], 10) || 100));

function requestOnce(url) {
  return new Promise((resolve) => {
    const lib = url.startsWith('https') ? https : http;
    const started = Date.now();
    lib.get(url, (res) => {
      res.resume();
      res.on('end', () => resolve({ ok: res.statusCode >= 200 && res.statusCode < 300, ms: Date.now() - started, code: res.statusCode }));
    }).on('error', () => resolve({ ok: false, ms: Date.now() - started, code: -1 }));
  });
}

async function run() {
  const url = `${baseUrl.replace(/\/$/, '')}/health`;
  console.log(`[STRESS] ${url} | concurrency=${concurrency} | requests=${totalRequests}`);
  const results = [];
  let index = 0;

  async function worker() {
    while (index < totalRequests) {
      const i = index++;
      results[i] = await requestOnce(url);
    }
  }

  const start = Date.now();
  await Promise.all(Array.from({ length: concurrency }, () => worker()));
  const elapsed = Date.now() - start;

  const ok = results.filter((r) => r && r.ok).length;
  const fail = results.length - ok;
  const avgMs = results.reduce((s, r) => s + (r?.ms || 0), 0) / results.length;
  const maxMs = Math.max(...results.map((r) => r?.ms || 0));

  console.log(`[STRESS] done in ${elapsed}ms | ok=${ok} fail=${fail} avg=${avgMs.toFixed(1)}ms max=${maxMs}ms`);
  process.exit(fail > totalRequests * 0.1 ? 1 : 0);
}

run();
