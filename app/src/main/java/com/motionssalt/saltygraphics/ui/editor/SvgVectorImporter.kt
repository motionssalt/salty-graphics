package com.motionssalt.saltygraphics.ui.editor

import android.graphics.*
import androidx.core.graphics.PathParser
import com.motionssalt.saltygraphics.engine.model.*
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import kotlin.math.ceil

/** Basic, offline SVG import. Curves/arcs are flattened at <=2px spacing (bounded to 2048 points/contour).
 * No scripts, external resources, filters, stylesheets or network access are executed. */
object SvgVectorImporter {
    fun parse(input: InputStream): List<VectorPathData> {
        val bytes=input.readBytesLimited(4*1024*1024)
        val text=bytes.toString(Charsets.UTF_8)
        require(!text.contains("<!DOCTYPE",true) && !text.contains("<!ENTITY",true)){"SVG entities are not supported"}
        val parser=XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(text.reader())
        data class Style(val fill:Long=0xFF000000L,val stroke:Long=0L,val width:Float=1f)
        val matrices=mutableListOf(Matrix())
        val styles=mutableListOf(Style())
        val output=mutableListOf<VectorPathData>()
        var count=0
        while(parser.eventType!=XmlPullParser.END_DOCUMENT) {
            if(parser.eventType==XmlPullParser.START_TAG) {
                require(++count<=10000){"SVG is too complex"}
                fun attr(key:String)=parser.getAttributeValue(null,key)
                fun number(key:String,default:Float=0f)=attr(key)?.trim()?.removeSuffix("px")?.toFloatOrNull()?.takeIf{it.isFinite()}?:default
                val matrix=Matrix(matrices.last())
                attr("transform")?.let { value ->
                    val local=Matrix()
                    Regex("([a-zA-Z]+)\\s*\\(([^)]*)\\)").findAll(value).forEach { match ->
                        val n=Regex("[-+]?(?:\\d*\\.\\d+|\\d+\\.?\\d*)(?:[eE][-+]?\\d+)?").findAll(match.groupValues[2]).map{it.value.toFloat()}.toList()
                        val op=Matrix()
                        when(match.groupValues[1]) {
                            "translate"->op.setTranslate(n[0],n.getOrElse(1){0f})
                            "scale"->op.setScale(n[0],n.getOrElse(1){n[0]})
                            "rotate"->op.setRotate(n[0],n.getOrElse(1){0f},n.getOrElse(2){0f})
                            "matrix"->{require(n.size==6);op.setValues(floatArrayOf(n[0],n[2],n[4],n[1],n[3],n[5],0f,0f,1f))}
                            else->error("Unsupported SVG transform: ${match.groupValues[1]}")
                        }
                        local.preConcat(op)
                    }
                    matrix.preConcat(local)
                }
                matrices.add(matrix)
                val inline=attr("style")?.split(';')?.mapNotNull{it.split(':',limit=2).takeIf{v->v.size==2}?.let{v->v[0].trim() to v[1].trim()}}?.toMap().orEmpty()
                fun property(key:String)=attr(key)?:inline[key]
                fun color(value:String?,fallback:Long):Long {
                    if(value==null)return fallback
                    if(value=="none")return 0L
                    require(!value.startsWith("url(")){"Gradient SVG fills are not supported yet"}
                    return Color.parseColor(value).toLong() and 0xFFFFFFFFL
                }
                val inherited=styles.last()
                val style=Style(color(property("fill"),inherited.fill),color(property("stroke"),inherited.stroke),property("stroke-width")?.removeSuffix("px")?.toFloatOrNull()?:inherited.width)
                styles.add(style)
                val path=when(parser.name.substringAfter(':')) {
                    "path" -> PathParser.createPathFromPathData(attr("d")?:"")
                    "rect" -> Path().apply {val x=number("x");val y=number("y");addRoundRect(x,y,x+number("width"),y+number("height"),number("rx"),number("ry",number("rx")),Path.Direction.CW)}
                    "circle","ellipse" -> Path().apply{val rx=number("rx",number("r"));val ry=number("ry",number("r"));val x=number("cx");val y=number("cy");addOval(x-rx,y-ry,x+rx,y+ry,Path.Direction.CW)}
                    "line" -> Path().apply{moveTo(number("x1"),number("y1"));lineTo(number("x2"),number("y2"))}
                    "polygon","polyline" -> Path().apply {
                        val coords=Regex("[-+]?(?:\\d*\\.\\d+|\\d+\\.?\\d*)(?:[eE][-+]?\\d+)?").findAll(attr("points")?:"").map{it.value.toFloat()}.toList()
                        require(coords.size%2==0){"Invalid SVG points"}
                        coords.chunked(2).forEachIndexed{i,p->if(i==0)moveTo(p[0],p[1]) else lineTo(p[0],p[1])}
                        if(parser.name=="polygon")close()
                    }
                    "script","image","use","foreignObject" -> error("Unsupported SVG element: ${parser.name}")
                    else -> null
                }
                if(path!=null) {
                    path.transform(matrix)
                    val measure=PathMeasure(path,false)
                    do {
                        val length=measure.length
                        if(length.isFinite() && length>0) {
                            val closed=measure.isClosed
                            val steps=ceil(length/2f).toInt().coerceIn(2,2048)
                            val pos=FloatArray(2)
                            val points=(0..if(closed)steps-1 else steps).map { step ->
                                measure.getPosTan(length*step/steps,pos,null)
                                BezierPoint(pos[0],pos[1])
                            }
                            output.add(VectorPathData(points=points,isClosed=closed,fillColor=style.fill,strokeColor=style.stroke,strokeWidth=style.width,hasFill=style.fill ushr 24!=0L,hasStroke=style.stroke ushr 24!=0L))
                            require(output.sumOf{it.points.size}<=20000){"SVG has too many points"}
                        }
                    }while(measure.nextContour())
                }
            } else if(parser.eventType==XmlPullParser.END_TAG) {matrices.removeAt(matrices.lastIndex);styles.removeAt(styles.lastIndex)}
            parser.next()
        }
        require(output.isNotEmpty()){"SVG contains no editable paths"}
        val all=output.flatMap{it.points};val cx=(all.minOf{it.x}+all.maxOf{it.x})/2;val cy=(all.minOf{it.y}+all.maxOf{it.y})/2
        return output.map{p->p.copy(points=p.points.map{VectorEditing.movePoint(it,it.x-cx,it.y-cy)})}
    }
    private fun InputStream.readBytesLimited(limit:Int):ByteArray {
        val output=java.io.ByteArrayOutputStream()
        val buffer=ByteArray(8192)
        while(true) {
            val count=read(buffer);if(count<0)break
            require(output.size()+count<=limit){"SVG exceeds 4 MB"}
            output.write(buffer,0,count)
        }
        return output.toByteArray()
    }
}
