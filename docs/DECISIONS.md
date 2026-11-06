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
