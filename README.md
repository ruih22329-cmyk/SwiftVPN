# SwiftVPN

SwiftVPN is a personal Android VPN client project designed to connect to a self-hosted Linux WireGuard gateway.

> **Status:** Early planning/scaffolding. The Android client, provisioning API, and gateway deployment are not implemented yet.

## Planned features

- Android connection UI built with Kotlin and Jetpack Compose
- VPN permission and lifecycle handling through Android `VpnService`
- WireGuard-based encrypted tunnel using a maintained implementation
- Device-specific configuration provisioning over HTTPS
- Linux gateway deployment with forwarding, firewall, and NAT
- Connection status, reconnect handling, and device revocation

## Repository structure

- `android/` — Android application (planned)
- `server/` — HTTPS provisioning API (planned)
- `gateway/` — Linux WireGuard deployment and firewall templates (planned)
- `docs/` — Architecture and operational documentation

## Development roadmap

1. Android project skeleton and Compose connection screen
2. VPN permission flow and service lifecycle
3. WireGuard tunnel integration
4. Provisioning API and device enrollment
5. Linux gateway deployment
6. Tests, security review, and setup documentation

## Security

Never commit private keys, production WireGuard configurations, API tokens, or server passwords. Each device should have its own key pair, and provisioning must use HTTPS.

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for the current architecture plan.
