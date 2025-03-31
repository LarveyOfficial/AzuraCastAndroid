package com.larvey.azuracastplayer.ui.mainActivity.settings.components

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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.larvey.azuracastplayer.db.settings.SettingsViewModel
import com.larvey.azuracastplayer.db.settings.SettingsViewModel.SettingsModelProvider
import com.larvey.azuracastplayer.db.settings.ThemeTypes
import kotlinx.coroutines.launch

@Composable
fun ThemePicker() {
  val scope = rememberCoroutineScope()
  val settingsModel: SettingsViewModel = viewModel(factory = SettingsModelProvider.Factory)
  val isDynamic by settingsModel.isDynamic.collectAsState()
  val themeType by settingsModel.themeType.collectAsState()
  var expanded by remember { mutableStateOf(false) }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .animateContentSize(
        animationSpec = tween(
          durationMillis = 300,
          easing = LinearOutSlowInEasing
        )
      )
      .padding(horizontal = 12.dp),
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
        imageVector = Icons.Outlined.Brush,
        contentDescription = "Theme Picker",
        modifier = Modifier.padding(end = 8.dp),
        tint = MaterialTheme.colorScheme.primary
      )
      Text(
        text = "Theme",
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
        Card(
          modifier = Modifier.padding(
            horizontal = 8.dp,
            vertical = 4.dp
          ),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
          Row(
            Modifier
              .fillMaxWidth()
              .height(42.dp)
              .selectable(
                selected = (themeType == ThemeTypes.SYSTEM),
                onClick = {
                  scope.launch {
                    settingsModel.updateThemeType(ThemeTypes.SYSTEM)
                  }
                },
                role = Role.RadioButton
              )
              .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Rounded.BrightnessAuto,
              contentDescription = "Default Theme Icon",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(end = 8.dp)
            )
            Text(
              "Default",
              Modifier.weight(1f)
            )
            RadioButton(
              selected = themeType == ThemeTypes.SYSTEM,
              onClick = null
            )
          }
        }
        Card(
          modifier = Modifier.padding(
            horizontal = 8.dp,
            vertical = 4.dp
          ),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
          Row(
            Modifier
              .fillMaxWidth()
              .height(42.dp)
              .selectable(
                selected = (themeType == ThemeTypes.LIGHT),
                onClick = {
                  scope.launch {
                    settingsModel.updateThemeType(ThemeTypes.LIGHT)
                  }
                },
                role = Role.RadioButton
              )
              .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Rounded.WbSunny,
              contentDescription = "Light Theme Icon",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(end = 8.dp)
            )
            Text(
              "Light",
              Modifier.weight(1f)
            )
            RadioButton(
              selected = themeType == ThemeTypes.LIGHT,
              onClick = null
            )
          }
        }
        Card(
          modifier = Modifier
            .padding(
              horizontal = 8.dp,
              vertical = 4.dp
            )
            .padding(bottom = 4.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
          Row(
            Modifier
              .fillMaxWidth()
              .height(42.dp)
              .selectable(
                selected = (themeType == ThemeTypes.DARK),
                onClick = { scope.launch { settingsModel.updateThemeType(ThemeTypes.DARK) } },
                role = Role.RadioButton
              )
              .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Rounded.NightsStay,
              contentDescription = "Night Theme Icon",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(end = 8.dp)
            )
            Text(
              "Dark",
              Modifier.weight(1f)
            )
            RadioButton(
              selected = themeType == ThemeTypes.DARK,
              onClick = null
            )
          }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          Card(
            modifier = Modifier
              .padding(
                horizontal = 8.dp,
                vertical = 4.dp
              )
              .padding(bottom = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
          ) {
            Row(
              Modifier
                .fillMaxWidth()
                .height(52.dp)
                .selectable(
                  selected = (isDynamic),
                  onClick = { scope.launch { settingsModel.toggleDynamicTheme() } },
                  role = Role.Switch
                )
                .padding(horizontal = 10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Rounded.ColorLens,
                contentDescription = "Dynamic Color Icon",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 8.dp)
              )
              Text(
                "Dynamic Color",
                Modifier.weight(1f)
              )
              Switch(
                checked = isDynamic,
                onCheckedChange = null
              )
            }
          }
        }
      }
    }
  }
}