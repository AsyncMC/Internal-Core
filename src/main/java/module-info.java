module com.github.asyncmc.internal.core {
    requires com.github.asyncmc.module.api;
    uses com.github.asyncmc.module.api.AsyncMcModule;

    requires kotlin.stdlib;
    requires clikt.jvm;

    exports com.github.asyncmc.core.boot;
}
