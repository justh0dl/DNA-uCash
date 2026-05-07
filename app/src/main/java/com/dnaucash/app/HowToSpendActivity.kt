package com.dnaucash.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.dnaucash.app.databinding.ActivityTextPageBinding

class HowToSpendActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTextPageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTextPageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        title = "How to Sweep"
        binding.textContent.text = SpendText.CONTENT
    }
}