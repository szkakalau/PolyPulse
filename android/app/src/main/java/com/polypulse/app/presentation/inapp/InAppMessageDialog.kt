package com.polypulse.app.presentation.inapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.polypulse.app.data.remote.dto.InAppMessageDto
import com.polypulse.app.presentation.profile.PaywallPlanCard

@Composable
fun InAppMessageDialog(
    message: InAppMessageDto,
    onDismiss: () -> Unit,
    onCtaClick: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = message.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = message.body,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                message.plans?.let { plans ->
                    plans.forEach { plan ->
                        PaywallPlanCard(plan = plan, isSelected = false, onSelected = {})
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                Button(
                    onClick = { onCtaClick(message.ctaAction) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = message.ctaText)
                }
            }
        }
    }
}
