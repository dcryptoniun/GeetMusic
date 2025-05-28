/*
 * Copyright (c) 2024 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.teqanta.geet.fragments.about

import android.content.*
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.teqanta.geet.BuildConfig
import com.teqanta.geet.R
import com.teqanta.geet.databinding.FragmentAboutBinding
import com.teqanta.geet.dialogs.MarkdownDialog
import com.teqanta.geet.extensions.MIME_TYPE_PLAIN_TEXT
import com.teqanta.geet.extensions.applyBottomWindowInsets
import com.teqanta.geet.extensions.openWeb
import com.teqanta.geet.extensions.showToast
import com.teqanta.geet.model.DeviceInfo

/**
 * @author Christians M. A. (teqanta)
 */
class AboutFragment : Fragment(R.layout.fragment_about), View.OnClickListener {

    private var _binding: FragmentAboutBinding? = null
    private val binding get() = _binding!!

    private lateinit var deviceInfo: DeviceInfo
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        deviceInfo = DeviceInfo(requireActivity())
        _binding = FragmentAboutBinding.bind(view)
        view.applyBottomWindowInsets()
        setupVersion()
        hideUnnecessaryButtons()
        setupListeners()
    }    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupVersion() {
        binding.cardApp.version.text = BuildConfig.VERSION_NAME
    }

    private fun hideUnnecessaryButtons() {
        // Hide buttons from card_app
        binding.cardApp.changelog.visibility = View.GONE
        binding.cardApp.forkOnGithub.visibility = View.GONE
        binding.cardApp.licenses.visibility = View.GONE
        
        // Hide buttons from card_support
        binding.cardSupport.reportBugs.visibility = View.GONE
        binding.cardSupport.translateApp.visibility = View.GONE
    }    private fun setupListeners() {
        // Only setup listeners for visible buttons
        binding.cardAuthor.github.setOnClickListener(this)
        binding.cardSupport.shareApp.setOnClickListener(this)
    }

    override fun onClick(view: View?) {
        when (view) {
            binding.cardAuthor.github -> {
                openUrl(AUTHOR_GITHUB_URL)
            }

            binding.cardSupport.shareApp -> {
                sendInvitationMessage()
            }
        }
    }

    private fun sendInvitationMessage() {
        val intent = Intent(Intent.ACTION_SEND)
            .putExtra(Intent.EXTRA_TEXT, getString(R.string.invitation_message_content, "${GITHUB_URL}/releases"))
            .setType(MIME_TYPE_PLAIN_TEXT)

        startActivity(Intent.createChooser(intent, getString(R.string.send_invitation_message)))
    }

    private fun openUrl(url: String) {
        startActivity(url.openWeb())
    }    private fun copyDeviceInfoToClipBoard() {
        val clipboard = requireContext().getSystemService<ClipboardManager>()
        if (clipboard != null) {
            val clip = ClipData.newPlainText(getString(R.string.device_info), deviceInfo.toMarkdown())
            clipboard.setPrimaryClip(clip)
        }
        showToast(R.string.copied_device_info_to_clipboard, Toast.LENGTH_LONG)
    }

    companion object {
        private const val AUTHOR_GITHUB_URL = "https://github.com/dcryptoniun"
        private const val GITHUB_URL = "https://github.com/dcryptoniun/GeetMusic"
        private const val RELEASES_LINK = "$GITHUB_URL/releases"
        private const val ISSUE_TRACKER_LINK = "$GITHUB_URL/issues"
        private const val CROWDIN_PROJECT_LINK = "https://crowdin.com/project/booming-music"
    }
}