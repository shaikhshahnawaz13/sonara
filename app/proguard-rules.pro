# Used by the "perf" and "release" build types.
# Names are kept readable so crash reports stay useful; R8 still removes dead code and optimises.
-dontobfuscate

# Optional annotation-only dependencies referenced by Guava and friends.
-dontwarn com.google.errorprone.annotations.**
-dontwarn com.google.j2objc.annotations.**
-dontwarn javax.annotation.**
-dontwarn org.checkerframework.**
-dontwarn org.codehaus.mojo.animal_sniffer.**

# Readable stack traces from the mapping-free release: keep file names and line numbers.
-keepattributes SourceFile,LineNumberTable
