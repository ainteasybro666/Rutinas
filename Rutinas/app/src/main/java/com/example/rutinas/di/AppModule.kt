package com.example.rutinas.di

import android.content.Context
import androidx.room.Room
import com.example.rutinas.data.local.AppDatabase
import com.example.rutinas.data.repository.RoutineRepository
import com.example.rutinas.data.repository.RoutineRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "rutinas.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideRoutineRepository(db: AppDatabase): RoutineRepository {
        return RoutineRepositoryImpl(db)
    }
}