package com.example.yourbagbuddy.domain.model

/**
 * Optional "Documents" sub-checklist item per trip: passport, visa, tickets, insurance
 * with reminder text (e.g. "Check visa validity") and optional link to official resources.
 */
data class TravelDocument(
    val id: String,
    val tripId: String,
    val type: TravelDocumentType,
    val displayName: String,
    val reminderText: String?,
    val resourceUrl: String?,
    val isChecked: Boolean
)

enum class TravelDocumentType(val displayName: String, val defaultReminder: String, val defaultResourceUrl: String?) {
    PASSPORT(
        displayName = "Passport",
        defaultReminder = "Check expiry (usually 6 months from return)",
        defaultResourceUrl = "https://www.usa.gov/passport"
    ),
    VISA(
        displayName = "Visa",
        defaultReminder = "Check visa validity and requirements for your destination",
        defaultResourceUrl = "https://travel.state.gov/content/travel/en/us-visas.html"
    ),
    TICKETS(
        displayName = "Tickets",
        defaultReminder = "Confirm booking and check-in times",
        defaultResourceUrl = "https://www.iata.org/en/youandiata/travelers/"
    ),
    INSURANCE(
        displayName = "Travel insurance",
        defaultReminder = "Verify coverage and emergency contact",
        defaultResourceUrl = "https://travel.state.gov/content/travel/en/international-travel/before-you-go/your-health-abroad.html"
    );

    companion object {
        fun defaultDocumentsForTrip(tripId: String): List<TravelDocument> = entries.map { type ->
            TravelDocument(
                id = "doc_${tripId}_${type.name}",
                tripId = tripId,
                type = type,
                displayName = type.displayName,
                reminderText = type.defaultReminder,
                resourceUrl = type.defaultResourceUrl,
                isChecked = false
            )
        }
    }
}
