import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.gradle.process.CommandLineArgumentProvider
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.TestIdePerformanceTask

// für DSL-Helfer im Tasks-Block
import org.gradle.kotlin.dsl.registering
import org.gradle.kotlin.dsl.withType

/* ------------------------------------------------------------------- */
/*  Helpers                                                            */
/* ------------------------------------------------------------------- */
fun properties(key: String)  = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)

/* ------------------------------------------------------------------- */
/*  Plugins                                                            */
/* ------------------------------------------------------------------- */
plugins {
    id("java")
    alias(libs.plugins.kotlin)
    alias(libs.plugins.gradleIntelliJPlugin)   // IntelliJ Platform Gradle Plugin 2.x
    alias(libs.plugins.changelog)
    alias(libs.plugins.qodana)
    alias(libs.plugins.kover)
}

group   = properties("pluginGroup").get()
version = properties("pluginVersion").get()

/* ------------------------------------------------------------------- */
/*  Repositories                                                       */
/* ------------------------------------------------------------------- */
repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

/* ------------------------------------------------------------------- */
/*  Dependencies                                                       */
/* ------------------------------------------------------------------- */
dependencies {
    // IntelliJ-Basis (ersetzt früheren intellij{}-Block)
    intellijPlatform {
        // Ziel-IDE (PyCharm Community)
        val type    = properties("platformType").get()    // z. B. "PC"
        val version = properties("platformVersion").get() // z. B. "2022.3.3"
        create(type, version)

        // Bundled Plugins (aus gradle.properties)
        bundledPlugins(
            providers.gradleProperty("platformBundledPlugins")
                .map { it.split(',').map(String::trim).filter(String::isNotEmpty) }
        )

        testFramework(TestFrameworkType.Platform)
    }

    // --- Weitere Bibliotheken ---
    implementation("jakarta.websocket:jakarta.websocket-api:2.2.0")
    implementation("jakarta.servlet:jakarta.servlet-api:6.1.0")
    implementation("org.glassfish.tyrus:tyrus-server:2.2.0")
    implementation("org.glassfish.tyrus:tyrus-container-grizzly-server:2.2.0")
    implementation("net.sourceforge.plantuml:plantuml:1.2025.4")
    implementation("com.google.code.gson:gson:2.11.0")

    testImplementation("org.mockito:mockito-core:4.11.0")
    testImplementation("org.mockito:mockito-inline:4.11.0")
    testImplementation("junit:junit:4.13.2")

    // Klassen aus dem Plugin-Quellset dem Test-Classpath hinzufügen
    testImplementation(sourceSets.main.get().output)
}

/* ------------------------------------------------------------------- */
/*  JVM                                                                */
/* ------------------------------------------------------------------- */
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

/* ------------------------------------------------------------------- */
/*  Signing (ENV → Dateien)                                            */
/* ------------------------------------------------------------------- */
val certChainEnv  = providers.environmentVariable("CERTIFICATE_CHAIN")
val privateKeyEnv = providers.environmentVariable("PRIVATE_KEY")
val privateKeyPwd = providers.environmentVariable("PRIVATE_KEY_PASSWORD")

val signingCertFile = layout.buildDirectory.file("signing/chain.crt")
val signingKeyFile  = layout.buildDirectory.file("signing/private.pem")

val writeSigningFiles by tasks.registering {
    notCompatibleWithConfigurationCache("Writes signing files from env vars at execution time")
    // nur ausführen, wenn Secrets gesetzt sind
    onlyIf { certChainEnv.orNull != null && privateKeyEnv.orNull != null }
    doNotTrackState("Writes secret files which must not be cached")
    outputs.files(signingCertFile, signingKeyFile)
    doLast {
        signingCertFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText(certChainEnv.get())
        }
        signingKeyFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText(privateKeyEnv.get())
        }
    }
}

/* ------------------------------------------------------------------- */
/*  IntelliJ Platform 2.x Extension                                    */
/* ------------------------------------------------------------------- */
intellijPlatform {
    pluginConfiguration {
        name.set(properties("pluginName"))
    }

    // IDE-Liste für Plugin-Verifier (keine EAP/Snapshots, nur stabile PC-Builds)
    pluginVerification {
        ides {
            ide("PC", "2025.2.4")
            ide("PC", "2025.1.5")
            ide("PC", "2024.3.6")
            ide("PC", "2024.2.6")
            ide("PC", "2024.1.7")
            ide("PC", "2023.3.7")
            ide("PC", "2022.3.3")
        }
    }

    signing {
        if (certChainEnv.orNull != null && privateKeyEnv.orNull != null) {
            // Variante: Secrets enthalten PEM-Inhalte → in Dateien schreiben
            certificateChainFile.set(signingCertFile)
            privateKeyFile.set(signingKeyFile)
            password.set(privateKeyPwd)

            // sicherstellen, dass Dateien existieren
            tasks.named("publishPlugin").configure { dependsOn(writeSigningFiles) }
            tasks.matching { it.name.contains("buildPlugin") }.configureEach { dependsOn(writeSigningFiles) }
        } else {
            // Fallback: Dateien aus dem Repo (lokale Builds)
            certificateChainFile.set(layout.projectDirectory.file("certs/chain.crt"))
            privateKeyFile.set(layout.projectDirectory.file("certs/private.pem"))
            password.set(providers.gradleProperty("CERT_PASSWORD").orNull)
        }
    }

    publishing {
        // JetBrains Marketplace Token aus ENV
        token.set(providers.environmentVariable("PUBLISH_TOKEN"))
    }
}

/* ------------------------------------------------------------------- */
/*  Changelog / Coverage                                               */
/* ------------------------------------------------------------------- */
changelog {
    groups.empty()
    repositoryUrl = properties("pluginRepositoryUrl")
}

kover { /* unverändert */ }

/* ------------------------------------------------------------------- */
/*  Tasks                                                              */
/* ------------------------------------------------------------------- */
tasks {

    /* ------------------------ Wrapper -------------------------------- */
    wrapper {
        gradleVersion = properties("gradleVersion").get()
    }

    /* ------------------------ Plugin-XML ----------------------------- */
    patchPluginXml {
        version    = properties("pluginVersion").get()
        sinceBuild = properties("pluginSinceBuild").get()

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

    /* ------------------------ Unit-Tests ----------------------------- */
    test {
        useJUnit()
        jvmArgs("--add-exports", "java.base/jdk.internal.vm=ALL-UNNAMED")
        systemProperty("java.awt.headless", "true")
    }

    /* ------------------------ Integration-Tests (echte IDE) ---------- */
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
    check { dependsOn(test, testIde) }

    /* ------------------------ IDE-Run-Task --------------------------- */
    runIde {
        jvmArgs = listOf("--add-exports", "java.base/jdk.internal.vm=ALL-UNNAMED")
    }
}