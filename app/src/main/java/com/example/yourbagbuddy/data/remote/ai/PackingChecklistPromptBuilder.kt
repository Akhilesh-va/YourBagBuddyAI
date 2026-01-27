package com.example.yourbagbuddy.data.remote.ai

import com.example.yourbagbuddy.domain.model.SmartPackRequest

/**
 * Centralised prompt builder for the packing checklist AI.
 *
 * UI and domain layers must never know about prompt details. They only pass
 * structured trip input (SmartPackRequest) into the SmartPackRepository,
 * which then delegates here to build the AI instructions.
 */
class PackingChecklistPromptBuilder {

    /**
     * Represents the final prompt as a pair of chat messages:
     * - [system] describes the AI role and global behaviour
     * - [user] carries the concrete trip input and strict output schema
     */
    data class Prompt(
        val system: String,
        val user: String
    )

    fun build(request: SmartPackRequest): Prompt {
        val systemMessage =
            "You are a packing checklist generator for a travel checklist mobile app. " +
                "Your job is to read structured trip details, understand the destination " +
                "weather for that month, and produce a concise, category-based packing " +
                "checklist that also includes key weather-related precautions."

        // We keep the user message compact for cost control while still being explicit
        // about input structure, output schema and strict JSON-only requirements.
        val userMessage = """
            Generate a packing checklist for this trip:
            {
              "destination": "${request.destination}",
              "month": "${request.month}",
              "tripType": "${request.tripType.name}",
              "durationDays": ${request.tripDuration},
              "numberOfPeople": ${request.numberOfPeople}
            }

            STRICT RULES:
            - Return ONLY valid JSON.
            - No explanations, no extra keys, no markdown, no emojis.
            - Items must be SHORT strings only (2–8 words).
            - Do not number or bullet items.
            - If a category has no items, return an empty array.

            You MUST:
            - Infer the typical weather for the destination in the given month.
            - Include specific items and short tips that help the user stay safe and comfortable.
            
            Allowed categories and mandatory output schema:
            {
              "Clothes": [],
              "Essentials": [],
              "Medicines": [],
              "Weather & Precautions": []
            }
        """.trimIndent()

        return Prompt(
            system = systemMessage,
            user = userMessage
        )
    }
}

