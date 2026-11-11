package com.motionssalt.saltygraphics.uikit.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.motionssalt.saltygraphics.engine.action.ActionIdentity
import com.motionssalt.saltygraphics.engine.action.ActionTree
import com.motionssalt.saltygraphics.engine.model.*
import com.motionssalt.saltygraphics.uikit.registry.*
import com.motionssalt.saltygraphics.ui.controls.*
import com.motionssalt.saltygraphics.ui.editor.PanelPage

/** Registry-driven page host: new pages and controls need registration data, never feature layout. */
@Composable
fun RegisteredFeaturePanel(feature: FeatureSpec, document: ProjectDocument, layer: Layer?, history: ActionTree,
    onDocument: (ProjectDocument)->Unit, onBack: ()->Unit) {
    val page=feature.pages.firstOrNull() ?: return
    PanelPage(page.title,onBack) {
        page.sections.forEach { section -> Section(section.title) {
            section.rows.forEach { row -> if(row.control.visible(document,layer)) {
                RegisteredControl(row.control,document,layer,history,onDocument)
            } }
        } }
    }
}

@Composable
fun RegisteredControl(spec: ControlSpec, document: ProjectDocument, layer: Layer?, history: ActionTree,
    onDocument: (ProjectDocument)->Unit) {
    val liveDocument by rememberUpdatedState(document)
    fun <T> preview(path: PropertyPath<T>, value: T) = onDocument(path.write(liveDocument,layer,value))
    fun <T> commit(path: PropertyPath<T>, value: T) {
        history.recordCommand(spec.command()) { current -> path.write(current,current.findLayerById(layer?.id?:""),value) }
        onDocument(history.currentDocument)
    }
    when(val binding=spec.binding) {
        is SwitchBinding -> ToggleControl(spec.label,binding.path.read(document,layer),{commit(binding.path,it)},
            enabled=spec.enabled(document,layer))
        is NumberBinding -> when(spec.kind) {
            ControlKind.PROGRESS -> ProgressSliderControl(spec.label,binding.path.read(document,layer),
                {preview(binding.path,it)},binding.range.start,binding.range.endInclusive,binding.defaultValue,
                binding.step,binding.unit,onValueChangeFinished={commit(binding.path,binding.path.read(liveDocument,layer))})
            else -> PanelNumberControl(spec.label,binding.path.read(document,layer),binding.range.start,
                binding.range.endInclusive,binding.step,binding.unit,{preview(binding.path,it)},{commit(binding.path,it)})
        }
        is ChoiceBinding -> if(spec.kind==ControlKind.SEGMENTED) SegmentedChipsControl(spec.label,binding.options,
            binding.path.read(document,layer),{commit(binding.path,it)})
            else DropdownControl(spec.label,binding.options,binding.path.read(document,layer),{commit(binding.path,it)})
        is ColorBinding -> PanelColorPage(spec.label,binding.path.read(document,layer),{preview(binding.path,it)},
            {commit(binding.path,it)},{})
        is TextBinding -> {
            var draft by remember(document,layer?.id) { mutableStateOf(binding.path.read(document,layer)) }
            OutlinedTextField(draft,{draft=it},label={Text(spec.label)},modifier=Modifier.fillMaxWidth().testTag("registered_${spec.id}"))
            TextButton(onClick={commit(binding.path,draft)}){Text("Apply ${spec.label}")}
        }
        is CustomBinding -> when(binding.componentId) {
            "sample.trackpad" -> TrackpadControl({},{})
            "sample.gradient" -> GradientEditorControl(GradientData(),{})
            "sample.media" -> MediaPickerGrid(emptyList(),{})
            "sample.layers" -> LayerSelector(document.activeLayers,null,{})
            else -> Text("Registered component: ${binding.componentId}")
        }
    }
}
