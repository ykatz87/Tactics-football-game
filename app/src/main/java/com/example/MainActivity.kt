package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.TacticsDialog
import androidx.compose.material.icons.filled.SwapHoriz
import com.example.ui.SubstitutionDialog
import com.example.ui.PlayerVisualRegistry
import com.example.ui.drawLivingPlayerFigurine
import androidx.compose.animation.core.LinearEasing
import com.example.PitchGridConfig
import com.example.PitchGridEvaluator
import com.example.MovementTile
import com.example.PassingTile
import com.example.GameMode
import com.example.ui.MainMenuScreen

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.zIndex
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import kotlinx.coroutines.delay
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F172A) // Dark slate background
                ) {
                    GameScreen()
                }
            }
        }
    }
}

@Composable
fun GameScreen(viewModel: GameViewModel = viewModel()) {
    val gameState by viewModel.gameState.collectAsState()
    
    when {
        gameState.isInMainMenu -> {
            MainMenuScreen(
                onSelectArcade = { viewModel.startArcadeMode() },
                onSelectTactical = { viewModel.startTacticalMode() },
                onResetFirstTime = { viewModel.resetFirstTimeExperience() }
            )
        }
        gameState.isPregame -> {
            PregameScreen(
                onStartGame = { formation -> viewModel.startGame(formation) },
                onBackToMenu = { viewModel.returnToMainMenu() }
            )
        }
        else -> {
            MatchScreen(viewModel, gameState)
        }
    }
}

@Composable
fun MatchScreen(viewModel: GameViewModel, gameState: GameState) {
    val haptic = LocalHapticFeedback.current

    var showSubDialog by remember { mutableStateOf(false) }
    var showTacticsDialog by remember { mutableStateOf(false) }
    var playerProfileToShow by remember { mutableStateOf<Player?>(null) }

    LaunchedEffect(gameState.uiMessage) {
        gameState.uiMessage?.let { msg ->
            if (msg.contains("מתחיל")) {
                SoundManager.playWhistle()
            }
            if (msg.contains("שער") || msg.contains("גול")) {
                // Goal vibration pattern
                SoundManager.playGoal()
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                delay(150)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                delay(150)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            } else if (msg.contains("איזה זינוק") || msg.contains("הצלה") || msg.contains("הדף")) {
                SoundManager.playSave()
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            } else if (msg.contains("קרן") || msg.contains("כדור קרן")) {
                SoundManager.playCorner()
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            } else if (msg.contains("כדור חוץ") || msg.contains("זריקת חוץ") || msg.contains("חוץ")) {
                SoundManager.playWhistle()
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            } else if (msg.contains("תיקול") || msg.contains("חטיפה")) {
                SoundManager.playKick()
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            } else if (msg.contains("עבירה") || msg.contains("פנדל") || msg.contains("סיום")) {
                SoundManager.playWhistle()
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            } else if (msg.contains("מסירה") || msg.contains("דריבל") || msg.contains("בועט")) {
                SoundManager.playKick()
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            } else if (msg.contains("תור")) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }
    }

    LaunchedEffect(gameState.gkSaveEffect) {
        if (gameState.gkSaveEffect != null) {
            delay(1600)
            viewModel.clearGkSaveEffect()
        }
    }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        // Top Bar: Clean Match Scoreboard & Action Points
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Turn & 3 Action Points
            Column(horizontalAlignment = Alignment.Start) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (gameState.currentTurn == Team.HOME) Color(0xFF22C55E) else Color(0xFFF59E0B))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (gameState.currentTurn == Team.HOME) "תורך!" else "תור היריב",
                        color = if (gameState.currentTurn == Team.HOME) Color(0xFF38BDF8) else Color(0xFFF59E0B),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                // Action Tokens (1 for Arcade, 3 for Tactical)
                val maxActions = if (gameState.gameMode == GameMode.ARCADE) 1 else 3
                Row(verticalAlignment = Alignment.CenterVertically) {
                    (1..maxActions).forEach { i ->
                        val isAvailable = i <= gameState.actionsLeft
                        Surface(
                            shape = CircleShape,
                            color = if (isAvailable) Color(0xFFF59E0B) else Color(0xFF1E293B),
                            border = BorderStroke(
                                1.dp,
                                if (isAvailable) Color(0xFFFDE047) else Color(0xFF475569)
                            ),
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(20.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "⚡",
                                    fontSize = 10.sp,
                                    color = if (isAvailable) Color.Black else Color(0xFF64748B)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (gameState.gameMode == GameMode.ARCADE) "פעולה 1" else "${gameState.actionsLeft}/3",
                        color = if (gameState.gameMode == GameMode.ARCADE) Color(0xFF10B981) else Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            
            // Clean Scoreboard
            Surface(
                color = Color(0xFF0F172A),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                shadowElevation = 4.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Surface(
                        color = Color(0xFF1D4ED8),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "ישראל",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${gameState.homeScore} - ${gameState.awayScore}",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "איטליה",
                            color = Color(0xFF1E3A8A),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Right side: Match Clock, End Turn & Menu Button
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Home / Main Menu Button
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF64748B)),
                    modifier = Modifier.clickable { viewModel.returnToMainMenu() }
                ) {
                    Box(modifier = Modifier.padding(6.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Dashboard,
                            contentDescription = "תפריט ראשי",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                    modifier = Modifier.clickable { 
                        val newDifficulty = when(gameState.aiDifficulty) {
                            AIDifficulty.AMATEUR -> AIDifficulty.PRO
                            AIDifficulty.PRO -> AIDifficulty.LEGEND
                            AIDifficulty.LEGEND -> AIDifficulty.AMATEUR
                        }
                        viewModel.setAIDifficulty(newDifficulty)
                    }
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        if (gameState.gameMode == GameMode.ARCADE) {
                            Text(
                                text = "יעד: 3 ⚽",
                                color = Color(0xFF10B981),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "ראשון ל-3 ⚡",
                                color = Color(0xFF38BDF8),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = "${gameState.matchMinute}'",
                                color = Color(0xFF38BDF8),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = if (gameState.half == 1) "מחצית 1" else "מחצית 2",
                                color = Color.LightGray,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // If Home Turn, End Turn button
                if (gameState.currentTurn == Team.HOME && !gameState.isHalfTime && !gameState.isMatchOver) {
                    Surface(
                        color = Color(0xFFB45309),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFFBBF24)),
                        shadowElevation = 4.dp,
                        modifier = Modifier.clickable { viewModel.endTurnManually() }
                    ) {
                        Text(
                            text = "סיום\nתור",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
        
        // AI Thought Banner
        AnimatedVisibility(
            visible = gameState.aiThought != null && gameState.currentTurn == Team.AWAY,
            enter = fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.9f),
            exit = fadeOut(animationSpec = tween(500))
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.85f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.7f))
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(text = "🤖", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = gameState.aiThought ?: "",
                        color = Color(0xFFFCD34D),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        // Pitch Area
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 8.dp) // less padding to match image
                .background(Color(0xFF1E1E1E), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)) // Pitch green will be drawn inside
        ) {
            PitchCanvas(
                gameState = gameState,
                onPlayerClick = { viewModel.onPlayerSelected(it) },
                onPlayerLongClick = { playerId ->
                    val player = gameState.players.find { it.id == playerId }
                    if (player != null) {
                        playerProfileToShow = player
                    }
                },
                onGridClick = { x, y -> viewModel.onGridCellSelected(x, y) }
            )

            // Tactical Pitch Grid HUD Chip (Speed Stat & Coordinate status)
            if (gameState.selectedPlayerId != null) {
                val selPlayer = gameState.players.find { it.id == gameState.selectedPlayerId }
                if (selPlayer != null) {
                    val coordLabel = PitchGridConfig.getCoordinateLabel(selPlayer.position.x, selPlayer.position.y)
                    val oppWithBall = gameState.players.find { it.id == gameState.playerWithBallId && it.team != selPlayer.team }
                    val tackleDist = if (oppWithBall != null) maxOf(abs(selPlayer.position.x - oppWithBall.position.x), abs(selPlayer.position.y - oppWithBall.position.y)) else 999
                    val tackleHintSuffix = if (oppWithBall != null && tackleDist <= selPlayer.moveRange) {
                        val costStr = if (tackleDist <= 1) "מהלך 1" else if (gameState.actionsLeft >= 2) "2 מהלכים" else "מהלך 1"
                        " • ⚔️ לחץ על ${oppWithBall.name} לחטיפה/תיקול ($costStr)!"
                    } else ""

                    val actionHint = when (gameState.selectedAction) {
                        ActionType.MOVE -> "⚡ מהירות: ${selPlayer.currentSpeed} • ${selPlayer.moveRange} משבצות מודגשות לתנועה$tackleHintSuffix"
                        ActionType.DRIBBLE -> "⚡ מהירות כדרור: ${selPlayer.currentSpeed} • ${selPlayer.moveRange} משבצות מודגשות לכדרור"
                        ActionType.PASS -> "🎯 מסירה: ${selPlayer.currentPassing} • טווח: ${selPlayer.passRange} משבצות"
                        ActionType.SHOOT -> {
                            val eval = PitchGridEvaluator.calculateShootEvaluation(gameState, selPlayer)
                            "🎯 בעיטה לשער! סיכוי: ${eval.finalProbability}% (בעיטה: ${eval.shootingStat} | מרחק: ${String.format("%.1f", eval.distanceToGoal)} משבצות) • לחץ על השער לביצוע"
                        }
                        ActionType.TACKLE -> "🛡️ תיקול יריב (הגנה: ${selPlayer.currentDefense})"
                        null -> if (selPlayer.team == gameState.currentTurn) "⚡ מהירות: ${selPlayer.currentSpeed} • ${selPlayer.moveRange} משבצות מודגשות לתנועה$tackleHintSuffix" else "שחקן יריב (${selPlayer.roleIcon})"
                    }
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0F172A).copy(alpha = 0.92f),
                        border = BorderStroke(1.2.dp, Color(0xFF10B981).copy(alpha = 0.7f)),
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${selPlayer.roleIcon} ${selPlayer.name} [$coordLabel] • $actionHint",
                                color = Color.White,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            // Render Player Profile Dialog if a player is selected
            playerProfileToShow?.let { player ->
                com.example.ui.PlayerProfileDialog(
                    player = player,
                    onDismiss = { playerProfileToShow = null }
                )
            }
            
            // Tactical Command Deck: only shown in TACTICAL mode if actionMenuOpen is explicitly enabled, never in Arcade mode
            if (gameState.gameMode == GameMode.TACTICAL && gameState.actionMenuOpen && gameState.selectedPlayerId != null) {
                val selectedPlayer = gameState.players.find { it.id == gameState.selectedPlayerId }
                if (selectedPlayer != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 8.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        TacticalCommandDeck(
                            player = selectedPlayer,
                            gameState = gameState,
                            onActionSelected = { action -> viewModel.onActionSelected(action) },
                            onEndTurn = { viewModel.endTurnManually() },
                            onDismiss = { viewModel.onGridCellSelected(selectedPlayer.position.x, selectedPlayer.position.y) }
                        )
                    }
                }
            }

            // Tactical Duel Clash Overlay (FTG Style RPG Dice Confrontation)
            gameState.tacticalDuel?.let { duel ->
                TacticalDuelOverlay(
                    duel = duel,
                    onDismiss = { viewModel.dismissTacticalDuel() }
                )
            }

            // Toast / Goal UI Message
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = gameState.uiMessage != null,
                    enter = fadeIn(animationSpec = tween(200)) + scaleIn(initialScale = 0.7f, animationSpec = tween(300)),
                    exit = fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 1.1f, animationSpec = tween(300))
                ) {
                    gameState.uiMessage?.let { msg ->
                        val isGoal = msg.contains("שער") || msg.contains("גול")
                        val isFoulOrPenalty = msg.contains("עבירה") || msg.contains("פנדל")
                        val isRedCard = msg.contains("אדום")
                        val isYellowCard = msg.contains("צהוב")
                        val isInterception = msg.contains("חטיפה")
                        val isSave = msg.contains("זינוק") || msg.contains("הצלה") || msg.contains("הדף")
                        val isCorner = msg.contains("קרן") || msg.contains("כדור קרן")
                        val isThrowIn = msg.contains("חוץ") || msg.contains("כדור חוץ")

                        val borderColor = when {
                            isGoal -> Brush.horizontalGradient(listOf(Color(0xFFFFD700), Color(0xFFFF8800), Color(0xFFFFD700)))
                            isSave -> Brush.horizontalGradient(listOf(Color(0xFF06B6D4), Color(0xFF3B82F6), Color(0xFF9333EA)))
                            isCorner -> Brush.horizontalGradient(listOf(Color(0xFFF59E0B), Color(0xFFEF4444), Color(0xFFF59E0B)))
                            isThrowIn -> Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFF059669)))
                            isRedCard -> Brush.horizontalGradient(listOf(Color(0xFFEF4444), Color(0xFF991B1B)))
                            isYellowCard -> Brush.horizontalGradient(listOf(Color(0xFFFFD700), Color(0xFFEAB308)))
                            isFoulOrPenalty -> Brush.horizontalGradient(listOf(Color(0xFFF97316), Color(0xFFEA580C)))
                            isInterception -> Brush.horizontalGradient(listOf(Color(0xFF38BDF8), Color(0xFF0284C7)))
                            else -> Brush.horizontalGradient(listOf(Color(0xFF475569), Color(0xFF334155)))
                        }

                        val titleBanner = when {
                            isGoal -> "⚽ GOOOAL! ⚽"
                            isSave -> "🧤 הצלה מרהיבה! 🧤"
                            isCorner -> "🚩 כדור קרן!"
                            isThrowIn -> "🤾 כדור חוץ!"
                            isRedCard -> "🟥 כרטיס אדום!"
                            isFoulOrPenalty && msg.contains("פנדל") -> "🎯 שריקה לפנדל!"
                            isFoulOrPenalty -> "⚠️ עבירה!"
                            isInterception -> "🛡️ חטיפת כדור!"
                            else -> null
                        }

                        val titleColor = when {
                            isGoal -> Color(0xFFFFD700)
                            isSave -> Color(0xFF38BDF8)
                            isCorner -> Color(0xFFFBBF24)
                            isThrowIn -> Color(0xFF34D399)
                            isRedCard -> Color(0xFFEF4444)
                            isYellowCard -> Color(0xFFFFD700)
                            isFoulOrPenalty -> Color(0xFFFB923C)
                            isInterception -> Color(0xFF38BDF8)
                            else -> Color.White
                        }

                        Surface(
                            color = Color(0xFF0F172A).copy(alpha = 0.95f),
                            shape = RoundedCornerShape(20.dp),
                            border = androidx.compose.foundation.BorderStroke(2.5.dp, borderColor),
                            shadowElevation = 16.dp,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 28.dp, vertical = 20.dp)
                            ) {
                                if (titleBanner != null) {
                                    Text(
                                        text = titleBanner,
                                        color = titleColor,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                                Text(
                                    text = msg,
                                    color = Color.White,
                                    fontSize = if (isGoal) 22.sp else 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                        
                        LaunchedEffect(msg) {
                            delay(if (isGoal || isRedCard || msg.contains("פנדל")) 2600 else 1800)
                            viewModel.clearMessage()
                        }
                    }
                }

                // Half-Time Overlay
                if (gameState.isHalfTime) {
                    HalfTimeOverlay(
                        gameState = gameState,
                        onStartSecondHalf = { viewModel.startSecondHalf() }
                    )
                }

                // Match Over Overlay (Full Stats)
                if (gameState.isMatchOver) {
                    MatchOverOverlay(
                        gameState = gameState,
                        onRestartMatch = { viewModel.restartMatch() },
                        onBackToMenu = { viewModel.returnToMainMenu() }
                    )
                }
            }
        }
        
        // Bottom Navigation Bar
        NavigationBar(
            containerColor = Color(0xFF1E293B), // match dark theme
            contentColor = Color.White
        ) {
            NavigationBarItem(
                icon = { Icon(Icons.Filled.PlayArrow, contentDescription = "משחק") },
                label = { Text("משחק") },
                selected = true,
                onClick = { },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    unselectedIconColor = Color.Gray,
                    selectedTextColor = Color.White,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = Color.Transparent
                )
            )
            NavigationBarItem(
                icon = { Icon(Icons.Filled.Dashboard, contentDescription = "טקטיקה") },
                label = { Text("טקטיקה") },
                selected = false,
                onClick = {
                    if (gameState.currentTurn == Team.HOME && gameState.actionsLeft > 0) {
                        showTacticsDialog = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    unselectedIconColor = if (gameState.actionsLeft > 0) Color.White else Color.Gray,
                    unselectedTextColor = if (gameState.actionsLeft > 0) Color.White else Color.Gray
                )
            )
            NavigationBarItem(
                icon = { Icon(Icons.Filled.SwapHoriz, contentDescription = "חילופים") },
                label = { Text("חילופים (${gameState.homeSubsLeft})") },
                selected = false,
                onClick = { 
                    if (gameState.currentTurn == Team.HOME && gameState.homeSubsLeft > 0) {
                        showSubDialog = true 
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    unselectedIconColor = if (gameState.homeSubsLeft > 0) Color.White else Color.Gray,
                    unselectedTextColor = if (gameState.homeSubsLeft > 0) Color.White else Color.Gray
                )
            )
        }
    }

    if (showTacticsDialog) {
        TacticsDialog(
            gameState = gameState,
            onDismiss = { showTacticsDialog = false },
            onSelectMentality = { mentality ->
                viewModel.setHomeMentality(mentality)
                showTacticsDialog = false
            }
        )
    }

    if (showSubDialog) {
        SubstitutionDialog(
            gameState = gameState,
            onDismiss = { showSubDialog = false },
            onSubstitute = { outId, inId ->
                viewModel.performSubstitution(outId, inId)
                showSubDialog = false
            }
        )
    }
}

data class PlayerAnimData(
    val player: Player,
    val animX: Float,
    val animY: Float,
    val elevation: Float,
    val isMoving: Boolean,
    val isSelected: Boolean,
    val idlePhase: Int
)

@Composable
fun PitchCanvas(
    gameState: GameState,
    onPlayerClick: (Int) -> Unit,
    onPlayerLongClick: (Int) -> Unit,
    onGridClick: (Int, Int) -> Unit
) {
    val gridWidth = 11
    val gridHeight = 15
    val textMeasurer = rememberTextMeasurer()

    // Create a list of animated positions for players with fluid spring physics
    val animatedPlayers = gameState.players.map { player ->
        val animatedX by animateFloatAsState(
            targetValue = player.position.x.toFloat(),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "playerX_${player.id}"
        )
        val animatedY by animateFloatAsState(
            targetValue = player.position.y.toFloat(),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "playerY_${player.id}"
        )
        // Dynamic running elevation bounce: player lifts slightly while traveling across grid
        val isMoving = kotlin.math.abs(animatedX - player.position.x.toFloat()) > 0.05f || 
                       kotlin.math.abs(animatedY - player.position.y.toFloat()) > 0.05f
        val animatedElevation by animateFloatAsState(
            targetValue = if (isMoving) 14f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "playerElev_${player.id}"
        )
        // Gentle rhythmic idle breathing bounce
        val idlePhase = (player.id * 37) % 100
        val isSelected = player.id == gameState.selectedPlayerId
        PlayerAnimData(player, animatedX, animatedY, animatedElevation, isMoving, isSelected, idlePhase)
    }

    // Create animated positions for the ball with snappy spring dynamics
    val ballXTarget = gameState.ballPosition?.x?.toFloat() 
        ?: gameState.players.find { it.id == gameState.playerWithBallId }?.position?.x?.toFloat() 
        ?: 5f
    val ballYTarget = gameState.ballPosition?.y?.toFloat() 
        ?: gameState.players.find { it.id == gameState.playerWithBallId }?.position?.y?.toFloat() 
        ?: 7f
        
    val animatedBallX by animateFloatAsState(
        targetValue = ballXTarget,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "ballX"
    )
    val animatedBallY by animateFloatAsState(
        targetValue = ballYTarget,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "ballY"
    )
    val ballRadiusTarget = if (gameState.playerWithBallId != null) 0.16f else 0.21f
    val animatedBallRadius by animateFloatAsState(
        targetValue = ballRadiusTarget,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "ballRadius"
    )
    val ballOffsetXTarget = if (gameState.playerWithBallId != null) 0.22f else 0f
    val animatedBallOffsetX by animateFloatAsState(
        targetValue = ballOffsetXTarget,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "ballOffsetX"
    )

    // Dynamic ball flight arc: when moving freely across pitch, ball arcs smoothly into the air
    val ballInMotion = kotlin.math.abs(animatedBallX - ballXTarget) > 0.08f || kotlin.math.abs(animatedBallY - ballYTarget) > 0.08f
    val ballHeightTarget = when {
        ballInMotion && gameState.playerWithBallId == null -> 1.45f
        gameState.ballPosition != null && gameState.playerWithBallId == null -> 1.25f
        else -> 1.0f
    }
    val animatedBallScale by animateFloatAsState(
        targetValue = ballHeightTarget,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "ballScale"
    )

    // Smooth physics-based trajectory progress for passes, shots, and set pieces
    val trajectoryProgress = remember { Animatable(0f) }
    LaunchedEffect(gameState.ballTrajectory?.timestamp) {
        val traj = gameState.ballTrajectory
        if (traj != null) {
            trajectoryProgress.snapTo(0f)
            trajectoryProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = traj.durationMs.toInt(),
                    easing = if (traj.isShot) FastOutLinearInEasing else FastOutSlowInEasing
                )
            )
        }
    }

    val currentGameState by rememberUpdatedState(gameState)
    val currentOnPlayerClick by rememberUpdatedState(onPlayerClick)
    val currentOnGridClick by rememberUpdatedState(onGridClick)

    val infiniteTransition = rememberInfiniteTransition(label = "pitchEffects")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val animClockMs by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 60000f,
        animationSpec = infiniteRepeatable(
            animation = tween(60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "animClockMs"
    )

    val selectedPlayer = gameState.players.find { it.id == gameState.selectedPlayerId }
    val validMoveTiles = remember(gameState.players, gameState.selectedPlayerId, gameState.selectedAction, gameState.actedPlayerIds) {
        if (selectedPlayer != null && (gameState.selectedAction == ActionType.MOVE || gameState.selectedAction == ActionType.DRIBBLE || (gameState.selectedAction == null && selectedPlayer.team == gameState.currentTurn && !gameState.actedPlayerIds.contains(selectedPlayer.id)))) {
            PitchGridEvaluator.computeValidMovementTiles(gameState, selectedPlayer)
        } else {
            emptyMap()
        }
    }

    val validPassTiles = remember(gameState.players, gameState.selectedPlayerId, gameState.selectedAction, gameState.playerWithBallId) {
        if (selectedPlayer != null && selectedPlayer.id == gameState.playerWithBallId && (gameState.selectedAction == ActionType.PASS || (gameState.selectedAction == null && selectedPlayer.team == gameState.currentTurn))) {
            PitchGridEvaluator.computeValidPassingTiles(gameState, selectedPlayer)
        } else {
            emptyMap()
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(14.dp))
            .graphicsLayer {
                rotationX = 0f
                scaleY = 1.0f
                scaleX = 1.0f
                shadowElevation = 8.dp.toPx()
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { offset ->
                        val cellWidth = size.width / gridWidth
                        val cellHeight = size.height / gridHeight
                        
                        val gridX = (offset.x / cellWidth).toInt().coerceIn(0, gridWidth - 1)
                        val gridY = (offset.y / cellHeight).toInt().coerceIn(0, gridHeight - 1)
                        
                        val state = currentGameState
                        val exactPlayer = state.players.find { it.position.x == gridX && it.position.y == gridY }
                        val player = exactPlayer ?: state.players.minByOrNull { p ->
                            val px = p.position.x * cellWidth + cellWidth / 2f
                            val py = p.position.y * cellHeight + cellHeight / 2f
                            val dx = px - offset.x
                            val dy = py - offset.y
                            dx * dx + dy * dy
                        }?.takeIf { p ->
                            val px = p.position.x * cellWidth + cellWidth / 2f
                            val py = p.position.y * cellHeight + cellHeight / 2f
                            val dx = px - offset.x
                            val dy = py - offset.y
                            (dx * dx + dy * dy) <= (cellWidth * 0.7f) * (cellWidth * 0.7f)
                        }
                        if (player != null) {
                            onPlayerLongClick(player.id)
                        }
                    },
                    onTap = { offset ->
                        val cellWidth = size.width / gridWidth
                        val cellHeight = size.height / gridHeight
                        
                        val gridX = (offset.x / cellWidth).toInt().coerceIn(0, gridWidth - 1)
                        val gridY = (offset.y / cellHeight).toInt().coerceIn(0, gridHeight - 1)
                        
                        val state = currentGameState
                        // Check if clicked an exact player position
                        val exactPlayer = state.players.find { it.position.x == gridX && it.position.y == gridY }
                        // Proximity fallback in case tap was on the player's circle border/radius
                        val player = exactPlayer ?: state.players.minByOrNull { p ->
                            val px = p.position.x * cellWidth + cellWidth / 2f
                            val py = p.position.y * cellHeight + cellHeight / 2f
                            val dx = px - offset.x
                            val dy = py - offset.y
                            dx * dx + dy * dy
                        }?.takeIf { p ->
                            val px = p.position.x * cellWidth + cellWidth / 2f
                            val py = p.position.y * cellHeight + cellHeight / 2f
                            val dx = px - offset.x
                            val dy = py - offset.y
                            (dx * dx + dy * dy) <= (cellWidth * 0.7f) * (cellWidth * 0.7f)
                        }
                        
                        if (state.selectedAction != null) {
                            // If an action is already selected (e.g. TCK or PAS), execute it on this grid cell
                            currentOnGridClick(gridX, gridY)
                        } else if (player != null) {
                            currentOnPlayerClick(player.id)
                        } else {
                            currentOnGridClick(gridX, gridY)
                        }
                    }
                )
            }
    ) {
        val cellWidth = size.width / gridWidth
        val cellHeight = size.height / gridHeight

        // 1. Draw Clean Lush Grass Turf with alternating lawn stripes
        val stripeCount = 15
        val stripeHeight = size.height / stripeCount
        for (i in 0 until stripeCount) {
            val stripeColor = if (i % 2 == 0) Color(0xFF1E7E34) else Color(0xFF238E3B)
            drawRect(
                color = stripeColor,
                topLeft = Offset(0f, i * stripeHeight),
                size = Size(size.width, stripeHeight)
            )
        }

        // Discrete, subtle tactical grid lines (quiet and clean so player can easily see cells)
        for (gx in 0..gridWidth) {
            val x = gx * cellWidth
            drawLine(
                color = Color.White.copy(alpha = 0.07f),
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1f
            )
        }
        for (gy in 0..gridHeight) {
            val y = gy * cellHeight
            drawLine(
                color = Color.White.copy(alpha = 0.07f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
        }

        // 2. Draw Crisp, Classic Pitch Markings
        val pitchLineColor = Color.White.copy(alpha = 0.85f)
        val lineWidth = 2.5f

        // Touchlines (Inset by half a cell for natural stadium pitch margin)
        val pitchLeft = cellWidth * 0.5f
        val pitchRight = size.width - cellWidth * 0.5f
        val pitchTop = cellHeight * 0.5f
        val pitchBottom = size.height - cellHeight * 0.5f
        val pitchW = pitchRight - pitchLeft
        val pitchH = pitchBottom - pitchTop

        drawRect(
            color = pitchLineColor,
            topLeft = Offset(pitchLeft, pitchTop),
            size = Size(pitchW, pitchH),
            style = Stroke(width = lineWidth)
        )

        // Halfway Line
        val midY = size.height / 2f
        drawLine(
            color = pitchLineColor,
            start = Offset(pitchLeft, midY),
            end = Offset(pitchRight, midY),
            strokeWidth = lineWidth
        )

        // Center Circle & Center Spot
        val centerPos = Offset(size.width / 2f, midY)
        drawCircle(
            color = pitchLineColor,
            radius = cellWidth * 1.5f,
            center = centerPos,
            style = Stroke(width = lineWidth)
        )
        drawCircle(
            color = pitchLineColor,
            radius = 4f,
            center = centerPos
        )

        // Penalty Boxes
        // Top Penalty Box (Away goal)
        drawRect(
            color = pitchLineColor,
            topLeft = Offset(cellWidth * 2f, pitchTop),
            size = Size(cellWidth * 7f, cellHeight * 2.2f),
            style = Stroke(width = lineWidth)
        )
        // Top 6-yard Box
        drawRect(
            color = pitchLineColor,
            topLeft = Offset(cellWidth * 3.5f, pitchTop),
            size = Size(cellWidth * 4f, cellHeight * 0.9f),
            style = Stroke(width = lineWidth * 0.8f)
        )
        // Top Penalty Spot
        drawCircle(
            color = pitchLineColor,
            radius = 3.5f,
            center = Offset(size.width / 2f, pitchTop + cellHeight * 1.6f)
        )

        // Bottom Penalty Box (Home goal)
        drawRect(
            color = pitchLineColor,
            topLeft = Offset(cellWidth * 2f, pitchBottom - cellHeight * 2.2f),
            size = Size(cellWidth * 7f, cellHeight * 2.2f),
            style = Stroke(width = lineWidth)
        )
        // Bottom 6-yard Box
        drawRect(
            color = pitchLineColor,
            topLeft = Offset(cellWidth * 3.5f, pitchBottom - cellHeight * 0.9f),
            size = Size(cellWidth * 4f, cellHeight * 0.9f),
            style = Stroke(width = lineWidth * 0.8f)
        )
        // Bottom Penalty Spot
        drawCircle(
            color = pitchLineColor,
            radius = 3.5f,
            center = Offset(size.width / 2f, pitchBottom - cellHeight * 1.6f)
        )

        // Corner Arcs
        val cornerArcSize = Size(cellWidth * 0.5f, cellWidth * 0.5f)
        drawArc(pitchLineColor, 0f, 90f, false, topLeft = Offset(pitchLeft - cornerArcSize.width / 2f, pitchTop - cornerArcSize.height / 2f), size = cornerArcSize, style = Stroke(lineWidth))
        drawArc(pitchLineColor, 90f, 90f, false, topLeft = Offset(pitchRight - cornerArcSize.width / 2f, pitchTop - cornerArcSize.height / 2f), size = cornerArcSize, style = Stroke(lineWidth))
        drawArc(pitchLineColor, 270f, 90f, false, topLeft = Offset(pitchLeft - cornerArcSize.width / 2f, pitchBottom - cornerArcSize.height / 2f), size = cornerArcSize, style = Stroke(lineWidth))
        drawArc(pitchLineColor, 180f, 90f, false, topLeft = Offset(pitchRight - cornerArcSize.width / 2f, pitchBottom - cornerArcSize.height / 2f), size = cornerArcSize, style = Stroke(lineWidth))

        // Goals (Top & Bottom)
        val goalW = cellWidth * 3f
        val goalLeft = (size.width - goalW) / 2f
        // Top Goal Net
        drawRect(
            color = Color.White.copy(alpha = 0.20f),
            topLeft = Offset(goalLeft, pitchTop - cellHeight * 0.4f),
            size = Size(goalW, cellHeight * 0.4f)
        )
        drawRect(
            color = Color.White,
            topLeft = Offset(goalLeft, pitchTop - cellHeight * 0.4f),
            size = Size(goalW, cellHeight * 0.4f),
            style = Stroke(width = 2f)
        )
        // Bottom Goal Net
        drawRect(
            color = Color.White.copy(alpha = 0.20f),
            topLeft = Offset(goalLeft, pitchBottom),
            size = Size(goalW, cellHeight * 0.4f)
        )
        drawRect(
            color = Color.White,
            topLeft = Offset(goalLeft, pitchBottom),
            size = Size(goalW, cellHeight * 0.4f),
            style = Stroke(width = 2f)
        )

        // 2.5 Draw Offside Line (if attacking)
        if (gameState.currentTurn == Team.HOME || gameState.currentTurn == Team.AWAY) {
            val isHome = gameState.currentTurn == Team.HOME
            val opponents = gameState.players.filter { it.team != gameState.currentTurn && !it.isRedCarded && it.position.y >= 0 }
            
            val offsideY = if (isHome) {
                opponents.sortedBy { it.position.y }.getOrNull(1)?.position?.y ?: 0
            } else {
                opponents.sortedByDescending { it.position.y }.getOrNull(1)?.position?.y ?: 14
            }

            // Only draw if it's in the attacking half
            if ((isHome && offsideY <= 6) || (!isHome && offsideY >= 8)) {
                // Ball position check
                val ballY = gameState.ballPosition?.y ?: gameState.players.find { it.id == gameState.playerWithBallId }?.position?.y ?: offsideY
                val actualOffsideLine = if (isHome) kotlin.math.min(offsideY, ballY) else kotlin.math.max(offsideY, ballY)
                
                // Draw the line at the edge of the grid cell
                // For Home (attacking top), offside area is ABOVE the line. We draw the line at the TOP of the safe cell.
                // Which is actualOffsideLine * cellHeight.
                val yPos = if (isHome) (actualOffsideLine * cellHeight) else (actualOffsideLine * cellHeight + cellHeight)
                
                drawLine(
                    color = Color.Red.copy(alpha = 0.5f),
                    start = Offset(0f, yPos),
                    end = Offset(size.width, yPos),
                    strokeWidth = 4f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f)
                )
            }
        }

        // 2.9 Draw Opponent Threat Aura (Zone of Control) when a player is selected
        if (gameState.selectedPlayerId != null) {
            val selectedPlayer = gameState.players.find { it.id == gameState.selectedPlayerId }
            if (selectedPlayer != null && selectedPlayer.team == gameState.currentTurn) {
                val nearbyOpponents = gameState.players.filter { 
                    it.team != selectedPlayer.team && !it.isRedCarded &&
                    kotlin.math.abs(it.position.x - selectedPlayer.position.x) <= 3 &&
                    kotlin.math.abs(it.position.y - selectedPlayer.position.y) <= 3
                }
                nearbyOpponents.forEach { opp ->
                    for (dx in -1..1) {
                        for (dy in -1..1) {
                            val tx = opp.position.x + dx
                            val ty = opp.position.y + dy
                            if (tx in 0 until gridWidth && ty in 0 until gridHeight) {
                                drawRect(
                                    color = Color(0xFFEF4444).copy(alpha = 0.08f),
                                    topLeft = Offset(tx * cellWidth, ty * cellHeight),
                                    size = Size(cellWidth, cellHeight)
                                )
                                drawRect(
                                    color = Color(0xFFEF4444).copy(alpha = 0.22f),
                                    topLeft = Offset(tx * cellWidth, ty * cellHeight),
                                    size = Size(cellWidth, cellHeight),
                                    style = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f))
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Highlight Valid Targets (Movement & Passing Grid Coordinates)
        if (selectedPlayer != null && selectedPlayer.team == gameState.currentTurn) {
            val pCx = selectedPlayer.position.x * cellWidth + cellWidth / 2f
            val pCy = selectedPlayer.position.y * cellHeight + cellHeight / 2f

            // 3.1 Valid Movement Coordinates based on Player's Speed stat
            val isMoveActive = gameState.selectedAction == ActionType.MOVE || gameState.selectedAction == ActionType.DRIBBLE || (gameState.selectedAction == null && selectedPlayer.team == gameState.currentTurn && !gameState.actedPlayerIds.contains(selectedPlayer.id))
            if (isMoveActive && validMoveTiles.isNotEmpty()) {
                val isDribble = selectedPlayer.id == gameState.playerWithBallId || gameState.selectedAction == ActionType.DRIBBLE

                // 3.1.0 Speed Outer Boundary Contour
                // Draw a sleek speed aura contour around the player indicating their maximum Speed reach on the grid
                val maxReachPx = selectedPlayer.moveRange * cellWidth
                val auraBaseColor = if (isDribble) Color(0xFFF59E0B) else Color(0xFF10B981)
                drawCircle(
                    color = auraBaseColor.copy(alpha = 0.06f * (pulseAlpha + 0.4f)),
                    radius = maxReachPx + cellWidth * 0.35f,
                    center = Offset(pCx, pCy)
                )
                drawCircle(
                    color = auraBaseColor.copy(alpha = 0.30f * (pulseAlpha + 0.5f)),
                    radius = maxReachPx + cellWidth * 0.35f,
                    center = Offset(pCx, pCy),
                    style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f))
                )

                validMoveTiles.forEach { (pos, moveInfo) ->
                    val x = pos.x
                    val y = pos.y
                    val tCx = x * cellWidth + cellWidth / 2f
                    val tCy = y * cellHeight + cellHeight / 2f

                    // Color based on step distance & speed intensity:
                    // Step 1 = Emerald Green (Normal step)
                    // Step 2 = Electric Cyan (Fast pace)
                    // Step 3 = Golden Amber (Max sprint burst)
                    val tileColor = when {
                        isDribble -> when (moveInfo.steps) {
                            1 -> Color(0xFFF59E0B) // Amber
                            2 -> Color(0xFFFB923C) // Orange
                            else -> Color(0xFFEA580C) // Deep Orange
                        }
                        else -> when (moveInfo.steps) {
                            1 -> Color(0xFF10B981) // Emerald Green
                            2 -> Color(0xFF06B6D4) // Electric Cyan
                            else -> Color(0xFFFACC15) // Golden Sprint Burst
                        }
                    }

                    val bx = x * cellWidth + 2f
                    val by = y * cellHeight + 2f
                    val bw = cellWidth - 4f
                    val bh = cellHeight - 4f

                    // Movement tile fill with pulsing radial alpha
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                tileColor.copy(alpha = pulseAlpha * 0.45f + 0.15f),
                                tileColor.copy(alpha = pulseAlpha * 0.18f + 0.05f)
                            ),
                            center = Offset(tCx, tCy),
                            radius = cellWidth * 0.7f
                        ),
                        topLeft = Offset(bx, by),
                        size = Size(bw, bh)
                    )
                    // Tile border
                    drawRect(
                        color = tileColor.copy(alpha = 0.90f),
                        topLeft = Offset(bx, by),
                        size = Size(bw, bh),
                        style = Stroke(width = if (moveInfo.isMaxRange) 2.2f else 1.6f)
                    )
                    // Corner brackets
                    val bracketLen = 7f
                    drawLine(Color.White, Offset(bx, by), Offset(bx + bracketLen, by), 2f)
                    drawLine(Color.White, Offset(bx, by), Offset(bx, by + bracketLen), 2f)
                    drawLine(Color.White, Offset(bx + bw, by + bh), Offset(bx + bw - bracketLen, by + bh), 2f)
                    drawLine(Color.White, Offset(bx + bw, by), Offset(bx + bw - bracketLen, by), 2f)
                    drawLine(Color.White, Offset(bx, by + bh), Offset(bx + bracketLen, by + bh), 2f)

                    // Step badge with Movement Safety / Success percentage directly on the tile
                    val tilePctText = if (moveInfo.isBallPickup) "⚽ 100%" else if (moveInfo.isContested) "⚠️ 70%" else "100%"
                    val pctBorderColor = if (moveInfo.isContested) Color(0xFFF59E0B) else Color(0xFF10B981)
                    val stepLayout = textMeasurer.measure(
                        tilePctText,
                        TextStyle(color = Color.White, fontSize = (cellWidth * 0.16f).toSp(), fontWeight = FontWeight.Black)
                    )
                    val badgeW = stepLayout.size.width + 10f
                    val badgeH = stepLayout.size.height + 4f
                    drawRoundRect(
                        color = Color(0xFF0F172A).copy(alpha = 0.88f),
                        topLeft = Offset(bx + 3f, by + 3f),
                        size = Size(badgeW, badgeH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                    )
                    drawRoundRect(
                        color = pctBorderColor,
                        topLeft = Offset(bx + 3f, by + 3f),
                        size = Size(badgeW, badgeH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f),
                        style = Stroke(width = 1.2f)
                    )
                    drawText(
                        textLayoutResult = stepLayout,
                        topLeft = Offset(bx + 8f, by + 5f)
                    )

                    // If max range tile for this player's Speed stat, render "MAX" badge in bottom right
                    if (moveInfo.isMaxRange) {
                        val maxLayout = textMeasurer.measure(
                            "MAX",
                            TextStyle(color = tileColor, fontSize = (cellWidth * 0.12f).toSp(), fontWeight = FontWeight.ExtraBold)
                        )
                        drawText(
                            textLayoutResult = maxLayout,
                            topLeft = Offset(bx + bw - maxLayout.size.width - 4f, by + bh - maxLayout.size.height - 3f)
                        )
                    }

                    // Ball pickup indicator on tile
                    if (moveInfo.isBallPickup) {
                        val ballLayout = textMeasurer.measure("⚽", TextStyle(fontSize = 12.sp))
                        drawText(
                            textLayoutResult = ballLayout,
                            topLeft = Offset(tCx - ballLayout.size.width / 2f, tCy - ballLayout.size.height / 2f)
                        )
                    }

                    // Opponent pressure indicator on tile
                    if (moveInfo.isContested) {
                        drawCircle(
                            color = Color(0xFFEF4444).copy(alpha = 0.9f),
                            radius = 4.5f,
                            center = Offset(bx + bw - 7f, by + 7f)
                        )
                    }

                    // Dynamic dashed vector line from player to movement coordinate
                    drawLine(
                        color = tileColor.copy(alpha = 0.55f),
                        start = Offset(pCx, pCy),
                        end = Offset(tCx, tCy),
                        strokeWidth = if (moveInfo.isMaxRange) 2f else 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )
                }
            }

            // 3.1.5 Highlight Opponent with Ball in Tackle Range
            val opponentWithBall = gameState.players.find { it.id == gameState.playerWithBallId && it.team != selectedPlayer.team }
            if (opponentWithBall != null) {
                val tDist = maxOf(abs(selectedPlayer.position.x - opponentWithBall.position.x), abs(selectedPlayer.position.y - opponentWithBall.position.y))
                if (tDist <= selectedPlayer.moveRange) {
                    val oppX = opponentWithBall.position.x
                    val oppY = opponentWithBall.position.y
                    val oppCx = oppX * cellWidth + cellWidth / 2f
                    val oppCy = oppY * cellHeight + cellHeight / 2f
                    val tackleColor = Color(0xFFA855F7) // Purple
                    val bx = oppX * cellWidth
                    val by = oppY * cellHeight
                    val bw = cellWidth
                    val bh = cellHeight

                    // Pulsing animated tackle target box
                    drawRect(
                        color = tackleColor.copy(alpha = 0.20f * (pulseAlpha + 0.4f)),
                        topLeft = Offset(bx, by),
                        size = Size(bw, bh)
                    )
                    drawRect(
                        color = tackleColor.copy(alpha = 0.85f),
                        topLeft = Offset(bx + 2f, by + 2f),
                        size = Size(bw - 4f, bh - 4f),
                        style = Stroke(width = 2.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f))
                    )
                    drawCircle(
                        color = tackleColor.copy(alpha = 0.5f * (pulseAlpha + 0.5f)),
                        radius = cellWidth * 0.48f,
                        center = Offset(oppCx, oppCy),
                        style = Stroke(width = 2f)
                    )

                    // Line from defender to opponent with ball
                    drawLine(
                        color = tackleColor.copy(alpha = 0.7f),
                        start = Offset(pCx, pCy),
                        end = Offset(oppCx, oppCy),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 5f), 0f)
                    )

                    // Badge: "⚔️ תיקול"
                    val costLabel = if (tDist <= 1) "⚔️ תיקול (מהלך 1)" else if (gameState.actionsLeft >= 2) "⚔️ תיקול (2 מהלכים)" else "⚔️ תיקול נואש (מהלך 1)"
                    val tBadge = textMeasurer.measure(
                        costLabel,
                        TextStyle(color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    )
                    val tBadgeW = tBadge.size.width + 8f
                    val tBadgeH = tBadge.size.height + 4f
                    drawRoundRect(
                        color = tackleColor,
                        topLeft = Offset(oppCx - tBadgeW / 2f, by - tBadgeH - 2f),
                        size = Size(tBadgeW, tBadgeH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                    )
                    drawText(
                        textLayoutResult = tBadge,
                        topLeft = Offset(oppCx - tBadgeW / 2f + 4f, by - tBadgeH)
                    )
                }
            }

            // 3.2 Valid Passing Coordinates with Floating Percentage directly on the Line
            val isPassActive = gameState.selectedAction == ActionType.PASS || (gameState.selectedAction == null && selectedPlayer.id == gameState.playerWithBallId)
            if (isPassActive && validPassTiles.isNotEmpty()) {
                validPassTiles.forEach { (pos, passInfo) ->
                    val x = pos.x
                    val y = pos.y
                    val tCx = x * cellWidth + cellWidth / 2f
                    val tCy = y * cellHeight + cellHeight / 2f

                    if (passInfo.isTeammate) {
                        val passColor = if (passInfo.isOffside) Color(0xFFEF4444) else if (passInfo.interceptRisk > 0) Color(0xFFF59E0B) else Color(0xFF38BDF8)

                        // Teammate pass target reticle
                        drawCircle(
                            color = passColor.copy(alpha = pulseAlpha + 0.35f),
                            radius = cellWidth * 0.48f,
                            center = Offset(tCx, tCy),
                            style = Stroke(width = 2.5f)
                        )
                        // Flight line glow + dashed line
                        drawLine(
                            color = passColor.copy(alpha = 0.35f),
                            start = Offset(pCx, pCy),
                            end = Offset(tCx, tCy),
                            strokeWidth = 6f
                        )
                        drawLine(
                            color = passColor.copy(alpha = 0.95f),
                            start = Offset(pCx, pCy),
                            end = Offset(tCx, tCy),
                            strokeWidth = 2.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                        )

                        // Success percentage badge FLOATING DIRECTLY ON THE FLIGHT LINE (at midpoint)
                        val mCx = (pCx + tCx) / 2f
                        val mCy = (pCy + tCy) / 2f
                        val badgeText = if (passInfo.isOffside) "🚩 נבדל" else if (passInfo.interceptRisk > 0) "⚠️ ${passInfo.successChance}%" else "⚽ ${passInfo.successChance}%"
                        val badgeLayout = textMeasurer.measure(
                            badgeText,
                            TextStyle(
                                color = Color.White,
                                fontSize = (cellWidth * 0.17f).toSp(),
                                fontWeight = FontWeight.Black
                            )
                        )
                        val bWidth = badgeLayout.size.width + 14f
                        val bHeight = badgeLayout.size.height + 6f
                        val bLeft = mCx - bWidth / 2f
                        val bTop = mCy - bHeight / 2f

                        drawRoundRect(
                            color = Color(0xFF0F172A).copy(alpha = 0.95f),
                            topLeft = Offset(bLeft, bTop),
                            size = Size(bWidth, bHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(bHeight / 2f, bHeight / 2f)
                        )
                        drawRoundRect(
                            color = passColor,
                            topLeft = Offset(bLeft, bTop),
                            size = Size(bWidth, bHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(bHeight / 2f, bHeight / 2f),
                            style = Stroke(width = 1.6f)
                        )
                        drawText(
                            textLayoutResult = badgeLayout,
                            topLeft = Offset(bLeft + 7f, bTop + 3f)
                        )
                    } else if (passInfo.isThroughBall && gameState.selectedAction == ActionType.PASS) {
                        // Open space through ball target
                        drawRect(
                            color = Color(0xFF38BDF8).copy(alpha = 0.12f),
                            topLeft = Offset(x * cellWidth + 2f, y * cellHeight + 2f),
                            size = Size(cellWidth - 4f, cellHeight - 4f)
                        )
                        drawRect(
                            color = Color(0xFF38BDF8).copy(alpha = 0.6f),
                            topLeft = Offset(x * cellWidth + 2f, y * cellHeight + 2f),
                            size = Size(cellWidth - 4f, cellHeight - 4f),
                            style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f))
                        )
                        val spaceLayout = textMeasurer.measure(
                            "${passInfo.successChance}%",
                            TextStyle(color = Color(0xFF38BDF8), fontSize = (cellWidth * 0.16f).toSp(), fontWeight = FontWeight.Bold)
                        )
                        drawText(
                            textLayoutResult = spaceLayout,
                            topLeft = Offset(tCx - spaceLayout.size.width / 2f, tCy - spaceLayout.size.height / 2f)
                        )
                    }
                }
            }

            // 3.3 Goal Shoot Target Reticle directly on Goal Net when Ball Carrier selected
            if (selectedPlayer.id == gameState.playerWithBallId) {
                val shootEval = PitchGridEvaluator.calculateShootEvaluation(gameState, selectedPlayer)
                val targetGoalY = if (selectedPlayer.team == Team.HOME) pitchTop else pitchBottom
                val goalCx = size.width / 2f
                val goalCy = if (selectedPlayer.team == Team.HOME) pitchTop + cellHeight * 0.75f else pitchBottom - cellHeight * 0.75f
                val shootColor = if (shootEval.finalProbability >= 60) Color(0xFF10B981) else Color(0xFFEF4444)

                // Pulsing goal target circle
                drawCircle(
                    color = shootColor.copy(alpha = pulseAlpha * 0.35f + 0.35f),
                    radius = cellWidth * 0.85f,
                    center = Offset(goalCx, goalCy),
                    style = Stroke(width = 2.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f))
                )
                // Goal shoot badge
                val sBadge = textMeasurer.measure(
                    "🎯 שער: ${shootEval.finalProbability}%",
                    TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                )
                val sW = sBadge.size.width + 16f
                val sH = sBadge.size.height + 8f
                drawRoundRect(
                    color = Color(0xFF0F172A).copy(alpha = 0.95f),
                    topLeft = Offset(goalCx - sW / 2f, goalCy - sH / 2f),
                    size = Size(sW, sH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                )
                drawRoundRect(
                    color = shootColor,
                    topLeft = Offset(goalCx - sW / 2f, goalCy - sH / 2f),
                    size = Size(sW, sH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
                    style = Stroke(width = 1.5f)
                )
                drawText(
                    textLayoutResult = sBadge,
                    topLeft = Offset(goalCx - sW / 2f + 8f, goalCy - sH / 2f + 4f)
                )
            }

            // 3.3 Shooting Target based on Shooting stat and distance to goal
            if (gameState.selectedAction == ActionType.SHOOT) {
                val shootEval = PitchGridEvaluator.calculateShootEvaluation(gameState, selectedPlayer)
                val targetGoalY = if (selectedPlayer.team == Team.HOME) 0f else size.height
                val targetGoalCenterX = size.width / 2f
                val shootColor = when {
                    shootEval.finalProbability >= 70 -> Color(0xFF10B981) // High chance (Green)
                    shootEval.finalProbability >= 45 -> Color(0xFFF59E0B) // Medium chance (Amber)
                    else -> Color(0xFFEF4444) // Long distance / tough angle (Red)
                }

                // 3.3.1 Pulsing Laser Trajectory Line
                drawLine(
                    color = shootColor.copy(alpha = pulseAlpha * 0.4f + 0.35f),
                    start = Offset(pCx, pCy),
                    end = Offset(targetGoalCenterX, targetGoalY),
                    strokeWidth = 5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 8f), 0f)
                )

                // 3.3.2 Glowing Target Goal Mouth
                val goalMouthTop = if (selectedPlayer.team == Team.HOME) 0f else size.height - (cellHeight * 0.85f)
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            shootColor.copy(alpha = pulseAlpha * 0.45f + 0.25f),
                            Color(0xFF0F172A).copy(alpha = 0.4f)
                        ),
                        startY = goalMouthTop,
                        endY = goalMouthTop + (cellHeight * 0.85f)
                    ),
                    topLeft = Offset(cellWidth * 3.8f, goalMouthTop),
                    size = Size(cellWidth * 3.4f, cellHeight * 0.85f)
                )
                drawRect(
                    color = shootColor,
                    topLeft = Offset(cellWidth * 3.8f, goalMouthTop),
                    size = Size(cellWidth * 3.4f, cellHeight * 0.85f),
                    style = Stroke(width = 2.5f)
                )

                // 3.3.3 Target Goal Reticle Crosshair
                val reticleY = if (selectedPlayer.team == Team.HOME) cellHeight * 0.42f else size.height - (cellHeight * 0.42f)
                drawCircle(
                    color = shootColor,
                    radius = cellWidth * 0.55f,
                    center = Offset(targetGoalCenterX, reticleY),
                    style = Stroke(width = 2.5f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.9f),
                    radius = 4f,
                    center = Offset(targetGoalCenterX, reticleY)
                )

                // 3.3.4 Overhead Goal Badge ("לחץ להבקעה!")
                val goalBadgeText = "🥅 לחץ להבקעה! (${shootEval.finalProbability}%)"
                val goalBadgeLayout = textMeasurer.measure(
                    goalBadgeText,
                    TextStyle(color = Color.White, fontSize = (cellWidth * 0.18f).toSp(), fontWeight = FontWeight.Black)
                )
                val gbw = goalBadgeLayout.size.width + 16f
                val gbh = goalBadgeLayout.size.height + 8f
                val gbLeft = targetGoalCenterX - gbw / 2f
                val gbTop = if (selectedPlayer.team == Team.HOME) cellHeight * 0.95f else size.height - (cellHeight * 1.35f)
                drawRoundRect(
                    color = Color(0xFF0F172A).copy(alpha = 0.92f),
                    topLeft = Offset(gbLeft, gbTop),
                    size = Size(gbw, gbh),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                )
                drawRoundRect(
                    color = shootColor,
                    topLeft = Offset(gbLeft, gbTop),
                    size = Size(gbw, gbh),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
                    style = Stroke(width = 1.5f)
                )
                drawText(
                    textLayoutResult = goalBadgeLayout,
                    topLeft = Offset(gbLeft + 8f, gbTop + 4f)
                )

                // 3.3.5 Mid-Trajectory Probability & Distance Breakdown Card
                val midX = (pCx + targetGoalCenterX) / 2f
                val midY = (pCy + targetGoalY) / 2f
                val distFormatted = String.format("%.1f", shootEval.distanceToGoal)
                val probCardText = "🎯 ${shootEval.finalProbability}% • בעיטה: ${shootEval.shootingStat} | מרחק: ${distFormatted} משבצות"
                val probLayout = textMeasurer.measure(
                    probCardText,
                    TextStyle(color = Color.White, fontSize = (cellWidth * 0.16f).toSp(), fontWeight = FontWeight.Bold)
                )
                val pw = probLayout.size.width + 14f
                val ph = probLayout.size.height + 6f
                drawRoundRect(
                    color = Color(0xFF0F172A).copy(alpha = 0.94f),
                    topLeft = Offset(midX - pw / 2f, midY - ph / 2f),
                    size = Size(pw, ph),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(5f, 5f)
                )
                drawRoundRect(
                    color = shootColor,
                    topLeft = Offset(midX - pw / 2f, midY - ph / 2f),
                    size = Size(pw, ph),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(5f, 5f),
                    style = Stroke(width = 1.5f)
                )
                drawText(
                    textLayoutResult = probLayout,
                    topLeft = Offset(midX - pw / 2f + 7f, midY - ph / 2f + 3f)
                )
            }
        }

        // 4. Draw Living 3D Upright Player Figurines with Individual Dynamics & Lifelike Animations
        val currentBallScreenX = animatedBallX * cellWidth + cellWidth / 2
        val currentBallScreenY = animatedBallY * cellHeight + cellHeight / 2
        val animTime = animClockMs.toLong()

        // Sort players by Y coordinate to maintain natural 3D depth sorting (foreground figures overlap background)
        val sortedAnimatedPlayers = animatedPlayers.sortedBy { it.animY * 100f + it.animX }

        sortedAnimatedPlayers.forEach { animData ->
            val player = animData.player
            val groundCx = animData.animX * cellWidth + cellWidth / 2
            val groundCy = animData.animY * cellHeight + cellHeight / 2
            val isActedThisTurn = player.id in gameState.actedPlayerIds && player.team == gameState.currentTurn
            val hasBall = player.id == gameState.playerWithBallId
            val isSelected = player.id == gameState.selectedPlayerId

            // Check if player is threatened / pressed by an opponent in adjacent cell
            val isThreatened = gameState.players.any { opp ->
                opp.team != player.team && !opp.isRedCarded &&
                kotlin.math.abs(opp.position.x - player.position.x) <= 1 &&
                kotlin.math.abs(opp.position.y - player.position.y) <= 1
            }

            drawLivingPlayerFigurine(
                player = player,
                groundCx = groundCx,
                groundCy = groundCy,
                cellWidth = cellWidth,
                cellHeight = cellHeight,
                elevation = animData.elevation,
                isMoving = animData.isMoving,
                isSelected = isSelected,
                hasBall = hasBall,
                ballScreenX = currentBallScreenX,
                ballScreenY = currentBallScreenY,
                animationTimeMs = animTime,
                pulseAlpha = pulseAlpha,
                isActedThisTurn = isActedThisTurn,
                textMeasurer = textMeasurer,
                isThreatened = isThreatened
            )
        }

        // 5. Draw 3D Soccer Ball with Dynamic Trajectory Animation, Elevation, Scale, & Ground Shadow
        val traj = gameState.ballTrajectory
        if (traj != null) {
            val t = trajectoryProgress.value.coerceIn(0f, 1f)
            val currentAltitude = kotlin.math.sin(t * kotlin.math.PI.toFloat()) * traj.arcHeight

            val deltaX = traj.targetX - traj.startX
            val deltaY = traj.targetY - traj.startY
            val lineLen = kotlin.math.sqrt(deltaX * deltaX + deltaY * deltaY).coerceAtLeast(0.01f)
            val perpX = -deltaY / lineLen
            val perpY = deltaX / lineLen
            val curlAmount = kotlin.math.sin(t * kotlin.math.PI.toFloat()) * traj.curveOffset

            val groundBallX = (traj.startX + t * deltaX + perpX * curlAmount) * cellWidth + cellWidth / 2f
            val groundBallY = (traj.startY + t * deltaY + perpY * curlAmount) * cellHeight + cellHeight / 2f

            // Ball Elevation lifts upwards on screen with altitude
            val aerialBallX = groundBallX
            val aerialBallY = groundBallY - currentAltitude * cellHeight * 1.35f
            val dynamicScale = 1.0f + currentAltitude * 0.40f
            val bRadius = cellWidth * 0.18f * dynamicScale

            // Trajectory Theme Color matching calculated probability
            val trajColor = when {
                traj.isShot && traj.isGoal -> Color(0xFFFF5722) // Blazing flame
                traj.isShot -> Color(0xFFF59E0B) // Amber shot
                traj.probability >= 80 -> Color(0xFF10B981) // High confidence emerald
                traj.probability >= 60 -> Color(0xFF38BDF8) // Electric cyan
                traj.probability >= 40 -> Color(0xFFF59E0B) // Contested amber
                else -> Color(0xFFEF4444) // Risky red
            }

            // 5.0 Entire Planned Flight Trajectory Arc (Subtle on turf so user sees the flight path)
            val fullPath = Path()
            val totalSteps = 24
            for (s in 0..totalSteps) {
                val stepT = s / totalSteps.toFloat()
                val stepCurl = kotlin.math.sin(stepT * kotlin.math.PI.toFloat()) * traj.curveOffset
                val stepAlt = kotlin.math.sin(stepT * kotlin.math.PI.toFloat()) * traj.arcHeight
                val stepBaseX = traj.startX + stepT * deltaX
                val stepBaseY = traj.startY + stepT * deltaY
                val stepGx = (stepBaseX + perpX * stepCurl) * cellWidth + cellWidth / 2f
                val stepGy = (stepBaseY + perpY * stepCurl) * cellHeight + cellHeight / 2f
                val stepAy = stepGy - stepAlt * cellHeight * 1.35f
                if (s == 0) fullPath.moveTo(stepGx, stepAy) else fullPath.lineTo(stepGx, stepAy)
            }
            // Ambient trajectory beam glow
            drawPath(
                path = fullPath,
                color = trajColor.copy(alpha = 0.22f),
                style = Stroke(width = bRadius * 1.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            // Trajectory dashed guide wire
            drawPath(
                path = fullPath,
                color = trajColor.copy(alpha = 0.65f),
                style = Stroke(
                    width = 2.2f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                )
            )

            // Landing Target Reticle on Grass
            val targetCx = traj.targetX * cellWidth + cellWidth / 2f
            val targetCy = traj.targetY * cellHeight + cellHeight / 2f
            drawCircle(
                color = trajColor.copy(alpha = (0.50f * (1f - t * 0.35f)).coerceIn(0.15f, 0.5f)),
                radius = cellWidth * 0.45f,
                center = Offset(targetCx, targetCy),
                style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 4f), 0f))
            )

            // Floating Calculated Probability Badge along flight trajectory
            val badgeAlpha = (1f - ((t - 0.70f).coerceAtLeast(0f) / 0.30f)).coerceIn(0f, 1f)
            if (badgeAlpha > 0.05f) {
                val midStepT = 0.5f
                val midCurl = kotlin.math.sin(midStepT * kotlin.math.PI.toFloat()) * traj.curveOffset
                val midAlt = kotlin.math.sin(midStepT * kotlin.math.PI.toFloat()) * traj.arcHeight
                val midGx = (traj.startX + midStepT * deltaX + perpX * midCurl) * cellWidth + cellWidth / 2f
                val midGy = (traj.startY + midStepT * deltaY + perpY * midCurl) * cellHeight + cellHeight / 2f
                val badgeAy = midGy - midAlt * cellHeight * 1.35f - (cellHeight * 0.45f)

                val badgeText = when {
                    traj.isShot && traj.isGoal -> "⚽ שער! ${traj.probability}%"
                    traj.isShot -> "🎯 בעיטה: ${traj.probability}%"
                    traj.isIntercepted -> "⚠️ סיכון: ${traj.probability}%"
                    else -> "⚽ ${traj.probability}%"
                }
                val badgeLayout = textMeasurer.measure(
                    badgeText,
                    TextStyle(
                        color = Color.White,
                        fontSize = (cellWidth * 0.16f).coerceIn(10f, 14f).sp,
                        fontWeight = FontWeight.Black
                    )
                )
                val bw = badgeLayout.size.width + 16f
                val bh = badgeLayout.size.height + 8f
                val bx = midGx - bw / 2f
                val by = badgeAy - bh / 2f

                drawRoundRect(
                    color = Color(0xFF0F172A).copy(alpha = 0.92f * badgeAlpha),
                    topLeft = Offset(bx, by),
                    size = Size(bw, bh),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(bh / 2f, bh / 2f)
                )
                drawRoundRect(
                    color = trajColor.copy(alpha = badgeAlpha),
                    topLeft = Offset(bx, by),
                    size = Size(bw, bh),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(bh / 2f, bh / 2f),
                    style = Stroke(width = 1.6f)
                )
                drawText(
                    textLayoutResult = badgeLayout,
                    topLeft = Offset(bx + 8f, by + 4f),
                    alpha = badgeAlpha
                )
            }

            // 5.1 Glowing Trajectory Streak Trail behind the flying ball
            val trailSteps = 16
            val trailStartT = (t - 0.32f).coerceAtLeast(0f)
            if (t > 0.03f) {
                val trailPath = Path()
                var firstPoint = true
                for (s in 0..trailSteps) {
                    val stepT = trailStartT + (t - trailStartT) * (s / trailSteps.toFloat())
                    val stepAlt = kotlin.math.sin(stepT * kotlin.math.PI.toFloat()) * traj.arcHeight
                    val stepBaseX = traj.startX + stepT * deltaX
                    val stepBaseY = traj.startY + stepT * deltaY
                    val stepCurl = kotlin.math.sin(stepT * kotlin.math.PI.toFloat()) * traj.curveOffset
                    val stepGx = (stepBaseX + perpX * stepCurl) * cellWidth + cellWidth / 2f
                    val stepGy = (stepBaseY + perpY * stepCurl) * cellHeight + cellHeight / 2f
                    val stepAx = stepGx
                    val stepAy = stepGy - stepAlt * cellHeight * 1.35f

                    if (firstPoint) {
                        trailPath.moveTo(stepAx, stepAy)
                        firstPoint = false
                    } else {
                        trailPath.lineTo(stepAx, stepAy)
                    }
                }

                // Outer aura glow
                drawPath(
                    path = trailPath,
                    color = trajColor.copy(alpha = 0.45f),
                    style = Stroke(width = bRadius * 1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                // Crisp core beam
                drawPath(
                    path = trailPath,
                    color = Color.White.copy(alpha = 0.90f),
                    style = Stroke(width = bRadius * 0.7f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            // 5.2 Dynamic Ground Shadow (diffuses and grows as altitude increases)
            val shadowSpread = 1.0f + currentAltitude * 0.85f
            val shadowAlpha = (0.50f / shadowSpread).coerceIn(0.18f, 0.55f)
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Black.copy(alpha = shadowAlpha), Color.Transparent),
                    center = Offset(groundBallX, groundBallY + bRadius * 0.40f),
                    radius = bRadius * 1.4f * shadowSpread
                ),
                topLeft = Offset(groundBallX - bRadius * 1.15f * shadowSpread, groundBallY + bRadius * 0.15f),
                size = Size(bRadius * 2.3f * shadowSpread, bRadius * 0.9f * shadowSpread)
            )

            // Dynamic Ball Aura Glow (matching calculated probability)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(trajColor.copy(alpha = 0.45f), Color.Transparent),
                    center = Offset(aerialBallX, aerialBallY),
                    radius = bRadius * 1.7f
                ),
                radius = bRadius * 1.7f,
                center = Offset(aerialBallX, aerialBallY)
            )

            // 5.3 3D Vector Soccer Ball with Physics Spin
            val ballRadiusPx = (bRadius * 1.05f).coerceAtLeast(8f)
            val spinAngle = t * (if (traj.isShot) 1440f else 960f) * (if (traj.curveOffset >= 0) 1f else -1f)

            // Ball base
            drawCircle(
                color = Color.White,
                radius = ballRadiusPx,
                center = Offset(aerialBallX, aerialBallY)
            )

            // Rotating pentagon
            val pentagonPath = Path().apply {
                val pRadius = ballRadiusPx * 0.40f
                for (i in 0 until 5) {
                    val angle = (i * 72f - 90f + spinAngle) * (kotlin.math.PI / 180f).toFloat()
                    val px = aerialBallX + pRadius * kotlin.math.cos(angle)
                    val py = aerialBallY + pRadius * kotlin.math.sin(angle)
                    if (i == 0) moveTo(px, py) else lineTo(px, py)
                }
                close()
            }
            drawPath(path = pentagonPath, color = Color(0xFF1E293B))

            // Seam lines with spin
            val seamColor = Color(0xFF64748B)
            for (i in 0 until 5) {
                val angle = (i * 72f - 90f + spinAngle) * (kotlin.math.PI / 180f).toFloat()
                val innerX = aerialBallX + ballRadiusPx * 0.40f * kotlin.math.cos(angle)
                val innerY = aerialBallY + ballRadiusPx * 0.40f * kotlin.math.sin(angle)
                val outerX = aerialBallX + ballRadiusPx * 0.95f * kotlin.math.cos(angle)
                val outerY = aerialBallY + ballRadiusPx * 0.95f * kotlin.math.sin(angle)
                drawLine(
                    color = seamColor,
                    start = Offset(innerX, innerY),
                    end = Offset(outerX, outerY),
                    strokeWidth = 1.2f
                )
            }

            // Crisp outer rim
            drawCircle(
                color = Color(0xFF0F172A).copy(alpha = 0.6f),
                radius = ballRadiusPx,
                center = Offset(aerialBallX, aerialBallY),
                style = Stroke(width = 1.2f)
            )

            // Specular gloss reflection highlight
            drawCircle(
                color = Color.White.copy(alpha = 0.75f),
                radius = ballRadiusPx * 0.22f,
                center = Offset(aerialBallX - ballRadiusPx * 0.35f, aerialBallY - ballRadiusPx * 0.35f)
            )

            // Goal Net Impact & Ripple Effect
            if (traj.isGoal && t > 0.82f) {
                val impactProgress = (t - 0.82f) / 0.18f
                drawCircle(
                    color = Color(0xFFFFD700).copy(alpha = (1f - impactProgress) * 0.8f),
                    radius = cellWidth * (0.4f + impactProgress * 1.2f),
                    center = Offset(aerialBallX, aerialBallY),
                    style = Stroke(width = 3.5f)
                )
            } else if (t > 0.88f) {
                // Turf landing arrival ripple
                val landProgress = (t - 0.88f) / 0.12f
                drawCircle(
                    color = trajColor.copy(alpha = (1f - landProgress) * 0.6f),
                    radius = cellWidth * (0.25f + landProgress * 0.55f),
                    center = Offset(groundBallX, groundBallY),
                    style = Stroke(width = 2.0f)
                )
            }
        } else if (gameState.ballPosition != null || gameState.playerWithBallId != null) {
            val ballCx = animatedBallX * cellWidth + cellWidth / 2
            val ballCy = animatedBallY * cellHeight + cellHeight / 2
            val offsetX = animatedBallOffsetX * cellWidth
            val offsetY = animatedBallOffsetX * cellHeight
            val groundBallX = ballCx + offsetX
            val groundBallY = ballCy + offsetY
            val bRadius = cellWidth * animatedBallRadius * animatedBallScale

            // Ball Elevation: when ball is in mid-air (scale > 1.0), it lifts upwards on screen
            val ballAltitudeOffset = (animatedBallScale - 1.0f) * cellHeight * 0.85f
            val aerialBallX = groundBallX
            val aerialBallY = groundBallY - ballAltitudeOffset

            // 5.1 Dynamic Ground Shadow (spreads out and diffuses as altitude increases)
            val shadowSpread = 1.0f + (animatedBallScale - 1.0f) * 0.6f
            val shadowAlpha = (0.55f / (shadowSpread * 0.9f)).coerceIn(0.2f, 0.6f)
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Black.copy(alpha = shadowAlpha), Color.Transparent),
                    center = Offset(groundBallX, groundBallY + bRadius * 0.45f),
                    radius = bRadius * 1.4f * shadowSpread
                ),
                topLeft = Offset(groundBallX - bRadius * 1.1f * shadowSpread, groundBallY + bRadius * 0.2f),
                size = Size(bRadius * 2.2f * shadowSpread, bRadius * 0.9f * shadowSpread)
            )

            // 5.2 Pure Vector Soccer Ball (Clean, sharp, no black rectangular JPEG artifacts)
            val ballRadiusPx = (bRadius * 1.05f).coerceAtLeast(8f)

            // Ball base (White leather sphere)
            drawCircle(
                color = Color.White,
                radius = ballRadiusPx,
                center = Offset(aerialBallX, aerialBallY)
            )

            // Center classic pentagon
            val pentagonPath = Path().apply {
                val pRadius = ballRadiusPx * 0.40f
                for (i in 0 until 5) {
                    val angle = (i * 72f - 90f) * (kotlin.math.PI / 180f).toFloat()
                    val px = aerialBallX + pRadius * kotlin.math.cos(angle)
                    val py = aerialBallY + pRadius * kotlin.math.sin(angle)
                    if (i == 0) moveTo(px, py) else lineTo(px, py)
                }
                close()
            }
            drawPath(path = pentagonPath, color = Color(0xFF1E293B))

            // Seam lines connecting center pentagon outward
            val seamColor = Color(0xFF64748B)
            for (i in 0 until 5) {
                val angle = (i * 72f - 90f) * (kotlin.math.PI / 180f).toFloat()
                val innerX = aerialBallX + ballRadiusPx * 0.40f * kotlin.math.cos(angle)
                val innerY = aerialBallY + ballRadiusPx * 0.40f * kotlin.math.sin(angle)
                val outerX = aerialBallX + ballRadiusPx * 0.95f * kotlin.math.cos(angle)
                val outerY = aerialBallY + ballRadiusPx * 0.95f * kotlin.math.sin(angle)
                drawLine(
                    color = seamColor,
                    start = Offset(innerX, innerY),
                    end = Offset(outerX, outerY),
                    strokeWidth = 1.2f
                )
            }

            // Crisp outer rim
            drawCircle(
                color = Color(0xFF0F172A).copy(alpha = 0.6f),
                radius = ballRadiusPx,
                center = Offset(aerialBallX, aerialBallY),
                style = Stroke(width = 1.2f)
            )

            // Specular gloss reflection highlight (top-left glint)
            drawCircle(
                color = Color.White.copy(alpha = 0.7f),
                radius = ballRadiusPx * 0.22f,
                center = Offset(aerialBallX - ballRadiusPx * 0.35f, aerialBallY - ballRadiusPx * 0.35f)
            )
        }

        // 6. Draw Goalkeeper Save & Dive Animation Effect
        gameState.gkSaveEffect?.let { save ->
            val gkPlayer = gameState.players.find { it.id == save.keeperId }
            if (gkPlayer != null) {
                val gkX = gkPlayer.position.x * cellWidth + cellWidth / 2
                val gkY = gkPlayer.position.y * cellHeight + cellHeight / 2
                val targetX = save.diveTargetPos.x * cellWidth + cellWidth / 2
                val targetY = save.diveTargetPos.y * cellHeight + cellHeight / 2

                // GK Dive streak / shockwave
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF38BDF8).copy(alpha = 0.9f), Color(0xFF0284C7).copy(alpha = 0.2f)),
                        start = Offset(gkX, gkY),
                        end = Offset(targetX, targetY)
                    ),
                    start = Offset(gkX, gkY),
                    end = Offset(targetX, targetY),
                    strokeWidth = 14f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )

                // Impact burst rings
                drawCircle(
                    color = Color(0xFF38BDF8).copy(alpha = pulseAlpha),
                    radius = cellWidth * 0.9f,
                    center = Offset(targetX, targetY),
                    style = Stroke(width = 4f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.8f),
                    radius = cellWidth * 0.45f,
                    center = Offset(targetX, targetY),
                    style = Stroke(width = 3f)
                )

                // Glowing glove icon / text
                val saveTextLayout = textMeasurer.measure(
                    text = "🧤 SAVE!",
                    style = TextStyle(
                        color = Color(0xFF38BDF8),
                        fontSize = (cellWidth * 0.28f).toSp(),
                        fontWeight = FontWeight.Black
                    )
                )
                val saveTextSize = saveTextLayout.size
                drawText(
                    textLayoutResult = saveTextLayout,
                    topLeft = Offset(targetX - saveTextSize.width / 2f, targetY - cellHeight * 0.6f)
                )
            }
        }

        // 7. Draw Set-Piece (Corner / Throw-In) Visual Highlights & Targets
        gameState.activeSetPiece?.let { sp ->
            val taker = gameState.players.find { it.id == sp.takerPlayerId }
            val spX = sp.ballPosition.x * cellWidth + cellWidth / 2
            val spY = sp.ballPosition.y * cellHeight + cellHeight / 2
            val isCorner = sp.type == SetPieceType.CORNER
            val spColor = if (isCorner) Color(0xFFFBBF24) else Color(0xFF34D399)
            val spIconText = if (isCorner) "🚩 CORNER" else "🤾 THROW-IN"

            // Pulsing target beacon at the set-piece spot
            drawCircle(
                color = spColor.copy(alpha = pulseAlpha * 0.8f),
                radius = cellWidth * 0.85f,
                center = Offset(spX, spY),
                style = Stroke(width = 5f)
            )
            drawCircle(
                color = spColor.copy(alpha = 0.35f),
                radius = cellWidth * 0.6f,
                center = Offset(spX, spY)
            )

            // Badge text on pitch
            val labelLayout = textMeasurer.measure(
                text = spIconText,
                style = TextStyle(
                    color = spColor,
                    fontSize = (cellWidth * 0.24f).toSp(),
                    fontWeight = FontWeight.ExtraBold
                )
            )
            val labelSize = labelLayout.size
            val labelY = if (sp.ballPosition.y <= 2) spY + cellHeight * 0.5f else spY - cellHeight * 0.8f
            drawText(
                textLayoutResult = labelLayout,
                topLeft = Offset((spX - labelSize.width / 2f).coerceIn(4f, size.width - labelSize.width - 4f), labelY)
            )

            // Delivery guide arcs into the penalty box for corners, or along sidelines for throw-in
            if (isCorner) {
                val boxCenterY = if (sp.ballPosition.y <= 2) cellHeight * 2.5f else size.height - (cellHeight * 2.5f)
                val boxCenterX = size.width / 2f
                drawLine(
                    color = spColor.copy(alpha = 0.6f),
                    start = Offset(spX, spY),
                    end = Offset(boxCenterX, boxCenterY),
                    strokeWidth = 3.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                )
                drawCircle(
                    color = spColor.copy(alpha = 0.7f),
                    radius = 8f,
                    center = Offset(boxCenterX, boxCenterY)
                )
            }
        }
    }
}

@Composable
fun RadialActionMenu(
    centerX: androidx.compose.ui.unit.Dp,
    centerY: androidx.compose.ui.unit.Dp,
    previewActions: List<ActionOption>,
    selectedAction: ActionType?,
    onActionSelected: (ActionType) -> Unit,
    onCancel: () -> Unit
) {
    val radius = 72.dp
    val btnSize = 56.dp
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onCancel() })
            }
    ) {
        // Center Cancel Button
        Surface(
            modifier = Modifier
                .offset(x = centerX - (btnSize / 2), y = centerY - (btnSize / 2))
                .size(btnSize)
                .clickable { onCancel() }
                .zIndex(10f),
            shape = CircleShape,
            color = Color.Red.copy(alpha = 0.8f),
            shadowElevation = 8.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("X", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        }

        previewActions.forEachIndexed { index, option ->
            val angle = Math.toRadians((index * (360.0 / previewActions.size)) - 90.0)
            
            val offsetX = centerX + (radius * Math.cos(angle).toFloat()) - (btnSize / 2)
            val offsetY = centerY + (radius * Math.sin(angle).toFloat()) - (btnSize / 2)
            
            val isSelected = option.actionType == selectedAction
            
            val shortName = when(option.actionType) {
                ActionType.MOVE -> "תנועה"
                ActionType.PASS -> "מסירה"
                ActionType.SHOOT -> "בעיטה"
                ActionType.DRIBBLE -> "דריבל"
                ActionType.TACKLE -> "תיקול"
            }
            
            val actionColor = when(option.actionType) {
                ActionType.MOVE -> Color(0xFF22C55E)
                ActionType.PASS -> Color(0xFF38BDF8)
                ActionType.SHOOT -> Color(0xFFEF4444)
                ActionType.DRIBBLE -> Color(0xFFF59E0B)
                ActionType.TACKLE -> Color(0xFFA855F7)
            }
            
            Surface(
                modifier = Modifier
                    .offset(x = offsetX, y = offsetY)
                    .size(btnSize)
                    .clickable { onActionSelected(option.actionType) },
                shape = CircleShape,
                color = if (isSelected) actionColor else Color(0xFF0F172A).copy(alpha = 0.95f),
                shadowElevation = if (isSelected) 12.dp else 6.dp,
                border = androidx.compose.foundation.BorderStroke(
                    if (isSelected) 2.5.dp else 1.5.dp, 
                    if (isSelected) Color.White else actionColor.copy(alpha = 0.8f)
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(2.dp)
                ) {
                    Text(
                        text = shortName,
                        color = if (isSelected) Color.Black else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = option.successProbability,
                        color = if (isSelected) Color.Black else actionColor,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun PlayerCard(player: Player) {
    val roleColor = when (player.role) {
        PlayerRole.ATTACKER -> Color(0xFFEF4444) // Red
        PlayerRole.MIDFIELDER -> Color(0xFF3B82F6) // Blue
        PlayerRole.DEFENDER -> Color(0xFF10B981) // Green
        PlayerRole.GOALKEEPER -> Color(0xFFF59E0B) // Amber
    }

    val roleName = when (player.role) {
        PlayerRole.ATTACKER -> "חלוץ"
        PlayerRole.MIDFIELDER -> "קשר"
        PlayerRole.DEFENDER -> "בלם"
        PlayerRole.GOALKEEPER -> "שוער"
    }

    val figurineRes = when {
        player.role == PlayerRole.GOALKEEPER -> R.drawable.img_player_gk_1790244917264
        player.team == Team.HOME -> R.drawable.img_player_home_1790244884615
        else -> R.drawable.img_player_away_1790244902512
    }

    Surface(
        color = Color(0xFF0F172A).copy(alpha = 0.95f),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, Brush.horizontalGradient(listOf(roleColor, Color(0xFF38BDF8)))),
        shadowElevation = 16.dp,
        modifier = Modifier.padding(14.dp).fillMaxWidth(0.96f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Player figurine avatar + Name & Number
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B))
                        .border(2.dp, roleColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = figurineRes),
                        contentDescription = player.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = roleColor.copy(alpha = 0.25f),
                            shape = CircleShape
                        ) {
                            Text(
                                text = "${player.roleIcon} #${player.number}",
                                color = roleColor,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = player.name,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${player.roleIcon} $roleName",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        player.trait?.let { trait ->
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${trait.icon} ${trait.name}",
                                color = Color(0xFF38BDF8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Stats grid (Pill badges)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatColumn(label = "SPD", value = player.currentSpeed)
                StatColumn(label = "PAS", value = player.currentPassing)
                StatColumn(label = "SHT", value = player.currentShooting)
                StatColumn(label = "DEF", value = player.currentDefense)
            }
        }
    }
}

@Composable
fun StatColumn(label: String, value: Int) {
    val valueColor = when {
        value >= 85 -> Color(0xFF10B981) // Green
        value >= 70 -> Color(0xFFFDE047) // Yellow
        else -> Color(0xFFEF4444) // Red
    }
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toString(),
            color = valueColor,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun TacticalCommandDeck(
    player: Player,
    gameState: GameState,
    onActionSelected: (ActionType) -> Unit,
    onEndTurn: () -> Unit,
    onDismiss: () -> Unit
) {
    val roleColor = when (player.role) {
        PlayerRole.ATTACKER -> Color(0xFFEF4444)
        PlayerRole.MIDFIELDER -> Color(0xFF3B82F6)
        PlayerRole.DEFENDER -> Color(0xFF10B981)
        PlayerRole.GOALKEEPER -> Color(0xFFF59E0B)
    }

    val roleName = when (player.role) {
        PlayerRole.ATTACKER -> "חלוץ"
        PlayerRole.MIDFIELDER -> "קשר"
        PlayerRole.DEFENDER -> "בלם"
        PlayerRole.GOALKEEPER -> "שוער"
    }

    val figurineRes = when {
        player.role == PlayerRole.GOALKEEPER -> R.drawable.img_player_gk_1790244917264
        player.team == Team.HOME -> R.drawable.img_player_home_1790244884615
        else -> R.drawable.img_player_away_1790244902512
    }

    val isHomeTurn = gameState.currentTurn == Team.HOME

    Surface(
        color = Color(0xFF0F172A).copy(alpha = 0.96f),
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
        border = BorderStroke(2.dp, Brush.horizontalGradient(listOf(roleColor, Color(0xFF38BDF8), roleColor))),
        shadowElevation = 20.dp,
        modifier = Modifier
            .fillMaxWidth(0.96f)
            .padding(horizontal = 4.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            // Top Row: Player Info & Attribute Badges & Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Figurine + Name + Number + Role
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                            .border(2.dp, roleColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = figurineRes),
                            contentDescription = player.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = roleColor.copy(alpha = 0.3f),
                                shape = CircleShape
                            ) {
                                Text(
                                    text = "#${player.number}",
                                    color = roleColor,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = player.name,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = roleName,
                                color = Color.LightGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            player.trait?.let { trait ->
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${trait.icon} ${trait.name}",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            val coordLabel = PitchGridConfig.getCoordinateLabel(player.position.x, player.position.y)
                            Surface(
                                color = Color(0xFF1E293B),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, Color(0xFF00F5FF).copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "📍 $coordLabel",
                                    color = Color(0xFF00F5FF),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            val staminaColor = when {
                                player.stamina > 60 -> Color(0xFF10B981)
                                player.stamina > 30 -> Color(0xFFF59E0B)
                                else -> Color(0xFFEF4444)
                            }
                            Text(
                                text = "כושר: ${player.stamina}%",
                                color = staminaColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Attributes (PAC, PAS, SHT, DEF) + Close Button
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    StatBadge(label = "PAC", value = player.currentPac, color = Color(0xFF10B981))
                    StatBadge(label = "PAS", value = player.currentPas, color = Color(0xFF38BDF8))
                    StatBadge(label = "SHT", value = player.currentSht, color = Color(0xFFEF4444))
                    StatBadge(label = "DEF", value = player.currentDef, color = Color(0xFFF59E0B))
                    
                    // Close button
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Surface(shape = CircleShape, color = Color(0xFF334155)) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("✕", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Action Buttons Row (The Core Football, Tactics & Glory Action Bar)
            if (isHomeTurn) {
                val actions = gameState.previewActions

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Action Buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        actions.forEach { opt ->
                            val isSelected = opt.actionType == gameState.selectedAction
                            val (actionLabel, actionColor) = when(opt.actionType) {
                                ActionType.MOVE -> "🏃 תנועה" to Color(0xFF22C55E)
                                ActionType.PASS -> "⚽ מסירה" to Color(0xFF38BDF8)
                                ActionType.SHOOT -> "🎯 בעיטה" to Color(0xFFEF4444)
                                ActionType.DRIBBLE -> "⚡ כדרור" to Color(0xFFF59E0B)
                                ActionType.TACKLE -> "🛡️ תיקול" to Color(0xFFA855F7)
                            }

                            Button(
                                onClick = { onActionSelected(opt.actionType) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) actionColor else Color(0xFF1E293B)
                                ),
                                shape = RoundedCornerShape(9.dp),
                                border = BorderStroke(
                                    1.5.dp, 
                                    if (isSelected) Color.White else actionColor.copy(alpha = 0.6f)
                                ),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 3.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = actionLabel,
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (opt.successProbability.isNotEmpty()) {
                                        Text(
                                            text = opt.successProbability,
                                            color = if (isSelected) Color.Black else actionColor,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // End Turn Button right on the command deck
                    Button(
                        onClick = onEndTurn,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB45309)),
                        shape = RoundedCornerShape(9.dp),
                        border = BorderStroke(1.dp, Color(0xFFFBBF24)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("🛑 סיום תור", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun StatBadge(label: String, value: Int, color: Color) {
    Surface(
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
        ) {
            Text(label, color = color, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Text("$value", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun TacticalDuelOverlay(
    duel: TacticalDuelState,
    onDismiss: () -> Unit
) {
    // Auto dismiss after 2.6 seconds
    LaunchedEffect(duel.timestamp) {
        delay(2600)
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clickable(enabled = false) {},
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(2.5.dp, Brush.horizontalGradient(
                listOf(
                    if (duel.isAttackerWin) Color(0xFF22C55E) else Color(0xFFEF4444),
                    Color(0xFF38BDF8),
                    if (duel.isAttackerWin) Color(0xFFEF4444) else Color(0xFF22C55E)
                )
            )),
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header badge
                Surface(
                    color = (if (duel.isAttackerWin) Color(0xFF22C55E) else Color(0xFFEF4444)).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (duel.isAttackerWin) Color(0xFF22C55E) else Color(0xFFEF4444))
                ) {
                    Text(
                        text = duel.title,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Clash Cards: Attacker vs Defender
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Attacker Card
                    DuelFighterCard(
                        name = duel.attackerName,
                        number = duel.attackerNumber,
                        role = duel.attackerRole,
                        statName = duel.attackerStatName,
                        statVal = duel.attackerStatVal,
                        rollVal = duel.attackerRoll,
                        isWinner = duel.isAttackerWin,
                        accentColor = Color(0xFF38BDF8)
                    )

                    // VS Clash emblem
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFF59E0B),
                            border = BorderStroke(2.dp, Color.White),
                            shadowElevation = 8.dp
                        ) {
                            Text(
                                text = "VS",
                                color = Color.Black,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Defender Card
                    DuelFighterCard(
                        name = duel.defenderName,
                        number = duel.defenderNumber,
                        role = duel.defenderRole,
                        statName = duel.defenderStatName,
                        statVal = duel.defenderStatVal,
                        rollVal = duel.defenderRoll,
                        isWinner = !duel.isAttackerWin,
                        accentColor = Color(0xFFF43F5E)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Power comparison bar
                val totalAttackerPower = duel.attackerStatVal + duel.attackerRoll
                val totalDefenderPower = duel.defenderStatVal + duel.defenderRoll
                val sumPower = (totalAttackerPower + totalDefenderPower).coerceAtLeast(1)
                val attackerRatio = totalAttackerPower.toFloat() / sumPower

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "עוצמה: $totalAttackerPower",
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "עוצמה: $totalDefenderPower",
                            color = Color(0xFFF43F5E),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color(0xFF1E293B))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(attackerRatio)
                                .fillMaxHeight()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF38BDF8), Color(0xFF22C55E))
                                    )
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Result announcement
                Text(
                    text = duel.resultTitle,
                    color = if (duel.isAttackerWin) Color(0xFF4ADE80) else Color(0xFFF87171),
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp
                )
                Text(
                    text = duel.resultSubtitle,
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Dismiss Button
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("המשך במשחק ↵", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun DuelFighterCard(
    name: String,
    number: Int,
    role: PlayerRole,
    statName: String,
    statVal: Int,
    rollVal: Int,
    isWinner: Boolean,
    accentColor: Color
) {
    Surface(
        color = if (isWinner) accentColor.copy(alpha = 0.15f) else Color(0xFF1E293B),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            if (isWinner) 2.dp else 1.dp,
            if (isWinner) accentColor else Color(0xFF475569)
        ),
        modifier = Modifier.width(135.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "#$number",
                color = accentColor,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp
            )
            Text(
                text = name,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = statName,
                color = Color.LightGray,
                fontSize = 11.sp
            )
            Text(
                text = "$statVal",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp
            )
            Text(
                text = "+$rollVal (גלגול)",
                color = Color(0xFFFBBF24),
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                color = accentColor.copy(alpha = 0.3f),
                shape = CircleShape
            ) {
                Text(
                    text = "${statVal + rollVal}",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun HalfTimeOverlay(
    gameState: GameState,
    onStartSecondHalf: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .zIndex(150f)
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(0xFF0F172A),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF38BDF8)),
            shadowElevation = 24.dp,
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "⏸️ שריקה למחצית",
                    color = Color(0xFF38BDF8),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "45 דקות הגיעו לסיומן",
                    color = Color.Gray,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Half Time Score Banner
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("ישראל", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("${gameState.homeScore}", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, fontFamily = FontFamily.Monospace)
                        }
                        Text("-", color = Color.Gray, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("איטליה", color = Color(0xFF93C5FD), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("${gameState.awayScore}", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Mid-match brief stats
                StatsRow(label = "בעיטות לשער", homeVal = gameState.stats.homeShots, awayVal = gameState.stats.awayShots)
                Spacer(modifier = Modifier.height(6.dp))
                StatsRow(label = "מסירות", homeVal = gameState.stats.homePasses, awayVal = gameState.stats.awayPasses)
                Spacer(modifier = Modifier.height(6.dp))
                StatsRow(label = "עבירות", homeVal = gameState.stats.homeFouls, awayVal = gameState.stats.awayFouls)

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onStartSecondHalf,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = "שריקת פתיחה למחצית 2 ▶️",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun MatchOverOverlay(
    gameState: GameState,
    onRestartMatch: () -> Unit,
    onBackToMenu: () -> Unit
) {
    val isArcade = gameState.gameMode == GameMode.ARCADE
    val winnerText = when {
        isArcade && gameState.homeScore >= 3 -> "🏆 ניצחון מוחץ לישראל בארקייד! (3 שערים)"
        isArcade && gameState.awayScore >= 3 -> "🏆 נבחרת איטליה מנצחת בארקייד! (3 שערים)"
        gameState.homeScore > gameState.awayScore -> "🏆 נבחרת ישראל מנצחת!"
        gameState.awayScore > gameState.homeScore -> "🏆 נבחרת איטליה מנצחת!"
        else -> "🤝 תיקו דרמטי!"
    }
    val winnerColor = when {
        gameState.homeScore > gameState.awayScore -> Color(0xFF38BDF8)
        gameState.awayScore > gameState.homeScore -> Color(0xFF93C5FD)
        else -> Color.White
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
            .zIndex(200f)
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(0xFF0F172A),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(2.5.dp, Brush.horizontalGradient(listOf(Color(0xFFFFD700), Color(0xFF38BDF8)))),
            shadowElevation = 32.dp,
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "🏁 סיום המשחק - 90'",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = winnerText,
                    color = winnerColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Final Score Banner
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("ישראל", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("${gameState.homeScore}", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 36.sp, fontFamily = FontFamily.Monospace)
                        }
                        Text("FINAL", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("איטליה", color = Color(0xFF93C5FD), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("${gameState.awayScore}", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 36.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Full Match Stats Table
                Surface(
                    color = Color(0xFF1E293B).copy(alpha = 0.6f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        StatsRow(label = "בעיטות", homeVal = gameState.stats.homeShots, awayVal = gameState.stats.awayShots)
                        Spacer(modifier = Modifier.height(6.dp))
                        StatsRow(label = "למסגרת", homeVal = gameState.stats.homeShotsOnTarget, awayVal = gameState.stats.awayShotsOnTarget)
                        Spacer(modifier = Modifier.height(6.dp))
                        StatsRow(label = "מסירות", homeVal = gameState.stats.homePasses, awayVal = gameState.stats.awayPasses)
                        Spacer(modifier = Modifier.height(6.dp))
                        StatsRow(label = "תיקולים", homeVal = gameState.stats.homeTackles, awayVal = gameState.stats.awayTackles)
                        Spacer(modifier = Modifier.height(6.dp))
                        StatsRow(label = "עבירות", homeVal = gameState.stats.homeFouls, awayVal = gameState.stats.awayFouls)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onRestartMatch,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Text(
                        text = "🔄 משחק חוזר",
                        color = Color(0xFF0F172A),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onBackToMenu,
                    border = BorderStroke(1.2.dp, Color(0xFF38BDF8)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Text(
                        text = "🏠 חזרה לתפריט הראשי",
                        color = Color(0xFF38BDF8),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun StatsRow(label: String, homeVal: Int, awayVal: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = homeVal.toString(),
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(36.dp)
        )
        Text(
            text = label,
            color = Color.LightGray,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = awayVal.toString(),
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(36.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}


@Composable
fun PregameScreen(
    onStartGame: (Formation) -> Unit,
    onBackToMenu: () -> Unit
) {
    var selectedFormation by remember { mutableStateOf(Formation.F442) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B132B))
    ) {
        // Background Stadium Image
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = R.drawable.img_stadium_banner_1790244952797),
            contentDescription = "Stadium",
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.42f)
                .align(Alignment.TopCenter),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )

        // Gradient fade over stadium image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.42f)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xFF0B132B).copy(alpha = 0.85f), Color(0xFF0B132B))
                    )
                )
                .align(Alignment.TopCenter)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header content with Back Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Back Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Surface(
                        color = Color(0xFF1E293B).copy(alpha = 0.85f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF64748B)),
                        modifier = Modifier.clickable { onBackToMenu() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Dashboard,
                                contentDescription = "תפריט ראשי",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("חזרה לתפריט", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = Color(0xFF0F172A).copy(alpha = 0.85f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.5.dp, Color(0xFF38BDF8).copy(alpha = 0.6f)),
                    shadowElevation = 12.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🏆", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ליגת האומות - משחק עונה טקטי",
                            color = Color(0xFF38BDF8),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "קרב טקטי יבשתי",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "ישראל  ⚔️  איטליה",
                    color = Color(0xFFFDE047),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Formation choices
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "בחר מערך טקטי פותח",
                    color = Color.LightGray,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Formation.entries.forEach { formation ->
                    val isSelected = formation == selectedFormation
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .padding(vertical = 5.dp)
                            .clickable { selectedFormation = formation },
                        color = if (isSelected) Color(0xFF0284C7) else Color(0xFF1E293B).copy(alpha = 0.85f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            2.dp,
                            if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155)
                        ),
                        shadowElevation = if (isSelected) 10.dp else 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isSelected) "⚡" else "📋",
                                    fontSize = 20.sp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = formation.displayName,
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = when(formation) {
                                            Formation.F442 -> "מערך מאוזן קלאסי (הגנה והתקפה)"
                                            Formation.F433 -> "שלישייה קדמית חזקה ולחץ גבוה"
                                            Formation.F532 -> "שלישיית בלמים להגנה מבוצרת ומתפרצות"
                                        },
                                        color = if (isSelected) Color(0xFFE0F2FE) else Color.Gray,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            if (isSelected) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White,
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Filled.Check,
                                            contentDescription = "נבחר",
                                            tint = Color(0xFF0284C7),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Start Match Button
            Button(
                onClick = { onStartGame(selectedFormation) },
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .height(60.dp)
                    .padding(bottom = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                shape = RoundedCornerShape(18.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 12.dp)
            ) {
                Text(
                    text = "עלה לכר הדשא ⚽",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.width(10.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "התחל",
                    tint = Color.White
                )
            }
        }
    }
}
