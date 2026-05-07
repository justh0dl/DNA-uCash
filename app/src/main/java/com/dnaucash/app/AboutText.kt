package com.dnaucash.app

object AboutText {

    val CONTENT = """
After 17 years, Bitcoin is finally, a real, physical & easily accessible token.

DNAuCash is an experimental Android app for creating and using physical Bitcoin tokens with NTAG424 DNA NFC chips.

It turns Bitcoin into something you can hold, pass, and interact with in the real world.

Each tag contains a Bitcoin private key stored in protected memory, along with a public record that can be read by any phone.

The idea is simple:
you can hand someone Bitcoin the same way you would hand them cash. They receive it & can spend it by giving it so someone else, or they can sweep the funds.

OPEN DIME / CASASCIUS COIN INSPIRATION

DNAuCash is heavily inspired by OpenDime & Casascius Coins 

With an OpenDime or Casascius Coins, you can verify that a device has not been physically tampered with to reveal the key.
Once it is opened, that state is visible and the device is considered spent.

DNAuCash aims to recreate that signal using NFC tags.

- An unrevealed tag signals that the private key has not been exposed through the intended process
- A revealed tag exposes the private key and should be treated as spent

The goal is not perfect enforcement, but a clear and understandable signal:
this coin has been opened

WHAT YOU CAN DO WITH IT

DNAuCash is designed for real-world, flexible use.

Coins can be:
- given as gifts
- used in poker or games
- handed out at events
- used for tipping or small payments
- distributed in batches
- used in experiments with physical Bitcoin

Because each tag has a unique UID, it is also possible to build systems on top of it:
- simple databases that track coins
- small community currencies
- event-based token systems
- trusted group economies

It is intentionally open-ended.

TOKEN TYPES

Bearer Tokens
Anyone with the tag can reveal and spend the funds.
This behaves like physical cash.

Guarded Tokens
Require a password or passphrase to reveal.
This adds an extra layer of protection.

Stealth Tokens
Appear as normal NFC tags and do not look like Bitcoin.
Require a password to access the hidden coin data.

Users can choose between simplicity and added friction.

WHY NTAG424 DNA

DNAuCash uses NTAG424 DNA tags because they offer a practical balance between:

- cost
- availability
- compatibility
- and security features 

They are high-security, cryptographically secure chips designed for anti-counterfeiting and offer reliable & consistent behavior across many NFC devices.

Typical cost:
- $1-$2 per tag, depending on quantity & supplier. A 10 pack can usually be purchased with shipping for under $15

Once minted, compared to simpler NFC tags our tags:
- cannot be freely read and overwritten
- are more secure for storing private keys
- are harder to brute force

Compared to secure-element devices:
- those are more secure, but more expensive and harder to develop for

DNAuCash sits in the middle:
strong enough for practical use, simple enough to scale.

HOW IT COMPARES TO HARDWARE DEVICES

Devices like Satscard use secure elements and enforce strict hardware boundaries.

DNAuCash takes a different approach:

- private keys are revealed and intended to be swept when used
- protection is handled through software and tag controls
- tokens are inexpensive and easy to create in large numbers

Typical cost comparison:
- Satscard: roughly $6–$20 per card
- Opendime: $69 for a 3-Pack
- NTAG424 DNA: bulk packs of about 10-50 tags for roughly $15–$60

This makes DNAuCash great for:
- low-value gifting
- events and distributions
- games and social use
- experimentation
- self-issued coins

It trades maximum hardware security for:
- accessibility
- flexibility
- scale

TRUST MODEL

DNAuCash is a semi-trusted system.

It does NOT guarantee:
- that a private key was not copied by the issuer
- that a coin was not duplicated before distribution
- that a coin actually holds the value claimed in its message

Because of this:

Coins should only be trusted if the issuer is trusted.

For bearer coins, a good rule is:

- small values can be accepted as-is
- larger values should be revealed and swept before trusting

If a coin comes from an unknown source, the safest action is:
reveal it and sweep the funds immediately.

THE HONOR SYSTEM

Like Casascius Coins, these tokens only work because people choose to use them honestly.

DNAuCash cannot prevent an issuer from keeping a copy of a private key.
That responsibility is on the person creating the coin.

If you create coins for others:

- do not misrepresent value
- do not deceive recipients
- only keep a copy of a private key if the recipient explicitly understands and agrees

There are valid cases for keeping a copy, such as:
- gifting coins to family
- helping with recovery if a key is lost
- managing funds in a trusted relationship

However:

If you keep a copy, you have full control over the funds.
You are effectively acting as a custodian.

With that comes responsibility.

If you want a coin to function like physical cash,
you should destroy all copies of the private key after minting.

In many cases, the safest approach is simple:
do not give yourself the ability to take the funds back.

The system provides flexibility.
Integrity is up to the user.

DESIGN PHILOSOPHY

DNAuCash is built around a simple idea:

make physical Bitcoin practical.

It focuses on:
- ease of use
- low cost
- real-world interaction
- and layered protection instead of absolute guarantees

It does not try to eliminate every possible attack.

Instead, it aims to make misuse:
- slower
- more frustrating
- and not economically worth the effort

In many cases, it makes more economic sense to simply sweep a coin than to try to break it.

IMPORTANT

DNAuCash is not a hardware wallet.

It is a tool for creating and using physical Bitcoin tokens.

Users should:
- use appropriate amounts for the level of trust involved
- use strong passphrases for guarded tokens
- treat revealed keys as compromised
- sweep funds promptly after revealing

A Bit of Physical Bitcoin | aka "The Sats Button"
""".trimIndent()
}