package com.example

import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.http.multipart.StreamingFileUpload
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest
import javax.annotation.security.PermitAll


@Controller("/api/release")
@PermitAll
@ExecuteOn(TaskExecutors.IO)
class UploadController {

    @Post(value = "{id}/upload", consumes = [MediaType.MULTIPART_FORM_DATA])
    fun testUpload(
        id: Long,
        file: StreamingFileUpload,
    ): Mono<Unit> {
        var tempFile = Paths.get(file.filename)
        if (Files.exists(tempFile)) {
            tempFile = (1..100).map { Paths.get("${file.filename}.$it") }
                .first { !Files.exists(it) }
        }
        return file.transferTo(tempFile.toFile())
            .toMono()
            .map {
                println("Upload $tempFile success=$it md5=${tempFile.hash("MD5").toHexString()} size=${Files.size(tempFile)}")
            }
    }
}

fun Path.hash(algorithm: String = "SHA-256") = Files.newInputStream(this).use {
    val digest = MessageDigest.getInstance(algorithm)
    val buffer = ByteArray(128 * 1024)
    var read = it.read(buffer)
    while (read != -1) {
        digest.update(buffer, 0, read)
        read = it.read(buffer)
    }
    digest.digest()!!
}

private const val HEX_CHARS = "0123456789abcdef"

fun ByteArray.toHexString() = String(
    CharArray(size * 2).also {
        forEachIndexed { i, a ->
            it[i * 2] = HEX_CHARS[a.toInt() shr 4 and 0x0f]
            it[i * 2 + 1] = HEX_CHARS[a.toInt() and 0x0f]
        }
    })
