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

package com.mardous.booming.fragments.lyrics

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.mardous.booming.appContext
import com.mardous.booming.database.LyricsDao
import com.mardous.booming.database.toLyricsEntity
import com.mardous.booming.extensions.files.getBestTag
import com.mardous.booming.extensions.files.getContentUri
import com.mardous.booming.extensions.files.readString
import com.mardous.booming.extensions.files.toAudioFile
import com.mardous.booming.extensions.hasR
import com.mardous.booming.extensions.isAllowedToDownloadMetadata
import com.mardous.booming.extensions.media.isArtistNameUnknown
import com.mardous.booming.http.Result
import com.mardous.booming.http.lyrics.LyricsService
import com.mardous.booming.lyrics.LrcLyrics
import com.mardous.booming.lyrics.LrcUtils
import com.mardous.booming.misc.TagWriter
import com.mardous.booming.model.DownloadedLyrics
import com.mardous.booming.model.Song
import com.mardous.booming.mvvm.LyricsResult
import com.mardous.booming.mvvm.LyricsSource
import com.mardous.booming.mvvm.LyricsType
import com.mardous.booming.mvvm.SaveLyricsResult
import com.mardous.booming.recordException
import com.mardous.booming.util.LyricsUtil
import com.mardous.booming.util.UriUtil
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import org.jaudiotagger.tag.FieldKey
import java.io.File
import java.util.EnumMap

/**
 * @author Christians M. A. (mardous)
 */
class LyricsViewModel(
    private val lyricsDao: LyricsDao,
    private val lyricsService: LyricsService
) : ViewModel() {

    private val silentHandler = CoroutineExceptionHandler { _, _ -> }

    fun getOnlineLyrics(song: Song, title: String, artist: String): LiveData<Result<DownloadedLyrics>> = liveData(IO) {
        if (song.id == Song.emptySong.id) {
            emit(Result.Error(IllegalArgumentException("Song is not valid")))
        } else {
            emit(Result.Loading)
            if (artist.isArtistNameUnknown()) {
                emit(Result.Error(IllegalArgumentException("Artist name is <unknown>")))
            } else {
                val result = try {
                    Result.Success(lyricsService.getLyrics(song, title, artist))
                } catch (e: Exception) {
                    Result.Error(e)
                }
                emit(result)
            }
        }
    }

    fun getAllLyrics(
        song: Song,
        allowDownload: Boolean = false,
        fromEditor: Boolean = false
    ): LiveData<LyricsResult> = liveData(IO + silentHandler) {
        check(song.id != Song.emptySong.id)

        emit(LyricsResult(id = song.id, loading = true))

        val embeddedLyrics = getEmbeddedLyrics(song).orEmpty()
        val embeddedSynced = LrcUtils.parse(embeddedLyrics)

        val localLrc = LyricsUtil.getSyncedLyricsFile(song)?.let { LrcUtils.parseLrcFromFile(it) }
        if (localLrc?.hasLines == true) {
            val embeddedSource = if (embeddedSynced.hasLines) LyricsSource.EmbeddedSynced else LyricsSource.Embedded
            emit(
                LyricsResult(
                    song.id,
                    embeddedLyrics,
                    localLrc,
                    sources = hashMapOf(
                        LyricsType.Embedded to embeddedSource,
                        LyricsType.External to LyricsSource.Lrc
                    )
                )
            )
            return@liveData
        }

        val storedSynced = lyricsDao.getLyrics(song.id)?.let { LrcUtils.parse(it.syncedLyrics) }
        if (embeddedSynced.hasLines) {
            if (fromEditor) {
                val lrcData = if (storedSynced?.hasLines == true) storedSynced else LrcLyrics()
                emit(
                    LyricsResult(
                        song.id,
                        data = embeddedLyrics,
                        lrcData = lrcData,
                        sources = hashMapOf(LyricsType.Embedded to LyricsSource.EmbeddedSynced)
                    )
                )
            } else {
                emit(LyricsResult(song.id, lrcData = embeddedSynced))
            }
            return@liveData
        }

        if (storedSynced?.hasLines == true) {
            emit(LyricsResult(song.id, data = embeddedLyrics, lrcData = storedSynced))
            return@liveData
        }

        if (allowDownload && appContext().isAllowedToDownloadMetadata()) {
            val downloaded = runCatching { lyricsService.getLyrics(song) }.getOrNull()
            if (downloaded?.isSynced == true) {
                val lrcData = LrcUtils.parse(downloaded.syncedLyrics!!)
                if (lrcData.hasLines) {
                    lyricsDao.insertLyrics(song.toLyricsEntity(lrcData.getText(), autoDownload = true))
                    emit(LyricsResult(song.id, data = embeddedLyrics, lrcData = lrcData))
                    return@liveData
                }
            }
        }

        emit(LyricsResult(song.id, embeddedLyrics))
    }

    fun getLyrics(song: Song): LiveData<LyricsResult> =
        liveData(IO + silentHandler) {
            if (song.id != Song.emptySong.id) {
                emit(LyricsResult(song.id, getEmbeddedLyrics(song)))
            }
        }

    fun deleteLyrics() = viewModelScope.launch(IO) {
        lyricsDao.removeLyrics()
    }

    fun shareSyncedLyrics(context: Context, song: Song): LiveData<Uri?> = liveData(IO) {
        if (song.id == Song.emptySong.id) {
            emit(null)
        } else {
            val lyrics = lyricsDao.getLyrics(song.id)
            if (lyrics != null) {
                val tempFile = appContext().externalCacheDir
                    ?.resolve("${song.artistName} - ${song.title}.lrc")
                if (tempFile == null) {
                    emit(null)
                } else {
                    val result = runCatching {
                        tempFile.bufferedWriter().use {
                            it.write(lyrics.syncedLyrics)
                        }
                        tempFile.getContentUri(context)
                    }
                    if (result.isSuccess) {
                        emit(result.getOrThrow())
                    } else {
                        emit(null)
                    }
                }
            } else {
                emit(null)
            }
        }
    }

    fun saveLyrics(
        context: Context,
        song: Song,
        plainLyrics: String?,
        syncedLyrics: String?,
        plainLyricsModified: Boolean
    ): LiveData<SaveLyricsResult> = liveData(IO) {
        val pendingLrcFile = try {
            saveSyncedLyrics(context, song, syncedLyrics)
        } catch (e: Exception) {
            recordException(e)
            null
        }
        if (!plainLyricsModified) {
            if (pendingLrcFile != null) {
                emit(
                    SaveLyricsResult(
                        isPending = true,
                        isSuccess = false,
                        pendingWrite = listOf(pendingLrcFile)
                    )
                )
            } else {
                emit(SaveLyricsResult(isPending = false, isSuccess = true))
            }
        } else {
            val fieldKeyValueMap = EnumMap<FieldKey, String>(FieldKey::class.java).apply {
                put(FieldKey.LYRICS, plainLyrics)
            }
            val writeInfo = TagWriter.WriteInfo(listOf(song.data), fieldKeyValueMap, null)
            if (hasR()) {
                val pending = runCatching {
                    TagWriter.writeTagsToFilesR(context, writeInfo).first() to song.mediaStoreUri
                }
                if (pending.isSuccess) {
                    emit(
                        SaveLyricsResult(
                            isPending = true,
                            isSuccess = false,
                            pendingWrite = listOfNotNull(pendingLrcFile, pending.getOrThrow())
                        )
                    )
                } else {
                    emit(SaveLyricsResult(isPending = false, isSuccess = false))
                }
            } else {
                val result = runCatching {
                    TagWriter.writeTagsToFiles(context, writeInfo)
                }
                if (result.isSuccess) {
                    emit(SaveLyricsResult(isPending = false, isSuccess = true))
                } else {
                    emit(SaveLyricsResult(isPending = false, isSuccess = false))
                }
            }
        }
    }

    private suspend fun saveSyncedLyrics(context: Context, song: Song, syncedLyrics: String?): Pair<File, Uri>? {
        val localLrcFile = LyricsUtil.getSyncedLyricsFile(song)
        if (localLrcFile != null) {
            if (hasR()) {
                val destinationUri = UriUtil.getUriFromPath(context, localLrcFile.absolutePath)
                val cacheFile = File(context.cacheDir, localLrcFile.name).also {
                    it.writeText(syncedLyrics ?: "")
                }
                return cacheFile to destinationUri
            } else {
                LyricsUtil.writeLrc(song, syncedLyrics ?: "")
            }
        } else {
            if (syncedLyrics.isNullOrEmpty()) {
                val lyrics = lyricsDao.getLyrics(song.id)
                if (lyrics != null) {
                    if (lyrics.autoDownload) {
                        // The user has deleted an automatically downloaded lyrics, perhaps
                        // because it was incorrect. In this case we do not delete the
                        // registry, we simply clean it, this way it will prevent us from
                        // trying to download it again in the future.
                        lyricsDao.insertLyrics(song.toLyricsEntity("", userCleared = true))
                    } else if (!lyrics.userCleared) {
                        lyricsDao.removeLyrics(song.id)
                    }
                }
            } else {
                val parsedLyrics = LrcUtils.parse(syncedLyrics)
                if (parsedLyrics.hasLines) {
                    lyricsDao.insertLyrics(song.toLyricsEntity(parsedLyrics.getText()))
                }
            }
        }
        return null
    }

    fun setLRCContentFromUri(context: Context, song: Song, uri: Uri?): LiveData<Boolean> =
        liveData(IO) {
            val path = uri?.path
            if (path != null && path.endsWith(".lrc")) {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val content = runCatching { stream.readString() }
                    if (content.isSuccess) {
                        val parsed = LrcUtils.parse(content.getOrThrow())
                        if (parsed.hasLines) {
                            lyricsDao.insertLyrics(song.toLyricsEntity(parsed.getText()))
                            emit(true)
                        } else {
                            emit(false)
                        }
                    } else {
                        emit(false)
                    }
                }
            } else {
                emit(false)
            }
        }

    private fun getEmbeddedLyrics(song: Song): String? {
        val file = File(song.data)
        try {
            return file.toAudioFile()?.getBestTag(false)?.getFirst(FieldKey.LYRICS)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return null
    }
}