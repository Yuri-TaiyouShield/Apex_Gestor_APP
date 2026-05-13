const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('apexDesktop', {
  platform: process.platform,
  isDesktop: true,
  defaultApiUrl: 'http://localhost:8080',
  getDeviceInfo: () => ipcRenderer.invoke('desktop:get-info'),
  secureStorage: {
    getItem: (key) => ipcRenderer.invoke('secure-store:get', key),
    setItem: (key, value) => ipcRenderer.invoke('secure-store:set', key, value),
    removeItem: (key) => ipcRenderer.invoke('secure-store:remove', key)
  }
});
