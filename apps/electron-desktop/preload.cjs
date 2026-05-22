const { contextBridge } = require('electron');

contextBridge.exposeInMainWorld('apexDesktop', {
  platform: process.platform,
  apiBaseUrl: process.env.APEX_API_URL || 'http://localhost:8080',
  versions: {
    chrome: process.versions.chrome,
    electron: process.versions.electron
  }
});
