# MaidCleaner — Android System Cleaning & Maintenance App

A privacy-focused, ad-free Android system cleaner built with Kotlin and Jetpack Compose. Similar in scope to SD Maid, MaidCleaner helps you reclaim storage space by finding orphaned files, clearing junk, detecting duplicates, and managing installed apps.

## 🏗 Project Structure

```
MaidCleaner/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/maidcleaner/
│       │   ├── MaidCleanerApp.kt          # Hilt Application class
│       │   ├── MainActivity.kt             # Single Activity with Compose
│       │   ├── di/
│       │   │   └── AppModule.kt            # Hilt dependency injection
│       │   ├── data/
│       │   │   ├── model/Models.kt         # All data models
│       │   │   ├── local/
│       │   │   │   ├── MaidDatabase.kt     # Room database + DI
│       │   │   │   ├── entity/Entities.kt  # Room entities
│       │   │   │   └── dao/Daos.kt         # Room DAOs
│       │   │   ├── repository/
│       │   │   │   ├── Repositories.kt     # Whitelist repo
│       │   │   │   ├── SchedulerRepository.kt
│       │   │   │   └── CleanerRepository.kt
│       │   │   └── scanner/
│       │   │       ├── CorpseFinderScanner.kt   # Orphaned file scanner
│       │   │       ├── SystemCleanerScanner.kt   # Junk file scanner
│       │   │       ├── AppScanner.kt             # Installed app scanner
│       │   │       ├── StorageAnalyzerScanner.kt # Storage usage scanner
│       │   │       ├── DuplicateFinderScanner.kt # Duplicate file scanner
│       │   │       └── DatabaseOptimizerScanner.kt
│       │   ├── service/
│       │   │   └── ScanSchedulerWorker.kt  # WorkManager scheduled scans
│       │   ├── root/
│       │   │   └── RootAccessManager.kt    # Root/Shizuku detection
│       │   ├── ui/
│       │   │   ├── theme/                   # Material 3 theme
│       │   │   ├── navigation/Navigation.kt # Compose Navigation
│       │   │   ├── common/                  # Shared UI components
│       │   │   ├── dashboard/               # Home screen
│       │   │   ├── corpsefinder/            # Module 1
│       │   │   ├── systemcleaner/           # Module 2
│       │   │   ├── appcleaner/              # Module 3
│       │   │   ├── appcontrol/              # Module 4
│       │   │   ├── storageanalyzer/         # Module 5
│       │   │   ├── duplicatefinder/         # Module 6
│       │   │   ├── optimizer/               # Module 7
│       │   │   ├── scheduler/               # Module 8
│       │   │   └── permissions/             # Permission handling
│       │   └── util/Utils.kt                # SizeFormatter, HashUtils, etc.
│       └── res/
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## 📋 Features & Module Overview

| Module | Unrooted | Root/Shizuku | Premium |
|--------|----------|-------------|---------|
| **Corpse Finder** | ✅ Limited (accessible dirs only) | ✅ Full access | Free |
| **System Cleaner** | ✅ Limited (public caches, APKs) | ✅ All app caches | Free |
| **App Cleaner** | ✅ Opens system settings | ✅ Direct cache clear | Free |
| **App Control** | ✅ Uninstall, export | ✅ Force-stop, disable, freeze | Free |
| **Storage Analyzer** | ✅ Full | ✅ Full | Free |
| **Duplicate Finder** | ✅ Full | ✅ Full | Premium |
| **Optimizer** | ❌ Public DBs only | ✅ All app DBs | Premium |
| **Scheduler** | ✅ Daily/Weekly | ✅ Daily/Weekly | Premium |

## 🔑 Required Permissions

### Runtime Permissions (requested with in-app rationale)

| Permission | Modules Using It | Rationale |
|------------|-----------------|-----------|
| `READ_EXTERNAL_STORAGE` (≤API 29) | Corpse Finder, System Cleaner, Storage Analyzer, Duplicate Finder | Scan and manage files on storage |
| `READ_MEDIA_IMAGES` (API 33+) | Storage Analyzer, Duplicate Finder | Access image files for analysis |
| `READ_MEDIA_VIDEO` (API 33+) | Storage Analyzer, Duplicate Finder | Access video files for analysis |
| `READ_MEDIA_AUDIO` (API 33+) | Storage Analyzer, Duplicate Finder | Access audio files for analysis |
| `MANAGE_EXTERNAL_STORAGE` (API 30+) | All scanning modules (optional, for full access) | Required for comprehensive scanning across all directories |
| `QUERY_ALL_PACKAGES` | Corpse Finder, App Cleaner, App Control | Identify installed apps for cross-referencing |
| `POST_NOTIFICATIONS` (API 33+) | Scheduler | Notify when scheduled scans complete |
| `SCHEDULE_EXACT_ALARM` | Scheduler | Run scans at exact scheduled times |

### Special Permissions (not runtime)

| Permission | Purpose |
|------------|---------|
| `REQUEST_DELETE_PACKAGES` | Initiate app uninstallation |
| `FOREGROUND_SERVICE` | Run background scan services |
| `WAKE_LOCK` | Wake device for scheduled scans |

## 🚀 Setup & Building

### Prerequisites
- **Android Studio Hedgehog (2023.1.1)** or later
- **JDK 17**
- **Android SDK 34** (compileSdk)
- **Min SDK 26** (Android 8.0)

### Build Steps

1. **Clone the project:**
   ```bash
   git clone https://github.com/your-org/MaidCleaner.git
   cd MaidCleaner
   ```

2. **Open in Android Studio:**
   - File → Open → Select the MaidCleaner directory

3. **Sync Gradle:**
   - Android Studio will prompt to sync; click "Sync Now"

4. **Build the project:**
   ```bash
   ./gradlew assembleDebug
   ```

5. **Install on device:**
   ```bash
   ./gradlew installDebug
   ```

### Shizuku Integration (Optional)

For advanced features without root:
1. Install [Shizuku](https://github.com/RikkaApps/Shizuku) from the Play Store or GitHub
2. Follow Shizuku's setup to start the service (via ADB or wireless debugging)
3. MaidCleaner will automatically detect Shizuku and unlock advanced features

### Root Integration (Optional)

1. If your device is rooted, MaidCleaner auto-detects `su` binary
2. Root access enables:
   - Direct cache clearing for all apps
   - Force-stopping and disabling apps
   - Vacuuming any app's SQLite databases
   - Full access to `/data/data` for corpse finding

## 🏛 Architecture

### Design Patterns
- **MVVM** with `ViewModel` + `StateFlow` for reactive UI
- **Repository pattern** for data access abstraction
- **Dependency injection** via Hilt
- **Coroutines + Flow** for asynchronous operations
- **WorkManager** for scheduled background tasks

### Key Libraries
| Library | Purpose |
|---------|---------|
| Jetpack Compose | Declarative UI |
| Material 3 | Design system |
| Hilt | Dependency injection |
| Room | Local database |
| WorkManager | Scheduled tasks |
| DataStore | Preferences |
| Coil | Image loading |
| Gson | JSON serialization |
| Shizuku API | Elevated access |
| Vico | Charts |

### Scoped Storage Compliance

MaidCleaner strictly respects Android's Scoped Storage restrictions:

- **API 30+**: Uses `MANAGE_EXTERNAL_STORAGE` (with user consent via system settings) or falls back to `READ_MEDIA_*` permissions for media files only
- **API 29**: Uses `READ_EXTERNAL_STORAGE` with `requestLegacyExternalStorage` if needed
- **API 28-**: Uses standard `READ/WRITE_EXTERNAL_STORAGE`
- **App-specific directories**: Accessed without special permissions
- **Other app directories**: Only accessible with root/Shizuku or `MANAGE_EXTERNAL_STORAGE`

The app gracefully degrades when permissions are not available, showing the user what features are limited.

## 🛡 Data Safety

### What MaidCleaner Does NOT Do
- ❌ Collect personal data
- ❌ Transmit any data to servers
- ❌ Include analytics or tracking SDKs
- ❌ Include advertising SDKs
- ❌ Access contacts, location, or camera
- ❌ Share data with third parties

### What MaidCleaner DOES Do
- ✅ All file scanning and cleaning is performed locally on-device
- ✅ Scan results are stored only in the app's private Room database
- ✅ Whitelist/blacklist data is stored locally only
- ✅ Scheduled scan configurations are stored locally only
- ✅ No network permissions are requested or used

### Play Store Data Safety Declaration

| Data Type | Collected | Shared | Purpose |
|-----------|-----------|--------|---------|
| Personal info | No | No | — |
| App activity | No | No | — |
| App info/performance | No | No | — |
| Device/other IDs | No | No | — |

## 💰 Monetization (If Applicable)

- **Core features** (Corpse Finder, System Cleaner, App Cleaner, App Control, Storage Analyzer): **Free forever**
- **Advanced features** (Duplicate Finder, Optimizer, Scheduler): **One-time unlock purchase**
  - No subscriptions
  - No ads
  - No pay-per-scan
  - Single purchase unlocks all premium features permanently

## 🧪 Testing

### Unit Tests
```bash
./gradlew test
```

Tests cover:
- `SizeFormatter` — byte formatting
- `FileClassifier` — file type classification
- `Models` — data model validation
- Repository logic

### Instrumentation Tests
```bash
./gradlew connectedAndroidTest
```

## 📝 Permission Explanation Flow

Every permission in MaidCleaner follows this flow:

1. **User opens a feature** that needs a permission
2. **Rationale dialog** appears explaining WHY the permission is needed
3. **User taps "Grant Permission"** → System permission dialog appears
4. **If denied**: Feature works with limited functionality; user can grant later from Settings
5. **If granted**: Feature works with full functionality

No permission is requested at app startup — only when the user actively uses a feature.

## 🎨 Design

- **Material 3** design language with dynamic color support (Android 12+)
- **Dark mode** fully supported
- **Dashboard** provides at-a-glance storage health with a ring chart
- **All destructive actions** require explicit confirmation with a summary
- **Undo** available where possible (whitelist protection)
- **Accessibility**: Content descriptions on all interactive elements

## 📄 License

This project is provided as-is for educational and development purposes. See individual files for license headers.

---

**Built with ❤️ for privacy-conscious Android users.**
