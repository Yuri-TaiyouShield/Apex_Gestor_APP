# Desktop PDV

Target v4 location for the Electron point-of-sale shell.

Current source: `../../Apex_Gestor/electron`

The desktop build continues to package the Angular/Ionic dist from
`../../Apex_Gestor` until the native builder paths are migrated.

Build the current shared source with the secure desktop pipeline:

```powershell
npm --prefix Apex_Gestor run build:electron:secure:dir
```

The unpacked desktop bundle is generated under `Apex_Gestor/release/`.
