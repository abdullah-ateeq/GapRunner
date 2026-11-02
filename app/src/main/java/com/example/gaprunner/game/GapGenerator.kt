package com.example.gaprunner.game

import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

object GapGenerator {

    /**
     * Calculates the safe gap width for an obstacle based on the number of obstacles passed.
     * Decreases gradually as difficulty increases, but never drops below config.minGapWidth.
     */
    fun calculateGapWidth(
        obstaclesPassed: Int,
        config: GameConfig = GameConfig(),
        random: Random = Random.Default
    ): Float {
        // Tiers:
        // 0..4: Wide gap (0.24f .. 0.28f)
        // 5..14: Medium gap (0.18f .. 0.23f)
        // 15+: Narrow gap (0.135f .. 0.17f)
        val (baseMin, baseMax) = when {
            obstaclesPassed < 5 -> 0.24f to 0.28f
            obstaclesPassed < 15 -> 0.18f to 0.23f
            else -> 0.135f to 0.17f
        }

        val width = baseMin + (random.nextFloat() * (baseMax - baseMin))
        return width.coerceIn(config.minGapWidth, config.maxGapWidth)
    }

    /**
     * Calculates maximum horizontal distance a gap can jump from previous gap center.
     * Prevents unfair sudden edge-to-edge jumps early in a run.
     */
    fun calculateMaxGapJump(obstaclesPassed: Int): Float {
        return when {
            obstaclesPassed < 5 -> 0.26f
            obstaclesPassed < 15 -> 0.42f
            else -> 0.65f
        }
    }

    /**
     * Determines the vertical spacing between the last obstacle and the new obstacle.
     * Decreases as difficulty increases to introduce more frequent obstacles.
     */
    fun calculateObstacleSpacing(obstaclesPassed: Int): Float {
        return when {
            obstaclesPassed < 5 -> 0.42f
            obstaclesPassed < 15 -> 0.35f
            else -> 0.28f
        }
    }

    /**
     * Generates a new obstacle placed at the given Y position, safely constrained
     * by previous gap position and playfield boundaries.
     */
    fun generateNextObstacle(
        id: Long,
        y: Float,
        previousGapCenter: Float?,
        obstaclesPassed: Int,
        config: GameConfig = GameConfig(),
        random: Random = Random.Default
    ): Obstacle {
        val gapWidth = calculateGapWidth(obstaclesPassed, config, random)
        val minGapLeft = config.borderMargin
        val maxGapLeft = 1f - config.borderMargin - gapWidth

        val gapLeft: Float = if (previousGapCenter == null) {
            // First obstacle: center-biased
            val centerTarget = 0.5f - (gapWidth / 2f)
            val variation = (random.nextFloat() - 0.5f) * 0.2f
            (centerTarget + variation).coerceIn(minGapLeft, maxGapLeft)
        } else {
            val maxJump = calculateMaxGapJump(obstaclesPassed)
            val minCenter = previousGapCenter - maxJump
            val maxCenter = previousGapCenter + maxJump

            // Random center inside reachable window
            val targetCenter = minCenter + (random.nextFloat() * (maxCenter - minCenter))
            val targetLeft = targetCenter - (gapWidth / 2f)

            targetLeft.coerceIn(minGapLeft, maxGapLeft)
        }

        return Obstacle(
            id = id,
            y = y,
            gapLeft = gapLeft,
            gapWidth = gapWidth,
            thickness = config.obstacleThickness,
            passed = false
        )
    }
}
