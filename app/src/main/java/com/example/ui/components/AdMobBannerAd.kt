package com.example.ui.components

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import java.io.File
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.BuildConfig
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

object AdMobConstants {
    const val APP_ID = "ca-app-pub-8519955082545459~1152490013"
    const val BANNER_AD_UNIT_ID = "ca-app-pub-8519955082545459/4440998339"
    // Google's official sample banner ad unit ID for development and emulators
    const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
}

fun isRunningOnEmulator(): Boolean {
    val fingerprint = Build.FINGERPRINT ?: ""
    val model = Build.MODEL ?: ""
    val manufacturer = Build.MANUFACTURER ?: ""
    val brand = Build.BRAND ?: ""
    val device = Build.DEVICE ?: ""
    val product = Build.PRODUCT ?: ""
    val hardware = Build.HARDWARE ?: ""

    return fingerprint.startsWith("generic")
            || fingerprint.startsWith("unknown")
            || model.contains("google_sdk", ignoreCase = true)
            || model.contains("Emulator", ignoreCase = true)
            || model.contains("Android SDK built for x86", ignoreCase = true)
            || manufacturer.contains("Genymotion", ignoreCase = true)
            || (brand.startsWith("generic") && device.startsWith("generic"))
            || "google_sdk" == product
            || hardware.contains("goldfish", ignoreCase = true)
            || hardware.contains("ranchu", ignoreCase = true)
            || hardware.contains("cuttlefish", ignoreCase = true)
            || product.contains("cuttlefish", ignoreCase = true)
            || device.contains("cuttlefish", ignoreCase = true)
            || hardware.contains("qemu", ignoreCase = true)
}

private fun isDeviceOnline(context: Context): Boolean {
    return try {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val network = connectivityManager?.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } catch (_: Throwable) {
        true
    }
}

/**
 * Smart Auto-Hiding AdMob Banner:
 * - If user is offline or ad fails to load: Auto-hides completely (0 height, 0 gap).
 * - If user turns off network while ad was loaded: Stays seamlessly displayed without flickering.
 * - Safely handles emulators and devices without crashing or JavaScript errors.
 */
@Composable
fun AdMobBannerAd(
    modifier: Modifier = Modifier,
    adUnitId: String = AdMobConstants.BANNER_AD_UNIT_ID,
    bannerHeight: Dp = 148.dp,
    fallback: (@Composable () -> Unit)? = null
) {
    val context = LocalContext.current
    val isOnline = remember(context) { isDeviceOnline(context) }
    var isAdLoaded by remember { mutableStateOf(false) }
    var adFailedToLoad by remember { mutableStateOf(false) }

    val effectiveAdUnitId = remember(adUnitId) {
        if (isRunningOnEmulator() || BuildConfig.DEBUG) {
            AdMobConstants.TEST_BANNER_AD_UNIT_ID
        } else {
            adUnitId
        }
    }

    val isEmulator = remember { isRunningOnEmulator() }

    // If running in an emulator/container without DRM hardware rendering or offline, immediately show fallback
    if ((isEmulator || !isOnline || adFailedToLoad) && fallback != null) {
        fallback()
        return
    }

    // AUTO-HIDE: If offline or failed to load and no fallback, collapse cleanly
    if ((!isOnline || adFailedToLoad) && !isAdLoaded) {
        return
    }

    // If ad is not loaded yet and fallback is provided, render fallback immediately to avoid any blank gap
    if (!isAdLoaded && fallback != null) {
        fallback()
        return
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(bannerHeight)
            .clip(RoundedCornerShape(18.dp))
            .testTag("admob_banner_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0F172A)
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

                // Center Ad Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth(),
                        factory = { ctx ->
                            try {
                                AdView(ctx).apply {
                                    if (isRunningOnEmulator()) {
                                        // Prevent Mesa GPU rendernode errors and video JS crashes on emulators
                                        setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
                                    }
                                    setAdSize(AdSize.LARGE_BANNER)
                                    this.adUnitId = effectiveAdUnitId
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
                                                if (!isAdLoaded) {
                                                    adFailedToLoad = true
                                                }
                                            }
                                        }
                                    }
                                    loadAd(AdRequest.Builder().build())
                                }
                            } catch (_: Throwable) {
                                adFailedToLoad = true
                                android.view.View(ctx)
                            }
                        }
                    )
                }

                // Bottom indicator pill
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
