package app.aryan447.mpvium.ui.preferences

import android.content.Intent
import android.content.pm.PackageManager
import android.widget.ImageView
import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import app.aryan447.mpvium.BuildConfig
import app.aryan447.mpvium.R
import app.aryan447.mpvium.preferences.AppearancePreferences
import app.aryan447.mpvium.presentation.Screen
import app.aryan447.mpvium.presentation.crash.CrashActivity.Companion.collectDeviceInfo
import app.aryan447.mpvium.ui.onboarding.OnboardingScreen
import app.aryan447.mpvium.ui.theme.LocalGlass
import app.aryan447.mpvium.ui.theme.glassRimStroke
import app.aryan447.mpvium.ui.theme.glassSheen
import app.aryan447.mpvium.ui.utils.LocalBackStack
import app.aryan447.mpvium.utils.update.UpdateViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import org.koin.compose.koinInject

@Serializable
object AboutScreen : Screen {
  @Suppress("DEPRECATION")
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val backstack = LocalBackStack.current
    val clipboardManager = LocalClipboardManager.current
    val appearancePreferences = koinInject<AppearancePreferences>()
    val packageManager: PackageManager = context.packageManager
    val packageInfo = packageManager.getPackageInfo(context.packageName, 0)
    val versionName = packageInfo.versionName?.substringBefore('-') ?: packageInfo.versionName ?: BuildConfig.VERSION_NAME
    val buildType = BuildConfig.BUILD_TYPE

    // Conditionally initialize update feature based on build config
    val updateViewModel: UpdateViewModel? = if (BuildConfig.ENABLE_UPDATE_FEATURE) {
      viewModel(context as androidx.activity.ComponentActivity)
    } else {
      null
    }
    val updateState by (updateViewModel?.updateState ?: MutableStateFlow(UpdateViewModel.UpdateState.Idle)).collectAsState()

    // Show toast when no update is available after manual check (only if update feature is enabled)
    LaunchedEffect(updateState) {
      if (BuildConfig.ENABLE_UPDATE_FEATURE && updateViewModel != null && updateState is UpdateViewModel.UpdateState.NoUpdate) {
        Toast.makeText(context, "Already using latest version", Toast.LENGTH_SHORT).show()
        updateViewModel.dismissNoUpdate()
      }
    }

    Scaffold(
      topBar = {
        SettingsTopBar(title = stringResource(id = R.string.pref_about_title))
      },
    ) { paddingValues ->
      ProvidePreferenceLocals {
        val cs = MaterialTheme.colorScheme
        val isGlass = LocalGlass.current
        val dark = androidx.compose.foundation.isSystemInDarkTheme()
        val transition = rememberInfiniteTransition()
        val fraction by transition.animateFloat(
          initialValue = 0f,
          targetValue = 1f,
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 5000),
              repeatMode = RepeatMode.Reverse,
            ),
        )

        Column(
          modifier =
            Modifier
              .padding(paddingValues)
              .verticalScroll(rememberScrollState()),
        ) {
          Card(
            modifier =
              Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .glassSheen(SettingsCardShape, isGlass),
            shape = SettingsCardShape,
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = if (isGlass) glassRimStroke(dark) else BorderStroke(
              1.dp,
              cs.outlineVariant.copy(alpha = 0.4f),
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
          ) {
            Box(
              modifier =
                Modifier
                  .fillMaxWidth()
                  .background(
                    Brush.linearGradient(
                      colors = listOf(
                        cs.primaryContainer,
                        cs.tertiaryContainer,
                        cs.secondaryContainer,
                      ),
                    ),
                  ),
            ) {
              // Decorative orbs for depth, gently drifted by the retained animation.
              Box(
                modifier =
                  Modifier
                    .size(180.dp)
                    .offset(x = (120 + fraction * 24).dp, y = (-70 - fraction * 12).dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.14f))
                    .align(Alignment.TopEnd),
              )
              Box(
                modifier =
                  Modifier
                    .size(120.dp)
                    .offset(x = (60 - fraction * 16).dp, y = (40 + fraction * 12).dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.10f))
                    .align(Alignment.TopEnd),
              )
              // Top gloss.
              Box(
                modifier =
                  Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(
                      Brush.verticalGradient(
                        colors = listOf(
                          Color.White.copy(alpha = 0.16f),
                          Color.Transparent,
                        ),
                      ),
                    )
                    .align(Alignment.TopCenter),
              )
              Column(
                modifier =
                  Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Box(
                    modifier =
                      Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f))
                        .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center,
                  ) {
                    Box(
                      modifier =
                        Modifier
                          .size(40.dp)
                          .clip(CircleShape),
                      contentAlignment = Alignment.Center,
                    ) {
                      AndroidView(
                        modifier = Modifier.matchParentSize(),
                        factory = { ctx ->
                          ImageView(ctx).apply {
                            setImageResource(R.mipmap.ic_launcher)
                          }
                        },
                      )
                    }
                  }

                  Spacer(modifier = Modifier.width(16.dp))

                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "mpvium",
                      style = MaterialTheme.typography.headlineSmall,
                      fontWeight = FontWeight.ExtraBold,
                      color = cs.onPrimaryContainer,
                    )
                    Spacer(Modifier.height(6.dp))
                    Surface(
                      shape = CircleShape,
                      color = cs.primary.copy(alpha = 0.9f),
                    ) {
                      Text(
                        text = "v$versionName $buildType",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = cs.onPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                      )
                    }
                  }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                  Button(
                    onClick = { backstack.add(LibrariesScreen) },
                    modifier =
                      Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = SettingsTileShape,
                    colors =
                      ButtonDefaults.buttonColors(
                        containerColor = cs.primary,
                        contentColor = cs.onPrimary,
                      ),
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.LibraryBooks,
                      contentDescription = null,
                      modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                      text = stringResource(id = R.string.pref_about_oss_libraries),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.SemiBold,
                    )
                  }

                  Button(
                    onClick = {
                      context.startActivity(
                        Intent(
                          Intent.ACTION_VIEW,
                          context.getString(R.string.github_repo_url).toUri(),
                        ),
                      )
                    },
                    modifier =
                      Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = SettingsTileShape,
                    colors =
                      ButtonDefaults.buttonColors(
                        containerColor = cs.primary,
                        contentColor = cs.onPrimary,
                      ),
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.OpenInNew,
                      contentDescription = null,
                      modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                      text = "GitHub",
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.SemiBold,
                    )
                  }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                  onClick = {
                    appearancePreferences.onboardingCompleted.set(false)
                    backstack.add(OnboardingScreen)
                  },
                  modifier =
                    Modifier
                      .fillMaxWidth()
                      .height(56.dp),
                  shape = SettingsCardShape,
                  colors =
                    ButtonDefaults.buttonColors(
                      containerColor = cs.secondaryContainer,
                      contentColor = cs.onSecondaryContainer,
                    ),
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(Modifier.width(8.dp))
                  Text(
                    text = stringResource(id = R.string.onboarding_replay),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                  )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Column(
                  modifier =
                    Modifier
                      .fillMaxWidth()
                      .clickable {
                        clipboardManager.setText(AnnotatedString(collectDeviceInfo()))
                      },
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.Smartphone,
                      contentDescription = "Device Info",
                      modifier = Modifier.size(20.dp),
                      tint = cs.onPrimaryContainer,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = "Device Info",
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.SemiBold,
                      color = cs.onPrimaryContainer,
                    )
                  }
                  Text(
                    text = collectDeviceInfo(),
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onPrimaryContainer.copy(alpha = 0.85f),
                  )
                }
              }
            }
          }

          Spacer(Modifier.height(8.dp))

          // Updates Section (only show if update feature is enabled)
          if (BuildConfig.ENABLE_UPDATE_FEATURE && updateViewModel != null) {
            PreferenceSectionHeader(title = "Updates", count = 2)
            PreferenceCard {
              val isAutoUpdateEnabled by updateViewModel.isAutoUpdateEnabled.collectAsState()
              HapticSwitchPreference(
                value = isAutoUpdateEnabled,
                onValueChange = { updateViewModel.toggleAutoUpdate(it) },
                icon = { PreferenceIconBox(icon = Icons.Outlined.Update) },
                title = { Text(text = "Auto Check for Updates") },
                summary = {
                  Text(
                    text = "Check on startup",
                    color = cs.onSurfaceVariant,
                  )
                },
              )

              PreferenceDivider()

              Column(modifier = Modifier.padding(16.dp)) {
                Button(
                  onClick = { updateViewModel.checkForUpdate(manual = true) },
                  modifier = Modifier.fillMaxWidth().height(50.dp),
                  shape = SettingsTileShape,
                  colors = ButtonDefaults.buttonColors(
                    containerColor = cs.secondaryContainer,
                    contentColor = cs.onSecondaryContainer
                  ),
                  elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                  Icon(Icons.Outlined.CheckCircle, null, modifier = Modifier.size(18.dp))
                  Spacer(Modifier.width(8.dp))
                  Text("Check for Updates Now", fontWeight = FontWeight.SemiBold)
                }
              }
            }

            Spacer(Modifier.height(8.dp))
          }

          Spacer(Modifier.height(12.dp))
        }
      }
    }
  }
}

@Suppress("DEPRECATION")
@Serializable
object LibrariesScreen : Screen {
  private data class LibraryInfo(
    val name: String,
    val copyright: String,
    val license: String,
    val url: String,
  )

  private val libraries = listOf(
    LibraryInfo(
      name = "mpv",
      copyright = "Copyright (c) the mpv contributors",
      license = "GPL-2.0-or-later — playback engine (libmpv)",
      url = "https://mpv.io",
    ),
    LibraryInfo(
      name = "mpv-android",
      copyright = "Copyright (c) the mpv-android contributors",
      license = "GPL-3.0-or-later — Android player bridge",
      url = "https://github.com/mpv-android/mpv-android",
    ),
    LibraryInfo(
      name = "FFmpeg",
      copyright = "Copyright (c) the FFmpeg developers",
      license = "LGPL-2.1-or-later / GPL-2.0-or-later — bundled codecs",
      url = "https://ffmpeg.org",
    ),
    LibraryInfo(
      name = "MediaInfo",
      copyright = "Copyright (c) MediaArea.net SARL",
      license = "BSD-2-Clause — media analysis",
      url = "https://mediaarea.net/MediaInfo",
    ),
    LibraryInfo(
      name = "Anime4K shaders",
      copyright = "Copyright (c) 2019-2021 bloc97",
      license = "MIT — upscaling shaders",
      url = "https://github.com/bloc97/Anime4K",
    ),
    LibraryInfo(
      name = "Fossify File Manager",
      copyright = "Copyright (c) the Fossify contributors",
      license = "GPL-3.0-or-later — file-browser logic adapted",
      url = "https://github.com/FossifyOrg/File-Manager",
    ),
    LibraryInfo(
      name = "Android Jetpack & Compose",
      copyright = "Copyright (c) Google LLC / AOSP",
      license = "Apache-2.0",
      url = "https://source.android.com",
    ),
  )

  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val backstack = LocalBackStack.current
    val context = LocalContext.current
    Scaffold(
      topBar = {
        SettingsTopBar(title = stringResource(id = R.string.pref_about_oss_libraries))
      },
    ) { paddingValues ->
      Column(
        modifier =
          Modifier
            .padding(paddingValues)
            .verticalScroll(rememberScrollState()),
      ) {
        PreferenceSectionHeader(title = "This app (GPL-3.0-or-later)")
        PreferenceCard {
          Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            PreferenceIconBox(icon = Icons.Outlined.Description)
            Spacer(modifier = Modifier.width(16.dp))
            Text(
              text = "mpvium as a whole is distributed under the GNU General Public License v3 or later. See LICENSE in the source repository. Individual components below keep their own licenses.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.weight(1f),
            )
          }
        }
        PreferenceSectionHeader(title = "Open-source components", count = libraries.size)
        PreferenceCard {
          libraries.forEachIndexed { index, lib ->
            if (index > 0) PreferenceDivider()
            Row(
              modifier =
                Modifier
                  .fillMaxWidth()
                  .clickable {
                    context.startActivity(Intent(Intent.ACTION_VIEW, lib.url.toUri()))
                  }
                  .padding(horizontal = 16.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              PreferenceIconBox(icon = Icons.Outlined.LibraryBooks)
              Spacer(modifier = Modifier.width(16.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = lib.name,
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.SemiBold,
                )
                Text(
                  text = lib.copyright,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                  text = lib.license,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Spacer(modifier = Modifier.width(8.dp))
              Box(
                modifier =
                  Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Outlined.OpenInNew,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(18.dp),
                )
              }
            }
          }
        }
        Spacer(modifier = Modifier.height(16.dp))
      }
    }
  }
}
