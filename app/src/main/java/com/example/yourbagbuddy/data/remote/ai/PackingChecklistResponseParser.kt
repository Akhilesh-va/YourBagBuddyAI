package com.example.yourbagbuddy.data.remote.ai

import com.example.yourbagbuddy.domain.model.ChecklistItem
import com.example.yourbagbuddy.domain.model.ItemCategory
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import java.util.UUID

/**
 * Validates and parses the raw JSON content returned by the AI into
 * safe domain [ChecklistItem] instances.
 *
 * This layer must be paranoid:
 * - Never trust the AI to follow the schema perfectly.
 * - Drop unknown fields and invalid data.
 * - Enforce short string lengths.
 * - Never throw to the caller – return [Result.failure] instead.
 */
class PackingChecklistResponseParser {

    private val allowedCategories = linkedSetOf(
        "Clothes",
        "Essentials",
        "Medicines",
        "Weather & Precautions"
    )

    // Reasonable upper bound to keep items concise and cost-controlled.
    private val maxItemLength = 80

    fun parseToChecklistItems(
        rawJson: String
    ): Result<List<ChecklistItem>> {
        val jsonObject = try {
            JsonParser.parseString(rawJson).asJsonObject
        } catch (e: IllegalStateException) {
            return Result.failure(InvalidAiResponseException("AI response was not a JSON object.", e))
        } catch (e: JsonSyntaxException) {
            return Result.failure(InvalidAiResponseException("AI returned invalid JSON.", e))
        } catch (e: Exception) {
            return Result.failure(InvalidAiResponseException("Failed to parse AI response.", e))
        }

        val items = mutableListOf<ChecklistItem>()

        for (categoryName in allowedCategories) {
            val categoryElement: JsonElement? = jsonObject.get(categoryName)
            if (categoryElement == null || !categoryElement.isJsonArray) {
                // Missing or invalid category – treated as empty as per contract.
                continue
            }

            val itemCategory = mapToDomainCategory(categoryName)

            categoryElement.asJsonArray.forEach { element ->
                if (!element.isJsonPrimitive || !element.asJsonPrimitive.isString) return@forEach

                val rawItem = element.asString.trim()
                if (rawItem.isEmpty()) return@forEach

                val normalizedItem = rawItem
                    .replace("\n", " ")
                    .replace("\r", " ")
                    .take(maxItemLength)
                    .trim()

                if (normalizedItem.isEmpty()) return@forEach

                items.add(
                    ChecklistItem(
                        id = UUID.randomUUID().toString(),
                        tripId = "",
                        name = normalizedItem,
                        category = itemCategory,
                        isPacked = false
                    )
                )
            }
        }

        if (items.isEmpty()) {
            return Result.failure(
                InvalidAiResponseException(
                    "AI response did not contain any valid checklist items in the allowed categories."
                )
            )
        }

        return Result.success(items)
    }

    private fun mapToDomainCategory(aiCategory: String): ItemCategory {
        return when (aiCategory) {
            "Clothes" -> ItemCategory.CLOTHES
            "Essentials" -> ItemCategory.ESSENTIALS
            "Medicines" -> ItemCategory.OTHER
            "Weather & Precautions" -> ItemCategory.OTHER
            else -> ItemCategory.OTHER
        }
    }
}

/**
 * Marker exception type used so callers can distinguish AI validation/parsing
 * issues from network or rate-limit errors.
 */
class InvalidAiResponseException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

