package app.aryan447.mpvium.di

import app.aryan447.mpvium.domain.anime4k.Anime4KManager
import app.aryan447.mpvium.domain.streaming.SeriesDetector
import app.aryan447.mpvium.domain.streaming.StreamingMetadataRepository
import app.aryan447.mpvium.repository.intro.IntroSkipRepository
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
    single { WyzieSearchRepository(androidContext(), get(), get(), get()) }
    single { IntroSkipRepository(get(), get(), get(), get()) }
    single { SeriesDetector(androidContext(), get()) }
    single { StreamingMetadataRepository(androidContext(), get(), get(), get()) }
}
