package com.dnaucash.app
 
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.nfc.NfcAdapter
import android.net.Uri
import android.nfc.Tag
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.os.Vibrator
import android.os.VibratorManager
import android.os.Build
import android.os.VibrationEffect
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.dnaucash.app.databinding.ActivityMainBinding
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope
import org.json.JSONObject
object ForgeRounds {
    fun get(forgeLevel: String?): Int {
        return when (forgeLevel) {
            PublicCoinRecord.FORGE_CAST -> 5_000
            PublicCoinRecord.FORGE_FORGED -> 20_000
            PublicCoinRecord.FORGE_TEMPERED -> 75_000
            PublicCoinRecord.FORGE_HARDENED -> 200_000
            else -> 5_000
        }
    }
}
 
class MainActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {
 
    private lateinit var binding: ActivityMainBinding
    private var nfcAdapter: NfcAdapter? = null
 
    private var pendingAction = PendingAction.NONE
    private var scannedRecord: PublicCoinRecord? = null
    private var lastPrivateKey: String? = null
 
    private var mintPhase = 1
    private var pendingMintAddress: String? = null
    private var pendingMintMessage: String? = null
    private var pendingMintPrivateKey: String? = null
 
    private var pendingGuardedSecret: String? = null
    private var pendingGuardedSecretGrade: String? = null
    private var pendingGuardedForgeLevel: String? = null
    private var pendingGuardedAuthKey: ByteArray? = null
    private var pendingGuardedContentKey: ByteArray? = null
 
    private var btcUsdPrice = 0.0
    private var lastKnownSats: Long = 0
    private var pendingBearerForgeLevel: String? = null
    private var pendingBearerSalt: String? = null
 
    private var pendingRevealAuthKey: ByteArray? = null
    private var pendingRevealContentKey: ByteArray? = null
 
    private var activeGuardedPopup: AlertDialog? = null
 
    private var pendingBearerAuthKey: ByteArray? = null
    private var pendingBearerContentKey: ByteArray? = null
 
    private var pendingStealthPublicMessage: String? = null
    private var pendingStealthForgeLevel: String? = null
 
    private var balanceSats: Long = 0
 
    private var balanceMode: Int = 0
 
    private var balanceLoaded: Boolean = false
 
    enum class PendingAction {
        NONE,
        PROGRAM,
        REVEAL,
        INSPECT_STEALTH
    }
 
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
 
        binding.textBalance.setOnClickListener {
            if (!balanceLoaded) return@setOnClickListener
 
            balanceMode = (balanceMode + 1) % 4
            updateBalanceDisplay()
        }
 
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
 
        binding.buttonInfo.setOnClickListener {
            startActivity(Intent(this, InfoMenuActivity::class.java))
        }
 
        binding.buttonTabCreate.setOnClickListener {
            setTab(true)
            updateLogMintToggleVisibility()
        }
 
        binding.buttonTabScan.setOnClickListener {
            setTab(false)
            updateLogMintToggleVisibility()
        }
 
        setTab(true)
        updateLogMintToggleVisibility()
 
        binding.radioTokenBearer.setOnCheckedChangeListener { _, _ ->
            updateTokenModeUi()
        }
 
        binding.radioTokenGuarded.setOnCheckedChangeListener { _, _ ->
            updateTokenModeUi()
        }
 
        binding.radioTokenStealth.setOnCheckedChangeListener { _, _ ->
            updateTokenModeUi()
        }
 
        binding.radioGroupStealthStyle.setOnCheckedChangeListener { _, _ ->
            updateTokenModeUi()
        }
 
        binding.inputMessage.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val count = s?.length ?: 0
                binding.textMessageCount.text = "$count / 48"
            }
 
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
 
        binding.inputStealthPublicMessage.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val count = s?.length ?: 0
                binding.textStealthPublicMessageCount.text = "$count / 160"
            }
 
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
 
        binding.inputGuardedSecret.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                updateSecretGradePreview()
            }
 
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
 
        binding.buttonProgramTag.setOnClickListener {
            val error = validateCreateFields()
            if (error != null) {
                binding.textCreateStatus.text = error
                return@setOnClickListener
            }
 
            pendingMintAddress = binding.inputAddress.text.toString().trim()
            pendingMintMessage = binding.inputMessage.text.toString()
            pendingMintPrivateKey = binding.inputPrivateKey.text.toString().trim()
 
            if (isGuardedSelected() || isStealthSelected()) {
                val secret = binding.inputGuardedSecret.text.toString()
                val confirm = binding.inputGuardedSecretConfirm.text.toString()
                if (secret != confirm) {
                    binding.textCreateStatus.text = "Guarded secret confirmation does not match."
                    return@setOnClickListener
                }
 
                pendingGuardedSecret = secret
                pendingGuardedSecretGrade = SecretGradeEstimator.estimate(secret)
                pendingGuardedForgeLevel = selectedForgeLevel()
                pendingBearerForgeLevel = null
                pendingBearerSalt = null
                pendingBearerAuthKey = null
                pendingBearerContentKey = null
 
                pendingStealthPublicMessage = null
                pendingStealthForgeLevel = null
 
                val seconds = GuardedKdf.estimatedSeconds(this, pendingGuardedForgeLevel)
 
                showGuardedProgressPopup(
                    title = if (isStealthSelected()) "Preparing Stealth Coin" else "Preparing Guarded Coin",
                    seconds = seconds,
                    stageProvider = { remaining ->
                        GuardedKdf.describeCountdownStage(
                            remaining,
                            pendingGuardedForgeLevel ?: PublicCoinRecord.FORGE_CAST
                        )
                    },
                    finalizingText = "Finalizing the alloy...",
                    work = {
                        val salt = guardedSalt(
                            address = pendingMintAddress ?: error("Missing mint address"),
                            message = pendingMintMessage ?: "",
                            forgeLevel = pendingGuardedForgeLevel ?: PublicCoinRecord.FORGE_CAST
                        )
 
 
                        val rounds = ForgeRounds.get(pendingGuardedForgeLevel)
 
                        val derived = GuardedKdf.deriveWithRounds(
                            password = pendingGuardedSecret ?: error("Missing secret"),
                            uid = salt,
                            salt = salt,
                            rounds = rounds
                        )
 
                        pendingGuardedAuthKey = derived.authKey
                        pendingGuardedContentKey = derived.contentKey
                    },
                    onReady = {
                        mintPhase = 1
                        pendingAction = PendingAction.PROGRAM
                        binding.textCreateStatus.text =
                            if (isStealthSelected())
                                "Tap stealth token and hold it against device."
                            else
                                "Tap token and hold it against device."
                    },
                    onFailure = { msg ->
                        binding.textCreateStatus.text = msg
                        clearMintState()
                    }
                )
            } else {
                // ---- BEARER FLOW (3 TAP IMPLEMENTATION) ----
 
                pendingGuardedSecret = null
                pendingGuardedSecretGrade = null
                pendingGuardedForgeLevel = null
                pendingGuardedAuthKey = null
                pendingGuardedContentKey = null
 
                pendingBearerForgeLevel = selectedForgeLevel()
                pendingBearerSalt = randomBearerSaltHex()
                pendingBearerAuthKey = null
                pendingBearerContentKey = null
 
                mintPhase = 1
                pendingAction = PendingAction.PROGRAM
 
                binding.textCreateStatus.text =
                    "Tap token and hold it against device.\n(Detect coin phase)"
            }
        }
 
        binding.buttonReveal.setOnClickListener {
            val record = scannedRecord ?: return@setOnClickListener
 
            if (record.tokenType == PublicCoinRecord.TOKEN_TYPE_STEALTH) {
                if (pendingRevealAuthKey == null || pendingRevealContentKey == null) {
                    binding.textRevealStatus.text =
                        "Scan and inspect this stealth token first, then reveal."
                    return@setOnClickListener
                }
 
                AlertDialog.Builder(this)
                    .setTitle("Reveal Stealth Token")
                    .setMessage(
                        "You are revealing a Stealth token.\n\n" +
                                "Once revealed, the private key can spend the funds.\n\n" +
                                "This action is permanent."
                    )
                    .setPositiveButton("Continue") { _, _ ->
                        pendingAction = PendingAction.REVEAL
                        binding.textRevealStatus.text = "Tap stealth token and hold it to reveal."
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
                return@setOnClickListener
            }
 
            if (record.tokenType == PublicCoinRecord.TOKEN_TYPE_GUARDED) {
                promptForGuardedReveal(record)
                return@setOnClickListener
            }
 
            val forgeLevel = record.forgeLevel ?: PublicCoinRecord.FORGE_CAST
            val forgeLabel = WorkTierEstimator.displayLabel(forgeLevel)
            val estimatedSeconds = WorkTierEstimator.estimateSeconds(this, forgeLevel)
 
            if (record.s == PublicCoinRecord.STATE_REVEALED) {
                showGuardedProgressPopup(
                    title = "Revealing $forgeLabel Bearer Coin",
                    seconds = estimatedSeconds,
                    stageProvider = { remaining ->
                        describeRevealStage(remaining, forgeLevel)
                    },
                    finalizingText = "Unearthing the contents...",
                    work = {},
                    onReady = {
                        pendingAction = PendingAction.REVEAL
                        binding.textRevealStatus.text = "Tap token and hold it against device."
                    },
                    onFailure = { msg ->
                        binding.textRevealStatus.text = msg
                        pendingAction = PendingAction.NONE
                    }
                )
                return@setOnClickListener
            }
 
            AlertDialog.Builder(this)
                .setTitle("Reveal Bearer Token")
                .setMessage(
                    "You are revealing a Bearer token.\n\n" +
                            "Once revealed, the private key can spend the funds.\n\n" +
                            "This action is permanent.\n\n" +
                            "Forge Level: $forgeLabel\n\n" +
                            "You will be prompted to tap and hold the token."
                )
                .setPositiveButton("Continue") { _, _ ->
                    showGuardedProgressPopup(
                        title = "Revealing $forgeLabel Bearer Coin",
                        seconds = estimatedSeconds,
                        stageProvider = { remaining ->
                            describeRevealStage(remaining, forgeLevel)
                        },
                        finalizingText = "Unearthing the contents...",
                        work = {},
                        onReady = {
                            pendingAction = PendingAction.REVEAL
                            binding.textRevealStatus.text = "Tap token and hold it against device."
                        },
                        onFailure = { msg ->
                            binding.textRevealStatus.text = msg
                            pendingAction = PendingAction.NONE
                        }
                    )
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
 
        binding.buttonCopyAddress.setOnClickListener {
            copy("Address", scannedRecord?.a ?: "")
        }
 
        binding.buttonCopyPrivateKey.setOnClickListener {
            copy("Private Key", lastPrivateKey ?: "")
        }
 
        binding.buttonOpenMempool.setOnClickListener {
            val addr = scannedRecord?.a ?: return@setOnClickListener
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://mempool.space/address/$addr")))
        }
 
        updateTokenModeUi()
        updateSecretGradePreview()
        binding.textMessageCount.text = "${binding.inputMessage.text?.length ?: 0} / 48"
        binding.textStealthPublicMessageCount.text =
            "${binding.inputStealthPublicMessage.text?.length ?: 0} / 160"
        fetchBtcPrice()
    }
 
    override fun onResume() {
        super.onResume()
        updateLogMintToggleVisibility()
        nfcAdapter?.enableReaderMode(this, this, NfcAdapter.FLAG_READER_NFC_A, null)
    }
 
    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
    }
 
    override fun onDestroy() {
        dismissGuardedPopup()
        super.onDestroy()
    }
 
    override fun onTagDiscovered(tag: Tag) {
 
        if (!Ntag424Service.isNtag424(tag)) {
            runOnUiThread {
                binding.textRevealStatus.text =
                    "Invalid tag type.\nDNAuCash requires NTAG424 DNA."
                Toast.makeText(
                    this,
                    "Invalid tag type. NTAG424 DNA required.",
                    Toast.LENGTH_LONG
                ).show()
            }
            return
        }
 
        if (!Ntag424Service.supportsAuthentication(tag)) {
            runOnUiThread {
                binding.textRevealStatus.text =
                    "Tag does not support authentication."
                Toast.makeText(
                    this,
                    "Tag does not support authentication.",
                    Toast.LENGTH_LONG
                ).show()
            }
            return
        }
 
        runOnUiThread {
 
            // CLOSE ONLY OLD POPUPS BEFORE ACTION
            dismissGuardedPopup()
 
            when (pendingAction) {
                PendingAction.PROGRAM -> programTag(tag)
                PendingAction.REVEAL -> reveal(tag)
                PendingAction.INSPECT_STEALTH -> inspectStealth(tag)
                PendingAction.NONE -> scan(tag)
            }
 
            // DO NOT DISMISS AFTER
        }
    }
 
    private fun dismissGuardedPopup() {
        activeGuardedPopup?.dismiss()
        activeGuardedPopup = null
    }
 
    private fun setTab(create: Boolean) {
        binding.layoutCreateSection.visibility = if (create) View.VISIBLE else View.GONE
        binding.layoutScanSection.visibility = if (create) View.GONE else View.VISIBLE
 
        if (create) {
            binding.buttonTabCreate.setBackgroundResource(R.drawable.bg_button_gold)
            binding.buttonTabCreate.setTextColor(
                ContextCompat.getColor(this, R.color.dnaucash_black)
            )
 
            binding.buttonTabScan.setBackgroundResource(R.drawable.bg_button_dark)
            binding.buttonTabScan.setTextColor(
                ContextCompat.getColor(this, R.color.dnaucash_gold)
            )
        } else {
            binding.buttonTabScan.setBackgroundResource(R.drawable.bg_button_gold)
            binding.buttonTabScan.setTextColor(
                ContextCompat.getColor(this, R.color.dnaucash_black)
            )
 
            binding.buttonTabCreate.setBackgroundResource(R.drawable.bg_button_dark)
            binding.buttonTabCreate.setTextColor(
                ContextCompat.getColor(this, R.color.dnaucash_gold)
            )
        }
    }
 
    private fun updateTokenModeUi() {
        val guarded = isGuardedSelected()
        val stealth = isStealthSelected()
 
        binding.layoutGuardedSection.visibility =
            if (guarded || stealth) View.VISIBLE else View.GONE
 
        binding.layoutStealthSection.visibility =
            if (stealth) View.VISIBLE else View.GONE
 
        binding.textCreateModeSummary.text = when {
            stealth -> "Mint cash-like secret Bitcoins. Looks like a generic tag to most  NFC readers. This app, the token & password are required to sweep funds."
            guarded -> "Mint cash-like Password Protected Bitcoins. The token & password are required to sweep funds."
            else -> "Mint cash-like Bitcoins. Whoever holds it can immediately sweep the funds."
        }
        // --- STEALTH UI CLEANUP ---
 
// Hide normal 48-char message field for stealth
        binding.inputMessage.visibility = if (stealth) View.GONE else View.VISIBLE
        binding.textMessageCount.visibility = if (stealth) View.GONE else View.VISIBLE
 
// Only allow typing in stealth public message if "Custom" is selected
        val customStealthSelected = stealth && binding.radioStealthCustom.isChecked
 
        binding.inputStealthPublicMessage.isEnabled = customStealthSelected
        binding.inputStealthPublicMessage.alpha = if (customStealthSelected) 1.0f else 0.4f
 
        binding.textStealthPublicMessageCount.alpha = if (customStealthSelected) 1.0f else 0.4f
    }
 
    private fun updateSecretGradePreview() {
        val secret = binding.inputGuardedSecret.text?.toString().orEmpty()
        val grade = SecretGradeEstimator.estimate(secret)
        binding.textSecretGradeValue.text = SecretGradeEstimator.displayLabel(grade)
        binding.textSecretGradeDescription.text = SecretGradeEstimator.description(grade)
    }
 
    private fun validateCreateFields(): String? {
        val addr = binding.inputAddress.text.toString().trim()
        val key = binding.inputPrivateKey.text.toString().trim()
        val msg = binding.inputMessage.text.toString()
 
        if (!BitcoinValidation.isSupportedMainnetAddress(addr)) {
            return "Invalid address"
        }
 
        if (!BitcoinValidation.isSupportedPrivateKeyFormat(key)) {
            return "Invalid private key"
        }
 
        if (!BitcoinValidation.privateKeyMatchesAddress(key, addr)) {
            return "Key does not match address"
        }
 
        if (msg.length > 48) {
            return "Message must be 48 characters or less."
        }
 
        if (!AsciiUtils.isAscii(msg)) {
            return "Message must be ASCII only."
        }
        if (isStealthSelected() && binding.radioStealthCustom.isChecked) {
            val stealthPublicMessage = binding.inputStealthPublicMessage.text.toString()
 
            if (stealthPublicMessage.isBlank()) {
                return "Custom public message is required for Stealth custom mode."
            }
 
            if (stealthPublicMessage.length > 160) {
                return "Stealth public message must be 160 characters or less."
            }
 
            if (!AsciiUtils.isAscii(stealthPublicMessage)) {
                return "Stealth public message must be ASCII only."
            }
        }
        if (isGuardedSelected() || isStealthSelected()) {
            val secret = binding.inputGuardedSecret.text.toString()
            val confirm = binding.inputGuardedSecretConfirm.text.toString()
 
            if (secret.isBlank()) {
                return "Guarded secret is required."
            }
 
            if (confirm.isBlank()) {
                return "Please confirm the guarded secret."
            }
 
            if (secret != confirm) {
                return "Guarded secret confirmation does not match."
            }
        }
 
        return null
    }
 
    private fun programTag(tag: Tag) {
        try {
            val address = pendingMintAddress ?: binding.inputAddress.text.toString().trim()
            val message = pendingMintMessage ?: binding.inputMessage.text.toString()
            val privateKey = pendingMintPrivateKey ?: binding.inputPrivateKey.text.toString().trim()
 
            if (pendingGuardedForgeLevel != null || isStealthSelected()) {
                val secretGrade = pendingGuardedSecretGrade ?: error("Missing guarded secret grade")
                val forgeLevel = pendingGuardedForgeLevel ?: error("Missing guarded forge level")
 
                val record = if (isStealthSelected()) {
                    val stealthMessage = stealthPublicMessageForSelectedOption()
 
                    PublicCoinRecordCodec.newUnrevealedStealth(
                        address = address,
                        message = stealthMessage,
                        secretGrade = secretGrade,
                        forgeLevel = forgeLevel
                    )
                } else {
                    PublicCoinRecordCodec.newUnrevealedGuarded(
                        address = address,
                        message = message,
                        secretGrade = secretGrade,
                        forgeLevel = forgeLevel
                    )
                }
                val publicJson = PublicCoinRecordCodec.toUnrevealedJson(record)
 
                if (mintPhase == 1) {
                    binding.textCreateStatus.text = "Guarded mint phase 1/2: preflight checks..."
                    Ntag424Service.preflightMintGuarded(tag, publicJson)
 
                    binding.textCreateStatus.text = "Guarded mint phase 1/2: writing public record..."
                    if (record.isStealth) {
                        val publicMessage = stealthPublicMessageForSelectedOption()
 
                        val stealthSalt = stealthV2Salt(
                            uid = tag.id,
                            publicMessage = publicMessage,
                            forgeLevel = forgeLevel
                        )
 
 
                        NdefUtils.writeText(tag, publicMessage)
 
                    } else {
                        NdefUtils.writeText(tag, publicJson)
                    }
 
                    binding.textCreateStatus.text = "Guarded mint phase 1/2: writing protected payload..."
                    Ntag424Service.mintGuardedPhase1(
                        tag = tag,
                        privateKey = privateKey,
                        contentKey = pendingGuardedContentKey ?: error("Missing guarded content key")
                    )
 
                    if (record.isStealth) {
                        binding.textCreateStatus.text = "Stealth mint phase 1/2: writing protected metadata..."
                        Ntag424Service.mintStealthMetadata(
                            tag = tag,
                            address = address,
                            message = "",
                            tokenType = PublicCoinRecord.TOKEN_TYPE_STEALTH,
                            forgeLevel = forgeLevel,
                            authKey = pendingGuardedAuthKey ?: error("Missing stealth auth key")
                        )
                    }
 
                    mintPhase = 2
                    pendingAction = PendingAction.PROGRAM
                    vibrateSuccess()
                    binding.textCreateStatus.text =
                        "Safe to remove.\nRemove token and tap & hold again to finalize."
                } else {
                    binding.textCreateStatus.text = "Guarded mint phase 2/2: finalizing auth key..."
                    Ntag424Service.mintGuardedPhase2(
                        tag = tag,
                        authKey = pendingGuardedAuthKey ?: error("Missing guarded auth key")
                    )
 
                    if (binding.switchLogMintedCoins.isChecked && CoinLogStore.isDatabaseEnabled(this)) {
                        val uid = tag.id.joinToString("") { "%02x".format(it) }
 
                        CoinLogStore.logMintedCoin(
                            context = this,
                            uid = uid,
                            record = record,
                            privateKey = if (CoinLogStore.isPrivateKeyLoggingEnabled(this)) pendingMintPrivateKey else null
                        )
                    }
 
                    clearMintState()
                    vibrateSuccess()
                    binding.textCreateStatus.text = "Safe to remove token.\nGuarded token minted."
                }
                return
            }
 
            val bearerForge = pendingBearerForgeLevel ?: error("Missing bearer forge level")
            val bearerSalt = pendingBearerSalt ?: error("Missing bearer salt")
 
            val record = PublicCoinRecordCodec.newUnrevealedBearer(
                address = address,
                message = message,
                forgeLevel = bearerForge,
                k = bearerSalt
            )
 
            val publicJson = PublicCoinRecordCodec.toUnrevealedJson(record)
 
            if (mintPhase == 1) {
 
                    // ✅ Tap 1 = UID ONLY
                    val uid = tag.id
 
                    binding.textCreateStatus.text = "Preparing security..."
 
                    showGuardedProgressPopup(
                        title = "Forging Bearer Security",
                        seconds = WorkTierEstimator.estimateSeconds(this, bearerForge),
                        stageProvider = { remaining ->
                            GuardedKdf.describeCountdownStage(remaining, bearerForge)
                        },
                        finalizingText = "Finalizing the alloy...",
                        work = {
                            val material = Ntag424Service.deriveBearerMaterial(
                                context = this,
                                uid = uid,
                                bearerSalt = bearerSalt,
                                forgeLevel = bearerForge
                            )
 
                            pendingBearerAuthKey = material.authKey
                            pendingBearerContentKey = material.contentKey
                        },
                        onReady = {
                            mintPhase = 2
                            pendingAction = PendingAction.PROGRAM
 
                            binding.textCreateStatus.text =
                                "Tap token again and hold it to write coin."
                        },
                        onFailure = { msg ->
                            binding.textCreateStatus.text = msg
                            clearMintState()
                        }
                    )
 
                    return
            } else if (mintPhase == 2) {
 
                binding.textCreateStatus.text = "Hold token steady while writing coin..."
 
                // 🔥 NOW we do ALL writes here
                Ntag424Service.preflightMintBearer(tag, privateKey, publicJson)
                NdefUtils.writeText(tag, publicJson)
                Ntag424Service.mintPhase1WithContentKey(
                    tag = tag,
                    privateKey = privateKey,
                    contentKey = pendingBearerContentKey ?: error("Missing derived content key")
                )
 
                binding.textCreateStatus.text = "Continue holding... applying security..."
 
                Ntag424Service.mintPhase2WithPrecomputed(
                    tag,
                    pendingBearerAuthKey ?: error("Missing derived auth key")
                )
 
                if (binding.switchLogMintedCoins.isChecked && CoinLogStore.isDatabaseEnabled(this)) {
                    val uid = tag.id.joinToString("") { "%02x".format(it) }
 
                    CoinLogStore.logMintedCoin(
                        context = this,
                        uid = uid,
                        record = record,
                        privateKey = if (CoinLogStore.isPrivateKeyLoggingEnabled(this)) pendingMintPrivateKey else null
                    )
                }
 
                clearMintState()
 
                vibrateSuccess()
                binding.textCreateStatus.text =
                    "Safe to remove token.\nBearer token minted."
            } else {
 
                // Tap 3 = just scan
                clearMintState()
 
                binding.textCreateStatus.text = "Bearer token minted and verified."
            }
 
        } catch (e: Exception) {
            binding.textCreateStatus.text = e.message ?: "Minting failed"
            clearMintState()
        }
    }
 
    private fun scan(tag: Tag) {
        try {
            binding.textRevealStatus.text = "Scan step 1/3: reading public NDEF..."
            val text = try {
                NdefUtils.readText(tag) ?: error("No public NDEF record found")
            } catch (e: Exception) {
                throw IllegalStateException("Scan step 1/3 failed: read public NDEF: ${e.message}", e)
            }
 
            binding.textRevealStatus.text = "Scan step 2/3: parsing public record..."
 
            val publicRecord = try {
                PublicCoinRecordCodec.parse(text)
            } catch (e: Exception) {
                val forgeLevel = stealthForgeFromPublicMessage(text)
                    ?: throw IllegalStateException("Scan step 2/3 failed: not a DNAuCash token")
 
                promptForStealthInspect(
                    publicMessage = text,
                    uid = tag.id,
                    forgeLevel = forgeLevel
                )
                return
            }
 
            val record = publicRecord
            fetchBalanceForAddress(record.a)
 
            scannedRecord = record
 
            lastPrivateKey = null
 
            binding.textScannedAddress.text = record.a
            binding.textScannedMessage.text = if (record.m.isBlank()) "-" else record.m
            binding.textScannedHelp.text = when {
                !record.h.isNullOrBlank() -> record.h
                !record.w.isNullOrBlank() -> record.w
                record.tokenType == PublicCoinRecord.TOKEN_TYPE_GUARDED -> buildGuardedSummary(record)
                else -> buildBearerSummary(record)
            }
            binding.textRevealStatus.text = "Tag scanned."
 
            updateState(record.s)
            setTab(false)
 
        } catch (e: Exception) {
            val msg = e.message ?: "Scan failed"
            binding.textRevealStatus.text = msg
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
        }
    }
 
    private fun inspectStealth(tag: Tag) {
        try {
            val publicMessage = pendingStealthPublicMessage
                ?: error("Missing stealth public message. Scan the token again.")
            val forgeLevel = pendingStealthForgeLevel
                ?: error("Missing stealth forge level. Scan the token again.")
            val authKey = pendingRevealAuthKey
                ?: error("Missing stealth auth key. Scan the token again.")
            val contentKey = pendingRevealContentKey
                ?: error("Missing stealth content key. Scan the token again.")
 
            binding.textRevealStatus.text = "Inspecting stealth metadata..."
 
            val metadataJson = Ntag424Service.readStealthMetadata(
                tag = tag,
                authKey = authKey
            )
 
            val record = PublicCoinRecordCodec.parse(metadataJson)
                .copy(
                    tokenType = PublicCoinRecord.TOKEN_TYPE_STEALTH,
                    forgeLevel = forgeLevel
                )
 
            scannedRecord = record
            lastPrivateKey = null
 
            fetchBalanceForAddress(record.a)
 
            binding.textScannedAddress.text = record.a
            binding.textScannedMessage.text = if (record.m.isBlank()) "-" else record.m
            binding.textScannedHelp.text = buildGuardedSummary(record)
            binding.textRevealStatus.text = "Stealth token inspected."
 
            updateState(record.s)
            setTab(false)
 
            pendingAction = PendingAction.NONE
            pendingStealthPublicMessage = publicMessage
            pendingStealthForgeLevel = forgeLevel
            pendingRevealAuthKey = authKey
            pendingRevealContentKey = contentKey
 
        } catch (e: Exception) {
            val msg = e.message ?: "Stealth inspect failed"
            binding.textRevealStatus.text = msg
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
 
            pendingAction = PendingAction.NONE
            pendingStealthPublicMessage = null
            pendingStealthForgeLevel = null
            pendingRevealAuthKey = null
            pendingRevealContentKey = null
        }
    }
    private fun reveal(tag: Tag) {
        val record = scannedRecord ?: return
        val alreadyRevealed = record.s == PublicCoinRecord.STATE_REVEALED
 
        try {
            if (record.tokenType == PublicCoinRecord.TOKEN_TYPE_GUARDED ||
                record.tokenType == PublicCoinRecord.TOKEN_TYPE_STEALTH
            ) {
                val authKey = pendingRevealAuthKey ?: error("Missing guarded reveal auth key")
                val contentKey = pendingRevealContentKey ?: error("Missing guarded reveal content key")
 
                binding.textRevealStatus.text = if (alreadyRevealed) {
                    "Read step 1/1: decrypting private key..."
                } else {
                    "Reveal step 1/4: decrypting protected payload..."
                }
 
                val result = try {
                    Ntag424Service.revealGuarded(
                        tag = tag,
                        expectedAddress = record.a,
                        authKey = authKey,
                        contentKey = contentKey
                    )
                } catch (e: Exception) {
                    val msg = e.message ?: "Reveal failed"
                    if (msg.contains("Auth Failed", ignoreCase = true)) {
                        throw IllegalStateException("Auth Failed. Wrong guarded secret. Please try again.", e)
                    }
 
                    throw IllegalStateException(
                        if (alreadyRevealed) {
                            "Read step 1/1 failed: $msg"
                        } else {
                            "Reveal step 1/4 failed: $msg"
                        },
                        e
                    )
                }
 
                val clean = result.privateKey.trim()
                if (record.tokenType == PublicCoinRecord.TOKEN_TYPE_STEALTH) {
 
                    binding.textRevealStatus.text = "Updating stealth state..."
 
                    Ntag424Service.markStealthMetadataRevealed(
                        tag = tag,
                        authKey = authKey
                    )
 
                    val updated = PublicCoinRecordCodec.toRevealed(record)
 
                    scannedRecord = updated
                    updateState(PublicCoinRecord.STATE_REVEALED)
                    lastPrivateKey = clean
                    binding.textRevealStatus.text = "Stealth token revealed.\n\n$clean"
 
                    pendingAction = PendingAction.NONE
                    return
                }
                val updated = PublicCoinRecordCodec.toRevealed(record)
                val revealedJson = PublicCoinRecordCodec.toRevealedJson(updated)
 
                if (!alreadyRevealed) {
                    binding.textRevealStatus.text = "Reveal step 2/4: preparing public record for final write..."
                    Ntag424Service.preparePublicRecordForRevealWrite(tag, authKey)
 
                    binding.textRevealStatus.text = "Reveal step 3/4: checking revealed JSON size..."
                    Ntag424Service.assertPublicJsonFits(tag, revealedJson)
 
                    binding.textRevealStatus.text = "Reveal step 4/4: writing and locking revealed record..."
                    NdefUtils.writeText(tag, revealedJson)
                    Ntag424Service.lockPublicRecord(tag, authKey)
 
                    scannedRecord = updated
                    binding.textScannedHelp.text = updated.w ?: "-"
                    updateState(PublicCoinRecord.STATE_REVEALED)
                    lastPrivateKey = clean
                    vibrateSuccess()
                    binding.textRevealStatus.text = "Safe to remove.\n\n$clean"
                } else {
                    lastPrivateKey = clean
                    binding.textRevealStatus.text = "Private key:\n\n$clean"
                }
 
                pendingRevealAuthKey = null
                pendingRevealContentKey = null
                pendingAction = PendingAction.NONE
                return
            }
 
            val updated = PublicCoinRecordCodec.toRevealed(record)
            val revealedJson = PublicCoinRecordCodec.toRevealedJson(updated)
 
            binding.textRevealStatus.text = if (alreadyRevealed) {
                "Read step 1/1: decrypting private key..."
            } else {
                "Reveal step 1/4: decrypting protected payload..."
            }
 
            val material = try {
                binding.textRevealStatus.text = "Reveal step 1/4: deriving bearer security..."
 
                Ntag424Service.deriveBearerMaterial(
                    context = this,
                    uid = tag.id,
                    bearerSalt = record.k,
                    forgeLevel = record.forgeLevel
                )
            } catch (e: Exception) {
                throw IllegalStateException("Reveal step 1/4 failed: derive bearer security: ${e.message}", e)
            }
 
            val result = try {
                binding.textRevealStatus.text = if (alreadyRevealed) {
                    "Read step 1/1: decrypting private key..."
                } else {
                    "Reveal step 1/4: decrypting protected payload..."
                }
 
                Ntag424Service.revealOfflineBearerFast(
                    tag = tag,
                    expectedAddress = record.a,
                    authKey = material.authKey,
                    contentKey = material.contentKey
                )
            } catch (e: Exception) {
                throw IllegalStateException(
                    if (alreadyRevealed) {
                        "Read step 1/1 failed: ${e.message}"
                    } else {
                        "Reveal step 1/4 failed: ${e.message}"
                    },
                    e
                )
            }
            val clean = result.privateKey.trim()
 
            if (!alreadyRevealed) {
                binding.textRevealStatus.text = "Reveal step 2/4: preparing public record for final write..."
                try {
                    Ntag424Service.preparePublicRecordForRevealWrite(
                        tag = tag,
                        authKey = material.authKey
                    )
                } catch (e: Exception) {
                    throw IllegalStateException("Reveal step 2/4 failed: ${e.message}", e)
                }
 
                binding.textRevealStatus.text = "Reveal step 3/4: checking revealed JSON size..."
                try {
                    Ntag424Service.assertPublicJsonFits(tag, revealedJson)
                } catch (e: Exception) {
                    throw IllegalStateException("Reveal step 3/4 failed: ${e.message}", e)
                }
 
                binding.textRevealStatus.text = "Reveal step 4/4: writing and locking revealed record..."
                try {
                    NdefUtils.writeText(tag, revealedJson)
                    Ntag424Service.lockPublicRecord(
                        tag = tag,
                        authKey = material.authKey
                    )
                } catch (e: Exception) {
                    throw IllegalStateException("Reveal step 4/4 failed: ${e.message}", e)
                }
 
                scannedRecord = updated
                binding.textScannedHelp.text = updated.w ?: "-"
                updateState(PublicCoinRecord.STATE_REVEALED)
                lastPrivateKey = clean
                binding.textRevealStatus.text = "Coin revealed and permanently locked.\n\n$clean"
            } else {
                lastPrivateKey = clean
                binding.textRevealStatus.text = "Private key:\n\n$clean"
            }
 
            pendingAction = PendingAction.NONE
 
        } catch (e: Exception) {
            dismissGuardedPopup()
            val msg = e.message ?: "Reveal failed"
            binding.textRevealStatus.text = msg
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            pendingRevealAuthKey = null
            pendingRevealContentKey = null
            pendingAction = PendingAction.NONE
        }
    }
 
    private fun promptForGuardedReveal(record: PublicCoinRecord) {
        val input = EditText(this)
        input.hint = "Enter guarded secret"
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
 
        val forgeLevel = record.forgeLevel ?: PublicCoinRecord.FORGE_CAST
        val forgeLabel = WorkTierEstimator.displayLabel(forgeLevel)
        val estimatedSeconds = GuardedKdf.estimatedSeconds(this, forgeLevel)
 
        AlertDialog.Builder(this)
            .setTitle("Reveal $forgeLabel Token")
            .setMessage(
                "You are revealing a $forgeLabel token.\n\n" +
                        "This will take an estimated $estimatedSeconds second" +
                        if (estimatedSeconds == 1) "" else "s" +
                                " on this device.\n\n" +
                                "You will be prompted to tap after the countdown."
            )
            .setView(input)
            .setPositiveButton("Continue") { _, _ ->
                val secret = input.text?.toString().orEmpty()
                if (secret.isBlank()) {
                    binding.textRevealStatus.text = "Guarded secret is required."
                    return@setPositiveButton
                }
 
                showGuardedProgressPopup(
                    title = "Revealing Guarded Coin",
                    seconds = estimatedSeconds,
                    stageProvider = { remaining ->
                        describeRevealStage(remaining, forgeLevel)
                    },
                    finalizingText = "Unearthing the contents...",
                    work = {
                        val salt = guardedSalt(
                            address = record.a,
                            message = record.m,
                            forgeLevel = forgeLevel
                        )
 
 
 
                        val rounds = ForgeRounds.get(forgeLevel)
 
                        val derived = GuardedKdf.deriveWithRounds(
                            password = secret,
                            uid = salt,
                            salt = salt,
                            rounds = rounds
                        )
 
                        pendingRevealAuthKey = derived.authKey
                        pendingRevealContentKey = derived.contentKey
                    },
                    onReady = {
                        pendingAction = PendingAction.REVEAL
                        binding.textRevealStatus.text = "Tap token and hold it against device."
                    },
                    onFailure = { msg ->
                        binding.textRevealStatus.text = msg
                        pendingRevealAuthKey = null
                        pendingRevealContentKey = null
                        pendingAction = PendingAction.NONE
                    }
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
 
    private fun showGuardedProgressPopup(
        title: String,
        seconds: Int,
        stageProvider: (Int) -> String,
        finalizingText: String,
        work: () -> Unit,
        onReady: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        dismissGuardedPopup()
 
        val titleView = TextView(this).apply {
            text = title
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
        }
 
        val stageView = TextView(this).apply {
            text = stageProvider(seconds)
            textSize = 18f
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 12)
        }
 
 
        val progressBar = ProgressBar(this).apply {
            visibility = View.VISIBLE
            isIndeterminate = true
        }
 
        val subtextView = TextView(this).apply {
            text = estimateTimeRange(seconds)
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(0, 18, 0, 0)
        }
 
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(48, 40, 48, 28)
            addView(titleView)
            addView(stageView)
            addView(progressBar)
            addView(subtextView)
        }
 
        val dialog = AlertDialog.Builder(this)
            .setView(container)
            .setCancelable(false)
            .create()
 
        activeGuardedPopup = dialog
        dialog.show()
 
        val workDone = AtomicBoolean(false)
        val failed = AtomicBoolean(false)
        val readyArmed = AtomicBoolean(false)
 
        fun showReadyState() {
            if (!readyArmed.compareAndSet(false, true)) return
            progressBar.visibility = View.GONE
            stageView.text = "Ready"
            subtextView.text = "Tap token and hold it against device."
            stageView.text = "Ready"
            subtextView.text = "Tap token and hold it against device."
            onReady()
        }
 
        Thread {
            try {
                work()
                workDone.set(true)
                runOnUiThread {
                    if (failed.get()) return@runOnUiThread
                    showReadyState()
                }
            } catch (e: Exception) {
                failed.set(true)
                runOnUiThread {
                    dismissGuardedPopup()
                    onFailure(e.message ?: "Preparation failed")
                }
            }
        }.start()
    }
 
    private fun describeRevealStage(secondsRemaining: Int, forgeLevel: String): String {
        return when (forgeLevel.lowercase()) {
            PublicCoinRecord.FORGE_CAST -> "Loosening the seal..."
            PublicCoinRecord.FORGE_FORGED -> {
                if (secondsRemaining >= 2) "Opening the chamber..." else "Loosening the seal..."
            }
            PublicCoinRecord.FORGE_TEMPERED -> {
                when {
                    secondsRemaining >= 5 -> "Separating the metals..."
                    secondsRemaining >= 2 -> "Opening the chamber..."
                    else -> "Loosening the seal..."
                }
            }
            PublicCoinRecord.FORGE_HARDENED -> {
                when {
                    secondsRemaining >= 14 -> "Heating the shell..."
                    secondsRemaining >= 9 -> "Softening the alloy..."
                    secondsRemaining >= 4 -> "Separating the metals..."
                    secondsRemaining >= 2 -> "Unearthing the contents..."
                    else -> "Loosening the seal..."
                }
            }
            else -> "Loosening the seal..."
        }
    }
 
    private fun selectedForgeLevel(): String {
        return when {
            binding.radioForgeHardened.isChecked -> PublicCoinRecord.FORGE_HARDENED
            binding.radioForgeTempered.isChecked -> PublicCoinRecord.FORGE_TEMPERED
            binding.radioForgeForged.isChecked -> PublicCoinRecord.FORGE_FORGED
            else -> PublicCoinRecord.FORGE_CAST
        }
    }
 
    private fun estimateTimeRange(seconds: Int): String {
        return when {
            seconds <= 1 -> "Usually takes under 1 second"
            seconds <= 3 -> "Usually takes 1–3 seconds"
            seconds <= 7 -> "Usually takes 3–10 seconds"
            seconds <= 21 -> "Usually takes 8–25 seconds"
            else -> "Processing..."
        }
    }
 
    private fun isGuardedSelected(): Boolean {
        return binding.radioTokenGuarded.isChecked
    }
 
    private fun isStealthSelected(): Boolean {
        return binding.radioTokenStealth.isChecked
    }
 
    private fun stealthPublicMessageForSelectedOption(): String {
        val custom = binding.inputStealthPublicMessage.text.toString()
        val prefix = stealthPrefixForForgeLevel(selectedForgeLevel())
 
        val body = when (binding.radioGroupStealthStyle.checkedRadioButtonId) {
            R.id.radioStealthCustom -> custom
            R.id.radioStealthDoor -> "Door Tag\nID: ${randomShortId()}"
            R.id.radioStealthMeet -> "https://meet.google.com/${randomShortId()}"
            R.id.radioStealthContact -> "Employee ID: ${randomShortId()}"
            R.id.radioStealthWifi -> "WIFI:T:WPA;S:Guest-${randomShortId()};P:guestpass;;"
            R.id.radioStealthNote -> "Maintenance Log\nTag: ${randomShortId()}"
            R.id.radioStealthAsset -> "Asset Tag\nID: ${randomShortId()}"
            R.id.radioStealthTicket -> "Event Ticket\nCode: ${randomShortId()}"
            else -> throw IllegalStateException("No stealth style selected")
        }
 
        return "$prefix $body"
    }
 
    private fun stealthPrefixForForgeLevel(forgeLevel: String): String {
        return when (forgeLevel) {
            PublicCoinRecord.FORGE_FORGED -> "Memo:"
            PublicCoinRecord.FORGE_TEMPERED -> "Msg:"
            PublicCoinRecord.FORGE_HARDENED -> "Log:"
            else -> "Note:"
        }
    }
 
    private fun stealthForgeFromPublicMessage(publicMessage: String): String? {
        return when {
            publicMessage.startsWith("Note:") -> PublicCoinRecord.FORGE_CAST
            publicMessage.startsWith("Memo:") -> PublicCoinRecord.FORGE_FORGED
            publicMessage.startsWith("Msg:") -> PublicCoinRecord.FORGE_TEMPERED
            publicMessage.startsWith("Log:") -> PublicCoinRecord.FORGE_HARDENED
            else -> null
        }
    }
 
    private fun randomShortId(): String {
        val bytes = ByteArray(4)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
 
    private fun buildGuardedSummary(record: PublicCoinRecord): String {
        val secret = SecretGradeEstimator.displayLabel(record.secretGrade)
        val forge = WorkTierEstimator.displayLabel(record.forgeLevel)
 
        return if (record.isStealth) {
            "Stealth $secret coin, $forge level"
        } else {
            "Guarded $secret coin, $forge level"
        }
    }
 
    private fun buildBearerSummary(record: PublicCoinRecord): String {
        val forge = WorkTierEstimator.displayLabel(record.forgeLevel)
        return "Bearer coin, $forge level"
    }
 
    private fun guardedSalt(address: String, message: String, forgeLevel: String): ByteArray {
        val material = "$address|$message|$forgeLevel|guarded".toByteArray()
        return MessageDigest.getInstance("SHA-256").digest(material).copyOf(16)
    }
 
    private fun stealthV2Salt(uid: ByteArray, publicMessage: String, forgeLevel: String): ByteArray {
        val uidHex = uid.joinToString("") { "%02x".format(it) }
        val material = "$uidHex|$publicMessage|$forgeLevel|stealth-v2".toByteArray()
        return MessageDigest.getInstance("SHA-256").digest(material).copyOf(16)
    }
 
    private fun randomBearerSaltHex(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
 
    private fun updateState(state: String) {
        binding.textScannedState.text = state.uppercase()
 
        if (state == PublicCoinRecord.STATE_UNREVEALED) {
            binding.textScannedState.setBackgroundColor(Color.parseColor("#1B5E20"))
            binding.textScannedState.setTextColor(Color.parseColor("#00FF88"))
        } else {
            binding.textScannedState.setBackgroundColor(Color.parseColor("#5E1B1B"))
            binding.textScannedState.setTextColor(Color.parseColor("#FF4C4C"))
        }
    }
 
    private fun clearMintState() {
        mintPhase = 1
        pendingAction = PendingAction.NONE
        pendingMintAddress = null
        pendingMintMessage = null
        pendingMintPrivateKey = null
 
        pendingGuardedSecret = null
        pendingGuardedSecretGrade = null
        pendingGuardedForgeLevel = null
        pendingGuardedAuthKey = null
        pendingGuardedContentKey = null
 
        pendingBearerForgeLevel = null
        pendingBearerSalt = null
        pendingBearerAuthKey = null
        pendingBearerContentKey = null
 
        pendingStealthPublicMessage = null
        pendingStealthForgeLevel = null
 
        dismissGuardedPopup()
    }
 
    private fun promptForStealthInspect(
        publicMessage: String,
        uid: ByteArray,
        forgeLevel: String
    ) {
        val input = EditText(this)
        input.hint = "Enter stealth password"
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
 
        val forgeLabel = WorkTierEstimator.displayLabel(forgeLevel)
        val estimatedSeconds = GuardedKdf.estimatedSeconds(this, forgeLevel)
 
        AlertDialog.Builder(this)
            .setTitle("Inspect $forgeLabel Stealth Token")
            .setMessage(
                "Potential Stealth Token Detected.\n\n" +
                        "Enter the stealth password to inspect the protected metadata."
            )
            .setView(input)
            .setPositiveButton("Continue") { _, _ ->
                val secret = input.text?.toString().orEmpty()
                if (secret.isBlank()) {
                    binding.textRevealStatus.text = "Stealth password is required."
                    return@setPositiveButton
                }
 
                showGuardedProgressPopup(
                    title = "Preparing Stealth Inspect",
                    seconds = estimatedSeconds,
                    stageProvider = { remaining ->
                        describeRevealStage(remaining, forgeLevel)
                    },
                    finalizingText = "Finalizing stealth key...",
                    work = {
                        val stealthSalt = stealthV2Salt(
                            uid = uid,
                            publicMessage = publicMessage,
                            forgeLevel = forgeLevel
                        )
 
 
                        val rounds = ForgeRounds.get(forgeLevel)
 
                        val derived = GuardedKdf.deriveWithRounds(
                            password = secret,
                            uid = stealthSalt,
                            salt = stealthSalt,
                            rounds = rounds
                        )
 
                        pendingStealthPublicMessage = publicMessage
                        pendingStealthForgeLevel = forgeLevel
                        pendingRevealAuthKey = derived.authKey
                        pendingRevealContentKey = derived.contentKey
                    },
                    onReady = {
                        pendingAction = PendingAction.INSPECT_STEALTH
                        binding.textRevealStatus.text = "Tap stealth token again and hold to inspect."
                    },
                    onFailure = { msg ->
                        binding.textRevealStatus.text = msg
                        pendingAction = PendingAction.NONE
                        pendingStealthPublicMessage = null
                        pendingStealthForgeLevel = null
                        pendingRevealAuthKey = null
                        pendingRevealContentKey = null
                    }
                )
            }
            .setNegativeButton("Cancel") { _, _ ->
                binding.textRevealStatus.text = "Stealth scan cancelled."
            }
            .show()
    }
 
    private fun copy(label: String, value: String) {
        if (value.isBlank()) return
 
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
        Toast.makeText(this, "$label copied", Toast.LENGTH_SHORT).show()
    }
 
    private fun updateLogMintToggleVisibility() {
        val enabled = CoinLogStore.isDatabaseEnabled(this)
 
        binding.switchLogMintedCoins.visibility =
            if (enabled) View.VISIBLE else View.GONE
 
        if (!enabled) {
            binding.switchLogMintedCoins.isChecked = false
        }
    }
    private fun fetchBalanceForAddress(address: String) {
        balanceLoaded = false
        balanceSats = 0
        balanceMode = 0
 
        binding.textBalance.text = "Checking..."
 
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = java.net.URL("https://mempool.space/api/address/$address")
                val connection = url.openConnection() as java.net.HttpURLConnection
 
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
 
                val jsonText = connection.inputStream.bufferedReader().use { it.readText() }
 
                val obj = JSONObject(jsonText)
                val stats = obj.getJSONObject("chain_stats")
 
                val funded = stats.getLong("funded_txo_sum")
                val spent = stats.getLong("spent_txo_sum")
 
                balanceSats = funded - spent
 
                withContext(Dispatchers.Main) {
                    balanceLoaded = true
                    updateBalanceDisplay()
                }
 
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.textBalance.text = "Error"
                }
            }
        }
    }
 
    private fun fetchBtcPrice() {
 
        Thread {
 
            try {
 
                val url = java.net.URL(
                    "https://mempool.space/api/v1/prices"
                )
 
                val json = url.readText()
 
                val obj = org.json.JSONObject(json)
 
                btcUsdPrice = obj.getDouble("USD")
 
                runOnUiThread {
                    updateBalanceDisplay()
                }
 
            } catch (_: Exception) {
            }
 
        }.start()
    }
 
    private fun vibrateSuccess() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
 
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            vibrator.vibrate(80)
        }
    }
    private fun updateBalanceDisplay() {
        val sats = balanceSats
 
        binding.textBalance.text = when (balanceMode) {
            0 -> "₿ %,d".format(sats)
            1 -> "%,d sats".format(sats)
            2 -> "%.8f BTC".format(sats / 100_000_000.0)
            3 -> "~ $%.2f".format(
                sats / 100_000_000.0 * btcUsdPrice
            )
            else -> "%,d sats".format(sats)
        }
    }
}
