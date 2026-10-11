package de.blutzuckerblick

/** Test an LCD-like glowing glyph field with a blinking status indicator. */
fun main() {
    val w=320;val h=260
    fun scene(dx:Int,dy:Int,blink:Boolean):DigitMotionTracker.GrayFrame {
        val p=IntArray(w*h){35}
        fun bar(x:Int,y:Int,bw:Int,bh:Int){
            for(yy in y+dy until y+dy+bh) for(xx in x+dx until x+dx+bw)
                if(xx in 0 until w&&yy in 0 until h) p[yy*w+xx]=230
        }
        // Three separate glowing digit glyphs, with more structure than a solid block.
        for(x in listOf(96,130,164)){
            bar(x,100,24,6);bar(x+18,105,6,27)
            bar(x,133,24,6);bar(x,139,6,25)
            bar(x,166,24,6)
        }
        if(blink) bar(130,203,14,14) // Beurer Bluetooth status below digits
        return DigitMotionTracker.GrayFrame(w,h,p)
    }
    val t=DigitMotionTracker()
    val first=scene(0,0,false)
    val fixed=t.lock(first,DigitMotionTracker.RectI(96,100,188,172),1000)!!
    require(fixed.top<100 && fixed.bottom<203) { "Statussymbol im Fokus: $fixed" }
    val moved=t.follow(scene(18,7,true),1500)!!
    require(kotlin.math.abs(moved.left-fixed.left-18)<=4) { "Nicht horizontal gefolgt: $fixed -> $moved" }
    require(kotlin.math.abs(moved.top-fixed.top-7)<=4) { "Nicht vertikal gefolgt: $fixed -> $moved" }
    // The bright, blinking symbol must remain OUTSIDE the tracked crop.
    require(moved.bottom<203+7) { "Bluetooth-Symbol im Ausschnitt: $moved" }
    val follow2=t.follow(scene(22,10,false),2000)!!
    require(kotlin.math.abs(follow2.left-fixed.left-22)<=5)
    require(t.follow(scene(22,10,false),11000)==null) { "Abgelaufene Sperre nicht freigegeben" }
    println("DigitMotionTracker: translation, blink exclusion, expiry OK")
}
