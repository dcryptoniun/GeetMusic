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

package com.mardous.geet.fragments.folders

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.content.edit
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.mardous.geet.R
import com.mardous.geet.adapters.FolderAdapter
import com.mardous.geet.extensions.navigation.folderDetailArgs
import com.mardous.geet.fragments.ReloadType
import com.mardous.geet.fragments.base.AbsRecyclerViewCustomGridSizeFragment
import com.mardous.geet.helper.menu.onSongsMenu
import com.mardous.geet.interfaces.IFolderCallback
import com.mardous.geet.model.Folder
import com.mardous.geet.model.GridViewType
import com.mardous.geet.util.sort.SortOrder
import com.mardous.geet.util.sort.prepareSortOrder
import com.mardous.geet.util.sort.selectedSortOrder

class FoldersListFragment : AbsRecyclerViewCustomGridSizeFragment<FolderAdapter, GridLayoutManager>(),
    IFolderCallback {

    override val titleRes: Int = R.string.folders_label
    override val isShuffleVisible: Boolean = false

    override val defaultGridSize: Int
        get() = if (isLandscape) resources.getInteger(R.integer.default_list_columns_land)
        else resources.getInteger(R.integer.default_list_columns)

    override val maxGridSize: Int
        get() = if (isLandscape) resources.getInteger(R.integer.default_grid_columns_land)
        else resources.getInteger(R.integer.default_grid_columns)

    override val maxGridSizeForList: Int
        get() = gridSize

    override val itemLayoutRes: Int
        get() = if (isGridMode) R.layout.item_grid_gradient
        else R.layout.item_list

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        libraryViewModel.getFolders().observe(viewLifecycleOwner) { folders ->
            adapter?.submitList(folders)
        }
    }

    override fun onResume() {
        super.onResume()
        libraryViewModel.forceReload(ReloadType.Folders)
    }

    override fun createLayoutManager(): GridLayoutManager {
        return GridLayoutManager(requireActivity(), gridSize)
    }

    override fun createAdapter(): FolderAdapter {
        notifyLayoutResChanged(itemLayoutRes)
        val dataSet = adapter?.folders ?: ArrayList()
        return FolderAdapter(mainActivity, dataSet, itemLayoutRes, this)
    }

    override fun folderClick(folder: Folder) {
        findNavController().navigate(R.id.nav_folder_detail, folderDetailArgs(folder))
    }

    override fun folderMenuItemClick(folder: Folder, menuItem: MenuItem): Boolean {
        if (menuItem.itemId == R.id.action_blacklist) {
            libraryViewModel.blacklistPath(folder.file)
            return true
        }
        return folder.songs.onSongsMenu(this, menuItem)
    }

    override fun foldersMenuItemClick(selection: List<Folder>, menuItem: MenuItem): Boolean {
        libraryViewModel.songs(selection).observe(viewLifecycleOwner) { songs ->
            songs.onSongsMenu(this, menuItem)
        }
        return true
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateMenu(menu, inflater)
        menu.removeItem(R.id.action_view_type)
        val sortOrderSubmenu = menu.findItem(R.id.action_sort_order)?.subMenu
        if (sortOrderSubmenu != null) {
            sortOrderSubmenu.clear()
            sortOrderSubmenu.add(0, R.id.action_sort_order_az, 0, R.string.sort_order_az)
            sortOrderSubmenu.add(0, R.id.action_sort_order_number_of_songs, 1, R.string.sort_order_number_of_songs)
            sortOrderSubmenu.add(1, R.id.action_sort_order_descending, 2, R.string.sort_order_descending)
            sortOrderSubmenu.setGroupCheckable(0, true, true)
            sortOrderSubmenu.prepareSortOrder(SortOrder.folderSortOrder)
        }
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        if (item.selectedSortOrder(SortOrder.folderSortOrder)) {
            libraryViewModel.forceReload(ReloadType.Folders)
            return true
        }
        return super.onMenuItemSelected(item)
    }

    override fun onMediaStoreChanged() {
        super.onMediaStoreChanged()
        libraryViewModel.forceReload(ReloadType.Folders)
    }

    override fun getSavedViewType(): GridViewType = GridViewType.Normal

    override fun saveViewType(viewType: GridViewType) {}

    override fun getSavedGridSize(): Int {
        return sharedPreferences.getInt(FOLDERS_GRID_SIZE_KEY, defaultGridSize)
    }

    override fun saveGridSize(newGridSize: Int) {
        sharedPreferences.edit {
            putInt(FOLDERS_GRID_SIZE_KEY, newGridSize)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onGridSizeChanged(isLand: Boolean, gridColumns: Int) {
        layoutManager?.spanCount = gridColumns
        adapter?.notifyDataSetChanged()
    }

    companion object {
        private const val FOLDERS_GRID_SIZE_KEY = "folders_grid_size"
    }
}