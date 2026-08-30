package app.aryan447.mpvium.di

import app.aryan447.mpvium.database.MpviumDatabase
import app.aryan447.mpvium.preferences.AdvancedPreferences
import app.aryan447.mpvium.preferences.AppearancePreferences
import app.aryan447.mpvium.preferences.AudioPreferences
import app.aryan447.mpvium.preferences.BrowserPreferences
import app.aryan447.mpvium.preferences.DecoderPreferences
import app.aryan447.mpvium.preferences.FoldersPreferences
import app.aryan447.mpvium.preferences.GesturePreferences
import app.aryan447.mpvium.preferences.PlayerPreferences
import app.aryan447.mpvium.preferences.SettingsManager
import app.aryan447.mpvium.preferences.SubtitlesPreferences
import app.aryan447.mpvium.preferences.preference.AndroidPreferenceStore
import app.aryan447.mpvium.preferences.preference.PreferenceStore
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
