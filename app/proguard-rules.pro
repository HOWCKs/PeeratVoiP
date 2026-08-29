# Add project specific ProGuard rules here.
# Keep Compose runtime metadata for smoother stack traces on release.
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}
