package com.dnaucash.app

object SpendText {

    val CONTENT = """
HOW TO SPEND A DNAuCash COIN

When you reveal a coin, you are given a Bitcoin private key.

This key controls the funds. Once it is revealed, it should be considered exposed.

CRITICAL RULE

Do not send funds back to the same address on the tag.

Always send funds to a completely different wallet you control.

After sweeping, the coin should not be reused.

PRIVATE KEY TYPE

The app generates compressed WIF private keys.

These:
- start with K or L
- commonly map to bc1q addresses

METHOD 1: BlueWallet

Download:
https://bluewallet.io/

Steps:
1. Install BlueWallet
2. Tap Add Wallet
3. Tap Import wallet
4. Paste your private key
5. Wallet will load

To move funds:
1. Tap Send
2. Paste a receiving address from a different wallet
3. Tap Send Max
4. Confirm the transaction

Note:
BlueWallet uses an import model followed by sending funds.
Move funds immediately and do not leave them in the imported wallet.

METHOD 2: Electrum

Download:
https://electrum.org/#download

Steps:
1. Install Electrum
2. Create a new wallet
3. Go to:
   Wallet → Private Keys → Sweep
4. Paste your private key

Electrum will detect the funds and create a sweep transaction.

Note:
Electrum performs a true sweep.
The private key is not stored in your wallet.

Quick Comparison

BlueWallet:
- easier
- imports key
- requires manual send

Electrum:
- true sweep
- cleaner
- recommended

Security Reminder

If a private key has been:
- revealed
- typed
- scanned
- shared

It should be treated as compromised.

Act immediately:
- sweep the funds
- send to a fresh wallet
- never reuse the key
""".trimIndent()
}