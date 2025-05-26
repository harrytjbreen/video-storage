package org.example

import software.amazon.awssdk.services.s3.S3Client

@Volatile
var interrupted = false

val partMeta = mutableListOf<PartMeta>()

fun main() {
    val videoUrl = System.getenv("VIDEO_URL") ?: error("VIDEO_URL not set")
    val bucket = System.getenv("S3_BUCKET") ?: error("S3_BUCKET not set")
    val key = System.getenv("S3_KEY") ?: error("S3_KEY not set")
    val partSize = 5 * 1024 * 1024L

    val s3 = S3Client.create()
    val uploader = S3Uploader(s3, bucket)

    val uploadId = uploader.initiateMultipartUpload(key)

    Runtime.getRuntime().addShutdownHook(Thread {
        uploader.abortMultipartUpload(key, uploadId)

        uploader.writeMetaDataToS3(
            key,
            S3ObjectMetaData(
                uploadId = uploadId,
                key = key,
                parts = partMeta,
                status = "incomplete",
                updated = System.currentTimeMillis().toString()
            )
        )

        interrupted = true
    })

    try {
        val parts = uploader.streamAndUploadParts(videoUrl, key, uploadId, partSize, partMeta)
        uploader.completeMultipartUpload(key, uploadId, parts)
    } catch (e: Exception) {
        uploader.abortMultipartUpload(key, uploadId)
        throw e
    }
}