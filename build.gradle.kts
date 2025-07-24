import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.gradle.process.CommandLineArgumentProvider
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.TestIdePerformanceTask

fun properties(key: String)      = providers.gradleProperty(key)
fun environment(key: String)     = providers.environmentVariable(key)

plugins {
    id("java")
    alias(libs.plugins.kotlin)
    alias(libs.plugins.gradleIntelliJPlugin)   // jetzt das neue 2.x-Plugin
    alias(libs.plugins.changelog)
    alias(libs.plugins.qodana)
    alias(libs.plugins.kover)
}

group   = properties("pluginGroup").get()
version = properties("pluginVersion").get()

/* ------------------------------------------------------------------- */
/*  REPOSITORIES                                                       */
/* ------------------------------------------------------------------- */
repositories {
    mavenCentral()
    intellijPlatform {                       // neue Helper-API
        defaultRepositories()
    }
}

/* ------------------------------------------------------------------- */
/*  DEPENDENCIES                                                       */
/* ------------------------------------------------------------------- */
dependencies {
    /* IntelliJ-Basis (ersetzt das frühere intellij{}-Block-Konzept) */
    intellijPlatform {
        // Ziel-IDE (PC = PyCharm Community) bleibt wie gehabt
        val type    = properties("platformType").get()        // "PC"
        val version = properties("platformVersion").get()     // "2022.3.3"
        create(type, version)

        /* -------------------------------------------------- */
        /*  Bundled Plugins                                   */
        /* -------------------------------------------------- */
        bundledPlugins(
            providers.gradleProperty("platformBundledPlugins")
                .map { it.split(',').map(String::trim).filter(String::isNotEmpty) }
        )

        // Falls irgendwann auf PY umgestellt werden sollte:
        // if (type == "PY") {
        //     bundledPlugins("PythonCore", "Pythonid")
        // }

        testFramework(TestFrameworkType.Platform)
    }

    // --- Deine bisherigen Bibliotheken ---
    implementation("jakarta.websocket:jakarta.websocket-api:2.2.0")
    implementation("jakarta.servlet:jakarta.servlet-api:6.1.0")
    implementation("org.glassfish.tyrus:tyrus-server:2.2.0")
    implementation("org.glassfish.tyrus:tyrus-container-grizzly-server:2.2.0")
    implementation("net.sourceforge.plantuml:plantuml:1.2025.4")

    testImplementation("org.mockito:mockito-core:4.11.0")
    testImplementation("org.mockito:mockito-inline:4.11.0")
    testImplementation("junit:junit:4.13.2")

    // Klassen aus dem Plugin-Quellset dem Test-Classpath hinzufügen
    testImplementation(sourceSets.main.get().output)
}

/* ------------------------------------------------------------------- */
/*  JVM-Einstellungen                                                  */
/* ------------------------------------------------------------------- */
kotlin {
    jvmToolchain(17)
}

/* ------------------------------------------------------------------- */
/*  IntelliJ-Platform-Extension (ersetzt intellij{}-Block)             */
/* ------------------------------------------------------------------- */
intellijPlatform {
    pluginConfiguration {
        name.set(properties("pluginName"))          // früher intellij.pluginName
    }
    // since/untilBuild werden wie gehabt von patchPluginXml gepflegt
}

/* ------------------------------------------------------------------- */
/*  CHANGELOG, COVERAGE, WRAPPER …                                    */
/* ------------------------------------------------------------------- */
changelog {
    groups.empty()
    repositoryUrl = properties("pluginRepositoryUrl")
}

kover { /* unverändert */ }

tasks {

    /* ------------------------ Wrapper -------------------------------- */
    wrapper {
        gradleVersion = properties("gradleVersion").get()
    }

    /* ------------------------ Plugin-XML ----------------------------- */
    patchPluginXml {
        version     = properties("pluginVersion")
        sinceBuild  = properties("pluginSinceBuild")
        untilBuild  = properties("pluginUntilBuild")

        pluginDescription = providers
            .fileContents(layout.projectDirectory.file("README.md"))
            .asText.map {
                val start = "<!-- Plugin description -->"
                val end   = "<!-- Plugin description end -->"
                with(it.lines()) {
                    if (!containsAll(listOf(start, end))) {
                        throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                    }
                    subList(indexOf(start) + 1, indexOf(end))
                        .joinToString("\n").let(::markdownToHTML)
                }
            }

        val changelog = project.changelog
        changeNotes = properties("pluginVersion").map { vers ->
            with(changelog) {
                renderItem(
                    (getOrNull(vers) ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    Changelog.OutputType.HTML,
                )
            }
        }
    }

    /* ------------------------ IDE-abhängige Unit-Tests --------------- */
    // JUnit-basierte Unit- / Light-Tests
    test {
        useJUnit()
        jvmArgs("--add-exports", "java.base/jdk.internal.vm=ALL-UNNAMED")
        systemProperty("java.awt.headless", "true") // optional
    }

    // Integration-Tests in einer echten IDE-Instanz (bleibt unverändert)
    val testIde by intellijPlatformTesting.testIde.registering {
        task {
            useJUnit()
            jvmArgs("--add-exports", "java.base/jdk.internal.vm=ALL-UNNAMED")
        }
    }

    /* ------------------------ UI-Tests ------------------------------- */
    val runIdeForUiTests by intellijPlatformTesting.runIde.registering {
        task {
            jvmArgumentProviders += CommandLineArgumentProvider {
                listOf(
                    "-Drobot-server.port=8082",
                    "-Dide.mac.message.dialogs.as.sheets=false",
                    "-Djb.privacy.policy.text=<!--999.999-->",
                    "-Djb.consents.confirmation.enabled=false",
                )
            }
        }
        plugins { robotServerPlugin() }
    }

    /* ------------------------ Performance-Tests deaktivieren --------- */
    withType<TestIdePerformanceTask>().configureEach {
        enabled = false
    }

    /* ------------------------ Build-Lifecycle ------------------------ */
    check { dependsOn(test, testIde) }          // sorgt dafür, dass testIde im build-Task läuft

    /* ------------------------ IDE-Run-Task --------------------------- */
    runIde {
        jvmArgs = listOf("--add-exports", "java.base/jdk.internal.vm=ALL-UNNAMED")
    }

    /* ------------------------ Sign & Publish ------------------------- */
    signPlugin {
        certificateChain = environment("CERTIFICATE_CHAIN")
        privateKey       = environment("PRIVATE_KEY")
        password         = environment("PRIVATE_KEY_PASSWORD")
    }

    publishPlugin {
        dependsOn("patchChangelog")
        token    = environment("PUBLISH_TOKEN")
        channels = properties("pluginVersion").map {
            listOf(it.substringAfter('-', "").substringBefore('.').ifEmpty { "default" })
        }
    }
}