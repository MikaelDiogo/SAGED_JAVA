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
	compileOnly("org.springframework.boot:spring-boot-starter-data-jpa:3.5.3")
}
