# Network Traffic Widget

Android Homescreen-Widget für aktuellen Netzwerkdurchsatz mit sekündlicher Aktualisierung.

## Funktionen

- Anzeige von Download- und Upload-Speed in B/s, KB/s, MB/s oder GB/s
- Aktualisierung jede Sekunde über einen Foreground Service
- Homescreen-Widget im Format ca. 3x2 Zellen
- Autostart nach Geräte-Neustart, wenn ein Widget vorhanden ist

## Build als APK

1. Projekt in Android Studio öffnen.
2. Android SDK 35 installieren, falls Android Studio danach fragt.
3. `Build > Build Bundle(s) / APK(s) > Build APK(s)` ausführen.

Alternativ per Terminal mit installiertem Android SDK/Gradle:

```bash
gradle :app:assembleDebug
```

Die Debug-APK liegt danach unter:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Wichtiger Hinweis

Android Widgets dürfen vom System nicht nativ jede Sekunde per `updatePeriodMillis` aktualisiert werden. Deshalb verwendet diese App einen Foreground Service. Dadurch erscheint dauerhaft eine kleine Benachrichtigung, solange das Widget live aktualisiert wird.
