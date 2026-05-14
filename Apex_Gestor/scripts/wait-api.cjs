const healthUrl = 'http://localhost:8080/actuator/health';
const timeoutMs = clamp(Number(process.env.APEX_API_WAIT_TIMEOUT_MS || 120000), 5000, 300000);
const intervalMs = clamp(Number(process.env.APEX_API_WAIT_INTERVAL_MS || 1500), 500, 10000);
const deadline = Date.now() + timeoutMs;

function clamp(value, minimum, maximum) {
  if (!Number.isFinite(value)) {
    return minimum;
  }
  return Math.min(Math.max(value, minimum), maximum);
}

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
