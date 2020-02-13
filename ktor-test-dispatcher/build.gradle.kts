
kotlin {
    sourceSets {
        configure(listOf(watchosArm32().compilations.get("main").defaultSourceSet)) {
            kotlin.srcDir("iosArm32/src")
        }

        configure(
            listOf(
                watchosArm64().compilations.get("main").defaultSourceSet,
                tvosArm64().compilations.get("main").defaultSourceSet
            )
        ) {
            kotlin.srcDir("iosArm64/src")
        }

        configure(
            listOf(
                watchosX86().compilations.get("main").defaultSourceSet,
                tvosX64().compilations.get("main").defaultSourceSet
            )
        ) {
            kotlin.srcDir("iosX64")
        }
    }
}
