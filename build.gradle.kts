import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    // Root project uses convention plugins only
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}

subprojects {
    plugins.withId("com.android.library") {
        apply(plugin = "jacoco")
        configureAndroidJacoco()
    }
    plugins.withId("com.android.application") {
        apply(plugin = "jacoco")
        configureAndroidJacoco()
    }
}

private fun Project.configureAndroidJacoco() {
    extensions.configure<JacocoPluginExtension> {
        toolVersion = "0.8.10"
    }

    tasks.withType<Test>().configureEach {
        extensions.configure(JacocoTaskExtension::class) {
            isIncludeNoLocationClasses = true
            excludes = listOf("jdk.internal.*")
        }
    }

    afterEvaluate {
        val testTask = tasks.findByName("testDebugUnitTest") as? Test ?: return@afterEvaluate
        val reportTask = tasks.register<JacocoReport>("jacocoDebugUnitTestReport") {
            dependsOn(testTask)

            reports {
                xml.required.set(true)
                html.required.set(true)
            }

            val coverageExcludes = coverageExclusionPatterns()
            val javaClasses = fileTree("${buildDir}/intermediates/javac/debug/classes") {
                excludeGeneratedClasses()
                exclude(coverageExcludes)
            }
            val kotlinClasses = fileTree("${buildDir}/tmp/kotlin-classes/debug") {
                excludeGeneratedClasses()
                exclude(coverageExcludes)
            }

            classDirectories.setFrom(files(javaClasses, kotlinClasses))
            sourceDirectories.setFrom(
                files(
                    "src/main/java",
                    "src/main/kotlin"
                )
            )
            executionData.setFrom(
                fileTree(buildDir) {
                    include(
                        "jacoco/testDebugUnitTest.exec",
                        "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec"
                    )
                }
            )
        }

        tasks.named("check").configure {
            dependsOn(reportTask)
        }
    }
}

private fun ConfigurableFileTree.excludeGeneratedClasses() {
    exclude("**/R.class", "**/R$*.class", "**/Manifest*.*", "**/BuildConfig.*", "**/*\$inlined\$*")
}

private fun coverageExclusionPatterns(): List<String> = listOf(
    "**/AgeRegressionEstimator*.class",
    "**/ImageProxyUtils.class",
    "**/MlKitFaceAnalyzer*.class",
    "**/FaceInsightsAnalyzer*.class",
    "**/ImageProxyFaceCropper*.class",
    "**/DefaultInterpreterFactory*.class"
)
