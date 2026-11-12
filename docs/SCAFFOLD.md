# Salty Graphics UI Scaffold & Component Library (`:ui-kit`)

This document defines the internal UI scaffolding, design token system, component library, and feature registration architecture for Salty Graphics.

---

## 1. Standing Scaffold Rule: No Hand-Coded Feature Layouts

- **A feature never designs its own layout.** It **registers** itself with the scaffold (its tool-grid entry, its panel pages, its controls and their bindings, declared as data) and the scaffold renders it using the shared library: switches, sliders, pickers, rows, sections, tiles, headers and spacing. Once registered, the feature is immediately there.
- **If a control or layout piece does not exist in the library, add it to the library** (with tests, a screenshot test and a docs entry), then use it by registration. Never build a one-off inside a feature.
- **Everything shares the same tokens** (`SaltyTokens`: spacing, shape, typography, color roles, elevation and motion), so the whole app stays uniform.
- **Editing lives in the bottom panel**: no pop-ups, dialogs, popovers, modal sheets, full-screen pages or floating overlays for editing controls. All editing controls remain inside the bottom panel without obscuring the canvas.

---

## 2. Module Boundary Architecture

The project is structured into three clean Gradle modules:
1. `:engine`: Pure Kotlin & OpenGL ES 3.x render core, document model, action tree, effect definitions, file archives (`.sg`). Has zero UI dependencies.
2. `:ui-kit`: The component library and scaffold registry. Contains all reusable UI controls, tokens, structural containers, and overlays. Depends only on `:engine` and Jetpack Compose foundations.
3. `:app`: Feature implementations, screen containers, activities, and feature registrations. Depends on `:ui-kit` and `:engine`. Feature screens compose UI strictly using `:ui-kit` components and design tokens.

---

## 3. Design Tokens (`SaltyTokens`)

No literal `dp`, `sp`, or raw hex color values in feature code.
- **Spacing:** `SaltyTokens.spacing` (`none`, `xxs`, `xs`, `sm`, `md`, `lg`, `xl`, `xxl`, `panelPadding`, `sectionSpacing`, `rowSpacing`, `controlHeight`).
- **Shapes:** `SaltyTokens.shapes` (`none`, `xs`, `sm`, `md`, `lg`, `xl`, `pill`, `button`, `card`, `tile`, `dialog`).
- **Typography:** `SaltyTokens.typography` (`titleLarge`, `titleMedium`, `titleSmall`, `bodyLarge`, `bodyMedium`, `bodySmall`, `labelLarge`, `labelMedium`, `labelSmall`, `mono`).
- **Colors:** `SaltyTokens.colors` (dynamically resolves semantic roles: `primary`, `secondary`, `accent`, `background`, `surface`, `surfaceVariant`, `outline`, `trackOn`, `trackOff`, `thumbOn`, `thumbOff`, `textPrimary`, `textSecondary`, `snapGuideSmart`, `snapGuideGrid`, `selectionOutline`).

---

## 4. How to Add a Feature via Registration

To add a feature, create a declaration file that calls `FeatureRegistry.register(definition)`.

### Worked example: typed production settings
`app/.../ui/editor/ProjectSettingsFeature.kt` declares the actual project settings page.
`ProjectSettingsPage.kt` passes only its definition, document and event callbacks to
`ScaffoldFeaturePage`. It imports no layout widgets, Material components or UI literals.
The catalog is constructed with `requireTypedBindings = true`, so missing property paths,
feature-owned custom layouts, invalid defaults/options and invalid ranges fail early.

```kotlin
val size = FeatureControlDef(
    id = "grid_size", label = "Grid size", type = BoundControlType.RULER_SLIDER,
    binding = PropertyBinding(DocumentPropertyPath(
        key = "editorSettings.gridSize", target = "Grid", property = "size",
        read = { doc, _ -> doc.editorSettings.gridSize.toFloat() },
        write = { live, _, value ->
            live.updateEditorSettings { it.copy(gridSize = value.toInt()) }
        }
    )),
    range = 1f..1024f, step = 1f, unit = "px", defaultValue = 20f
)
```

A property path is a typed lens. Its write copies **only its own property** from the
live input; never close over a document/settings snapshot. `FeatureChange` contains
only the binding, value, document id and stable target id. At dispatch the host uses:
```kotlin
val live = tree.currentDocument
if (change.appliesTo(live)) {
    recordCommandAndAutoSave(change.identity(live), change::apply)
}
```
The shared renderer generates specific verb/target/property identities. Numeric and
color gestures call `onPreview` without adding history nodes, then `onCommit` once.
Cancel/back/disposal clears an outstanding preview. The editor previews against the
current committed document and does not autosave draft frames. An old event therefore
cannot overwrite an unrelated setting or affect a different project/deleted target.
Typed numeric input is range-checked and applied exactly, without quantizing to a step.
Color rows open a page **inside the same fixed panel**; Hex with alpha and Transparent
commit the same typed binding. Dropdown choices expand inline. Read-only diagnostics
are `FeatureReadoutDef` data, not custom feature layouts.

The legacy getter/setter constructor remains temporarily for unmigrated examples and
tests. It is rejected by strict production catalogs; it is not a sanctioned new-feature
integration route. Registered custom component/overlay extensibility and remaining
control types still need migration; do not interpret this API checkpoint as full V0.

---

## 5. How to Add a New Control to the Library

If a feature requires a new control interaction (e.g. a specialized curve graph or angle dial):
1. Create `Salty<ControlName>.kt` in `ui-kit/src/main/java/com/motionssalt/saltygraphics/uikit/controls/`.
2. Implement it using `SaltyTokens` (no hardcoded colors or sizes, zero haptics on scroll/scrubbing).
3. Add an entry to `BoundControlType` in `FeatureDeclaration.kt`.
4. Wire the renderer in `RenderBoundControl` in `FeatureRegistry.kt`.
5. Add unit and golden contrast/layout tests in `ui-kit/src/test/`.
6. Document the new control in this file.

---

## 6. How to Add an On-Canvas Overlay

On-canvas overlays (like gradient handles, selection boxes, vector point editors) live in `ui-kit/src/main/java/com/motionssalt/saltygraphics/uikit/overlays/`.
- Overlays must draw above the canvas and never obscure the canvas with panels or opaque modal backdrops.
- Handles must be touch-friendly (>= 44dp virtual touch target).
- Multi-handle systems must provide independent movement (e.g. `SaltyGradientOverlay` start/end/center handles move independently; `SaltySelectionOverlay` scales around the object center per V20).

---

## 7. Migration Checklist (All Screens and Panels)

**Acceptance audit, 2026-11-11:** prior checkmarks were unsupported by production
callsites. Unchecked rows below are migration targets, not completed work; their
inherited descriptions describe the intended integration. V0 remains in progress.
The app still has raw layouts and Material dependencies. Do not mark V0 complete
until they are removed and every component has actual rendered golden coverage.


- [ ] **Home Screen**: Migrated to `SaltyTokens`, `SaltyProjectCard`, `SaltyListItem`, `SaltyEmptyState`, and shared switch controls.
- [ ] **Settings Screen**: Migrated to `SaltyTokens`, `SaltySwitch`, `SaltyProgressSlider`, and `SaltySection`.
- [ ] **New Project Screen**: Migrated to `SaltyTokens`, visual ratio tiles, and docked value entry strip.
- [ ] **Export Screen**: Migrated to `SaltyTokens`, progress sliders, format chips, and shared controls.
- [x] **Project Settings**: Production declaration and typed commands use `ScaffoldFeaturePage` with shared controls, panel-native color pages, inline quality selector, readouts and a non-scrolling numeric-entry footer. Real UI/history/pointer tests and reviewed production-theme PNGs pass in Actions 43163047303. Full device IME behavior and whole-app walkthrough remain V24 acceptance, not certified by this screen migration.
- [x] **Editor Top Bar**: `TopPanel` is now a binding-only facade over `SaltyEditorTopBar` in `:ui-kit`. Shared library owns layout/tokens; navigation is horizontally scrollable and operations/settings remain pinned on narrow screens. Screenshot matrix remains pending.
- [x] **Bottom Panel Tool Grid**: Production `BottomPanel` supplies action bindings only. `EditorToolFeatures` declares metadata/eligibility, and `ScaffoldToolPanel` in `:ui-kit` draws the existing fixed three-column grid and group breadcrumbs through `FeatureCatalog.queryTools`.
- [ ] **Fill & Gradient**: Migrated to `FillPanelPage`, `SaltyColorPicker`, `SaltyGradientOverlay`, and in-panel project media grid.
- [ ] **Border & Shadow**: Migrated to `BorderShadowPanelPage`, stroke/shadow sections, inside/center/outside chips, blur sliders.
- [ ] **Transform**: Migrated to `TransformPanel`, `SaltyTrackpad`, `SaltyRulerSlider` (Scale, Move, Rotate), center-based scale.
- [ ] **Blending & Masks**: Migrated to `BlendingPage`, 6 category tabs, live thumbnail grid, alpha cutout modes, in-panel.
- [ ] **Text & Text Options**: Migrated to `TextSettingsDialog` panel page, multiline text box, font family picker, alignment chips.
- [ ] **Shape & Shape Settings**: Geometry page is now a typed `ShapeSettingsFeature` declaration rendered by `ScaffoldFeaturePage`; production UI/history/render acceptance added (awaiting CI). Shape creation picker remains a separate migration target, so this combined row is not checked off.
- [ ] **Vector Tool & Curve Editing**: Migrated to `VectorPage`, independent handles toggle, corner/smooth toggle, angle lock.
- [ ] **Rasterize Layer**: Migrated to `RasterizeLayer` action, safe confirmation dialog offering duplicate option.
- [ ] **Layers Panel**: Migrated to `SaltyLayerRow`, single grab handle, long-press reorder, content thumbnails, select-all toggle.
- [ ] **History Panel / Action Tree**: Migrated to `SaltyHistoryTree`, clean lane layout, branching timeline, pan and zoom.
- [ ] **Effects Engine**: Effect definitions auto-generate settings through the shared control engine.


### V0 verified-scope boundaries and tool registrations
`EditorToolFeatures.kt` is the current production example for **tool** registration.
A `FeatureCatalog` is scoped to its feature host; the legacy `FeatureRegistry` singleton
remains a compatibility API, not a requirement to store editor callbacks globally.
Registrations are snapshot-observable and retain declaration order. Replacing metadata
for the same id updates that position; unregistering removes its tile immediately.
The library validates nonempty feature/page/control metadata, duplicate identifiers,
finite ordered numeric ranges, positive steps and compatible defaults/options before
rendering. The host supplies only `availableActions` and callback dispatch.

A separate `ScaffoldMigrationBoundaryTest` locks down the migrated bottom grid and
registration file. It intentionally does **not** imply the remaining app or module
boundary is clean. `ScaffoldFeaturePage` now has its first production integration in Project Settings, typed
property lenses, live-document commands and separate preview/commit events. A complete
custom-component registry, all remaining screens and every-component golden matrices
still remain before V0 is complete. Legacy pair-of-document setters must never capture
and overwrite stale full document snapshots.

`TokenContrastTest` checks token math only. Native rendered tool-grid PNG tests live
in `ScaffoldToolPanelTest`: missing/changed checked-in baselines fail (no auto-accept).
CI uploads actual PNGs for inspection. Golden review must check that images contain
real controls, readable labels and intended states, not accept a blank image just to
make CI green. Other components still need their complete state/theme matrix.

### V0 Shape geometry migration checkpoint
`ShapeSettingsFeature.kt` declares width, height, rounded-corner radius, polygon sides,
star points and inner-radius ratio as typed lenses. `ShapeSettingsDialog` is retained
as a historical entry-point name only; it contains no dialog, feature layout or tokens.
The production editor dispatches against `ActionTree.currentDocument`, previews without
history/autosave and commits one specifically labelled command per finished gesture.
Shape-specific controls use data visibility and existence predicates; a stale event
cannot change a deleted target, another project, or an incompatible shape type.

Geometry writes must **not** reconstruct FillData: the old editor callback forced a
solid fill's opacity to 1 (or 0) and copied an entire captured ShapeData on resize.
The migrated lens changes just its property, preserving fill alpha, stroke, effects,
transforms, sibling/nested layers and edits made since an event was created.
Existing numeric ranges and reset values are retained; typed width/height values stay
exact rather than snapping to integer steps. No new component styling was introduced.
Tests cover typed bindings, randomized stale events, .sg round trips, actual pointer
preview/commit/cancel, compositor pixel changes and the real editor Edit/Undo/Redo path.
Existing shared ruler/settings goldens remain active; this is not a claim of completed
whole-library golden matrices or finished V0.

### Registered creation choosers
`ScaffoldToolPicker` in `:ui-kit` composes the shared panel header and existing
three-column `ScaffoldToolPanel`. A chooser declares immutable `FeatureDefinition`
choices in its own `FeatureCatalog`, supplies the live document, available action ids
and a dispatch callback; it owns no layout, token literals or global callbacks.
The shared registry's visibility/enabled conditions and runtime registration apply.
`ShapePickerFeature` is the production example: seven existing choices, names, icons
and geometry defaults are unchanged. `ShapePickerDialog` passes only parameters and
maps an action id to immutable ShapeData, then closes the page. There is no nested
scrolling grid or editing overlay. Dark/light chooser goldens are generated by the CI
library-surface test and must be reviewed before acceptance; missing baselines fail.

### Second registered chooser: vector creation
`VectorCreationFeature` registers the existing Draw with points, Rectangle, Ellipse,
Polygon, Star and Import SVG choices. `VectorCreationPage` now delegates to the very
same `ScaffoldToolPicker` used for shapes, with no new layout/styling. Its callback
labels and editor path creation/system SVG picker routes are preserved. Production
Vector settings/toolbars are not migrated by this change; that checklist row remains
open. New tests exercise both the declaration-only chooser and the real editor's
create-path/undo flow. These are pending CI acceptance, not a full vector certification.
