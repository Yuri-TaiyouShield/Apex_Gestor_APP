const crypto = require('node:crypto');
const fs = require('node:fs');
const path = require('node:path');

const root = path.resolve(__dirname, '..', 'dist', 'apex-gestor', 'browser');
const manifestPath = path.join(root, 'integrity-manifest.json');
const files = {};

function walk(dir) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const absolute = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      walk(absolute);
      continue;
    }
    if (entry.name === 'integrity-manifest.json') {
      continue;
    }
    const relative = path.relative(root, absolute).replace(/\\/g, '/');
    files[relative] = crypto.createHash('sha256').update(fs.readFileSync(absolute)).digest('hex');
  }
}

if (!fs.existsSync(root)) {
  throw new Error(`Build directory not found: ${root}`);
}

walk(root);
fs.writeFileSync(manifestPath, JSON.stringify({ algorithm: 'sha256', generatedAt: new Date().toISOString(), files }, null, 2));
console.log(`Integrity manifest generated with ${Object.keys(files).length} files.`);
