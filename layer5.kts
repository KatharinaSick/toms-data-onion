#!/usr/bin/env kscript

import java.io.File
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import javax.crypto.spec.IvParameterSpec

@file:Include("utils.kts")

fun File.decodeLayer5(resultFileName: String) {
    val bytes = decodeAscii85()

    val keyEncryptingKey = bytes.subList(0, 32).map { it.toByte() }.toByteArray()
    val keyInitializationVector = bytes.subList(32, 40).map { it.toByte() }.toByteArray()
    val wrappedKey = bytes.subList(40, 80).map { it.toByte() }.toByteArray()
    val wrappedInitializationVector = bytes.subList(80, 96).map { it.toByte() }.toByteArray()
    val encryptedPayload = bytes.subList(96, bytes.size).map { it.toByte() }.toByteArray()

    // init the cipher to unwrap the key
    val keyCipher = Cipher.getInstance("AESWrap");
    keyCipher.init(Cipher.UNWRAP_MODE, SecretKeySpec(keyEncryptingKey, "AES"));

    // use reflection to set the IV as it is hardcoded in a private final field (WTF why?!!)
    val spiField = keyCipher::class.java.getDeclaredField("firstSpi")
    spiField.isAccessible = true
    println(spiField)

    val clazz = Class.forName("com.sun.crypto.provider.AESWrapCipher")
    val spiIvField = clazz.getDeclaredField("IV")
    spiIvField.isAccessible = true
    val modifiersField = Field::class.java.getDeclaredField("modifiers")
    modifiersField.isAccessible = true
    modifiersField.setInt(spiIvField, spiIvField.getModifiers() and Modifier.FINAL.inv())
    spiIvField.set(this, keyInitializationVector)

    // unwrap the key
    val key = keyCipher.unwrap(wrappedKey, "AES", Cipher.SECRET_KEY)

    // decrypt the payload
    val cipher = Cipher.getInstance("AES/CBC/NOPADDING")
    cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(wrappedInitializationVector))
    val result = cipher.doFinal(encryptedPayload)

    // store the result
    result
            .toText()
            .saveToFile(resultFileName)
}