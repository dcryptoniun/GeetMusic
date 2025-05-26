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

package com.mardous.geet.fragments.player.styles.defaultstyle

import android.animation.Animator
import android.animation.TimeInterpolator
import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.slider.Slider
import com.mardous.geet.R
import com.mardous.geet.databinding.FragmentDefaultPlayerPlaybackControlsBinding
import com.mardous.geet.extensions.resources.centerPivot
import com.mardous.geet.extensions.resources.controlColorNormal
import com.mardous.geet.extensions.resources.getPrimaryTextColor
import com.mardous.geet.extensions.resources.showBounceAnimation
import com.mardous.geet.fragments.player.PlayerAnimator
import com.mardous.geet.fragments.player.base.AbsPlayerControlsFragment
import com.mardous.geet.helper.handler.PrevNextButtonOnTouchHandler
import com.mardous.geet.model.NowPlayingAction
import com.mardous.geet.model.Song
import com.mardous.geet.service.MusicPlayer
import com.mardous.geet.util.Preferences
import java.util.LinkedList

/**
 * @author Christians M. A. (mardous)
 */
class DefaultPlayerControlsFragment : AbsPlayerControlsFragment(R.layout.fragment_default_player_playback_controls) {

    private var _binding: FragmentDefaultPlayerPlaybackControlsBinding? = null
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
        _binding = FragmentDefaultPlayerPlaybackControlsBinding.bind(view)
        setupViews()
        setupColors()
        setupListeners()
        setViewAction(binding.queueInfo, NowPlayingAction.OpenPlayQueue)
    }

    private fun setupViews() {
        binding.playPauseButton.doOnLayout {
            it.centerPivot()
        }
    }

    private fun setupColors() {
        playbackControlsColor = controlColorNormal()
        disabledPlaybackControlsColor = getPrimaryTextColor(requireContext(), isDisabled = true)
        setColors(Color.TRANSPARENT, playbackControlsColor, disabledPlaybackControlsColor)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupListeners() {
        binding.text.setOnClickListener(this)
        binding.playPauseButton.setOnClickListener(this)
        binding.nextButton.setOnTouchListener(PrevNextButtonOnTouchHandler(PrevNextButtonOnTouchHandler.DIRECTION_NEXT))
        binding.previousButton.setOnTouchListener(PrevNextButtonOnTouchHandler(PrevNextButtonOnTouchHandler.DIRECTION_PREVIOUS))
        binding.shuffleButton.setOnClickListener(this)
        binding.repeatButton.setOnClickListener(this)
    }

    override fun onCreatePlayerAnimator(): PlayerAnimator {
        return DefaultPlayerAnimator(binding, Preferences.animateControls)
    }

    override fun onSongInfoChanged(song: Song) {
        _binding?.let { nonNullBinding ->
            nonNullBinding.title.text = song.title
            nonNullBinding.text.text = getSongArtist(song)
            if (isExtraInfoEnabled()) {
                nonNullBinding.songInfo?.text = getExtraInfoString(song)
                nonNullBinding.songInfo?.isVisible = true
            } else {
                nonNullBinding.songInfo?.isVisible = false
            }
        }
    }

    override fun onQueueInfoChanged(newInfo: String?) {
        _binding?.queueInfo?.text = newInfo
    }

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

    override fun onShow() {
        super.onShow()
        binding.playPauseButton.animate()
            .scaleX(1f)
            .scaleY(1f)
            .rotation(360f)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    override fun onHide() {
        super.onHide()
        binding.playPauseButton.apply {
            scaleX = 0f
            scaleY = 0f
            rotation = 0f
        }
    }

    override fun onClick(view: View) {
        super.onClick(view)
        when (view) {
            binding.repeatButton -> MusicPlayer.cycleRepeatMode()
            binding.shuffleButton -> MusicPlayer.toggleShuffleMode()
            binding.playPauseButton -> {
                MusicPlayer.togglePlayPause()
                view.showBounceAnimation()
            }
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

    private class DefaultPlayerAnimator(
        private val binding: FragmentDefaultPlayerPlaybackControlsBinding,
        isEnabled: Boolean
    ) : PlayerAnimator(isEnabled) {
        override fun onAddAnimation(animators: LinkedList<Animator>, interpolator: TimeInterpolator) {
            addScaleAnimation(animators, binding.shuffleButton, interpolator, 100)
            addScaleAnimation(animators, binding.repeatButton, interpolator, 100)
            addScaleAnimation(animators, binding.previousButton, interpolator, 200)
            addScaleAnimation(animators, binding.nextButton, interpolator, 200)
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
