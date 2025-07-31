package ru.dumch.spaced.di

import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance
import ru.dumch.spaced.Greeting
import ru.dumch.spaced.getPlatform

val diCommonMainModule = DI.Module("main") {
    bindSingleton { getPlatform() }
    bindSingleton { Greeting(instance()) }
}