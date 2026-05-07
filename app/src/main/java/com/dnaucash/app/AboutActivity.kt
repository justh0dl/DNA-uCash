package com.dnaucash.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.dnaucash.app.databinding.ActivityTextPageBinding

class AboutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTextPageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTextPageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        title = "About DNAuCash"
        binding.textContent.text = AboutText.CONTENT
    }
}