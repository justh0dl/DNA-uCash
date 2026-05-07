    package com.dnaucash.app

    import org.bitcoinj.core.Base58
    import org.bitcoinj.core.DumpedPrivateKey
    import org.bitcoinj.core.LegacyAddress
    import org.bitcoinj.core.NetworkParameters
    import org.bitcoinj.core.SegwitAddress
    import org.bitcoinj.params.MainNetParams

    object BitcoinValidation {
        private val params: NetworkParameters = MainNetParams.get()

        fun isSupportedMainnetAddress(address: String): Boolean {
            return try {
                when {
                    address.startsWith("bc1q") -> {
                        val a = SegwitAddress.fromBech32(params, address)
                        a.witnessVersion == 0
                    }
                    address.startsWith("1") -> {
                        LegacyAddress.fromBase58(params, address)
                        true
                    }
                    else -> false
                }
            } catch (_: Exception) {
                false
            }
        }

        fun isSupportedPrivateKeyFormat(privateKey: String): Boolean {
            return try {
                val dumped = DumpedPrivateKey.fromBase58(params, privateKey)
                val base58Version = Base58.decodeChecked(privateKey)[0].toInt() and 0xff
                base58Version == params.dumpedPrivateKeyHeader && dumped.key.isCompressed
            } catch (_: Exception) {
                false
            }
        }

        fun deriveSupportedAddress(privateKey: String, desiredAddress: String): String {
            val dumped = DumpedPrivateKey.fromBase58(params, privateKey)
            val key = dumped.key
            return when {
                desiredAddress.startsWith("bc1q") -> SegwitAddress.fromKey(params, key).toString()
                desiredAddress.startsWith("1") -> LegacyAddress.fromKey(params, key).toString()
                else -> throw IllegalArgumentException("Unsupported address type")
            }
        }

        fun privateKeyMatchesAddress(privateKey: String, address: String): Boolean {
            if (!isSupportedMainnetAddress(address) || !isSupportedPrivateKeyFormat(privateKey)) return false
            return deriveSupportedAddress(privateKey, address) == address
        }
    }
