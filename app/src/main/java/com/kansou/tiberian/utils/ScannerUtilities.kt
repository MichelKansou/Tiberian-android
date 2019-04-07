package com.kansou.tiberian.utils

import android.net.Uri
import android.util.Log
import com.kansou.tiberian.model.KeyModel


class ScannerUtilities() {

    /** The tag for log messages  */
    private val LOCAL_TAG = "Scanner Utilities"

    private val OTP_SCHEME = "otpauth"
    private val TOTP = "totp" // time-based
    private val HOTP = "hotp" // counter-based
    private val SECRET_PARAM = "secret"
    private val ISSUER_PARAM = "issuer"
    private val ALGORITHM_PARAM = "algorithm"
    private val DIGITS_PARAM = "digits"
    private val COUNTER_PARAM = "counter"
    private val COUNTER_PARAM2 = "period"


    private val PIN_LENGTH = 6 // HOTP or TOTP
    private val REFLECTIVE_PIN_LENGTH = 9 // ROTP


    /**
     * Parses a secret value from a URI. The format will be:
     *
     * otpauth://totp/user@example.com?secret=FFF...
     * otpauth://hotp/user@example.com?secret=FFF...&counter=123
     *
     * @param uri The URI containing the secret key
     * prompted for confirmation before updating the otp
     * account information.
     */
     fun parseSecret(uri: Uri): KeyModel? {
        val scheme = uri.getScheme().toLowerCase()
        val path = uri.getPath()
        val authority = uri.getAuthority()
        val account: String?
        val secret: String?
        var issuer: String?
        var algorithm: String?
        val digits: Int?
        val type: OtpType
        val counter: Int?

        Log.d(LOCAL_TAG, "uri " + uri)
        Log.d(LOCAL_TAG, "scheme " + scheme)
        Log.d(LOCAL_TAG, "path " + path)
        Log.d(LOCAL_TAG, "authority " + authority)

        if (!OTP_SCHEME.equals(scheme)) {
            Log.e(LOCAL_TAG, ": Invalid or missing scheme in uri")
            return null
        }

        if (TOTP.equals(authority)) {
            type = OtpType.TOTP
            counter = 30 // only interesting for HOTP
        } else if (HOTP.equals(authority)) {
            type = OtpType.HOTP
            val counterParameter = if(uri.getQueryParameter(COUNTER_PARAM) != null) uri.getQueryParameter(COUNTER_PARAM) else uri.getQueryParameter(COUNTER_PARAM2)
            if (counterParameter != null) {
                try {
                    counter = Integer.parseInt(counterParameter)
                } catch (e: NumberFormatException) {
                    Log.e(LOCAL_TAG, ": Invalid counter in uri")
                    throw NumberFormatException()
                }

            } else {
                counter = 30
            }
        } else {
            Log.e(LOCAL_TAG, ": Invalid or missing authority in uri")
            return null
        }

        issuer = uri.getQueryParameter(ISSUER_PARAM)

        if (issuer == null || issuer!!.length == 0) {
            Log.e(LOCAL_TAG, ": Issuer key not found in URI")
            issuer = "unknown"
        }

        algorithm = uri.getQueryParameter(ALGORITHM_PARAM)

        if (algorithm == null || algorithm!!.length == 0) {
            algorithm = ""
            Log.e(LOCAL_TAG, ": Algorithm key not found in URI")
        }

        digits = if(uri.getQueryParameter(DIGITS_PARAM) != null) Integer.parseInt(uri.getQueryParameter(DIGITS_PARAM)) else 6


        account = validateAndGetUserInPath(path, issuer)
        if (account == null) {
            Log.e(LOCAL_TAG, ": Missing user id in uri")
            return null
        }

        secret = uri.getQueryParameter(SECRET_PARAM)

        if (secret == null || secret!!.length == 0) {
            Log.e(LOCAL_TAG, ": Secret key not found in URI")
            return null
        }

        Log.d(LOCAL_TAG, "type " + type)
        Log.d(LOCAL_TAG, "issuer " + issuer)
        Log.d(LOCAL_TAG, "user " + account)
        Log.d(LOCAL_TAG, "secret " + secret)
        Log.d(LOCAL_TAG, "counter " + counter)

        return KeyModel(0, account, issuer, secret, algorithm, counter.toString(), type.toString(), uri.toString())
    }

    private fun validateAndGetUserInPath(path: String?, issuer: String?): String? {
        if (path == null || !path.startsWith("/")) {
            return null
        }

        if (issuer == null ) {
            val user = path.substring(1).trim { it <= ' ' }
            return if (user.length == 0) {
                null // only white spaces.
            } else user
        }

        // path is "/user", so remove leading "/", and trailing white spaces
        if (path.startsWith("/$issuer:")) {
            return path.removePrefix("/$issuer:")
        }

        return path
    }

    /**
     * Types of secret keys.
     */
    enum class OtpType private constructor(// counter based

            val value: Int?  // value as stored in SQLite database
    ) {  // must be the same as in res/values/strings.xml:type
        TOTP(0), // time based
        HOTP(1);


        companion object {

            fun getEnum(i: Int?): OtpType? {
                for (type in OtpType.values()) {
                    if (type.value == i) {
                        return type
                    }
                }

                return null
            }
        }

    }
}