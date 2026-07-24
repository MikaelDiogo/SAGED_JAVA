plugins {
	`java-library`
}

group = "br.gov.crateus.bcm"
version = "0.1.0"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	api(project(":bcm-sdk-api"))

	compileOnly("org.springframework.boot:spring-boot-starter-web:3.5.3")
	compileOnly("org.springframework.boot:spring-boot-starter-data-jpa:3.5.3")
	compileOnly("org.springframework.boot:spring-boot-starter-security:3.5.3")
	compileOnly("org.springframework.boot:spring-boot-starter-validation:3.5.3")
	compileOnly("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.9")

	testImplementation("org.springframework.boot:spring-boot-starter-test:3.5.3")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
