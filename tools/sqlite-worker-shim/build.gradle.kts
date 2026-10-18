// Build-time only: put on the classpath of SQLDelight's process-isolated workers.
// Never packaged into the app.
plugins {
    `java-library`
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
