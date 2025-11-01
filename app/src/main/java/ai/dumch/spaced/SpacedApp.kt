package sensus.ai

import android.app.Application
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.bindSingleton

class SpacedApp : Application(), DIAware {

    override val di: DI by DI.lazy {
        bindSingleton<SpacedApp> { this@SpacedApp }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: SpacedApp
    }
}