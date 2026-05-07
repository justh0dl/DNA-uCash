package com.dnaucash.app

object HowCoinsWorkText {

    val CONTENT = """
HOW DNAuCash COINS WORK

A DNAuCash coin is an NFC tag that contains:

- a Bitcoin address (public)
- a protected private key (not directly readable)
- metadata describing how the coin should be accessed

The tag can be scanned by any compatible phone to inspect its public data.

To access the funds, the private key must be revealed.

------------------------------------------------------------
BEARER COINS
------------------------------------------------------------
Bearer coins are the simplest form.

- Anyone with the tag can reveal the private key
- No password is required
- Possession = control

This behaves like physical cash:

- If you have it, you can spend it
- If you lose it, someone else can spend it

Bearer coins are:
- fast
- simple
- ideal for low-friction transfers

------------------------------------------------------------
GUARDED COINS
------------------------------------------------------------
Guarded coins require an additional secret.

- The tag alone is not enough
- A password or passphrase is required
- The secret is never stored on the tag

When revealing:

- the app combines the tag data and the secret
- derives the required keys
- and unlocks the private key

Guarded coins provide:
- protection against loss or theft of the tag
- resistance to unauthorized access

Security depends on:
- the strength of the chosen secret
- the selected forge level

------------------------------------------------------------
FORGE LEVELS
------------------------------------------------------------
Forge levels control how much work is required to attempt access.

They introduce a delay for each unlock attempt.

Available levels:

Cast (~1 second)
- minimal delay
- fastest user experience

Forged (~3 seconds)
- light friction
- slightly slower attempts

Tempered (~7 seconds)
- moderate protection
- noticeable delay

Hardened (~21 seconds)
- high friction
- very slow repeated attempts

------------------------------------------------------------
WHAT FORGE LEVELS DO
------------------------------------------------------------
Forge levels increase the cost of trying to unlock a coin.

They apply to:
- password attempts (guarded and stealth tokens)
- reveal operations (bearer tokens)

This makes:
- brute force attempts slower
- repeated guessing more expensive

------------------------------------------------------------
WHAT FORGE LEVELS DO NOT DO
------------------------------------------------------------
Forge levels do NOT:

- make a coin unbreakable
- prevent someone with full control from eventually accessing data
- replace good secrets (for guarded coins)

They are a friction mechanism.

------------------------------------------------------------
STEALTH TOKENS
------------------------------------------------------------
Stealth tokens are designed to look like ordinary NFC tags while still securely holding Bitcoin.

What makes them different?

Instead of showing obvious wallet data when scanned, a stealth token displays normal-looking content, such as:

An access panel (door code)
A meeting link
A maintenance tag
An employee contact card
An asset or ticket record

To anyone using a generic NFC app, the tag appears completely unrelated to Bitcoin.

Where is the Bitcoin data?

The actual coin data is not visible in plain text.

The Bitcoin data is not visible in the public message.

It is stored in the protected area of the tag and only accessible through the app using the correct password.
If protected, it prompts for the password to proceed
Security model

Stealth tokens combine multiple layers:

Obfuscation
The tag does not look like a crypto device
Encryption (guarded/stealth types)
The private key is never readable without authentication
Tamper signaling
Once a coin is revealed, it is permanently marked as revealed
Important notes
Stealth tokens are meant for plausible deniability, not invisibility
The encoded field must remain intact, modifying or truncating it will break the coin
If a password is used and forgotten, the coin cannot be recovered

When to use stealth tokens

Stealth tokens are ideal when you want:

A coin that does not attract attention
A physical bearer instrument that blends into normal environments
An additional layer of discretion beyond standard guarded tokens
A durable, water-resistant form factor for carrying small amounts in uncertain situations

In short:

A stealth token behaves like a Bitcoin coin to your app
but looks like an ordinary NFC tag to everyone else.

------------------------------------------------------------
DESIGN INTENT

The system is designed so that:

- legitimate use is simple
- misuse is slower and more frustrating

For most coins:

it should be easier to simply reveal and sweep the funds
than to attempt to bypass the system.

------------------------------------------------------------

REVEALING A COIN

When a coin is revealed:

- the private key is exposed
- the tag state is updated
- the coin should be considered spent

After revealing:
- funds must be swept to a new wallet
- the tag should not be reused for value storage

------------------------------------------------------------

SUMMARY

DNAuCash coins are physical Bitcoin tokens that combine:

- NFC hardware
- software-based key derivation
- configurable friction (forge levels)

They are designed for:

- real-world interaction
- flexible use cases
- practical, low-cost deployment

with clear tradeoffs in security and trust.
""".trimIndent()
}