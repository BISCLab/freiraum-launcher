package com.bisc.flauncher.data.backup

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * A backup locked with a password: AES-256-GCM in chunks of 64 KB, the key derived from the
 * password with PBKDF2 (HMAC-SHA256) and a random salt. Every chunk carries its own tag, and the
 * last one is marked as last, so a changed, swapped or cut-off file is noticed. Big backups never
 * need to fit into memory at once.
 *
 * File: magic, version, iterations, salt, nonce prefix; then per chunk its plain length, whether it
 * is the last one, and the sealed bytes.
 */
object BackupCrypto {
    private val MAGIC = "FRAUMBK1".toByteArray(Charsets.US_ASCII)
    private const val VERSION = 1
    private const val ITERATIONS = 150_000
    private const val SALT_SIZE = 16
    private const val PREFIX_SIZE = 8
    private const val TAG_BITS = 128
    private const val CHUNK = 64 * 1024

    class WrongPasswordException : IOException("wrong password or damaged file")

    /** Whether [header] (the file's first bytes) starts a locked backup. */
    fun isLocked(header: ByteArray): Boolean = header.size >= MAGIC.size && header.copyOf(MAGIC.size).contentEquals(MAGIC)

    const val HEADER_PEEK = 8

    private fun key(password: CharArray, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, iterations, 256)
        try {
            val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            return SecretKeySpec(bytes, "AES")
        } finally {
            spec.clearPassword()
        }
    }

    private fun nonce(prefix: ByteArray, counter: Int) = ByteArray(12).also {
        prefix.copyInto(it)
        it[8] = (counter ushr 24).toByte(); it[9] = (counter ushr 16).toByte(); it[10] = (counter ushr 8).toByte(); it[11] = counter.toByte()
    }

    /** Wraps [out]: what is written to the result lands in [out] locked with [password]. */
    fun lock(out: OutputStream, password: CharArray): OutputStream {
        val random = SecureRandom()
        val salt = ByteArray(SALT_SIZE).also(random::nextBytes)
        val prefix = ByteArray(PREFIX_SIZE).also(random::nextBytes)
        val key = key(password, salt, ITERATIONS)
        val data = DataOutputStream(out)
        data.write(MAGIC); data.writeByte(VERSION); data.writeInt(ITERATIONS); data.write(salt); data.write(prefix)
        return object : OutputStream() {
            private val buffer = ByteArray(CHUNK)
            private var filled = 0
            private var counter = 0
            private var closed = false

            private fun seal(last: Boolean) {
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, nonce(prefix, counter++)))
                cipher.updateAAD(byteArrayOf(if (last) 1 else 0))
                val sealed = cipher.doFinal(buffer, 0, filled)
                data.writeInt(filled); data.writeBoolean(last); data.write(sealed)
                filled = 0
            }

            override fun write(b: Int) {
                if (filled == CHUNK) seal(last = false)
                buffer[filled++] = b.toByte()
            }

            override fun write(b: ByteArray, off: Int, len: Int) {
                var at = off; var left = len
                while (left > 0) {
                    if (filled == CHUNK) seal(last = false)
                    val n = minOf(left, CHUNK - filled)
                    System.arraycopy(b, at, buffer, filled, n)
                    filled += n; at += n; left -= n
                }
            }

            override fun flush() = data.flush()

            override fun close() {
                if (closed) return
                closed = true
                seal(last = true)
                data.flush()
                out.close()
            }
        }
    }

    /** Wraps [input], a locked backup: reading the result gives the plain backup. Throws
     *  [WrongPasswordException] on the first chunk if [password] is wrong. */
    fun unlock(input: InputStream, password: CharArray): InputStream {
        val data = DataInputStream(input)
        val magic = ByteArray(MAGIC.size).also(data::readFully)
        if (!magic.contentEquals(MAGIC)) throw IOException("not a locked backup")
        if (data.readUnsignedByte() != VERSION) throw IOException("unknown version")
        val iterations = data.readInt()
        if (iterations !in 10_000..5_000_000) throw IOException("damaged header")
        val salt = ByteArray(SALT_SIZE).also(data::readFully)
        val prefix = ByteArray(PREFIX_SIZE).also(data::readFully)
        val key = key(password, salt, iterations)
        return object : InputStream() {
            private var plain = ByteArray(0)
            private var pos = 0
            private var counter = 0
            private var done = false

            private fun next(): Boolean {
                if (done) return false
                val length = try { data.readInt() } catch (e: EOFException) { throw IOException("backup is cut off") }
                if (length !in 0..CHUNK) throw IOException("damaged chunk")
                val last = data.readBoolean()
                val sealed = ByteArray(length + TAG_BITS / 8).also(data::readFully)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, nonce(prefix, counter++)))
                cipher.updateAAD(byteArrayOf(if (last) 1 else 0))
                plain = try { cipher.doFinal(sealed) } catch (e: javax.crypto.AEADBadTagException) { throw WrongPasswordException() }
                pos = 0
                done = last
                return true
            }

            override fun read(): Int {
                while (pos >= plain.size) if (!next()) return -1
                return plain[pos++].toInt() and 0xFF
            }

            override fun read(b: ByteArray, off: Int, len: Int): Int {
                if (len == 0) return 0
                while (pos >= plain.size) if (!next()) return -1
                val n = minOf(len, plain.size - pos)
                System.arraycopy(plain, pos, b, off, n)
                pos += n
                return n
            }

            override fun close() = input.close()
        }
    }
}
