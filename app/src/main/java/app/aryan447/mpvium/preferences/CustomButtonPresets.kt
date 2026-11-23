package app.aryan447.mpvium.preferences

/**
 * Curated preset templates for custom player buttons.
 * Users can pick and add these to their player controls overlay if desired.
 */
object CustomButtonPresets {

  data class PresetButton(
    val title: String,
    val description: String,
    val code: String,
    val longPressCode: String = "",
    val requiredScript: String? = null,
  )

  val PRESETS: List<PresetButton> = listOf(
    PresetButton(
      title = "Sub Delay +",
      description = "Increase subtitle delay by 100ms (long-press to decrease)",
      code = "add sub-delay 0.1",
      longPressCode = "add sub-delay -0.1",
    ),
    PresetButton(
      title = "Audio Delay +",
      description = "Increase audio delay by 100ms (long-press to decrease)",
      code = "add audio-delay 0.1",
      longPressCode = "add audio-delay -0.1",
    ),
    PresetButton(
      title = "Cycle Speed",
      description = "Cycle speed (1.25x, 1.5x, 2.0x, 1.0x; long-press resets to 1x)",
      code = "cycle-values speed 1.25 1.5 2.0 1.0",
      longPressCode = "set speed 1.0",
    ),
    PresetButton(
      title = "A-B Loop",
      description = "Set point A, point B, and clear loop (long-press clears)",
      code = "ab-loop",
      longPressCode = "set ab-loop-a no; set ab-loop-b no; show-text \"A-B loop cleared\"",
    ),
    PresetButton(
      title = "Toggle Stats",
      description = "Toggle video frame, bitrate, and decoder stats overlay",
      code = "script-binding stats/display-stats-toggle",
      longPressCode = "show-progress",
    ),
    PresetButton(
      title = "Snapshot",
      description = "Capture video frame snapshot (long-press captures with subtitles)",
      code = "screenshot video",
      longPressCode = "screenshot subtitles",
    ),
    PresetButton(
      title = "Night Mode",
      description = "Toggle loudness normalization audio filter for loud dialogue & quiet effects",
      code = "cycle-values af \"loudnorm=I=-16:TP=-1.5:LRA=11\" \"\"",
      longPressCode = "set af \"\"",
    ),
    PresetButton(
      title = "Rotate 90°",
      description = "Rotate video clockwise by 90 degrees (long-press resets)",
      code = "cycle-values video-rotate 90 180 270 0",
      longPressCode = "set video-rotate 0",
    ),
    PresetButton(
      title = "Toggle Anime4K",
      description = "Toggle Anime4K restore shader on or off",
      code = "no-osd change-list glsl-shaders toggle \"~~/shaders/Anime4K_Restore_CNN_M.glsl\"; show-text \"Anime4K toggled\"",
      longPressCode = "no-osd change-list glsl-shaders clr \"\"; show-text \"Shaders cleared\"",
    ),
    PresetButton(
      title = "Delete File",
      description = "Mark file for deletion via Script.lua (long-press lists marked files)",
      code = "mp.command(\"script-binding Script/delete_file\")",
      longPressCode = "mp.command(\"script-binding Script/list_marks\")",
      requiredScript = "Script.lua",
    ),
    PresetButton(
      title = "D-Crop",
      description = "Toggle automatic black bar cropping via Script.lua",
      code = "mp.command(\"script-binding Script/toggle_crop\")",
      longPressCode = "",
      requiredScript = "Script.lua",
    ),
  )
}
