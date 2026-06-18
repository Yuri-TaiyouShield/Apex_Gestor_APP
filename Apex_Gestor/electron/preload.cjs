const { contextBridge, ipcRenderer } = require('electron');

const defaultApiUrl = String(
  process.env.APEX_API_BASE_URL
    || process.env.ELECTRON_API_URL
    || process.env.API_BASE_URL
    || 'http://localhost:8080'
).trim().replace(/\/$/, '');

contextBridge.exposeInMainWorld('apexDesktop', {
  platform: process.platform,
  isDesktop: true,
  defaultApiUrl,
  getDeviceInfo: () => ipcRenderer.invoke('desktop:get-info'),
  secureStorage: {
    getItem: (key) => ipcRenderer.invoke('secure-store:get', key),
    setItem: (key, value) => ipcRenderer.invoke('secure-store:set', key, value),
    removeItem: (key) => ipcRenderer.invoke('secure-store:remove', key)
  }
});
