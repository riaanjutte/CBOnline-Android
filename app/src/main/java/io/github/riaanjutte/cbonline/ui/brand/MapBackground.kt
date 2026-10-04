package io.github.riaanjutte.cbonline.ui.brand

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import io.github.riaanjutte.cbonline.BuildConfig
import io.github.riaanjutte.cbonline.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The WWII operations map behind everything; fixed, it doesn't scroll with the list.
 *
 * Decoded at half size (960 × 1000) in 16-bit colour, about 2 MB instead of 15 MB: it's a faint, 12-colour texture
 * mostly covered by panels, so neither shows. It's decoded once per app run, so turning the phone (which rebuilds
 * the screen) shows it straight away. Until the first decode, the window's dark background shows through.
 */
@Composable
fun MapBackground(modifier: Modifier = Modifier) {
    val resources = LocalContext.current.resources
    val map: ImageBitmap? by produceState(decodedMap.value, resources) {
        if (value == null) value = withContext(Dispatchers.IO) { decodedMap.getOrMake { decodeMap(resources) } }
    }
    map?.let {
        Image(bitmap = it, contentDescription = null, modifier = modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}

private val decodedMap = KeptValue<ImageBitmap>()

private fun decodeMap(resources: Resources): ImageBitmap? {
    if (BuildConfig.DEBUG) Log.d("CBOnline", "decode map")
    val options = BitmapFactory.Options().apply {
        inSampleSize = 2
        inPreferredConfig = Bitmap.Config.RGB_565
    }
    return BitmapFactory.decodeResource(resources, R.drawable.cb_map, options)?.asImageBitmap()
}

/** Holds the first value made successfully; a failed attempt (null) is tried again next time. */
internal class KeptValue<T : Any> {
    @Volatile
    var value: T? = null
        private set

    @Synchronized
    fun getOrMake(make: () -> T?): T? = value ?: make()?.also { value = it }
}
