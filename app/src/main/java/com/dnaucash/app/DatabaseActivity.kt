package com.dnaucash.app

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.widget.Button
import android.app.AlertDialog
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class DatabaseActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {

    private var nfcAdapter: NfcAdapter? = null
    private var scanLoggingEnabled = false
    private var scannedCount = 0
    private var privateKeyTapCount = 0

    private lateinit var switchDatabase: Switch
    private lateinit var buttonStartScan: Button
    private lateinit var buttonStopScan: Button
    private lateinit var buttonExportScanned: Button
    private lateinit var buttonExportMinted: Button
    private lateinit var textScanCount: TextView
    private lateinit var textPrivateKeyUnlock: TextView
    private lateinit var switchLogPrivateKeys: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_database)

        switchDatabase = findViewById(R.id.switchDatabase)
        buttonStartScan = findViewById(R.id.buttonStartScan)
        buttonStopScan = findViewById(R.id.buttonStopScan)
        buttonExportScanned = findViewById(R.id.buttonExportScanned)
        buttonExportMinted = findViewById(R.id.buttonExportMinted)
        textScanCount = findViewById(R.id.textScanCount)
        textPrivateKeyUnlock = findViewById(R.id.textPrivateKeyUnlock)
        switchLogPrivateKeys = findViewById(R.id.switchLogPrivateKeys)
        switchLogPrivateKeys.visibility =
            if (CoinLogStore.isPrivateKeyLoggingUnlocked(this)) android.view.View.VISIBLE else android.view.View.GONE

        switchLogPrivateKeys.isChecked = CoinLogStore.isPrivateKeyLoggingEnabled(this)

        switchLogPrivateKeys.setOnCheckedChangeListener { _, isChecked ->
            CoinLogStore.setPrivateKeyLoggingEnabled(this, isChecked)
        }

        textPrivateKeyUnlock.setOnClickListener {
            if (CoinLogStore.isPrivateKeyLoggingUnlocked(this)) return@setOnClickListener

            privateKeyTapCount += 1
            val remaining = 21 - privateKeyTapCount

            if (remaining > 0) {
                Toast.makeText(this, "$remaining taps until private key logging unlock", Toast.LENGTH_SHORT).show()
            } else {
                showPrivateKeyLoggingWarning()
            }
        }

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        switchDatabase.isChecked = CoinLogStore.isDatabaseEnabled(this)

        switchDatabase.setOnCheckedChangeListener { _, isChecked ->
            CoinLogStore.setDatabaseEnabled(this, isChecked)

            if (!isChecked) {
                stopScanLogging()
            }
        }

        buttonStartScan.setOnClickListener {
            if (!CoinLogStore.isDatabaseEnabled(this)) {
                Toast.makeText(this, "Enable Database first.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            startScanLogging()
        }

        buttonStopScan.setOnClickListener {
            stopScanLogging()
        }

        buttonExportScanned.setOnClickListener {
            val uri = CoinLogStore.exportScannedCsv(this)
            Toast.makeText(this, "Scanned CSV exported:\n$uri", Toast.LENGTH_LONG).show()
        }

        buttonExportMinted.setOnClickListener {
            val uri = CoinLogStore.exportMintedCsv(this)
            Toast.makeText(this, "Minted CSV exported:\n$uri", Toast.LENGTH_LONG).show()
        }

        updateScanCount()
    }

    override fun onResume() {
        super.onResume()

        if (scanLoggingEnabled) {
            enableReader()
        }
    }

    override fun onPause() {
        super.onPause()
        disableReader()
    }

    override fun onDestroy() {
        disableReader()
        super.onDestroy()
    }

    override fun onTagDiscovered(tag: Tag) {
        if (!scanLoggingEnabled) return

        try {
            if (!Ntag424Service.isNtag424(tag)) {
                runOnUiThread {
                    Toast.makeText(this, "Invalid tag. NTAG424 required.", Toast.LENGTH_SHORT).show()
                }
                return
            }

            val uid = tag.id.joinToString("") { "%02x".format(it) }
            val text = NdefUtils.readText(tag) ?: ""

            val record = try {
                PublicCoinRecordCodec.parse(text)
            } catch (_: Exception) {
                null
            }

            CoinLogStore.logScannedCoin(
                context = this,
                uid = uid,
                publicText = text,
                record = record
            )

            scannedCount += 1

            runOnUiThread {
                updateScanCount()
                Toast.makeText(this, "Scanned token logged.", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            runOnUiThread {
                Toast.makeText(
                    this,
                    e.message ?: "Scan log failed",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showPrivateKeyLoggingWarning() {
        AlertDialog.Builder(this)
            .setTitle("Are you SURE?")
            .setMessage(
                "Logging private keys is dangerous.\n\n" +
                        "Anyone with access to the exported CSV can sweep the funds from logged coins.\n\n" +
                        "This should normally remain OFF.\n\n" +
                        "Only enable this if you fully understand the risk."
            )
            .setPositiveButton("Unlock") { _, _ ->
                CoinLogStore.setPrivateKeyLoggingUnlocked(this, true)
                switchLogPrivateKeys.visibility = android.view.View.VISIBLE
                Toast.makeText(this, "Private key logging unlocked.", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Cancel") { _, _ ->
                privateKeyTapCount = 0
            }
            .show()
    }
    private fun startScanLogging() {
        scanLoggingEnabled = true
        scannedCount = 0
        updateScanCount()
        enableReader()
        Toast.makeText(this, "Scan logging started.", Toast.LENGTH_SHORT).show()
    }

    private fun stopScanLogging() {
        scanLoggingEnabled = false
        disableReader()
        updateScanCount()
    }

    private fun updateScanCount() {
        textScanCount.text = "Scanned: $scannedCount"
    }

    private fun enableReader() {
        nfcAdapter?.enableReaderMode(
            this,
            this,
            NfcAdapter.FLAG_READER_NFC_A,
            null
        )
    }

    private fun disableReader() {
        nfcAdapter?.disableReaderMode(this)
    }
}