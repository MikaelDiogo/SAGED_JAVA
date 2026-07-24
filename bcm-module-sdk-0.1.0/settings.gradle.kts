plugins {
	id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

rootProject.name = "bcm-module-sdk"

include("bcm-sdk-api")
include("bcm-dev-host")
include("module-skeleton")
include("saged")