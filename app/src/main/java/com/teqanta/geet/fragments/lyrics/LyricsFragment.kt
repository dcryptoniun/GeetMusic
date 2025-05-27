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
package com.teqanta.geet.fragments.lyrics

import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ShareCompat
import androidx.core.view.MenuProvider
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.teqanta.geet.R
import com.teqanta.geet.databinding.FragmentLyricsBinding
import com.teqanta.geet.extensions.*
import com.teqanta.geet.extensions.resources.accentColor
import com.teqanta.geet.fragments.base.AbsMainActivityFragment
import com.teqanta.geet.helper.MusicProgressViewUpdateHelper
import com.teqanta.geet.model.Song
import com.teqanta.geet.service.MusicPlayer
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import kotlin.properties.Delegates

/**
 * @author Christians M. A. (teqanta)
 */
class LyricsFragment : AbsMainActivityFragment(R.layout.fragment_lyrics),
    MenuProvider, MusicProgressViewUpdateHelper.Callback {

    private var _binding: FragmentLyricsBinding? = null
    private val binding get() = _binding!!

    private val lyricsViewModel: LyricsViewModel by activityViewModel()

    private lateinit var importLyricsLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var progressViewUpdateHelper: MusicProgressViewUpdateHelper

    private var song: Song by Delegates.notNull()
    private var importLRCFor: Song? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.applyWindowInsets(left = true, right = true, bottom = true)

        _binding = FragmentLyricsBinding.bind(view)
        materialSharedAxis(view)
        setSupportActionBar(binding.toolbar)
        setupViews()
        importLyricsLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { data: Uri? ->
            if (data != null) {
                importLRCFor?.let { importLRCFile(it, data) }
            }
        }
        progressViewUpdateHelper = MusicProgressViewUpdateHelper(this, 500, 1000)
    }

    private fun setupViews() {
        binding.edit.setOnClickListener { editLyrics(song) }
        binding.lyricsView.apply {
            setCurrentColor(accentColor())
            setTimeTextColor(accentColor())
            setTimelineColor(accentColor())
            setTimelineTextColor(accentColor())
            setDraggable(true) {
                MusicPlayer.seekTo(it.toInt())
                true
            }
        }
    }

    private fun loadLyrics() {
        lyricsViewModel.getAllLyrics(song, allowDownload = true)
            .observe(viewLifecycleOwner) { lyrics ->
                if (lyrics.loading) {
                    binding.progress.show()
                    binding.normalLyrics.isGone = true
                    binding.lyricsView.isGone = true
                } else {
                    binding.progress.hide()
                    binding.normalLyrics.text = lyrics.data
                    binding.normalLyrics.isGone = lyrics.isEmpty || lyrics.isSynced
                    binding.lyricsView.setLRCContent(lyrics.lrcData)
                    binding.lyricsView.updateTime(MusicPlayer.songProgressMillis.toLong())
                    binding.lyricsView.isVisible = lyrics.isEmpty || lyrics.isSynced
                }
            }
    }

    private fun updateCurrentSong() {
        song = MusicPlayer.currentSong
        if (song == Song.emptySong) {
            binding.edit.hide()
        } else {
            binding.edit.show()
        }
        loadLyrics()
    }

    override fun onResume() {
        super.onResume()
        updateCurrentSong()
        progressViewUpdateHelper.start()
        requireActivity().keepScreenOn(true)
    }

    override fun onPause() {
        super.onPause()
        progressViewUpdateHelper.stop()
        requireActivity().keepScreenOn(false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onUpdateProgressViews(progress: Long, total: Long) {
        binding.lyricsView.updateTime(progress)
    }

    override fun onPlayingMetaChanged() {
        super.onPlayingMetaChanged()
        updateCurrentSong()
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_lyrics, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            android.R.id.home -> {
                findNavController().navigateUp()
                return true
            }

            R.id.action_import_lyrics -> {
                importSyncedLyrics(song)
                return true
            }

            R.id.action_share_lyrics -> {
                shareSyncedLyrics(song)
                return true
            }
        }
        return false
    }

    private fun editLyrics(song: Song) {
        findNavController().navigate(
            R.id.nav_lyrics_editor,
            LyricsEditorFragmentArgs.Builder(song)
                .build()
                .toBundle()
        )
    }

    private fun importSyncedLyrics(song: Song) {
        importLRCFor = song.also { currentSong ->
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.action_import_synchronized_lyrics)
                .setMessage(
                    getString(
                        R.string.do_you_want_to_import_lrc_lyrics_for_song_x,
                        currentSong.title
                    ).toHtml()
                )
                .setPositiveButton(R.string.yes) { _: DialogInterface, _: Int ->
                    try {
                        importLyricsLauncher.launch(arrayOf("application/*"))
                        showToast(
                            R.string.select_a_file_containing_synchronized_lyrics,
                            Toast.LENGTH_SHORT
                        )
                    } catch (_: ActivityNotFoundException) {
                    }
                }
                .setNegativeButton(R.string.no, null)
                .show()
        }
    }

    private fun shareSyncedLyrics(song: Song) {
        lyricsViewModel.shareSyncedLyrics(requireContext(), song).observe(viewLifecycleOwner) {
            if (it == null) {
                showToast(R.string.no_synced_lyrics_found, Toast.LENGTH_SHORT)
            } else {
                startActivity(
                    ShareCompat.IntentBuilder(requireContext())
                        .setType(MIME_TYPE_APPLICATION)
                        .setChooserTitle(R.string.action_share_synchronized_lyrics)
                        .setStream(it)
                        .createChooserIntent()
                )
            }
        }
    }

    private fun importLRCFile(song: Song, data: Uri) {
        lyricsViewModel.setLRCContentFromUri(requireContext(), song, data)
            .observe(viewLifecycleOwner) { isSuccess ->
                if (isSuccess) {
                    showToast(getString(R.string.import_lyrics_for_song_x, song.title))
                } else {
                    showToast(getString(R.string.could_not_import_lyrics_for_song_x, song.title))
                }
            }
    }
}