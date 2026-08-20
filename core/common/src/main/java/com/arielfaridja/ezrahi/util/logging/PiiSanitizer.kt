package com.arielfaridja.ezrahi.util.logging

/**
 * PII sanitizer (spec §5.4): applied to every field that could carry user data
 * (message, cause, stackTrace, breadcrumbs) before it leaves the device.
 * Idempotent so it can safely run again during crash-dump flush.
 */
object PiiSanitizer {

    private val phone = Regex("""\+?\d[\d\s\-().]{8,}\d""")
    private val email = Regex("""[a-zA-Z0-9._%+-]+@[a-zA-Z0-9-]+(\.[a-zA-Z0-9-]+)+""")
    private val coords = Regex(
        """(lat|lng|latitude|longitude|lon)\s*[:=]\s*[-+]?\d{1,3}(\.\d+)?([,;]\s*[-+]?\d{1,3}(\.\d+)?)?""",
        RegexOption.IGNORE_CASE
    )
    private val googleApiKey = Regex("""AIza[0-9A-Za-z_\-]{35}""")
    private val secret = Regex("""(?i)(bearer|password|passwd|secret|authorization|apikey|api_key|token)[:=]\s*[^\s,;&"']+""")
    private val uri = Regex("""(content://|file:///|https?://)[^\s"']+""")
    private val path = Regex("""(/(storage|data)/[^\s"']+)""")

    fun sanitize(text: String): String = text
        .replace(phone, "[PHONE]")
        .replace(email, "[EMAIL]")
        .replace(coords, "[COORDS]")
        .replace(googleApiKey, "[API_KEY]")
        .replace(secret, "[SECRET]")
        .replace(uri, "[URI]")
        .replace(path, "[PATH]")
}