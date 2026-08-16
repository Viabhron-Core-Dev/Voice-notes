# Application Architecture Blueprint: Offline Voice Notes (ColorNote Style)

---

## 1. System Overview & Core Directives

- **Primary Purpose:** Purely offline, lightweight, speech-to-text note-taking and meeting transcription app with a classic ColorNote-style visual identity.
- **Target Platform:** Android 15 Go Edition (64-bit ARM / `arm64-v8a`, 3 GB RAM).
- **Target SDK:** Android API 24–35.
- **Network Footprint:** **Zero network dependencies.** Manifest strictly omits `android.permission.INTERNET` ensuring 100% air-gapped security and privacy.
- **System Insets & Edge-to-Edge:** Proper `WindowInsets.systemBars` / `WindowInsets.safeDrawing` padding applied across all screens so the Android status bar (top) and system gesture/button navigation bar (bottom) are never covered or overlapped by UI elements or FABs.

---

## 2. Audio Processing & Speech-to-Text (STT) Architecture

### A. Raw Audio Recording (No Android Voice Recognition Service / No Cloud)
- **Audio Capture API:** Android Native `AudioRecord`.
- **Audio Format:** 16,000 Hz sample rate, 16-bit linear PCM, Mono channel.
- **Volatile In-Memory Ring Buffer:** Raw audio buffers are processed entirely in memory while recording.
- **Zero Audio Storage:** **No audio files (`.wav`/`.mp3`/`.pcm`) are saved to storage.** Only recognized text and note metadata are persisted. Once transcribed, audio buffers are wiped immediately.
- **Voice Activity Detection (VAD):** Energy-based/WebRTC VAD to segment speech into 3–10s conversational sentences, discarding silence to minimize CPU and battery usage.

### B. Fast On-Demand STT Engine
- **Inference Runtime:** Native `whisper.cpp` / `sherpa-onnx` utilizing ARM NEON SIMD optimizations.
- **On-Demand Fast Loading:** The model is not loaded at cold app start (keeping startup fast and memory under 50 MB). When voice recording is triggered, model weights load quickly into memory (~200–400ms via `mmap`) and stay warm for active use.
- **Zero Background Footprint:** No background services or wakelocks. When the app is closed, all native memory, audio handles, and background threads terminate immediately.

### C. Local Model Import System (Zero Download Bloat)
- **Import Method:** Android Storage Access Framework (`ActivityResultContracts.OpenDocument`).
- **Supported File Types:**
  - `.gguf` (Recommended: `tiny-q5_1.gguf`, `base-q8_0.gguf`, `distil-small-q5_1.gguf`)
  - `.bin` (GGML format: `ggml-tiny.bin`, `ggml-base.en.bin`, `ggml-small.bin`)
- **In-App Guidance & Verification:**
  - In-app specs:
    - *Tiny (~39 MB):* Fast, low latency for quick single-speaker dictation.
    - *Base (~75–140 MB):* Balanced, optimal for multi-speaker meetings and accents.
    - *Small (~240–460 MB):* Highest accuracy for complex audio.
  - Model verification: Validates file headers/quantization and copies model to sandboxed private app storage (`context.filesDir/models/`).

---

## 3. UI & Interaction Design (ColorNote Aesthetic)

### A. System Inset Handling (Edge-to-Edge)
- All root composables and modal sheets utilize `Modifier.windowInsetsPadding(WindowInsets.safeDrawing)` or `Scaffold(contentWindowInsets = ...)` ensuring top status bar and bottom 3-button/gesture system navigation bars never overlap content, headers, or floating action buttons.

### B. Main Screen & Top Bar
- **Top App Bar:**
  - App title ("Voice Notes").
  - **Search Button (`IconButton`):** Quick filter query across titles, body text, and tags.
  - **Multi-Select Toggle:** Enters batch selection mode.
  - **View Toggle:** Switch between 2-column Grid and single-column List view.
  - **Sort Dropdown:** "Sort by color ▼", "Sort by date modified", "Sort by title", "Sort by folder".
  - **Overflow Menu (`⋮`):** Access to *Log Keeper*, *Import Model*, *Backup & Restore*, and *Settings*.
- **Bottom Navigation / Tabs:**
  1. **Notes List:** Active notes.
  2. **Calendar View:** Date-associated notes & meeting schedules.
  3. **Archive Tab:** Archived notes separated from active list.
  4. **Folders Drawer:** Categorized folder notebook view.
  - Center **Floating Action Button (`+`)** for quick creation of text or voice notes.

### C. Color Note Cards & Multi-Select Mode
- **Card Styling:** Full-width or grid cards with pastel background colors:
  - 🟨 **Yellow** (`#FFF9C4`)
  - 🟧 **Peach / Orange** (`#FFE0B2`)
  - 🟥 **Pink / Coral** (`#FFCDD2`)
  - 🟩 **Mint Green** (`#C8E6C9`)
  - 🟦 **Sky Blue** (`#BBDEFB`)
  - 🟪 **Lavender** (`#E1BEE7`)
- **Left colored vertical stripe**, bold title, line clamp, timestamp (e.g. `3:11 pm`, `14 Aug`), and folder badge.
- **Multi-Select Batch Toolbar:** Batch Archive/Unarchive, Delete/Trash, Move to Folder, Change Color, and Multi-Note Export.

### D. Full-Screen Lined Notebook Note Viewer & Editor
- **Full-Screen Notebook Canvas:** Ruled horizontal notebook paper lines matching font line-height.
- **Default View Mode:** Opening a note card opens in read-only View Mode (preventing accidental keyboard pop-up or unintended edits).
- **Double-Tap Anywhere to Edit:**
  - Double-tapping anywhere on the text canvas calculates the exact character offset via `TextLayoutResult.getOffsetForPosition(offset)`.
  - Instantly transitions to Edit Mode, places the blinking cursor/caret at that exact spot, and raises the system keyboard.
- **Session Lifecycle:** Active note stays open while the app is active; auto-saves and closes when the app process exits.
- **Floating Voice Transcriber Widget:**
  - Floating mic button with live audio waveform.
  - Appends recognized speech directly at the active cursor position with Undo/Redo.

---

## 4. Diagnostic LogKeeper Subsystem (Global Access)

- **Access:** Accessible via **global Floating Action Button overlay across all scenes** and through the top-bar 3-dots (`⋮`) menu.
- **UI Specification (Matching LogKeeper reference):**
  - **Top Bar:** Back button (`←`), bold title **Log Keeper**, master logging toggle switch, **Copy All** button, and **Download/Export Logs** button.
  - **Time Filter Tabs:** **`6h`** | **`12h`** | **`24h`** | **`All`**
  - **Card List:** Timestamped entries (`16:53:26.361`), category badge (`VoiceEngine`, `Navigation`, `UI/Editor`, `PdfExport`, `Storage`, `System`), and descriptive log message.

---

## 5. Folder Organization & Multi-Chapter Document Export

### A. Folder Hierarchy
- Notes can be grouped under folders (e.g., *"Project Alpha"*, *"Board Meeting"*, *"Ideas"*).

### B. Multi-Chapter Export (PDF & Plain Text)
When exporting a folder or multi-selected notes:

1. **PDF Export (Native `android.graphics.pdf.PdfDocument`):**
   - Cover header with folder name, date, and chapter count.
   - Each note treated as a separate **Chapter** with chapter title header, creation timestamp, color metadata, and automatic page breaks.
2. **Plain Text Export (`.txt` / `.md`):**
   - Clean UTF-8 structured plain text with chapter boundaries:
   ```text
   ==================================================
   FOLDER: Board Meeting
   Exported: 2026-08-16
   Total Chapters: 3
   ==================================================

   --------------------------------------------------
   CHAPTER 1: Agenda & Objectives
   Date: 16 Aug 2026, 10:00 am
   --------------------------------------------------
   [Note content...]
   ```

---

## 6. Persistence, Security & Offline Backup/Restore

- **Encrypted Local Database:** Android Room Database (SQLite) with encrypted local storage for notes, folders, and model configurations.
- **Survives App Kill/Reopen:** Reactive state management flushes notes automatically on change and lifecycle pause.
- **Offline Backup & Restore:**
  - Backup creation exports an encrypted/raw `.vnbak` / `.json` bundle to internal or SD card storage via SAF.
  - Restore allows selecting existing backup bundles with Merge or Replace options.

---

## 7. Technical Stack Summary

| Component | Technology |
| :--- | :--- |
| **Language & Framework** | Kotlin, Jetpack Compose, Material 3 |
| **Window Insets** | `WindowInsets.systemBars` / `safeDrawing` (No status/nav bar overlap) |
| **Audio Input** | `android.media.AudioRecord` (16kHz 16-bit PCM Mono, in-memory stream only) |
| **STT Engine** | `whisper.cpp` / `sherpa-onnx` on-demand with `mmap` |
| **Model Import** | Storage Access Framework (`.bin`, `.gguf`) |
| **Editor / Canvas** | Lined notebook paper, View Mode default, Double-Tap to Edit with caret placement |
| **Diagnostics** | Global LogKeeper console (Time filters `6h`/`12h`/`24h`/`All`, Export, Copy) |
| **Persistence & Backup** | Encrypted Room Database (SQLite) + SAF Backup/Restore |
| **Export Engine** | Native `android.graphics.pdf.PdfDocument` & UTF-8 Text Streams |
| **Security** | 100% Offline (Zero `INTERNET` permission in AndroidManifest) |

---

## 8. Real-Device Testable Mini-Phases Roadmap

To facilitate on-device testing via direct APK installations, the project is divided into 9 self-contained, buildable mini-phases. Each phase compiles to a working APK and connects to the central LogKeeper engine.

### 🟢 Mini-Phase 1: App Shell, System Bar Inset Protections & LogKeeper UI
- **Scope:**
  - Full edge-to-edge layout with `WindowInsets.safeDrawing` / `systemBars` padding (verifying zero overlap with device status and navigation bars).
  - Global `LogKeeperManager` circular in-memory buffer with millisecond timestamps (`HH:mm:ss.SSS`), category tags (`System`, `Navigation`, `VoiceEngine`, `UI/Editor`, `PdfExport`, `Storage`), and active logging toggle.
  - Complete **LogKeeper Screen** (matching reference image: `6h`/`12h`/`24h`/`All` filters, master toggle, Copy All, and Export `.log`).
  - Global floating FAB overlay & Top-bar 3-dots menu access.
- **On-Phone Test Checkpoint:**
  1. Install APK on phone.
  2. Verify top status bar and bottom gesture/button navigation bar are unobstructed.
  3. Tap LogKeeper FAB, switch time tabs, toggle logging, and test Copy All.
  4. Confirm initial `System` and `Navigation` logs appear with timestamps.

---

### 🟢 Mini-Phase 2: Encrypted Local Database & Auto-Persistence
- **Scope:**
  - Encrypted Room Database (SQLite) with DAOs for Notes, Folders, and Models.
  - Reactive StateFlow with automatic flush on change and lifecycle pause.
  - Starter sample notes for initial testing.
- **On-Phone Test Checkpoint:**
  1. Open app, inspect notes.
  2. Force close app from Recent Apps; relaunch to confirm 100% data retention.
  3. Check LogKeeper `Storage` events for DB connection and transaction times.

---

### 🟢 Mini-Phase 3: ColorNote Main Screen (Cards, Sort, Search & Archive Tab)
- **Scope:**
  - ColorNote card grid and list views with pastel palette (Yellow, Peach, Pink, Mint, Blue, Lavender) and left accent stripes.
  - Top bar **Search** button with real-time text query filtering.
  - Sort dropdown ("Sort by color ▼", "Sort by date", "Sort by title").
  - Bottom navigation bar: **Notes**, **Calendar**, **Archive**, and **Folders** tabs.
- **On-Phone Test Checkpoint:**
  1. Toggle Grid/List views.
  2. Perform search queries and sort by color/date.
  3. Switch between active Notes and Archive tabs.
  4. Inspect search and navigation entries in LogKeeper.

---

### 🟢 Mini-Phase 4: Multi-Select Mode & Batch Operations
- **Scope:**
  - Multi-select toggle button in top bar and card long-press triggers.
  - Batch action toolbar: Batch Archive/Unarchive, Batch Color Change, Batch Move to Folder, Batch Delete.
- **On-Phone Test Checkpoint:**
  1. Multi-select multiple cards; batch change colors.
  2. Batch archive and unarchive notes.
  3. Verify batch operation audit records in LogKeeper.

---

### 🟢 Mini-Phase 5: Lined Notebook Viewer & Double-Tap to Edit
- **Scope:**
  - Full-screen ruled notebook paper canvas matching text line-height.
  - **Default View Mode:** Opens read-only without popping keyboard.
  - **Double-Tap to Edit:** Calculates exact character index from double-tap offset, transitions to Edit mode, positions caret at that exact spot, and raises keyboard.
  - Title editing, color picker dot, and character/word counter.
- **On-Phone Test Checkpoint:**
  1. Open note in View mode (confirm keyboard remains closed).
  2. Double-tap on a specific word; confirm cursor appears at exact spot and keyboard opens.
  3. Edit text, back out to auto-save, and inspect caret coordinates in LogKeeper.

---

### 🟢 Mini-Phase 6: Local Model Importer (SAF .gguf / .bin)
- **Scope:**
  - Model Manager screen accessible via 3-dots menu.
  - Storage Access Framework file picker for local `.gguf` and `.bin` files.
  - In-app specs guidance (Tiny, Base, Small) and header/quantization validator.
  - Active model selection toggle.
- **On-Phone Test Checkpoint:**
  1. Open Model Manager, tap "Import Model File", pick a `.gguf`/`.bin` file.
  2. Verify detected model name, quantization type, and file size.
  3. Confirm `VoiceEngine` validation logs in LogKeeper.

---

### 🟢 Mini-Phase 7: Raw Audio Capture & On-Demand Whisper STT
- **Scope:**
  - Raw `AudioRecord` capture (16kHz 16-bit Mono PCM) in volatile in-memory stream (zero audio written to disk).
  - Fast on-demand model loading (`mmap`) triggered by mic button.
  - Real-time soundwave amplitude visualizer.
  - Real-time text insertion at active cursor with Undo/Redo.
- **On-Phone Test Checkpoint:**
  1. Tap mic button in note editor, speak, and observe real-time speech-to-text insertion.
  2. Check LogKeeper for audio buffer allocations, VAD cuts, and inference latency (RTF factor).

---

### 🟢 Mini-Phase 8: Folder Drawer & Multi-Chapter PDF / Text Export
- **Scope:**
  - Folder creation, assignment, and category drawer.
  - **Multi-Chapter PDF Export:** Native `PdfDocument` with cover header, numbered chapters for each note, timestamps, and clean page breaks.
  - **Multi-Chapter Plain Text Export:** Formatted `.txt` / `.md` file with ASCII chapter borders.
- **On-Phone Test Checkpoint:**
  1. Create a folder with 2–3 notes.
  2. Export folder as PDF and verify chapter headers in a PDF viewer.
  3. Export as Text and verify formatting.
  4. Inspect `PdfExport` page counts and draw times in LogKeeper.

---

### 🟢 Mini-Phase 9: Offline Backup & Restore Engine
- **Scope:**
  - Full database snapshot export to encrypted/raw `.vnbak` / `.json` file via SAF.
  - Restore engine with "Merge" and "Clean Overwrite" options.
- **On-Phone Test Checkpoint:**
  1. Create and save a backup file to phone storage.
  2. Delete a note, restore from backup with "Merge", and confirm note is recovered.
  3. Inspect `Storage` backup/restore entries in LogKeeper.

