
package com.example.unidash

import aws.sdk.kotlin.runtime.auth.credentials.Credentials
import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.smithy.kotlin.runtime.content.ByteStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object S3ProfilePhotoManager {

    private const val MAX_PHOTO_SIZE = 500_000L
    private const val PHOTO_FOLDER = "profile-photos"

    private fun createS3Client(): S3Client {
        return S3Client {
            region = BuildConfig.AWS_REGION

            credentialsProvider = StaticCredentialsProvider(
                Credentials(
                    accessKeyId = BuildConfig.AWS_ACCESS_KEY_ID,
                    secretAccessKey = BuildConfig.AWS_SECRET_ACCESS_KEY
                )
            )
        }
    }

    fun createPhotoUrl(uid: String): String {
        val key = "$PHOTO_FOLDER/$uid.jpg"
        val encodedKey = key.split("/")
            .joinToString("/") {
                URLEncoder.encode(it, StandardCharsets.UTF_8.name())
                    .replace("+", "%20")
            }

        return "https://${BuildConfig.AWS_S3_BUCKET}.s3.${BuildConfig.AWS_REGION}.amazonaws.com/$encodedKey"
    }

    suspend fun uploadProfilePhoto(
        uid: String,
        imageBytes: ByteArray
    ): String = withContext(Dispatchers.IO) {

        require(imageBytes.isNotEmpty()) {
            "Please select a valid photo."
        }

        require(imageBytes.size.toLong() < MAX_PHOTO_SIZE) {
            "The photo must be smaller than 0.5 MB."
        }

        val key = "$PHOTO_FOLDER/$uid.jpg"

        createS3Client().use { s3 ->
            val request = PutObjectRequest {
                bucket = BuildConfig.AWS_S3_BUCKET
                this.key = key
                body = ByteStream.fromBytes(imageBytes)
                contentType = "image/jpeg"
            }

            s3.putObject(request)
        }

        createPhotoUrl(uid)
    }

    suspend fun downloadProfilePhoto(
        uid: String
    ): ByteArray? = withContext(Dispatchers.IO) {

        val key = "$PHOTO_FOLDER/$uid.jpg"

        createS3Client().use { s3 ->
            val request = GetObjectRequest {
                bucket = BuildConfig.AWS_S3_BUCKET
                this.key = key
            }

            s3.getObject(request) { response ->
                response.body?.toByteArray()
            }
        }
    }
}
