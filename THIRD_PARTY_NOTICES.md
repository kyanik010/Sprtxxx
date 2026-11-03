# Third-party notices

## Client for OpenVPN Gate

The VPN server directory, VPN controller, and server-selection integration are based on the open-source project pinned at `vendor/OpenVPNGateClientApp`:

- Source: https://github.com/zabotinegor/OpenVPNGateClientApp
- License: GPL-2.0-only; see `vendor/OpenVPNGateClientApp/LICENSE`.

## OpenVPN engine

The OpenVPN engine is included as a nested Git submodule at `vendor/OpenVPNGateClientApp/src/external/OpenVPNEngine` and contains OpenVPN/related native components with their own license notices. Review `vendor/OpenVPNGateClientApp/src/external/OpenVPNEngine/main/doc/LICENSE.txt` and the notices within that source tree before redistributing builds.

## Distribution reminder

If distributing an APK that incorporates these components, preserve the applicable license notices and meet the corresponding source-code and license obligations. The VPN Gate relay network is volunteer-operated; server availability, performance, and privacy practices can vary by operator.
