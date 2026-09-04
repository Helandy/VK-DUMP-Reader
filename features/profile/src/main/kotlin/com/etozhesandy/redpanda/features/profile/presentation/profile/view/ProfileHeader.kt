package com.etozhesandy.redpanda.features.profile.presentation.profile.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.etozhesandy.redpanda.core.model.Profile
import com.etozhesandy.redpanda.core.model.Sex
import com.etozhesandy.redpanda.features.profile.R

@Composable
fun ProfileHeader(profile: Profile, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (profile.avatarPath != null) {
                AsyncImage(
                    model = profile.avatarPath,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(72.dp).clip(CircleShape),
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp),
                )
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(profile.displayName, style = MaterialTheme.typography.titleLarge)
                profile.vkId?.let { vkId ->
                    Text(
                        stringResource(R.string.profile_vk_id, vkId),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        profile.birthDate?.let { InfoRow(stringResource(R.string.profile_birth_date), it) }
        InfoRow(stringResource(R.string.profile_sex), sexLabel(profile.sex))
        profile.country?.let { InfoRow(stringResource(R.string.profile_country), it) }
        profile.city?.let { InfoRow(stringResource(R.string.profile_city), it) }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 8.dp),
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun sexLabel(sex: Sex): String = stringResource(
    when (sex) {
        Sex.MALE -> R.string.profile_sex_male
        Sex.FEMALE -> R.string.profile_sex_female
        Sex.UNKNOWN -> R.string.profile_sex_unknown
    },
)
