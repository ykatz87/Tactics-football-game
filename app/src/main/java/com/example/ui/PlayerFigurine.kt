package com.example.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.Player
import com.example.PlayerRole
import com.example.Team

/**
 * Metadata for tactical player identity
 */
data class PlayerVisualIdentity(
    val personalityMotto: String = "מוכן לקרב!",
    val heightScale: Float = 1.0f
)

object PlayerVisualRegistry {
    fun getIdentity(player: Player): PlayerVisualIdentity {
        val motto = when (player.name) {
            "גלזר" -> "שער נעול 🧤"
            "דסה" -> "פריצה באגף 🏃‍♂️"
            "ויטור" -> "חומה בצורה 🛡️"
            "גולדברג" -> "תיקול מדויק 🛡️"
            "רביבו" -> "תמיכה שוטפת ⚡"
            "נחמיאס" -> "סגירה הרמטית 🧱"
            "לביא", "נטע לביא" -> "גרזן במרכז ⚓"
            "פרץ" -> "מנוע מרכז השדה 🔋"
            "גלוך" -> "ראיית משחק וקסם 🪄"
            "סולומון" -> "כדרור מפרק ⚡"
            "זהבי" -> "סיומת קטלנית 🎯"
            "חלאילי" -> "מהירות מסחררת 🌪️"
            "בריבו" -> "לחץ בלתי פוסק 🐾"
            "תורג'מן" -> "עוצמה וסיומת 💥"
            "משפתי" -> "דרוך בין הקורות 🧤"
            "קניקובסקי" -> "ניידות חכמה 🧠"
            "דוידה" -> "חיתוך פנימה ⚡"
            "Donnarumma" -> "No entry! 🧤"
            "Di Lorenzo" -> "Solid rock 🛡️"
            "Bastoni" -> "Precision ball playing 📐"
            "Calafiori" -> "Forward elegance 💫"
            "Dimarco" -> "Wicked left foot 🎯"
            "Barella" -> "Box-to-box engine 🔋"
            "Jorginho" -> "Metronome tempo ⏱️"
            "Frattesi" -> "Dangerous runs ⚡"
            "Chiesa" -> "Electric winger ⚡"
            "Scamacca" -> "Target man rocket 🚀"
            "Retegui" -> "Clinical finish 🎯"
            "Vicario" -> "Reflex master 🧤"
            "Pellegrini" -> "Creative maestro 🪄"
            "Raspadori" -> "Sharp turn & shoot 🎯"
            else -> "ריכוז מלא ⚽"
        }
        return PlayerVisualIdentity(personalityMotto = motto)
    }
}

/**
 * Draws a clean, comfortable, high-legibility tactical player disc token.
 * Simple, elegant, clear contrast between teams, no cluttered animations.
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
    isThreatened: Boolean
) {
    val isHome = player.team == Team.HOME
    val isGk = player.role == PlayerRole.GOALKEEPER
    val effectiveAlpha = if (isActedThisTurn) 0.52f else 1.0f

    // Token radius sized neatly to fit comfortably in cell with breathing room
    val tokenRadius = cellWidth * 0.35f
    val liftPx = if (isMoving) 6f else 0f
    val tokenCy = groundCy - liftPx

    // 1. Soft Ground Contact Shadow
    drawOval(
        color = Color.Black.copy(alpha = 0.28f * effectiveAlpha),
        topLeft = Offset(groundCx - tokenRadius * 0.95f, groundCy - tokenRadius * 0.25f),
        size = Size(tokenRadius * 1.9f, tokenRadius * 0.7f)
    )

    // 2. Selection Ring
    if (isSelected) {
        drawCircle(
            color = Color(0xFF06B6D4), // Bright Cyan
            radius = tokenRadius + 5f,
            center = Offset(groundCx, tokenCy),
            style = Stroke(width = 3.5f)
        )
        // Neat selection arrow above player
        val arrowTop = tokenCy - tokenRadius - 14f
        val arrowPath = Path().apply {
            moveTo(groundCx, arrowTop + 8f)
            lineTo(groundCx - 6f, arrowTop)
            lineTo(groundCx + 6f, arrowTop)
            close()
        }
        drawPath(arrowPath, color = Color(0xFF06B6D4))
    }

    // 3. Ball-Carrier Glow Ring
    if (hasBall) {
        drawCircle(
            color = Color(0xFFF59E0B), // Vibrant Amber
            radius = tokenRadius + 4f,
            center = Offset(groundCx, tokenCy),
            style = Stroke(width = 3f)
        )
    }

    // 4. Team Colors (High Contrast: Israel = Deep Blue / White; Italy = Pure White / Deep Azure)
    val bodyColor = when {
        isGk -> if (isHome) Color(0xFFEAB308) else Color(0xFF10B981) // Gold vs Emerald
        isHome -> Color(0xFF1D4ED8) // Israel Royal Blue
        else -> Color(0xFFFFFFFF)   // Italy Clean White
    }

    val rimColor = when {
        isGk -> Color(0xFF0F172A)
        isHome -> Color(0xFFFFFFFF) // White rim for Israel
        else -> Color(0xFF1E3A8A)   // Navy rim for Italy
    }

    val numberColor = when {
        isGk -> Color(0xFF0F172A)
        isHome -> Color(0xFFFFFFFF) // White number on Blue
        else -> Color(0xFF1E3A8A)   // Navy number on White
    }

    // Token Body Fill
    drawCircle(
        color = bodyColor.copy(alpha = effectiveAlpha),
        radius = tokenRadius,
        center = Offset(groundCx, tokenCy)
    )

    // Outer Crisp Border
    drawCircle(
        color = rimColor.copy(alpha = effectiveAlpha),
        radius = tokenRadius,
        center = Offset(groundCx, tokenCy),
        style = Stroke(width = 2.4f)
    )

    // 5. Perfectly Sized & Centered Jersey Number
    val numberText = player.number.toString()
    val isDoubleDigit = player.number >= 10
    // Density-safe sizing: calculate font size proportionally
    val baseFontSize = if (isDoubleDigit) (tokenRadius * 0.95f) else (tokenRadius * 1.15f)
    val numberStyle = TextStyle(
        color = numberColor.copy(alpha = effectiveAlpha),
        fontSize = baseFontSize.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.SansSerif
    )
    var numberLayout = textMeasurer.measure(numberText, numberStyle)

    // Ensure number never spills outside token
    val maxAllowedWidth = tokenRadius * 1.35f
    if (numberLayout.size.width > maxAllowedWidth && maxAllowedWidth > 0f) {
        val scaledSize = baseFontSize * (maxAllowedWidth / numberLayout.size.width)
        numberLayout = textMeasurer.measure(
            numberText,
            numberStyle.copy(fontSize = scaledSize.sp)
        )
    }

    drawText(
        textLayoutResult = numberLayout,
        topLeft = Offset(
            groundCx - numberLayout.size.width / 2f,
            tokenCy - numberLayout.size.height / 2f
        )
    )

    // 6. Discreet Role Dot (Small color pip on bottom-right of token)
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

    // 7. Yellow / Red Card indicator
    if (player.isRedCarded) {
        drawRoundRect(
            color = Color(0xFFEF4444),
            topLeft = Offset(groundCx + tokenRadius * 0.4f, tokenCy - tokenRadius * 0.95f),
            size = Size(tokenRadius * 0.35f, tokenRadius * 0.48f),
            cornerRadius = CornerRadius(2f, 2f)
        )
    } else if (player.yellowCards > 0) {
        drawRoundRect(
            color = Color(0xFFFFD700),
            topLeft = Offset(groundCx + tokenRadius * 0.4f, tokenCy - tokenRadius * 0.95f),
            size = Size(tokenRadius * 0.32f, tokenRadius * 0.44f),
            cornerRadius = CornerRadius(2f, 2f)
        )
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.5f),
            topLeft = Offset(groundCx + tokenRadius * 0.4f, tokenCy - tokenRadius * 0.95f),
            size = Size(tokenRadius * 0.32f, tokenRadius * 0.44f),
            cornerRadius = CornerRadius(2f, 2f),
            style = Stroke(width = 1f)
        )
    }

    // 8. Clean, non-intrusive Name Label ONLY for selected player or ball carrier
    if (isSelected || hasBall) {
        val pillNameText = "${player.name} (${player.number})"
        val pillLayout = textMeasurer.measure(
            text = pillNameText,
            style = TextStyle(
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif
            )
        )
        val pillWidth = pillLayout.size.width + 12f
        val pillHeight = pillLayout.size.height + 6f
        val pillTop = tokenCy + tokenRadius + 4f
        val pillLeft = groundCx - pillWidth / 2f

        drawRoundRect(
            color = Color(0xFF0F172A).copy(alpha = 0.90f),
            topLeft = Offset(pillLeft, pillTop),
            size = Size(pillWidth, pillHeight),
            cornerRadius = CornerRadius(4f, 4f)
        )
        drawRoundRect(
            color = if (isSelected) Color(0xFF06B6D4) else Color(0xFFF59E0B),
            topLeft = Offset(pillLeft, pillTop),
            size = Size(pillWidth, pillHeight),
            cornerRadius = CornerRadius(4f, 4f),
            style = Stroke(width = 1f)
        )
        drawText(
            textLayoutResult = pillLayout,
            topLeft = Offset(
                pillLeft + 6f,
                pillTop + 3f
            )
        )
    }
}
