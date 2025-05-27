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

package com.teqanta.geet.search

import android.content.Context
import android.os.Parcelable
import android.provider.MediaStore.Audio.AudioColumns
import com.teqanta.geet.R
import com.teqanta.geet.database.PlaylistEntity
import com.teqanta.geet.extensions.media.displayName
import com.teqanta.geet.model.Album
import com.teqanta.geet.model.Artist
import com.teqanta.geet.model.Folder
import com.teqanta.geet.model.Genre
import com.teqanta.geet.model.ReleaseYear
import com.teqanta.geet.search.SearchQuery.FilterMode
import com.teqanta.geet.search.filters.BasicSearchFilter
import com.teqanta.geet.search.filters.LastAddedSearchFilter
import com.teqanta.geet.search.filters.SmartSearchFilter
import kotlinx.parcelize.Parcelize

/**
 * @author Christians M. A. (teqanta)
 */
interface SearchFilter : Parcelable {
    fun getName(): CharSequence

    fun getCompatibleModes(): List<FilterMode>

    suspend fun getResults(searchMode: FilterMode, query: String): List<Any>
}

@Parcelize
class FilterSelection(
    /**
     * What mode this filter work with
     */
    internal val mode: FilterMode,
    /**
     * What column this filter search for
     */
    internal val column: String,
    /**
     * How this filter will select what it's searching.
     */
    internal val selection: String,
    /**
     * What arguments this filter will pass to the repository.
     */
    internal vararg val arguments: String
) : Parcelable

/**
 * Return a [SearchFilter] that may be used to search songs from this album.
 */
fun Album.searchFilter(context: Context) =
    SmartSearchFilter(
        context.getString(R.string.search_album_x_label, name), null,
        FilterSelection(FilterMode.Songs, AudioColumns.TITLE, AudioColumns.ALBUM_ID + "=?", id.toString())
    )

/**
 * Return a [SearchFilter] that may be used to search songs and albums from this artist.
 */
fun Artist.searchFilter(context: Context): SmartSearchFilter {
    return if (isAlbumArtist) {
        SmartSearchFilter(
            context.getString(R.string.search_artist_x_label, displayName()), null,
            FilterSelection(FilterMode.Songs, AudioColumns.TITLE, "${AudioColumns.ALBUM_ARTIST}=?", name),
            FilterSelection(FilterMode.Albums, AudioColumns.ALBUM, "${AudioColumns.ALBUM_ARTIST}=?", name)
        )
    } else {
        SmartSearchFilter(
            context.getString(R.string.search_artist_x_label, displayName()), null,
            FilterSelection(FilterMode.Songs, AudioColumns.TITLE, "${AudioColumns.ARTIST_ID}=?", id.toString()),
            FilterSelection(FilterMode.Albums, AudioColumns.ALBUM, "${AudioColumns.ARTIST_ID}=?", id.toString())
        )
    }
}

fun Folder.searchFilter(context: Context): SearchFilter =
    BasicSearchFilter(context.getString(R.string.search_in_folder_x, name), this)

fun Genre.searchFilter(context: Context): SearchFilter =
    BasicSearchFilter(context.getString(R.string.search_from_x_label, name), this)

fun ReleaseYear.searchFilter(context: Context): SearchFilter =
    BasicSearchFilter(context.getString(R.string.search_year_x_label, name), this)

fun PlaylistEntity.searchFilter(context: Context): SearchFilter =
    BasicSearchFilter(context.getString(R.string.search_list_x_label, playlistName), this)

fun lastAddedSearchFilter(context: Context): SearchFilter =
    LastAddedSearchFilter(context.getString(R.string.search_last_added))