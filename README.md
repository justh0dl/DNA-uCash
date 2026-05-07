# DNAuCash

DNAuCash is an open-source Android app for creating low-cost physical Bitcoin tokens using NFC-based NTAG424 DNA tags.

The project is designed to make small-to-medium Bitcoin transfers cheaper, simpler, and more practical in the real world by turning BTC into physical NFC bearer-style tokens that can be handed, gifted, traded, or swept later.

DNAuCash focuses on:

- low-cost physical Bitcoin
- offline-first operation
- controlled reveal mechanics
- NTAG424 protected storage
- practical security tradeoffs
- small UTXO usability
- simple in-person Bitcoin transfer

DNAuCash is best understood as a practical physical Bitcoin system with layered protections and explicit tradeoffs — not as a replacement for a hardware wallet or long-term institutional custody solution.

---

# Features

## Token Types

### Bearer Tokens

- possession of the tag is enough to reveal the key
- designed to behave like physical cash
- fast and simple to use

### Guarded Tokens

- require both the tag and a password
- secret is never stored on the tag
- derivation occurs locally on-device

### Stealth Tokens

- disguised public appearance
- protected metadata stored behind NTAG424 authentication
- designed to avoid obviously advertising itself as a Bitcoin token

---

# Forge Levels

DNAuCash supports configurable Forge Levels that add computational friction during reveal operations:

- Cast
- Forged
- Tempered
- Hardened

Forge Levels increase time/cost per access attempt and help make many low-value attacks economically unattractive.

They are not a substitute for strong secrets.

---

# Security Model

DNAuCash uses:

- NTAG424 authenticated operations
- protected encrypted payload storage
- UID-bound derivation paths
- controlled reveal state transitions
- local private-key validation
- encrypted protected payloads

The app is designed so protected tokens transition to revealed state before the private key is displayed.

DNAuCash does **NOT** guarantee:

- issuer honesty
- unique minting
- destruction of backups
- protection after reveal
- hardware-wallet-grade isolation

If a token comes from an untrusted source, the safest option is to reveal and sweep immediately.

---

# Offline Operation

DNAuCash is designed to function primarily offline.

Internet access is **not required** for:

- minting
- revealing
- authentication
- encryption/decryption
- key derivation
- Bitcoin private key validation
- address validation

The app only uses internet access for optional balance display features.

Private keys, guarded secrets, and decrypted payloads are processed locally on-device and are not sent to external servers during normal operation.

---

# Supported Hardware

Currently supported:

- NTAG424 DNA

Generic NFC tags such as NTAG213/215/216 are not sufficient for protected DNAuCash functionality.

---

# Requirements

- Android device with NFC support
- Android Studio
- NTAG424 DNA tags

---

# Building

Open the project in Android Studio and build normally:

```
./gradlew assembleDebug
```

APK output is typically located at:

```text
app/build/outputs/apk/
```

---

# Open Source

DNAuCash is open source by design.

Users should understand the difference between:

- genuine DNAuCash mint flows
- modified forks
- imitation tokens
- trusted vs untrusted issuers

Open source improves transparency and auditability, but users must still evaluate trust assumptions.

---

# Intended Use Cases

DNAuCash is intended for:

- small-to-medium Bitcoin transfers
- physical Bitcoin gifts
- in-person exchange
- experiments with physical BTC
- collectible or bearer-style Bitcoin instruments
- community mint projects

It is NOT intended to replace:

- hardware wallets
- multisig custody
- high-value cold storage

---

# Future Possibilities

NTAG424 DNA tags include SUN/SDM messaging and tag-server authentication features.

These may support future community mint systems with stronger anti-counterfeit verification and server-backed authenticity checks.

These features are optional and are not required for normal offline DNAuCash operation.

---

# License

MIT License

Use at your own risk. No warranty is provided.
