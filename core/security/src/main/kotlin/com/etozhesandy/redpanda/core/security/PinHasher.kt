package com.etozhesandy.redpanda.core.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Derives a salted PBKDF2 hash of the login PIN. The PIN itself is never stored, so a copy of the
 * DataStore file does not reveal it.
 */
@Singleton
class PinHasher @Inject constructor() {

    fun newSalt(): ByteArray = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }

    fun hash(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_BITS)
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    /** Constant-time comparison, so a wrong PIN cannot be narrowed down by timing. */
    fun verify(pin: String, salt: ByteArray, expected: ByteArray): Boolean =
        MessageDigest.isEqual(hash(pin, salt), expected)

    private companion object {
        const val ALGORITHM = "PBKDF2WithHmacSHA256"
        const val SALT_BYTES = 16
        const val ITERATIONS = 120_000
        const val KEY_BITS = 256
    }
}
