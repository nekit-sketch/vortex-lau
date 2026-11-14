package com.vortex.launcher.di
import android.content.Context
import androidx.room.Room
import com.vortex.launcher.data.AppDatabase
import com.vortex.launcher.data.ProfileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module @InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton fun db(@ApplicationContext c: Context): AppDatabase =
        Room.databaseBuilder(c, AppDatabase::class.java, "vortex.db").build()
    @Provides fun dao(db: AppDatabase): ProfileDao = db.profiles()
}
