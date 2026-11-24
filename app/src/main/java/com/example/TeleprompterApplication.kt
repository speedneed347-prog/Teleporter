package com.example

import android.app.Application
import com.example.data.local.SettingsDataStore
import com.example.data.local.TeleprompterDatabase
import com.example.data.repository.DocumentImporter
import com.example.data.repository.ScriptRepository
import com.example.data.repository.VideoRepository

class AppContainer(application: Application) {
    private val database: TeleprompterDatabase = TeleprompterDatabase.getInstance(application)
    val scriptRepository: ScriptRepository = ScriptRepository(database.scriptDao())
    val settingsDataStore: SettingsDataStore = SettingsDataStore(application)
    val documentImporter: DocumentImporter = DocumentImporter(application)
    val videoRepository: VideoRepository = VideoRepository(application)
}

class TeleprompterApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
