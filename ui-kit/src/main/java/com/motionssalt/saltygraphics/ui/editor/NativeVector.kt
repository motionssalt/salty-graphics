package com.motionssalt.saltygraphics.ui.editor

import android.graphics.*
import com.motionssalt.saltygraphics.engine.model.*

fun nativeVectorPath(data: VectorPathData): Path = Path().apply {
    val points = data.points
    if(points.isNotEmpty()) {
        moveTo(points[0].x,points[0].y)
        val segments=if(data.isClosed)points.size else points.size-1
        for(i in 0 until segments) {
            val a=points[i];val b=points[(i+1)%points.size]
            cubicTo(a.outHandleX,a.outHandleY,b.inHandleX,b.inHandleY,b.x,b.y)
        }
        if(data.isClosed)close()
    }
}

fun drawNativeVector(canvas: Canvas,data: VectorPathData,opacity: Float=1f) {
    val path=nativeVectorPath(data)
    val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    fun setColor(argb:Long) {paint.color=argb.toInt();paint.alpha=(Color.alpha(argb.toInt())*opacity.coerceIn(0f,1f)).toInt()}
    if(data.hasFill) {setColor(data.fillColor);paint.style=Paint.Style.FILL;canvas.drawPath(path,paint)}
    if(data.hasStroke && data.strokeWidth>0) {
        setColor(data.strokeColor);paint.style=Paint.Style.STROKE;paint.strokeWidth=data.strokeWidth
        paint.strokeCap=Paint.Cap.valueOf(data.strokeCap.name)
        paint.strokeJoin=Paint.Join.valueOf(data.strokeJoin.name)
        if(data.dashLength>0 && data.dashGap>0)paint.pathEffect=DashPathEffect(floatArrayOf(data.dashLength,data.dashGap),0f)
        canvas.drawPath(path,paint)
    }
}
