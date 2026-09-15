package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.data.CachedSponsor

/**
 * Modern compact sponsored banner that complies with height, styling and
 * complete collapse when offline or no active sponsor.
 */
@Composable
fun SponsorBanner(
    sponsor: CachedSponsor?,
    onSponsorClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    CompactSponsorBanner(
        sponsor = sponsor,
        onSponsorClick = onSponsorClick,
        modifier = modifier
    )
}
