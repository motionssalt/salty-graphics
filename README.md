# Salty Graphics (Android)

**Salty Graphics** is a mobile graphic design application for Android, built by **Victor** (solo developer and motion designer, brand **MOTIONSALT**, based in Nigeria). It combines Photoshop-style raster editing and Illustrator-style vector editing into a single unified mobile canvas.

Developed with native Kotlin, Jetpack Compose (Material 3 Expressive), an offline-first GPU render engine, and a branching command action tree.

---

## Key Features

1. **Hybrid Canvas & GPU Render Core**:
   - Offscreen Framebuffer Object (FBO) pipeline with preview proxy scaling (Low 25%, Medium 50%, High 100%).
   - Layer texture caching: transformations do not re-render textures.
   - Safe 8K canvas memory budgeting and GPU texture limits clamping (`GL_MAX_TEXTURE_SIZE`).

2. **Branching Action Tree (Headline Feature)**:
   - History is an open directed tree, not a linear undo stack.
   - If an action is undone and a new action taken, previous work is preserved in sibling branches.
   - Jump directly to any node across any branch.
   - The global clipboard survives branch jumps: copy on branch A, jump to branch B, and paste.

3. **Multi-Type Layer Engine**:
   - **Media**: Import any image (PNG, JPG, WebP) with replace functionality.
   - **Text**: Complete typography controls with **Move along path** (text follows any shape or vector outline).
   - **Shape**: Rectangle, rounded rectangle (corner radius reaches circle/oval), circle, triangle, polygon, star, line, and arrow.
   - **Vector**: Pen tool with cubic Bezier handles.
   - **Adjustment**: Canvas-wide adjustment layer affecting all layers beneath it.
   - **Group**: Hierarchical layer folders with nested editing.

4. **2D Flat Transform System**:
   - Scale with chain-style link/unlink toggle (re-linking scales relatively without resetting values).
   - Precision trackpad with sensitivity slider (switchable to X/Y sliders).
   - Center rotation slider on the flat canvas.

5. **Layer Blending & Masking**:
   - All 16 standard blend modes (Normal, Multiply, Screen, Overlay, Darken, Lighten, Color Dodge, Color Burn, Soft Light, Hard Light, Difference, Exclusion, Hue, Saturation, Color, Luminosity).
   - In-mask (show inside) and out-mask (show outside) modes.

6. **Layer Properties Copy & Paste**:
   - Copy a layer and paste onto another layer or multiple selected layers.
   - User chooses: **Transform only**, **Effects only**, or **Both**.
   - Pasting replaces the target's existing values as specified.

7. **Project Archive (`.sg`)**:
   - Self-contained ZIP package containing `manifest.json`, `project.json`, and `assets/`.
   - Continuous auto-save protects working projects against crashes or system kills.
   - Storage Access Framework (SAF) and Android Share Sheet integration (zero broad storage permissions required).

8. **Export Screen**:
   - PNG (Lossless), JPEG, and WebP at full master document quality.

---

## Project Structure

```
salty-graphics/
├── app/                     # Jetpack Compose UI (Material 3 Expressive)
│   ├── src/main/java/com/motionssalt/saltygraphics/
│   │   ├── MainActivity.kt
│   │   ├── config/SocialConfig.kt
│   │   ├── data/ProjectRepository.kt, DeviceInfoProvider.kt, SettingsManager.kt
│   │   ├── navigation/NavGraph.kt
│   │   ├── theme/ (Theme.kt, Color.kt, Type.kt)
│   │   └── ui/ (HomeScreen, SettingsScreen, NewProjectScreen, EditorScreen, ExportScreen)
│   └── src/main/res/
├── engine/                  # Pure Kotlin + OpenGL ES 3.x Render Engine (Clean API)
│   ├── src/main/java/com/motionssalt/saltygraphics/engine/
│   │   ├── model/ (ProjectDocument, Layer, ShapeData, TextLayerData, Transform2D, etc.)
│   │   ├── action/ (ActionTree, ActionNode, GlobalClipboard, PasteMode)
│   │   ├── effect/ (EffectDefinition, ControlDefinition, ControlType, EffectRegistry)
│   │   ├── render/ (GLRenderEngine, PreviewQuality)
│   │   └── file/ (SgProjectArchive, SgManifest)
│   └── src/test/java/       # Comprehensive Engine Unit Tests
├── docs/
│   ├── BUILD_PROMPT.md      # Full prompt specification
│   ├── KEYFLAW_NOTES.md     # Architecture study of Keyflaw APK
│   └── DECISIONS.md         # Architecture and technical decisions
├── .github/workflows/       # GitHub Actions automated APK build & release CI
└── BUILD_STATE.json         # State and resume manifest
```

---

## Building and Delivery

Victor works entirely from an Android phone (Termux) and cannot run builds locally. All building happens via GitHub Actions:

- Every push to `main` and tag `v*` runs engine unit tests (`./gradlew test`) and builds release APKs (`./gradlew assembleRelease`).
- Produces:
  - Universal APK
  - `arm64-v8a` APK
  - `armeabi-v7a` APK
  - `x86_64` APK
- Automatically published to GitHub Releases for direct download and installation.

---

## Developer Contact

- **Brand**: MOTIONSALT
- **Developer**: Victor (Lagos, Nigeria)
- **Instagram / X / Threads / YouTube**: `@motionssalt`
- **TikTok**: `@motionsalt`
- **WhatsApp**: `09011565415`
