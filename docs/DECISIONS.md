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
