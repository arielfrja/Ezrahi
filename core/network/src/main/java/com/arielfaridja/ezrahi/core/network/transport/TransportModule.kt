package com.arielfaridja.ezrahi.core.network.transport

import com.arielfaridja.ezrahi.util.logging.ErrorType
import com.arielfaridja.ezrahi.util.logging.ExceptionLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TransportModule {

    @Provides
    @Singleton
    fun provideAppCoroutineScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Singleton
    fun provideTransportErrorLogger(logger: ExceptionLogger): TransportErrorLogger =
        TransportErrorLogger { error, eventId, screen ->
            logger.log(error, ErrorType.NETWORK, eventId, screen)
        }

    @Provides
    @IntoSet
    fun provideFirebaseTransportAdapter(adapter: FirebaseTransportAdapter): TacticalTransportAdapter = adapter

    @Provides
    @IntoSet
    fun provideMeshTransportAdapter(adapter: MeshTransportAdapter): TacticalTransportAdapter = adapter
}