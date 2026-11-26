package app.aryan447.mpvium.ui.preferences.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.aryan447.mpvium.R
import app.aryan447.mpvium.preferences.AppIcon
import app.aryan447.mpvium.preferences.AppIconManager
import app.aryan447.mpvium.ui.theme.spacing

@Composable
fun AppIconPicker(
  currentIcon: AppIcon,
  onIconSelected: (AppIcon) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val haptic = LocalHapticFeedback.current

  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = MaterialTheme.spacing.medium, vertical = MaterialTheme.spacing.smaller),
  ) {
    Text(
      text = stringResource(R.string.pref_app_icon_title),
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(
      text = stringResource(R.string.pref_app_icon_summary),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
    ) {
      AppIcon.entries.forEach { icon ->
        val isSelected = icon == currentIcon
        AppIconOptionCard(
          icon = icon,
          isSelected = isSelected,
          onClick = {
            if (!isSelected) {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              onIconSelected(icon)
              AppIconManager.setAppIcon(context, icon)
            }
          },
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
}

@Composable
private fun AppIconOptionCard(
  icon: AppIcon,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val borderColor by animateColorAsState(
    targetValue = if (isSelected) {
      MaterialTheme.colorScheme.primary
    } else {
      MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    },
    label = "appIconCardBorder",
  )

  val containerColor by animateColorAsState(
    targetValue = if (isSelected) {
      MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
    } else {
      MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.35f)
    },
    label = "appIconCardContainer",
  )

  val cardShape = RoundedCornerShape(20.dp)

  Column(
    modifier = modifier
      .clip(cardShape)
      .background(containerColor)
      .border(
        width = if (isSelected) 2.dp else 1.dp,
        color = borderColor,
        shape = cardShape,
      )
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick,
      )
      .padding(MaterialTheme.spacing.medium),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Box(contentAlignment = Alignment.TopEnd) {
      val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
      val iconBackground = when (icon) {
        AppIcon.PORTAL -> if (isDark) {
          Brush.linearGradient(
            colors = listOf(
              Color(0xFFFF6B8B),
              Color(0xFFE040FB),
              Color(0xFF7C4DFF),
              Color(0xFF448AFF),
            ),
          )
        } else {
          SolidColor(Color.White)
        }
        AppIcon.CLASSIC -> SolidColor(Color(0xFF0E0B14))
      }
      val previewDrawable = when (icon) {
        AppIcon.PORTAL -> if (isDark) {
          R.drawable.ic_launcher_portal_monochrome
        } else {
          R.drawable.ic_splash_portal_foreground
        }
        AppIcon.CLASSIC -> icon.previewRes
      }
      Box(
        modifier = Modifier
          .size(68.dp)
          .clip(RoundedCornerShape(18.dp))
          .background(iconBackground)
          .padding(8.dp),
        contentAlignment = Alignment.Center,
      ) {
        Image(
          painter = painterResource(previewDrawable),
          contentDescription = null,
          modifier = Modifier.size(52.dp),
        )
      }

      if (isSelected) {
        Box(
          modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(14.dp),
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(MaterialTheme.spacing.smaller))

    Text(
      text = stringResource(icon.titleRes),
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
      color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
      textAlign = TextAlign.Center,
      maxLines = 1,
    )
  }
}
