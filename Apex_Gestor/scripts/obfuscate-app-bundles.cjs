const fs = require('node:fs');
const path = require('node:path');
const JavaScriptObfuscator = require('javascript-obfuscator');

const root = path.resolve(__dirname, '..', 'dist', 'apex-gestor', 'browser');

if (!fs.existsSync(root)) {
  throw new Error(`Build directory not found: ${root}`);
}

const targets = fs.readdirSync(root)
  .filter((file) => /^main-[A-Z0-9]+\.js$/.test(file))
  .map((file) => path.join(root, file));

for (const target of targets) {
  const source = fs.readFileSync(target, 'utf8');
  const obfuscated = JavaScriptObfuscator.obfuscate(source, {
    compact: true,
    stringArray: true,
    stringArrayThreshold: 0.2,
    target: 'browser',
    ignoreImports: true
  }).getObfuscatedCode();

  fs.writeFileSync(target, obfuscated);
  console.log(`Obfuscated app bundle: ${path.basename(target)}`);
}

if (targets.length === 0) {
  console.warn('No app main bundle found to obfuscate.');
}
