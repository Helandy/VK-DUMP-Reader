package com.etozhesandy.redpanda.features.settings.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.etozhesandy.redpanda.core.designsystem.components.BaseScreen
import com.etozhesandy.redpanda.core.designsystem.components.DIALOG_SORT_OPTIONS
import com.etozhesandy.redpanda.core.designsystem.components.MEDIA_SORT_OPTIONS
import com.etozhesandy.redpanda.core.designsystem.components.MESSAGE_SORT_OPTIONS
import com.etozhesandy.redpanda.core.settings.AppSettings
import com.etozhesandy.redpanda.features.settings.R
import com.etozhesandy.redpanda.features.settings.presentation.SettingsState
import com.etozhesandy.redpanda.features.settings.presentation.utils.formatBytes
import com.etozhesandy.redpanda.features.settings.presentation.view.SecuritySection
import com.etozhesandy.redpanda.features.settings.presentation.view.SortDefaultItem

@Composable
fun SettingsScreen(
    state: SettingsState.State,
    onEvent: (SettingsState.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    BaseScreen(
        title = stringResource(R.string.settings_title),
        modifier = modifier,
        onBack = { onEvent(SettingsState.Event.BackClicked) },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        ) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_profiles_cache)) },
                supportingContent = {
                    Text(
                        if (state.isCacheSizeLoading) {
                            stringResource(R.string.settings_cache_calculating)
                        } else {
                            formatBytes(context, state.profilesCacheBytes ?: 0L)
                        },
                    )
                },
                trailingContent = {
                    if (state.isCacheSizeLoading) {
                        CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                    }
                },
            )

            var sliderValue by remember(state.coilCacheSizeMb) {
                mutableFloatStateOf(state.coilCacheSizeMb.toFloat())
            }
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_image_cache_size)) },
                supportingContent = { Text(stringResource(R.string.value_megabytes, sliderValue.toInt())) },
            )
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = {
                    onEvent(SettingsState.Event.CoilCacheSizeChanged(sliderValue.toInt()))
                },
                valueRange = AppSettings.COIL_CACHE_MIN_MB.toFloat()..AppSettings.COIL_CACHE_MAX_MB.toFloat(),
                steps = (AppSettings.COIL_CACHE_MAX_MB - AppSettings.COIL_CACHE_MIN_MB) /
                    AppSettings.COIL_CACHE_STEP_MB - 1,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )

            var imageWidthSliderValue by remember(state.mediaImageWidthDp) {
                mutableFloatStateOf(state.mediaImageWidthDp.toFloat())
            }
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_media_image_width)) },
                supportingContent = { Text(stringResource(R.string.value_dp, imageWidthSliderValue.toInt())) },
            )
            Slider(
                value = imageWidthSliderValue,
                onValueChange = { imageWidthSliderValue = it },
                onValueChangeFinished = {
                    onEvent(SettingsState.Event.MediaImageWidthChanged(imageWidthSliderValue.toInt()))
                },
                valueRange = AppSettings.MEDIA_IMAGE_MIN_WIDTH_DP.toFloat()..AppSettings.MEDIA_IMAGE_MAX_WIDTH_DP.toFloat(),
                steps = (AppSettings.MEDIA_IMAGE_MAX_WIDTH_DP - AppSettings.MEDIA_IMAGE_MIN_WIDTH_DP) /
                    AppSettings.MEDIA_IMAGE_WIDTH_STEP_DP - 1,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )

            HorizontalDivider()
            SecuritySection(state = state, onEvent = onEvent)

            HorizontalDivider()
            Text(
                text = stringResource(R.string.settings_default_sort),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
            )

            SortDefaultItem(
                title = stringResource(R.string.settings_dialog_list),
                options = DIALOG_SORT_OPTIONS,
                selected = state.defaultDialogSort,
                ascending = state.defaultDialogSortAscending,
                onSelect = { onEvent(SettingsState.Event.DefaultDialogSortSelected(it)) },
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_newest_first)) },
                supportingContent = {
                    Text(
                        if (state.defaultChatReversed) {
                            stringResource(R.string.settings_newest_first_on)
                        } else {
                            stringResource(R.string.settings_newest_first_off)
                        },
                    )
                },
                trailingContent = {
                    Switch(
                        checked = state.defaultChatReversed,
                        onCheckedChange = { onEvent(SettingsState.Event.DefaultChatReversedChanged(it)) },
                    )
                },
            )

            SortDefaultItem(
                title = stringResource(R.string.settings_chat_media),
                options = MEDIA_SORT_OPTIONS,
                selected = state.defaultMediaSort,
                ascending = state.defaultMediaSortAscending,
                onSelect = { onEvent(SettingsState.Event.DefaultMediaSortSelected(it)) },
            )

            SortDefaultItem(
                title = stringResource(R.string.settings_message_search),
                options = MESSAGE_SORT_OPTIONS,
                selected = state.defaultSearchSort,
                ascending = state.defaultSearchSortAscending,
                onSelect = { onEvent(SettingsState.Event.DefaultSearchSortSelected(it)) },
            )
        }
    }
}
