package com.example.testdemo.auth
import java.util.Base64
import org.junit.Assert.*
import org.junit.Test
class PasswordCipherTest {
    @Test fun `AES CBC matches independent OpenSSL UTF8 vector`() {
        val encrypted = PasswordCipher.encryptBytes("Password123!中文", "0123456789abcdef", "abcdef0123456789")
        assertEquals("ltIrycFqeoeiofw6GBXxyVfzF+hA4kH7G1ykmn5VpKA=", Base64.getEncoder().encodeToString(encrypted))
    }
    @Test(expected = IllegalArgumentException::class) fun `missing configuration fails before login`() {
        PasswordCipher.encryptBytes("password", "", "")
    }
}
