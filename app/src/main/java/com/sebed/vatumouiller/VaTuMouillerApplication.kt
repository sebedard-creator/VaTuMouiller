package com.sebed.vatumouiller

import android.app.Application
import com.sebed.vatumouiller.di.DependencyContainer

class VaTuMouillerApplication : Application() {

    lateinit var container: DependencyContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DependencyContainer(this)
    }
}
