package com.tanasi.streamflix.utils

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoAES {
    fun decrypt(encryptedData: String, key: String): String {
        return try {
            val decodedData = Base64.decode(encryptedData, Base64.DEFAULT)

            // Extraemos el IV de los primeros 16 bytes de los datos decodificados
            val iv = decodedData.copyOfRange(0, 16)

            // El resto son los datos cifrados
            val ciphertext = decodedData.copyOfRange(16, decodedData.size)

            val secretKeySpec = SecretKeySpec(key.toByteArray(), "AES")
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, IvParameterSpec(iv))

            val decryptedData = cipher.doFinal(ciphertext)

            String(decryptedData)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}