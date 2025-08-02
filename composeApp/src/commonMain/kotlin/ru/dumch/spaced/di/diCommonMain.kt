package ru.dumch.spaced.di

import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance
import ru.dumch.spaced.Greeting
import ru.dumch.spaced.getPlatform
import ru.dumch.spaced.sync.SyncDataClient
import ru.dumch.spaced.sync.SyncDataServer

val diCommonMainModule = DI.Module("main") {
    bindSingleton { SyncDataServer() }
    bindSingleton { SyncDataClient() }
    bindSingleton { getPlatform() }
    bindSingleton { Greeting(instance()) }
}