const { app, BrowserWindow, ipcMain, net, protocol, safeStorage, session, shell } = require('electron');
const crypto = require('node:crypto');
const fs = require('node:fs');
const path = require('node:path');
const { pathToFileURL } = require('node:url');

const isDev = !app.isPackaged;

protocol.registerSchemesAsPrivileged([
  {
    scheme: 'app',
    privileges: {
      standard: true,
      secure: true,
      supportFetchAPI: true
    }
  }
]);

function resolveAssetPath(requestUrl) {
  const distRoot = path.join(app.getAppPath(), 'dist', 'apex-gestor', 'browser');
  const parsed = new URL(requestUrl);
  const requestedPath = decodeURIComponent(parsed.pathname === '/' ? '/index.html' : parsed.pathname);
  const candidate = path.normalize(path.join(distRoot, requestedPath));

  if (!candidate.startsWith(distRoot)) {
    return path.join(distRoot, 'index.html');
  }

  if (fs.existsSync(candidate) && fs.statSync(candidate).isFile()) {
    return candidate;
  }

  return path.join(distRoot, 'index.html');
}

function distRoot() {
  return path.join(app.getAppPath(), 'dist', 'apex-gestor', 'browser');
}

function verifyPackagedIntegrity() {
  if (isDev) {
    return;
  }
  const root = distRoot();
  const manifestPath = path.join(root, 'integrity-manifest.json');
  if (!fs.existsSync(manifestPath)) {
    return;
  }
  const manifest = JSON.parse(fs.readFileSync(manifestPath, 'utf8'));
  for (const [relativePath, expectedHash] of Object.entries(manifest.files || {})) {
    const absolutePath = path.normalize(path.join(root, relativePath));
    if (!absolutePath.startsWith(root) || !fs.existsSync(absolutePath)) {
      throw new Error(`Falha de integridade: ${relativePath}`);
    }
    const actualHash = crypto.createHash('sha256').update(fs.readFileSync(absolutePath)).digest('hex');
    if (actualHash !== expectedHash) {
      throw new Error(`Falha de integridade: ${relativePath}`);
    }
  }
}

function secureStorePath() {
  return path.join(app.getPath('userData'), 'secure-store.json');
}

function readSecureStore() {
  const file = secureStorePath();
  if (!fs.existsSync(file)) {
    return {};
  }
  try {
    return JSON.parse(fs.readFileSync(file, 'utf8'));
  } catch {
    return {};
  }
}

function writeSecureStore(store) {
  fs.mkdirSync(path.dirname(secureStorePath()), { recursive: true });
  fs.writeFileSync(secureStorePath(), JSON.stringify(store, null, 2), { encoding: 'utf8', mode: 0o600 });
}

function assertSecureStoreKey(key) {
  if (!/^apex\.[a-zA-Z0-9._-]{1,64}$/.test(key)) {
    throw new Error('Chave de armazenamento invalida');
  }
}

function encryptValue(value) {
  if (safeStorage.isEncryptionAvailable()) {
    return {
      encrypted: true,
      value: safeStorage.encryptString(value).toString('base64')
    };
  }
  return {
    encrypted: false,
    value: Buffer.from(value, 'utf8').toString('base64')
  };
}

function decryptValue(entry) {
  if (!entry) {
    return null;
  }
  const bytes = Buffer.from(entry.value, 'base64');
  if (entry.encrypted) {
    return safeStorage.decryptString(bytes);
  }
  return bytes.toString('utf8');
}

function desktopDeviceInfo() {
  const source = [
    app.getPath('userData'),
    process.platform,
    process.arch,
    process.env.COMPUTERNAME || process.env.HOSTNAME || ''
  ].join('|');

  return {
    platform: process.platform,
    isDesktop: true,
    defaultApiUrl: 'http://localhost:8080',
    deviceFingerprint: crypto.createHash('sha256').update(source).digest('hex'),
    deviceLabel: `Apex Desktop ${process.platform} ${process.arch}`
  };
}

function configureSecureStoreIpc() {
  ipcMain.handle('secure-store:get', (_event, key) => {
    assertSecureStoreKey(key);
    return decryptValue(readSecureStore()[key]);
  });

  ipcMain.handle('secure-store:set', (_event, key, value) => {
    assertSecureStoreKey(key);
    const store = readSecureStore();
    store[key] = encryptValue(String(value ?? ''));
    writeSecureStore(store);
    return true;
  });

  ipcMain.handle('secure-store:remove', (_event, key) => {
    assertSecureStoreKey(key);
    const store = readSecureStore();
    delete store[key];
    writeSecureStore(store);
    return true;
  });

  ipcMain.handle('desktop:get-info', () => desktopDeviceInfo());
}

function configureSecurityHeaders() {
  const csp = [
    "default-src 'self' app:",
    "script-src 'self' app:",
    "style-src 'self' 'unsafe-inline' app:",
    "img-src 'self' data: app: https:",
    "font-src 'self' data: app:",
    "connect-src 'self' app: http://localhost:* http://127.0.0.1:* ws://localhost:* ws://127.0.0.1:* https:",
    "object-src 'none'",
    "frame-ancestors 'none'",
    "base-uri 'self'"
  ].join('; ');

  session.defaultSession.webRequest.onHeadersReceived((details, callback) => {
    callback({
      responseHeaders: {
        ...details.responseHeaders,
        'Content-Security-Policy': [csp],
        'X-Content-Type-Options': ['nosniff'],
        'Referrer-Policy': ['no-referrer']
      }
    });
  });

  session.defaultSession.setPermissionRequestHandler((_webContents, _permission, callback) => {
    callback(false);
  });
}

async function createWindow() {
  const win = new BrowserWindow({
    width: 1360,
    height: 860,
    minWidth: 1040,
    minHeight: 680,
    backgroundColor: '#f8fafc',
    title: 'Apex Gestor',
    webPreferences: {
      preload: path.join(__dirname, 'preload.cjs'),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: true,
      webSecurity: true,
      allowRunningInsecureContent: false
    }
  });

  win.webContents.setWindowOpenHandler(({ url }) => {
    shell.openExternal(url);
    return { action: 'deny' };
  });

  win.webContents.on('will-navigate', (event, url) => {
    const allowed = isDev ? url.startsWith('http://localhost:4200') || url.startsWith('http://127.0.0.1:4200') : url.startsWith('app://localhost');
    if (!allowed) {
      event.preventDefault();
      shell.openExternal(url);
    }
  });

  if (isDev) {
    await win.loadURL(process.env.ELECTRON_START_URL || 'http://localhost:4200');
    win.webContents.openDevTools({ mode: 'detach' });
  } else {
    await win.loadURL('app://localhost/index.html');
  }
}

app.whenReady().then(async () => {
  configureSecureStoreIpc();
  configureSecurityHeaders();
  verifyPackagedIntegrity();

  if (!isDev) {
    protocol.handle('app', (request) => net.fetch(pathToFileURL(resolveAssetPath(request.url)).toString()));
  }

  await createWindow();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createWindow();
    }
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});
