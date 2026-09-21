plugins {
    id("org.springframework.boot") version "4.1.0"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "2.3.20"
    kotlin("plugin.spring") version "2.3.20"
    kotlin("plugin.allopen") version "2.3.20"
    kotlin("plugin.serialization") version "2.3.20"
    kotlin("plugin.jpa") version "2.3.20"
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.Embeddable")
    annotation("jakarta.persistence.MappedSuperclass")
}

group = "kr.karanda"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-authorization-server")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-quartz")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    compileOnly("org.projectlombok:lombok")
    runtimeOnly("com.h2database:h2")
    runtimeOnly("org.mariadb.jdbc:mariadb-java-client")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation("com.google.firebase:firebase-admin:9.9.0")
    // firebase-admin이 transitive로 해석하는 버전과 동일하게 핀 고정(버전 드리프트 방지).
    implementation("com.google.cloud:google-cloud-storage:2.64.0")

    implementation("org.jsoup:jsoup:1.18.1")

    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

// 테스트 분류 (@Tag 기준)
//  - 태그 없음/`unit`: 외부 의존성이 없는 테스트. `test`에서 실행되며 Dockerfile 빌드의 게이트가 된다.
//  - `integration`: Spring 컨텍스트가 필요해 DB·Redis·Firebase 자격증명을 요구한다. docker build 시점엔 없으므로 제외.
//  - `external`: 실제 라이브 외부 API(거래소·모험가 프로필)를 호출한다. 외부 사정으로 배포가 막히면 안 되므로 제외.
//
// 제외(include가 아닌 exclude) 방식이라 새로 추가한 테스트는 기본적으로 게이트에 포함된다.
// 외부 의존성이 있는 테스트를 태그 없이 추가하면 docker build가 즉시 깨지는데, 이는 의도된 실패다(태그를 붙이라는 신호).
tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.test {
    useJUnitPlatform {
        excludeTags("integration", "external")
    }
}

tasks.register<Test>("integrationTest") {
    description = "Spring 컨텍스트가 필요한 테스트만 실행한다. 로컬 develop 환경에서 수동 실행."
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        includeTags("integration")
    }
}

tasks.register<Test>("externalTest") {
    description = "실제 외부 API를 호출하는 계약 테스트만 실행한다. 수동 또는 스케줄 실행용."
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        includeTags("external")
    }
}
