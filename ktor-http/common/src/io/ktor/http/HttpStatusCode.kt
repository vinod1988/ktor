/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.http

/**
 * Represents an HTTP status code and description.
 * @param value is a numeric code.
 * @param description is free form description of a status.
 */
@Suppress(
    "Unused",
    "KDocMissingDocumentation",
    "PublicApiImplicitType",
    "NO_EXPLICIT_RETURN_TYPE_IN_API_MODE_WARNING"
)
public data class HttpStatusCode(val value: Int, val description: String) {
    override fun toString(): String = "$value $description"

    override fun equals(other: Any?): Boolean = other is HttpStatusCode && other.value == value

    override fun hashCode(): Int = value.hashCode()

    /**
     * Returns a copy of `this` code with a description changed to [value].
     */
    public fun description(value: String): HttpStatusCode = copy(description = value)

    public companion object {
        // =============================================================================================================
        // Disclaimer
        // Adding a new status code here please remember [allStatusCodes] as well
        //

        public val Continue = HttpStatusCode(100, "Continue")
        public val SwitchingProtocols = HttpStatusCode(101, "Switching Protocols")
        public val Processing = HttpStatusCode(102, "Processing")

        public val OK = HttpStatusCode(200, "OK")
        public val Created = HttpStatusCode(201, "Created")
        public val Accepted = HttpStatusCode(202, "Accepted")
        public val NonAuthoritativeInformation = HttpStatusCode(203, "Non-Authoritative Information")
        public val NoContent = HttpStatusCode(204, "No Content")
        public val ResetContent = HttpStatusCode(205, "Reset Content")
        public val PartialContent = HttpStatusCode(206, "Partial Content")
        public val MultiStatus = HttpStatusCode(207, "Multi-Status")

        public val MultipleChoices = HttpStatusCode(300, "Multiple Choices")
        public val MovedPermanently = HttpStatusCode(301, "Moved Permanently")
        public val Found = HttpStatusCode(302, "Found")
        public val SeeOther = HttpStatusCode(303, "See Other")
        public val NotModified = HttpStatusCode(304, "Not Modified")
        public val UseProxy = HttpStatusCode(305, "Use Proxy")
        public val SwitchProxy = HttpStatusCode(306, "Switch Proxy")
        public val TemporaryRedirect = HttpStatusCode(307, "Temporary Redirect")
        public val PermanentRedirect = HttpStatusCode(308, "Permanent Redirect")

        public val BadRequest = HttpStatusCode(400, "Bad Request")
        public val Unauthorized = HttpStatusCode(401, "Unauthorized")
        public val PaymentRequired = HttpStatusCode(402, "Payment Required")
        public val Forbidden = HttpStatusCode(403, "Forbidden")
        public val NotFound = HttpStatusCode(404, "Not Found")
        public val MethodNotAllowed = HttpStatusCode(405, "Method Not Allowed")
        public val NotAcceptable = HttpStatusCode(406, "Not Acceptable")
        public val ProxyAuthenticationRequired = HttpStatusCode(407, "Proxy Authentication Required")
        public val RequestTimeout = HttpStatusCode(408, "Request Timeout")
        public val Conflict = HttpStatusCode(409, "Conflict")
        public val Gone = HttpStatusCode(410, "Gone")
        public val LengthRequired = HttpStatusCode(411, "Length Required")
        public val PreconditionFailed = HttpStatusCode(412, "Precondition Failed")
        public val PayloadTooLarge = HttpStatusCode(413, "Payload Too Large")
        public val RequestURITooLong = HttpStatusCode(414, "Request-URI Too Long")

        public val UnsupportedMediaType = HttpStatusCode(415, "Unsupported Media Type")
        public val RequestedRangeNotSatisfiable = HttpStatusCode(416, "Requested Range Not Satisfiable")
        public val ExpectationFailed = HttpStatusCode(417, "Expectation Failed")
        public val UnprocessableEntity = HttpStatusCode(422, "Unprocessable Entity")
        public val Locked = HttpStatusCode(423, "Locked")
        public val FailedDependency = HttpStatusCode(424, "Failed Dependency")
        public val UpgradeRequired = HttpStatusCode(426, "Upgrade Required")
        public val TooManyRequests = HttpStatusCode(429, "Too Many Requests")
        public val RequestHeaderFieldTooLarge = HttpStatusCode(431, "Request Header Fields Too Large")

        public val InternalServerError = HttpStatusCode(500, "Internal Server Error")
        public val NotImplemented = HttpStatusCode(501, "Not Implemented")
        public val BadGateway = HttpStatusCode(502, "Bad Gateway")
        public val ServiceUnavailable = HttpStatusCode(503, "Service Unavailable")
        public val GatewayTimeout = HttpStatusCode(504, "Gateway Timeout")
        public val VersionNotSupported = HttpStatusCode(505, "HTTP Version Not Supported")
        public val VariantAlsoNegotiates = HttpStatusCode(506, "Variant Also Negotiates")
        public val InsufficientStorage = HttpStatusCode(507, "Insufficient Storage")

        /**
         * All known status codes
         */
        public val allStatusCodes: List<HttpStatusCode> = allStatusCodes()

        private val byValue: Array<HttpStatusCode?> = Array(1000) { idx ->
            allStatusCodes.firstOrNull { it.value == idx }
        }

        /**
         * Creates an instance of [HttpStatusCode] with the given numeric value.
         */
        public fun fromValue(value: Int): HttpStatusCode {
            val knownStatus = if (value in 1 until 1000) byValue[value] else null
            return knownStatus ?: HttpStatusCode(value, "Unknown Status Code")
        }
    }
}

@Suppress("UNUSED", "KDocMissingDocumentation")
@Deprecated(
    "Use ExpectationFailed instead",
    ReplaceWith("ExpectationFailed", "io.ktor.http.HttpStatusCode.Companion.ExpectationFailed"),
    level = DeprecationLevel.ERROR
)
public inline val HttpStatusCode.Companion.ExceptionFailed: HttpStatusCode
    get() = ExpectationFailed

internal fun allStatusCodes(): List<HttpStatusCode> = listOf(
    HttpStatusCode.Continue,
    HttpStatusCode.SwitchingProtocols,
    HttpStatusCode.Processing,
    HttpStatusCode.OK,
    HttpStatusCode.Created,
    HttpStatusCode.Accepted,
    HttpStatusCode.NonAuthoritativeInformation,
    HttpStatusCode.NoContent,
    HttpStatusCode.ResetContent,
    HttpStatusCode.PartialContent,
    HttpStatusCode.MultiStatus,
    HttpStatusCode.MultipleChoices,
    HttpStatusCode.MovedPermanently,
    HttpStatusCode.Found,
    HttpStatusCode.SeeOther,
    HttpStatusCode.NotModified,
    HttpStatusCode.UseProxy,
    HttpStatusCode.SwitchProxy,
    HttpStatusCode.TemporaryRedirect,
    HttpStatusCode.PermanentRedirect,
    HttpStatusCode.BadRequest,
    HttpStatusCode.Unauthorized,
    HttpStatusCode.PaymentRequired,
    HttpStatusCode.Forbidden,
    HttpStatusCode.NotFound,
    HttpStatusCode.MethodNotAllowed,
    HttpStatusCode.NotAcceptable,
    HttpStatusCode.ProxyAuthenticationRequired,
    HttpStatusCode.RequestTimeout,
    HttpStatusCode.Conflict,
    HttpStatusCode.Gone,
    HttpStatusCode.LengthRequired,
    HttpStatusCode.PreconditionFailed,
    HttpStatusCode.PayloadTooLarge,
    HttpStatusCode.RequestURITooLong,
    HttpStatusCode.UnsupportedMediaType,
    HttpStatusCode.RequestedRangeNotSatisfiable,
    HttpStatusCode.ExpectationFailed,
    HttpStatusCode.UnprocessableEntity,
    HttpStatusCode.Locked,
    HttpStatusCode.FailedDependency,
    HttpStatusCode.UpgradeRequired,
    HttpStatusCode.TooManyRequests,
    HttpStatusCode.RequestHeaderFieldTooLarge,
    HttpStatusCode.InternalServerError,
    HttpStatusCode.NotImplemented,
    HttpStatusCode.BadGateway,
    HttpStatusCode.ServiceUnavailable,
    HttpStatusCode.GatewayTimeout,
    HttpStatusCode.VersionNotSupported,
    HttpStatusCode.VariantAlsoNegotiates,
    HttpStatusCode.InsufficientStorage
)

/**
 * Checks if a given status code is a success code according to HTTP standards.
 *
 * Codes from 200 to 299 are considered to be successful.
 */
public fun HttpStatusCode.isSuccess(): Boolean = value in (200 until 300)
