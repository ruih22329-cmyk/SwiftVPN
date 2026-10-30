package com.swiftvpn.app

import android.net.VpnService
import android.os.Bundle
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    private var statusText by mutableStateOf("尚未连接")
    private var detailText by mutableStateOf("点击下方按钮申请 Android VPN 系统授权。")
    private var showSettings by mutableStateOf(false)
    private var configSaved by mutableStateOf(false)
    private var settingsMessage by mutableStateOf("导入服务商或自建服务器提供的 WireGuard .conf 配置文件。")

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
                    if (validationError != null) {
                        throw IllegalArgumentException(validationError)
                    }
                    ConfigVault.save(this, config)
                    configSaved = true
                    settingsMessage = "配置已安全保存到本机。当前版本已完成配置导入，隧道连接功能仍在开发中。"
                } catch (error: Exception) {
                    settingsMessage = error.message ?: "导入失败，请检查配置文件后重试。"
                }
            }
        }

    private val vpnPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                statusText = "系统授权已通过"
                detailText = currentHomeDetail()
            } else {
                statusText = "尚未获得授权"
                detailText = "你可以稍后再次点击按钮，并在系统弹窗中允许 SwiftVPN 创建 VPN 连接。"
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configSaved = ConfigVault.hasConfig(this)
        setContent {
            MaterialTheme {
                if (showSettings) {
                    SwiftVpnSettings(
                        configSaved = configSaved,
                        message = settingsMessage,
                        onBack = { showSettings = false },
                        onImport = { configPicker.launch(arrayOf("text/plain", "application/octet-stream", "*/*")) },
                        onClear = {
                            ConfigVault.clear(this)
                            configSaved = false
                            settingsMessage = "本机保存的 WireGuard 配置已清除。"
                        }
                    )
                } else {
                    SwiftVpnHome(
                        status = statusText,
                        detail = currentHomeDetail(),
                        onRequestPermission = ::requestVpnPermission,
                        onOpenSettings = { showSettings = true }
                    )
                }
            }
        }
    }

    private fun currentHomeDetail(): String =
        if (configSaved) {
            "已导入 WireGuard 配置并加密保存在本机。当前版本尚未启动隧道，因此还没有建立 VPN 连接。"
        } else {
            "已获得 VPN 使用授权。请先在设置中导入 WireGuard 配置文件；隧道连接功能仍在开发中。"
        }

    private fun requestVpnPermission() {
        val intent = VpnService.prepare(this)
        if (intent == null) {
            statusText = "系统授权已通过"
            detailText = currentHomeDetail()
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
    val hasInterface = lines.any { it.equals("[Interface]", ignoreCase = true) }
    val hasPeer = lines.any { it.equals("[Peer]", ignoreCase = true) }
    if (!hasInterface || !hasPeer) {
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
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onOpenSettings) {
                    Text("⚙ 设置")
                }
            }
            Text(
                text = "SwiftVPN",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "简单、安全地连接自己的 VPN",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(32.dp))
            Card(
                modifier = Modifier.widthIn(max = 420.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("当前状态", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = status,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(detail)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRequestPermission) {
                Text("申请 VPN 系统授权")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "版本 0.1.2 · 中文开发预览版",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun SwiftVpnSettings(
    configSaved: Boolean,
    message: String,
    onBack: () -> Unit,
    onImport: () -> Unit,
    onClear: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                TextButton(onClick = onBack) { Text("← 返回") }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "VPN 设置",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("WireGuard 配置", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        if (configSaved) "配置状态：已导入并加密保存" else "配置状态：尚未导入"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(message)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onImport,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (configSaved) "重新导入 .conf 配置" else "导入 WireGuard .conf 配置")
            }
            if (configSaved) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onClear,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("删除本机配置")
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                "配置文件应由你的 VPN 服务商或你自己的 WireGuard 服务器生成。请勿把配置文件或私钥发送给他人。",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "提示：当前版本支持导入和加密保存配置，真实隧道连接功能仍在开发中。",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
