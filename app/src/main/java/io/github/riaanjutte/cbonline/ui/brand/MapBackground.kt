package io.github.riaanjutte.cbonline.ui.brand

import android.graphics.BitmapFactory
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
import io.github.riaanjutte.cbonline.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The WWII operations map behind everything; fixed, it doesn't scroll with the list.
 *
 * Decoded at half size (960 × 1000, about 4 MB instead of 15 MB): it's a faint texture mostly covered by
 * panels, so the lost detail doesn't show. Until it's decoded, the window's dark background shows through.
 */
@Composable
fun MapBackground(modifier: Modifier = Modifier) {
    val resources = LocalContext.current.resources
    val map: ImageBitmap? by produceState<ImageBitmap?>(null, resources) {
        value = withContext(Dispatchers.IO) {
            val options = BitmapFactory.Options().apply { inSampleSize = 2 }
            BitmapFactory.decodeResource(resources, R.drawable.cb_map, options)?.asImageBitmap()
        }
    }
    map?.let {
        Image(bitmap = it, contentDescription = null, modifier = modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}
