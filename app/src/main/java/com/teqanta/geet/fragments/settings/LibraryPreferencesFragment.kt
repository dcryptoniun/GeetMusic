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

package com.teqanta.geet.fragments.settings

import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import com.teqanta.geet.R
import com.teqanta.geet.database.InclExclDao
import com.teqanta.geet.dialogs.library.BlacklistWhitelistDialog
import com.teqanta.geet.extensions.hasR
import com.teqanta.geet.fragments.LibraryViewModel
import com.teqanta.geet.fragments.ReloadType
import com.teqanta.geet.preferences.SwitchWithButtonPreference
import com.teqanta.geet.util.BLACKLIST_ENABLED
import com.teqanta.geet.util.LAST_ADDED_CUTOFF
import com.teqanta.geet.util.TRASH_MUSIC_FILES
import com.teqanta.geet.util.WHITELIST_ENABLED
import org.koin.androidx.viewmodel.ext.android.activityViewModel

/**
 * @author Christians M. A. (teqanta)
 */
class LibraryPreferencesFragment : PreferencesScreenFragment() {

    private val libraryViewModel by activityViewModel<LibraryViewModel>()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_screen_library)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!hasR()) {
            findPreference<Preference>(TRASH_MUSIC_FILES)?.isVisible = false
        }

        findPreference<Preference>(LAST_ADDED_CUTOFF)?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, _ ->
                libraryViewModel.forceReload(ReloadType.Suggestions)
                true
            }

        findPreference<SwitchWithButtonPreference>(WHITELIST_ENABLED)?.apply {
            setButtonPressedListener(object : SwitchWithButtonPreference.OnButtonPressedListener {
                override fun onButtonPressed() {
                    showLibraryFolderSelector(InclExclDao.WHITELIST)
                }
            })
        }

        findPreference<SwitchWithButtonPreference>(BLACKLIST_ENABLED)?.apply {
            setButtonPressedListener(object : SwitchWithButtonPreference.OnButtonPressedListener {
                override fun onButtonPressed() {
                    showLibraryFolderSelector(InclExclDao.BLACKLIST)
                }
            })
        }
    }

    private fun showLibraryFolderSelector(type: Int) {
        BlacklistWhitelistDialog.newInstance(type).show(childFragmentManager, "LIBRARY_PATHS_PREFERENCE")
    }
}