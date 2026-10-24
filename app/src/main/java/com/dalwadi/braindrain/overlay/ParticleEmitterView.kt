package com.dalwadi.braindrain.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class ParticleEmitterView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private data class Spark(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var length: Float,
        var alpha: Float,
        val color: Int,
        val creationTime: Long,
        val lifetimeMs: Long
    )

    private data class Ember(
        var x: Float,
        var y: Float,
        var vy: Float,
        var size: Float,
        var alpha: Float,
        val color: Int,
        var swaySeed: Float,
        val creationTime: Long,
        val lifetimeMs: Long
    )

    private val sparkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeCap = Paint.Cap.ROUND
    }

    private val emberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val sparks = ArrayList<Spark>(32)
    private val embers = ArrayList<Ember>(32)

    private var isThermalActive = false
    private var lastFrameTime = SystemClock.uptimeMillis()
    private var lastEmberSpawnTime = 0L

    private val sparkColors = intArrayOf(
        0xFFFFD54F.toInt(), // Amber gold
        0xFFFFCA28.toInt(), // Warm yellow
        0xFFFFA000.toInt(), // Intense welding orange
        0xFFFFFFFF.toInt()  // Hot white core
    )

    private val emberColors = intArrayOf(
        0xFFFF1744.toInt(), // Crimson red
        0xFFFF5252.toInt(), // Coral red
        0xFFFF3D00.toInt(), // Molten blaze orange
        0xFFFF9100.toInt()  // Fiery amber
    )

    fun emitSparks(originX: Float, originY: Float, count: Int = 10) {
        val now = SystemClock.uptimeMillis()
        for (i in 0 until count) {
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = Random.nextFloat() * 380f + 120f // pixels per second
            val vx = cos(angle) * speed
            val vy = sin(angle) * speed
            val lifetime = Random.nextLong(280L, 550L)
            val color = sparkColors[Random.nextInt(sparkColors.size)]

            sparks.add(
                Spark(
                    x = originX,
                    y = originY,
                    vx = vx,
                    vy = vy,
                    length = Random.nextFloat() * 8f + 4f,
                    alpha = 1.0f,
                    color = color,
                    creationTime = now,
                    lifetimeMs = lifetime
                )
            )
        }
        postInvalidateOnAnimation()
    }

    fun setThermalActive(active: Boolean) {
        if (isThermalActive != active) {
            isThermalActive = active
            if (active) {
                lastFrameTime = SystemClock.uptimeMillis()
                postInvalidateOnAnimation()
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val now = SystemClock.uptimeMillis()
        val dt = ((now - lastFrameTime).coerceIn(1L, 50L)) / 1000f
        lastFrameTime = now

        // 1. Update and render welding sparks
        val sparkIter = sparks.iterator()
        while (sparkIter.hasNext()) {
            val s = sparkIter.next()
            val age = now - s.creationTime
            if (age >= s.lifetimeMs) {
                sparkIter.remove()
                continue
            }

            val progress = age.toFloat() / s.lifetimeMs
            s.alpha = (1.0f - progress).coerceIn(0f, 1f)
            s.x += s.vx * dt
            s.y += s.vy * dt
            s.vy += 450f * dt // Subtle gravity effect on sparks

            sparkPaint.color = s.color
            sparkPaint.alpha = (s.alpha * 255).toInt()
            sparkPaint.strokeWidth = 2.5f

            val tailX = s.x - (s.vx * 0.025f)
            val tailY = s.y - (s.vy * 0.025f)
            canvas.drawLine(s.x, s.y, tailX, tailY, sparkPaint)
        }

        // 2. Spawn and update rising embers if thermal active (Attention Depleted state)
        if (isThermalActive) {
            if (now - lastEmberSpawnTime > 75L && embers.size < 24) {
                lastEmberSpawnTime = now
                val spawnX = width * (0.15f + Random.nextFloat() * 0.70f)
                val spawnY = height * 0.85f + Random.nextFloat() * (height * 0.15f)
                val vy = -(Random.nextFloat() * 80f + 40f) // Drifting upward
                val size = Random.nextFloat() * 3.5f + 2f
                val color = emberColors[Random.nextInt(emberColors.size)]
                val lifetime = Random.nextLong(600L, 1200L)

                embers.add(
                    Ember(
                        x = spawnX,
                        y = spawnY,
                        vy = vy,
                        size = size,
                        alpha = 1.0f,
                        color = color,
                        swaySeed = Random.nextFloat() * 10f,
                        creationTime = now,
                        lifetimeMs = lifetime
                    )
                )
            }
        }

        val emberIter = embers.iterator()
        while (emberIter.hasNext()) {
            val e = emberIter.next()
            val age = now - e.creationTime
            if (age >= e.lifetimeMs) {
                emberIter.remove()
                continue
            }

            val progress = age.toFloat() / e.lifetimeMs
            e.alpha = (1.0f - progress).coerceIn(0f, 1f)
            e.y += e.vy * dt
            e.x += sin((now * 0.005f) + e.swaySeed).toFloat() * 18f * dt

            emberPaint.color = e.color
            emberPaint.alpha = (e.alpha * 220).toInt()

            // Draw crisp diamond ember
            canvas.save()
            canvas.translate(e.x, e.y)
            canvas.rotate(45f)
            canvas.drawRect(-e.size / 2, -e.size / 2, e.size / 2, e.size / 2, emberPaint)
            canvas.restore()
        }

        // Loop animation if active
        if (sparks.isNotEmpty() || (isThermalActive && embers.isNotEmpty()) || isThermalActive) {
            postInvalidateOnAnimation()
        }
    }
}
