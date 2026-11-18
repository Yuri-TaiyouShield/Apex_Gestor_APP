# Mobile Gestao

Target v4 location for the Ionic staff app.

Current source: `../../Apex_Gestor`

The mobile staff app uses the shared Angular/Ionic source with the
`mobile-staff` configuration. The configuration must be synchronized before
producing an APK:

```powershell
npm --prefix Apex_Gestor run build:mobile:staff:sync
cd Apex_Gestor\android
\.\gradlew.bat assembleStaffDebug
```

The resulting APK is `Apex_Gestor/android/app/build/outputs/apk/staff/debug/app-staff-debug.apk`.
