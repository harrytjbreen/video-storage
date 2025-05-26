package org.example

import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.*
import java.net.URI

data class S3ObjectMetaData(
    val uploadId: String,
    val key: String,
    val parts: List<PartMeta>,
    val status: String,
    val updated: String
)

data class UploadResult(
    val completedParts: List<CompletedPart>,
    val partMeta: List<PartMeta>
)

data class PartMeta(val partNumber: Int, val etag: String)

class S3Uploader(private val s3: S3Client, private val bucket: String) {

    fun initiateMultipartUpload(key: String): String {
        val response = s3.createMultipartUpload(
            CreateMultipartUploadRequest.builder()
                .bucket(bucket)
                .key(key)
                .build()
        )
        println("🔄 Initiated multipart upload: ${response.uploadId()}")
        return response.uploadId()
    }

    fun streamAndUploadParts(
        videoUrl: String,
        key: String,
        uploadId: String,
        partSize: Long,
        partMeta: MutableList<PartMeta>
    ): List<CompletedPart> {
        val completedParts = mutableListOf<CompletedPart>()
        val url = URI.create(videoUrl).toURL()

        url.openStream().use { inputStream ->
            var partNumber = 1
            val buffer = ByteArray(partSize.toInt())
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1 && !interrupted) {
                val request = UploadPartRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .uploadId(uploadId)
                    .partNumber(partNumber)
                    .contentLength(bytesRead.toLong())
                    .build()

                val response = s3.uploadPart(
                    request,
                    RequestBody.fromBytes(buffer.copyOfRange(0, bytesRead))
                )

                completedParts.add(
                    CompletedPart.builder()
                        .partNumber(partNumber)
                        .eTag(response.eTag())
                        .build()
                )

                partMeta.add(
                    PartMeta(partNumber, response.eTag())
                )

                println("🧩 Uploaded part $partNumber (${bytesRead / 1024} KB)")
                partNumber++
            }
        }

        return completedParts
    }

    fun completeMultipartUpload(
        key: String,
        uploadId: String,
        parts: List<CompletedPart>
    ) {
        s3.completeMultipartUpload(
            CompleteMultipartUploadRequest.builder()
                .bucket(bucket)
                .key(key)
                .uploadId(uploadId)
                .multipartUpload(
                    CompletedMultipartUpload.builder().parts(parts).build()
                )
                .build()
        )
    }

    fun abortMultipartUpload(key: String, uploadId: String) {
        s3.abortMultipartUpload(
            AbortMultipartUploadRequest.builder()
                .bucket(bucket)
                .key(key)
                .uploadId(uploadId)
                .build()
        )
        println("❌ Multipart upload aborted.")
    }

    fun writeMetaDataToS3(
        key: String,
        metaData: S3ObjectMetaData
    ) {
        val metaDataKey = "$key.meta.json"

        val metaDataJson = """
            {
                "uploadId": "${metaData.uploadId}",
                "key": "${metaData.key}",
                "parts": ${metaData.parts},
                "status": "${metaData.status}",
                "updated": "${metaData.updated}"
            }
        """.trimIndent()

        s3.putObject(
            PutObjectRequest.builder()
                .bucket(bucket)
                .key(metaDataKey)
                .build(),
            RequestBody.fromString(metaDataJson)
        )
        println("Metadata written to S3: $metaDataKey")
    }
}