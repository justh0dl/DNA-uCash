package com.dnaucash.app

import android.content.Context
import android.content.SharedPreferences
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.SystemClock
import net.bplearning.ntag424.CommunicationMode
import net.bplearning.ntag424.DnaCommunicator
import net.bplearning.ntag424.command.ChangeFileSettings
import net.bplearning.ntag424.command.ChangeKey
import net.bplearning.ntag424.command.GetFileSettings
import net.bplearning.ntag424.command.ReadData
import net.bplearning.ntag424.command.WriteData
import net.bplearning.ntag424.constants.Ntag424
import net.bplearning.ntag424.constants.Permissions
import net.bplearning.ntag424.encryptionmode.AESEncryptionMode
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import org.json.JSONObject

object Ntag424Service {

    data class RevealResult(val privateKey: String)

    data class BearerMaterial(
        val authKey: ByteArray,
        val contentKey: ByteArray
    )

    data class MintPreflightResult(
        val dataFileSize: Int,
        val ndefFileSize: Int,
        val estimatedPublicNdefSize: Int
    )

    private val secureRandom = SecureRandom()
    private val BOOTSTRAP = hexToBytes("8d1f07c5a2b94763ef1c6e49b98d0a31")

    private const val BEARER_PREFS_NAME = "dnaucash_bearer_kdf"
    private const val BEARER_KEY_BENCHMARK_UNITS_PER_SEC = "benchmark_units_per_sec"
    private const val BEARER_DEFAULT_UNITS_PER_SEC = 8000.0
    private val BEARER_BENCHMARK_KEY =
        "DNAuCash-Bearer-KDF-Benchmark".toByteArray(StandardCharsets.UTF_8)

    private const val FLAG_REVEALED = 0x01

    private const val RAW_PRIV_LEN = 32
    private const val GCM_IV_LEN = 12
    private const val GCM_TAG_LEN = 16
    private const val CIPHERTEXT_LEN = RAW_PRIV_LEN + GCM_TAG_LEN
    private const val PAYLOAD_LEN = 1 + GCM_IV_LEN + CIPHERTEXT_LEN // flags + iv + ciphertext

    private data class Payload(
        val flags: Int,
        val iv: ByteArray,
        val ciphertext: ByteArray
    )

    // --------------------------------------------------
    // PRE-FLIGHT
    // --------------------------------------------------

    fun preflightMintBearer(tag: Tag, privateKey: String, publicJson: String): MintPreflightResult {
        require(BitcoinValidation.isSupportedPrivateKeyFormat(privateKey)) {
            "Private key must be a valid mainnet compressed WIF"
        }
        return preflightMintCommon(tag, publicJson)
    }

    fun preflightMintGuarded(tag: Tag, publicJson: String): MintPreflightResult {
        return preflightMintCommon(tag, publicJson)
    }

    private fun preflightMintCommon(tag: Tag, publicJson: String): MintPreflightResult {
        return withComm(tag) { comm ->
            require(AESEncryptionMode.authenticateEV2(comm, Permissions.ACCESS_KEY0, Ntag424.FACTORY_KEY)) {
                "Tag is not factory fresh"
            }

            val dataFs = GetFileSettings.run(comm, Ntag424.DATA_FILE_NUMBER)
            val ndefFs = GetFileSettings.run(comm, Ntag424.NDEF_FILE_NUMBER)

            require(PAYLOAD_LEN <= dataFs.fileSize) {
                "Protected payload is $PAYLOAD_LEN bytes but DATA file is only ${dataFs.fileSize} bytes"
            }

            val publicSize = estimateNdefTextFileBytes(publicJson)
            require(publicSize <= ndefFs.fileSize) {
                "Public JSON is $publicSize bytes but NDEF file is only ${ndefFs.fileSize} bytes"
            }

            MintPreflightResult(
                dataFileSize = dataFs.fileSize,
                ndefFileSize = ndefFs.fileSize,
                estimatedPublicNdefSize = publicSize
            )
        }
    }

    // --------------------------------------------------
    // BEARER MINT
    // --------------------------------------------------

    // Old compatibility path
    fun mintPhase1(tag: Tag, privateKey: String) {
        mintPhase1(tag, privateKey, null, null)
    }

    // New bearer path with k + forge level
    fun mintPhase1(tag: Tag, privateKey: String, bearerSalt: String?, forgeLevel: String?) {
        require(BitcoinValidation.isSupportedPrivateKeyFormat(privateKey)) {
            "Private key must be a valid mainnet compressed WIF"
        }

        withComm(tag) { comm ->
            require(AESEncryptionMode.authenticateEV2(comm, Permissions.ACCESS_KEY0, Ntag424.FACTORY_KEY)) {
                "Factory auth failed"
            }

            val uid = uid(tag)
            val raw = decodeCompressedWifToRaw32(privateKey)
            val payload = encrypt(uid, bearerSalt, forgeLevel, raw)

            val dataFs = GetFileSettings.run(comm, Ntag424.DATA_FILE_NUMBER)
            require(payload.size <= dataFs.fileSize) {
                "Protected payload is ${payload.size} bytes but DATA file is only ${dataFs.fileSize} bytes"
            }

            dataFs.commMode = CommunicationMode.FULL
            dataFs.readPerm = Permissions.ACCESS_KEY0
            dataFs.writePerm = Permissions.ACCESS_KEY0
            dataFs.readWritePerm = Permissions.ACCESS_KEY0
            dataFs.changePerm = Permissions.ACCESS_KEY0
            ChangeFileSettings.run(comm, Ntag424.DATA_FILE_NUMBER, dataFs)

            require(AESEncryptionMode.authenticateEV2(comm, Permissions.ACCESS_KEY0, Ntag424.FACTORY_KEY)) {
                "Factory re-auth failed after DATA settings change"
            }

            WriteData.run(comm, Ntag424.DATA_FILE_NUMBER, payload)

            val ndefFs = GetFileSettings.run(comm, Ntag424.NDEF_FILE_NUMBER)
            ndefFs.readPerm = Permissions.ACCESS_EVERYONE
            ndefFs.writePerm = Permissions.ACCESS_KEY0
            ndefFs.readWritePerm = Permissions.ACCESS_KEY0
            ndefFs.changePerm = Permissions.ACCESS_KEY0
            ChangeFileSettings.run(comm, Ntag424.NDEF_FILE_NUMBER, ndefFs)
        }
    }
    fun mintPhase1WithContentKey(tag: Tag, privateKey: String, contentKey: ByteArray) {
        require(BitcoinValidation.isSupportedPrivateKeyFormat(privateKey)) {
            "Private key must be a valid mainnet compressed WIF"
        }
        require(contentKey.size == 16) {
            "Bearer content key must be 16 bytes"
        }

        withComm(tag) { comm ->
            require(AESEncryptionMode.authenticateEV2(comm, Permissions.ACCESS_KEY0, Ntag424.FACTORY_KEY)) {
                "Factory auth failed"
            }

            val raw = decodeCompressedWifToRaw32(privateKey)
            val payload = encryptWithContentKey(contentKey, raw)

            val dataFs = GetFileSettings.run(comm, Ntag424.DATA_FILE_NUMBER)
            require(payload.size <= dataFs.fileSize) {
                "Protected payload is ${payload.size} bytes but DATA file is only ${dataFs.fileSize} bytes"
            }

            dataFs.commMode = CommunicationMode.FULL
            dataFs.readPerm = Permissions.ACCESS_KEY0
            dataFs.writePerm = Permissions.ACCESS_KEY0
            dataFs.readWritePerm = Permissions.ACCESS_KEY0
            dataFs.changePerm = Permissions.ACCESS_KEY0
            ChangeFileSettings.run(comm, Ntag424.DATA_FILE_NUMBER, dataFs)

            require(AESEncryptionMode.authenticateEV2(comm, Permissions.ACCESS_KEY0, Ntag424.FACTORY_KEY)) {
                "Factory re-auth failed after DATA settings change"
            }

            WriteData.run(comm, Ntag424.DATA_FILE_NUMBER, payload)

            val ndefFs = GetFileSettings.run(comm, Ntag424.NDEF_FILE_NUMBER)
            ndefFs.readPerm = Permissions.ACCESS_EVERYONE
            ndefFs.writePerm = Permissions.ACCESS_KEY0
            ndefFs.readWritePerm = Permissions.ACCESS_KEY0
            ndefFs.changePerm = Permissions.ACCESS_KEY0
            ChangeFileSettings.run(comm, Ntag424.NDEF_FILE_NUMBER, ndefFs)
        }
    }
    // Old compatibility path
    fun mintPhase2(tag: Tag) {
        mintPhase2(tag, null, null)
    }

    // New bearer path with k + forge level
    fun mintPhase2(tag: Tag, bearerSalt: String?, forgeLevel: String?) {
        withComm(tag) { comm ->
            require(AESEncryptionMode.authenticateEV2(comm, Permissions.ACCESS_KEY0, Ntag424.FACTORY_KEY)) {
                "Factory auth failed in phase 2"
            }

            val newKey = deriveAuthKey(uid(tag), bearerSalt, forgeLevel)
            ChangeKey.run(comm, Permissions.ACCESS_KEY0, null, newKey, 1)
        }
    }

    fun mintPhase2WithPrecomputed(tag: Tag, authKey: ByteArray) {
        require(authKey.size == 16) {
            "Bearer auth key must be 16 bytes"
        }

        withComm(tag) { comm ->
            require(AESEncryptionMode.authenticateEV2(comm, Permissions.ACCESS_KEY0, Ntag424.FACTORY_KEY)) {
                "Factory auth failed in phase 2"
            }

            ChangeKey.run(comm, Permissions.ACCESS_KEY0, null, authKey, 1)
        }
    }

    // Compatibility wrappers
    fun commitMintBearer(tag: Tag, privateKey: String) {
        throw IllegalStateException(
            "This build uses 2-phase minting. Use mintPhase1(tag, privateKey, bearerSalt, forgeLevel), remove tag, then mintPhase2(tag, bearerSalt, forgeLevel)."
        )
    }

    fun programOfflineBearer(tag: Tag, privateKey: String) {
        throw IllegalStateException(
            "This build uses 2-phase minting. Use mintPhase1(tag, privateKey, bearerSalt, forgeLevel), remove tag, then mintPhase2(tag, bearerSalt, forgeLevel)."
        )
    }

    // --------------------------------------------------
    // GUARDED MINT
    // --------------------------------------------------

    fun mintGuardedPhase1(tag: Tag, privateKey: String, contentKey: ByteArray) {
        require(BitcoinValidation.isSupportedPrivateKeyFormat(privateKey)) {
            "Private key must be a valid mainnet compressed WIF"
        }
        require(contentKey.size == 16) {
            "Guarded content key must be 16 bytes"
        }

        withComm(tag) { comm ->
            require(AESEncryptionMode.authenticateEV2(comm, Permissions.ACCESS_KEY0, Ntag424.FACTORY_KEY)) {
                "Factory auth failed"
            }

            val raw = decodeCompressedWifToRaw32(privateKey)
            val payload = encryptWithContentKey(contentKey, raw)

            val dataFs = GetFileSettings.run(comm, Ntag424.DATA_FILE_NUMBER)
            require(payload.size <= dataFs.fileSize) {
                "Protected payload is ${payload.size} bytes but DATA file is only ${dataFs.fileSize} bytes"
            }

            dataFs.commMode = CommunicationMode.FULL
            dataFs.readPerm = Permissions.ACCESS_KEY0
            dataFs.writePerm = Permissions.ACCESS_KEY0
            dataFs.readWritePerm = Permissions.ACCESS_KEY0
            dataFs.changePerm = Permissions.ACCESS_KEY0
            ChangeFileSettings.run(comm, Ntag424.DATA_FILE_NUMBER, dataFs)

            require(AESEncryptionMode.authenticateEV2(comm, Permissions.ACCESS_KEY0, Ntag424.FACTORY_KEY)) {
                "Factory re-auth failed after DATA settings change"
            }

            WriteData.run(comm, Ntag424.DATA_FILE_NUMBER, payload)

            val ndefFs = GetFileSettings.run(comm, Ntag424.NDEF_FILE_NUMBER)
            ndefFs.readPerm = Permissions.ACCESS_EVERYONE
            ndefFs.writePerm = Permissions.ACCESS_KEY0
            ndefFs.readWritePerm = Permissions.ACCESS_KEY0
            ndefFs.changePerm = Permissions.ACCESS_KEY0
            ChangeFileSettings.run(comm, Ntag424.NDEF_FILE_NUMBER, ndefFs)
        }
    }

    fun mintGuardedPhase2(tag: Tag, authKey: ByteArray) {
        require(authKey.size == 16) {
            "Guarded auth key must be 16 bytes"
        }

        withComm(tag) { comm ->
            require(AESEncryptionMode.authenticateEV2(comm, Permissions.ACCESS_KEY0, Ntag424.FACTORY_KEY)) {
                "Factory auth failed in guarded phase 2"
            }

            ChangeKey.run(comm, Permissions.ACCESS_KEY0, null, authKey, 1)
        }
    }
    // --------------------------------------------------
    // STEALTH METADATA (NEW MODEL)
    // --------------------------------------------------

    fun mintStealthMetadata(
        tag: Tag,
        address: String,
        message: String,
        tokenType: String,
        forgeLevel: String,
        authKey: ByteArray
    ) {
        require(authKey.size == 16) {
            "Auth key must be 16 bytes"
        }

        val metadataJson = JSONObject().apply {
            put("a", address)
            put("s", PublicCoinRecord.WIRE_STATE_UNREVEALED)
        }.toString()

        val metadataBytes = metadataJson.toByteArray(StandardCharsets.UTF_8)

        withComm(tag) { comm ->

            require(
                AESEncryptionMode.authenticateEV2(
                    comm,
                    Permissions.ACCESS_KEY0,
                    Ntag424.FACTORY_KEY
                )
            ) {
                "Factory auth failed (stealth metadata)"
            }

            val dataFs = GetFileSettings.run(comm, Ntag424.DATA_FILE_NUMBER)

            val maxLen = dataFs.fileSize - PAYLOAD_LEN
            require(maxLen > 0) {
                "No space for metadata"
            }

            require(metadataBytes.size <= maxLen) {
                "Metadata is ${metadataBytes.size} bytes but only $maxLen bytes are available"
            }

            // ⚠️ Write metadata at OFFSET (after payload)
            WriteData.run(
                comm,
                Ntag424.DATA_FILE_NUMBER,
                metadataBytes,
                PAYLOAD_LEN
            )
        }
    }
    fun readStealthMetadata(tag: Tag, authKey: ByteArray): String {
        require(authKey.size == 16) {
            "Auth key must be 16 bytes"
        }

        return withComm(tag) { comm ->

            val authenticated = try {
                AESEncryptionMode.authenticateEV2(
                    comm,
                    Permissions.ACCESS_KEY0,
                    authKey
                )
            } catch (_: Exception) {
                false
            }

            require(authenticated) {
                "Auth failed (stealth metadata)"
            }

            val fs = GetFileSettings.run(comm, Ntag424.DATA_FILE_NUMBER)

            val maxLen = fs.fileSize - PAYLOAD_LEN
            require(maxLen > 0) {
                "No space for metadata"
            }

            val raw = ReadData.run(
                comm,
                Ntag424.DATA_FILE_NUMBER,
                PAYLOAD_LEN,
                maxLen
            )

            val text = raw.toString(StandardCharsets.UTF_8).trim { it <= ' ' }

            require(text.startsWith("{") && text.contains("\"a\"")) {
                "No valid stealth metadata found"
            }

            text
        }
    }
    fun markStealthMetadataRevealed(tag: Tag, authKey: ByteArray) {
        require(authKey.size == 16) {
            "Auth key must be 16 bytes"
        }

        withComm(tag) { comm ->
            val authenticated = try {
                AESEncryptionMode.authenticateEV2(
                    comm,
                    Permissions.ACCESS_KEY0,
                    authKey
                )
            } catch (_: Exception) {
                false
            }

            require(authenticated) {
                "Auth failed (stealth reveal)"
            }

            val fs = GetFileSettings.run(comm, Ntag424.DATA_FILE_NUMBER)

            val maxLen = fs.fileSize - PAYLOAD_LEN
            require(maxLen > 0) {
                "No space for metadata"
            }

            val raw = ReadData.run(
                comm,
                Ntag424.DATA_FILE_NUMBER,
                PAYLOAD_LEN,
                maxLen
            )

            val json = raw.toString(StandardCharsets.UTF_8).trim { it <= ' ' }

            require(json.startsWith("{") && json.contains("\"a\"")) {
                "No valid stealth metadata found"
            }

            val obj = JSONObject(json)
            obj.put("s", PublicCoinRecord.WIRE_STATE_REVEALED)

            val updatedBytes = obj.toString().toByteArray(StandardCharsets.UTF_8)

            require(updatedBytes.size <= maxLen) {
                "Updated metadata too large"
            }

            WriteData.run(
                comm,
                Ntag424.DATA_FILE_NUMBER,
                updatedBytes,
                PAYLOAD_LEN
            )
        }
    }
    // --------------------------------------------------
    // STATE / SCAN
    // --------------------------------------------------

    // Old compatibility path
    fun getEffectiveState(tag: Tag): String {
        return getEffectiveState(tag, null, null)
    }

    // New bearer path with k + forge level
    fun getEffectiveState(tag: Tag, bearerSalt: String?, forgeLevel: String?): String {
        val authKey = deriveAuthKey(uid(tag), bearerSalt, forgeLevel)

        return withComm(tag) { comm ->
            require(AESEncryptionMode.authenticateEV2(comm, Permissions.ACCESS_KEY0, authKey)) {
                "Authentication failed"
            }

            val payload = readPayload(comm)
            if ((payload.flags and FLAG_REVEALED) != 0) "revealed" else "unrevealed"
        }
    }
    fun getEffectiveState(tag: Tag, authKey: ByteArray): String {
        require(authKey.size == 16) {
            "Bearer auth key must be 16 bytes"
        }

        return withComm(tag) { comm ->
            require(AESEncryptionMode.authenticateEV2(comm, Permissions.ACCESS_KEY0, authKey)) {
                "Authentication failed"
            }

            val payload = readPayload(comm)
            if ((payload.flags and FLAG_REVEALED) != 0) "revealed" else "unrevealed"
        }
    }
    // --------------------------------------------------
    // BEARER REVEAL
    // --------------------------------------------------

    // Old compatibility path
    fun revealOfflineBearer(tag: Tag, expectedAddress: String): RevealResult {
        return revealOfflineBearer(tag, expectedAddress, null, null)
    }

    // New bearer path with k + forge level
    fun revealOfflineBearer(
        tag: Tag,
        expectedAddress: String,
        bearerSalt: String?,
        forgeLevel: String?
    ): RevealResult {
        require(BitcoinValidation.isSupportedMainnetAddress(expectedAddress)) {
            "Expected address is invalid"
        }

        val uid = uid(tag)
        val authKey = deriveAuthKey(uid, bearerSalt, forgeLevel)

        return withComm(tag) { comm ->
            require(AESEncryptionMode.authenticateEV2(comm, Permissions.ACCESS_KEY0, authKey)) {
                "Authentication failed"
            }

            var payload = readPayload(comm)

            if ((payload.flags and FLAG_REVEALED) == 0) {
                val updated = encodePayload(
                    flags = payload.flags or FLAG_REVEALED,
                    iv = payload.iv,
                    ciphertext = payload.ciphertext
                )
                WriteData.run(comm, Ntag424.DATA_FILE_NUMBER, updated)
                payload = readPayload(comm)
            }

            val contentKey = deriveContentKey(uid, bearerSalt, forgeLevel)
            val rawPriv = decryptWithContentKey(contentKey, payload)
            val wif = encodeRaw32ToCompressedWif(rawPriv)

            require(BitcoinValidation.privateKeyMatchesAddress(wif, expectedAddress)) {
                "Decrypted private key does not match the expected address"
            }

            RevealResult(wif)
        }
    }

    // --------------------------------------------------
    // GUARDED REVEAL
    // --------------------------------------------------
    fun revealOfflineBearerFast(
        tag: Tag,
        expectedAddress: String,
        bearerSalt: String?,
        forgeLevel: String?
    ): RevealResult {

        require(BitcoinValidation.isSupportedMainnetAddress(expectedAddress)) {
            "Expected address is invalid"
        }

        // Derive BEFORE opening IsoDep/NFC session.
        val uid = tag.id
        val authKey = deriveAuthKey(uid, bearerSalt, forgeLevel)
        val contentKey = deriveContentKey(uid, bearerSalt, forgeLevel)

        return withComm(tag) { comm ->
            require(AESEncryptionMode.authenticateEV2(comm, Permissions.ACCESS_KEY0, authKey)) {
                "Authentication failed"
            }

            var payload = readPayload(comm)

            // IMPORTANT:
            // Mark revealed BEFORE decrypting/exposing the private key.
            if ((payload.flags and FLAG_REVEALED) == 0) {
                val updated = encodePayload(
                    flags = payload.flags or FLAG_REVEALED,
                    iv = payload.iv,
                    ciphertext = payload.ciphertext
                )

                WriteData.run(comm, Ntag424.DATA_FILE_NUMBER, updated)

                // Re-read the payload after marking revealed.
                payload = readPayload(comm)
            }

            val rawPriv = decryptWithContentKey(contentKey, payload)
            val wif = encodeRaw32ToCompressedWif(rawPriv)

            require(BitcoinValidation.privateKeyMatchesAddress(wif, expectedAddress)) {
                "Decrypted private key does not match the expected address"
            }

            RevealResult(wif)
        }
    }
    fun revealOfflineBearerFast(
        tag: Tag,
        expectedAddress: String,
        authKey: ByteArray,
        contentKey: ByteArray
    ): RevealResult {
        require(BitcoinValidation.isSupportedMainnetAddress(expectedAddress)) {
            "Expected address is invalid"
        }
        require(authKey.size == 16) {
            "Bearer auth key must be 16 bytes"
        }
        require(contentKey.size == 16) {
            "Bearer content key must be 16 bytes"
        }

        return withComm(tag) { comm ->
            require(AESEncryptionMode.authenticateEV2(comm, Permissions.ACCESS_KEY0, authKey)) {
                "Authentication failed"
            }

            var payload = readPayload(comm)

            // Mark revealed BEFORE decrypting/exposing the private key.
            if ((payload.flags and FLAG_REVEALED) == 0) {
                val updated = encodePayload(
                    flags = payload.flags or FLAG_REVEALED,
                    iv = payload.iv,
                    ciphertext = payload.ciphertext
                )

                WriteData.run(comm, Ntag424.DATA_FILE_NUMBER, updated)

                // Re-read the payload after marking revealed.
                payload = readPayload(comm)
            }

            val rawPriv = decryptWithContentKey(contentKey, payload)
            val wif = encodeRaw32ToCompressedWif(rawPriv)

            require(BitcoinValidation.privateKeyMatchesAddress(wif, expectedAddress)) {
                "Decrypted private key does not match the expected address"
            }

            RevealResult(wif)
        }
    }
    fun revealGuarded(
        tag: Tag,
        expectedAddress: String,
        authKey: ByteArray,
        contentKey: ByteArray
    ): RevealResult {
        require(BitcoinValidation.isSupportedMainnetAddress(expectedAddress)) {
            "Expected address is invalid"
        }
        require(authKey.size == 16) {
            "Guarded auth key must be 16 bytes"
        }
        require(contentKey.size == 16) {
            "Guarded content key must be 16 bytes"
        }

        return withComm(tag) { comm ->
            val authenticated = try {
                AESEncryptionMode.authenticateEV2(comm, Permissions.ACCESS_KEY0, authKey)
            } catch (_: Exception) {
                false
            }

            require(authenticated) {
                "Auth Failed. Wrong guarded secret. Please try again."
            }

            var payload = readPayload(comm)

            // IMPORTANT:
            // Mark revealed BEFORE decrypting/exposing the private key.
            if ((payload.flags and FLAG_REVEALED) == 0) {
                val updated = encodePayload(
                    flags = payload.flags or FLAG_REVEALED,
                    iv = payload.iv,
                    ciphertext = payload.ciphertext
                )

                WriteData.run(comm, Ntag424.DATA_FILE_NUMBER, updated)

                // Re-read the payload after marking revealed.
                payload = readPayload(comm)
            }

            val rawPriv = decryptWithContentKey(contentKey, payload)
            val wif = encodeRaw32ToCompressedWif(rawPriv)

            require(BitcoinValidation.privateKeyMatchesAddress(wif, expectedAddress)) {
                "Decrypted private key does not match the expected address"
            }

            RevealResult(wif)
        }
    }

    // --------------------------------------------------
    // PUBLIC RECORD CONTROL
    // --------------------------------------------------

    fun hardenPublicRecordAfterMint(tag: Tag) {
        // No-op:
        // NDEF hardening is already done in phase 1.
    }

    // Old compatibility path
    fun preparePublicRecordForRevealWrite(tag: Tag) {
        preparePublicRecordForRevealWrite(tag, null, null)
    }

    // New bearer path with k + forge level
    fun preparePublicRecordForRevealWrite(tag: Tag, bearerSalt: String?, forgeLevel: String?) {
        val authKey = deriveAuthKey(uid(tag), bearerSalt, forgeLevel)
        preparePublicRecordForRevealWrite(tag, authKey)
    }

    fun preparePublicRecordForRevealWrite(tag: Tag, authKey: ByteArray) {
        require(authKey.size == 16) { "Auth key must be 16 bytes" }

        withComm(tag) { comm ->
            require(AESEncryptionMode.authenticateEV2(comm, Permissions.ACCESS_KEY0, authKey)) {
                "Authentication failed"
            }

            val fs = GetFileSettings.run(comm, Ntag424.NDEF_FILE_NUMBER)
            fs.readPerm = Permissions.ACCESS_EVERYONE
            fs.writePerm = Permissions.ACCESS_EVERYONE
            fs.readWritePerm = Permissions.ACCESS_EVERYONE
            fs.changePerm = Permissions.ACCESS_KEY0
            ChangeFileSettings.run(comm, Ntag424.NDEF_FILE_NUMBER, fs)
        }
    }

    // Old compatibility path
    fun lockPublicRecord(tag: Tag) {
        lockPublicRecord(tag, null, null)
    }

    // New bearer path with k + forge level
    fun lockPublicRecord(tag: Tag, bearerSalt: String?, forgeLevel: String?) {
        val authKey = deriveAuthKey(uid(tag), bearerSalt, forgeLevel)
        lockPublicRecord(tag, authKey)
    }

    fun lockPublicRecord(tag: Tag, authKey: ByteArray) {
        require(authKey.size == 16) { "Auth key must be 16 bytes" }

        withComm(tag) { comm ->
            require(AESEncryptionMode.authenticateEV2(comm, Permissions.ACCESS_KEY0, authKey)) {
                "Authentication failed"
            }

            val fs = GetFileSettings.run(comm, Ntag424.NDEF_FILE_NUMBER)
            fs.readPerm = Permissions.ACCESS_EVERYONE
            fs.writePerm = Permissions.ACCESS_NONE
            fs.readWritePerm = Permissions.ACCESS_NONE
            fs.changePerm = Permissions.ACCESS_NONE
            ChangeFileSettings.run(comm, Ntag424.NDEF_FILE_NUMBER, fs)
        }
    }

    fun assertPublicJsonFits(tag: Tag, json: String) {
        withComm(tag) { comm ->
            val fs = GetFileSettings.run(comm, Ntag424.NDEF_FILE_NUMBER)
            val estimated = estimateNdefTextFileBytes(json)
            require(estimated <= fs.fileSize) {
                "Public JSON is $estimated bytes but NDEF file is only ${fs.fileSize} bytes"
            }
        }
    }

    // --------------------------------------------------
    // PAYLOAD
    // --------------------------------------------------

    private fun encrypt(uid: ByteArray, bearerSalt: String?, forgeLevel: String?, rawPriv: ByteArray): ByteArray {
        require(rawPriv.size == RAW_PRIV_LEN) {
            "Raw private key must be $RAW_PRIV_LEN bytes"
        }

        val contentKey = deriveContentKey(uid, bearerSalt, forgeLevel)
        val iv = ByteArray(GCM_IV_LEN).also { secureRandom.nextBytes(it) }

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(contentKey, "AES"),
            GCMParameterSpec(128, iv)
        )

        val ciphertext = cipher.doFinal(rawPriv)
        require(ciphertext.size == CIPHERTEXT_LEN) {
            "Ciphertext is ${ciphertext.size} bytes, expected $CIPHERTEXT_LEN bytes"
        }

        return encodePayload(
            flags = 0,
            iv = iv,
            ciphertext = ciphertext
        )
    }

    private fun encryptWithContentKey(contentKey: ByteArray, rawPriv: ByteArray): ByteArray {
        require(contentKey.size == 16) {
            "Guarded content key must be 16 bytes"
        }
        require(rawPriv.size == RAW_PRIV_LEN) {
            "Raw private key must be $RAW_PRIV_LEN bytes"
        }

        val iv = ByteArray(GCM_IV_LEN).also { secureRandom.nextBytes(it) }

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(contentKey, "AES"),
            GCMParameterSpec(128, iv)
        )

        val ciphertext = cipher.doFinal(rawPriv)
        require(ciphertext.size == CIPHERTEXT_LEN) {
            "Ciphertext is ${ciphertext.size} bytes, expected $CIPHERTEXT_LEN bytes"
        }

        return encodePayload(
            flags = 0,
            iv = iv,
            ciphertext = ciphertext
        )
    }

    private fun decrypt(uid: ByteArray, bearerSalt: String?, forgeLevel: String?, payload: Payload): ByteArray {
        val contentKey = deriveContentKey(uid, bearerSalt, forgeLevel)
        return decryptWithContentKey(contentKey, payload)
    }

    private fun decryptWithContentKey(contentKey: ByteArray, payload: Payload): ByteArray {
        require(contentKey.size == 16) {
            "Content key must be 16 bytes"
        }

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(contentKey, "AES"),
            GCMParameterSpec(128, payload.iv)
        )

        return try {
            cipher.doFinal(payload.ciphertext)
        } catch (e: Exception) {
            throw IllegalStateException("Protected payload failed integrity/decryption check")
        }
    }

    private fun encodePayload(
        flags: Int,
        iv: ByteArray,
        ciphertext: ByteArray
    ): ByteArray {
        require(iv.size == GCM_IV_LEN) { "IV must be $GCM_IV_LEN bytes" }
        require(ciphertext.size == CIPHERTEXT_LEN) { "Ciphertext must be $CIPHERTEXT_LEN bytes" }

        return byteArrayOf(flags.toByte()) + iv + ciphertext
    }

    private fun readPayload(comm: DnaCommunicator): Payload {
        val fs = GetFileSettings.run(comm, Ntag424.DATA_FILE_NUMBER)
        require(fs.fileSize >= PAYLOAD_LEN) {
            "DATA file is only ${fs.fileSize} bytes but payload requires at least $PAYLOAD_LEN bytes"
        }

        val raw = ReadData.run(comm, Ntag424.DATA_FILE_NUMBER, 0, PAYLOAD_LEN)
        require(raw.size >= PAYLOAD_LEN) {
            "Read ${raw.size} bytes from DATA file, expected at least $PAYLOAD_LEN bytes"
        }

        val flags = raw[0].toInt() and 0xFF
        val iv = raw.copyOfRange(1, 1 + GCM_IV_LEN)
        val ciphertext = raw.copyOfRange(1 + GCM_IV_LEN, PAYLOAD_LEN)

        return Payload(
            flags = flags,
            iv = iv,
            ciphertext = ciphertext
        )
    }

    // --------------------------------------------------
    // KEY DERIVATION (BEARER WITH SALT + FORGE LEVEL)
    // --------------------------------------------------
    fun deriveBearerMaterial(
        context: Context,
        uid: ByteArray,
        bearerSalt: String?,
        forgeLevel: String?
    ): BearerMaterial {
        require(uid.isNotEmpty()) { "UID cannot be empty" }

        return BearerMaterial(
            authKey = deriveAuthKey(uid, bearerSalt, forgeLevel),
            contentKey = deriveContentKey(uid, bearerSalt, forgeLevel)
        )
    }

    fun deriveBearerMaterialWithRounds(
        uid: ByteArray,
        bearerSalt: String?,
        rounds: Int
    ): BearerMaterial {
        require(uid.isNotEmpty()) { "UID cannot be empty" }
        require(rounds >= 1) { "Rounds must be >= 1" }

        val salt = (bearerSalt ?: "").toByteArray(StandardCharsets.UTF_8)

        val master = deriveBearerMasterMaterial(
            uid = uid,
            salt = salt,
            rounds = rounds
        )

        val authKey = hmac(
            master,
            "dnaucash-bearer-auth-v1".toByteArray(StandardCharsets.UTF_8)
        ).copyOf(16)

        val contentKey = hmac(
            master,
            "dnaucash-bearer-content-v1".toByteArray(StandardCharsets.UTF_8)
        ).copyOf(16)

        return BearerMaterial(
            authKey = authKey,
            contentKey = contentKey
        )
    }

    fun ensureBearerBenchmarked(context: Context) {
        val prefs = bearerPrefs(context)
        if (prefs.contains(BEARER_KEY_BENCHMARK_UNITS_PER_SEC)) return

        runBearerUnitWork(1000)

        val roundsPerSample = 4000
        val samples = mutableListOf<Double>()

        repeat(5) {
            val elapsedMs = benchmarkBearerRounds(roundsPerSample)
            val unitsPerSec = roundsPerSample / (elapsedMs / 1000.0)
            samples.add(unitsPerSec)
        }

        val average = samples.average()
        prefs.edit()
            .putFloat(BEARER_KEY_BENCHMARK_UNITS_PER_SEC, average.toFloat())
            .apply()
    }

    private fun deriveBearerMasterMaterial(
        uid: ByteArray,
        salt: ByteArray,
        rounds: Int
    ): ByteArray {
        val seed = BOOTSTRAP + uid + salt
        var out = hmac(
            seed,
            "dnaucash-bearer-seed-v1".toByteArray(StandardCharsets.UTF_8)
        )

        repeat(rounds) {
            out = hmac(seed, out)
        }

        return out
    }

    private fun benchmarkBearerRounds(rounds: Int): Double {
        val start = SystemClock.elapsedRealtimeNanos()
        runBearerUnitWork(rounds)
        val end = SystemClock.elapsedRealtimeNanos()
        return (end - start) / 1_000_000.0
    }

    private fun runBearerUnitWork(rounds: Int) {
        var out = ByteArray(32) { it.toByte() }

        repeat(rounds) {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(BEARER_BENCHMARK_KEY, "HmacSHA256"))
            out = mac.doFinal(out)
        }
    }

    private fun bearerPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(BEARER_PREFS_NAME, Context.MODE_PRIVATE)
    }
    fun deriveAuthKey(uid: ByteArray, bearerSalt: String?, forgeLevel: String?): ByteArray {
        val salt = (bearerSalt ?: "").toByteArray(StandardCharsets.UTF_8)

        val base = hmac(
            BOOTSTRAP,
            "dnaucash-auth-v7".toByteArray(StandardCharsets.US_ASCII) + uid + salt
        )

        return applyForgeWork(base, forgeLevel).copyOf(16)
    }

    fun deriveContentKey(uid: ByteArray, bearerSalt: String?, forgeLevel: String?): ByteArray {
        val salt = (bearerSalt ?: "").toByteArray(StandardCharsets.UTF_8)

        val base = hmac(
            BOOTSTRAP,
            "dnaucash-content-v7".toByteArray(StandardCharsets.US_ASCII) + uid + salt
        )

        return applyForgeWork(base, forgeLevel).copyOf(16)
    }

    private fun applyForgeWork(input: ByteArray, forgeLevel: String?): ByteArray {
        val rounds = when (forgeLevel?.trim()?.lowercase()) {
            PublicCoinRecord.FORGE_CAST -> 1
            PublicCoinRecord.FORGE_FORGED -> 3_000
            PublicCoinRecord.FORGE_TEMPERED -> 10_000
            PublicCoinRecord.FORGE_HARDENED -> 30_000
            else -> 1
        }

        var out = input
        repeat(rounds) {
            out = hmac(out, input)
        }
        return out
    }

    // --------------------------------------------------
    // UID
    // --------------------------------------------------

    private fun uid(tag: Tag): ByteArray {
        val uid = tag.id ?: error("Android did not provide tag UID")
        require(uid.isNotEmpty()) { "Android tag UID is empty" }
        return uid
    }

    // --------------------------------------------------
    // WIF / BASE58CHECK
    // --------------------------------------------------

    private fun decodeCompressedWifToRaw32(wif: String): ByteArray {
        val decoded = base58CheckDecode(wif)

        require(decoded.size == 34) { "Compressed WIF payload must be 34 bytes" }
        require(decoded[0] == 0x80.toByte()) { "Only mainnet WIF is supported" }
        require(decoded[33] == 0x01.toByte()) { "Only compressed WIF is supported" }

        return decoded.copyOfRange(1, 33)
    }

    private fun encodeRaw32ToCompressedWif(raw: ByteArray): String {
        require(raw.size == RAW_PRIV_LEN) { "Raw private key must be 32 bytes" }
        val payload = byteArrayOf(0x80.toByte()) + raw + byteArrayOf(0x01)
        return base58CheckEncode(payload)
    }

    private fun base58CheckEncode(payload: ByteArray): String {
        val checksum = sha256(sha256(payload)).copyOfRange(0, 4)
        return base58Encode(payload + checksum)
    }

    private fun base58CheckDecode(s: String): ByteArray {
        val all = base58Decode(s)
        require(all.size >= 5) { "Base58Check data too short" }

        val payload = all.copyOfRange(0, all.size - 4)
        val checksum = all.copyOfRange(all.size - 4, all.size)
        val expected = sha256(sha256(payload)).copyOfRange(0, 4)

        require(checksum.contentEquals(expected)) { "Base58Check checksum mismatch" }
        return payload
    }

    private val BASE58_ALPHABET =
        "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"

    private fun base58Encode(input: ByteArray): String {
        if (input.isEmpty()) return ""

        var zeros = 0
        while (zeros < input.size && input[zeros].toInt() == 0) zeros++

        val encoded = ArrayList<Char>()
        val num = input.copyOf()

        var startAt = zeros
        while (startAt < num.size) {
            var remainder = 0
            for (i in startAt until num.size) {
                val digit = num[i].toInt() and 0xFF
                val temp = remainder * 256 + digit
                num[i] = (temp / 58).toByte()
                remainder = temp % 58
            }
            encoded.add(BASE58_ALPHABET[remainder])
            while (startAt < num.size && num[startAt].toInt() == 0) {
                startAt++
            }
        }

        repeat(zeros) { encoded.add('1') }

        return encoded.reversed().joinToString("")
    }

    private fun base58Decode(input: String): ByteArray {
        if (input.isEmpty()) return ByteArray(0)

        val input58 = ByteArray(input.length)
        for (i in input.indices) {
            val c = input[i]
            val digit = BASE58_ALPHABET.indexOf(c)
            require(digit >= 0) { "Invalid Base58 character: $c" }
            input58[i] = digit.toByte()
        }

        var zeros = 0
        while (zeros < input58.size && input58[zeros].toInt() == 0) zeros++

        val decoded = ArrayList<Byte>()
        var startAt = zeros
        val temp = input58.copyOf()

        while (startAt < temp.size) {
            var remainder = 0
            for (i in startAt until temp.size) {
                val digit = temp[i].toInt() and 0xFF
                val value = remainder * 58 + digit
                temp[i] = (value / 256).toByte()
                remainder = value % 256
            }
            decoded.add(remainder.toByte())
            while (startAt < temp.size && temp[startAt].toInt() == 0) {
                startAt++
            }
        }

        repeat(zeros) { decoded.add(0) }

        return decoded.reversed().toByteArray()
    }

    // --------------------------------------------------
    // CRYPTO UTILS
    // --------------------------------------------------

    private fun hmac(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }

    private fun sha256(data: ByteArray): ByteArray {
        return MessageDigest.getInstance("SHA-256").digest(data)
    }

    private fun estimateNdefTextFileBytes(text: String): Int {
        val lang = "en".toByteArray(StandardCharsets.US_ASCII)
        val body = text.toByteArray(StandardCharsets.UTF_8)

        val payloadLen = 1 + lang.size + body.size

        val shortRecordOverhead =
            1 + // header
                    1 + // type length
                    1 + // payload length (SR)
                    1   // type "T"

        val ndefMessageLen = shortRecordOverhead + payloadLen
        return 2 + ndefMessageLen
    }

    // --------------------------------------------------
    // NFC TRANSPORT
    // --------------------------------------------------

    private fun <T> withComm(tag: Tag, block: (DnaCommunicator) -> T): T {
        val iso = IsoDep.get(tag) ?: error("Tag does not support IsoDep")
        iso.connect()
        try {
            iso.timeout = 10000
            val comm = DnaCommunicator()
            comm.setTransceiver { bytes -> iso.transceive(bytes) }
            comm.beginCommunication()
            return block(comm)
        } finally {
            runCatching { iso.close() }
        }
    }

    private fun hexToBytes(hex: String): ByteArray {
        require(hex.length % 2 == 0) { "Hex string must have even length" }
        return ByteArray(hex.length / 2) { i ->
            hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }

// --------------------------------------------------
// TAG VALIDATION HELPERS
// --------------------------------------------------

    fun isNtag424(tag: Tag): Boolean {
        val techList = tag.techList
        return techList.any { it.contains("NfcA") } &&
                techList.any { it.contains("IsoDep") }
    }

    fun supportsAuthentication(tag: Tag): Boolean {
        return try {
            IsoDep.get(tag) != null
        } catch (_: Exception) {
            false
        }
    }

    fun hasProtectedFile(tag: Tag): Boolean {
        return try {
            withComm(tag) { comm ->
                val dataFs = GetFileSettings.run(comm, Ntag424.DATA_FILE_NUMBER)
                val ndefFs = GetFileSettings.run(comm, Ntag424.NDEF_FILE_NUMBER)

                dataFs.fileSize > 0 && ndefFs.fileSize > 0
            }
        } catch (_: Exception) {
            false
        }
    }
}