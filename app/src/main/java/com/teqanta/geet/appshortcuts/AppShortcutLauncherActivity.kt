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
package com.teqanta.geet.appshortcuts

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.core.os.bundleOf
import com.teqanta.geet.appshortcuts.shortcuttype.LastAddedShortcutType
import com.teqanta.geet.appshortcuts.shortcuttype.ShuffleAllShortcutType
import com.teqanta.geet.appshortcuts.shortcuttype.TopTracksShortcutType
import com.teqanta.geet.extensions.extraNotNull
import com.teqanta.geet.model.Playlist
import com.teqanta.geet.model.smartplaylist.LastAddedPlaylist
import com.teqanta.geet.model.smartplaylist.ShuffleAllPlaylist
import com.teqanta.geet.model.smartplaylist.TopTracksPlaylist
import com.teqanta.geet.service.MusicService
import com.teqanta.geet.service.constants.ServiceAction
import com.teqanta.geet.service.playback.Playback

/**
 * @author Adrian Campos
 */
class AppShortcutLauncherActivity : Activity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        when (extraNotNull(KEY_SHORTCUT_TYPE, SHORTCUT_TYPE_NONE).value) {
            SHORTCUT_TYPE_SHUFFLE_ALL -> {
                startServiceWithPlaylist(Playback.ShuffleMode.ON, ShuffleAllPlaylist())
                DynamicShortcutManager.reportShortcutUsed(this, ShuffleAllShortcutType.ID)
            }

            SHORTCUT_TYPE_TOP_TRACKS -> {
                startServiceWithPlaylist(Playback.ShuffleMode.OFF, TopTracksPlaylist())
                DynamicShortcutManager.reportShortcutUsed(this, TopTracksShortcutType.ID)
            }

            SHORTCUT_TYPE_LAST_ADDED -> {
                startServiceWithPlaylist(Playback.ShuffleMode.OFF, LastAddedPlaylist())
                DynamicShortcutManager.reportShortcutUsed(this, LastAddedShortcutType.ID)
            }
        }
        finish()
    }

    private fun startServiceWithPlaylist(shuffleMode: Int, playlist: Playlist) {
        val intent = Intent(this, MusicService::class.java)
        intent.action = ServiceAction.ACTION_PLAY_PLAYLIST

        val bundle = bundleOf(
            ServiceAction.Extras.EXTRA_PLAYLIST to playlist,
            ServiceAction.Extras.EXTRA_SHUFFLE_MODE to shuffleMode
        )
        intent.setPackage(this.packageName)

        intent.putExtras(bundle)

        startService(intent)
    }

    companion object {
        const val KEY_SHORTCUT_TYPE = "com.teqanta.geet.appshortcuts.ShortcutType"

        const val SHORTCUT_TYPE_SHUFFLE_ALL: Int = 0
        const val SHORTCUT_TYPE_TOP_TRACKS: Int = 1
        const val SHORTCUT_TYPE_LAST_ADDED: Int = 2
        const val SHORTCUT_TYPE_NONE: Int = 3
    }
}
