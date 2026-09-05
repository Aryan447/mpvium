package app.aryan447.mpvium.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.aryan447.mpvium.R
import app.aryan447.mpvium.preferences.AppearancePreferences
import app.aryan447.mpvium.presentation.Screen
import app.aryan447.mpvium.ui.browser.MainScreen
import app.aryan447.mpvium.ui.utils.LocalBackStack
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

  @Composable
  override fun Content() {
    val backstack = LocalBackStack.current
    val preferences = koinInject<AppearancePreferences>()
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState { onboardingPages.size }
    val isLastPage = pagerState.currentPage == onboardingPages.lastIndex

    fun finishOnboarding() {
      haptic.performHapticFeedback(HapticFeedbackType.LongPress)
      preferences.onboardingCompleted.set(true)
      backstack.clear()
      backstack.add(MainScreen)
    }

    // System back goes to the previous page instead of exiting the app.
    BackHandler(enabled = pagerState.currentPage > 0) {
      scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
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
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
        ) {
          if (!isLastPage) {
            TextButton(onClick = ::finishOnboarding) {
              Text(text = stringResource(R.string.onboarding_skip))
            }
          } else {
            // Keep the header height stable across pages.
            Spacer(modifier = Modifier.height(40.dp))
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
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Box(
              modifier = Modifier
                .size(128.dp)
                .clip(RoundedCornerShape(36.dp))
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
            val dotWidth = animateDpAsState(
              targetValue = if (selected) 28.dp else 8.dp,
              label = "onboarding_dot",
            )
            Box(
              modifier = Modifier
                .padding(horizontal = 4.dp)
                .height(8.dp)
                .width(dotWidth.value)
                .clip(CircleShape)
                .background(
                  if (selected) {
                    MaterialTheme.colorScheme.primary
                  } else {
                    MaterialTheme.colorScheme.surfaceVariant
                  },
                ),
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
              scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            }
          },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = stringResource(
              if (isLastPage) {
                R.string.onboarding_get_started
              } else {
                R.string.onboarding_next
              },
            ),
            modifier = Modifier.padding(vertical = 4.dp),
          )
        }

        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }
}
