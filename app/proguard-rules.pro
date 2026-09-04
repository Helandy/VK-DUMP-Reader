# Type-safe navigation and every model that crosses a route boundary are (de)serialized by
# kotlinx.serialization, which looks the generated serializer up reflectively. R8 has no way to see
# that use, so the serializers and the classes carrying them are kept explicitly.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault,InnerClasses,Signature

-keepclasseswithmembers class com.etozhesandy.redpanda.**.*$$serializer { *; }
-keepclassmembers class com.etozhesandy.redpanda.** {
    *** Companion;
}
-keep,includedescriptorclasses class com.etozhesandy.redpanda.core.navigation.Routes$** { *; }
-keep class com.etozhesandy.redpanda.core.model.** { *; }

# Room entities are instantiated by generated code that R8 does see, but the FTS content table is
# referenced by name from SQL only.
-keep class com.etozhesandy.redpanda.core.storage.db.**.*Entity { *; }

# junrar reads its header classes reflectively while walking a RAR archive.
-keep class com.github.junrar.** { *; }
-dontwarn com.github.junrar.**
