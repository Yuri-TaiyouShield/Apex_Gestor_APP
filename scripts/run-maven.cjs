const { spawnSync } = require('node:child_process');
const path = require('node:path');

const repoRoot = path.resolve(__dirname, '..');
const backendDir = path.join(repoRoot, 'Apex-Gestordemo');
const executable = process.platform === 'win32' ? 'mvnw.cmd' : './mvnw';
const result = spawnSync(executable, process.argv.slice(2), {
  cwd: backendDir,
  stdio: 'inherit',
  shell: process.platform === 'win32'
});

process.exit(result.status ?? 1);
