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

package com.teqanta.geet.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.teqanta.geet.R
import com.teqanta.geet.adapters.base.MediaEntryViewHolder
import com.teqanta.geet.adapters.extension.setColors
import com.teqanta.geet.extensions.glide.asBitmapPalette
import com.teqanta.geet.extensions.glide.getSongGlideModel
import com.teqanta.geet.extensions.glide.songOptions
import com.teqanta.geet.extensions.media.sectionName
import com.teqanta.geet.extensions.media.songsStr
import com.teqanta.geet.extensions.resources.hide
import com.teqanta.geet.extensions.resources.toColorStateList
import com.teqanta.geet.extensions.resources.useAsIcon
import com.teqanta.geet.glide.GeetColoredTarget
import com.teqanta.geet.helper.color.MediaNotificationProcessor
import com.teqanta.geet.interfaces.IGenreCallback
import com.teqanta.geet.model.Genre
import com.teqanta.geet.repository.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.zhanghai.android.fastscroll.PopupTextProvider
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.properties.Delegates
import kotlin.reflect.KProperty

/**
 * @author Christians M. A. (teqanta)
 */
class GenreAdapter(
    private val requestManager: RequestManager,
    dataSet: List<Genre>,
    @LayoutRes
    private val itemLayoutRes: Int,
    private val uiScope: CoroutineScope,
    private val callback: IGenreCallback?,
) : RecyclerView.Adapter<GenreAdapter.ViewHolder>(), PopupTextProvider, KoinComponent {

    private val repository: Repository by inject()

    var dataSet by Delegates.observable(dataSet) { _: KProperty<*>, _: List<Genre>, _: List<Genre> ->
        notifyDataSetChanged()
    }

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(itemLayoutRes, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val genre = dataSet[position]
        holder.title?.text = genre.name
        if (itemLayoutRes == R.layout.item_grid_gradient) {
            loadGenreImage(holder, genre)
            holder.text?.text = genre.songCount.toString()
        } else {
            holder.image?.setImageResource(R.drawable.ic_radio_24dp)
            holder.text?.text = genre.songCount.songsStr(holder.itemView.context)
        }
    }

    private fun loadGenreImage(holder: ViewHolder, genre: Genre) {
        if (holder.image != null) {
            uiScope.launch {
                val song = withContext(Dispatchers.IO) { repository.songByGenre(genre.id) }
                requestManager.asBitmapPalette()
                    .load(song.getSongGlideModel())
                    .songOptions(song)
                    .into(object : GeetColoredTarget(holder.image) {
                        override fun onColorReady(colors: MediaNotificationProcessor) {
                            holder.setColors(colors)
                            if (holder.text != null) {
                                TextViewCompat.setCompoundDrawableTintList(
                                    holder.text, colors.secondaryTextColor.toColorStateList()
                                )
                            }
                        }
                    })
            }
        }
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    override fun getItemId(position: Int): Long {
        return dataSet[position].id
    }

    override fun getPopupText(view: View, position: Int): CharSequence {
        val genre = dataSet.getOrNull(position) ?: return ""
        return if (genre.id != -1L) genre.name.sectionName() else ""
    }

    inner class ViewHolder(itemView: View) : MediaEntryViewHolder(itemView) {

        override fun onClick(view: View) {
            callback?.genreClick(dataSet[layoutPosition])
        }

        init {
            if (itemLayoutRes == R.layout.item_list) {
                image?.useAsIcon()
            }
            menu?.hide()
        }
    }
}