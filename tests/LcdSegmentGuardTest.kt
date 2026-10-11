package de.blutzuckerblick

fun main() {
    // Synthetic three-digit LCD, only the final digit's two upper side
    // segments matter for distinguishing 3 from 5.
    val w = 270; val h = 120
    val lit = BooleanArray(w * h)
    fun paint(x0:Int,x1:Int,y0:Int,y1:Int) {
        for (y in y0 until y1) for (x in x0 until x1) lit[y*w+x] = true
    }
    // Last digit occupies x=180..270.
    paint(184,198,24,48)  // 5: upper-left segment
    val read: (Int, Int) -> Boolean = { x,y -> lit[y*w+x] }
    check(LcdSegmentGuard.contradictsOcr("133",0,0,w,h,read))
    check(!LcdSegmentGuard.contradictsOcr("135",0,0,w,h,read))
    java.util.Arrays.fill(lit, false)
    paint(253,267,24,48) // 3: upper-right segment
    check(LcdSegmentGuard.contradictsOcr("135",0,0,w,h,read))
    check(!LcdSegmentGuard.contradictsOcr("133",0,0,w,h,read))
    check(!LcdSegmentGuard.contradictsOcr("147",0,0,w,h,read))
    check(!LcdSegmentGuard.contradictsOcr("133",0,0,40,20,{ _, _ -> false }))
    println("LCD segment guard: OK")
}
