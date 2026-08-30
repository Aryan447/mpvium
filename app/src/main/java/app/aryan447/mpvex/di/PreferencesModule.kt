package app.aryan447.mpvex.di

import app.aryan447.mpvex.database.MpvExDatabase
import app.aryan447.mpvex.preferences.AdvancedPreferences
import app.aryan447.mpvex.preferences.AppearancePreferences
import app.aryan447.mpvex.preferences.AudioPreferences
import app.aryan447.mpvex.preferences.BrowserPreferences
import app.aryan447.mpvex.preferences.DecoderPreferences
import app.aryan447.mpvex.preferences.FoldersPreferences
import app.aryan447.mpvex.preferences.GesturePreferences
import app.aryan447.mpvex.preferences.PlayerPreferences
import app.aryan447.mpvex.preferences.SettingsManager
import app.aryan447.mpvex.preferences.SubtitlesPreferences
import app.aryan447.mpvex.preferences.preference.AndroidPreferenceStore
import app.aryan447.mpvex.preferences.preference.PreferenceStore
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val PreferencesModule =
  module {
    single { AndroidPreferenceStore(androidContext()) }.bind(PreferenceStore::class)

    single { AppearancePreferences(get()) }
    singleOf(::PlayerPreferences)
    singleOf(::GesturePreferences)
    singleOf(::DecoderPreferences)
    singleOf(::SubtitlesPreferences)
    singleOf(::AudioPreferences)
    singleOf(::AdvancedPreferences)
    single { BrowserPreferences(get(), androidContext()) }
    singleOf(::FoldersPreferences)
    singleOf(::SettingsManager)
  }
