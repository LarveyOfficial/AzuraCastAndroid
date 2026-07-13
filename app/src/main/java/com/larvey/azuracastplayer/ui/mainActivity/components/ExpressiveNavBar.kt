package com.larvey.azuracastplayer.ui.mainActivity.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.larvey.azuracastplayer.ui.mainActivity.AppDestinations
import com.larvey.azuracastplayer.ui.theme.AppMotion

/**
 * A floating "pill" bottom navigation bar with Material 3 Expressive motion, adapted from
 * PixelPlayer's CustomNavigationBarItem. Rendered only on phones (tablets keep the stock
 * NavigationRail); it lives in the Scaffold bottomBar, stacked below the mini player.
 */
@Composable
fun FloatingExpressiveNavBar(
  destinations: List<AppDestinations>,
  current: AppDestinations,
  onSelect: (AppDestinations) -> Unit,
  fusedTop: Boolean,
  modifier: Modifier = Modifier
) {
  // Top corners square off (nesting under the mini player) only when one sits above; with no
  // mini player the bar is uniformly rounded. Animates as playback starts/stops.
  val topCorner by animateDpAsState(
    targetValue = if (fusedTop) 14.dp else 30.dp,
    animationSpec = AppMotion.spatial(),
    label = "navTopCorner"
  )
  Surface(
    modifier = modifier
      .fillMaxWidth()
      .padding(
        start = 12.dp,
        end = 12.dp,
        top = 4.dp,
        bottom = 6.dp
      ),
    shape = RoundedCornerShape(
      topStart = topCorner,
      topEnd = topCorner,
      bottomStart = 30.dp,
      bottomEnd = 30.dp
    ),
    color = MaterialTheme.colorScheme.surfaceContainer,
    shadowElevation = 3.dp
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(68.dp)
        .selectableGroup(),
      horizontalArrangement = Arrangement.SpaceAround,
      verticalAlignment = Alignment.CenterVertically
    ) {
      destinations.forEach { destination ->
        ExpressiveNavItem(
          selected = destination == current,
          onClick = { onSelect(destination) },
          icon = destination.icon,
          label = destination.label
        )
      }
    }
  }
}

@Composable
private fun RowScope.ExpressiveNavItem(
  selected: Boolean,
  onClick: () -> Unit,
  icon: ImageVector,
  label: String,
  indicatorColor: Color = MaterialTheme.colorScheme.secondaryContainer,
  selectedIconColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
  unselectedIconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
  selectedTextColor: Color = MaterialTheme.colorScheme.onSurface,
  unselectedTextColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
  val iconColor by animateColorAsState(
    targetValue = if (selected) selectedIconColor else unselectedIconColor,
    animationSpec = tween(durationMillis = 150),
    label = "navIconColor"
  )
  val textColor by animateColorAsState(
    targetValue = if (selected) selectedTextColor else unselectedTextColor,
    animationSpec = tween(durationMillis = 150),
    label = "navTextColor"
  )
  val iconScale by animateFloatAsState(
    targetValue = if (selected) 1.1f else 1f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessMedium
    ),
    label = "navIconScale"
  )

  Column(
    modifier = Modifier
      .weight(1f)
      .fillMaxHeight()
      .clickable(
        onClick = onClick,
        role = Role.Tab,
        interactionSource = remember { MutableInteractionSource() },
        indication = null
      ),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier.size(
        width = 64.dp,
        height = 32.dp
      )
    ) {
      // Bouncy scale-in pill indicator behind the selected icon.
      val indicatorProgress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(
          dampingRatio = Spring.DampingRatioMediumBouncy,
          stiffness = Spring.StiffnessLow
        ),
        label = "navIndicator"
      )
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 4.dp)
          .graphicsLayer {
            alpha = indicatorProgress
            scaleX = indicatorProgress
            scaleY = indicatorProgress
          }
          .background(
            color = indicatorColor,
            shape = RoundedCornerShape(16.dp)
          )
      )
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(
            width = 48.dp,
            height = 24.dp
          )
          .graphicsLayer {
            scaleX = iconScale
            scaleY = iconScale
          }
      ) {
        Icon(
          imageVector = icon,
          contentDescription = label,
          tint = iconColor,
          modifier = Modifier.size(24.dp)
        )
      }
    }
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium.copy(
        fontSize = 13.sp,
        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
      ),
      color = textColor,
      maxLines = 1
    )
  }
}
