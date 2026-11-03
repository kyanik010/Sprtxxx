package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SubscriptionProfile
import com.example.data.model.SubscriptionType
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AccountScreen(
    subscription: SubscriptionProfile,
    onSaveSubscription: suspend (SubscriptionType, String, String, String, String) -> Result<Int>,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var showEditDialog by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(NightNavy)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = CalmGold,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "إدارة الاشتراك والجهاز",
                    style = MaterialTheme.typography.headlineMedium,
                    color = SnowWhite,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Active Status Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = BorderStroke(1.dp, CalmGold.copy(alpha = 0.7f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("subscription_status_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = subscription.name,
                                style = MaterialTheme.typography.titleLarge,
                                color = SnowWhite,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "النوع: ${if (subscription.type == SubscriptionType.XTREAM_CODES) "Xtream Codes API" else if (subscription.type == SubscriptionType.M3U_PLAYLIST) "M3U Playlist" else "غير محدد"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = CalmGold
                            )
                        }

                        Surface(
                            color = SportsGreen.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, SportsGreen)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(SportsGreen)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (subscription.isActive) "تم الاتصال" else "غير متصل",
                                    color = SportsGreen,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = CardBorder, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "القنوات المعروضة مأخوذة من قائمة الاشتراك المحمّلة.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }
        }

        // Device & Hardware Info Card
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "معلومات الجهاز",
                        style = MaterialTheme.typography.titleMedium,
                        color = SnowWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("طراز الجهاز:", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                        Text(subscription.deviceModel, style = MaterialTheme.typography.bodyMedium, color = SnowWhite, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("مُعرّف الجهاز (Device ID):", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                        Text(subscription.deviceId, style = MaterialTheme.typography.bodyMedium, color = CalmGold, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("المنصة المعتمدة:", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                        Text("Android TV & Mobile", style = MaterialTheme.typography.bodyMedium, color = SnowWhite)
                    }
                }
            }
        }

        // Subscription Setup / Change Button
        item {
            Button(
                onClick = { showEditDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = DarkSportsBlue),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CalmGold),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("edit_subscription_btn")
            ) {
                Icon(Icons.Default.SettingsInputAntenna, contentDescription = null, tint = CalmGold)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ربط أو تحديث بيانات اشتراك IPTV الخاص بك",
                    style = MaterialTheme.typography.titleMedium,
                    color = SnowWhite,
                    fontWeight = FontWeight.Bold
                )
            }
        }


    }

    // Edit Subscription Dialog
    if (showEditDialog) {
        var selectedType by remember { mutableStateOf(subscription.type) }
        var serverInput by remember { mutableStateOf(subscription.serverUrl) }
        var userInput by remember { mutableStateOf(subscription.username) }
        var passInput by remember { mutableStateOf(subscription.password) }
        var m3uInput by remember { mutableStateOf(subscription.m3uUrl) }
        var localError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { if (!isSaving) showEditDialog = false },
            title = {
                Text(
                    text = "تهيئة اشتراك IPTV",
                    style = MaterialTheme.typography.titleLarge,
                    color = SnowWhite,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Type Selector Tabs
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedType == SubscriptionType.XTREAM_CODES,
                            onClick = { selectedType = SubscriptionType.XTREAM_CODES },
                            label = { Text("Xtream API") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CalmGold,
                                selectedLabelColor = NightNavy,
                                containerColor = DarkSportsBlue,
                                labelColor = SnowWhite
                            )
                        )
                        FilterChip(
                            selected = selectedType == SubscriptionType.M3U_PLAYLIST,
                            onClick = { selectedType = SubscriptionType.M3U_PLAYLIST },
                            label = { Text("M3U رابط") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CalmGold,
                                selectedLabelColor = NightNavy,
                                containerColor = DarkSportsBlue,
                                labelColor = SnowWhite
                            )
                        )

                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (selectedType == SubscriptionType.XTREAM_CODES) {
                        OutlinedTextField(
                            value = serverInput,
                            onValueChange = { serverInput = it },
                            label = { Text("عنوان السيرفر (http://host:port)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("xtream_server_input")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = userInput,
                            onValueChange = { userInput = it },
                            label = { Text("اسم المستخدم (Username)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("xtream_user_input")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = passInput,
                            onValueChange = { passInput = it },
                            label = { Text("كلمة المرور (Password)") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("xtream_pass_input")
                        )
                    } else if (selectedType == SubscriptionType.M3U_PLAYLIST) {
                        OutlinedTextField(
                            value = m3uInput,
                            onValueChange = { m3uInput = it },
                            label = { Text("رابط ملف M3U / M3U8") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("m3u_url_input")
                        )
                    }

                    if (localError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = localError!!, color = LiveRed, style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSaving = true
                        localError = null
                        coroutineScope.launch {
                            val res = onSaveSubscription(selectedType, serverInput, userInput, passInput, m3uInput)
                            isSaving = false
                            if (res.isSuccess) {
                                showEditDialog = false
                            } else {
                                localError = res.exceptionOrNull()?.localizedMessage ?: "حدث خطأ أثناء الاتصال بالسيرفر"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CalmGold),
                    enabled = !isSaving,
                    modifier = Modifier.testTag("confirm_subscription_btn")
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = NightNavy)
                    } else {
                        Text("حفظ والاتصال", color = NightNavy, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEditDialog = false },
                    enabled = !isSaving
                ) {
                    Text("إلغاء", color = SnowWhite)
                }
            },
            containerColor = CardBackground
        )
    }
}
