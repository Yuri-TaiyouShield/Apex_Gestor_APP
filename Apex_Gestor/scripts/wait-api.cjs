const fs = require('node:fs');
const path = require('node:path');

function loadLocalEnv() {
  const envPath = path.join(process.cwd(), '.env.local');
  if (!fs.existsSync(envPath)) {
    return;
  }

  const lines = fs.readFileSync(envPath, 'utf8').split(/\r?\n/);
  for (const line of lines) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith('#') || !trimmed.includes('=')) {
      continue;
    }
    const [key, ...valueParts] = trimmed.split('=');
    if (!process.env[key]) {
      process.env[key] = valueParts.join('=').replace(/^['"]|['"]$/g, '');
    }
  }
}

function readArgValue(name) {
  const direct = process.argv.find((value) => value.startsWith(`${name}=`));
  if (direct) {
    return direct.slice(name.length + 1);
  }
  const index = process.argv.indexOf(name);
  if (index >= 0 && process.argv[index + 1]) {
    return process.argv[index + 1];
  }
  return undefined;
}

function normalizeBaseUrl(value) {
  return String(value || 'http://localhost:8080').trim().replace(/\/$/, '');
}

loadLocalEnv();

const apiBaseUrl = normalizeBaseUrl(
  readArgValue('--api-url')
    || process.env.APEX_API_BASE_URL
    || process.env.ELECTRON_API_URL
    || process.env.API_BASE_URL
);
const healthUrl = process.env.APEX_API_HEALTH_URL || `${apiBaseUrl}/actuator/health`;
const timeoutMs = 120000;
const intervalMs = 1500;
const deadline = Date.now() + timeoutMs;

async function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function isUp() {
  try {
    const response = await fetch(healthUrl);
    if (!response.ok) {
      return false;
    }
    const body = await response.json();
    return body.status === 'UP';
  } catch {
    return false;
  }
}

(async () => {
  while (Date.now() < deadline) {
    if (await isUp()) {
      console.log(`API ready: ${healthUrl}`);
      return;
    }
    await sleep(intervalMs);
  }
  console.error(`API did not become healthy before timeout: ${healthUrl}`);
  process.exit(1);
})();
