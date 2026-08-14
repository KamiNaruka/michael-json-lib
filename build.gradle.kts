plugins {
    `java-library`
    `maven-publish`
}

group = "heehee.michael.json"
version = "2.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }

    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {
                name.set("michael-json-lib")
                description.set("A simple JSON library for Java")
                url.set("https://github.com/KamiNaruka/michael-json-lib")

                developers {
                    developer {
                        id.set("KamiNaruka")
                        name.set("kamimuse.")
                    }
                }

                scm {
                    connection.set("scm:git:https://github.com/KamiNaruka/michael-json-lib.git")
                    developerConnection.set("scm:git:ssh://git@github.com/KamiNaruka/michael-json-lib.git")
                    url.set("https://github.com/KamiNaruka/michael-json-lib")
                }
            }
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/KamiNaruka/michael-json-lib")

            credentials {
                username = System.getenv("USERNAME")
                password = System.getenv("TOKEN")
            }
        }
    }
}
