package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.GameMode

@Composable
fun MainMenuScreen(
    onSelectArcade: () -> Unit,
    onSelectTactical: () -> Unit,
    onResetFirstTime: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0F172A) // Dark luxury slate
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Header: Title & Badges
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    color = Color(0xFF1E3A8A).copy(alpha = 0.6f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "⚽ כדורגל טקטי • גרסת 2026",
                        color = Color(0xFF38BDF8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "בחר מצב משחק",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )

                Text(
                    text = "מאקשן מהיר בטאפ אחד ועד טקטיקת שחמט עמוקה",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Game Mode Cards List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                // 1. Arcade Mode (Active)
                item {
                    GameModeCard(
                        title = "מצב ארקייד (משחק מהיר)",
                        subtitle = "כניסה מהירה, פשוטה וזורמת. טאפ אחד למסירה, דריבל ובעיטה לשער ללא חיכוך.",
                        tag = "מומלץ למתחילים",
                        tagColor = Color(0xFF10B981),
                        iconEmoji = "⚡",
                        isLocked = false,
                        accentGradient = listOf(Color(0xFF059669), Color(0xFF10B981)),
                        buttonText = "שחק עכשיו ⚡",
                        onClick = onSelectArcade
                    )
                }

                // 2. Tactical Pro Mode (Active)
                item {
                    GameModeCard(
                        title = "מצב טקטיקה מתקדם",
                        subtitle = "עומק מלא: ניהול מערכים, דואלים מבוססי קוביות, כושר שחקנים ומנטליות קבוצתית.",
                        tag = "למאמנים מנוסים",
                        tagColor = Color(0xFF38BDF8),
                        iconEmoji = "🧠",
                        isLocked = false,
                        accentGradient = listOf(Color(0xFF1D4ED8), Color(0xFF0284C7)),
                        buttonText = "כניסה למצב טקטי",
                        onClick = onSelectTactical
                    )
                }

                // 3. Career Mode (Locked)
                item {
                    GameModeCard(
                        title = "מצב קריירה",
                        subtitle = "נהל מועדון שלם לאורך עונות: חלון העברות, שדרוג מתקנים, פיתוח כשרונות צעירים והובלה לאליפות.",
                        tag = "🔒 נעול (בקרוב)",
                        tagColor = Color(0xFFE2E8F0).copy(alpha = 0.6f),
                        iconEmoji = "🏆",
                        isLocked = true,
                        accentGradient = listOf(Color(0xFF334155), Color(0xFF1E293B)),
                        buttonText = "בקרוב...",
                        onClick = {}
                    )
                }

                // 4. Tournaments (Locked)
                item {
                    GameModeCard(
                        title = "טורנירים וגביעים",
                        subtitle = "גביע המדינה, ליגת האלופות ומוקדמות גביע העולם: שלבי בתים ונוקאאוט עד להנפת הגביע.",
                        tag = "🔒 נעול (בקרוב)",
                        tagColor = Color(0xFFE2E8F0).copy(alpha = 0.6f),
                        iconEmoji = "🌍",
                        isLocked = true,
                        accentGradient = listOf(Color(0xFF334155), Color(0xFF1E293B)),
                        buttonText = "בקרוב...",
                        onClick = {}
                    )
                }
            }

            // Footer: Reset First-Time Experience (For testing first-run entry)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                color = Color(0xFF1E293B).copy(alpha = 0.7f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onResetFirstTime() }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "איפוס",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "בדיקת כניסה ראשונית (התחלה ישר בארקייד)",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun GameModeCard(
    title: String,
    subtitle: String,
    tag: String,
    tagColor: Color,
    iconEmoji: String,
    isLocked: Boolean,
    accentGradient: List<Color>,
    buttonText: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isLocked, onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (isLocked) Color(0xFF1E293B).copy(alpha = 0.5f) else Color(0xFF1E293B),
        border = BorderStroke(
            1.2.dp,
            if (isLocked) Color(0xFF334155).copy(alpha = 0.5f) else accentGradient.last().copy(alpha = 0.6f)
        ),
        shadowElevation = if (isLocked) 0.dp else 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Emoji icon + Title + Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (isLocked) Color(0xFF334155).copy(alpha = 0.6f)
                                else accentGradient.first().copy(alpha = 0.25f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = iconEmoji,
                            fontSize = 20.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = title,
                            color = if (isLocked) Color(0xFF94A3B8) else Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Surface(
                    color = tagColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, tagColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = tag,
                        color = tagColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Subtitle Description
            Text(
                text = subtitle,
                color = if (isLocked) Color(0xFF64748B) else Color(0xFFCBD5E1),
                fontSize = 12.5.sp,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Action Button
            if (!isLocked) {
                Button(
                    onClick = onClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentGradient.first()
                    )
                ) {
                    Text(
                        text = buttonText,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "נעול",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "בפיתוח • יפתח בעדכון הבא",
                            color = Color(0xFF64748B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
