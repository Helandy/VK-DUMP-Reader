package com.etozhesandy.redpanda.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Derived from M3's default type scale via `.copy(...)` rather than fresh `TextStyle(...)`s, so
// each style keeps M3's default lineHeight/letterSpacing — building bare TextStyles here previously
// dropped them, collapsing line spacing on multi-line text (most visible in chat messages).
private val defaultTypography = Typography()

val RedPandaTypography = defaultTypography.copy(
    titleLarge = defaultTypography.titleLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleMedium = defaultTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    bodyLarge = defaultTypography.bodyLarge.copy(fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = defaultTypography.bodyMedium.copy(fontWeight = FontWeight.Normal, fontSize = 14.sp),
    labelSmall = defaultTypography.labelSmall.copy(fontWeight = FontWeight.Medium, fontSize = 12.sp),
)
