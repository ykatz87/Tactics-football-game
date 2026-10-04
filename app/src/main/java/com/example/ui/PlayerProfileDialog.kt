package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.Player
import com.example.PlayerRole
import com.example.Team
import com.example.R

@Composable
fun PlayerProfileDialog(
    player: Player,
    onDismiss: () -> Unit
) {
    val figurineRes = when {
        player.role == PlayerRole.GOALKEEPER -> R.drawable.img_player_gk_1790244917264
        player.team == Team.HOME -> R.drawable.img_player_home_1790244884615
        else -> R.drawable.img_player_away_1790244902512
    }

    val teamColor = if (player.team == Team.HOME) Color(0xFFFDE047) else Color(0xFF38BDF8)
    val roleColor = when (player.role) {
        PlayerRole.GOALKEEPER -> Color(0xFFF59E0B)
        PlayerRole.DEFENDER -> Color(0xFF10B981)
        PlayerRole.MIDFIELDER -> Color(0xFF3B82F6)
        PlayerRole.ATTACKER -> Color(0xFFEF4444)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A),
            border = androidx.compose.foundation.BorderStroke(2.dp, Brush.linearGradient(listOf(teamColor, roleColor))),
            shadowElevation = 24.dp,
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top: 3D Figurine Showcase with glowing aura
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(teamColor.copy(alpha = 0.35f), Color(0xFF1E293B), Color(0xFF0F172A))
                            )
                        )
                        .border(3.dp, Brush.linearGradient(listOf(teamColor, Color.White)), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = figurineRes),
                        contentDescription = player.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Player Jersey & Name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Surface(
                        color = roleColor.copy(alpha = 0.2f),
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, roleColor)
                    ) {
                        Text(
                            text = "#${player.number}",
                            color = roleColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = player.name,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Text(
                    text = when(player.role) {
                        PlayerRole.GOALKEEPER -> "${player.roleIcon} שוער (Goalkeeper)"
                        PlayerRole.DEFENDER -> "${player.roleIcon} שחקן הגנה (Defender)"
                        PlayerRole.MIDFIELDER -> "${player.roleIcon} קשר (Midfielder)"
                        PlayerRole.ATTACKER -> "${player.roleIcon} חלוץ (Attacker)"
                    },
                    color = roleColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                )

                // Player Personality Motto
                val identity = PlayerVisualRegistry.getIdentity(player)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E293B),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "💬", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = "מורל ומוטו אישי", color = Color(0xFFFFD700), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(text = "\"${identity.personalityMotto}\"", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Trait card
                player.trait?.let { trait ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E293B))
                            .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = trait.icon, fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = "יכולת מיוחדת (Special Trait)", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(text = trait.description, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Stats Grid
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1E293B).copy(alpha = 0.8f))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    StatRow(label = "מהירות (Speed)", base = player.speed, current = player.currentSpeed)
                    Spacer(modifier = Modifier.height(8.dp))
                    StatRow(label = "מסירה (Passing)", base = player.passing, current = player.currentPassing)
                    Spacer(modifier = Modifier.height(8.dp))
                    StatRow(label = "בעיטה (Shooting)", base = player.shooting, current = player.currentShooting)
                    Spacer(modifier = Modifier.height(8.dp))
                    StatRow(label = "הגנה (Defense)", base = player.defense, current = player.currentDefense)
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color(0xFF334155))
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Stamina Bar
                    val staminaColor = when {
                        player.stamina > 60 -> Color(0xFF10B981)
                        player.stamina > 30 -> Color(0xFFF59E0B)
                        else -> Color(0xFFEF4444)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("כושר גופני (Stamina)", color = Color.LightGray, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1.2f))
                        Box(modifier = Modifier.weight(1.8f).height(10.dp).clip(RoundedCornerShape(50)).background(Color(0xFF0F172A))) {
                            Box(modifier = Modifier.fillMaxWidth(player.stamina / 100f).fillMaxHeight().background(
                                Brush.horizontalGradient(listOf(staminaColor.copy(alpha = 0.7f), staminaColor))
                            ))
                        }
                        Text("${player.stamina}%", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
                    }
                }

                // Cards section
                if (player.yellowCards > 0 || player.isRedCarded) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        if (player.yellowCards > 0) {
                            Text("🟨 ${player.yellowCards} כרטיס צהוב", color = Color(0xFFFFD700), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        if (player.isRedCarded) {
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("🟥 הורחק מהמשחק", color = Color(0xFFEF4444), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Text("סגור כרטיס שחקן", color = Color(0xFF0F172A), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
fun StatRow(label: String, base: Int, current: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color.Gray, fontSize = 14.sp, modifier = Modifier.weight(1f))
        
        val valueColor = if (current < base) Color(0xFFEF4444) else if (current > 80) Color(0xFF10B981) else Color.White
        
        Text(
            text = current.toString(), 
            color = valueColor, 
            fontSize = 16.sp, 
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(end = 8.dp)
        )
        
        // Mini bar
        Box(modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(50)).background(Color(0xFF334155))) {
            Box(modifier = Modifier.fillMaxWidth(current / 100f).fillMaxHeight().background(valueColor))
        }
    }
}
