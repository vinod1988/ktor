import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*

apply(from = rootProject.file("gradle/experimental.gradle"))

val experimentalAnnotations: List<String> by project.ext

kotlin {
    linuxX64()
    mingwX64()

    val nsTargets = listOf(
        macosX64(),
        iosX64(),
        iosArm32(),
        iosArm64(),
        tvosArm64(),
        tvosX64(),
        watchosX86(),
        watchosArm32(),
        watchosArm64()
    )

    data class DefaultSourceSets(val main: KotlinSourceSet, val test: KotlinSourceSet)

    fun KotlinNativeTarget.defaultSourceSets(): DefaultSourceSets = DefaultSourceSets(
        compilations.getByName(KotlinCompilation.Companion.MAIN_COMPILATION_NAME).defaultSourceSet,
        compilations.getByName(KotlinCompilation.Companion.TEST_COMPILATION_NAME).defaultSourceSet
    )

    fun List<KotlinNativeTarget>.defaultSourceSets(): List<DefaultSourceSets> = map { it.defaultSourceSets() }

    fun createIntermediateSourceSet(
        name: String,
        children: List<KotlinSourceSet>,
        parent: KotlinSourceSet? = null
    ): KotlinSourceSet = sourceSets.maybeCreate(name).apply {
        parent?.let { dependsOn(parent) }
        children.forEach {
            it.dependsOn(this)
        }
    }

    fun createIntermediateSourceSets(
        namePrefix: String,
        children: List<DefaultSourceSets>,
        parent: DefaultSourceSets? = null
    ): DefaultSourceSets {
        val main = createIntermediateSourceSet("${namePrefix}Main", children.map { it.main }, parent?.main)
        val test = createIntermediateSourceSet("${namePrefix}Test", children.map { it.test }, parent?.test)
        return DefaultSourceSets(main, test)
    }

    fun mostCommonSourceSets() = DefaultSourceSets(
        sourceSets.getByName(KotlinSourceSet.Companion.COMMON_MAIN_SOURCE_SET_NAME),
        sourceSets.getByName(KotlinSourceSet.COMMON_TEST_SOURCE_SET_NAME)
    )


    val children = nsTargets.defaultSourceSets()
    val parent = mostCommonSourceSets()
    createIntermediateSourceSets("ns", children, parent)

    configure(sourceSets) {
        experimentalAnnotations.forEach {
            languageSettings.useExperimentalAnnotation(it)
        }
    }

    sourceSets {
        val nsMain by getting {
            kotlin.srcDir("ns/src")
        }
    }
}
