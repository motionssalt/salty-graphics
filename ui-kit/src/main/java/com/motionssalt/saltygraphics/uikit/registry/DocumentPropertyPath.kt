package com.motionssalt.saltygraphics.uikit.registry

import com.motionssalt.saltygraphics.engine.action.ActionIdentity
import com.motionssalt.saltygraphics.engine.model.ProjectDocument

/** A typed lens, not a captured document. A write must copy only this property of its live input. */
data class DocumentPropertyPath<T>(
    val key: String,
    val target: String,
    val property: String,
    val read: (ProjectDocument, String?) -> T,
    val write: (ProjectDocument, String?, T) -> ProjectDocument,
    val exists: (ProjectDocument, String?) -> Boolean = { _, _ -> true },
    val verb: String? = null
) {
    init {
        require(key.matches(Regex("[A-Za-z][A-Za-z0-9_.]*"))) { "Invalid property path: $key" }
        require(target.isNotBlank() && property.isNotBlank()) { "Property identity is required: $key" }
    }

    fun identity(document: ProjectDocument, layerId: String?, value: T): ActionIdentity {
        val old = read(document, layerId)
        val resolvedVerb = verb ?: when {
            value is Boolean -> if (value) "Enable" else "Disable"
            value is Float && old is Float -> if (value >= old) "Increase" else "Decrease"
            else -> "Change"
        }
        return ActionIdentity(resolvedVerb, target, property)
    }
}

/**
 * An event contains a property/value and stable target id only. The host applies it against
 * ActionTree.currentDocument at dispatch time, even when this event predates a recomposition.
 */
class FeatureChange<T> internal constructor(
    private val documentId: String,
    private val layerId: String?,
    private val binding: PropertyBinding<T>,
    val value: T
) {
    val propertyKey: String? get() = binding.path?.key

    fun appliesTo(document: ProjectDocument): Boolean = document.id == documentId &&
        (binding.path?.exists?.invoke(document, layerId) ?: true)

    fun apply(document: ProjectDocument): ProjectDocument =
        if (appliesTo(document)) binding.setter(document, layerId, value).first else document

    fun identity(document: ProjectDocument): ActionIdentity = binding.path?.identity(document, layerId, value)
        ?: ActionIdentity.inferFromDescription(binding.setter(document, layerId, value).second)
}

fun <T> FeatureControlDef<T>.change(document: ProjectDocument, layerId: String?, value: T): FeatureChange<T> {
    require(defaultValue!!::class == value!!::class) { "Value type does not match: $id" }
    if (value is Float) {
        require(value.isFinite() && (range == null || value in range)) { "Value outside range: $id" }
    }
    if (type == BoundControlType.INLINE_DROPDOWN) require(options.orEmpty().any { it.value == value })
    if (type == BoundControlType.SEGMENTED_CHIPS) require(segmentOptions.orEmpty().any { it.value == value })
    return FeatureChange(document.id, layerId, binding, value)
}
