package com.gnutux.tahakom.di

import android.content.Context
import com.gnutux.tahakom.core.transport.Transport
import com.gnutux.tahakom.core.transport.TransportRegistry
import com.gnutux.tahakom.core.transport.impl.IrTransport
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * وحدة Hilt الرئيسية — تجمّع وسائل النقل وتبني [TransportRegistry].
 *
 * لإضافة وسيلة نقل جديدة (Android TV, Roku, Samsung...) أضِف تطبيقها إلى
 * القائمة في [provideTransportRegistry] فقط — لا تتغيّر بقية الشيفرة.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideTransportRegistry(
        @ApplicationContext context: Context,
    ): TransportRegistry {
        val transports: List<Transport> = listOf(
            IrTransport(context),
            // م2: AndroidTvTransport(context), RokuTransport(...)
            // م3: SamsungTizenTransport(...), WebosTransport(...), SonyBraviaTransport(...)
            // م4: BroadlinkTransport(...)
        )
        return TransportRegistry(transports)
    }
}
