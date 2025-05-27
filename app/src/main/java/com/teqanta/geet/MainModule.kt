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

package com.teqanta.geet

import androidx.preference.PreferenceManager
import androidx.room.Room
import com.teqanta.geet.activities.tageditor.TagEditorViewModel
import com.teqanta.geet.androidauto.AutoMusicProvider
import com.teqanta.geet.audio.SoundSettings
import com.teqanta.geet.database.GeetDatabase
import com.teqanta.geet.fragments.LibraryViewModel
import com.teqanta.geet.fragments.albums.AlbumDetailViewModel
import com.teqanta.geet.fragments.artists.ArtistDetailViewModel
import com.teqanta.geet.fragments.equalizer.EqualizerViewModel
import com.teqanta.geet.fragments.folders.FolderDetailViewModel
import com.teqanta.geet.fragments.genres.GenreDetailViewModel
import com.teqanta.geet.fragments.info.InfoViewModel
import com.teqanta.geet.fragments.lyrics.LyricsViewModel
import com.teqanta.geet.fragments.playlists.PlaylistDetailViewModel
import com.teqanta.geet.fragments.search.SearchViewModel
import com.teqanta.geet.fragments.sound.SoundSettingsViewModel
import com.teqanta.geet.fragments.years.YearDetailViewModel
import com.teqanta.geet.helper.UriSongResolver
import com.teqanta.geet.http.deezer.DeezerService
import com.teqanta.geet.http.github.GitHubService
import com.teqanta.geet.http.jsonHttpClient
import com.teqanta.geet.http.lastfm.LastFmService
import com.teqanta.geet.http.lyrics.LyricsService
import com.teqanta.geet.http.provideDefaultCache
import com.teqanta.geet.http.provideOkHttp
import com.teqanta.geet.model.Genre
import com.teqanta.geet.providers.MediaStoreWriter
import com.teqanta.geet.repository.*
import com.teqanta.geet.service.equalizer.EqualizerManager
import com.teqanta.geet.service.queue.ShuffleManager
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
        Room.databaseBuilder(androidContext(), GeetDatabase::class.java, "music_database.db")
            .build()
    }

    factory {
        get<GeetDatabase>().playlistDao()
    }

    factory {
        get<GeetDatabase>().playCountDao()
    }

    factory {
        get<GeetDatabase>().historyDao()
    }

    factory {
        get<GeetDatabase>().inclExclDao()
    }

    factory {
        get<GeetDatabase>().lyricsDao()
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

    single {
        ShuffleManager(get())
    }
}

private val viewModule = module {
    viewModel {
        LibraryViewModel(get(), get(), get(), get(), get())
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