package com.example.yourbagbuddy.data.remote.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

/**
 * Service to submit user feedback to a Google Apps Script web app
 * that appends a row to a Google Sheet with: Date, Message, Name, Email ID, Contact No., Rating
 */
interface FeedbackApiService {

    @POST
    suspend fun submitFeedback(@Url url: String, @Body body: FeedbackRequestBody): Unit
}

/**
 * Matches the keys expected by the sheet script: data.Message, data.Name, data["Email ID"], data["Contact No."], data.Rating
 */
data class FeedbackRequestBody(
    val Message: String = "",
    val Name: String = "",
    @SerializedName("Email ID") val emailId: String = "",
    @SerializedName("Contact No.") val contactNo: String = "",
    val Rating: Int = 0
)
