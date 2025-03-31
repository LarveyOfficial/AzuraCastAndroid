package com.larvey.azuracastplayer.ui.mainActivity.settings.components

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowRight
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri

@Composable
fun ContactMeDropdown() {
  var expanded by remember { mutableStateOf(false) }
  val context = LocalContext.current
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .animateContentSize(
        animationSpec = tween(
          durationMillis = 300,
          easing = LinearOutSlowInEasing
        )
      )
      .padding(
        12.dp
      ),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    shape = RoundedCornerShape(12.dp),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .clickable { expanded = !expanded }
        .padding(
          horizontal = 10.dp,
          vertical = 12.dp
        ),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center

    ) {
      Icon(
        imageVector = Icons.Rounded.Person,
        contentDescription = "Contact Me Icon",
        modifier = Modifier.padding(end = 8.dp),
        tint = MaterialTheme.colorScheme.primary
      )
      Text(
        text = "Contact Me",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.weight(1f)
      )
      val rotate by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "Arrow"
      )
      Icon(
        imageVector = Icons.Rounded.ArrowDropDown,
        contentDescription = "Drop Down",
        modifier = Modifier.rotate(rotate)
      )
    }
    AnimatedVisibility(expanded) {
      Column {
        val emailMe = Intent(Intent.ACTION_SENDTO)
        emailMe.data = "mailto:luis@mtu.lol".toUri()
        Card(
          modifier = Modifier
            .padding(
              horizontal = 8.dp,
              vertical = 4.dp
            ),
          onClick = { context.startActivity(emailMe) },
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
          Row(
            Modifier
              .fillMaxWidth()
              .height(42.dp)
              .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Rounded.AlternateEmail,
              contentDescription = "Email Icon",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier
                .padding(end = 8.dp)
            )
            Column(Modifier.weight(1f)) {
              Text(
                "Email",
              )
              Text(
                text = "luis@mtu.lol",
                style = MaterialTheme.typography.labelSmall
              )
            }

            Icon(
              imageVector = Icons.AutoMirrored.Rounded.ArrowRight,
              contentDescription = "Right Arrow"
            )
          }
        }
        val goToReportIssue = remember {
          Intent(
            Intent.ACTION_VIEW,
            "https://github.com/LarveyOfficial/AzuraCastAndroid/issues".toUri()
          )
        }
        Card(
          modifier = Modifier.padding(
            horizontal = 8.dp,
            vertical = 4.dp
          ),
          onClick = { context.startActivity(goToReportIssue) },
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
          Row(
            Modifier
              .fillMaxWidth()
              .height(42.dp)
              .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Rounded.BugReport,
              contentDescription = "Issue Icon",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(end = 8.dp)
            )
            Text(
              "Report a Bug",
              Modifier.weight(1f)
            )
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.ArrowRight,
              contentDescription = "Right Arrow"
            )
          }
        }
        val goToKoFi = remember {
          Intent(
            Intent.ACTION_VIEW,
            "https://ko-fi.com/larveyofficial".toUri()
          )
        }
        Card(
          modifier = Modifier.padding(
            horizontal = 8.dp,
            vertical = 4.dp
          ),
          onClick = { context.startActivity(goToKoFi) },
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
          Row(
            Modifier
              .fillMaxWidth()
              .height(42.dp)
              .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Rounded.Coffee,
              contentDescription = "Tip Icon",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(end = 8.dp)
            )
            Text(
              "Leave a tip",
              Modifier.weight(1f)
            )
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.ArrowRight,
              contentDescription = "Right Arrow"
            )
          }
        }
      }
    }
  }
}