package com.example

data class Position(val x: Int, val y: Int)

enum class Team { HOME, AWAY }

enum class PlayerRole { DEFENDER, MIDFIELDER, ATTACKER, GOALKEEPER }

data class PlayerStats(
    val pac: Int, // Pace / Speed
    val def: Int, // Defense
    val pas: Int, // Passing
    val sht: Int  // Shooting
) {
    val speed: Int get() = pac
    val passing: Int get() = pas
    val shooting: Int get() = sht
    val defense: Int get() = def
}

enum class PlayerTrait(val icon: String, val description: String) {
    SNIPER("🎯", "צלף - תוספת 20% לסיכויי הבקעה מחוץ לרחבה"),
    PLAYMAKER("🪄", "פליימייקר - המסירות שלו חסינות לחטיפה"),
    BULLDOZER("🚜", "בולדוזר - מנצח כל תיקול, אך סיכוי גבוה יותר לעבירה"),
    TIRELESS("🔋", "בלתי נגמר - לא מאבד כוח כשהוא רץ")
}

/**
 * Football player data class featuring individual stats (Speed, Passing, Shooting, Defense),
 * match coordinates, and distinct tactical visual icons for representation on the grid pitch.
 */
data class FootballPlayer(
    val id: Int,
    val name: String,
    val number: Int,
    val team: Team,
    val role: PlayerRole,
    val speed: Int,
    val passing: Int,
    val shooting: Int,
    val defense: Int = 50,
    var position: Position = Position(0, 0),
    var stamina: Int = 100,
    val trait: PlayerTrait? = null,
    var skillCooldown: Int = 0,
    var yellowCards: Int = 0,
    var isRedCarded: Boolean = false
) {
    /**
     * Distinct icon representing player role on the pitch grid
     */
    val roleIcon: String get() = when (role) {
        PlayerRole.GOALKEEPER -> "🧤" // Goalkeeper Gloves
        PlayerRole.DEFENDER -> "🛡️"   // Defense Shield
        PlayerRole.MIDFIELDER -> "⚙️"  // Midfield Maestro / Engine
        PlayerRole.ATTACKER -> "⚡"    // Striker / Attack Lightning
    }

    val tacticalBadge: String get() = when {
        trait != null -> trait.icon
        role == PlayerRole.GOALKEEPER -> "🧤"
        role == PlayerRole.DEFENDER -> "🛡️"
        role == PlayerRole.MIDFIELDER -> "⚙️"
        role == PlayerRole.ATTACKER -> "⚡"
        else -> "⚽"
    }

    val moveRange: Int
        get() = when {
            speed >= 85 -> 3
            speed >= 65 -> 2
            else -> 1
        }

    fun toPlayer(): Player = Player(
        id = id,
        team = team,
        position = position,
        number = number,
        role = role,
        stats = PlayerStats(speed, defense, passing, shooting),
        name = name,
        yellowCards = yellowCards,
        isRedCarded = isRedCarded,
        stamina = stamina,
        trait = trait,
        skillCooldown = skillCooldown
    )
}

data class Player(
    val id: Int,
    val team: Team,
    var position: Position,
    val number: Int,
    val role: PlayerRole,
    val stats: PlayerStats,
    val name: String,
    var yellowCards: Int = 0,
    var isRedCarded: Boolean = false,
    var stamina: Int = 100,
    val trait: PlayerTrait? = null,
    var skillCooldown: Int = 0
) {
    val speed: Int get() = stats.pac
    val passing: Int get() = stats.pas
    val shooting: Int get() = stats.sht
    val defense: Int get() = stats.def

    /**
     * Distinct icon representing player on the grid
     */
    val roleIcon: String get() = when (role) {
        PlayerRole.GOALKEEPER -> "🧤" // Goalkeeper Gloves
        PlayerRole.DEFENDER -> "🛡️"   // Defense Shield
        PlayerRole.MIDFIELDER -> "⚙️"  // Midfield Engine
        PlayerRole.ATTACKER -> "⚡"    // Attack Lightning
    }

    val tacticalBadge: String get() = when {
        trait != null -> trait.icon
        role == PlayerRole.GOALKEEPER -> "🧤"
        role == PlayerRole.DEFENDER -> "🛡️"
        role == PlayerRole.MIDFIELDER -> "⚙️"
        role == PlayerRole.ATTACKER -> "⚡"
        else -> "⚽"
    }

    val currentPac: Int get() = if (stamina < 30) (stats.pac * 0.7).toInt() else stats.pac
    val currentPas: Int get() = if (stamina < 30) (stats.pas * 0.7).toInt() else stats.pas
    val currentSht: Int get() = if (stamina < 30) (stats.sht * 0.7).toInt() else stats.sht
    val currentDef: Int get() = if (stamina < 30) (stats.def * 0.7).toInt() else stats.def

    val currentSpeed: Int get() = currentPac
    val currentPassing: Int get() = currentPas
    val currentShooting: Int get() = currentSht
    val currentDefense: Int get() = currentDef

    val moveRange: Int
        get() = when {
            currentSpeed >= 85 -> 3 // Sprint burst (up to 3 tiles)
            currentSpeed >= 65 -> 2 // Dynamic pace (2 tiles)
            else -> 1               // Standard pace (1 tile)
        }

    fun getMoveRange(gameMode: GameMode): Int = when (gameMode) {
        GameMode.ARCADE -> when {
            currentSpeed >= 70 -> 3
            else -> 2 // In arcade, minimum pace is 2 tiles for ultra-fast action
        }
        else -> moveRange
    }
    
    val passRange: Int
        get() = if (currentPas >= 80) 7 else if (currentPas >= 60) 5 else 3

    fun getPassRange(gameMode: GameMode): Int = if (gameMode == GameMode.ARCADE) passRange + 1 else passRange

    fun toFootballPlayer(): FootballPlayer = FootballPlayer(
        id = id,
        name = name,
        number = number,
        team = team,
        role = role,
        speed = speed,
        passing = passing,
        shooting = shooting,
        defense = defense,
        position = position,
        stamina = stamina,
        trait = trait,
        skillCooldown = skillCooldown,
        yellowCards = yellowCards,
        isRedCarded = isRedCarded
    )
}

enum class ActionType {
    MOVE, PASS, SHOOT, DRIBBLE, TACKLE
}

data class ActionOption(
    val actionType: ActionType,
    val targetPosition: Position?,
    val successProbability: String
)

data class AxisStats(
    val attackProgression: Int = 50, // ציר התקדמות התקפית (מעבר לחצי יריב)
    val pressingIntensity: Int = 50, // ציר עוצמת לחץ (חטיפות בשטח יריב)
    val tempo: Int = 50              // ציר קצב משחק (מהירות פעולה)
)

data class GameStats(
    val homeShots: Int = 0,
    val awayShots: Int = 0,
    val homeShotsOnTarget: Int = 0,
    val awayShotsOnTarget: Int = 0,
    val homePasses: Int = 0,
    val awayPasses: Int = 0,
    val homeTackles: Int = 0,
    val awayTackles: Int = 0,
    val homeFouls: Int = 0,
    val awayFouls: Int = 0,
    val axisStats: AxisStats = AxisStats()
)

enum class AIDifficulty { AMATEUR, PRO, LEGEND }

enum class Formation(val displayName: String) {
    F442("4-4-2 מאוזן"),
    F433("4-3-3 התקפי"),
    F532("5-3-2 הגנתי")
}

enum class TeamMentality(val displayName: String) {
    PARK_THE_BUS("החנה אוטובוס"),
    BALANCED("מאוזן"),
    ALL_OUT_ATTACK("התקפה בכל הכוח")
}

enum class SetPieceType {
    CORNER, THROW_IN
}

data class SetPieceState(
    val type: SetPieceType,
    val takingTeam: Team,
    val takerPlayerId: Int,
    val ballPosition: Position
)

data class GkSaveEffect(
    val keeperId: Int,
    val keeperName: String,
    val diveTargetPos: Position,
    val timestamp: Long = System.currentTimeMillis()
)

data class BallTrajectory(
    val startX: Float,
    val startY: Float,
    val targetX: Float,
    val targetY: Float,
    val arcHeight: Float = 1.0f,
    val curveOffset: Float = 0f,
    val probability: Int = 100,
    val isShot: Boolean = false,
    val isGoal: Boolean = false,
    val isSaved: Boolean = false,
    val isIntercepted: Boolean = false,
    val durationMs: Long = 520L,
    val timestamp: Long = System.currentTimeMillis()
)

data class TacticalDuelState(
    val title: String,
    val attackerName: String,
    val attackerNumber: Int,
    val attackerTeam: Team,
    val attackerRole: PlayerRole,
    val attackerStatName: String,
    val attackerStatVal: Int,
    val attackerRoll: Int,
    val defenderName: String,
    val defenderNumber: Int,
    val defenderTeam: Team,
    val defenderRole: PlayerRole,
    val defenderStatName: String,
    val defenderStatVal: Int,
    val defenderRoll: Int,
    val resultTitle: String,
    val resultSubtitle: String,
    val isAttackerWin: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

enum class GameMode(val title: String, val subtitle: String, val isLocked: Boolean = false) {
    ARCADE("ארקייד מהיר", "משחק מהיר וזורם בטאפ אחד, ללא חיכוך", false),
    TACTICAL("טקטיקה מתקדמת", "עומק מלא: מערכים, דואלים, כושר ומנטליות", false),
    CAREER("מצב קריירה", "ניהול מועדון, העברות ושדרוג סגל לאורך עונות", true),
    TOURNAMENT("טורנירים וגביעים", "גביע המדינה, ליגת האלופות ומפעלים בינלאומיים", true)
}

data class GameState(
    val players: List<Player>,
    var ballPosition: Position?, // If null, a player has the ball
    var playerWithBallId: Int?,
    var currentTurn: Team,
    var actionsLeft: Int,
    var homeScore: Int,
    var awayScore: Int,
    var selectedPlayerId: Int?,
    var actionMenuOpen: Boolean,
    var selectedAction: ActionType?,
    var targetPosition: Position?,
    var previewActions: List<ActionOption>,
    val uiMessage: String? = null,
    val matchMinute: Int = 0,
    val half: Int = 1,
    val isHalfTime: Boolean = false,
    val isMatchOver: Boolean = false,
    val isPregame: Boolean = true,
    val gameMode: GameMode = GameMode.ARCADE,
    val isInMainMenu: Boolean = false,
    val isFirstTimeUser: Boolean = false,
    val stats: GameStats = GameStats(),
    val aiDifficulty: AIDifficulty = AIDifficulty.PRO,
    val aiThought: String? = null,
    val homeBench: List<Player> = emptyList(),
    val awayBench: List<Player> = emptyList(),
    val homeSubsLeft: Int = 3,
    val awaySubsLeft: Int = 3,
    val homeMentality: TeamMentality = TeamMentality.BALANCED,
    val actedPlayerIds: Set<Int> = emptySet(),
    val activeSetPiece: SetPieceState? = null,
    val gkSaveEffect: GkSaveEffect? = null,
    val tacticalDuel: TacticalDuelState? = null,
    val ballTrajectory: BallTrajectory? = null
) {
    companion object {
        fun createInitialPlayers(formation: Formation = Formation.F442, existingCards: Map<Int, Pair<Int, Boolean>> = emptyMap()): List<Player> {
            val players = mutableListOf<Player>()
            fun cardsFor(id: Int) = existingCards[id] ?: Pair(0, false)

            // Setup home team based on formation
            players.add(Player(1, Team.HOME, Position(5, 14), 1, PlayerRole.GOALKEEPER, PlayerStats(40, 80, 50, 20), "גלזר", cardsFor(1).first, cardsFor(1).second))
            
            when (formation) {
                Formation.F442 -> {
                    players.add(Player(2, Team.HOME, Position(2, 12), 2, PlayerRole.DEFENDER, PlayerStats(75, 85, 60, 30), "דסה", cardsFor(2).first, cardsFor(2).second))
                    players.add(Player(3, Team.HOME, Position(4, 12), 3, PlayerRole.DEFENDER, PlayerStats(60, 90, 65, 40), "ויטור", cardsFor(3).first, cardsFor(3).second, 100, PlayerTrait.BULLDOZER))
                    players.add(Player(4, Team.HOME, Position(6, 12), 4, PlayerRole.DEFENDER, PlayerStats(60, 90, 65, 40), "גולדברג", cardsFor(4).first, cardsFor(4).second))
                    players.add(Player(5, Team.HOME, Position(8, 12), 5, PlayerRole.DEFENDER, PlayerStats(75, 85, 60, 30), "רביבו", cardsFor(5).first, cardsFor(5).second))
                    players.add(Player(6, Team.HOME, Position(2, 9), 6, PlayerRole.MIDFIELDER, PlayerStats(70, 70, 80, 65), "נטע לביא", cardsFor(6).first, cardsFor(6).second))
                    players.add(Player(7, Team.HOME, Position(4, 9), 7, PlayerRole.MIDFIELDER, PlayerStats(65, 75, 88, 70), "פרץ", cardsFor(7).first, cardsFor(7).second))
                    players.add(Player(8, Team.HOME, Position(6, 9), 8, PlayerRole.MIDFIELDER, PlayerStats(65, 75, 88, 70), "גלוך", cardsFor(8).first, cardsFor(8).second, 100, PlayerTrait.PLAYMAKER))
                    players.add(Player(9, Team.HOME, Position(8, 9), 9, PlayerRole.MIDFIELDER, PlayerStats(70, 70, 80, 65), "סולומון", cardsFor(9).first, cardsFor(9).second))
                    players.add(Player(10, Team.HOME, Position(3, 7), 10, PlayerRole.ATTACKER, PlayerStats(85, 30, 75, 88), "זהבי", cardsFor(10).first, cardsFor(10).second, 100, PlayerTrait.SNIPER))
                    players.add(Player(11, Team.HOME, Position(7, 7), 11, PlayerRole.ATTACKER, PlayerStats(88, 25, 70, 90), "תורג'מן", cardsFor(11).first, cardsFor(11).second))
                }
                Formation.F433 -> {
                    players.add(Player(2, Team.HOME, Position(2, 12), 2, PlayerRole.DEFENDER, PlayerStats(75, 85, 60, 30), "דסה", cardsFor(2).first, cardsFor(2).second))
                    players.add(Player(3, Team.HOME, Position(4, 12), 3, PlayerRole.DEFENDER, PlayerStats(60, 90, 65, 40), "ויטור", cardsFor(3).first, cardsFor(3).second, 100, PlayerTrait.BULLDOZER))
                    players.add(Player(4, Team.HOME, Position(6, 12), 4, PlayerRole.DEFENDER, PlayerStats(60, 90, 65, 40), "גולדברג", cardsFor(4).first, cardsFor(4).second))
                    players.add(Player(5, Team.HOME, Position(8, 12), 5, PlayerRole.DEFENDER, PlayerStats(75, 85, 60, 30), "רביבו", cardsFor(5).first, cardsFor(5).second))
                    players.add(Player(6, Team.HOME, Position(3, 10), 6, PlayerRole.MIDFIELDER, PlayerStats(70, 70, 80, 65), "נטע לביא", cardsFor(6).first, cardsFor(6).second))
                    players.add(Player(7, Team.HOME, Position(5, 10), 7, PlayerRole.MIDFIELDER, PlayerStats(65, 75, 88, 70), "פרץ", cardsFor(7).first, cardsFor(7).second))
                    players.add(Player(8, Team.HOME, Position(7, 10), 8, PlayerRole.MIDFIELDER, PlayerStats(65, 75, 88, 70), "גלוך", cardsFor(8).first, cardsFor(8).second, 100, PlayerTrait.PLAYMAKER))
                    players.add(Player(9, Team.HOME, Position(2, 7), 9, PlayerRole.ATTACKER, PlayerStats(88, 25, 70, 90), "סולומון", cardsFor(9).first, cardsFor(9).second))
                    players.add(Player(10, Team.HOME, Position(5, 7), 10, PlayerRole.ATTACKER, PlayerStats(85, 30, 75, 88), "זהבי", cardsFor(10).first, cardsFor(10).second, 100, PlayerTrait.SNIPER))
                    players.add(Player(11, Team.HOME, Position(8, 7), 11, PlayerRole.ATTACKER, PlayerStats(88, 25, 70, 90), "חלאילי", cardsFor(11).first, cardsFor(11).second))
                }
                Formation.F532 -> {
                    players.add(Player(2, Team.HOME, Position(1, 12), 2, PlayerRole.DEFENDER, PlayerStats(75, 85, 60, 30), "דסה", cardsFor(2).first, cardsFor(2).second))
                    players.add(Player(3, Team.HOME, Position(3, 12), 3, PlayerRole.DEFENDER, PlayerStats(60, 90, 65, 40), "נחמיאס", cardsFor(3).first, cardsFor(3).second))
                    players.add(Player(4, Team.HOME, Position(5, 12), 4, PlayerRole.DEFENDER, PlayerStats(60, 90, 65, 40), "ויטור", cardsFor(4).first, cardsFor(4).second))
                    players.add(Player(5, Team.HOME, Position(7, 12), 5, PlayerRole.DEFENDER, PlayerStats(60, 90, 65, 40), "גולדברג", cardsFor(5).first, cardsFor(5).second))
                    players.add(Player(6, Team.HOME, Position(9, 12), 6, PlayerRole.DEFENDER, PlayerStats(75, 85, 60, 30), "רביבו", cardsFor(6).first, cardsFor(6).second))
                    players.add(Player(7, Team.HOME, Position(3, 9), 7, PlayerRole.MIDFIELDER, PlayerStats(70, 70, 80, 65), "נטע לביא", cardsFor(7).first, cardsFor(7).second))
                    players.add(Player(8, Team.HOME, Position(5, 9), 8, PlayerRole.MIDFIELDER, PlayerStats(65, 75, 88, 70), "פרץ", cardsFor(8).first, cardsFor(8).second))
                    players.add(Player(9, Team.HOME, Position(7, 9), 9, PlayerRole.MIDFIELDER, PlayerStats(65, 75, 88, 70), "גלוך", cardsFor(9).first, cardsFor(9).second))
                    players.add(Player(10, Team.HOME, Position(4, 7), 10, PlayerRole.ATTACKER, PlayerStats(85, 30, 75, 88), "זהבי", cardsFor(10).first, cardsFor(10).second, 100, PlayerTrait.SNIPER))
                    players.add(Player(11, Team.HOME, Position(6, 7), 11, PlayerRole.ATTACKER, PlayerStats(88, 25, 70, 90), "תורג'מן", cardsFor(11).first, cardsFor(11).second))
                }
            }

            // Setup away team (e.g. 4-4-2)
            players.add(Player(12, Team.AWAY, Position(5, 0), 1, PlayerRole.GOALKEEPER, PlayerStats(40, 80, 50, 20), "Donnarumma", cardsFor(12).first, cardsFor(12).second))
            players.add(Player(13, Team.AWAY, Position(2, 2), 2, PlayerRole.DEFENDER, PlayerStats(75, 85, 60, 30), "Di Lorenzo", cardsFor(13).first, cardsFor(13).second))
            players.add(Player(14, Team.AWAY, Position(4, 2), 3, PlayerRole.DEFENDER, PlayerStats(60, 90, 65, 40), "Bastoni", cardsFor(14).first, cardsFor(14).second))
            players.add(Player(15, Team.AWAY, Position(6, 2), 4, PlayerRole.DEFENDER, PlayerStats(60, 90, 65, 40), "Calafiori", cardsFor(15).first, cardsFor(15).second))
            players.add(Player(16, Team.AWAY, Position(8, 2), 5, PlayerRole.DEFENDER, PlayerStats(75, 85, 60, 30), "Dimarco", cardsFor(16).first, cardsFor(16).second))
            players.add(Player(17, Team.AWAY, Position(2, 4), 6, PlayerRole.MIDFIELDER, PlayerStats(70, 70, 80, 65), "Barella", cardsFor(17).first, cardsFor(17).second, 100, PlayerTrait.TIRELESS))
            players.add(Player(18, Team.AWAY, Position(4, 4), 7, PlayerRole.MIDFIELDER, PlayerStats(65, 75, 88, 70), "Jorginho", cardsFor(18).first, cardsFor(18).second))
            players.add(Player(19, Team.AWAY, Position(6, 4), 8, PlayerRole.MIDFIELDER, PlayerStats(65, 75, 88, 70), "Frattesi", cardsFor(19).first, cardsFor(19).second))
            players.add(Player(20, Team.AWAY, Position(8, 4), 9, PlayerRole.MIDFIELDER, PlayerStats(70, 70, 80, 65), "Chiesa", cardsFor(20).first, cardsFor(20).second))
            players.add(Player(21, Team.AWAY, Position(4, 5), 10, PlayerRole.ATTACKER, PlayerStats(85, 30, 75, 88), "Scamacca", cardsFor(21).first, cardsFor(21).second)) 
            players.add(Player(22, Team.AWAY, Position(6, 5), 11, PlayerRole.ATTACKER, PlayerStats(88, 25, 70, 90), "Retegui", cardsFor(22).first, cardsFor(22).second))
            
            return players
        }

        fun createHomeBench(): List<Player> {
            return listOf(
                Player(23, Team.HOME, Position(-1, -1), 12, PlayerRole.DEFENDER, PlayerStats(70, 80, 60, 30), "פיבן"),
                Player(24, Team.HOME, Position(-1, -1), 13, PlayerRole.MIDFIELDER, PlayerStats(65, 75, 80, 70), "קניקובסקי"),
                Player(25, Team.HOME, Position(-1, -1), 14, PlayerRole.ATTACKER, PlayerStats(85, 30, 70, 80), "דוידה"),
                Player(26, Team.HOME, Position(-1, -1), 15, PlayerRole.MIDFIELDER, PlayerStats(70, 70, 75, 70), "שחר"),
                Player(27, Team.HOME, Position(-1, -1), 16, PlayerRole.GOALKEEPER, PlayerStats(40, 75, 50, 20), "משפתי")
            )
        }

        fun createAwayBench(): List<Player> {
            return listOf(
                Player(28, Team.AWAY, Position(-1, -1), 12, PlayerRole.DEFENDER, PlayerStats(70, 80, 60, 30), "Darmian"),
                Player(29, Team.AWAY, Position(-1, -1), 13, PlayerRole.MIDFIELDER, PlayerStats(65, 75, 80, 70), "Pellegrini"),
                Player(30, Team.AWAY, Position(-1, -1), 14, PlayerRole.ATTACKER, PlayerStats(85, 30, 70, 80), "Raspadori"),
                Player(31, Team.AWAY, Position(-1, -1), 15, PlayerRole.MIDFIELDER, PlayerStats(70, 70, 75, 70), "Cristante"),
                Player(32, Team.AWAY, Position(-1, -1), 16, PlayerRole.GOALKEEPER, PlayerStats(40, 75, 50, 20), "Vicario")
            )
        }

        fun initial(): GameState {
            val players = createInitialPlayers()
            return GameState(
                players = players,
                ballPosition = Position(5, 7),
                playerWithBallId = null,
                currentTurn = Team.HOME,
                actionsLeft = 3,
                homeScore = 0,
                awayScore = 0,
                selectedPlayerId = null,
                actionMenuOpen = false,
                selectedAction = null,
                targetPosition = null,
                previewActions = emptyList(),
                matchMinute = 0,
                half = 1,
                isHalfTime = false,
                isMatchOver = false,
                stats = GameStats(),
                aiDifficulty = AIDifficulty.PRO,
                aiThought = null,
                homeBench = createHomeBench(),
                awayBench = createAwayBench(),
                homeSubsLeft = 3,
                awaySubsLeft = 3,
homeMentality = TeamMentality.BALANCED
            )
        }
    }
}
