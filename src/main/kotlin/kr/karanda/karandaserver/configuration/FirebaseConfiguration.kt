package kr.karanda.karandaserver.configuration

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.io.File
import java.io.FileInputStream
import javax.annotation.PostConstruct

@Profile("develop")
@Configuration("FirebaseConfiguration")
class FirebaseConfigurationForDev {

    @PostConstruct
    fun initializeFirebase() {
        val token = FileInputStream(File("./credentials/token.json"))
        val options: FirebaseOptions = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(token))
            .build()
        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options)
        }
    }
}

@Profile("production")
@Configuration("FirebaseConfiguration")
class FirebaseConfigurationForProd {

    @PostConstruct
    fun initializeFirebase() {
        val options: FirebaseOptions = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.getApplicationDefault())
            .setProjectId("karanda-384102")
            .build()
        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options)
        }
    }
}
