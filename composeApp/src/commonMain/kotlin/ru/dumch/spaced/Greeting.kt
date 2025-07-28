package ru.dumch.spaced

class Greeting(private val platform: Platform) {

    fun greet(): String {
        return "Hello, ${platform.name}!"
    }
}