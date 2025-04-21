package com.example.rutinas.di

import com.example.rutinas.data.dao.RoutineDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object DaoModule {
    @Provides
    fun provideRoutineDao(appDatabase: com.example.rutinas.data.database.AppDatabase): RoutineDao {
        return appDatabase.routineDao()
    }
}