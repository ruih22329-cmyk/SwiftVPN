# SwiftVPN Architecture

SwiftVPN is a personal Android VPN client with a self-hosted Linux gateway.

## Components

- **Android app:** Kotlin, Jetpack Compose, Android `VpnService`.
- **Tunnel:** WireGuard userspace/kernel integration. The app must use a maintained WireGuard implementation rather than implementing cryptography itself.
- **Provisioning API:** HTTPS API that authenticates a user/device and returns a short-lived, device-specific tunnel configuration.
- **Gateway:** Linux server running WireGuard, IP forwarding, firewall rules, and NAT.

## Initial development milestones

1. Create the Android project and a basic Compose connection screen.
2. Add VPN permission flow through Android `VpnService`.
3. Integrate the maintained WireGuard Android tunnel library.
4. Define provisioning API contracts and device enrollment.
5. Configure a Linux WireGuard gateway with least-privilege firewall rules.
6. Add connection state, error reporting, reconnect behavior, and tests.
7. Document deployment, key rotation, and recovery.

## Security requirements

- Generate a unique key pair per device; keep private keys on the device.
- Never commit private keys, server credentials, API tokens, or production configs.
- Authenticate provisioning requests over HTTPS.
- Do not log tunnel private keys or sensitive user traffic.
- Use explicit user consent for VPN activation and clearly show connection state.
- Provide a way to revoke a device and rotate its credentials.

## Repository layout (planned)

```text
android/       Android application
server/        Provisioning API
gateway/       Linux WireGuard and firewall deployment templates
docs/          Architecture, setup, and operations guides
```

This document is a development plan; it does not claim that the app, API, or gateway has been implemented yet.
