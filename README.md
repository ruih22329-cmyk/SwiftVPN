# SwiftVPN

SwiftVPN 是一个面向个人自建服务器的 Android VPN 客户端项目，默认使用**简体中文**。

> **当前状态：** Android 中文界面骨架和项目配置已开始搭建。VPN 隧道、配置接口和 Linux 网关仍在开发中，当前版本不能提供真实 VPN 连接。

## 当前功能

- Kotlin + Jetpack Compose Android 项目骨架
- 简体中文首页和连接状态展示
- Android 最低版本：Android 8.0（API 26）
- 应用版本：0.1.0（开发预览）

## 计划功能

- 使用 Android `VpnService` 管理 VPN 生命周期和系统授权
- 集成经过维护的 WireGuard 实现
- 通过 HTTPS 配置接口进行设备注册和隧道配置下发
- Linux WireGuard 网关、转发、防火墙和 NAT
- 连接状态、自动重连、设备撤销和密钥轮换

## 项目目录

- `app/` — Android 应用
- `server/` — HTTPS 配置服务（计划中）
- `gateway/` — Linux WireGuard 网关部署（计划中）
- `docs/` — 架构和运维文档

## 安全说明

不要将私钥、服务器密码、API Token 或生产环境 WireGuard 配置提交到仓库。每台设备应使用独立密钥，并通过 HTTPS 获取配置。

架构说明见 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)。
