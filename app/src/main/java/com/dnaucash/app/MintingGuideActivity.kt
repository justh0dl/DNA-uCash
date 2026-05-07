package com.dnaucash.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.dnaucash.app.databinding.ActivityTextPageBinding

class MintingGuideActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTextPageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTextPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.textContent.text = MintingGuideText.CONTENT
    }
}