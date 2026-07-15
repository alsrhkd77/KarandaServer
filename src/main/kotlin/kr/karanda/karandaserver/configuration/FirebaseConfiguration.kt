package kr.karanda.karandaserver.configuration

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.Firestore
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.FirestoreClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.io.File
import java.io.FileInputStream
import jakarta.annotation.PostConstruct

private const val GCP_PROJECT_ID = "karanda-384102"

@Profile("develop")
@Configuration("FirebaseConfiguration")
class FirebaseConfigurationForDev {

    @PostConstruct
    fun initializeFirebase() {
        val options: FirebaseOptions = FirebaseOptions.builder()
            .setCredentials(credentials())
            .build()
        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options)
        }
    }

    @Bean
    fun firestore(): Firestore {
        return FirestoreClient.getFirestore()
    }

    // 버킷은 Firebase에 연결되어 있지 않으므로 firebase-admin과 동일한 자격증명으로 GCS 클라이언트를 구성한다.
    @Bean
    fun storage(): Storage {
        return StorageOptions.newBuilder()
            .setCredentials(credentials())
            .setProjectId(GCP_PROJECT_ID)
            .build()
            .service
    }

    private fun credentials(): GoogleCredentials {
        return FileInputStream(File("./credentials/token.json")).use { GoogleCredentials.fromStream(it) }
    }
}

@Profile("production")
@Configuration("FirebaseConfiguration")
class FirebaseConfigurationForProd {

    @PostConstruct
    fun initializeFirebase() {
        val options: FirebaseOptions = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.getApplicationDefault())
            .setProjectId(GCP_PROJECT_ID)
            .build()
        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options)
        }
    }

    @Bean
    fun firestore(): Firestore {
        return FirestoreClient.getFirestore()
    }

    // 버킷은 Firebase에 연결되어 있지 않으므로 firebase-admin과 동일한 ADC 자격증명으로 GCS 클라이언트를 구성한다.
    @Bean
    fun storage(): Storage {
        return StorageOptions.newBuilder()
            .setCredentials(GoogleCredentials.getApplicationDefault())
            .setProjectId(GCP_PROJECT_ID)
            .build()
            .service
    }
}
