package processing.app.ui.preferences

import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import processing.app.LocalPreferences
import processing.app.ui.PDEPreference
import processing.app.ui.PDEPreferencePane
import processing.app.ui.PDEPreferencePanes
import processing.app.ui.PDEPreferences
import processing.app.ui.preferences.Sketches.Companion.sketches
import processing.app.ui.theme.LocalLocale

class Other {
    companion object{
        val other = PDEPreferencePane(
            nameKey = "preferences.pane.other",
            icon = {
                Icon(Icons.Default.Lightbulb, contentDescription = "Other Preferences")
            },
            after = sketches
        )

        // Updated function signature: removed PDEPreferencePanes parameter 
        fun register() {
            PDEPreferences.register(
                PDEPreference(
                    key = "preferences.show_other",
                    descriptionKey = "preferences.other",
                    pane = other,
                    control = { preference, setPreference ->
                        val showOther = preference?.toBoolean() ?: false
                        Switch(
                            checked = showOther,
                            onCheckedChange = {
                                setPreference(it.toString())
                            }
                        )
                    }
                )
            )
        }

/**
 * Handles the registration and grouping of additional preference options.
 *
 * This composable checks whether the "show_other" preference is enabled.
 * If enabled, it dynamically adds related preference options to the same
 * preference group using a DisposableEffect tied to the provided panes.
 *
 * @param panes The collection of preference panes used to locate and
 *              modify the appropriate preference group.
*/
        @Composable
        fun handleOtherPreferences(panes: PDEPreferencePanes) {
            // This function can be used to handle any specific logic related to other preferences if needed
            val prefs = LocalPreferences.current
            val locale = LocalLocale.current

            // Exit early if the "show_other" preference is disabled
            if (prefs["preferences.show_other"]?.toBoolean() != true) {
                return
            }
            DisposableEffect(panes) {
                // add all the other options to the same group as the current one
                val group =
                    panes[other]?.find { group -> group.any { preference -> preference.key == "preferences.show_other" } } as? MutableList<PDEPreference>

                // Collect all existing preference keys already registered in panes
                // Flattening because panes → groups → preferences is a nested structure
                val existing = panes.values.flatten().flatten().map { preference -> preference.key }

                // Identify preference keys that are present in prefs but not yet registered
                // Only include String keys and sort them for consistent ordering
                val keys = prefs.keys.mapNotNull { it as? String }.filter { it !in existing }.sorted()

                // Dynamically create and register missing preferences 
                for (prefKey in keys) {
                    val descriptionKey = "preferences.$prefKey"
                    val preference = PDEPreference(
                        key = prefKey,
                        descriptionKey = if (locale.containsKey(descriptionKey)) descriptionKey else prefKey,
                        pane = other,
                        // Dynamically choose UI control based on preference type
                        control = { preference, updatePreference ->

                            // If the stored value can be parsed strictly as Boolean,
                            // render a Switch control
                            if (preference?.toBooleanStrictOrNull() != null) {
                                Switch(
                                    checked = preference.toBoolean(),
                                    onCheckedChange = {
                                        updatePreference(it.toString())
                                    }
                                )
                                return@PDEPreference
                            }

                            // Otherwise render a text input field for string values 
                            OutlinedTextField(
                                modifier = Modifier.widthIn(max = 300.dp),
                                value = preference ?: "",
                                onValueChange = {
                                    updatePreference(it)
                                }
                            )
                        }
                    )
                    // Add the dynamically created preferences to the identified group
                    group?.add(preference)
                }

                // When the composable leaves composition,
                // remove dynamically added preferences while keeping the base toogle
                onDispose {
                    group?.apply {
                        removeIf { it.key != "preferences.show_other" }
                    }
                }
            }
        }
    }
}