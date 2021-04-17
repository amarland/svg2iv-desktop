plugins {
    groovy
}

/*
 * repositories {
 *     mavenCentral()
 * }
 */

dependencies {
    implementation(gradleApi())
    implementation(localGroovy())
    /*
     * All hell breaks loose when I add the tests, so no tests. For now?
     * testImplementation("org.jetbrains:annotations:20.1.0")
     * testImplementation("org.junit.vintage:junit-vintage-engine:5.7.1")
     * testImplementation("org.mockito:mockito-core:3.9.0")
     */
}
