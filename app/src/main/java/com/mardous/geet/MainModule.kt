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

package com.mardous.geet

import androidx.preference.PreferenceManager
import androidx.room.Room
import com.mardous.geet.activities.tageditor.TagEditorViewModel
import com.mardous.geet.androidauto.AutoMusicProvider
import com.mardous.geet.audio.SoundSettings
import com.mardous.geet.database.BoomingDatabase
import com.mardous.geet.fragments.LibraryViewModel
import com.mardous.geet.fragments.albums.AlbumDetailViewModel
import com.mardous.geet.fragments.artists.ArtistDetailViewModel
import com.mardous.geet.fragments.equalizer.EqualizerViewModel
import com.mardous.geet.fragments.folders.FolderDetailViewModel
import com.mardous.geet.fragments.genres.GenreDetailViewModel
import com.mardous.geet.fragments.info.InfoViewModel
import com.mardous.geet.fragments.lyrics.LyricsViewModel
import com.mardous.geet.fragments.playlists.PlaylistDetailViewModel
import com.mardous.geet.fragments.search.SearchViewModel
import com.mardous.geet.fragments.sound.SoundSettingsViewModel
import com.mardous.geet.fragments.years.YearDetailViewModel
import com.mardous.geet.helper.UriSongResolver
import com.mardous.geet.http.deezer.DeezerService
import com.mardous.geet.http.github.GitHubService
import com.mardous.geet.http.jsonHttpClient
import com.mardous.geet.http.lastfm.LastFmService
import com.mardous.geet.http.lyrics.LyricsService
import com.mardous.geet.http.provideDefaultCache
import com.mardous.geet.http.provideOkHttp
import com.mardous.geet.model.Genre
import com.mardous.geet.providers.MediaStoreWriter
import com.mardous.geet.repository.*
import com.mardous.geet.service.equalizer.EqualizerManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

val networkModule = module {
    factory {
        jsonHttpClient(get())
    }
    factory {
        provideDefaultCache()
    }
    factory {
        provideOkHttp(get(), get())
    }
    single {
        GitHubService(androidContext(), get())
    }
    single {
        DeezerService(get())
    }
    single {
        LastFmService(get())
    }
    single {
        LyricsService(androidContext(), get())
    }
}

private val autoModule = module {
    single {
        AutoMusicProvider(androidContext(), get())
    }
}

private val mainModule = module {
    single {
        androidContext().contentResolver
    }
    single {
        EqualizerManager(androidContext())
    }
    single {
        SoundSettings(androidContext())
    }
    single {
        MediaStoreWriter(androidContext(), get())
    }
    single {
        PreferenceManager.getDefaultSharedPreferences(androidContext())
    }
}

private val roomModule = module {
    single {
        Room.databaseBuilder(androidContext(), BoomingDatabase::class.java, "music_database.db")
            .build()
    }

    factory {
        get<BoomingDatabase>().playlistDao()
    }

    factory {
        get<BoomingDatabase>().playCountDao()
    }

    factory {
        get<BoomingDatabase>().historyDao()
    }

    factory {
        get<BoomingDatabase>().inclExclDao()
    }

    factory {
        get<BoomingDatabase>().lyricsDao()
    }
}

private val dataModule = module {
    single {
        RealRepository(
            androidContext(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get()
        )
    } bind Repository::class

    single {
        RealSongRepository(get())
    } bind SongRepository::class

    single {
        RealAlbumRepository(get())
    } bind AlbumRepository::class

    single {
        RealArtistRepository(get(), get())
    } bind ArtistRepository::class

    single {
        RealPlaylistRepository(androidContext(), get(), get())
    } bind PlaylistRepository::class

    single {
        RealGenreRepository(get(), get())
    } bind GenreRepository::class

    single {
        RealSearchRepository(get(), get(), get(), get(), get(), get())
    } bind SearchRepository::class

    single {
        RealSmartRepository(androidContext(), get(), get(), get(), get(), get())
    } bind SmartRepository::class

    single {
        RealSpecialRepository(get())
    } bind SpecialRepository::class

    single {
        UriSongResolver(androidContext(), get(), get())
    }
}

private val viewModule = module {
    viewModel {
        LibraryViewModel(get(), get(), get(), get())
    }

    viewModel {
        EqualizerViewModel(get(), get(), get())
    }

    viewModel { (albumId: Long) ->
        AlbumDetailViewModel(get(), albumId)
    }

    viewModel { (artistId: Long, artistName: String?) ->
        ArtistDetailViewModel(get(), artistId, artistName)
    }

    viewModel { (playlistId: Long) ->
        PlaylistDetailViewModel(get(), playlistId)
    }

    viewModel { (genre: Genre) ->
        GenreDetailViewModel(get(), genre)
    }

    viewModel { (year: Int) ->
        YearDetailViewModel(get(), year)
    }

    viewModel { (path: String) ->
        FolderDetailViewModel(get(), path)
    }

    viewModel {
        SearchViewModel(get())
    }

    viewModel { (id: Long, name: String?) ->
        TagEditorViewModel(get(), id, name)
    }

    viewModel {
        LyricsViewModel(get(), get())
    }

    viewModel {
        InfoViewModel(get())
    }

    viewModel {
        SoundSettingsViewModel(get())
    }
}

val appModules = listOf(networkModule, autoModule, mainModule, roomModule, dataModule, viewModule)