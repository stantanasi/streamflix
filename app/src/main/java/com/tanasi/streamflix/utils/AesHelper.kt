package com.tanasi.streamflix.utils

import android.util.Base64
import com.google.gson.Gson
import java.security.DigestException
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

// https://github.com/recloudstream/cloudstream/blob/master/library/src/commonMain/kotlin/com/lagradost/cloudstream3/extractors/helper/AesHelper.kt
object AesHelper {

    private const val HASH = "AES/CBC/PKCS5PADDING"
    private const val KDF = "MD5"

    fun cryptoAESHandler(
        data: String,
        pass: ByteArray,
        encrypt: Boolean = true,
        padding: String = HASH,
    ): String? {
        val parse = Gson().fromJson(data, AesData::class.java)

        val (key, iv) = generateKeyAndIv(
            pass,
            parse.s.hexToByteArray(),
            ivLength = parse.iv.length / 2,
            saltLength = parse.s.length / 2
        ) ?: return null
        val cipher = Cipher.getInstance(padding)
        return if (!encrypt) {
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
            String(cipher.doFinal(Base64.decode(parse.ct, Base64.DEFAULT)))
        } else {
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
            Base64.encode(cipher.doFinal(parse.ct.toByteArray()), Base64.DEFAULT).toString(Charsets.UTF_8)
        }
    }

    // https://stackoverflow.com/a/41434590/8166854
    fun generateKeyAndIv(
        password: ByteArray,
        salt: ByteArray,
        hashAlgorithm: String = KDF,
        keyLength: Int = 32,
        ivLength: Int,
        saltLength: Int,
        iterations: Int = 1
    ): Pair<ByteArray,ByteArray>? {
        val md = MessageDigest.getInstance(hashAlgorithm)
        val digestLength = md.digestLength
        val targetKeySize = keyLength + ivLength
        val requiredLength = (targetKeySize + digestLength - 1) / digestLength * digestLength
        val generatedData = ByteArray(requiredLength)
        var generatedLength = 0

        try {
            md.reset()

            while (generatedLength < targetKeySize) {
                if (generatedLength > 0)
                    md.update(
                        generatedData,
                        generatedLength - digestLength,
                        digestLength
                    )

                md.update(password)
                md.update(salt, 0, saltLength)
                md.digest(generatedData, generatedLength, digestLength)

                for (i in 1 until iterations) {
                    md.update(generatedData, generatedLength, digestLength)
                    md.digest(generatedData, generatedLength, digestLength)
                }

                generatedLength += digestLength
            }
            return generatedData.copyOfRange(0, keyLength) to generatedData.copyOfRange(keyLength, targetKeySize)
        } catch (e: DigestException) {
            return null
        }
    }

    fun String.hexToByteArray(): ByteArray {
        check(length % 2 == 0) { "Must have an even length" }
        return chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
    }

    private data class AesData(
        val ct: String,
        val iv: String,
        val s: String
    )
}