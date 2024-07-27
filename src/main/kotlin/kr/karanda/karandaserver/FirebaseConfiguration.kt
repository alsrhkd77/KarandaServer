package kr.karanda.karandaserver

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.springframework.context.annotation.Configuration
import java.io.File
import java.io.FileInputStream
import javax.annotation.PostConstruct

@Configuration
class FirebaseConfiguration {

    @PostConstruct
    fun initializeFirebase() {
        val token = FileInputStream(File("./credentials/token.json"))
        val options: FirebaseOptions =
            FirebaseOptions.builder().setCredentials(GoogleCredentials.fromStream(token)).build()
        FirebaseApp.initializeApp(options)
        /*
        * In google cloud: FirebaseApp.initializeApp()
        *
        */
    }
}