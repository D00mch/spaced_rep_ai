package ru.dumch.spaced

import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance

val mainDiModule = DI.Module("myModule") {
    bindSingleton { getPlatform() }
    bindSingleton { Greeting(instance()) }
}
