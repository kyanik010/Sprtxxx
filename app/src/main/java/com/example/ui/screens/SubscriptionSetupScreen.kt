package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.model.SubscriptionType
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SubscriptionSetupScreen(
    onConnect: suspend (SubscriptionType, String, String, String, String) -> Result<Int>,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var selectedType by remember { mutableStateOf(SubscriptionType.XTREAM_CODES) }
    var server by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var playlist by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize().background(NightNavy)
    ) {
        val compact = maxHeight < 600.dp
        val cardPadding = if (compact) 16.dp else 24.dp
        val sectionSpacing = if (compact) 9.dp else 14.dp
        val scrollState = rememberScrollState()

        // The card stays centered and compact when it fits. On short screens or
        // when the keyboard is open, the whole form can be scrolled instead of
        // being clipped at the bottom.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = maxHeight)
                .verticalScroll(scrollState)
                .imePadding()
                .padding(horizontal = if (compact) 12.dp else 24.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = CardBackground,
                border = BorderStroke(1.dp, CalmGold.copy(alpha = 0.55f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
            ) {
                Column(
                    modifier = Modifier.padding(cardPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(sectionSpacing)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = CalmGold.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, CalmGold.copy(alpha = 0.55f))
                    ) {
                        Icon(
                            Icons.Default.Tv,
                            contentDescription = null,
                            tint = CalmGold,
                            modifier = Modifier.padding(if (compact) 10.dp else 15.dp)
                                .size(if (compact) 30.dp else 38.dp)
                        )
                    }

                    Text(
                        "مرحباً بك في ماتش نايت",
                        style = if (compact) MaterialTheme.typography.titleLarge
                            else MaterialTheme.typography.headlineMedium,
                        color = SnowWhite,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        "أضف اشتراك IPTV الخاص بك للبدء",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = selectedType == SubscriptionType.XTREAM_CODES,
                            onClick = { selectedType = SubscriptionType.XTREAM_CODES; error = null },
                            label = { Text("Xtream Codes", maxLines = 1) },
                            leadingIcon = { Icon(Icons.Default.Tv, contentDescription = null) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CalmGold,
                                selectedLabelColor = NightNavy,
                                containerColor = DarkSportsBlue,
                                labelColor = SnowWhite
                            )
                        )
                        FilterChip(
                            selected = selectedType == SubscriptionType.M3U_PLAYLIST,
                            onClick = { selectedType = SubscriptionType.M3U_PLAYLIST; error = null },
                            label = { Text("رابط M3U", maxLines = 1) },
                            leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CalmGold,
                                selectedLabelColor = NightNavy,
                                containerColor = DarkSportsBlue,
                                labelColor = SnowWhite
                            )
                        )
                    }

                    if (selectedType == SubscriptionType.XTREAM_CODES) {
                        OutlinedTextField(
                            value = server,
                            onValueChange = { server = it },
                            label = { Text("عنوان السيرفر") },
                            placeholder = { Text("http://server:port") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("اسم المستخدم") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("كلمة المرور") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        OutlinedTextField(
                            value = playlist,
                            onValueChange = { playlist = it },
                            label = { Text("رابط قائمة M3U / M3U8") },
                            placeholder = { Text("https://...") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    error?.let {
                        Text(it, color = LiveRed, style = MaterialTheme.typography.bodySmall)
                    }

                    Button(
                        onClick = {
                            busy = true
                            error = null
                            scope.launch {
                                val result = onConnect(selectedType, server, username, password, playlist)
                                busy = false
                                if (result.isFailure) {
                                    error = result.exceptionOrNull()?.localizedMessage
                                        ?: "تعذر الاتصال أو تحميل قائمة القنوات"
                                }
                            }
                        },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().height(if (compact) 48.dp else 52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CalmGold),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (busy) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = NightNavy,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    "جارٍ الاتصال وتحميل القنوات...",
                                    color = NightNavy,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Text("دخول", color = NightNavy, fontWeight = FontWeight.Bold)
                        }
                    }

                    Text(
                        "لن تظهر قنوات تجريبية. سيعرض التطبيق القنوات التي يوفرها اشتراكك فقط.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }
        }
    }
}
