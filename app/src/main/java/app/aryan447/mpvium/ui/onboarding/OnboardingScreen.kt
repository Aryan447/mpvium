package app.aryan447.mpvium.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.aryan447.mpvium.R
import app.aryan447.mpvium.preferences.AppearancePreferences
import app.aryan447.mpvium.presentation.Screen
import app.aryan447.mpvium.ui.browser.MainScreen
import app.aryan447.mpvium.ui.utils.LocalBackStack
import kotlin.math.abs
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

private data class OnboardingPage(
  val icon: ImageVector,
  val titleRes: Int,
  val messageRes: Int,
)

private val onboardingPages = listOf(
  OnboardingPage(
    icon = Icons.Filled.PlayCircle,
    titleRes = R.string.onboarding_page1_title,
    messageRes = R.string.onboarding_page1_message,
  ),
  OnboardingPage(
    icon = Icons.Filled.Folder,
    titleRes = R.string.onboarding_page2_title,
    messageRes = R.string.onboarding_page2_message,
  ),
  OnboardingPage(
    icon = Icons.Filled.Palette,
    titleRes = R.string.onboarding_page3_title,
    messageRes = R.string.onboarding_page3_message,
  ),
)

@Serializable
object OnboardingScreen : Screen {

  @OptIn(ExperimentalMaterial3ExpressiveApi::class)
  @Composable
  override fun Content() {
    val backstack = LocalBackStack.current
    val preferences = koinInject<AppearancePreferences>()
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState { onboardingPages.size }
    val isLastPage = pagerState.currentPage == onboardingPages.lastIndex

    val enterEffects = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val exitEffects = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
    val enterOffset = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
    val exitOffset = MaterialTheme.motionScheme.fastSpatialSpec<IntOffset>()
    val pagerScrollSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()

    val buttonInteraction = remember { MutableInteractionSource() }
    val buttonPressed by buttonInteraction.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(
      targetValue = if (buttonPressed) 0.97f else 1f,
      animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
      label = "onboarding_primary_press",
    )

    fun finishOnboarding() {
      haptic.performHapticFeedback(HapticFeedbackType.LongPress)
      preferences.onboardingCompleted.set(true)
      backstack.clear()
      backstack.add(MainScreen)
    }

    // System back goes to the previous page instead of exiting the app.
    BackHandler(enabled = pagerState.currentPage > 0) {
      scope.launch {
        pagerState.animateScrollToPage(
          page = pagerState.currentPage - 1,
          animationSpec = pagerScrollSpec,
        )
      }
    }

    Scaffold(
      modifier = Modifier.fillMaxSize(),
    ) { padding ->
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(padding)
          .padding(horizontal = 32.dp),
      ) {
        // Skip action, hidden on the last page where the primary
        // button already finishes onboarding.
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(40.dp),
          contentAlignment = Alignment.CenterEnd,
        ) {
          AnimatedContent(
            targetState = !isLastPage,
            transitionSpec = {
              fadeIn(animationSpec = enterEffects) togetherWith
                fadeOut(animationSpec = exitEffects)
            },
            contentAlignment = Alignment.CenterEnd,
            label = "onboarding_skip",
          ) { showSkip ->
            if (showSkip) {
              TextButton(onClick = ::finishOnboarding) {
                Text(text = stringResource(R.string.onboarding_skip))
              }
            }
          }
        }

        HorizontalPager(
          state = pagerState,
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          verticalAlignment = Alignment.CenterVertically,
        ) { index ->
          val page = onboardingPages[index]
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .graphicsLayer {
                val offset = abs(pagerState.getOffsetDistanceInPages(index)).coerceIn(0f, 1f)
                alpha = 1f - offset * 0.35f
                val pageScale = 1f - offset * 0.08f
                scaleX = pageScale
                scaleY = pageScale
                translationY = offset * 24.dp.toPx()
              },
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            val iconCorner by animateDpAsState(
              targetValue = when {
                pagerState.currentPage != index -> 36.dp
                index == 1 -> 52.dp
                index == 2 -> 28.dp
                else -> 36.dp
              },
              animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
              label = "onboarding_icon_shape",
            )
            Box(
              modifier = Modifier
                .size(128.dp)
                .clip(RoundedCornerShape(iconCorner))
                .background(MaterialTheme.colorScheme.primaryContainer),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = page.icon,
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
              )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
              text = stringResource(page.titleRes),
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.Bold,
              textAlign = TextAlign.Center,
              color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
              text = stringResource(page.messageRes),
              style = MaterialTheme.typography.bodyLarge,
              textAlign = TextAlign.Center,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        // Page indicator dots; the active dot stretches into a pill.
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          repeat(onboardingPages.size) { index ->
            val selected = index == pagerState.currentPage
            val dotWidth by animateDpAsState(
              targetValue = if (selected) 28.dp else 8.dp,
              animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
              label = "onboarding_dot_width",
            )
            val dotColor by animateColorAsState(
              targetValue = if (selected) {
                MaterialTheme.colorScheme.primary
              } else {
                MaterialTheme.colorScheme.surfaceVariant
              },
              animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
              label = "onboarding_dot_color",
            )
            Box(
              modifier = Modifier
                .padding(horizontal = 4.dp)
                .height(8.dp)
                .width(dotWidth)
                .clip(CircleShape)
                .background(dotColor),
            )
          }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
          onClick = {
            if (isLastPage) {
              finishOnboarding()
            } else {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              scope.launch {
                pagerState.animateScrollToPage(
                  page = pagerState.currentPage + 1,
                  animationSpec = pagerScrollSpec,
                )
              }
            }
          },
          interactionSource = buttonInteraction,
          modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
              scaleX = buttonScale
              scaleY = buttonScale
            },
        ) {
          AnimatedContent(
            targetState = isLastPage,
            transitionSpec = {
              (
                fadeIn(animationSpec = enterEffects) +
                  slideInVertically(animationSpec = enterOffset) { it / 2 }
                ).togetherWith(
                  fadeOut(animationSpec = exitEffects) +
                    slideOutVertically(animationSpec = exitOffset) { -it / 2 },
                )
            },
            contentAlignment = Alignment.Center,
            label = "onboarding_primary_label",
          ) { lastPage ->
            Text(
              text = stringResource(
                if (lastPage) {
                  R.string.onboarding_get_started
                } else {
                  R.string.onboarding_next
                },
              ),
              modifier = Modifier.padding(vertical = 4.dp),
            )
          }
        }

        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }
}
