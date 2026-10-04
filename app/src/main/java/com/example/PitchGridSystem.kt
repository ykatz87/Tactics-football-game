package com.example

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Grid system configuration and coordinate utilities for the 11x15 pitch.
 * Every coordinate on the pitch represents a discrete, evaluated tactical position
 * for movement, passing, shooting, and positional control.
 */
object PitchGridConfig {
    const val COLUMNS = 11 // X: 0..10 (A..K)
    const val ROWS = 15    // Y: 0..14 (1..15)

    val HOME_GOAL = Position(5, 14)
    val AWAY_GOAL = Position(5, 0)
    val CENTER_SPOT = Position(5, 7)

    fun isValidCoordinate(x: Int, y: Int): Boolean = x in 0 until COLUMNS && y in 0 until ROWS
    fun isValidCoordinate(pos: Position): Boolean = isValidCoordinate(pos.x, pos.y)

    fun getCoordinateLabel(x: Int, y: Int): String {
        val colChar = ('A' + x.coerceIn(0, 10))
        val rowNum = y + 1
        return "$colChar$rowNum"
    }

    fun getZoneName(x: Int, y: Int): String {
        val isWingLeft = x <= 1
        val isWingRight = x >= 9
        val isCenter = x in 4..6

        return when {
            y == 0 && isCenter -> "שער יריב (קו השער)"
            y in 0..3 && x in 2..8 -> "רחבת ה-16 של היריב"
            y in 0..4 -> if (isWingLeft) "אגף שמאל התקפי" else if (isWingRight) "אגף ימין התקפי" else "שליש התקפי (סכנת שער)"
            y in 5..6 -> if (isWingLeft) "אגף שמאל (קישור קדמי)" else if (isWingRight) "אגף ימין (קישור קדמי)" else "קישור קדמי"
            y == 7 -> if (isCenter) "עיגול האמצע" else "קו חצי המגרש"
            y in 8..9 -> if (isWingLeft) "אגף שמאל (קישור אחורי)" else if (isWingRight) "אגף ימין (קישור אחורי)" else "קישור אחורי"
            y in 10..14 && x in 2..8 && y >= 11 -> "רחבת ה-16 שלנו"
            y == 14 && isCenter -> "שער בית (קו השער)"
            y in 10..14 -> if (isWingLeft) "אגף שמאל הגנתי" else if (isWingRight) "אגף ימין הגנתי" else "שליש הגנתי"
            else -> "מגרש"
        }
    }
}

/**
 * Evaluated movement target on a specific coordinate based on player's Speed stat
 */
data class MovementTile(
    val position: Position,
    val steps: Int,
    val isContested: Boolean, // adjacent to an opponent defender
    val isBallPickup: Boolean, // contains loose ball
    val speedCost: Int = steps * 10,
    val speedRequired: Int = when (steps) {
        1 -> 40
        2 -> 65
        else -> 85
    },
    val speedTierName: String = when (steps) {
        1 -> "רגיל"
        2 -> "מהיר"
        else -> "ספרינט"
    },
    val isMaxRange: Boolean = false
)

/**
 * Evaluated passing target on a specific coordinate
 */
data class PassingTile(
    val position: Position,
    val targetPlayer: Player?,
    val isTeammate: Boolean,
    val successChance: Int,
    val isOffside: Boolean,
    val interceptorName: String?,
    val interceptRisk: Int,
    val isThroughBall: Boolean // pass into open grass
)

/**
 * Evaluated shooting attempt towards the opponent's goal.
 * Calculates success probability based on the player's Shooting stat and distance to goal.
 */
data class ShootEvaluation(
    val shootingStat: Int,
    val targetGoal: Position,
    val distanceToGoal: Double,
    val distanceInTiles: Int,
    val distancePenalty: Int,
    val anglePenalty: Int,
    val traitBonus: Int,
    val gkContest: Int,
    val opponentGk: Player?,
    val finalProbability: Int,
    val breakdownText: String
)

/**
 * Full tactical tile state for rendering and decision making
 */
data class PitchTileState(
    val position: Position,
    val label: String,
    val zoneName: String,
    val occupyingPlayer: Player?,
    val hasLooseBall: Boolean,
    val movementInfo: MovementTile?,
    val passingInfo: PassingTile?,
    val isUnderOpponentPressure: Boolean
)

object PitchGridEvaluator {

    /**
     * Calculates shooting success probability based on the player's Shooting stat,
     * Euclidean/Chebyshev distance to the goal, angle to center, traits, and opponent GK defense.
     */
    fun calculateShootEvaluation(state: GameState, shooter: Player): ShootEvaluation {
        val targetGoalPos = if (shooter.team == Team.HOME) PitchGridConfig.AWAY_GOAL else PitchGridConfig.HOME_GOAL
        val dx = abs(shooter.position.x - targetGoalPos.x)
        val dy = abs(shooter.position.y - targetGoalPos.y)
        val euclideanDist = sqrt((dx * dx + dy * dy).toDouble())
        val distInTiles = max(dx, dy)

        val shootingStat = shooter.currentShooting

        // Distance penalty based on distance to the goal
        val distancePenalty = when {
            distInTiles <= 2 -> (euclideanDist * 3.0).toInt()
            distInTiles <= 5 -> (euclideanDist * 4.5).toInt()
            distInTiles <= 8 -> (euclideanDist * 6.0).toInt()
            else -> (euclideanDist * 7.5).toInt()
        }

        // Angle penalty: wider shooting angle reduces probability
        val anglePenalty = dx * 3

        // Trait bonus (SNIPER trait grants +20% for long-range shots outside box)
        val traitBonus = if (shooter.trait == PlayerTrait.SNIPER && distInTiles >= 3) 20 else 0

        // Goalkeeper contest
        val opponentGk = state.players.find { it.team != shooter.team && it.role == PlayerRole.GOALKEEPER && !it.isRedCarded }
        val gkContest = if (opponentGk != null) {
            val gkDistToGoal = abs(opponentGk.position.x - targetGoalPos.x) + abs(opponentGk.position.y - targetGoalPos.y)
            val isGkInPosition = gkDistToGoal <= 2
            val gkDef = opponentGk.currentDefense
            if (isGkInPosition) (gkDef * 0.28).toInt() else (gkDef * 0.12).toInt()
        } else {
            0
        }

        val rawChance = shootingStat - distancePenalty - anglePenalty + traitBonus - gkContest
        val finalProbability = rawChance.coerceIn(5, 95)

        val distFormatted = String.format("%.1f", euclideanDist)
        val breakdown = "בעיטה: $shootingStat | מרחק: $distFormatted משבצות (-$distancePenalty%) | סיכוי: $finalProbability%"

        return ShootEvaluation(
            shootingStat = shootingStat,
            targetGoal = targetGoalPos,
            distanceToGoal = euclideanDist,
            distanceInTiles = distInTiles,
            distancePenalty = distancePenalty,
            anglePenalty = anglePenalty,
            traitBonus = traitBonus,
            gkContest = gkContest,
            opponentGk = opponentGk,
            finalProbability = finalProbability,
            breakdownText = breakdown
        )
    }

    /**
     * Calculates all valid movement coordinates for the given player.
     * Respects player pace, movement range (1-3 tiles), grid boundaries, and pitch obstacles.
     */
    fun computeValidMovementTiles(state: GameState, player: Player): Map<Position, MovementTile> {
        val result = mutableMapOf<Position, MovementTile>()
        val range = player.getMoveRange(state.gameMode)
        val px = player.position.x
        val py = player.position.y

        val activeOpponents = state.players.filter { it.team != player.team && !it.isRedCarded }

        for (dx in -range..range) {
            for (dy in -range..range) {
                if (dx == 0 && dy == 0) continue
                val tx = px + dx
                val ty = py + dy

                if (!PitchGridConfig.isValidCoordinate(tx, ty)) continue

                // Tile must NOT be occupied by another player
                val isOccupied = state.players.any { it.position.x == tx && it.position.y == ty && it.id != player.id }
                if (isOccupied) continue

                // Steps in grid distance (Chebyshev)
                val steps = max(abs(dx), abs(dy))

                // Check if target coordinate is contested by adjacent opponent
                val isContested = activeOpponents.any { opp ->
                    abs(opp.position.x - tx) <= 1 && abs(opp.position.y - ty) <= 1
                }

                val isBallPickup = state.ballPosition != null && state.ballPosition!!.x == tx && state.ballPosition!!.y == ty

                val pos = Position(tx, ty)
                result[pos] = MovementTile(
                    position = pos,
                    steps = steps,
                    isContested = isContested,
                    isBallPickup = isBallPickup,
                    speedCost = steps * 10,
                    isMaxRange = steps == range
                )
            }
        }
        return result
    }

    /**
     * Calculates all valid passing coordinates for the ball holder.
     * Evaluates passing range, distance, teammate targets, open space through-balls,
     * offside positions, and interception hazards.
     */
    fun computeValidPassingTiles(state: GameState, passer: Player): Map<Position, PassingTile> {
        val result = mutableMapOf<Position, PassingTile>()
        val passRange = passer.getPassRange(state.gameMode)
        val px = passer.position.x
        val py = passer.position.y

        val opponents = state.players.filter { it.team != passer.team && !it.isRedCarded && it.position.y >= 0 }
        
        // Calculate offside reference line
        val offsideLineY = if (passer.team == Team.HOME) {
            opponents.sortedBy { it.position.y }.getOrNull(1)?.position?.y ?: 0
        } else {
            opponents.sortedByDescending { it.position.y }.getOrNull(1)?.position?.y ?: 14
        }

        for (tx in 0 until PitchGridConfig.COLUMNS) {
            for (ty in 0 until PitchGridConfig.ROWS) {
                if (tx == px && ty == py) continue

                val dx = (px - tx).toDouble()
                val dy = (py - ty).toDouble()
                val dist = sqrt(dx * dx + dy * dy)

                if (dist > passRange) continue

                val targetPos = Position(tx, ty)
                val targetPlayer = state.players.find { it.position == targetPos }
                
                // An opponent's exact tile cannot be passed to
                if (targetPlayer != null && targetPlayer.team != passer.team) continue

                val isTeammate = targetPlayer != null && targetPlayer.team == passer.team
                val isThroughBall = targetPlayer == null

                // Offside evaluation
                var isOffside = false
                if (isTeammate) {
                    if (passer.team == Team.HOME) {
                        if (ty <= 6 && ty < py && ty < offsideLineY) {
                            isOffside = true
                        }
                    } else {
                        if (ty >= 8 && ty > py && ty > offsideLineY) {
                            isOffside = true
                        }
                    }
                }

                // Interception evaluation along raycast line
                val interceptors = getInterceptorsOnLine(passer.position, targetPos, opponents)
                val highestThreatOpponent = interceptors.maxByOrNull { it.currentDef }
                val interceptRisk = if (highestThreatOpponent != null && passer.trait != PlayerTrait.PLAYMAKER) {
                    (35 + (highestThreatOpponent.currentDef - passer.currentPas) / 2).coerceIn(15, 85)
                } else {
                    0
                }

                // Base success probability
                val manhattanDist = abs(px - tx) + abs(py - ty)
                val baseChance = (passer.currentPas - (manhattanDist * 4)).coerceIn(15, 95)
                val adjustedChance = if (isOffside) 0 else if (interceptRisk > 0) (baseChance * (100 - interceptRisk) / 100).coerceIn(10, 90) else baseChance

                result[targetPos] = PassingTile(
                    position = targetPos,
                    targetPlayer = targetPlayer,
                    isTeammate = isTeammate,
                    successChance = adjustedChance,
                    isOffside = isOffside,
                    interceptorName = highestThreatOpponent?.name,
                    interceptRisk = interceptRisk,
                    isThroughBall = isThroughBall
                )
            }
        }
        return result
    }

    /**
     * Checks if any opponents obstruct the pass corridor between start and target
     */
    fun getInterceptorsOnLine(start: Position, target: Position, opponents: List<Player>): List<Player> {
        val x1 = start.x.toDouble()
        val y1 = start.y.toDouble()
        val x2 = target.x.toDouble()
        val y2 = target.y.toDouble()

        val dx = x2 - x1
        val dy = y2 - y1
        val lenSq = dx * dx + dy * dy
        if (lenSq < 1.0) return emptyList()

        return opponents.filter { opp ->
            val px = opp.position.x.toDouble()
            val py = opp.position.y.toDouble()
            val t = ((px - x1) * dx + (py - y1) * dy) / lenSq
            if (t in 0.12..0.88) {
                val projX = x1 + t * dx
                val projY = y1 + t * dy
                val distSq = (px - projX) * (px - projX) + (py - projY) * (py - projY)
                distSq <= 0.48 // Within corridor
            } else {
                false
            }
        }
    }

    /**
     * Evaluates a full grid of tiles with all current tactical context
     */
    fun evaluateGrid(
        state: GameState,
        selectedPlayerId: Int?,
        selectedAction: ActionType?
    ): Map<Position, PitchTileState> {
        val selectedPlayer = state.players.find { it.id == selectedPlayerId }
        val hasBall = selectedPlayer != null && selectedPlayer.id == state.playerWithBallId

        val movementMap = if (selectedPlayer != null && (selectedAction == ActionType.MOVE || selectedAction == ActionType.DRIBBLE || (selectedAction == null && selectedPlayer.team == state.currentTurn))) {
            computeValidMovementTiles(state, selectedPlayer)
        } else {
            emptyMap()
        }

        val passingMap = if (selectedPlayer != null && hasBall && (selectedAction == ActionType.PASS || (selectedAction == null && selectedPlayer.team == state.currentTurn))) {
            computeValidPassingTiles(state, selectedPlayer)
        } else {
            emptyMap()
        }

        val opponentDefenders = state.players.filter { 
            val team = selectedPlayer?.team ?: state.currentTurn
            it.team != team && !it.isRedCarded 
        }

        val grid = mutableMapOf<Position, PitchTileState>()

        for (x in 0 until PitchGridConfig.COLUMNS) {
            for (y in 0 until PitchGridConfig.ROWS) {
                val pos = Position(x, y)
                val occupying = state.players.find { it.position.x == x && it.position.y == y }
                val hasLooseBall = state.ballPosition != null && state.ballPosition!!.x == x && state.ballPosition!!.y == y

                val isUnderPressure = opponentDefenders.any { opp ->
                    abs(opp.position.x - x) <= 1 && abs(opp.position.y - y) <= 1
                }

                grid[pos] = PitchTileState(
                    position = pos,
                    label = PitchGridConfig.getCoordinateLabel(x, y),
                    zoneName = PitchGridConfig.getZoneName(x, y),
                    occupyingPlayer = occupying,
                    hasLooseBall = hasLooseBall,
                    movementInfo = movementMap[pos],
                    passingInfo = passingMap[pos],
                    isUnderOpponentPressure = isUnderPressure
                )
            }
        }

        return grid
    }
}
