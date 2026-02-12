/**
 * to relaase a new library, just do this:
 * $ gradle clean publishMavenKotlinPublicationToPreDeployRepository
 * $ gradle publish jreleaserDeploy
 */
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val signingPassword: String by project
val centralUsername: String by project
val centralPassword: String by project

plugins {
    java
    kotlin("jvm") version "2.1.20"
    `maven-publish`
    signing
    id("org.jreleaser") version "1.18.0"
}

group = "com.github.isyscore"
version = "1.0.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.isyscore:common-jvm:3.0.0.7")
    testImplementation("junit:junit:4.13.2")
}

java {
    withJavadocJar()
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenKotlin") {
            from(components["java"])

            pom {
                name.set("ison-jvm")
                description.set("The JVM implementation of ISON")
                url.set("https://github.com/rarnu/ison-jvm")
                packaging = "jar"

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://github.com/rarnu/ison-jvm/blob/main/LICENSE")
                    }
                }
                developers {
                    developer {
                        id.set("isyscore")
                        name.set("isyscore")
                        email.set("hexj@isyscore.com")
                    }
                }
                scm {
                    connection.set("https://github.com/rarnu/ison-jvm")
                    developerConnection.set("https://github.com/rarnu/ison-jvm")
                    url.set("https://github.com/rarnu/ison-jvm")
                }
            }
        }
    }

    repositories {
        maven {
            name = "LocalMavenWithChecksums"
            url = uri(layout.buildDirectory.dir("staging-deploy"))
        }
        maven {
            name = "PreDeploy"
            url = uri(layout.buildDirectory.dir("pre-deploy"))
        }
    }
}

tasks.withType<Jar> {
    doLast {
        ant.withGroovyBuilder {
            "checksum"("algorithm" to "md5", "file" to archiveFile.get())
            "checksum"("algorithm" to "sha1", "file" to archiveFile.get())
        }
    }
}

jreleaser {
    project {
        copyright.set("isyscore.com")
        description.set("iSysCore Common Kotlin Library")
    }
    signing {
        setActive("ALWAYS")
        armored = true
        setMode("FILE")
        publicKey = "public.key"
        secretKey = "private.key"
        passphrase = signingPassword
    }
    deploy {
        maven {
            mavenCentral {
                create("sonatype") {
                    setActive("ALWAYS")
                    url = "https://central.sonatype.com/api/v1/publisher"
                    username = centralUsername
                    password = centralPassword
                    stagingRepository("build/pre-deploy")
                }
            }
        }
    }
}