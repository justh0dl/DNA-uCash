package com.dnaucash.app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.dnaucash.app.databinding.ActivityInfoMenuBinding

class InfoMenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInfoMenuBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInfoMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonAbout.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }

        binding.buttonSecurity.setOnClickListener {
            startActivity(Intent(this, SecurityActivity::class.java))
        }

        binding.buttonHowCoinsWork.setOnClickListener {
            startActivity(Intent(this, HowCoinsWorkActivity::class.java))
        }

        binding.buttonMintingGuide.setOnClickListener {
            startActivity(Intent(this, MintingGuideActivity::class.java))
        }

        binding.buttonSpend.setOnClickListener {
            startActivity(Intent(this, HowToSpendActivity::class.java))
        }

        binding.buttonDatabase.setOnClickListener {
            startActivity(Intent(this, DatabaseActivity::class.java))
        }
    }
}