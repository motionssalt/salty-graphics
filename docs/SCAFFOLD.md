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

### Worked Example:
```kotlin
val myFeature = FeatureDefinition(
    id = "blur_effect",
    title = "Blur",
    icon = Icons.Default.BlurOn,
    placement = ToolPlacement.LAYER_SELECTED,
    supportedLayerTypes = setOf(LayerType.MEDIA, LayerType.SHAPE),
    pages = listOf(
        FeaturePageDef(
            id = "blur_main",
            title = "Blur Settings",
            sections = listOf(
                FeatureSectionDef(
                    id = "blur_sec",
                    title = "Parameters",
                    controls = listOf(
                        FeatureControlDef(
                            id = "blur_radius",
                            label = "Radius",
                            type = BoundControlType.RULER_SLIDER,
                            range = 0f..100f,
                            step = 1f,
                            unit = "px",
                            defaultValue = 10f,
                            binding = PropertyBinding(
                                getter = { doc, layerId -> getBlurRadius(doc, layerId) },
                                setter = { doc, layerId, radius ->
                                    val updated = setBlurRadius(doc, layerId, radius)
                                    updated to "Adjust Blur Radius" // Automatic history label!
                                }
                            )
                        )
                    )
                )
            )
        )
    )
)

FeatureRegistry.register(myFeature)
```

Once registered, the tool appears automatically in the bottom panel tool grid, navigates into its page stack, and commits structured history commands on modification.

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
- [ ] **Project Settings**: Migrated to `SaltyPanelPage`, canvas background color picker, grid size ruler slider, and snap toggles.
- [x] **Editor Top Bar**: `TopPanel` is now a binding-only facade over `SaltyEditorTopBar` in `:ui-kit`. Shared library owns layout/tokens; navigation is horizontally scrollable and operations/settings remain pinned on narrow screens. Screenshot matrix remains pending.
- [x] **Bottom Panel Tool Grid**: Production `BottomPanel` supplies action bindings only. `EditorToolFeatures` declares metadata/eligibility, and `ScaffoldToolPanel` in `:ui-kit` draws the existing fixed three-column grid and group breadcrumbs through `FeatureCatalog.queryTools`.
- [ ] **Fill & Gradient**: Migrated to `FillPanelPage`, `SaltyColorPicker`, `SaltyGradientOverlay`, and in-panel project media grid.
- [ ] **Border & Shadow**: Migrated to `BorderShadowPanelPage`, stroke/shadow sections, inside/center/outside chips, blur sliders.
- [ ] **Transform**: Migrated to `TransformPanel`, `SaltyTrackpad`, `SaltyRulerSlider` (Scale, Move, Rotate), center-based scale.
- [ ] **Blending & Masks**: Migrated to `BlendingPage`, 6 category tabs, live thumbnail grid, alpha cutout modes, in-panel.
- [ ] **Text & Text Options**: Migrated to `TextSettingsDialog` panel page, multiline text box, font family picker, alignment chips.
- [ ] **Shape & Shape Settings**: Migrated to `ShapeSettingsDialog` panel page, width/height/radius ruler sliders.
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
boundary is clean. `ScaffoldFeaturePage` still needs production integration, typed
property-path validation, live-document commands, one history command per gesture,
and a complete custom-component registry before V0 is complete. Its old pair-of-document
setter API must not be used to capture and overwrite stale full document snapshots.

`TokenContrastTest` checks token math only. Native rendered tool-grid PNG tests live
in `ScaffoldToolPanelTest`: missing/changed checked-in baselines fail (no auto-accept).
CI uploads actual PNGs for inspection. Golden review must check that images contain
real controls, readable labels and intended states, not accept a blank image just to
make CI green. Other components still need their complete state/theme matrix.
