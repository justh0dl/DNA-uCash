package com.dnaucash.app

object MintingGuideText {

    val CONTENT = """
---------------------------------------------------------
MINTING VALUE GUIDE
---------------------------------------------------------

Choose amounts based on how the coin will be used:

- Bearer: small amounts
1,000–250,000 sats / 0.00001–0.0025 BTC
Anyone holding it can spend it.

- Guarded: medium amounts
100,000–1,000,000 sats / 0.001–0.01+ BTC
Requires a password to unlock.

- Stealth: depends on user risk tolerance
Use when you want the coin to look like a normal NFC tag. Security relies on passphrase & obfuscation 

Tokens rely on economic incentives.

Try to mint amounts that aren't worth cheating, large amounts break the model.

---------------------------------------------------------
MINTING GUIDE
---------------------------------------------------------

This guide explains how to create a DNAuCash coin using Electrum Android Wallet.

The basic flow is:

1. Create or open an Electrum wallet
2. Copy a receiving address
3. Reveal that address’s private key
4. Paste both into DNAuCash
5. Mint the NFC coin

---------------------------------------------------------
INSTALL ELECTRUM
---------------------------------------------------------

Download Electrum:

https://electrum.org/#download

Install Electrum on Android.

Only download Electrum from the official Electrum website.

---------------------------------------------------------
CREATE OR RESTORE A WALLET
---------------------------------------------------------

Open Electrum.

You can either:

Create a new wallet:
- choose standard wallet
- create a new seed
- write down the seed safely
- confirm the seed
- set a wallet password

Or restore an existing wallet:
- choose restore wallet
- enter your seed
- wait for the wallet to load

Important:

Your Electrum seed controls your wallet.

Do not share it.

DNAuCash only needs one address and its matching private key.

---------------------------------------------------------
GET A RECEIVING ADDRESS
---------------------------------------------------------

In Electrum [Android]:

1. Open your wallet
2. Click on "wallet" on the top left hand corner
3. Click on "Addresses/Coins"
4. Click an unused address
5. From here you can copy the address

The bc1q address is what people will see when they scan the DNAuCash coin.

Paste this address into DNAuCash:

Address field:
Paste address here

--

---------------------------------------------------------
HOW TO COPY THE PRIVATE KEY FOR THAT ADDRESS
---------------------------------------------------------

In Electrum, after copying the bc1q.. address, go back to the same screen

1. Find the same address you copied
2. Scroll down.
3. Click on "Tap to show private key"
4. Copy the private key

The private key should be a compressed WIF key.

It usually starts with:

K

or

L

Paste this private key into DNAuCash:

Private key field:
Paste private key here

---------------------------------------------------------
IMPORTANT
---------------------------------------------------------

The address and private key must match.

DNAuCash checks this before minting.

If they do not match, the app will reject the coin.

This helps prevent creating fake or unrecoverable coins.

DNAuCash checks this locally on your device before minting.

The app does NOT send your private key, address, or any data to any server.
Nothing is stored, uploaded, or shared.

Verification is done independently by deriving the address from the private key
and comparing it to what you entered.

If they do not match, the app will reject the coin.

Network permissions are NOT used for this process.

The app only uses internet access to optionally fetch the Bitcoin balance
from public APIs. The core functionality — minting, scanning, and revealing —
works completely offline.

---------------------------------------------------------
ADD AN OPTIONAL MESSAGE
---------------------------------------------------------

You may add a short message to the coin.

Examples:

- Happy Birthday
- Poker Prize
- Event Token
- 10,000 sats

Important:

The message is not enforced.

Always verify the actual Bitcoin balance on-chain.

---------------------------------------------------------
CHOOSE TOKEN TYPE
---------------------------------------------------------

Bearer Token

- anyone with the tag can reveal it
- works like physical cash
- best for simple handoff

Guarded Token

- requires a secret to reveal
- better if the coin could be lost or stolen
- do not forget the secret

Stealth Token

- appears as a normal NFC tag
- does not show Bitcoin-related data when scanned
- requires a secret to access the hidden coin

Best for:
- discretion
- low visibility environments
- avoiding attention

---------------------------------------------------------
CHOOSE FORGE LEVEL
---------------------------------------------------------

Forge level controls how much delay is added when minting or revealing a coin.

Cast:
fastest

Forged:
light friction

Tempered:
moderate friction

Hardened:
highest friction

Higher forge levels make repeated attempts slower.

---------------------------------------------------------
MINT THE COIN
---------------------------------------------------------

In DNAuCash:

1. Paste the address
2. Paste the private key
3. Add an optional message
4. Choose Bearer or Guarded
5. Choose Forge Level
6. Tap Program Tag

---------------------------------------------------------
TWO-TAP MINTING
---------------------------------------------------------

Minting requires two NFC taps.

Tap 1:
- writes the public coin record
- writes the protected private key payload

Then remove the tag.

Tap 2:
- finalizes the security key
- locks the coin into its final state

Both taps must complete.

---------------------------------------------------------
DO NOT INTERRUPT MINTING
---------------------------------------------------------

If minting is interrupted between tap 1 and tap 2:

- the public message may appear on the tag
- but the coin may not be usable
- authentication may fail
- the tag may become unrecoverable

Always wait for the app to tell you what to do next.

---------------------------------------------------------
TEST FIRST
---------------------------------------------------------

Before minting meaningful value:

- test with a small amount
- reveal and sweep one test coin
- confirm your process works

---------------------------------------------------------
AFTER MINTING
---------------------------------------------------------

Once the coin is minted:

- the coin can be scanned
- the public address can be checked
- the private key remains protected until reveal

If you are giving the coin to someone else:

- tell them to verify the balance
- tell them to sweep after revealing
- do not reuse revealed coins

---------------------------------------------------------
FINAL REMINDER
---------------------------------------------------------
DNAuCash does not create Bitcoin funds.

It turns an existing Bitcoin private key and address into a physical NFC coin.

You are responsible for:

- funding the address
- verifying the balance
- protecting the seed/private key
- completing both minting taps
""".trimIndent()
}