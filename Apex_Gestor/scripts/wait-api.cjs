const healthUrl = process.env.APEX_API_HEALTH_URL || 'http://localhost:8080/actuator/health';
const timeoutMs = Number(process.env.APEX_API_WAIT_TIMEOUT_MS || 120000);
const intervalMs = Number(process.env.APEX_API_WAIT_INTERVAL_MS || 1500);
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
