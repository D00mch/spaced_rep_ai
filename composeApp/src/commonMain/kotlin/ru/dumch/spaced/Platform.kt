package ru.dumch.spaced

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform