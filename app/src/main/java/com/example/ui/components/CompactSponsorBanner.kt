package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
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
import coil.compose.SubcomposeAsyncImage
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.AutoAwesome
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

    // Record impression for settled sponsor banner
    LaunchedEffect(pagerState.settledPage, validSponsors) {
        if (validSponsors.isNotEmpty()) {
            val currentSponsor = validSponsors[pagerState.settledPage.coerceIn(0, validSponsors.lastIndex)]
            onSponsorImpression?.invoke(currentSponsor.id)
        }
    }

    // Smooth auto-scroll ticker: runs continuously without cancelling mid-scroll
    LaunchedEffect(validSponsors.size) {
        if (validSponsors.size > 1) {
            while (true) {
                delay(slideIntervalMillis)
                if (!pagerState.isScrollInProgress) {
                    val nextPage = (pagerState.currentPage + 1) % validSponsors.size
                    try {
                        pagerState.animateScrollToPage(
                            page = nextPage,
                            animationSpec = tween(
                                durationMillis = 650,
                                easing = FastOutSlowInEasing
                            )
                        )
                    } catch (_: Exception) {
                        // Safely ignore touch cancellation
                    }
                }
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Touch-Swipeable Horizontal Pager with snap and page spacing
        HorizontalPager(
            state = pagerState,
            pageSpacing = 12.dp,
            beyondViewportPageCount = 1,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("compact_sponsor_carousel")
        ) { pageIndex ->
            val sponsor = validSponsors[pageIndex.coerceIn(0, validSponsors.lastIndex)]
            val hasCustomImage = sponsor.imageUrl.isNotBlank()

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
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
                            SubcomposeAsyncImage(
                                model = imageRequest,
                                contentDescription = sponsor.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                loading = {
                                    SponsorOfflineCardContent(sponsor = sponsor)
                                },
                                error = {
                                    SponsorOfflineCardContent(sponsor = sponsor)
                                }
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
                            SubcomposeAsyncImage(
                                model = DEFAULT_STUDENT_BANNER_IMAGE,
                                contentDescription = sponsor.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                error = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color(0xFF1E293B)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.School,
                                            contentDescription = null,
                                            tint = Color(0xFF38BDF8).copy(alpha = 0.18f),
                                            modifier = Modifier.size(72.dp)
                                        )
                                    }
                                }
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
                    .padding(top = 5.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in validSponsors.indices) {
                    val isSelected = i == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(
                                width = if (isSelected) 18.dp else 6.dp,
                                height = 6.dp
                            )
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Color(0xFF38BDF8)
                                else Color(0x8894A3B8)
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

/**
 * Beautiful offline fallback card that ensures zero black/empty space when offline.
 */
@Composable
private fun SponsorOfflineCardContent(sponsor: CachedSponsor) {
    val displayName = if (sponsor.name.isNotBlank()) sponsor.name else "Educational Partner"
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E293B),
                        Color(0xFF0F172A)
                    )
                )
            )
    ) {
        // Decorative background watermark icon
        Icon(
            imageVector = Icons.Default.School,
            contentDescription = null,
            tint = Color(0xFF38BDF8).copy(alpha = 0.08f),
            modifier = Modifier
                .size(130.dp)
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF0284C7).copy(alpha = 0.25f),
                    border = BorderStroke(0.6.dp, Color(0xFF38BDF8).copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "Featured Sponsor",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFBAE6FD)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color.White.copy(alpha = 0.08f)
                ) {
                    Text(
                        text = "Student Offer",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 16.sp,
                        letterSpacing = 0.2.sp
                    ),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Special educational support & resources for students",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = Color(0xFF94A3B8),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.16f),
                border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.3f)),
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = "Tap to Learn More",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(9.dp)
                    )
                }
            }
        }
    }
}

