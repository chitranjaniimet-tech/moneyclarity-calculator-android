-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.moneyclarity.calc.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.moneyclarity.calc.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
