const { spawnSync } = require('node:child_process');
const fs = require('node:fs');
const path = require('node:path');

const projectRoot = path.resolve(__dirname, '..');
const stageRoot = path.join(projectRoot, '.electron-package');
const appRoot = path.join(stageRoot, 'app');
const distSource = path.join(projectRoot, 'dist', 'apex-gestor', 'browser');
const electronSource = path.join(projectRoot, 'electron');
const builderCli = path.join(projectRoot, 'node_modules', 'electron-builder', 'cli.js');
const releaseDir = path.join(projectRoot, 'release');

function assertInsideProject(target) {
  const relative = path.relative(projectRoot, target);
  if (relative.startsWith('..') || path.isAbsolute(relative)) {
    throw new Error(`Unsafe path outside project: ${target}`);
  }
}

function copyDirectory(source, destination) {
  if (!fs.existsSync(source)) {
    throw new Error(`Missing required path: ${source}`);
  }
  fs.mkdirSync(path.dirname(destination), { recursive: true });
  fs.cpSync(source, destination, { recursive: true });
}

assertInsideProject(stageRoot);
fs.rmSync(stageRoot, { recursive: true, force: true });
fs.mkdirSync(appRoot, { recursive: true });

copyDirectory(distSource, path.join(appRoot, 'dist', 'apex-gestor', 'browser'));
copyDirectory(electronSource, path.join(appRoot, 'electron'));

const packageJson = {
  name: 'apex-gestor-desktop',
  version: '1.0.0',
  private: true,
  author: 'Apex Gestor',
  description: 'Apex Gestor desktop shell.',
  main: 'electron/main.cjs',
  dependencies: {},
  build: {
    appId: 'br.com.apexgestor.desktop',
    productName: 'Apex Gestor',
    directories: {
      output: releaseDir
    },
    asar: true,
    compression: 'maximum',
    npmRebuild: true,
    nodeGypRebuild: false,
    buildDependenciesFromSource: false,
    beforeBuild: 'build/before-build.cjs',
    files: [
      'dist/apex-gestor/browser/**/*',
      'electron/**/*',
      'package.json'
    ],
    win: {
      target: 'nsis',
      signAndEditExecutable: false
    },
    mac: {
      target: 'dmg'
    },
    linux: {
      target: 'AppImage'
    }
  }
};

fs.writeFileSync(path.join(appRoot, 'package.json'), JSON.stringify(packageJson, null, 2));
fs.mkdirSync(path.join(appRoot, 'build'), { recursive: true });
fs.writeFileSync(
  path.join(appRoot, 'build', 'before-build.cjs'),
  "module.exports = async function beforeBuild() { return false; };\n"
);

const result = spawnSync(process.execPath, [builderCli, ...process.argv.slice(2)], {
  cwd: appRoot,
  stdio: 'inherit',
  env: {
    ...process.env,
    ELECTRON_BUILDER_CACHE: process.env.ELECTRON_BUILDER_CACHE || path.join(projectRoot, 'node_modules', '.cache', 'electron-builder')
  }
});

process.exit(result.status ?? 1);
