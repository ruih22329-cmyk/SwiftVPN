package com.swiftvpn.app

import android.net.VpnService

/**
 * SwiftVPN 的 Android VPN 服务入口。
 *
 * 后续由 WireGuard GoBackend 接管隧道建立、数据转发和断开。
 * 当前阶段只声明系统服务，不建立空 TUN，以免误导用户认为已连接。
 */
class SwiftVpnService : VpnService()
