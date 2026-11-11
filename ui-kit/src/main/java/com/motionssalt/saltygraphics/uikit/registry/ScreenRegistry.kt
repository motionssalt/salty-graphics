package com.motionssalt.saltygraphics.uikit.registry
/** Non-canvas screens are registered destinations, not app-module layouts. */
enum class AppScreenId { HOME, SETTINGS, NEW_PROJECT, EDITOR, EXPORT }
data class ScreenSpec(val id: AppScreenId, val title: String, val componentId: String, val icon: IconToken)
object ScreenRegistry {
    val screens = listOf(
        ScreenSpec(AppScreenId.HOME,"Projects","screen.home",IconToken.MEDIA),
        ScreenSpec(AppScreenId.SETTINGS,"Settings","screen.settings",IconToken.SETTINGS),
        ScreenSpec(AppScreenId.NEW_PROJECT,"New Project","screen.new_project",IconToken.SHAPE),
        ScreenSpec(AppScreenId.EDITOR,"Editor","screen.editor",IconToken.EDIT),
        ScreenSpec(AppScreenId.EXPORT,"Export","screen.export",IconToken.MEDIA))
    init { require(screens.map { it.id }.distinct().size == screens.size) }
    fun get(id: AppScreenId) = screens.single { it.id == id }
}
