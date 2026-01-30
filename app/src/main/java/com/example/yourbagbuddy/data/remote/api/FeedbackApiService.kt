package com.example.yourbagbuddy.data.remote.api

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * Service to submit user feedback to a Google Apps Script web app
 * that appends a row to a Google Sheet with: Date, Message, Name, Email ID, Contact No., Rating
 *
 * Sends form-encoded fields with simple keys (no spaces) so Apps Script receives them
 * reliably via e.parameter.message, e.parameter.name, e.parameter.emailId,
 * e.parameter.contactNo, e.parameter.rating. Map these to your sheet columns in the script.
 */
interface FeedbackApiService {

    @FormUrlEncoded
    @POST
    suspend fun submitFeedback(
        @Url url: String,
        @Field("message") message: String,
        @Field("name") name: String,
        @Field("emailId") emailId: String,
        @Field("contactNo") contactNo: String,
        @Field("rating") rating: Int
    ): Unit

    /**
     * GET with query params used when following 302 redirect. The redirect URL
     * (script.googleusercontent.com) does not pass POST body into e.parameter, so we
     * send data as query params and the script's doGet(e) reads e.parameter.
     */
    @GET
    suspend fun submitFeedbackGet(
        @Url urlWithQueryParams: String
    ): Unit
}
