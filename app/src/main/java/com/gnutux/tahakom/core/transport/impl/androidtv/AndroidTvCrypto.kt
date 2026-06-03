package com.gnutux.tahakom.core.transport.impl.androidtv

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPublicKey
import javax.net.ssl.KeyManager
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import javax.security.auth.x500.X500Principal

/**
 * تشفير اتصال Android TV Remote v2: شهادة عميل ذاتية التوقيع (في AndroidKeyStore)
 * لمصادقة TLS المتبادلة، ومصنع سوكِت يثق بشهادة التلفاز ذاتية التوقيع، وحساب السرّ
 * المشترك أثناء الإقران.
 *
 * **تجريبي:** حساب السرّ (لا سيّما معالجة بايتات المُعامِل/الأُسّ ورمز الإقران) يحتاج
 * تحقّقاً على جهاز فعلي.
 */
object AndroidTvCrypto {

    private const val ALIAS = "gt_tahakom_androidtv_client"
    private const val KS = "AndroidKeyStore"

    /** يُنشئ زوج مفاتيح RSA + شهادة ذاتية التوقيع في AndroidKeyStore إن لم يوجد. */
    private fun ensureKey() {
        val ks = KeyStore.getInstance(KS).apply { load(null) }
        if (ks.containsAlias(ALIAS)) return
        val gen = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, KS)
        gen.initialize(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY)
                .setKeySize(2048)
                .setCertificateSubject(X500Principal("CN=GT-TAHAKOM"))
                .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_NONE)
                .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                .build(),
        )
        gen.generateKeyPair()
    }

    fun clientCertificate(): X509Certificate {
        ensureKey()
        val ks = KeyStore.getInstance(KS).apply { load(null) }
        return ks.getCertificate(ALIAS) as X509Certificate
    }

    private fun keyManagers(): Array<KeyManager> {
        ensureKey()
        val ks = KeyStore.getInstance(KS).apply { load(null) }
        val kmf = KeyManagerFactory.getInstance("X509")
        kmf.init(ks, null)
        return kmf.keyManagers
    }

    /** مدير ثقة متساهل: التلفاز يقدّم شهادة ذاتية التوقيع (لا CA). محلي فقط. */
    private val trustAll = arrayOf<TrustManager>(object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    })

    fun socketFactory(): SSLSocketFactory {
        val ctx = SSLContext.getInstance("TLS")
        ctx.init(keyManagers(), trustAll, java.security.SecureRandom())
        return ctx.socketFactory
    }

    /** التمثيل غير المُوقَّع (big-endian، بلا بايت الإشارة 0x00 البادئ). */
    private fun unsigned(v: BigInteger): ByteArray {
        val b = v.toByteArray()
        return if (b.size > 1 && b[0].toInt() == 0) b.copyOfRange(1, b.size) else b
    }

    /**
     * يحسب سرّ الإقران: SHA-256 على (مُعامِل وأُسّ شهادة العميل + مُعامِل وأُسّ شهادة
     * الخادم + بايتات الرمز عدا الأول). يُرجع التجزئة (32 بايت) إن طابق بايت التحقّق.
     */
    fun pairingSecret(client: X509Certificate, server: X509Certificate, code: String): ByteArray? {
        val codeBytes = code.chunked(2).mapNotNull { it.toIntOrNull(16)?.toByte() }.toByteArray()
        if (codeBytes.size < 2) return null
        val cPub = client.publicKey as RSAPublicKey
        val sPub = server.publicKey as RSAPublicKey
        val md = MessageDigest.getInstance("SHA-256")
        md.update(unsigned(cPub.modulus))
        md.update(unsigned(cPub.publicExponent))
        md.update(unsigned(sPub.modulus))
        md.update(unsigned(sPub.publicExponent))
        md.update(codeBytes, 1, codeBytes.size - 1)
        val hash = md.digest()
        // بايت التحقّق: أول بايت من الرمز يجب أن يطابق أول بايت من التجزئة.
        return if (hash[0] == codeBytes[0]) hash else null
    }
}
