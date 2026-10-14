# The library's own classes need no keep rules: it uses no reflection.
# AdBannerView in XML layouts is kept by the rules Android generates for layouts.

# The Meta Audience Network SDK refers to an annotation that is not shipped with it.
# Without this line, R8 stops release builds with "Missing class ...Nullsafe".
-dontwarn com.facebook.infer.annotation.Nullsafe
