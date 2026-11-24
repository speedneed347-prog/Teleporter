package com.example.ui.navigation

object NavRoutes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val MY_SCRIPTS = "my_scripts"
    const val VIDEOS = "videos"
    const val SETTINGS = "settings"
    const val IMPORT_DOCUMENT = "import_document"

    const val SCRIPT_EDITOR_PATTERN = "script_editor/{scriptId}"
    const val TELEPROMPTER_READER_PATTERN = "teleprompter_reader/{scriptId}"
    const val CAMERA_TELEPROMPTER_PATTERN = "camera_teleprompter/{scriptId}"

    fun scriptEditor(scriptId: Long = 0L): String = "script_editor/$scriptId"
    fun teleprompterReader(scriptId: Long): String = "teleprompter_reader/$scriptId"
    fun cameraTeleprompter(scriptId: Long = -1L): String = "camera_teleprompter/$scriptId"
}
