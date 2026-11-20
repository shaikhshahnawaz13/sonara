# Used by the "perf" build type only (release minification is configured in M8).
# Names are kept readable so crash reports stay useful; R8 still removes dead code and optimises.
-dontobfuscate

# Optional annotation-only dependencies referenced by Guava and friends.
-dontwarn com.google.errorprone.annotations.**
-dontwarn com.google.j2objc.annotations.**
-dontwarn javax.annotation.**
-dontwarn org.checkerframework.**
-dontwarn org.codehaus.mojo.animal_sniffer.**
