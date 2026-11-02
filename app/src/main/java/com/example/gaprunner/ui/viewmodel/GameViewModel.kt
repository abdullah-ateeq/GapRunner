package com.example.gaprunner.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gaprunner.audio.HapticFeedbackManager
import com.example.gaprunner.audio.SoundEffectManager
import com.example.gaprunner.data.UserPreferencesRepository
import com.example.gaprunner.game.GameConfig
import com.example.gaprunner.game.GameEngine
import com.example.gaprunner.game.GamePlayState
import com.example.gaprunner.game.GameState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

sealed class GameViewEvent {
    data class GameOver(val finalScore: Int, val obstaclesPassed: Int, val highestStreak: Int) : GameViewEvent()
}

class GameViewModel(
    private val preferencesRepository: UserPreferencesRepository,
    private val soundManager: SoundEffectManager,
    private val hapticManager: HapticFeedbackManager,
    private val gameEngine: GameEngine = GameEngine(GameConfig())
) : ViewModel() {

    private val _gameState = MutableStateFlow(gameEngine.createInitialState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _gameEvents = MutableSharedFlow<GameViewEvent>()
    val gameEvents: SharedFlow<GameViewEvent> = _gameEvents.asSharedFlow()

    private var gameLoopJob: Job? = null

    init {
        // Collect preferences to keep sound and haptic toggles synced
        viewModelScope.launch {
            preferencesRepository.userPreferencesFlow.collect { prefs ->
                soundManager.isSoundEnabled = prefs.soundEnabled
                hapticManager.isHapticsEnabled = prefs.hapticsEnabled
            }
        }
    }

    fun startGame() {
        _gameState.value = gameEngine.createInitialState(Random.Default).copy(playState = GamePlayState.RUNNING)
        launchGameLoop()
    }

    fun pauseGame() {
        if (_gameState.value.playState == GamePlayState.RUNNING) {
            _gameState.value = _gameState.value.copy(playState = GamePlayState.PAUSED)
            gameLoopJob?.cancel()
        }
    }

    fun resumeGame() {
        if (_gameState.value.playState == GamePlayState.PAUSED) {
            _gameState.value = _gameState.value.copy(playState = GamePlayState.RUNNING)
            launchGameLoop()
        }
    }

    fun restartGame() {
        gameLoopJob?.cancel()
        startGame()
    }

    fun onPlayerDrag(deltaNormalizedX: Float) {
        _gameState.value = gameEngine.movePlayerDelta(_gameState.value, deltaNormalizedX)
    }

    fun onPlayerTap(isRightSide: Boolean) {
        hapticManager.vibrateClick()
        soundManager.playButtonClick()
        _gameState.value = gameEngine.nudgePlayer(_gameState.value, if (isRightSide) 1.0f else -1.0f)
    }

    private fun launchGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            var lastTimeNanos = System.nanoTime()
            while (isActive && _gameState.value.playState == GamePlayState.RUNNING) {
                delay(16L) // ~60 FPS update interval
                val nowNanos = System.nanoTime()
                val dt = ((nowNanos - lastTimeNanos) / 1_000_000_000.0).toFloat().coerceIn(0.001f, 0.05f)
                lastTimeNanos = nowNanos

                val (nextState, events) = gameEngine.update(_gameState.value, dt, Random.Default)
                _gameState.value = nextState

                if (events.obstaclePassed) {
                    soundManager.playPassSound(events.isPerfect)
                    hapticManager.vibratePass()
                }

                if (events.hasCollided) {
                    soundManager.playCollisionSound()
                    hapticManager.vibrateCollision()

                    // Persist results
                    preferencesRepository.recordRunResults(
                        score = nextState.score,
                        streak = nextState.highestStreakThisRun
                    )

                    // Allow brief slowdown / impact freeze animation before transitioning to result
                    launch {
                        delay(650L)
                        _gameEvents.emit(
                            GameViewEvent.GameOver(
                                finalScore = nextState.score,
                                obstaclesPassed = nextState.obstaclesPassed,
                                highestStreak = nextState.highestStreakThisRun
                            )
                        )
                    }
                    break
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        gameLoopJob?.cancel()
    }
}
