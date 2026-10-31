package com.swiftvpn.app

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private const val PREFS_NAME = "swiftvpn_settings"

private data class VpnSettings(
    val dnsMode: String = "系统默认",
    val customDns: String = "",
    val mtu: Int = 1420,
    val autoConnect: Boolean = false,
    val bypassLan: Boolean = false
)

class MainActivity : ComponentActivity() {
    private var statusText by mutableStateOf("尚未连接")
    private var showSettings by mutableStateOf(false)
    private var configSaved by mutableStateOf(false)
    private var settingsMessage by mutableStateOf("导入服务商或自建服务器提供的 WireGuard .conf 配置文件。")
    private var vpnSettings by mutableStateOf(VpnSettings())

    private val configPicker =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                try {
                    val config = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: throw IllegalArgumentException("无法读取所选文件")
                    if (config.length > 256 * 1024) {
                        throw IllegalArgumentException("配置文件过大，请选择小于 256 KB 的文件")
                    }
                    val validationError = validateWireGuardConfig(config)
                    if (validationError != null) throw IllegalArgumentException(validationError)
                    ConfigVault.save(this, config)
                    configSaved = true
                    settingsMessage = "配置已加密保存在本机。当前版本尚未启动隧道。"
                } catch (error: Exception) {
                    settingsMessage = error.message ?: "导入失败，请检查配置文件后重试。"
                }
            }
        }

    private val vpnPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            statusText = if (result.resultCode == RESULT_OK) "系统授权已通过" else "尚未获得授权"
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configSaved = ConfigVault.hasConfig(this)
        vpnSettings = loadSettings()
        setContent {
            MaterialTheme {
                if (showSettings) {
                    SwiftVpnSettings(
                        configSaved = configSaved,
                        message = settingsMessage,
                        settings = vpnSettings,
                        onBack = { showSettings = false },
                        onImport = { configPicker.launch(arrayOf("text/plain", "application/octet-stream", "*/*")) },
                        onClear = {
                            ConfigVault.clear(this)
                            configSaved = false
                            settingsMessage = "本机保存的 WireGuard 配置已清除。"
                        },
                        onSettingsChange = ::saveSettings,
                        onOpenAndroidVpnSettings = {
                            runCatching { startActivity(Intent(Settings.ACTION_VPN_SETTINGS)) }
                        }
                    )
                } else {
                    SwiftVpnHome(
                        status = if (configSaved) "配置已准备好" else statusText,
                        detail = if (configSaved) {
                            "WireGuard 配置已加密保存在本机。真实隧道连接功能仍在开发中。"
                        } else {
                            "请在设置中导入 WireGuard 配置文件。系统授权不代表 VPN 已连接。"
                        },
                        onRequestPermission = ::requestVpnPermission,
                        onOpenSettings = { showSettings = true }
                    )
                }
            }
        }
    }

    private fun loadSettings(): VpnSettings {
        val p = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        return VpnSettings(
            dnsMode = p.getString("dnsMode", "系统默认") ?: "系统默认",
            customDns = p.getString("customDns", "") ?: "",
            mtu = p.getInt("mtu", 1420),
            autoConnect = p.getBoolean("autoConnect", false),
            bypassLan = p.getBoolean("bypassLan", false)
        )
    }

    private fun saveSettings(value: VpnSettings) {
        vpnSettings = value
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
            .putString("dnsMode", value.dnsMode)
            .putString("customDns", value.customDns)
            .putInt("mtu", value.mtu)
            .putBoolean("autoConnect", value.autoConnect)
            .putBoolean("bypassLan", value.bypassLan)
            .apply()
    }

    private fun requestVpnPermission() {
        val intent = VpnService.prepare(this)
        if (intent == null) {
            statusText = "系统授权已通过"
        } else {
            vpnPermissionLauncher.launch(intent)
        }
    }
}

private fun validateWireGuardConfig(config: String): String? {
    val lines = config.lineSequence()
        .map { it.substringBefore("#").substringBefore(";").trim() }
        .filter { it.isNotEmpty() }
        .toList()
    if (lines.none { it.equals("[Interface]", ignoreCase = true) } ||
        lines.none { it.equals("[Peer]", ignoreCase = true) }) {
        return "配置格式不正确：必须包含 [Interface] 和 [Peer] 两个部分。"
    }
    fun hasValue(key: String): Boolean = lines.any {
        it.substringBefore("=").trim().equals(key, ignoreCase = true) &&
            it.contains("=") && it.substringAfter("=").trim().isNotEmpty()
    }
    if (!hasValue("PrivateKey")) return "配置缺少 Interface PrivateKey。"
    if (!hasValue("PublicKey")) return "配置缺少 Peer PublicKey。"
    if (!hasValue("Endpoint")) return "配置缺少 Peer Endpoint。"
    return null
}

@Composable
private fun SwiftVpnHome(
    status: String,
    detail: String,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onOpenSettings) { Text("⚙ 设置") }
            }
            Text("SwiftVPN", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Text("简单、安全地连接自己的 VPN", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(32.dp))
            Card(
                modifier = Modifier.widthIn(max = 420.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("当前状态", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(status, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(detail)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRequestPermission) { Text("申请 VPN 系统授权") }
            Spacer(modifier = Modifier.height(16.dp))
            Text("版本 0.2.0 · 中文开发预览版", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SwiftVpnSettings(
    configSaved: Boolean,
    message: String,
    settings: VpnSettings,
    onBack: () -> Unit,
    onImport: () -> Unit,
    onClear: () -> Unit,
    onSettingsChange: (VpnSettings) -> Unit,
    onOpenAndroidVpnSettings: () -> Unit
) {
    val scroll = rememberScrollState()
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(scroll).padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            TextButton(onClick = onBack) { Text("← 返回首页") }
            Text("设置", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("管理服务器配置、网络参数和连接偏好", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(18.dp))

            SettingsSection("服务器配置") {
                Text(if (configSaved) "配置状态：已导入并加密保存" else "配置状态：尚未导入")
                Spacer(modifier = Modifier.height(6.dp))
                Text(message, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onImport, modifier = Modifier.fillMaxWidth()) {
                    Text(if (configSaved) "更换 WireGuard 配置文件" else "导入 WireGuard .conf 文件")
                }
                if (configSaved) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = onClear, modifier = Modifier.fillMaxWidth()) {
                        Text("删除本机配置")
                    }
                }
                Text("配置文件包含私钥，请只导入可信服务商或自建服务器生成的文件。", style = MaterialTheme.typography.bodySmall)
            }

            SettingsSection("DNS 设置") {
                Text("选择 VPN 隧道使用的 DNS")
                listOf("系统默认", "Cloudflare（1.1.1.1）", "Google（8.8.8.8）", "自定义").forEach { option ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = settings.dnsMode == option,
                            onClick = { onSettingsChange(settings.copy(dnsMode = option)) }
                        )
                        Text(option)
                    }
                }
                if (settings.dnsMode == "自定义") {
                    OutlinedTextField(
                        value = settings.customDns,
                        onValueChange = { onSettingsChange(settings.copy(customDns = it)) },
                        label = { Text("自定义 DNS 地址") },
                        placeholder = { Text("例如 9.9.9.9") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            SettingsSection("网络参数") {
                Text("MTU：${settings.mtu}")
                Slider(
                    value = settings.mtu.toFloat(),
                    onValueChange = { onSettingsChange(settings.copy(mtu = it.toInt())) },
                    valueRange = 1280f..1420f,
                    steps = 13
                )
                Text("较低 MTU 可能改善部分网络兼容性；默认值为 1420。", style = MaterialTheme.typography.bodySmall)
                SettingSwitch(
                    title = "允许访问本地网络",
                    subtitle = "启用后，隧道配置将保留对局域网设备的访问。",
                    checked = settings.bypassLan,
                    onCheckedChange = { onSettingsChange(settings.copy(bypassLan = it)) }
                )
            }

            SettingsSection("连接偏好") {
                SettingSwitch(
                    title = "启动应用时自动连接",
                    subtitle = "保存偏好；自动连接将在隧道引擎接入后启用。",
                    checked = settings.autoConnect,
                    onCheckedChange = { onSettingsChange(settings.copy(autoConnect = it)) }
                )
                OutlinedButton(onClick = onOpenAndroidVpnSettings, modifier = Modifier.fillMaxWidth()) {
                    Text("打开 Android 系统 VPN 设置")
                }
                Text("如需始终开启或断开时阻止网络，请在 Android 系统 VPN 设置中查看本应用选项（系统版本可能不同）。", style = MaterialTheme.typography.bodySmall)
            }

            SettingsSection("隐私与安全") {
                Text("• WireGuard 配置使用 Android Keystore 加密保存在应用私有目录。")
                Text("• 配置不会自动上传到 GitHub 或其他服务器。")
                Text("• 请勿向他人发送包含 PrivateKey 的配置文件。")
            }

            SettingsSection("关于 SwiftVPN") {
                Text("版本 0.2.0 · 中文开发预览版")
                Text("当前支持：配置文件导入、加密保存、网络偏好设置。")
                Text("当前尚未支持：实际 WireGuard 隧道连接、自动连接和网络参数应用。")
            }
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
