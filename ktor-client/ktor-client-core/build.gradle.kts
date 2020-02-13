import org.jetbrains.kotlin.gradle.plugin.*

description = "Ktor http client"

val coroutines_version: String by project

val node_fetch_version: String by project
val abort_controller_version: String by project
val ws_version: String by project

kotlin.sourceSets {
    commonMain {
        dependencies {
            api(project(":ktor-http"))
            api(project(":ktor-http:ktor-http-cio"))
        }
    }

    jvmMain {
        dependencies {
            api(project(":ktor-network"))
        }
    }

    jsMain {
        dependencies {
            api(npm("node-fetch", node_fetch_version))
            api(npm("abort-controller", abort_controller_version))
            api(npm("ws", ws_version))
        }
    }

    jvmTest {
        dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-debug:$coroutines_version")
        }
    }
}
