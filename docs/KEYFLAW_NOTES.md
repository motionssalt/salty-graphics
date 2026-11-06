# Keyflaw Architecture Study & Technical Notes

**Reference Asset**: `keyflaw-reference.apk` saved permanently to GitHub pre-release `reference-keyflaw` (`https://github.com/motionssalt/salty-graphics/releases/tag/reference-keyflaw`).

---

## 1. License & Attribution Investigation

- **APK Contents Checked**: Examined `AndroidManifest.xml`, `META-INF/`, assets, resource strings, and decompiled DEX class tables across `classes.dex`, `classes2.dex`, and `classes3.dex`.
- **Findings**: Only standard AndroidX / Google Play Services Apache 2.0 licenses exist in `META-INF/`. No standalone open-source license file (e.g. MIT/GPL) was packaged inside the distribution APK.
- **Decision (per Section 6 working rules)**:
  - Do **not** copy or decompile-transcribe any proprietary source code directly.
  - Study the high-level architecture (FBO render passes, texture caching, shader parameter plumbing) and re-implement the ideas independently in clean, idiomatic Kotlin and OpenGL ES 3.x for Salty Graphics.

---

## 2. Rendering Engine Architecture

Keyflaw achieves exceptional responsiveness on mobile through three core techniques:

### A. Preview Proxy Rendering
- The editing canvas does not continuously composite at 4K or 8K resolution.
- It maintains an offscreen Framebuffer Object (FBO) scaled by the current preview quality factor:
  - **Low**: 25% linear resolution (1/16th pixels, extreme speed, smooth 60fps on low-end chipsets)
  - **Medium**: 50% linear resolution (1/4th pixels, crisp balance)
  - **High**: 100% full project resolution
- On export, rendering switches to a dedicated offscreen full-resolution pipeline.

### B. Texture Caching & Invalidation
- Each layer maintains a cached texture ID.
- Transformations (position, scale, rotation) do not re-rasterize the layer; they only transform the textured quad in vertex coordinates.
- Only when content changes (text edit, shape point move, effect slider change) is the layer's FBO invalidated and re-rendered.

### C. Multi-Input Shader Compositing
- Shaders receive:
  1. `uTexture`: The layer's own texture.
  2. `uBackdrop`: The composited canvas of all layers below it (essential for adjustment layers, blending, and optical effects).
  3. `uCanvasSize`: The total canvas dimension in pixels.
  4. `uLayerBounds`: Normalized bounding box `vec4(x, y, width, height)` of the current layer.
  5. `uTime`: Monotonically increasing time value (defaults to 0.0 for static graphics, ready for future animation).

---

## 3. Effect Parameter Plumbing (.kfpack / Suite Architecture)

- Keyflaw represents plugins/effects using a metadata structure with dynamic control declarations.
- Parameters define `id`, `name`, `type` (`slider`, `toggle`, `dropdown`, `color`, `progress`, `layerSelector`), `default`, `min`, `max`, and options.
- The UI layer dynamically renders controls without bespoke UI components for each effect.
- Salty Graphics adapts this pattern cleanly with Kotlin data classes:
  - `EffectDefinition`
  - `ControlDefinition`
  - Strongly-typed `EffectInstance` holding values
  - Auto-generated Jetpack Compose settings sheets
