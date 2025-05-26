/*
 * Copyright (c) 2025 Christians Martínez Alvarado
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

package com.mardous.geet.fragments.player.styles.plainstyle

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type
import androidx.core.view.updatePadding
import com.mardous.geet.R
import com.mardous.geet.databinding.FragmentPlainPlayerBinding
import com.mardous.geet.extensions.getOnBackPressedDispatcher
import com.mardous.geet.extensions.whichFragment
import com.mardous.geet.fragments.player.base.AbsPlayerControlsFragment
import com.mardous.geet.fragments.player.base.AbsPlayerFragment
import com.mardous.geet.model.Song

/**
 * @author Christians M. A. (mardous)
 */
class PlainPlayerFragment : AbsPlayerFragment(R.layout.fragment_plain_player) {

    private var _binding: FragmentPlainPlayerBinding? = null
    private val binding get() = _binding!!

    private lateinit var controlsFragment: PlainPlayerControlsFragment

    override val playerControlsFragment: AbsPlayerControlsFragment
        get() = controlsFragment

    override val playerToolbar: Toolbar
        get() = binding.toolbar

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPlainPlayerBinding.bind(view)
        setupToolbar()
        inflateMenuInView(playerToolbar)
        ViewCompat.setOnApplyWindowInsetsListener(view) { v: View, insets: WindowInsetsCompat ->
            val systemBars = insets.getInsets(Type.systemBars())
            v.updatePadding(top = systemBars.top, bottom = systemBars.bottom)
            val displayCutout = insets.getInsets(Type.displayCutout())
            v.updatePadding(left = displayCutout.left, right = displayCutout.right)
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun setupToolbar() {
        if (playerToolbar.navigationIcon == null)
            return

        playerToolbar.setNavigationOnClickListener {
            getOnBackPressedDispatcher().onBackPressed()
        }
    }

    override fun onMenuInflated(menu: Menu) {
        super.onMenuInflated(menu)
        menu.setShowAsAction(R.id.action_playing_queue, mode = MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu.setShowAsAction(R.id.action_favorite, mode = MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu.setShowAsAction(R.id.action_sleep_timer, mode = MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu.setShowAsAction(R.id.action_show_lyrics, mode = MenuItem.SHOW_AS_ACTION_ALWAYS)
    }

    override fun onCreateChildFragments() {
        super.onCreateChildFragments()
        controlsFragment = whichFragment(R.id.playbackControlsFragment)
    }

    override fun onSongInfoChanged(song: Song) {
        super.onSongInfoChanged(song)
        _binding?.let { nonNullBinding ->
            nonNullBinding.title.text = song.title
            nonNullBinding.text.text = getSongArtist(song)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}