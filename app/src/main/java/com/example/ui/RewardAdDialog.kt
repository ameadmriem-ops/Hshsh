package com.example.ui

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.admob.RewardPreferences
import com.example.admob.RewardUiState

@Composable
fun RewardAdDialog(
    uiState: RewardUiState,
    onWatchAdClicked: (Activity) -> Unit,
    onDismissRequest: () -> Unit = {}
) {
    if (!uiState.showDialog) return

    val context = LocalContext.current
    val activity = context as? Activity

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Dialog(
            onDismissRequest = {
                if (uiState.isAdFree) {
                    onDismissRequest()
                }
            },
            properties = DialogProperties(
                dismissOnBackPress = uiState.isAdFree,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(24.dp))
                    .border(
                        width = 1.dp,
                        color = Color(0x337C5CFF),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .testTag("reward_ad_dialog"),
                color = Color(0xFF11131A),
                tonalElevation = 8.dp,
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .animateContentSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Icon / Badge
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF7C5CFF).copy(alpha = 0.35f),
                                        Color(0xFF181B26)
                                    )
                                )
                            )
                            .border(1.dp, Color(0x447C5CFF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stars,
                            contentDescription = "مكافأة",
                            tint = Color(0xFFA18CFF),
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Expired notice banner if applicable
                    AnimatedVisibility(
                        visible = uiState.hasExpiredNotice,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp)
                                .testTag("expired_notice_banner"),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF261820)
                            ),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = Brush.linearGradient(
                                    listOf(Color(0xFFFF476F), Color(0x55FF476F))
                                )
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LockReset,
                                    contentDescription = null,
                                    tint = Color(0xFFFF476F),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "انتهت مدة الاستخدام المجاني",
                                        color = Color(0xFFFF859F),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "شاهد 5 إعلانات للحصول على 24 ساعة إضافية",
                                        color = Color(0xFFC4C6CF),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    // Dialog Title
                    Text(
                        text = "شاهد 5 إعلانات واحصل على استخدام بدون إعلانات لمدة 24 ساعة",
                        color = Color(0xFFF7F7FB),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp,
                        modifier = Modifier.testTag("dialog_title_text")
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Counter box with glowing gradient
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF181B26))
                            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(16.dp))
                            .padding(vertical = 18.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "التقدم الحالي",
                                color = Color(0xFF9297A8),
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${uiState.progress} / ${RewardPreferences.MAX_PROGRESS}",
                                color = if (uiState.progress >= RewardPreferences.MAX_PROGRESS) {
                                    Color(0xFF4EFA8B)
                                } else {
                                    Color(0xFFFFFFFF)
                                },
                                fontSize = 34.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.testTag("counter_text")
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Smooth animated progress bar
                            val animatedProgress by animateFloatAsState(
                                targetValue = uiState.progress.toFloat() / RewardPreferences.MAX_PROGRESS.toFloat(),
                                label = "reward_progress_animation"
                            )
                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .testTag("progress_indicator"),
                                color = Color(0xFF7C5CFF),
                                trackColor = Color(0xFF242735)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Searching state with 9-second live timer
                    AnimatedVisibility(
                        visible = uiState.isSearching,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1F2230))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .testTag("searching_ad_indicator"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color(0xFFA18CFF),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "جاري البحث عن إعلان... (${uiState.searchSecondsLeft} ثانية)",
                                color = Color(0xFFD4D6DF),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Action Button: "مشاهدة إعلان"
                    val isButtonEnabled = !uiState.isRewardProcessing && activity != null
                    Button(
                        onClick = {
                            if (activity != null) {
                                onWatchAdClicked(activity)
                            }
                        },
                        enabled = isButtonEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("watch_ad_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF7C5CFF),
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFF383A49),
                            disabledContentColor = Color(0xFF757885)
                        )
                    ) {
                        if (uiState.isRewardProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "جاري المعالجة...",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "مشاهدة إعلان",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Explanatory note
                    Text(
                        text = "كل محاولة مكتملة تحتسب نقطة واحدة. بعد 5 محاولات يتاح استخدام التطبيق بدون إعلانات لمدة 24 ساعة.",
                        color = Color(0xFF7A7E8D),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
