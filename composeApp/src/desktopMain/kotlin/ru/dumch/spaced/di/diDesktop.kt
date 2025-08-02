package ru.dumch.spaced.di

import com.appstractive.dnssd.NetService
import com.appstractive.dnssd.createNetService
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import ru.dumch.spaced.sync.SyncCommon
import java.util.UUID

val diDesktopModule = DI.Module("desktop") {
    import(diCommonMainModule)
    import(diJvmModule)
    bindSingleton<NetService> {
        createNetService(
            type = SyncCommon.SERVICE_TYPE,
            name = "jvm-${UUID.randomUUID()}",
            port = SyncCommon.PORT,
            txt = mapOf(
                "key1" to "value1",
                "key2" to "value2",
            ),
        )
    }
}
