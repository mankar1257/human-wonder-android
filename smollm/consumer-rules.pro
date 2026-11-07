# Keep native methods
-keepclasseswithmembernames class ai.humanwonder.poc.smollm.** {
    native <methods>;
}
-keep class ai.humanwonder.poc.smollm.SmolLM { *; }
-keep class ai.humanwonder.poc.smollm.GGUFReader { *; }
