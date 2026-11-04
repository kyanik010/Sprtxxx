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

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(NightNavy)) {
        val landscapeCompact = maxWidth > maxHeight
        val compact = landscapeCompact || maxHeight < 600.dp
        val scrollState = rememberScrollState()
        Box(
            modifier = Modifier.fillMaxSize()
                .then(if (landscapeCompact) Modifier else Modifier.verticalScroll(scrollState))
                .padding(horizontal = if (compact) 10.dp else 20.dp, vertical = if (compact) 6.dp else 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(if (compact) 16.dp else 22.dp),
                color = CardBackground,
                border = BorderStroke(1.dp, CalmGold.copy(alpha = 0.55f)),
                shadowElevation = 12.dp,
                modifier = Modifier.fillMaxWidth().widthIn(max = if (landscapeCompact) 520.dp else 440.dp)
            ) {
                Column(
                    modifier = Modifier.padding(if (landscapeCompact) 10.dp else if (compact) 14.dp else 22.dp),
                    verticalArrangement = Arrangement.spacedBy(if (landscapeCompact) 5.dp else if (compact) 8.dp else 12.dp)
                ) {
                    if (landscapeCompact) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(shape = RoundedCornerShape(10.dp), color = CalmGold.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, CalmGold.copy(alpha = 0.5f))) {
                                Icon(Icons.Default.Tv, null, tint = CalmGold, modifier = Modifier.padding(7.dp).size(25.dp))
                            }
                            Column {
                                Text("مرحباً بك في SONIX TV", color = SnowWhite, fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.titleMedium)
                                Text("أدخل بيانات اشتراك IPTV للبدء", color = TextMuted, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Surface(shape = RoundedCornerShape(14.dp), color = CalmGold.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, CalmGold.copy(alpha = 0.55f))) {
                                Icon(Icons.Default.Tv, null, tint = CalmGold,
                                    modifier = Modifier.padding(10.dp).size(if (compact) 30.dp else 38.dp))
                            }
                            Text("مرحباً بك في SONIX TV",
                                style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                                color = SnowWhite, fontWeight = FontWeight.ExtraBold)
                            Text("أضف اشتراك IPTV الخاص بك للبدء", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        FilterChip(selected = selectedType == SubscriptionType.XTREAM_CODES,
                            onClick = { selectedType = SubscriptionType.XTREAM_CODES; error = null },
                            label = { Text("Xtream Codes", maxLines = 1) },
                            leadingIcon = { Icon(Icons.Default.Tv, null, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CalmGold, selectedLabelColor = NightNavy,
                                containerColor = DarkSportsBlue, labelColor = SnowWhite))
                        FilterChip(selected = selectedType == SubscriptionType.M3U_PLAYLIST,
                            onClick = { selectedType = SubscriptionType.M3U_PLAYLIST; error = null },
                            label = { Text("رابط M3U", maxLines = 1) },
                            leadingIcon = { Icon(Icons.Default.Link, null, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CalmGold, selectedLabelColor = NightNavy,
                                containerColor = DarkSportsBlue, labelColor = SnowWhite))
                    }
                    if (selectedType == SubscriptionType.XTREAM_CODES) {
                        if (landscapeCompact) {
                            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(server, { server = it }, label = { Text("عنوان السيرفر") },
                                    placeholder = { Text("http://host:port") }, singleLine = true,
                                    modifier = Modifier.weight(1.25f), textStyle = MaterialTheme.typography.bodySmall)
                                OutlinedTextField(username, { username = it }, label = { Text("اسم المستخدم") },
                                    singleLine = true, modifier = Modifier.weight(1f), textStyle = MaterialTheme.typography.bodySmall)
                                OutlinedTextField(password, { password = it }, label = { Text("كلمة المرور") },
                                    singleLine = true, visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.weight(1f), textStyle = MaterialTheme.typography.bodySmall)
                            }
                        } else {
                            OutlinedTextField(server, { server = it }, label = { Text("عنوان السيرفر") },
                                placeholder = { Text("http://host:port") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(username, { username = it }, label = { Text("اسم المستخدم") },
                                singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(password, { password = it }, label = { Text("كلمة المرور") },
                                singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                        }
                    } else {
                        OutlinedTextField(playlist, { playlist = it }, label = { Text("رابط قائمة M3U / M3U8") },
                            placeholder = { Text("https://...") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                    error?.let { Text(it, color = LiveRed, style = MaterialTheme.typography.labelSmall, maxLines = 2) }
                    Button(onClick = {
                        busy = true
                        error = null
                        scope.launch {
                            val result = onConnect(selectedType, server, username, password, playlist)
                            busy = false
                            if (result.isFailure) error = result.exceptionOrNull()?.localizedMessage ?: "تعذر الاتصال أو تحميل قائمة القنوات"
                        }
                    }, enabled = !busy, modifier = Modifier.fillMaxWidth().height(if (landscapeCompact) 40.dp else if (compact) 46.dp else 50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CalmGold), shape = RoundedCornerShape(11.dp)) {
                        if (busy) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(Modifier.size(17.dp), color = NightNavy, strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("جارٍ الاتصال وتحميل القنوات...", color = NightNavy, fontWeight = FontWeight.Bold)
                            }
                        } else Text("دخول", color = NightNavy, fontWeight = FontWeight.Bold)
                    }
                    if (!landscapeCompact) Text("لن تظهر قنوات تجريبية. سيعرض التطبيق قنوات اشتراكك فقط.",
                        style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
            }
        }
    }
}
