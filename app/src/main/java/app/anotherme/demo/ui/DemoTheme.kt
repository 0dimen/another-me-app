package app.anotherme.demo.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** v9 reference + requested v10 direction. All visual tokens live here.
 * Virtual 1280x480 units are scaled together to fit 1920x720 and tablet windows.
 * SATIN_BAR / DARK_KEY are generic demo variants, not an OEM-certified component.
 */
object DemoTheme {
    const val referenceWidth = 1280f
    const val referenceHeight = 480f
    val black = Color(0xFF000000)
    val panel = Color(0xFF0B0D10)
    val white = Color(0xFFF0F2F4)
    val muted = Color(0xFF969DA5)
    val line = Color(0xFF282D32)
    val amber = Color(0xFFFFD09A)
    val silverTop = Color(0xFFF0F2F4)
    val silverMid = Color(0xFF828A93)
    val silverBottom = Color(0xFFD1D6DC)
    val darkTop = Color(0xFF30353B)
    val darkBottom = Color(0xFF101215)
    val rail = 168.dp
    val half = 640.dp
    val contentPadding = 28.dp
    val outerPadding = 18.dp
    val gap = 12.dp
    val smallGap = 6.dp
    val hairline = 1.dp
    val keyHeight = 64.dp
    val actionHeight = 56.dp
    val dial = 132.dp
    val dialRing = 7.dp
    val dialInset = 12.dp
    val keyCorner = 5.dp
    val actionCorner = 14.dp
    val indicatorWidth = 24.dp
    val indicatorHeight = 3.dp
    val keyFaceHeight = 18.dp
    val title = 31.sp
    val subtitle = 21.sp
    val body = 17.sp
    val small = 13.sp
    val tiny = 11.sp
    val dialValue = 27.sp
    val titleLineHeight = 42.sp
    val subtitleLineHeight = 29.sp
    val bodyLineHeight = 24.sp
    val smallLineHeight = 19.sp
    val tinyLineHeight = 16.sp
    val dialLineHeight = 34.sp
    val timelineHeight = 122.dp
    val formWidth = 760.dp
    val formHeight = 560.dp
    val fieldHeight = 74.dp
    val listRowPadding = 14.dp
    val toolbarHeight = 68.dp
    val timelineStroke = 3.dp
}
