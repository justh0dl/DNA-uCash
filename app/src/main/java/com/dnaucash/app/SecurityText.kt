package com.dnaucash.app

object SecurityText {

    val CONTENT = """
DNAuCash Security Overview

This document describes the security model, assumptions, and tradeoffs behind DNAuCash.

It is intended for technical users who want to understand how the system works at a deeper level.

SECURITY MODEL
 
DNAuCash is designed to create physical Bitcoin bearer-style tokens using NTAG424 DNA NFC tags.

We attempt to make physical Bitcoin cheaper and more practical by turning small to medium BTC amounts into low-cost NFC tokens, making smaller UTXOs easier to use while keeping many low-value tokens economically unattractive to attack.
 
The goal is not to create a fully hardened custody device or secure element. The goal is to create a practical, low-cost physical Bitcoin token with clear security boundaries, controlled reveal behavior, and protection against common misuse.
 
DNAuCash security comes from several layers working together:
 
- NTAG424 DNA protected memory
- authenticated access control
- per-tag hardware UID binding
- encrypted private-key storage
- key/address validation during minting
- controlled reveal flow
- optional user secrets for Guarded and Stealth tokens
- clear warnings around trust and issuer risk
 
DNAuCash should be understood as a practical physical Bitcoin system with explicit tradeoffs, not as a replacement for a hardware wallet or long-term high-value cold storage.
 
 
------------------------------------------------------------
 
TOKEN MODELS
 
DNAuCash currently supports three token models:
 
- Bearer Tokens
- Guarded Tokens
- Stealth Tokens
 
 
------------------------------------------------------------
 
BEARER TOKENS
 
Bearer tokens are the simplest DNAuCash token type.
 
They are designed to behave like physical cash.
 
Security model:
 
- Possession of the physical tag is enough to reveal the private key
- No additional password or secret is required
- Whoever controls the tag can reveal and sweep the funds
- If the tag is lost or stolen, the funds should be treated as at risk
 
Bearer tokens are best for:
 
- simple physical transfers
- gifts, tips, payments
- small-value hand-to-hand exchange
- situations where ease of use is key
 
Bearer tokens are NOT ideal for:
 
- high-value storage
- situations where theft or loss is likely
- situations where the receiver does not trust the source of the token
 
Important:
 
A bearer token is only as safe as physical possession of the tag.
 
If someone can access the tag, they can reveal the private key.
 
 
------------------------------------------------------------
 
GUARDED TOKENS
 
Guarded tokens require both:
 
- the physical NTAG424 tag
- a user-provided secret/password
 
The password is never stored on the tag or in the app.
 
Instead, the app derives the needed authentication and content keys at runtime using the user secret and token-specific data.
 
Security model:
 
- The tag alone is not enough
- The password alone is not enough
- Both the tag and the correct password are required
- Wrong password attempts are intentionally slowed by the selected Forge Level
- Security depends heavily on the strength of the chosen password
 
Guarded tokens are best for:
 
- tokens that may be stored or transported
- situations where loss/theft is a concern
- users who want extra protection beyond physical possession
 
Guarded tokens are NOT magic protection.
 
If the password is weak, guessed, reused, or written down with the token, the protection is reduced.
 
 
------------------------------------------------------------
 
STEALTH TOKENS
 
Stealth tokens are guarded tokens with a disguised public appearance.
 
Instead of showing a normal DNAuCash public record to generic NFC readers, a Stealth token can appear as something ordinary, such as:
 
- a note
- a door tag
- an employee ID
- a WiFi-style record
- a generic maintenance or asset tag
 
Security model:
 
- The public NDEF record is intentionally disguised
- The real protected metadata is stored behind NTAG424 authentication
- The token requires the physical tag and the correct password
- The visible public message is used as part of the stealth derivation path
- The app must inspect the token before it can reveal it
 
Stealth tokens are useful when the user wants the tag to avoid obviously advertising itself as a Bitcoin bearer token.
 
Important:
 
Stealth does not make the Bitcoin private key mathematically safer than Guarded mode.
 
It adds privacy and disguise at the public NFC layer.
 
 
------------------------------------------------------------
 
FORGE LEVELS AND ATTEMPT COST
 
DNAuCash uses Forge Levels to add friction to key derivation and reveal operations.
 
Forge Levels are not secrets.
 
They do not make a key impossible to break.
 
They increase the amount of work required per access attempt.
 
Current Forge Levels:
 
- Cast
- Forged
- Tempered
- Hardened
 
Higher Forge Levels require more computation before the app can derive the needed keys.
 
This creates economic resistance:
 
- casual guessing becomes slower
- repeated password attempts become more expensive
- misuse becomes less convenient
- attackers must spend more time per attempt
 
Forge Levels help most with Guarded and Stealth tokens, where an attacker may try to guess the secret.
 
For Bearer tokens, Forge Levels add delay/friction, but they do not add secrecy because no user password is required.
 
Important:
 
Forge Levels are not a substitute for a strong secret.
 
For Guarded and Stealth tokens, the user secret still matters most.
 
 
------------------------------------------------------------
 
WHAT THE APP ENFORCES
 
DNAuCash attempts to enforce consistent and verifiable token structure during minting and reveal operations.
 
Specifically, the app:
 
- verifies that the provided private key matches the intended Bitcoin address
- prevents creating tokens with invalid or mismatched key/address pairs
- requires NTAG424 DNA support before minting or revealing protected tokens
- derives protected authentication data from token-specific inputs
- binds derivation paths to the genuine tag hardware UID where applicable
- uses protected NTAG424 memory for sensitive payloads
- encrypts protected private-key data
- structures reveal operations so protected tokens transition from unrevealed to revealed before displaying the private key
 
This helps reduce:
 
- fake tokens created with mismatched keys
- malformed key/address pairs
- accidental minting mistakes
- simple copy/paste NFC clones
- generic NFC tags pretending to be protected DNAuCash tokens
- protected tokens being viewed without transitioning to revealed state
 
These protections apply to tokens created and handled through the genuine DNAuCash mint/reveal flow.
 
 
------------------------------------------------------------
 
WHAT THE APP DOES NOT GUARANTEE
 
DNAuCash is open source.
 
That is intentional.
 
Open source makes the system easier to inspect, fork, audit, and improve. But it also means the app cannot guarantee that every token claiming to be DNAuCash was created honestly or with the official minting flow.
 
DNAuCash does NOT guarantee:
 
- that a minter created only one copy of a token
- that a minter destroyed all backups of the private key
- that a minter never exported or saved the private key elsewhere
- that a third party did not fork or modify the app to create misleading tokens
- that funds will remain at the associated Bitcoin address forever
- that a token received from a stranger is safe to hold without sweeping
 
The app can display the current balance of the Bitcoin address, but a displayed balance is not the same as a guarantee.
 
Funds can be moved later by anyone who already has the private key.
 
If a token comes from an unknown, untrusted, or dishonest source, the safest option is:
 
- reveal the token
- sweep the funds into a wallet you control
- treat the original token as spent
 
DNAuCash tokens should be treated similarly to physical cash:
 
the security of the token depends both on the technology and on trust in the source that created and funded it.
 
 
------------------------------------------------------------
 
BALANCE DISPLAY
 
DNAuCash can query and display the current balance of the Bitcoin address associated with a token.
 
This is useful because it helps users see whether funds appear to exist at the address.
 
However:
 
- balance checks depend on external Bitcoin data sources
- balance can change after the scan
- an address can be funded and later emptied
- a balance does not prove that the minter did not keep a copy of the private key
 
The balance display is a convenience feature.
 
It is not a custody guarantee.
 
 
------------------------------------------------------------
 
UID BINDING AND ANTI-CLONE PROPERTIES
 
NTAG424 DNA tags have a hardware UID.
 
The UID is manufacturer-programmed and is not user-modifiable.
 
DNAuCash uses the tag UID as part of its derivation logic where appropriate.
 
This means that simply copying public NDEF data from one tag to another should not create a working clone.
 
A copied tag with a different UID will derive different authentication/content material and should fail protected operations.
 
In simple terms:
 
same visible data + different hardware UID = different derived keys
 
This protects against simple cloning attempts such as:
 
- copying public JSON
- copying visible NDEF records
- writing the same public text to another tag
- using a generic NTAG213/215/216 tag as a fake DNAuCash token
 
However, the UID is not treated as secret.
 
Many NFC tools can display a tag UID.
 
That is expected.
 
The security does not come from hiding the UID.
 
The security comes from combining UID-bound derivation with NTAG424 protected authentication and encrypted storage.
 
 
------------------------------------------------------------
 
GENERIC NFC TAGS AND IMITATION TOKENS
 
Most NFC tags have a UID.
 
This includes common tags such as NTAG213, NTAG215, NTAG216, MIFARE Ultralight, and NTAG424 DNA.
 
But having a UID is not enough.
 
Generic NFC tags do not provide the same protected NTAG424 features required by DNAuCash.
 
They generally lack:
 
- NTAG424 secure file structure
- AES authenticated sessions
- protected encrypted file access
- authenticated writes
- EV2-style authentication behavior
 
A generic NFC tag may be able to imitate visible public text.
 
It should not be able to imitate a genuine protected DNAuCash token.
 
This is why DNAuCash checks for NTAG424 support before protected operations.
 
 
------------------------------------------------------------
 
OPEN SOURCE AND FORKS
 
Because DNAuCash is open source, anyone can inspect or modify the code.
 
This is good for transparency.
 
But it also means users should understand the difference between:
 
- a token created through the genuine DNAuCash app flow
- a token created by a modified fork
- a token created by an unknown issuer
- a token that merely imitates the public appearance of DNAuCash
 
A modified fork could change warnings, labels, behavior, or scanning logic.
 
For this reason, users should be careful when accepting tokens from unknown sources.
 
If the source is not trusted, the safest action is to reveal and sweep.
 
 
------------------------------------------------------------
 
REVEAL FLOW AND FINALITY
 
DNAuCash is designed so protected tokens reveal in a controlled sequence.
 
For unrevealed tokens, the intended reveal flow is:
 
- authenticate to the protected area
- mark the protected payload as revealed
- decrypt the private key
- display the private key to the user
- the token is locked
 
The private key should not be shown before the token state has transitioned to revealed.
 
For Bearer and Guarded tokens:
 
- the protected payload is marked revealed
- the public record is updated to revealed
- the public record is locked
- the private key is displayed
 
For Stealth tokens:
 
- the protected payload is marked revealed
- protected stealth metadata is updated
- the public NDEF record remains disguised
- the private key is displayed
 
Once a token is revealed:
 
- the private key must be treated as exposed
- anyone who sees or copies it may be able to spend the funds
- the funds should be swept immediately
- the tag should not be reused as value storage
 
A revealed token should be treated as spent or compromised.
 
 
------------------------------------------------------------
 
ALREADY-REVEALED TOKENS
 
If a token is already revealed, DNAuCash allows the user to view the private key again.
 
This is intentional.
 
A revealed token is no longer treated as secure value storage.
 
The user may need to view or copy the private key in order to sweep the funds.
 
The important rule is:
 
- unrevealed token: mark revealed before showing key
- revealed token: allow key viewing because the token is already compromised/revealed
 
 
------------------------------------------------------------
 
PARTIAL MINTING AND FAILURE STATES
 
DNAuCash uses multi-step minting for all tokens.
 
This is necessary because the app must:
 
- write public data
- write encrypted private-key payloads
- configure protected access
- finalize authentication behavior
 
If minting is interrupted, the tag may end up partially initialized.
 
Possible failure states include:
 
- public record written but protected payload missing
- protected payload written but authentication not finalized
- authentication keys changed before the final step
- tag state inconsistent with the app’s expected flow
 
In some cases, a partially minted tag may become unusable within DNAuCash.
 
This is an intentional tradeoff.
 
The system prioritizes preventing rollback, tampering, and unsafe reuse over easy recovery of interrupted mints.
 
 
------------------------------------------------------------
 
PRIVATE KEY LOGGING AND DATABASE FEATURES
 
DNAuCash includes optional logging/database features for local records.
 
Private key logging should be treated as dangerous.
 
If private key logging is enabled:
 
- private keys may be stored outside the protected tag
- the physical token no longer represents the only copy of the spend key
- anyone with access to the device logs may be able to recover keys
 
For strongest bearer-token behavior:
 
- keep private key logging disabled
- do not store private keys outside the token
- sweep funds if a key was ever exported, copied, logged, screenshotted, or shared
 
 In order to enable private key logging tap "Database options" 21 times.
 
 There are valid cases for keeping a copy, such as:
- gifting coins to friends/family
- helping with recovery if a key is lost
- managing funds in a trusted relationship

The export/database features may also support future community mint workflows.
 
NTAG424 DNA tags include SUN/SDM messaging and tag-server authentication features that can be used by private or community mints to add stronger anti-counterfeit checks. In that model, exported mint records could help an issuer or verifier track minted tokens, verify expected tag data, and support server-backed authenticity checks.
 
These features are optional and are not required for normal offline DNAuCash use.
 
------------------------------------------------------------
 
OFFLINE OPERATION
 
DNAuCash is designed to function primarily offline.
 
Internet access is not required for:
 
- minting tokens
- revealing tokens
- deriving authentication keys
- validating private keys
- validating Bitcoin addresses
- encrypting or decrypting protected payloads
 
The app only uses internet connectivity for optional balance display features.
 
Private key and address validation are performed locally on-device using standard Bitcoin cryptographic operations and local derivation checks. DNAuCash does not send private keys, guarded secrets, or decrypted payloads to external servers as part of normal operation.
 
Protected token operations are handled locally using NTAG424 authentication libraries and standard cryptographic primitives running entirely on the device.

------------------------------------------------------------
 
PASSWORD AND SECRET QUALITY
 
Guarded and Stealth tokens depend heavily on the strength of the user secret.
 
A weak secret can undermine the entire protection model.
 
Bad secrets include:
 
- short PINs
- birthdays
- names
- common words
- reused passwords
- simple phrases
- anything written on or stored with the token
 
Better secrets include:
 
- long unique passphrases
- multiple random words
- secrets not reused anywhere else
- secrets not stored with the tag
 
If the password is lost, DNAuCash cannot recover the token.
 
This is intentional.
 
There is no recovery backdoor.
 
 
------------------------------------------------------------
 
TRUST MODEL
 
DNAuCash operates under a semi-trusted physical bearer-token model.
 
The technology can help prevent many common technical failures.
 
It cannot remove all trust assumptions.
 
Users must still consider:
 
- who minted the token
- whether the private key was ever backed up
- whether multiple tokens were created for the same key
- whether the funding source is trusted
- whether the token came from a known or unknown party
- whether the amount is small enough for this kind of physical transfer
 
Tokens from trusted sources may be held like physical Bitcoin cash.
 
Tokens from unknown sources should generally be revealed and swept.
 
 
------------------------------------------------------------
 
LIMITATIONS
 
DNAuCash does not provide:
 
- hardware-wallet-grade key isolation
- guaranteed non-duplication by the issuer
- guaranteed destruction of minter backups
- protection against advanced physical chip attacks
- protection against malicious forks
- recovery if passwords are lost
- recovery if minting is interrupted
- protection after the private key has been revealed
 
NTAG424 DNA provides useful protected NFC features.
 
It is not the same as a dedicated secure element or hardware wallet.
 
Higher-value storage requires stronger systems.
 
 
------------------------------------------------------------
 
DESIGN PHILOSOPHY
 
DNAuCash does not assume that all attacks can be prevented.
 
Instead, it focuses on practical layered protection:
 
- make accidental mistakes harder
- make malformed tokens less likely
- make casual cloning harder
- make unauthorized access slower
- make reveal state transitions explicit
- make attacks more costly than simply sweeping legitimate funds
 
The intended use case is:
 
- small to medium value physical Bitcoin transfers
- gifts
- in-person exchange
- collectible or cash-like Bitcoin tokens
- experiments with low-cost physical Bitcoin instruments
 
DNAuCash is designed to be transparent about its tradeoffs.
 
It should not encourage users to treat NFC tags as invincible vaults.
 
 
------------------------------------------------------------
 
SUMMARY
 
DNAuCash provides:
 
- structured minting
- private key/address validation
- NTAG424 protected storage
- UID-bound derivation
- controlled reveal mechanics
- optional password-protected tokens
- optional stealth/disguised public appearance
- balance display for convenience
- practical friction against misuse
 
DNAuCash does not provide:
 
- guaranteed issuer honesty
- guaranteed uniqueness of private keys
- hardware-wallet-level custody
- protection after reveal
- recovery from lost secrets
 
Best understood:
 
DNAuCash is a practical system for physical Bitcoin with layered protections and clear tradeoffs.
 
It is strongest when used with trusted minting, strong secrets, small to medium values, and immediate sweeping when trust is uncertain.""".trimIndent()
}
