package com.example.gaprunner.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gaprunner.game.GamePlayState
import com.example.gaprunner.game.GameState
import com.example.gaprunner.ui.theme.ArcadeAccentCyan
import com.example.gaprunner.ui.theme.ArcadeAccentBlue
import com.example.gaprunner.ui.theme.ArcadeGold
import com.example.gaprunner.ui.theme.ArcadeImpactRed
import com.example.gaprunner.ui.viewmodel.GameViewEvent
import com.example.gaprunner.ui.viewmodel.GameViewModel
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onGameOver: (score: Int, obstacles: Int, streak: Int) -> Unit,
    onNavigateHome: () -> Unit
) {
    val gameState by viewModel.gameState.collectAsState()

    // Start game on initial composition if in READY state
    LaunchedEffect(Unit) {
        if (gameState.playState == GamePlayState.READY) {
            viewModel.startGame()
        }
    }

    // Listen for Game Over event
    LaunchedEffect(viewModel) {
        viewModel.gameEvents.collect { event ->
            when (event) {
                is GameViewEvent.GameOver -> {
                    onGameOver(event.finalScore, event.obstaclesPassed, event.highestStreak)
                }
            }
        }
    }

    // Screen Shake calculation
    val shakeOffset = if (gameState.screenShakeIntensity > 0f) {
        val intensity = gameState.screenShakeIntensity
        val t = gameState.timeElapsedSeconds * 50f
        val x = (sin(t) * 14f * intensity).roundToInt()
        val y = (cos(t * 1.3f) * 10f * intensity).roundToInt()
        IntOffset(x, y)
    } else {
        IntOffset.Zero
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .offset { shakeOffset }
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Constrain playfield width on large screens / tablets for optimal gameplay
            val playfieldWidth = if (maxWidth > 540.dp) 500.dp else maxWidth

            Box(
                modifier = Modifier
                    .width(playfieldWidth)
                    .fillMaxHeight()
                    // Tap controls
                    .pointerInput(gameState.playState) {
                        detectTapGestures { offset ->
                            if (gameState.playState == GamePlayState.RUNNING) {
                                val isRight = offset.x > size.width / 2f
                                viewModel.onPlayerTap(isRight)
                            }
                        }
                    }
                    // Drag gesture controls
                    .pointerInput(gameState.playState) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            if (gameState.playState == GamePlayState.RUNNING && size.width > 0) {
                                val deltaNormalized = dragAmount.x / size.width.toFloat()
                                viewModel.onPlayerDrag(deltaNormalized)
                            }
                        }
                    }
            ) {
                // Compose Canvas rendering of playfield
                GameCanvas(
                    gameState = gameState,
                    modifier = Modifier.fillMaxSize()
                )

                // Top HUD
                GameHud(
                    gameState = gameState,
                    onPauseClick = { viewModel.pauseGame() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                )

                // Pause Dialog Overlay
                AnimatedVisibility(
                    visible = gameState.playState == GamePlayState.PAUSED,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    PauseOverlay(
                        onResume = { viewModel.resumeGame() },
                        onRestart = { viewModel.restartGame() },
                        onHome = onNavigateHome
                    )
                }
            }
        }
    }
}

@Composable
fun GameHud(
    gameState: GameState,
    onPauseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Score & Streak
        Column {
            Text(
                text = "${gameState.score}",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (gameState.currentStreak > 1) {
                    Text(
                        text = "STREAK ${gameState.currentStreak}x",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = ArcadeAccentCyan
                        )
                    )
                }

                // Speed / Difficulty badge
                Text(
                    text = gameState.difficulty.label.uppercase(),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }

        // Small Pause Button
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                .clickable(onClick = onPauseClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "❚❚",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun PauseOverlay(
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onHome: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "PAUSED",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onResume,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ArcadeAccentCyan,
                    contentColor = Color(0xFF090D16)
                )
            ) {
                Text(
                    text = "RESUME",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            OutlinedButton(
                onClick = onRestart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "RESTART",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            OutlinedButton(
                onClick = onHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "HOME",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
fun GameCanvas(
    gameState: GameState,
    modifier: Modifier = Modifier
) {
    val obstacleBaseColor = MaterialTheme.colorScheme.outline
    val obstaclePassedColor = ArcadeAccentCyan
    val playerColor = ArcadeAccentCyan
    val impactColor = ArcadeImpactRed
    val textColorArgb = MaterialTheme.colorScheme.onBackground.hashCode()

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 1. Subtle scrolling speed lines
        val lineSpacing = h * 0.15f
        val lineOffset = (gameState.timeElapsedSeconds * gameState.currentSpeed * h) % lineSpacing
        var curY = lineOffset
        while (curY < h) {
            drawLine(
                color = obstacleBaseColor.copy(alpha = 0.12f),
                start = Offset(w * 0.1f, curY),
                end = Offset(w * 0.9f, curY),
                strokeWidth = 1.dp.toPx()
            )
            curY += lineSpacing
        }

        // 2. Draw Moving Obstacles with Safe Gaps
        for (obs in gameState.obstacles) {
            val obsCenterY = obs.y * h
            val thicknessPx = obs.thickness * h
            val halfThick = thicknessPx / 2f
            val gapLeftPx = obs.gapLeft * w
            val gapRightPx = obs.gapRight * w

            val barColor = if (obs.highlightProgress > 0f) {
                obstaclePassedColor.copy(alpha = 0.4f + 0.6f * obs.highlightProgress)
            } else {
                obstacleBaseColor
            }

            // Left obstacle segment
            if (gapLeftPx > 0f) {
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(0f, obsCenterY - halfThick),
                    size = Size(gapLeftPx, thicknessPx),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }

            // Right obstacle segment
            if (gapRightPx < w) {
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(gapRightPx, obsCenterY - halfThick),
                    size = Size(w - gapRightPx, thicknessPx),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }

            // Subtle vertical glow beam marking the safe gap
            drawRect(
                color = playerColor.copy(alpha = if (obs.highlightProgress > 0f) 0.18f else 0.05f),
                topLeft = Offset(gapLeftPx, 0f),
                size = Size(gapRightPx - gapLeftPx, h)
            )

            // Gap edge markers
            drawRoundRect(
                color = playerColor.copy(alpha = 0.85f),
                topLeft = Offset(gapLeftPx - 2.dp.toPx(), obsCenterY - halfThick),
                size = Size(3.dp.toPx(), thicknessPx),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
            drawRoundRect(
                color = playerColor.copy(alpha = 0.85f),
                topLeft = Offset(gapRightPx - 1.dp.toPx(), obsCenterY - halfThick),
                size = Size(3.dp.toPx(), thicknessPx),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
        }

        // 3. Subtle Player Trail
        for (pt in gameState.trail) {
            val trailW = w * 0.065f
            val trailH = h * 0.028f
            drawRoundRect(
                color = playerColor.copy(alpha = pt.alpha * 0.35f),
                topLeft = Offset((pt.x * w) - (trailW / 2f), (pt.y * h) - (trailH / 2f)),
                size = Size(trailW, trailH),
                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
            )
        }

        // 4. Player Runner
        val playerCenterXPx = gameState.playerX * w
        val playerCenterYPx = gameState.playerY * h
        val playerWPx = w * 0.075f
        val playerHPx = h * 0.038f

        // Outer glow
        drawRoundRect(
            color = playerColor.copy(alpha = 0.25f),
            topLeft = Offset(playerCenterXPx - (playerWPx * 1.3f) / 2f, playerCenterYPx - (playerHPx * 1.3f) / 2f),
            size = Size(playerWPx * 1.3f, playerHPx * 1.3f),
            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
        )

        // Main player body
        val bodyColor = if (gameState.playState == GamePlayState.GAME_OVER) impactColor else playerColor
        drawRoundRect(
            color = bodyColor,
            topLeft = Offset(playerCenterXPx - playerWPx / 2f, playerCenterYPx - playerHPx / 2f),
            size = Size(playerWPx, playerHPx),
            cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
        )

        // Player core accent
        drawRoundRect(
            color = Color.White.copy(alpha = 0.7f),
            topLeft = Offset(playerCenterXPx - (playerWPx * 0.4f) / 2f, playerCenterYPx - (playerHPx * 0.4f) / 2f),
            size = Size(playerWPx * 0.4f, playerHPx * 0.4f),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )

        // 5. Collision Impact Visuals
        if (gameState.playState == GamePlayState.GAME_OVER && gameState.collisionImpactX != null) {
            val impX = gameState.collisionImpactX * w
            val impY = (gameState.collisionImpactY ?: gameState.playerY) * h
            val ringRadius = (1.0f - gameState.screenShakeIntensity) * 45.dp.toPx()

            drawCircle(
                color = impactColor.copy(alpha = gameState.screenShakeIntensity),
                radius = ringRadius.coerceAtLeast(10f),
                center = Offset(impX, impY),
                style = Stroke(width = 3.dp.toPx())
            )
        }

        // 6. Floating Score Pops (using native canvas text)
        for (pop in gameState.scorePops) {
            val textPaint = android.graphics.Paint().apply {
                color = if (pop.isPerfect) android.graphics.Color.YELLOW else android.graphics.Color.WHITE
                alpha = (pop.alpha * 255).toInt().coerceIn(0, 255)
                textSize = 36f
                isFakeBoldText = true
                textAlign = android.graphics.Paint.Align.CENTER
            }
            drawContext.canvas.nativeCanvas.drawText(
                pop.text,
                pop.x * w,
                pop.y * h,
                textPaint
            )
        }
    }
}
