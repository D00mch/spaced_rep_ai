package ru.dumch.spaced.sync

interface MdnsController {
    fun start(serviceName: String, port: Int)
    fun stop()
}