# Personal File Scraper

A local, on-device Android app that watches folders you choose (defaulting to
DCIM, Pictures, Downloads, and every subfolder under `Android/media/`), copies
new images/videos/documents into a private archive as soon as they appear, and
auto-deletes archived copies after a configurable window (48h by default).
Everything happens on-device — there is no server, no account, and no network
call anywhere in this codebase. There is no `android.permission.INTERNET` entry
in the manifest at all, which means the OS itself refuses this app any socket
access — that's enforced below the app layer, not just a promise in this text.

## Changes after CI + folder-management feedback

- **Release workflow was actually broken.** It called a bare `gradle` command
  with no Gradle Wrapper committed to the repo — that's non-standard and not
  reliably available on the runner, which is almost certainly why it failed.
  The project now includes a real, official Gradle Wrapper (`gradlew`,
  `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar` — fetched directly from
  the `v8.7.0` tag of `gradle/gradle` on GitHub, matching the project's
  declared Gradle version), and both workflows now run `./gradlew` instead of
  a bare `gradle` invocation, which is the pattern every current official
  example uses. If it still fails after this, paste the exact step that
  failed from the Actions log and I'll pin it down precisely.
- **Found the real cause of "auto-selects all files and lags."** `SyncWorker`'s
  reconciliation scan was archiving *every* pre-existing file in a folder the
  first time it scanned it, not just new ones — because it only checked "is
  this in the archive DB yet," not "did this appear after I started watching
  this folder." For a folder like DCIM with years of photos, that's a huge
  one-time burst of copying. It now only considers files modified after the
  folder was added to the watch list (`addedAtMillis`), so "new" actually
  means new.
- **Folders can now be swiped left to remove them — including default ones.**
  Previously, default folders (DCIM, Pictures, Downloads, and every
  auto-added `Android/media/` subfolder) had no way to be removed from the UI
  at all, which meant there was no way to prune the list down if one of those
  was the source of lag. Tap the pencil icon on a folder to rename it.

## Changes after the first install

- **APK size / distribution.** The first build was an unshrunk debug APK
  (~59MB). `app/build.gradle.kts` now enables R8 code shrinking and resource
  shrinking for the release build type, and `.github/workflows/release.yml`
  publishes that shrunk release APK straight to this repo's **Releases** page
  whenever you push a version tag (`git tag v1.0 && git push origin v1.0`) or
  run the workflow manually. The debug workflow from before is still there for
  quick test builds.
- **Lag/crash while browsing folders.** Root cause: the folder picker was
  listing directory contents synchronously on the UI thread on every tap —
  real disk I/O blocking the main thread, which reads as lag or a freeze/crash
  depending on the folder size. It now runs off the main thread with a loading
  spinner. Separately, `RecursiveFileObserver` now catches per-directory
  failures instead of letting one bad folder take the whole thing down, and
  caps how many directories it watches per root so it can't exhaust the
  system's inotify-watch limit (a real failure mode for recursively watching
  something like a messaging app's media folder, which can have dozens of
  subfolders on its own).
- **Folder picker now shows every storage volume** (internal + SD card, where
  present), not just the primary volume, so you can select anywhere on the
  device.
- **"Export all" button** on the Archive screen — copies everything currently
  archived into a folder you pick, before the retention timer deletes it.
  Exported copies are permanent copies; only the internal archive is subject
  to cleanup.
- **File metadata**: the archived copy's filesystem last-modified time is now
  explicitly copied from the original (a fresh output stream doesn't inherit
  this on its own). Embedded metadata (EXIF, etc.) was already preserved
  because copying was always a raw byte copy with no re-encoding step.
- **Not implemented, on purpose**: a notification-listener feature to capture
  and store other apps' message content (referenced as "WAMR"-style) was
  requested and intentionally left out. That requires Android's
  `NotificationListenerService` permission, which exposes literally every
  notification on the device system-wide, and the specific request matches a
  well-known app category built around retaining messages the sender deleted
  — which conflicts with this project's own no-spyware requirement below.

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

Since you're working from mobile, there are two GitHub Actions workflows —
push this project to a GitHub repo (the GitHub app or the mobile web editor
both work for that) and use whichever fits:

- **`build-debug-apk.yml`** — push to `main`, or trigger manually from the
  **Actions** tab. Builds an unshrunk debug APK fast, attached to the run as a
  downloadable artifact. Good for quick test installs while iterating.
- **`release.yml`** — push a version tag (`git tag v1.0 && git push origin v1.0`),
  or trigger manually. Builds the shrunk release APK and publishes it to this
  repo's **Releases** page as `personal-file-scraper.apk` — this is the small,
  properly-shrunk build for actually installing day to day.

Both build on GitHub's runners; no desktop needed at any point.

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
