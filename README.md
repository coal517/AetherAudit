# AetherAudit: Passive Wireless Infrastructure Scout

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![Android SDK](https://img.shields.io/badge/SDK-26%2B-green.svg)](https://developer.android.com/)
[![SDG Alignment](https://img.shields.io/badge/SDG-Goal%209%20%7C%20Target%209.1-cyan.svg)](https://sdgs.un.org/goals)

AetherAudit is a sophisticated, offline-first passive wireless auditing tool built natively for Android using **Kotlin** and **Jetpack Compose**. The application acts as a digital perimeter blue-team defender, passively monitoring, mapping, and logging unpatched legacy Internet of Things (IoT) hardware and Bluetooth peripherals prone to high-severity protocol exploitation (specifically targeting vulnerability signatures of the unauthenticated Airoha RACE debugging protocol).

Aligning with **UN SDG Goal 9: Industry, Innovation, and Infrastructure (Target 9.1: Sustainable & Resilient Infrastructure)**, AetherAudit provides site IT administrators, facility managers, and digital defense officers with physical-perimeter visibility to actively safeguard corporate networks, administrative facilities, and campuses against proximal over-the-air hijacking risks.

---

## 🛡️ Cybersecurity Context: The RACE Protocol Vulnerability

Traditional network monitoring suites are completely blind to physical-perimeter wireless exposure. Bluetooth and Bluetooth Low Energy (BLE) systems-on-a-chip (SoCs) are highly privileged on host operating systems, making unpatched system-level firmware vulnerability a catastrophic entry point for enterprise intrusion.

AetherAudit specifically audits against unauthenticated execution vulnerabilities in the **Airoha RACE (Robust Audio Connection Engine)** protocol, a proprietary debug framework natively embedded within millions of Bluetooth devices manufactured by industry leaders (including unpatched legacy models from Sony, JBL, Bose, Marshall, and Jabra).

### Exploit Mechanics Checked (The Attack Vector Chain)
1. **Unauthenticated Connection (CVE-2025-20700 & CVE-2025-20701):** Affected chipsets accept GATT (BLE) and RFCOMM (Classic) connections from any nearby host without enforcing standard cryptographic pairing or user interaction.
2. **Exposure of Custom Protocol (CVE-2025-20702):** Once connected, the unauthenticated host has full access to the Airoha RACE debug utility.
3. **Firmware and Memory Dump:** Attackers use RACE commands (`STORAGE_PAGE_READ` 0x0403 and `READ_ADDRESS` 0x1680) to dump volatile RAM contents and non-volatile flash partitions (such as the NVDM config block).
4. **Link Key Hijacking & Identity Spoofing:** The flash partition contains the device's peer Bluetooth link keys. By exfiltrating these paired keys, an attacker can impersonate the trusted headphones directly to the victim’s paired phone, bypassing screen locks via the Hands-Free Profile (HFP) to intercept calls, trigger voice assistants, download contacts, or establish silent microphone audio streams.

### AetherAudit's Defensive Design (Respecting the Android Sandbox)
Standard Android operating systems prohibit third-party user applications from transmitting malformed packets or raw Host Controller Interface (HCI) commands. Intercepting or executing low-level over-the-air exploits directly in Kotlin is physically restricted by the Android system's security architecture.

AetherAudit maps this threat landscape safely and passively:
* **Manufacturer OUI Lookup:** The background engine passively scans public BLE advertisement packets. It extracts the first three bytes of the hardware address—the **Organizationally Unique Identifier (OUI)**—and cross-references them with the synchronized blacklist to identify vulnerable chipset vendors.
* **Asynchronous RSSI Proximity Analysis:** Using real-time signal strength (RSSI), the app maps proximity. If an unpatched OUI device is detected within the immediate physical threat radius (RSSI > -65 dBm), the threat level escalates to **CRITICAL**, advising security teams of active spatial exposure.

---

## 🛠️ System Architecture & Data Flow

AetherAudit utilizes a clean, modern Android architecture adhering strictly to **MVVM (Model-View-ViewModel)** guidelines with unidirectional data flow:

```
[Physical Peripherals] 
      │ (BLE Advertisement Packets)
      ▼
[BleSecurityScanner] ──(Kotlin Flow Flow)──► [AetherAuditViewModel] ──(UiState)──► [Jetpack Compose UI]
                                                   │
                        ┌──────────────────────────┴──────────────────────────┐
                        ▼ (Room SQLite - Local)                               ▼ (Supabase JSON API - Remote)
                 [AetherAuditDatabase]                                  [Supabase Security Hub]
```

### 1. Unified Operator Authentication Gate
The application enforces authentication using **Supabase GoTrue REST** endpoints. System logs cannot be transmitted to the remote database unless an operator holds verified credentials. To prevent client-side credential extraction, communication is lightweight and executes directly via optimized **OkHttp** calls, bypassing heavy SDK compilation dependencies.

### 2. Double-Layer Sync Protocol
* **Remote Pull (Vulnerability Blacklist):** Downloads master security signatures from Supabase PostgreSQL to the local SQLite database. Uses a flag (`isUserDefined = 0`) to distinguish remote definitions, preserving local user-defined manual overrides.
* **Remote Push (Resilience Logs):** Asynchronously publishes physical perimeter scan records to the cloud. If network connectivity is lost, the scan persists offline within the local Room DB until a reconnection is established.

---

## 🚀 Installation & Build Requirements

* **Operating System Support:** Android 8.0+ (API Level 26) up to Android 15.0 (API Level 35).
* **Hardware Requirements:** Must be executed on a physical Android device. *Emulators do not natively support BLE scanning controllers.*
* **Required Build Tools:**
  * Android Studio Ladybug (or newer)
  * Kotlin Gradle DSL with Version Catalog configuration (`libs.versions.toml`)
  * Kotlin Symbol Processing (KSP) for Room compiler optimization
  * JDK 17

---

## 📝 License

Distributed under the **MIT License**. See `LICENSE` for more information.

```
Copyright (c) 2026 AetherAudit Dev Team

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
