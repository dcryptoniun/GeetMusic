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

package com.teqanta.geet.fragments.player.styles.m3style

import android.animation.Animator
import android.animation.TimeInterpolator
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.slider.Slider
import com.teqanta.geet.R
import com.teqanta.geet.databinding.FragmentM3PlayerPlaybackControlsBinding
import com.teqanta.geet.extensions.resources.controlColorNormal
import com.teqanta.geet.extensions.resources.getPrimaryTextColor
import com.teqanta.geet.fragments.player.PlayerAnimator
import com.teqanta.geet.fragments.player.base.AbsPlayerControlsFragment
import com.teqanta.geet.helper.handler.PrevNextButtonOnTouchHandler
import com.teqanta.geet.model.Song
import com.teqanta.geet.service.MusicPlayer
import com.teqanta.geet.util.Preferences
import java.util.LinkedList

/**
 * @author Christians M. A. (teqanta)
 */
class M3PlayerControlsFragment : AbsPlayerControlsFragment(R.layout.fragment_m3_player_playback_controls) {

    private var _binding: FragmentM3PlayerPlaybackControlsBinding? = null
    private val binding get() = _binding!!

    override val playPauseFab: FloatingActionButton
        get() = binding.playPauseButton

    override val progressSlider: Slider
        get() = binding.progressSlider

    override val songCurrentProgress: TextView
        get() = binding.songCurrentProgress

    override val songTotalTime: TextView
        get() = binding.songTotalTime

    override val songInfoView: TextView?
        get() = binding.songInfo

    private var playbackControlsColor = 0
    private var disabledPlaybackControlsColor = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentM3PlayerPlaybackControlsBinding.bind(view)
        binding.playPauseButton.setOnClickListener(this)
        binding.nextButton.setOnTouchListener(PrevNextButtonOnTouchHandler(PrevNextButtonOnTouchHandler.DIRECTION_NEXT))
        binding.previousButton.setOnTouchListener(PrevNextButtonOnTouchHandler(PrevNextButtonOnTouchHandler.DIRECTION_PREVIOUS))
        binding.shuffleButton.setOnClickListener(this)
        binding.repeatButton.setOnClickListener(this)

        playbackControlsColor = controlColorNormal()
        disabledPlaybackControlsColor = getPrimaryTextColor(requireContext(), isDisabled = true)
        setColors(Color.TRANSPARENT, playbackControlsColor, disabledPlaybackControlsColor)
    }

    override fun onCreatePlayerAnimator(): PlayerAnimator {
        return M3PlayerAnimator(binding, Preferences.animateControls)
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

    override fun onClick(view: View) {
        super.onClick(view)
        when (view) {
            binding.repeatButton -> MusicPlayer.cycleRepeatMode()
            binding.shuffleButton -> MusicPlayer.toggleShuffleMode()
            binding.playPauseButton -> MusicPlayer.togglePlayPause()
        }
    }

    override fun setColors(backgroundColor: Int, primaryControlColor: Int, secondaryControlColor: Int) {
        _binding?.shuffleButton?.setColors(secondaryControlColor, primaryControlColor)
        _binding?.repeatButton?.setColors(secondaryControlColor, primaryControlColor)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class M3PlayerAnimator(
        private val binding: FragmentM3PlayerPlaybackControlsBinding,
        isEnabled: Boolean
    ) : PlayerAnimator(isEnabled) {
        override fun onAddAnimation(animators: LinkedList<Animator>, interpolator: TimeInterpolator) {
            addScaleAnimation(animators, binding.shuffleButton, interpolator, 100)
            addScaleAnimation(animators, binding.repeatButton, interpolator, 100)
            addScaleAnimation(animators, binding.previousButton, interpolator, 100)
            addScaleAnimation(animators, binding.nextButton, interpolator, 100)
            addScaleAnimation(animators, binding.songCurrentProgress, interpolator, 200)
            addScaleAnimation(animators, binding.songTotalTime, interpolator, 200)
        }

        override fun onPrepareForAnimation() {
            prepareForScaleAnimation(binding.previousButton)
            prepareForScaleAnimation(binding.nextButton)
            prepareForScaleAnimation(binding.shuffleButton)
            prepareForScaleAnimation(binding.repeatButton)
            prepareForScaleAnimation(binding.songCurrentProgress)
            prepareForScaleAnimation(binding.songTotalTime)
        }
    }
}