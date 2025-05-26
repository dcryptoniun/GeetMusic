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

package com.mardous.geet.glide

import android.content.Context
import android.graphics.Bitmap
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.AppGlideModule
import com.mardous.geet.glide.artistimage.ArtistImage
import com.mardous.geet.glide.artistimage.ArtistImageLoader
import com.mardous.geet.glide.audiocover.AudioFileCover
import com.mardous.geet.glide.audiocover.AudioFileCoverLoader
import com.mardous.geet.glide.palette.BitmapPaletteTranscoder
import com.mardous.geet.glide.palette.BitmapPaletteWrapper
import com.mardous.geet.glide.playlistPreview.PlaylistPreview
import com.mardous.geet.glide.playlistPreview.PlaylistPreviewLoader
import java.io.InputStream

@GlideModule
class BoomingGlideModule : AppGlideModule() {

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        registry.append(PlaylistPreview::class.java, Bitmap::class.java, PlaylistPreviewLoader.Factory(context))
        registry.append(AudioFileCover::class.java, InputStream::class.java, AudioFileCoverLoader.Factory())
        registry.append(ArtistImage::class.java, InputStream::class.java, ArtistImageLoader.Factory(context))
        registry.register(Bitmap::class.java, BitmapPaletteWrapper::class.java, BitmapPaletteTranscoder())
    }

    override fun isManifestParsingEnabled(): Boolean {
        return false
    }
}