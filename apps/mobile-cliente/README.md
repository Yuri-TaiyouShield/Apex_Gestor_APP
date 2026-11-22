# Mobile Cliente

Target v4 location for the Ionic consumer app.

Current source: `../../Apex_Gestor`

The mobile client uses the shared Angular/Ionic source with the `mobile-client`
configuration. The configuration must be synchronized before producing an APK:

```powershell
npm --prefix Apex_Gestor run build:mobile:client:sync
cd Apex_Gestor\android
\.\gradlew.bat assembleClientDebug
```

The resulting APK is `Apex_Gestor/android/app/build/outputs/apk/client/debug/app-client-debug.apk`.
