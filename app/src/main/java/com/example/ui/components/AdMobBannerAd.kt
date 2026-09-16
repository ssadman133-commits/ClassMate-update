package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

object AdMobConstants {
    const val APP_ID = "ca-app-pub-8519955082545459~1152490013"
    const val BANNER_AD_UNIT_ID = "ca-app-pub-8519955082545459/4440998339"
}

/**
 * Large Hero AdMob Banner matching the exact height (148.dp) and visual style of the
 * sponsor banner carousel, ensuring seamless layout consistency.
 */
@Composable
fun AdMobBannerAd(
    modifier: Modifier = Modifier,
    adUnitId: String = AdMobConstants.BANNER_AD_UNIT_ID,
    bannerHeight: Dp = 148.dp
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val hasPlayServices = remember(context) {
        try {
            com.google.android.gms.common.GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(context) == com.google.android.gms.common.ConnectionResult.SUCCESS
        } catch (_: Throwable) {
            false
        }
    }
    var isAdLoaded by remember { mutableStateOf(false) }
    var adFailedToLoad by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(bannerHeight)
            .clip(RoundedCornerShape(18.dp))
            .testTag("admob_banner_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0F172A) // Matches exact sponsor carousel Midnight Slate theme
        ),
        border = BorderStroke(
            width = 1.dp,
            color = Color(0xFF1E293B)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Subtle premium gradient backdrop
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1E293B).copy(alpha = 0.55f),
                                Color(0xFF0F172A)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top compliant header row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF2563EB).copy(alpha = 0.35f)
                        ) {
                            Text(
                                text = "Ad",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF93C5FD),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Text(
                            text = "Google Partner Network",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }

                    Text(
                        text = "Sponsored",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF94A3B8)
                    )
                }

                // Center Ad Area - Loads Large Banner (320x100) or Adaptive/Standard Banner (320x50)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasPlayServices) {
                        AndroidView(
                            modifier = Modifier.fillMaxWidth(),
                            factory = { context ->
                                AdView(context).apply {
                                    setAdSize(AdSize.LARGE_BANNER)
                                    this.adUnitId = adUnitId
                                    adListener = object : AdListener() {
                                        override fun onAdLoaded() {
                                            isAdLoaded = true
                                            adFailedToLoad = false
                                        }

                                        override fun onAdFailedToLoad(error: LoadAdError) {
                                            if (adSize == AdSize.LARGE_BANNER) {
                                                setAdSize(AdSize.BANNER)
                                                loadAd(AdRequest.Builder().build())
                                            } else {
                                                isAdLoaded = false
                                                adFailedToLoad = true
                                            }
                                        }
                                    }
                                    loadAd(AdRequest.Builder().build())
                                }
                            }
                        )
                    } else {
                        // Environment without Play Services (e.g. cloud emulator preview)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Google AdMob Network",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE2E8F0)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Ad ready • Live on Play Store devices with GMS",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                // Bottom subtle indicator pill matching carousel pagination aesthetic
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 28.dp, height = 3.dp)
                            .background(Color(0xFF334155), RoundedCornerShape(2.dp))
                    )
                }
            }
        }
    }
}
