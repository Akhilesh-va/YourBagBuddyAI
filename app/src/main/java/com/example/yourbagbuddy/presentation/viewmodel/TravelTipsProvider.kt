package com.example.yourbagbuddy.presentation.viewmodel

import java.util.Calendar

/**
 * Central place to store and fetch travel tips.
 *
 * You can freely add more tips to [TRAVEL_TIPS]; the app will automatically
 * rotate through them based on the current day of year.
 */
object TravelTipsProvider {

    private val TRAVEL_TIPS = listOf(
        "Pack light, pack smart! Roll your clothes to save space.",
        "Keep a digital copy of your passport and important documents.",
        "Wear your bulkiest items on the plane to save luggage space.",
        "Always carry a reusable water bottle to stay hydrated on the go.",
        "Use packing cubes to keep your suitcase organized.",
        "Keep a small first-aid kit with essentials in your bag.",
        "Download offline maps before you leave your accommodation.",
        "Keep your chargers and adapters in one dedicated pouch.",
        "Put a luggage tag inside and outside your bag.",
        "Take a photo of your luggage before checking it in."
    )

    fun getTipForToday(calendar: Calendar = Calendar.getInstance()): String {
        if (TRAVEL_TIPS.isEmpty()) {
            return "Pack light, pack smart! Roll your clothes to save space."
        }

        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        // Deterministically pick a tip based on the day so
        // it stays the same all day but changes when the date changes.
        val index = dayOfYear % TRAVEL_TIPS.size
        return TRAVEL_TIPS[index]
    }
}

