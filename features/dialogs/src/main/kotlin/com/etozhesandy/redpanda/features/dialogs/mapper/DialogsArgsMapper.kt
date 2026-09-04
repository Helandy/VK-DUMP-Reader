package com.etozhesandy.redpanda.features.dialogs.mapper

import com.etozhesandy.redpanda.core.navigation.Routes
import com.etozhesandy.redpanda.features.dialogs.model.DialogsArgs

fun Routes.Dialogs.toArgs(): DialogsArgs = DialogsArgs(profileId = profileId)
