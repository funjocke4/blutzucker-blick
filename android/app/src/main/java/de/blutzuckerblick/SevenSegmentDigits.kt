// SPDX-License-Identifier: GPL-3.0-only
package de.blutzuckerblick

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Dedicated, offline reader for illuminated seven-segment glucose displays.
 * Unlike text OCR it operates only on the large digits in an explicitly chosen area.
 * It intentionally returns null on ambiguity; NO result is ever saved automatically.
 */
internal object SevenSegmentDigits {
    data class Result(val mgdl: Int, val bounds: Rect, val confidence: Float)
    private data class Range(val a: Int, val b: Int) { val size get() = b-a }
    private val glyphs = mapOf(
        '0' to "abcdef", '1' to "bc", '2' to "abged", '3' to "abgcd",
        '4' to "fgbc", '5' to "afgcd", '6' to "afgcde", '7' to "abc",
        '8' to "abcdefg", '9' to "abcdfg"
    )
    private val segments = listOf(
        'a' to floatArrayOf(.28f,.72f,.07f,.22f),
        'g' to floatArrayOf(.28f,.72f,.42f,.58f),
        'd' to floatArrayOf(.28f,.72f,.78f,.94f),
        'f' to floatArrayOf(.04f,.26f,.20f,.42f),
        'b' to floatArrayOf(.74f,.96f,.20f,.42f),
        'e' to floatArrayOf(.04f,.26f,.58f,.80f),
        'c' to floatArrayOf(.74f,.96f,.58f,.80f)
    )

    fun detect(source: Bitmap, target: Rect, preferDarkBackground: Boolean = true): Result? {
        val rect = Rect(target)
        if (!rect.intersect(0,0,source.width,source.height) || rect.width() < 35 || rect.height() < 35) return null
        val original = Bitmap.createBitmap(source,rect.left,rect.top,rect.width(),rect.height())
        // Up to 900px wide: avoids huge allocations on 50MP camera photos.
        val resized = if (original.width > 900) Bitmap.createScaledBitmap(
            original,900,max(1,original.height*900/original.width),true) else original
        if (resized !== original) original.recycle()
        try {
            val w=resized.width; val h=resized.height
            val pixels=IntArray(w*h);resized.getPixels(pixels,0,w,0,0,w,h)
            val lum=IntArray(pixels.size)
            for (i in pixels.indices) {
                val c=pixels[i]
                lum[i]=(Color.red(c)*77+Color.green(c)*150+Color.blue(c)*29) shr 8
            }
            // Find the TALL illuminated numeral row first. A glucose meter often
            // has small, bright date/time above and mg/dL below the measurement.
            // The old 9-window search mixed those letters with the numeric glyphs.
            // Here every readGlyph() receives a tightly bounded full digit row.
            val candidates = mutableListOf<ScanConsensus.Vote<Result>>()
            for (threshold in intArrayOf(140, 165, 185, 205, 220)) {
                val band = largeDigitBand(lum, w, h, threshold) ?: continue
                val areaH = band.b - band.a
                val sample = IntArray(w * areaH)
                System.arraycopy(lum, band.a * w, sample, 0, sample.size)
                val parsed = parseMask(sample, w, areaH, threshold, preferDarkBackground) ?: continue
                val sx = rect.width().toFloat() / w
                val sy = rect.height().toFloat() / h
                val mapped = parsed.copy(bounds = Rect(
                    rect.left + (parsed.bounds.left * sx).toInt(),
                    rect.top + ((band.a + parsed.bounds.top) * sy).toInt(),
                    rect.left + (parsed.bounds.right * sx).toInt(),
                    rect.top + ((band.a + parsed.bounds.bottom) * sy).toInt()
                ))
                candidates.add(ScanConsensus.Vote(mapped.mgdl, mapped.confidence, threshold, mapped))
            }
            return ScanConsensus.choose(candidates)?.payload
        } finally { resized.recycle() }
    }

    /**
     * Locate the long continuous stripe made by the BIG digits. Small date/time
     * characters have much shorter vertical spans; in particular, a blinking
     * Bluetooth icon cannot form a full-height numeric row.
     * The threshold uses the central area so a white meter housing at the edge
     * does not drown out the dark screen (Accu-Chek Instant).
     */
    private fun largeDigitBand(lum: IntArray, w: Int, h: Int, threshold: Int): Range? {
        val x0 = (w * .16f).toInt()
        val x1 = (w * .83f).toInt().coerceAtLeast(x0 + 1)
        val required = max(5, ((x1 - x0) * .10f).toInt())
        val rows = BooleanArray(h)
        for (y in 0 until h) {
            var bright = 0
            val start = y * w
            for (x in x0 until x1) if (lum[start + x] >= threshold) bright++
            rows[y] = bright >= required
        }
        val found = mutableListOf<Range>()
        var begin = -1
        for (y in 0..h) {
            val on = y < h && rows[y]
            if (on && begin < 0) begin = y
            if (!on && begin >= 0) { found.add(Range(begin, y)); begin = -1 }
        }
        // Bridge only tiny gaps inside an LCD segment, NOT the large vertical
        // separation between the date/time row and the glucose number.
        val joined = mutableListOf<Range>()
        val gap = max(2, h / 100)
        for (range in found) {
            if (joined.isNotEmpty() && range.a - joined.last().b <= gap) {
                val prior = joined.removeAt(joined.lastIndex)
                joined.add(Range(prior.a, range.b))
            } else joined.add(range)
        }
        val winner = joined.filter { it.size >= max(34, (h * .26f).toInt()) }
            .maxByOrNull { it.size } ?: return null
        val pad = max(2, (h * .015f).toInt())
        return Range(max(0, winner.a - pad), min(h, winner.b + pad))
    }

    private fun parseMask(lum:IntArray,w:Int,h:Int,threshold:Int,darkBackground:Boolean):Result? {
        val mask=BooleanArray(w*h)
        var white=0
        for (i in lum.indices) {
            mask[i]=if (darkBackground) lum[i]>=threshold else lum[i]<threshold
            if(mask[i]) white++
        }
        if (white < w*h*.025 || white > w*h*.65) return null
        val col=IntArray(w)
        for(y in 0 until h) for(x in 0 until w) if(mask[y*w+x]) col[x]++
        val minimum=max(3,(h*.052).toInt())
        val raw=mutableListOf<Range>(); var start=-1
        for(x in 0..w) {
            val on=x<w && col[x]>=minimum
            if(on && start<0) start=x
            if(!on && start>=0) {raw.add(Range(start,x));start=-1}
        }
        // Do not join unrelated date text. Close only very small digit segment gaps.
        val runs=mutableListOf<Range>()
        for(r in raw) {
            if(runs.isNotEmpty() && r.a-runs.last().b<=max(2,h/90)) {
                val old=runs.removeAt(runs.lastIndex);runs.add(Range(old.a,r.b))
            } else runs.add(r)
        }
        data class Glyph(val span:Range,val top:Int,val bottom:Int,val digit:Char,val clarity:Float)
        val glyphsFound=mutableListOf<Glyph>()
        for(r in runs) {
            if(r.size < max(5,h/18)) continue
            val topRow=IntArray(h)
            for(y in 0 until h) for(x in r.a until r.b) if(mask[y*w+x]) topRow[y]++
            val ys=(0 until h).filter { topRow[it]>=max(2,(r.size*.08).toInt()) }
            if(ys.isEmpty()) continue
            val t=ys.first();val b=ys.last()+1;val digitHeight=b-t
            // In the enlarged scan area, short Accu-Chek digits still need to pass.
            // Date/time glyphs are much smaller and remain below this bound.
            if(digitHeight<h*.34 || digitHeight<30 || r.size>digitHeight*.93) continue
            val label=readGlyph(mask,w,Range(r.a,r.b),Range(t,b)) ?: continue
            glyphsFound.add(Glyph(r,t,b,label.first,label.second))
        }
        // Edge reflections from a white meter housing and a small arrow can
        // produce EXTRA glyph-shaped objects. Never fail merely because four
        // components were found: select a coherent neighboring 2/3-digit group.
        data class Group(val count: Int, val score: Float, val result: Result)
        val groups = mutableListOf<Group>()
        for (count in intArrayOf(3, 2)) {
            if (glyphsFound.size < count) continue
            for (start in 0..glyphsFound.size - count) {
                val items = glyphsFound.subList(start, start + count)
                val maxHeight = items.maxOf { it.bottom - it.top }
                val minHeight = items.minOf { it.bottom - it.top }
                if (minHeight < maxHeight * .80f) continue
                if (items.zipWithNext().any { (a, b) ->
                    b.span.a - a.span.b > maxHeight * .62f
                }) continue
                val v = items.joinToString("") { it.digit.toString() }.toIntOrNull() ?: continue
                if (v !in 20..600) continue
                val left = items.first().span.a
                val right = items.last().span.b
                val top = items.minOf { it.top }
                val bottom = items.maxOf { it.bottom }
                val minClarity = items.minOf { it.clarity }
                val group = Result(v, Rect(left, top, right, bottom), minClarity)
                // A complete three-digit measurement is preferred over a
                // coincidental pair, but only when the digits really line up.
                groups.add(Group(count, minClarity + count * .30f, group))
            }
        }
        val best = groups.maxByOrNull { it.score } ?: return null
        val competitor = groups.filter { it.count == best.count && it.result.mgdl != best.result.mgdl }
            .maxByOrNull { it.score }
        // Do not offer a medical number if two plausible groups disagree.
        if (competitor != null && best.score - competitor.score < .16f) return null
        return best.result
    }

    private fun readGlyph(mask:BooleanArray, width:Int, x:Range, y:Range): Pair<Char,Float>? {
        val digitW=x.size.toFloat();val digitH=y.size.toFloat()
        fun level(z:FloatArray):Float {
            val xa=(x.a+digitW*z[0]).toInt().coerceIn(x.a,x.b-1)
            val xb=(x.a+digitW*z[1]).toInt().coerceIn(xa+1,x.b)
            val ya=(y.a+digitH*z[2]).toInt().coerceIn(y.a,y.b-1)
            val yb=(y.a+digitH*z[3]).toInt().coerceIn(ya+1,y.b)
            var count=0; var active=0
            for(py in ya until yb) for(px in xa until xb) {
                if(mask[py*width+px]) active++
                count++
            }
            return if(count>0) active.toFloat()/count else 0f
        }
        val observed=segments.associate { (id,zone)->id to level(zone) }
        // Narrow glyphs, such as both '1's on Accu-Chek Instant, are one digit.
        if(digitW/digitH < .29f) return '1' to .95f
        // Beurer's angled '1' has lit upper-left and top, but no middle/bottom bars.
        if(digitW/digitH < .51f && observed.getValue('b')>.64f &&
            observed.getValue('c')>.64f && observed.getValue('g')<.36f &&
            observed.getValue('d')<.41f && observed.getValue('e')<.19f) return '1' to .85f
        val ranked=glyphs.map { (digit,pattern)->
            val score=segments.sumOf { (name,_)->
                val val0=observed.getValue(name).toDouble()
                if(name in pattern) val0-.20 else .20-val0
            }
            digit to score
        }.sortedByDescending { it.second }
        val first=ranked[0];val second=ranked[1]
        if(first.second<1.3 || first.second-second.second<.23) return null
        return first.first to min(1f,((first.second-second.second)/1.4).toFloat())
    }
}
