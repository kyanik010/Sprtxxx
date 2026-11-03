package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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

    Column(
        modifier = modifier.fillMaxSize().background(NightNavy).padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = CardBackground,
            border = BorderStroke(1.dp, CalmGold.copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(Icons.Default.Tv, contentDescription = null, tint = CalmGold, modifier = Modifier.size(42.dp))
                Text("مرحباً بك", style = MaterialTheme.typography.headlineMedium, color = SnowWhite, fontWeight = FontWeight.ExtraBold)
                Text(
                    "أضف اشتراك IPTV الخاص بك للبدء",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextMuted
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = selectedType == SubscriptionType.XTREAM_CODES,
                        onClick = { selectedType = SubscriptionType.XTREAM_CODES; error = null },
                        label = { Text("Xtream Codes") },
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
                        label = { Text("رابط M3U") },
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
                        value = server, onValueChange = { server = it },
                        label = { Text("عنوان السيرفر") },
                        placeholder = { Text("http://server:port") },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = username, onValueChange = { username = it },
                        label = { Text("اسم المستخدم") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = password, onValueChange = { password = it },
                        label = { Text("كلمة المرور") }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = playlist, onValueChange = { playlist = it },
                        label = { Text("رابط قائمة M3U / M3U8") },
                        placeholder = { Text("https://...") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                }
                if (error != null) {
                    Text(error!!, color = LiveRed, style = MaterialTheme.typography.bodySmall)
                }
                Button(
                    onClick = {
                        busy = true
                        error = null
                        scope.launch {
                            val result = onConnect(selectedType, server, username, password, playlist)
                            busy = false
                            if (result.isSuccess) {
                                // The host screen switches to the player interface after a successful import.
                            } else {
                                error = result.exceptionOrNull()?.localizedMessage ?: "تعذر الاتصال أو تحميل قائمة القنوات"
                            }
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CalmGold),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (busy) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = NightNavy)
                    else Text("اتصال وتحميل القنوات", color = NightNavy, fontWeight = FontWeight.Bold)
                }
                Text(
                    "لن تظهر قنوات أو مباريات تجريبية. سيعرض التطبيق المحتوى الذي يوفره اشتراكك فقط.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }
    }
}
