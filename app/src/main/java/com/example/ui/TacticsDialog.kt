package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.GameState
import com.example.TeamMentality

@Composable
fun TacticsDialog(
    gameState: GameState,
    onDismiss: () -> Unit,
    onSelectMentality: (TeamMentality) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E293B),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "גישה קבוצתית",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "שינוי הגישה יעלה נקודת פעולה אחת ויזיז את כל שחקני השדה שלך.",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                MentalityOption(
                    title = TeamMentality.ALL_OUT_ATTACK.displayName,
                    description = "כולם למעלה! לחץ גבוה וסיכון למתפרצות.",
                    icon = "⚔️",
                    isSelected = gameState.homeMentality == TeamMentality.ALL_OUT_ATTACK,
                    onClick = { onSelectMentality(TeamMentality.ALL_OUT_ATTACK) }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                MentalityOption(
                    title = TeamMentality.BALANCED.displayName,
                    description = "שליטה במרכז השדה ומבנה מסודר.",
                    icon = "⚖️",
                    isSelected = gameState.homeMentality == TeamMentality.BALANCED,
                    onClick = { onSelectMentality(TeamMentality.BALANCED) }
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                MentalityOption(
                    title = TeamMentality.PARK_THE_BUS.displayName,
                    description = "הגנה עמוקה, כולם מאחורי הכדור.",
                    icon = "🚌",
                    isSelected = gameState.homeMentality == TeamMentality.PARK_THE_BUS,
                    onClick = { onSelectMentality(TeamMentality.PARK_THE_BUS) }
                )

                Spacer(modifier = Modifier.height(24.dp))

                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("חזור", color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun MentalityOption(
    title: String,
    description: String,
    icon: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Color(0xFF38BDF8).copy(alpha = 0.2f) else Color(0xFF0F172A))
            .border(
                width = 1.dp,
                color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick, enabled = !isSelected)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icon, fontSize = 28.sp)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = title, color = if (isSelected) Color(0xFF38BDF8) else Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(text = description, color = Color.LightGray, fontSize = 12.sp)
        }
    }
}
