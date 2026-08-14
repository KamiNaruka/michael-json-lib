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

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            pom {
                description.set("a simple json java lib")
                url.set("https://github.com/KaniNaurka/michael-json-lib")
                developers {
                    developer {
                        id.set("KaniNaurka")
                        name.set("kamimuse.")
                    }
                }
                scm {
                    connection.set("scm:git:github.com/KaniNaurka/michael-json-lib.git")
                    developerConnection.set("scm:git:ssh://github.com/KaniNaurka/michael-json-lib.git")
                    url.set("https://github.com/KaniNaurka/michael-json-lib")
                }
            }
        }
    }
    
    repositories {
        maven {
            name = "Build"
            url = uri(layout.buildDirectory.dir("maven"))
        }
    }
}
