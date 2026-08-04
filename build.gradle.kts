plugins{
    id("java-library")
    id("maven-publish")
}
val group_id:String=rootProject.property("group_id").toString()
val core_version:String=rootProject.property("version").toString()
val java_version:String=rootProject.property("java_version").toString()
group=group_id
version=core_version
java{
    toolchain{
        languageVersion.set(JavaLanguageVersion.of(java_version.toInt()))
    }
    withSourcesJar()
    withJavadocJar()
}
repositories{
    maven("https://maven.aliyun.com/repository/public")
    mavenCentral()
}
dependencies{
    implementation("com.github.wendykierp:JTransforms:3.2"){
        isTransitive=false
    }
    implementation("org.visnow:JLargeArrays:1.7"){
        isTransitive=false
    }
    implementation("org.apache.commons:commons-math3:3.6.1")
    implementation("org.yaml:snakeyaml:2.6")
}
publishing{
    publications{
        create<MavenPublication>("maven"){
            from(components["java"])
        }
    }
}