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

package com.teqanta.geet.fragments.player.styles.gradientstyle

import android.annotation.SuppressLint
import android.graphics.PorterDuff
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import com.google.android.material.slider.Slider
import com.teqanta.geet.R
import com.teqanta.geet.databinding.FragmentGradientPlayerPlaybackControlsBinding
import com.teqanta.geet.extensions.resources.applyColor
import com.teqanta.geet.extensions.resources.toColorStateList
import com.teqanta.geet.fragments.player.base.AbsPlayerControlsFragment
import com.teqanta.geet.helper.handler.PrevNextButtonOnTouchHandler
import com.teqanta.geet.model.NowPlayingAction
import com.teqanta.geet.model.Song
import com.teqanta.geet.service.MusicPlayer

class GradientPlayerControlsFragment : AbsPlayerControlsFragment(R.layout.fragment_gradient_player_playback_controls),
    SeekBar.OnSeekBarChangeListener {

    private var _binding: FragmentGradientPlayerPlaybackControlsBinding? = null
    private val binding get() = _binding!!

    override val progressSlider: Slider
        get() = binding.progressSlider

    override val songCurrentProgress: TextView
        get() = binding.songCurrentProgress

    override val songTotalTime: TextView
        get() = binding.songTotalTime

    override val songInfoView: TextView
        get() = binding.songInfo

    private var isFavorite: Boolean = false
    private var popupMenu: PopupMenu? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentGradientPlayerPlaybackControlsBinding.bind(view)
        setupListeners()
        setViewAction(binding.favorite, NowPlayingAction.ToggleFavoriteState)
        popupMenu = playerFragment?.inflateMenuInView(binding.menu)
        ViewCompat.setOnApplyWindowInsetsListener(view) { v: View, insets: WindowInsetsCompat ->
            val displayCutout = insets.getInsets(Type.displayCutout())
            v.updatePadding(left = displayCutout.left, right = displayCutout.right)
            insets
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupListeners() {
        binding.text.setOnClickListener(this)
        binding.playPauseButton.setOnClickListener(this)
        binding.next.setOnTouchListener(PrevNextButtonOnTouchHandler(PrevNextButtonOnTouchHandler.DIRECTION_NEXT))
        binding.previous.setOnTouchListener(PrevNextButtonOnTouchHandler(PrevNextButtonOnTouchHandler.DIRECTION_PREVIOUS))
        binding.shuffleButton.setOnClickListener(this)
        binding.repeatButton.setOnClickListener(this)
    }

    override fun onClick(view: View) {
        super.onClick(view)
        when (view) {
            binding.shuffleButton -> MusicPlayer.toggleShuffleMode()
            binding.repeatButton -> MusicPlayer.cycleRepeatMode()
            binding.playPauseButton -> MusicPlayer.togglePlayPause()
        }
    }

    override fun setColors(backgroundColor: Int, primaryControlColor: Int, secondaryControlColor: Int) {
        binding.controlContainer.setBackgroundColor(backgroundColor)

        binding.title.setTextColor(primaryControlColor)
        binding.text.setTextColor(primaryControlColor)
        binding.songInfo.setTextColor(secondaryControlColor)

        val primaryTintList = primaryControlColor.toColorStateList()
        binding.menu.imageTintList = primaryTintList
        binding.favorite.imageTintList = primaryTintList

        binding.progressSlider.applyColor(primaryControlColor)
        binding.songCurrentProgress.setTextColor(secondaryControlColor)
        binding.songTotalTime.setTextColor(secondaryControlColor)

        binding.playPauseButton.setColorFilter(primaryControlColor, PorterDuff.Mode.SRC_IN)
        binding.shuffleButton.setColors(secondaryControlColor, primaryControlColor)
        binding.repeatButton.setColors(secondaryControlColor, primaryControlColor)
        binding.next.setColorFilter(primaryControlColor, PorterDuff.Mode.SRC_IN)
        binding.previous.setColorFilter(primaryControlColor, PorterDuff.Mode.SRC_IN)
    }

    override fun onSongInfoChanged(song: Song) {
        _binding?.let { nonNullBinding ->
            nonNullBinding.title.text = song.title
            nonNullBinding.text.text = getSongArtist(song)
            if (isExtraInfoEnabled()) {
                nonNullBinding.songInfo.text = getExtraInfoString(song)
                nonNullBinding.songInfo.isVisible = true
            } else {
                nonNullBinding.songInfo.isVisible = false
            }
        }
    }

    override fun onQueueInfoChanged(newInfo: String?) {}

    override fun onUpdatePlayPause(isPlaying: Boolean) {
        if (isPlaying) {
            _binding?.playPauseButton?.setImageResource(R.drawable.ic_pause_24dp)
        } else {
            _binding?.playPauseButton?.setImageResource(R.drawable.ic_play_24dp)
        }
    }

    override fun onUpdateRepeatMode(repeatMode: Int) {
        _binding?.repeatButton?.setRepeatMode(repeatMode)
    }

    override fun onUpdateShuffleMode(shuffleMode: Int) {
        _binding?.shuffleButton?.setShuffleMode(shuffleMode)
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        if (binding.progressSlider == seekBar && fromUser) {
            MusicPlayer.seekTo(progress)
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {}

    override fun onStopTrackingTouch(seekBar: SeekBar?) {}

    internal fun setLyricsVisible(lyricsVisible: Boolean) {
        popupMenu?.menu?.findItem(R.id.action_show_lyrics)?.let {
            if (lyricsVisible) {
                it.setTitle(R.string.action_hide_lyrics)
            } else {
                it.setTitle(R.string.action_show_lyrics)
            }
        }
    }

    internal fun setFavorite(isFavorite: Boolean, withAnimation: Boolean) {
        if (this.isFavorite != isFavorite) {
            this.isFavorite = isFavorite
            val iconRes = if (withAnimation) {
                if (isFavorite) R.drawable.avd_favorite else R.drawable.avd_unfavorite
            } else {
                if (isFavorite) R.drawable.ic_favorite_24dp else R.drawable.ic_favorite_outline_24dp
            }
            binding.favorite.setImageResource(iconRes)
            binding.favorite.drawable?.let {
                if (it is AnimatedVectorDrawable) {
                    it.start()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}