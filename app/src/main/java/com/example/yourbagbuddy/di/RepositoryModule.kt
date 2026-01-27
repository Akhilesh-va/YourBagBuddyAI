package com.example.yourbagbuddy.di

import com.example.yourbagbuddy.data.repository.AuthRepositoryImpl
import com.example.yourbagbuddy.data.repository.ChecklistRepositoryImpl
import com.example.yourbagbuddy.data.repository.MedicineRepositoryImpl
import com.example.yourbagbuddy.data.repository.SmartPackRepositoryImpl
import com.example.yourbagbuddy.data.repository.TripRepositoryImpl
import com.example.yourbagbuddy.domain.repository.AuthRepository
import com.example.yourbagbuddy.domain.repository.ChecklistRepository
import com.example.yourbagbuddy.domain.repository.MedicineRepository
import com.example.yourbagbuddy.domain.repository.SmartPackRepository
import com.example.yourbagbuddy.domain.repository.TripRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindTripRepository(
        tripRepositoryImpl: TripRepositoryImpl
    ): TripRepository
    
    @Binds
    @Singleton
    abstract fun bindChecklistRepository(
        checklistRepositoryImpl: ChecklistRepositoryImpl
    ): ChecklistRepository
    
    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository
    
    @Binds
    @Singleton
    abstract fun bindSmartPackRepository(
        smartPackRepositoryImpl: SmartPackRepositoryImpl
    ): SmartPackRepository
    
    @Binds
    @Singleton
    abstract fun bindMedicineRepository(
        medicineRepositoryImpl: MedicineRepositoryImpl
    ): MedicineRepository
}
