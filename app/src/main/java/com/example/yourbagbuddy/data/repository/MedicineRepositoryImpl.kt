package com.example.yourbagbuddy.data.repository

import com.example.yourbagbuddy.domain.model.MedicineCategory
import com.example.yourbagbuddy.domain.model.TravelMedicine
import com.example.yourbagbuddy.domain.repository.MedicineRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class MedicineRepositoryImpl @Inject constructor() : MedicineRepository {
    
    private val medicines = listOf(
        // Fever/Pain
        TravelMedicine("1", "Paracetamol", MedicineCategory.FEVER_PAIN, "For fever and pain relief"),
        TravelMedicine("2", "Ibuprofen", MedicineCategory.FEVER_PAIN, "For pain and inflammation"),
        
        // Stomach Issues
        TravelMedicine("3", "Antacid", MedicineCategory.STOMACH_ISSUES, "For indigestion and heartburn"),
        TravelMedicine("4", "Anti-diarrheal", MedicineCategory.STOMACH_ISSUES, "For diarrhea relief"),
        
        // Motion Sickness
        TravelMedicine("5", "Dramamine", MedicineCategory.MOTION_SICKNESS, "For motion sickness prevention"),
        TravelMedicine("6", "Ginger Supplements", MedicineCategory.MOTION_SICKNESS, "Natural motion sickness relief"),
        
        // First Aid
        TravelMedicine("7", "Bandages", MedicineCategory.FIRST_AID, "For cuts and wounds"),
        TravelMedicine("8", "Antiseptic", MedicineCategory.FIRST_AID, "For wound cleaning"),
        TravelMedicine("9", "Gauze", MedicineCategory.FIRST_AID, "For wound dressing"),
        
        // Dehydration
        TravelMedicine("10", "Electrolyte Tablets", MedicineCategory.DEHYDRATION, "For rehydration"),
        TravelMedicine("11", "Oral Rehydration Salts", MedicineCategory.DEHYDRATION, "For severe dehydration")
    )
    
    override fun getAllMedicines(): Flow<List<TravelMedicine>> {
        return flowOf(medicines)
    }
    
    override fun getMedicinesByCategory(category: MedicineCategory): Flow<List<TravelMedicine>> {
        return flowOf(medicines.filter { it.category == category })
    }
}
