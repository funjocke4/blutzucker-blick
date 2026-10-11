package de.blutzuckerblick

fun main() {
    check(ScanWindow.CAMERA_HEIGHT_FRACTION == 0.60f)
    val r = ScanWindow.digitArea(1000, 1000)
    check(r.left in 239..240 && r.right in 760..761) { "Not 52% of width: $r" }
    check(r.top == 385 && r.bottom == 775) { "Not 39% of height: $r" }
    val top = ScanWindow.digitArea(1000, 1000, ScanWindow.Center(.5f,.3f))
    check(top.top < r.top && top.bottom < r.bottom) { "Tap must shift ROI: $top" }
    val edge = ScanWindow.digitArea(720, 960, ScanWindow.Center(0f,1f))
    check(edge.left >= 0 && edge.top >= 0 && edge.right <= 720 && edge.bottom <= 960)
    println("OK: preview stays 60%, ROI starts below date, adjustable center")
}
