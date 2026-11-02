package com.example.gaprunner.game

object GameCollision {

    /**
     * Checks if player collides with a given obstacle.
     * Uses forgiving hitbox boundaries to ensure the gameplay feels responsive and fair.
     */
    fun isColliding(
        playerX: Float,
        playerY: Float,
        obstacle: Obstacle,
        config: GameConfig = GameConfig()
    ): Boolean {
        val playerHalfH = config.playerHeight / 2f
        val playerTop = playerY - playerHalfH
        val playerBottom = playerY + playerHalfH

        val obstacleHalfH = obstacle.thickness / 2f
        val obstacleTop = obstacle.y - obstacleHalfH
        val obstacleBottom = obstacle.y + obstacleHalfH

        // Check if there is vertical overlap between player and obstacle
        val verticalOverlap = playerBottom >= obstacleTop && playerTop <= obstacleBottom
        if (!verticalOverlap) {
            return false
        }

        // Apply forgiving inner margin to player's horizontal bounds
        val forgiveness = config.playerWidth * config.hitboxForgiveness
        val effectiveLeft = (playerX - config.playerWidth / 2f) + forgiveness
        val effectiveRight = (playerX + config.playerWidth / 2f) - forgiveness

        // The player survives if completely inside safe gap
        val insideGap = effectiveLeft >= obstacle.gapLeft && effectiveRight <= obstacle.gapRight

        return !insideGap
    }

    /**
     * Checks if the obstacle has safely crossed the player's collision plane.
     */
    fun hasPassedPlayer(
        playerY: Float,
        obstacle: Obstacle
    ): Boolean {
        // Obstacle has cleared the player when its top is lower than player's bottom
        return obstacle.y - (obstacle.thickness / 2f) > playerY
    }

    /**
     * Clamps the player's horizontal coordinate within playfield boundaries.
     */
    fun clampPlayerX(
        targetX: Float,
        config: GameConfig = GameConfig()
    ): Float {
        val halfW = config.playerWidth / 2f
        val minX = config.borderMargin + halfW
        val maxX = 1f - config.borderMargin - halfW
        return targetX.coerceIn(minX, maxX)
    }
}
