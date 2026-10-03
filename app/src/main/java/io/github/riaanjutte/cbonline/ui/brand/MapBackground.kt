package io.github.riaanjutte.cbonline.ui.brand

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import io.github.riaanjutte.cbonline.R

/** The WWII operations map behind everything; fixed, it doesn't scroll with the list. */
@Composable
fun MapBackground(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.cb_map),
        contentDescription = null,
        modifier = modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
    )
}
