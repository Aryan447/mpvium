package app.aryan447.mpvium.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import app.aryan447.mpvium.MainActivity
import app.aryan447.mpvium.R
import app.aryan447.mpvium.domain.playbackstate.repository.PlaybackStateRepository
import app.aryan447.mpvium.domain.recentlyplayed.repository.RecentlyPlayedRepository
import app.aryan447.mpvium.ui.player.PlayerActivity
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

/**
 * Home-screen "Continue watching" widget. Shows up to three recent
 * in-progress videos with progress; tapping one resumes playback.
 * Header opens the app, the sync icon forces a refresh.
 *
 * Battery: no periodic polling (updatePeriodMillis=0). The widget
 * refreshes on demand — manual tap, resize, and automatically when the
 * player pauses/exits and saves its playback state.
 */
class ContinueWatchingWidgetProvider : AppWidgetProvider() {

  companion object {
    private const val TAG = "ContinueWidget"
    private const val ACTION_REFRESH = "app.aryan447.mpvium.action.WIDGET_REFRESH"
    private const val MAX_ITEMS = 3
    private const val WATCHED_THRESHOLD = 0.95f

    private data class WidgetItem(
      val title: String,
      val subtitle: String,
      val progress: Int,
      val filePath: String,
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Refresh all widget instances. Safe to call from anywhere (e.g.
     * PlayerActivity.onPause after saving playback state). Never throws.
     */
    fun refreshAll(context: Context) {
      scope.launch {
        runCatching {
          refreshWidgets(context.applicationContext)
        }.onFailure { e ->
          Log.w(TAG, "Widget refresh failed", e)
        }
      }
    }

    private suspend fun refreshWidgets(context: Context) {
      val manager = AppWidgetManager.getInstance(context)
      val ids = manager.getAppWidgetIds(
        ComponentName(context, ContinueWatchingWidgetProvider::class.java),
      )
      if (ids.isEmpty()) return
      val items = loadItems()
      ids.forEach { id ->
        runCatching {
          manager.updateAppWidget(id, buildViews(context, id, items))
        }.onFailure { e ->
          Log.w(TAG, "Widget update failed", e)
        }
      }
    }

    private suspend fun loadItems(): List<WidgetItem> {
      return try {
        val koin = GlobalContext.get()
        val recentRepo: RecentlyPlayedRepository = koin.get()
        val stateRepo: PlaybackStateRepository = koin.get()
        recentRepo.getRecentlyPlayed(10).mapNotNull { entity ->
          val title = entity.videoTitle?.takeIf { it.isNotBlank() } ?: entity.fileName
          val state = runCatching { stateRepo.getVideoDataByTitle(title) }.getOrNull()
            ?: runCatching { stateRepo.getVideoDataByTitle(entity.fileName) }.getOrNull()
          val positionMs = (state?.lastPosition ?: 0) * 1000L
          val durationMs = entity.duration.takeIf { it > 0 }
            ?: state?.let { (it.lastPosition + it.timeRemaining) * 1000L }?.takeIf { d -> d > 0 }
            ?: 0L
          val progress = if (durationMs > 0) {
            (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
          } else {
            0f
          }
          if (positionMs <= 10_000L || progress >= WATCHED_THRESHOLD) return@mapNotNull null
          val remainingMins = ((durationMs - positionMs) / (1000 * 60)).coerceAtLeast(0)
          WidgetItem(
            title = title,
            subtitle = if (remainingMins > 0) "$remainingMins min left" else "Tap to resume",
            progress = (progress * 100).toInt(),
            filePath = entity.filePath,
          )
        }.take(MAX_ITEMS)
      } catch (e: Exception) {
        Log.w(TAG, "Widget data load failed", e)
        emptyList()
      }
    }

    private fun buildViews(
      context: Context,
      appWidgetId: Int,
      items: List<WidgetItem>,
    ): RemoteViews {
      val views = RemoteViews(context.packageName, R.layout.widget_continue_watching)

      // Header opens the app.
      views.setOnClickPendingIntent(
        R.id.widget_title,
        PendingIntent.getActivity(
          context,
          appWidgetId,
          Intent(context, MainActivity::class.java),
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        ),
      )

      // Manual refresh.
      val refreshIntent = Intent(context, ContinueWatchingWidgetProvider::class.java).apply {
        action = ACTION_REFRESH
      }
      views.setOnClickPendingIntent(
        R.id.widget_refresh,
        PendingIntent.getBroadcast(
          context,
          appWidgetId,
          refreshIntent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        ),
      )

      val containers = listOf(R.id.widget_item_1, R.id.widget_item_2, R.id.widget_item_3)
      val titles = listOf(R.id.widget_item_1_title, R.id.widget_item_2_title, R.id.widget_item_3_title)
      val subtitles = listOf(R.id.widget_item_1_subtitle, R.id.widget_item_2_subtitle, R.id.widget_item_3_subtitle)
      val bars = listOf(R.id.widget_item_1_progress, R.id.widget_item_2_progress, R.id.widget_item_3_progress)

      views.setViewVisibility(
        R.id.widget_empty,
        if (items.isEmpty()) View.VISIBLE else View.GONE,
      )
      views.setOnClickPendingIntent(
        R.id.widget_empty,
        PendingIntent.getActivity(
          context,
          appWidgetId,
          Intent(context, MainActivity::class.java),
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        ),
      )

      containers.forEachIndexed { index, containerId ->
        val item = items.getOrNull(index)
        if (item == null) {
          views.setViewVisibility(containerId, View.GONE)
        } else {
          views.setViewVisibility(containerId, View.VISIBLE)
          views.setTextViewText(titles[index], item.title)
          views.setTextViewText(subtitles[index], item.subtitle)
          views.setProgressBar(bars[index], 100, item.progress, false)
          views.setOnClickPendingIntent(containerId, playPendingIntent(context, appWidgetId, index, item))
        }
      }
      return views
    }

    private fun playPendingIntent(
      context: Context,
      appWidgetId: Int,
      index: Int,
      item: WidgetItem,
    ): PendingIntent {
      // Mirrors MediaUtils.playFile(String): local paths become file Uris,
      // anything else is parsed as-is.
      val uri: Uri = if (item.filePath.startsWith("/")) {
        Uri.fromFile(File(item.filePath))
      } else {
        Uri.parse(item.filePath)
      }
      val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        setClass(context, PlayerActivity::class.java)
        putExtra("internal_launch", true)
        putExtra("launch_source", "widget")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
      return PendingIntent.getActivity(
        context,
        appWidgetId * 10 + index,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
    }
  }

  override fun onUpdate(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetIds: IntArray,
  ) {
    val pendingResult = goAsync()
    scope.launch {
      try {
        refreshWidgets(context)
      } finally {
        pendingResult.finish()
      }
    }
  }

  override fun onReceive(context: Context, intent: Intent) {
    super.onReceive(context, intent)
    if (intent.action == ACTION_REFRESH) {
      val pendingResult = goAsync()
      scope.launch {
        try {
          refreshWidgets(context)
        } finally {
          pendingResult.finish()
        }
      }
    }
  }

  override fun onAppWidgetOptionsChanged(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
    newOptions: Bundle,
  ) {
    // Re-render on resize so empty slots collapse correctly.
    val pendingResult = goAsync()
    scope.launch {
      try {
        refreshWidgets(context)
      } finally {
        pendingResult.finish()
      }
    }
  }
}
