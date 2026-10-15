package com.example.unidash

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.content.toByteArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object S3ProfilePhotoManager {

    // Maximum photo size: strictly less than 0.5 MB (500,000 bytes).
    private const val MAX_PHOTO_SIZE = 500_000
    private const val PHOTO_FOLDER = "profile-photos"

    private fun createS3Client(): S3Client {
        return S3Client {
            region = BuildConfig.AWS_REGION
            credentialsProvider = StaticCredentialsProvider {
                accessKeyId = BuildConfig.AWS_ACCESS_KEY_ID
                secretAccessKey = BuildConfig.AWS_SECRET_ACCESS_KEY
            }
        }
    }

    private fun photoKey(uid: String): String {
        return "$PHOTO_FOLDER/$uid.jpg"
    }

    /**
     * Creates the S3 object URL for a user's profile photo.
     * The bucket must allow the relevant access for this URL to work.
     */
    fun createPhotoUrl(uid: String): String {
        val encodedKey = photoKey(uid)
            .split("/")
            .joinToString("/") { segment ->
                URLEncoder.encode(
                    segment,
                    StandardCharsets.UTF_8.name()
                ).replace("+", "%20")
            }

        return "https://${BuildConfig.AWS_S3_BUCKET}.s3.${BuildConfig.AWS_REGION}.amazonaws.com/$encodedKey"
    }

    /**
     * Uploads a profile photo to S3 and returns its object URL.
     */
    suspend fun uploadProfilePhoto(
        uid: String,
        imageBytes: ByteArray
    ): String = withContext(Dispatchers.IO) {

        require(uid.isNotBlank()) {
            "User ID is missing."
        }

        require(imageBytes.isNotEmpty()) {
            "Please select a valid photo."
        }

        require(imageBytes.size < MAX_PHOTO_SIZE) {
            "The photo must be smaller than 0.5 MB."
        }

        createS3Client().use { s3 ->
            val request = PutObjectRequest {
                bucket = BuildConfig.AWS_S3_BUCKET
                key = photoKey(uid)
                body = ByteStream.fromBytes(imageBytes)
                contentType = "image/jpeg"
            }

            s3.putObject(request)
        }

        createPhotoUrl(uid)
    }

    /**
     * Downloads a profile photo from S3.
     * Returns null if the response has no body.
     */
    suspend fun downloadProfilePhoto(
        uid: String
    ): ByteArray? = withContext(Dispatchers.IO) {

        require(uid.isNotBlank()) {
            "User ID is missing."
        }

        createS3Client().use { s3 ->
            val request = GetObjectRequest {
                bucket = BuildConfig.AWS_S3_BUCKET
                key = photoKey(uid)
            }

            s3.getObject(request) { response ->
                response.body?.toByteArray()
            }
        }
    }
}
