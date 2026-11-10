package com.motionssalt.saltygraphics.engine.action

import kotlinx.serialization.Serializable

@Serializable
enum class ActionVerb(val displayName: String) {
    CREATE("Create"),
    ADD("Add"),
    DELETE("Delete"),
    MOVE("Move"),
    RESIZE("Resize"),
    SCALE("Scale"),
    ROTATE("Rotate"),
    TRANSFORM("Transform"),
    INCREASE("Increase"),
    DECREASE("Decrease"),
    CHANGE("Change"),
    SET("Set"),
    ENABLE("Enable"),
    DISABLE("Disable"),
    TOGGLE("Toggle"),
    RENAME("Rename"),
    DUPLICATE("Duplicate"),
    REORDER("Reorder"),
    GROUP("Group"),
    UNGROUP("Ungroup"),
    PASTE("Paste"),
    IMPORT("Import"),
    EDIT("Edit")
}

@Serializable
data class ActionIdentity(
    val verb: String,
    val target: String,
    val property: String? = null
) {
    fun toLabel(): String {
        val p = property?.trim()
        val t = target.trim()
        val v = verb.trim()
        return when {
            !p.isNullOrBlank() -> {
                val pLower = p.lowercase()
                val tLower = t.lowercase()
                if (tLower.endsWith(pLower) || tLower.contains(pLower)) {
                    "$v $t"
                } else {
                    "$v $t $p"
                }
            }
            t.isNotBlank() -> "$v $t"
            else -> v
        }
    }

    companion object {
        fun of(verb: ActionVerb, target: String, property: String? = null): ActionIdentity =
            ActionIdentity(verb.displayName, target, property)

        fun of(verb: String, target: String, property: String? = null): ActionIdentity =
            ActionIdentity(verb, target, property)

        fun forNumericChange(target: String, property: String, oldValue: Float, newValue: Float): ActionIdentity {
            val verb = if (newValue >= oldValue) ActionVerb.INCREASE else ActionVerb.DECREASE
            return ActionIdentity(verb.displayName, target, property)
        }

        fun forBooleanChange(target: String, property: String, enabled: Boolean): ActionIdentity {
            val verb = if (enabled) ActionVerb.ENABLE else ActionVerb.DISABLE
            return ActionIdentity(verb.displayName, target, property)
        }

        fun inferFromDescription(description: String): ActionIdentity {
            val trimmed = description.trim()
            val parts = trimmed.split(Regex("\\s+"))
            if (parts.isEmpty()) return ActionIdentity("Edit", "Action")
            val first = parts.first()
            val recognizedVerb = ActionVerb.values().find { it.displayName.equals(first, ignoreCase = true) }
            return if (recognizedVerb != null) {
                if (parts.size >= 3) {
                    val target = parts.subList(1, parts.size - 1).joinToString(" ")
                    val property = parts.last()
                    ActionIdentity(recognizedVerb.displayName, target, property)
                } else if (parts.size == 2) {
                    ActionIdentity(recognizedVerb.displayName, parts[1])
                } else {
                    ActionIdentity(recognizedVerb.displayName, "")
                }
            } else {
                ActionIdentity("Change", trimmed)
            }
        }
    }
}
