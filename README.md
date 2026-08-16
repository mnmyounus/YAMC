# Personal File Scraper

A local, on-device Android app that watches folders you choose (defaulting to
DCIM, Pictures, Downloads, and every subfolder under `Android/media/`), copies
new images/videos/documents into a private archive as soon as they appear, and
auto-deletes archived copies after a configurable window (48h by default).
Everything happens on-device — there is no server, no account, and no network
call anywhere in this codebase.

## Architecture

- **MVVM** — one `ViewModel` per screen, exposing `StateFlow`s that Compose collects.
- **Room** — two tables: `archived_files` (what's been archived, with timestamps)
  and `watched_folders` (what's being monitored).
- **DataStore (Preferences)** — monitoring on/off and the retention-hours setting.
- **Manual DI** (`di/AppContainer.kt`) instead of Hilt, so the project builds
  without an annotation-processor/DI-framework setup. Every class already takes
  its dependencies through its constructor, so swapping in Hilt later is a
  mechanical change if you want it.
- **Detection is two-layered, on purpose:**
  - `FileMonitorService` + `RecursiveFileObserver` — a foreground `Service` that
    sets up an inotify-backed `FileObserver` per directory (recursing into
    subfolders) for near-instant detection while it's alive.
  - `SyncWorker` (WorkManager, every 15 min) — a reconciliation pass that
    re-scans watched folders for anything the observer missed. This exists
    because `FileObserver` only reports events while the service process is
    alive, and Android *will* kill that process under some conditions (see
    below) — the periodic scan is what keeps monitoring eventually-consistent.
  - `CleanupWorker` (WorkManager, hourly, as specified) deletes expired files
    and their DB rows.

## Project structure

```
app/src/main/kotlin/com/personal/filescraper/
├── data/db/          Room entities + DAOs + database
├── data/model/        FileType enum
├── data/repository/   ArchiveRepository, SettingsRepository
├── di/                 Manual dependency container + ViewModel factory
├── monitor/            FileObserver wrapper, foreground Service, boot receiver
├── worker/             CleanupWorker, SyncWorker, scheduling
├── notification/       Notification channels + builders
├── util/                File classification, permissions
└── ui/                  MainActivity, theme, nav, and one package per screen
```

## Setup

1. Open the project root in Android Studio (Koala or newer) and let it sync.
2. **This project was scaffolded in a sandboxed environment with no access to
   Google's Maven repository or the Gradle distribution servers, so it has not
   been compiled anywhere.** The code is written carefully and every API used
   is real, but Android Studio's first sync is the first real compile — treat
   that first sync as the actual verification step, and expect to possibly
   nudge a dependency version or two.
3. The Gradle wrapper **jar** (a binary file) isn't included, only
   `gradle/wrapper/gradle-wrapper.properties`. Android Studio will offer to
   regenerate it on sync. For the CLI, run `gradle wrapper` once (needs Gradle
   installed) to create `gradlew`.

### Building without a desktop

Since you're working from mobile: `.github/workflows/build-debug-apk.yml` is a
ready-to-use GitHub Actions workflow. Push this project to a GitHub repo (the
GitHub app or the mobile web editor both work for that), then either push to
`main` or trigger it manually from the **Actions** tab. It builds a debug APK
on GitHub's runners and attaches it to the run as a downloadable artifact — no
desktop needed at any point.

## Permissions, and why each is there

| Permission | Why |
|---|---|
| `MANAGE_EXTERNAL_STORAGE` | The one that does the real work on Android 11+: raw filesystem access to folders like `Android/media/<other app>/`, and `FileObserver` needs real paths, not content URIs. Requested via Settings (`ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION`) — Android doesn't allow a normal runtime dialog for it. |
| `READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE` | Pre-scoped-storage fallback, capped with `maxSdkVersion` since they stop mattering once granular/All-Files permissions take over. |
| `READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO` | Android 13+'s granular replacement, declared for completeness even though `MANAGE_EXTERNAL_STORAGE` supersedes them here. |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_DATA_SYNC` | Required to run the monitoring service. Android 14 requires the type-specific permission in addition to the general one. |
| `POST_NOTIFICATIONS` | Android 13+ requires this even for the persistent "monitoring active" notification. |
| `RECEIVE_BOOT_COMPLETED` | Restarts monitoring after a reboot if it was on. |

## Things worth knowing before you rely on this

- **Android 14's background time limit.** A `dataSync`-typed foreground
  service is capped at roughly 6 hours of *background* runtime per 24-hour
  window on Android 14 — Google intends the type for finite sync jobs, not
  indefinite monitoring. Once the cap hits, the system calls `onTimeout()` and
  the service has to stop; it isn't a crash, but real-time detection pauses
  until you reopen the app or reboot. `SyncWorker`'s 15-minute reconciliation
  isn't subject to this cap, so new files still get caught — just with up to
  ~15 minutes of latency instead of instantly, until the foreground service is
  restarted. This is a real OS policy, not a bug in this code.
- **OEM battery management.** Some manufacturers (MIUI, ColorOS, some Samsung
  configurations, etc.) kill background services more aggressively than stock
  Android regardless of foreground status. If monitoring seems to stop, check
  the device's battery-optimization exemption list for this app.
- **`Android/data/` vs `Android/media/`.** Even with `MANAGE_EXTERNAL_STORAGE`,
  Android blocks direct access to *other apps'* `Android/data/` folders — but
  not `Android/media/`, which is why that's the one this app can watch.
- **`FileObserver` reliability on scoped storage's FUSE layer** has had
  version- and OEM-dependent quirks historically. `SyncWorker` exists
  specifically as a safety net for this, not just for the service-killed case.
- **Play Store policy**, if you ever intend to publish this rather than
  sideload it: `MANAGE_EXTERNAL_STORAGE` apps get manual review, and Google
  expects broad file access to be a clearly-disclosed, core function of the
  app — which matches this app's actual purpose, but is worth knowing going in.

## Customizing

- Default folders / supported extensions: `util/FileUtils.kt`
- Retention presets shown in Settings: `ui/settings/SettingsScreen.kt`
- Cleanup / reconciliation cadence: `worker/WorkerScheduler.kt`
