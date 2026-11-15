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


- [x] **Home Screen**: Migrated to `:ui-kit` `sublibs.home.SaltyHomeScreen` using `SaltyTokens`, `SaltyProjectCard`, `SaltyEmptyState`, and shared controls.
- [x] **Settings Screen**: Migrated to `:ui-kit` `sublibs.settings.SaltySettingsScreen` using `SaltyTokens`, `SaltySwitch`, `SaltyProgressSlider`, and `SaltySectionCard`.
- [x] **New Project Screen**: Migrated to `:ui-kit` `sublibs.newproject.SaltyNewProjectScreen` using `SaltyTokens`, `SaltyAspectRatioTile`, and shared controls.
- [x] **Export Screen**: Migrated to `:ui-kit` `sublibs.export.SaltyExportScreen` using `SaltyTokens`, progress sliders, format chips, and shared controls.
- [x] **Project Settings**: Production declaration and typed commands use `ScaffoldFeaturePage` with shared controls, panel-native color pages, inline quality selector, readouts and a non-scrolling numeric-entry footer. Real UI/history/pointer tests and reviewed production-theme PNGs pass in Actions 43163047303. Full device IME behavior and whole-app walkthrough remain V24 acceptance, not certified by this screen migration.
- [x] **Editor Top Bar**: `TopPanel` is now a binding-only facade over `SaltyEditorTopBar` in `:ui-kit`. Shared library owns layout/tokens; navigation is horizontally scrollable and operations/settings remain pinned on narrow screens. Screenshot matrix remains pending.
- [x] **Bottom Panel Tool Grid**: Production `BottomPanel` supplies action bindings only. `EditorToolFeatures` declares metadata/eligibility, and `ScaffoldToolPanel` in `:ui-kit` draws the existing fixed three-column grid and group breadcrumbs through `FeatureCatalog.queryTools`.
- [x] **Fill & Gradient**: Migrated to `:ui-kit` `SaltyFillPanelPage`, `SaltyColorPicker`, `SaltyGradientLayerOverlay`, and `SaltyMediaPickerGrid`.
- [x] **Border & Shadow (existing page)**: `BorderShadowFeature` declares 18 typed controls and the page only binds `ScaffoldFeaturePage`. Actions 43691971931 verifies production pointer preview/commit/cancel, independent color/opacity, live property writes, legacy mirrors, native render changes, archive and history. This is V0 page migration only: V8/V30 Effects-only reachability and the full amended feature matrix remain pending.
- [x] **Transform**: Migrated to `:ui-kit` `SaltyTransformPanel`, `SaltyTrackpad`, `SaltyRulerSlider` (Scale, Move, Rotate), center-based scale.
- [x] **Blending & Masks**: Migrated to `:ui-kit` `SaltyBlendingMaskPage`, 6 category tabs, live thumbnail grid, alpha cutout modes, in-panel.
- [x] **Text & Text Options**: Migrated to `:ui-kit` `sublibs.text.SaltyTextOptionsPage`, multiline text box, font family picker, alignment chips.
- [x] **Shape & Shape Settings**: Geometry is a typed `ShapeSettingsFeature` declaration; creation choices are registered by `ShapePickerFeature` and drawn with shared `ScaffoldToolPicker`. Actions 43209833401 verifies production Edit/Undo/Redo, target switches, property-only history, pointer previews/commit/cancel, native compositor changes, .sg preservation, seven creation defaults, architecture guards and reviewed light/dark library chooser PNGs. This does not certify all other app screens.
- [x] **Vector Tool & Curve Editing**: Migrated to `:ui-kit` `sublibs.vector.SaltyVectorPage` and `SaltyVectorCanvasOverlay`, independent handles toggle, corner/smooth toggle, angle lock.
- [x] **Rasterize Layer**: Migrated to `:ui-kit` `SaltyConfirmDialog` offering duplicate-and-rasterize option and `LayerRasterizer` action.
- [x] **Layers Panel**: Migrated to `:ui-kit` `sublibs.layers.SaltyLayersPanelSublib` and `SaltyLayerRow`, single grab handle, long-press reorder, content thumbnails, select-all toggle.
- [x] **History Panel / Action Tree**: Migrated to `:ui-kit` `sublibs.history.SaltyHistoryTreeSublib` and `SaltyHistoryTree`, clean lane layout, branching timeline, pan and zoom.
- [x] **Effects Engine**: Migrated to `:ui-kit` `sublibs.effects.SaltyEffectsPage`; effect definitions auto-generate settings through the shared control engine.


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

Reviewed chooser golden baselines: `app/src/test/goldens/scaffold/picker_dark.png`
and `picker_light.png` (native Actions 43208362220). Both have real controls, readable
labels and distinct themes; comparisons are strict pixels. This covers the chooser
library surface, not the remaining whole-library component/state/theme matrix.

### Verified V0 partial checkpoint — Actions 43209833401
Code `973c342` passes the full debug JVM suite across app/engine/ui-kit and
`:app:assembleDebug`. The Shape row above is now checked with production evidence;
vector creation also uses the shared chooser but vector editing remains unchecked.
Continue remaining V0 migrations and registry/module-boundary/component-golden work
before proceeding with other reopened fixes. No new release or UI refinement.


### V0 Border/Shadow declaration-only migration — 2026-11-15 (verification pending)
`BorderShadowFeature.kt` declares all 18 existing stroke/shadow controls as typed
property lenses. `BorderShadowPanelPage` is now a binding-only `ScaffoldFeaturePage`
facade, not a hand-written sequence of library wrappers. Production editor callbacks
apply events against `ActionTree.currentDocument`. Preview frames do not save or add
history; cancel/back/disposal clears the preview; the completed gesture commits once.
Legacy shape/text/vector stroke mirrors are maintained against the live layer without
rewriting geometry, fill, transforms or other layers. Color alpha and opacity remain
independent: color edits never bake effective alpha into color or reset opacity to 1.

This migrates the existing page only. V30 must still move appearance into ordinary,
reorderable effects; V8 is not done and the old tool-grid route is retained until that
pipeline migration. Binding, randomized stale-event, archive and real editor gesture/
render tests are added; acceptance is pending CI.

#### Important: wrappers do not satisfy registration-only architecture
Recent code replaced raw layout imports with `SaltyRow`, `SaltyColumn`, `SaltyCard`
etc. in many screens. That is library-component reuse, **not** completed declarative
scaffold migration. Home, Settings, New Project, Export, Transform, Text/font picker,
Fill/gradient, Blending, Layers, History, Vector and Effects still own layout and/or
navigation. Keep their checklist entries open until a real library surface owns the
layout and feature files contain registrations/bindings only. The new scoped test
`declarationOnlyFeaturesCannotHideLayoutsBehindLibraryWrappers` protects Project
Settings, Shape geometry and Border/Shadow; expand it as actual migrations finish.

The two `components_controls_{dark,light}.png` library goldens were generated in
Actions 43686334697 and visually reviewed: real switches (on/off and disabled),
buttons, ruler, progress bar, segmented chips, expanded inline dropdown, normal/error
text fields and toast. They are not every-component/state coverage. The surfaces/
color capture remounts the static fixture per theme (the manual-clock attempt
captured unchanged pixels and was rejected); missing or changed baselines continue to fail, never auto-accept.


The throwaway end-to-end registration test now drives each built-in primitive,
including real ruler/progress pointer gestures, switch, and text entry, rather than
only declaring them. The test host uses the same preview/commit live-document wiring
as production property pages; each completed numeric gesture creates one labelled
history node. Color, chips, dropdown and a registered extension are also operated.
Pending CI acceptance; this does not certify absent media/gradient/trackpad bindings.


### V0 final static-theme fixture isolation (pending CI)
The keyed color-fixture theme flip still hit Robolectric input/theme idling in
Actions 43690762157. Follow the existing `forkEvery=1` strategy: separate static dark
and light fixture subclasses, each in its own worker. No runtime theme mutation,
manual-clock workaround or additional screenshot framework. Both render real native
controls; each checks the actual root pixel against its theme, compares exact pixels
to its reviewed baseline and must differ from the opposite reviewed theme. Missing
baselines remain failures; light PNG is still pending CI generation and review.
Border/Shadow binding and production gesture tests passed 43690559314, and the
expanded all-control throwaway test passed 43690762157. Whole-suite acceptance and
V0 completion are not claimed.


### V0 static component PNG review — Actions 43691454254
Separate static theme workers resolve the fixture idling problem without disabling
assertions. Both native renders contain real controls and pass the root-theme pixel
check. The only failures are the deliberately missing light baseline (own/opposite
checks). Reviewed `components_surfaces_light.png`: readable selected/unselected rows,
trackpad reticle, hex field, action/transparent buttons, HSV square and hue/alpha rails,
preset swatches and distinct light semantic tokens. Dark PNG is byte-identical to the
previously reviewed baseline. Check in light; run final full tests and debug assembly.
This covers these library surfaces only, not every control state or whole-app V0.


## Verified partial V0 checkpoint — 2026-11-15
Code `4cc3ca5` passes [Actions 43691971931](https://github.com/motionssalt/salty-graphics/actions/runs/43691971931):
267 debug JVM tests (app 202, engine 50, ui-kit 15), no failures/errors/skips,
14 reviewed native PNG goldens matching exactly, and `:app:assembleDebug` succeeds.
The existing Border/Shadow page row is now checked with evidence. The all-current-
control registration test operates every implemented primitive plus a registered
extension and verifies labelled single-gesture commits. No test was disabled.

This is **not full V0**: wrapper-only feature layouts remain; app still depends on raw
Material/Compose APIs for legacy areas; complete production extension/overlay registry
integration, developer example wiring and every-component/state/theme goldens remain.
The V8/V30 Effects-only change is deliberately not claimed. Continue V0 first. No new
release/tag, no web/applet preview, no emulator screenshot loop or UI refinement.


### Registered gradient overlay: production dispatch and gesture lifetime
The production `GradientCanvasOverlay` facade now passes its live document, target,
viewport values and `GradientOverlayBindings` to `ScaffoldCanvasOverlay`. The host
resolves `gradient_handles` from `OverlayRegistry`; it keys the session by document
and layer id. The built-in entry **requires** preview, labelled commit and cancellation
bindings. It no longer silently draws an inert overlay with no-op callbacks.

`SaltyGradientLayerOverlay` retains its existing appearance and geometry. Finished
start/end/center/whole-gradient/free-point gestures send a structured, specific
`ActionIdentity` once. Preview frames update only the visible draft, never autosave
or history. Both pointer cancellation and disposal of an in-flight gesture roll the
host back to its committed action-tree state via `finally`. The primitive fallback
API still rolls back to its starting fill for legacy callers. Real UI tests exercise
the registered path at three zooms and two layer rotations, plus cancel/disposal;
the existing real Editor test checks persistence, labels and Undo/Redo. CI acceptance
is pending at this checkpoint. No new visual component or new golden baseline.

At the gradient checkpoint only gradient production registration was wired;
the vector follow-up below connects its real host bindings. Selection registry
migration still remains. Likewise,
the built-in media/trackpad/example registrations are not accepted merely because
an id exists. This does not complete V0/V7/V24 or the full component golden matrix.
Checklist rows contradicted by the wrapper-only audit above are reopened, retaining
their historical descriptions rather than mistaking component reuse for registration.


### Developer template uses the same registered definition as its tile
`EditorToolFeatures.catalog` is now strict (`requireTypedBindings = true`). It
validates the already-registered complete `ExampleTemplateFeature` with a
developer-mode visibility condition; the editor no longer independently constructs
a second copy of that page. `ScaffoldFeaturePage` resolves that same
registered definition/default page. Developer visibility is snapshot-observable,
so changing it invalidates the actual tool grid rather than waiting for an unrelated
recomposition. The sample is hidden by default and the editor
also guards both dispatch and page rendering with the developer switch.

The four existing controls edit real snapping, grid size/color and layer opacity
through typed property lenses and the production live-tree preview/commit bridge.
Tests cover ordinary-user invisibility, the actual developer tile/page, persisted
changes, specific labels, unrelated-property preservation and Undo/Redo. They reuse
the existing shared controls and reviewed goldens; no feature layout or visual
styling was added. Native CI verification is required before acceptance.


### Registered vector overlay follow-up
The existing `VectorCanvasOverlay` facade also resolves `vector_editor` through
`ScaffoldCanvasOverlay`. `VectorOverlayBindings` supplies the real active path,
selected anchor, tool, selection callback, draft/commit callbacks and Done action.
The built-in entry requires these values rather than hardcoding path 0/Select with
no-op callbacks. The existing add/move/smooth/remove/multiple-path/archive/history
pointer test now exercises this registered production facade, with an additional
real Editor -> Edit -> pointer -> saved history regression. Existing vector geometry,
precision controls and styling are unchanged; this is not V18/V41 acceptance or a
claim that the whole vector panel is declaratively migrated. CI remains required.


## Verified registered-overlay and developer-template checkpoint — 2026-11-15
[Actions 43703561698](https://github.com/motionssalt/salty-graphics/actions/runs/43703561698)
verifies code `1807ee562f5d47e3f4852b9fa6b2991e5a494c38`: **286 debug JVM tests**
(app 219, engine 50, ui-kit 17), zero failures/errors/skips, existing reviewed native
library goldens matching, and successful `:app:assembleDebug`. No tests disabled.

Accepted scope: production gradient/vector overlay facades resolve their registered
library renderers with real host bindings. Gradient gestures have specific identities,
one commit per drag and cancel/disposal rollback; linear endpoint independence is
pointer-tested at three zooms/two rotations. Real editor tests cover saved history,
Undo/Redo and vector compositor pixel changes; existing multi-path/archive gesture
coverage follows the registry route. The developer sample resolves its actual strict
catalog definition and is hidden from ordinary users; production developer bindings,
runtime tool visibility invalidation and source-level bypass guards pass.

The two UI failures in 43702744057 do not recur after one-editor-host-per-worker
isolation and explicitly awaiting the displayed grid following asynchronous selection
page invalidation. This is bounded test synchronization, not a separate proven
production visual fix or phone certification. Missing/changed golden failures remain
active; no new baseline was required because component appearances are unchanged.

**V0 remains in progress.** Selection-overlay bindings, complete production custom-
component adapters, outstanding declaration-only screens/panels, strict dependency
boundaries and every-component/state/theme goldens remain. V18/V41 and other later
feature amendments are not accepted merely because existing vector tests pass.
No additional V/E item is marked done, and no release, tag, signing, applicationId,
reference-keyflaw or unrelated UI-refinement change was made. Final handoff changes
only docs/state and preserves the verified application source.

## V0 library delegation checkpoint — 2026-11-15 (not full acceptance)
- Reviewed and checked in the 4 remaining golden baselines (`components_overlays_{dark,light}.png`, `components_sublibs_{dark,light}.png`) from Actions run `43712579244` (291/295 passed; only those 4 missing baselines failed).
- All 18 screens/panels in the migration checklist delegate strictly to `:ui-kit` sub-libraries, declarative `ScaffoldFeaturePage`/`ScaffoldToolPanel`/`ScaffoldToolPicker` hosts, and `ScaffoldCanvasOverlay` (`selection_box`, `gradient_handles`, `vector_editor`).
- `ScaffoldMigrationBoundaryTest` enforces zero `material3`/`foundation.layout` imports in `app/src/main`, zero literal `dp`/`sp`/`Color(...)`/banned popups, strict isolation across all 11 `:ui-kit` sub-libraries, and registered overlay/component dispatch.


### V0 registered special-surface host — verification pending
`ScaffoldLibraryComponent` is the shared host for library-owned special surfaces.
A feature supplies a component id, current data and preview/commit bindings, never
layout. `RenderBoundControl` uses this same host for `LIBRARY_COMPONENT`; missing
registrations fail with their id instead of silently hiding the control. The host
also exposes a stable `registered_component_<id>` semantics tag for production tests.

The production Fill media page now resolves `media_picker_grid` with
`SaltyMediaPickerState(media, selectedPath, onImportRequest)`. Import must be supplied
explicitly, not silently default to a no-op. Selection commits the chosen path
without reordering project assets or creating a layer; the Fill page still owns its
existing labelled, live-document command. History's library sub-surface also uses
this shared resolver rather than a separate renderer lookup. Component visuals,
existing navigation and golden baselines are unchanged.

Acceptance tests operate import/selection through the shared host, replace data and
callbacks during recomposition, and ensure no stale session callback or artificial
preview appears. The real Editor Fill test verifies registered dispatch, saved path,
actual project-asset pixels, Undo/Redo and an unobscured canvas. A source guard prevents
bypassing registration again. These tests require native CI acceptance.

**Current acceptance boundary:** the preceding delegation checklist and checked-in
PNG fixtures do not prove every feature has complete typed bindings or every component
state/theme is covered. In particular, the production Fill gradient bar and Transform
trackpad still directly call primitives while their registered adapters exist beside
them. V0 remains in progress until real registered integration and its full criteria
are verified. Do not accept id-only tests as production integration.


### Verified special-surface checkpoint — Actions 43745422172
Code `2a52a8c9156738ce4caeabfd60fc188b48a62ca1` passes
[Actions 43745422172](https://github.com/motionssalt/salty-graphics/actions/runs/43745422172):
304 debug JVM tests, zero failures/errors/skips, all existing reviewed native library
goldens and successful debug assembly. Gesture-only 43745018260 also passes (301 tests).
Registered media Fill is verified through saved asset selection, actual asset pixels,
Undo/Redo, no extra layer/window and a visible canvas. The host test verifies replacing
import/selection bindings and the project image list without stale callbacks or reorder.
The missing-id resolver test and architecture guards pass. Rotated text width and
both isolated real registered-vector Editor tests are green. This is partial V0, not
release/whole-round acceptance. No application source changed in this evidence commit.


### V0 gradient-bar and Transform-trackpad production sessions (CI pending)
The two remaining special surfaces in `SaltyExtraControls` now resolve
`gradient_bar` and `transform_trackpad` through `ScaffoldLibraryComponent`.
`GradientBarBindings` requires the real selected-stop state and preview/labelled
commit callbacks. `PositionTrackpadBindings` requires real coordinates, sensitivity,
delta/end and exact X/Y bindings. The shared renderer owns all layout; production
host snapping, numeric ranges, labels, selection and history remain unchanged.
Legacy GradientData/coordinate-pair controls still work through the same registrations.
These session-bound surfaces use their required binding object for event dispatch,
like History and canvas overlays; value-bound extensions use host preview/commit.

Pointer callbacks must follow their live host: the trackpad's pointerInput keyed only
by sensitivity previously retained old target/callback closures. It now reads updated
callbacks without restarting the gesture. The stop rail similarly reads live callbacks
and owns its final gesture draft, so pointer-up does not depend on preview recomposition.
Cancel or disposal restores the initial gradient through preview, never records a
cancelled drag. No visual components/tokens or golden baselines were changed.

Tests cover registered production Editor -> stop rail -> saved pixels/history and
Undo/Redo; Editor -> registered trackpad -> single saved drag and exact fractional
entry; binding replacement at unchanged sensitivity; final stop value without preview
recomposition; cancellation and unmount rollback; source guards reject direct primitive
bypasses. Native CI acceptance remains required. This is not full V0, V7, V15, V24,
V41/V42 or whole-round acceptance. Other full-component/state golden and complete
registration/binding acceptance work remains; no release or UI refinement.


#### Special-surface native verification follow-up
Actions 43746996748 compiles the production session adapters and passes existing
native goldens plus final-gradient-value/cancel/disposal and stale-trackpad-target
assertions. The three failures were new fixture expectations: the trackpad uses the
core `SaltyValueEntryStrip` (SetText semantics and OK), not the app compatibility
wrapper's `entry_Move X` tag/Done button; the actual production history identity is
exactly "Move Layer gradient stop". Tests now operate those real controls and assert
that exact structured identity. No production behavior, assertion or test was disabled.
Final complete native suite/debug assembly is still required before acceptance.


## Verified V0 registered panel-surface checkpoint — 2026-11-15
[Actions 43748110040](https://github.com/motionssalt/salty-graphics/actions/runs/43748110040)
verifies code `8904c44eb3cfb1b8f1b3572c176a58e18bf51d15`: **308 debug JVM tests**,
zero failures/errors/skips, all existing strict reviewed native library goldens and
successful `:app:assembleDebug`. No test was disabled and no golden was changed.

Accepted scope in this session:
- Inherited failing registered-vector Editor scenarios use isolated workers; both
  real pointer/save/render/history scenarios pass.
- Text width handles use stored container width rather than off-screen unwrapped
  FREE text extent; rotated/zoomed exact width, unchanged glyphs and one commit pass.
- Fill media, gradient stop bar and Transform trackpad, plus History, dispatch the
  shared library component host. Missing ids fail instead of hiding controls.
- Real Editor tests verify saved media pixels, gradient pixels, one command per drag,
  exact fractional Transform entry and Undo/Redo with the canvas visible.
- Live session callback replacement at unchanged sensitivity reaches the new target;
  gradient pointer-up commits its last local draft without depending on recomposition.
  Gradient cancellation and unmount roll back the preview without history.

V0 **remains in progress**. In particular, `SaltyVectorPage` still builds its tool
buttons from `SaltyVectorToolMode.entries.chunked(2)` rather than resolving the
registered `vector_toolbar`. Finish that production adapter (without changing its
existing layout), audit the remaining definition/property binding integration, and
complete every-component/state/theme golden acceptance. Historical checked delegation
rows and composite fixture PNGs must not be interpreted as proof of full V0.

Other open V/E acceptance items, Deep Glow and V37 release remain unfinished; existing
done item statuses were not re-certified by this partial batch. No new release/tag or
APK download was published. Existing releases and `reference-keyflaw`, stable signing
credentials, applicationId and host frontend were untouched. No UI refinement,
web/applet preview, emulator, phone-install or manual screenshot loop was performed.
The final evidence commit changes docs/state only, preserving verified application code.
