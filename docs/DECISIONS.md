# Salty Graphics Architectural Decisions

## 1. Engine vs UI Boundary
- `:engine` is a pure Kotlin + OpenGL ES 3.0 Android library module. It has zero dependency on Jetpack Compose or UI frameworks, guaranteeing complete reusability for future apps (such as the planned motion graphics app).
- `:app` is the Jetpack Compose Material 3 Expressive UI layer. It observes engine document state and issues commands through the `ActionTree`.

## 2. Rendering Stack & OpenGL ES 3.0
- **Stack**: OpenGL ES 3.0 with EGL surface management.
- **Why**: OpenGL ES 3.0 is supported on virtually 100% of Android devices running Android 8.0+ (API 26+). It supports multiple render targets, floating-point textures, uniform buffer objects, and standard GLSL shaders.
- **Preview Proxy**: Render pipeline supports 25%, 50%, and 100% proxy resolutions. Sliders adjust uniforms without reallocating textures.
- **Texture Caching**: Layers render into FBOs and cache texture IDs. Transforms (scale, translation, rotation) only update vertex matrices, keeping framerates at a constant 60fps during gesture manipulation.

## 3. Large Canvas & 8K Safety Strategy
- An 8K RGBA layer (7680x4320x4 bytes) consumes ~132 MB of VRAM per layer.
- **Safety Measures**:
  1. **GPU Texture Limit Check**: Query `GL_MAX_TEXTURE_SIZE` at startup. If canvas exceeds GPU limits (e.g. 4096 on low-end devices), cap texture dimensions to maximum supported texture size and use downsampled preview proxy.
  2. **Memory Budget Warning**: When creating canvases above 4K (e.g., 8K), display a memory warning indicating expected memory pressure.
  3. **Texture Eviction**: Layers not currently in the viewport or hidden are lazily allocated.

## 4. Compatibility, minSdk & ABIs
- **minSdk**: `26` (Android 8.0 Oreo). Enables full OpenGL ES 3.0 support, hardware-accelerated rendering, Java 8/17 language APIs, and modern Storage Access Framework without backward compatibility hacks.
- **targetSdk**: `35` (Android 15).
- **ABIs**: `arm64-v8a`, `armeabi-v7a`, and `x86_64`. Universal APK and per-ABI split APKs generated automatically.

## 5. Action Tree & Command History
- The history is a directed acyclic tree (`ActionTree`) with branching rather than a linear stack.
- Every user modification is encapsulated in an `ActionCommand`.
- Undo moves pointer to the parent node. A new action from an undone state creates a sibling branch rather than discarding previous work.
- Periodic document snapshots are taken every N actions to enable fast O(1) state jumping across distant branches without replaying hundreds of commands.
- The clipboard is global and independent of the active tree branch: users can copy on branch A, jump to branch B, and paste.

## 6. Project Container Format (`.sg`)
- Standalone ZIP archive containing:
  - `manifest.json`: format version (1.0), app version, author, timestamp.
  - `project.json`: full document model (layers, transforms, shapes, vector paths, blend modes, masks, and action tree state).
  - `assets/`: embedded raster images (PNG/JPG/WebP) and custom fonts (TTF/OTF).
- Zero external backend required; fully offline-first.

## Fix round 1: R6 fixed tools pane
The usable workspace (system bars and toolbar excluded) gives portrait tools 40% height, clamped 260–340dp but never more than 45%. At width >=840dp or width greater than height, tools dock right: 40% width, clamped 280–360dp and capped at 45%. Main grids are three columns and never scroll; subpages scroll vertically inside the same bounds. No resizer or drag handle. Canvas fit is recomputed from its actual viewport and document dimensions, with 6% breathing room. Delete records one history action. Existing tool dialogs are now in-panel pages; numeric/color input dialogs remain dedicated input surfaces.

Releases publish only from new v* tags. Main builds upload APK artifacts but must never overwrite beta1 or any previous release.

## R7 project settings
Editor Settings opens a page inside the fixed tools pane, never app settings. Home is the sole app-settings entry. EditorSettings is backward-compatible document data (legacy canvas stays black): background ARGB, grid visibility/size/colour, snap preference, default preview quality. Changes use document history. Alpha background is composited over checkerboard only in preview; PNG/WebP get actual alpha; JPEG flattens over white.

## R8 theme choice
App settings stores Dark/Light/System in SharedPreferences, defaults Dark, and exposes a StateFlow observed above the navigation host. System mode follows configuration changes; dynamic colour remains enabled on Android 12+. All UI surfaces and text/control colours use Material 3 semantic roles, not fixed dark colours. Document/artwork ARGB colours do not change with the app theme. System bars remain transparent with icon contrast following the selected theme. The activity consumes safeDrawing insets at the common root; descendant inset padding does not double-apply consumed insets.

## Historical R6–R8 verification checkpoint (not final release)
R6–R8 were implemented and verified in Actions run 42255375629 (all debug/release JVM tests and release builds). R1–R5 were inherited as done; their missing imports/quarantine enum cases were repaired before advancing. At that historical checkpoint R9–R14 remained pending; consult BUILD_STATE.json for the current queue. No new release tag was published at that checkpoint. UI refinement has not started.

Signing audit: that Actions run used the existing workflow's newly generated fallback keystore, not persistent release secrets. These artifacts must not be described as stable in-place upgrades. Before R14 publishes, persist signing credentials securely and determine whether the beta1 certificate can be retained; never overwrite beta1 assets. The repository contains no keystore or PAT.

## R11 vector editing checkpoint
Backward-compatible `vectorPathData` remains the first path; `additionalVectorPaths` stores subsequent paths. Each path owns fill/border/cap/join/dash. Every completed canvas gesture records one history action; in-flight geometry is preview-only. Coordinate conversion accounts for fit, zoom, pan, rotation, mirroring and nonuniform scaling. Cubic splitting uses de Casteljau so inserting on a curve preserves it. Vector tools and six tabs remain inside the fixed pane; numeric and colour editors reuse the control library. SVG import is offline and bounded (4 MiB, 10,000 XML elements, 20,000 total points); imported curves/arcs become editable sampled polylines with 2px target spacing capped at 2048 samples/contour. Entities, external resources, scripts and gradient fills are rejected. No Keyflaw code or overall visual design was copied. The reference release currently has only the APK, no screenshots. Text path geometry follows the referenced vector transform. Vector image export uses the same native stroke/fill renderer as preview.

Baseline verification: inherited font serialization import and filename family/style parsing were repaired. Robolectric Compose classes require isolated JVM workers (`forkEvery=1`): run 42268003824 passed all debug/release tests and APK compilation after isolation; no tests were disabled. UI refinement remains out of scope.

## Current R11 verification and handoff (2026-11-06)
Actions https://github.com/motionssalt/salty-graphics/actions/runs/42270618002 passed for code commit 2522962: app 23 tests and engine 14 tests in **each** debug/release variant (74 executions total, zero failures/errors/skips), plus all release APK builds. R11 is now done. R1–R10/R12 status was inherited and is not a claim of a new exhaustive acceptance audit. R13 and R14 remain pending; this is a partial fix-round checkpoint, not completion. No new version tag or release was published. Resume at R13; final audit must address the inherited numeric-control, seed-migration, text-layout, font-portability/licence and signing gaps listed in BUILD_STATE.json before any release. UI refinement has not started.


## Fix round 2: S1–S18 Architecture Decisions

### 1. Release Versioning and Stable Signing Key
- Permanent release keystore stored in repository secrets (`RELEASE_KEYSTORE_BASE64`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`).
- Stable certificate fingerprint: SHA-256 `96:65:26:AF:4F:0A:85:3C:95:AA:8E:16:37:3F:ED:70:9D:58:A8:8D:6B:27:54:6C:63:93:E9:E3:9A:FC:47:85`.
- Dynamic versioning in GitHub Actions: `versionCode = 100 + github.run_number`, `versionName = "1.0.${github.run_number}-beta"`. Each release creates a unique tag `v1.0.${github.run_number}`.
- Because previous pre-releases used ephemeral keystores, users must uninstall earlier beta builds once; all subsequent builds share this permanent key and update in-place seamlessly.

### 2. Group Blending and Offscreen Buffer Compositing
- Standard 2D graphics compositing model: nested groups composite their children to an isolated buffer before compositing the group result to the underlying backdrop.
- Supported blend modes expanded to all 23 standard modes across 6 categories (Normal, Darken, Lighten, Contrast, Inversion, Component).
- Reference CPU blending formulas in `BlendMath` implement W3C / Porter-Duff compositing with full alpha blending.

### 3. Storage Scope and Folder Persistence
- StorageManager persists folder tree URI permissions via SAF (`ACTION_OPEN_DOCUMENT_TREE` + `takePersistableUriPermission`).
- Directly creates and writes into `Images/` and `Projects/` subfolders with collision auto-numbering (`_1`, `_2`).
- Graceful fallback to Android MediaStore when no custom folder has been chosen.

### 4. Canvas Gesture Stability and Handle Hierarchy
- Replaced unstable pointerInput keys with `rememberUpdatedState` so pinch-to-zoom and pan gestures are not canceled on state updates.
- Touch target priority: Rotation knob (with 45° snapping within 3° and angle readout pill) -> 4 Corner scale handles -> 4 Edge midpoint handles -> Layer body drag (1:1 with finger) -> Background canvas pan / deselect.
- In-flight drag preview updates canvas at 60fps; single undoable action committed on pointer up.

### 5. High-Contrast Switch Discipline
- Custom `SaltySwitch` enforces >= 3:1 contrast ratio against dark and light surfaces in both ON and OFF states.
- Visible outline in OFF state; solid primary accent track with checkmark icon in ON state; 48dp accessible touch target.

### 6. Shape Live Editing
- Removed modal "Apply" button from Shape dialog. Geometry updates live on canvas; single history action recorded on slider release.

## Fix round 3 — T1 transform safety
The stable canvas pointer coroutine must read both geometry **and callbacks** through `rememberUpdatedState`. Retaining an initial callback reintroduced an obsolete document even though geometry itself was current; this is why the round-2 gesture-key fix did not stop apparent layer deletion. Transform requests now operate on the live document through `SelectionGeometry.apply`, which only copies a target transform and rejects missing/locked targets and invalid numbers. Resizing uses the opposite local anchor transformed into document space; linked corner scaling preserves the existing X:Y ratio, not just equal scale values. Rotation uses pointer-down angular delta about the measured visible center. Selection geometry measures native text metrics and sampled rendered vector paths, unions child/group and multi-selection bounds, and draws one high-contrast overlay last. Targets are 44dp; graphics remain constant-size at every zoom.

Android Compose 1.7.5 synthesizes consumed up changes for ACTION_CANCEL; these must roll back preview rather than commit. Pinch interruptions also roll back the uncommitted edit, and pure layer taps never create a transform node. Verification: [Actions 42351917072](https://github.com/motionssalt/salty-graphics/actions/runs/42351917072), 146 test executions across debug/release, no failures/errors/skips, signed universal and ABI APK artifacts. The round is not complete and no round-3 release is published. No UI refinement.

Release discipline: main pushes retain CI/APK artifacts but no longer publish premature releases. Explicit new version tags publish only after final acceptance; existing stable signing secrets and reference release remain untouched.

## Fix round 3 — T2 bounded project recovery (verified partial checkpoint)
The never-ending spinner came from swallowed repository errors returning null to an editor that only knew how to display Loading. Direct overwrite of the live ZIP and synchronous thumbnail decode during autosave created additional intermittent failure opportunities. Foreground loading now has four measured phases, a 15-second deadline, terminal Loaded/Error and Retry / Open in safe mode / Back. Blocking IO runs in a bounded two-worker bridge; timeout cancels/invalidates the continuation without structurally waiting for a non-cooperating codec. A saturated pool immediately returns a recoverable busy error. Late progress/results are discarded. Timings append to the existing crash diagnostic attachment.

Projects, indexes and restored assets use a unique temporary sibling, flush + fsync, then atomic rename. Failed writes preserve the previous file. Essential autosave is serialized, waits while a foreground load is active and does not await thumbnails. Thumbnail jobs are independent/cancelled when opening. Media is checked off-main during loading; preview cache misses are also asynchronous, without changing export decode/resolution.

Safe mode retains failed raw layers as hidden quarantines and preserves healthy siblings; it does not automatically overwrite the original archive. The media layer active at timeout is bypassed on a safe-mode retry. Recovery uses fresh history so damaged historical snapshots cannot immediately reintroduce failed media. Normal opening restores available saved history.

Verification: [Actions 42353088901](https://github.com/motionssalt/salty-graphics/actions/runs/42353088901), code aea7d95b; app 46 + engine 36 tests in **each** debug/release variant, 164 executions total, zero failures/errors/skips; universal and ABI APKs assembled and signature-verified as artifacts. Tests include interrupted writes, concurrent archive readers/writers, missing and corrupt assets/layers/files, half-written ZIP, an interrupt-ignoring decoder timeout and safe recovery, concurrent autosave/thumbnail work, phase order and the actual recovery UI. Test-fixture compilation/signature issues were corrected; no tests were disabled.

**This is not round completion.** T1/T2 are done; T3–T29 remain pending, with T3 next. No round-3 release/tag was published. Existing releases and stable signing secrets remain untouched. Full sanitized round acceptance brief is `docs/FIX_ROUND_3_PROMPT.md`. No physical-phone tests and no UI refinement.


## Fix round 3 — T3/T4 persistent tools and UI-only deselection
The fixed tools Surface was never dismissed: stale requested-page booleans suppressed the grid even after the selected layer ceased to exist or became incompatible, while each subpage independently refused to render. Resolve page eligibility synchronously from the live selection and clear incompatible requests when selection changes; this guarantees content immediately and prevents a closed page reappearing on a later selection. Transform stays open for compatible layer changes. Empty canvas taps already clear UI selection without history; an accessible Select None action now provides an explicit drawer alternative. Canvas accessibility announces selection count.

Verified [Actions 42357187092](https://github.com/motionssalt/salty-graphics/actions/runs/42357187092), code 718085b: app 49 + engine 36 tests per debug/release variant, 170 executions, zero failures/errors/skips; stable-signed universal/per-ABI APK artifacts assembled and signature checked. Real EditorScreen tests cover same-layer/other-layer/empty taps during Transform, incompatible text-to-shape changes, Select None, empty deselection and creation tools afterwards. Isolated JVM classes resolve Compose idling leakage without disabling tests. T5 is next; round and release remain incomplete. No phone tests, preview frontend or UI refinement.
