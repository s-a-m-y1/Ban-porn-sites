package com.contentfilter.app

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : Activity() {

    private lateinit var toggleButton: Button
    private lateinit var statusText: TextView
    private lateinit var statsText: TextView
    private var vpnActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toggleButton = findViewById(R.id.toggleButton)
        statusText = findViewById(R.id.statusText)
        statsText = findViewById(R.id.statsText)

        val repo = BlocklistRepository(this)
        lifecycleScope.launch {
            repo.ensureInitialBlocklist()
            SyncWorker.schedule(this@MainActivity)
            statsText.text = getString(
                R.string.blocklist_size_fmt,
                repo.count(),
            )
        }

        toggleButton.setOnClickListener {
            if (vpnActive) {
                FilterVpnService.stop(this)
                setUiState(false)
                return@setOnClickListener
            }
            val prepare = VpnService.prepare(this)
            if (prepare != null) {
                startActivityForResult(prepare, 1)
            } else {
                onVpnApproved()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK) onVpnApproved()
    }

    private fun onVpnApproved() {
        FilterVpnService.start(this)
        setUiState(true)
        Toast.makeText(this, R.string.status_on, Toast.LENGTH_SHORT).show()
    }

    private fun setUiState(active: Boolean) {
        vpnActive = active
        toggleButton.text = getString(
            if (active) R.string.stop_blocking else R.string.start_blocking,
        )
        statusText.text = getString(
            if (active) R.string.status_on else R.string.status_off,
        )
    }
}
