package com.larvey.azuracastplayer.ui.mainActivity.components.settings

import android.content.Intent
import android.os.Build
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
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.Update
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
fun AboutDropdown() {
  var expanded by remember { mutableStateOf(false) }
  val context = LocalContext.current
  val packageInfo = context.packageManager.getPackageInfo(
    context.packageName,
    0
  )


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
        horizontal = 12.dp
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
        imageVector = Icons.Rounded.Info,
        contentDescription = "About Icon",
        modifier = Modifier.padding(end = 8.dp),
        tint = MaterialTheme.colorScheme.primary
      )
      Text(
        text = "About",
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
        val goToPlayStore = remember {
          Intent(
            Intent.ACTION_VIEW,
            "https://play.google.com/store/apps/details?id=com.larvey.azuracastplayer".toUri()
          )
        }
        Card(
          modifier = Modifier
            .padding(
              horizontal = 8.dp,
              vertical = 4.dp
            ),
          onClick = { context.startActivity(goToPlayStore) },
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
              imageVector = Icons.Rounded.Update,
              contentDescription = "Version",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier
                .padding(end = 8.dp)
            )
            Text(
              "${packageInfo.versionName} ${
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                  "(${packageInfo.longVersionCode})"
                } else {
                  ""
                }
              }",
              Modifier.weight(1f)
            )
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.ArrowRight,
              contentDescription = "Right Arrow"
            )
          }
        }
        val goToSourceCode = remember {
          Intent(
            Intent.ACTION_VIEW,
            "https://github.com/LarveyOfficial/AzuraCastAndroid".toUri()
          )
        }
        Card(
          modifier = Modifier.padding(
            horizontal = 8.dp,
            vertical = 4.dp
          ),
          onClick = { context.startActivity(goToSourceCode) },
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
              imageVector = Icons.Rounded.Code,
              contentDescription = "Source Code Icon",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(end = 8.dp)
            )
            Text(
              "Source Code",
              Modifier.weight(1f)
            )
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.ArrowRight,
              contentDescription = "Right Arrow"
            )
          }
        }
        val goToPrivacyPolicy = remember {
          Intent(
            Intent.ACTION_VIEW,
            "https://github.com/LarveyOfficial/AzuraCastAndroid/blob/main/PrivacyPolicy.md".toUri()
          )
        }
        Card(
          modifier = Modifier
            .padding(
              horizontal = 8.dp,
              vertical = 4.dp
            )
            .padding(bottom = 4.dp),
          onClick = { context.startActivity(goToPrivacyPolicy) },
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
              imageVector = Icons.Rounded.Policy,
              contentDescription = "Privacy Policy Icon",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(end = 8.dp)
            )
            Text(
              "Privacy Policy",
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