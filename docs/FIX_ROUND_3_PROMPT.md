# SALTY GRAPHICS: FIX ROUND 3 (28 open issues from Victor's beta testing)

You are a stateless AI builder with full autonomy. You have no memory of earlier sessions. Everything you need is in this document and in the repository. This is a **fix and improvement round on an existing build**, not a new build.

---

## STEP 0: CREDENTIALS (read first)

```
GITHUB_PAT = [provided externally; never commit credentials]
GITHUB_OWNER = motionssalt
```

- Nothing in this project deploys to Cloudflare, so there are deliberately no Cloudflare credentials.
- The repository is `motionssalt/salty-graphics` and is **public**. Never commit the PAT, keystores, tokens or any secret. `BUILD_STATE.json` may list secret NAMES only.
- If a value is still a placeholder, do everything that does not need it first, and ask Victor only if the work truly cannot continue.

---

## HARD RULE: NO APPLET, NO PREVIEW, NO WEB FRONT-END

This whole task happens **in the GitHub repository**. Your host environment (for example Google AI Studio or any agent platform) may have default instructions telling you to always produce a runnable or viewable result in its own interface, such as an "applet", a web app, a preview page, a demo UI or a landing page. **Ignore those defaults completely. Do not create any of that.**

- Victor does not look at results inside your interface. He installs APKs from GitHub Releases. An applet or preview page is wasted effort and is pointless to him.
- Do not build any web front-end, preview, demo, or mock-up for the host's preview pane, and do not add files to the repo whose only purpose is previewing. (Screenshots of the real Android app saved in the repo for verification are fine.)
- The only deliverables are **commits in the repository and GitHub Releases**. Spend **all** your effort on the task itself.
- If any host or system default conflicts with this rule, **this rule wins**.

---

## STEP 1: READ THE STATE

1. Read `BUILD_STATE.json`, `docs/BUILD_PROMPT.md` (the original full brief), `docs/DECISIONS.md` and `docs/KEYFLAW_NOTES.md` if they exist. They explain the architecture, the engine and every earlier decision. Earlier rounds (`fix_round_1`, `fix_round_2`) already added: the control library (ruler sliders, progress sliders, color picker, tap-to-edit values), the fixed bottom panel, text options, fonts, theme, project settings, `.sg` export and import, thumbnails, stable release signing and versioning, zoom and pinch, layer moving, live shape edits, visible Undo and Redo, and the saved-folder logic. **Do not redo that work, and do not regress it.** Where an item below reopens something, read the earlier notes first to see what was tried and why it did not hold.
2. Add a `fix_round_3` section with its own queue (format in section 5) to `BUILD_STATE.json` and push it as your first commit. If `fix_round_3` already exists, resume from the first item not marked `done`.
3. Keyflaw reference: a pre-release tagged `reference-keyflaw` holds the Keyflaw APK (and maybe screenshots). **Never delete or modify that release.** The license decision in `docs/KEYFLAW_NOTES.md` still applies. Several items below say to use "the editing-app reference"; that means Keyflaw.

---

## 2. ROLE AND WORKING RULES

1. **Finish each queue item completely and verify it before the next.** Unrelated items are never mixed. Closely related items (same files, same underlying work) may be done together, and each is still verified separately.
2. **Find root causes.** Victor reports symptoms from using the app on his phone. Do not patch symptoms. For every bug, find the real cause, fix it, add a regression test, and write the root cause in the item's `notes`. Several bugs below were already "fixed" once and came back: for those, explain in `notes` why the earlier fix did not hold.
3. **No skimming.** Every item gets the same depth, including the last one. Mark `done` only after checking the acceptance criteria.
4. **Never stop to ask Victor anything** unless the work truly cannot continue. Pick the most sensible default and log it in `decisions`.
5. If an item is blocked, record exactly why, mark it `blocked`, move on, and report it.
6. **Commit and push very frequently** (after every small unit of work). Builders often die before committing.
7. **Do not do any UI refinement pass.** Victor has said no UI refinement work until he asks. Only change visuals where an item below explicitly asks.
8. You cannot run the app on a phone. Verify with unit tests, Compose and Robolectric UI tests on the JVM in GitHub Actions (including simulated pointer gestures), property and fuzz tests where stated, CPU reference tests for rendering math, and careful review. A failing Actions build must be fixed before moving on.
9. Order matters: the queue starts with data-loss and crash bugs, then shared foundations that later items build on. Keep this order.

---

## 3. CONTEXT: WHAT VICTOR FOUND

Victor keeps a tracker of everything he finds. The open items (status "New") are the whole job of this round. Their full details are in the queue below. For orientation, these tracker items are already **Done** and must stay working: font page stays open after choosing, font names in their own typeface, switch visibility, release versioning and signing, zoom and pinch, media import, trackpad and canvas dragging, live shape edits without Apply, visible Undo and Redo, saving to a remembered folder, project export. Note: if an item in the Done list actually still seems broken when you inspect the code or tests, record it in `known_issues` rather than silently redoing it.

---

## 4. TASK QUEUE (work in order)

### T1: Selection box: handles must never delete the layer, and must actually transform (CRITICAL)

**Symptom:** The selection box should consistently fit the selected layer and look professional, and its point and edge handles must resize, including the scale/width handle, and the top rotation control must work. **Critical failure: using the selection-box points or corners to resize or rotate currently deletes the layer instead of transforming it.**

Requirements:
- **Find the root cause of the deletion** (a handle gesture reaching the delete command, a zero or NaN size, a transform command applied to the wrong or removed layer, a history bug) and fix it. **Transform gestures must never be able to delete a layer.** Deletion happens only through the explicit Delete action.
- Guard against degenerate values: minimum size, no zero or negative scale, no NaN or infinity, rejected invalid commands. Never remove a layer as a side effect of a transform.
- Add a **property or fuzz test**: thousands of random handle drags (all corner and edge handles, rotation handle, random zoom, rotation, flips, tiny and huge deltas, multi-touch interruptions) never change the layer count and never produce an invalid transform.
- The box **fits the selected layer consistently**: an oriented box for rotated layers, the text bounds for text, the combined bounds for groups and multi-selection, updated live.
- Professional look: a thin crisp outline that stays visible on any background (light-and-dark pair), small square corner handles, smaller edge-midpoint handles, a rotation handle above the top center joined by a thin line. Handle size is constant on screen regardless of zoom, with touch targets of at least 44dp.
- Corner handles scale from the opposite corner (uniform when X and Y are linked, otherwise free). Edge handles scale one axis from the opposite edge. The rotation handle rotates around the layer's center with light snapping near 0, 45 and 90 degrees and a live angle readout. Locked layers show only the outline.
- Each gesture is exactly **one** action in the history.
- *Done when:* the fuzz test passes, handle gestures change size and rotation as described (including on rotated layers), and no transform gesture can reduce the layer count.

### T2: Projects that load forever

**Symptom:** Opening a project sometimes leaves it loading forever and the project never opens. The failure is intermittent.

Requirements:
- Find the root causes (for example deadlocks, never-completing awaits, exceptions swallowed in a coroutine, heavy decoding on the main thread, contention between loading and auto-save or thumbnail generation, half-written project files).
- Make project **writes atomic** (write to a temporary file, then rename) so a half-written file can never be loaded.
- Loading is an explicit state machine: **Loading, then Loaded or Error**. Loading has a timeout and per-step progress. It must **never spin indefinitely**: on failure or timeout, show a clear recoverable error with **Retry**, **Open in safe mode** (skips layers or media that fail to load and keeps the rest, using the quarantine behaviour from round 1), and **Back**.
- Log each load phase with timings to the local diagnostic log (the crash log from round 1) and include it in "Report a problem".
- Loading must not block on thumbnail generation or auto-save, and they must not run against a project while it is loading.
- *Done when:* tests simulating a slow asset, a missing asset, a corrupt layer, a half-written file, and concurrent auto-save and thumbnail work each end in either an opened project or a recoverable error within the timeout, never a stuck spinner.

### T3: Bottom control panel must stay open during transform editing

**Symptom:** When Transform editing is selected, tapping outside closes the bottom control panel and the controls disappear. The panel holds the main controls and must stay open.

Requirements:
- Tapping the canvas or anywhere outside the panel must **never dismiss the panel** or make the controls disappear. The panel only changes page through its own back arrow or tool buttons.
- If a tap on the canvas changes the selection while a layer-specific page is open (for example Transform), the panel stays visible and returns to the page that fits the new selection (the no-selection tool grid if nothing is selected). It never disappears.
- *Done when:* UI tests tap the canvas, the empty area, and another layer while the Transform page is open, and the panel remains visible each time.

### T4: Clearing the layer selection

- Tapping an **empty area of the canvas deselects all layers**. Also provide a dedicated **Select None** action in the layers panel as an explicit alternative.
- The app must work correctly with **no layer selected** (the bottom panel shows the no-selection tools, nothing crashes, no stale bounding box).
- Selection changes are never recorded as history actions (see T5).
- *Done when:* tests cover tap-empty-canvas deselect, Select None, and using the no-selection tools afterwards.

### T5: Layer taps must not jump to another action-tree branch (bug)

**Symptom:** Tapping a layer sometimes switches the Action tree to another branch. Selecting a layer should not change the active history branch; branch navigation should happen only through an intentional Action-tree interaction.

- Find the root cause (for example selection stored in or restored from history nodes, taps being handled as history jumps, or branch pointers updated on selection) and fix it. Selection is **UI state outside the history**. Undo and redo may restore a sensible selection, but a tap on a layer never moves the history pointer or changes the branch.
- *Done when:* tests build a history with several branches, tap layers repeatedly, and verify the current node and branch never change.

### T6: Branched action-history tree (zoomable and pannable)

Replace the current stacked or list display with a real tree.

- Show action order and branches. The **active path from the start through the current state is clearly highlighted**; undone actions and alternate branches are **faded** but readable.
- Behaviour: **Undo moves the current point back while retaining the abandoned actions.** Continuing after an undo **creates a new branch**. **Redo returns along the previous branch** (when a node has several children, redo follows the most recently active one). The user can **open the tree fully**, inspect all actions, and **select an action or branch to continue from that state**.
- The tree is **zoomable and pannable**, auto-scrolls to the current node when opened, and stays smooth with hundreds of nodes (draw only what is visible, optionally collapse long straight runs with an expand control).
- Nodes show short plain labels and icons. Mark branch points clearly. Mark the current node clearly.
- The clipboard still survives branch jumps.
- Required test sequences: **create, move, delete, create, undo, redo**, and **several undos followed by new actions** (check the resulting branches, the active path, and where Redo goes).
- Keep saved projects compatible; migrate if the data model must change.
- *Done when:* tests for both sequences check the tree structure, the active path flags, the faded flags, the redo target, and jumping to a node restores the right state.

### T7: Shared scroll-wheel (ruler) control: smooth, precise, and used everywhere

**Symptom:** The existing scroll-wheel control used by text effects is laggy.

Requirements:
- Fix the **shared engine control** itself so it is smooth, precise and responsive: 1:1 finger tracking with no lag, immediate response, natural fling, slow precise drags, correct behaviour inside scrolling panels, light throttled haptics, and no heavy per-frame work (the preview updates live, the history commits once on release).
- **Replace bounded sliders** for transform **scale, position and rotation** with this control, and use it instead of sliders for **shape settings and most other numeric settings and effects**. Keep the progress slider only where a bounded 0 to 100 style bar really fits (for example opacity) or where an effect definition chooses it.
- **Continuous values must not be trapped inside arbitrary slider limits.** Give each control a hard validity range only where it is mathematically or physically meaningful (for example scale above zero, opacity 0 to 100%). Position, size, spacing and similar values may scroll far beyond the visible canvas. The wheel never stops at an artificial end; typed values are accepted anywhere inside the hard validity range, and anything outside it is refused with the red-field behaviour from round 1.
- Test across **text effects, other effects, shapes and transform settings** that the same control behaves identically.
- *Done when:* simulated gesture tests (slow drag, fast fling, tiny nudge, drag inside a scrolling panel, long drags beyond the old limits) pass for transform, shape, text and effect controls, and the measured per-frame cost is noted in `notes`.

### T8: Clickable transform values everywhere

- **Every numerical transform value is clickable** and opens the precise-entry popup (the value editor from round 1, above the keyboard, with red-field refusal for invalid input). Victor reports it only appears in some controls. Audit **all** transform controls (position X and Y, scale X and Y, uniform scale, rotation, and any others) and make it consistent and reliable.
- *Done when:* a UI test taps every transform value and enters a valid value (applied) and an invalid one (refused).

### T9: Blending modes must really apply

**Symptom:** Blend modes currently fail to apply to the target layer.

- Find why and fix the rendering and compositing pipeline so that selecting a mode **visibly and reliably** changes how the selected layer blends with the layers beneath it. This item was attempted in an earlier round and still fails on the device: read the earlier notes, find out why the CPU reference tests passed while the real pipeline did not (for example shader not receiving the backdrop, blend state never reaching the renderer, mode stored but not rendered, caching serving a stale texture), and add a test that exercises the **real render path**, not only the formula table.
- Verify across **all supported modes and all layer types** (media, text, shape, vector, group), with opacity, and inside groups. Normal mode must stay unchanged.
- *Done when:* tests that run the render path (a GL test harness or an equivalent CPU-side compositor sharing the exact same blend definitions and wired through the same layer-to-renderer path) show every mode changing the output for every layer type, and a regression test fails if the blend mode stops reaching the renderer.

### T10: Correct non-destructive layer masks

**Symptom:** The current mask implementation is wrong. Replace it with correct masks.

- **Non-destructive compositing masks** that control the target layer's visibility **while its blend mode is applied**.
- Mask types: **Alpha** (source opacity), **Luminance** (source brightness), and **Vector/path** masks. Include **inverted** Alpha and Luminance options, a **selectable mask source layer**, and a **mask preview** that shows the mask being used. **Preserve partial values** so soft and feathered edges work.
- Masks keep working on groups, and on layers inside groups. Old projects that used the earlier in-mask and out-mask must still open and render the same (migrate the stored data: in-mask and out-mask become the matching alpha or vector mask with the invert flag).
- Reference behaviour: Figma's Alpha, Vector and Luminance masks, and After Effects' Alpha, Luma and inverted mattes.
- *Done when:* CPU reference tests and render-path tests cover each mask type, inversion, soft edges, blend mode with mask, groups, and the migration of old projects.

### T11: Blending UI: categories, previews, and masking inside it

- Organise blend modes into **categories** (Normal, Darken, Lighten, Contrast, Inversion, Component), inside the fixed bottom panel as category tabs and a grid of tiles, each tile with a **thumbnail** rendered with the real pipeline from a **representative development image** bundled with the app (generate one in code or use a CC0 image, with its source and license recorded in `docs/licenses/`). Thumbnails are cached after the first render. The selected mode is clearly highlighted and tapping applies it live.
- **Masking lives inside the blending experience, not behind a separate button.** The Blending page has a Mask section: choose the **mask type and the mask source layer**, with the **inversion option**, and a clear **preview of the mask being used**. Remove the separate Mask button from the layer-selected tool grid.
- *Done when:* every mode is reachable through its category, thumbnails match real results, the mask section works end to end with T10, the separate Mask button is gone, and old projects open unchanged.

### T12: Re-implement the adjustment layer (reopened)

**Expected behaviour (from Victor):** An adjustment layer should be an **invisible, full-canvas layer with no visible rectangle or ordinary layer boundary box**. Adjustments and effects applied to it affect **all layers beneath it in the stack**, without creating any visible shape content.

- It has **no fill, no visible bounds, and no ordinary selection box or handles** (selecting it in the layers panel shows it as selected, with a clear indicator of its row, but nothing is drawn on the canvas for it). It cannot be moved, scaled or rotated like a normal layer.
- Its effect stack is applied to the **composite of everything beneath it** (render the backdrop to a texture, run the effects, composite back using the adjustment layer's own opacity, blend mode and mask). Layers above it are unaffected. Inside a group it affects only that group's layers beneath it.
- It respects visibility, lock, reordering, grouping, history, copy and paste of effects, and the new mask controls.
- Use the hidden **developer option** (tap the version in About seven times) that enables the debug test effects to verify it on the phone; keep that switch off by default, and make sure it still works.
- *Done when:* render-path tests show an adjustment layer changes only the layers beneath it, draws nothing by itself with zero effects, has no canvas selection box, and works inside groups.

### T13: Preview quality that actually works, with fractional levels

**Symptom:** Preview quality stays at Full even after changing the setting, so the selected quality does not appear to affect the preview.

- Fix the control and the rendering so the selected level really changes the preview resolution (use the editing-app reference, Keyflaw, for how its proxy rendering behaves). Find the root cause (setting stored but never read by the renderer, caching, wrong scale applied) and add a test on the real render path that the proxy size changes.
- Show the choices as **1/4, 1/2, 3/4 and Full** instead of percentages, and make the control **clearly distinct from the zoom or magnification percentage** (different position, different icon, different label such as "Preview" with the fraction).
- Preview quality never affects export or the project's own resolution.
- *Done when:* tests prove each level changes the rendered preview size by the right factor, export is unaffected, and the labels read 1/4, 1/2, 3/4, Full.

### T14: Shared gradient selector (engine component)

- Build a **reusable gradient selector in the app engine**, like the color selector, to be used wherever a gradient is chosen. Support **linear, radial and freeform** gradients, with **color and opacity stops** (add, remove, move, edit), an adjustable **direction or angle**, and **live updates** on the canvas as it is edited.
- Freeform means colors placed at free points that blend smoothly (a mesh-like or point-based gradient); choose a sensible implementation and log it.
- Controls come from the control library, values accept tap-to-edit input, and everything is undoable with one action per edit.
- Reference: Illustrator's linear, radial and freeform gradients and its gradient editing controls.
- *Done when:* tests cover stop editing, angle, each gradient type's rendering against a CPU reference, and serialization in `.sg` files.

### T15: Universal Fill control across all layers

- Add a **Fill** button or control available across layer types, with **solid color, gradient (T14) and media (T16) fill** options.
- Keep existing fill settings (for example text fill in the text settings) **synchronized with this shared fill model**: they are the same value, not two copies.
- **Solid fill must support transparency independently of stroke.** A transparent fill leaves the stroke visible, unless the stroke's own color or opacity is changed.
- Fill is part of the document, history, copy and paste, and `.sg` files. Migrate old projects (old fill values map to a solid fill).
- *Done when:* tests cover fill on every layer type, text fill sync, transparent fill with a visible stroke, and migration.

### T16: Media fill modes: Fill, Fit, Stretch

- Imported **images** (design it so video can be supported later, but do not implement video now) can **fill** a layer, **fit** inside its bounds, or **stretch** to its bounds:
  - **Fill** covers the area and crops overflow.
  - **Fit** shows the whole media inside the layer (empty space stays transparent).
  - **Stretch** resizes independently in width and height to the exact layer bounds.
- The media is always **clipped to the layer's shape** (for example a rounded rectangle or a star). Include media offset or alignment adjustment inside the fill area if simple.
- This is one of the options in the universal Fill control (T15).
- *Done when:* tests check the computed placement for each mode on wide and tall images in wide and tall layers, and clipping to non-rectangular shapes.

### T17: Unified Border & Shadow panel

- One **Border & Shadow** button and panel, usable across layer types, with separate **Stroke** and **Shadow** sections.
- **Stroke:** enable or disable; solid or dashed styles with dash and gap spacing; width; color and opacity; **inside, center or outside** alignment; line **caps and joins**. Stroke color and opacity are independent of the fill (see T15).
- **Shadow:** a **non-destructive, live-preview drop shadow** with color and blend mode, opacity, **light angle**, distance or offset, blur size or softness, and optional spread or contour. A shared **light angle** option keeps shadows consistent across layers.
- Both use the control library and the gradient or color selectors where relevant, and are part of the document, history, copy and paste, and `.sg` files.
- References: Illustrator stroke controls, Photoshop layer-style shadow controls, and Alight Motion's Border & Shadow panel layout.
- *Done when:* tests cover stroke alignment, dashes, caps and joins geometry, and shadow rendering against a CPU reference, and the panel works for text, shapes, vectors and media.

### T18: Shape picker with editable shape-specific settings

**Symptom:** The Shape button currently inserts only a rounded-corner square.

- Replace it with a **shape picker** for core shapes: **rectangle (and rounded rectangle), ellipse (and circle), line, arrow, polygon, and star**.
- Shared controls: width, height, rotation, fill and stroke (using T15 and T17). Shapes **stay editable after insertion**.
- Type-specific settings: **rectangle**: independent corner radii and corner styles; **ellipse**: width and height with a circle constraint, and optional pie or arc controls; **line and arrow**: length, angle, and endpoint arrowheads; **polygon**: side count, angle, and corner type and radius; **star**: point count, inner and outer radius, and corner rounding.
- Shapes expose their path data so text can follow them (as in the original brief), and they keep working with masks.
- Reference: Figma's shape tools and Illustrator's live-shape controls.
- *Done when:* tests build each shape from its settings and check the geometry, and shapes survive save and reload.

### T19: Vector curve editing (reopened)

**Symptom (still present):** After creating a path, dragging its curve handle or point creates a new path instead of adjusting the current one.

- Provide a way to **control a path's curvature while drawing** (for example click-and-drag on a new point to pull out its curve handles, as in a pen tool) and **keep those controls editable after the path is created**.
- Make the distinction between **drawing a new path** and **editing the selected path** reliable and visible: an explicit mode (Draw or Edit) shown on the canvas and in the panel. In Edit mode, the touch priority is: **bezier handle, then anchor point, then path segment (dragging a segment bends it), then empty space (does nothing)**. A new path or point is only created by the Draw tool, the **Add point** tool, or **+ Add**, never by an accidental drag.
- This was attempted in round 2 and failed on the device: read the earlier notes, find why the tests passed but the real behaviour did not (for example the gesture detector restarting, the mode resetting after recomposition, hit-testing in the wrong coordinate space under zoom), and add tests with simulated gestures at several zoom levels and on a rotated or scaled vector layer.
- *Done when:* tests simulate dragging a handle, an anchor and a segment on an existing path (the path count stays at one and the geometry changes), drawing a new curved point by click-and-drag, and dragging in empty space (nothing changes), all at several zoom levels.

### T20: Vector-aware selection and editing for shape and vector layers

This is a design improvement, separate from the T1 bug.

- Replace the generic layer selection box, for **shape and vector layers**, with controls suited to their geometry: a clear **Edit shape** mode with path points and handles for vectors, and the live-shape handles from T18 for shapes (for example a corner-radius handle, a star inner-radius handle, a polygon side control on canvas). The generic transform box stays available as the **Transform** mode for moving, scaling and rotating the whole layer, and switching between them is quick and obvious (for example tapping the selected layer again, or a mode toggle in the panel).
- Log the exact interaction in `decisions`. Whatever the design, the T1 rules still hold: no gesture can delete the layer.
- *Done when:* tests show both modes on shape and vector layers, and that switching modes never changes the geometry by itself.

### T21: Reliable snapping with visible Smart Guides

- Fix and expand the snapping system. Show **visible red guide lines whenever a snap engages**, with clear feedback that the layer is aligned and held in place.
- Snap to the **canvas center and edges**, and to **other layers' centers, edges and geometry**. Include useful **spacing guides** (equal gaps between layers).
- Use a **predictable snap tolerance**, in screen pixels so it is stable across zoom levels, **configurable** in settings, with hysteresis so snapping does not flicker.
- Apply the **same snapping behaviour when moving, resizing or scaling, and rotating**, through both the transform controls and the direct canvas and selection-box gestures. Rotation snaps to meaningful angles (0, 45, 90 and the angles of other layers).
- A toggle turns snapping on or off. Light haptic tick when a snap engages.
- Research references: Figma's red guides and snapping to objects and geometry, and Adobe Smart Guides with center, edge, spacing and measurement guides and a configurable tolerance.
- *Done when:* tests cover snap decisions (to canvas, to layers, spacing), tolerance stability across zoom levels, hysteresis (no flicker around the threshold), and move, scale and rotate all using the same engine.

### T22: Layer panel: long-press and drag to reorder, and grouping

- Replace or supplement the one-step up and down arrows with a **clear grab area** on each layer row. **Long-press to select** a layer, then **drag up or down** to reorder, with the selected and lifted state shown while dragging and a drop indicator.
- **Grouping** from the layer-selection workflow: multi-select layers, then a Group action; also Ungroup. Group membership and order stay correct after reordering.
- Reordering and grouping are single history actions.
- *Done when:* gesture tests cover long-press-then-drag reorder, dragging across many rows with auto-scroll, grouping and ungrouping, and history.

### T23: Duplicate layer button

- Duplication already exists. **Assess and decide** whether a dedicated Duplicate button or icon improves discoverability without cluttering the layer panel, then implement the better option (for example a Duplicate button in the layer-selected tool grid, or an icon in the layer row's actions). **Keep the existing duplication action available either way.** Log the decision.
- The duplicate lands directly above the original with a clear name, copies every property including fill, stroke, shadow, blend, mask and effects, and is one history action.
- *Done when:* a test duplicates every layer type and compares properties.

### T24: Selective copy and paste of layer properties

- Extend single-layer property copy beyond effects and transforms to include **blending mode, fill, stroke, shadow, mask, opacity and other applicable appearance properties**.
- **One layer copied and a target layer selected:** show a **chooser** listing the source layer's available properties, **all selected by default**, so the user can deselect any they do not want to paste. Pasting **replaces** the target's values for the chosen properties (the earlier replace rule stays).
- **No target layer selected:** paste the copied layer as a **new layer**.
- **Multiple layers copied and a target selected:** do **not** apply the single-layer property-paste action and do **not** show its chooser.
- The clipboard still survives history jumps.
- *Done when:* tests cover each case (chooser contents and defaults, partial paste, paste as new layer, multi-copy behaviour) and replace semantics.

### T25: Font search must stay visible with the keyboard open

**Symptom:** In text editing, tapping the font search opens the keyboard over the search UI, hiding what the user types and the results.

- Make font search a **button that opens a popup or font picker** with a visible search field and font list **while the keyboard is open** (handle IME insets so the field and results sit above the keyboard).
- Keep the earlier font behaviour: names in their own typeface, built-in and imported tags, the page stays open after choosing.
- *Done when:* a UI test with a simulated keyboard shows the search field and the first results visible and above the keyboard.

### T26: Grid lines: square-grid toggle and canvas-matched sizing

- Add a **toggle for square grid cells**.
- **By default, match the grid's aspect ratio to the canvas**, so grid lines wrap corner to corner when the grid is resized and the **final row or column is not cropped**. With square cells on, cells are squares and the grid is centered or anchored sensibly, and a partial last row or column is acceptable only in that mode.
- Grid settings (show, size, color, square toggle) live in the project settings and the View mode popover as already defined.
- *Done when:* tests check that, for several canvas ratios and grid sizes, default grids tile the canvas exactly with no cropped last row or column, and that square mode produces equal-sized cells.

### T27: Remove the redundant project-export menu

- Remove the **three-dot menu in the top status panel** that contains only one option, exporting the project file. Project-file saving is already available on the Export page, so the duplicate action is unnecessary.
- Make sure the top panel layout still looks balanced and nothing else breaks.
- *Done when:* the menu is gone, project saving still works from the Export page, and layout tests of the top panel pass.

### T28: Oversized scrollbar in the Animation tab

**Symptom:** The Animation tab's scrollbar is too large and difficult to operate, making the panel hard to scroll.

- Find the Animation tab in the app. Make its scrollbar a normal, slim size with a comfortable touch target, and make scrolling the panel easy and predictable (smooth touch scrolling with fling, the bar optional and unobtrusive).
- If you cannot find an "Animation tab" in the code, record exactly what you searched and found in `notes`, mark the item `blocked`, and report it in the final report. Do not invent an animation feature.
- *Done when:* a UI test shows the panel scrolls with a touch drag and fling, and the scrollbar is at the new size.

### T29: Verify, release, and hand off

- Run all tests and fix every failure. Check that projects from earlier builds still open (migrate stored data where needed: masks, fills, strokes, history).
- Update `docs/DECISIONS.md` and `BUILD_STATE.json` (`fix_round_3`).
- Publish a **new release** under the round 2 release rules (new unique version and tag, the same stable signing key, earlier releases untouched), with the universal and per-ABI APKs.
- **Then STOP and report back.** Do not start any UI refinement.

---

## 5. STATE FORMAT

Add to `BUILD_STATE.json` and keep it updated after every meaningful step, committed together with the code:

```json
{
  "fix_round_3": {
    "status": "not_started | in_progress | blocked | complete",
    "queue": [
      { "id": "T1", "title": "...", "status": "pending | in_progress | done | blocked", "notes": "root cause, what changed, how it was verified" }
    ],
    "decisions": [],
    "known_issues": [],
    "last_updated": "ISO timestamp",
    "last_session_note": "what the last AI was doing when it stopped"
  }
}
```

On start: read it, continue from the first item not marked `done`, never redo finished work.

---

## 6. COMMIT AND PUSH DISCIPLINE

- First commit: the `fix_round_3` state, pushed immediately.
- Commit and push after every small unit of work. Meaningful messages.
- Never start something new with uncommitted work.
- Every push leaves the repo in a state the next AI can resume from `BUILD_STATE.json` alone.

---

## 7. OUT OF SCOPE

The UI refinement pass, real effects (only the engine and the hidden debug effects), brush, eraser, crop, 3D, video support and animation features, new backend, Rust or native code, anything not listed above.

---

## 8. FINAL REPORT (short)

- New release tag and APK download
- Which T-items are done and which are blocked
- The root cause found for each bug (one line each), and for the reopened bugs why the earlier fix did not hold
- Key decisions made (mask migration, fill model, vector interaction modes, snapping tolerance, duplicate button)
- Known issues
- Confirmation that no UI refinement was done
