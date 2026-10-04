package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.GameState
import com.example.Player
import com.example.Team

@Composable
fun SubstitutionDialog(
    gameState: GameState,
    onDismiss: () -> Unit,
    onSubstitute: (outId: Int, inId: Int) -> Unit
) {
    var selectedOut by remember { mutableStateOf<Player?>(null) }
    var selectedIn by remember { mutableStateOf<Player?>(null) }

    val homePlayers = gameState.players.filter { it.team == Team.HOME }
    val benchPlayers = gameState.homeBench

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E293B),
            modifier = Modifier.fillMaxWidth().height(500.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "חילופים (${gameState.homeSubsLeft} נותרו)",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(modifier = Modifier.weight(1f)) {
                    // Field Players Column
                    Column(modifier = Modifier.weight(1f)) {
                        Text("על המגרש (יוצא)", color = Color.Gray, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyColumn {
                            items(homePlayers) { player ->
                                PlayerSubItem(
                                    player = player,
                                    isSelected = selectedOut?.id == player.id,
                                    onClick = { selectedOut = player }
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // Bench Players Column
                    Column(modifier = Modifier.weight(1f)) {
                        Text("ספסל (נכנס)", color = Color.Gray, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyColumn {
                            items(benchPlayers) { player ->
                                PlayerSubItem(
                                    player = player,
                                    isSelected = selectedIn?.id == player.id,
                                    onClick = { selectedIn = player }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("ביטול", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (selectedOut != null && selectedIn != null) {
                                onSubstitute(selectedOut!!.id, selectedIn!!.id)
                            }
                        },
                        enabled = selectedOut != null && selectedIn != null,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
                    ) {
                        Text("אשר חילוף", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerSubItem(player: Player, isSelected: Boolean, onClick: () -> Unit) {
    val staminaColor = when {
        player.stamina > 60 -> Color(0xFF22C55E)
        player.stamina > 30 -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0xFF38BDF8).copy(alpha = 0.2f) else Color(0xFF0F172A))
            .border(
                width = 1.dp,
                color = if (isSelected) Color(0xFF38BDF8) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = "${player.number} ${player.name} ${player.trait?.icon ?: ""}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "כוח: ${player.stamina}%", color = Color.LightGray, fontSize = 10.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(staminaColor))
            }
        }
    }
}
