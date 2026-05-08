# DNA uCash

DNA uCash is an open-source Android app for creating low-cost physical Bitcoin tokens using NFC-based NTAG424 DNA tags.

The project is designed to make small-to-medium Bitcoin transfers cheaper, simpler, and more practical in the real world by turning BTC into physical NFC bearer-style tokens that can be handed, gifted, traded, or swept later.

DNA uCash focuses on:

- low-cost physical Bitcoin
- offline-first operation
- controlled reveal mechanics
- NTAG424 protected storage
- practical security tradeoffs
- small UTXO usability
- simple in-person Bitcoin transfer

DNA uCash is best understood as a practical physical Bitcoin system with layered protections and explicit tradeoffs — not as a replacement for a hardware wallet or long-term institutional custody solution.

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

DNA uCash supports configurable Forge Levels that add computational friction during reveal operations:

- Cast
- Forged
- Tempered
- Hardened

Forge Levels increase time/cost per access attempt and help make many low-value attacks economically unattractive.

They are not a substitute for strong secrets.

---

# Security Model

DNA uCash uses:

- NTAG424 authenticated operations
- protected encrypted payload storage
- UID-bound derivation paths
- controlled reveal state transitions
- local private-key validation
- encrypted protected payloads

The app is designed so protected tokens transition to revealed state before the private key is displayed.

DNA uCash does **NOT** guarantee:

- issuer honesty
- unique minting
- destruction of backups
- protection after reveal
- hardware-wallet-grade isolation

If a token comes from an untrusted source, the safest option is to reveal and sweep immediately.

---

# Offline Operation

DNA uCash is designed to function primarily offline.

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

Generic NFC tags such as NTAG213/215/216 are not sufficient for protected DNA uCash functionality.

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

DNA uCash is open source by design.

Users should understand the difference between:

- genuine DNA uCash mint flows
- modified forks
- imitation tokens
- trusted vs untrusted issuers

Open source improves transparency and auditability, but users must still evaluate trust assumptions.

---

# Intended Use Cases

DNA uCash is intended for:

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

These features are optional and are not required for normal offline DNA uCash operation.

DNA uCash may also integrate ecash protocls on bitcoin like Cashu.

---

# Development Notes / About The Project / Donate / Contact

DNA uCash was vibe coded with the assistance of ChatGPT.

I'm not a professional app developer or security engineer. My background is mostly self-taught and comes from years of casually learning technology, starting with basic HTML in the MySpace days, then later picking up bits and pieces, working on WordPress and Shopify sites and experimenting with various tech projects over time.

I have been interested in Bitcoin since 2018 and spent years learning about:
- private/public key systems
- Bitcoin address formats
- UTXOs
- custody models
- hardware wallets
- physical Bitcoin concepts
- Bitcoin security tradeoffs
- Linux, cgminer, bitaxe, mining

I regularly consume "more technical Bitcoin content" including:
- podcasts
- mailing list conversations
- protocol/security articles
- github/bitcointalk comments & threads

This is intentionally a Bitcoin-only project.

DNAu Cash will never support altcoins. Altcoins as scams, distractions, or unnecessary complexity.

The original inspiration for DNA uCash came from an older idea I had around 2020–2021 involving “sneakernet coins” — offline physical NFC Bitcoin bearer instruments that could be passed around somewhat like digital cash.

At the time, I could not realistically build the idea because:
- my coding knowledge was too limited
- I did not fully understand NFC hardware capabilities
- the cheap tags I initially researched (such as NTAG215s commonly found on Amazon) did not appear secure enough to be taken seriously for Bitcoin storage

Years later, after finding the idea in an old notes app, I popped idea into a modern LLM and discovered NTAG424 DNA tags, so I revisited the concept.

After three weeks of late nights, testing, debugging, rewriting, research, and extensive back-and-forth AI-assisted development sessions, DNA uCash is the result.

The application was developed through a combination of:
- iterative testing
- manual auditing
- design reasoning
- extensive AI-assisted coding sessions
- reviewing and refining generated code over many revisions

I can generally:
- understand high-level code structure
- follow logic flows
- debug issues
- make targeted edits
- reason about UX/security tradeoffs
- carefully review generated code

But I do **not** claim to fully understand every:
- low-level Android detail
- NFC edge case
- java or kotlin function
- cryptographic implementation detail
- hardware interaction
- memory behavior
- lifecycle interaction
- attack surface

I have done my best to:
- audit the logic carefully
- document design assumptions
- explain tradeoffs honestly
- avoid misleading claims
- keep the architecture understandable and transparent

However, this project should still be considered experimental software.

There may be:
- bugs
- incorrect assumptions
- edge cases
- security weaknesses
- interoperability issues
- flaws that neither I nor the AI recognized

Users should independently review, audit, test, and evaluate the software before trusting it with meaningful funds.

The open-source nature of the project is intentional and meant to encourage transparency, experimentation, review, and improvement by the broader Bitcoin community.

#Donate

Like the project? Feel free to donate!
bc1q90p7vq843wk2ws9f8k89r60ws6a8q97ac06n7y
email: j@10msats.net
x: @justh0dl

---

# License / Disclaimer 

MIT License

Use at your own risk. No warranty is provided.
