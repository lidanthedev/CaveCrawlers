plugins {
    java
    `maven-publish`
    id("com.gradleup.shadow") version "9.3.2"
}

group = "me.lidan"
version = project.findProperty("version")?.toString()?.takeUnless { it == "unspecified" } ?: "dev"

repositories {
    mavenCentral()
    mavenLocal()
    maven {
        url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    }
    maven {
        name = "papermc-repo"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        name = "sonatype"
        url = uri("https://oss.sonatype.org/content/groups/public/")
    }
    maven { url = uri("https://jitpack.io") }
    maven {
        name = "CodeMC"
        url = uri("https://repo.codemc.io/repository/maven-public/")
    }
    maven { url = uri("https://mvn.lumine.io/repository/maven-public/") }
    maven {
        url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    }
    maven {
        url = uri("https://repo.granny.dev/snapshots/")
    }
    maven("https://repo.xenondevs.xyz/releases")
}

dependencies {
    compileOnly("org.projectlombok:lombok:1.18.46")
    annotationProcessor("org.projectlombok:lombok:1.18.46")
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("com.zaxxer:HikariCP:5.1.0")
    compileOnly("org.jdbi:jdbi3-core:3.45.4")
    compileOnly("org.jdbi:jdbi3-sqlobject:3.45.4")
    compileOnly("org.jetbrains:annotations:23.0.0")
    implementation("xyz.xenondevs.invui:invui:2.1.0")
    implementation("io.github.revxrsal:lamp.common:4.0.0-rc.17")
    implementation("io.github.revxrsal:lamp.bukkit:4.0.0-rc.17")
    implementation("io.github.revxrsal:lamp.brigadier:4.0.0-rc.17")
    compileOnly("net.dmulloy2:ProtocolLib:5.1.0")
    compileOnly("com.googlecode.json-simple:json-simple:1.1.1")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") {
        exclude(group = "org.bukkit", module = "bukkit")
    }
    compileOnly("io.lumine:Mythic-Dist:5.11.2")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("net.luckperms:api:5.4")
    implementation("com.github.cryptomorin:XSeries:13.7.0")
    implementation("dev.dejvokep:boosted-yaml:1.3.7")
    implementation("dev.dejvokep:boosted-yaml-spigot:1.5")
    implementation("com.github.Robotv2:PlaceholderAnnotationLib:v1.1.0")
}

tasks.compileJava {
    options.compilerArgs.add("-parameters")
}

tasks.shadowJar {
    archiveClassifier.set(null as String?)
    relocate("revxrsal.commands", "me.lidan.cavecrawlers.lamp")
    relocate("xyz.xenondevs.invui", "me.lidan.cavecrawlers.invui")
    relocate("com.cryptomorin.xseries", "me.lidan.cavecrawlers.xseries")
    relocate("dev.dejvokep.boostedyaml", "me.lidan.cavecrawlers.boostedyaml")
    relocate("fr.robotv2.placeholderannotationlib", "me.lidan.cavecrawlers.placeholderannotationlib")
}

tasks.jar {
    enabled = false
}

tasks.named("build") {
    dependsOn(tasks.shadowJar)
}

val targetJavaVersion = 25
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(targetJavaVersion)
}

tasks.processResources {
    val props = mapOf("version" to project.version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.register<Jar>("sourcesJar") {
    from(sourceSets.main.get().allSource)
    archiveClassifier.set("sources")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifact(tasks.shadowJar)
            artifact(tasks.named("sourcesJar"))
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()
        }
    }
}
