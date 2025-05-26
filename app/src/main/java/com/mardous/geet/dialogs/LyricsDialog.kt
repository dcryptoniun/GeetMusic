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

package com.mardous.geet.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.core.os.BundleCompat
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mardous.geet.R
import com.mardous.geet.extensions.EXTRA_SONG
import com.mardous.geet.extensions.create
import com.mardous.geet.extensions.withArgs
import com.mardous.geet.fragments.lyrics.LyricsViewModel
import com.mardous.geet.fragments.player.base.goToDestination
import com.mardous.geet.model.Song
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class LyricsDialog : DialogFragment() {

    private val lyricsViewModel: LyricsViewModel by activityViewModel()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val song = BundleCompat.getParcelable(requireArguments(), EXTRA_SONG, Song::class.java)!!
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(song.title)
            .setMessage(R.string.no_lyrics_found)
            .setPositiveButton(R.string.open_lyrics_editor) { _: DialogInterface, _: Int ->
                goToDestination(requireActivity(), R.id.nav_lyrics)
            }
            .create { dialog ->
                lyricsViewModel.getLyrics(song).observe(this) { result ->
                    if (result.hasData) {
                        dialog.setMessage(result.data)
                    }
                }
            }
    }

    companion object {
        fun create(song: Song) = LyricsDialog().withArgs {
            putParcelable(EXTRA_SONG, song)
        }
    }
}