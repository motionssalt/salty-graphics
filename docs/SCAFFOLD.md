# Salty Graphics internal UI scaffold

**Read this document before changing any UI.** The integration module declares screens, features, bindings and commands. The `ui-kit` Android library owns widgets, domain surfaces, navigation, themes, overlays and design tokens. The `engine` module never imports Compose or UI.

## Module boundary

- `:app` contains Android lifecycle setup and `SaltyFeatureRegistrations.kt`. It has no direct Compose Material, layout, navigation or graphics dependency. It may not contain widgets, dimensions, colors, dialogs or feature layout.
- `:ui-kit` contains the registry, control renderers, panel stack, screen hosts, reusable domain surfaces, canvas overlays and all tokens. The legacy complex screens were moved here unchanged as registered custom components; this was an architectural migration, not a visual redesign.
- `:engine` owns model, persistence, media, history and rendering. It has no dependency on `:ui-kit` or `:app`.
- Build enforcement scans the integration/feature registration code and rejects Compose UI imports, literal `dp`/`sp`/colors and overlay constructors. Popup checks also scan editing flows. Only rasterize/save confirmations, system pickers, toasts and non-canvas screens are exceptions.

## Add a feature: one registration

Add a `FeatureSpec` in `SaltyFeatureRegistrations.registry(...)`. Declare its stable id, title, `IconToken`, placement, eligible layer types and condition. Its `PanelPageSpec` contains sections, rows and `ControlSpec` objects. Use a typed `PropertyPath<T>` to read and write exactly one document or layer property. The path must be mutation-safe: write only its property on the current document.

```kotlin
private val gridSize = PropertyPath(
    "editor.grid_size",
    read = { document, _ -> document.editorSettings.gridSize.toFloat() },
    write = { document, _, value -> document.updateEditorSettings {
        it.copy(gridSize = value.toInt().coerceIn(4, 200))
    } }
)

val feature = FeatureSpec(
    id = "grid", title = "Grid", icon = IconToken.GRID,
    placement = FeaturePlacement.OPERATION,
    pages = listOf(PanelPageSpec("grid", "Grid", listOf(
        SectionSpec("layout", "Layout", listOf(RowSpec("size",
            ControlSpec("size", "Grid size", ControlKind.RULER,
                NumberBinding(gridSize, 4f..200f, step = 1f, unit = "px"),
                verb = "Change", target = "Project")))))
    ))
)
```

`RegisteredFeaturePanel` renders this data using shared controls. Each commit goes through `ActionTree.recordCommand` with `ActionIdentity(verb, target, property)`, so the resulting history label is specific and unrelated settings cannot be reset by a stale whole-object write. Visibility and enablement are evaluated from current document and selection.

## Add a page

Add a `PanelPageSpec` to the feature and use a stable page id and human-readable title. Use `parentId` for a nested page. The fixed `EditorPanelState` owns one page stack. Back pops that stack; dismissing an editing flow clears it so a stale layer-specific page cannot reappear after selection changes. Long content scrolls inside the fixed pane. Do not add a `Dialog`, `ModalBottomSheet`, `Popup` or `DropdownMenu`; use a page or inline expansion.

## Add or reuse a control

Prefer `SwitchBinding`, `NumberBinding`, `ChoiceBinding`, `ColorBinding` and `TextBinding`. `ControlKind` selects switch, ruler, progress, inline dropdown, segmented chips, panel color, text, trackpad, gradient editor, media grid, layer selector or a registered custom surface. Ranges, steps, units, defaults, visibility and enablement belong in the binding/control declaration.

A genuinely new control requires all of the following in the same change:

1. Its reusable implementation under `ui-kit/.../uikit/components` or the existing shared `ui/controls` compatibility package.
2. A `ControlKind` and binding type if it is data-driven.
3. A branch in `RegisteredControl`; no call-site layout in the feature registration.
4. JVM interaction tests, registry round-trip tests and golden coverage in light and dark themes for every supported state.
5. A short entry here describing the control and binding contract.

Effect definitions already use the same shared controls through `EffectSettingsSheet` and `ControlRegistry`. New effect controls must extend that shared mapping, not create another settings UI.

## Custom components and overlays

Register a stable id in `UiComponentRegistry` and use `CustomBinding`. Custom components are for special surfaces only: gradient bar/editor, media grid, trackpad, history tree, vector toolbar and similarly complex interaction surfaces. Selection, gradient, vector, snapping and grid overlays have `CanvasOverlayToken` declarations. They may draw geometry above the canvas, but may not place editing panels or floating controls over it. Vector precision controls live in `VectorPrecisionControls` in the fixed pane and share `VectorPrecisionState` with the geometry overlay.

## Tokens and themes

Use `SaltySpacing`, `SaltyShapes`, `SaltyElevation`, `SaltyMotion`, the shared typography and Material color roles. Dark, light and system/dynamic themes are selected once by `SaltyGraphicsTheme`; UI scale is applied once at that host. Do not put literal dimensions or colors in registrations. This round does not polish visual design; later visual changes are made centrally in the library.

## Numeric entry, color and confirmations

`ValueEditorDialog` is the retained source-compatible API name, but its implementation is a compact inline `ValueEntryStrip`; it is not an Android dialog. In editing it occupies the fixed pane and IME-safe content shrinks/re-fits rather than covering the layer. `ColorPickerRow` expands inline and `PanelColorPage` provides alpha and transparent. `ConfirmationDialog` is the only library component wrapping an alert dialog and is restricted to destructive/irreversible actions. System file/media pickers and short toasts remain Android-owned exceptions.

## Worked hidden example

`ScaffoldSample.page` is registered as `developer_scaffold_sample` and appears only when the existing developer/debug-effects switch is enabled. It includes every generic control kind, typed property paths, generated history identities and custom-surface registrations. `ApplicationRegistryTest` and `ScaffoldRegistrationTest` exercise placement, validation, document round trips, control rendering and labelled history commands without feature layout code.

## Migration checklist

- [x] Home, project cards, multi-select and delete confirmation moved into `ui-kit`; Home destination registered.
- [x] Settings, shared switches, theme/UI-scale and device diagnostics moved into `ui-kit`; Settings destination registered.
- [x] New Project, ratio/quality controls and safe-dimension entry moved into `ui-kit`; destination registered.
- [x] Export, full-quality render actions and share/save pickers moved into `ui-kit`; destination registered.
- [x] Project settings and per-property history bindings retained in the registered project-settings surface.
- [x] Editor top bar, view quality, zoom/grid and navigation moved into `ui-kit`.
- [x] Bottom tool grid consumes `FeatureRegistry`: Media, Text, Adjustment, Shape, Vector, Edit, Transform, Effects, Blending/Mask, Fill, Border & Shadow, Duplicate, Rasterize and Delete.
- [x] Text options and font picker are registered custom surfaces in the fixed pane.
- [x] Shape creation/settings and vector creation/settings are fixed-pane surfaces.
- [x] Fill, gradient editor, media grid and panel-native color/alpha/transparent controls are shared library surfaces.
- [x] Border & Shadow and Blending/Mask use shared controls and fixed-pane pages.
- [x] Effects tiles, applied-effect list and definition-generated controls use the shared effect/control registry.
- [x] Rasterize retains its allowed destructive confirmation; group entry/breadcrumbs remain fixed-pane controls.
- [x] Layers list and rename/paste-property editing use the fixed pane, not a canvas drawer or editing dialogs.
- [x] Operation/history tree is a panel page; it no longer opens a full-screen window over the canvas.
- [x] View mode, snapping and grid settings retain shared engine state; canvas geometry is a registered overlay concern.
- [x] Gradient handles are a library overlay with independent endpoint math and line translation.
- [x] Vector point/handles are a library overlay; precision controls are panel-native.
- [x] Numeric value entry is a compact inline/docked strip, not a window.
- [x] App navigation consumes `ScreenRegistry`; app code contains registrations and Android integration only.

## Verification

Run `./gradlew test` for engine, UI and application registration suites. `BottomPanelArchitectureEnforcementTest`, `ScaffoldArchitectureEnforcementTest`, `ApplicationRegistryTest` and `ScaffoldRegistrationTest` protect the boundary, popup rule, bindings, history labels and sample. `LibraryGoldenTest` renders the component/state manifest in dark and light themes; update goldens only after intentional library changes with `./gradlew :ui-kit:testDebugUnitTest -PupdateGoldens`, inspect the images, and commit them.
