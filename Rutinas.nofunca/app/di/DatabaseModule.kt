package com.example.rutinas.di

import android.content.Context
import androidx.room.Room
import com.example.rutinas.data.dao.RoutineDao
import com.example.rutinas.data.database.AppDatabase
import com.example.rutinas.data.workers.WorkManagerScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).build()
    }

    @Provides
    @Singleton
    fun provideRoutineDao(
        database: AppDatabase
    ): RoutineDao {
        return database.routineDao()
}