package com.example

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.abs
import kotlin.math.sign

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("football_game_prefs", Context.MODE_PRIVATE)
    private val PREF_HAS_PLAYED_FIRST = "has_played_arcade_first"

    private val _gameState = MutableStateFlow(createInitialGameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()
    
    private var aiJob: Job? = null

    private fun createInitialGameState(): GameState {
        val hasPlayedFirst = prefs.getBoolean(PREF_HAS_PLAYED_FIRST, false)
        val baseState = GameState.initial()
        return if (!hasPlayedFirst) {
            // First time launching: directly enter Arcade mode without friction!
            prefs.edit().putBoolean(PREF_HAS_PLAYED_FIRST, true).apply()
            baseState.copy(
                isInMainMenu = false,
                isPregame = false,
                gameMode = GameMode.ARCADE,
                actionsLeft = 1,
                isFirstTimeUser = true,
                uiMessage = "⚡ מצב ארקייד מהיר: פעולה 1 לתור! הראשון ל-3 שערים מנצח! 🎯"
            )
        } else {
            // Already played before: open Main Menu
            baseState.copy(
                isInMainMenu = true,
                isPregame = false,
                isFirstTimeUser = false
            )
        }
    }

    fun startArcadeMode() {
        prefs.edit().putBoolean(PREF_HAS_PLAYED_FIRST, true).apply()
        aiJob?.cancel()
        _gameState.update {
            val newPlayers = GameState.createInitialPlayers(formation = Formation.F442)
            it.copy(
                players = newPlayers,
                isPregame = false,
                isInMainMenu = false,
                gameMode = GameMode.ARCADE,
                ballPosition = Position(5, 7),
                playerWithBallId = null,
                currentTurn = Team.HOME,
                actionsLeft = 1,
                homeScore = 0,
                awayScore = 0,
                matchMinute = 0,
                half = 1,
                isHalfTime = false,
                isMatchOver = false,
                actedPlayerIds = emptySet(),
                activeSetPiece = null,
                tacticalDuel = null,
                uiMessage = "⚡ מצב ארקייד מהיר: פעולה 1 לתור! הראשון ל-3 שערים מנצח! 🎯"
            )
        }
    }

    fun startTacticalMode() {
        prefs.edit().putBoolean(PREF_HAS_PLAYED_FIRST, true).apply()
        aiJob?.cancel()
        _gameState.update {
            it.copy(
                isInMainMenu = false,
                isPregame = true, // Opens tactical formation selection
                gameMode = GameMode.TACTICAL,
                actionsLeft = 3,
                homeScore = 0,
                awayScore = 0,
                matchMinute = 0,
                half = 1,
                isHalfTime = false,
                isMatchOver = false,
                actedPlayerIds = emptySet(),
                activeSetPiece = null,
                tacticalDuel = null
            )
        }
    }

    fun returnToMainMenu() {
        aiJob?.cancel()
        _gameState.update {
            it.copy(
                isInMainMenu = true,
                selectedPlayerId = null,
                activeSetPiece = null,
                tacticalDuel = null
            )
        }
    }

    fun resetFirstTimeExperience() {
        prefs.edit().putBoolean(PREF_HAS_PLAYED_FIRST, false).apply()
        aiJob?.cancel()
        _gameState.update {
            val newPlayers = GameState.createInitialPlayers(formation = Formation.F442)
            it.copy(
                players = newPlayers,
                isInMainMenu = false,
                isPregame = false,
                gameMode = GameMode.ARCADE,
                isFirstTimeUser = true,
                ballPosition = Position(5, 7),
                playerWithBallId = null,
                currentTurn = Team.HOME,
                actionsLeft = 1,
                homeScore = 0,
                awayScore = 0,
                matchMinute = 0,
                half = 1,
                isHalfTime = false,
                isMatchOver = false,
                actedPlayerIds = emptySet(),
                activeSetPiece = null,
                tacticalDuel = null,
                uiMessage = "⚡ מצב ארקייד מהיר: פעולה 1 לתור! הראשון ל-3 שערים מנצח! 🎯"
            )
        }
    }

    private fun triggerAIIfNeeded() {
        val state = _gameState.value
        if (state.currentTurn == Team.AWAY && state.actionsLeft > 0) {
            aiJob?.cancel()
            aiJob = viewModelScope.launch {
                delay(450) // snappier tactical response while retaining smooth pacing
                makeAIMove()
            }
        }
    }

    fun setAIDifficulty(difficulty: AIDifficulty) {
        _gameState.update { it.copy(aiDifficulty = difficulty) }
    }

    private fun setAIThought(thought: String?) {
        _gameState.update { it.copy(aiThought = thought) }
    }

    fun startGame(formation: Formation) {
        _gameState.update { 
            val newPlayers = GameState.createInitialPlayers(formation = formation)
            it.copy(
                players = newPlayers,
                isPregame = false,
                isInMainMenu = false,
                gameMode = GameMode.TACTICAL,
                ballPosition = Position(5, 7),
                playerWithBallId = null,
                currentTurn = Team.HOME,
                actionsLeft = 3,
                actedPlayerIds = emptySet(),
                uiMessage = "המשחק הטקטי מתחיל!"
            )
        }
    }

    private fun makeAIMove() {
        val state = _gameState.value
        if (state.currentTurn != Team.AWAY || state.actionsLeft <= 0 || state.isHalfTime || state.isMatchOver) return

        // -------------------------------------------------------------
        // SCENARIO 0: ACTIVE SET PIECE (CORNER OR THROW-IN FOR AI)
        // -------------------------------------------------------------
        val setPiece = state.activeSetPiece
        if (setPiece != null && setPiece.takingTeam == Team.AWAY) {
            val taker = state.players.find { it.id == setPiece.takerPlayerId }
            if (taker != null) {
                if (setPiece.type == SetPieceType.CORNER) {
                    // AI Crosses corner into box (target home goal is at y=14)
                    val boxTargets = state.players.filter { 
                        it.team == Team.AWAY && it.id != taker.id && !it.isRedCarded && it.position.y in 11..13 && it.position.x in 2..8 
                    }
                    val target = boxTargets.maxByOrNull { it.stats.sht } 
                        ?: state.players.filter { it.team == Team.AWAY && it.id != taker.id && !it.isRedCarded }.minByOrNull { abs(it.position.x - 5) + abs(it.position.y - 14) }
                    val crossPos = target?.position ?: Position(5, 12)
                    setAIThought("מגביה כדור קרן מסוכן לתוך הרחבה!")
                    takeSetPiece(taker.id, crossPos)
                    return
                } else {
                    // Throw-in to nearest teammate
                    val nearbyTeammate = state.players.filter { 
                        it.team == Team.AWAY && it.id != taker.id && !it.isRedCarded 
                    }.minByOrNull { abs(it.position.x - taker.position.x) + abs(it.position.y - taker.position.y) }
                    val targetPos = nearbyTeammate?.position ?: Position(taker.position.x.coerceIn(1, 9), taker.position.y)
                    setAIThought("מוציא כדור חוץ מהיר לחבר לקבוצה")
                    takeSetPiece(taker.id, targetPos)
                    return
                }
            }
        }

        val awayPlayers = state.players.filter { 
            it.team == Team.AWAY && !it.isRedCarded && !state.actedPlayerIds.contains(it.id) 
        }
        if (awayPlayers.isEmpty()) {
            consumeAction(state.players, state.ballPosition, state.playerWithBallId, "AI סיים את התור", null, forceEndTurn = true)
            return
        }

        val playerWithBall = state.players.find { it.id == state.playerWithBallId }
        val ballPos = state.ballPosition ?: playerWithBall?.position ?: return

        // -------------------------------------------------------------
        // SCENARIO 1: AI HAS THE BALL (OFFENSIVE PHASE)
        // -------------------------------------------------------------
        if (playerWithBall != null && playerWithBall.team == Team.AWAY && !playerWithBall.isRedCarded && !state.actedPlayerIds.contains(playerWithBall.id)) {
            val distToGoal = kotlin.math.sqrt(
                ((5 - playerWithBall.position.x) * (5 - playerWithBall.position.x) + 
                 (14 - playerWithBall.position.y) * (14 - playerWithBall.position.y)).toDouble()
            )
            val shootChance = calculateShootProbability(playerWithBall, state.players).first

            // 1. Shoot Decision: Legends shoot precisely, Amateurs might shoot randomly
            val shootThresholdDist = if (state.aiDifficulty == AIDifficulty.LEGEND) 6.0 else if (state.aiDifficulty == AIDifficulty.PRO) 5.5 else 4.2
            val shootThresholdChance = if (state.aiDifficulty == AIDifficulty.LEGEND) 25 else if (state.aiDifficulty == AIDifficulty.PRO) 30 else 35

            if (distToGoal <= 4.2 || (distToGoal <= shootThresholdDist && shootChance >= shootThresholdChance)) {
                setAIThought("מזהה מצב הבקעה - בועט לשער!")
                performShoot(playerWithBall.id)
                return
            }

            // 2. Passing Decision: 
            val passRange = playerWithBall.passRange
            val homeOpponents = state.players.filter { it.team == Team.HOME && !it.isRedCarded }
            val awayOffsideLine = homeOpponents.sortedByDescending { it.position.y }.getOrNull(1)?.position?.y ?: 14

            val potentialPassTargets = awayPlayers.filter { teammate ->
                val isOffside = teammate.position.y >= 8 && teammate.position.y > playerWithBall.position.y && teammate.position.y > awayOffsideLine
                
                teammate.id != playerWithBall.id &&
                !isOffside &&
                kotlin.math.sqrt(
                    ((teammate.position.x - playerWithBall.position.x) * (teammate.position.x - playerWithBall.position.x) + 
                     (teammate.position.y - playerWithBall.position.y) * (teammate.position.y - playerWithBall.position.y)).toDouble()
                ) <= passRange
            }

            // Legend / Pro will check passing lanes. Amateur might just pass blindly forward
            val safePassTarget = potentialPassTargets
                .filter { target ->
                    if (state.aiDifficulty == AIDifficulty.AMATEUR) true // Amateurs don't check for interceptors!
                    else getInterceptors(playerWithBall, target.position, state.players).isEmpty()
                }
                .filter { it.position.y >= playerWithBall.position.y - 2 } // Allow slight backward passes for Legend
                .maxByOrNull { target ->
                    val forwardScore = (target.position.y - playerWithBall.position.y) * 10
                    val goalDist = kotlin.math.sqrt(((5 - target.position.x) * (5 - target.position.x) + (14 - target.position.y) * (14 - target.position.y)).toDouble())
                    forwardScore - (goalDist * 3).toInt()
                }

            if (safePassTarget != null && (safePassTarget.position.y > playerWithBall.position.y || distToGoal > 7)) {
                setAIThought("מסירת עומק חכמה לחבר פנוי!")
                performPass(playerWithBall.id, safePassTarget.position)
                return
            }

            // 3. Give & Go (Legend only)
            if (state.aiDifficulty == AIDifficulty.LEGEND && state.actionsLeft >= 2) {
                // If there's a short pass available, make it, to trigger a run next turn
                val shortPassTarget = potentialPassTargets.filter { getInterceptors(playerWithBall, it.position, state.players).isEmpty() }
                                      .minByOrNull { kotlin.math.sqrt(((it.position.x - playerWithBall.position.x)*(it.position.x - playerWithBall.position.x) + (it.position.y - playerWithBall.position.y)*(it.position.y - playerWithBall.position.y)).toDouble()) }
                if (shortPassTarget != null) {
                    setAIThought("מתכנן דאבל-פס (Give & Go) שובר הגנה!")
                    performPass(playerWithBall.id, shortPassTarget.position)
                    return
                }
            }

            // 4. Dribble Decision
            val moveRange = playerWithBall.moveRange
            var bestDribblePos = playerWithBall.position
            var bestScore = -9999f

            for (dx in -moveRange..moveRange) {
                for (dy in -1..moveRange) { 
                    val candidateX = (playerWithBall.position.x + dx).coerceIn(0, 10)
                    val candidateY = (playerWithBall.position.y + dy).coerceIn(0, 14)
                    val candidatePos = Position(candidateX, candidateY)

                    if (state.players.none { it.position == candidatePos }) {
                        val forwardProgress = candidateY - playerWithBall.position.y
                        val distToCenterGoal = kotlin.math.sqrt(((candidateX - 5) * (candidateX - 5) + (candidateY - 14) * (candidateY - 14)).toDouble()).toFloat()
                        
                        val closestHomeDefenderDist = state.players
                            .filter { it.team == Team.HOME && !it.isRedCarded }
                            .minOfOrNull { abs(it.position.x - candidateX) + abs(it.position.y - candidateY) } ?: 5
                        
                        val defenderPenalty = if (state.aiDifficulty == AIDifficulty.AMATEUR) 0f // Amateurs dribble into defenders
                        else if (closestHomeDefenderDist <= 1) -15f else (closestHomeDefenderDist * 2f)

                        val score = (forwardProgress * 8f) - (distToCenterGoal * 3f) + defenderPenalty

                        if (score > bestScore) {
                            bestScore = score
                            bestDribblePos = candidatePos
                        }
                    }
                }
            }

            if (bestDribblePos != playerWithBall.position) {
                setAIThought("דוהר לשטח מת כדי לפתוח זווית")
                performDribble(playerWithBall.id, bestDribblePos)
                return
            } else if (potentialPassTargets.isNotEmpty()) {
                setAIThought("מוסר אחורה כי השטח חסום")
                val fallbackTarget = potentialPassTargets.maxByOrNull { it.position.y }!!
                performPass(playerWithBall.id, fallbackTarget.position)
                return
            } else {
                setAIThought("השחקן נתקע במקום!")
                consumeAction(state.players, null, playerWithBall.id, "AI חסום", playerWithBall.id)
                return
            }
        }

        // -------------------------------------------------------------
        // SCENARIO 1.5: AI HAS BALL, BUT BALL CARRIER ALREADY ACTED THIS TURN
        // -------------------------------------------------------------
        if (playerWithBall != null && playerWithBall.team == Team.AWAY && state.actedPlayerIds.contains(playerWithBall.id)) {
            val supportingPlayers = awayPlayers.filter { it.id != playerWithBall.id && it.role != PlayerRole.GOALKEEPER }
            val runner = supportingPlayers.minByOrNull { abs(it.position.x - 5) + abs(it.position.y - 14) }
            if (runner != null) {
                val moveRange = runner.moveRange
                var bestPos = runner.position
                var bestDist = Float.MAX_VALUE
                for (dx in -moveRange..moveRange) {
                    for (dy in 0..moveRange) {
                        val cX = (runner.position.x + dx).coerceIn(0, 10)
                        val cY = (runner.position.y + dy).coerceIn(0, 14)
                        val cPos = Position(cX, cY)
                        if (state.players.none { it.position == cPos }) {
                            val distToGoal = kotlin.math.sqrt(((cX - 5) * (cX - 5) + (cY - 14) * (cY - 14)).toDouble()).toFloat()
                            if (distToGoal < bestDist) {
                                bestDist = distToGoal
                                bestPos = cPos
                            }
                        }
                    }
                }
                if (bestPos != runner.position) {
                    setAIThought("תנועה ללא כדור של ${runner.name} לסיוע בהתקפה")
                    performMove(runner.id, bestPos)
                    return
                }
            }
        }

        // -------------------------------------------------------------
        // SCENARIO 2: DEFENSIVE PHASE (USER HAS BALL OR LOOSE BALL)
        // -------------------------------------------------------------
        if (playerWithBall != null && playerWithBall.team == Team.HOME) {
            
            // 0. Sweeper Keeper (Legend / Pro)
            if (state.aiDifficulty != AIDifficulty.AMATEUR && playerWithBall.position.y <= 4) { // Home attacker close to Away goal (y=0)
                val keeper = awayPlayers.find { it.role == PlayerRole.GOALKEEPER }
                if (keeper != null && keeper.position.y <= 2) { // Keeper is near line
                    // If no defender is closer to the ball than the keeper...
                    val closestDefenderDist = awayPlayers.filter { it.role != PlayerRole.GOALKEEPER }.minOfOrNull { abs(it.position.x - playerWithBall.position.x) + abs(it.position.y - playerWithBall.position.y) } ?: 99
                    val keeperDist = abs(keeper.position.x - playerWithBall.position.x) + abs(keeper.position.y - playerWithBall.position.y)
                    
                    if (keeperDist <= closestDefenderDist + 1) { // Keeper rushes out
                        // Move keeper towards ball
                        val kMoveX = (playerWithBall.position.x - keeper.position.x).sign
                        val kMoveY = (playerWithBall.position.y - keeper.position.y).sign
                        val newKeeperPos = Position((keeper.position.x + kMoveX).coerceIn(3, 7), (keeper.position.y + kMoveY).coerceIn(0, 3))
                        if (state.players.none { it.position == newKeeperPos }) {
                            setAIThought("השוער יוצא לסגור זווית באחד-על-אחד!")
                            performMove(keeper.id, newKeeperPos)
                            return
                        }
                    }
                }
            }

            // A. Immediate Tackle if adjacent
            val adjacentAI = awayPlayers.find { isAdjacent(it.position, playerWithBall.position) }
            if (adjacentAI != null) {
                setAIThought("יוצא לתיקול אגרסיבי!")
                performTackle(adjacentAI.id, playerWithBall.id)
                return
            }

            // B. Tactical Cut / Passing Lane block (Legend / Pro)
            if (state.aiDifficulty != AIDifficulty.AMATEUR) {
                val openHomeAttackers = state.players.filter { 
                    it.team == Team.HOME && 
                    it.id != playerWithBall.id && 
                    !it.isRedCarded && 
                    it.position.y < playerWithBall.position.y
                }

                if (openHomeAttackers.isNotEmpty()) {
                    for (defender in awayPlayers.filter { it.role == PlayerRole.DEFENDER || it.role == PlayerRole.MIDFIELDER }) {
                        val dMove = defender.moveRange
                        for (attacker in openHomeAttackers) {
                            for (dx in -dMove..dMove) {
                                for (dy in -dMove..dMove) {
                                    val cX = (defender.position.x + dx).coerceIn(0, 10)
                                    val cY = (defender.position.y + dy).coerceIn(0, 14)
                                    val cPos = Position(cX, cY)
                                    if (state.players.none { it.position == cPos }) {
                                        val simulatedDefender = defender.copy(position = cPos)
                                        val simulatedPlayers = state.players.filter { it.id != defender.id } + simulatedDefender
                                        if (getInterceptors(playerWithBall, attacker.position, simulatedPlayers).any { it.id == defender.id }) {
                                            setAIThought("מזהה חלוץ פנוי – סוגר את קו המסירה!")
                                            performMove(defender.id, cPos)
                                            return
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // C. Pressing
            val closestAI = awayPlayers.minByOrNull { 
                abs(it.position.x - ballPos.x) + abs(it.position.y - ballPos.y) 
            }
            
            if (closestAI != null) {
                val moveRange = closestAI.moveRange
                var bestPos = closestAI.position
                var minDistanceSq = Float.MAX_VALUE
                
                for (dx in -moveRange..moveRange) {
                    for (dy in -moveRange..moveRange) {
                        val candidateX = (closestAI.position.x + dx).coerceIn(0, 10)
                        val candidateY = (closestAI.position.y + dy).coerceIn(0, 14)
                        
                        if (state.players.none { it.position.x == candidateX && it.position.y == candidateY }) {
                            val distSq = ((candidateX - ballPos.x) * (candidateX - ballPos.x) + (candidateY - ballPos.y) * (candidateY - ballPos.y)).toFloat()
                            if (distSq < minDistanceSq) {
                                minDistanceSq = distSq
                                bestPos = Position(candidateX, candidateY)
                            }
                        }
                    }
                }
                
                if (bestPos != closestAI.position) {
                    setAIThought("לוחץ על מוביל הכדור לצמצום מרחב")
                    performMove(closestAI.id, bestPos)
                    return
                }
            }
        }

        // -------------------------------------------------------------
        // SCENARIO 3: LOOSE BALL ON PITCH
        // -------------------------------------------------------------
        val closestToBall = awayPlayers.minByOrNull { 
            abs(it.position.x - ballPos.x) + abs(it.position.y - ballPos.y) 
        }
        if (closestToBall != null) {
            val moveRange = closestToBall.moveRange
            var bestPos = closestToBall.position
            var minDistanceSq = Float.MAX_VALUE
            
            for (dx in -moveRange..moveRange) {
                for (dy in -moveRange..moveRange) {
                    val candidateX = (closestToBall.position.x + dx).coerceIn(0, 10)
                    val candidateY = (closestToBall.position.y + dy).coerceIn(0, 14)
                    
                    if (state.players.none { it.position.x == candidateX && it.position.y == candidateY }) {
                        val distSq = ((candidateX - ballPos.x) * (candidateX - ballPos.x) + (candidateY - ballPos.y) * (candidateY - ballPos.y)).toFloat()
                        if (distSq < minDistanceSq) {
                            minDistanceSq = distSq
                            bestPos = Position(candidateX, candidateY)
                        }
                    }
                }
            }
            if (bestPos != closestToBall.position) {
                setAIThought("דוהר לכדור החופשי כדי להשתלט עליו!")
                performMove(closestToBall.id, bestPos)
                return
            }
        }

        setAIThought("ה-AI עומד במקום ושומר על העמדה.")
        consumeAction(state.players, state.ballPosition, state.playerWithBallId, "AI דילג על מהלך", null)
    }

    fun onPlayerSelected(playerId: Int) {
        val state = _gameState.value
        if (state.isHalfTime || state.isMatchOver) return
        android.util.Log.d("GameViewModel", "onPlayerSelected: playerId=$playerId, currentTurn=${state.currentTurn}")
        if (playerId == -1) {
            _gameState.update { it.copy(selectedPlayerId = null, actionMenuOpen = false, previewActions = emptyList(), selectedAction = null) }
            return
        }
        val player = state.players.find { it.id == playerId } ?: return
        
        if (player.isRedCarded) {
            _gameState.update { it.copy(uiMessage = "השחקן מורחק בכרטיס אדום ולא יכול לפעול!") }
            return
        }

        // Can only select own team players unless inspecting or tackling an opponent holding the ball!
        if (player.team != state.currentTurn) {
            val currentSelected = state.players.find { 
                it.id == state.selectedPlayerId && 
                it.team == state.currentTurn && 
                !state.actedPlayerIds.contains(it.id) 
            }
            if (currentSelected != null && player.id == state.playerWithBallId) {
                // User has a friendly player selected and clicked directly on the opponent with the ball to tackle!
                val dist = maxOf(abs(currentSelected.position.x - player.position.x), abs(currentSelected.position.y - player.position.y))
                if (dist <= currentSelected.moveRange) {
                    performTackle(currentSelected.id, player.id)
                    return
                } else {
                    _gameState.update { 
                        it.copy(
                            uiMessage = "⚠️ ${player.name} מחוץ לטווח התנועה של ${currentSelected.name} (מרחק: $dist, טווח מהירות: ${currentSelected.moveRange})!"
                        ) 
                    }
                    return
                }
            }

            _gameState.update { 
                it.copy(
                    selectedPlayerId = playerId, 
                    actionMenuOpen = false, 
                    previewActions = emptyList(),
                    uiMessage = "שחקן יריב (צפייה בנתונים)" 
                ) 
            }
            return
        }

        // Prevent player from acting multiple times in the same turn
        if (state.actedPlayerIds.contains(playerId)) {
            _gameState.update { 
                it.copy(
                    selectedPlayerId = playerId,
                    actionMenuOpen = false,
                    previewActions = emptyList(),
                    uiMessage = "${player.name} כבר ביצע פעולה בתור זה!"
                ) 
            }
            return
        }

        
        _gameState.update { currentState ->
            val hasBall = currentState.playerWithBallId == player.id
            val opponentWithBall = currentState.players.find { it.id == currentState.playerWithBallId && it.team != player.team }
            val tackleHint = if (opponentWithBall != null) {
                val dist = maxOf(abs(player.position.x - opponentWithBall.position.x), abs(player.position.y - opponentWithBall.position.y))
                if (dist <= player.moveRange) {
                    val costStr = if (dist <= 1) "מהלך 1" else if (currentState.actionsLeft >= 2) "2 מהלכים" else "מהלך 1 (נואש)"
                    " • ⚔️ לחץ על ${opponentWithBall.name} לתיקול ($costStr)!"
                } else ""
            } else ""

            val speedNotice = if (hasBall) {
                "${player.roleIcon} ${player.name} | מהירות כדרור: ${player.currentSpeed} • ${player.moveRange} משבצות מודגשות"
            } else {
                "${player.roleIcon} ${player.name} | מהירות: ${player.currentSpeed} • ${player.moveRange} משבצות מודגשות לתנועה$tackleHint"
            }
            val defaultAction = if (hasBall) null else ActionType.MOVE
            val newState = currentState.copy(
                selectedPlayerId = playerId,
                actionMenuOpen = false,
                selectedAction = defaultAction,
                previewActions = generateActions(player, state),
                uiMessage = speedNotice
            )
            android.util.Log.d("GameViewModel", "[STATE EMIT] Selection opened for ${player.role} (ID: $playerId). Speed: ${player.currentSpeed}, Range: ${player.moveRange}")
            newState
        }
    }

    private fun generateActions(player: Player, state: GameState): List<ActionOption> {
        val actions = mutableListOf<ActionOption>()
        val hasBall = state.playerWithBallId == player.id
        
        if (hasBall) {
            actions.add(ActionOption(ActionType.DRIBBLE, null, "100%"))
            actions.add(ActionOption(ActionType.PASS, null, "PAS"))
            actions.add(ActionOption(ActionType.SHOOT, null, "${calculateShootProbability(player, state.players).first}%"))
        } else {
            actions.add(ActionOption(ActionType.MOVE, null, "100%"))
            
            // Check if opponent with ball is within movement/tackle range
            val opponentWithBall = state.players.find { it.id == state.playerWithBallId && it.team != player.team }
            if (opponentWithBall != null) {
                val dist = maxOf(abs(player.position.x - opponentWithBall.position.x), abs(player.position.y - opponentWithBall.position.y))
                if (dist <= player.moveRange) {
                    val prob = calculateTackleProbability(player, opponentWithBall)
                    val costStr = if (dist <= 1) "מהלך 1" else if (state.actionsLeft >= 2) "2 מהלכים" else "מהלך 1"
                    actions.add(ActionOption(ActionType.TACKLE, opponentWithBall.position, "$prob% ($costStr)"))
                }
            }
        }
        
        return actions
    }

    private fun isAdjacent(p1: Position, p2: Position): Boolean {
        return abs(p1.x - p2.x) <= 1 && abs(p1.y - p2.y) <= 1
    }

    fun calculateShootProbability(player: Player, allPlayers: List<Player>): Pair<Int, Player?> {
        val eval = PitchGridEvaluator.calculateShootEvaluation(_gameState.value, player)
        return Pair(eval.finalProbability, eval.opponentGk)
    }

    private fun calculateTackleProbability(tackler: Player, target: Player): Int {
        val advantage = tackler.currentDef - target.currentPac
        val baseChance = 50 + (advantage / 2)
        val dist = abs(tackler.position.x - target.position.x) + abs(tackler.position.y - target.position.y)
        return (baseChance - (dist * 5)).coerceIn(10, 95)
    }

    private fun calculatePassProbability(passer: Player, targetPos: Position): Int {
        val dist = abs(passer.position.x - targetPos.x) + abs(passer.position.y - targetPos.y)
        val chance = passer.currentPas - (dist * 5)
        return chance.coerceIn(10, 95)
    }

    fun onActionSelected(action: ActionType) {
        val state = _gameState.value
        val player = state.players.find { it.id == state.selectedPlayerId } ?: return
        if (action == ActionType.SHOOT) {
            if (state.selectedAction == ActionType.SHOOT) {
                // If shoot action is already armed, clicking it again confirms and shoots!
                performShoot(player.id)
            } else {
                val eval = PitchGridEvaluator.calculateShootEvaluation(state, player)
                _gameState.update {
                    it.copy(
                        selectedAction = ActionType.SHOOT,
                        actionMenuOpen = false,
                        uiMessage = "🎯 כוון לשער! ${eval.breakdownText} (לחץ על השער או אשר לביצוע)"
                    )
                }
            }
        } else if (action == ActionType.TACKLE) {
            val opponentWithBall = state.players.find { it.id == state.playerWithBallId && it.team != player.team }
            if (opponentWithBall != null) {
                val dist = maxOf(abs(player.position.x - opponentWithBall.position.x), abs(player.position.y - opponentWithBall.position.y))
                if (dist <= player.moveRange) {
                    performTackle(player.id, opponentWithBall.id)
                    return
                } else {
                    _gameState.update {
                        it.copy(
                            uiMessage = "⚠️ ${opponentWithBall.name} מחוץ לטווח התנועה של ${player.name} (מרחק: $dist, טווח מהירות: ${player.moveRange})!"
                        )
                    }
                    return
                }
            }
        } else {
            val actionNotice = when (action) {
                ActionType.MOVE -> "⚡ טווח תנועה לפי מהירות (${player.currentSpeed}): ${player.moveRange} משבצות מודגשות"
                ActionType.DRIBBLE -> "⚡ טווח כדרור לפי מהירות (${player.currentSpeed}): ${player.moveRange} משבצות מודגשות"
                ActionType.PASS -> "🎯 טווח מסירה (${player.currentPassing}): ${player.passRange} משבצות מודגשות"
                else -> null
            }
            _gameState.update { it.copy(selectedAction = action, actionMenuOpen = false, uiMessage = actionNotice) }
        }
    }

    fun onGridCellSelected(x: Int, y: Int) {
        val state = _gameState.value
        if (state.isHalfTime || state.isMatchOver) return

        // If an active set-piece is pending for the human player (e.g. Corner or Throw-in)
        if (state.activeSetPiece != null && state.activeSetPiece.takingTeam == Team.HOME) {
            val takerId = state.activeSetPiece.takerPlayerId
            takeSetPiece(takerId, Position(x, y))
            return
        }

        val action = state.selectedAction
        if (action == null) {
            val clickedPlayer = state.players.find { it.position.x == x && it.position.y == y }
            val currentSelected = state.players.find { it.id == state.selectedPlayerId }

            // In Arcade mode: if ball-carrier is selected and clicks towards opponent's goal (Y <= 2, X in 2..8) -> Direct Shoot!
            if (currentSelected != null && currentSelected.team == state.currentTurn && currentSelected.id == state.playerWithBallId) {
                if (state.gameMode == GameMode.ARCADE && y <= 2 && x in 2..8) {
                    performShoot(currentSelected.id)
                    return
                }
            }

            if (clickedPlayer != null) {
                // If friendly ball-carrier is selected and clicks on a teammate within pass range, execute direct pass!
                if (currentSelected != null && currentSelected.team == state.currentTurn && currentSelected.id == state.playerWithBallId && 
                    clickedPlayer.team == currentSelected.team && clickedPlayer.id != currentSelected.id) {
                    val passTiles = PitchGridEvaluator.computeValidPassingTiles(state, currentSelected)
                    if (passTiles.containsKey(Position(x, y))) {
                        performPass(currentSelected.id, Position(x, y))
                        return
                    }
                }

                // If friendly player is selected and clicks on opponent with ball within move/tackle range: Direct Tackle!
                if (currentSelected != null && currentSelected.team == state.currentTurn && !state.actedPlayerIds.contains(currentSelected.id) &&
                    clickedPlayer.team != currentSelected.team && clickedPlayer.id == state.playerWithBallId) {
                    val dist = maxOf(abs(currentSelected.position.x - x), abs(currentSelected.position.y - y))
                    if (dist <= currentSelected.moveRange) {
                        performTackle(currentSelected.id, clickedPlayer.id)
                        return
                    } else {
                        _gameState.update {
                            it.copy(
                                uiMessage = "⚠️ ${clickedPlayer.name} מחוץ לטווח התנועה של ${currentSelected.name} (מרחק: $dist, טווח מהירות: ${currentSelected.moveRange})!"
                            )
                        }
                        return
                    }
                }

                onPlayerSelected(clickedPlayer.id)
            } else {
                // Clicked an empty pitch tile: check if it's a valid movement coordinate for selected player
                if (currentSelected != null && currentSelected.team == state.currentTurn && !state.actedPlayerIds.contains(currentSelected.id)) {
                    val moveTiles = PitchGridEvaluator.computeValidMovementTiles(state, currentSelected)
                    val targetPos = Position(x, y)
                    if (moveTiles.containsKey(targetPos)) {
                        if (currentSelected.id == state.playerWithBallId) {
                            performDribble(currentSelected.id, targetPos)
                        } else {
                            performMove(currentSelected.id, targetPos)
                        }
                        return
                    } else {
                        val dist = maxOf(abs(currentSelected.position.x - x), abs(currentSelected.position.y - y))
                        _gameState.update {
                            it.copy(
                                uiMessage = "משבצת מחוץ לטווח המהירות של ${currentSelected.name} (מהירות: ${currentSelected.currentSpeed}, מרחק: $dist, טווח: ${currentSelected.moveRange})!"
                            )
                        }
                        return
                    }
                }
                _gameState.update { it.copy(selectedPlayerId = null, actionMenuOpen = false, previewActions = emptyList(), selectedAction = null) }
            }
            return
        }
        val player = state.players.find { it.id == state.selectedPlayerId } ?: return
        
        // If clicking on themselves while an action is pending, cancel the action and reopen the menu
        if (player.position.x == x && player.position.y == y) {
            _gameState.update { it.copy(actionMenuOpen = true, selectedAction = null, uiMessage = null) }
            return
        }
        
        val targetPos = Position(x, y)
        when (action) {
            ActionType.MOVE -> {
                // Check if target coordinate has the opponent holding the ball -> Tackle!
                val opponentWithBall = state.players.find { it.position.x == x && it.position.y == y && it.team != player.team && it.id == state.playerWithBallId }
                if (opponentWithBall != null) {
                    val dist = maxOf(abs(player.position.x - x), abs(player.position.y - y))
                    if (dist <= player.moveRange) {
                        performTackle(player.id, opponentWithBall.id)
                        return
                    } else {
                        _gameState.update {
                            it.copy(
                                uiMessage = "⚠️ ${opponentWithBall.name} מחוץ לטווח התנועה של ${player.name} (מרחק: $dist, טווח מהירות: ${player.moveRange})!"
                            )
                        }
                        return
                    }
                }

                val moveTiles = PitchGridEvaluator.computeValidMovementTiles(state, player)
                if (moveTiles.containsKey(targetPos)) {
                    performMove(player.id, targetPos)
                } else {
                    val dist = maxOf(abs(player.position.x - x), abs(player.position.y - y))
                    _gameState.update { it.copy(uiMessage = "משבצת מחוץ לטווח המהירות של ${player.name} (מהירות: ${player.currentSpeed}, מרחק: $dist, טווח: ${player.moveRange})!", actionMenuOpen = true, selectedAction = ActionType.MOVE) }
                }
            }
            ActionType.DRIBBLE -> {
                val moveTiles = PitchGridEvaluator.computeValidMovementTiles(state, player)
                if (moveTiles.containsKey(targetPos)) {
                    performDribble(player.id, targetPos)
                } else {
                    val dist = maxOf(abs(player.position.x - x), abs(player.position.y - y))
                    _gameState.update { it.copy(uiMessage = "משבצת מחוץ לטווח הכדרור של ${player.name} (מהירות: ${player.currentSpeed}, מרחק: $dist, טווח: ${player.moveRange})!", actionMenuOpen = true, selectedAction = ActionType.DRIBBLE) }
                }
            }
            ActionType.PASS -> {
                val passTiles = PitchGridEvaluator.computeValidPassingTiles(state, player)
                if (passTiles.containsKey(targetPos)) {
                    performPass(player.id, targetPos)
                } else {
                    _gameState.update { it.copy(uiMessage = "משבצת מחוץ לטווח מסירה!", actionMenuOpen = true, selectedAction = null) }
                }
            }
            ActionType.SHOOT -> {
                val targetY = if (player.team == Team.HOME) 0 else 14
                val isTowardGoal = if (player.team == Team.HOME) y <= 4 else y >= 10
                if (isTowardGoal || abs(y - targetY) <= 5) {
                    performShoot(player.id)
                } else {
                    _gameState.update { it.copy(uiMessage = "לחץ ישירות על אזור שער היריב כדי לבעוט לשער!") }
                }
            }
            ActionType.TACKLE -> {
                // Allowed on player with ball or adjacent/in-range
                if (state.playerWithBallId != null) {
                    val target = state.players.find { it.id == state.playerWithBallId }
                    if (target != null) {
                        val moveRange = player.moveRange
                        val dist = maxOf(abs(player.position.x - target.position.x), abs(player.position.y - target.position.y))
                        if (dist <= moveRange) {
                            performTackle(player.id, target.id)
                        } else {
                            _gameState.update { 
                                it.copy(
                                    uiMessage = "⚠️ ${target.name} מחוץ לטווח התנועה של ${player.name} (מרחק: $dist, טווח: $moveRange)!",
                                    actionMenuOpen = false, 
                                    selectedAction = null
                                ) 
                            }
                        }
                    } else {
                        _gameState.update { it.copy(actionMenuOpen = false, selectedAction = null, selectedPlayerId = null) }
                    }
                } else {
                    _gameState.update { it.copy(actionMenuOpen = false, selectedAction = null, selectedPlayerId = null) }
                }
            }
        }
    }

    private fun performMove(playerId: Int, newPos: Position) {
        val updatedPlayers = _gameState.value.players.map { 
            if (it.id == playerId) it.copy(position = newPos) else it 
        }
        var newBallPos = _gameState.value.ballPosition
        var newPlayerWithBall = _gameState.value.playerWithBallId
        
        if (newBallPos != null && newPos == newBallPos) {
            newPlayerWithBall = playerId
            newBallPos = null // Ball is picked up
        }
        
        consumeAction(updatedPlayers, newBallPos, newPlayerWithBall, null, playerId)
    }

    private fun performDribble(playerId: Int, newPos: Position) {
        val updatedPlayers = _gameState.value.players.map { 
            if (it.id == playerId) it.copy(position = newPos) else it 
        }
        consumeAction(updatedPlayers, null, playerId, null, playerId) // ball position is null because player holds it
    }

    fun getInterceptors(passer: Player, targetPos: Position, players: List<Player>): List<Player> {
        val x1 = passer.position.x.toDouble()
        val y1 = passer.position.y.toDouble()
        val x2 = targetPos.x.toDouble()
        val y2 = targetPos.y.toDouble()
        
        val dx = x2 - x1
        val dy = y2 - y1
        val lengthSq = dx * dx + dy * dy
        if (lengthSq < 1.0) return emptyList()

        return players.filter { opp ->
            opp.team != passer.team && !opp.isRedCarded && run {
                val px = opp.position.x.toDouble()
                val py = opp.position.y.toDouble()
                val t = ((px - x1) * dx + (py - y1) * dy) / lengthSq
                if (t in 0.12..0.88) {
                    val projX = x1 + t * dx
                    val projY = y1 + t * dy
                    val distSq = (px - projX) * (px - projX) + (py - projY) * (py - projY)
                    distSq <= 0.45 // within ~0.67 units of perpendicular pass line
                } else {
                    false
                }
            }
        }
    }

    private fun performPass(passerId: Int, targetPos: Position) {
        val passer = _gameState.value.players.find { it.id == passerId } ?: return
        
        val dx = (passer.position.x - targetPos.x).toDouble()
        val dy = (passer.position.y - targetPos.y).toDouble()
        val distance = kotlin.math.sqrt(dx * dx + dy * dy)
        
        if (distance > passer.passRange) {
            _gameState.update { it.copy(uiMessage = "מסירה ארוכה מדי!", actionMenuOpen = false, selectedAction = null, selectedPlayerId = null) }
            return
        }

        val targetPlayer = _gameState.value.players.find { it.position == targetPos }
        
        // --- OFFSIDE RULE ---
        if (targetPlayer != null && targetPlayer.team == passer.team) {
            val opponents = _gameState.value.players.filter { it.team != passer.team && !it.isRedCarded && it.position.y >= 0 }
            var isOffside = false
            
            if (passer.team == Team.HOME) {
                val offsideLine = opponents.sortedBy { it.position.y }.getOrNull(1)?.position?.y ?: 0
                if (targetPlayer.position.y <= 6 && 
                    targetPlayer.position.y < passer.position.y && 
                    targetPlayer.position.y < offsideLine) {
                    isOffside = true
                }
            } else {
                val offsideLine = opponents.sortedByDescending { it.position.y }.getOrNull(1)?.position?.y ?: 14
                if (targetPlayer.position.y >= 8 && 
                    targetPlayer.position.y > passer.position.y && 
                    targetPlayer.position.y > offsideLine) {
                    isOffside = true
                }
            }

            if (isOffside) {
                val nearestOpponent = opponents.minByOrNull { 
                    abs(it.position.x - targetPos.x) + abs(it.position.y - targetPos.y) 
                }
                if (nearestOpponent != null) {
                    consumeAction(
                        _gameState.value.players, 
                        null, 
                        nearestOpponent.id, 
                        "🚩 נבדל! ${targetPlayer.name} נתפס בנבדל.", 
                        passerId,
                        forceEndTurn = true
                    )
                    return
                }
            }
        }
        // --------------------

        val opponents = _gameState.value.players.filter { it.team != passer.team && !it.isRedCarded && it.position.y >= 0 }
        val passTiles = PitchGridEvaluator.computeValidPassingTiles(_gameState.value, passer)
        val passingInfo = passTiles[targetPos]
        val successChance = passingInfo?.successChance ?: calculatePassProbability(passer, targetPos)
        val interceptors = PitchGridEvaluator.getInterceptorsOnLine(passer.position, targetPos, opponents)
        val success = (0..100).random() <= successChance

        // Record pass stats
        val currentStats = _gameState.value.stats
        val updatedPassStats = if (passer.team == Team.HOME) {
            currentStats.copy(homePasses = currentStats.homePasses + 1)
        } else {
            currentStats.copy(awayPasses = currentStats.awayPasses + 1)
        }
        _gameState.update { it.copy(stats = updatedPassStats) }

        // Determine destination position and outcome
        val (finalDestPos, isIntercepted, onArrival) = when {
            // Offside outcome
            targetPlayer != null && targetPlayer.team == passer.team && run {
                val opponents = _gameState.value.players.filter { it.team != passer.team && !it.isRedCarded && it.position.y >= 0 }
                if (passer.team == Team.HOME) {
                    val offsideLine = opponents.sortedBy { it.position.y }.getOrNull(1)?.position?.y ?: 0
                    targetPlayer.position.y <= 6 && targetPlayer.position.y < passer.position.y && targetPlayer.position.y < offsideLine
                } else {
                    val offsideLine = opponents.sortedByDescending { it.position.y }.getOrNull(1)?.position?.y ?: 14
                    targetPlayer.position.y >= 8 && targetPlayer.position.y > passer.position.y && targetPlayer.position.y > offsideLine
                }
            } -> {
                val opponents = _gameState.value.players.filter { it.team != passer.team && !it.isRedCarded && it.position.y >= 0 }
                val nearestOpponent = opponents.minByOrNull { 
                    abs(it.position.x - targetPos.x) + abs(it.position.y - targetPos.y) 
                }
                Triple(targetPos, false) {
                    if (nearestOpponent != null) {
                        consumeAction(
                            _gameState.value.players, 
                            null, 
                            nearestOpponent.id, 
                            "🚩 נבדל! ${targetPlayer.name} נתפס בנבדל.", 
                            passerId,
                            forceEndTurn = true
                        )
                    }
                }
            }
            // Interception outcome
            interceptors.isNotEmpty() && passer.trait != PlayerTrait.PLAYMAKER && run {
                val interceptor = interceptors.minByOrNull { 
                    abs(it.position.x - passer.position.x) + abs(it.position.y - passer.position.y) 
                }!!
                val interceptChance = (35 + (interceptor.currentDef - passer.currentPas) / 2).coerceIn(20, 80)
                (0..100).random() <= interceptChance
            } -> {
                val interceptor = interceptors.minByOrNull { 
                    abs(it.position.x - passer.position.x) + abs(it.position.y - passer.position.y) 
                }!!
                val teamStr = if (interceptor.team == Team.HOME) "קבוצת הבית" else "היריב"
                Triple(interceptor.position, true) {
                    consumeAction(
                        _gameState.value.players, 
                        null, 
                        interceptor.id, 
                        "❌ חטיפה! מס' ${interceptor.number} ($teamStr) חסם את המסירה!", 
                        passerId,
                        forceEndTurn = true
                    )
                }
            }
            // Successful pass
            success -> {
                Triple(targetPos, false) {
                    val targetP = _gameState.value.players.find { it.position == targetPos }
                    if (targetP != null) {
                        consumeAction(_gameState.value.players, null, targetP.id, "מסירה מדויקת של ${passer.name}!", passerId)
                    } else {
                        consumeAction(_gameState.value.players, targetPos, null, "${passer.name} מוסר לשטח הריק.", passerId)
                    }
                }
            }
            // Inaccurate pass / throw-in
            else -> {
                val errX = targetPos.x + (-2..2).random()
                val errY = (targetPos.y + (-1..1).random()).coerceIn(0, 14)
                if (errX < 0 || errX > 10) {
                    val defendingTeam = if (passer.team == Team.HOME) Team.AWAY else Team.HOME
                    val outPos = Position(if (errX < 0) 0 else 10, errY)
                    Triple(outPos, false) {
                        setupThrowIn(takingTeam = defendingTeam, touchlinePos = outPos, outPlayerName = passer.name)
                    }
                } else {
                    val randomPos = Position(errX.coerceIn(0, 10), errY)
                    Triple(randomPos, false) {
                        consumeAction(_gameState.value.players, randomPos, null, "מסירה לא מדויקת של ${passer.name}!", passerId)
                    }
                }
            }
        }

        // Launch smooth trajectory animation matching the calculated probability
        val flightDist = kotlin.math.sqrt(
            (passer.position.x - finalDestPos.x).toDouble().let { it * it } + 
            (passer.position.y - finalDestPos.y).toDouble().let { it * it }
        )
        val animDuration = (480L + (flightDist * 40L).toLong()).coerceIn(580L, 850L)
        val curve = if (passer.currentPas >= 75) 0.32f else 0.14f
        val trajectory = BallTrajectory(
            startX = passer.position.x.toFloat(),
            startY = passer.position.y.toFloat(),
            targetX = finalDestPos.x.toFloat(),
            targetY = finalDestPos.y.toFloat(),
            arcHeight = (flightDist / 3.6f).toFloat().coerceIn(0.7f, 2.0f),
            curveOffset = if (finalDestPos.x >= passer.position.x) curve else -curve,
            probability = successChance,
            isShot = false,
            isIntercepted = isIntercepted,
            durationMs = animDuration
        )

        _gameState.update {
            it.copy(
                ballTrajectory = trajectory,
                ballPosition = null,
                playerWithBallId = null,
                selectedPlayerId = null,
                actionMenuOpen = false,
                selectedAction = null
            )
        }

        viewModelScope.launch {
            delay(animDuration + 60L)
            _gameState.update { it.copy(ballTrajectory = null) }
            onArrival()
        }
    }

    private fun performShoot(shooterId: Int) {
        val shooter = _gameState.value.players.find { it.id == shooterId } ?: return
        val eval = PitchGridEvaluator.calculateShootEvaluation(_gameState.value, shooter)
        val chance = eval.finalProbability
        val gk = eval.opponentGk
        
        val roll = (0..100).random()
        val success = roll <= chance
        
        var homeScore = _gameState.value.homeScore
        var awayScore = _gameState.value.awayScore

        val currentStats = _gameState.value.stats
        val updatedShootStats = if (shooter.team == Team.HOME) {
            currentStats.copy(
                homeShots = currentStats.homeShots + 1,
                homeShotsOnTarget = if (success || roll <= chance + 25) currentStats.homeShotsOnTarget + 1 else currentStats.homeShotsOnTarget
            )
        } else {
            currentStats.copy(
                awayShots = currentStats.awayShots + 1,
                awayShotsOnTarget = if (success || roll <= chance + 25) currentStats.awayShotsOnTarget + 1 else currentStats.awayShotsOnTarget
            )
        }
        _gameState.update { it.copy(stats = updatedShootStats) }

        val targetGoalY = if (shooter.team == Team.HOME) 0 else 14
        val targetGoalX = 5f + if (success) (if ((0..1).random() == 0) -1.2f else 1.2f) else (if ((0..1).random() == 0) -3.2f else 3.2f)
        val animDuration = (540L + (eval.distanceToGoal * 35L).toLong()).coerceIn(620L, 860L)
        val trajectory = BallTrajectory(
            startX = shooter.position.x.toFloat(),
            startY = shooter.position.y.toFloat(),
            targetX = targetGoalX,
            targetY = targetGoalY.toFloat(),
            arcHeight = (eval.distanceToGoal / 2.8f).toFloat().coerceIn(0.9f, 2.4f),
            curveOffset = if (shooter.trait == PlayerTrait.SNIPER) 0.45f else 0.22f,
            probability = chance,
            isShot = true,
            isGoal = success,
            isSaved = (roll <= chance + 25 && gk != null),
            durationMs = animDuration
        )

        _gameState.update {
            it.copy(
                ballTrajectory = trajectory,
                ballPosition = null,
                playerWithBallId = null,
                selectedPlayerId = null,
                actionMenuOpen = false,
                selectedAction = null
            )
        }

        viewModelScope.launch {
            delay(animDuration + 60L)
            _gameState.update { it.copy(ballTrajectory = null) }

            if (success) {
                if (shooter.team == Team.HOME) homeScore++ else awayScore++
                
                val isArcade = _gameState.value.gameMode == GameMode.ARCADE
                val isArcadeWin = isArcade && (homeScore >= 3 || awayScore >= 3)
                val arcadeWinnerMsg = if (homeScore >= 3) "🏆 ניצחון מוחץ לישראל בארקייד! הראשונה ל-3 שערים! ⚽⚽⚽" else "סיום המשחק בארקייד - איטליה הגיעה ראשונה ל-3 שערים!"
                val maxActions = if (isArcade) 1 else 3

                // Goal scored, reset positions but preserve match clock and cards
                val cardMap = _gameState.value.players.associate { it.id to Pair(it.yellowCards, it.isRedCarded) }
                val resetPlayers = GameState.createInitialPlayers(existingCards = cardMap)
                
                val attackerRoll = (15..35).random()
                val defenderRoll = (10..30).random()
                val distStr = String.format("%.1f", eval.distanceToGoal)
                val duelState = TacticalDuelState(
                    title = "⚽ שער מרהיב!",
                    attackerName = shooter.name,
                    attackerNumber = shooter.number,
                    attackerTeam = shooter.team,
                    attackerRole = shooter.role,
                    attackerStatName = "בעיטה ($distStr משבצות)",
                    attackerStatVal = shooter.currentShooting,
                    attackerRoll = attackerRoll,
                    defenderName = gk?.name ?: "שוער היריב",
                    defenderNumber = gk?.number ?: 1,
                    defenderTeam = if (shooter.team == Team.HOME) Team.AWAY else Team.HOME,
                    defenderRole = PlayerRole.GOALKEEPER,
                    defenderStatName = "עצירה (DEF)",
                    defenderStatVal = gk?.currentDef ?: 50,
                    defenderRoll = defenderRoll,
                    resultTitle = "GOOOAL!",
                    resultSubtitle = "${shooter.name} דייק ממרחק $distStr משבצות! (${eval.breakdownText})",
                    isAttackerWin = true
                )
                
                _gameState.update { 
                    it.copy(
                        homeScore = homeScore,
                        awayScore = awayScore,
                        ballPosition = Position(5, 7),
                        playerWithBallId = null,
                        players = resetPlayers,
                        selectedPlayerId = null,
                        actionMenuOpen = false,
                        selectedAction = null,
                        actionsLeft = maxActions,
                        actedPlayerIds = emptySet(),
                        activeSetPiece = null,
                        gkSaveEffect = null,
                        isMatchOver = if (isArcadeWin) true else it.isMatchOver,
                        tacticalDuel = if (isArcade) null else duelState,
                        currentTurn = if (shooter.team == Team.HOME) Team.AWAY else Team.HOME,
                        uiMessage = if (isArcadeWin) arcadeWinnerMsg else "⚽ שער!!! גול נהדר של ${shooter.name}! (${eval.breakdownText})"
                    )
                }
                if (!isArcadeWin) {
                    triggerAIIfNeeded()
                }
            } else {
                // Missed shot or Saved by GK
                val savedByGk = roll <= chance + 25 && gk != null
                val attackerRoll = (10..28).random()
                val defenderRoll = (15..35).random()
                val distStr = String.format("%.1f", eval.distanceToGoal)
                val duelState = TacticalDuelState(
                    title = if (savedByGk) "🧤 הצלה של השוער!" else "❌ בעיטה החוצה",
                    attackerName = shooter.name,
                    attackerNumber = shooter.number,
                    attackerTeam = shooter.team,
                    attackerRole = shooter.role,
                    attackerStatName = "בעיטה ($distStr משבצות)",
                    attackerStatVal = shooter.currentShooting,
                    attackerRoll = attackerRoll,
                    defenderName = gk?.name ?: "שוער היריב",
                    defenderNumber = gk?.number ?: 1,
                    defenderTeam = if (shooter.team == Team.HOME) Team.AWAY else Team.HOME,
                    defenderRole = PlayerRole.GOALKEEPER,
                    defenderStatName = "עצירה (DEF)",
                    defenderStatVal = gk?.currentDef ?: 50,
                    defenderRoll = defenderRoll,
                    resultTitle = if (savedByGk) "SAVED!" else "MISSED!",
                    resultSubtitle = if (savedByGk) "${gk?.name ?: "השוער"} הדף בעיטה ממרחק $distStr משבצות (סיכוי: $chance%)!" else "הכדור לא מצא את המסגרת ממרחק $distStr משבצות (${eval.breakdownText})",
                    isAttackerWin = false
                )
                _gameState.update { it.copy(tacticalDuel = if (_gameState.value.gameMode == GameMode.ARCADE) null else duelState) }
                
                if (savedByGk && gk != null) {
                    // GK SAVE ANIMATION & DIVE EFFECT
                    val diveX = if (shooter.position.x < 5) 4 else if (shooter.position.x > 5) 6 else 5
                    val divePos = Position(diveX, targetGoalY)
                    val saveEffect = GkSaveEffect(
                        keeperId = gk.id,
                        keeperName = gk.name,
                        diveTargetPos = divePos
                    )
                    _gameState.update { it.copy(gkSaveEffect = saveEffect) }

                    // 60% chance GK tips it over/wide for a CORNER KICK, 40% rebound into box
                    val isCorner = (0..100).random() < 60
                    if (isCorner) {
                        val cornerX = if (shooter.position.x <= 5) 0 else 10
                        setupCornerKick(attackingTeam = shooter.team, cornerPos = Position(cornerX, targetGoalY), gkName = gk.name)
                    } else {
                        // Rebound inside box
                        val reboundY = if (targetGoalY == 0) (1..2).random() else (12..13).random()
                        val reboundX = (3..7).random()
                        val reboundPos = Position(reboundX, reboundY)
                        consumeAction(
                            _gameState.value.players, 
                            reboundPos, 
                            null, 
                            "🧤 איזו הצלה ענקית של ${gk.name}! הריבאונד חופשי ברחבה!", 
                            shooterId, 
                            staminaCost = 10
                        )
                    }
                } else {
                    // Missed shot wide -> 50% Goal Kick (turnover to GK), 50% deflection to corner
                    val isCornerMiss = (0..100).random() < 40
                    if (isCornerMiss) {
                        val cornerX = if (shooter.position.x <= 5) 0 else 10
                        setupCornerKick(attackingTeam = shooter.team, cornerPos = Position(cornerX, targetGoalY), gkName = null)
                    } else {
                        // Ball goes wide to goalkeeper for goal kick
                        val defendingTeam = if (shooter.team == Team.HOME) Team.AWAY else Team.HOME
                        val opposingGk = _gameState.value.players.find { it.team == defendingTeam && it.role == PlayerRole.GOALKEEPER }
                        if (opposingGk != null) {
                            consumeAction(
                                _gameState.value.players, 
                                null, 
                                opposingGk.id, 
                                "${shooter.name} בועט מחוץ למסגרת... כדור שוער!", 
                                shooterId, 
                                staminaCost = 10,
                                forceEndTurn = true
                            )
                        } else {
                            val widePos = Position((2..8).random(), targetGoalY)
                            consumeAction(_gameState.value.players, widePos, null, "${shooter.name} בועט מחוץ למסגרת!", shooterId, staminaCost = 10)
                        }
                    }
                }
            }
        }
    }

    private fun setupCornerKick(attackingTeam: Team, cornerPos: Position, gkName: String?) {
        val state = _gameState.value
        // Find best corner taker (highest passing skill among midfielders or wingers)
        val attackingPlayers = state.players.filter { it.team == attackingTeam && !it.isRedCarded && it.role != PlayerRole.GOALKEEPER }
        val taker = attackingPlayers.maxByOrNull { it.stats.pas } ?: attackingPlayers.firstOrNull() ?: return
        
        // Move taker to corner position
        val updatedPlayers = state.players.map {
            if (it.id == taker.id) it.copy(position = cornerPos) else it
        }

        val cornerNotice = if (gkName != null) {
            "🧤 זינוק אדיר של $gkName שהדף לקרן! 🚩 כדור קרן!"
        } else {
            "🚩 הכדור הוסט מעבר לקו השער - כדור קרן!"
        }

        _gameState.update {
            it.copy(
                players = updatedPlayers,
                ballPosition = cornerPos,
                playerWithBallId = taker.id,
                currentTurn = attackingTeam,
                actionsLeft = 3,
                actedPlayerIds = emptySet(),
                activeSetPiece = SetPieceState(
                    type = SetPieceType.CORNER,
                    takingTeam = attackingTeam,
                    takerPlayerId = taker.id,
                    ballPosition = cornerPos
                ),
                selectedPlayerId = taker.id,
                actionMenuOpen = false,
                selectedAction = null,
                previewActions = emptyList(),
                uiMessage = cornerNotice
            )
        }
        if (attackingTeam == Team.AWAY) {
            triggerAIIfNeeded()
        }
    }

    private fun setupThrowIn(takingTeam: Team, touchlinePos: Position, outPlayerName: String) {
        val state = _gameState.value
        val clampedX = if (touchlinePos.x <= 1) 0 else 10
        val clampedPos = Position(clampedX, touchlinePos.y.coerceIn(1, 13))
        
        // Find closest player of taking team to take the throw-in
        val availablePlayers = state.players.filter { it.team == takingTeam && !it.isRedCarded && it.role != PlayerRole.GOALKEEPER }
        val taker = availablePlayers.minByOrNull { abs(it.position.x - clampedPos.x) + abs(it.position.y - clampedPos.y) } ?: return

        val updatedPlayers = state.players.map {
            if (it.id == taker.id) it.copy(position = clampedPos) else it
        }

        val teamHebrew = if (takingTeam == Team.HOME) "ישראל" else "איטליה"
        val msg = "🤾 הכדור יצא מחוץ למגרש (אחרון נגע: $outPlayerName). כדור חוץ לטובת $teamHebrew!"

        _gameState.update {
            it.copy(
                players = updatedPlayers,
                ballPosition = clampedPos,
                playerWithBallId = taker.id,
                currentTurn = takingTeam,
                actionsLeft = 3,
                actedPlayerIds = emptySet(),
                activeSetPiece = SetPieceState(
                    type = SetPieceType.THROW_IN,
                    takingTeam = takingTeam,
                    takerPlayerId = taker.id,
                    ballPosition = clampedPos
                ),
                selectedPlayerId = taker.id,
                actionMenuOpen = false,
                selectedAction = null,
                previewActions = emptyList(),
                uiMessage = msg
            )
        }
        if (takingTeam == Team.AWAY) {
            triggerAIIfNeeded()
        }
    }

    fun takeSetPiece(takerId: Int, targetPos: Position) {
        val state = _gameState.value
        val setPiece = state.activeSetPiece ?: return
        val taker = state.players.find { it.id == takerId } ?: return

        val animDuration = if (setPiece.type == SetPieceType.CORNER) 580L else 400L
        val trajectory = BallTrajectory(
            startX = taker.position.x.toFloat(),
            startY = taker.position.y.toFloat(),
            targetX = targetPos.x.toFloat(),
            targetY = targetPos.y.toFloat(),
            arcHeight = if (setPiece.type == SetPieceType.CORNER) 2.2f else 0.8f,
            curveOffset = if (setPiece.type == SetPieceType.CORNER) 0.42f else 0.08f,
            probability = if (setPiece.type == SetPieceType.CORNER) (taker.currentPas - 10).coerceIn(40, 90) else 95,
            isShot = false,
            durationMs = animDuration
        )

        // Clear active set piece & launch ball trajectory
        _gameState.update { 
            it.copy(
                activeSetPiece = null,
                ballTrajectory = trajectory,
                ballPosition = null,
                playerWithBallId = null
            ) 
        }

        viewModelScope.launch {
            delay(animDuration)
            _gameState.update { it.copy(ballTrajectory = null) }

            if (setPiece.type == SetPieceType.CORNER) {
                // Corner cross into the box
                val receiver = _gameState.value.players.find { it.position == targetPos }
                val roll = (0..100).random()
                val crossAccuracy = (taker.currentPas - 10).coerceIn(40, 90)

                if (roll <= crossAccuracy && receiver != null) {
                    val duelMsg = if (receiver.team == taker.team) {
                        "🎯 הרמת קרן מדויקת של ${taker.name} היישר לראש של ${receiver.name}!"
                    } else {
                        "🛡️ הרמת קרן נבלמה על ידי ${receiver.name} שהרחיק בנגיחה!"
                    }
                    consumeAction(_gameState.value.players, null, receiver.id, duelMsg, takerId)
                } else {
                    // Ball drops loose in the box
                    consumeAction(_gameState.value.players, targetPos, null, "🚩 כדור קרן מוגבה למרכז הרחבה, כדור חופשי באוויר!", takerId)
                }
            } else {
                // Throw-in delivery
                val receiver = _gameState.value.players.find { it.position == targetPos }
                if (receiver != null) {
                    consumeAction(_gameState.value.players, null, receiver.id, "🤾 זריקת חוץ טובה של ${taker.name} אל ${receiver.name}!", takerId)
                } else {
                    consumeAction(_gameState.value.players, targetPos, null, "🤾 ${taker.name} זורק חוץ לשטח פתוח.", takerId)
                }
            }
        }
    }

    fun clearGkSaveEffect() {
        _gameState.update { it.copy(gkSaveEffect = null) }
    }

    private fun performTackle(tacklerId: Int, targetId: Int) {
        val tackler = _gameState.value.players.find { it.id == tacklerId } ?: return
        val target = _gameState.value.players.find { it.id == targetId } ?: return
        
        val dist = maxOf(abs(tackler.position.x - target.position.x), abs(tackler.position.y - target.position.y))
        val actionsCost = if (dist <= 1) 1 else if (_gameState.value.actionsLeft >= 2) 2 else 1
        val staminaCost = if (dist <= 1) 8 else 14
        val tackleDesc = if (dist <= 1) "תיקול עומד (מהלך 1)" else "תיקול בריצה וגלישה ($actionsCost מהלכים)"

        // In a distance tackle, move tackler adjacent to the target
        var newTacklerPos = tackler.position
        if (dist > 1) {
            val signX = (target.position.x - tackler.position.x).sign
            val signY = (target.position.y - tackler.position.y).sign
            val candidates = listOf(
                Position(target.position.x - signX, target.position.y - signY),
                Position(target.position.x, target.position.y - signY),
                Position(target.position.x - signX, target.position.y),
                Position(target.position.x + signX, target.position.y),
                Position(target.position.x, target.position.y + signY)
            ).filter { it.x in 0..10 && it.y in 0..14 }
            
            val freeTile = candidates.firstOrNull { pos ->
                _gameState.value.players.none { it.id != tackler.id && it.position == pos }
            }
            if (freeTile != null) {
                newTacklerPos = freeTile
            }
        }
        val movedPlayers = _gameState.value.players.map {
            if (it.id == tacklerId) it.copy(position = newTacklerPos) else it
        }

        val chance = calculateTackleProbability(tackler, target)
        var success = (0..100).random() <= chance
        
        if (tackler.trait == PlayerTrait.BULLDOZER) {
            success = true // Bulldozer always succeeds in tackling
        }

        val currentStats = _gameState.value.stats
        var updatedTackleStats = if (tackler.team == Team.HOME) {
            currentStats.copy(homeTackles = currentStats.homeTackles + 1)
        } else {
            currentStats.copy(awayTackles = currentStats.awayTackles + 1)
        }
        
        // For bulldozer, if it "succeeds", it still has a very high foul chance (70%)
        val isBulldozerFoul = tackler.trait == PlayerTrait.BULLDOZER && (0..100).random() < 70
        
        val tacklerRoll = (12..35).random()
        val targetRoll = (10..30).random()

        if (success && !isBulldozerFoul) {
            _gameState.update { it.copy(stats = updatedTackleStats) }
            val cleanTackleMsg = if (tackler.team == Team.HOME) "⚡ תיקול נקי ומצוין של ${tackler.name}! ($tackleDesc). הכדור שלך." else "⚡ ${tackler.name} חטף את הכדור! ($tackleDesc)"
            val duelState = TacticalDuelState(
                title = "🛡️ תיקול מוצלח!",
                attackerName = tackler.name,
                attackerNumber = tackler.number,
                attackerTeam = tackler.team,
                attackerRole = tackler.role,
                attackerStatName = "תיקול (DEF)",
                attackerStatVal = tackler.currentDef,
                attackerRoll = tacklerRoll,
                defenderName = target.name,
                defenderNumber = target.number,
                defenderTeam = target.team,
                defenderRole = target.role,
                defenderStatName = "שליטה (PAC)",
                defenderStatVal = target.currentPac,
                defenderRoll = targetRoll,
                resultTitle = "TACKLE WON!",
                resultSubtitle = "${tackler.name} חילץ את הכדור בנחישות! ($tackleDesc)",
                isAttackerWin = true
            )
            _gameState.update { it.copy(tacticalDuel = if (_gameState.value.gameMode == GameMode.ARCADE) null else duelState) }
            consumeAction(movedPlayers, null, tacklerId, cleanTackleMsg, tacklerId, staminaCost = staminaCost, actionsConsumed = actionsCost)
        } else {
            // Failed tackle OR bulldozer foul: Check for Foul (50% chance for normal, 100% for bulldozer foul)
            val isFoul = isBulldozerFoul || (0..100).random() < 50
            if (isFoul) {
                updatedTackleStats = if (tackler.team == Team.HOME) {
                    updatedTackleStats.copy(homeFouls = updatedTackleStats.homeFouls + 1)
                } else {
                    updatedTackleStats.copy(awayFouls = updatedTackleStats.awayFouls + 1)
                }
                _gameState.update { it.copy(stats = updatedTackleStats) }

                // In penalty area?
                val inPenaltyArea = if (target.team == Team.HOME) {
                    target.position.y <= 2 && target.position.x in 2..8
                } else {
                    target.position.y >= 12 && target.position.x in 2..8
                }

                // Cards roll
                var newYellow = tackler.yellowCards
                var newRed = tackler.isRedCarded
                val cardRoll = (0..100).random()
                var cardMessage = ""

                if (cardRoll < 8) { // 8% Direct Red Card
                    newRed = true
                    cardMessage = " 🟥 כרטיס אדום ישיר למס' ${tackler.number}!"
                } else if (cardRoll < 45) { // 37% Yellow Card
                    newYellow++
                    if (newYellow >= 2) {
                        newRed = true
                        cardMessage = " 🟨🟥 צהוב שני ואדום למס' ${tackler.number}!"
                    } else {
                        cardMessage = " 🟨 כרטיס צהוב למס' ${tackler.number}!"
                    }
                }

                val updatedPlayers = movedPlayers.map {
                    if (it.id == tacklerId) it.copy(yellowCards = newYellow, isRedCarded = newRed) else it
                }

                val foulHeading = if (inPenaltyArea) "⚠️ שריקה לפנדל נגד ${tackler.name}!! 🎯" else "⚠️ עבירה של ${tackler.name}!"
                val foulNotice = "$foulHeading$cardMessage"

                val duelState = TacticalDuelState(
                    title = "⚠️ עבירה של ${tackler.name}!",
                    attackerName = tackler.name,
                    attackerNumber = tackler.number,
                    attackerTeam = tackler.team,
                    attackerRole = tackler.role,
                    attackerStatName = "תיקול (DEF)",
                    attackerStatVal = tackler.currentDef,
                    attackerRoll = tacklerRoll,
                    defenderName = target.name,
                    defenderNumber = target.number,
                    defenderTeam = target.team,
                    defenderRole = target.role,
                    defenderStatName = "שליטה (PAC)",
                    defenderStatVal = target.currentPac,
                    defenderRoll = targetRoll + 15,
                    resultTitle = "FOUL!",
                    resultSubtitle = "עבירה חריפה! השופט עוצר את המשחק.",
                    isAttackerWin = false
                )
                _gameState.update { it.copy(tacticalDuel = if (_gameState.value.gameMode == GameMode.ARCADE) null else duelState) }

                // Turn immediately transfers to the fouled player's team
                consumeAction(
                    updatedPlayers, 
                    null, 
                    target.id, 
                    foulNotice, 
                    tacklerId,
                    forceEndTurn = true,
                    staminaCost = staminaCost,
                    actionsConsumed = actionsCost
                )
            } else {
                _gameState.update { it.copy(stats = updatedTackleStats) }
                val duelState = TacticalDuelState(
                    title = "⚡ כדרור מוצלח של ${target.name}!",
                    attackerName = tackler.name,
                    attackerNumber = tackler.number,
                    attackerTeam = tackler.team,
                    attackerRole = tackler.role,
                    attackerStatName = "תיקול (DEF)",
                    attackerStatVal = tackler.currentDef,
                    attackerRoll = tacklerRoll,
                    defenderName = target.name,
                    defenderNumber = target.number,
                    defenderTeam = target.team,
                    defenderRole = target.role,
                    defenderStatName = "שליטה (PAC)",
                    defenderStatVal = target.currentPac,
                    defenderRoll = targetRoll + 20,
                    resultTitle = "EVADED!",
                    resultSubtitle = "${target.name} חלף על פני התיקול באלגנטיות! ($tackleDesc)",
                    isAttackerWin = false
                )
                _gameState.update { it.copy(tacticalDuel = if (_gameState.value.gameMode == GameMode.ARCADE) null else duelState) }
                consumeAction(movedPlayers, _gameState.value.ballPosition, targetId, "${tackler.name} פספס בתיקול! ($tackleDesc)", tacklerId, staminaCost = staminaCost, actionsConsumed = actionsCost)
            }
        }
    }

    private fun consumeAction(
        updatedPlayers: List<Player>, 
        newBallPos: Position?, 
        newPlayerWithBallId: Int?, 
        actionMessage: String? = null, 
        actorId: Int? = null,
        forceEndTurn: Boolean = false,
        staminaCost: Int = 5,
        actionsConsumed: Int = 1
    ) {
        val state = _gameState.value
        
        val isArcade = state.gameMode == GameMode.ARCADE
        val maxActions = if (isArcade) 1 else 3
        
        val finalPlayers = updatedPlayers.map { player ->
            val newCooldown = if (player.skillCooldown > 0) player.skillCooldown - 1 else 0
            if (player.id == actorId) {
                val actualCost = if (player.trait == PlayerTrait.TIRELESS) 0 else staminaCost
                player.copy(stamina = (player.stamina - actualCost).coerceAtLeast(0), skillCooldown = newCooldown)
            } else {
                player.copy(skillCooldown = newCooldown)
            }
        }
        
        // In Arcade mode, every action ends the turn immediately (1 action per turn ping-pong)
        var newActionsLeft = if (forceEndTurn || isArcade) 0 else (state.actionsLeft - actionsConsumed).coerceAtLeast(0)
        var newTurn = state.currentTurn
        var message = actionMessage
        var newActedPlayerIds = if (actorId != null) state.actedPlayerIds + actorId else state.actedPlayerIds
        
        if (newActionsLeft <= 0) {
            newActionsLeft = maxActions
            newTurn = if (state.currentTurn == Team.HOME) Team.AWAY else Team.HOME
            val turnMsg = if (newTurn == Team.HOME) "התור שלך!" else "תור היריב (חישוב מהלכים...)"
            message = if (message != null) "$message\n$turnMsg" else turnMsg
            newActedPlayerIds = emptySet()
        }

        // Advance match clock (1-2 minutes per action, plus extra if turn finished)
        // Note: In ARCADE mode, clock does not end the match - it ends strictly on 3 goals!
        val minuteAdvance = if (forceEndTurn || newActionsLeft == maxActions) 2 else 1
        var nextMinute = (state.matchMinute + minuteAdvance).coerceAtMost(90)
        var isHalfTime = state.isHalfTime
        var isMatchOver = state.isMatchOver

        if (!isArcade) {
            if (state.half == 1 && nextMinute >= 45) {
                nextMinute = 45
                isHalfTime = true
                message = "⏸️ שריקה לסיום המחצית הראשונה (45')!"
            } else if (state.half == 2 && nextMinute >= 90) {
                nextMinute = 90
                isMatchOver = true
                val winnerText = when {
                    state.homeScore > state.awayScore -> "ניצחון היסטורי לישראל!"
                    state.awayScore > state.homeScore -> "ניצחון לאיטליה!"
                    else -> "שוויון דרמטי!"
                }
                message = "🏁 שריקת סיום המשחק! $winnerText"
            }
        }
        
        // Update Tactical Axis Stats
        val currentAxis = state.stats.axisStats
        val updatedAxis = currentAxis.copy(
            attackProgression = if (newPlayerWithBallId != null && state.players.find { it.id == newPlayerWithBallId }?.team == Team.HOME) 
                (currentAxis.attackProgression + 2).coerceIn(10, 95) else currentAxis.attackProgression,
            tempo = if (isArcade) 90 else 55
        )
        val updatedStatsWithAxis = state.stats.copy(axisStats = updatedAxis)
        
        android.util.Log.d("GameViewModel", "consumeAction updating state -> ActorId: $actorId, Next Turn: $newTurn, Actions Left: $newActionsLeft, ForceEnd: $forceEndTurn, Min: $nextMinute")
        
        _gameState.update { currentState ->
            val newState = currentState.copy(
                players = finalPlayers,
                ballPosition = newBallPos,
                playerWithBallId = newPlayerWithBallId,
                actionsLeft = newActionsLeft,
                currentTurn = newTurn,
                actedPlayerIds = newActedPlayerIds,
                selectedPlayerId = null,
                actionMenuOpen = false,
                selectedAction = null,
                previewActions = emptyList(),
                uiMessage = message,
                matchMinute = nextMinute,
                isHalfTime = isHalfTime,
                isMatchOver = isMatchOver,
                stats = updatedStatsWithAxis
            )
            val actedPlayer = updatedPlayers.find { it.id == actorId }
            android.util.Log.d("GameViewModel", "[STATE EMIT] New Turn: ${newState.currentTurn}, InteractedPlayer: ${actedPlayer?.role} (ID: $actorId), ActionsLeft: ${newState.actionsLeft}")
            newState
        }
        if (!isHalfTime && !isMatchOver) {
            triggerAIIfNeeded()
        }
    }

    fun startSecondHalf() {
        val state = _gameState.value
        val cardMap = state.players.associate { it.id to Pair(it.yellowCards, it.isRedCarded) }
        val resetPlayers = GameState.createInitialPlayers(existingCards = cardMap)
        _gameState.update {
            it.copy(
                players = resetPlayers,
                ballPosition = Position(5, 7),
                playerWithBallId = null,
                half = 2,
                matchMinute = 45,
                isHalfTime = false,
                currentTurn = Team.AWAY, // Away starts the second half
                actionsLeft = 3,
                actedPlayerIds = emptySet(),
                selectedPlayerId = null,
                actionMenuOpen = false,
                selectedAction = null,
                uiMessage = "▶️ שריקת פתיחת המחצית השנייה!"
            )
        }
        triggerAIIfNeeded()
    }

    fun restartMatch() {
        _gameState.value = GameState.initial()
    }

    fun clearMessage() {
        _gameState.update { it.copy(uiMessage = null) }
    }
    
    fun setHomeMentality(mentality: TeamMentality) {
        val state = _gameState.value
        if (state.currentTurn != Team.HOME || state.homeMentality == mentality) return
        
        // Mentality shift costs 1 action point
        if (state.actionsLeft < 1) return

        val shiftedPlayers = state.players.map { player ->
            if (player.team == Team.HOME && player.role != PlayerRole.GOALKEEPER && !player.isRedCarded) {
                // Apply a Y-axis shift based on the new mentality relative to the old one
                val yShift = when (mentality) {
                    TeamMentality.PARK_THE_BUS -> 2 // Move back towards goal (Y=14)
                    TeamMentality.ALL_OUT_ATTACK -> -2 // Move up towards away goal (Y=0)
                    TeamMentality.BALANCED -> if (state.homeMentality == TeamMentality.PARK_THE_BUS) -2 else 2
                }
                
                var newY = (player.position.y + yShift).coerceIn(1, 13) // Don't let them walk out of bounds
                // For all out attack, defenders push past midline (y=7)
                // For park the bus, attackers fall back behind midline
                
                player.copy(position = Position(player.position.x, newY))
            } else {
                player
            }
        }
        
        val nextActions = state.actionsLeft - 1
        var newTurn = state.currentTurn
        var newActed = state.actedPlayerIds
        var msg = "🗣️ שינוי מערך: ${mentality.displayName}!"
        if (nextActions <= 0) {
            newTurn = Team.AWAY
            newActed = emptySet()
            msg += "\nתור היריב (חישוב מהלכים...)"
        }

        _gameState.update { 
            it.copy(
                players = shiftedPlayers,
                homeMentality = mentality,
                actionsLeft = if (nextActions <= 0) 3 else nextActions,
                currentTurn = newTurn,
                actedPlayerIds = newActed,
                uiMessage = msg
            )
        }
        if (newTurn == Team.AWAY) {
            triggerAIIfNeeded()
        }
    }
    
    fun claimBall(playerId: Int) {
         _gameState.update { 
            it.copy(playerWithBallId = playerId)
        }
    }

    fun performSubstitution(playerOutId: Int, playerInId: Int) {
        val state = _gameState.value
        if (state.currentTurn != Team.HOME || state.homeSubsLeft <= 0) return

        val playerOut = state.players.find { it.id == playerOutId } ?: return
        val playerIn = state.homeBench.find { it.id == playerInId } ?: return

        // Create the modified players
        val updatedPlayers = state.players.map {
            if (it.id == playerOutId) {
                // Remove from pitch by moving to bench coords, or simply substitute
                // Actually it's easier to just replace him entirely in the list
                playerIn.copy(position = it.position, id = it.id) // keep the same position and ID for simplicity in state management?
                // No, ID should be unique to keep cards/stamina working right. 
                // Let's replace the object in the list.
            } else {
                it
            }
        }.toMutableList()

        // Replace logic:
        val index = updatedPlayers.indexOfFirst { it.id == playerOutId }
        if (index != -1) {
            val positionOnPitch = playerOut.position
            // Ensure if player out had the ball, player in gets it. (Not realistic, but prevents ball getting stuck)
            val holdsBall = (state.playerWithBallId == playerOutId)
            
            updatedPlayers[index] = playerIn.copy(position = positionOnPitch)
            
            // Remove playerIn from bench and add playerOut to bench
            val updatedBench = state.homeBench.map {
                if (it.id == playerInId) playerOut.copy(position = Position(-1,-1)) else it
            }

            _gameState.update {
                it.copy(
                    players = updatedPlayers,
                    homeBench = updatedBench,
                    homeSubsLeft = it.homeSubsLeft - 1,
                    actedPlayerIds = it.actedPlayerIds - playerOutId,
                    playerWithBallId = if (holdsBall) playerIn.id else it.playerWithBallId,
                    uiMessage = "🔄 חילוף: ${playerIn.name} נכנס במקום ${playerOut.name}"
                )
            }
        }
    }

    fun dismissTacticalDuel() {
        _gameState.update { it.copy(tacticalDuel = null) }
    }

    fun endTurnManually() {
        val state = _gameState.value
        if (state.currentTurn == Team.HOME && !state.isHalfTime && !state.isMatchOver) {
            consumeAction(state.players, state.ballPosition, state.playerWithBallId, "סיום תור יזום.", forceEndTurn = true)
            triggerAIIfNeeded()
        }
    }
}
