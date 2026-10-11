// SPDX-License-Identifier: GPL-3.0-only
package de.blutzuckerblick

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Moves a focus rectangle with the illuminated large numerals after the FIRST
 * successful OCR observation. The reference template contains only the digit
 * area, not the status bar or the intermittently blinking Bluetooth icon.
 *
 * Pure Kotlin; no remote processing, no recognition of medical values here.
 * Updates need a low matching error. If tracking is lost, the caller returns
 * to the broad scan instead of offering a guessed value.
 */
internal class DigitMotionTracker {
    data class RectI(val left:Int, val top:Int, val right:Int, val bottom:Int) {
        val width get()=right-left
        val height get()=bottom-top
        fun translated(dx:Int,dy:Int)=RectI(left+dx,top+dy,right+dx,bottom+dy)
    }
    data class GrayFrame(val width:Int, val height:Int, val luminance:IntArray) {
        init { require(width>0 && height>0 && luminance.size == width*height) }
        fun sample(x:Int,y:Int)=luminance[y.coerceIn(0,height-1)*width+x.coerceIn(0,width-1)]
    }
    private val cols=18
    private val rows=12
    private var template:IntArray?=null
    private var focus:RectI?=null
    private var lastLockedMs=0L
    private var lastFollowMs=0L

    fun clear(){ template=null;focus=null;lastLockedMs=0L;lastFollowMs=0L }
    fun current():RectI?=focus

    /** Builds a focus area from the digit glyphs, with an intentionally small
     * margin below them: Beurer's blinking Bluetooth symbol sits underneath. */
    fun lock(gray:GrayFrame, digits:RectI, nowMs:Long):RectI? {
        if(digits.width<12 || digits.height<16) return null
        val topPad=(digits.height*.35f).toInt()
        val bottomPad=(digits.height*.08f).toInt()
        val sidePad=(digits.height*.14f).toInt()
        val proposal=fit(RectI(digits.left-sidePad,digits.top-topPad,
            digits.right+sidePad,digits.bottom+bottomPad),gray.width,gray.height) ?: return null
        focus=proposal
        template=readTemplate(gray,proposal)
        lastLockedMs=nowMs
        lastFollowMs=nowMs
        return proposal
    }

    /** Match a small luminance template to nearby camera pixels. We never
     * let an arbitrary dark/bright patch take the guide away from the digits. */
    fun follow(gray:GrayFrame, nowMs:Long):RectI? {
        val old=focus ?: return null
        val expected=template ?: return null
        if(nowMs-lastLockedMs>7000L || nowMs-lastFollowMs>3000L){clear();return null}
        val dxMax=max(10,(gray.width*.10f).toInt())
        val dyMax=max(10,(gray.height*.085f).toInt())
        val step=max(3,min(gray.width,gray.height)/120)
        var best=old
        var bestError=error(gray,old,expected)
        val initialError=bestError
        for(dy in -dyMax..dyMax step step) for(dx in -dxMax..dxMax step step){
            if(dx==0 && dy==0) continue
            val shifted=old.translated(dx,dy)
            if(!inside(shifted,gray.width,gray.height)) continue
            val e=error(gray,shifted,expected)
            if(e<bestError){bestError=e;best=shifted}
        }
        // A good match is required for a frame to move the guide. Larger
        // differences usually mean blur, glare or the meter leaving the view.
        if(bestError>48.0){
            if(nowMs-lastFollowMs>2200L){clear();return null}
            return old
        }
        if(bestError<initialError*.92 || initialError<=48.0){
            focus=best
            lastFollowMs=nowMs
        }
        return focus
    }

    private fun readTemplate(f:GrayFrame,r:RectI):IntArray {
        return IntArray(cols*rows){k->
            val yy=k/cols;val xx=k%cols
            val x=r.left+((xx+.5f)*r.width/cols).toInt()
            val y=r.top+((yy+.5f)*r.height/rows).toInt()
            f.sample(x,y)
        }
    }
    private fun error(f:GrayFrame,r:RectI,ref:IntArray):Double{
        var err=0L
        for(y in 0 until rows) for(x in 0 until cols){
            val px=r.left+((x+.5f)*r.width/cols).toInt()
            val py=r.top+((y+.5f)*r.height/rows).toInt()
            err+=abs(f.sample(px,py)-ref[y*cols+x])
        }
        return err.toDouble()/ref.size
    }
    private fun inside(r:RectI,w:Int,h:Int)=r.left>=0&&r.top>=0&&r.right<=w&&r.bottom<=h
    private fun fit(r:RectI,w:Int,h:Int):RectI?{
        if(r.width<=0||r.height<=0||r.width>w||r.height>h)return null
        val x=r.left.coerceIn(0,w-r.width)
        val y=r.top.coerceIn(0,h-r.height)
        return RectI(x,y,x+r.width,y+r.height)
    }
}
