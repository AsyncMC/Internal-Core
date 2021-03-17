module com.github.asyncmc.internal.core {
    requires transitive com.github.asyncmc.module.api;
    uses com.github.asyncmc.module.api.ModuleLoader;

    requires static clikt.jvm;
    requires kotlin.inline.logger.jvm;

    exports com.github.asyncmc.core.boot;
}
