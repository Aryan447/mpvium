package app.aryan447.mpvium.di

import app.aryan447.mpvium.domain.anime4k.Anime4KManager
import app.aryan447.mpvium.domain.audio.AudioCollectionsRepository
import app.aryan447.mpvium.domain.audio.AudioArtworkStore
import app.aryan447.mpvium.domain.audio.AudioRepository
import app.aryan447.mpvium.domain.streaming.LibraryIndexRepository
import app.aryan447.mpvium.domain.streaming.SeriesDetector
import app.aryan447.mpvium.domain.streaming.StreamingArtworkStore
import app.aryan447.mpvium.domain.streaming.StreamingMetadataRepository
import app.aryan447.mpvium.repository.explain.ExplainRepository
import app.aryan447.mpvium.repository.intro.IntroSkipRepository
import app.aryan447.mpvium.repository.subtitle.SubtitleProfileRepository
import app.aryan447.mpvium.repository.wyzie.SubsourceProvider
import app.aryan447.mpvium.repository.wyzie.WyzieSearchRepository
import okhttp3.OkHttpClient
import org.koin.dsl.module
import org.koin.android.ext.koin.androidContext
import java.util.concurrent.TimeUnit

val domainModule = module {
    single {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    single { Anime4KManager(androidContext()) }
    single { SubsourceProvider(get(), get(), get()) }
    single { WyzieSearchRepository(androidContext(), get(), get(), get(), get()) }
    single { IntroSkipRepository(androidContext(), get(), get(), get(), get()) }
    single { ExplainRepository(get(), get()) }
    single { SeriesDetector(androidContext(), get()) }
    single { LibraryIndexRepository(androidContext(), get(), get(), get()) }
    single { StreamingArtworkStore(androidContext(), get()) }
    single { StreamingMetadataRepository(androidContext(), get(), get(), get(), get()) }
    single { SubtitleProfileRepository(androidContext(), get(), get()) }
    single { AudioArtworkStore(androidContext(), get()) }
    single { AudioRepository(androidContext(), get()) }
    single { AudioCollectionsRepository(androidContext(), get(), get(), get()) }
}
