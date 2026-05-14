const fs = require('node:fs');

const files = process.argv.slice(2);
const conflictMarker = /^(<<<<<<<|=======|>>>>>>>) /m;
let failed = false;

for (const file of files) {
  if (!fs.existsSync(file) || !fs.statSync(file).isFile()) {
    continue;
  }
  const source = fs.readFileSync(file, 'utf8');
  if (conflictMarker.test(source)) {
    console.error(`${file}: unresolved merge conflict marker found`);
    failed = true;
  }
}

if (failed) {
  process.exit(1);
}
