plugins {
	java
	id("org.springframework.boot") version "4.1.2"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.opscopilot"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.2.0")
	annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

	// ─────────────────────────────────────────────────────────────
	// Spring AI – LLM integration (Week 1)
	// ─────────────────────────────────────────────────────────────
	implementation(platform("org.springframework.ai:spring-ai-bom:2.0.0"))
	implementation("org.springframework.ai:spring-ai-starter-model-openai")
	//Support multi-providers
    implementation("org.springframework.ai:spring-ai-starter-model-anthropic")
    implementation("org.springframework.ai:spring-ai-starter-model-ollama")



	testImplementation("org.springframework.ai:spring-ai-test")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
