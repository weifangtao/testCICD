package com.example.testdemo.auth

import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/** 只实现旧 WMS 登录协议，固定 IV 是服务端兼容要求。 */
object PasswordCipher {
    fun encryptBytes(password: String, key: String, iv: String): ByteArray {
        require(key.toByteArray(Charsets.UTF_8).size == 16 && iv.toByteArray(Charsets.UTF_8).size == 16) {
            "请配置 wms-auth.properties 中的 WMS_AES_KEY 和 WMS_AES_IV"
        }
        return Cipher.getInstance("AES/CBC/PKCS5Padding").run {
            init(Cipher.ENCRYPT_MODE, SecretKeySpec(key.toByteArray(Charsets.UTF_8), "AES"),
                IvParameterSpec(iv.toByteArray(Charsets.UTF_8)))
            doFinal(password.toByteArray(Charsets.UTF_8))
        }
    }
}
