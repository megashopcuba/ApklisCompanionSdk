import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.maven.publish)
}

android {
    namespace = "cu.apkuba.sdk"
    compileSdk = 36

    defaultConfig {
        minSdk = 21

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        // Información de la librería
        buildConfigField("String", "LIBRARY_VERSION", "\"${libs.versions.apKubaSdk.get()}\"")
        buildConfigField("String", "LIBRARY_NAME", "\"ApKubaSdk\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    // Add or modify the kotlin block like this:
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11) // Or JVM_11, JVM_17, etc.
            // You might have other compiler options here
        }
    }
    buildFeatures {
        buildConfig = true
    }
    // Configuración para generar fuentes y documentación
    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.logger)
    implementation(libs.okhttp)
    implementation(libs.okhttp.tls)

    //   Qr implementations
    implementation(libs.core)
    implementation(libs.javase)
    implementation(libs.code.scanner)
    implementation(libs.gson)
    implementation(libs.apklislicensevalidator)


}

// Configuración de publicación
publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "cu.apkuba.sdk"
            artifactId = "apkuba-sdk"
            version = libs.versions.apKubaSdk.get()

            afterEvaluate {
                from(components["release"])
            }

            pom {
                name.set("ApKuba SDK")
                description.set("ApKuba SDK para verificación de compras y licencias de Apklis")
                url.set("https://github.com/virtualShopRest/ApKubaSdk")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("virtualShopRest")
                        name.set("VirtualShop")
                        email.set("virtual.shop.rest@gmail.com")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/virtualShopRest/ApKubaSdk.git")
                    developerConnection.set("scm:git:ssh://github.com:virtualShopRest/ApKubaSdk.git")
                    url.set("https://github.com/virtualShopRest/ApKubaSdk/tree/master")
                }
            }
        }
    }
}