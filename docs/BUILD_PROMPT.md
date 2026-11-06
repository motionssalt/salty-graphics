# SALTY GRAPHICS: BUILD PROMPT (new build)

You are a stateless AI builder with full autonomy. You have no memory of earlier sessions. Everything you need is in this document and in the repository. If the repository already exists, read `BUILD_STATE.json` first and continue from where the last AI stopped.

---

## STEP 0: CREDENTIALS (read first)

- Nothing in this project deploys to Cloudflare, so there are deliberately no Cloudflare credentials.
- Use the PAT to create the repository, push to it, create GitHub Releases, and create Actions secrets.
- Never commit the PAT or any keystore. `BUILD_STATE.json` may list secret NAMES only, never values.
- If a value is still a placeholder, do everything that does not need it first. Ask Victor only if the work genuinely cannot continue.

---

## 1. ROLE, AUTONOMY AND WORKING RULES

You own the whole implementation. The brief below states the outcome and constraints, not step-by-step instructions. Choose the implementation details yourself.

Rules, read these twice:

1. **Finish each queue item completely and verify it before starting the next.** Never leave item N half done to start item N+1. Related items (same files, same underlying work) may be done together, and each is still verified separately. Unrelated items are never mixed.
2. **No skimming.** Every item gets proper work: understand it, implement it thoroughly, check it against its acceptance criteria, fix what you find, and only then mark it `done`. Items late in the session get exactly the same depth as the first ones.
3. **Never stop to ask Victor anything** (clarification, confirmation, choices) unless the work truly cannot continue. When something is ambiguous, pick the most sensible default, proceed, and record the assumption in `BUILD_STATE.json` under `decisions`.
4. If an item is truly blocked (missing credential, unreachable dependency), record exactly why, mark it `blocked`, move on, and report it at the end. Difficulty is not a reason to skip or stop.
5. **Commit and push very frequently.** See section 9.
6. **UI refinement is a separate task and is NOT part of this session.** See section 11.

---

## 2. PROJECT BACKGROUND

**Salty Graphics** is a graphic design app for Android, built by Victor (solo developer and motion designer, brand **MOTIONSALT**, based in Nigeria). It is a hybrid of Photoshop-style raster editing and Illustrator-style vector editing in one canvas: user-friendly, with the commonly used features, not every possible feature.

It is the first of a planned family. A motion graphics app will be built later as a separate app, so keep the engine clean and reusable, but do not build any motion or animation features now.

Victor works entirely from an Android phone (Termux). He cannot run builds locally. All building happens through GitHub Actions, and he installs APKs from GitHub Releases.

The app will be a long-term project that grows stage by stage, so **the foundation matters more than the amount of features**: a strong scaffold, a clean engine, and a reusable control system.

---

## 3. PRODUCT SPECIFICATION (the beta)

### 3.1 Screens and navigation

Three main screens: **Home**, **Editing (project) screen**, **Export screen**. Plus Settings and New Project. Every screen except Home has a Back button that returns to the previous screen.

Layer **groups** are layers that contain other layers. Entering a group opens a nested editing screen for that group's contents, with a Back button to return to the parent.

### 3.2 Home screen

- Shows the list of projects.
- A plus button creates a new project (opens the New Project screen).
- An import action imports an `.sg` project file (see 3.12).
- Settings entry.
- Project management: multi-select projects, duplicate, rename, delete.

### 3.3 Settings

- About section
- A place to write down issues you have
- UI scale slider, or "use system default"
- Social links: Instagram, Threads, X and YouTube all use the handle `@motionssalt`; TikTok uses `@motionsalt`. Also a WhatsApp contact: `09011565415`. Keep all of these in one config file so they are easy to change.
- Device info (model, Android version, ABI, screen size, GPU/renderer, max texture size, RAM)
- Report a problem: composes a message with the device info and the written issue and sends it via the Android share sheet, so it works with no backend.

### 3.4 New Project screen

Looks and feels like the Settings panel. Choose an **aspect ratio template** (1:1, 4:5, 9:16, 16:9, 3:2, A4 and similar) or enter a **custom** aspect ratio. Choose **quality** from 360p up to 8K. Quality sets the canvas resolution for the chosen ratio.

8K canvases are heavy (about 130 MB per full-size RGBA layer). Check the device's real GPU limits, warn the user when a size is risky, and make sure the app never crashes on large canvases (tiling, caps, or lazy texture allocation). Record your approach in `decisions`.

### 3.5 Editing screen: top panel

A compact top panel so it takes little space. Buttons, in this arrangement (left to right):

**Back, Layers, View mode, Preview quality** | **Operation, Export, Settings**

- **Layers**: opens the layers side panel.
- **View mode**: zoom in, zoom out, fit to screen, 100%, and a gridlines toggle (with grid size). Pinch to zoom and two-finger pan also always work.
- **Preview quality**: tapping changes the preview resolution. Suggested levels: Low (25%), Medium (50%), High (100%). This only affects the on-screen preview, never export or the project's own quality.
- **Operation**: undo, redo, and the **action tree** (see 3.10).
- **Export**: opens the Export screen.
- **Settings**: opens Settings.
- **Back**: previous screen.

### 3.6 Editing screen: bottom panel

At the bottom of the screen (or floating). It changes depending on selection.

**No layer selected:**
- **Media**: import any kind of image (PNG, JPG, WebP at least) as a media layer.
- **Text**: create a text layer.
- **Adjustment**: create an adjustment layer. It fills the whole canvas, and whatever effects are on it apply to all layers underneath it.
- **Shape**: create a shape layer: rectangle, rounded rectangle, circle/ellipse, triangle, polygon, star, line and arrow (add more if cheap).
- **Vector**: draw your own shape with a pen tool (points, bezier handles, close path). The path-drawing input method must be reusable, because a brush will later reuse the same drawing input.

**A layer selected:**
- **Edit** (behaviour depends on layer type):
  - Media: replace the media with another image.
  - Text: open the text settings (a text layer is created first and its settings only open when Edit is tapped).
  - Adjustment: does nothing.
  - Shape: open the shape settings. Shapes have their own per-shape point settings. Example: a rounded rectangle has a corner radius that can go all the way until it becomes a circle/oval.
  - Vector: edit points.
- **Transform**: scale, move, rotate (see 3.7).
- **Effects**: opens the effect picker. In this beta it is empty because no effects ship yet, but it must be fully wired to the effect engine (see section 4).
- **Blending**: choose the blend mode of the layer (normal, multiply, screen, overlay, darken, lighten, color dodge, color burn, soft light, hard light, difference, exclusion, hue, saturation, color, luminosity).
- **Mask**: use the layer as a mask, with **in-mask** (content only shows inside the mask) or **out-mask** (content only shows outside the mask). A mask applies to the layers it is set to affect.

A layer can be selected from the layers panel or by tapping it on the canvas. A selected layer shows a **bounding box** with handles for quick move, scale and rotate.

### 3.7 Transform (flat 2D only, no 3D)

- **Scale**: sliders. A chain-style toggle links or unlinks X and Y. Linked: one control scales both. Unlinked: separate X and Y sliders. Re-linking does NOT reset values. It just makes them scale relatively again from their current values.
- **Move**: a trackpad with a sensitivity slider, so small drags can give very precise movement. It can be switched to slider mode with separate X and Y sliders.
- **Rotate**: slider(s), rotation around the layer's center on the flat canvas (no X/Y tilt, no perspective).

All 3D and pseudo-3D is out of scope for now.

### 3.8 Layers panel (side panel)

Opens from the Layers button on the **right edge** as a side panel (overlay drawer on phones, docked on wide screens). Each layer row has: thumbnail, name, visibility toggle, lock toggle, and a three-dots drag handle that you hold to reorder. Also: delete, rename, multi-select, select all, copy and paste. Hold a layer for the multi-select/selection mode.

### 3.9 Copy and paste of layer properties

- Copy a single layer. Paste onto another layer or a group of layers.
- When pasting, the user chooses: **Transform only**, **Effects only**, or **Both**.
- Pasting **replaces** the target's existing values. Example: copy a layer that has one effect and paste its effects onto a layer that has 10 effects, and the target ends up with only that one effect.
- Select all layers is supported.

### 3.10 Undo, redo and the action tree

This is a headline feature.

- Every user action is a recorded command. Undo and redo are always available from Operation.
- The history is a **tree, not a line**. If the user does something, undoes it, then does something else, the first path is kept as a separate branch.
- The Operation panel shows the **action tree** with all actions and all branches. The user can jump to any node on any branch, which restores the project to that state.
- The **clipboard survives jumps**: the user can copy something on one branch, jump to another branch, and paste it there.
- Keep it fast and memory-safe: store small commands, take periodic snapshots so jumping is quick, and cap or compact the history for very long sessions. Record your approach in `decisions`.

### 3.11 Text layers

Text settings: font, size, color, justification, alignment, letter and line spacing, and more standard text options. Support the system fonts and importing a font file (TTF/OTF).

**Move along path**: a text setting where the user picks another layer (a shape, or a vector layer) and the text follows that layer's outline/stroke. Shapes and vector layers must therefore expose their path data from the start.

### 3.12 Project file: `.sg`

- Saving a project saves everything the project uses into one file with the extension **`.sg`** (media, fonts, project data), so it can be shared and imported on another phone.
- Suggested format: a ZIP container with `manifest.json` (format version, app version), `project.json` (document model, optionally the action tree), and an `assets/` folder. Always include a format version and keep import backward-compatible.
- Use the Storage Access Framework for saving and opening, so no broad storage permission is needed. Export via the Android share sheet is also supported.
- Auto-save the working project, so a crash or kill never loses work.
- Importing an `.sg` adds it to the Home project list.

### 3.13 Export screen

Choose format (PNG, JPG and WebP at least), quality, and resolution (up to the project's quality). Saves through the Storage Access Framework or the share sheet. Export always renders at full quality from the document, never from the preview proxy.

---

## 4. THE ENGINE (core of this build)

### 4.1 Architecture rules

- The **UI layer** (Jetpack Compose, Material 3 Expressive) only sends commands to the document and shows its state. The **engine** owns the document model, command history, and rendering. They must be cleanly separable.
- **Render on the GPU.** Prefer OpenGL ES 3.x with GLSL shaders, so shader code in the style of Keyflaw `.kfpack` effects (GLSL) can later be reused. Decide the final stack after studying Keyflaw (section 6) and record it.
- Layers are cached as textures and only re-rendered when they change.
- A **preview proxy** renders at the chosen preview quality (25/50/100%) while editing. Export renders from the full-resolution document.
- Responsiveness is the top priority. Sliders must feel immediate even with many layers.

### 4.2 Effect engine (build the engine, ship no real effects)

Victor wants to add effects later **without recoding UI or plumbing each time**. Build it so that adding an effect means adding one definition:

- An **effect definition** declares: name, id, a shader (GLSL), and a list of **controls**.
- Control types, all reusable across the whole app (effects, text settings, shape settings, transform settings): **toggle, slider, dropdown, color picker, progress slider, and layer selector** (to pick another layer). Each control has an id, label, default, and range/options.
- The app **builds the settings panel automatically** from the control list.
- Effects can be applied to a layer, to a group of layers, or to an adjustment layer (where they affect everything below).
- An effect's shader inputs must include: its own layer, the canvas, the **layers below it (backdrop)**, and the **stroke and path data** of shapes and vectors. Do not limit effects to "one layer in, one layer out".
- Provide a **time** input that is fixed at 0 for now. No timeline exists, but this lets future animation work without rewriting effects.
- Effect parameter values are part of the document, copied and pasted with the layer (section 3.9), and recorded in the action tree.
- **Ship zero real effects.** To prove the engine works, include one throwaway test effect that exercises every control type, verify it end to end, then remove it before finishing (or hide it behind a debug flag). The Effects panel stays empty in the beta.

Out of scope now, but the engine must not block them later: brush, eraser (as a mask), crop and gradients (all of which Victor plans to build as effects), text effects, and `.kfpack` import. Do not implement them now.

---

## 5. ENVIRONMENT AND CONSTRAINTS

- **Platform:** native Android, Kotlin, Jetpack Compose, Material 3 with the Expressive style. Use Material Symbols icons only. **No emoji anywhere in the UI.** Proper layered adaptive launcher icon (no white-border square icon).
- **Keep dependencies lean.** No dependency without a clear reason. Use R8/minify for release.
- **Compatibility:** Victor wants it to work on many phones. Choose `minSdk` deliberately (suggest 26 unless the render stack needs more) and record why. Configure ABIs: **arm64-v8a and armeabi-v7a** (plus x86_64 for emulators and Chromebooks) whenever native code is present. If the app is pure Kotlin, still make sure no ABI is excluded by accident.
- **No Rust or custom native engine in this build.** Pure Kotlin plus OpenGL ES. A native core may be added in a later project stage if profiling shows a need, so keep the engine behind clean interfaces.
- **Build and delivery, GitHub Actions only:** every push builds an APK. Releases publish a **universal APK** and per-ABI APKs as GitHub Release assets, tagged `vN`. Sign releases with a stable release keystore: generate it once, store it as GitHub Actions secrets (base64 and passwords), and never commit it. If the PAT lacks permission to write secrets, fall back to a debug-signed APK, log it in `known_issues`, and carry on.
- **Free tier only.** No paid services. No backend.
- **Offline-first.** The app needs no internet.
- **Tests:** unit-test the engine core at least: action tree (branching, jumping, clipboard across branches), copy/paste semantics, `.sg` save/load round trip, and the effect-definition-to-panel mapping. A failing Actions build must be fixed before moving on.
- Follow the same conventions as Victor's other Android builds: Material 3 theme with dynamic color and dark mode, lean Gradle, checkpoint commits, `BUILD_STATE.json` as the source of truth.

---

## 6. KEYFLAW REFERENCE (do not reinvent the wheel)

Victor also uses **Keyflaw**, an open-source video editing app, which is unusually responsive for its effect preview, preview quality and view mode. He wants Salty Graphics to get the same responsiveness without reinventing it.

```
KEYFLAW_APK_URL = https://cf.linkulu.com/dl/6aedb58fde32c56eaccc9f47
```

Keyflaw is distributed through Telegram, so **there is no repository URL**. The reference is its APK.
---
