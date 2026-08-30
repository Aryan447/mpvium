package app.aryan447.mpvium.di

import com.github.k1rakishou.fsaf.FileManager
import org.koin.dsl.module

val FileManagerModule =
  module {
    single { FileManager(get()) }
  }
