package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import com.example.R
import com.example.data.CachedSponsor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest

// Default high-resolution student library portrait matching user's design
private const val DEFAULT_STUDENT_BANNER_IMAGE =
    "https://images.unsplash.com/photo-1523240795612-9a054b0db644?q=80&w=800&auto=format&fit=crop"

/**
 * Rich Hero Promo Card Banner with 4-second auto-slide animation and pagination dots,
 * perfectly matching the user's uploaded reference screenshot.
 */
@Composable
fun CompactSponsorCarousel(
    sponsors: List<CachedSponsor>,
    onSponsorClick: (String, String) -> Unit,
    onSponsorImpression: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
    slideIntervalMillis: Long = 4000L
) {
    val validSponsors = remember(sponsors) {
        sponsors.filter { it.isValidCurrently() }
    }

    if (validSponsors.isEmpty()) {
        return
    }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { validSponsors.size }
    )
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Record impression for currently displayed sponsor
    LaunchedEffect(pagerState.currentPage, validSponsors) {
        if (validSponsors.isNotEmpty()) {
            val currentSponsor = validSponsors[pagerState.currentPage.coerceIn(0, validSponsors.lastIndex)]
            onSponsorImpression?.invoke(currentSponsor.id)
        }
    }

    // Auto-scroll ticker every 4 seconds if there are 2 or more sponsors
    LaunchedEffect(validSponsors.size, pagerState.currentPage) {
        if (validSponsors.size > 1) {
            delay(slideIntervalMillis)
            if (!pagerState.isScrollInProgress) {
                val nextPage = (pagerState.currentPage + 1) % validSponsors.size
                pagerState.animateScrollToPage(
                    page = nextPage,
                    animationSpec = tween(600)
                )
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Touch-Swipeable Horizontal Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("compact_sponsor_carousel")
        ) { pageIndex ->
            val sponsor = validSponsors[pageIndex.coerceIn(0, validSponsors.lastIndex)]
            val hasCustomImage = sponsor.imageUrl.isNotBlank()

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(142.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onSponsorClick(sponsor.websiteUrl, sponsor.id) },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0F172A) // Dark Slate Blue / Midnight
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = Color(0xFF1E293B)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                if (hasCustomImage) {
                    // FULL-BLEED POSTER BANNER (Direct uploaded image or Base64 data URI where client put all texts & graphics)
                    val base64Bitmap = remember(sponsor.imageUrl) {
                        if (sponsor.imageUrl.startsWith("data:image") && sponsor.imageUrl.contains("base64,")) {
                            try {
                                val cleanBase64 = sponsor.imageUrl.substringAfter("base64,")
                                val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                                BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)?.asImageBitmap()
                            } catch (_: Exception) {
                                null
                            }
                        } else null
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        if (base64Bitmap != null) {
                            Image(
                                bitmap = base64Bitmap,
                                contentDescription = sponsor.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            val imageRequest = remember(sponsor.imageUrl) {
                                ImageRequest.Builder(context)
                                    .data(sponsor.imageUrl)
                                    .crossfade(true)
                                    .diskCacheKey(sponsor.imageUrl)
                                    .memoryCacheKey(sponsor.imageUrl)
                                    .build()
                            }
                            AsyncImage(
                                model = imageRequest,
                                contentDescription = sponsor.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                } else {
                    // Clean Default Layout with user-configured Name & Tagline
                    val displayName = remember(sponsor.name) {
                        if (sponsor.name.isNotBlank()) sponsor.name else "ClassMate Partner"
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        // Right Side: Default Photo with smooth fade gradient
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(0.55f)
                                .align(Alignment.CenterEnd)
                        ) {
                            AsyncImage(
                                model = DEFAULT_STUDENT_BANNER_IMAGE,
                                contentDescription = sponsor.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            // Dark gradient overlay extending from left to right
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            colorStops = arrayOf(
                                                0.0f to Color(0xFF0F172A),
                                                0.35f to Color(0xEB0F172A),
                                                0.75f to Color(0x660F172A),
                                                1.0f to Color(0x1A0F172A)
                                            )
                                        )
                                    )
                            )
                        }

                        // Left Side: Typography & Action Button
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 16.dp, top = 14.dp, end = 14.dp, bottom = 14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth(0.68f)
                            ) {
                                Text(
                                    text = displayName,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 15.sp,
                                        letterSpacing = 0.3.sp
                                    ),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = "Featured Sponsor",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Action Pill Button: "Tap to Learn More >"
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.18f),
                                border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.35f)),
                                modifier = Modifier.clip(RoundedCornerShape(12.dp))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Tap to Learn More",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(8.5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Centered Pagination Dots below the card (matches actual number of sponsors)
        if (validSponsors.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in validSponsors.indices) {
                    val isSelected = i == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(
                                width = if (isSelected) 16.dp else 5.dp,
                                height = 5.dp
                            )
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else Color(0x5594A3B8)
                            )
                            .clickable {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(i)
                                }
                            }
                    )
                }
            }
        }
    }
}

/**
 * Backward-compatible single sponsor banner.
 */
@Composable
fun CompactSponsorBanner(
    sponsor: CachedSponsor?,
    onSponsorClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    CompactSponsorCarousel(
        sponsors = listOfNotNull(sponsor),
        onSponsorClick = { url, _ -> onSponsorClick(url) },
        modifier = modifier
    )
}

