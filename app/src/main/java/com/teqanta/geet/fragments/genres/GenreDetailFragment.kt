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

package com.teqanta.geet.fragments.genres

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.teqanta.geet.R
import com.teqanta.geet.adapters.extension.isNullOrEmpty
import com.teqanta.geet.adapters.song.SongAdapter
import com.teqanta.geet.databinding.FragmentDetailListBinding
import com.teqanta.geet.extensions.applyScrollableContentInsets
import com.teqanta.geet.extensions.materialSharedAxis
import com.teqanta.geet.extensions.media.songCountStr
import com.teqanta.geet.extensions.media.songsDurationStr
import com.teqanta.geet.extensions.navigation.searchArgs
import com.teqanta.geet.extensions.setSupportActionBar
import com.teqanta.geet.extensions.utilities.buildInfoString
import com.teqanta.geet.fragments.base.AbsMainActivityFragment
import com.teqanta.geet.helper.menu.onSongMenu
import com.teqanta.geet.helper.menu.onSongsMenu
import com.teqanta.geet.interfaces.ISongCallback
import com.teqanta.geet.model.Genre
import com.teqanta.geet.model.Song
import com.teqanta.geet.search.searchFilter
import com.teqanta.geet.service.MusicPlayer
import com.teqanta.geet.util.sort.SortOrder
import com.teqanta.geet.util.sort.prepareSortOrder
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class GenreDetailFragment : AbsMainActivityFragment(R.layout.fragment_detail_list), ISongCallback {

    private val arguments by navArgs<GenreDetailFragmentArgs>()
    private val detailViewModel: GenreDetailViewModel by viewModel {
        parametersOf(arguments.extraGenre)
    }
    private lateinit var genre: Genre
    private lateinit var songAdapter: SongAdapter
    private lateinit var requestManager: RequestManager
    private var _binding: FragmentDetailListBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDetailListBinding.bind(view)
        requestManager = Glide.with(this)
        genre = arguments.extraGenre

        materialSharedAxis(view)
        setSupportActionBar(binding.toolbar)
        view.applyScrollableContentInsets(binding.recyclerView)
        binding.collapsingAppBarLayout.title = genre.name
        binding.title.text = genre.name

        setupButtons()
        setupRecyclerView()
        detailViewModel.getSongs().observe(viewLifecycleOwner) {
            songs(it)
        }
    }

    private fun setupButtons() {
        binding.playAction.setOnClickListener {
            MusicPlayer.openQueue(songAdapter.dataSet, keepShuffleMode = false)
        }
        binding.shuffleAction.setOnClickListener {
            MusicPlayer.openQueueShuffle(songAdapter.dataSet)
        }
    }

    private fun setupRecyclerView() {
        songAdapter = SongAdapter(requireActivity(), requestManager, ArrayList(), R.layout.item_list, callback = this)
        binding.recyclerView.apply {
            itemAnimator = DefaultItemAnimator()
            layoutManager = LinearLayoutManager(requireContext())
            adapter = songAdapter
        }
        songAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                checkIsEmpty()
            }
        })
    }

    fun songs(songs: List<Song>) {
        binding.progressIndicator.hide()
        binding.subtitle.text = buildInfoString(
            songs.songCountStr(requireContext()),
            songs.songsDurationStr()
        )
        songAdapter.dataSet = songs
    }

    private fun checkIsEmpty() {
        if (songAdapter.isNullOrEmpty) {
            findNavController().navigateUp()
        }
    }

    override fun songMenuItemClick(
        song: Song,
        menuItem: MenuItem,
        sharedElements: Array<Pair<View, String>>?
    ): Boolean = song.onSongMenu(this, menuItem)

    override fun songsMenuItemClick(songs: List<Song>, menuItem: MenuItem) {
        songs.onSongsMenu(this, menuItem)
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_genre_detail, menu)
    }

    override fun onPrepareMenu(menu: Menu) {
        menu.prepareSortOrder(SortOrder.genreSongSortOrder)
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                findNavController().navigateUp()
                true
            }

            R.id.action_search -> {
                findNavController().navigate(
                    R.id.nav_search,
                    searchArgs(genre.searchFilter(requireContext()))
                )
                true
            }
            else -> songAdapter.dataSet.onSongsMenu(this, item)
        }
    }

    override fun onMediaStoreChanged() {
        super.onMediaStoreChanged()
        detailViewModel.loadGenreSongs()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
