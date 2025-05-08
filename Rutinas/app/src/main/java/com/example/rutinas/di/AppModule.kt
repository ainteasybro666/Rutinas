package com.example.rutinas.di

import android.content.Context
import androidx.room.Room
import com.example.rutinas.data.local.AppDatabase
import com.example.rutinas.data.repository.RoutineRepository
import com.example.rutinas.data.repository.RoutineRepositoryImpl
import com.example.rutinas.data.local.dao.RoutineDao // Importa RoutineDao
import com.example.rutinas.data.local.dao.ActionDao // Importa ActionDao
import com.example.rutinas.data.local.dao.TriggerDao // Importa TriggerDao

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
            .fallbackToDestructiveMigration() // Consider removing this in production if you need proper migrations
            .build()
    }

    @Provides
    @Singleton
    fun provideRoutineRepository(
        db: AppDatabase // Inyecta la base de datos
        // Hilt puede inyectar los DAOs directamente desde la base de datos si los Provees aquí también:
        // routineDao: RoutineDao, // Opción 1: Inyectar DAOs directamente (si Hilt los puede resolver)
        // actionDao: ActionDao,
        // triggerDao: TriggerDao
    ): RoutineRepository {
        // Opción 2: Obtener los DAOs desde la instancia de la base de datos
        return RoutineRepositoryImpl(
            routineDao = db.routineDao(), // Obtiene RoutineDao desde la base de datos
            actionDao = db.actionDao(),   // Obtiene ActionDao desde la base de datos
            triggerDao = db.triggerDao() // Obtiene TriggerDao desde la base de datos
        )
    }

    // Opcional: Proveer DAOs individualmente si prefieres inyectarlos directamente en el repositorio
    // Esto es útil si tienes otros componentes que solo necesitan un DAO específico.
    @Provides
    fun provideRoutineDao(db: AppDatabase): RoutineDao {
        return db.routineDao()
    }

    @Provides
    fun provideActionDao(db: AppDatabase): ActionDao {
        return db.actionDao()
    }

    @Provides
    fun provideTriggerDao(db: AppDatabase): TriggerDao {
        return db.triggerDao()
    }
}
