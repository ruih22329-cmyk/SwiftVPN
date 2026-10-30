package com.swiftvpn.app

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    private var statusText by mutableStateOf("尚未连接")
    private var detailText by mutableStateOf("点击下方按钮申请 Android VPN 系统授权。")

    private val vpnPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                statusText = "系统授权已通过"
                detailText = "已获得 VPN 使用授权。WireGuard 隧道和服务器配置尚未接入，因此目前还没有建立 VPN 连接。"
            } else {
                statusText = "尚未获得授权"
                detailText = "你可以稍后再次点击按钮，并在系统弹窗中允许 SwiftVPN 创建 VPN 连接。"
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                SwiftVpnHome(
                    status = statusText,
                    detail = detailText,
                    onRequestPermission = ::requestVpnPermission
                )
            }
        }
    }

    private fun requestVpnPermission() {
        val intent = VpnService.prepare(this)
        if (intent == null) {
            statusText = "系统授权已通过"
            detailText = "已获得 VPN 使用授权。WireGuard 隧道和服务器配置尚未接入，因此目前还没有建立 VPN 连接。"
        } else {
            vpnPermissionLauncher.launch(intent)
        }
    }
}

@Composable
private fun SwiftVpnHome(
    status: String,
    detail: String,
    onRequestPermission: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
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
                text = "版本 0.1.1 · 中文开发预览版",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
