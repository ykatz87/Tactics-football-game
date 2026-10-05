package com.example.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.Player
import com.example.PlayerRole
import com.example.Team

enum class PlayerActionStance {
    IDLE,
    RUNNING,
    KICKING,
    SLIDE_TACKLE,
    GK_DIVE_LEFT,
    GK_DIVE_RIGHT,
    CELEBRATION
}

enum class HairStyle {
    SHORT,
    BUZZ_CUT,
    FLOWING_WAVY,
    CURLY,
    QUIFF
}

/**
 * Metadata for individual player appearance and identity
 */
data class PlayerVisualIdentity(
    val personalityMotto: String = "מוכן לקרב!",
    val heightScale: Float = 1.0f,
    val skinTone: Color = Color(0xFFFDBA74),
    val hairColor: Color = Color(0xFF261C14),
    val hairStyle: HairStyle = HairStyle.SHORT,
    val hasBeard: Boolean = false,
    val isCaptain: Boolean = false
)

object PlayerVisualRegistry {
    fun getIdentity(player: Player): PlayerVisualIdentity {
        return when (player.name) {
            "זהבי" -> PlayerVisualIdentity(
                personalityMotto = "סיומת קטלנית 🎯",
                heightScale = 1.04f,
                skinTone = Color(0xFFFDBA74),
                hairColor = Color(0xFF1F1F1F),
                hairStyle = HairStyle.SHORT,
                hasBeard = true,
                isCaptain = true
            )
            "דסה" -> PlayerVisualIdentity(
                personalityMotto = "פריצה באגף 🏃‍♂️",
                heightScale = 1.0f,
                skinTone = Color(0xFF8D5524),
                hairColor = Color(0xFF0F0F0F),
                hairStyle = HairStyle.BUZZ_CUT,
                isCaptain = true
            )
            "סולומון" -> PlayerVisualIdentity(
                personalityMotto = "כדרור מפרק ⚡",
                heightScale = 0.98f,
                skinTone = Color(0xFFFDBA74),
                hairColor = Color(0xFF2E1C12),
                hairStyle = HairStyle.QUIFF
            )
            "גלוך" -> PlayerVisualIdentity(
                personalityMotto = "ראיית משחק וקסם 🪄",
                heightScale = 0.96f,
                skinTone = Color(0xFFFDBA74),
                hairColor = Color(0xFF382316),
                hairStyle = HairStyle.CURLY
            )
            "פרץ" -> PlayerVisualIdentity(
                personalityMotto = "מנוע מרכז השדה 🔋",
                heightScale = 1.08f,
                skinTone = Color(0xFFFDBA74),
                hairColor = Color(0xFF1E1712),
                hairStyle = HairStyle.SHORT
            )
            "לביא", "נטע לביא" -> PlayerVisualIdentity(
                personalityMotto = "גרזן במרכז ⚓",
                heightScale = 1.02f,
                skinTone = Color(0xFFFDBA74),
                hairColor = Color(0xFF221811),
                hairStyle = HairStyle.SHORT,
                hasBeard = true
            )
            "ויטור" -> PlayerVisualIdentity(
                personalityMotto = "חומה בצורה 🛡️",
                heightScale = 1.08f,
                skinTone = Color(0xFFFDBA74),
                hairColor = Color(0xFF261C14),
                hairStyle = HairStyle.SHORT,
                hasBeard = true
            )
            "גולדברג" -> PlayerVisualIdentity(
                personalityMotto = "תיקול מדויק 🛡️",
                heightScale = 1.04f,
                skinTone = Color(0xFFFDBA74),
                hairColor = Color(0xFF332014),
                hairStyle = HairStyle.SHORT
            )
            "רביבו" -> PlayerVisualIdentity(
                personalityMotto = "תמיכה שוטפת ⚡",
                heightScale = 1.0f,
                skinTone = Color(0xFFFDBA74),
                hairColor = Color(0xFF261C14),
                hairStyle = HairStyle.QUIFF
            )
            "תורג'מן" -> PlayerVisualIdentity(
                personalityMotto = "עוצמה וסיומת 💥",
                heightScale = 1.03f,
                skinTone = Color(0xFFFDBA74),
                hairColor = Color(0xFF1E1712),
                hairStyle = HairStyle.SHORT
            )
            "חלאילי" -> PlayerVisualIdentity(
                personalityMotto = "מהירות מסחררת 🌪️",
                heightScale = 1.01f,
                skinTone = Color(0xFFD69460),
                hairColor = Color(0xFF1A1A1A),
                hairStyle = HairStyle.SHORT
            )
            "גלזר" -> PlayerVisualIdentity(
                personalityMotto = "שער נעול 🧤",
                heightScale = 1.08f,
                skinTone = Color(0xFFFDBA74),
                hairColor = Color(0xFF261C14),
                hairStyle = HairStyle.SHORT
            )
            "נחמיאס" -> PlayerVisualIdentity(
                personalityMotto = "סגירה הרמטית 🧱",
                heightScale = 1.05f,
                skinTone = Color(0xFFFDBA74),
                hairColor = Color(0xFF2A1C14),
                hairStyle = HairStyle.SHORT
            )
            "בריבו" -> PlayerVisualIdentity(
                personalityMotto = "לחץ בלתי פוסק 🐾",
                heightScale = 1.02f,
                skinTone = Color(0xFFFDBA74),
                hairColor = Color(0xFF261C14),
                hairStyle = HairStyle.SHORT
            )
            "Donnarumma" -> PlayerVisualIdentity(
                personalityMotto = "No entry! 🧤",
                heightScale = 1.15f,
                skinTone = Color(0xFFFDBA74),
                hairColor = Color(0xFF221811),
                hairStyle = HairStyle.SHORT,
                hasBeard = true
            )
            "Calafiori" -> PlayerVisualIdentity(
                personalityMotto = "Forward elegance 💫",
                heightScale = 1.07f,
                skinTone = Color(0xFFFDBA74),
                hairColor = Color(0xFF3E2723),
                hairStyle = HairStyle.FLOWING_WAVY
            )
            "Dimarco" -> PlayerVisualIdentity(
                personalityMotto = "Wicked left foot 🎯",
                heightScale = 0.97f,
                skinTone = Color(0xFFFDBA74),
                hairColor = Color(0xFFD4AF37), // Blonde buzz cut
                hairStyle = HairStyle.BUZZ_CUT
            )
            "Bastoni" -> PlayerVisualIdentity(
                personalityMotto = "Precision ball playing 📐",
                heightScale = 1.08f,
                skinTone = Color(0xFFFDBA74),
                hairColor = Color(0xFF261C14),
                hairStyle = HairStyle.SHORT,
                hasBeard = true
            )
            "Barella" -> PlayerVisualIdentity(
                personalityMotto = "Box-to-box engine 🔋",
                heightScale = 0.98f,
                skinTone = Color(0xFFFDBA74),
                hairColor = Color(0xFF332014),
                hairStyle = HairStyle.SHORT
            )
            "Chiesa" -> PlayerVisualIdentity(
                personalityMotto = "Electric winger ⚡",
                heightScale = 1.02f,
                skinTone = Color(0xFFFDBA74),
                hairColor = Color(0xFF261C14),
                hairStyle = HairStyle.QUIFF
            )
            "Scamacca" -> PlayerVisualIdentity(
                personalityMotto = "Target man rocket 🚀",
                heightScale = 1.12f,
                skinTone = Color(0xFFFDBA74),
                hairColor = Color(0xFF1F1F1F),
                hairStyle = HairStyle.BUZZ_CUT,
                hasBeard = true
            )
            "Jorginho" -> PlayerVisualIdentity(
                personalityMotto = "Metronome tempo ⏱️",
                heightScale = 1.02f,
                skinTone = Color(0xFFFDBA74),
                hairColor = Color(0xFF261C14),
                hairStyle = HairStyle.SHORT,
                hasBeard = true
            )
            "Di Lorenzo" -> PlayerVisualIdentity(
                personalityMotto = "Solid rock 🛡️",
                heightScale = 1.04f,
                skinTone = Color(0xFFFDBA74),
                hairColor = Color(0xFF1E1712),
                hairStyle = HairStyle.SHORT
            )
            "Frattesi" -> PlayerVisualIdentity(
                personalityMotto = "Dangerous runs ⚡",
                heightScale = 1.01f,
                skinTone = Color(0xFFFDBA74),
                hairColor = Color(0xFF2A1C14),
                hairStyle = HairStyle.SHORT
            )
            "Retegui" -> PlayerVisualIdentity(
                personalityMotto = "Clinical finish 🎯",
                heightScale = 1.06f,
                skinTone = Color(0xFFFDBA74),
                hairColor = Color(0xFF1F1F1F),
                hairStyle = HairStyle.SHORT
            )
            else -> PlayerVisualIdentity(personalityMotto = "ריכוז מלא ⚽")
        }
    }
}

/**
 * Main dispatcher for drawing a living player figurine or 2D tactical disc token.
 */
fun DrawScope.drawLivingPlayerFigurine(
    player: Player,
    groundCx: Float,
    groundCy: Float,
    cellWidth: Float,
    cellHeight: Float,
    elevation: Float,
    isMoving: Boolean,
    isSelected: Boolean,
    hasBall: Boolean,
    ballScreenX: Float,
    ballScreenY: Float,
    animationTimeMs: Long,
    pulseAlpha: Float,
    isActedThisTurn: Boolean,
    textMeasurer: TextMeasurer,
    isThreatened: Boolean,
    is3D: Boolean = false,
    stance: PlayerActionStance = PlayerActionStance.IDLE
) {
    if (is3D) {
        drawUpright3DPlayerFigurine(
            player = player,
            groundCx = groundCx,
            groundCy = groundCy,
            cellWidth = cellWidth,
            cellHeight = cellHeight,
            elevation = elevation,
            isMoving = isMoving,
            isSelected = isSelected,
            hasBall = hasBall,
            ballScreenX = ballScreenX,
            ballScreenY = ballScreenY,
            animationTimeMs = animationTimeMs,
            pulseAlpha = pulseAlpha,
            isActedThisTurn = isActedThisTurn,
            textMeasurer = textMeasurer,
            isThreatened = isThreatened,
            stance = stance
        )
        return
    }

    val isHome = player.team == Team.HOME
    val isGk = player.role == PlayerRole.GOALKEEPER
    val effectiveAlpha = if (isActedThisTurn) 0.52f else 1.0f

    val tokenRadius = cellWidth * 0.36f
    val liftPx = if (isMoving) 6f else 0f
    val tokenCy = groundCy - liftPx

    // Ground Contact Shadow
    drawOval(
        color = Color.Black.copy(alpha = 0.28f * effectiveAlpha),
        topLeft = Offset(groundCx - tokenRadius * 0.95f, groundCy - tokenRadius * 0.25f),
        size = Size(tokenRadius * 1.9f, tokenRadius * 0.7f)
    )

    // Selection Ring
    if (isSelected) {
        drawCircle(
            color = Color(0xFF06B6D4),
            radius = tokenRadius + 5f,
            center = Offset(groundCx, tokenCy),
            style = Stroke(width = 3.5f)
        )
        val arrowTop = tokenCy - tokenRadius - 14f
        val arrowPath = Path().apply {
            moveTo(groundCx, arrowTop + 8f)
            lineTo(groundCx - 6f, arrowTop)
            lineTo(groundCx + 6f, arrowTop)
            close()
        }
        drawPath(arrowPath, color = Color(0xFF06B6D4))
    }

    // Ball-Carrier Glow Ring
    if (hasBall) {
        drawCircle(
            color = Color(0xFFF59E0B),
            radius = tokenRadius + 4f,
            center = Offset(groundCx, tokenCy),
            style = Stroke(width = 3f)
        )
    }

    val bodyColor = when {
        isGk -> if (isHome) Color(0xFFEAB308) else Color(0xFF10B981)
        isHome -> Color(0xFF1D4ED8)
        else -> Color(0xFFFFFFFF)
    }

    val rimColor = when {
        isGk -> Color(0xFF0F172A)
        isHome -> Color(0xFFFFFFFF)
        else -> Color(0xFF1E3A8A)
    }

    val numberColor = when {
        isGk -> Color(0xFF0F172A)
        isHome -> Color(0xFFFFFFFF)
        else -> Color(0xFF1E3A8A)
    }

    drawCircle(
        color = bodyColor.copy(alpha = effectiveAlpha),
        radius = tokenRadius,
        center = Offset(groundCx, tokenCy)
    )

    drawCircle(
        color = rimColor.copy(alpha = effectiveAlpha),
        radius = tokenRadius,
        center = Offset(groundCx, tokenCy),
        style = Stroke(width = 2.4f)
    )

    val numberText = player.number.toString()
    val isDoubleDigit = player.number >= 10
    val baseFontSize = if (isDoubleDigit) (tokenRadius * 0.95f) else (tokenRadius * 1.15f)
    val numberStyle = TextStyle(
        color = numberColor.copy(alpha = effectiveAlpha),
        fontSize = baseFontSize.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.SansSerif
    )
    val numberLayout = textMeasurer.measure(numberText, numberStyle)

    drawText(
        textLayoutResult = numberLayout,
        topLeft = Offset(
            groundCx - numberLayout.size.width / 2f,
            tokenCy - numberLayout.size.height / 2f
        )
    )

    // Role Indicator Pip
    val roleColor = when (player.role) {
        PlayerRole.ATTACKER -> Color(0xFFEF4444)
        PlayerRole.MIDFIELDER -> Color(0xFF38BDF8)
        PlayerRole.DEFENDER -> Color(0xFF22C55E)
        PlayerRole.GOALKEEPER -> Color(0xFFF59E0B)
    }
    val roleDotRadius = 3.5f
    drawCircle(
        color = Color(0xFF0F172A),
        radius = roleDotRadius + 1f,
        center = Offset(groundCx + tokenRadius * 0.7f, tokenCy + tokenRadius * 0.7f)
    )
    drawCircle(
        color = roleColor,
        radius = roleDotRadius,
        center = Offset(groundCx + tokenRadius * 0.7f, tokenCy + tokenRadius * 0.7f)
    )
}

/**
 * Draws an authentic upright standing 3D football character figurine with real posture,
 * tabletop pedestal disc, athletic jersey, shorts, legs, boots, detailed face & hair, and
 * floating stadium identity tag.
 */
fun DrawScope.drawUpright3DPlayerFigurine(
    player: Player,
    groundCx: Float,
    groundCy: Float,
    cellWidth: Float,
    cellHeight: Float,
    elevation: Float,
    isMoving: Boolean,
    isSelected: Boolean,
    hasBall: Boolean,
    ballScreenX: Float,
    ballScreenY: Float,
    animationTimeMs: Long,
    pulseAlpha: Float,
    isActedThisTurn: Boolean,
    textMeasurer: TextMeasurer,
    isThreatened: Boolean,
    stance: PlayerActionStance = PlayerActionStance.IDLE
) {
    val isHome = player.team == Team.HOME
    val isGk = player.role == PlayerRole.GOALKEEPER
    val effectiveAlpha = if (isActedThisTurn) 0.55f else 1.0f

    val identity = PlayerVisualRegistry.getIdentity(player)
    val heightScale = identity.heightScale

    // 1. High-Quality 3D Tabletop Pedestal Disc on grass
    val pedRadiusX = cellWidth * 0.38f
    val pedRadiusY = cellHeight * 0.16f
    val pedColor = if (isHome) Color(0xFF1E3A8A) else Color(0xFF0F172A)
    val pedRimColor = if (isHome) Color(0xFF93C5FD) else Color(0xFFCBD5E1)

    // Soft Ground Shadow under pedestal
    val isDive = stance == PlayerActionStance.GK_DIVE_LEFT || stance == PlayerActionStance.GK_DIVE_RIGHT
    val shadowW = if (isDive) cellWidth * 1.15f else pedRadiusX * 2.2f
    val shadowH = cellHeight * 0.22f
    drawOval(
        color = Color.Black.copy(alpha = 0.42f * effectiveAlpha),
        topLeft = Offset(groundCx - shadowW / 2f, groundCy - shadowH / 2f + 3f),
        size = Size(shadowW, shadowH)
    )

    if (!isDive) {
        // 3D Beveled Pedestal Base (Subbuteo / Tabletop Figurine Style)
        drawOval(
            brush = Brush.verticalGradient(
                colors = listOf(pedColor.copy(alpha = effectiveAlpha), pedColor.copy(alpha = 0.85f * effectiveAlpha), Color(0xFF020617)),
                startY = groundCy - pedRadiusY,
                endY = groundCy + pedRadiusY
            ),
            topLeft = Offset(groundCx - pedRadiusX, groundCy - pedRadiusY),
            size = Size(pedRadiusX * 2f, pedRadiusY * 2f)
        )
        // Specular Metallic Bevel Rim
        drawOval(
            color = pedRimColor.copy(alpha = 0.80f * effectiveAlpha),
            topLeft = Offset(groundCx - pedRadiusX, groundCy - pedRadiusY),
            size = Size(pedRadiusX * 2f, pedRadiusY * 2f),
            style = Stroke(width = 1.8f)
        )
    }

    // 1.5 Turf Spray Particles for Slide Tackle
    if (stance == PlayerActionStance.SLIDE_TACKLE) {
        val sprayDir = if (isHome) -1f else 1f
        for (i in 0..7) {
            val pX = groundCx + (i * 3.8f * sprayDir) + ((animationTimeMs / 25 + i * 17) % 12 - 6f)
            val pY = groundCy + 2f - ((animationTimeMs / 25 + i * 23) % 14f)
            val pColor = if (i % 3 == 0) Color(0xFF22C55E) else if (i % 3 == 1) Color(0xFF16A34A) else Color(0xFF854D0E)
            drawCircle(pColor.copy(alpha = 0.85f * effectiveAlpha), radius = 2.4f, center = Offset(pX, pY))
        }
    }

    // 1.6 Turf Puff for Kicking Strike
    if (stance == PlayerActionStance.KICKING) {
        for (i in 0..5) {
            val puffX = groundCx - cellWidth * 0.10f + ((animationTimeMs / 18 + i * 11) % 8 - 4f)
            val puffY = groundCy - ((animationTimeMs / 18 + i * 13) % 10f)
            drawCircle(Color(0xFF22C55E).copy(alpha = 0.70f), radius = 2.0f, center = Offset(puffX, puffY))
        }
    }

    // 2. Selection Ring around the Pedestal
    if (isSelected) {
        drawOval(
            color = Color(0xFF00E5FF),
            topLeft = Offset(groundCx - pedRadiusX * 1.25f, groundCy - pedRadiusY * 1.25f),
            size = Size(pedRadiusX * 2.5f, pedRadiusY * 2.5f),
            style = Stroke(width = 3.5f)
        )
        // Selection Arrow in 3D air above player
        val arrowTop = groundCy - cellHeight * 1.45f
        val arrowPath = Path().apply {
            moveTo(groundCx, arrowTop + 8f)
            lineTo(groundCx - 6f, arrowTop)
            lineTo(groundCx + 6f, arrowTop)
            close()
        }
        drawPath(arrowPath, color = Color(0xFF00E5FF))
    }

    // 3. Ball Possession Aura on Grass
    if (hasBall) {
        drawOval(
            color = Color(0xFFFFD700),
            topLeft = Offset(groundCx - pedRadiusX * 1.18f, groundCy - pedRadiusY * 1.18f),
            size = Size(pedRadiusX * 2.36f, pedRadiusY * 2.36f),
            style = Stroke(width = 3.2f)
        )
    }

    // Upgraded Figurine Heights & Coordinates (Larger, heroic presence)
    val figH = cellHeight * 1.22f * heightScale
    val walkPhase = (animationTimeMs % 600) / 600f
    val walkBob = if (isMoving && stance == PlayerActionStance.RUNNING) kotlin.math.abs(kotlin.math.sin(walkPhase * kotlin.math.PI * 2)).toFloat() * 4f else 0f
    val legStride = if (isMoving && stance == PlayerActionStance.RUNNING) kotlin.math.sin(walkPhase * kotlin.math.PI * 2).toFloat() * 5f else 0f

    val figRotation = when (stance) {
        PlayerActionStance.GK_DIVE_LEFT -> -62f
        PlayerActionStance.GK_DIVE_RIGHT -> 62f
        PlayerActionStance.SLIDE_TACKLE -> if (isHome) -36f else 36f
        PlayerActionStance.KICKING -> if (isHome) 18f else -18f
        PlayerActionStance.CELEBRATION -> 0f
        PlayerActionStance.RUNNING -> if (isMoving) 5f else 0f
        PlayerActionStance.IDLE -> 0f
    }
    val stanceLiftY = when (stance) {
        PlayerActionStance.GK_DIVE_LEFT, PlayerActionStance.GK_DIVE_RIGHT -> cellHeight * 0.38f
        PlayerActionStance.SLIDE_TACKLE -> -cellHeight * 0.08f
        PlayerActionStance.CELEBRATION -> -cellHeight * 0.08f
        else -> 0f
    }

    val footY = groundCy - walkBob - stanceLiftY
    val shortsBottomY = footY - figH * 0.22f
    val shortsTopY = footY - figH * 0.40f
    val torsoTopY = footY - figH * 0.72f
    val headRadius = cellWidth * 0.20f * heightScale
    val headCenterY = torsoTopY - headRadius * 0.92f

    // Team Colors & Kit Design (Israel = Royal Blue Home, Italy = Pristine White Away Kit)
    val jerseyColor = when {
        isGk -> if (isHome) Color(0xFFF59E0B) else Color(0xFF10B981) // Glazer Gold vs Donnarumma Emerald
        isHome -> Color(0xFF1D4ED8) // Israel Royal Blue
        else -> Color(0xFFF8FAFC)   // Italy Classic White Away Kit
    }
    val jerseyShade = when {
        isGk -> if (isHome) Color(0xFFD97706) else Color(0xFF059669)
        isHome -> Color(0xFF1E3A8A)
        else -> Color(0xFFCBD5E1)
    }
    val shortsColor = when {
        isGk -> Color(0xFF1E293B)
        isHome -> Color(0xFFFFFFFF) // Israel White Shorts
        else -> Color(0xFF1E3A8A)   // Italy Azzurri Navy Shorts
    }
    val sockColor = when {
        isGk -> Color(0xFF334155)
        isHome -> Color(0xFF1D4ED8) // Israel Blue Socks
        else -> Color(0xFF0284C7)   // Italy Azure Socks
    }

    rotate(degrees = figRotation, pivot = Offset(groundCx, groundCy - figH * 0.45f - stanceLiftY)) {
        // 4. Boots & Cleats
        val isSlide = stance == PlayerActionStance.SLIDE_TACKLE
        val isKick = stance == PlayerActionStance.KICKING
        val leftBootX = if (isSlide) groundCx + cellWidth * 0.22f else groundCx - cellWidth * 0.12f - legStride * 0.5f
        val rightBootX = if (isSlide) groundCx - cellWidth * 0.18f else if (isKick) groundCx + cellWidth * 0.22f else groundCx + cellWidth * 0.12f + legStride * 0.5f
        val rightBootYOffset = if (isKick) cellHeight * 0.12f else 0f
        val bootW = cellWidth * 0.17f
        val bootH = cellHeight * 0.09f

        // Draw Left & Right Boots (Black cleats with gold/neon studs and laces)
        drawRoundRect(
            color = Color(0xFF0F172A).copy(alpha = effectiveAlpha),
            topLeft = Offset(leftBootX - bootW / 2f, footY - bootH),
            size = Size(bootW, bootH),
            cornerRadius = CornerRadius(3.5f, 3.5f)
        )
        drawRoundRect(
            color = Color(0xFF0F172A).copy(alpha = effectiveAlpha),
            topLeft = Offset(rightBootX - bootW / 2f, footY - bootH - rightBootYOffset),
            size = Size(bootW, bootH),
            cornerRadius = CornerRadius(3.5f, 3.5f)
        )
        // 3D Leather Upper Specular highlight
        drawRoundRect(
            color = Color.White.copy(alpha = 0.22f * effectiveAlpha),
            topLeft = Offset(leftBootX - bootW * 0.35f, footY - bootH),
            size = Size(bootW * 0.7f, bootH * 0.4f),
            cornerRadius = CornerRadius(2f, 2f)
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.22f * effectiveAlpha),
            topLeft = Offset(rightBootX - bootW * 0.35f, footY - bootH - rightBootYOffset),
            size = Size(bootW * 0.7f, bootH * 0.4f),
            cornerRadius = CornerRadius(2f, 2f)
        )
        // Crossing Athletic Laces
        val laceColor = Color.White.copy(alpha = 0.85f * effectiveAlpha)
        drawLine(laceColor, Offset(leftBootX - 2.5f, footY - bootH + 2f), Offset(leftBootX + 2.5f, footY - bootH + 5f), 1f)
        drawLine(laceColor, Offset(leftBootX + 2.5f, footY - bootH + 2f), Offset(leftBootX - 2.5f, footY - bootH + 5f), 1f)
        drawLine(laceColor, Offset(rightBootX - 2.5f, footY - bootH - rightBootYOffset + 2f), Offset(rightBootX + 2.5f, footY - bootH - rightBootYOffset + 5f), 1f)
        drawLine(laceColor, Offset(rightBootX + 2.5f, footY - bootH - rightBootYOffset + 2f), Offset(rightBootX - 2.5f, footY - bootH - rightBootYOffset + 5f), 1f)

        // 3D Metallic Sole Studs
        val studColor = Color(0xFFFFD700).copy(alpha = effectiveAlpha)
        drawCircle(studColor, radius = 1.3f, center = Offset(leftBootX - bootW * 0.28f, footY))
        drawCircle(studColor, radius = 1.3f, center = Offset(leftBootX + bootW * 0.28f, footY))
        drawCircle(studColor, radius = 1.3f, center = Offset(rightBootX - bootW * 0.28f, footY - rightBootYOffset))
        drawCircle(studColor, radius = 1.3f, center = Offset(rightBootX + bootW * 0.28f, footY - rightBootYOffset))

        // 5. Socks & Lower Legs
        val legW = cellWidth * 0.11f
        drawRoundRect(
            color = sockColor.copy(alpha = effectiveAlpha),
            topLeft = Offset(leftBootX - legW / 2f, shortsBottomY),
            size = Size(legW, (footY - bootH) - shortsBottomY),
            cornerRadius = CornerRadius(2.5f, 2.5f)
        )
        drawRoundRect(
            color = sockColor.copy(alpha = effectiveAlpha),
            topLeft = Offset(rightBootX - legW / 2f, shortsBottomY),
            size = Size(legW, (footY - bootH - rightBootYOffset) - shortsBottomY),
            cornerRadius = CornerRadius(2.5f, 2.5f)
        )
        // White turnover band on sock top
        val sockTopColor = Color.White.copy(alpha = effectiveAlpha)
        drawRect(sockTopColor, topLeft = Offset(leftBootX - legW / 2f, shortsBottomY), size = Size(legW, 4f))
        drawRect(sockTopColor, topLeft = Offset(rightBootX - legW / 2f, shortsBottomY), size = Size(legW, 4f))

        // 6. Athletic Shorts
        val shortsW = cellWidth * 0.42f
        val shortsH = shortsBottomY - shortsTopY
        drawRoundRect(
            color = shortsColor.copy(alpha = effectiveAlpha),
            topLeft = Offset(groundCx - shortsW / 2f, shortsTopY),
            size = Size(shortsW, shortsH),
            cornerRadius = CornerRadius(3f, 3f)
        )
        // Shorts side stripe
        val stripeColor = if (isHome) Color(0xFF1D4ED8) else Color(0xFF0284C7)
        drawLine(
            color = stripeColor.copy(alpha = effectiveAlpha),
            start = Offset(groundCx - shortsW / 2f + 1f, shortsTopY),
            end = Offset(groundCx - shortsW / 2f + 1f, shortsBottomY),
            strokeWidth = 2.5f
        )
        drawLine(
            color = stripeColor.copy(alpha = effectiveAlpha),
            start = Offset(groundCx + shortsW / 2f - 1f, shortsTopY),
            end = Offset(groundCx + shortsW / 2f - 1f, shortsBottomY),
            strokeWidth = 2.5f
        )

        // 7. Torso / Jersey (3D Cylindrical Shaded)
        val torsoW = cellWidth * 0.46f
        val torsoH = shortsTopY - torsoTopY
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(jerseyShade.copy(alpha = effectiveAlpha), jerseyColor.copy(alpha = effectiveAlpha), jerseyShade.copy(alpha = effectiveAlpha)),
                startX = groundCx - torsoW / 2f,
                endX = groundCx + torsoW / 2f
            ),
            topLeft = Offset(groundCx - torsoW / 2f, torsoTopY),
            size = Size(torsoW, torsoH),
            cornerRadius = CornerRadius(4.5f, 4.5f)
        )

        // National Team Crest on Chest
        if (isHome) {
            // Israel: Star of David Crest
            val crestCx = groundCx - torsoW * 0.26f
            val crestCy = torsoTopY + torsoH * 0.32f
            drawCircle(Color.White, radius = 3.6f, center = Offset(crestCx, crestCy))
            drawCircle(Color(0xFF1D4ED8), radius = 2.2f, center = Offset(crestCx, crestCy))
        } else {
            // Italy: Tricolor Shield (Green / White / Red)
            val crestX = groundCx - torsoW * 0.32f
            val crestY = torsoTopY + torsoH * 0.26f
            drawRect(Color(0xFF16A34A), topLeft = Offset(crestX, crestY), size = Size(2.2f, 6.5f))
            drawRect(Color.White, topLeft = Offset(crestX + 2.2f, crestY), size = Size(2.2f, 6.5f))
            drawRect(Color(0xFFDC2626), topLeft = Offset(crestX + 4.4f, crestY), size = Size(2.2f, 6.5f))
        }

        // Collar / Crewneck
        val collarColor = if (isHome) Color.White else Color(0xFF0284C7)
        drawArc(
            color = collarColor.copy(alpha = effectiveAlpha),
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(groundCx - cellWidth * 0.11f, torsoTopY - 2.5f),
            size = Size(cellWidth * 0.22f, cellHeight * 0.09f),
            style = Stroke(width = 2.2f)
        )

        // 8. Arms & Gloves (Dynamic Poses)
        val armW = cellWidth * 0.10f
        val armH = torsoH * 0.88f
        val isCelebration = stance == PlayerActionStance.CELEBRATION

        if (isDive) {
            // Outstretched Goalkeeper Arms towards corner
            drawRoundRect(
                color = jerseyColor.copy(alpha = effectiveAlpha),
                topLeft = Offset(groundCx - armW, torsoTopY - armH * 0.75f),
                size = Size(armW, armH),
                cornerRadius = CornerRadius(3f, 3f)
            )
            drawRoundRect(
                color = jerseyColor.copy(alpha = effectiveAlpha),
                topLeft = Offset(groundCx + 2f, torsoTopY - armH * 0.75f),
                size = Size(armW, armH),
                cornerRadius = CornerRadius(3f, 3f)
            )
            // Big white padded goalkeeper gloves blocking the ball
            drawCircle(Color.White, radius = armW * 1.15f, center = Offset(groundCx - armW / 2f, torsoTopY - armH * 0.82f))
            drawCircle(Color.White, radius = armW * 1.15f, center = Offset(groundCx + armW / 2f + 2f, torsoTopY - armH * 0.82f))
        } else if (isCelebration) {
            // Victory Arms Raised High in the Air ("V" for victory)
            drawRoundRect(
                color = jerseyColor.copy(alpha = effectiveAlpha),
                topLeft = Offset(groundCx - torsoW / 2f - armW, torsoTopY - armH * 0.75f),
                size = Size(armW, armH),
                cornerRadius = CornerRadius(3f, 3f)
            )
            drawRoundRect(
                color = jerseyColor.copy(alpha = effectiveAlpha),
                topLeft = Offset(groundCx + torsoW / 2f, torsoTopY - armH * 0.75f),
                size = Size(armW, armH),
                cornerRadius = CornerRadius(3f, 3f)
            )
            drawCircle(identity.skinTone, radius = armW * 0.75f, center = Offset(groundCx - torsoW / 2f - armW / 2f, torsoTopY - armH * 0.82f))
            drawCircle(identity.skinTone, radius = armW * 0.75f, center = Offset(groundCx + torsoW / 2f + armW / 2f, torsoTopY - armH * 0.82f))
        } else {
            // Natural arms with running swing or shooting balance
            val armSwing = if (isMoving) legStride * 0.8f else 0f
            drawRoundRect(
                color = jerseyColor.copy(alpha = effectiveAlpha),
                topLeft = Offset(groundCx - torsoW / 2f - armW + 1f, torsoTopY + armSwing),
                size = Size(armW, armH),
                cornerRadius = CornerRadius(3f, 3f)
            )
            drawRoundRect(
                color = jerseyColor.copy(alpha = effectiveAlpha),
                topLeft = Offset(groundCx + torsoW / 2f - 1f, torsoTopY - armSwing),
                size = Size(armW, armH),
                cornerRadius = CornerRadius(3f, 3f)
            )
            val handColor = if (isGk) Color(0xFFFFFFFF) else identity.skinTone
            drawCircle(
                color = handColor.copy(alpha = effectiveAlpha),
                radius = armW * 0.68f,
                center = Offset(groundCx - torsoW / 2f - armW / 2f + 1f, torsoTopY + armH + armSwing)
            )
            drawCircle(
                color = handColor.copy(alpha = effectiveAlpha),
                radius = armW * 0.68f,
                center = Offset(groundCx + torsoW / 2f + armW / 2f - 1f, torsoTopY + armH - armSwing)
            )
        }

        // Captain's Armband (e.g. Zahavi, Dasa)
        if (identity.isCaptain) {
            val armLeft = groundCx - torsoW / 2f - armW + 1f
            drawRoundRect(
                color = Color(0xFFFFD700),
                topLeft = Offset(armLeft - 0.5f, torsoTopY + 4f),
                size = Size(armW + 1f, 5.5f),
                cornerRadius = CornerRadius(1.5f, 1.5f)
            )
            val cLayout = textMeasurer.measure("C", TextStyle(color = Color(0xFF0F172A), fontSize = 5.5.sp, fontWeight = FontWeight.Black))
            drawText(textLayoutResult = cLayout, topLeft = Offset(armLeft + (armW + 1f - cLayout.size.width) / 2f, torsoTopY + 3.8f))
        }

        // 9. Centered Jersey Number
        val numText = player.number.toString()
        val numLayout = textMeasurer.measure(
            text = numText,
            style = TextStyle(
                color = (if (isGk) Color(0xFF0F172A) else if (isHome) Color.White else Color(0xFF1E3A8A)).copy(alpha = effectiveAlpha),
                fontSize = (torsoH * 0.45f).toSp(),
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif
            )
        )
        drawText(
            textLayoutResult = numLayout,
            topLeft = Offset(
                groundCx - numLayout.size.width / 2f,
                torsoTopY + (torsoH - numLayout.size.height) / 2f
            )
        )

        // 10. Head & Face (3D Spherical Shading)
        drawCircle(
            color = identity.skinTone.copy(alpha = effectiveAlpha),
            radius = headRadius,
            center = Offset(groundCx, headCenterY)
        )
        // Specular 3D Highlight on forehead
        drawCircle(
            color = Color.White.copy(alpha = 0.28f * effectiveAlpha),
            radius = headRadius * 0.45f,
            center = Offset(groundCx - headRadius * 0.28f, headCenterY - headRadius * 0.28f)
        )

        // Focused Eyes
        val eyeY = headCenterY - headRadius * 0.05f
        val leftEyeX = groundCx - headRadius * 0.35f
        val rightEyeX = groundCx + headRadius * 0.35f
        // White Sclera
        drawCircle(Color.White.copy(alpha = effectiveAlpha), radius = 2.0f, center = Offset(leftEyeX, eyeY))
        drawCircle(Color.White.copy(alpha = effectiveAlpha), radius = 2.0f, center = Offset(rightEyeX, eyeY))
        // Focused Pupil
        drawCircle(Color(0xFF0F172A).copy(alpha = effectiveAlpha), radius = 1.1f, center = Offset(leftEyeX + 0.3f, eyeY))
        drawCircle(Color(0xFF0F172A).copy(alpha = effectiveAlpha), radius = 1.1f, center = Offset(rightEyeX + 0.3f, eyeY))
        // Specular Catchlight Glints (Unity stylized life reflection)
        drawCircle(Color.White.copy(alpha = effectiveAlpha), radius = 0.55f, center = Offset(leftEyeX + 0.5f, eyeY - 0.4f))
        drawCircle(Color.White.copy(alpha = effectiveAlpha), radius = 0.55f, center = Offset(rightEyeX + 0.5f, eyeY - 0.4f))

        // Beard / Stubble (Zahavi, Vitor, Bastoni, etc.)
        if (identity.hasBeard) {
            drawArc(
                color = identity.hairColor.copy(alpha = 0.65f * effectiveAlpha),
                startAngle = 10f,
                sweepAngle = 160f,
                useCenter = false,
                topLeft = Offset(groundCx - headRadius * 0.75f, headCenterY - headRadius * 0.2f),
                size = Size(headRadius * 1.5f, headRadius * 1.1f),
                style = Stroke(width = 2.8f)
            )
        }

        // Custom Hair Silhouettes
        when (identity.hairStyle) {
            HairStyle.BUZZ_CUT -> {
                drawArc(
                    color = identity.hairColor.copy(alpha = effectiveAlpha),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(groundCx - headRadius * 0.98f, headCenterY - headRadius),
                    size = Size(headRadius * 1.96f, headRadius * 1.25f)
                )
            }
            HairStyle.FLOWING_WAVY -> {
                // Calafiori style locks down around ears
                drawArc(
                    color = identity.hairColor.copy(alpha = effectiveAlpha),
                    startAngle = 170f,
                    sweepAngle = 200f,
                    useCenter = true,
                    topLeft = Offset(groundCx - headRadius * 1.15f, headCenterY - headRadius * 1.1f),
                    size = Size(headRadius * 2.3f, headRadius * 1.7f)
                )
                // Headband
                drawArc(
                    color = Color.White.copy(alpha = effectiveAlpha),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(groundCx - headRadius * 1.05f, headCenterY - headRadius * 0.55f),
                    size = Size(headRadius * 2.1f, headRadius * 0.7f),
                    style = Stroke(width = 1.8f)
                )
            }
            HairStyle.QUIFF -> {
                drawArc(
                    color = identity.hairColor.copy(alpha = effectiveAlpha),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(groundCx - headRadius * 1.05f, headCenterY - headRadius * 1.25f),
                    size = Size(headRadius * 2.1f, headRadius * 1.6f)
                )
            }
            HairStyle.CURLY -> {
                drawArc(
                    color = identity.hairColor.copy(alpha = effectiveAlpha),
                    startAngle = 175f,
                    sweepAngle = 190f,
                    useCenter = true,
                    topLeft = Offset(groundCx - headRadius * 1.1f, headCenterY - headRadius * 1.15f),
                    size = Size(headRadius * 2.2f, headRadius * 1.55f)
                )
            }
            HairStyle.SHORT -> {
                drawArc(
                    color = identity.hairColor.copy(alpha = effectiveAlpha),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(groundCx - headRadius, headCenterY - headRadius),
                    size = Size(headRadius * 2f, headRadius * 1.4f)
                )
            }
        }

        // 11. Role Badge Pip (Near shoulder)
        val roleColor = when (player.role) {
            PlayerRole.ATTACKER -> Color(0xFFEF4444)
            PlayerRole.MIDFIELDER -> Color(0xFF38BDF8)
            PlayerRole.DEFENDER -> Color(0xFF22C55E)
            PlayerRole.GOALKEEPER -> Color(0xFFF59E0B)
        }
        drawCircle(Color(0xFF0F172A), radius = 4f, center = Offset(groundCx + torsoW / 2f, torsoTopY + 3f))
        drawCircle(roleColor, radius = 3f, center = Offset(groundCx + torsoW / 2f, torsoTopY + 3f))
    }

    // 12. Floating 3D Broadcast Name & Number Pill (Clean, high-legibility stadium label)
    if (isSelected || hasBall) {
        val pillText = "${player.roleIcon} ${player.name} (${player.number})"
        val pillLayout = textMeasurer.measure(
            text = pillText,
            style = TextStyle(
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif
            )
        )
        val pillW = pillLayout.size.width + 12f
        val pillH = pillLayout.size.height + 5f
        val pillTop = headCenterY - headRadius - pillH - 6f
        val pillLeft = groundCx - pillW / 2f

        drawRoundRect(
            color = Color(0xFF0F172A).copy(alpha = 0.94f),
            topLeft = Offset(pillLeft, pillTop),
            size = Size(pillW, pillH),
            cornerRadius = CornerRadius(5f, 5f)
        )
        drawRoundRect(
            color = if (isSelected) Color(0xFF00E5FF) else Color(0xFFFFD700),
            topLeft = Offset(pillLeft, pillTop),
            size = Size(pillW, pillH),
            cornerRadius = CornerRadius(5f, 5f),
            style = Stroke(width = 1.4f)
        )
        drawText(
            textLayoutResult = pillLayout,
            topLeft = Offset(pillLeft + 6f, pillTop + 2.5f)
        )
    } else {
        // High-legibility compact name label floating above
        val miniText = player.name
        val miniLayout = textMeasurer.measure(
            text = miniText,
            style = TextStyle(
                color = Color.White.copy(alpha = 0.92f),
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif
            )
        )
        val mW = miniLayout.size.width + 8f
        val mH = miniLayout.size.height + 3f
        val mTop = headCenterY - headRadius - mH - 3.5f
        val mLeft = groundCx - mW / 2f
        drawRoundRect(
            color = Color(0xFF0F172A).copy(alpha = 0.78f),
            topLeft = Offset(mLeft, mTop),
            size = Size(mW, mH),
            cornerRadius = CornerRadius(3.5f, 3.5f)
        )
        drawText(
            textLayoutResult = miniLayout,
            topLeft = Offset(mLeft + 4f, mTop + 1.5f)
        )
    }
}
