// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.4.1" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("com.google.devtools.ksp") version "1.9.22-1.0.17" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22" apply false
}

// Workaround for IDEs that request this task per-module.
subprojects {
    tasks.register("prepareKotlinBuildScriptModel") {}
}
