const { execFileSync } = require('node:child_process');
const fs = require('node:fs');

const files = execFileSync('git', ['ls-files'], { encoding: 'utf8' })
  .split(/\r?\n/)
  .filter(Boolean)
  .filter((file) => !/(package-lock\.json|\.png|\.jpg|\.jpeg|\.pdf|\.ico)$/i.test(file));

const detectors = [
  { label: 'private key', regex: /-----BEGIN [A-Z ]*PRIVATE KEY-----/ },
  { label: 'GitHub token', regex: /\bgh[pousr]_[A-Za-z0-9_]{36,}\b/ },
  { label: 'AWS access key', regex: /\bAKIA[0-9A-Z]{16}\b/ },
  { label: 'Slack token', regex: /\bxox[baprs]-[A-Za-z0-9-]{20,}\b/ }
];

const findings = [];

for (const file of files) {
  const content = fs.readFileSync(file, 'utf8');
  for (const detector of detectors) {
    if (detector.regex.test(content)) {
      findings.push(`${file}: ${detector.label}`);
    }
  }
}

if (findings.length > 0) {
  console.error('Potential committed secret found:');
  for (const finding of findings) {
    console.error(`- ${finding}`);
  }
  process.exit(1);
}

console.log('Secret audit passed: no high-confidence secrets found.');
