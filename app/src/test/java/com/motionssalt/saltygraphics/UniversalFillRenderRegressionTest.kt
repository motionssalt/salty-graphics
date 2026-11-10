package com.motionssalt.saltygraphics

import android.graphics.*
import com.motionssalt.saltygraphics.engine.model.*
import com.motionssalt.saltygraphics.engine.render.FillRenderer
import com.motionssalt.saltygraphics.engine.render.LayerCompositor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class UniversalFillRenderRegressionTest {
    private fun shape(fill:FillData) = Layer(id="shape",type=LayerType.SHAPE,
        shapeData=ShapeData(width=120f,height=120f),fillData=fill)

    @Test fun everyGradientTypeRendersAndRoundTripsAllParameters() {
        GradientType.entries.forEach { type ->
            val gradient=GradientData(type=type,angleDegrees=23f,centerX=.42f,centerY=.6f,
                radius=.7f,aspect=1.4f,repeatMode=GradientRepeatMode.MIRROR,
                stops=listOf(GradientStop(0f,0xFFFF0000L,midpoint=.3f),GradientStop(1f,0x0000FF00L)))
            val layer=shape(FillData(type=FillType.GRADIENT,gradient=gradient))
            val saved=Json.decodeFromString<Layer>(Json.encodeToString(layer))
            assertEquals(layer,saved)
            val bmp=LayerCompositor.renderLayerContent(saved,200,200)
            val pixels=IntArray(40000);bmp.getPixels(pixels,0,200,0,0,200,200)
            assertTrue("$type must paint visible pixels",pixels.any{Color.alpha(it)>10})
            assertTrue("$type must not paint a constant solid",pixels.filter{Color.alpha(it)>0}.distinct().size>4)
        }
    }

    @Test fun colorToTransparentHasHalfAlphaAtMidpointAndInterpolationIsNotIgnored() {
        val g=GradientData(interpolation=GradientInterpolation.LINEAR,
            stops=listOf(GradientStop(0f,0xFFFF0000L),GradientStop(1f,0x00FF0000L)))
        fun render(gradient:GradientData):Bitmap {
            val bmp=Bitmap.createBitmap(200,200,Bitmap.Config.ARGB_8888)
            val p=Paint();FillRenderer.configureFillPaint(p,FillData(type=FillType.GRADIENT,gradient=gradient),RectF(0f,0f,200f,200f))
            Canvas(bmp).drawRect(0f,0f,200f,200f,p);return bmp
        }
        val linear=render(g)
        assertTrue(Color.alpha(linear.getPixel(100,100)) in 125..130)
        val smooth=render(g.copy(interpolation=GradientInterpolation.SMOOTH))
        assertNotEquals(Color.alpha(linear.getPixel(40,100)),Color.alpha(smooth.getPixel(40,100)))
        val biased=render(g.copy(stops=g.stops.mapIndexed{i,s->if(i==0)s.copy(midpoint=.2f)else s}))
        assertNotEquals(Color.alpha(linear.getPixel(100,100)),Color.alpha(biased.getPixel(100,100)))
    }

    @Test fun mediaScaleOffsetRotationAndFitTransparencyReachTheRealRenderer() {
        val source=Bitmap.createBitmap(20,10,Bitmap.Config.ARGB_8888)
        Canvas(source).apply {drawColor(Color.BLUE);drawRect(0f,0f,10f,10f,Paint().apply{color=Color.RED})}
        fun render(fill:FillData)=LayerCompositor.renderLayerContent(shape(fill),200,200,mediaResolver={source})
        val f=FillData(type=FillType.MEDIA,mediaAssetPath="assets/source.png",mediaScaleMode=MediaScaleMode.FIT)
        val fit=render(f)
        assertEquals(0,Color.alpha(fit.getPixel(100,45)))
        assertTrue(Color.alpha(fit.getPixel(100,100))>200)
        val scaled=render(f.copy(mediaScale=.5f))
        assertEquals(0,Color.alpha(scaled.getPixel(50,100)))
        val moved=render(f.copy(mediaOffsetY=.5f))
        assertEquals(0,Color.alpha(moved.getPixel(100,80)))
        val rotated=render(f.copy(mediaRotation=90f))
        assertTrue(Color.alpha(rotated.getPixel(100,45))>200)
        assertEquals(0,Color.alpha(rotated.getPixel(45,100)))
    }

    @Test fun repeatAndReversePreserveMatteAlphaAndMidpointIntervals() {
        val g=GradientData(interpolation=GradientInterpolation.LINEAR,
            stops=listOf(GradientStop(0f,0xFFFF0000L,midpoint=.3f),GradientStop(1f,0x00FF0000L)))
        assertEquals(g.stops.last().effectiveArgb,FillRenderer.sampleGradient(g,1.5f))
        assertEquals(FillRenderer.sampleGradient(g,.5f),FillRenderer.sampleGradient(g.copy(repeatMode=GradientRepeatMode.REPEAT),1.5f))
        assertEquals(FillRenderer.sampleGradient(g,.5f),FillRenderer.sampleGradient(g.copy(repeatMode=GradientRepeatMode.MIRROR),1.5f))
        assertEquals(.7f,g.reversed().stops.first().midpoint,.00001f)
        assertEquals(g.stops.last().color,g.reversed().stops.first().color)
        val three=g.copy(stops=g.stops+GradientStop(.2f,0xFF0000FFL))
        assertEquals(listOf(0f,.5f,1f),three.distributeEvenly().stops.map{it.position})
    }

    @Test fun textPaintMetricsAndExplicitTransparencySurviveFillSetup() {
        val p=Paint().apply{textSize=81f;typeface=Typeface.MONOSPACE}
        FillRenderer.configureFillPaint(p,FillData(solidOpacity=0f),RectF(0f,0f,100f,100f))
        assertEquals(81f,p.textSize,0f);assertEquals(Typeface.MONOSPACE,p.typeface);assertEquals(0,p.alpha)
        val text=Layer(type=LayerType.TEXT,textData=TextLayerData(text="Visible",fontSize=70f),fillData=FillData(solidOpacity=0f))
        val bmp=LayerCompositor.renderLayerContent(text,200,200)
        val px=IntArray(40000);bmp.getPixels(px,0,200,0,0,200,200)
        assertTrue("Transparent text must not fall back to its legacy color",px.all{Color.alpha(it)==0})
        val media=Layer(type=LayerType.MEDIA,mediaAssetPath="old",fillData=FillData(solidOpacity=0f))
        val bm=LayerCompositor.renderLayerContent(media,200,200,mediaResolver={Bitmap.createBitmap(2,2,Bitmap.Config.ARGB_8888).apply{eraseColor(Color.RED)}})
        assertEquals("Explicit solid fill must override old media asset",0,Color.alpha(bm.getPixel(100,100)))
    }
}
