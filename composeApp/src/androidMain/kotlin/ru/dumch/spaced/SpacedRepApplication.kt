package ru.dumch.spaced

import android.app.Application
import com.appstractive.dnssd.createNetService
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.bindSingleton
import ru.dumch.spaced.di.diCommonMainModule
import ru.dumch.spaced.di.diJvmModule
import ru.dumch.spaced.sync.SyncCommon
import java.util.*

class SpacedRepApplication : Application(), DIAware {
    
    override val di: DI by DI.lazy {
        import(diCommonMainModule)
        import(diJvmModule)
        bindSingleton {
            createNetService(
                type = SyncCommon.SERVICE_TYPE,
                name = "android-${UUID.randomUUID()}",
                port = SyncCommon.PORT,
                txt = mapOf(
                    "key1" to "value1",
                    "key2" to "value2",
                ),
            )
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: SpacedRepApplication
    }
}