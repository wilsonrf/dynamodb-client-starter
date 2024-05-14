/*
* Copyright 2024 Wilson da Rocha França
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
 */
plugins {
    id("java-library")
    id("org.springframework.boot") apply false
    id("io.spring.dependency-management")
    id("com.github.ben-manes.versions")
    id("maven-publish")
    id("signing")
}

group = "com.wilsonfranca"

repositories {
    mavenLocal()
    mavenCentral()
}
dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    }
}
dependencies {
    api("com.wilsonfranca:dynamodb-client-autoconfigure:1.0.0-SNAPSHOT")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
}

tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(sourceSets["main"].allSource)
}

tasks.register<Jar>("javadocJar") {
    dependsOn("javadoc")
    archiveClassifier.set("javadoc")
    from(tasks["javadoc"].outputs)
}

val zipArtifacts by tasks.registering(Zip::class) {
    dependsOn("publishMavenJavaPublicationToInternalRepoRepository")
    from("${layout.buildDirectory.get()}/repo") {
        exclude("**/maven-metadata*.*")
    }
    archiveFileName.set("${project.name}-${version}.zip")
    destinationDirectory.set(file("${layout.buildDirectory.get()}/outputs"))
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(tasks["sourcesJar"])
            artifact(tasks["javadocJar"])
            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }
            pom {
                name = "DynamoDB Client Starter"
                description = "DynamoDB Client Spring Boot Starter"
                url = "https://github.com/wilsonrf/dynamodb-client-starter"
                licenses {
                    license {
                        name = "The Apache License, Version 2.0"
                        url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }
                developers {
                    developer {
                        id = "wilsonrf"
                        name = "Wilson da Rocha França"
                        email = "wilsonrf@gmail.com"
                    }
                }
                scm {
                    connection = "scm:git:git@github.com:wilsonrf/dynamodb-client-starter.git"
                    developerConnection = "scm:git:git@github.com:wilsonrf/dynamodb-client-starter.git"
                    url = "https://github.com/wilsonrf/dynamodb-client-starter"
                }
            }
        }
    }

    repositories {
        maven {
            name = "internalRepo"
            url = uri("${layout.buildDirectory.get()}/repo")
        }
    }
}

signing {
    setRequired {
        gradle.taskGraph.allTasks.any { it is PublishToMavenLocal }.not()
    }

    val signingKey: String? by project
    val signingKeyId: String? by project
    val signingPassword: String? by project

    useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)

    sign(publishing.publications["mavenJava"])
}