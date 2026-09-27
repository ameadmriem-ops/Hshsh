package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.admob.RewardViewModel
import com.example.ui.IptvWebView
import com.example.ui.RewardAdDialog
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val rewardViewModel: RewardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    val uiState by rewardViewModel.uiState.collectAsStateWithLifecycle()
                    val context = LocalContext.current

                    // Handle toast notifications
                    LaunchedEffect(uiState.toastMessage) {
                        uiState.toastMessage?.let { msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            rewardViewModel.consumeToast()
                        }
                    }

                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF07080C)),
                        containerColor = Color(0xFF07080C)
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Top Status Bar when 24h Ad-Free mode is active
                                AnimatedVisibility(
                                    visible = uiState.isAdFree,
                                    enter = fadeIn(),
                                    exit = fadeOut()
                                ) {
                                    AdFreeTopBar(
                                        remainingText = uiState.remainingAdFreeFormatted,
                                        onClick = { rewardViewModel.openDialog() }
                                    )
                                }

                                // Main IPTV Web Application
                                IptvWebView(
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // 5 Ads Reward Dialog
                            RewardAdDialog(
                                uiState = uiState,
                                onWatchAdClicked = { activity ->
                                    rewardViewModel.onWatchAdClicked(activity)
                                },
                                onDismissRequest = {
                                    rewardViewModel.closeDialog()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        rewardViewModel.refreshStatus()
    }
}

@Composable
fun AdFreeTopBar(
    remainingText: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("ad_free_status_bar"),
        color = Color(0xFF11131A),
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4EFA8B),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "الاستخدام بدون إعلانات مفعل",
                    color = Color(0xFFE2E4EB),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E212D))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "متبقي: $remainingText",
                    color = Color(0xFFA18CFF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
