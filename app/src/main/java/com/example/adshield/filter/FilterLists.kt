package com.example.adshield.filter

object FilterLists {
    // Safety Allowlist - Critical domains that should NEVER be blocked
    val allowlist = setOf(
        "googleapis.com", "gstatic.com", "googleusercontent.com",
        "fbcdn.net",
        "twitch.tv", "ttvnw.net",
        "googlevideo.com", "ytimg.com", // YouTube CDNs
        "hdrezka.ac", "hdrezka.ag", "hdrezka.me", "hdrezka.co", "hdrezka.re", "hdrezka-home.tv",
        "rezka.ag", "rezka.me", "static.hdrezka.ac", "hls.hdrezka.ac",
        "voidboost.net", "voidboost.cc",
        "cloudfront.net", "cdn.net", "cloudflare.com", "icloud.com",
        "gvt1.com", "gvt2.com", "gvt3.com",
        "ss.lv", "m.ss.lv", "www.ss.lv", "inbox.lv"
    )

    // Domains used by browsers for DNS-over-HTTPS. Blocking these forces fallback to our VPN DNS.
    val dohBypassList = setOf(
        "cloudflare-dns.com", "dns.google", "dns.nextdns.io", "doh.opendns.com",
        "dns.quad9.net", "doh.cleanbrowsing.org"
    )

    // Fallback block rules to ensure basic protection even if download fails
    val fallbackBlocklist = setOf(
        "doubleclick.net", "ad.google.com", "mc.yandex.ru", "an.yandex.ru",
        "videoroll.net", "marketgid.com", "mgid.com", "googleadservices.com"
    )
}
