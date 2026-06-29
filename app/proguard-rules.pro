# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keep class com.bridge.calculator.** { *; }
