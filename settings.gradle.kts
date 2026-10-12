pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}

rootProject.name = "checkout-mvi"

// Dependency rule, enforced by the module graph:
//
//   :app ──► :feature:checkout ──► :core:ui ──► :core:mvi
//     │              │                 │
//     │              └───────┬─────────┘
//     │                      ▼
//     └──► :data ────────► :domain  (pure Kotlin, depends on nothing but coroutines + javax.inject)
//
// :feature:checkout cannot see :data. Only :app knows both, so only :app assembles the Hilt graph.

include(":app")
include(":core:mvi")
include(":core:ui")
include(":domain")
include(":data")
include(":feature:checkout")
