package com.motionssalt.saltygraphics.ui.editor

import android.content.Context
import android.graphics.Paint
import android.graphics.Path
import com.motionssalt.saltygraphics.data.FontManager
import com.motionssalt.saltygraphics.engine.model.*
import kotlin.math.*

/** The same paint metrics and path geometry used by the actual canvas, not character-count guesses. */
internal fun selectionTextPaint(context: Context, text: TextLayerData): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    textSize=text.fontSize
    letterSpacing=text.letterSpacing/1000f
    isFakeBoldText=text.isBold
    if(text.isItalic) textSkewX=-.25f
    textAlign=when(text.justification) {
        TextJustification.LEFT -> Paint.Align.LEFT
        TextJustification.RIGHT -> Paint.Align.RIGHT
        else -> Paint.Align.CENTER
    }
    val font=FontManager.getAllFonts(context).find { it.familyName.equals(text.fontFamily,ignoreCase=true) }
    typeface=FontManager.getTypeface(context,font)
}

internal fun layerSelectionBounds(context: Context, document: ProjectDocument, layer: Layer): SelectionBounds {
    fun pathBounds(path: Path): SelectionBounds {
        val samples=path.approximate(.1f)
        return SelectionBounds.enclosing((samples.indices step 3).map { SelectionPoint(samples[it+1],samples[it+2]) })
    }
    return when(layer.type) {
        LayerType.GROUP -> SelectionBounds.enclosing(layer.children.filter { it.isVisible }.flatMap { child ->
            layerSelectionBounds(context,document,child).corners.map { SelectionGeometry.world(it,child.transform) }
        })
        LayerType.VECTOR -> SelectionBounds.enclosing(layer.editablePaths.flatMap { pathBounds(nativeVectorPath(it)).corners })
        LayerType.TEXT -> {
            val data=layer.textData ?: TextLayerData(); val paint=selectionTextPaint(context,data)
            val pathLayer=data.followPathLayerId?.let { document.findLayerById(it) }
            val vector=pathLayer?.vectorPathData ?: pathLayer?.shapeData?.toVectorPath()
            if(vector!=null && pathLayer!=null) {
                val b=pathBounds(nativeVectorPath(vector))
                val points=b.corners.map {
                    val p=SelectionGeometry.world(it,pathLayer.transform)
                    val local=VectorEditing.toLocal(p.x,p.y,layer.transform)
                    SelectionPoint(local.first,local.second)
                }
                val bounds=SelectionBounds.enclosing(points)
                val pad=data.fontSize
                SelectionBounds(bounds.left-pad,bounds.top-pad,bounds.right+pad,bounds.bottom+pad)
            } else {
                val width=max(.001f,paint.measureText(data.text))
                val x=when(data.justification) { TextJustification.LEFT -> -data.boxWidth/2; TextJustification.RIGHT -> data.boxWidth/2-width; else -> -width/2 }
                val baseline=-(paint.descent()+paint.ascent())/2
                val pad=if(data.hasOutline) data.outlineWidth/2 else 0f
                SelectionBounds(x-pad,baseline+paint.ascent()-pad,x+width+pad,baseline+paint.descent()+pad)
            }
        }
        else -> SelectionBounds(-layer.localWidth/2,-layer.localHeight/2,layer.localWidth/2,layer.localHeight/2)
    }
}

internal fun selectionProxy(context: Context, document: ProjectDocument, ids: Set<String>): Layer? {
    val layers=ids.mapNotNull { document.findLayerById(it) }.filter { it.isVisible && it.type!=LayerType.ADJUSTMENT }
    if(layers.size<=1) return layers.firstOrNull()
    val bounds=SelectionBounds.enclosing(layers.flatMap { layer -> layerSelectionBounds(context,document,layer).corners.map { SelectionGeometry.world(it,layer.transform) } })
    return Layer(id="__selection__",type=LayerType.GROUP,isLocked=layers.any { it.isLocked },
        transform=Transform2D(translationX=bounds.center.x,translationY=bounds.center.y),
        shapeData=ShapeData(width=bounds.width,height=bounds.height))
}

internal fun selectionBounds(context: Context, document: ProjectDocument, layer: Layer): SelectionBounds =
    if(layer.id=="__selection__") {
        val s=layer.shapeData!!; SelectionBounds(-s.width/2,-s.height/2,s.width/2,s.height/2)
    } else layerSelectionBounds(context,document,layer)
