plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)

    alias(libs.plugins.googleService)

    alias(libs.plugins.map.secret)
}

android {
    namespace = "com.myiphone.store"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.myiphone.store"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["MAPAS_KEY"] = "DEFAULT_API_KEY"
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
    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.lottie) /*Animaciones*/
    implementation(libs.firebaseAuth) /*Autenticaciión Firebase*/
    implementation(libs.firebaseDatabase) /*Base de datos Firebase*/
    implementation(libs.imagePicker) /*Recortar una imagen*/
    implementation(libs.glide) /*Leer Imagenes*/
    implementation(libs.storage) /*Subir archivos multimedia*/
    implementation(libs.authGoogle) /*Iniciar con google*/
    implementation(libs.google.maps) /*Google Maps*/
    implementation(libs.biometric) /*Autenticacion biometrica (huella)*/
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}