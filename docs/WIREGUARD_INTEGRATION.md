# WireGuard 客户端集成说明

SwiftVPN 使用 WireGuard 官方 Android Tunnel Library，而不是自行实现加密协议。

## 当前集成状态

- 已在 Android Gradle 模块中声明官方依赖：com.wireguard.android:tunnel:1.0.20260102
- 已启用 Java 语言库 desugaring
- Android VpnService 入口已声明
- 尚未完成 GoBackend 生命周期、隧道配置解析、连接/断开状态同步
- 尚未进行 Android 真机编译和连接测试

## 为什么暂时不能连接

WireGuard 客户端需要一份有效的隧道配置，至少包含本机私钥、接口地址、DNS，以及服务端公钥和 Endpoint。没有服务端和有效配置时，不应伪造“已连接”状态。

## 下一步

1. 实现安全的配置导入（用户通过系统文件选择器选择 .conf 文件）。
2. 使用官方库解析配置并验证必要字段。
3. 实现隧道启动、停止和状态观察。
4. 对私钥采用 Android Keystore 支持的安全存储方案；避免写入日志或仓库。
5. 在真实 Android 设备上验证授权、连接、断开和异常恢复。

## 官方参考

- https://git.zx2c4.com/wireguard-android/about/
- https://www.wireguard.com/embedding/
- Maven Central artifact: com.wireguard.android:tunnel
