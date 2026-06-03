package com.gnutux.tahakom.di

import android.content.Context
import com.gnutux.tahakom.core.discovery.DeviceDiscovery
import com.gnutux.tahakom.core.discovery.DiscoveryManager
import com.gnutux.tahakom.core.discovery.MdnsDiscovery
import com.gnutux.tahakom.core.discovery.MulticastLockHolder
import com.gnutux.tahakom.core.discovery.SsdpDiscovery
import com.gnutux.tahakom.core.irdb.IrDatabase
import com.gnutux.tahakom.core.irdb.online.OnlineIrRepository
import com.gnutux.tahakom.core.store.SavedDevicesRepository
import com.gnutux.tahakom.core.transport.Transport
import com.gnutux.tahakom.core.transport.TransportRegistry
import com.gnutux.tahakom.core.transport.impl.AndroidTvTransport
import com.gnutux.tahakom.core.transport.impl.BroadlinkTransport
import com.gnutux.tahakom.core.transport.impl.IrTransport
import com.gnutux.tahakom.core.transport.impl.RokuTransport
import com.gnutux.tahakom.core.transport.impl.SamsungTizenTransport
import com.gnutux.tahakom.core.transport.impl.WebosTransport
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
            RokuTransport(),
            WebosTransport(context),
            SamsungTizenTransport(context),
            AndroidTvTransport(), // تجريبي — تحت التطوير (Remote v2، يحتاج إقراناً)
            BroadlinkTransport(), // تجريبي — تحت التطوير (جسر WiFi-IR)
            // التالي: SonyBravia
        )
        return TransportRegistry(transports)
    }

    @Provides
    @Singleton
    fun provideDiscoveryManager(
        @ApplicationContext context: Context,
    ): DiscoveryManager {
        val discoveries: List<DeviceDiscovery> = listOf(
            MdnsDiscovery(context),
            SsdpDiscovery(),
        )
        return DiscoveryManager(discoveries, MulticastLockHolder(context))
    }

    @Provides
    @Singleton
    fun provideIrDatabase(
        @ApplicationContext context: Context,
    ): IrDatabase = IrDatabase(context)

    @Provides
    @Singleton
    fun provideSavedDevicesRepository(
        @ApplicationContext context: Context,
    ): SavedDevicesRepository = SavedDevicesRepository(context)

    @Provides
    @Singleton
    fun provideOnlineIrRepository(
        @ApplicationContext context: Context,
        db: IrDatabase,
    ): OnlineIrRepository = OnlineIrRepository(context, db)
}
